package com.mokamusic.player.network

import android.content.Context
import java.net.URL

/**
 * A no-credentials, no-packets preflight for the configured Navidrome URL.
 * Root-managed WireGuard is not an Android Network and cannot be used by
 * PrivateVpnNetworkGate unless an independently verified transport is added.
 *
 * This NEVER authorizes private HTTP connections. Actual API and audio streams
 * continue to use PrivateVpnNetworkGate, which can refuse at connection time.
 */
internal object PrivateNavidromeRoutePreflight {
    enum class Result(val description: String) {
        SERVER_MISSING("Enter a Navidrome server URL before checking the route."),
        SERVER_INVALID("The server URL is invalid. Use HTTPS or an approved private numeric HTTP IP:port."),
        HTTPS_ONLY("HTTPS configured. No connection was attempted; this does not confirm server reachability."),
        PRIVATE_VPN_READY(
            "An Android-recognized VPN route is currently available. " +
                "No connection was attempted, and each private HTTP request will check the route again."
        ),
        PRIVATE_VPN_BLOCKED(
            "Protected private HTTP is blocked: Android does not expose exactly one " +
                "approved VPN route to this address. Wonhee's root-only WireGuard " +
                "route is not enough to authorize Moka traffic. " +
                "No credentials or connection requests were sent."
        )
    }

    /** Injectable route test permits offline tests without using Android networking. */
    internal fun classify(
        input: String,
        vpnRouteVerified: (String) -> Boolean
    ): Result {
        if (input.isBlank()) return Result.SERVER_MISSING
        val url = runCatching { URL(input.trim()) }.getOrNull()
            ?: return Result.SERVER_INVALID
        // A URL bearing user info, a query, or a fragment is not accepted.
        if (url.host.isBlank() || url.userInfo != null ||
            url.query != null || url.ref != null) return Result.SERVER_INVALID
        if (url.protocol.equals("https", ignoreCase = true)) return Result.HTTPS_ONLY
        if (!PrivateVpnServerRules.isPrivateWireGuard(url)) return Result.SERVER_INVALID
        return if (runCatching { vpnRouteVerified(url.host) }.getOrDefault(false)) {
            Result.PRIVATE_VPN_READY
        } else {
            Result.PRIVATE_VPN_BLOCKED
        }
    }

    fun inspect(context: Context, input: String): Result =
        classify(input) { host ->
            PrivateVpnNetworkGate.requireVpnNetwork(context, host)
            true
        }
}
