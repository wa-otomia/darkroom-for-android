package io.github.wa_otomia.darkroom.core

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * improve-id-photo-matting: reply checks (AIPV030-R04), hair-keeping inward feather
 * (EDID040-R07) and the on-device mapping (EDID040-R11). Every image here is synthetic; the
 * user's evidence photos never enter the repository.
 */
class IdPhotoMattingTest {
    /** A mutable luma/alpha canvas, row-major, unsigned. */
    private class Img(val w: Int, val h: Int, fill: Int = 0) {
        val px = ByteArray(w * h) { fill.toByte() }
        operator fun get(x: Int, y: Int): Int = px[y * w + x].toInt() and 0xFF
        operator fun set(x: Int, y: Int, v: Int) {
            px[y * w + x] = v.coerceIn(0, 255).toByte()
        }
        fun rect(x0: Int, y0: Int, x1: Int, y1: Int, v: Int) {
            for (y in y0 until y1) for (x in x0 until x1) this[x, y] = v
        }
        fun count(pred: (Int) -> Boolean): Int = px.count { pred(it.toInt() and 0xFF) }
    }

    private fun verdict(img: Img, coloured: Long = 0): IdMaskVerdict = validateIdMask(img.px, img.w, img.h, coloured)

    private fun isMid(v: Int) = v in 33..222
    private fun isWhite(v: Int) = v >= 223

    private fun inBand(x: Int, y: Int, w: Int, h: Int): Boolean {
        val b = idMaskBorderBand(w, h)
        return y < b || x < b || x >= w - b
    }

    // ---------------------------------------------------------------------------------------
    // Reply checks
    // ---------------------------------------------------------------------------------------

    @Test
    fun bandAndRadiusRounding() {
        // Evidence sizes (design D2): 36 / 33 px bands, 18 / 17 px radii.
        assertEquals(36, idMaskBorderBand(1776, 2368))
        assertEquals(33, idMaskBorderBand(1664, 2496))
        assertEquals(18, idMaskInteriorRadius(1776, 2368))
        assertEquals(17, idMaskInteriorRadius(1664, 2496))
        // Minimum 2, half rounds up, never past the short edge.
        assertEquals(2, idMaskBorderBand(100, 100))
        assertEquals(2, idMaskBorderBand(49, 300))
        assertEquals(3, idMaskBorderBand(125, 200))
        assertEquals(1, idMaskBorderBand(1, 1))
        assertEquals(0, idMaskBorderBand(0, 5))
        assertEquals(2, idMaskInteriorRadius(100, 100))
        assertEquals(2, idMaskInteriorRadius(249, 400))
        assertEquals(3, idMaskInteriorRadius(250, 400))
    }

    @Test
    fun emptyAreaHasNoPerson() {
        assertEquals(IdMaskVerdict.NO_PERSON, validateIdMask(ByteArray(0), 0, 0, 0))
        assertEquals(IdMaskVerdict.NO_PERSON, validateIdMask(ByteArray(3), 2, 2, 0))
    }

    /** 100×100, bottom-centre white body; a valid mask with no grey. */
    private fun body100(): Img = Img(100, 100).apply { rect(20, 40, 80, 100, 255) }

    @Test
    fun colouredPixelsOverTwoPercentAreNotAMask() {
        val img = body100()
        assertEquals(IdMaskVerdict.OK, verdict(img, 0))
        assertEquals(IdMaskVerdict.OK, verdict(img, 200))
        assertEquals(IdMaskVerdict.NOT_BINARY, verdict(img, 201))
        // A colour edit of the person: most pixels coloured.
        assertEquals(IdMaskVerdict.NOT_BINARY, verdict(img, 6000))
    }

