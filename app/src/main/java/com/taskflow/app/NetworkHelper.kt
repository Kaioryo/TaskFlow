package com.taskflow.app

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log

class NetworkHelper(private val context: Context) {

    private var connectivityManager: ConnectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var onNetworkAvailableCallback: (() -> Unit)? = null

    // Check if device is currently connected to internet
    fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    // Register listener untuk detect ketika network kembali online
    fun registerNetworkCallback(onNetworkAvailable: () -> Unit) {
        this.onNetworkAvailableCallback = onNetworkAvailable

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                Log.d("NetworkHelper", "✅ Network is now available")
                onNetworkAvailableCallback?.invoke()
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                Log.d("NetworkHelper", "❌ Network lost")
            }
        }

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback!!)
    }

    // Unregister listener
    fun unregisterNetworkCallback() {
        networkCallback?.let {
            try {
                connectivityManager.unregisterNetworkCallback(it)
                Log.d("NetworkHelper", "🔌 Network callback unregistered")
            } catch (e: Exception) {
                Log.e("NetworkHelper", "Error unregistering callback: ${e.message}")
            }
        }
        networkCallback = null
        onNetworkAvailableCallback = null
    }
}
