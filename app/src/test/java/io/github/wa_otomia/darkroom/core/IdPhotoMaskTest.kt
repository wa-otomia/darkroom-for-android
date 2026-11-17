package io.github.wa_otomia.darkroom.core

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test

class IdPhotoMaskTest {
    private fun luma(vararg runs: Pair<Int, Int>): ByteArray {
        val out = ArrayList<Byte>()
        for ((value, count) in runs) repeat(count) { out += value.toByte() }
        return out.toByteArray()
    }

    private fun alphaAt(a: ByteArray, w: Int, x: Int, y: Int): Int = a[y * w + x].toInt() and 0xFF

    @Test
    fun aspectToleranceIsOnePercentRelative() {
        // Grok upload 3:4 answered with a square: fail, never stretch or crop to fit.
        assertFalse(idMaskAspectMatches(1152, 1536, 1024, 1024))
        assertTrue(idMaskAspectMatches(1536, 1152, 1536, 1152))
        assertTrue(idMaskAspectMatches(1536, 1152, 768, 576))
        assertTrue(idMaskAspectMatches(1000, 1000, 1009, 1000))
        assertFalse(idMaskAspectMatches(1000, 1000, 1011, 1000))
        assertFalse(idMaskAspectMatches(0, 1000, 1000, 1000))
        assertFalse(idMaskAspectMatches(1000, 1000, 1000, 0))
        assertEquals(0.01, ID_MASK_ASPECT_TOLERANCE, 0.0)
    }

    @Test
    fun openAiPadsTheUploadOntoTheClosestCanvas() {
        val upload = aiUploadSize(4000, 3000)
        assertEquals(OrientedSize(1536, 1152), upload)
        val canvas = openAiMaskCanvas(upload.width, upload.height)
        assertEquals(1536 to 1024, canvas)
        val content = fitMaskContent(upload.width, upload.height, canvas.first, canvas.second)
        assertEquals(IdRect(85, 0, 1365, 1024), content)
        // The canvas-sized reply passes the aspect check.
        assertTrue(idMaskAspectMatches(1536, 1024, 1536, 1024))
        // Portrait and square sources pick their own canvas.
        assertEquals(1024 to 1536, openAiMaskCanvas(1152, 1536))
        assertEquals(1024 to 1024, openAiMaskCanvas(1200, 1100))
        assertEquals(listOf(1024 to 1024, 1536 to 1024, 1024 to 1536), OPENAI_MASK_CANVASES)
    }

    @Test
    fun maskContentIsCentredAndNeverUpscaled() {
        assertEquals(IdRect(368, 212, 800, 600), fitMaskContent(800, 600, 1536, 1024))
        assertEquals(IdRect(0, 0, 1024, 1024), fitMaskContent(1024, 1024, 1024, 1024))
        val tall = fitMaskContent(1152, 1536, 1024, 1536)
        assertEquals(IdRect(0, 85, 1024, 1365), tall)
    }

    @Test
    fun uploadSizeCapsTheLongEdgeWithoutUpscaling() {
        assertEquals(OrientedSize(1152, 1536), aiUploadSize(3000, 4000))
        assertEquals(OrientedSize(800, 600), aiUploadSize(800, 600))
        assertEquals(OrientedSize(1536, 1536), aiUploadSize(1536, 1536))
        assertEquals(OrientedSize(1, 1), aiUploadSize(0, 0))
        assertEquals(1536, AI_UPLOAD_MAX_EDGE)
    }

    @Test
    fun lumaVerdicts() {
        assertEquals(IdMaskVerdict.OK, validateIdMaskLuma(luma(0 to 50, 255 to 50)))
        // Thresholds: 32 is black, 223 is white.
        assertEquals(IdMaskVerdict.OK, validateIdMaskLuma(luma(32 to 50, 223 to 50)))
        // Exactly 10 % mid-grey passes; above it is not a matte.
        assertEquals(IdMaskVerdict.OK, validateIdMaskLuma(luma(0 to 45, 128 to 10, 255 to 45)))
        assertEquals(IdMaskVerdict.NOT_BINARY, validateIdMaskLuma(luma(0 to 44, 33 to 11, 255 to 45)))
        assertEquals(IdMaskVerdict.NOT_BINARY, validateIdMaskLuma(luma(0 to 44, 222 to 11, 255 to 45)))
        // A colour photo is mostly mid-grey.
        assertEquals(IdMaskVerdict.NOT_BINARY, validateIdMaskLuma(luma(90 to 40, 150 to 40, 255 to 20)))
        // All black: no person. Under 1 % white: no person. Exactly 1 %: fine.
        assertEquals(IdMaskVerdict.NO_PERSON, validateIdMaskLuma(luma(0 to 100)))
        assertEquals(IdMaskVerdict.NO_PERSON, validateIdMaskLuma(luma(0 to 1000, 255 to 9)))
        assertEquals(IdMaskVerdict.OK, validateIdMaskLuma(luma(0 to 99, 255 to 1)))
        // All white or over 99 % white: no background.
        assertEquals(IdMaskVerdict.NO_BACKGROUND, validateIdMaskLuma(luma(255 to 100)))
        assertEquals(IdMaskVerdict.NO_BACKGROUND, validateIdMaskLuma(luma(0 to 9, 255 to 1000)))
        assertEquals(IdMaskVerdict.OK, validateIdMaskLuma(luma(0 to 1, 255 to 99)))
        assertEquals(IdMaskVerdict.NO_PERSON, validateIdMaskLuma(ByteArray(0)))
    }

