package io.github.wa_otomia.darkroom.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IdPhotoPoseTest {
    private val eps = 1e-3f
    private val upright = idPhotoLayout(idPhotoPreset("35x45")) // 2×1, cell 717×921
    private val turned = idPhotoLayout(idPhotoPreset("25x35")) // rotated 2×2, cell 512×717
    private val imageW = 4000
    private val imageH = 3000

    /** Source px under a cell-local point (offset from cell centre, upright cell px). */
    private fun imagePointUnder(
        pose: IdPhotoPose,
        layout: IdPhotoLayout,
        localX: Float,
        localY: Float,
    ): Pair<Float, Float> {
        val cw = layout.cellWidth.toFloat()
        val ch = layout.cellHeight.toFloat()
        val scale = coverScale(cw, ch, imageW, imageH) * pose.zoom
        val (rx, ry) = rotatePan(localX - pose.panX * cw, localY - pose.panY * ch, -pose.rotation)
        return rx / scale to ry / scale
    }

    @Test
    fun initialPoseTakesTheNearestRightAngle() {
        assertEquals(IdPhotoPose(1f, 0f, 0f, 90f), idInitialPose(88f))
        assertEquals(IdPhotoPose(1f, 0f, 0f, 0f), idInitialPose(-44f))
        assertEquals(IdPhotoPose(1f, 0f, 0f, 270f), idInitialPose(-46f))
        assertEquals(IdPhotoPose(1f, 0f, 0f, 0f), idInitialPose(359f))
        assertEquals(IdPhotoPose(), idInitialPose(Float.NaN))
    }

    @Test
    fun cellPlacementConvertsFractionsToCellPixels() {
        val pose = IdPhotoPose(zoom = 1.5f, panX = 0.1f, panY = -0.05f, rotation = 0f)
        val p = pose.toCellPlacement(717, 921, imageW, imageH)
        assertEquals(1.5f, p.zoom, eps)
        assertEquals(71.7f, p.offsetX, eps)
        assertEquals(-46.05f, p.offsetY, eps)
        assertEquals(717f, p.frameWidth, 0f)
        assertEquals(921f, p.frameHeight, 0f)
        assertEquals(imageW, p.imageWidth)
        assertEquals(imageH, p.imageHeight)
        assertEquals(
            viewportPlacement(717f, 921f, imageW, imageH, 1.5f, pose.panX * 717, pose.panY * 921, 0f),
            p,
        )
    }

    @Test
    fun clampedRoundTripsAnInRangePoseAndClampsTheRest() {
        val inRange = IdPhotoPose(1.5f, 0.1f, -0.05f, 0f)
        val same = inRange.clamped(717, 921, imageW, imageH)
        assertEquals(inRange.panX, same.panX, 1e-6f)
        assertEquals(inRange.panY, same.panY, 1e-6f)

        val far = IdPhotoPose(0.1f, 5f, -5f, -90f).clamped(717, 921, imageW, imageH)
        assertEquals(MIN_PRINT_ZOOM, far.zoom, 0f)
        assertEquals(270f, far.rotation, 0f)
        val (mx, my) = clampPan(5f * 717, -5f * 921, MIN_PRINT_ZOOM, 717f, 921f, imageW, imageH, 270f)
        assertEquals(mx / 717f, far.panX, 1e-6f)
        assertEquals(my / 921f, far.panY, 1e-6f)
    }

    @Test
    fun clampedLeavesPanWhileImageSizeIsUnknown() {
        val pose = IdPhotoPose(9f, 0.7f, -0.3f, 450f).clamped(717, 921, 0, 0)
        assertEquals(IdPhotoPose(MAX_PRINT_ZOOM, 0.7f, -0.3f, 90f), pose)
    }

    @Test
    fun rotateButtonTurnsPanInCellPixelsWithoutStretching() {
        // Portrait source in a non-square cell; zoom 2 leaves room to pan both ways.
        val start = IdPhotoPose(zoom = 2f, panX = 0.1f, panY = 0f, rotation = 0f)
        val next = start.rotatedQuarter(1, 717, 921, 3000, 4000)
        assertEquals(90f, next.rotation, 0f)
        assertEquals(0f, next.panX, 1e-6f)
        // 71.7 cell px right becomes 71.7 cell px down, which is 71.7/921 of the height.
        assertEquals(71.7f / 921f, next.panY, 1e-5f)
        assertNotEquals(0.1f, next.panY, 1e-3f)
        assertEquals(2f, next.zoom, 0f)

        val back = next.rotatedQuarter(-1, 717, 921, 3000, 4000)
        assertEquals(0f, back.rotation, 0f)
        assertEquals(0.1f, back.panX, 1e-5f)
        assertEquals(0f, back.panY, 1e-5f)
    }

    @Test
    fun rotateButtonLandsOnARightAngleAfterATwist() {
        val twisted = IdPhotoPose(zoom = 1f, rotation = 88f)
        assertEquals(180f, twisted.rotatedQuarter(1, 717, 921, imageW, imageH).rotation, 0f)
        assertEquals(0f, twisted.rotatedQuarter(-1, 717, 921, imageW, imageH).rotation, 0f)
        assertEquals(0f, IdPhotoPose(rotation = 270f).rotatedQuarter(1, 717, 921, imageW, imageH).rotation, 0f)
    }

    @Test
    fun presetChangeKeepsZoomAndRotationAndReclampsPan() {
        val next = idPhotoLayout(idPhotoPreset("33x48"))
        val pose = IdPhotoPose(zoom = 1.5f, panX = 1.2f, panY = 0.02f, rotation = 0f)
        val moved = pose.forPreset(upright, next, imageW, imageH)
        assertEquals(1.5f, moved.zoom, 0f)
        assertEquals(0f, moved.rotation, 0f)
        val (ex, ey) = clampPan(1.2f * 676, 0.02f * 983, 1.5f, 676f, 983f, imageW, imageH, 0f)
        assertEquals(ex / 676f, moved.panX, 1e-6f)
        assertEquals(ey / 983f, moved.panY, 1e-6f)
        assertTrue(moved.panX < 1.2f)

        // A pan that fits the new cell keeps its fraction.
        val small = IdPhotoPose(zoom = 1.5f, panX = 0.05f, panY = 0.01f).forPreset(upright, next, imageW, imageH)
        assertEquals(0.05f, small.panX, 1e-6f)
        assertEquals(0.01f, small.panY, 1e-6f)
    }

    @Test
    fun sheetDeltaMapsIntoRotatedCellsSoImagesFollowTheFinger() {
        assertEquals(30f to 5f, idSheetDeltaToCell(30f, 5f, rotated = false))
        val (cx, cy) = idSheetDeltaToCell(30f, 0f, rotated = true)
        assertEquals(0f, cx, 0f)
        assertEquals(30f, cy, 0f)
        // Cell content turns counter-clockwise into the slot: mapping back gives the finger delta.
        for ((dx, dy) in listOf(30f to 0f, 0f to 12f, -7f to 4f)) {
            val (x, y) = idSheetDeltaToCell(dx, dy, rotated = true)
            val (sx, sy) = rotatePan(x, y, 3)
            assertEquals(dx, sx, 0f)
            assertEquals(dy, sy, 0f)
        }
    }

    @Test
    fun anchorIsRelativeToTheSlotUnderTheCentroid() {
        val right = upright.slots[1]
        val cx = right.left + right.width / 2f
        val cy = right.top + right.height / 2f
        assertEquals(10f to 20f, idCellAnchor(upright, cx + 10f, cy + 20f))
        val left = upright.slots[0]
        assertEquals(
            -5f to 0f,
            idCellAnchor(upright, left.left + left.width / 2f - 5f, left.top + left.height / 2f),
        )
    }

    @Test
    fun anchorOutsideEverySlotUsesTheNearestPointOfTheNearestSlot() {
        val left = upright.slots[0]
        // Left margin: the left slot's left edge at the same height.
        val (ax, ay) = idCellAnchor(upright, 10f, left.top + left.height / 2f + 30f)
        assertEquals(-left.width / 2f, ax, 0f)
        assertEquals(30f, ay, 0f)
        // Above the right slot.
        val right = upright.slots[1]
        val (bx, by) = idCellAnchor(upright, right.left + 100f, 0f)
        assertEquals(100f - right.width / 2f, bx, 0f)
        assertEquals(-right.height / 2f, by, 0f)
    }

    @Test
    fun anchorInARotatedSlotIsInUprightCellAxes() {
        val slot = turned.slots[3]
        val cx = slot.left + slot.width / 2f
        val cy = slot.top + slot.height / 2f
        // Right on the sheet is down in the upright cell.
        val (ax, ay) = idCellAnchor(turned, cx + 100f, cy)
        assertEquals(0f, ax, 0f)
        assertEquals(100f, ay, 0f)
        // Up on the sheet is right in the upright cell.
        val (bx, by) = idCellAnchor(turned, cx, cy - 50f)
        assertEquals(50f, bx, 0f)
        assertEquals(0f, by, 0f)
    }

    @Test
    fun dragPansEveryCellByTheSameVisibleDistance() {
        val start = IdPhotoPose(zoom = 1.5f)
        val gap = (upright.slots[0].right + upright.slots[1].left) / 2f
        val next = applyIdGesture(start, upright, imageW, imageH, gap, 500f, 30f, 0f, 1f, 0f)
        assertEquals(30f / 717f, next.panX, 1e-6f)
        assertEquals(0f, next.panY, 0f)
        assertEquals(1.5f, next.zoom, 0f)
    }

    @Test
    fun dragOnRotatedGridMovesTheImageWithTheFinger() {
        val start = IdPhotoPose(zoom = 1.5f)
        val slot = turned.slots[0]
        val next = applyIdGesture(start, turned, imageW, imageH, slot.left + 10f, slot.top + 10f, 30f, 0f, 1f, 0f)
        // Right on screen = down in the upright cell (717 px tall).
        assertEquals(0f, next.panX, 1e-6f)
        assertEquals(30f / 717f, next.panY, 1e-6f)
        // The upright-cell move, turned back counter-clockwise into the slot, is the finger delta.
        val (sx, sy) = rotatePan(next.panX * turned.cellWidth, next.panY * turned.cellHeight, 3)
        assertEquals(30f, sx, 1e-3f)
        assertEquals(0f, sy, 1e-3f)
    }

    @Test
    fun pinchKeepsTheAnchoredImagePointFixed() {
        val start = IdPhotoPose(zoom = 1.2f, panX = 0.02f, panY = -0.01f)
        val right = upright.slots[1]
        val px = right.left + 300f
        val py = right.top + 250f
        val (lx, ly) = idCellAnchor(upright, px, py)
        val before = imagePointUnder(start, upright, lx, ly)
        val next = applyIdGesture(start, upright, imageW, imageH, px, py, 0f, 0f, 1.25f, 0f)
        assertEquals(1.5f, next.zoom, 1e-6f)
        val after = imagePointUnder(next, upright, lx, ly)
        assertEquals(before.first, after.first, 1e-2f)
        assertEquals(before.second, after.second, 1e-2f)
        // The same point of the left cell shows the same image point too.
        val left = upright.slots[0]
        val (llx, lly) = idCellAnchor(upright, left.left + 300f, left.top + 250f)
        assertEquals(lx, llx, 0f)
        assertEquals(ly, lly, 0f)
    }

    @Test
    fun pinchAndTwistOnRotatedGridKeepTheAnchoredPointFixed() {
        val start = IdPhotoPose(zoom = 2f, panX = 0.03f, panY = 0.01f, rotation = 0f)
        val slot = turned.slots[2]
        val px = slot.left + 200f
        val py = slot.top + 150f
        val (lx, ly) = idCellAnchor(turned, px, py)
        val before = imagePointUnder(start, turned, lx, ly)
        val next = applyIdGesture(start, turned, imageW, imageH, px, py, 0f, 0f, 1.1f, 10f)
        assertEquals(2.2f, next.zoom, 1e-5f)
        assertEquals(10f, next.rotation, 1e-5f)
        val after = imagePointUnder(next, turned, lx, ly)
        assertEquals(before.first, after.first, 1e-2f)
        assertEquals(before.second, after.second, 1e-2f)
    }

    @Test
    fun gestureClampsZoomAndPan() {
        val small = applyIdGesture(IdPhotoPose(), upright, imageW, imageH, 400f, 500f, 0f, 0f, 0.01f, 0f)
        assertEquals(MIN_PRINT_ZOOM, small.zoom, 0f)
        val big = applyIdGesture(IdPhotoPose(), upright, imageW, imageH, 400f, 500f, 5000f, 0f, 1f, 0f)
        val (mx, _) = clampPan(1e9f, 0f, 1f, 717f, 921f, imageW, imageH, 0f)
        assertEquals(mx / 717f, big.panX, 1e-6f)
        val turnedBack = applyIdGesture(IdPhotoPose(rotation = 5f), upright, imageW, imageH, 400f, 500f, 0f, 0f, 1f, -10f)
        assertEquals(355f, turnedBack.rotation, 1e-4f)
    }
}
