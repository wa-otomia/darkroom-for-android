package io.github.wa_otomia.darkroom.ui.components

import androidx.compose.ui.geometry.Offset
import io.github.wa_otomia.darkroom.core.SnapGuide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SnapDragStateTest {
    @Test
    fun syncClearsPinAndNextUpdateIsRelativeToSyncedPose() {
        val snap = SnapDragState(positionThresholdPx = 10f, rotationThresholdDeg = 3f)
        val guidesX = listOf(SnapGuide("c", 0f))
        val guidesY = emptyList<SnapGuide>()
        snap.begin(-30f, 5f, 10f)
        val inhaled = snap.update(Offset(20f, 0f), 1f, 0f, guidesX, guidesY)
        assertTrue(inhaled.snappedIn)
        assertEquals(0f, snap.x, 0.01f)
        assertTrue(snap.activeGuides.isNotEmpty())

        snap.sync(-40f, 8f, 45f)
        assertEquals(-40f, snap.x, 0.01f)
        assertEquals(8f, snap.y, 0.01f)
        assertEquals(45f, snap.rotation, 0.01f)
        assertTrue(snap.activeGuides.isEmpty())

        val next = snap.update(Offset(5f, 2f), 1f, 0f, guidesX, guidesY)
        assertFalse(next.snappedIn)
        assertEquals(-35f, snap.x, 0.01f)
        assertEquals(10f, snap.y, 0.01f)
        assertEquals(45f, snap.rotation, 0.01f)
    }
}
