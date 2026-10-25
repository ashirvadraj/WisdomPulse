package com.wisdompulse.app.util

import android.content.Context
import android.content.res.AssetFileDescriptor
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

    private fun prepareAndPlayAsset(mp: MediaPlayer, assetName: String) {
        val assetPath = "audio/$assetName"
        val cacheFile = File(context.cacheDir, assetName)

        // 1. If previously cached and intact, play directly from local cache (fastest & most reliable)
        if (cacheFile.exists() && cacheFile.length() > 5000L) {
            try {
                mp.reset()
                mp.setDataSource(cacheFile.absolutePath)
                mp.prepare()
                mp.start()
                return
            } catch (e: Exception) {
                e.printStackTrace()
                cacheFile.delete()
                mp.reset()
            }
        }

        // 2. Try playing directly from asset via AssetFileDescriptor (for uncompressed APK assets)
        try {
            val afd: AssetFileDescriptor = context.assets.openFd(assetPath)
            mp.reset()
            mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            mp.prepare()
            mp.start()
            try { afd.close() } catch (e: Exception) {}
            return
        } catch (e: Exception) {
            e.printStackTrace()
            mp.reset()
        }

        // 3. Infallible Fallback: Stream from assets InputStream into cacheFile and play
        // context.assets.open() works on 100% of Android devices regardless of APK compression
        context.assets.open(assetPath).use { input ->
            FileOutputStream(cacheFile).use { output ->
                input.copyTo(output)
            }
        }
        mp.reset()
        mp.setDataSource(cacheFile.absolutePath)
        mp.prepare()
        mp.start()
    }

    private fun startAuthenticPlayback(quote: Quote, mode: AudioMode) {
        try {
            cleanupMediaPlayer()

            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                try {
                    setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                setOnCompletionListener {
                    this@AtalAudioPlayer.isPaused = false
                    stop()
                }
                setOnErrorListener { _, what, extra ->
                    stop()
                    notifyError("प्लेबैक त्रुटि ($what, $extra)")
                    true
                }
            }

            val assetName = "poem_${quote.id}.mp3"
            var playSuccess = false

            // Check if this quote has an offline asset
            if (quote.id in authenticPoemIds) {
                try {
                    prepareAndPlayAsset(mp, assetName)
                    playSuccess = true
                } catch (e: Exception) {
                    e.printStackTrace()
                    playSuccess = false
                }
            }

            // If offline asset failed or not present, try online stream
            if (!playSuccess) {
                val onlineUrl = getOnlineStreamUrl(quote.id)
                if (onlineUrl != null) {
                    mp.setDataSource(onlineUrl)
                    mp.prepareAsync()
                    mp.setOnPreparedListener { preparedMp ->
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
                    return
                } else {
                    notifyError("ऑडियो फ़ाइल उपलब्ध नहीं है")
                    stop()
                    return
                }
            }

            this.mediaPlayer = mp
            this.isPlaying = true
            this.isPaused = false
            notifyStateChanged(PlayState.PLAYING, mode)
            handler.post(progressRunnable)

            // Start foreground playback service safely (does not break playback if service encounters any delay)
            try {
                AtalAudioService.startPlayback(context, quote)
            } catch (e: Exception) {
                e.printStackTrace()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            notifyError("ऑडियो शुरू नहीं हो सका: ${e.localizedMessage ?: e.message}")
            stop()
        }
    }

    private fun startOratoricalRecital(quote: Quote) {
        if (!isTtsReady || tts == null) {
            notifyError("काव्य पाठ इंजन तैयार हो रहा है...")
            return
        }

        AtalProsodyHelper.configureAtalVoice(tts!!)
        val speechText = AtalProsodyHelper.formatTextForAtalRecital(quote.title, quote.text)

        tts?.speak(speechText, TextToSpeech.QUEUE_FLUSH, null, "ATAL_RECITAL_${quote.id}")
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
        if (isPaused && mediaPlayer != null) {
            mediaPlayer?.start()
            isPlaying = true
            isPaused = false
            handler.post(progressRunnable)
            notifyStateChanged(PlayState.PLAYING, currentMode)
            AtalAudioService.updatePlaybackState(context, currentQuote, isPlaying = true)
        } else {
            start(quote)
        }
    }

    fun togglePlayPause(quote: Quote) {
        if (isPlaying) {
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
            tts?.setLanguage(Locale("hi", "IN"))
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
