package com.wisdompulse.app.ui

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.wisdompulse.app.R
import com.wisdompulse.app.databinding.ItemQuoteCardBinding
import com.wisdompulse.app.model.Quote

class QuoteAdapter(
    private val context: Context,
    private var quotes: List<Quote>,
    private val onFavoriteToggle: (Quote) -> Boolean,
    private val isFavorite: (Quote) -> Boolean,
    private val onSpeak: (Quote) -> Unit,
    private val onWallpaper: (Quote) -> Unit
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
            binding.tvQuoteText.text = if (quote.isPoem) quote.text else "\"" + quote.text + "\""
            binding.tvAuthor.text = quote.author
            binding.tvAuthorRole.text = quote.authorRole
            binding.tvCategory.text = quote.category

            if (quote.isPoem) {
                binding.tvPoemBadge.visibility = View.VISIBLE
            } else {
                binding.tvPoemBadge.visibility = View.GONE
            }

            val fav = isFavorite(quote)
            updateFavoriteIcon(fav)

            binding.btnFavorite.setOnClickListener {
                val newFav = onFavoriteToggle(quote)
                updateFavoriteIcon(newFav)
            }

            binding.btnSpeak.setOnClickListener {
                onSpeak(quote)
            }

            binding.btnShareText.setOnClickListener {
                val shareBody = quote.text + "\n\n— " + quote.author + " (" + quote.authorRole + ")\n\nVia WisdomPulse App"
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Quote by " + quote.author)
                    putExtra(Intent.EXTRA_TEXT, shareBody)
                }
                context.startActivity(Intent.createChooser(intent, "Share Quote via"))
            }

            binding.btnWallpaper.setOnClickListener {
                onWallpaper(quote)
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
