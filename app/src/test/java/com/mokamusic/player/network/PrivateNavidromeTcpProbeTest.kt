package com.mokamusic.player.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.ConnectException
import java.net.SocketTimeoutException

class PrivateNavidromeTcpProbeTest {
    private val lab = "http://10.0.0.169:4533"

    @Test fun noConnectionUnlessExactPrivateHomeEndpoint() {
        val failures = listOf(
            "",
            "https://10.0.0.169:4533",
            "http://10.0.0.169:4534",
            "http://10.0.0.170:4533",
            "http://8.8.8.8:4533",
            "http://user:password@10.0.0.169:4533",
            "http://10.0.0.169:4533/?token=secret",
            "http://10.0.0.169:4533/#fragment",
            "http://navidrome.local:4533",
            "http://10.0.0.169"
        )
        var called = false
        failures.forEach { invalid ->
            assertEquals(PrivateNavidromeTcpProbe.Result.URL_INVALID,
                PrivateNavidromeTcpProbe.test(invalid, true, true) { called = true })
        }
        assertFalse(called)
    }

    @Test fun cellularAndPrivateDnsAreRequiredBeforeSocket() {
        var called = false
        assertEquals(PrivateNavidromeTcpProbe.Result.NOT_CELLULAR,
            PrivateNavidromeTcpProbe.test(lab, false, true) { called = true })
        assertEquals(PrivateNavidromeTcpProbe.Result.PRIVATE_DNS_NOT_OFF,
            PrivateNavidromeTcpProbe.test(lab, true, false) { called = true })
        assertFalse(called)
    }

    @Test fun exactlyOneHandshakeAndNoHttpPayload() {
        var attempts = 0
        assertEquals(PrivateNavidromeTcpProbe.Result.TCP_CONNECTED,
            PrivateNavidromeTcpProbe.test(lab, true, true) { target ->
                attempts++
                assertEquals("10.0.0.169", target.address.hostAddress)
                assertEquals(4533, target.port)
                // This injected connector deliberately does not send any data.
            })
        assertEquals(1, attempts)
    }

    @Test fun timeoutRefusalAndFailureAreRedacted() {
        val timeout = PrivateNavidromeTcpProbe.test(lab, true, true) {
            throw SocketTimeoutException("sensitive internal address")
        }
        assertEquals(PrivateNavidromeTcpProbe.Result.TCP_TIMEOUT, timeout)
        assertFalse(timeout.description.contains("sensitive"))
        val reject = PrivateNavidromeTcpProbe.test(lab, true, true) {
            throw ConnectException("sensitive internal address")
        }
        assertEquals(PrivateNavidromeTcpProbe.Result.TCP_REFUSED, reject)
        val general = PrivateNavidromeTcpProbe.test(lab, true, true) {
            throw IllegalStateException("sensitive internal address")
        }
        assertEquals(PrivateNavidromeTcpProbe.Result.TCP_ERROR, general)
        assertTrue(general.description.contains("No HTTP"))
    }
}
