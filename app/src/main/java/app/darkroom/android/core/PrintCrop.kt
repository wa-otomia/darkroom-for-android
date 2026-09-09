package app.darkroom.android.core

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

const val PRINT_WIDTH = 1040
const val PRINT_HEIGHT = 1560
const val PRINT_ASPECT = PRINT_WIDTH.toDouble() / PRINT_HEIGHT.toDouble()
const val PRINT_ASPECT_LANDSCAPE = PRINT_HEIGHT.toDouble() / PRINT_WIDTH.toDouble()
const val DEFAULT_PRINT_QUALITY = 90
const val THUMB_WIDTH = 720

/** Zoom 1 covers the paper frame; below 1 letterboxes on the sheet. */
const val MIN_PRINT_ZOOM = 0.4f
const val MAX_PRINT_ZOOM = 3f

/** The sheet is always portrait, so a landscape frame is turned one quarter clockwise to fill it. */
const val PRINT_SHEET_QUARTER_TURNS = 1

data class OrientedSize(val width: Int, val height: Int)

fun orientedDimensions(width: Int, height: Int, orientation: Int = 1): OrientedSize {
    return if (orientation in 5..8) OrientedSize(height, width) else OrientedSize(width, height)
}

/** Landscape sheet when the decoded, EXIF-corrected image is wider than it is tall. Square is portrait. */
fun defaultLandscape(width: Int, height: Int): Boolean = width > height

/**
 * Pixel size of the image a print job will read: the chosen edit when [source] is an
 * edit id with stored dimensions, otherwise the original [PhotoMeta] size.
 */
fun printImageSize(photo: PhotoMeta?, source: String): OrientedSize? {
    if (photo == null) return null
    val edit = source.takeIf { it.isNotBlank() && it != "original" }?.let { id ->
        photo.edits.find { it.id == id }?.takeIf { it.width > 0 && it.height > 0 }
    }
    val width = edit?.width ?: photo.width
    val height = edit?.height ?: photo.height
    if (width <= 0 || height <= 0) return null
    return OrientedSize(width, height)
}

/** Studio (or any caller) can pass [explicit]; otherwise fall back to [defaultLandscape], or portrait. */
fun resolvePrintLandscape(explicit: Boolean?, size: OrientedSize?): Boolean =
    explicit ?: size?.let { defaultLandscape(it.width, it.height) } ?: false

fun cropAspect(landscape: Boolean): Double = if (landscape) PRINT_ASPECT_LANDSCAPE else PRINT_ASPECT

fun normalizeQuarterTurns(quarterTurns: Int): Int = ((quarterTurns % 4) + 4) % 4

/** Size of an image after turning it [quarterTurns] quarters clockwise. */
fun rotatedSize(width: Int, height: Int, quarterTurns: Int): OrientedSize {
    return if (normalizeQuarterTurns(quarterTurns) % 2 == 1) {
        OrientedSize(height, width)
    } else {
        OrientedSize(width, height)
    }
}

fun clampZoom(zoom: Float): Float = zoom.coerceIn(MIN_PRINT_ZOOM, MAX_PRINT_ZOOM)

fun normalizeRotationDegrees(degrees: Float): Float {
    val wrapped = degrees % 360f
    return if (wrapped < 0f) wrapped + 360f else wrapped
}

/** Right-angle turns when [degrees] is on a 90° multiple; otherwise null. */
fun exactQuarterTurns(degrees: Float, epsilon: Float = 0.5f): Int? {
    val d = normalizeRotationDegrees(degrees)
    val nearest = Math.round(d / 90f)
    val snapped = ((nearest % 4) + 4) % 4
    val dist = minOf(abs(d - snapped * 90f), abs(d - 360f))
    return if (dist <= epsilon) snapped % 4 else null
}

/** Axis-aligned box of a [width]×[height] rectangle turned [rotationDegrees]. */
fun rotatedAabbSize(width: Float, height: Float, rotationDegrees: Float): Pair<Float, Float> {
    val turns = exactQuarterTurns(rotationDegrees, epsilon = 0.01f)
    if (turns != null) {
        return if (turns % 2 == 1) height to width else width to height
    }
    val rad = Math.toRadians(rotationDegrees.toDouble())
    val c = abs(cos(rad)).toFloat()
    val s = abs(sin(rad)).toFloat()
    return (width * c + height * s) to (width * s + height * c)
}

