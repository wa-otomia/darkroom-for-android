package io.github.wa_otomia.darkroom.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

/**
 * Studio keeps pan in print-sheet pixels so that the drawer opening or closing (which resizes
 * the on-screen cropper frame) never changes the composition or the printed result.
 */
class StudioPoseUnitsTest {
    // A 1080 px wide phone: drawer open leaves a 613×920 frame, drawer closed a 1080×1620 one.
    private val small = largestFrameSize(1080f, 920f, PRINT_ASPECT.toFloat())
    private val large = largestFrameSize(1080f, 1853f, PRINT_ASPECT.toFloat())
    private val sheet = framingSheetSize(landscape = false)

    @Test
    fun drawerFramesAreDifferentSizes() {
        assertEquals(613.33f, small.first, 0.01f)
        assertEquals(1080f, large.first, 0.01f)
    }

    @Test
    fun samePoseShowsTheSameCompositionInBothFrames() {
        val panX = 180f
        val panY = -95f
        for ((frameW, _) in listOf(small, large)) {
            val unit = panUnitScale(frameW, sheet.first)
            // Fraction of the frame the image is shifted by is the same at every size.
            assertEquals(panX / sheet.first, panX * unit / frameW, 1e-5f)
            assertEquals(panY / sheet.second, panY * unit / (frameW / PRINT_ASPECT.toFloat()), 1e-4f)
        }
    }

    @Test
    fun cropOnScreenMatchesThePrintedCropAtEitherDrawerState() {
        val image = 4000 to 6000
        val zoom = 1.5f
        val placement = studioPlacement(false, image.first, image.second, zoom, 120f, -200f, 0f)
        assertNotNull(placement)
        val printed = studioCrop(placement)!!
        for ((frameW, frameH) in listOf(small, large)) {
            val unit = panUnitScale(frameW, sheet.first)
            val onScreen = viewportCrop(frameW, frameH, image.first, image.second, zoom, 120f * unit, -200f * unit)
            assertEquals(printed.x.toFloat(), onScreen.x.toFloat(), 2f)
            assertEquals(printed.y.toFloat(), onScreen.y.toFloat(), 2f)
            assertEquals(printed.width.toFloat(), onScreen.width.toFloat(), 2f)
            assertEquals(printed.height.toFloat(), onScreen.height.toFloat(), 2f)
        }
    }

    @Test
    fun clampBoundScalesWithTheFrameSoResizingNeverTrimsPan() {
        val (iw, ih) = 4000 to 6000
        for (rotation in listOf(0f, 17f, 90f)) {
            val (sx, sy) = clampPan(10_000f, -10_000f, 1.5f, sheet.first, sheet.second, iw, ih, rotation)
            for ((frameW, frameH) in listOf(small, large)) {
                val unit = panUnitScale(frameW, sheet.first)
                val (px, py) = clampPan(10_000f, -10_000f, 1.5f, frameW, frameH, iw, ih, rotation)
                assertEquals(sx * unit, px, 0.05f)
                assertEquals(sy * unit, py, 0.05f)
            }
        }
    }

    @Test
    fun inRangePanIsReturnedUnchangedSoTheCropperDoesNotWriteBack() {
        val (x, y) = clampPan(120f, -200f, 1.5f, sheet.first, sheet.second, 4000, 6000, 0f)
        assertEquals(120f, x, 0f)
        assertEquals(-200f, y, 0f)
    }

    @Test
    fun gesturePixelsRoundTripThroughSheetUnits() {
        for ((frameW, _) in listOf(small, large)) {
            val unit = panUnitScale(frameW, sheet.first)
            val px = 77.7f
            assertEquals(px, (px / unit) * unit, 1e-3f)
        }
        assertEquals(1f, panUnitScale(0f, 1040f), 0f)
        assertEquals(1f, panUnitScale(500f, 0f), 0f)
    }