    @Test
    fun verdictMessages() {
        assertEquals("", idMaskVerdictMessage(IdMaskVerdict.OK))
        assertEquals(ID_MASK_MSG_ASPECT, idMaskVerdictMessage(IdMaskVerdict.ASPECT_MISMATCH))
        assertEquals(ID_MASK_MSG_NOT_BINARY, idMaskVerdictMessage(IdMaskVerdict.NOT_BINARY))
        assertEquals(ID_MASK_MSG_NO_PERSON, idMaskVerdictMessage(IdMaskVerdict.NO_PERSON))
        assertEquals(ID_MASK_MSG_NO_BACKGROUND, idMaskVerdictMessage(IdMaskVerdict.NO_BACKGROUND))
    }

    @Test
    fun lumaToAlphaLevels() {
        assertEquals(0, idMaskLumaToAlpha(0))
        assertEquals(0, idMaskLumaToAlpha(32))
        assertEquals(1, idMaskLumaToAlpha(33))
        assertEquals(128, idMaskLumaToAlpha(128))
        assertEquals(254, idMaskLumaToAlpha(222))
        assertEquals(255, idMaskLumaToAlpha(223))
        assertEquals(255, idMaskLumaToAlpha(255))
        assertEquals(0, idMaskLumaToAlpha(-5))
        assertEquals(255, idMaskLumaToAlpha(400))
        var last = 0
        for (v in 0..255) {
            val a = idMaskLumaToAlpha(v)
            assertTrue(a >= last)
            last = a
        }
    }

    @Test
    fun featherRadiiFollowPrintMillimetres() {
        assertEquals(listOf(0, 2, 5, 10), IdFeather.entries.map { it.radiusPx() })
        assertEquals(listOf(0, 4, 10, 20), IdFeather.entries.map { it.radiusPx(2) })
        assertEquals(listOf(0, 2, 5, 10), IdFeather.entries.map { it.radiusPx(0) })
        assertEquals(listOf("none", "weak", "medium", "strong"), IdFeather.entries.map { it.id })
    }

    @Test
    fun zeroRadiusReturnsACopy() {
        val a = byteArrayOf(0, 255.toByte(), 7, 9)
        val out = featherAlpha(a, 2, 2, 0)
        assertArrayEquals(a, out)
        assertNotSame(a, out)
        assertArrayEquals(a, featherAlpha(a, 2, 2, -3))
    }

    @Test
    fun featherSmoothsOnlyWithinTheRadiusOfAnEdge() {
        val w = 80
        val h = 30
        val edge = 40 // x < 40 is black, x ≥ 40 white
        val a = ByteArray(w * h) { i -> if (i % w >= edge) 255.toByte() else 0 }
        for (r in listOf(2, 5, 10, 20)) {
            val out = featherAlpha(a, w, h, r)
            for (y in 0 until h) {
                for (x in 0 until w) {
                    val v = alphaAt(out, w, x, y)
                    when {
                        x < edge - r -> assertEquals("r=$r x=$x", 0, v)
                        x >= edge + r -> assertEquals("r=$r x=$x", 255, v)
                    }
                }
            }
            // The edge itself is softened.
            assertTrue(alphaAt(out, w, edge - 1, 5) in 1..254)
            assertTrue(alphaAt(out, w, edge, 5) in 1..254)
            // And monotonic across it.
            for (x in 1 until w) assertTrue(alphaAt(out, w, x, 5) >= alphaAt(out, w, x - 1, 5))
        }
    }

