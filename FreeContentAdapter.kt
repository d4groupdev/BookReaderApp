package com.devforfun.example.ui.home.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.devforfun.example.R
import com.devforfun.example.model.search.Data
import kotlinx.android.synthetic.main.item_free_content.view.*

class FreeContentAdapter(var list: ArrayList<Data>, val context: Context, val callback: Callback) :
    RecyclerView.Adapter<FreeContentAdapter.FreeContentHolder>() {

    interface Callback {
        fun onItemClicked(data: Data)
        fun onFavorites(data: Data)
        fun onMaxPosition()
    }

    fun setMovieListItems(movieList: List<Data>) {
        this.list.clear()
        this.list.addAll(movieList)
        notifyDataSetChanged()
    }

    fun addToList(itemList: List<Data>) {
        this.list.addAll(itemList)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FreeContentHolder {
        return FreeContentHolder(
            LayoutInflater.from(context)
                .inflate(R.layout.item_free_content, parent, false)
        )
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: FreeContentHolder, position: Int) {
        if (list[position].image_small.isEmpty()) {
            holder.imageFreeContentBookPlaceHolder.visibility = View.VISIBLE
            holder.cardViewItemMenu.visibility = View.GONE
           Glide.with(context)
               .load(R.drawable.ic_placeholder_book_small)
               .into(holder.imageFreeContentBook)
        } else {
            holder.imageFreeContentBookPlaceHolder.visibility = View.GONE
            holder.cardViewItemMenu.visibility = View.VISIBLE
            Glide.with(context)
                .load(list[position].image_small)
                .into(holder.imageFreeContentBook)
        }

        if (list[position].image_small.isEmpty() && list[position].is_new) {
            holder.ivFreeNewPla.visibility = View.VISIBLE
        } else {
            holder.ivFreeNewPla.visibility = View.GONE
        }

        holder.tvItemNameBookFreeContent.text = list[position].title

        holder.tvItemAuthorBookFreeContent.text = ""
        for (item in list[position].author) {
            holder.tvItemAuthorBookFreeContent.text =
                "${holder.tvItemAuthorBookFreeContent.text} ${item.name}"
        }
        holder.tvItemAuthorBookFreeContent.text = holder.tvItemAuthorBookFreeContent.text.trim()

        holder.tvFreeRecomendedRat.text = list[position].likes.toString()

        holder.itemView.setOnClickListener { callback.onItemClicked(list[position]) }

        if (list[position].is_favorites) {
            Glide.with(context)
                .load(R.drawable.ic_favorites_search_is_favorites)
                .into(holder.ivFavoritesFreeContent)
        } else {
            Glide.with(context)
                .load(R.drawable.ic_add_to_favorites_black)
                .into(holder.ivFavoritesFreeContent)
        }

        holder.llFavoritesFreeContent.setOnClickListener {
            if (list[position].is_favorites) {
                Glide.with(context)
                    .load(R.drawable.ic_add_to_favorites_black)
                    .into(holder.ivFavoritesFreeContent)
            } else {
                Glide.with(context)
                    .load(R.drawable.ic_favorites_search_is_favorites)
                    .into(holder.ivFavoritesFreeContent)
            }

            callback.onFavorites(list[position])
        }
        if (position + 1 >= list.size) {
            callback.onMaxPosition()
        }

        if (list[position].is_new) {
            holder.ivFreeNew.visibility = View.VISIBLE
        } else {
            holder.ivFreeNew.visibility = View.GONE
        }
    }

    class FreeContentHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageFreeContentBook = view.imageFreeContentBook
        val tvItemNameBookFreeContent = view.tvItemNameBookFreeContent
        val tvItemAuthorBookFreeContent = view.tvItemAuthorBookFreeContent
        val ivFavoritesFreeContent = view.ivFavoritesFreeContent
        val tvFreeRecomendedRat = view.tvFreeRecommendedRat
        val llFavoritesFreeContent = view.llFavoritesFreeContent
        val ivFreeNew = view.ivFreeNew
        val imageFreeContentBookPlaceHolder = view.imageFreeContentBookPlaceHolder
        val cardViewItemMenu = view.cardViewItemMenu
        val ivFreeNewPla = view.ivFreeNewPla
    }
}