/** Turns a viewport pan offset (screen px) along with the image it pans. */
fun rotatePan(offsetX: Float, offsetY: Float, quarterTurns: Int): Pair<Float, Float> {
    return when (normalizeQuarterTurns(quarterTurns)) {
        1 -> -offsetY to offsetX
        2 -> -offsetX to -offsetY
        3 -> offsetY to -offsetX
        else -> offsetX to offsetY
    }
}

fun rotatePan(offsetX: Float, offsetY: Float, degrees: Float): Pair<Float, Float> {
    val turns = exactQuarterTurns(degrees, epsilon = 0.01f)
    if (turns != null) return rotatePan(offsetX, offsetY, turns)
    val rad = Math.toRadians(degrees.toDouble())
    val cos = cos(rad).toFloat()
    val sin = sin(rad).toFloat()
    return (offsetX * cos - offsetY * sin) to (offsetX * sin + offsetY * cos)
}

/**
 * Maps a crop taken on an [imageWidth]×[imageHeight] image onto the same image
 * turned [quarterTurns] quarters clockwise.
 */
fun rotateCrop(crop: PixelCrop, imageWidth: Int, imageHeight: Int, quarterTurns: Int): PixelCrop {
    return when (normalizeQuarterTurns(quarterTurns)) {
        1 -> PixelCrop(imageHeight - crop.y - crop.height, crop.x, crop.height, crop.width)
        2 -> PixelCrop(
            imageWidth - crop.x - crop.width,
            imageHeight - crop.y - crop.height,
            crop.width,
            crop.height,
        )
        3 -> PixelCrop(crop.y, imageWidth - crop.x - crop.width, crop.height, crop.width)
        else -> crop
    }
}

/**
 * Inverse of [rotateCrop]: takes a crop framed on the turned image and expresses it
 * on the unturned source of [sourceWidth]×[sourceHeight].
 */
fun cropInSourceSpace(
    crop: PixelCrop,
    sourceWidth: Int,
    sourceHeight: Int,
    quarterTurns: Int,
): PixelCrop {
    val working = rotatedSize(sourceWidth, sourceHeight, quarterTurns)
    return rotateCrop(crop, working.width, working.height, -quarterTurns)
}

fun formatCropSpec(crop: PixelCrop?): String =
    if (crop == null) "" else "${crop.x},${crop.y},${crop.width},${crop.height}"

fun parseCropSpec(spec: String): PixelCrop? {
    if (spec.isBlank()) return null
    val parts = spec.split(',')
    if (parts.size != 4) return null
    val nums = parts.map { it.trim().toIntOrNull() ?: return null }
    if (nums[2] <= 0 || nums[3] <= 0) return null
    return PixelCrop(nums[0], nums[1], nums[2], nums[3])
}

fun centeredAspectCrop(
    width: Int,
    height: Int,
    aspect: Double = PRINT_ASPECT,
): PixelCrop {
    if (width == 0 || height == 0) {
        return PixelCrop(0, 0, width.coerceAtLeast(1), height.coerceAtLeast(1))
    }
    val imageAspect = width.toDouble() / height.toDouble()
    return if (imageAspect > aspect) {
        val cropHeight = height
        val cropWidth = Math.round(height * aspect).toInt()
        PixelCrop(
            x = Math.round((width - cropWidth) / 2.0).toInt(),
            y = 0,
            width = cropWidth,
            height = cropHeight,
        )
    } else {
        val cropWidth = width
        val cropHeight = Math.round(width / aspect).toInt()
        PixelCrop(
            x = 0,
            y = Math.round((height - cropHeight) / 2.0).toInt(),
            width = cropWidth,
            height = cropHeight,
        )
    }
}

fun parsePrintFit(value: String?, fallback: PrintFit = PrintFit.COVER): PrintFit {
    return if (value == "contain") PrintFit.CONTAIN else fallback
}

fun printFitWire(fit: PrintFit): String = if (fit == PrintFit.CONTAIN) "contain" else "cover"

fun coverScale(frameWidth: Float, frameHeight: Float, imageWidth: Int, imageHeight: Int): Float {
    if (frameWidth <= 0f || frameHeight <= 0f || imageWidth <= 0 || imageHeight <= 0) return 1f
    return maxOf(frameWidth / imageWidth, frameHeight / imageHeight)
}

