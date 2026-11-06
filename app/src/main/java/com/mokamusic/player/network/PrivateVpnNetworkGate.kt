package com.mokamusic.player.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import java.io.IOException
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.Proxy
import java.net.URL

/**
 * Restricts an explicitly chosen private numeric Navidrome endpoint to exactly one
 * Android VPN Network with the most-specific route to that destination.
 * Neither the API client nor playback may quietly switch to Wi-Fi/cellular.
 */
internal object PrivateVpnNetworkGate {
    @Throws(IOException::class)
    fun requireVpnNetwork(context: Context, host: String): Network {
        val destination = PrivateVpnServerRules.privateNumericAddress(host)
            ?: throw IOException("Private HTTP requires a numeric private IPv4 or ULA IPv6 address")
        val manager = context.applicationContext
            .getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: throw IOException("Android VPN network unavailable")
        // Prefer the most-specific VPN route. A full-tunnel default also
        // matches 10.0.0.169, but must not win over a /24 Home VPN route.
        val candidates = manager.allNetworks.mapNotNull { network ->
            val capabilities = manager.getNetworkCapabilities(network)
            val routes = manager.getLinkProperties(network)?.routes
            if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) != true) {
                return@mapNotNull null
            }
            val best = routes?.filter { it.matches(destination) }
                ?.maxOfOrNull { it.destination.prefixLength } ?: return@mapNotNull null
            network to best
        }
        val longest = candidates.maxOfOrNull { it.second } ?: throw IOException(
            "No Android-recognized VPN route to the private Navidrome endpoint; " +
                    "kernel-only routing is not sufficient for Moka's fail-closed HTTP mode")
        val winners = candidates.filter { it.second == longest }
        if (winners.size != 1) {
            throw IOException("Ambiguous private Navidrome VPN route; refusing HTTP fallback")
        }
        return winners.single().first
    }

    @Throws(IOException::class)
    fun requireVpnNetwork(context: Context, uri: Uri): Network {
        if (!PrivateVpnServerRules.isPrivateWireGuard(uri)) {
            throw IOException("Private HTTP stream destination is not approved")
        }
        return requireVpnNetwork(context, uri.host.orEmpty())
    }

    @Throws(IOException::class)
    fun openConnection(context: Context, url: URL): HttpURLConnection {
        if (!PrivateVpnServerRules.isPrivateWireGuard(url)) {
            throw IOException("Private HTTP is restricted to the configured WireGuard endpoint")
        }
        return (requireVpnNetwork(context, url.host)
            .openConnection(url, Proxy.NO_PROXY) as HttpURLConnection).apply {
            // Never follow HTTP redirects to a non-VPN destination.
            instanceFollowRedirects = false
        }
    }
}
