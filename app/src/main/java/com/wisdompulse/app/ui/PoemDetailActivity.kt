package com.wisdompulse.app.ui

import android.content.Intent
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

        // If a different poem was playing when opening this screen, stop the old poem
        // so user does not hear an unrelated poem playing on this screen!
        audioPlayer = AtalAudioPlayer.getInstance(this)
        val activeQuote = audioPlayer?.getCurrentQuote()
        if (activeQuote != null && activeQuote.id != quote?.id && audioPlayer?.isPlaying() == true) {
            audioPlayer?.stop()
        }

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
        binding.tvDetailAuthorSignoff.text = q.author

        // Bind Poem Illustration Banner
        val imgKey = q.illustrationKey ?: "art_diya_lamp"
        val resId = resources.getIdentifier(imgKey, "drawable", packageName)
        if (resId != 0) {
            binding.ivDetailBanner.setImageResource(resId)
        } else {
            binding.ivDetailBanner.setImageResource(R.drawable.art_diya_lamp)
        }

        // Check if authentic recording or musical rendition exists
        val artist = q.audioArtist ?: ""
        val source = q.audioSource ?: ""
        val hasAudio = audioPlayer?.hasAuthenticRecording(q.id) == true

        when {
            q.id in listOf(12, 13, 15, 37, 40, 46) || artist.contains("जगजीत") -> {
                binding.tvOriginalVoiceBadge.visibility = View.VISIBLE
                val label = if (q.id == 15) "🎵 जगजीत सिंह व अटल बिहारी वाजपेयी (संवेदना)" else "🎵 गायन व संगीत: जगजीत सिंह (संवेदना)"
                binding.tvOriginalVoiceBadge.text = label
                binding.tvOriginalVoiceBadge.backgroundTintList = ColorStateList.valueOf(0xFF4A148C.toInt())
                binding.btnRecitePoem.text = if (q.id == 15) "जगजीत व अटल जी के स्वर में सुनें" else "जगजीत सिंह के स्वर में सुनें"
                binding.btnRecitePoem.visibility = View.VISIBLE
            }
            q.id == 14 -> {
                binding.tvOriginalVoiceBadge.visibility = View.VISIBLE
                binding.tvOriginalVoiceBadge.text = "🎵 गायन: शंकर महादेवन (नई दिशा)"
                binding.tvOriginalVoiceBadge.backgroundTintList = ColorStateList.valueOf(0xFF00695C.toInt())
                binding.btnRecitePoem.text = "शंकर महादेवन के स्वर में सुनें"
                binding.btnRecitePoem.visibility = View.VISIBLE
            }
            q.id == 31 -> {
                binding.tvOriginalVoiceBadge.visibility = View.VISIBLE
                binding.tvOriginalVoiceBadge.text = "🇮🇳 अमर देशभक्ति राष्ट्रगान (उनकी याद करें)"
                binding.tvOriginalVoiceBadge.backgroundTintList = ColorStateList.valueOf(0xFFE65100.toInt())
                binding.btnRecitePoem.text = "अमर गान सुनें (उनकी याद करें)"
                binding.btnRecitePoem.visibility = View.VISIBLE
            }
            q.id == 51 -> {
                binding.tvOriginalVoiceBadge.visibility = View.VISIBLE
                binding.tvOriginalVoiceBadge.text = "🎵 गायन: अलका याज्ञिक व शंकर महादेवन"
                binding.tvOriginalVoiceBadge.backgroundTintList = ColorStateList.valueOf(0xFF00695C.toInt())
                binding.btnRecitePoem.text = "गायन व संगीत में सुनें"
                binding.btnRecitePoem.visibility = View.VISIBLE
            }
            hasAudio || artist.contains("अटल") -> {
                binding.tvOriginalVoiceBadge.visibility = View.VISIBLE
                val srcLabel = if (source.isNotEmpty()) " ($source)" else ""
                binding.tvOriginalVoiceBadge.text = "🎙️ अटल जी का मूल स्वर$srcLabel"
                binding.tvOriginalVoiceBadge.backgroundTintList = ColorStateList.valueOf(0xFFC0392B.toInt())
                binding.btnRecitePoem.text = "अटल जी के स्वर में सुनें"
                binding.btnRecitePoem.visibility = View.VISIBLE
            }
            else -> {
                binding.tvOriginalVoiceBadge.visibility = View.VISIBLE
                binding.tvOriginalVoiceBadge.text = "📖 केवल पठन हेतु (मूल ऑडियो उपलब्ध नहीं)"
                binding.tvOriginalVoiceBadge.backgroundTintList = ColorStateList.valueOf(0xFF5D6D7E.toInt())
                binding.btnRecitePoem.text = "अटल शैली काव्य पाठ"
                binding.btnRecitePoem.visibility = View.VISIBLE
            }
        }
    }

    private val audioPlayerListener = object : AtalAudioPlayer.AudioPlayerListener {
        override fun onStateChanged(state: AtalAudioPlayer.PlayState, mode: AtalAudioPlayer.AudioMode) {
            runOnUiThread {
                val activeQuote = audioPlayer?.getCurrentQuote()
                if (activeQuote?.id == quote?.id) {
                    updateAudioUiState(state, mode)
                } else if (state == AtalAudioPlayer.PlayState.STOPPED) {
                    updateAudioUiState(AtalAudioPlayer.PlayState.STOPPED, mode)
                }
            }
        }

        override fun onProgress(currentMs: Int, totalMs: Int) {
            runOnUiThread {
                val activeQuote = audioPlayer?.getCurrentQuote()
                if (activeQuote?.id == quote?.id) {
                    updateAudioProgress(currentMs, totalMs)
                }
            }
        }

        override fun onError(message: String) {
            runOnUiThread {
                Toast.makeText(this@PoemDetailActivity, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupAudioPlayer() {
        audioPlayer = AtalAudioPlayer.getInstance(this)
        audioPlayer?.setListener(audioPlayerListener)

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
            audioPlayer?.togglePlayPause(q)
        }

        binding.btnStopAudio.setOnClickListener {
            audioPlayer?.stop()
        }

        binding.btnReplay10.setOnClickListener {
            audioPlayer?.seekBy(-10000)
        }

        binding.btnForward10.setOnClickListener {
            audioPlayer?.seekBy(10000)
        }
    }

    private fun updateAudioUiState(state: AtalAudioPlayer.PlayState, mode: AtalAudioPlayer.AudioMode) {
        when (state) {
            AtalAudioPlayer.PlayState.PLAYING -> {
                binding.btnRecitePoem.text = "विराम (Pause)"
                binding.btnRecitePoem.setIconResource(R.drawable.ic_pause)
                binding.btnStopAudio.visibility = View.VISIBLE
                binding.audioProgressRow.visibility = View.VISIBLE

                val q = quote
                val badgeTitle = when {
                    q?.id in listOf(12, 13, 15, 37, 40, 46) || q?.audioArtist?.contains("जगजीत") == true ->
                        if (q?.id == 15) "🎵 जगजीत सिंह व अटल जी (संवेदना)" else "🎵 जगजीत सिंह (संवेदना)"
                    q?.id == 14 -> "🎵 शंकर महादेवन (नई दिशा)"
                    q?.id == 31 -> "🇮🇳 देशभक्ति अमर गान"
                    q?.id == 51 -> "🎵 अलका याज्ञिक व शंकर महादेवन"
                    mode == AtalAudioPlayer.AudioMode.ORIGINAL_VOICE || q?.audioArtist?.contains("अटल") == true ->
                        "🎙️ अटल जी का मूल स्वर"
                    else -> "🎙️ अटल वाग्मिता शैली"
                }
                binding.tvAudioModeBadge.text = badgeTitle

                if (mode == AtalAudioPlayer.AudioMode.ORATORICAL_RECITAL) {
                    binding.seekBarAudio.visibility = View.GONE
                    binding.btnReplay10.visibility = View.GONE
                    binding.btnForward10.visibility = View.GONE
                    binding.tvAudioTime.text = "काव्य पाठ जारी..."
                } else {
                    binding.seekBarAudio.visibility = View.VISIBLE
                    binding.btnReplay10.visibility = View.VISIBLE
                    binding.btnForward10.visibility = View.VISIBLE
                }
            }
            AtalAudioPlayer.PlayState.PAUSED -> {
                binding.btnRecitePoem.text = "जारी रखें (Resume)"
                binding.btnRecitePoem.setIconResource(R.drawable.ic_play_arrow)
                binding.btnStopAudio.visibility = View.VISIBLE
                binding.audioProgressRow.visibility = View.VISIBLE
                if (mode != AtalAudioPlayer.AudioMode.ORATORICAL_RECITAL) {
                    binding.seekBarAudio.visibility = View.VISIBLE
                    binding.btnReplay10.visibility = View.VISIBLE
                    binding.btnForward10.visibility = View.VISIBLE
                }
            }
            AtalAudioPlayer.PlayState.STOPPED -> {
                val q = quote
                when {
                    q != null && (q.id in listOf(12, 13, 15, 37, 40, 46) || q.audioArtist?.contains("जगजीत") == true) -> {
                        binding.btnRecitePoem.text = if (q.id == 15) "जगजीत व अटल जी के स्वर में सुनें" else "जगजीत सिंह के स्वर में सुनें"
                    }
                    q?.id == 14 -> binding.btnRecitePoem.text = "शंकर महादेवन के स्वर में सुनें"
                    q?.id == 31 -> binding.btnRecitePoem.text = "अमर गान सुनें (उनकी याद करें)"
                    q?.id == 51 -> binding.btnRecitePoem.text = "गायन व संगीत में सुनें"
                    q != null && (audioPlayer?.hasAuthenticRecording(q.id) == true || q.audioArtist?.contains("अटल") == true) -> {
                        binding.btnRecitePoem.text = "अटल जी के स्वर में सुनें"
                    }
                    else -> {
                        binding.btnRecitePoem.text = "अटल शैली काव्य पाठ"
                    }
                }
                binding.btnRecitePoem.setIconResource(R.drawable.ic_volume_up)
                binding.btnStopAudio.visibility = View.GONE
                binding.audioProgressRow.visibility = View.GONE
                binding.seekBarAudio.visibility = View.GONE
            }
        }
    }

    private fun updateAudioProgress(currentMs: Int, totalMs: Int) {
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

    private fun syncAudioPlayerState() {
        val player = audioPlayer ?: return
        val currentActiveQuote = player.getCurrentQuote()
        val q = quote ?: return

        if (currentActiveQuote?.id == q.id) {
            when {
                player.isPlaying() -> {
                    updateAudioUiState(AtalAudioPlayer.PlayState.PLAYING, player.getCurrentMode())
                    val cur = player.getCurrentPosition()
                    val total = player.getDuration()
                    if (total > 0) {
                        updateAudioProgress(cur, total)
                    }
                }
                player.isPaused() -> {
                    updateAudioUiState(AtalAudioPlayer.PlayState.PAUSED, player.getCurrentMode())
                    val cur = player.getCurrentPosition()
                    val total = player.getDuration()
                    if (total > 0) {
                        updateAudioProgress(cur, total)
                    }
                }
                else -> {
                    updateAudioUiState(AtalAudioPlayer.PlayState.STOPPED, player.getCurrentMode())
                }
            }
        } else {
            updateAudioUiState(AtalAudioPlayer.PlayState.STOPPED, AtalAudioPlayer.AudioMode.ORATORICAL_RECITAL)
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
                binding.btnStopAudio.iconTint = ColorStateList.valueOf(accent)
                binding.btnReplay10.setTextColor(accent)
                binding.btnForward10.setTextColor(accent)
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
                binding.btnStopAudio.iconTint = ColorStateList.valueOf(accent)
                binding.btnReplay10.setTextColor(accent)
                binding.btnForward10.setTextColor(accent)
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
                binding.btnStopAudio.iconTint = ColorStateList.valueOf(accent)
                binding.btnReplay10.setTextColor(accent)
                binding.btnForward10.setTextColor(accent)
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

    override fun onResume() {
        super.onResume()
        audioPlayer?.setListener(audioPlayerListener)
        syncAudioPlayerState()
    }

    override fun onPause() {
        super.onPause()
        audioPlayer?.setListener(null)
    }

    override fun onDestroy() {
        audioPlayer?.setListener(null)
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val newQuote = intent.getSerializableExtra("extra_quote") as? Quote
        if (newQuote != null && newQuote.id != quote?.id) {
            quote = newQuote
            setupPoemContent()
            syncAudioPlayerState()
        }
    }
}
