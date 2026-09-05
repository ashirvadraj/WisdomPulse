package com.wisdompulse.app.ui

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.wisdompulse.app.R
import com.wisdompulse.app.databinding.ItemQuoteCardBinding
import com.wisdompulse.app.model.Quote

class QuoteAdapter(
    private val context: Context,
    private var quotes: List<Quote>,
    private val onFavoriteToggle: (Quote) -> Boolean,
    private val isFavorite: (Quote) -> Boolean,
    private val isRead: (Quote) -> Boolean,
    private val onPoemClick: (Quote) -> Unit
) : RecyclerView.Adapter<QuoteAdapter.QuoteViewHolder>() {

    fun updateList(newQuotes: List<Quote>) {
        quotes = newQuotes
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuoteViewHolder {
        val binding = ItemQuoteCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return QuoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: QuoteViewHolder, position: Int) {
        holder.bind(quotes[position])
    }

    override fun getItemCount(): Int = quotes.size

    inner class QuoteViewHolder(private val binding: ItemQuoteCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(quote: Quote) {
            binding.tvPoemTitle.text = quote.title ?: "कविता"
            binding.tvCategory.text = quote.category
            binding.tvQuoteText.text = quote.text

            // Bind Poem Illustration
            val imgKey = quote.illustrationKey ?: "art_diya_lamp"
            val resId = context.resources.getIdentifier(imgKey, "drawable", context.packageName)
            if (resId != 0) {
                binding.ivPoemBanner.setImageResource(resId)
            } else {
                binding.ivPoemBanner.setImageResource(R.drawable.art_diya_lamp)
            }

            // Read badge
            binding.tvReadBadge.visibility = if (isRead(quote)) View.VISIBLE else View.GONE

            // Audio badge
            if (quote.audioArtist?.contains("जगजीत") == true) {
                binding.tvAudioBadge.visibility = View.VISIBLE
                binding.tvAudioBadge.text = "🎵 जगजीत सिंह"
                binding.tvAudioBadge.setTextColor(ContextCompat.getColor(context, R.color.accent_purple))
            } else if (quote.audioArtist?.contains("अटल") == true || quote.id in listOf(1, 3, 6, 20, 49)) {
                binding.tvAudioBadge.visibility = View.VISIBLE
                binding.tvAudioBadge.text = "🎙️ मूल स्वर"
                binding.tvAudioBadge.setTextColor(ContextCompat.getColor(context, R.color.primary))
            } else {
                binding.tvAudioBadge.visibility = View.GONE
            }

            // Favorite state
            val fav = isFavorite(quote)
            updateFavoriteIcon(fav)

            binding.btnFavorite.setOnClickListener {
                val newFav = onFavoriteToggle(quote)
                updateFavoriteIcon(newFav)
            }

            // Clicking card opens dedicated reading screen
            binding.poemCardRoot.setOnClickListener {
                onPoemClick(quote)
            }
        }

        private fun updateFavoriteIcon(isFav: Boolean) {
            if (isFav) {
                binding.btnFavorite.setImageResource(R.drawable.ic_favorite)
            } else {
                binding.btnFavorite.setImageResource(R.drawable.ic_favorite_border)
            }
        }
    }
}
