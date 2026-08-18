package com.supernet.app.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities

class NetworkCapabilitiesDetector(context: Context) {
    private val connectivity = context.getSystemService(ConnectivityManager::class.java)

    fun activeLinks(): List<NetworkLink> = connectivity.allNetworks.mapNotNull { network ->
        val capabilities = connectivity.getNetworkCapabilities(network) ?: return@mapNotNull null
        val transport = when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> TransportType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> TransportType.CELLULAR
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> TransportType.ETHERNET
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> TransportType.BLUETOOTH
            else -> return@mapNotNull null
        }

        val id = "${transport.name}-${network.networkHandle}"
        NetworkLink(
            id = id,
            transport = transport,
            state = LinkState.ACTIVE,
            bandwidthMbps = capabilities.linkDownstreamBandwidthKbps / 1000.0,
            priority = when (transport) {
                TransportType.ETHERNET -> 100
                TransportType.WIFI -> 80
                TransportType.CELLULAR -> 60
                TransportType.BLUETOOTH -> 20
            }
        )
    }
}
