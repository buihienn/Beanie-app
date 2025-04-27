package com.bh.beanie.user.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bh.beanie.R
import com.bh.beanie.model.Notification
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationsAdapter : RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder>() {

    private val notifications = mutableListOf<Notification>()
    var onNotificationClickListener: ((Notification) -> Unit)? = null

    val currentList: List<Notification>
        get() = notifications.toList()

    fun updateNotifications(newNotifications: List<Notification>) {
        notifications.clear()
        notifications.addAll(newNotifications)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false) // Changed from notification_item to item_notification
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(notifications[position])
    }

    override fun getItemCount() = notifications.size

    inner class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val unreadIndicator: View = itemView.findViewById(R.id.unreadIndicator)
        private val titleTextView: TextView = itemView.findViewById(R.id.notificationTitle)
        private val messageTextView: TextView = itemView.findViewById(R.id.notificationMessage)
        private val timeTextView: TextView = itemView.findViewById(R.id.notificationTime)

        init {
            itemView.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onNotificationClickListener?.invoke(notifications[adapterPosition])
                }
            }
        }

        fun bind(notification: Notification) {
            titleTextView.text = notification.title
            messageTextView.text = notification.message
            timeTextView.text = getTimeAgo(notification.timestamp)

            // Show/hide unread indicator
            unreadIndicator.visibility = if (notification.read) View.INVISIBLE else View.VISIBLE
        }

        private fun getTimeAgo(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            return when {
                diff < 60 * 1000 -> "Just now"
                diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)} minutes ago"
                diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)} hours ago"
                diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)} days ago"
                else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
            }
        }
    }

    private class NotificationDiffCallback(
        private val oldList: List<Notification>,
        private val newList: List<Notification>
    ) : DiffUtil.Callback() {

        override fun getOldListSize() = oldList.size

        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val old = oldList[oldItemPosition]
            val new = newList[newItemPosition]
            return old.title == new.title &&
                    old.message == new.message &&
                    old.read == new.read &&
                    old.timestamp == new.timestamp
        }
    }
}