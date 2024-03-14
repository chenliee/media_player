package com.heyday.media_player

import android.annotation.SuppressLint
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 *@Author：chenliee
 *@Date：2023/10/17 09:17
 *Describe:
 */
class MediaPlayerAdapter (
    private val data: List<String?>
) : RecyclerView.Adapter<MediaPlayerAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.media_player_item, parent, false)
        return ViewHolder(itemView)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]
        holder.title?.text = item
        if (position == selectedIndex) {
            holder.itemView.setBackgroundColor(Color.parseColor("#CCCCCC"))
        } else {
            holder.itemView.setBackgroundColor(Color.parseColor("#f9f9f9"))
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var title: TextView? = itemView.findViewById<View>(
            R.id.title) as TextView
    }

    private var selectedIndex: Int = 0

    fun setSelectedIndex(position: Int) {
        selectedIndex = position
        notifyDataSetChanged()
    }

}