package com.taskflow.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaskActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "COMPLETE_TASK" -> {
                val taskId = intent.getIntExtra("TASK_ID", -1)
                if (taskId != -1) {
                    completeTask(context, taskId)
                }
            }
        }
    }

    private fun completeTask(context: Context, taskId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getDatabase(context)
                val task = database.taskDao().getTaskByIdOnce(taskId)

                if (task != null) {
                    val updatedTask = task.copy(isCompleted = true)
                    database.taskDao().updateTask(updatedTask)

                    // Show toast on main thread
                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(context, "✅ Task marked as complete!", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
