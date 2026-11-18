package io.github.wa_otomia.darkroom.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class IdPhotoLayoutTest {
    private data class Expected(
        val id: String,
        val cellW: Int,
        val cellH: Int,
        val rotated: Boolean,
        val columns: Int,
        val rows: Int,
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int,
    ) {
        val count: Int get() = columns * rows
    }

    // Verified table (map/geometry.md §9): margins in px at print scale.
    private val table = listOf(
        Expected("35x45", 717, 921, false, 2, 1, 58, 59, 58, 60),
        Expected("kr-visa", 717, 921, false, 2, 1, 58, 59, 58, 60),
        Expected("jp-visa", 921, 921, false, 1, 1, 319, 59, 320, 60),
        Expected("33x48", 676, 983, false, 2, 1, 99, 28, 99, 29),
        Expected("25x35", 512, 717, true, 2, 2, 58, 3, 58, 3),
        Expected("22x32", 450, 655, true, 2, 2, 120, 65, 120, 65),
        Expected("35x49", 717, 1003, false, 2, 1, 58, 18, 58, 19),
        Expected("2x2in", 1040, 1040, false, 1, 1, 260, 0, 260, 0),
        Expected("50x70", 1024, 1433, true, 1, 1, 63, 8, 64, 8),
        Expected("30x40", 614, 819, false, 2, 1, 161, 110, 161, 111),
        Expected("24x30", 491, 614, true, 2, 2, 161, 24, 161, 24),
        Expected("40x60", 819, 1228, true, 1, 1, 166, 110, 166, 111),
        Expected("35x50", 717, 1024, false, 2, 1, 58, 8, 58, 8),
    )

    private fun layout(id: String) = idPhotoLayout(idPhotoPreset(id))

    private fun intersects(a: IdRect, b: IdRect): Boolean =
        a.left < b.right && b.left < a.right && a.top < b.bottom && b.top < a.bottom

    @Test
    fun presetTableHasThirteenFixedIdsInOrder() {
        assertEquals(table.map { it.id }, ID_PHOTO_PRESETS.map { it.id })
        assertEquals(13, ID_PHOTO_PRESETS.size)
        assertEquals(IdPhotoPreset("kr-visa", 35.0, 45.0), idPhotoPreset("kr-visa"))
        assertEquals(IdPhotoPreset("jp-visa", 45.0, 45.0), idPhotoPreset("jp-visa"))
        // Ids are not sizes: the Korean visa entry is its own choice, kept as stored.
        assertEquals("kr-visa", normalizeIdPhotoPresetId("kr-visa"))
        assertEquals(IdPhotoPreset("2x2in", 50.8, 50.8), idPhotoPreset("2x2in"))
        assertEquals("35x45", DEFAULT_ID_PHOTO_PRESET_ID)
    }

    @Test
    fun millimetresUseTheNominalTwoByThreeInchScale() {
        assertEquals(1560.0 / 76.2, ID_PX_PER_MM, 1e-12)
        assertEquals(717, idCellPx(35.0))
        assertEquals(921, idCellPx(45.0))
        assertEquals(1040, idCellPx(50.8))
        assertEquals(1560, idCellPx(76.2))
    }

    @Test
    fun everyPresetMatchesTheVerifiedGrid() {
        for (e in table) {
            val l = layout(e.id)
            val msg = e.id
            assertEquals(msg, ID_SHEET_WIDTH, l.sheetWidth)
            assertEquals(msg, ID_SHEET_HEIGHT, l.sheetHeight)
            assertEquals(msg, e.cellW, l.cellWidth)
            assertEquals(msg, e.cellH, l.cellHeight)
            assertEquals(msg, e.rotated, l.rotated)
            assertEquals(msg, e.columns, l.columns)
            assertEquals(msg, e.rows, l.rows)
            assertEquals(msg, e.count, l.count)
            assertEquals(msg, 1, l.scale)
            assertEquals(msg, e.left, l.slots.minOf { it.left })
            assertEquals(msg, e.top, l.slots.minOf { it.top })
            assertEquals(msg, e.right, l.sheetWidth - l.slots.maxOf { it.right })
            assertEquals(msg, e.bottom, l.sheetHeight - l.slots.maxOf { it.bottom })
            assertEquals(msg, minOf(e.left, e.top, e.right, e.bottom), l.minMarginPx)
            val slotW = if (e.rotated) e.cellH else e.cellW
            val slotH = if (e.rotated) e.cellW else e.cellH
            for (s in l.slots) {
                assertEquals(msg, slotW, s.width)
                assertEquals(msg, slotH, s.height)
            }
        }
    }

    @Test
    fun countsMatchTheSpecList() {
        assertEquals(
            listOf(2, 2, 1, 2, 4, 4, 2, 1, 1, 2, 4, 1, 2),
            ID_PHOTO_PRESETS.map { idPhotoLayout(it).count },
        )
    }

    @Test
    fun slotsAreRowMajorWithTenPixelGaps() {
        val l = layout("25x35")
        assertEquals(
            listOf(
                IdRect(58, 3, 717, 512),
                IdRect(785, 3, 717, 512),
                IdRect(58, 525, 717, 512),
                IdRect(785, 525, 717, 512),
            ),
            l.slots,
        )
        val upright = layout("35x45")
        assertEquals(listOf(IdRect(58, 59, 717, 921), IdRect(785, 59, 717, 921)), upright.slots)
        assertEquals(ID_GAP_PX, upright.slots[1].left - upright.slots[0].right)
    }

    @Test
    fun japaneseVisaSquareSitsUprightWithoutEdgeWarning() {
        val l = layout("jp-visa")
        assertEquals(listOf(IdRect(319, 59, 921, 921)), l.slots)
        assertEquals(59, l.minMarginPx)
        assertFalse(l.showsEdgeWarning())
        assertEquals(layout("35x45").slots, layout("kr-visa").slots)
    }

    @Test
    fun tiesKeepTheUprightGrid() {
        // 2×2 in: upright 1×1 and rotated 1×1 tie, so the upright grid stays.
        assertFalse(layout("2x2in").rotated)
    }

    @Test
    fun slotsAndCutLinesNeverOverlapAndStayOnTheSheet() {
        for (preset in ID_PHOTO_PRESETS) {
            for (scale in listOf(1, 2)) {
                val l = idPhotoLayout(preset).scaled(scale)
                val msg = "${preset.id}@$scale"
                for ((i, a) in l.slots.withIndex()) {
                    assertTrue(msg, a.left >= 0 && a.top >= 0)
                    assertTrue(msg, a.right <= l.sheetWidth && a.bottom <= l.sheetHeight)
                    for (b in l.slots.drop(i + 1)) assertFalse(msg, intersects(a, b))
                }
                assertTrue(msg, l.cutLines.isNotEmpty())
                for (c in l.cutLines) {
                    assertTrue(msg, c.width > 0 && c.height > 0)
                    assertTrue(msg, c.left >= 0 && c.top >= 0)
                    assertTrue(msg, c.right <= l.sheetWidth && c.bottom <= l.sheetHeight)
                    for (s in l.slots) assertFalse("$msg $c vs $s", intersects(c, s))
                }
            }
        }
    }

    @Test
    fun cutLinesRingEachSlotTwoPixelsOutside() {
        val l = layout("35x45")
        val first = l.slots[0]
        assertTrue(IdRect(first.left - 2, first.top - 2, first.width + 4, 2) in l.cutLines)
        assertTrue(IdRect(first.left - 2, first.bottom, first.width + 4, 2) in l.cutLines)
        assertTrue(IdRect(first.left - 2, first.top, 2, first.height) in l.cutLines)
        assertTrue(IdRect(first.right, first.top, 2, first.height) in l.cutLines)
        assertEquals(8, l.cutLines.size)
        // Two lines in the 10 px gap with 6 px of white between them.
        val inGap = l.cutLines.filter { it.left >= first.right && it.right <= l.slots[1].left && it.height == first.height }
        assertEquals(listOf(775, 783), inGap.map { it.left }.sorted())
        assertEquals(2, ID_CUT_LINE_PX)
        assertEquals(0xFFBDBDBD.toInt(), ID_CUT_LINE_COLOR)
    }

    @Test
    fun cutLinesAreClippedAtTheSheetEdge() {
        // 2×2 in touches the top and bottom edges: those lines vanish, the side ones stay.
        val square = layout("2x2in")
        assertEquals(listOf(IdRect(258, 0, 2, 1040), IdRect(1300, 0, 2, 1040)), square.cutLines)
        // 25×35 has 3 px top margin: the top line keeps its full 2 px.
        val one = layout("25x35")
        assertTrue(one.cutLines.any { it.top == 1 && it.height == 2 })
    }

    @Test
    fun scaleTwoDoublesEveryCoordinate() {
        for (preset in ID_PHOTO_PRESETS) {
            val one = idPhotoLayout(preset)
            val two = one.scaled(2)
            val msg = preset.id
            assertEquals(msg, 2, two.scale)
            assertEquals(msg, 3120, two.sheetWidth)
            assertEquals(msg, 2080, two.sheetHeight)
            assertEquals(msg, one.cellWidth * 2, two.cellWidth)
            assertEquals(msg, one.cellHeight * 2, two.cellHeight)
            assertEquals(msg, one.rotated, two.rotated)
            assertEquals(msg, one.columns, two.columns)
            assertEquals(msg, one.rows, two.rows)
            assertEquals(msg, one.slots.map { IdRect(it.left * 2, it.top * 2, it.width * 2, it.height * 2) }, two.slots)
            assertEquals(msg, one.cutLines.map { IdRect(it.left * 2, it.top * 2, it.width * 2, it.height * 2) }, two.cutLines)
            assertEquals(msg, one.minMarginPx * 2, two.minMarginPx)
            assertEquals(msg, one.showsEdgeWarning(), two.showsEdgeWarning())
        }
    }

    @Test
    fun scaleBelowOneIsScaleOne() {
        val l = layout("35x45")
        assertSame(l, l.scaled(1))
        assertEquals(l, l.scaled(0))
        assertEquals(l, l.scaled(-3))
    }

    @Test
    fun edgeWarningOnlyForMarginsUnderOneMillimetre() {
        val warned = ID_PHOTO_PRESETS.filter { idPhotoLayout(it).showsEdgeWarning() }.map { it.id }.toSet()
        assertEquals(setOf("25x35", "35x49", "2x2in", "50x70", "35x50"), warned)
        assertEquals(20, ID_EDGE_WARNING_PX)
    }
}
