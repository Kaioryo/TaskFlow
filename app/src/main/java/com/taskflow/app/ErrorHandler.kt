package com.taskflow.app

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.firebase.crashlytics.FirebaseCrashlytics
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object ErrorHandler {

    private val crashlytics = FirebaseCrashlytics.getInstance()

    /**
     * Handle exceptions with user-friendly messages and crash reporting
     */
    fun handleException(
        context: Context,
        exception: Exception,
        userMessage: String = "An error occurred",
        showToast: Boolean = true
    ) {
        // Log to Logcat
        Log.e("ErrorHandler", "Error: ${exception.message}", exception)

        // Log to Crashlytics
        crashlytics.recordException(exception)

        // Show user-friendly message
        if (showToast) {
            val message = getUserFriendlyMessage(exception, userMessage)
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Get user-friendly error message based on exception type
     */
    private fun getUserFriendlyMessage(exception: Exception, defaultMessage: String): String {
        return when (exception) {
            is UnknownHostException, is IOException -> {
                "❌ No internet connection. Please check your network."
            }
            is SocketTimeoutException -> {
                "⏱️ Connection timeout. Please try again."
            }
            is com.google.firebase.FirebaseNetworkException -> {
                "❌ Network error. Please check your connection."
            }
            is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> {
                "❌ Invalid email or password."
            }
            is com.google.firebase.auth.FirebaseAuthUserCollisionException -> {
                "❌ This email is already registered."
            }
            is com.google.firebase.auth.FirebaseAuthWeakPasswordException -> {
                "❌ Password is too weak. Use at least 6 characters."
            }
            else -> {
                "❌ $defaultMessage: ${exception.message}"
            }
        }
    }

    /**
     * Log custom events to Crashlytics
     */
    fun logEvent(key: String, value: String) {
        crashlytics.setCustomKey(key, value)
        Log.d("ErrorHandler", "Event logged: $key = $value")
    }

    /**
     * Set user identifier for crash reports
     */
    fun setUserId(userId: String) {
        crashlytics.setUserId(userId)
        Log.d("ErrorHandler", "User ID set: $userId")
    }

    /**
     * Log breadcrumb (activity trail)
     */
    fun logBreadcrumb(message: String) {
        crashlytics.log(message)
        Log.d("ErrorHandler", "Breadcrumb: $message")
    }
}
