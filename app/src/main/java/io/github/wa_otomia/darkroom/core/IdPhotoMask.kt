package io.github.wa_otomia.darkroom.core

import kotlin.math.abs
import kotlin.math.roundToInt

// Mask checks, feathering and compositing for ID photo backgrounds (AIPV030-R04, EDID040).

const val ID_MASK_ASPECT_TOLERANCE = 0.01

/** Luma at or above this is white (person). */
const val ID_MASK_WHITE_LUMA = 223

/** Luma at or below this is black (background); 33..222 is mid-grey. */
const val ID_MASK_BLACK_LUMA = 32

/** A pixel is coloured when max(R,G,B) − min(R,G,B), flattened over black, exceeds this. */
const val ID_MASK_COLOUR_SPREAD = 24
const val ID_MASK_MAX_COLOURED_FRACTION = 0.02
const val ID_MASK_MAX_MID_FRACTION = 0.35
const val ID_MASK_MIN_WHITE_FRACTION = 0.01
const val ID_MASK_MAX_WHITE_FRACTION = 0.99

/** Top/left/right band width = max(2, round(short edge × this)). */
const val ID_MASK_BORDER_BAND_FRACTION = 0.02
const val ID_MASK_MAX_BORDER_NONBLACK_FRACTION = 0.60

/** "Far from black" radius = max(2, round(short edge × this)) px (Chebyshev). */
const val ID_MASK_INTERIOR_RADIUS_FRACTION = 0.01
const val ID_MASK_MAX_INTERIOR_GREY_FRACTION = 0.15

/** On-device confidence c maps to luma 0 at or below this, 255 at or above [ID_MASK_LOCAL_CONFIDENCE_HIGH]. */
const val ID_MASK_LOCAL_CONFIDENCE_LOW = 0.25
const val ID_MASK_LOCAL_CONFIDENCE_HIGH = 0.75

enum class IdMaskVerdict { OK, ASPECT_MISMATCH, NOT_BINARY, NO_PERSON, NO_BACKGROUND, BACKGROUND_KEPT, SHADED }

/** True when the returned aspect is within [ID_MASK_ASPECT_TOLERANCE] (relative) of the expected one. */
fun idMaskAspectMatches(expectedWidth: Int, expectedHeight: Int, actualWidth: Int, actualHeight: Int): Boolean {
    if (expectedWidth <= 0 || expectedHeight <= 0 || actualWidth <= 0 || actualHeight <= 0) return false
    val expected = expectedWidth.toDouble() / expectedHeight
    val actual = actualWidth.toDouble() / actualHeight
    return abs(actual - expected) / expected <= ID_MASK_ASPECT_TOLERANCE
}

/**
 * True when an unpremultiplied ARGB reply pixel, flattened over black (each channel
 * `(c·a+127)/255` when a≠255, as the luma is), has max(R,G,B) − min(R,G,B) > [ID_MASK_COLOUR_SPREAD].
 */
fun idMaskColoured(argb: Int): Boolean {
    val a = (argb ushr 24) and 0xFF
    var r = (argb shr 16) and 0xFF
    var g = (argb shr 8) and 0xFF
    var b = argb and 0xFF
    if (a != 255) {
        r = (r * a + 127) / 255
        g = (g * a + 127) / 255
        b = (b * a + 127) / 255
    }
    return maxOf(r, g, b) - minOf(r, g, b) > ID_MASK_COLOUR_SPREAD
}

/** Width of each top/left/right band: max(2, round(short edge × 2%)), never past the short edge. */
fun idMaskBorderBand(width: Int, height: Int): Int {
    val short = minOf(width, height)
    if (short <= 0) return 0
    return maxOf(2, (short * ID_MASK_BORDER_BAND_FRACTION).roundToInt()).coerceAtMost(short)
}

/** "Far from black" radius: max(2, round(short edge × 1%)). */
fun idMaskInteriorRadius(width: Int, height: Int): Int =
    maxOf(2, (minOf(width, height).coerceAtLeast(0) * ID_MASK_INTERIOR_RADIUS_FRACTION).roundToInt())

