package com.mokamusic.player.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivateNavidromeRoutePreflightTest {
    @Test fun emptyAndMalformedNeverInspectRoutes() {
        var called = false
        val route: (String) -> Boolean = { called = true; true }
        assertEquals(PrivateNavidromeRoutePreflight.Result.SERVER_MISSING,
            PrivateNavidromeRoutePreflight.classify(" ", route))
        assertEquals(PrivateNavidromeRoutePreflight.Result.SERVER_INVALID,
            PrivateNavidromeRoutePreflight.classify("not a url", route))
        assertEquals(PrivateNavidromeRoutePreflight.Result.SERVER_INVALID,
            PrivateNavidromeRoutePreflight.classify("http://8.8.8.8:4533", route))
        assertEquals(PrivateNavidromeRoutePreflight.Result.SERVER_INVALID,
            PrivateNavidromeRoutePreflight.classify("http://10.0.0.169:4533/?u=secret", route))
        assertEquals(PrivateNavidromeRoutePreflight.Result.SERVER_INVALID,
            PrivateNavidromeRoutePreflight.classify("http://user:secret@10.0.0.169:4533", route))
        assertFalse(called)
    }

    @Test fun httpsIsIndependentOfPrivateHttpAndDoesNotProbe() {
        var called = false
        val result = PrivateNavidromeRoutePreflight.classify(
            "https://example.invalid/navidrome") { called = true; false }
        assertEquals(PrivateNavidromeRoutePreflight.Result.HTTPS_ONLY, result)
        assertFalse(called)
        assertTrue(result.description.contains("No connection"))
    }

    @Test fun privateHttpRequiresPositiveAndroidVpnProof() {
        val address = "http://10.0.0.169:4533"
        var calls = 0
        assertEquals(PrivateNavidromeRoutePreflight.Result.PRIVATE_VPN_BLOCKED,
            PrivateNavidromeRoutePreflight.classify(address) {
                calls++
                false
            })
        assertEquals(PrivateNavidromeRoutePreflight.Result.PRIVATE_VPN_BLOCKED,
            PrivateNavidromeRoutePreflight.classify(address) {
                calls++
                throw IllegalStateException("No Android VPN route")
            })
        assertEquals(PrivateNavidromeRoutePreflight.Result.PRIVATE_VPN_READY,
            PrivateNavidromeRoutePreflight.classify(address) {
                calls++
                it == "10.0.0.169"
            })
        assertEquals(3, calls)
    }

    @Test fun privateIpv6AndTailscaleRangesRemainPrivateOnly() {
        assertEquals(PrivateNavidromeRoutePreflight.Result.PRIVATE_VPN_BLOCKED,
            PrivateNavidromeRoutePreflight.classify("http://100.96.12.2:4533") { false })
        assertEquals(PrivateNavidromeRoutePreflight.Result.PRIVATE_VPN_BLOCKED,
            PrivateNavidromeRoutePreflight.classify("http://[fd12::4]:4533") { false })
    }
}
