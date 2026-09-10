package io.github.wa_otomia.darkroom.ui.studio

import org.junit.Assert.assertEquals
import org.junit.Test

class StudioPanelHeightTest {
    @Test
    fun tallestWinsWhenUnderCap() {
        assertEquals(80, studioEqualPanelHeight(intArrayOf(20, 80, 40), capPx = 200))
    }

    @Test
    fun capClampsTallest() {
        assertEquals(100, studioEqualPanelHeight(intArrayOf(20, 180, 40), capPx = 100))
    }

    @Test
    fun emptyOrNonPositiveCapIsZero() {
        assertEquals(0, studioEqualPanelHeight(intArrayOf(), capPx = 100))
        assertEquals(0, studioEqualPanelHeight(intArrayOf(40, 80), capPx = 0))
        assertEquals(0, studioEqualPanelHeight(intArrayOf(40, 80), capPx = -1))
    }
}
