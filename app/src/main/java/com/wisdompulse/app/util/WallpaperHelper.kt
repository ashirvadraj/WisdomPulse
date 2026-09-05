package com.wisdompulse.app.util

import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object WallpaperHelper {

    fun renderHighResWallpaper(view: View, targetWidth: Int = 1080, targetHeight: Int = 1920): Bitmap {
        val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val widthSpec = View.MeasureSpec.makeMeasureSpec(targetWidth, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(targetHeight, View.MeasureSpec.EXACTLY)
        view.measure(widthSpec, heightSpec)
        view.layout(0, 0, targetWidth, targetHeight)
        view.draw(canvas)
        return bitmap
    }

    fun setAsDeviceWallpaper(context: Context, bitmap: Bitmap): Boolean {
        return try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
            } else {
                wallpaperManager.setBitmap(bitmap)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun saveAndShareForWhatsApp(context: Context, bitmap: Bitmap) {
        try {
            val filename = "WisdomPulse_Wallpaper_" + System.currentTimeMillis() + ".png"
            var imageUri: Uri? = null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/WisdomPulse")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    val stream: OutputStream? = resolver.openOutputStream(uri)
                    if (stream != null) {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                        stream.close()
                    }
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                    imageUri = uri
                }
            } else {
                val imagesDir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "WisdomPulse")
                imagesDir.mkdirs()
                val imageFile = File(imagesDir, filename)
                FileOutputStream(imageFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                imageUri = FileProvider.getUriForFile(context, "com.wisdompulse.app.fileprovider", imageFile)
            }

            Toast.makeText(
                context,
                "Saved to Gallery! In WhatsApp: Settings > Chats > Wallpaper > My Photos",
                Toast.LENGTH_LONG
            ).show()

            if (imageUri != null) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, imageUri)
                    putExtra(Intent.EXTRA_TEXT, "Wallpaper created with WisdomPulse ✨")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    setPackage("com.whatsapp")
                }

                try {
                    context.startActivity(shareIntent)
                } catch (e: Exception) {
                    val chooser = Intent.createChooser(
                        Intent(Intent.ACTION_SEND).apply {
                            type = "image/png"
                            putExtra(Intent.EXTRA_STREAM, imageUri)
                            putExtra(Intent.EXTRA_TEXT, "Wallpaper created with WisdomPulse ✨")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        },
                        "Set as WhatsApp Wallpaper / Share"
                    )
                    context.startActivity(chooser)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error saving wallpaper: " + e.message, Toast.LENGTH_SHORT).show()
        }
    }
}