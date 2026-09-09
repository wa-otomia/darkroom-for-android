package app.darkroom.android.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PrintOrientationTest {
    @Test
    fun defaultLandscapeFollowsAspect() {
        assertTrue(defaultLandscape(4000, 3000))
        assertFalse(defaultLandscape(3000, 4000))
        assertFalse(defaultLandscape(2000, 2000))
        assertFalse(defaultLandscape(0, 0))
    }

    @Test
    fun resolvePrintLandscapeHonorsExplicitStudioChoice() {
        val wide = OrientedSize(4000, 3000)
        assertFalse(resolvePrintLandscape(false, wide))
        assertTrue(resolvePrintLandscape(true, OrientedSize(3000, 4000)))
        assertTrue(resolvePrintLandscape(null, wide))
        assertFalse(resolvePrintLandscape(null, OrientedSize(3000, 4000)))
        assertFalse(resolvePrintLandscape(null, null))
    }

    @Test
    fun printImageSizePrefersEditWhenSourceIsAnEdit() {
        val edit = EditRecord("e1", "film", "t", "e1.jpg", width = 3200, height = 1800)
        val photo = PhotoMeta("p1", "a.jpg", "", "", 2000, 3000, 1, edits = listOf(edit))
        assertEquals(OrientedSize(2000, 3000), printImageSize(photo, "original"))
        assertEquals(OrientedSize(3200, 1800), printImageSize(photo, "e1"))
        assertEquals(OrientedSize(2000, 3000), printImageSize(photo, "missing"))
        assertNull(printImageSize(null, "original"))
    }

    @Test
    fun printImageSizeFallsBackWhenEditHasNoDimensions() {
        val edit = EditRecord("e1", "film", "t", "e1.jpg", width = 0, height = 0)
        val photo = PhotoMeta("p1", "a.jpg", "", "", 2400, 1800, 1, edits = listOf(edit))
        assertEquals(OrientedSize(2400, 1800), printImageSize(photo, "e1"))
        assertTrue(resolvePrintLandscape(null, printImageSize(photo, "e1")))
    }
}
