package io.github.wa_otomia.darkroom.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.math.abs

class PrintCropTest {
    @Test
    fun orientedDimensionsSwapsForExif5to8() {
        assertEquals(OrientedSize(3024, 4032), orientedDimensions(4032, 3024, 6))
        assertEquals(OrientedSize(4032, 3024), orientedDimensions(4032, 3024, 1))
        assertEquals(OrientedSize(4032, 3024), orientedDimensions(4032, 3024, 3))
    }

    @Test
    fun centeredAspectCropLandscape() {
        val crop = centeredAspectCrop(4000, 3000, PRINT_ASPECT)
        assertEquals(0, crop.y)
        assertEquals(3000, crop.height)
        assertEquals(2000, crop.width)
        assertEquals(1000, crop.x)
        assertTrue(abs(crop.width.toDouble() / crop.height - PRINT_ASPECT) < 0.001)
    }

    @Test
    fun centeredAspectCropPortrait() {
        val crop = centeredAspectCrop(3000, 4000, PRINT_ASPECT)
        assertEquals(0, crop.y)
        assertEquals(4000, crop.height)
        assertEquals(2667, crop.width)
        assertEquals(Math.round((3000 - 2667) / 2.0).toInt(), crop.x)
        assertTrue(abs(crop.width.toDouble() / crop.height - PRINT_ASPECT) < 0.001)
    }

    @Test
    fun centeredAspectCropExact() {
        assertEquals(PixelCrop(0, 0, 1040, 1560), centeredAspectCrop(1040, 1560, PRINT_ASPECT))
    }

    @Test
    fun parsePrintFitValues() {
        assertEquals(PrintFit.CONTAIN, parsePrintFit("contain"))
        assertEquals(PrintFit.COVER, parsePrintFit("cover"))
        assertEquals(PrintFit.COVER, parsePrintFit(null))
        assertEquals(PrintFit.CONTAIN, parsePrintFit("nope", PrintFit.CONTAIN))
    }

