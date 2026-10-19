package io.github.wa_otomia.darkroom.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DecodeBudgetTest {
    @Test
    fun a7crPortraitPrintZoom1SubsamplesTo1584x2112() {
        val oriented = a7crOriented()
        assertEquals(OrientedSize(6336, 8448), oriented)

        val sample = coverSampleSize(oriented.width, oriented.height, PRINT_WIDTH, PRINT_HEIGHT, zoom = 1f)
        val decoded = sampledSize(oriented.width, oriented.height, sample)

        assertEquals(4, sample)
        assertEquals(OrientedSize(1584, 2112), decoded)
        assertTrue(decoded.height >= PRINT_HEIGHT)
        assertTrue(decoded.height < PRINT_HEIGHT * 2)
        assertTrue(decoded.width < oriented.width)
        assertTrue(decoded.height < oriented.height)
    }

    @Test
    fun a7crPortraitPrintZoom3KeepsAtLeast4680PxOfHeight() {
        val oriented = a7crOriented()
        val sample = coverSampleSize(oriented.width, oriented.height, PRINT_WIDTH, PRINT_HEIGHT, zoom = 3f)
        val decoded = sampledSize(oriented.width, oriented.height, sample)

        assertEquals(1, sample)
        assertEquals(oriented, decoded)
        assertTrue(decoded.height >= 4680)
    }

    @Test
    fun a7crExport2080x3120AtZoom3UsesFullResolution() {
        val oriented = a7crOriented()
        val export = exportSheetSize(landscape = false, outputScale = 2)
        assertEquals(OrientedSize(2080, 3120), export)

        val sample = coverSampleSize(oriented.width, oriented.height, export.width, export.height, zoom = 3f)
        assertEquals(1, sample)
        assertEquals(oriented, sampledSize(oriented.width, oriented.height, sample))
    }

    @Test
    fun a7crThumbnailDecodes792x1056AndOutputs720x960() {
        val oriented = a7crOriented()
        val sample = widthSampleSize(oriented.width, THUMB_WIDTH)
        val decoded = sampledSize(oriented.width, oriented.height, sample)
        val outW = decoded.width.coerceAtMost(THUMB_WIDTH)
        val outH = decoded.height * outW / decoded.width

        assertEquals(8, sample)
        assertEquals(OrientedSize(792, 1056), decoded)
        assertEquals(720, outW)
        assertEquals(960, outH)
        assertTrue(outW <= THUMB_WIDTH)
    }

    @Test
    fun a7crAiUploadSamplesBy4AndFitsLongEdge1536() {
        val oriented = a7crOriented()
        val sample = longEdgeSampleSize(oriented.width, oriented.height, AI_UPLOAD_MAX_EDGE)
        val decoded = sampledSize(oriented.width, oriented.height, sample)
        val outW = decoded.width * AI_UPLOAD_MAX_EDGE / decoded.height
        val outH = AI_UPLOAD_MAX_EDGE

        assertEquals(4, sample)
        assertEquals(OrientedSize(1584, 2112), decoded)
        assertEquals(1152, outW)
        assertEquals(1536, outH)
        assertTrue(maxOf(outW, outH) <= AI_UPLOAD_MAX_EDGE)
        assertTrue(decoded.height > AI_UPLOAD_MAX_EDGE)
    }

    @Test
    fun a7crPreviewIs3072x4096AndUnder100MiB() {
        val oriented = a7crOriented()
        val preview = previewDecodeSize(oriented.width, oriented.height, PREVIEW_MAX_EDGE)
        val bytes = preview.width.toLong() * preview.height * 4L

        assertEquals(OrientedSize(3072, 4096), preview)
        assertEquals(50_331_648L, bytes)
        assertTrue(bytes < 100L * 1024 * 1024)
        assertTrue(bytes < 64L * 1024 * 1024)
        assertTrue(maxOf(preview.width, preview.height) <= PREVIEW_MAX_EDGE)
        assertTrue(preview.width <= oriented.width && preview.height <= oriented.height)
    }

    @Test
    fun smallImagesAreNotUpscaled() {
        assertEquals(OrientedSize(1600, 1200), previewDecodeSize(1600, 1200, PREVIEW_MAX_EDGE))
        assertEquals(1, widthSampleSize(600, THUMB_WIDTH))
        assertEquals(1, longEdgeSampleSize(600, 400, AI_UPLOAD_MAX_EDGE))
        assertEquals(1, coverSampleSize(1600, 1200, PRINT_WIDTH, PRINT_HEIGHT, zoom = 3f))
        assertEquals(1, containSampleSize(1600, 1200, PRINT_WIDTH, PRINT_HEIGHT))
        assertEquals(OrientedSize(600, 400), sampledSize(600, 400, widthSampleSize(600, THUMB_WIDTH)))
    }

    @Test
    fun recordSizedCropMapsOntoSampledPixelsInsteadOfASliver() {
        val oriented = a7crOriented()
        val sample = coverSampleSize(oriented.width, oriented.height, PRINT_WIDTH, PRINT_HEIGHT, zoom = 1f)
        val decoded = sampledSize(oriented.width, oriented.height, sample)
        assertEquals(OrientedSize(1584, 2112), decoded)

        val record = PixelCrop(4000, 5000, 2000, 3000)
        val naive = clampCrop(record, decoded.width, decoded.height)
        val mapped = resolveCropOnBitmap(
            record,
            oriented.width,
            oriented.height,
            decoded.width,
            decoded.height,
        )!!

        assertEquals(1, naive.width)
        assertEquals(1, naive.height)
        assertEquals(PixelCrop(1000, 1250, 500, 750), mapped)
        assertTrue(mapped.width > 100)
        assertTrue(mapped.height > 100)
        assertTrue(mapped.x + mapped.width <= decoded.width)
        assertTrue(mapped.y + mapped.height <= decoded.height)
        assertEquals(
            PixelCrop(0, 0, decoded.width, decoded.height),
            resolveCropOnBitmap(
                PixelCrop(0, 0, oriented.width, oriented.height),
                oriented.width,
                oriented.height,
                decoded.width,
                decoded.height,
            ),
        )
    }

    @Test
    fun exifTransformsMatchDecodeRotatedCornersAndOutputSize() {
        val width = 10
        val height = 6
        val cases = listOf(
            ExifCase(1, width, height, 0f to 0f, width.toFloat() to 0f, 0f to height.toFloat(), width.toFloat() to height.toFloat()),
            ExifCase(2, width, height, width.toFloat() to 0f, 0f to 0f, width.toFloat() to height.toFloat(), 0f to height.toFloat()),
            ExifCase(3, width, height, width.toFloat() to height.toFloat(), 0f to height.toFloat(), width.toFloat() to 0f, 0f to 0f),
            ExifCase(4, width, height, 0f to height.toFloat(), width.toFloat() to height.toFloat(), 0f to 0f, width.toFloat() to 0f),
            ExifCase(5, height, width, height.toFloat() to width.toFloat(), height.toFloat() to 0f, 0f to width.toFloat(), 0f to 0f),
            ExifCase(6, height, width, height.toFloat() to 0f, height.toFloat() to width.toFloat(), 0f to 0f, 0f to width.toFloat()),
            ExifCase(7, height, width, 0f to 0f, 0f to width.toFloat(), height.toFloat() to 0f, height.toFloat() to width.toFloat()),
            ExifCase(8, height, width, 0f to width.toFloat(), 0f to 0f, height.toFloat() to width.toFloat(), height.toFloat() to 0f),
        )
        for (expected in cases) {
            val transform = exifOrientationTransform(expected.orientation, width, height)
            assertEquals(expected.outputWidth, transform.outputWidth)
            assertEquals(expected.outputHeight, transform.outputHeight)
            assertEquals(
                OrientedSize(expected.outputWidth, expected.outputHeight),
                orientedDimensions(width, height, expected.orientation),
            )
            assertMaps(transform, 0f, 0f, expected.origin.first, expected.origin.second)
            assertMaps(transform, width.toFloat(), 0f, expected.topRight.first, expected.topRight.second)
            assertMaps(transform, 0f, height.toFloat(), expected.bottomLeft.first, expected.bottomLeft.second)
            assertMaps(transform, width.toFloat(), height.toFloat(), expected.far.first, expected.far.second)
            assertMappedOrigin(transform, width, height)
            assertEquals(listOf(0f, 0f, 1f), transform.matrixValues().takeLast(3))
        }

        val interior = exifOrientationTransform(6, width, height)
        assertMaps(interior, 1f, 2f, height - 2f, 1f)
        val mirror = exifOrientationTransform(5, width, height)
        assertMaps(mirror, 1f, 2f, height - 2f, width - 1f)
        val transverse = exifOrientationTransform(7, width, height)
        assertMaps(transverse, 1f, 2f, 2f, 1f)
        val rotate270 = exifOrientationTransform(8, width, height)
        assertMaps(rotate270, 1f, 2f, 2f, width - 1f)

        val a7cr = exifOrientationTransform(6, A7CR_STORAGE_W, A7CR_STORAGE_H)
        assertEquals(OrientedSize(6336, 8448), OrientedSize(a7cr.outputWidth, a7cr.outputHeight))
        assertEquals(
            listOf(0f, -1f, A7CR_STORAGE_H.toFloat(), 1f, 0f, 0f, 0f, 0f, 1f),
            a7cr.matrixValues().toList(),
        )
        assertMaps(a7cr, 0f, 0f, A7CR_STORAGE_H.toFloat(), 0f)
        assertMaps(a7cr, A7CR_STORAGE_W.toFloat(), A7CR_STORAGE_H.toFloat(), 0f, A7CR_STORAGE_W.toFloat())

        val identity = exifOrientationTransform(1, A7CR_STORAGE_W, A7CR_STORAGE_H)
        assertEquals(listOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f), identity.matrixValues().toList())
        assertEquals(A7CR_STORAGE_W, identity.outputWidth)
        assertEquals(A7CR_STORAGE_H, identity.outputHeight)
        for (unknown in listOf(0, 9)) {
            val transform = exifOrientationTransform(unknown, width, height)
            assertEquals(listOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f), transform.matrixValues().toList())
            assertEquals(width, transform.outputWidth)
            assertEquals(height, transform.outputHeight)
            assertMaps(transform, 3f, 4f, 3f, 4f)
        }
    }

    @Test
    fun sampleSizeStaysPowerOfTwoAndDoesNotExceedTheFactor() {
        assertEquals(1, sampleSizeAtMost(0.0))
        assertEquals(1, sampleSizeAtMost(1.0))
        assertEquals(1, sampleSizeAtMost(1.999))
        assertEquals(2, sampleSizeAtMost(2.0))
        assertEquals(2, sampleSizeAtMost(3.999))
        assertEquals(4, sampleSizeAtMost(4.0))
        assertEquals(8, sampleSizeAtMost(8.8))
        assertEquals(1, sampleSizeAtMost(Double.NaN))
        assertEquals(1, sampleSizeAtMost(Double.POSITIVE_INFINITY))
        assertEquals(1, sampleSizeAtMost(Double.NEGATIVE_INFINITY))
        assertEquals(1, sampleSizeAtMost(-4.0))

        val oriented = a7crOriented()
        assertEquals(
            coverSampleSize(oriented.width, oriented.height, PRINT_WIDTH, PRINT_HEIGHT, zoom = 0.4f),
            coverSampleSize(oriented.width, oriented.height, PRINT_WIDTH, PRINT_HEIGHT, zoom = 0.2f),
        )
        assertEquals(
            coverSampleSize(10_000, 10_000, 100, 100, zoom = 3f),
            coverSampleSize(10_000, 10_000, 100, 100, zoom = 9f),
        )
        assertEquals(32, coverSampleSize(10_000, 10_000, 100, 100, zoom = 9f))
        assertEquals(8, coverSampleSize(oriented.width, oriented.height, PRINT_WIDTH, PRINT_HEIGHT, zoom = 0.2f))
        assertEquals(1, coverSampleSize(0, 100, 10, 10))
        assertEquals(1, coverSampleSize(100, 100, 0, 10))
        assertEquals(1, containSampleSize(100, 100, 10, 0))
        assertEquals(1, widthSampleSize(0, THUMB_WIDTH))
        assertEquals(1, longEdgeSampleSize(100, 100, 0))

        assertEquals(2, coverSampleSize(8000, 2000, 1000, 1000))
        assertEquals(8, containSampleSize(8000, 2000, 1000, 1000))
        assertEquals(2, widthSampleSize(2000, THUMB_WIDTH))
        assertEquals(4, longEdgeSampleSize(2000, 8000, AI_UPLOAD_MAX_EDGE))
    }

    @Test
    fun previewDecodeSizeUsesIntegerScaleAndABoxWhenUnknown() {
        assertEquals(OrientedSize(PREVIEW_MAX_EDGE, PREVIEW_MAX_EDGE), previewDecodeSize(0, 1200, PREVIEW_MAX_EDGE))
        assertEquals(OrientedSize(PREVIEW_MAX_EDGE, PREVIEW_MAX_EDGE), previewDecodeSize(100, -1, PREVIEW_MAX_EDGE))
        assertEquals(OrientedSize(4096, 1024), previewDecodeSize(8000, 2000, PREVIEW_MAX_EDGE))
        assertEquals(OrientedSize(1, 3), previewDecodeSize(5, 8, 3))
        assertEquals(OrientedSize(1, 1), previewDecodeSize(0, 0, 0))
        val portrait = previewDecodeSize(1000, 3000, 1000)
        assertEquals(OrientedSize(333, 1000), portrait)
        assertTrue(maxOf(portrait.width, portrait.height) <= 1000)
    }

    private fun a7crOriented(): OrientedSize = orientedDimensions(A7CR_STORAGE_W, A7CR_STORAGE_H, 6)

    private fun sampledSize(width: Int, height: Int, sampleSize: Int): OrientedSize {
        val sample = sampleSize.coerceAtLeast(1)
        return OrientedSize(width / sample, height / sample)
    }

    private fun assertMaps(transform: Affine2x3, x: Float, y: Float, expectedX: Float, expectedY: Float) {
        val (mappedX, mappedY) = transform.map(x, y)
        assertEquals(expectedX, mappedX, 0f)
        assertEquals(expectedY, mappedY, 0f)
    }

    private fun assertMappedOrigin(transform: Affine2x3, width: Int, height: Int) {
        val corners = listOf(0f to 0f, width.toFloat() to 0f, 0f to height.toFloat(), width.toFloat() to height.toFloat())
            .map { (x, y) -> transform.map(x, y) }
        assertEquals(0f, corners.minOf { it.first }, 0f)
        assertEquals(0f, corners.minOf { it.second }, 0f)
        assertEquals(transform.outputWidth.toFloat(), corners.maxOf { it.first }, 0f)
        assertEquals(transform.outputHeight.toFloat(), corners.maxOf { it.second }, 0f)
    }
}

private data class ExifCase(
    val orientation: Int,
    val outputWidth: Int,
    val outputHeight: Int,
    val origin: Pair<Float, Float>,
    val topRight: Pair<Float, Float>,
    val bottomLeft: Pair<Float, Float>,
    val far: Pair<Float, Float>,
)

private const val A7CR_STORAGE_W = 8448
private const val A7CR_STORAGE_H = 6336
