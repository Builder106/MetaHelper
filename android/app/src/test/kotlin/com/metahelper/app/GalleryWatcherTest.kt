package com.metahelper.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests the Meta-image path predicate — the single line that decides whether a
 * new gallery photo triggers the whole capture→backend→audio pipeline.
 */
class GalleryWatcherTest {

    @Test
    fun matchesMetaCapturePaths() {
        assertTrue(isMetaImagePath("/storage/emulated/0/Pictures/Meta AI/IMG_001.jpg"))
        assertTrue(isMetaImagePath("/storage/emulated/0/DCIM/Ray-Ban_0420.jpg"))
        // case-insensitive
        assertTrue(isMetaImagePath("/storage/emulated/0/pictures/meta/x.jpg"))
    }

    @Test
    fun ignoresOrdinaryCameraAndScreenshotPaths() {
        assertFalse(isMetaImagePath("/storage/emulated/0/DCIM/Camera/IMG_2024.jpg"))
        assertFalse(isMetaImagePath("/storage/emulated/0/Pictures/Screenshots/Screenshot_1.png"))
        assertFalse(isMetaImagePath("/storage/emulated/0/Download/receipt.jpg"))
    }

    @Test
    fun documentsKnownFalsePositives() {
        // The current substring match is intentionally broad. These cases pin
        // the over-matching contract so that tightening it later (e.g. anchoring
        // to the real RELATIVE_PATH folder) is a deliberate, test-visible change.
        assertTrue(isMetaImagePath("/storage/emulated/0/DCIM/Panorama/pano_1.jpg")) // "Pano"
        assertTrue(isMetaImagePath("/storage/emulated/0/Pictures/metalwork/weld.jpg")) // "meta"
    }
}