fun largestFrameSize(
    containerWidth: Float,
    containerHeight: Float,
    aspect: Float = PRINT_ASPECT.toFloat(),
): Pair<Float, Float> {
    if (containerWidth <= 0f || containerHeight <= 0f || aspect <= 0f) return 0f to 0f
    val containerAspect = containerWidth / containerHeight
    return if (containerAspect > aspect) {
        val height = containerHeight
        (height * aspect) to height
    } else {
        val width = containerWidth
        width to (width / aspect)
    }
}

fun clampPan(
    offsetX: Float,
    offsetY: Float,
    zoom: Float,
    frameWidth: Float,
    frameHeight: Float,
    imageWidth: Int,
    imageHeight: Int,
    rotationDegrees: Float = 0f,
): Pair<Float, Float> {
    val z = clampZoom(zoom)
    val cover = coverScale(frameWidth, frameHeight, imageWidth, imageHeight)
    val displayedW = imageWidth * cover * z
    val displayedH = imageHeight * cover * z
    val (aabbW, aabbH) = rotatedAabbSize(displayedW, displayedH, rotationDegrees)
    val maxX = abs(aabbW - frameWidth) / 2f
    val maxY = abs(aabbH - frameHeight) / 2f
    return offsetX.coerceIn(-maxX, maxX) to offsetY.coerceIn(-maxY, maxY)
}

/**
 * Pan snap targets in image-centre offset space (same units as [ViewportPlacement.offsetX]).
 *
 * Each axis gets a centre line at `0`, image-edge-to-frame-edge at `±|disp − frame|/2`,
 * and image-edge-to-safe-line at `±(|disp − frame|/2 − inset)`. Displayed size uses
 * cover × zoom, then [rotatedAabbSize]. Cover (`disp ≥ frame`) and letterbox
 * (`disp < frame`) use the matching signed form from the snap plan; magnitudes match
 * [clampPan] extrema.
 */
fun panSnapGuides(
    frameW: Float,
    frameH: Float,
    imageW: Int,
    imageH: Int,
    zoom: Float,
    rotationDegrees: Float,
    inset: Float,
): Pair<List<SnapGuide>, List<SnapGuide>> {
    if (frameW <= 0f || frameH <= 0f || imageW <= 0 || imageH <= 0) {
        return listOf(SnapGuide(GUIDE_CENTER_X, 0f)) to listOf(SnapGuide(GUIDE_CENTER_Y, 0f))
    }
    val z = clampZoom(zoom)
    val cover = coverScale(frameW, frameH, imageW, imageH)
    val dispW = imageW * cover * z
    val dispH = imageH * cover * z
    val (aabbW, aabbH) = rotatedAabbSize(dispW, dispH, rotationDegrees)
    return axisPanGuides(aabbW, frameW, inset, xAxis = true) to
        axisPanGuides(aabbH, frameH, inset, xAxis = false)
}

/** Right-angle rotation targets: 0 / 90 / 180 / 270. */
fun rotationSnapGuides(): List<SnapGuide> = listOf(
    SnapGuide(GUIDE_ROT_0, 0f),
    SnapGuide(GUIDE_ROT_90, 90f),
    SnapGuide(GUIDE_ROT_180, 180f),
    SnapGuide(GUIDE_ROT_270, 270f),
)

private fun axisPanGuides(disp: Float, frame: Float, inset: Float, xAxis: Boolean): List<SnapGuide> {
    val center = SnapGuide(if (xAxis) GUIDE_CENTER_X else GUIDE_CENTER_Y, 0f)
    val framePosId = if (xAxis) GUIDE_FRAME_X_POS else GUIDE_FRAME_Y_POS
    val frameNegId = if (xAxis) GUIDE_FRAME_X_NEG else GUIDE_FRAME_Y_NEG
    val safePosId = if (xAxis) GUIDE_SAFE_X_POS else GUIDE_SAFE_Y_POS
    val safeNegId = if (xAxis) GUIDE_SAFE_X_NEG else GUIDE_SAFE_Y_NEG
    return if (disp >= frame) {
        val edge = (disp - frame) / 2f
        listOf(
            center,
            SnapGuide(framePosId, edge),
            SnapGuide(frameNegId, -edge),
            SnapGuide(safePosId, edge - inset),
            SnapGuide(safeNegId, -(edge - inset)),
        )
    } else {
        val edge = (frame - disp) / 2f
        listOf(
            center,
            SnapGuide(framePosId, edge),
            SnapGuide(frameNegId, -edge),
            SnapGuide(safePosId, edge - inset),
            SnapGuide(safeNegId, -(edge - inset)),
        )
    }
}

