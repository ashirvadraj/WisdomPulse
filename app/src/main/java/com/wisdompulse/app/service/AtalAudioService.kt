package com.wisdompulse.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.wisdompulse.app.R
import com.wisdompulse.app.model.Quote
import com.wisdompulse.app.ui.PoemDetailActivity
import com.wisdompulse.app.util.AtalAudioPlayer

class AtalAudioService : Service() {

    companion object {
        const val CHANNEL_ID = "wisdom_pulse_playback_channel"
        const val NOTIFICATION_ID = 2001

        const val ACTION_START = "com.wisdompulse.app.action.AUDIO_START"
        const val ACTION_PAUSE = "com.wisdompulse.app.action.AUDIO_PAUSE"
        const val ACTION_RESUME = "com.wisdompulse.app.action.AUDIO_RESUME"
        const val ACTION_TOGGLE = "com.wisdompulse.app.action.AUDIO_TOGGLE"
        const val ACTION_STOP = "com.wisdompulse.app.action.AUDIO_STOP"

        const val EXTRA_QUOTE = "extra_quote"

        fun startPlayback(context: Context, quote: Quote) {
            val intent = Intent(context, AtalAudioService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_QUOTE, quote)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun updatePlaybackState(context: Context, quote: Quote?, isPlaying: Boolean) {
            val intent = Intent(context, AtalAudioService::class.java).apply {
                action = if (isPlaying) ACTION_RESUME else ACTION_PAUSE
                quote?.let { putExtra(EXTRA_QUOTE, it) }
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isPlaying) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun stopPlayback(context: Context) {
            val intent = Intent(context, AtalAudioService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private var currentQuote: Quote? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "WisdomPulse::AudioPlaybackWakeLock"
            ).apply {
                setReferenceCounted(false)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val quote = intent?.getSerializableExtra(EXTRA_QUOTE) as? Quote
        if (quote != null) {
            currentQuote = quote
        }

        when (action) {
            ACTION_START, ACTION_RESUME -> {
                acquireWakeLock()
                val notif = buildNotification(currentQuote, isPlaying = true)
                startInForeground(notif)
            }
            ACTION_PAUSE -> {
                releaseWakeLock()
                val notif = buildNotification(currentQuote, isPlaying = false)
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(NOTIFICATION_ID, notif)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_DETACH)
                }
            }
            ACTION_TOGGLE -> {
                val player = AtalAudioPlayer.getInstance(applicationContext)
                currentQuote?.let { player.togglePlayPause(it) }
            }
            ACTION_STOP -> {
                releaseWakeLock()
                val player = AtalAudioPlayer.getInstance(applicationContext)
                player.stop()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    stopForeground(true)
                }
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun startInForeground(notification: Notification) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback for edge cases
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun acquireWakeLock() {
        try {
            if (wakeLock?.isHeld != true) {
                wakeLock?.acquire(120 * 60 * 1000L) // 2 hour max timeout
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "काव्य व संगीत प्लेबैक"
            val descriptionText = "पृष्ठभूमि ऑडियो एवं संगीत नियंत्रण"
            val channel = NotificationChannel(
                CHANNEL_ID,
                name,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = descriptionText
                setShowBadge(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(quote: Quote?, isPlaying: Boolean): Notification {
        val title = quote?.title ?: "काव्य पाठ"
        val artist = quote?.audioArtist ?: "अटल बिहारी वाजपेयी"

        val contentIntent = Intent(this, PoemDetailActivity::class.java).apply {
            quote?.let { putExtra("extra_quote", it) }
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            quote?.id ?: 0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleIntent = Intent(this, AtalAudioService::class.java).apply {
            action = ACTION_TOGGLE
            quote?.let { putExtra(EXTRA_QUOTE, it) }
        }
        val togglePendingIntent = PendingIntent.getService(
            this,
            1,
            toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, AtalAudioService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            2,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIcon = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow
        val playPauseText = if (isPlaying) "विराम" else "चलाएँ"

        val largeIcon = try {
            BitmapFactory.decodeResource(resources, R.drawable.img_atal_vajpayee)
        } catch (e: Exception) {
            null
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_volume_up)
            .setContentTitle(title)
            .setContentText(artist)
            .setSubText("मेरी इक्यावन कविताएँ")
            .setContentIntent(contentPendingIntent)
            .setOngoing(isPlaying)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(playPauseIcon, playPauseText, togglePendingIntent)
            .addAction(R.drawable.ic_stop, "रोकें", stopPendingIntent)

        if (largeIcon != null) {
            builder.setLargeIcon(largeIcon)
        }

        return builder.build()
    }

    override fun onDestroy() {
        releaseWakeLock()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
