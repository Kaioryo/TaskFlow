package com.taskflow.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast

object ReminderHelper {

    fun setReminder(
        context: Context,
        task: Task,
        reminderTimeInMillis: Long
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // ✅ CHECK PERMISSION
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Toast.makeText(
                    context,
                    "⚠️ Please enable exact alarm permission in settings",
                    Toast.LENGTH_LONG
                ).show()
                return
            }
        }

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("TASK_ID", task.id)
            putExtra("TASK_TITLE", task.title)
            putExtra("TASK_DESC", task.description)
            putExtra("TASK_TIME", "${task.dueDate} ${task.dueTime}")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Set alarm
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminderTimeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    reminderTimeInMillis,
                    pendingIntent
                )
            }

            // ✅ LOG SUCCESS
            android.util.Log.d("ReminderHelper", "✅ Reminder set for: ${java.util.Date(reminderTimeInMillis)}")

        } catch (e: Exception) {
            Toast.makeText(context, "❌ Error setting reminder: ${e.message}", Toast.LENGTH_SHORT).show()
            android.util.Log.e("ReminderHelper", "Error: ${e.message}")
        }
    }

    // ✅ TEST REMINDER (1 MINUTE FROM NOW)
    fun setTestReminder(context: Context) {
        val testTimeMillis = System.currentTimeMillis() + (1 * 60 * 1000L) // 1 minute

        val testTask = Task(
            id = 999,
            title = "Test Reminder",
            description = "This is a test notification",
            location = "Test",
            dueDate = "25-11-2025",
            dueTime = "06:30",
            priority = "high"
        )

        setReminder(context, testTask, testTimeMillis)

        Toast.makeText(
            context,
            "🔔 Test reminder set for 1 minute from now!",
            Toast.LENGTH_LONG
        ).show()
    }

    fun cancelReminder(context: Context, taskId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        alarmManager.cancel(pendingIntent)

        android.util.Log.d("ReminderHelper", "❌ Reminder cancelled for task: $taskId")
    }
}