    @Test
    fun colouredMeansSpreadOverTwentyFourAfterFlattening() {
        assertFalse(idMaskColoured(0xFF646464.toInt()))
        assertFalse(idMaskColoured(0xFF7C6464.toInt())) // spread 24
        assertTrue(idMaskColoured(0xFF7D6464.toInt())) // spread 25
        assertFalse(idMaskColoured(0xFFFFFFFF.toInt()))
        assertFalse(idMaskColoured(0xFF000000.toInt()))
        assertTrue(idMaskColoured(0xFFFF0000.toInt()))
        // Flattened over black first: a faint red becomes a dark grey-ish red.
        assertTrue(idMaskColoured(0x80FF0000.toInt())) // (128,0,0)
        assertFalse(idMaskColoured(0x0AFF0000)) // (10,0,0)
        assertFalse(idMaskColoured(0x00FF00FF))
    }

    @Test
    fun midGreyOverThirtyFivePercentIsNotAMask() {
        // Rows 0..69 alternate grey/black (every grey touches black), rows 70..99 a white body.
        val img = Img(100, 100)
        for (y in 0 until 70) for (x in 0 until 100 step 2) img[x, y] = 128
        img.rect(2, 70, 98, 100, 255)
        assertEquals(3500, img.count(::isMid))
        assertEquals(IdMaskVerdict.OK, verdict(img))
        img[1, 0] = 128
        assertEquals(3501, img.count(::isMid))
        assertEquals(IdMaskVerdict.NOT_BINARY, verdict(img))
        // A whole greyscale image (40 % mid-grey) is not a mask.
        val grey = body100()
        var made = 0
        for (i in grey.px.indices) if (made < 4000 && grey.px[i].toInt() == 0) { grey.px[i] = 90; made++ }
        assertEquals(IdMaskVerdict.NOT_BINARY, verdict(grey))
    }

    @Test
    fun whiteShareBounds() {
        // 1 % white at the bottom centre: fine; one px less: no person; all black: no person.
        val small = Img(100, 100).apply { rect(45, 90, 55, 100, 255) }
        assertEquals(100, small.count(::isWhite))
        assertEquals(IdMaskVerdict.OK, verdict(small))
        small[45, 90] = 0
        assertEquals(IdMaskVerdict.NO_PERSON, verdict(small))
        assertEquals(IdMaskVerdict.NO_PERSON, verdict(Img(100, 100)))
        // Over 99 % white: no background. Exactly 99 % passes the white check (and then
        // fails on the lit border instead).
        val big = Img(100, 100, 255)
        assertEquals(IdMaskVerdict.NO_BACKGROUND, verdict(big))
        for (x in 0 until 99) big[x, 50] = 0
        assertEquals(IdMaskVerdict.NO_BACKGROUND, verdict(big))
        big[99, 50] = 0
        assertEquals(9900, big.count(::isWhite))
        assertEquals(IdMaskVerdict.BACKGROUND_KEPT, verdict(big))
    }

    @Test
    fun borderBandOverSixtyPercentKeptTheBackground() {
        val w = 100
        val h = 100
        val band = ArrayList<Pair<Int, Int>>()
        for (y in 0 until h) for (x in 0 until w) if (inBand(x, y, w, h)) band += x to y
        // Union of top 2 rows and the left/right 2 columns, each pixel once.
        assertEquals(2 * 100 + 98 * 4, band.size)
        val limit = (band.size * ID_MASK_MAX_BORDER_NONBLACK_FRACTION).toInt() // 355
        fun lit(k: Int): Img {
            val img = Img(w, h)
            img.rect(20, 40, 80, 100, 255)
            for ((x, y) in band.take(k)) img[x, y] = 255
            return img
        }
        assertEquals(IdMaskVerdict.OK, verdict(lit(limit)))
        assertEquals(IdMaskVerdict.BACKGROUND_KEPT, verdict(lit(limit + 1)))
        // Grey counts as non-black too.
        val grey = lit(0)
        for ((x, y) in band.take(limit + 1)) grey[x, y] = 40
        assertEquals(IdMaskVerdict.BACKGROUND_KEPT, verdict(grey))
        // 32 is black.
        val dark = lit(0)
        for ((x, y) in band) dark[x, y] = 32
        assertEquals(IdMaskVerdict.OK, verdict(dark))
        // The bottom edge is not part of the band: a body filling the bottom rows is fine.
        val bottom = Img(w, h).apply { rect(2, 60, 98, 100, 255) }
        assertEquals(IdMaskVerdict.OK, verdict(bottom))
    }

