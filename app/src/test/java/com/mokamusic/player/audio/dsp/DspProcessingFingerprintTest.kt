package com.mokamusic.player.audio.dsp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DspProcessingFingerprintTest {
    @Test fun displayNamesCannotTriggerAudioChainRebuild() {
        val original = DspSettings(
            masterEnabled = true,
            eqEnabled = true,
            ddcEnabled = true,
            ddcUri = "content://test/ddc",
            ddcName = "Old Name",
            convolverEnabled = true,
            convolverUri = "content://test/impulse",
            convolverName = "Old Impulse"
        )
        val renamed = original.copy(ddcName = "New Name", convolverName = "New Impulse")
        assertEquals(original.processingFingerprint(), renamed.processingFingerprint())
        assertNotEquals(original.processingFingerprint(),
            original.copy(eqGainsDb = original.eqGainsDb.toMutableList().apply { this[0] += 1f })
                .processingFingerprint())
        assertNotEquals(original.processingFingerprint(),
            original.copy(convolverGainDb = 3f).processingFingerprint())
    }
}
