package com.wisdompulse.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.wisdompulse.app.data.QuoteRepository

class QuoteNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val repo = QuoteRepository(context)
        val quote = repo.getRandomQuote() ?: return
        NotificationHelper.showQuoteNotification(context, quote)

        // Reschedule next morning reminder
        if (repo.isDailyNotificationEnabled()) {
            NotificationHelper.scheduleDailyMorningNotification(context)
        }
    }
}