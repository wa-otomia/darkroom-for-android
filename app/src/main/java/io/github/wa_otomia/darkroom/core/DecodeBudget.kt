package io.github.wa_otomia.darkroom.core

/**
 * Long-edge cap for the studio preview decode. Coil must not be asked for the original raster:
 * a portrait A7CR software bitmap exceeds the 100 MiB draw limit.
 */
const val PREVIEW_MAX_EDGE = 4096

/** Long-edge cap for AI upload JPEG. Matches the historical upload limit of 1536. */
const val AI_UPLOAD_MAX_EDGE = 1536

/**
 * Largest power of two that does not exceed [maxFactor], and never less than 1.
 *
 * A factor below 2 (including non-finite or negative input) stays at 1, so a decode never
 * asks for more pixels than the source. The result fits in a positive [Int] (at most 2^30).
 */
fun sampleSizeAtMost(maxFactor: Double): Int {
    if (!maxFactor.isFinite() || maxFactor < 2.0) return 1
    var size = 1
    while (size <= (1 shl 29) && size.toDouble() * 2.0 <= maxFactor) {
        size = size shl 1
    }
    return size
}

/**
 * [inSampleSize] for a cover placement of a [regionW]×[regionH] source into [destW]×[destH]
 * at [zoom]. [zoom] is clamped with [clampZoom]. Invalid sizes return 1 (full resolution).
 *
 * The factor is the scarcer axis, `min(region/dest) / zoom`: that is how many source pixels
 * cover one destination pixel after the cover scale.
 */
fun coverSampleSize(
    regionW: Int,
    regionH: Int,
    destW: Int,
    destH: Int,
    zoom: Float = 1f,
): Int {
    if (regionW <= 0 || regionH <= 0 || destW <= 0 || destH <= 0) return 1
    val clamped = clampZoom(zoom).toDouble()
    if (clamped <= 0.0) return 1
    val factor = minOf(regionW.toDouble() / destW, regionH.toDouble() / destH) / clamped
    return sampleSizeAtMost(factor)
}

/**
 * [inSampleSize] for a contain (letterbox) fit. Invalid sizes return 1.
 * The factor is the longer axis, `max(region/dest)`.
 */
fun containSampleSize(regionW: Int, regionH: Int, destW: Int, destH: Int): Int {
    if (regionW <= 0 || regionH <= 0 || destW <= 0 || destH <= 0) return 1
    val factor = maxOf(regionW.toDouble() / destW, regionH.toDouble() / destH)
    return sampleSizeAtMost(factor)
}

/** [inSampleSize] that keeps an oriented width of about [targetW] without upscaling. */
fun widthSampleSize(fullW: Int, targetW: Int): Int {
    if (fullW <= 0 || targetW <= 0) return 1
    return sampleSizeAtMost(fullW.toDouble() / targetW)
}

/** [inSampleSize] that keeps the longer oriented edge at about [targetLong] without upscaling. */
fun longEdgeSampleSize(fullW: Int, fullH: Int, targetLong: Int): Int {
    if (targetLong <= 0) return 1
    val longEdge = maxOf(fullW, fullH)
    if (longEdge <= 0) return 1
    return sampleSizeAtMost(longEdge.toDouble() / targetLong)
}

/**
 * Integer downscale so the long edge is at most [maxEdge]. Never upscales.
 * Either dimension ≤ 0 yields an [maxEdge]×[maxEdge] box (at least 1×1).
 */
fun previewDecodeSize(width: Int, height: Int, maxEdge: Int): OrientedSize {
    val edge = maxEdge.coerceAtLeast(1)
    if (width <= 0 || height <= 0) return OrientedSize(edge, edge)
    if (maxOf(width, height) <= edge) return OrientedSize(width, height)
    return if (width >= height) {
        OrientedSize(edge, ((height.toLong() * edge) / width).toInt().coerceAtLeast(1))
    } else {
        OrientedSize(((width.toLong() * edge) / height).toInt().coerceAtLeast(1), edge)
    }
}

/**
 * 2×3 affine on storage-raster coordinates, in Android `Matrix.setValues` order:
 * `x' = scaleX * x + skewX * y + transX`, `y' = skewY * x + scaleY * y + transY`.
 *
 * [outputWidth] and [outputHeight] are the bitmap size after [Bitmap.createBitmap] translates
 * the mapped bounds back to the origin.
 */
data class Affine2x3(
    val scaleX: Float,
    val skewX: Float,
    val transX: Float,
    val skewY: Float,
    val scaleY: Float,
    val transY: Float,
    val outputWidth: Int,
    val outputHeight: Int,
) {
    fun map(x: Float, y: Float): Pair<Float, Float> =
        (scaleX * x + skewX * y + transX) to (skewY * x + scaleY * y + transY)

    /**
     * Nine coefficients for `Matrix.setValues`. Indices 0–5 are this affine;
     * 6–8 are the perspective identity `(0, 0, 1)`.
     */
    fun matrixValues(): FloatArray = floatArrayOf(
        scaleX,
        skewX,
        transX,
        skewY,
        scaleY,
        transY,
        0f,
        0f,
        1f,
    )
}

/**
 * EXIF 1–8 mapped the way [decodeRotated] does: the existing matrix, then a translation that
 * puts the mapped bitmap bounds at the origin (`Bitmap.createBitmap(raw, matrix)`).
 *
 * [width] and [height] are the storage raster the matrix is applied to (full size, or the
 * sampled bitmap). Unknown orientations stay identity, matching the decoder's `when`.
 *
 * 1 (x, y); 2 (w−x, y); 3 (w−x, h−y); 4 (x, h−y);
 * 5 (h−y, w−x); 6 (h−y, x); 7 (y, x); 8 (y, w−x).
 */
fun exifOrientationTransform(orientation: Int, width: Int, height: Int): Affine2x3 {
    val w = width.toFloat()
    val h = height.toFloat()
    val swapsAxes = orientation in 5..8
    val outputWidth = if (swapsAxes) height else width
    val outputHeight = if (swapsAxes) width else height
    return when (orientation) {
        2 -> Affine2x3(-1f, 0f, w, 0f, 1f, 0f, outputWidth, outputHeight)
        3 -> Affine2x3(-1f, 0f, w, 0f, -1f, h, outputWidth, outputHeight)
        4 -> Affine2x3(1f, 0f, 0f, 0f, -1f, h, outputWidth, outputHeight)
        5 -> Affine2x3(0f, -1f, h, -1f, 0f, w, outputWidth, outputHeight)
        6 -> Affine2x3(0f, -1f, h, 1f, 0f, 0f, outputWidth, outputHeight)
        7 -> Affine2x3(0f, 1f, 0f, 1f, 0f, 0f, outputWidth, outputHeight)
        8 -> Affine2x3(0f, 1f, 0f, -1f, 0f, w, outputWidth, outputHeight)
        else -> Affine2x3(1f, 0f, 0f, 0f, 1f, 0f, width, height)
    }
}
