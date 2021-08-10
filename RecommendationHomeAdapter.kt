package com.devforfun.example.ui.home.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.devforfun.example.R
import com.devforfun.example.model.search.Data
import kotlinx.android.synthetic.main.item_free_recommendation_book.view.*

class RecommendationHomeAdapter(
    var list: List<Data>, val context: Context,
    val callback: MyCallback
) :
    RecyclerView.Adapter<RecommendationHomeAdapter.RecommendationHomeHolder>() {

    interface MyCallback {
        fun onItemClickedRecommended(data: Data)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecommendationHomeHolder {
        return RecommendationHomeHolder(
            LayoutInflater.from(context)
                .inflate(R.layout.item_free_recommendation_book, parent, false)
        )
    }

    fun setMovieListItems(movieList: List<Data>) {
        this.list = movieList;
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: RecommendationHomeHolder, position: Int) {

        if (list[position].image_small.isEmpty()) {
            holder.imageItemBookPlaceHolder.visibility = View.VISIBLE
            holder.cardViewItemMenu.visibility = View.GONE
            Glide.with(context)
                .load(R.drawable.ic_placeholder_book_small)
                .into(holder.imageItemBook)
        } else {
            holder.imageItemBookPlaceHolder.visibility = View.GONE
            holder.cardViewItemMenu.visibility = View.VISIBLE
            Glide.with(context)
                .load(list[position].image_small)
                .into(holder.imageItemBook)
        }

        holder.tvItemNameBook.text = list[position].title

        holder.tvItemAuthorBook.text = ""
        for (item in list[position].author) {
            holder.tvItemAuthorBook.text = "${holder.tvItemAuthorBook.text} ${item.name}"
        }
        holder.tvItemAuthorBook.text = holder.tvItemAuthorBook.text.trim()

        holder.tvFreeAndRecomendedRating.text = list[position].likes.toString()

        if (list[position].status == "free") {
            holder.ivItemIsFree.visibility = View.VISIBLE
        } else {
            holder.ivItemIsFree.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            callback.onItemClickedRecommended(list[position])
        }


    }

    class RecommendationHomeHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageItemBook = view.imageItemBook
        val tvItemNameBook = view.tvItemNameBook
        val tvItemAuthorBook = view.tvItemAuthorBook
        val ivItemIsFree = view.ivItemIsFree
        val tvFreeAndRecomendedRating = view.tvFreeAndRecomendedRating
        val imageItemBookPlaceHolder = view.imageItemBookPlaceHolder
        val cardViewItemMenu = view.cardViewItemMenu
    }
}