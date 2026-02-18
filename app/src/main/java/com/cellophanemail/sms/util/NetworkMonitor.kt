package com.cellophanemail.sms.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkMonitor @Inject constructor(
    @ApplicationContext context: Context
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isConnected = MutableStateFlow(checkConnected())
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _isOnWifi = MutableStateFlow(checkWifi())
    val isOnWifi: StateFlow<Boolean> = _isOnWifi.asStateFlow()

    init {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _isConnected.value = true
                _isOnWifi.value = checkWifi()
            }

            override fun onLost(network: Network) {
                _isConnected.value = checkConnected()
                _isOnWifi.value = checkWifi()
            }

            override fun onCapabilitiesChanged(
                network: Network,
                capabilities: NetworkCapabilities
            ) {
                _isConnected.value = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                _isOnWifi.value = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            }
        })
    }

    private fun checkConnected(): Boolean {
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    private fun checkWifi(): Boolean {
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        return capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    }
}