/**
 * Checks a provider reply's content area (AIPV030-R04). [luma] is the unsigned luma of the
 * [width]×[height] area, row-major; [colouredCount] is how many of its pixels [idMaskColoured].
 * The first failing check wins:
 * 1. coloured > 2 % → NOT_BINARY;
 * 2. mid-grey (33–222) > 35 % → NOT_BINARY;
 * 3. white (≥223) < 1 % → NO_PERSON; 4. white > 99 % → NO_BACKGROUND;
 * 5. non-black (>32) share of the top, left and right bands ([idMaskBorderBand], union, bottom
 *    excluded) > 60 % → BACKGROUND_KEPT;
 * 6. mid-grey with no black pixel within [idMaskInteriorRadius] + [extraInteriorRadius]
 *    (Chebyshev, edges replicated), as a share of non-black pixels, > 15 % → SHADED. The extra
 *    radius is the edge gradient an AI-feathered reply was asked for (EDID040-R13), in area px.
 * An empty area has no person.
 */
fun validateIdMask(
    luma: ByteArray,
    width: Int,
    height: Int,
    colouredCount: Long,
    extraInteriorRadius: Int = 0,
): IdMaskVerdict {
    if (width <= 0 || height <= 0 || luma.size < width.toLong() * height) return IdMaskVerdict.NO_PERSON
    val n = width * height
    val total = n.toDouble()
    if (colouredCount / total > ID_MASK_MAX_COLOURED_FRACTION) return IdMaskVerdict.NOT_BINARY
    var white = 0L
    var mid = 0L
    for (i in 0 until n) {
        val v = luma[i].toInt() and 0xFF
        if (v >= ID_MASK_WHITE_LUMA) white++ else if (v > ID_MASK_BLACK_LUMA) mid++
    }
    if (mid / total > ID_MASK_MAX_MID_FRACTION) return IdMaskVerdict.NOT_BINARY
    if (white / total < ID_MASK_MIN_WHITE_FRACTION) return IdMaskVerdict.NO_PERSON
    if (white / total > ID_MASK_MAX_WHITE_FRACTION) return IdMaskVerdict.NO_BACKGROUND

    val band = idMaskBorderBand(width, height)
    var bandTotal = 0L
    var bandLit = 0L
    fun countBand(i: Int) {
        bandTotal++
        if ((luma[i].toInt() and 0xFF) > ID_MASK_BLACK_LUMA) bandLit++
    }
    for (y in 0 until height) {
        val base = y * width
        if (y < band) {
            for (x in 0 until width) countBand(base + x)
        } else {
            val leftEnd = minOf(band, width)
            for (x in 0 until leftEnd) countBand(base + x)
            for (x in maxOf(leftEnd, width - band) until width) countBand(base + x)
        }
    }
    if (bandTotal > 0 && bandLit.toDouble() / bandTotal > ID_MASK_MAX_BORDER_NONBLACK_FRACTION) {
        return IdMaskVerdict.BACKGROUND_KEPT
    }

    val nonBlack = ByteArray(n)
    var nonBlackCount = 0L
    for (i in 0 until n) {
        if ((luma[i].toInt() and 0xFF) > ID_MASK_BLACK_LUMA) {
            nonBlack[i] = 255.toByte()
            nonBlackCount++
        }
    }
    val interiorRadius = idMaskInteriorRadius(width, height) + extraInteriorRadius.coerceAtLeast(0)
    val farFromBlack = erodeAlpha(nonBlack, width, height, interiorRadius)
    var interiorGrey = 0L
    for (i in 0 until n) {
        val v = luma[i].toInt() and 0xFF
        if (v > ID_MASK_BLACK_LUMA && v < ID_MASK_WHITE_LUMA && (farFromBlack[i].toInt() and 0xFF) == 255) interiorGrey++
    }
    if (nonBlackCount > 0 && interiorGrey.toDouble() / nonBlackCount > ID_MASK_MAX_INTERIOR_GREY_FRACTION) {
        return IdMaskVerdict.SHADED
    }
    return IdMaskVerdict.OK
}

