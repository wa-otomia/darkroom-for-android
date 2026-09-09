package app.darkroom.android.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class PanSnapGuidesTest {
    private fun guide(guides: List<SnapGuide>, id: String): Float =
        guides.first { it.id == id }.value

    @Test
    fun coverCaseUsesDispMinusFrame() {
        val frameW = 200f
        val frameH = 300f
        val imageW = 4000
        val imageH = 3000
        val zoom = 1f
        val inset = 10f
        val cover = coverScale(frameW, frameH, imageW, imageH)
        val dispW = imageW * cover * zoom
        val dispH = imageH * cover * zoom
        val (aabbW, aabbH) = rotatedAabbSize(dispW, dispH, 0f)
        assertTrue(aabbW >= frameW)
        assertTrue(aabbH >= frameH - 0.01f)

        val (xs, ys) = panSnapGuides(frameW, frameH, imageW, imageH, zoom, 0f, inset)
        assertEquals(0f, guide(xs, GUIDE_CENTER_X), 0.01f)
        assertEquals(0f, guide(ys, GUIDE_CENTER_Y), 0.01f)

        val edgeX = (aabbW - frameW) / 2f
        assertEquals(edgeX, guide(xs, GUIDE_FRAME_X_POS), 0.05f)
        assertEquals(-edgeX, guide(xs, GUIDE_FRAME_X_NEG), 0.05f)
        assertEquals(edgeX - inset, guide(xs, GUIDE_SAFE_X_POS), 0.05f)
        assertEquals(-(edgeX - inset), guide(xs, GUIDE_SAFE_X_NEG), 0.05f)

        val edgeY = (aabbH - frameH) / 2f
        assertEquals(edgeY, guide(ys, GUIDE_FRAME_Y_POS), 0.05f)
        assertEquals(-edgeY, guide(ys, GUIDE_FRAME_Y_NEG), 0.05f)
        assertEquals(edgeY - inset, guide(ys, GUIDE_SAFE_Y_POS), 0.05f)
        assertEquals(-(edgeY - inset), guide(ys, GUIDE_SAFE_Y_NEG), 0.05f)
    }

    @Test
    fun letterboxCaseUsesFrameMinusDisp() {
        val frameW = 200f
        val frameH = 300f
        val imageW = 4000
        val imageH = 3000
        val zoom = 0.4f
        val inset = 10f
        val cover = coverScale(frameW, frameH, imageW, imageH)
        val dispW = imageW * cover * zoom
        val dispH = imageH * cover * zoom
        val (aabbW, aabbH) = rotatedAabbSize(dispW, dispH, 0f)
        assertTrue(aabbW < frameW)
        assertTrue(aabbH < frameH)

        val (xs, ys) = panSnapGuides(frameW, frameH, imageW, imageH, zoom, 0f, inset)
        val edgeX = (frameW - aabbW) / 2f
        val edgeY = (frameH - aabbH) / 2f
        assertEquals(edgeX, guide(xs, GUIDE_FRAME_X_POS), 0.05f)
        assertEquals(-edgeX, guide(xs, GUIDE_FRAME_X_NEG), 0.05f)
        assertEquals(edgeX - inset, guide(xs, GUIDE_SAFE_X_POS), 0.05f)
        assertEquals(-(edgeX - inset), guide(xs, GUIDE_SAFE_X_NEG), 0.05f)
        assertEquals(edgeY, guide(ys, GUIDE_FRAME_Y_POS), 0.05f)
        assertEquals(-edgeY, guide(ys, GUIDE_FRAME_Y_NEG), 0.05f)
    }

    @Test
    fun rotationUsesRotatedAabb() {
        val frameW = 200f
        val frameH = 300f
        val imageW = 4000
        val imageH = 3000
        val inset = safeInset(frameW, frameH)
        assertEquals(SAFE_INSET_FRACTION * 200f, inset, 0.01f)

        val (xs0, _) = panSnapGuides(frameW, frameH, imageW, imageH, 1f, 0f, inset)
        val (xs90, _) = panSnapGuides(frameW, frameH, imageW, imageH, 1f, 90f, inset)
        val cover = coverScale(frameW, frameH, imageW, imageH)
        val (aabbW0, _) = rotatedAabbSize(imageW * cover, imageH * cover, 0f)
        val (aabbW90, _) = rotatedAabbSize(imageW * cover, imageH * cover, 90f)
        assertTrue(abs(aabbW0 - aabbW90) > 1f)
        assertEquals((aabbW0 - frameW) / 2f, guide(xs0, GUIDE_FRAME_X_POS), 0.05f)
        assertEquals((aabbW90 - frameW) / 2f, guide(xs90, GUIDE_FRAME_X_POS), 0.05f)
    }

    @Test
    fun rotationSnapGuidesAreRightAngles() {
        val guides = rotationSnapGuides()
        assertEquals(listOf(0f, 90f, 180f, 270f), guides.map { it.value })
        assertEquals(GUIDE_ROT_0, guides[0].id)
    }

    @Test
    fun constantsMatchThePlan() {
        assertEquals(8f, SNAP_POSITION_DP, 0f)
        assertEquals(3f, SNAP_ROTATION_DEG, 0f)
        assertEquals(0.05f, SAFE_INSET_FRACTION, 0f)
    }
}
