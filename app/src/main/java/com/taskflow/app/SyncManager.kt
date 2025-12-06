package com.taskflow.app

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SyncManager(private val context: Context) {

    private val networkHelper = NetworkHelper(context)
    private var pendingSyncTasks = mutableListOf<Task>()
    private var isSyncing = false

    // Callback untuk update UI
    var onSyncStatusChanged: ((isSyncing: Boolean, message: String) -> Unit)? = null

    init {
        // Register network callback untuk auto-sync ketika online
        networkHelper.registerNetworkCallback {
            syncPendingTasks()
        }
    }

    // Add task ke pending sync queue
    fun addTaskToSyncQueue(task: Task) {
        if (!pendingSyncTasks.any { it.id == task.id }) {
            pendingSyncTasks.add(task)
            Log.d("SyncManager", "📝 Task added to sync queue: ${task.title}")
        }

        // Langsung sync jika ada internet
        if (networkHelper.isNetworkAvailable()) {
            syncPendingTasks()
        }
    }

    // Sync all pending tasks
    private fun syncPendingTasks() {
        if (isSyncing || pendingSyncTasks.isEmpty()) return

        isSyncing = true
        onSyncStatusChanged?.invoke(true, "Syncing ${pendingSyncTasks.size} tasks...")

        CoroutineScope(Dispatchers.IO).launch {
            val tasksToSync = pendingSyncTasks.toList()
            var successCount = 0
            var failCount = 0

            for (task in tasksToSync) {
                try {
                    FirebaseManager.syncTaskToCloud(task)
                    pendingSyncTasks.remove(task)
                    successCount++
                    Log.d("SyncManager", "✅ Synced: ${task.title}")
                } catch (e: Exception) {
                    failCount++
                    Log.e("SyncManager", "❌ Sync failed: ${task.title} - ${e.message}")
                }
            }

            isSyncing = false
            val message = if (failCount == 0) {
                "✅ All tasks synced ($successCount)"
            } else {
                "⚠️ Synced: $successCount, Failed: $failCount"
            }

            onSyncStatusChanged?.invoke(false, message)
        }
    }

    // Manual sync trigger
    fun syncNow() {
        if (!networkHelper.isNetworkAvailable()) {
            onSyncStatusChanged?.invoke(false, "❌ No internet connection")
            return
        }
        syncPendingTasks()
    }

    // Check if there are pending tasks
    fun hasPendingSyncTasks(): Boolean = pendingSyncTasks.isNotEmpty()

    // Cleanup
    fun cleanup() {
        networkHelper.unregisterNetworkCallback()
    }
}
