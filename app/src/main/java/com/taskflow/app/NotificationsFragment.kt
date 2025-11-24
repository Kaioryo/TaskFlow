package com.taskflow.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class NotificationsFragment : Fragment() {

    private lateinit var rvNotifications: RecyclerView
    private lateinit var tvMarkRead: TextView
    private lateinit var repository: TaskRepository
    private lateinit var notificationAdapter: NotificationAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_notifications, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvNotifications = view.findViewById(R.id.rv_notifications)
        tvMarkRead = view.findViewById(R.id.tv_mark_read)

        // Initialize database
        val database = AppDatabase.getDatabase(requireContext())
        repository = TaskRepository(database.taskDao())

        setupRecyclerView()
        loadNotifications()

        tvMarkRead.setOnClickListener {
            // Mark all as read (just hide unread dots)
            notificationAdapter.markAllAsRead()
        }
    }

    private fun setupRecyclerView() {
        notificationAdapter = NotificationAdapter(emptyList())
        rvNotifications.layoutManager = LinearLayoutManager(requireContext())
        rvNotifications.adapter = notificationAdapter
    }

    private fun loadNotifications() {
        viewLifecycleOwner.lifecycleScope.launch {
            repository.incompleteTasks.collect { tasks ->
                // Generate notifications from tasks
                val notifications = tasks.map { task ->
                    NotificationItem(
                        title = "Reminder: ${task.title}",
                        subtitle = "${task.description} · Due: ${task.dueDate}",
                        isUnread = true
                    )
                }
                notificationAdapter.updateNotifications(notifications)
            }
        }
    }
}

data class NotificationItem(
    val title: String,
    val subtitle: String,
    var isUnread: Boolean
)

class NotificationAdapter(
    private var notifications: List<NotificationItem>
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    fun updateNotifications(newNotifications: List<NotificationItem>) {
        notifications = newNotifications
        notifyDataSetChanged()
    }

    fun markAllAsRead() {
        notifications.forEach { it.isUnread = false }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(notifications[position])
    }

    override fun getItemCount() = notifications.size

    inner class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_notif_title)
        private val tvSubtitle: TextView = itemView.findViewById(R.id.tv_notif_subtitle)
        private val viewDot: View = itemView.findViewById(R.id.view_unread_dot)

        fun bind(notification: NotificationItem) {
            tvTitle.text = notification.title
            tvSubtitle.text = notification.subtitle
            viewDot.visibility = if (notification.isUnread) View.VISIBLE else View.INVISIBLE
        }
    }
}