    @Test
    fun interiorGreyOverFifteenPercentOfNonBlackIsShaded() {
        // 200×200: a white block 150 rows × 120 cols away from the top/left/right bands, with a
        // grey patch more than R (2 px) from any black.
        fun shaded(k: Int): Img {
            val img = Img(200, 200)
            img.rect(40, 50, 160, 200, 255)
            var left = k
            for (y in 60 until 190) for (x in 50 until 150) if (left > 0) { img[x, y] = 150; left-- }
            return img
        }
        val nonBlack = 150 * 120
        val limit = (nonBlack * ID_MASK_MAX_INTERIOR_GREY_FRACTION).toInt() // 2700
        assertEquals(2700, limit)
        assertEquals(IdMaskVerdict.OK, verdict(shaded(limit)))
        assertEquals(IdMaskVerdict.SHADED, verdict(shaded(limit + 1)))
        // The same amount of grey hugging black is hair, not shading.
        val hair = Img(200, 200)
        hair.rect(40, 50, 160, 200, 255)
        for (y in 20 until 50) for (x in 40 until 160 step 2) hair[x, y] = 150
        assertTrue(hair.count(::isMid).toDouble() / hair.count { it > 32 } > 0.05)
        assertEquals(IdMaskVerdict.OK, verdict(hair))
    }

    @Test
    fun interiorRadiusBoundary() {
        // A grey pixel exactly R px from black is near it; R+1 px away is far.
        val r = idMaskInteriorRadius(200, 200)
        assertEquals(2, r)
        fun oneGreyAt(distance: Int): Img {
            val img = Img(200, 200)
            img.rect(40, 100, 160, 200, 255)
            // Leftmost white column is x=40, black at x=39: put the grey at x = 39 + distance.
            img[39 + distance, 150] = 150
            return img
        }
        // One far pixel is 1/12000 of non-black: below the limit either way, so probe the mask.
        val near = oneGreyAt(r)
        val far = oneGreyAt(r + 1)
        val nb = { img: Img -> ByteArray(img.px.size) { i -> if ((img.px[i].toInt() and 0xFF) > 32) 255.toByte() else 0 } }
        assertEquals(0, erodeAlpha(nb(near), 200, 200, r)[150 * 200 + 39 + r].toInt() and 0xFF)
        assertEquals(255, erodeAlpha(nb(far), 200, 200, r)[150 * 200 + 39 + r + 1].toInt() and 0xFF)
        assertEquals(IdMaskVerdict.OK, verdict(near))
        assertEquals(IdMaskVerdict.OK, verdict(far))
    }

    // Synthetic stand-ins for the three evidence replies (design D2).

    /** Head and shoulders with fine grey hair strands over black: evidence 1's traits. */
    private fun softHairSilhouette(): Img {
        val img = Img(300, 400)
        // Head: ellipse centred (150,150), radii 60×75.
        for (y in 0 until 400) for (x in 0 until 300) {
            val dx = (x - 150) / 60.0
            val dy = (y - 150) / 75.0
            if (dx * dx + dy * dy <= 1.0) img[x, y] = 255
        }
        img.rect(120, 210, 180, 280, 255) // neck
        img.rect(0, 280, 300, 400, 255) // shoulders touch left, right and bottom
        // Flyaways: 1 px grey strands rising from the crown into black, with a soft fringe.
        for (s in 0 until 9) {
            val x0 = 110 + s * 10
            for (t in 0 until 40) {
                val x = x0 + (t % 7) / 3
                val y = 80 - t
                img[x, y] = 200 - t * 3
                if (t % 5 == 0) img[x + 1, y] = 90
            }
        }
        return img
    }

