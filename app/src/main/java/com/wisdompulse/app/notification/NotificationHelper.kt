package com.wisdompulse.app.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.wisdompulse.app.model.Quote
import com.wisdompulse.app.ui.MainActivity
import com.wisdompulse.app.ui.WallpaperActivity
import java.util.Calendar

object NotificationHelper {
    const val CHANNEL_ID = "daily_wisdom_channel"
    const val ACTION_DAILY_QUOTE = "com.wisdompulse.app.ACTION_DAILY_QUOTE"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Daily Wisdom Quotes"
            val descriptionText = "Daily morning quotes from world legends"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showQuoteNotification(context: Context, quote: Quote) {
        createNotificationChannel(context)

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val mainPendingIntent = PendingIntent.getActivity(
            context,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val wpIntent = Intent(context, WallpaperActivity::class.java).apply {
            putExtra("extra_quote", quote)
        }
        val wpPendingIntent = PendingIntent.getActivity(
            context,
            quote.id,
            wpIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val bigTextContent = quote.text + "\n\n— " + quote.author + " (" + quote.authorRole + ")"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("✨ " + quote.author)
            .setContentText(quote.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigTextContent))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(mainPendingIntent)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_menu_gallery, "Set Wallpaper", wpPendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(quote.id, notification)
    }

    fun scheduleDailyMorningNotification(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, QuoteNotificationReceiver::class.java).apply {
            action = ACTION_DAILY_QUOTE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 7)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}
