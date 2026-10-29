package com.mokamusic.player.network

import android.net.Uri
import java.net.URL

/**
 * Single explicitly opt-in cleartext endpoint. No LAN ranges, hostnames,
 * custom ports or user-defined HTTP destinations are accepted.
 */
internal object PrivateVpnServerRules {
    const val HOST = "10.77.0.1"
    const val PORT = 4533
    const val BASE_URL = "http://10.77.0.1:4533"

    fun isPrivateWireGuard(input: String): Boolean =
        runCatching { isPrivateWireGuard(URL(input.trim())) }.getOrDefault(false)

    fun isPrivateWireGuard(url: URL): Boolean =
        url.protocol.equals("http", ignoreCase = true) &&
            url.host == HOST && url.port == PORT

    fun isPrivateWireGuard(uri: Uri): Boolean =
        uri.scheme.equals("http", ignoreCase = true) &&
            uri.host == HOST && uri.port == PORT
}
