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
import io.github.wa_otomia.darkroom.core.coverScale
import io.github.wa_otomia.darkroom.core.cropAspect
import io.github.wa_otomia.darkroom.core.cropInSourceSpace
import io.github.wa_otomia.darkroom.core.exportSheetSize
import io.github.wa_otomia.darkroom.core.sheetQuarterTurns
import io.github.wa_otomia.darkroom.core.ensureJpegBytes
import io.github.wa_otomia.darkroom.core.exactQuarterTurns
import io.github.wa_otomia.darkroom.core.normalizeQuarterTurns
import io.github.wa_otomia.darkroom.core.orientedDimensions
import io.github.wa_otomia.darkroom.core.resolveCropOnBitmap
import io.github.wa_otomia.darkroom.core.rotatedSize
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

    fun thumbnailJpeg(bytes: ByteArray, quality: Int = 72): ByteArray {
        val bmp = decodeRotated(bytes)
        val w = bmp.width.coerceAtMost(THUMB_WIDTH)
        val h = if (bmp.width == 0) bmp.height else (bmp.height * w) / bmp.width
        val scaled = if (w == bmp.width) bmp else Bitmap.createScaledBitmap(bmp, w.coerceAtLeast(1), h.coerceAtLeast(1), true)
        return encodeJpeg(scaled, quality).also {
            if (scaled != bmp) scaled.recycle()
            bmp.recycle()
        }
    }

    fun grokUploadJpeg(bytes: ByteArray): ByteArray {
        val bmp = decodeRotated(bytes)
        val max = 1536
        val scale = minOf(1f, max.toFloat() / maxOf(bmp.width, bmp.height))
        val w = (bmp.width * scale).toInt().coerceAtLeast(1)
        val h = (bmp.height * scale).toInt().coerceAtLeast(1)
        val scaled = if (w == bmp.width && h == bmp.height) bmp else Bitmap.createScaledBitmap(bmp, w, h, true)
        return encodeJpeg(scaled, 85).also {
            if (scaled != bmp) scaled.recycle()
            bmp.recycle()
        }
    }

    /**
     * [crop] is expressed on the photo as the user framed it: EXIF applied by [decodeRotated],
     * then turned [rotateQuarters] quarters clockwise. The rect is mapped back onto the decoded
     * pixels so only the cut has to be turned, and [landscape] adds the extra quarter that lays a
     * 3:2 frame onto the portrait sheet.
     *
     * When [placement] is set (or [rotationDegrees] is not a right angle), the source is drawn
     * onto the white sheet at that zoom/pan/angle so letterbox margins are kept.
     *
     * [watermark] is painted after the photo and only on this print/export path.
     * [cropToJpeg] and [grokUploadJpeg] never accept a watermark. [outputScale] multiplies
     * the 1040×1560 sheet (use `2` for a 2080×3120 album export). [context] is required
     * to tint SNS [android.graphics.drawable.VectorDrawable]s; date-only marks still draw without it.
     *
     * [sheetForPrinter] (default true) quarter-turns a landscape frame onto the portrait
     * printer sheet. Export passes false so 横幅 stays 3120×2080 and 竖幅 stays 2080×3120.
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
    ): ByteArray {
        val bmp = decodeRotated(bytes)
        if (placement != null || exactQuarterTurns(rotationDegrees) == null) {
            val placed = placement ?: implicitPlacement(bmp, rotationDegrees, landscape)
            val sheet = renderPlacementSheet(
                bmp,
                placed,
                landscape,
                ontoPortraitSheet = sheetForPrinter,
                outputScale = outputScale,
                watermark = watermark,
                photo = photo,
                context = context,
            )
            return encodeJpeg(sheet, quality).also {
                if (sheet !== bmp) sheet.recycle()
                bmp.recycle()
            }
        }
        val framed = rotatedSize(bmp.width, bmp.height, rotateQuarters)
        val resolved = resolveCropOnBitmap(
            crop,
            cropImageWidth ?: framed.width,
            cropImageHeight ?: framed.height,
            framed.width,
            framed.height,
        )
        val framedCrop = resolved
            ?: if (fit == PrintFit.COVER) centeredAspectCrop(framed.width, framed.height, cropAspect(landscape)) else null
        val cut = if (framedCrop != null) {
            val c = sourceCut(framedCrop, framed, bmp.width, bmp.height, rotateQuarters)
            Bitmap.createBitmap(bmp, c.x, c.y, c.width, c.height)
        } else {
            bmp
        }
        val sheet = rotateBitmap(cut, rotateQuarters + sheetQuarterTurns(landscape, sheetForPrinter))
        val out = preparePrintBitmap(
            sheet,
            if (framedCrop != null) PrintFit.COVER else fit,
            outputScale,
            landscape = landscape && !sheetForPrinter,
        )
        applyWatermark(out, watermark, photo, out.width, out.height, context)
        return encodeJpeg(out, quality).also {
            if (out !== sheet) out.recycle()
            if (sheet !== cut) sheet.recycle()
            if (cut !== bmp) cut.recycle()
            bmp.recycle()
        }
    }

    /**
     * Cuts what the user framed and leaves it upright; the sheet turn happens at print time.
     * Never accepts or draws a watermark — AI upload must stay unmarked.
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
    ): ByteArray {
        val bmp = decodeRotated(bytes)
        if (placement != null || exactQuarterTurns(rotationDegrees) == null) {
            val placed = placement ?: implicitPlacement(bmp, rotationDegrees, landscape)
            val framed = renderPlacementSheet(bmp, placed, landscape, ontoPortraitSheet = false)
            return encodeJpeg(framed, quality).also {
                if (framed !== bmp) framed.recycle()
                bmp.recycle()
            }
        }
        val framed = rotatedSize(bmp.width, bmp.height, rotateQuarters)
        val framedCrop = resolveCropOnBitmap(
            crop,
            cropImageWidth ?: framed.width,
            cropImageHeight ?: framed.height,
            framed.width,
            framed.height,
        ) ?: centeredAspectCrop(framed.width, framed.height, cropAspect(landscape))
        val c = sourceCut(framedCrop, framed, bmp.width, bmp.height, rotateQuarters)
        val cut = Bitmap.createBitmap(bmp, c.x, c.y, c.width, c.height)
        val turned = rotateBitmap(cut, rotateQuarters)
        return encodeJpeg(turned, quality).also {
            if (turned !== cut) turned.recycle()
            if (cut !== bmp) cut.recycle()
            bmp.recycle()
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

    private fun implicitPlacement(bmp: Bitmap, rotationDegrees: Float, landscape: Boolean): ViewportPlacement {
        val destW = if (landscape) PRINT_HEIGHT else PRINT_WIDTH
        val destH = if (landscape) PRINT_WIDTH else PRINT_HEIGHT
        return ViewportPlacement(
            zoom = 1f,
            offsetX = 0f,
            offsetY = 0f,
            rotationDegrees = rotationDegrees,
            frameWidth = destW.toFloat(),
            frameHeight = destH.toFloat(),
            imageWidth = bmp.width,
            imageHeight = bmp.height,
        )
    }

    /**
     * Draws [placement] onto a white frame-sized bitmap. [preparePrintBitmap] is not used:
     * a second cover-fit would erase letterbox margins.
     */
    private fun renderPlacementSheet(
        source: Bitmap,
        placement: ViewportPlacement,
        landscape: Boolean,
        ontoPortraitSheet: Boolean,
        outputScale: Int = 1,
        watermark: WatermarkSettings? = null,
        photo: PhotoMeta? = null,
        context: Context? = null,
    ): Bitmap {
        val dest = exportSheetSize(landscape, outputScale)
        val destW = dest.width
        val destH = dest.height
        val framed = Bitmap.createBitmap(destW, destH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(framed)
        canvas.drawColor(Color.WHITE)
        drawPlacement(canvas, source, placement, destW, destH)
        applyWatermark(framed, watermark, photo, destW, destH, context)
        if (sheetQuarterTurns(landscape, ontoPortraitSheet) == 0) return framed
        val sheet = rotateBitmap(framed, sheetQuarterTurns(landscape, ontoPortraitSheet))
        if (sheet !== framed) framed.recycle()
        return sheet
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

    private fun drawPlacement(canvas: Canvas, source: Bitmap, placement: ViewportPlacement, destW: Int, destH: Int) {
        val cover = coverScale(destW.toFloat(), destH.toFloat(), source.width, source.height)
        val scale = cover * clampZoom(placement.zoom)
        val frameW = placement.frameWidth.takeIf { it > 0f } ?: destW.toFloat()
        val frameH = placement.frameHeight.takeIf { it > 0f } ?: destH.toFloat()
        val panX = placement.offsetX * destW / frameW
        val panY = placement.offsetY * destH / frameH
        val matrix = Matrix()
        matrix.postTranslate(-source.width / 2f, -source.height / 2f)
        matrix.postScale(scale, scale)
        matrix.postRotate(placement.rotationDegrees)
        matrix.postTranslate(destW / 2f + panX, destH / 2f + panY)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(source, matrix, paint)
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
}
