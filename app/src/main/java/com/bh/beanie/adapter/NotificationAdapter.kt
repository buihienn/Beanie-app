package com.bh.beanie.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bh.beanie.R
import com.bh.beanie.model.NotificationItem

class NotificationAdapter(private val notifications: List<NotificationItem>) :
    RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    // ViewHolder (chứa các view của một item)
    class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.ivNotificationIcon)
        val title: TextView = itemView.findViewById(R.id.tvNotificationTitle)
        val content: TextView = itemView.findViewById(R.id.tvNotificationContent)
        val time: TextView = itemView.findViewById(R.id.tvNotificationTime)
    }

    // Tạo ViewHolder mới (khi cần)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.notification_item, parent, false) // Inflate layout item
        return NotificationViewHolder(itemView)
    }

    // Gắn dữ liệu vào ViewHolder
    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val currentItem = notifications[position]

        holder.icon.setImageResource(currentItem.iconResId)
        holder.title.text = currentItem.title
        holder.content.text = currentItem.content
        holder.time.text = currentItem.time
    }

    // Trả về số lượng item
    override fun getItemCount() = notifications.size
}