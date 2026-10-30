package io.github.wa_otomia.darkroom.data.imaging

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import androidx.exifinterface.media.ExifInterface
import io.github.wa_otomia.darkroom.core.AI_UPLOAD_MAX_EDGE
import io.github.wa_otomia.darkroom.core.Affine2x3
import io.github.wa_otomia.darkroom.core.DEFAULT_PRINT_QUALITY
import io.github.wa_otomia.darkroom.core.OrientedSize
import io.github.wa_otomia.darkroom.core.PRINT_HEIGHT
import io.github.wa_otomia.darkroom.core.PRINT_WIDTH
import io.github.wa_otomia.darkroom.core.PhotoMeta
import io.github.wa_otomia.darkroom.core.PixelCrop
import io.github.wa_otomia.darkroom.core.PrintFit
import io.github.wa_otomia.darkroom.core.THUMB_WIDTH
import io.github.wa_otomia.darkroom.core.ViewportPlacement
import io.github.wa_otomia.darkroom.core.WatermarkSettings
import io.github.wa_otomia.darkroom.core.centeredAspectCrop
import io.github.wa_otomia.darkroom.core.clampCrop
import io.github.wa_otomia.darkroom.core.clampZoom
import io.github.wa_otomia.darkroom.core.containSampleSize
import io.github.wa_otomia.darkroom.core.coverSampleSize
import io.github.wa_otomia.darkroom.core.coverScale
import io.github.wa_otomia.darkroom.core.cropAspect
import io.github.wa_otomia.darkroom.core.cropInSourceSpace
import io.github.wa_otomia.darkroom.core.exifOrientationTransform
import io.github.wa_otomia.darkroom.core.exportSheetSize
import io.github.wa_otomia.darkroom.core.longEdgeSampleSize
import io.github.wa_otomia.darkroom.core.sheetQuarterTurns
import io.github.wa_otomia.darkroom.core.ensureJpegBytes
import io.github.wa_otomia.darkroom.core.exactQuarterTurns
import io.github.wa_otomia.darkroom.core.normalizeQuarterTurns
import io.github.wa_otomia.darkroom.core.orientedDimensions
import io.github.wa_otomia.darkroom.core.resolveCropOnBitmap
import io.github.wa_otomia.darkroom.core.rotateCrop
import io.github.wa_otomia.darkroom.core.rotatedSize
import io.github.wa_otomia.darkroom.core.widthSampleSize
import kotlinx.coroutines.CancellationException
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer

data class ImageInfo(
    val width: Int,
    val height: Int,
    val orientation: Int,
)

object ImagePipeline {
    /**
     * Long edge an imported photo is scaled down to by [decodeToJpeg].
     *
     * Nothing downstream can use more: the print sheet is [PRINT_WIDTH]x[PRINT_HEIGHT],
     * [grokUploadJpeg] caps its upload at 1536 and [thumbnailJpeg] at [THUMB_WIDTH]. A centred
     * 2:3 cut out of a 4:3 frame at 2048 still measures 1024x1536, which is 98% of the sheet, so
     * the crop the user frames is not visibly softer than one taken from the full-size original.
     */
    const val IMPORT_MAX_EDGE = 2048

    /** Matches [DEFAULT_PRINT_QUALITY]: the import is the archived original. */
    private const val IMPORT_QUALITY = 90

    /**
     * Long-edge cap used only when a Grok still has to be transcoded to JPEG.
     *
     * Settings allow only 1k / 2k, so this matches the largest paid render and
     * does not downscale it. Print ([PRINT_WIDTH]x[PRINT_HEIGHT]) and
     * [grokUploadJpeg] (1536) cannot use more. Already-JPEG bytes skip this
     * entirely and keep whatever size arrived.
     */
    const val GENERATE_MAX_EDGE = 2048

