package com.mokamusic.player.network

import android.net.Uri
import java.net.InetAddress
import java.net.URL

/**
 * User-configured private numeric HTTP endpoints (custom port supported).
 * Public internet and DNS hostnames continue to require HTTPS.
 */
internal object PrivateVpnServerRules {
    fun privateNumericAddress(host: String): InetAddress? {
        val literal = host.removePrefix("[").removeSuffix("]")
        if (literal.contains(':')) {
            // Only literal ULA IPv6 (fc00::/7), no scope IDs or DNS resolution.
            if (!literal.matches(Regex("[0-9a-fA-F:]+"))) return null
            return runCatching { InetAddress.getByName(literal) }.getOrNull()
                ?.takeIf { it.address.size == 16 && (it.address[0].toInt() and 0xfe) == 0xfc }
        }
        val parts = literal.split('.')
        if (parts.size != 4 || parts.any {
                it.isEmpty() || it.length > 3 || !it.all(Char::isDigit)
            }) return null
        val bytes = parts.map { it.toIntOrNull() ?: return null }
        if (bytes.any { it !in 0..255 }) return null
        val permitted = bytes[0] == 10 ||
            (bytes[0] == 172 && bytes[1] in 16..31) ||
            (bytes[0] == 192 && bytes[1] == 168) ||
            (bytes[0] == 100 && bytes[1] in 64..127)
        if (!permitted) return null
        return InetAddress.getByAddress(bytes.map { it.toByte() }.toByteArray())
    }

    fun isPrivateWireGuard(input: String): Boolean =
        runCatching { isPrivateWireGuard(URL(input.trim())) }.getOrDefault(false)

    fun isPrivateWireGuard(url: URL): Boolean =
        url.protocol.equals("http", ignoreCase = true) &&
            url.port in 1..65535 &&
            privateNumericAddress(url.host) != null

    fun isPrivateWireGuard(uri: Uri): Boolean =
        uri.scheme.equals("http", ignoreCase = true) &&
            uri.port in 1..65535 &&
            privateNumericAddress(uri.host.orEmpty()) != null
}