/**
 * Checks an on-device mask (EDID040-R11): only the white share, since a confidence map is grey
 * by nature. Under 1 % white has no person, over 99 % no background.
 */
fun validateLocalMaskLuma(luma: ByteArray): IdMaskVerdict {
    if (luma.isEmpty()) return IdMaskVerdict.NO_PERSON
    var white = 0L
    for (b in luma) if ((b.toInt() and 0xFF) >= ID_MASK_WHITE_LUMA) white++
    val share = white.toDouble() / luma.size
    return when {
        share < ID_MASK_MIN_WHITE_FRACTION -> IdMaskVerdict.NO_PERSON
        share > ID_MASK_MAX_WHITE_FRACTION -> IdMaskVerdict.NO_BACKGROUND
        else -> IdMaskVerdict.OK
    }
}

/** On-device person confidence → mask luma: c ≤ 0.25 is 0, c ≥ 0.75 is 255, linear and rounded between. */
fun idMaskConfidenceLuma(confidence: Float): Int {
    if (confidence.isNaN()) return 0
    val span = ID_MASK_LOCAL_CONFIDENCE_HIGH - ID_MASK_LOCAL_CONFIDENCE_LOW
    return ((confidence - ID_MASK_LOCAL_CONFIDENCE_LOW) / span * 255.0).roundToInt().coerceIn(0, 255)
}

/** The data-layer failure message for [verdict]; [IdMaskVerdict.OK] has none. */
fun idMaskVerdictMessage(verdict: IdMaskVerdict): String = when (verdict) {
    IdMaskVerdict.OK -> ""
    IdMaskVerdict.ASPECT_MISMATCH -> ID_MASK_MSG_ASPECT
    IdMaskVerdict.NOT_BINARY -> ID_MASK_MSG_NOT_BINARY
    IdMaskVerdict.NO_PERSON -> ID_MASK_MSG_NO_PERSON
    IdMaskVerdict.NO_BACKGROUND -> ID_MASK_MSG_NO_BACKGROUND
    IdMaskVerdict.BACKGROUND_KEPT -> ID_MASK_MSG_BACKGROUND_KEPT
    IdMaskVerdict.SHADED -> ID_MASK_MSG_SHADED
}

/** Black (≤32) is 0, white (≥223) is 255, linear and rounded in between. */
fun idMaskLumaToAlpha(luma: Int): Int {
    val v = luma.coerceIn(0, 255)
    if (v <= ID_MASK_BLACK_LUMA) return 0
    if (v >= ID_MASK_WHITE_LUMA) return 255
    val span = (ID_MASK_WHITE_LUMA - ID_MASK_BLACK_LUMA).toDouble()
    return Math.round((v - ID_MASK_BLACK_LUMA) * 255.0 / span).toInt().coerceIn(0, 255)
}

/**
 * Separable box blur of [alpha] (unsigned, row-major) with a (2r+1)² window and edge
 * replication. Only pixels within [radius] (Chebyshev) of a different α change; a pixel whose
 * whole window is uniform keeps its exact α. A radius of 0 or less returns a copy.
 */
fun featherAlpha(alpha: ByteArray, width: Int, height: Int, radius: Int): ByteArray {
    if (radius <= 0 || width <= 0 || height <= 0 || alpha.size < width * height) return alpha.copyOf()
    val r = radius
    val window = 2 * r + 1
    // Horizontal pass keeps raw window sums so the vertical pass rounds only once.
    val rows = IntArray(width * height)
    for (y in 0 until height) {
        val base = y * width
        var sum = 0
        for (k in -r..r) sum += alpha[base + k.coerceIn(0, width - 1)].toInt() and 0xFF
        for (x in 0 until width) {
            rows[base + x] = sum
            val outIdx = (x - r).coerceIn(0, width - 1)
            val inIdx = (x + r + 1).coerceIn(0, width - 1)
            sum += (alpha[base + inIdx].toInt() and 0xFF) - (alpha[base + outIdx].toInt() and 0xFF)
        }
    }
    val out = alpha.copyOf()
    val divisor = window * window
    val half = divisor / 2
    for (x in 0 until width) {
        var sum = 0
        for (k in -r..r) sum += rows[k.coerceIn(0, height - 1) * width + x]
        for (y in 0 until height) {
            out[y * width + x] = ((sum + half) / divisor).coerceIn(0, 255).toByte()
            val outIdx = (y - r).coerceIn(0, height - 1)
            val inIdx = (y + r + 1).coerceIn(0, height - 1)
            sum += rows[inIdx * width + x] - rows[outIdx * width + x]
        }
    }
    return out
}