    /** A relit greyscale photo: light wall (≥223 luma), a black chair, grey face shading. */
    private fun relitGreyscalePhoto(): Img {
        val img = Img(300, 400, 230) // wall reads as "white"
        img.rect(0, 250, 90, 400, 0) // chair
        for (y in 120 until 260) for (x in 110 until 190) img[x, y] = 120 + ((x + y) % 60) // face shading
        return img
    }

    /** A shaded greyscale render of a cup on black: grey inside the silhouette. */
    private fun cupRender(): Img {
        val img = Img(300, 400)
        for (y in 150 until 330) for (x in 90 until 210) {
            img[x, y] = 60 + (x - 90) * 150 / 120 // left-to-right shading
        }
        img.rect(180, 170, 200, 260, 250) // specular highlight
        return img
    }

    @Test
    fun evidenceLikeRepliesGetTheirVerdicts() {
        val hair = softHairSilhouette()
        assertTrue(hair.count(::isMid) > 0)
        assertEquals(IdMaskVerdict.OK, verdict(hair))

        val relit = relitGreyscalePhoto()
        val n = relit.px.size.toDouble()
        assertTrue(relit.count(::isWhite) / n in 0.5..0.99)
        assertTrue(relit.count(::isMid) / n < ID_MASK_MAX_MID_FRACTION)
        assertEquals(IdMaskVerdict.BACKGROUND_KEPT, verdict(relit))

        val cup = cupRender()
        assertTrue(cup.count(::isWhite) / n >= 0.01)
        assertTrue(cup.count(::isMid) / n < ID_MASK_MAX_MID_FRACTION)
        assertEquals(IdMaskVerdict.SHADED, verdict(cup))
    }

    // ---------------------------------------------------------------------------------------
    // Hair-keeping inward feather (design D3)
    // ---------------------------------------------------------------------------------------

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

    private fun bruteDilate(a: ByteArray, w: Int, h: Int, r: Int): ByteArray = ByteArray(w * h) { i ->
        val x = i % w
        val y = i / w
        var m = 0
        for (dy in -r..r) for (dx in -r..r) {
            val v = a[(y + dy).coerceIn(0, h - 1) * w + (x + dx).coerceIn(0, w - 1)].toInt() and 0xFF
            if (v > m) m = v
        }
        m.toByte()
    }

    private fun u(b: Byte) = b.toInt() and 0xFF

    @Test
    fun dilateIsTheWindowMaximumWithReplicatedEdges() {
        for ((w, h) in listOf(1 to 1, 1 to 9, 9 to 1, 17 to 11, 40 to 33)) {
            val a = randomMask(w, h, (w * 17 + h).toLong())
            for (r in listOf(1, 2, 3, 5, 10, 25)) {
                assertArrayEquals("${w}x$h r=$r", bruteDilate(a, w, h, r), dilateAlpha(a, w, h, r))
            }
        }
        val a = byteArrayOf(0, 255.toByte(), 7, 9)
        val out = dilateAlpha(a, 2, 2, 0)
        assertArrayEquals(a, out)
        assertNotSame(a, out)
    }