fun viewportCrop(
    frameWidth: Float,
    frameHeight: Float,
    imageWidth: Int,
    imageHeight: Int,
    zoom: Float,
    offsetX: Float,
    offsetY: Float,
): PixelCrop {
    if (frameWidth <= 0f || frameHeight <= 0f || imageWidth <= 0 || imageHeight <= 0) {
        return PixelCrop(0, 0, imageWidth.coerceAtLeast(1), imageHeight.coerceAtLeast(1))
    }
    val z = zoom.coerceAtLeast(1f)
    val (ox, oy) = clampPan(offsetX, offsetY, z, frameWidth, frameHeight, imageWidth, imageHeight)
    val cover = coverScale(frameWidth, frameHeight, imageWidth, imageHeight)
    val displayedW = imageWidth * cover * z
    val displayedH = imageHeight * cover * z
    val left = (frameWidth - displayedW) / 2f + ox
    val top = (frameHeight - displayedH) / 2f + oy
    val x = Math.round((0f - left) / displayedW * imageWidth).coerceIn(0, imageWidth - 1)
    val y = Math.round((0f - top) / displayedH * imageHeight).coerceIn(0, imageHeight - 1)
    val w = Math.round(frameWidth / displayedW * imageWidth).coerceIn(1, imageWidth)
    val h = Math.round(frameHeight / displayedH * imageHeight).coerceIn(1, imageHeight)
    return PixelCrop(x, y, minOf(w, imageWidth - x), minOf(h, imageHeight - y))
}

/**
 * How the source sits in the paper frame: zoom (1 = cover), pan, and continuous rotation.
 *
 * [toPixelCrop] is only a cover-crop when the image fills the frame axis-aligned.
 * Zoom below 1 or a non-90° angle must be drawn onto the white sheet instead.
 */
data class ViewportPlacement(
    val zoom: Float,
    val offsetX: Float,
    val offsetY: Float,
    val rotationDegrees: Float,
    val frameWidth: Float,
    val frameHeight: Float,
    val imageWidth: Int,
    val imageHeight: Int,
) {
    fun usesLetterbox(): Boolean =
        zoom < 1f || exactQuarterTurns(rotationDegrees) == null

    fun toPixelCrop(): PixelCrop? {
        if (usesLetterbox()) return null
        val turns = exactQuarterTurns(rotationDegrees) ?: return null
        val working = rotatedSize(imageWidth, imageHeight, turns)
        return viewportCrop(frameWidth, frameHeight, working.width, working.height, zoom, offsetX, offsetY)
    }
}

fun viewportPlacement(
    frameWidth: Float,
    frameHeight: Float,
    imageWidth: Int,
    imageHeight: Int,
    zoom: Float,
    offsetX: Float,
    offsetY: Float,
    rotationDegrees: Float = 0f,
): ViewportPlacement {
    val z = clampZoom(zoom)
    val rotation = normalizeRotationDegrees(rotationDegrees)
    val (ox, oy) = clampPan(
        offsetX,
        offsetY,
        z,
        frameWidth,
        frameHeight,
        imageWidth,
        imageHeight,
        rotation,
    )
    return ViewportPlacement(
        zoom = z,
        offsetX = ox,
        offsetY = oy,
        rotationDegrees = rotation,
        frameWidth = frameWidth,
        frameHeight = frameHeight,
        imageWidth = imageWidth,
        imageHeight = imageHeight,
    )
}

fun clampCrop(crop: PixelCrop, width: Int, height: Int): PixelCrop {
    val x = crop.x.coerceAtLeast(0)
    val y = crop.y.coerceAtLeast(0)
    val w = crop.width.coerceAtLeast(1)
    val h = crop.height.coerceAtLeast(1)
    return PixelCrop(
        x = x.coerceAtMost((width - 1).coerceAtLeast(0)),
        y = y.coerceAtMost((height - 1).coerceAtLeast(0)),
        width = w.coerceAtMost(width - x).coerceAtLeast(1),
        height = h.coerceAtMost(height - y).coerceAtLeast(1),
    )
}

fun PixelCrop.toNormalized(imageWidth: Int, imageHeight: Int): NormalizedCrop {
    val w = imageWidth.coerceAtLeast(1).toDouble()
    val h = imageHeight.coerceAtLeast(1).toDouble()
    return NormalizedCrop(
        x = x / w,
        y = y / h,
        width = width / w,
        height = height / h,
    )
}

