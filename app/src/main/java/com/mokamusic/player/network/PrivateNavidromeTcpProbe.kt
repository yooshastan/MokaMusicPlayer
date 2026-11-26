package com.mokamusic.player.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.provider.Settings
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.URL

/**
 * Diagnostic ONLY: one credential-free TCP handshake from Moka's own UID.
 *
 * Never sends an HTTP request, authentication, token, or stream bytes. This
 * MUST NOT be treated as proof of Wonhee routing, fail-closed protection or
 * permission to bypass PrivateVpnNetworkGate.
 */
internal object PrivateNavidromeTcpProbe {
    internal enum class Result(val description: String) {
        URL_INVALID("Enter an approved private numeric HTTP server URL without credentials or query parameters."),
        NOT_CELLULAR("Test refused: use cellular with Wi-Fi disconnected and no Android VPN, as required by this RootLab experiment."),
        PRIVATE_DNS_NOT_OFF("Test refused: Android Private DNS must remain OFF for this RootLab experiment."),
        TCP_CONNECTED("TCP handshake succeeded from Moka's UID. NO HTTP, credentials, or playback bytes were sent. This does NOT prove Home tunnel routing, DNS safety or no-fallback; compare Wonhee's live UID and firewall counters."),
        TCP_TIMEOUT("TCP handshake timed out. No HTTP or credentials were sent; compare Wonhee's Home and REJECT counters."),
        TCP_REFUSED("TCP connection was refused or rejected. No HTTP or credentials were sent; compare Wonhee's Home and REJECT counters."),
        TCP_ERROR("TCP handshake failed. No HTTP or credentials were sent; inspect Wonhee's socket and firewall counters.")
    }

    /** This lab's exact Home guard permits TCP/4533 to 10.0.0.169 only. */
    private const val LAB_HOST = "10.0.0.169"
    private const val LAB_PORT = 4533

    private fun verifiedTarget(input: String): InetSocketAddress? {
        val url = runCatching { URL(input.trim()) }.getOrNull() ?: return null
        if (url.userInfo != null || url.query != null || url.ref != null) return null
        if (!url.protocol.equals("http", ignoreCase = true)
            || url.host != LAB_HOST || url.port != LAB_PORT
            || url.path.trim('/').isNotEmpty()) return null
        val address = PrivateVpnServerRules.privateNumericAddress(url.host) ?: return null
        if (address.address.size != 4) return null
        return InetSocketAddress(address, url.port)
    }

    /**
     * Injectable connector keeps all policy tests offline. One handshake,
     * no HTTP requests, no automatic retries, no socket reuse.
     */
    internal fun test(
        server: String,
        onCellularWithoutVpn: Boolean,
        privateDnsOff: Boolean,
        connectOnce: (InetSocketAddress) -> Unit
    ): Result {
        val target = verifiedTarget(server) ?: return Result.URL_INVALID
        if (!privateDnsOff) return Result.PRIVATE_DNS_NOT_OFF
        if (!onCellularWithoutVpn) return Result.NOT_CELLULAR
        return try {
            connectOnce(target)
            Result.TCP_CONNECTED
        } catch (e: SocketTimeoutException) {
            Result.TCP_TIMEOUT
        } catch (e: java.net.ConnectException) {
            Result.TCP_REFUSED
        } catch (e: Exception) {
            Result.TCP_ERROR
        }
    }

    /** Must be executed on Dispatchers.IO, never on the UI thread. */
    fun runOnce(context: Context, server: String): Result {
        val app = context.applicationContext
        val dnsOff = Settings.Global.getString(
            app.contentResolver, "private_dns_mode"
        ) == "off"
        val cm = app.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val active = cm?.activeNetwork
        val capabilities = active?.let { cm.getNetworkCapabilities(it) }
        val cellular = capabilities != null &&
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) &&
            !capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
            !capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)

        return test(server, cellular, dnsOff) { target ->
            Socket().use { socket ->
                // No HTTP application bytes, no read, no request or credentials.
                socket.connect(target, 2500)
            }
        }
    }
}
