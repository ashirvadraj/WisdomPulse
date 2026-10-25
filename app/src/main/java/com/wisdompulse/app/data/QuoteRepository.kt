package com.wisdompulse.app.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.wisdompulse.app.model.Quote

class QuoteRepository(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("wisdom_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private var allQuotes: List<Quote> = emptyList()

    init {
        loadQuotesFromAssets()
    }

    private fun loadQuotesFromAssets() {
        try {
            val jsonString = context.assets.open("quotes.json").bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<Quote>>() {}.type
            allQuotes = gson.fromJson(jsonString, type)
        } catch (e: Exception) {
            e.printStackTrace()
            allQuotes = emptyList()
        }
    }

    fun getAllQuotes(): List<Quote> = allQuotes

    fun getRandomQuote(): Quote? {
        if (allQuotes.isEmpty()) return null
        return allQuotes.random()
    }

    fun getFavorites(): Set<Int> {
        val favs = prefs.getStringSet("favorite_ids", emptySet()) ?: emptySet()
        return favs.mapNotNull { it.toIntOrNull() }.toSet()
    }

    fun toggleFavorite(quoteId: Int): Boolean {
        val currentFavs = getFavorites().toMutableSet()
        val isNowFavorite = if (currentFavs.contains(quoteId)) {
            currentFavs.remove(quoteId)
            false
        } else {
            currentFavs.add(quoteId)
            true
        }
        prefs.edit().putStringSet("favorite_ids", currentFavs.map { it.toString() }.toSet()).apply()
        return isNowFavorite
    }

    fun isFavorite(quoteId: Int): Boolean {
        return getFavorites().contains(quoteId)
    }

    fun isDailyNotificationEnabled(): Boolean {
        return prefs.getBoolean("daily_notifications_enabled", true)
    }

    fun setDailyNotificationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("daily_notifications_enabled", enabled).apply()
    }

    // Reading Progress & Tracking
    fun getReadPoemIds(): Set<Int> {
        val readSet = prefs.getStringSet("read_poem_ids", emptySet()) ?: emptySet()
        return readSet.mapNotNull { it.toIntOrNull() }.toSet()
    }

    fun markPoemAsRead(quoteId: Int) {
        val currentRead = getReadPoemIds().toMutableSet()
        if (!currentRead.contains(quoteId)) {
            currentRead.add(quoteId)
            prefs.edit().putStringSet("read_poem_ids", currentRead.map { it.toString() }.toSet()).apply()
        }
    }

    fun isPoemRead(quoteId: Int): Boolean {
        return getReadPoemIds().contains(quoteId)
    }

    // Reader Preferences
    fun getReadingTheme(): String {
        return prefs.getString("reader_theme", "parchment") ?: "parchment"
    }

    fun setReadingTheme(theme: String) {
        prefs.edit().putString("reader_theme", theme).apply()
    }

    fun getFontSizeSp(): Float {
        return prefs.getFloat("reader_font_size", 18f)
    }

    fun setFontSizeSp(sizeSp: Float) {
        prefs.edit().putFloat("reader_font_size", sizeSp).apply()
    }
}