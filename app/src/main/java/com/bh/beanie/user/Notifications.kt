package com.bh.beanie.user

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import com.bh.beanie.BeanieApplication
import com.bh.beanie.R
import com.bh.beanie.user.UserMainActivity
import com.bh.beanie.databinding.ActivityNotificationsBinding
import com.bh.beanie.model.Notification
import com.bh.beanie.repository.NotificationRepository
import com.bh.beanie.user.adapter.NotificationsAdapter
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class NotificationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationsBinding
    private lateinit var adapter: NotificationsAdapter
    private val notificationRepository = NotificationRepository()
    private var userId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get userId from application
        userId = (application as BeanieApplication).getUserId() ?: ""
        if (userId.isEmpty()) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        setupRecyclerView()
        loadNotifications()

        binding.markAllAsReadButton.setOnClickListener {
            markAllAsRead()
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = NotificationsAdapter()
        binding.notificationsRecyclerView.adapter = adapter
        binding.notificationsRecyclerView.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        )

        adapter.onNotificationClickListener = { notification ->
            if (!notification.read) {
                lifecycleScope.launch {
                    try {
                        notificationRepository.markAsRead(notification.id)
                        // Update the notification in the adapter to show it as read
                        val updatedNotification = notification.copy(read = true)
                        val currentList = adapter.currentList.toMutableList()
                        val index = currentList.indexOfFirst { it.id == notification.id }
                        if (index != -1) {
                            currentList[index] = updatedNotification
                            adapter.updateNotifications(currentList)
                        }

                        Toast.makeText(this@NotificationsActivity,
                            "Notification marked as read",
                            Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Log.e("NotificationsActivity", "Error marking notification as read", e)
                    }
                }
            }

            // No navigation code here, just mark as read
        }
    }

    private fun loadNotifications() {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            notificationRepository.getUserNotifications(userId)
                .catch { e ->
                    binding.progressBar.visibility = View.GONE
                    showEmptyState(true)
                    Log.e("NotificationsActivity", "Error loading notifications", e)
                    Toast.makeText(this@NotificationsActivity,
                        "Error loading notifications: ${e.message}",
                        Toast.LENGTH_SHORT).show()
                }
                .collect { notifications ->
                    binding.progressBar.visibility = View.GONE
                    if (notifications.isEmpty()) {
                        showEmptyState(true)
                    } else {
                        showEmptyState(false)
                        adapter.updateNotifications(notifications)
                    }
                }
        }
    }

    private fun markAllAsRead() {
        lifecycleScope.launch {
            val success = notificationRepository.markAllAsRead(userId)
            if (success) {
                Toast.makeText(this@NotificationsActivity,
                    "All notifications marked as read",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showEmptyState(show: Boolean) {
        binding.emptyStateLayout.visibility = if (show) View.VISIBLE else View.GONE
        binding.notificationsRecyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }
}