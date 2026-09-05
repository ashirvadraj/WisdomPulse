package com.wisdompulse.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.wisdompulse.app.R
import com.wisdompulse.app.data.QuoteRepository
import com.wisdompulse.app.databinding.ActivityMainBinding
import com.wisdompulse.app.model.Quote
import com.wisdompulse.app.notification.NotificationHelper
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: QuoteRepository
    private lateinit var adapter: QuoteAdapter

    private var currentCategory = "All"
    private var currentSearchQuery = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = QuoteRepository(this)

        NotificationHelper.createNotificationChannel(this)
        requestNotificationPermission()
        NotificationHelper.scheduleDailyMorningNotification(this)

        setupRecyclerView()
        setupSearch()
        setupCategoryChips()
        setupNotificationButton()
    }

    override fun onResume() {
        super.onResume()
        updateReadingProgress()
        filterQuotes()
    }

    private fun updateReadingProgress() {
        val total = repository.getAllQuotes().size
        val readCount = repository.getReadPoemIds().size
        binding.tvReadingProgress.text = "प्रगति: $total में से $readCount कविताएँ पढ़ी गईं"
        binding.progressReading.max = if (total > 0) total else 51
        binding.progressReading.progress = readCount
    }

    private fun setupRecyclerView() {
        adapter = QuoteAdapter(
            context = this,
            quotes = repository.getAllQuotes(),
            onFavoriteToggle = { quote -> repository.toggleFavorite(quote.id) },
            isFavorite = { quote -> repository.isFavorite(quote.id) },
            isRead = { quote -> repository.isPoemRead(quote.id) },
            onPoemClick = { quote ->
                val intent = Intent(this, PoemDetailActivity::class.java).apply {
                    putExtra("extra_quote", quote)
                }
                startActivity(intent)
            }
        )

        binding.rvQuotes.layoutManager = LinearLayoutManager(this)
        binding.rvQuotes.adapter = adapter
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentSearchQuery = s?.toString()?.trim() ?: ""
                binding.btnClearSearch.visibility = if (currentSearchQuery.isNotEmpty()) View.VISIBLE else View.GONE
                filterQuotes()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnClearSearch.setOnClickListener {
            binding.etSearch.text?.clear()
        }
    }

    private fun setupCategoryChips() {
        val chips = listOf(
            Pair(binding.chipAll, "All"),
            Pair(binding.chipSec1, "अनुभूति के स्वर"),
            Pair(binding.chipSec2, "राष्ट्रीयता के स्वर"),
            Pair(binding.chipSec3, "चुनौती के स्वर"),
            Pair(binding.chipSec4, "विविध के स्वर"),
            Pair(binding.chipFavorites, "Favorites")
        )

        for ((chipView, categoryKey) in chips) {
            chipView.setOnClickListener {
                currentCategory = categoryKey
                updateChipSelectionUI(chips, chipView)
                filterQuotes()
            }
        }
    }

    private fun updateChipSelectionUI(chips: List<Pair<TextView, String>>, selected: TextView) {
        for ((chip, _) in chips) {
            if (chip == selected) {
                chip.setBackgroundResource(R.drawable.bg_chip_selected)
                chip.setTextColor(ContextCompat.getColor(this, R.color.black))
            } else {
                chip.setBackgroundResource(R.drawable.bg_chip_unselected)
                chip.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            }
        }
    }

    private fun filterQuotes() {
        val all = repository.getAllQuotes()
        val favIds = repository.getFavorites()

        val filtered = all.filter { quote ->
            val matchesCategory = when (currentCategory) {
                "All" -> true
                "Favorites" -> favIds.contains(quote.id)
                else -> quote.category.equals(currentCategory, ignoreCase = true)
            }

            val matchesSearch = if (currentSearchQuery.isEmpty()) {
                true
            } else {
                quote.text.contains(currentSearchQuery, ignoreCase = true) ||
                (quote.title?.contains(currentSearchQuery, ignoreCase = true) == true) ||
                quote.category.contains(currentSearchQuery, ignoreCase = true)
            }

            matchesCategory && matchesSearch
        }

        adapter.updateList(filtered)
        binding.tvEmptyState.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun setupNotificationButton() {
        binding.btnTestNotification.setOnClickListener {
            val random = repository.getRandomQuote()
            if (random != null) {
                NotificationHelper.showQuoteNotification(this, random)
                Toast.makeText(this, "आज की कविता भेजी गई! ✨", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }
}