    /**
     * Transcodes a still that is not already JPEG (HEIC/HEIF, PNG, WebP) into JPEG bytes.
     *
     * [BitmapFactory] cannot decode HEIC at all, which is why this goes through [ImageDecoder]
     * instead. That also settles orientation: ImageDecoder applies the EXIF tag and the HEIF
     * `irot` property itself, so the bytes returned here are upright and carry no orientation of
     * their own. That is exactly what the rest of the pipeline expects of a stored original —
     * [decodeRotated] reads ORIENTATION_NORMAL and leaves it alone — and it is why this does not
     * repeat [decodeRotated]'s matrix work.
     *
     * Scaling is requested before the pixels are produced, so a 48 MP HEIC never materialises at
     * full size: peak cost is the [IMPORT_MAX_EDGE]-bounded output bitmap, about 13 MB.
     *
     * Lossy by nature. Callers are expected to tell the user the file was converted.
     */
    fun decodeToJpeg(
        bytes: ByteArray,
        quality: Int = IMPORT_QUALITY,
        maxEdge: Int = IMPORT_MAX_EDGE,
    ): ByteArray {
        // createSource(ByteArray) is API 31; the ByteBuffer overload is API 28.
        val source = ImageDecoder.createSource(ByteBuffer.wrap(bytes))
        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            // The default allocator hands back a hardware bitmap, whose pixels encodeJpeg's
            // compress() cannot read back.
            decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE)
            val size = info.size
            val longEdge = maxOf(size.width, size.height)
            if (longEdge > maxEdge) {
                val scale = maxEdge.toDouble() / longEdge
                decoder.setTargetSize(
                    (size.width * scale).toInt().coerceAtLeast(1),
                    (size.height * scale).toInt().coerceAtLeast(1),
                )
            }
        }
        return encodeJpeg(bitmap, quality).also { bitmap.recycle() }
    }

    /**
     * Archive a Grok still as JPEG. Camera / FTP originals never call this;
     * those stay on the reject-non-JPEG path. Orientation tags are applied by
     * [decodeToJpeg] / ImageDecoder, so the stored file is upright.
     */
    fun ensureStoredJpeg(bytes: ByteArray): ByteArray = ensureJpegBytes(bytes) { raw ->
        try {
            decodeToJpeg(raw, quality = IMPORT_QUALITY, maxEdge = GENERATE_MAX_EDGE)
        } catch (e: CancellationException) {
            throw e
        } catch (_: OutOfMemoryError) {
            error("无法解码 JPEG")
        } catch (_: Exception) {
            error("无法解码 JPEG")
        }
    }

    fun probe(bytes: ByteArray): ImageInfo {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val orientation = readOrientation(bytes)
        val size = orientedDimensions(bounds.outWidth, bounds.outHeight, orientation)
        return ImageInfo(size.width, size.height, orientation)
    }

    /**
     * Full-size EXIF correction, kept so the 5/7 mapping stays the reference
     * [exifOrientationTransform] was locked to. The bounded JPEG entry points do not call this:
     * they fold that same affine into the output draw and never allocate a second source-sized copy.
     */
    fun decodeRotated(bytes: ByteArray): Bitmap {
        val raw = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: error("无法解码 JPEG")
        val orientation = readOrientation(bytes)
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.preScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.preScale(-1f, 1f)
            }
        }
        return if (matrix.isIdentity) raw else Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true).also {
            if (it != raw) raw.recycle()
        }
    }

    fun thumbnailJpeg(bytes: ByteArray, quality: Int = 72): ByteArray = reportingDecodeFailure {
        val source = inspectJpeg(bytes)
        val raw = decodeSampled(bytes, widthSampleSize(source.oriented.width, THUMB_WIDTH))
        try {
            val outW = source.oriented.width.coerceAtMost(THUMB_WIDTH)
            val outH = if (source.oriented.width == 0) {
                source.oriented.height
            } else {
                (source.oriented.height * outW) / source.oriented.width
            }
            encodeScaled(raw, source.orientation, outW.coerceAtLeast(1), outH.coerceAtLeast(1), quality)
        } finally {
            recycle(raw)
        }
    }

    fun grokUploadJpeg(bytes: ByteArray): ByteArray = reportingDecodeFailure {
        val source = inspectJpeg(bytes)
        val sample = longEdgeSampleSize(
            source.oriented.width,
            source.oriented.height,
            AI_UPLOAD_MAX_EDGE,
        )
        val raw = decodeSampled(bytes, sample)
        try {
            val (outW, outH) = uploadSize(source.oriented)
            encodeScaled(raw, source.orientation, outW, outH, 85)
        } finally {
            recycle(raw)
        }
    }

    /**
     * [crop] is expressed on the photo as the user framed it: EXIF-corrected, then turned
     * [rotateQuarters] quarters clockwise. When [cropImageWidth] or [cropImageHeight] is omitted,
     * that record space is the full-size oriented frame, not the subsampled bitmap. The rect is
     * mapped onto the sampled frame and drawn with one matrix (output ∘ quarter-turn ∘ EXIF).
     *
     * When [placement] is set (or [rotationDegrees] is not a right angle), the source is drawn
     * onto the white sheet at that zoom/pan/angle so letterbox margins are kept. Cover scale uses
     * the orientation-corrected size of the sampled bitmap; the rest of the placement formula is
     * unchanged.
     *
     * [watermark] is painted after the photo and only on this print/export path.
     * [cropToJpeg] and [grokUploadJpeg] never accept a watermark. [outputScale] multiplies
     * the 1040×1560 sheet (use `2` for a 2080×3120 album export). [context] is required
     * to tint SNS [android.graphics.drawable.VectorDrawable]s; date-only marks still draw without it.
     *
     * [sheetForPrinter] (default true) quarter-turns a landscape frame onto the portrait
     * printer sheet. Export passes false so 横幅 stays 3120×2080 and 竖幅 stays 2080×3120.
     * That turn is applied to the finished sheet (watermark included), never to a source-sized copy.
     */
    fun renderPrintJpeg(
        bytes: ByteArray,
        crop: PixelCrop?,
        fit: PrintFit,
        quality: Int = DEFAULT_PRINT_QUALITY,
        cropImageWidth: Int? = null,
        cropImageHeight: Int? = null,
        rotateQuarters: Int = 0,
        landscape: Boolean = false,
        rotationDegrees: Float = rotateQuarters * 90f,
        placement: ViewportPlacement? = null,
        watermark: WatermarkSettings? = null,
        photo: PhotoMeta? = null,
        outputScale: Int = 1,
        context: Context? = null,
        sheetForPrinter: Boolean = true,
    ): ByteArray = reportingDecodeFailure {
        val source = inspectJpeg(bytes)
        val placementPath = placement != null || exactQuarterTurns(rotationDegrees) == null
        val sample = if (placementPath) {
            val dest = exportSheetSize(landscape, outputScale)
            coverSampleSize(
                source.oriented.width,
                source.oriented.height,
                dest.width,
                dest.height,
                placement?.zoom ?: 1f,
            )
        } else {
            quarterSheetSampleSize(
                source,
                crop,
                cropImageWidth,
                cropImageHeight,
                rotateQuarters,
                landscape,
                fit,
                outputScale,
            )
        }
        val raw = decodeSampled(bytes, sample)
        try {
            val exif = exifOrientationTransform(source.orientation, raw.width, raw.height)
            val sheet = if (placementPath) {
                val placed = placement ?: implicitPlacement(
                    exif.outputWidth,
                    exif.outputHeight,
                    rotationDegrees,
                    landscape,
                )
                renderPlacementSheet(
                    raw,
                    exif,
                    placed,
                    landscape,
                    ontoPortraitSheet = sheetForPrinter,
                    outputScale = outputScale,
                    watermark = watermark,
                    photo = photo,
                    context = context,
                )
            } else {
                renderQuarterSheet(
                    raw,
                    exif,
                    source.oriented,
                    crop,
                    cropImageWidth,
                    cropImageHeight,
                    rotateQuarters,
                    landscape,
                    fit,
                    outputScale,
                    watermark,
                    photo,
                    context,
                    sheetForPrinter,
                )
            }
            try {
                encodeJpeg(sheet, quality)
            } finally {
                recycle(sheet)
            }
        } finally {
            recycle(raw)
        }
    }

    /**
     * Cuts what the user framed and leaves it upright; the sheet turn happens at print time.
     * Never accepts or draws a watermark — AI upload must stay unmarked.
     *
     * A placement (or a non-right-angle) is drawn onto the frame-sized sheet. A right-angle crop
     * is emitted at the sampled crop size, decoded so the oriented long edge is about
     * [AI_UPLOAD_MAX_EDGE]. The crop record space follows [renderPrintJpeg].
     */
    fun cropToJpeg(
        bytes: ByteArray,
        crop: PixelCrop?,
        cropImageWidth: Int? = null,
        cropImageHeight: Int? = null,
        quality: Int = DEFAULT_PRINT_QUALITY,
        rotateQuarters: Int = 0,
        landscape: Boolean = false,
        rotationDegrees: Float = rotateQuarters * 90f,
        placement: ViewportPlacement? = null,
    ): ByteArray = reportingDecodeFailure {
        val source = inspectJpeg(bytes)
        val placementPath = placement != null || exactQuarterTurns(rotationDegrees) == null
        val sample = if (placementPath) {
            val dest = exportSheetSize(landscape, outputScale = 1)
            coverSampleSize(
                source.oriented.width,
                source.oriented.height,
                dest.width,
                dest.height,
                placement?.zoom ?: 1f,
            )
        } else {
            longEdgeSampleSize(source.oriented.width, source.oriented.height, AI_UPLOAD_MAX_EDGE)
        }
        val raw = decodeSampled(bytes, sample)
        try {
            val exif = exifOrientationTransform(source.orientation, raw.width, raw.height)
            if (placementPath) {
                val placed = placement ?: implicitPlacement(
                    exif.outputWidth,
                    exif.outputHeight,
                    rotationDegrees,
                    landscape,
                )
                val framed = renderPlacementSheet(raw, exif, placed, landscape, ontoPortraitSheet = false)
                try {
                    encodeJpeg(framed, quality)
                } finally {
                    recycle(framed)
                }
            } else {
                val cut = renderCropBitmap(
                    raw,
                    exif,
                    source.oriented,
                    crop,
                    cropImageWidth,
                    cropImageHeight,
                    rotateQuarters,
                    landscape,
                )
                try {
                    encodeJpeg(cut, quality)
                } finally {
                    recycle(cut)
                }
            }
        } finally {
            recycle(raw)
        }
    }

    private fun sourceCut(
        framedCrop: PixelCrop,
        framed: OrientedSize,
        bitmapWidth: Int,
        bitmapHeight: Int,
        rotateQuarters: Int,
    ): PixelCrop {
        val inFrame = clampCrop(framedCrop, framed.width, framed.height)
        val inSource = cropInSourceSpace(inFrame, bitmapWidth, bitmapHeight, rotateQuarters)
        return clampCrop(inSource, bitmapWidth, bitmapHeight)
    }

    private fun implicitPlacement(
        imageWidth: Int,
        imageHeight: Int,
        rotationDegrees: Float,
        landscape: Boolean,
    ): ViewportPlacement {
        val destW = if (landscape) PRINT_HEIGHT else PRINT_WIDTH
        val destH = if (landscape) PRINT_WIDTH else PRINT_HEIGHT
        return ViewportPlacement(
            zoom = 1f,
            offsetX = 0f,
            offsetY = 0f,
            rotationDegrees = rotationDegrees,
            frameWidth = destW.toFloat(),
            frameHeight = destH.toFloat(),
            imageWidth = imageWidth,
            imageHeight = imageHeight,
        )
    }

    /**
     * Draws [placement] onto a white frame-sized bitmap. [preparePrintBitmap] is not used:
     * a second cover-fit would erase letterbox margins.
     *
     * [source] is the unrotated sample. [exif] maps it into the oriented space the placement
     * formula was written against; cover uses that oriented size, not the storage size.
     */
    private fun renderPlacementSheet(
        source: Bitmap,
        exif: Affine2x3,
        placement: ViewportPlacement,
        landscape: Boolean,
        ontoPortraitSheet: Boolean,
        outputScale: Int = 1,
        watermark: WatermarkSettings? = null,
        photo: PhotoMeta? = null,
        context: Context? = null,
    ): Bitmap {
        val dest = exportSheetSize(landscape, outputScale)
        val framed = Bitmap.createBitmap(dest.width, dest.height, Bitmap.Config.ARGB_8888)
        var handedOff = false
        try {
            val canvas = Canvas(framed)
            canvas.drawColor(Color.WHITE)
            drawPlacement(canvas, source, exif, placement, dest.width, dest.height)
            val sheet = finishSheet(framed, landscape, ontoPortraitSheet, watermark, photo, context)
            handedOff = true
            return sheet
        } finally {
            if (!handedOff) recycle(framed)
        }
    }

    private fun applyWatermark(
        sheet: Bitmap,
        watermark: WatermarkSettings?,
        photo: PhotoMeta?,
        frameW: Int,
        frameH: Int,
        context: Context?,
    ) {
        if (watermark == null || photo == null) return
        if (!watermark.sns.enabled && !watermark.date.enabled) return
        WatermarkRenderer.apply(sheet, watermark, photo, frameW, frameH, context)
    }

    /**
     * Same placement formula as before, with [exif] applied first so the sample does not need
     * an oriented copy. Cover is computed from the EXIF output size of [source].
     */
    private fun drawPlacement(
        canvas: Canvas,
        source: Bitmap,
        exif: Affine2x3,
        placement: ViewportPlacement,
        destW: Int,
        destH: Int,
    ) {
        val orientedW = exif.outputWidth
        val orientedH = exif.outputHeight
        val cover = coverScale(destW.toFloat(), destH.toFloat(), orientedW, orientedH)
        val scale = cover * clampZoom(placement.zoom)
        val frameW = placement.frameWidth.takeIf { it > 0f } ?: destW.toFloat()
        val frameH = placement.frameHeight.takeIf { it > 0f } ?: destH.toFloat()
        val panX = placement.offsetX * destW / frameW
        val panY = placement.offsetY * destH / frameH
        val matrix = Matrix()
        matrix.setValues(exif.matrixValues())
        matrix.postTranslate(-orientedW / 2f, -orientedH / 2f)
        matrix.postScale(scale, scale)
        matrix.postRotate(placement.rotationDegrees)
        matrix.postTranslate(destW / 2f + panX, destH / 2f + panY)
        canvas.drawBitmap(source, matrix, bitmapPaint())
    }

    private fun rotateBitmap(source: Bitmap, quarterTurns: Int): Bitmap {
        val turns = normalizeQuarterTurns(quarterTurns)
        if (turns == 0) return source
        val matrix = Matrix().apply { postRotate(90f * turns) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    fun preparePrintBitmap(
        source: Bitmap,
        fit: PrintFit,
        outputScale: Int = 1,
        landscape: Boolean = false,
    ): Bitmap {
        val dest = exportSheetSize(landscape, outputScale)
        val destW = dest.width
        val destH = dest.height
        val out = Bitmap.createBitmap(destW, destH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val src = Rect(0, 0, source.width, source.height)
        val dst = if (fit == PrintFit.CONTAIN) {
            letterbox(source.width, source.height, destW, destH)
        } else {
            coverRect(source.width, source.height, destW, destH)
        }
        canvas.drawBitmap(source, src, dst, paint)
        return out
    }

    fun encodeJpeg(bitmap: Bitmap, quality: Int): ByteArray {
        val stream = ByteArrayOutputStream()
        if (!bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)) {
            error("JPEG 编码失败")
        }
        return stream.toByteArray()
    }

    fun writeFile(file: File, bytes: ByteArray) {
        file.parentFile?.mkdirs()
        file.writeBytes(bytes)
    }

    private fun readOrientation(bytes: ByteArray): Int {
        return try {
            ExifInterface(ByteArrayInputStream(bytes)).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        } catch (_: Exception) {
            1
        }
    }

    private fun letterbox(sw: Int, sh: Int, dw: Int, dh: Int): Rect {
        val scale = minOf(dw.toFloat() / sw, dh.toFloat() / sh)
        val w = (sw * scale).toInt().coerceAtLeast(1)
        val h = (sh * scale).toInt().coerceAtLeast(1)
        val x = (dw - w) / 2
        val y = (dh - h) / 2
        return Rect(x, y, x + w, y + h)
    }

    private fun coverRect(sw: Int, sh: Int, dw: Int, dh: Int): Rect {
        val scale = maxOf(dw.toFloat() / sw, dh.toFloat() / sh)
        val w = (sw * scale).toInt().coerceAtLeast(1)
        val h = (sh * scale).toInt().coerceAtLeast(1)
        val x = (dw - w) / 2
        val y = (dh - h) / 2
        return Rect(x, y, x + w, y + h)
    }

    /**
     * Storage size plus EXIF, before any pixels are allocated. Crop baselines live in this
     * oriented space (and the quarter-turned frame derived from it), never in the sample.
     */
    private data class JpegBounds(
        val orientation: Int,
        val oriented: OrientedSize,
    )

    private fun inspectJpeg(bytes: ByteArray): JpegBounds {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) error("无法解码 JPEG")
        val orientation = readOrientation(bytes)
        return JpegBounds(
            orientation,
            orientedDimensions(bounds.outWidth, bounds.outHeight, orientation),
        )
    }

    /** Unrotated sample. [sampleSize] comes from DecodeBudget and is at least 1. */
    private fun decodeSampled(bytes: ByteArray, sampleSize: Int): Bitmap {
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize.coerceAtLeast(1)
            inJustDecodeBounds = false
        }
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
            ?: error("无法解码 JPEG")
        if (bitmap.width <= 0 || bitmap.height <= 0) {
            recycle(bitmap)
            error("无法解码 JPEG")
        }
        return bitmap
    }

    private fun uploadSize(oriented: OrientedSize): Pair<Int, Int> {
        val longEdge = maxOf(oriented.width, oriented.height)
        val scale = if (longEdge <= 0) 1f else minOf(1f, AI_UPLOAD_MAX_EDGE.toFloat() / longEdge)
        return (oriented.width * scale).toInt().coerceAtLeast(1) to
            (oriented.height * scale).toInt().coerceAtLeast(1)
    }

    /**
     * Scale the oriented sample to [outW]×[outH]. An identity EXIF at the same size encodes
     * the sample directly, matching the old "already the right size" path.
     */
    private fun encodeScaled(raw: Bitmap, orientation: Int, outW: Int, outH: Int, quality: Int): ByteArray {
        val exif = exifOrientationTransform(orientation, raw.width, raw.height)
        if (exif.isIdentity() && outW == raw.width && outH == raw.height) {
            return encodeJpeg(raw, quality)
        }
        val out = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(out)
            val matrix = Matrix()
            matrix.setValues(exif.matrixValues())
            matrix.postConcat(mapRectMatrix(0f, 0f, exif.outputWidth.toFloat(), exif.outputHeight.toFloat(), Rect(0, 0, outW, outH)))
            canvas.drawBitmap(raw, matrix, bitmapPaint())
            return encodeJpeg(out, quality)
        } finally {
            recycle(out)
        }
    }

    private fun quarterSheetSampleSize(
        source: JpegBounds,
        crop: PixelCrop?,
        cropImageWidth: Int?,
        cropImageHeight: Int?,
        rotateQuarters: Int,
        landscape: Boolean,
        fit: PrintFit,
        outputScale: Int,
    ): Int {
        val fullFramed = rotatedSize(source.oriented.width, source.oriented.height, rotateQuarters)
        val recordW = cropImageWidth ?: fullFramed.width
        val recordH = cropImageHeight ?: fullFramed.height
        val region = if (crop != null) {
            resolveCropOnBitmap(crop, recordW, recordH, fullFramed.width, fullFramed.height)
        } else if (fit == PrintFit.COVER) {
            centeredAspectCrop(fullFramed.width, fullFramed.height, cropAspect(landscape))
        } else {
            null
        }
        val dest = exportSheetSize(landscape, outputScale)
        return if (region != null) {
            coverSampleSize(region.width, region.height, dest.width, dest.height)
        } else {
            containSampleSize(fullFramed.width, fullFramed.height, dest.width, dest.height)
        }
    }

    /**
     * Right-angle print/export sheet. The crop is resolved in full-size framed space (or the
     * caller's record size), then mapped onto the sample. One draw applies EXIF, the user
     * quarter-turn and the cover/letterbox rect. The printer turn, if any, is [finishSheet].
     */
    private fun renderQuarterSheet(
        raw: Bitmap,
        exif: Affine2x3,
        fullOriented: OrientedSize,
        crop: PixelCrop?,
        cropImageWidth: Int?,
        cropImageHeight: Int?,
        rotateQuarters: Int,
        landscape: Boolean,
        fit: PrintFit,
        outputScale: Int,
        watermark: WatermarkSettings?,
        photo: PhotoMeta?,
        context: Context?,
        sheetForPrinter: Boolean,
    ): Bitmap {
        val dest = exportSheetSize(landscape, outputScale)
        val framed = Bitmap.createBitmap(dest.width, dest.height, Bitmap.Config.ARGB_8888)
        var handedOff = false
        try {
            val canvas = Canvas(framed)
            canvas.drawColor(Color.WHITE)
            drawQuarterCrop(
                canvas,
                raw,
                exif,
                fullOriented,
                crop,
                cropImageWidth,
                cropImageHeight,
                rotateQuarters,
                landscape,
                fit,
                dest.width,
                dest.height,
            )
            val sheet = finishSheet(framed, landscape, sheetForPrinter, watermark, photo, context)
            handedOff = true
            return sheet
        } finally {
            if (!handedOff) recycle(framed)
        }
    }

    /** Crop-sized output for [cropToJpeg]. No sheet, no watermark. */
    private fun renderCropBitmap(
        raw: Bitmap,
        exif: Affine2x3,
        fullOriented: OrientedSize,
        crop: PixelCrop?,
        cropImageWidth: Int?,
        cropImageHeight: Int?,
        rotateQuarters: Int,
        landscape: Boolean,
    ): Bitmap {
        val content = sampledContentRect(
            exif,
            fullOriented,
            crop,
            cropImageWidth,
            cropImageHeight,
            rotateQuarters,
            landscape,
            fallbackCover = true,
        )
        val out = Bitmap.createBitmap(
            content.width.coerceAtLeast(1),
            content.height.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888,
        )
        try {
            val canvas = Canvas(out)
            drawContent(canvas, raw, exif, content, Rect(0, 0, out.width, out.height), rotateQuarters)
            return out
        } catch (t: Throwable) {
            recycle(out)
            throw t
        }
    }

    private fun drawQuarterCrop(
        canvas: Canvas,
        raw: Bitmap,
        exif: Affine2x3,
        fullOriented: OrientedSize,
        crop: PixelCrop?,
        cropImageWidth: Int?,
        cropImageHeight: Int?,
        rotateQuarters: Int,
        landscape: Boolean,
        fit: PrintFit,
        destW: Int,
        destH: Int,
    ) {
        val content = sampledContentRect(
            exif,
            fullOriented,
            crop,
            cropImageWidth,
            cropImageHeight,
            rotateQuarters,
            landscape,
            fallbackCover = fit == PrintFit.COVER,
        )
        val usedFit = if (content.fromCrop) PrintFit.COVER else fit
        val dst = if (usedFit == PrintFit.CONTAIN) {
            letterbox(content.width, content.height, destW, destH)
        } else {
            coverRect(content.width, content.height, destW, destH)
        }
        drawContent(canvas, raw, exif, content, dst, rotateQuarters)
    }

    /**
     * Pixel rect of what [preparePrintBitmap] used to receive, expressed in sampled framed space.
     * [fromCrop] is false only for a contain of the whole frame (no centred cover crop).
     */
    private data class SampledContent(
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int,
        val fromCrop: Boolean,
    )

    private fun sampledContentRect(
        exif: Affine2x3,
        fullOriented: OrientedSize,
        crop: PixelCrop?,
        cropImageWidth: Int?,
        cropImageHeight: Int?,
        rotateQuarters: Int,
        landscape: Boolean,
        fallbackCover: Boolean,
    ): SampledContent {
        val fullFramed = rotatedSize(fullOriented.width, fullOriented.height, rotateQuarters)
        val sampledOriented = OrientedSize(exif.outputWidth, exif.outputHeight)
        val sampledFramed = rotatedSize(sampledOriented.width, sampledOriented.height, rotateQuarters)
        val recordW = cropImageWidth ?: fullFramed.width
        val recordH = cropImageHeight ?: fullFramed.height
        val mapped = if (crop != null) {
            resolveCropOnBitmap(crop, recordW, recordH, sampledFramed.width, sampledFramed.height)
        } else if (fallbackCover) {
            val centered = centeredAspectCrop(fullFramed.width, fullFramed.height, cropAspect(landscape))
            resolveCropOnBitmap(
                centered,
                fullFramed.width,
                fullFramed.height,
                sampledFramed.width,
                sampledFramed.height,
            )
        } else {
            null
        }
        if (mapped == null) {
            return SampledContent(0, 0, sampledFramed.width.coerceAtLeast(1), sampledFramed.height.coerceAtLeast(1), false)
        }
        val cut = sourceCut(mapped, sampledFramed, sampledOriented.width, sampledOriented.height, rotateQuarters)
        val framedCut = rotateCrop(cut, sampledOriented.width, sampledOriented.height, rotateQuarters)
        return SampledContent(
            framedCut.x,
            framedCut.y,
            framedCut.width.coerceAtLeast(1),
            framedCut.height.coerceAtLeast(1),
            true,
        )
    }

    /**
     * One matrix: rect map ∘ user quarter-turn ∘ EXIF. The quarter-turn coefficients are the
     * EXIF 6/3/8 affines, which are the [decodeRotated] / [rotateBitmap] rotations plus the
     * origin translation. Clip to [dst] so pixels outside the crop cannot paint the margins.
     */
    private fun drawContent(
        canvas: Canvas,
        raw: Bitmap,
        exif: Affine2x3,
        content: SampledContent,
        dst: Rect,
        rotateQuarters: Int,
    ) {
        val matrix = Matrix()
        matrix.setValues(exif.matrixValues())
        matrix.postConcat(
            quarterTurnMatrix(rotateQuarters, exif.outputWidth, exif.outputHeight),
        )
        matrix.postConcat(
            mapRectMatrix(
                content.x.toFloat(),
                content.y.toFloat(),
                content.width.toFloat(),
                content.height.toFloat(),
                dst,
            ),
        )
        canvas.save()
        try {
            canvas.clipRect(dst)
            canvas.drawBitmap(raw, matrix, bitmapPaint())
        } finally {
            canvas.restore()
        }
    }

    /**
     * Watermark stays in frame space. A landscape printer sheet is then turned as an
     * output-sized bitmap — never as a second copy of the source.
     */
    private fun finishSheet(
        framed: Bitmap,
        landscape: Boolean,
        ontoPortraitSheet: Boolean,
        watermark: WatermarkSettings?,
        photo: PhotoMeta?,
        context: Context?,
    ): Bitmap {
        applyWatermark(framed, watermark, photo, framed.width, framed.height, context)
        val turns = sheetQuarterTurns(landscape, ontoPortraitSheet)
        if (turns == 0) return framed
        val sheet = rotateBitmap(framed, turns)
        if (sheet !== framed) recycle(framed)
        return sheet
    }

    /** User quarter-turn on an already EXIF-corrected raster, matching [rotateBitmap]. */
    private fun quarterTurnMatrix(quarterTurns: Int, width: Int, height: Int): Matrix {
        val orientation = when (normalizeQuarterTurns(quarterTurns)) {
            1 -> ExifInterface.ORIENTATION_ROTATE_90
            2 -> ExifInterface.ORIENTATION_ROTATE_180
            3 -> ExifInterface.ORIENTATION_ROTATE_270
            else -> ExifInterface.ORIENTATION_NORMAL
        }
        val matrix = Matrix()
        matrix.setValues(exifOrientationTransform(orientation, width, height).matrixValues())
        return matrix
    }

    /** Maps a source rect onto [dst] the way [Canvas.drawBitmap] does for a src/dst pair. */
    private fun mapRectMatrix(srcLeft: Float, srcTop: Float, srcWidth: Float, srcHeight: Float, dst: Rect): Matrix {
        val matrix = Matrix()
        val sx = if (srcWidth == 0f) 1f else dst.width().toFloat() / srcWidth
        val sy = if (srcHeight == 0f) 1f else dst.height().toFloat() / srcHeight
        matrix.postTranslate(-srcLeft, -srcTop)
        matrix.postScale(sx, sy)
        matrix.postTranslate(dst.left.toFloat(), dst.top.toFloat())
        return matrix
    }

    private fun Affine2x3.isIdentity(): Boolean =
        scaleX == 1f && skewX == 0f && transX == 0f &&
            skewY == 0f && scaleY == 1f && transY == 0f

    private fun bitmapPaint(): Paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    private fun recycle(bitmap: Bitmap) {
        if (!bitmap.isRecycled) bitmap.recycle()
    }

    /**
     * Decode, allocate and draw can throw [OutOfMemoryError]. Callers only catch [Exception];
     * this message is what UserErrors maps to the localised decode failure. Cancellation is
     * not an allocation failure and must keep propagating.
     */
    private inline fun <T> reportingDecodeFailure(block: () -> T): T {
        try {
            return block()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (oom: OutOfMemoryError) {
            throw IllegalStateException("无法解码 JPEG", oom)
        }
    }
}
