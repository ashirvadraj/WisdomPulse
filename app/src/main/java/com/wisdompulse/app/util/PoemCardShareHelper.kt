package com.wisdompulse.app.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.FileProvider
import com.wisdompulse.app.databinding.ViewPoemShareCardBinding
import com.wisdompulse.app.model.Quote
import java.io.File
import java.io.FileOutputStream

object PoemCardShareHelper {

    fun renderAndSharePoemCard(context: Context, quote: Quote) {
        try {
            val inflater = LayoutInflater.from(context)
            val binding = ViewPoemShareCardBinding.inflate(inflater)

            binding.tvCardSection.text = quote.category
            binding.tvCardTitle.text = quote.title ?: "कविता"
            binding.tvCardPoemText.text = quote.text

            val width = 1080
            val widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
            val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            binding.cardContainer.measure(widthSpec, heightSpec)
            val height = binding.cardContainer.measuredHeight.coerceAtLeast(600)
            binding.cardContainer.layout(0, 0, width, height)

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            binding.cardContainer.draw(canvas)

            val cacheDir = File(context.cacheDir, "cards").apply { mkdirs() }
            val file = File(cacheDir, "poem_${quote.id}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareText = "${quote.title ?: "कविता"}\n\n${quote.text}\n\n— अटल बिहारी वाजपेयी (मेरी इक्यावन कविताएँ)"

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, shareText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "काव्य कार्ड साझा करें"))

        } catch (e: Exception) {
            e.printStackTrace()
            val shareText = "${quote.title ?: "कविता"}\n\n${quote.text}\n\n— अटल बिहारी वाजपेयी (मेरी इक्यावन कविताएँ)"
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            context.startActivity(Intent.createChooser(intent, "कविता साझा करें"))
        }
    }
}