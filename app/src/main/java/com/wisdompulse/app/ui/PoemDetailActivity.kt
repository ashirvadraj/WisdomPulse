package com.wisdompulse.app.ui

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.wisdompulse.app.R
import com.wisdompulse.app.data.QuoteRepository
import com.wisdompulse.app.databinding.ActivityPoemDetailBinding
import com.wisdompulse.app.model.Quote
import com.wisdompulse.app.util.AtalAudioPlayer
import com.wisdompulse.app.util.PoemCardShareHelper

class PoemDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPoemDetailBinding
    private lateinit var repository: QuoteRepository
    private var quote: Quote? = null

    private var audioPlayer: AtalAudioPlayer? = null
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

        setupAudioPlayer()
        setupPoemContent()
        setupToolbarActions()
        setupThemeControls()
        setupFontControls()

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

        // Check if authentic recording exists
        if (audioPlayer?.hasAuthenticRecording(q.id) == true) {
            binding.tvOriginalVoiceBadge.visibility = View.VISIBLE
            binding.btnRecitePoem.text = "अटल जी के स्वर में सुनें"
        } else {
            binding.tvOriginalVoiceBadge.visibility = View.GONE
            binding.btnRecitePoem.text = "अटल शैली काव्य पाठ"
        }
    }

    private fun setupAudioPlayer() {
        audioPlayer = AtalAudioPlayer(
            context = this,
            onStateChanged = { isPlaying, mode ->
                runOnUiThread {
                    if (isPlaying) {
                        binding.btnRecitePoem.text = "विराम"
                        binding.btnRecitePoem.setIconResource(android.R.drawable.ic_media_pause)
                        binding.audioProgressRow.visibility = View.VISIBLE
                        if (mode == AtalAudioPlayer.AudioMode.ORIGINAL_VOICE) {
                            binding.tvAudioModeBadge.text = "🎙️ अटल जी का मूल स्वर"
                            binding.seekBarAudio.visibility = View.VISIBLE
                        } else {
                            binding.tvAudioModeBadge.text = "🎙️ अटल वाग्मिता शैली (गंभीर स्वर व विराम)"
                            binding.seekBarAudio.visibility = View.GONE
                            binding.tvAudioTime.text = "काव्य पाठ जारी..."
                        }
                    } else {
                        val q = quote
                        if (q != null && audioPlayer?.hasAuthenticRecording(q.id) == true) {
                            binding.btnRecitePoem.text = "अटल जी के स्वर में सुनें"
                        } else {
                            binding.btnRecitePoem.text = "अटल शैली काव्य पाठ"
                        }
                        binding.btnRecitePoem.setIconResource(R.drawable.ic_volume_up)
                        binding.audioProgressRow.visibility = View.GONE
                        binding.seekBarAudio.visibility = View.GONE
                    }
                }
            },
            onProgress = { currentMs, totalMs ->
                runOnUiThread {
                    if (totalMs > 0) {
                        binding.seekBarAudio.max = totalMs
                        binding.seekBarAudio.progress = currentMs
                        val curSec = (currentMs / 1000) % 60
                        val curMin = (currentMs / 1000) / 60
                        val totSec = (totalMs / 1000) % 60
                        val totMin = (totalMs / 1000) / 60
                        binding.tvAudioTime.text = String.format("%02d:%02d / %02d:%02d", curMin, curSec, totMin, totSec)
                    }
                }
            },
            onError = { msg ->
                runOnUiThread {
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                }
            }
        )

        binding.seekBarAudio.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    audioPlayer?.seekTo(progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.btnRecitePoem.setOnClickListener {
            val q = quote ?: return@setOnClickListener
            if (audioPlayer?.isPlaying() == true) {
                audioPlayer?.stop()
            } else {
                audioPlayer?.start(q)
            }
        }
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
                binding.seekBarAudio.progressTintList = ColorStateList.valueOf(accent)
                binding.seekBarAudio.thumbTintList = ColorStateList.valueOf(accent)
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
                binding.seekBarAudio.progressTintList = ColorStateList.valueOf(accent)
                binding.seekBarAudio.thumbTintList = ColorStateList.valueOf(accent)
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
                binding.seekBarAudio.progressTintList = ColorStateList.valueOf(accent)
                binding.seekBarAudio.thumbTintList = ColorStateList.valueOf(accent)
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

    override fun onPause() {
        super.onPause()
        audioPlayer?.pause()
    }

    override fun onDestroy() {
        audioPlayer?.release()
        super.onDestroy()
    }
}