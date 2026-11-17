package io.github.wa_otomia.darkroom.ui.studio

import io.github.wa_otomia.darkroom.core.ID_MASK_MSG_LOCAL_FAILED
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IdPhotoUiStateTest {
    private val navy10 = CutoutKey(
        file = "/lib/p/original.jpg",
        maskRev = "rev1",
        argb = 0xFF1F3A68.toInt(),
        feather = "weak",
        featherInward = true,
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

    @Test
    fun transitionFreezesOnLookChanges() {
        assertEquals(CutoutTransition.FREEZE, cutoutTransition(navy10, navy10.copy(argb = -1)))
        assertEquals(CutoutTransition.FREEZE, cutoutTransition(navy10, navy10.copy(feather = "strong")))
        assertEquals(CutoutTransition.FREEZE, cutoutTransition(navy10, navy10.copy(featherInward = false)))
        assertEquals(CutoutTransition.FREEZE, cutoutTransition(navy10, navy10.copy(maskRev = "rev2")))
        // A look change together with a zoom step still freezes.
        assertEquals(CutoutTransition.FREEZE, cutoutTransition(navy10, navy10.copy(argb = -1, zoomStep = 12)))
        // Nothing on screen yet (first open, fresh cut-out): freeze on the source.
        assertEquals(CutoutTransition.FREEZE, cutoutTransition(null, navy10))
    }

    @Test
    fun transitionKeepsOldCutoutOnZoomOnly() {
        assertEquals(CutoutTransition.ZOOM_ONLY, cutoutTransition(navy10, navy10.copy(zoomStep = 13)))
        assertEquals(CutoutTransition.ZOOM_ONLY, cutoutTransition(navy10, navy10.copy(cellWidth = 717, cellHeight = 921)))
        assertEquals(CutoutTransition.NONE, cutoutTransition(navy10, navy10))
    }

    @Test
    fun transitionToOriginalIsImmediate() {
        assertEquals(CutoutTransition.ORIGINAL, cutoutTransition(navy10, null))
        assertEquals(CutoutTransition.ORIGINAL, cutoutTransition(null, null))
        assertEquals(CutoutView(showHeld = false, frozen = false), cutoutView(navy10, null, null, null))
    }

    @Test
    fun colourChangeKeepsHeldCutoutAndFreezes() {
        val red = navy10.copy(argb = 0xFFFF0000.toInt())
        assertEquals(CutoutView(showHeld = true, frozen = true), cutoutView(navy10, red, null, null))
        // Landed: the new one is held, nothing frozen.
        assertEquals(CutoutView(showHeld = true, frozen = false), cutoutView(red, red, null, null))
    }

    @Test
    fun firstCompositeFreezesOnTheSource() {
        assertEquals(CutoutView(showHeld = false, frozen = true), cutoutView(null, navy10, null, null))
    }

    @Test
    fun unknownSourceSizeNeverFreezes() {
        // No render can start without the source size (missing file, unreadable header, legacy 0x0
        // row), so the source is shown and the controls stay live instead of freezing for good.
        assertEquals(CutoutView(showHeld = false, frozen = false), cutoutView(null, navy10, null, null, sizeKnown = false))
        val red = navy10.copy(argb = 0xFFFF0000.toInt())
        assertEquals(CutoutView(showHeld = false, frozen = false), cutoutView(navy10, red, null, null, sizeKnown = false))
        // A held cut-out of the same key, or a zoom-only step, is still shown.
        assertEquals(CutoutView(showHeld = true, frozen = false), cutoutView(navy10, navy10, null, null, sizeKnown = false))
        assertEquals(CutoutView(showHeld = true, frozen = false), cutoutView(navy10, navy10.copy(zoomStep = 14), null, null, sizeKnown = false))
    }

    @Test
    fun zoomOnlyNeverFreezes() {
        val view = cutoutView(navy10, navy10.copy(zoomStep = 14), null, null)
        assertTrue(view.showHeld)
        assertFalse(view.frozen)
    }

    @Test
    fun heldCutoutOfAnotherFileIsNeverShown() {
        val other = navy10.copy(file = "/lib/p/edit1.jpg")
        assertEquals(CutoutView(showHeld = false, frozen = true), cutoutView(navy10, other, null, null))
    }

    @Test
    fun missingMaskUnfreezesAndShowsTheSource() {
        val white = navy10.copy(argb = -1)
        assertEquals(CutoutView(showHeld = false, frozen = false), cutoutView(null, white, failedMask = white, gaveUp = null))
        // Remembered by mask revision: a feather change after the failure does not freeze again.
        assertFalse(cutoutView(null, white.copy(feather = "strong"), failedMask = white, gaveUp = null).frozen)
        // A new revision is tried again, frozen.
        assertTrue(cutoutView(null, white.copy(maskRev = "rev2"), failedMask = white, gaveUp = null).frozen)
    }

    @Test
    fun exhaustedRetriesUnfreezeWithoutShowingTheOldColour() {
        val white = navy10.copy(argb = -1)
        assertEquals(CutoutView(showHeld = false, frozen = false), cutoutView(navy10, white, null, gaveUp = white))
        // Only for that key: the next change freezes again.
        assertTrue(cutoutView(navy10, white.copy(feather = "medium"), null, gaveUp = white).frozen)
    }

    @Test
    fun zoomOnlyChangeAfterGiveUpNeverRefreezes() {
        val white = navy10.copy(argb = -1)
        val pinched = white.copy(zoomStep = white.zoomStep + 1)
        val resized = white.copy(cellWidth = white.cellWidth + 40, cellHeight = white.cellHeight + 60)
        // A pinch or size preset after the give-up keeps the source on screen and the controls live.
        assertEquals(CutoutView(showHeld = false, frozen = false), cutoutView(navy10, pinched, null, gaveUp = white))
        assertEquals(CutoutView(showHeld = false, frozen = false), cutoutView(navy10, resized, null, gaveUp = white))
        assertTrue(gaveUpCovers(white, pinched))
        assertTrue(gaveUpCovers(white, white))
        // Colour, feather, inward feather, mask or file changes are not covered.
        assertFalse(gaveUpCovers(white, navy10))
        assertFalse(gaveUpCovers(white, white.copy(feather = "medium")))
        assertFalse(gaveUpCovers(white, white.copy(featherInward = !white.featherInward)))
        assertFalse(gaveUpCovers(white, white.copy(maskRev = "other")))
        assertFalse(gaveUpCovers(white, white.copy(file = "other.jpg")))
        assertFalse(gaveUpCovers(null, white))
        assertFalse(gaveUpCovers(white, null))
    }

    @Test
    fun cutOutLabelFollowsMaskAndMode() {
        assertEquals(IdCutOutLabel.AI, idCutOutLabel(hasMask = false, local = false))
        assertEquals(IdCutOutLabel.LOCAL, idCutOutLabel(hasMask = false, local = true))
        assertEquals(IdCutOutLabel.RECUT, idCutOutLabel(hasMask = true, local = false))
        assertEquals(IdCutOutLabel.RECUT, idCutOutLabel(hasMask = true, local = true))
    }

    @Test
    fun canCutOutLocalNeedsNoKeyAndAnyJobBlocks() {
        // AI: needs a key.
        assertTrue(idCanCutOut(local = false, keySet = true, busy = false))
        assertFalse(idCanCutOut(local = false, keySet = false, busy = false))
        // On device: no key needed.
        assertTrue(idCanCutOut(local = true, keySet = false, busy = false))
        assertTrue(idCanCutOut(local = true, keySet = true, busy = false))
        // Any job on the photo (hidden ones included) blocks both modes.
        for (local in listOf(false, true)) {
            for (keySet in listOf(false, true)) {
                assertFalse(idCanCutOut(local = local, keySet = keySet, busy = true))
            }
        }
    }

    @Test
    fun needKeyHintOnlyForAiWithoutKey() {
        assertTrue(idShowNeedKey(local = false, keySet = false))
        assertFalse(idShowNeedKey(local = false, keySet = true))
        assertFalse(idShowNeedKey(local = true, keySet = false))
        assertFalse(idShowNeedKey(local = true, keySet = true))
    }

    @Test
    fun localSwitchDisabledWhileBusyOrCompositing() {
        assertTrue(idLocalSwitchEnabled(busy = false, compositing = false))
        assertFalse(idLocalSwitchEnabled(busy = true, compositing = false))
        assertFalse(idLocalSwitchEnabled(busy = false, compositing = true))
        assertFalse(idLocalSwitchEnabled(busy = true, compositing = true))
    }

    @Test
    fun localFailureRecognisedByItsMessage() {
        assertTrue(idFailureIsLocal("本地抠图失败：UnsatisfiedLinkError"))
        assertTrue(idFailureIsLocal(ID_MASK_MSG_LOCAL_FAILED))
        assertFalse(idFailureIsLocal("抠图失败：返回图不是黑白蒙版"))
        assertFalse(idFailureIsLocal(""))
    }
}