    @Test
    fun strongFeatherLeavesThePersonInteriorUntouched() {
        // A white square, 30 px inside it is untouched by a 10 px feather (EDID040-R07).
        val w = 120
        val h = 120
        val a = ByteArray(w * h) { i ->
            val x = i % w
            val y = i / w
            if (x in 20 until 100 && y in 20 until 100) 255.toByte() else 0
        }
        val r = IdFeather.STRONG.radiusPx()
        val out = featherAlpha(a, w, h, r)
        assertEquals(255, alphaAt(out, w, 50, 50))
        for (y in 0 until h) {
            for (x in 0 until w) {
                val inside = x in 20 + r until 100 - r && y in 20 + r until 100 - r
                val outside = x !in 20 - r until 100 + r || y !in 20 - r until 100 + r
                if (inside) assertEquals(255, alphaAt(out, w, x, y))
                if (outside) assertEquals(0, alphaAt(out, w, x, y))
            }
        }
        // Image borders are not edges: a uniform mask stays uniform.
        val solid = ByteArray(40 * 30) { 255.toByte() }
        assertArrayEquals(solid, featherAlpha(solid, 40, 30, 10))
    }

    @Test
    fun compositeKeepsOriginalPixelsUnderOpaqueMask() {
        val src = 0xFFC99678.toInt() // RGB(201,150,120)
        val navy = IdBackground.NAVY.argb!!
        assertEquals(src, compositeIdPixel(src, 255, navy))
        val white = IdBackground.WHITE.argb!!
        assertEquals(0xFFFFFFFF.toInt(), compositeIdPixel(src, 0, white))
        assertEquals(navy, compositeIdPixel(src, 0, navy))
        // Transparent source (outside the original): background colour.
        assertEquals(navy, compositeIdPixel(0x00C99678, 255, navy))
        // Half and half.
        val mixed = compositeIdPixel(0xFF000000.toInt(), 128, 0xFFFFFFFF.toInt())
        assertEquals(0xFF7F7F7F.toInt(), mixed)
        // Always opaque.
        assertEquals(0xFF, compositeIdPixel(0x80102030.toInt(), 200, 0x00000000) ushr 24)
    }

    @Test
    fun compositeInPlace() {
        val px = intArrayOf(0xFFC99678.toInt(), 0xFFC99678.toInt(), 0x00000000)
        val alpha = byteArrayOf(255.toByte(), 0, 255.toByte())
        compositeIdPixels(px, alpha, 0xFF1E4B91.toInt())
        assertArrayEquals(intArrayOf(0xFFC99678.toInt(), 0xFF1E4B91.toInt(), 0xFF1E4B91.toInt()), px)
    }

    @Test
    fun featherSourcePixelsFollowCoverAndZoom() {
        val cover = coverScale(717f, 921f, 4000, 3000)
        assertEquals(10f / cover, idFeatherSourcePx(10, 717, 921, 4000, 3000, 1f), 1e-3f)
        assertEquals(10f / (cover * 2f), idFeatherSourcePx(10, 717, 921, 4000, 3000, 2f), 1e-3f)
        assertEquals(0f, idFeatherSourcePx(0, 717, 921, 4000, 3000, 1f), 0f)
    }

    @Test
    fun backgroundPalette() {
        assertEquals(IdBackground.ORIGINAL, idBackground(null))
        assertEquals(IdBackground.ORIGINAL, idBackground("purple"))
        assertEquals(IdBackground.LIGHT_GREY, idBackground("light_grey"))
        assertFalse(IdBackground.ORIGINAL.needsMask)
        assertTrue(IdBackground.entries.drop(1).all { it.needsMask })
        assertEquals(0xFF1E4B91.toInt(), IdBackground.NAVY.argb)
        assertEquals(0xFF438EDB.toInt(), IdBackground.ID_BLUE.argb)
        assertEquals(0xFFFF0000.toInt(), IdBackground.RED.argb)
        assertEquals(0xFFE5E5E5.toInt(), IdBackground.LIGHT_GREY.argb)
        assertEquals(0xFFF2EEE3.toInt(), IdBackground.OFF_WHITE.argb)
        assertEquals(0xFFD6E4F0.toInt(), IdBackground.PALE_BLUE.argb)
        assertEquals(IdBackground.NAVY, DEFAULT_ID_BACKGROUND_AFTER_MASK)
        assertEquals(IdFeather.WEAK, idFeather("bogus"))
        assertEquals(IdFeather.STRONG, idFeather("strong"))
    }

    @Test
    fun maskPromptIsFixedEnglishAndAsksForAMatte() {
        assertTrue(ID_PHOTO_MASK_PROMPT.contains("white"))
        assertTrue(ID_PHOTO_MASK_PROMPT.contains("black"))
        assertTrue(ID_PHOTO_MASK_PROMPT.all { it.code < 128 })
    }
}
