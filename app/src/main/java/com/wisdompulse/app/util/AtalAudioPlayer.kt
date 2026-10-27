package com.wisdompulse.app.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.wisdompulse.app.model.Quote
import com.wisdompulse.app.service.AtalAudioService
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class AtalAudioPlayer(
    private val context: Context,
    private var onStateChanged: ((state: PlayState, mode: AudioMode) -> Unit)? = null,
    private var onProgress: ((currentMs: Int, totalMs: Int) -> Unit)? = null,
    private var onError: ((message: String) -> Unit)? = null
) : TextToSpeech.OnInitListener {

    interface AudioPlayerListener {
        fun onStateChanged(state: PlayState, mode: AudioMode)
        fun onProgress(currentMs: Int, totalMs: Int)
        fun onError(message: String)
    }

    companion object {
        @Volatile
        private var instance: AtalAudioPlayer? = null

        fun getInstance(context: Context): AtalAudioPlayer {
            return instance ?: synchronized(this) {
                instance ?: AtalAudioPlayer(context.applicationContext).also { instance = it }
            }
        }
    }

    enum class PlayState {
        PLAYING,
        PAUSED,
        STOPPED
    }

    enum class AudioMode {
        ORIGINAL_VOICE,
        JAGJIT_SINGH_GHAZAL,
        MUSICAL_RENDITION,
        ORATORICAL_RECITAL
    }

    private var mediaPlayer: MediaPlayer? = null
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private var currentMode: AudioMode = AudioMode.ORATORICAL_RECITAL
    private var isPlaying = false
    private var isPaused = false
    private var currentQuote: Quote? = null

    private var listener: AudioPlayerListener? = null

    private val handler = Handler(Looper.getMainLooper())
    private val progressRunnable = object : Runnable {
        override fun run() {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    notifyProgress(mp.currentPosition, mp.duration)
                    handler.postDelayed(this, 500)
                }
            }
        }
    }

    init {
        tts = TextToSpeech(context, this)
    }

    fun setListener(listener: AudioPlayerListener?) {
        this.listener = listener
        if (isPlaying) {
            listener?.onStateChanged(PlayState.PLAYING, currentMode)
            mediaPlayer?.let { mp ->
                try {
                    listener?.onProgress(mp.currentPosition, mp.duration)
                } catch (e: Exception) {}
            }
        } else if (isPaused) {
            listener?.onStateChanged(PlayState.PAUSED, currentMode)
            mediaPlayer?.let { mp ->
                try {
                    listener?.onProgress(mp.currentPosition, mp.duration)
                } catch (e: Exception) {}
            }
        }
    }

    private fun notifyStateChanged(state: PlayState, mode: AudioMode) {
        onStateChanged?.invoke(state, mode)
        listener?.onStateChanged(state, mode)
    }

    private fun notifyProgress(currentMs: Int, totalMs: Int) {
        onProgress?.invoke(currentMs, totalMs)
        listener?.onProgress(currentMs, totalMs)
    }

    private fun notifyError(message: String) {
        onError?.invoke(message)
        listener?.onError(message)
    }

    // 26 Authentic Audio Poems in assets/audio/poem_<id>.mp3
    private val authenticPoemIds = setOf(
        1, 3, 4, 6, 7, 9, 11, 12, 13, 14, 15, 20, 24, 25, 26, 27, 29, 30, 31, 36, 37, 40, 41, 46, 49, 51
    )

    fun hasAuthenticRecording(quoteId: Int): Boolean {
        return quoteId in authenticPoemIds || getOnlineStreamUrl(quoteId) != null
    }

    fun isMusicalGhazal(quoteId: Int): Boolean {
        return quoteId in listOf(12, 13, 15, 37, 40, 46)
    }

    fun isMusicalRendition(quoteId: Int): Boolean {
        return quoteId in listOf(14, 31, 51)
    }

    fun isOriginalVoice(quoteId: Int): Boolean {
        return quoteId in listOf(1, 3, 4, 6, 7, 9, 11, 20, 24, 25, 26, 27, 29, 30, 36, 41, 49)
    }

    fun getOnlineStreamUrl(quoteId: Int): String? {
        return when (quoteId) {
            46 -> "https://archive.org/download/02-ek-baras-beet-gaya/Kya_Khoya_Kya_Paya.mp3"
            40 -> "https://archive.org/download/02-ek-baras-beet-gaya/Kadam_Milakar_Chalna_Hoga.mp3"
            12 -> "https://archive.org/download/02-ek-baras-beet-gaya/Door_Kahin_Koi_Rota_Hai.mp3"
            13 -> "https://archive.org/download/02-ek-baras-beet-gaya/Jeevan_Beet_Chala.mp3"
            37 -> "https://archive.org/download/02-ek-baras-beet-gaya/Ek_Baras_Beet_Gaya.mp3"
            49 -> "https://archive.org/download/01-aao-man-ki-gaanthe-khole/03%20jung%20na%20hone%20denge.mp3"
            20 -> "https://archive.org/download/01-aao-man-ki-gaanthe-khole/01%20aao%20man%20ki%20gaanthe%20khole.mp3"
            else -> null
        }
    }

    private fun cleanupMediaPlayer() {
        handler.removeCallbacks(progressRunnable)
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {}
        mediaPlayer = null

        try {
            tts?.stop()
        } catch (e: Exception) {}

        isPlaying = false
        isPaused = false
    }

    fun start(quote: Quote, forceMode: AudioMode? = null) {
        cleanupMediaPlayer()
        currentQuote = quote

        val mode = forceMode ?: when {
            isMusicalGhazal(quote.id) -> AudioMode.JAGJIT_SINGH_GHAZAL
            isMusicalRendition(quote.id) -> AudioMode.MUSICAL_RENDITION
            hasAuthenticRecording(quote.id) -> AudioMode.ORIGINAL_VOICE
            else -> AudioMode.ORATORICAL_RECITAL
        }
        currentMode = mode

        if (mode != AudioMode.ORATORICAL_RECITAL) {
            startAuthenticPlayback(quote, mode)
        } else {
            startOratoricalRecital(quote)
        }
    }

    /**
     * Extracts the specific poem's MP3 directly into cacheDir without temporary file rename.
     * Direct extraction guarantees 100% reliability on all Android devices and filesystems.
     */
    private fun getOrExtractAudioFile(quoteId: Int): File {
        val assetName = "poem_$quoteId.mp3"
        val assetPath = "audio/$assetName"
        val cacheFile = File(context.cacheDir, assetName)

        var expectedSize = -1L
        try {
            context.assets.openFd(assetPath).use { afd ->
                expectedSize = afd.length
            }
        } catch (e: Exception) {}

        val needExtract = !cacheFile.exists()
            || cacheFile.length() < 1000L
            || (expectedSize > 0 && cacheFile.length() != expectedSize)

        if (needExtract) {
            context.assets.open(assetPath).use { input ->
                FileOutputStream(cacheFile).use { output ->
                    input.copyTo(output)
                }
            }
        }

        if (!cacheFile.exists() || cacheFile.length() < 1000L) {
            throw java.io.FileNotFoundException("Could not extract audio for poem $quoteId")
        }

        return cacheFile
    }

    private fun startAuthenticPlayback(quote: Quote, mode: AudioMode) {
        try {
            cleanupMediaPlayer()
            currentQuote = quote

            var playSuccess = false

            // 1. Try offline asset first (26 authentic audio tracks)
            if (quote.id in authenticPoemIds) {
                try {
                    val file = getOrExtractAudioFile(quote.id)

                    val mp = MediaPlayer().apply {
                        try {
                            setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .build()
                        )
                        setOnCompletionListener {
                            this@AtalAudioPlayer.isPaused = false
                            stop()
                        }
                        setOnErrorListener { _, what, extra ->
                            stop()
                            notifyError("प्लेबैक त्रुटि ($what, $extra)")
                            true
                        }
                        setDataSource(file.absolutePath)
                        prepare()
                        start()
                    }

                    this.mediaPlayer = mp
                    this.isPlaying = true
                    this.isPaused = false
                    notifyStateChanged(PlayState.PLAYING, mode)
                    handler.post(progressRunnable)

                    try {
                        AtalAudioService.startPlayback(context, quote)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    playSuccess = true
                } catch (e: Exception) {
                    e.printStackTrace()
                    playSuccess = false
                }
            }

            // 2. Fall back to online stream if offline failed or not available
            if (!playSuccess) {
                val onlineUrl = getOnlineStreamUrl(quote.id)
                if (onlineUrl != null) {
                    val mp = MediaPlayer().apply {
                        try {
                            setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
                        } catch (e: Exception) { e.printStackTrace() }
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .build()
                        )
                        setOnCompletionListener {
                            this@AtalAudioPlayer.isPaused = false
                            stop()
                        }
                        setOnErrorListener { _, what, extra ->
                            stop()
                            notifyError("प्लेबैक त्रुटि ($what, $extra)")
                            true
                        }
                        setDataSource(onlineUrl)
                        prepareAsync()
                        setOnPreparedListener { preparedMp ->
                            preparedMp.start()
                            this@AtalAudioPlayer.mediaPlayer = preparedMp
                            this@AtalAudioPlayer.isPlaying = true
                            this@AtalAudioPlayer.isPaused = false
                            notifyStateChanged(PlayState.PLAYING, mode)
                            handler.post(progressRunnable)
                            try {
                                AtalAudioService.startPlayback(context, quote)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                } else {
                    notifyError("इस कविता के लिए मूल ऑडियो उपलब्ध नहीं है।")
                    stop()
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            notifyError("ऑडियो शुरू नहीं हो सका: ${e.localizedMessage ?: e.message}")
            stop()
        }
    }

    private fun startOratoricalRecital(quote: Quote) {
        if (!isTtsReady || tts == null) {
            notifyError("इस कविता का मूल ऑडियो उपलब्ध नहीं है। काव्य पाठ तैयार हो रहा है...")
            return
        }

        AtalProsodyHelper.configureAtalVoice(tts!!)
        val speechText = AtalProsodyHelper.formatTextForAtalRecital(quote.title, quote.text)

        val res = tts?.speak(speechText, TextToSpeech.QUEUE_FLUSH, null, "ATAL_RECITAL_${quote.id}")
        if (res == TextToSpeech.ERROR) {
            notifyError("इस कविता के लिए मूल ऑडियो उपलब्ध नहीं है एवं डिवाइस में हिंदी वॉइस पाठ अनुपलब्ध है।")
            stop()
            return
        }
        isPlaying = true
        isPaused = false
        notifyStateChanged(PlayState.PLAYING, AudioMode.ORATORICAL_RECITAL)
    }

    fun pause() {
        if (currentMode != AudioMode.ORATORICAL_RECITAL) {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                    isPlaying = false
                    isPaused = true
                    handler.removeCallbacks(progressRunnable)
                    notifyStateChanged(PlayState.PAUSED, currentMode)
                    AtalAudioService.updatePlaybackState(context, currentQuote, isPlaying = false)
                }
            }
        } else {
            stop()
        }
    }

    fun resume(quote: Quote) {
        if (currentQuote?.id == quote.id && isPaused && mediaPlayer != null) {
            mediaPlayer?.start()
            isPlaying = true
            isPaused = false
            handler.post(progressRunnable)
            notifyStateChanged(PlayState.PLAYING, currentMode)
            try {
                AtalAudioService.updatePlaybackState(context, currentQuote, isPlaying = true)
            } catch (e: Exception) {}
        } else {
            start(quote)
        }
    }

    fun togglePlayPause(quote: Quote) {
        if (currentQuote == null || currentQuote?.id != quote.id) {
            // A different poem was selected: immediately switch and start the new poem!
            start(quote)
        } else if (isPlaying) {
            pause()
        } else if (isPaused) {
            resume(quote)
        } else {
            start(quote)
        }
    }

    fun seekBy(deltaMs: Int) {
        mediaPlayer?.let { mp ->
            val target = (mp.currentPosition + deltaMs).coerceIn(0, mp.duration)
            mp.seekTo(target)
            notifyProgress(target, mp.duration)
        }
    }

    fun stop() {
        handler.removeCallbacks(progressRunnable)
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {}
        mediaPlayer = null

        try {
            tts?.stop()
        } catch (e: Exception) {}

        isPlaying = false
        isPaused = false
        notifyStateChanged(PlayState.STOPPED, currentMode)

        AtalAudioService.stopPlayback(context)
    }

    fun seekTo(positionMs: Int) {
        mediaPlayer?.let { mp ->
            mp.seekTo(positionMs)
            notifyProgress(positionMs, mp.duration)
        }
    }

    fun isPlaying(): Boolean = isPlaying

    fun isPaused(): Boolean = isPaused

    fun getCurrentMode(): AudioMode = currentMode

    fun getCurrentQuote(): Quote? = currentQuote

    fun getCurrentPosition(): Int = try { mediaPlayer?.currentPosition ?: 0 } catch (e: Exception) { 0 }

    fun getDuration(): Int = try { mediaPlayer?.duration ?: 0 } catch (e: Exception) { 0 }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true
            val res = tts?.setLanguage(Locale("hi", "IN"))
            if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale("hi"))
            }
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    handler.post { stop() }
                }
                override fun onError(utteranceId: String?) {
                    handler.post { stop() }
                }
            })
        }
    }

    fun release() {
        stop()
        try {
            tts?.shutdown()
        } catch (e: Exception) {}
        tts = null
    }
}