    @Test
    fun softAlphaSeparatesHairFromHardRims() {
        val w = 30
        val h = 10
        val img = Img(w, h)
        img.rect(20, 0, 30, 10, 255)
        for (y in 0 until h) img[19, y] = 128 // one-px rim between 0 and 255
        for (y in 0 until h) img[5, y] = 100 // isolated hair strand in black
        val soft = idMaskSoftAlpha(img.px, w, h, 1)
        for (y in 0 until h) {
            assertEquals(0, u(soft[y * w + 19]))
            assertEquals(100, u(soft[y * w + 5]))
            assertEquals(0, u(soft[y * w + 25])) // 255 is never soft
            assertEquals(0, u(soft[y * w + 0])) // 0 is never soft
        }
        // A strand 2 px from the body is soft at s=1 but on the rim at s=2.
        val near = Img(w, h)
        near.rect(20, 0, 30, 10, 255)
        for (y in 0 until h) near[18, y] = 100
        assertEquals(100, u(idMaskSoftAlpha(near.px, w, h, 1)[5 * w + 18]))
        assertEquals(0, u(idMaskSoftAlpha(near.px, w, h, 2)[5 * w + 18]))
    }

    @Test
    fun keepSoftNeverSpillsAndNeverExceedsTheMask() {
        // Properties 1 and 2.
        for (seed in 1L..6L) {
            val w = 60
            val h = 45
            val a = randomMask(w, h, seed, noiseEvery = 6)
            for (r in listOf(1, 2, 5, 10)) for (s in listOf(1, 2, 3)) {
                val out = featherAlphaInwardKeepSoft(a, w, h, r, s)
                for (i in a.indices) {
                    if (u(a[i]) == 0) assertEquals("seed=$seed r=$r s=$s i=$i", 0, u(out[i]))
                    assertTrue("seed=$seed r=$r s=$s i=$i", u(out[i]) <= u(a[i]))
                }
            }
        }
    }

    @Test
    fun keepSoftKeepsEverySoftPixelExactly() {
        // Property 3, generic: any soft pixel keeps its α.
        for (seed in 7L..10L) {
            val w = 50
            val h = 40
            val a = randomMask(w, h, seed, noiseEvery = 8)
            for (r in listOf(2, 10)) {
                val soft = idMaskSoftAlpha(a, w, h, 1)
                val out = featherAlphaInwardKeepSoft(a, w, h, r, 1)
                var seen = 0
                for (i in a.indices) if (u(soft[i]) != 0) {
                    assertEquals("seed=$seed r=$r i=$i", u(a[i]), u(out[i]))
                    seen++
                }
                assertTrue(seen > 0)
            }
        }
    }

    @Test
    fun flyawayStrandSurvivesTheInwardFeather() {
        // Property 3, the spec scenario: a 1 px α=128 strand in black, no 255 within s px.
        val w = 60
        val h = 60
        val img = Img(w, h)
        img.rect(45, 0, 60, 60, 255) // the body, 15 px away
        for (y in 5 until 55) img[30, y] = 128
        for (r in listOf(2, 10)) {
            val out = featherAlphaInwardKeepSoft(img.px, w, h, r, 1)
            val old = featherAlphaInward(img.px, w, h, r)
            for (y in 5 until 55) {
                assertEquals("r=$r y=$y", 128, u(out[y * w + 30]))
                assertEquals("r=$r y=$y", 0, u(old[y * w + 30]))
            }
            // Pixels next to the strand stay background.
            for (y in 0 until h) {
                assertEquals(0, u(out[y * w + 29]))
                assertEquals(0, u(out[y * w + 31]))
            }
        }
    }

    /** Binary rectangles, optionally with a one-px 128 ring (8-neighbourhood) around each. */
    private fun hardMask(w: Int, h: Int, ring: Boolean): ByteArray {
        val img = Img(w, h)
        val rects = listOf(intArrayOf(5, 5, 25, 30), intArrayOf(32, 8, 55, 20), intArrayOf(32, 26, 70, 44))
        for (r in rects) img.rect(r[0], r[1], r[2], r[3], 255)
        if (ring) {
            for (r in rects) {
                for (y in r[1] - 1..r[3]) for (x in r[0] - 1..r[2]) {
                    if (x in 0 until w && y in 0 until h && img[x, y] == 0) img[x, y] = 128
                }
            }
        }
        return img.px
    }

