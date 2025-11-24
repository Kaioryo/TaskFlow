package com.taskflow.app

import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object DateTimeHelper {

    // Get current date in DD-MM-YYYY format
    fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        return sdf.format(Date())
    }

    // Get current time in HH:mm format
    fun getCurrentTime(): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date())
    }

    // Get current date & time as readable string
    fun getCurrentDateTime(): String {
        val sdf = SimpleDateFormat("EEEE, dd MMM yyyy - HH:mm", Locale.getDefault())
        return sdf.format(Date())
    }

    // Get day of week
    fun getCurrentDayOfWeek(): String {
        val sdf = SimpleDateFormat("EEEE", Locale.getDefault())
        return sdf.format(Date())
    }

    // Calculate time remaining until deadline
    fun getTimeRemaining(dueDate: String, dueTime: String): String {
        try {
            // Parse due date & time
            val sdf = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
            val deadline = sdf.parse("$dueDate $dueTime") ?: return "Invalid date"

            // Calculate difference
            val currentTime = Date()
            val diffInMillis = deadline.time - currentTime.time

            if (diffInMillis < 0) {
                return "⚠️ Overdue"
            }

            val days = TimeUnit.MILLISECONDS.toDays(diffInMillis)
            val hours = TimeUnit.MILLISECONDS.toHours(diffInMillis) % 24
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis) % 60

            return when {
                days > 0 -> "$days days left"
                hours > 0 -> "$hours hours left"
                else -> "$minutes minutes left"
            }

        } catch (e: Exception) {
            return "Invalid date"
        }
    }

    // Check if task is due soon (less than 24 hours)
    fun isDueSoon(dueDate: String, dueTime: String): Boolean {
        try {
            val sdf = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
            val deadline = sdf.parse("$dueDate $dueTime") ?: return false

            val currentTime = Date()
            val diffInMillis = deadline.time - currentTime.time
            val hoursRemaining = TimeUnit.MILLISECONDS.toHours(diffInMillis)

            return hoursRemaining in 1..24
        } catch (e: Exception) {
            return false
        }
    }

    // Check if task is overdue
    fun isOverdue(dueDate: String, dueTime: String): Boolean {
        try {
            val sdf = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
            val deadline = sdf.parse("$dueDate $dueTime") ?: return false

            val currentTime = Date()
            return currentTime.after(deadline)
        } catch (e: Exception) {
            return false
        }
    }

    // Format date to readable format
    fun formatDateReadable(date: String): String {
        try {
            val inputFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            val outputFormat = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())
            val parsedDate = inputFormat.parse(date) ?: return date
            return outputFormat.format(parsedDate)
        } catch (e: Exception) {
            return date
        }
    }
}