fun NormalizedCrop.toPixel(imageWidth: Int, imageHeight: Int): PixelCrop {
    val iw = imageWidth.coerceAtLeast(1)
    val ih = imageHeight.coerceAtLeast(1)
    val x = Math.round(this.x * iw).toInt().coerceIn(0, (iw - 1).coerceAtLeast(0))
    val y = Math.round(this.y * ih).toInt().coerceIn(0, (ih - 1).coerceAtLeast(0))
    val w = Math.round(this.width * iw).toInt().coerceIn(1, iw)
    val h = Math.round(this.height * ih).toInt().coerceIn(1, ih)
    return PixelCrop(x, y, minOf(w, iw - x), minOf(h, ih - y))
}

fun resolveCropOnBitmap(
    crop: PixelCrop?,
    cropOnWidth: Int,
    cropOnHeight: Int,
    bitmapWidth: Int,
    bitmapHeight: Int,
): PixelCrop? {
    if (crop == null) return null
    if (cropOnWidth == bitmapWidth && cropOnHeight == bitmapHeight) return crop
    return crop.toNormalized(cropOnWidth, cropOnHeight).toPixel(bitmapWidth, bitmapHeight)
}

fun isJpegName(filename: String): Boolean {
    val ext = filename.substringAfterLast('.', "").lowercase()
    return ext == "jpg" || ext == "jpeg"
}

private val INGESTIBLE_UPLOAD_EXTS = setOf("jpg", "jpeg", "png", "heic", "heif", "webp")

fun isIngestibleUploadName(fileName: String): Boolean {
    val base = fileName.replace('\\', '/').substringAfterLast('/')
    if (base.isEmpty() || base.startsWith(".")) return false
    if (base.endsWith(".tmp", ignoreCase = true) || base.endsWith(".part", ignoreCase = true)) return false
    val ext = base.substringAfterLast('.', "").lowercase()
    return ext in INGESTIBLE_UPLOAD_EXTS
}

fun isJpegBytes(buf: ByteArray): Boolean = buf.size >= 2 && buf[0] == 0xFF.toByte() && buf[1] == 0xD8.toByte()

/** How far from the end to look for JPEG EOI. Cameras sometimes append trailing bytes. */
const val JPEG_EOI_TAIL_BYTES = 4096

/** True when `FF D9` appears in the last [tailBytes] of [bytes]. */
fun hasJpegEoi(bytes: ByteArray, tailBytes: Int = JPEG_EOI_TAIL_BYTES): Boolean {
    if (bytes.size < 2) return false
    val start = (bytes.size - tailBytes.coerceAtLeast(2)).coerceAtLeast(0)
    var i = start
    while (i < bytes.size - 1) {
        if (bytes[i] == 0xFF.toByte() && bytes[i + 1] == 0xD9.toByte()) return true
        i++
    }
    return false
}

/** SOI plus the marker that always follows it. Used to skip a re-encode. */
fun looksLikeJpeg(buf: ByteArray): Boolean =
    buf.size >= 3 &&
        buf[0] == 0xFF.toByte() &&
        buf[1] == 0xD8.toByte() &&
        buf[2] == 0xFF.toByte()

/**
 * Archive helper for stills that are not camera JPEGs. Already-JPEG bytes are
 * returned as-is; anything else is handed to [transcode], which must produce
 * [looksLikeJpeg] bytes or the existing decode error is thrown.
 */
fun ensureJpegBytes(bytes: ByteArray, transcode: (ByteArray) -> ByteArray): ByteArray {
    if (looksLikeJpeg(bytes)) return bytes
    val jpeg = transcode(bytes)
    if (!looksLikeJpeg(jpeg)) error("无法解码 JPEG")
    return jpeg
}

fun jpegSofMarker(jpeg: ByteArray): Int {
    var i = 2
    while (i < jpeg.size - 1) {
        if (jpeg[i] != 0xFF.toByte()) error("bad segment at $i")
        val m = jpeg[i + 1].toInt() and 0xFF
        if (m == 0xC0 || m == 0xC1 || m == 0xC2 || m == 0xC3) return m
        if (m == 0xD8 || m == 0xD9 || m in 0xD0..0xD7) {
            i += 2
            continue
        }
        if (i + 3 >= jpeg.size) break
        val len = ((jpeg[i + 2].toInt() and 0xFF) shl 8) or (jpeg[i + 3].toInt() and 0xFF)
        i += 2 + len
    }
    error("no SOF marker found")
}