/**
 * Separable (2r+1)² minimum filter of [alpha] (unsigned, row-major) with edge replication: the
 * mask shrinks by [radius] px (Chebyshev). A radius of 0 or less returns a copy.
 */
fun erodeAlpha(alpha: ByteArray, width: Int, height: Int, radius: Int): ByteArray {
    if (radius <= 0 || width <= 0 || height <= 0 || alpha.size < width * height) return alpha.copyOf()
    val r = radius
    val maxLen = maxOf(width, height) + 2 * r
    val line = IntArray(maxLen)
    val prefix = IntArray(maxLen)
    val suffix = IntArray(maxLen)
    val result = IntArray(maxOf(width, height))
    val rows = ByteArray(width * height)
    for (y in 0 until height) {
        val base = y * width
        minFilterLine(width, r, line, prefix, suffix, result) { i -> alpha[base + i].toInt() and 0xFF }
        for (x in 0 until width) rows[base + x] = result[x].toByte()
    }
    val out = alpha.copyOf()
    for (x in 0 until width) {
        minFilterLine(height, r, line, prefix, suffix, result) { i -> rows[i * width + x].toInt() and 0xFF }
        for (y in 0 until height) out[y * width + x] = result[y].toByte()
    }
    return out
}

/**
 * Sliding (2r+1) minimum over a line of [n] values read through [read], edges replicated, into
 * [out] (van Herk / Gil-Werman: three comparisons per value whatever the radius).
 */
private inline fun minFilterLine(
    n: Int,
    r: Int,
    line: IntArray,
    prefix: IntArray,
    suffix: IntArray,
    out: IntArray,
    read: (Int) -> Int,
) {
    val window = 2 * r + 1
    val len = n + 2 * r
    for (j in 0 until len) line[j] = read((j - r).coerceIn(0, n - 1))
    for (j in 0 until len) {
        prefix[j] = if (j % window == 0) line[j] else minOf(prefix[j - 1], line[j])
    }
    for (j in len - 1 downTo 0) {
        suffix[j] = if (j == len - 1 || (j + 1) % window == 0) line[j] else minOf(suffix[j + 1], line[j])
    }
    for (i in 0 until n) out[i] = minOf(suffix[i], prefix[i + window - 1])
}

/**
 * Inward feather (EDID040-R07): erode by [radius], then [featherAlpha] by [radius]. A pixel with
 * α 0 stays 0, and a pixel whose (4r+1)² window is all 255 stays 255, so the blend lies inside
 * the mask's edge. A radius of 0 or less returns a copy.
 */
fun featherAlphaInward(alpha: ByteArray, width: Int, height: Int, radius: Int): ByteArray =
    featherAlpha(erodeAlpha(alpha, width, height, radius), width, height, radius)

/**
 * Separable (2r+1)² maximum filter of [alpha] with edge replication, the mirror of [erodeAlpha]:
 * the mask grows by [radius] px (Chebyshev). A radius of 0 or less returns a copy.
 */
fun dilateAlpha(alpha: ByteArray, width: Int, height: Int, radius: Int): ByteArray {
    if (radius <= 0 || width <= 0 || height <= 0 || alpha.size < width * height) return alpha.copyOf()
    // max(a) = 255 − min(255 − a): the same van Herk pass on the inverted mask.
    val inverted = ByteArray(width * height) { i -> (255 - (alpha[i].toInt() and 0xFF)).toByte() }
    val eroded = erodeAlpha(inverted, width, height, radius)
    val out = alpha.copyOf()
    for (i in 0 until width * height) out[i] = (255 - (eroded[i].toInt() and 0xFF)).toByte()
    return out
}

