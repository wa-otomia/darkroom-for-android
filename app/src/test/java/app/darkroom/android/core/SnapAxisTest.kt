package app.darkroom.android.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class SnapAxisTest {
    private val center = listOf(SnapGuide("c", 0f))

    @Test
    fun minus30ToMinus10SnapsToZero() {
        val axis = SnapAxis(threshold = 10f)
        axis.begin(-30f)
        assertFalse(axis.drag(10f, center))
        assertEquals(-20f, axis.value, 0.01f)
        assertFalse(axis.drag(9f, center))
        assertEquals(-11f, axis.value, 0.01f)
        assertEquals(-11f, axis.raw, 0.01f)
        assertTrue(axis.drag(1f, center))
        assertEquals(0f, axis.value, 0.01f)
        assertEquals(-10f, axis.raw, 0.01f)
        assertEquals("c", axis.snapped?.id)
    }

    @Test
    fun rawWanderingInsideDeadZoneKeepsValuePinned() {
        val axis = SnapAxis(threshold = 10f)
        axis.begin(-30f)
        assertTrue(axis.drag(20f, center))
        assertEquals(0f, axis.value, 0.01f)
        assertEquals(-10f, axis.raw, 0.01f)

        assertFalse(axis.drag(1f, center))
        assertEquals(0f, axis.value, 0.01f)
        assertEquals(-9f, axis.raw, 0.01f)

        assertFalse(axis.drag(4f, center))
        assertEquals(0f, axis.value, 0.01f)
        assertEquals(-5f, axis.raw, 0.01f)

        assertFalse(axis.drag(8f, center))
        assertEquals(0f, axis.value, 0.01f)
        assertEquals(3f, axis.raw, 0.01f)

        assertFalse(axis.drag(6f, center))
        assertEquals(0f, axis.value, 0.01f)
        assertEquals(9f, axis.raw, 0.01f)
    }

    @Test
    fun rawMinus11ExitsToMinus1() {
        val axis = SnapAxis(threshold = 10f)
        axis.begin(-30f)
        assertTrue(axis.drag(20f, center))
        assertFalse(axis.drag(19f, center))
        assertEquals(9f, axis.raw, 0.01f)
        assertFalse(axis.drag(-20f, center))
        assertEquals(-1f, axis.value, 0.01f)
        assertEquals(-1f, axis.raw, 0.01f)
        assertNull(axis.snapped)
    }

    @Test
    fun movingAwayAfterExitDoesNotResnap() {
        val axis = exitToMinus1()
        assertEquals(-1f, axis.value, 0.01f)
        assertNull(axis.snapped)

        assertFalse(axis.drag(-1f, center))
        assertEquals(-2f, axis.value, 0.01f)
        assertNull(axis.snapped)
        assertFalse(axis.drag(-1f, center))
        assertEquals(-3f, axis.value, 0.01f)
        assertNull(axis.snapped)
    }

    @Test
    fun reversingTowardGuideResnaps() {
        val axis = exitToMinus1()
        assertFalse(axis.drag(-1f, center))
        assertEquals(-2f, axis.value, 0.01f)

        assertTrue(axis.drag(1f, center))
        assertEquals(0f, axis.value, 0.01f)
        assertEquals(-1f, axis.raw, 0.01f)
        assertEquals("c", axis.snapped?.id)
    }

    private fun exitToMinus1(): SnapAxis {
        val axis = SnapAxis(threshold = 10f)
        axis.begin(-30f)
        axis.drag(20f, center)
        axis.drag(19f, center)
        axis.drag(-20f, center)
        return axis
    }

    @Test
    fun rotationWrapsAcrossZeroThenExitsToOne() {
        val axis = SnapAxis(threshold = 3f, wrap = 360f)
        val guides = rotationSnapGuides()
        axis.begin(357f)
        assertTrue(axis.drag(3f, guides))
        assertEquals(0f, axis.value, 0.01f)
        assertEquals(0f, axis.raw, 0.01f)
        assertEquals(GUIDE_ROT_0, axis.snapped?.id)

        assertFalse(axis.drag(4f, guides))
        assertEquals(1f, axis.value, 0.01f)
        assertEquals(1f, axis.raw, 0.01f)
        assertNull(axis.snapped)
    }

    @Test
    fun multipleGuidesChooseNearest() {
        val axis = SnapAxis(threshold = 10f)
        val guides = listOf(SnapGuide("near", 5f), SnapGuide("far", 8f))
        axis.begin(0f)
        assertTrue(axis.drag(1f, guides))
        assertEquals(5f, axis.value, 0.01f)
        assertEquals("near", axis.snapped?.id)
    }

    @Test
    fun deltaZeroNeverSnaps() {
        val axis = SnapAxis(threshold = 10f)
        axis.begin(5f)
        assertFalse(axis.drag(0f, center))
        assertEquals(5f, axis.value, 0.01f)
        assertEquals(5f, axis.raw, 0.01f)
        assertNull(axis.snapped)

        axis.begin(-30f)
        axis.drag(20f, center)
        assertEquals(0f, axis.value, 0.01f)
        assertFalse(axis.drag(0f, center))
        assertEquals(0f, axis.value, 0.01f)
        assertEquals(-10f, axis.raw, 0.01f)
    }

    @Test
    fun beginResyncsRawToValue() {
        val axis = SnapAxis(threshold = 10f)
        axis.begin(-30f)
        axis.drag(20f, center)
        assertTrue(abs(axis.raw - axis.value) > 1f)
        axis.begin(axis.value)
        assertEquals(axis.value, axis.raw, 0.01f)
        assertNull(axis.snapped)
    }

    @Test
    fun syncClearsPinAndNextDragIsRelativeToSyncedValue() {
        val axis = SnapAxis(threshold = 10f)
        axis.begin(-30f)
        assertTrue(axis.drag(20f, center))
        assertEquals(0f, axis.value, 0.01f)
        assertEquals("c", axis.snapped?.id)
        assertTrue(abs(axis.raw - axis.value) > 1f)

        axis.sync(-40f)
        assertEquals(-40f, axis.value, 0.01f)
        assertEquals(-40f, axis.raw, 0.01f)
        assertNull(axis.snapped)

        assertFalse(axis.drag(5f, center))
        assertEquals(-35f, axis.value, 0.01f)
        assertEquals(-35f, axis.raw, 0.01f)
        assertNull(axis.snapped)
    }

    @Test
    fun wrapDistIsShortestSignedArc() {
        val axis = SnapAxis(threshold = 3f, wrap = 360f)
        assertEquals(3f, axis.dist(357f, 0f), 0.01f)
        assertEquals(-3f, axis.dist(0f, 357f), 0.01f)
        assertEquals(0f, axis.norm(360f), 0.01f)
        assertEquals(10f, axis.norm(-350f), 0.01f)
    }
}
