package com.wisdompulse.app.util

import android.speech.tts.TextToSpeech
import java.util.Locale

object AtalProsodyHelper {

    const val ATAL_PITCH = 0.72f // Deep resonant baritone
    const val ATAL_SPEECH_RATE = 0.70f // Deliberate oratorical tempo

    /**
     * Pre-processes poetic text to insert dramatic breath stops, caesuras,
     * and rhythmic pauses characteristic of Atal Bihari Vajpayee's speech style.
     * Cleaned of all periods/dots/symbols so Android TTS never pronounces "डॉट".
     */
    fun formatTextForAtalRecital(title: String?, rawPoemText: String): String {
        // Strip leading numbers like "4. " or "27. " so TTS does not say "चार डॉट"
        val cleanTitle = title?.replace(Regex("""^\d+[\.\s\-]+\s*"""), "")?.trim() ?: ""

        val intro = if (cleanTitle.isNotEmpty()) {
            "$cleanTitle। रचयिता, अटल बिहारी वाजपेयी।\n\n"
        } else {
            "रचयिता, अटल बिहारी वाजपेयी।\n\n"
        }

        // Clean any dots, ellipsis, asterisks, dashes, etc. from raw text
        val cleanedText = rawPoemText
            .replace(Regex("""\.+"""), "।") // Replace single or multiple dots with purnaviram
            .replace("…", "।")              // Unicode ellipsis
            .replace("—", " , ")            // Em-dash to comma pause
            .replace("–", " , ")            // En-dash to comma pause
            .replace("-", " ")              // Hyphen to space
            .replace(Regex("""[*#_~❖✦✤•]"""), " ") // Decorative glyphs
            .replace(Regex("""[ \t]+"""), " ")

        // Split stanzas
        val stanzas = cleanedText.split(Regex("""\n\s*\n"""))
        val formattedStanzas = stanzas.map { stanza ->
            val lines = stanza.lines().map { it.trim() }.filter { it.isNotEmpty() }
            lines.map { line ->
                var l = line
                // If line doesn't end with sentence-ending punctuation, add comma for natural cadence pause
                if (!l.endsWith("।") && !l.endsWith("?") && !l.endsWith("!") && !l.endsWith(",")) {
                    l += " ,"
                }
                l
            }.joinToString("\n")
        }

        // Deliberate pause between stanzas with purnaviram and double newline
        val body = formattedStanzas.joinToString("\n।\n\n")

        return intro + body + "\n।\n"
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