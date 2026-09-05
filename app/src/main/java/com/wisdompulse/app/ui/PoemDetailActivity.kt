package com.wisdompulse.app.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.wisdompulse.app.R
import com.wisdompulse.app.data.QuoteRepository
import com.wisdompulse.app.databinding.ActivityPoemDetailBinding
import com.wisdompulse.app.model.Quote
import com.wisdompulse.app.util.PoemCardShareHelper
import java.util.Locale

class PoemDetailActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ActivityPoemDetailBinding
    private lateinit var repository: QuoteRepository
    private var quote: Quote? = null

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var isSpeaking = false

    private var currentFontSizeSp = 18f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPoemDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = QuoteRepository(this)
        quote = intent.getSerializableExtra("extra_quote") as? Quote

        if (quote == null) {
            Toast.makeText(this, "कविता नहीं मिली", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Mark as read
        quote?.let { repository.markPoemAsRead(it.id) }

        tts = TextToSpeech(this, this)

        setupPoemContent()
        setupToolbarActions()
        setupThemeControls()
        setupFontControls()
        setupAudioRecital()

        // Apply saved theme & font size
        applyTheme(repository.getReadingTheme())
        currentFontSizeSp = repository.getFontSizeSp()
        applyFontSize(currentFontSizeSp)
    }

    private fun setupPoemContent() {
        val q = quote ?: return
        binding.tvDetailSection.text = q.category
        binding.tvDetailTitle.text = q.title ?: "कविता"
        binding.tvDetailPoemText.text = q.text
        binding.tvDetailAuthorSignoff.text = "— " + q.author
    }

    private fun setupToolbarActions() {
        binding.btnBack.setOnClickListener { finish() }

        val q = quote ?: return
        updateBookmarkIcon(repository.isFavorite(q.id))

        binding.btnBookmark.setOnClickListener {
            val isFav = repository.toggleFavorite(q.id)
            updateBookmarkIcon(isFav)
            val msg = if (isFav) "पसंदीदा में जोड़ा गया!" else "पसंदीदा से हटाया गया"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        binding.btnShareCard.setOnClickListener {
            PoemCardShareHelper.renderAndSharePoemCard(this, q)
        }

        binding.btnThemeToggle.setOnClickListener {
            binding.themeSelectorBar.visibility =
                if (binding.themeSelectorBar.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
    }

    private fun updateBookmarkIcon(isFav: Boolean) {
        if (isFav) {
            binding.btnBookmark.setImageResource(R.drawable.ic_favorite)
        } else {
            binding.btnBookmark.setImageResource(R.drawable.ic_favorite_border)
        }
    }

    private fun setupThemeControls() {
        binding.btnThemeParchment.setOnClickListener {
            applyTheme("parchment")
            repository.setReadingTheme("parchment")
        }
        binding.btnThemeMidnight.setOnClickListener {
            applyTheme("midnight")
            repository.setReadingTheme("midnight")
        }
        binding.btnThemeSlate.setOnClickListener {
            applyTheme("slate")
            repository.setReadingTheme("slate")
        }
    }

    private fun applyTheme(themeKey: String) {
        when (themeKey) {
            "midnight" -> {
                val bg = ContextCompat.getColor(this, R.color.midnight_bg)
                val text = ContextCompat.getColor(this, R.color.midnight_text)
                val textSec = ContextCompat.getColor(this, R.color.midnight_text_sec)
                val accent = ContextCompat.getColor(this, R.color.midnight_accent)
                val card = ContextCompat.getColor(this, R.color.midnight_card)
                val divider = ContextCompat.getColor(this, R.color.midnight_divider)

                binding.readingRoot.setBackgroundColor(bg)
                binding.tvHeaderBookTitle.setTextColor(text)
                binding.tvDetailTitle.setTextColor(accent)
                binding.tvDetailFlourish.setTextColor(accent)
                binding.tvDetailFlourishBottom.setTextColor(accent)
                binding.tvDetailPoemText.setTextColor(text)
                binding.tvDetailAuthorSignoff.setTextColor(textSec)

                binding.btnBack.imageTintList = ColorStateList.valueOf(accent)
                binding.btnThemeToggle.imageTintList = ColorStateList.valueOf(accent)
                binding.btnBookmark.imageTintList = ColorStateList.valueOf(accent)
                binding.btnShareCard.imageTintList = ColorStateList.valueOf(accent)

                binding.bottomReadingBar.setCardBackgroundColor(card)
                binding.barDivider.setBackgroundColor(divider)
                binding.btnFontDecrease.setTextColor(accent)
                binding.btnFontIncrease.setTextColor(accent)
                binding.tvFontSizeLabel.setTextColor(textSec)
                binding.btnRecitePoem.setTextColor(accent)
                binding.btnRecitePoem.iconTint = ColorStateList.valueOf(accent)
            }
            "slate" -> {
                val bg = ContextCompat.getColor(this, R.color.slate_bg)
                val text = ContextCompat.getColor(this, R.color.slate_text)
                val textSec = ContextCompat.getColor(this, R.color.slate_text_sec)
                val accent = ContextCompat.getColor(this, R.color.slate_accent)
                val card = ContextCompat.getColor(this, R.color.slate_card)
                val divider = ContextCompat.getColor(this, R.color.slate_divider)

                binding.readingRoot.setBackgroundColor(bg)
                binding.tvHeaderBookTitle.setTextColor(text)
                binding.tvDetailTitle.setTextColor(accent)
                binding.tvDetailFlourish.setTextColor(accent)
                binding.tvDetailFlourishBottom.setTextColor(accent)
                binding.tvDetailPoemText.setTextColor(text)
                binding.tvDetailAuthorSignoff.setTextColor(textSec)

                binding.btnBack.imageTintList = ColorStateList.valueOf(accent)
                binding.btnThemeToggle.imageTintList = ColorStateList.valueOf(accent)
                binding.btnBookmark.imageTintList = ColorStateList.valueOf(accent)
                binding.btnShareCard.imageTintList = ColorStateList.valueOf(accent)

                binding.bottomReadingBar.setCardBackgroundColor(card)
                binding.barDivider.setBackgroundColor(divider)
                binding.btnFontDecrease.setTextColor(accent)
                binding.btnFontIncrease.setTextColor(accent)
                binding.tvFontSizeLabel.setTextColor(textSec)
                binding.btnRecitePoem.setTextColor(accent)
                binding.btnRecitePoem.iconTint = ColorStateList.valueOf(accent)
            }
            else -> { // parchment
                val bg = ContextCompat.getColor(this, R.color.parchment_bg)
                val text = ContextCompat.getColor(this, R.color.parchment_text)
                val textSec = ContextCompat.getColor(this, R.color.parchment_text_sec)
                val accent = ContextCompat.getColor(this, R.color.parchment_accent)
                val card = ContextCompat.getColor(this, R.color.parchment_card)
                val divider = ContextCompat.getColor(this, R.color.parchment_divider)

                binding.readingRoot.setBackgroundColor(bg)
                binding.tvHeaderBookTitle.setTextColor(text)
                binding.tvDetailTitle.setTextColor(accent)
                binding.tvDetailFlourish.setTextColor(accent)
                binding.tvDetailFlourishBottom.setTextColor(accent)
                binding.tvDetailPoemText.setTextColor(text)
                binding.tvDetailAuthorSignoff.setTextColor(textSec)

                binding.btnBack.imageTintList = ColorStateList.valueOf(accent)
                binding.btnThemeToggle.imageTintList = ColorStateList.valueOf(accent)
                binding.btnBookmark.imageTintList = ColorStateList.valueOf(accent)
                binding.btnShareCard.imageTintList = ColorStateList.valueOf(accent)

                binding.bottomReadingBar.setCardBackgroundColor(card)
                binding.barDivider.setBackgroundColor(divider)
                binding.btnFontDecrease.setTextColor(accent)
                binding.btnFontIncrease.setTextColor(accent)
                binding.tvFontSizeLabel.setTextColor(textSec)
                binding.btnRecitePoem.setTextColor(accent)
                binding.btnRecitePoem.iconTint = ColorStateList.valueOf(accent)
            }
        }
    }

    private fun setupFontControls() {
        binding.btnFontDecrease.setOnClickListener {
            if (currentFontSizeSp > 14f) {
                currentFontSizeSp -= 2f
                applyFontSize(currentFontSizeSp)
                repository.setFontSizeSp(currentFontSizeSp)
            }
        }

        binding.btnFontIncrease.setOnClickListener {
            if (currentFontSizeSp < 28f) {
                currentFontSizeSp += 2f
                applyFontSize(currentFontSizeSp)
                repository.setFontSizeSp(currentFontSizeSp)
            }
        }
    }

    private fun applyFontSize(sizeSp: Float) {
        binding.tvDetailPoemText.textSize = sizeSp
        binding.tvFontSizeLabel.text = "${sizeSp.toInt()}sp"
    }

    private fun setupAudioRecital() {
        binding.btnRecitePoem.setOnClickListener {
            val q = quote ?: return@setOnClickListener
            if (isSpeaking) {
                stopRecital()
            } else {
                startRecital(q)
            }
        }
    }

    private fun startRecital(q: Quote) {
        if (!isTtsReady || tts == null) {
            Toast.makeText(this, "काव्य पाठ प्रारंभ हो रहा है...", Toast.LENGTH_SHORT).show()
            return
        }

        val speechText = "${q.title ?: ""}. ${q.text}. रचयिता अटल बिहारी वाजपेयी."
        tts?.setSpeechRate(0.88f)
        tts?.speak(speechText, TextToSpeech.QUEUE_FLUSH, null, "POEM_RECITAL")
        isSpeaking = true
        binding.btnRecitePoem.text = "विराम"
        binding.btnRecitePoem.setIconResource(android.R.drawable.ic_media_pause)
    }

    private fun stopRecital() {
        tts?.stop()
        isSpeaking = false
        binding.btnRecitePoem.text = "काव्य पाठ सुनें"
        binding.btnRecitePoem.setIconResource(R.drawable.ic_volume_up)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true
            tts?.setLanguage(Locale("hi", "IN"))
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    runOnUiThread { stopRecital() }
                }
                override fun onError(utteranceId: String?) {
                    runOnUiThread { stopRecital() }
                }
            })
        }
    }

    override fun onDestroy() {
        stopRecital()
        tts?.shutdown()
        super.onDestroy()
    }
}