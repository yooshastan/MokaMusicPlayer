package com.mokamusic.player.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import java.io.IOException
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.Proxy
import java.net.URL

/**
 * Restricts the explicitly selected private Navidrome endpoint to exactly one
 * Android VPN Network with a route to the configured WireGuard server.
 * Neither the API client nor playback may quietly switch to Wi-Fi/cellular.
 */
internal object PrivateVpnNetworkGate {
    private val destination = InetAddress.getByAddress(byteArrayOf(10, 77, 0, 1))

    @Throws(IOException::class)
    fun requireVpnNetwork(context: Context): Network {
        val manager = context.applicationContext
            .getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: throw IOException("Android VPN network unavailable")
        val candidates = manager.allNetworks.filter { network ->
            val capabilities = manager.getNetworkCapabilities(network)
            val routes = manager.getLinkProperties(network)?.routes
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true &&
                routes?.any { it.matches(destination) } == true
        }
        if (candidates.size != 1) {
            throw IOException("Private Navidrome requires one active VPN route to 10.77.0.1")
        }
        return candidates.single()
    }

    @Throws(IOException::class)
    fun openConnection(context: Context, url: URL): HttpURLConnection {
        if (!PrivateVpnServerRules.isPrivateWireGuard(url)) {
            throw IOException("Private HTTP is restricted to the configured WireGuard endpoint")
        }
        return requireVpnNetwork(context)
            .openConnection(url, Proxy.NO_PROXY) as HttpURLConnection
    }
}