    @Test
    fun legacyRowMeasuredOnTheSmallFrameConvertsToTheSameComposition() {
        val legacy = Framing(
            landscape = false,
            zoom = 1.5f,
            offsetX = 50f,
            offsetY = -30f,
            frameWidth = small.first,
            frameHeight = small.second,
            imageWidth = 4000,
            imageHeight = 6000,
        )
        val (x, y) = legacy.sheetPan()
        assertEquals(50f / small.first, x / sheet.first, 1e-5f)
        assertEquals(-30f / small.second, y / sheet.second, 1e-5f)
        // The renderer scales offset by its frame; the printed shift is unchanged by conversion.
        val before = legacy.toPlacement()
        val after = studioPlacement(false, 4000, 6000, legacy.zoom, x, y, 0f)!!
        assertEquals(before.offsetX / before.frameWidth, after.offsetX / after.frameWidth, 1e-5f)
        assertEquals(before.offsetY / before.frameHeight, after.offsetY / after.frameHeight, 1e-5f)
    }

    @Test
    fun reopeningStudioDoesNotDriftThePan() {
        var row = studioPlacement(false, 4000, 6000, 1.5f, 120f, -200f, 0f)!!.toFraming(false)
        repeat(5) {
            val (x, y) = row.sheetPan()
            row = studioPlacement(false, 4000, 6000, row.zoom, x, y, row.rotationDegrees)!!.toFraming(false)
        }
        assertEquals(120f, row.offsetX, 1e-3f)
        assertEquals(-200f, row.offsetY, 1e-3f)
        assertEquals(sheet.first, row.frameWidth, 0f)
        assertEquals(sheet.second, row.frameHeight, 0f)
    }

    @Test
    fun placementUsesTheSheetOfTheOrientationAndWaitsForTheImageSize() {
        assertNull(studioPlacement(false, 0, 6000, 1f, 0f, 0f, 0f))
        val portrait = studioPlacement(false, 4000, 6000, 1f, 0f, 0f, 0f)!!
        assertEquals(PRINT_WIDTH.toFloat(), portrait.frameWidth, 0f)
        assertEquals(PRINT_HEIGHT.toFloat(), portrait.frameHeight, 0f)
        val landscape = studioPlacement(true, 6000, 4000, 1f, 0f, 0f, 0f)!!
        assertEquals(PRINT_HEIGHT.toFloat(), landscape.frameWidth, 0f)
        assertEquals(PRINT_WIDTH.toFloat(), landscape.frameHeight, 0f)
    }

    @Test
    fun placementClampsAnOutOfRangePan() {
        val placement = studioPlacement(false, 4000, 6000, 1f, 500f, 0f, 0f)!!
        assertEquals(0f, placement.offsetX, 0f)
    }

    @Test
    fun rotateButtonsTurnASheetPanWithoutStretchingIt() {
        val (x, y) = rotatePan(100f, 0f, 90f)
        assertEquals(0f, x, 1e-4f)
        assertEquals(100f, y, 1e-4f)
        val (fx, fy) = rotatePan(60f, 80f, 33f)
        assertEquals(100f, hypot(fx, fy), 1e-3f)
    }

    @Test
    fun cropFollowsTheRightAngleCoverCutAndFallsBackForFreeAngles() {
        val square = studioPlacement(false, 4000, 6000, 1f, 0f, 0f, 0f)!!
        assertEquals(square.toPixelCrop(), studioCrop(square))
        val tilted = studioPlacement(false, 4000, 6000, 1.2f, 0f, 0f, 10f)!!
        assertNull(tilted.toPixelCrop())
        assertNotNull(studioCrop(tilted))
        assertNull(studioCrop(null))
    }

    @Test
    fun defaultPoseInSheetUnitsIsStillTreatedAsAutoDefault() {
        val framing = liveFraming(
            landscape = false,
            zoom = 1f,
            offsetX = 0f,
            offsetY = 0f,
            rotationDegrees = 0f,
            placement = studioPlacement(false, 4000, 6000, 1f, 0f, 0f, 0f),
            imageWidth = 4000,
            imageHeight = 6000,
        )
        assertTrue(framing.matchesAutoDefault())
        assertNull(persistableFraming(framing))
    }
}