    @Test
    fun hardEdgesFeatherExactlyAsBefore() {
        // Property 4: only 0/255, or a single-px transition touching both, gives featherAlphaInward.
        val w = 80
        val h = 50
        for (ring in listOf(false, true)) {
            val a = hardMask(w, h, ring)
            if (ring) assertTrue(idMaskSoftAlpha(a, w, h, 1).all { it.toInt() == 0 })
            for (r in listOf(2, 5, 10)) {
                assertArrayEquals("ring=$ring r=$r", featherAlphaInward(a, w, h, r), featherAlphaInwardKeepSoft(a, w, h, r, 1))
            }
        }
        // A random binary mask too.
        val bin = randomMask(64, 48, 3).map { if (u(it) >= 128) 255.toByte() else 0.toByte() }.toByteArray()
        for (r in listOf(2, 5, 10)) {
            assertArrayEquals(featherAlphaInward(bin, 64, 48, r), featherAlphaInwardKeepSoft(bin, 64, 48, r, 1))
        }
    }

    @Test
    fun noDarkRingBetweenHairAndHead() {
        // Property 5: black → 12 px linear soft edge → white core, r = 10.
        val w = 90
        val h = 8
        val r = 10
        val img = Img(w, h)
        for (y in 0 until h) {
            for (x in 0 until w) {
                img[x, y] = when {
                    x < 20 -> 0
                    x < 32 -> Math.round(255.0 * (x - 19) / 13).toInt()
                    else -> 255
                }
            }
        }
        val out = featherAlphaInwardKeepSoft(img.px, w, h, r, 1)
        for (y in 0 until h) {
            for (x in 1 until w) {
                assertTrue("y=$y x=$x", u(out[y * w + x]) >= u(out[y * w + x - 1]))
                assertTrue(u(out[y * w + x]) <= img[x, y])
            }
            // The soft edge keeps its α; the core within 2r is at least the adjacent soft α.
            for (x in 20 until 32) assertEquals(img[x, y], u(out[y * w + x]))
            for (x in 32 until 32 + 2 * r) assertTrue("x=$x", u(out[y * w + x]) >= img[31, y])
            for (x in 0 until 20) assertEquals(0, u(out[y * w + x]))
        }
        // Without the hair rule the edge is eroded away (the dark ring this fixes).
        val old = featherAlphaInward(img.px, w, h, r)
        assertTrue(u(old[4 * w + 32]) < img[31, 4])
    }

    @Test
    fun keepSoftLeavesTheWideInteriorOpaque() {
        // Property 6.
        for (seed in 11L..13L) {
            val w = 70
            val h = 52
            val a = randomMask(w, h, seed, noiseEvery = 400)
            for (r in listOf(1, 2, 5, 10)) {
                val out = featherAlphaInwardKeepSoft(a, w, h, r, 1)
                val wide = erodeAlpha(a, w, h, 2 * r)
                for (i in a.indices) if (u(wide[i]) == 255) assertEquals(255, u(out[i]))
            }
        }
    }

    @Test
    fun zeroRadiusIsIdentityAndSymmetricIsUnchanged() {
        // Properties 7 and 8.
        val a = randomMask(33, 21, 5, noiseEvery = 5)
        for (s in listOf(1, 3)) {
            val out = featherAlphaInwardKeepSoft(a, 33, 21, 0, s)
            assertArrayEquals(a, out)
            assertNotSame(a, out)
            assertArrayEquals(a, featherAlphaFor(a, 33, 21, 0, inward = true, rimRadius = s))
        }
        for (r in listOf(1, 2, 5, 10)) {
            assertArrayEquals(featherAlpha(a, 33, 21, r), featherAlphaFor(a, 33, 21, r, inward = false, rimRadius = 4))
        }
    }