    @Test
    fun ingestibleNames() {
        assertTrue(isIngestibleUploadName("DSC0001.JPG"))
        assertTrue(isJpegBytes(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())))
        assertTrue(!isIngestibleUploadName(".hidden.jpg"))
        assertTrue(!isIngestibleUploadName("foo.tmp"))
        assertTrue(!isIngestibleUploadName("note.txt"))
    }

    @Test
    fun viewportCropZoom1MatchesCenteredLandscape() {
        val crop = viewportCrop(200f, 300f, 4000, 3000, 1f, 0f, 0f)
        assertEquals(centeredAspectCrop(4000, 3000), crop)
        assertTrue(abs(crop.width.toDouble() / crop.height - PRINT_ASPECT) < 0.001)
    }

    @Test
    fun viewportCropZoom1MatchesCenteredPortrait() {
        val crop = viewportCrop(200f, 300f, 3000, 4000, 1f, 0f, 0f)
        assertEquals(centeredAspectCrop(3000, 4000), crop)
        assertTrue(abs(crop.width.toDouble() / crop.height - PRINT_ASPECT) < 0.001)
    }

    @Test
    fun viewportCropExactPrintSize() {
        assertEquals(PixelCrop(0, 0, 1040, 1560), viewportCrop(208f, 312f, 1040, 1560, 1f, 0f, 0f))
    }

    @Test
    fun viewportCropZoomKeepsPrintAspect() {
        val crop = viewportCrop(200f, 300f, 4000, 3000, 2f, 50f, 0f)
        assertTrue(abs(crop.width.toDouble() / crop.height - PRINT_ASPECT) < 0.02)
        assertTrue(crop.x >= 0)
        assertTrue(crop.y >= 0)
        assertTrue(crop.x + crop.width <= 4000)
        assertTrue(crop.y + crop.height <= 3000)
    }

    @Test
    fun clampPanKeepsImageCoveringFrame() {
        val (x, y) = clampPan(10_000f, 10_000f, 1f, 200f, 300f, 4000, 3000)
        assertEquals(100f, x, 0.01f)
        assertEquals(0f, y, 0.01f)
        val (zx, zy) = clampPan(-10_000f, -10_000f, 2f, 200f, 300f, 4000, 3000)
        assertEquals(-300f, zx, 0.01f)
        assertEquals(-150f, zy, 0.01f)
    }

    @Test
    fun largestFrameFitsContainer() {
        val (wideW, wideH) = largestFrameSize(360f, 500f)
        assertEquals(500f * PRINT_ASPECT.toFloat(), wideW, 0.01f)
        assertEquals(500f, wideH, 0.01f)
        val (tallW, tallH) = largestFrameSize(200f, 400f)
        assertEquals(200f, tallW, 0.01f)
        assertEquals(200f / PRINT_ASPECT.toFloat(), tallH, 0.01f)
    }

    @Test
    fun viewportCropPanExtremaAlignToEdges() {
        val frameW = 200f
        val frameH = 300f
        val imageW = 4000
        val imageH = 3000
        val cover = coverScale(frameW, frameH, imageW, imageH)
        val maxX = ((imageW * cover - frameW) / 2f).coerceAtLeast(0f)

        val left = viewportCrop(frameW, frameH, imageW, imageH, 1f, maxX, 0f)
        assertEquals(0, left.x)
        assertTrue(left.y >= 0)
        assertTrue(left.x + left.width <= imageW)

        val right = viewportCrop(frameW, frameH, imageW, imageH, 1f, -maxX, 0f)
        assertEquals(imageW, right.x + right.width)
        assertTrue(right.x >= 0)
        assertTrue(right.y + right.height <= imageH)

        val zoom = 2f
        val maxXz = ((imageW * cover * zoom - frameW) / 2f).coerceAtLeast(0f)
        val zoomLeft = viewportCrop(frameW, frameH, imageW, imageH, zoom, maxXz, 0f)
        assertEquals(0, zoomLeft.x)
        val zoomRight = viewportCrop(frameW, frameH, imageW, imageH, zoom, -maxXz, 0f)
        assertEquals(imageW, zoomRight.x + zoomRight.width)
    }

    @Test
    fun normalizeQuarterTurnsWrapsBothWays() {
        assertEquals(0, normalizeQuarterTurns(0))
        assertEquals(1, normalizeQuarterTurns(5))
        assertEquals(3, normalizeQuarterTurns(-1))
        assertEquals(2, normalizeQuarterTurns(-6))
    }

    @Test
    fun rotatedSizeSwapsOnOddTurns() {
        assertEquals(OrientedSize(4000, 3000), rotatedSize(4000, 3000, 0))
        assertEquals(OrientedSize(3000, 4000), rotatedSize(4000, 3000, 1))
        assertEquals(OrientedSize(4000, 3000), rotatedSize(4000, 3000, 2))
        assertEquals(OrientedSize(3000, 4000), rotatedSize(4000, 3000, -1))
    }

    @Test
    fun landscapeAspectIsTheReciprocalOfTheSheet() {
        assertEquals(1.0, PRINT_ASPECT * PRINT_ASPECT_LANDSCAPE, 1e-12)
        assertEquals(PRINT_ASPECT, cropAspect(false), 1e-12)
        assertEquals(PRINT_ASPECT_LANDSCAPE, cropAspect(true), 1e-12)
        val crop = centeredAspectCrop(4000, 3000, PRINT_ASPECT_LANDSCAPE)
        assertTrue(abs(crop.width.toDouble() / crop.height - PRINT_ASPECT_LANDSCAPE) < 0.001)
        assertTrue(crop.x >= 0 && crop.y >= 0)
        assertTrue(crop.x + crop.width <= 4000)
        assertTrue(crop.y + crop.height <= 3000)
    }

    @Test
    fun rotateCropMapsCornersClockwise() {
        val topLeft = PixelCrop(0, 0, 100, 50)
        assertEquals(PixelCrop(2950, 0, 50, 100), rotateCrop(topLeft, 4000, 3000, 1))
        assertEquals(PixelCrop(3900, 2950, 100, 50), rotateCrop(topLeft, 4000, 3000, 2))
        assertEquals(PixelCrop(0, 3900, 50, 100), rotateCrop(topLeft, 4000, 3000, 3))
        assertEquals(topLeft, rotateCrop(topLeft, 4000, 3000, 0))
    }

    @Test
    fun rotateCropStaysInsideTheTurnedImage() {
        val crop = PixelCrop(1200, 400, 1500, 1000)
        for (turns in 0..3) {
            val size = rotatedSize(4000, 3000, turns)
            val turned = rotateCrop(crop, 4000, 3000, turns)
            assertTrue(turned.x >= 0)
            assertTrue(turned.y >= 0)
            assertTrue(turned.x + turned.width <= size.width)
            assertTrue(turned.y + turned.height <= size.height)
        }
    }

    @Test
    fun rotateCropRoundTripsThroughEveryTurn() {
        val crop = PixelCrop(137, 42, 900, 601)
        for (turns in -4..4) {
            val turned = rotateCrop(crop, 4000, 3000, turns)
            val size = rotatedSize(4000, 3000, turns)
            assertEquals(crop, rotateCrop(turned, size.width, size.height, -turns))
            assertEquals(crop, cropInSourceSpace(turned, 4000, 3000, turns))
        }
    }

    @Test
    fun landscapeFrameCropTurnsIntoASheetShapedCrop() {
        val turns = 1
        val sourceW = 4000
        val sourceH = 3000
        val working = rotatedSize(sourceW, sourceH, turns)
        val frame = largestFrameSize(1000f, 1000f, PRINT_ASPECT_LANDSCAPE.toFloat())
        val framed = viewportCrop(frame.first, frame.second, working.width, working.height, 1f, 0f, 0f)
        assertTrue(abs(framed.width.toDouble() / framed.height - PRINT_ASPECT_LANDSCAPE) < 0.01)

        val onSheet = rotateCrop(framed, working.width, working.height, PRINT_SHEET_QUARTER_TURNS)
        assertTrue(abs(onSheet.width.toDouble() / onSheet.height - PRINT_ASPECT) < 0.01)
        assertEquals(framed.width, onSheet.height)
        assertEquals(framed.height, onSheet.width)

        val onSource = cropInSourceSpace(framed, sourceW, sourceH, turns)
        assertTrue(onSource.x >= 0)
        assertTrue(onSource.y >= 0)
        assertTrue(onSource.x + onSource.width <= sourceW)
        assertTrue(onSource.y + onSource.height <= sourceH)
        assertEquals(framed.width, onSource.height)
        assertEquals(framed.height, onSource.width)
    }

    @Test
    fun exifAndUserTurnsComposeWithoutDoubling() {
        val storedW = 4032
        val storedH = 3024
        val exif = orientedDimensions(storedW, storedH, 6)
        assertEquals(OrientedSize(3024, 4032), exif)

        val working = rotatedSize(exif.width, exif.height, 1)
        assertEquals(OrientedSize(4032, 3024), working)

        val framed = viewportCrop(300f, 200f, working.width, working.height, 1f, 0f, 0f)
        val onExifImage = cropInSourceSpace(framed, exif.width, exif.height, 1)
        assertTrue(onExifImage.x + onExifImage.width <= exif.width)
        assertTrue(onExifImage.y + onExifImage.height <= exif.height)
        assertEquals(framed, rotateCrop(onExifImage, exif.width, exif.height, 1))
    }

    @Test
    fun pipelineMapsAFramedCropBackOntoDecodedPixels() {
        val bitmapW = 4000
        val bitmapH = 3000
        val turns = 1
        val framed = rotatedSize(bitmapW, bitmapH, turns)
        val uiCrop = viewportCrop(300f, 200f, framed.width, framed.height, 1f, 0f, 0f)

        val resolved = resolveCropOnBitmap(uiCrop, framed.width, framed.height, framed.width, framed.height)!!
        val inFrame = clampCrop(resolved, framed.width, framed.height)
        val inSource = clampCrop(cropInSourceSpace(inFrame, bitmapW, bitmapH, turns), bitmapW, bitmapH)

        assertTrue(inSource.x >= 0)
        assertTrue(inSource.y >= 0)
        assertTrue(inSource.x + inSource.width <= bitmapW)
        assertTrue(inSource.y + inSource.height <= bitmapH)
        assertEquals(inFrame, rotateCrop(inSource, bitmapW, bitmapH, turns))

        val onSheet = rotateCrop(inFrame, framed.width, framed.height, PRINT_SHEET_QUARTER_TURNS)
        assertTrue(abs(onSheet.width.toDouble() / onSheet.height - PRINT_ASPECT) < 0.02)
    }

    @Test
    fun pipelineSurvivesADownscaledDecode() {
        val uiW = 4000
        val uiH = 3000
        val bitmapW = 1024
        val bitmapH = 768
        val turns = 3
        val uiFramed = rotatedSize(uiW, uiH, turns)
        val uiCrop = viewportCrop(300f, 200f, uiFramed.width, uiFramed.height, 1f, 0f, 0f)

        val bitmapFramed = rotatedSize(bitmapW, bitmapH, turns)
        val resolved = resolveCropOnBitmap(
            uiCrop,
            uiFramed.width,
            uiFramed.height,
            bitmapFramed.width,
            bitmapFramed.height,
        )!!
        val inFrame = clampCrop(resolved, bitmapFramed.width, bitmapFramed.height)
        val inSource = clampCrop(cropInSourceSpace(inFrame, bitmapW, bitmapH, turns), bitmapW, bitmapH)

        assertTrue(inSource.width > 100)
        assertTrue(inSource.height > 100)
        assertTrue(inSource.x + inSource.width <= bitmapW)
        assertTrue(inSource.y + inSource.height <= bitmapH)
        val cutAspect = inFrame.width.toDouble() / inFrame.height
        assertTrue(abs(cutAspect - uiCrop.width.toDouble() / uiCrop.height) < 0.05)
    }

    @Test
    fun rotatePanFollowsTheImageAndReturnsAfterFourTurns() {
        assertEquals(-20f to 10f, rotatePan(10f, 20f, 1))
        assertEquals(-10f to -20f, rotatePan(10f, 20f, 2))
        assertEquals(20f to -10f, rotatePan(10f, 20f, 3))
        var pan = 13f to -7f
        repeat(4) { pan = rotatePan(pan.first, pan.second, 1) }
        assertEquals(13f to -7f, pan)
    }

    @Test
    fun cropSpecRoundTrips() {
        val crop = PixelCrop(12, 34, 560, 780)
        assertEquals(crop, parseCropSpec(formatCropSpec(crop)))
        assertEquals("", formatCropSpec(null))
        assertNull(parseCropSpec(""))
        assertNull(parseCropSpec("   "))
        assertNull(parseCropSpec("1,2,3"))
        assertNull(parseCropSpec("1,2,3,4,5"))
        assertNull(parseCropSpec("a,2,3,4"))
        assertNull(parseCropSpec("0,0,0,10"))
        assertNull(parseCropSpec("0,0,10,0"))
    }

    @Test
    fun normalizedCropOnSmallerBitmapIsNotASliver() {
        val crop = viewportCrop(200f, 300f, 4000, 3000, 1f, 0f, 0f)
        val naive = clampCrop(crop, 1024, 768)
        assertTrue(naive.width < 100)

        val scaled = resolveCropOnBitmap(crop, 4000, 3000, 1024, 768)!!
        assertTrue(scaled.width > 100)
        assertTrue(scaled.height > 100)
        assertTrue(abs(scaled.width.toDouble() / scaled.height - PRINT_ASPECT) < 0.02)
        assertTrue(scaled.x >= 0)
        assertTrue(scaled.y >= 0)
        assertTrue(scaled.x + scaled.width <= 1024)
        assertTrue(scaled.y + scaled.height <= 768)
        assertTrue(scaled.width * 4 > 1024)
    }

    @Test
    fun hasJpegEoiLooksInTheLast4Kb() {
        val soi = 0xFF.toByte()
        val eoi = 0xD9.toByte()
        assertFalse(hasJpegEoi(byteArrayOf()))
        assertFalse(hasJpegEoi(byteArrayOf(soi, 0xD8.toByte(), soi)))
        assertTrue(hasJpegEoi(byteArrayOf(soi, 0xD8.toByte(), soi, eoi)))
        val trailing = ByteArray(100) { 0x00 }
        val withTail = byteArrayOf(soi, 0xD8.toByte(), soi, eoi) + trailing
        assertTrue(hasJpegEoi(withTail))
        val farEoi = byteArrayOf(soi, eoi) + ByteArray(JPEG_EOI_TAIL_BYTES + 8) { 0x11 }
        assertFalse(hasJpegEoi(farEoi))
        val eoiAtTailStart = byteArrayOf(soi, eoi) + ByteArray(JPEG_EOI_TAIL_BYTES - 2) { 0x11 }
        assertTrue(hasJpegEoi(eoiAtTailStart))
    }

    @Test
    fun looksLikeJpegRequiresSoiAndMarker() {
        val soi = 0xFF.toByte()
        val marker = 0xD8.toByte()
        assertTrue(looksLikeJpeg(byteArrayOf(soi, marker, 0xFF.toByte())))
        assertFalse(looksLikeJpeg(byteArrayOf(soi, marker)))
        assertFalse(looksLikeJpeg(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)))
        assertFalse(
            looksLikeJpeg(
                byteArrayOf(
                    0x52, 0x49, 0x46, 0x46,
                    0, 0, 0, 0,
                    0x57, 0x45, 0x42, 0x50,
                ),
            ),
        )
        assertTrue(isJpegBytes(byteArrayOf(soi, marker)))
    }

    @Test
    fun ensureJpegBytesPassesJpegThrough() {
        val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte())
        assertSame(jpeg, ensureJpegBytes(jpeg) { error("must not transcode") })
    }

    @Test
    fun ensureJpegBytesTranscodesNonJpegToJpegMagic() {
        val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte())
        val out = ensureJpegBytes(png) { raw ->
            assertTrue(raw.contentEquals(png))
            jpeg
        }
        assertSame(jpeg, out)
        assertTrue(looksLikeJpeg(out))
        assertEquals(0xFF.toByte(), out[0])
        assertEquals(0xD8.toByte(), out[1])
        assertEquals(0xFF.toByte(), out[2])
    }

    @Test
    fun ensureJpegBytesRejectsFailedTranscode() {
        try {
            ensureJpegBytes(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)) { byteArrayOf(1, 2, 3) }
            fail("expected decode error")
        } catch (e: IllegalStateException) {
            assertTrue(e.message.orEmpty().contains("无法解码 JPEG"))
        }
    }

    @Test
    fun clampZoomUsesSharedLetterboxRange() {
        assertEquals(0.4f, MIN_PRINT_ZOOM, 0f)
        assertEquals(3f, MAX_PRINT_ZOOM, 0f)
        assertEquals(MIN_PRINT_ZOOM, clampZoom(0.1f), 0f)
        assertEquals(MAX_PRINT_ZOOM, clampZoom(9f), 0f)
        assertEquals(1f, clampZoom(1f), 0f)
    }

    @Test
    fun clampPanAllowsTravelInsideLetterbox() {
        val (x, y) = clampPan(10_000f, 10_000f, 0.5f, 200f, 300f, 4000, 3000)
        assertEquals(0f, x, 0.01f)
        assertEquals(75f, y, 0.01f)
        val (insideX, insideY) = clampPan(0f, 40f, 0.5f, 200f, 300f, 4000, 3000)
        assertEquals(0f, insideX, 0.01f)
        assertEquals(40f, insideY, 0.01f)
    }

    @Test
    fun clampPanUsesRotatedAabbForArbitraryAngle() {
        val displayedW = 4000 * coverScale(200f, 300f, 4000, 3000)
        val displayedH = 3000 * coverScale(200f, 300f, 4000, 3000)
        val (aabbW, aabbH) = rotatedAabbSize(displayedW, displayedH, 45f)
        val (x, y) = clampPan(10_000f, 10_000f, 1f, 200f, 300f, 4000, 3000, 45f)
        assertEquals((aabbW - 200f) / 2f, x, 0.05f)
        assertEquals((aabbH - 300f) / 2f, y, 0.05f)
    }

    @Test
    fun viewportPlacementLetterboxIsNotACoverCrop() {
        val placed = viewportPlacement(200f, 300f, 4000, 3000, 0.4f, 0f, 50f)
        assertEquals(0.4f, placed.zoom, 0.001f)
        assertEquals(0f, placed.offsetX, 0.01f)
        assertEquals(50f, placed.offsetY, 0.01f)
        assertTrue(placed.usesLetterbox())
        assertNull(placed.toPixelCrop())
        val cover = viewportCrop(200f, 300f, 4000, 3000, 0.4f, 0f, 0f)
        assertEquals(viewportCrop(200f, 300f, 4000, 3000, 1f, 0f, 0f), cover)
    }

    @Test
    fun viewportPlacementArbitraryAngleStaysOnTheSheet() {
        val placed = viewportPlacement(200f, 300f, 4000, 3000, 1f, 0f, 0f, 33f)
        assertEquals(33f, placed.rotationDegrees, 0.01f)
        assertTrue(placed.usesLetterbox())
        assertNull(placed.toPixelCrop())
        assertNull(exactQuarterTurns(33f))
    }

    @Test
    fun viewportPlacementCoverAtZeroStillMatchesViewportCrop() {
        val placed = viewportPlacement(200f, 300f, 4000, 3000, 1f, 0f, 0f)
        assertFalse(placed.usesLetterbox())
        assertEquals(viewportCrop(200f, 300f, 4000, 3000, 1f, 0f, 0f), placed.toPixelCrop())
    }

    @Test
    fun ninetyDegreeButtonStacksOnGestureAngle() {
        val gesture = 37f
        assertEquals(127f, normalizeRotationDegrees(gesture + 90f), 0.01f)
        assertEquals(307f, normalizeRotationDegrees(gesture - 90f), 0.01f)
        assertEquals(37f, normalizeRotationDegrees(gesture + 90f - 90f), 0.01f)
        assertEquals(1, exactQuarterTurns(45f + 45f))
        assertNull(exactQuarterTurns(gesture + 90f))
        val stacked = viewportPlacement(200f, 300f, 4000, 3000, 1.2f, 8f, -4f, gesture + 90f)
        assertEquals(127f, stacked.rotationDegrees, 0.01f)
        assertTrue(stacked.usesLetterbox())
        val (px, py) = rotatePan(10f, 20f, gesture)
        val (qx, qy) = rotatePan(px, py, 90f)
        val (rx, ry) = rotatePan(10f, 20f, gesture + 90f)
        assertEquals(qx, rx, 0.02f)
        assertEquals(qy, ry, 0.02f)
        assertEquals(-20f to 10f, rotatePan(10f, 20f, 90f))
    }
}
