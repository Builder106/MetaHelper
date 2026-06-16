package com.metahelper.app

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests the volume save/restore invariant — the logic that must put the user's
 * media volume back instead of leaving the device pinned at level 1. Uses the
 * injectable read/write seam so no AudioManager is needed.
 */
class VolumeControllerTest {

    private fun controllerOver(initial: Int): Pair<VolumeController, () -> Int> {
        var volume = initial
        val vc = VolumeController(getVolume = { volume }, setVolume = { volume = it })
        return vc to { volume }
    }

    @Test
    fun restoresTheOriginalVolume() {
        val (vc, volume) = controllerOver(7)
        vc.setQuietVolume()
        assertEquals(1, volume())   // quieted
        vc.restoreVolume()
        assertEquals(7, volume())   // back to original
    }

    @Test
    fun backToBackQuietRestoresOriginalNotOne() {
        // The guard must not re-capture the already-quieted value: two captures
        // followed by one restore should return to the true original (9), not 1.
        val (vc, volume) = controllerOver(9)
        vc.setQuietVolume()
        vc.setQuietVolume()
        vc.restoreVolume()
        assertEquals(9, volume())
    }

    @Test
    fun restoreWithoutQuietIsANoOp() {
        var writes = 0
        var volume = 5
        val vc = VolumeController(getVolume = { volume }, setVolume = { volume = it; writes++ })
        vc.restoreVolume()
        assertEquals(5, volume)
        assertEquals(0, writes)     // nothing captured -> nothing written
    }
}