/**
 * Inward feather that also feathers fine parts (EDID040-R07): with e = [erodeAlpha] by r,
 * F = [featherAlpha] of e (the body, as [featherAlphaInward]) and T = α − [dilateAlpha] of e (the
 * opening residue: strands and flyaways narrower than 2r+1 that the opening cannot restore),
 * `out = max(F, min(T, featherAlpha(T, r)))`. Fine parts are smoothed by the same radius and
 * clipped to their own footprint, so they fade with the feather instead of vanishing or staying
 * crisp. α 0 stays 0, no pixel ends above its α, and a mask the opening restores (wide parts,
 * monotone ramps) gives exactly F. A radius of 0 or less returns a copy.
 */
fun featherAlphaInwardFine(alpha: ByteArray, width: Int, height: Int, radius: Int): ByteArray {
    if (radius <= 0 || width <= 0 || height <= 0 || alpha.size < width * height) return alpha.copyOf()
    val n = width * height
    val eroded = erodeAlpha(alpha, width, height, radius)
    val out = featherAlpha(eroded, width, height, radius)
    val opened = dilateAlpha(eroded, width, height, radius)
    val fine = ByteArray(n)
    var any = false
    for (i in 0 until n) {
        val t = (alpha[i].toInt() and 0xFF) - (opened[i].toInt() and 0xFF)
        if (t > 0) {
            fine[i] = t.toByte()
            any = true
        }
    }
    if (!any) return out
    val smoothed = featherAlpha(fine, width, height, radius)
    for (i in 0 until n) {
        val keep = minOf(fine[i].toInt() and 0xFF, smoothed[i].toInt() and 0xFF)
        if (keep > (out[i].toInt() and 0xFF)) out[i] = keep.toByte()
    }
    return out
}

/**
 * [featherAlphaInwardFine] when [inward], otherwise the symmetric [featherAlpha] (which also
 * spreads fine parts outward).
 */
fun featherAlphaFor(alpha: ByteArray, width: Int, height: Int, radius: Int, inward: Boolean): ByteArray =
    if (inward) featherAlphaInwardFine(alpha, width, height, radius) else featherAlpha(alpha, width, height, radius)

/**
 * `out = src·a + bg·(1−a)` with `a = srcA/255 · α/255`. The result is opaque; an opaque
 * source under α 255 keeps its exact RGB, and α 0 (or a transparent source) gives [background].
 */
fun compositeIdPixel(src: Int, alpha: Int, background: Int): Int {
    val srcA = (src ushr 24) and 0xFF
    val a = (srcA * alpha.coerceIn(0, 255) + 127) / 255
    val inv = 255 - a
    fun channel(shift: Int): Int {
        val s = (src ushr shift) and 0xFF
        val b = (background ushr shift) and 0xFF
        return (s * a + b * inv + 127) / 255
    }
    return (0xFF shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
}

/** [compositeIdPixel] over [src] in place, pairing each pixel with the same index of [alpha]. */
fun compositeIdPixels(src: IntArray, alpha: ByteArray, background: Int) {
    val n = minOf(src.size, alpha.size)
    for (i in 0 until n) {
        src[i] = compositeIdPixel(src[i], alpha[i].toInt() and 0xFF, background)
    }
}

/**
 * A print-px feather radius in source px for a mask kept in source space: one cell px covers
 * `1 / (cover · zoom)` source px.
 */
fun idFeatherSourcePx(
    featherPrintPx: Int,
    cellWidth: Int,
    cellHeight: Int,
    imageWidth: Int,
    imageHeight: Int,
    zoom: Float,
): Float {
    if (featherPrintPx <= 0) return 0f
    val cover = coverScale(cellWidth.toFloat(), cellHeight.toFloat(), imageWidth, imageHeight)
    val z = clampZoom(if (zoom.isFinite()) zoom else 1f)
    val perSource = cover * z
    if (!(perSource > 0f) || !perSource.isFinite()) return featherPrintPx.toFloat()
    return featherPrintPx / perSource
}
