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
import io.github.wa_otomia.darkroom.core.ID_CUT_LINE_COLOR
import io.github.wa_otomia.darkroom.core.ID_MASK_MSG_MISSING
import io.github.wa_otomia.darkroom.core.ID_MASK_MSG_UNDECODABLE
import io.github.wa_otomia.darkroom.core.IdMaskVerdict
import io.github.wa_otomia.darkroom.core.IdPhotoLayout
import io.github.wa_otomia.darkroom.core.IdPhotoState
import io.github.wa_otomia.darkroom.core.IdRect
import io.github.wa_otomia.darkroom.core.OrientedSize
import io.github.wa_otomia.darkroom.core.PRINT_HEIGHT
import io.github.wa_otomia.darkroom.core.PRINT_WIDTH
import io.github.wa_otomia.darkroom.core.PhotoMeta
import io.github.wa_otomia.darkroom.core.PixelCrop
import io.github.wa_otomia.darkroom.core.PrintFit
import io.github.wa_otomia.darkroom.core.THUMB_WIDTH
import io.github.wa_otomia.darkroom.core.ViewportPlacement
import io.github.wa_otomia.darkroom.core.WatermarkSettings
import io.github.wa_otomia.darkroom.core.aiUploadSize
import io.github.wa_otomia.darkroom.core.centeredAspectCrop
import io.github.wa_otomia.darkroom.core.clampCrop
import io.github.wa_otomia.darkroom.core.clampZoom
import io.github.wa_otomia.darkroom.core.containSampleSize
import io.github.wa_otomia.darkroom.core.coverSampleSize
import io.github.wa_otomia.darkroom.core.coverScale
import io.github.wa_otomia.darkroom.core.cropAspect
import io.github.wa_otomia.darkroom.core.cropInSourceSpace
import io.github.wa_otomia.darkroom.core.compositeIdPixels
import io.github.wa_otomia.darkroom.core.exifOrientationTransform
import io.github.wa_otomia.darkroom.core.exportSheetSize
import io.github.wa_otomia.darkroom.core.featherAlphaFor
import io.github.wa_otomia.darkroom.core.idMaskColoured
import io.github.wa_otomia.darkroom.core.fitMaskContent
import io.github.wa_otomia.darkroom.core.idBackground
import io.github.wa_otomia.darkroom.core.idFeather
import io.github.wa_otomia.darkroom.core.idMaskAspectMatches
import io.github.wa_otomia.darkroom.core.idMaskLumaToAlpha
import io.github.wa_otomia.darkroom.core.idMaskVerdictMessage
import io.github.wa_otomia.darkroom.core.idPhotoLayout
import io.github.wa_otomia.darkroom.core.idPhotoPreset
import io.github.wa_otomia.darkroom.core.longEdgeSampleSize
import io.github.wa_otomia.darkroom.core.offsetAlpha
import io.github.wa_otomia.darkroom.core.sheetQuarterTurns
import io.github.wa_otomia.darkroom.core.ensureJpegBytes
import io.github.wa_otomia.darkroom.core.exactQuarterTurns
import io.github.wa_otomia.darkroom.core.normalizeQuarterTurns
import io.github.wa_otomia.darkroom.core.openAiMaskCanvas
import io.github.wa_otomia.darkroom.core.orientedDimensions
import io.github.wa_otomia.darkroom.core.previewDecodeSize
import io.github.wa_otomia.darkroom.core.resolveCropOnBitmap
import io.github.wa_otomia.darkroom.core.rotateCrop
import io.github.wa_otomia.darkroom.core.rotatedSize
import io.github.wa_otomia.darkroom.core.scaled
import io.github.wa_otomia.darkroom.core.toCellPlacement
import io.github.wa_otomia.darkroom.core.validateIdMask
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

    /** JPEG quality of the padded mask upload, matching [grokUploadJpeg]. */
    private const val ID_MASK_UPLOAD_QUALITY = 85

    /** Long-edge cap when decoding a mask reply; replies are at most about 2k, so this rarely bites. */
    private const val ID_MASK_DECODE_MAX_EDGE = AI_UPLOAD_MAX_EDGE * 2

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

    // -----------------------------------------------------------------------------------------
    // ID photo sheet (EDID020/030/040) and AI mask transport helpers (AIPV030-R04)
    // -----------------------------------------------------------------------------------------

    /**
     * The landscape ID sheet for [state]: one upright cell rendered once and copied into every
     * slot of the size preset's layout (rotated slots turned 90° counter-clockwise), grey cut
     * lines in the gaps, white elsewhere. Never watermarked.
     *
     * Geometry is the scale-1 layout times [outputScale]. [sheetForPrinter] turns the finished
     * 1560×1040 sheet onto the portrait printer sheet (clockwise), like [renderPrintJpeg].
     *
     * A colour background needs [mask] (the stored grey PNG, in the version's EXIF-corrected
     * space). When it is missing or cannot be decoded this throws [ID_MASK_MSG_MISSING] before
     * the source is decoded; it never falls back to the original background.
     */
    fun renderIdPhotoSheetBitmap(
        bytes: ByteArray,
        state: IdPhotoState,
        mask: ByteArray?,
        outputScale: Int = 1,
        sheetForPrinter: Boolean = false,
    ): Bitmap = reportingDecodeFailure {
        val id = state.normalized()
        val background = idBackground(id.background)
        val scale = outputScale.coerceAtLeast(1)
        val maskBitmap = if (background.needsMask) decodeStoredIdMask(mask) else null
        try {
            val base = idPhotoLayout(idPhotoPreset(id.presetId))
            val layout = base.scaled(scale)
            val cell = renderIdCell(
                bytes,
                id,
                base,
                layout.cellWidth,
                layout.cellHeight,
                background.argb,
                maskBitmap,
                idFeather(id.feather).radiusPx(scale),
                id.featherInward,
                id.maskOffset * scale,
            )
            assembleIdSheet(cell, layout, sheetForPrinter)
        } finally {
            maskBitmap?.let { recycle(it) }
        }
    }

    fun renderIdPhotoSheetJpeg(
        bytes: ByteArray,
        state: IdPhotoState,
        mask: ByteArray?,
        outputScale: Int = 1,
        sheetForPrinter: Boolean = false,
        quality: Int = DEFAULT_PRINT_QUALITY,
    ): ByteArray = reportingDecodeFailure {
        val sheet = renderIdPhotoSheetBitmap(bytes, state, mask, outputScale, sheetForPrinter)
        try {
            encodeJpeg(sheet, quality)
        } finally {
            recycle(sheet)
        }
    }

    /**
     * Studio preview base: the whole version, EXIF-corrected, long edge at most [maxEdge],
     * composited over [background] through [mask]. The result is opaque. [featherSourcePx] is a
     * radius in full-size oriented source px (see `idFeatherSourcePx`); it is scaled to the
     * preview size here, so the feather is an approximation of the print one. [featherInward]
     * picks the inward feather, as the print does (EDID040-R07).
     */
    fun renderIdPreviewCutout(
        bytes: ByteArray,
        mask: ByteArray,
        background: Int,
        featherSourcePx: Float,
        featherInward: Boolean,
        offsetSourcePx: Float = 0f,
        maxEdge: Int = 2048,
    ): Bitmap = reportingDecodeFailure {
        val maskBitmap = decodeStoredIdMask(mask)
        try {
            val source = inspectJpeg(bytes)
            val out = previewDecodeSize(source.oriented.width, source.oriented.height, maxEdge)
            val raw = decodeSampled(bytes, longEdgeSampleSize(source.oriented.width, source.oriented.height, maxEdge))
            val image = try {
                Bitmap.createBitmap(out.width, out.height, Bitmap.Config.ARGB_8888)
            } catch (t: Throwable) {
                recycle(raw)
                throw t
            }
            try {
                try {
                    val exif = exifOrientationTransform(source.orientation, raw.width, raw.height)
                    val matrix = Matrix()
                    matrix.setValues(exif.matrixValues())
                    matrix.postConcat(
                        mapRectMatrix(0f, 0f, exif.outputWidth.toFloat(), exif.outputHeight.toFloat(), Rect(0, 0, out.width, out.height)),
                    )
                    Canvas(image).drawBitmap(raw, matrix, bitmapPaint())
                } finally {
                    recycle(raw)
                }
                val radius = if (featherSourcePx.isFinite() && featherSourcePx > 0f) {
                    Math.round(featherSourcePx * out.width / source.oriented.width.coerceAtLeast(1)).coerceAtLeast(1)
                } else {
                    0
                }
                val maskMatrix = mapRectMatrix(
                    0f,
                    0f,
                    maskBitmap.width.toFloat(),
                    maskBitmap.height.toFloat(),
                    Rect(0, 0, out.width, out.height),
                )
                // EDID040-R14: a non-zero shift stays at least 1 preview px.
                val offset = if (offsetSourcePx.isFinite() && offsetSourcePx != 0f) {
                    val px = Math.round(Math.abs(offsetSourcePx) * out.width / source.oriented.width.coerceAtLeast(1))
                        .coerceAtLeast(1)
                    if (offsetSourcePx < 0f) -px else px
                } else {
                    0
                }
                compositeThroughMask(image, maskBitmap, maskMatrix, background, radius, featherInward, offset)
                image
            } catch (t: Throwable) {
                recycle(image)
                throw t
            }
        } finally {
            recycle(maskBitmap)
        }
    }

    /**
     * What a mask request uploads. [expectedWidth]×[expectedHeight] is the size the reply is
     * checked against (the upload itself, or the padded canvas); [content] is where the photo
     * sits inside it, in those px.
     */
    data class IdMaskUpload(
        val jpeg: ByteArray,
        val expectedWidth: Int,
        val expectedHeight: Int,
        val content: IdRect,
        val padded: Boolean,
    )

    /**
     * The whole version, EXIF applied, no crop, no watermark, long edge at most 1536 and never
     * upscaled ([aiUploadSize]). Without [padToCanvas] the bytes are exactly [grokUploadJpeg].
     * With it (providers with fixed output sizes) the image is fitted, unscaled-up and centred,
     * onto the closest supported canvas ([openAiMaskCanvas]) with black padding, JPEG q85.
     */
    fun idMaskUpload(bytes: ByteArray, padToCanvas: Boolean): IdMaskUpload = reportingDecodeFailure {
        val source = inspectJpeg(bytes)
        val upload = aiUploadSize(source.oriented.width, source.oriented.height)
        if (!padToCanvas) {
            IdMaskUpload(
                jpeg = grokUploadJpeg(bytes),
                expectedWidth = upload.width,
                expectedHeight = upload.height,
                content = IdRect(0, 0, upload.width, upload.height),
                padded = false,
            )
        } else {
            val (canvasW, canvasH) = openAiMaskCanvas(upload.width, upload.height)
            val content = fitMaskContent(upload.width, upload.height, canvasW, canvasH)
            val raw = decodeSampled(
                bytes,
                longEdgeSampleSize(source.oriented.width, source.oriented.height, AI_UPLOAD_MAX_EDGE),
            )
            val jpeg = try {
                val out = Bitmap.createBitmap(canvasW, canvasH, Bitmap.Config.ARGB_8888)
                try {
                    val canvas = Canvas(out)
                    canvas.drawColor(Color.BLACK)
                    val exif = exifOrientationTransform(source.orientation, raw.width, raw.height)
                    val matrix = Matrix()
                    matrix.setValues(exif.matrixValues())
                    matrix.postConcat(
                        mapRectMatrix(
                            0f,
                            0f,
                            exif.outputWidth.toFloat(),
                            exif.outputHeight.toFloat(),
                            Rect(content.left, content.top, content.right, content.bottom),
                        ),
                    )
                    canvas.save()
                    try {
                        canvas.clipRect(content.left, content.top, content.right, content.bottom)
                        canvas.drawBitmap(raw, matrix, bitmapPaint())
                    } finally {
                        canvas.restore()
                    }
                    encodeJpeg(out, ID_MASK_UPLOAD_QUALITY)
                } finally {
                    recycle(out)
                }
            } finally {
                recycle(raw)
            }
            IdMaskUpload(
                jpeg = jpeg,
                expectedWidth = canvasW,
                expectedHeight = canvasH,
                content = content,
                padded = true,
            )
        }
    }

    /** Unsigned luma of the mask's content area, row-major, [width]×[height]. */
    data class IdMaskDecoded(val luma: ByteArray, val width: Int, val height: Int)

    /**
     * Decodes a provider reply (any format ImageDecoder reads) and checks it against [upload]:
     * aspect within 1% of the expected size, then the content rect (scaled to the reply size)
     * is cut out and only that area is checked ([validateIdMask]: colour, mid-grey, white share,
     * kept background, shading). Failures throw [IllegalStateException] with an `ID_MASK_MSG_*` message;
     * nothing is stretched or re-aligned.
     */
    fun decodeIdMask(response: ByteArray, upload: IdMaskUpload): IdMaskDecoded {
        var replyW = 0
        var replyH = 0
        val bitmap = try {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(ByteBuffer.wrap(response))) { decoder, info, _ ->
                decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE)
                replyW = info.size.width
                replyH = info.size.height
                val longEdge = maxOf(replyW, replyH)
                if (longEdge > ID_MASK_DECODE_MAX_EDGE) {
                    val scale = ID_MASK_DECODE_MAX_EDGE.toDouble() / longEdge
                    decoder.setTargetSize(
                        (replyW * scale).toInt().coerceAtLeast(1),
                        (replyH * scale).toInt().coerceAtLeast(1),
                    )
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: OutOfMemoryError) {
            error(ID_MASK_MSG_UNDECODABLE)
        } catch (_: Exception) {
            error(ID_MASK_MSG_UNDECODABLE)
        }
        try {
            if (bitmap.width <= 0 || bitmap.height <= 0) error(ID_MASK_MSG_UNDECODABLE)
            if (!idMaskAspectMatches(upload.expectedWidth, upload.expectedHeight, replyW, replyH)) {
                error(idMaskVerdictMessage(IdMaskVerdict.ASPECT_MISMATCH))
            }
            val sx = bitmap.width.toDouble() / upload.expectedWidth
            val sy = bitmap.height.toDouble() / upload.expectedHeight
            val left = Math.round(upload.content.left * sx).toInt().coerceIn(0, bitmap.width - 1)
            val top = Math.round(upload.content.top * sy).toInt().coerceIn(0, bitmap.height - 1)
            val right = Math.round(upload.content.right * sx).toInt().coerceIn(left + 1, bitmap.width)
            val bottom = Math.round(upload.content.bottom * sy).toInt().coerceIn(top + 1, bitmap.height)
            val w = right - left
            val h = bottom - top
            // Callers only catch Exception, so every post-decode allocation maps OOM to a message.
            var coloured = 0L
            val luma = try {
                val pixels = IntArray(w * h).also { bitmap.getPixels(it, 0, w, left, top, w, h) }
                ByteArray(w * h) { i ->
                    val px = pixels[i]
                    if (idMaskColoured(px)) coloured++
                    idMaskLuma(px).toByte()
                }
            } catch (_: OutOfMemoryError) {
                error(ID_MASK_MSG_UNDECODABLE)
            }
            val verdict = try {
                validateIdMask(luma, w, h, coloured)
            } catch (_: OutOfMemoryError) {
                error(ID_MASK_MSG_UNDECODABLE)
            }
            if (verdict != IdMaskVerdict.OK) error(idMaskVerdictMessage(verdict))
            return IdMaskDecoded(luma, w, h)
        } finally {
            recycle(bitmap)
        }
    }

    /** Lossless grey PNG of [mask], so a stored mask never picks up JPEG ringing. */
    fun encodeIdMaskPng(mask: IdMaskDecoded): ByteArray = reportingDecodeFailure {
        require(mask.width > 0 && mask.height > 0 && mask.luma.size >= mask.width * mask.height) { "蒙版尺寸无效" }
        val pixels = IntArray(mask.width * mask.height) { i ->
            val v = mask.luma[i].toInt() and 0xFF
            (0xFF shl 24) or (v shl 16) or (v shl 8) or v
        }
        val bitmap = Bitmap.createBitmap(mask.width, mask.height, Bitmap.Config.ARGB_8888)
        try {
            bitmap.setPixels(pixels, 0, mask.width, 0, 0, mask.width, mask.height)
            val stream = ByteArrayOutputStream()
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) error("PNG 编码失败")
            stream.toByteArray()
        } finally {
            recycle(bitmap)
        }
    }

    /**
     * On-device segmentation input (EDID040-R11): the whole version as [grokUploadJpeg] sees it
     * (EXIF applied, no crop, no watermark, long edge at most [AI_UPLOAD_MAX_EDGE], never
     * upscaled), as a software ARGB_8888 bitmap the caller recycles. No JPEG round trip.
     * [OutOfMemoryError] propagates so the local job can report it as a local failure.
     */
    fun localMaskInput(bytes: ByteArray): Bitmap {
        val source = inspectJpeg(bytes)
        val raw = decodeSampled(
            bytes,
            longEdgeSampleSize(source.oriented.width, source.oriented.height, AI_UPLOAD_MAX_EDGE),
        )
        try {
            val (outW, outH) = uploadSize(source.oriented)
            val out = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
            try {
                val exif = exifOrientationTransform(source.orientation, raw.width, raw.height)
                val matrix = Matrix()
                matrix.setValues(exif.matrixValues())
                matrix.postConcat(
                    mapRectMatrix(0f, 0f, exif.outputWidth.toFloat(), exif.outputHeight.toFloat(), Rect(0, 0, outW, outH)),
                )
                Canvas(out).drawBitmap(raw, matrix, bitmapPaint())
                return out
            } catch (t: Throwable) {
                recycle(out)
                throw t
            }
        } finally {
            recycle(raw)
        }
    }

    /** The stored mask, or [ID_MASK_MSG_MISSING] when it is absent or does not decode. */
    private fun decodeStoredIdMask(mask: ByteArray?): Bitmap {
        if (mask == null || mask.isEmpty()) error(ID_MASK_MSG_MISSING)
        val bitmap = try {
            BitmapFactory.decodeByteArray(mask, 0, mask.size)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        } ?: error(ID_MASK_MSG_MISSING)
        if (bitmap.width <= 0 || bitmap.height <= 0) {
            recycle(bitmap)
            error(ID_MASK_MSG_MISSING)
        }
        return bitmap
    }

    /**
     * One upright cell at [cellW]×[cellH]. Without a mask it is the source drawn on white with
     * [drawPlacement] (the cover is per cell). With one, the source is drawn on a transparent
     * cell and composited over [background], so the empty cell area becomes the background.
     * The source sample is freed before the mask work starts.
     */
    private fun renderIdCell(
        bytes: ByteArray,
        state: IdPhotoState,
        base: IdPhotoLayout,
        cellW: Int,
        cellH: Int,
        background: Int?,
        maskBitmap: Bitmap?,
        featherPx: Int,
        featherInward: Boolean,
        offsetPx: Int,
    ): Bitmap {
        val source = inspectJpeg(bytes)
        val placement = state.pose.toCellPlacement(
            base.cellWidth,
            base.cellHeight,
            source.oriented.width,
            source.oriented.height,
        )
        val sample = coverSampleSize(source.oriented.width, source.oriented.height, cellW, cellH, placement.zoom)
        val raw = decodeSampled(bytes, sample)
        var drawn: Bitmap? = null
        var orientedW = 0
        var orientedH = 0
        try {
            val exif = exifOrientationTransform(source.orientation, raw.width, raw.height)
            orientedW = exif.outputWidth
            orientedH = exif.outputHeight
            val target = Bitmap.createBitmap(cellW, cellH, Bitmap.Config.ARGB_8888)
            drawn = target
            val canvas = Canvas(target)
            if (maskBitmap == null || background == null) canvas.drawColor(Color.WHITE)
            drawPlacement(canvas, raw, exif, placement, cellW, cellH)
        } catch (t: Throwable) {
            drawn?.let { recycle(it) }
            throw t
        } finally {
            recycle(raw)
        }
        val cell = checkNotNull(drawn)
        if (maskBitmap == null || background == null) return cell
        try {
            // The mask is in oriented space (no EXIF), stretched x and y to the sampled oriented
            // size, then placed with the very matrix the source used.
            val maskMatrix = mapRectMatrix(
                0f,
                0f,
                maskBitmap.width.toFloat(),
                maskBitmap.height.toFloat(),
                Rect(0, 0, orientedW, orientedH),
            )
            placementMatrix(orientedW, orientedH, placement, cellW, cellH, maskMatrix)
            compositeThroughMask(cell, maskBitmap, maskMatrix, background, featherPx, featherInward, offsetPx)
            return cell
        } catch (t: Throwable) {
            recycle(cell)
            throw t
        }
    }

    /**
     * Draws [maskBitmap] through [maskMatrix] onto black at [target]'s size, turns its luma into
     * α, shifts its edge by [offsetPx] (EDID040-R14), feathers it by [featherPx] in [target] px
     * (inward when [featherInward], fine parts included, EDID040-R07) and composites
     * [target] over [background] in place. [maskBitmap] is recycled once drawn.
     */
    private fun compositeThroughMask(
        target: Bitmap,
        maskBitmap: Bitmap,
        maskMatrix: Matrix,
        background: Int,
        featherPx: Int,
        featherInward: Boolean,
        offsetPx: Int = 0,
    ) {
        val w = target.width
        val h = target.height
        val pixels = IntArray(w * h)
        val maskLayer = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(maskLayer)
            canvas.drawColor(Color.BLACK)
            canvas.drawBitmap(maskBitmap, maskMatrix, bitmapPaint())
            maskLayer.getPixels(pixels, 0, w, 0, 0, w, h)
        } finally {
            recycle(maskLayer)
        }
        recycle(maskBitmap)
        val alpha = ByteArray(w * h) { i -> idMaskLumaToAlpha(idMaskLuma(pixels[i])).toByte() }
        val shifted = if (offsetPx != 0) offsetAlpha(alpha, w, h, offsetPx) else alpha
        val feathered = featherAlphaFor(shifted, w, h, featherPx, featherInward)
        target.getPixels(pixels, 0, w, 0, 0, w, h)
        compositeIdPixels(pixels, feathered, background)
        target.setPixels(pixels, 0, w, 0, 0, w, h)
    }

    /**
     * White landscape sheet: cut lines first (they sit outside every slot), then [cell] copied
     * into each slot at whole-px offsets, so slots never spill. [cell] is recycled before the
     * optional printer turn.
     */
    private fun assembleIdSheet(cell: Bitmap, layout: IdPhotoLayout, sheetForPrinter: Boolean): Bitmap {
        val sheet = try {
            Bitmap.createBitmap(layout.sheetWidth, layout.sheetHeight, Bitmap.Config.ARGB_8888)
        } catch (t: Throwable) {
            recycle(cell)
            throw t
        }
        var handedOff = false
        try {
            try {
                val canvas = Canvas(sheet)
                canvas.drawColor(Color.WHITE)
                val linePaint = Paint().apply {
                    color = ID_CUT_LINE_COLOR
                    style = Paint.Style.FILL
                    isAntiAlias = false
                }
                for (line in layout.cutLines) {
                    canvas.drawRect(
                        line.left.toFloat(),
                        line.top.toFloat(),
                        line.right.toFloat(),
                        line.bottom.toFloat(),
                        linePaint,
                    )
                }
                val turn = Matrix()
                for (slot in layout.slots) {
                    if (layout.rotated) {
                        // Counter-clockwise: the cell's top lands on the slot's left edge, so the
                        // clockwise printer turn brings every head upright.
                        turn.reset()
                        turn.postRotate(-90f)
                        turn.postTranslate(slot.left.toFloat(), (slot.top + layout.cellWidth).toFloat())
                        canvas.drawBitmap(cell, turn, null)
                    } else {
                        canvas.drawBitmap(cell, slot.left.toFloat(), slot.top.toFloat(), null)
                    }
                }
            } finally {
                recycle(cell)
            }
            val result = finishSheet(
                sheet,
                landscape = true,
                ontoPortraitSheet = sheetForPrinter,
                watermark = null,
                photo = null,
                context = null,
            )
            handedOff = true
            return result
        } finally {
            if (!handedOff) recycle(sheet)
        }
    }

    /** Rec. 601 luma of an unpremultiplied ARGB pixel, flattened over black. */
    private fun idMaskLuma(argb: Int): Int {
        val a = (argb ushr 24) and 0xFF
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        val y = (299 * r + 587 * g + 114 * b + 500) / 1000
        return if (a == 255) y else (y * a + 127) / 255
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
        val matrix = Matrix()
        matrix.setValues(exif.matrixValues())
        placementMatrix(exif.outputWidth, exif.outputHeight, placement, destW, destH, matrix)
        canvas.drawBitmap(source, matrix, bitmapPaint())
    }

    /**
     * Oriented space → [destW]×[destH] for [placement], post-applied to [matrix] (identity by
     * default) one step at a time, so [drawPlacement] performs the same operations in the same
     * order as before. Cover uses [orientedW]×[orientedH]; pan is rescaled from the placement's
     * frame to dest px.
     */
    private fun placementMatrix(
        orientedW: Int,
        orientedH: Int,
        placement: ViewportPlacement,
        destW: Int,
        destH: Int,
        matrix: Matrix = Matrix(),
    ): Matrix {
        val cover = coverScale(destW.toFloat(), destH.toFloat(), orientedW, orientedH)
        val scale = cover * clampZoom(placement.zoom)
        val frameW = placement.frameWidth.takeIf { it > 0f } ?: destW.toFloat()
        val frameH = placement.frameHeight.takeIf { it > 0f } ?: destH.toFloat()
        val panX = placement.offsetX * destW / frameW
        val panY = placement.offsetY * destH / frameH
        matrix.postTranslate(-orientedW / 2f, -orientedH / 2f)
        matrix.postScale(scale, scale)
        matrix.postRotate(placement.rotationDegrees)
        matrix.postTranslate(destW / 2f + panX, destH / 2f + panY)
        return matrix
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
        val size = aiUploadSize(oriented.width, oriented.height)
        return size.width to size.height
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
