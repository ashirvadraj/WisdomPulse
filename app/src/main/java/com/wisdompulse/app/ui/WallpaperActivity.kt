package com.wisdompulse.app.ui

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.wisdompulse.app.R
import com.wisdompulse.app.databinding.ActivityWallpaperBinding
import com.wisdompulse.app.model.Quote
import com.wisdompulse.app.util.WallpaperHelper

class WallpaperActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWallpaperBinding
    private var quote: Quote? = null
    private var isPortraitVisible = true

    private val themeOled = ThemePalette(
        bgColor = Color.parseColor("#0B0C10"),
        quoteColor = Color.parseColor("#FFFFFF"),
        authorColor = Color.parseColor("#E0A96D"),
        dividerColor = Color.parseColor("#E0A96D"),
        watermarkColor = Color.parseColor("#66FFFFFF")
    )
    private val themeGold = ThemePalette(
        bgColor = Color.parseColor("#221A0F"),
        quoteColor = Color.parseColor("#FFF4E0"),
        authorColor = Color.parseColor("#F6C90E"),
        dividerColor = Color.parseColor("#F6C90E"),
        watermarkColor = Color.parseColor("#88F6C90E")
    )
    private val themeCrimson = ThemePalette(
        bgColor = Color.parseColor("#2B0C1D"),
        quoteColor = Color.parseColor("#FFFFFF"),
        authorColor = Color.parseColor("#FF7675"),
        dividerColor = Color.parseColor("#FF7675"),
        watermarkColor = Color.parseColor("#66FFFFFF")
    )
    private val themeForest = ThemePalette(
        bgColor = Color.parseColor("#0C2314"),
        quoteColor = Color.parseColor("#E8F8F0"),
        authorColor = Color.parseColor("#2ECC71"),
        dividerColor = Color.parseColor("#2ECC71"),
        watermarkColor = Color.parseColor("#662ECC71")
    )
    private val themeIndigo = ThemePalette(
        bgColor = Color.parseColor("#11172E"),
        quoteColor = Color.parseColor("#F0F3FF"),
        authorColor = Color.parseColor("#74B9FF"),
        dividerColor = Color.parseColor("#74B9FF"),
        watermarkColor = Color.parseColor("#6674B9FF")
    )
    private val themeSunset = ThemePalette(
        bgColor = Color.parseColor("#3B1218"),
        quoteColor = Color.parseColor("#FFF0ED"),
        authorColor = Color.parseColor("#FF9F43"),
        dividerColor = Color.parseColor("#FF9F43"),
        watermarkColor = Color.parseColor("#66FF9F43")
    )
    private val themeParchment = ThemePalette(
        bgColor = Color.parseColor("#F5EFE6"),
        quoteColor = Color.parseColor("#1E1E24"),
        authorColor = Color.parseColor("#784212"),
        dividerColor = Color.parseColor("#784212"),
        watermarkColor = Color.parseColor("#88784212")
    )

    data class ThemePalette(
        val bgColor: Int,
        val quoteColor: Int,
        val authorColor: Int,
        val dividerColor: Int,
        val watermarkColor: Int
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWallpaperBinding.inflate(layoutInflater)
        setContentView(binding.root)

        quote = intent.getSerializableExtra("extra_quote") as? Quote

        setupUI()
        setupThemeSwatches()
        setupActions()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { finish() }

        quote?.let { q ->
            binding.tvWpQuote.text = if (q.isPoem) q.text else "\"" + q.text + "\""
            binding.tvWpAuthor.text = "— " + q.author
            binding.tvWpRole.text = q.authorRole

            // Load Author Avatar into Wallpaper Badge
            val resId = resources.getIdentifier(q.avatarKey, "drawable", packageName)
            if (resId != 0) {
                binding.ivWpAvatar.setImageResource(resId)
            } else {
                binding.ivWpAvatar.setImageResource(R.drawable.avatar_generic)
            }
        }

        // Portrait Toggle
        binding.btnTogglePortrait.setOnClickListener {
            isPortraitVisible = !isPortraitVisible
            binding.ivWpAvatar.visibility = if (isPortraitVisible) View.VISIBLE else View.GONE
            binding.btnTogglePortrait.text = if (isPortraitVisible) "Portrait Badge: ON" else "Portrait Badge: OFF"
        }

        applyTheme(themeOled)
    }

    private fun setupThemeSwatches() {
        binding.themeOled.setOnClickListener { applyTheme(themeOled) }
        binding.themeRoyalGold.setOnClickListener { applyTheme(themeGold) }
        binding.themeCrimson.setOnClickListener { applyTheme(themeCrimson) }
        binding.themeForest.setOnClickListener { applyTheme(themeForest) }
        binding.themeIndigo.setOnClickListener { applyTheme(themeIndigo) }
        binding.themeSunset.setOnClickListener { applyTheme(themeSunset) }
        binding.themeParchment.setOnClickListener { applyTheme(themeParchment) }
    }

    private fun applyTheme(theme: ThemePalette) {
        binding.wallpaperContainer.setBackgroundColor(theme.bgColor)
        binding.tvWpQuote.setTextColor(theme.quoteColor)
        binding.tvWpAuthor.setTextColor(theme.authorColor)
        binding.wpDivider.setBackgroundColor(theme.dividerColor)
        binding.tvWpRole.setTextColor(theme.authorColor)
        binding.tvWpWatermark.setTextColor(theme.watermarkColor)
        binding.ivQuoteDeco.setColorFilter(theme.authorColor)
    }

    private fun setupActions() {
        binding.btnSetPhoneWallpaper.setOnClickListener {
            val highResBitmap = WallpaperHelper.renderHighResWallpaper(binding.wallpaperContainer)
            val success = WallpaperHelper.setAsDeviceWallpaper(this, highResBitmap)
            if (success) {
                Toast.makeText(this, "✨ Wallpaper set successfully on your device!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Could not set wallpaper automatically. Try saving to gallery.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnSaveWhatsAppWallpaper.setOnClickListener {
            val highResBitmap = WallpaperHelper.renderHighResWallpaper(binding.wallpaperContainer)
            WallpaperHelper.saveAndShareForWhatsApp(this, highResBitmap)
        }
    }
}
