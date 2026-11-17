package io.github.wa_otomia.darkroom.core

import kotlin.math.abs

// Mask checks, feathering and compositing for ID photo backgrounds (AIPV030-R04, EDID040).

const val ID_MASK_ASPECT_TOLERANCE = 0.01

/** Luma at or above this is white (person). */
const val ID_MASK_WHITE_LUMA = 223

/** Luma at or below this is black (background); 33..222 is mid-grey. */
const val ID_MASK_BLACK_LUMA = 32
const val ID_MASK_MAX_MID_FRACTION = 0.10
const val ID_MASK_MIN_WHITE_FRACTION = 0.01
const val ID_MASK_MAX_WHITE_FRACTION = 0.99

enum class IdMaskVerdict { OK, ASPECT_MISMATCH, NOT_BINARY, NO_PERSON, NO_BACKGROUND }

/** True when the returned aspect is within [ID_MASK_ASPECT_TOLERANCE] (relative) of the expected one. */
fun idMaskAspectMatches(expectedWidth: Int, expectedHeight: Int, actualWidth: Int, actualHeight: Int): Boolean {
    if (expectedWidth <= 0 || expectedHeight <= 0 || actualWidth <= 0 || actualHeight <= 0) return false
    val expected = expectedWidth.toDouble() / expectedHeight
    val actual = actualWidth.toDouble() / actualHeight
    return abs(actual - expected) / expected <= ID_MASK_ASPECT_TOLERANCE
}

/**
 * Checks the content area's luma (unsigned bytes): too much mid-grey is not a matte, and the
 * white share must leave both a person and some background. An empty area has no person.
 */
fun validateIdMaskLuma(luma: ByteArray): IdMaskVerdict {
    if (luma.isEmpty()) return IdMaskVerdict.NO_PERSON
    var white = 0L
    var mid = 0L
    for (b in luma) {
        val v = b.toInt() and 0xFF
        if (v >= ID_MASK_WHITE_LUMA) white++ else if (v > ID_MASK_BLACK_LUMA) mid++
    }
    val total = luma.size.toDouble()
    return when {
        mid / total > ID_MASK_MAX_MID_FRACTION -> IdMaskVerdict.NOT_BINARY
        white / total < ID_MASK_MIN_WHITE_FRACTION -> IdMaskVerdict.NO_PERSON
        white / total > ID_MASK_MAX_WHITE_FRACTION -> IdMaskVerdict.NO_BACKGROUND
        else -> IdMaskVerdict.OK
    }
}

/** The data-layer failure message for [verdict]; [IdMaskVerdict.OK] has none. */
fun idMaskVerdictMessage(verdict: IdMaskVerdict): String = when (verdict) {
    IdMaskVerdict.OK -> ""
    IdMaskVerdict.ASPECT_MISMATCH -> ID_MASK_MSG_ASPECT
    IdMaskVerdict.NOT_BINARY -> ID_MASK_MSG_NOT_BINARY
    IdMaskVerdict.NO_PERSON -> ID_MASK_MSG_NO_PERSON
    IdMaskVerdict.NO_BACKGROUND -> ID_MASK_MSG_NO_BACKGROUND
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
