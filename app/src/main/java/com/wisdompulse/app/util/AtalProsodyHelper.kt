package com.wisdompulse.app.util

import android.speech.tts.TextToSpeech
import java.util.Locale

object AtalProsodyHelper {

    const val ATAL_PITCH = 0.72f // Deep resonant baritone
    const val ATAL_SPEECH_RATE = 0.70f // Deliberate oratorical tempo

    /**
     * Pre-processes poetic text to insert dramatic breath stops, caesuras,
     * and rhythmic pauses characteristic of Atal Bihari Vajpayee's speech style.
     */
    fun formatTextForAtalRecital(title: String?, rawPoemText: String): String {
        val intro = if (!title.isNullOrEmpty()) {
            "$title... । ... रचयिता, अटल बिहारी वाजपेयी... । ... "
        } else {
            "रचयिता, अटल बिहारी वाजपेयी... । ... "
        }

        // Split stanzas
        val stanzas = rawPoemText.split("\n\n")
        val formattedStanzas = stanzas.map { stanza ->
            val lines = stanza.split("\n")
            lines.joinToString(" ... \n") { line ->
                line.trim()
                    .replace("?", "? ... ")
                    .replace("!", "! ... ")
                    .replace(";", "; ... ")
                    .replace("—", " ... ")
            }
        }

        // Deliberate pause between stanzas
        val body = formattedStanzas.joinToString("\n ... । ... \n")

        return intro + body + " ... । ... "
    }

    /**
     * Configures the TextToSpeech engine with Atal Ji's baritone pitch,
     * oratorical rate, and a male Hindi voice if available.
     */
    fun configureAtalVoice(tts: TextToSpeech) {
        tts.setPitch(ATAL_PITCH)
        tts.setSpeechRate(ATAL_SPEECH_RATE)

        try {
            val voices = tts.voices
            val hindiVoices = voices?.filter { it.locale.language == "hi" }
            val maleVoice = hindiVoices?.firstOrNull { voice ->
                voice.name.contains("male", ignoreCase = true) ||
                voice.name.contains("hi-in-x-hie", ignoreCase = true) ||
                voice.name.contains("hi-in-x-hid", ignoreCase = true)
            } ?: hindiVoices?.firstOrNull()

            if (maleVoice != null) {
                tts.voice = maleVoice
            } else {
                tts.setLanguage(Locale("hi", "IN"))
            }
        } catch (e: Exception) {
            tts.setLanguage(Locale("hi", "IN"))
        }
    }
}