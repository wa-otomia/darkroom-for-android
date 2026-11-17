package io.github.wa_otomia.darkroom.ui.studio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IdPhotoUiStateTest {
    private val navy10 = CutoutKey(
        file = "/lib/p/original.jpg",
        maskRev = "rev1",
        argb = 0xFF1F3A68.toInt(),
        feather = "weak",
        cellWidth = 413,
        cellHeight = 531,
        zoomStep = 10,
    )

    @Test
    fun heldMatchSkips() {
        assertEquals(CutoutStart.Skip, cutoutStart(navy10, held = navy10, lastRequested = navy10, failedMask = null))
    }

    @Test
    fun colourChangeRendersAtOnce() {
        val white = navy10.copy(argb = -1)
        assertEquals(CutoutStart.Now, cutoutStart(white, held = navy10, lastRequested = navy10, failedMask = null))
    }

    @Test
    fun firstRenderRendersAtOnce() {
        assertEquals(CutoutStart.Now, cutoutStart(navy10, held = null, lastRequested = null, failedMask = null))
    }

    @Test
    fun zoomStepIsDebouncedWhileNothingIsHeldYet() {
        // A colour tap started a render (nothing held yet), then the pinch moves the zoom step.
        val step11 = navy10.copy(zoomStep = 11)
        assertEquals(CutoutStart.Debounced, cutoutStart(step11, held = null, lastRequested = navy10, failedMask = null))
        // A re-cut (new mask revision) is in flight while the pinch continues.
        val recut12 = navy10.copy(maskRev = "rev2", zoomStep = 12)
        val recut11 = recut12.copy(zoomStep = 11)
        assertEquals(
            CutoutStart.Debounced,
            cutoutStart(recut12, held = navy10, lastRequested = recut11, failedMask = null),
        )
    }

    @Test
    fun zoomOnlyDifferenceFromHeldIsDebounced() {
        val step12 = navy10.copy(zoomStep = 12)
        assertEquals(CutoutStart.Debounced, cutoutStart(step12, held = navy10, lastRequested = step12, failedMask = null))
    }

    @Test
    fun failedMaskRevisionIsNotRetriedOnZoomOrFeather() {
        val failed = navy10
        assertEquals(CutoutStart.Skip, cutoutStart(navy10.copy(zoomStep = 15), null, navy10, failed))
        assertEquals(CutoutStart.Skip, cutoutStart(navy10.copy(feather = "strong"), null, navy10, failed))
        assertEquals(CutoutStart.Skip, cutoutStart(navy10.copy(argb = -1), null, navy10, failed))
        // A new revision is tried again.
        assertEquals(CutoutStart.Now, cutoutStart(navy10.copy(maskRev = "rev2"), null, navy10, failed))
    }

    @Test
    fun transientCutoutFailureRetriesWithBoundedBackoff() {
        val delays = generateSequence(0) { it + 1 }.map { cutoutRetryDelayMs(it) }.takeWhile { it != null }.toList()
        assertEquals(listOf(500L, 1_500L, 4_000L), delays)
        assertNull(cutoutRetryDelayMs(3))
        assertNull(cutoutRetryDelayMs(100))
    }

    @Test
    fun pendingChoiceSurvivesAnOlderRowValueEqualToTheLatestTap() {
        // Stored navy. Tap white (write A), then navy (write B) before A lands.
        var p = PendingIdChoice().begin("white")
        val a = p.seq
        p = p.begin("navy")
        val b = p.seq
        // The row still says navy: the latest tap must not be cleared while A is in flight.
        p = p.settle("navy")
        assertEquals("navy", p.value)
        // A lands: row white. The preview keeps the latest tap.
        p = p.finish(a, ok = true).settle("white")
        assertEquals("navy", p.value)
        // B lands and the row catches up.
        p = p.finish(b, ok = true)
        assertEquals("navy", p.settle("white").value)
        assertNull(p.settle("navy").value)
    }

    @Test
    fun pendingChoiceClearsWhenANoOpWriteCompletes() {
        var p = PendingIdChoice().begin("navy")
        assertEquals("navy", p.settle("navy").value)
        p = p.finish(p.seq, ok = true)
        assertNull(p.settle("navy").value)
    }

    @Test
    fun onlyTheLatestTapsFailureDropsTheOptimisticChoice() {
        var p = PendingIdChoice().begin("white")
        val a = p.seq
        p = p.begin("grey")
        val b = p.seq
        p = p.finish(a, ok = false)
        assertEquals("grey", p.value)
        p = p.finish(b, ok = false)
        assertNull(p.value)
        assertEquals(0, p.inFlight)
    }

    @Test
    fun maskErrorIsShownOnlyOnItsVersion() {
        assertEquals("boom", maskErrorFor("boom", "e1", "e1"))
        assertNull(maskErrorFor("boom", "e1", "original"))
        assertNull(maskErrorFor("boom", null, "original"))
        assertNull(maskErrorFor("", "e1", "e1"))
        assertNull(maskErrorFor(null, "e1", "e1"))
    }
}
