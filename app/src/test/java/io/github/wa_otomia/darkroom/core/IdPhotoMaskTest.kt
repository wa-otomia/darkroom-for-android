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
    fun verdictMessages() {
        assertEquals("", idMaskVerdictMessage(IdMaskVerdict.OK))
        assertEquals(ID_MASK_MSG_ASPECT, idMaskVerdictMessage(IdMaskVerdict.ASPECT_MISMATCH))
        assertEquals(ID_MASK_MSG_NOT_BINARY, idMaskVerdictMessage(IdMaskVerdict.NOT_BINARY))
        assertEquals(ID_MASK_MSG_NO_PERSON, idMaskVerdictMessage(IdMaskVerdict.NO_PERSON))
        assertEquals(ID_MASK_MSG_NO_BACKGROUND, idMaskVerdictMessage(IdMaskVerdict.NO_BACKGROUND))
        assertEquals(ID_MASK_MSG_BACKGROUND_KEPT, idMaskVerdictMessage(IdMaskVerdict.BACKGROUND_KEPT))
        assertEquals(ID_MASK_MSG_SHADED, idMaskVerdictMessage(IdMaskVerdict.SHADED))
        assertEquals("抠图失败：返回图保留了原背景，不是蒙版", ID_MASK_MSG_BACKGROUND_KEPT)
        assertEquals("抠图失败：返回图是带明暗的灰度图，不是蒙版", ID_MASK_MSG_SHADED)
        assertEquals("本地抠图失败", ID_MASK_MSG_LOCAL_FAILED)
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

    /** Deterministic irregular masks: blobs, noise and grey edges. */
    private fun randomMask(w: Int, h: Int, seed: Long, noiseEvery: Int = 25): ByteArray {
        val rnd = java.util.Random(seed)
        val a = ByteArray(w * h)
        repeat(6) {
            val cx = rnd.nextInt(w)
            val cy = rnd.nextInt(h)
            val rad = 4 + rnd.nextInt(maxOf(5, minOf(w, h) / 2))
            for (y in 0 until h) for (x in 0 until w) {
                val dx = x - cx
                val dy = y - cy
                if (dx * dx + dy * dy <= rad * rad) a[y * w + x] = 255.toByte()
            }
        }
        repeat(w * h / noiseEvery) { a[rnd.nextInt(w * h)] = rnd.nextInt(256).toByte() }
        return a
    }

    /** Brute-force (2r+1)² minimum with edge replication. */
    private fun bruteErode(a: ByteArray, w: Int, h: Int, r: Int): ByteArray = ByteArray(w * h) { i ->
        val x = i % w
        val y = i / w
        var m = 255
        for (dy in -r..r) for (dx in -r..r) {
            val v = a[(y + dy).coerceIn(0, h - 1) * w + (x + dx).coerceIn(0, w - 1)].toInt() and 0xFF
            if (v < m) m = v
        }
        m.toByte()
    }

    @Test
    fun erodeIsTheWindowMinimumWithReplicatedEdges() {
        for ((w, h) in listOf(1 to 1, 1 to 9, 9 to 1, 17 to 11, 40 to 33)) {
            val a = randomMask(w, h, (w * 31 + h).toLong())
            for (r in listOf(1, 2, 3, 5, 10, 25)) {
                assertArrayEquals("${w}x$h r=$r", bruteErode(a, w, h, r), erodeAlpha(a, w, h, r))
            }
        }
        val a = byteArrayOf(0, 255.toByte(), 7, 9)
        for (r in listOf(0, -2)) {
            val out = erodeAlpha(a, 2, 2, r)
            assertArrayEquals(a, out)
            assertNotSame(a, out)
        }
    }

    @Test
    fun inwardFeatherWithZeroRadiusIsIdentity() {
        val a = randomMask(23, 19, 7)
        for (inward in listOf(true, false)) {
            val out = featherAlphaFor(a, 23, 19, 0, inward)
            assertArrayEquals(a, out)
            assertNotSame(a, out)
        }
        assertArrayEquals(a, featherAlphaInward(a, 23, 19, 0))
    }

    @Test
    fun outwardChoiceIsExactlyTheSymmetricFeather() {
        val a = randomMask(50, 37, 11)
        for (r in listOf(0, 1, 2, 5, 10)) {
            assertArrayEquals("r=$r", featherAlpha(a, 50, 37, r), featherAlphaFor(a, 50, 37, r, inward = false))
            assertArrayEquals(
                "r=$r",
                featherAlphaInwardFine(a, 50, 37, r),
                featherAlphaFor(a, 50, 37, r, inward = true),
            )
        }
    }

    @Test
    fun inwardFeatherNeverSpillsOutsideTheMask() {
        for (seed in 1L..4L) {
            val w = 60
            val h = 45
            val a = randomMask(w, h, seed)
            for (r in listOf(1, 2, 5, 10)) {
                val out = featherAlphaInward(a, w, h, r)
                for (i in a.indices) {
                    if ((a[i].toInt() and 0xFF) == 0) assertEquals("seed=$seed r=$r i=$i", 0, out[i].toInt() and 0xFF)
                }
            }
        }
    }

    @Test
    fun inwardFeatherKeepsPixelsWhoseWideWindowIsOpaque() {
        // Any pixel whose (4r+1)² window (edges replicated) is all 255 keeps 255.
        for (seed in 5L..8L) {
            val w = 70
            val h = 52
            val a = randomMask(w, h, seed, noiseEvery = 400)
            for (r in listOf(1, 2, 5, 10)) {
                val out = featherAlphaInward(a, w, h, r)
                val wide = bruteErode(a, w, h, 2 * r)
                var checked = 0
                for (i in a.indices) {
                    if ((wide[i].toInt() and 0xFF) == 255) {
                        assertEquals("seed=$seed r=$r i=$i", 255, out[i].toInt() and 0xFF)
                        checked++
                    }
                }
                if (r <= 2) assertTrue("seed=$seed r=$r", checked > 0)
            }
        }
    }

    @Test
    fun inwardFeatherBlendsInsideTheEdgeOnly() {
        val w = 80
        val h = 30
        val edge = 40 // x < 40 is black, x ≥ 40 white
        val a = ByteArray(w * h) { i -> if (i % w >= edge) 255.toByte() else 0 }
        for (r in listOf(2, 5, 10)) {
            val out = featherAlphaInward(a, w, h, r)
            for (y in 0 until h) {
                for (x in 0 until w) {
                    val v = alphaAt(out, w, x, y)
                    when {
                        x < edge -> assertEquals("r=$r x=$x", 0, v)
                        x >= edge + 2 * r -> assertEquals("r=$r x=$x", 255, v)
                    }
                }
            }
            // The blend sits just inside the edge and rises monotonically.
            assertTrue(alphaAt(out, w, edge + r, 5) in 1..254)
            for (x in 1 until w) assertTrue(alphaAt(out, w, x, 5) >= alphaAt(out, w, x - 1, 5))
        }
        // A uniform mask stays uniform: image borders are not edges.
        val solid = ByteArray(40 * 30) { 255.toByte() }
        assertArrayEquals(solid, featherAlphaInward(solid, 40, 30, 10))
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

    @Test
    fun maskPromptKeepsOnlyThePerson() {
        // AIPV030-R04 / design D1 fragments.
        for (fragment in listOf(
            "main person only",
            "chair backs",
            "even where it is white or light-coloured",
            "flat, solid, pure white",
            "fine hair strands",
            "all-black",
            "do not crop, pad, shift, rotate, resize or redraw",
            "not a photo edit",
            "headrests",
        )) {
            assertTrue(fragment, ID_PHOTO_MASK_PROMPT.contains(fragment))
        }
        // Verbatim text of design D1: sentences joined by exactly one space.
        assertEquals(EXPECTED_MASK_PROMPT, ID_PHOTO_MASK_PROMPT)
        assertFalse(ID_PHOTO_MASK_PROMPT.contains("  "))
        assertEquals(ID_PHOTO_MASK_PROMPT.trim(), ID_PHOTO_MASK_PROMPT)
    }

    private companion object {
        const val EXPECTED_MASK_PROMPT =
            "Task: produce an alpha matte for replacing the background of a portrait or ID photo. This is a segmentation task, not a photo edit: do not relight, recolour, restyle or re-render the photo. Output only the matte, at exactly the same width, height, framing and composition as the input; do not crop, pad, shift, rotate, resize or redraw anything, so that every matte pixel lines up with the same pixel of the input. Foreground is the main person only: hair, face, ears, neck, shoulders, arms and hands if visible, worn clothing and worn accessories such as glasses, earrings, hats and headscarves. Background is everything else, even where it touches or overlaps the person: walls, floors, ceilings, sky, doors, windows, furniture including chairs, chair backs, headrests, sofas and cushions, shelves, hanging clothes and coats, bags, objects, other people, shadows cast on the background, and any border or padding. Paint the background pure black (#000000), even where it is white or light-coloured in the photo. Paint the person as one flat, solid, pure white (#FFFFFF) silhouette with no shading, lighting, gradients, texture, facial features or outlines inside it; eyes, mouth, glasses, hair texture and clothing details must not be visible. Keep the edge tight to the person, with no glow, halo or margin around it. Only where fine hair strands or flyaways overlap the background, use grey levels to show partial transparency (lighter means more hair, darker means more background); the main mass of the hair stays solid white, and everywhere else make a clean, sharp edge. If no person is visible, return an all-black image. Use only black, white and that grey hair transition: no colours, text, watermarks or other content."
    }
}
