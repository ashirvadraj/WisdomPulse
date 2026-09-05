package com.wisdompulse.app.util

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.wisdompulse.app.model.Quote
import java.util.Locale

class AtalAudioPlayer(
    private val context: Context,
    private val onStateChanged: (isPlaying: Boolean, mode: AudioMode) -> Unit,
    private val onProgress: (currentMs: Int, totalMs: Int) -> Unit,
    private val onError: (message: String) -> Unit
) : TextToSpeech.OnInitListener {

    enum class AudioMode {
        ORIGINAL_VOICE,
        JAGJIT_SINGH_GHAZAL,
        ORATORICAL_RECITAL
    }

    private var mediaPlayer: MediaPlayer? = null
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private var currentMode: AudioMode = AudioMode.ORATORICAL_RECITAL
    private var isPlaying = false

    private val handler = Handler(Looper.getMainLooper())
    private val progressRunnable = object : Runnable {
        override fun run() {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    onProgress(mp.currentPosition, mp.duration)
                    handler.postDelayed(this, 500)
                }
            }
        }
    }

    init {
        tts = TextToSpeech(context, this)
    }

    fun hasAuthenticRecording(quoteId: Int): Boolean {
        return try {
            val list = context.assets.list("audio") ?: emptyArray()
            list.contains("poem_$quoteId.mp3") || getOnlineStreamUrl(quoteId) != null
        } catch (e: Exception) {
            false
        }
    }

    fun isMusicalGhazal(quoteId: Int): Boolean {
        return quoteId in listOf(12, 13, 37, 40, 46)
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

    fun start(quote: Quote, forceMode: AudioMode? = null) {
        stop()

        val mode = forceMode ?: when {
            isMusicalGhazal(quote.id) -> AudioMode.JAGJIT_SINGH_GHAZAL
            hasAuthenticRecording(quote.id) -> AudioMode.ORIGINAL_VOICE
            else -> AudioMode.ORATORICAL_RECITAL
        }
        currentMode = mode

        if (mode == AudioMode.ORIGINAL_VOICE || mode == AudioMode.JAGJIT_SINGH_GHAZAL) {
            startAuthenticPlayback(quote.id, mode)
        } else {
            startOratoricalRecital(quote)
        }
    }

    private fun startAuthenticPlayback(quoteId: Int, mode: AudioMode) {
        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )

                val list = context.assets.list("audio") ?: emptyArray()
                if (list.contains("poem_$quoteId.mp3")) {
                    val afd: AssetFileDescriptor = context.assets.openFd("audio/poem_$quoteId.mp3")
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    afd.close()
                    prepare()
                    start()
                } else {
                    val onlineUrl = getOnlineStreamUrl(quoteId)
                    if (onlineUrl != null) {
                        setDataSource(onlineUrl)
                        prepareAsync()
                        setOnPreparedListener {
                            it.start()
                            this@AtalAudioPlayer.isPlaying = true
                            onStateChanged(true, mode)
                            handler.post(progressRunnable)
                        }
                        return
                    } else {
                        onError("ऑडियो फ़ाइल उपलब्ध नहीं है")
                        return
                    }
                }

                setOnCompletionListener {
                    stop()
                }
                setOnErrorListener { _, _, _ ->
                    stop()
                    onError("ऑडियो चलाने में त्रुटि हुई")
                    true
                }
            }

            isPlaying = true
            onStateChanged(true, mode)
            handler.post(progressRunnable)

        } catch (e: Exception) {
            e.printStackTrace()
            onError("ऑडियो शुरू नहीं हो सका")
            stop()
        }
    }

    private fun startOratoricalRecital(quote: Quote) {
        if (!isTtsReady || tts == null) {
            onError("काव्य पाठ इंजन तैयार हो रहा है...")
            return
        }

        AtalProsodyHelper.configureAtalVoice(tts!!)
        val speechText = AtalProsodyHelper.formatTextForAtalRecital(quote.title, quote.text)

        tts?.speak(speechText, TextToSpeech.QUEUE_FLUSH, null, "ATAL_RECITAL_${quote.id}")
        isPlaying = true
        onStateChanged(true, AudioMode.ORATORICAL_RECITAL)
    }

    fun pause() {
        if (currentMode == AudioMode.ORIGINAL_VOICE || currentMode == AudioMode.JAGJIT_SINGH_GHAZAL) {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                    isPlaying = false
                    handler.removeCallbacks(progressRunnable)
                    onStateChanged(false, currentMode)
                }
            }
        } else {
            stop()
        }
    }

    fun resume(quote: Quote) {
        if (currentMode == AudioMode.ORIGINAL_VOICE || currentMode == AudioMode.JAGJIT_SINGH_GHAZAL) {
            mediaPlayer?.let {
                it.start()
                isPlaying = true
                handler.post(progressRunnable)
                onStateChanged(true, currentMode)
            } ?: start(quote, currentMode)
        } else {
            start(quote, AudioMode.ORATORICAL_RECITAL)
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
        onStateChanged(false, currentMode)
    }

    fun seekTo(positionMs: Int) {
        mediaPlayer?.seekTo(positionMs)
    }

    fun isPlaying(): Boolean = isPlaying

    fun getCurrentMode(): AudioMode = currentMode

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