    @Test
    fun rimRadiusIsOneMaskPixelRoundedUp() {
        assertEquals(1, idMaskRimRadius(0.2f))
        assertEquals(1, idMaskRimRadius(1f))
        assertEquals(2, idMaskRimRadius(1.2f))
        assertEquals(2, idMaskRimRadius(2f))
        assertEquals(4, idMaskRimRadius(3.5f))
        assertEquals(1, idMaskRimRadius(Float.NaN))
        assertEquals(1, idMaskRimRadius(-3f))
        assertEquals(1, idMaskRimRadius(Float.POSITIVE_INFINITY))
    }

    @Test
    fun stretchedHardEdgeIsStillARim() {
        // A mask px spanning 3 render px stretches the one-px transition to ~3 px; s = 3 keeps
        // it hard while s = 1 would call its middle soft.
        val w = 40
        val h = 6
        val img = Img(w, h)
        for (y in 0 until h) for (x in 0 until w) {
            img[x, y] = when {
                x < 18 -> 0
                x < 21 -> 64 * (x - 17)
                else -> 255
            }
        }
        assertNotEquals(0, u(idMaskSoftAlpha(img.px, w, h, 1)[2 * w + 19]))
        assertTrue(idMaskSoftAlpha(img.px, w, h, idMaskRimRadius(3f)).all { it.toInt() == 0 })
        assertArrayEquals(featherAlphaInward(img.px, w, h, 4), featherAlphaInwardKeepSoft(img.px, w, h, 4, 3))
    }

    // ---------------------------------------------------------------------------------------
    // On-device cut-out (EDID040-R10/R11)
    // ---------------------------------------------------------------------------------------

    @Test
    fun confidenceMapsLinearlyBetweenQuarterAndThreeQuarters() {
        assertEquals(0, idMaskConfidenceLuma(0f))
        assertEquals(0, idMaskConfidenceLuma(0.25f))
        assertEquals(128, idMaskConfidenceLuma(0.5f))
        assertEquals(255, idMaskConfidenceLuma(0.75f))
        assertEquals(255, idMaskConfidenceLuma(1f))
        assertEquals(0, idMaskConfidenceLuma(-1f))
        assertEquals(255, idMaskConfidenceLuma(2f))
        assertEquals(0, idMaskConfidenceLuma(Float.NaN))
        var last = 0
        for (i in 0..1000) {
            val v = idMaskConfidenceLuma(i / 1000f)
            assertTrue(v >= last)
            last = v
        }
    }

    @Test
    fun localMaskChecksOnlyTheWhiteShare() {
        fun luma(vararg runs: Pair<Int, Int>): ByteArray {
            val out = ArrayList<Byte>()
            for ((value, count) in runs) repeat(count) { out += value.toByte() }
            return out.toByteArray()
        }
        assertEquals(IdMaskVerdict.OK, validateLocalMaskLuma(luma(0 to 50, 255 to 50)))
        assertEquals(IdMaskVerdict.OK, validateLocalMaskLuma(luma(0 to 99, 255 to 1)))
        assertEquals(IdMaskVerdict.NO_PERSON, validateLocalMaskLuma(luma(0 to 1000, 255 to 9)))
        assertEquals(IdMaskVerdict.NO_PERSON, validateLocalMaskLuma(luma(0 to 100)))
        assertEquals(IdMaskVerdict.NO_PERSON, validateLocalMaskLuma(ByteArray(0)))
        assertEquals(IdMaskVerdict.OK, validateLocalMaskLuma(luma(0 to 1, 255 to 99)))
        assertEquals(IdMaskVerdict.NO_BACKGROUND, validateLocalMaskLuma(luma(0 to 9, 255 to 1000)))
        // A confidence map is grey by nature: lots of mid-grey is fine.
        assertEquals(IdMaskVerdict.OK, validateLocalMaskLuma(luma(0 to 5, 128 to 90, 255 to 5)))
    }

    @Test
    fun localCutoutStartsOff() {
        assertFalse(DEFAULT_ID_PHOTO_LOCAL_CUTOUT)
    }
}
