package io.github.wa_otomia.darkroom.core

import org.junit.Assert.assertEquals
import org.junit.Test

class ExportSheetTest {
    @Test
    fun exportSheetSizePortraitAndLandscape() {
        assertEquals(OrientedSize(1040, 1560), exportSheetSize(false, 1))
        assertEquals(OrientedSize(1560, 1040), exportSheetSize(true, 1))
        assertEquals(OrientedSize(2080, 3120), exportSheetSize(false, 2))
        assertEquals(OrientedSize(3120, 2080), exportSheetSize(true, 2))
    }

    @Test
    fun exportSheetSizeCoercesScaleToAtLeastOne() {
        assertEquals(OrientedSize(1040, 1560), exportSheetSize(false, 0))
        assertEquals(OrientedSize(1560, 1040), exportSheetSize(true, -3))
    }

    @Test
    fun sheetQuarterTurnsOnlyForPrinterLandscape() {
        assertEquals(PRINT_SHEET_QUARTER_TURNS, sheetQuarterTurns(landscape = true, sheetForPrinter = true))
        assertEquals(0, sheetQuarterTurns(landscape = true, sheetForPrinter = false))
        assertEquals(0, sheetQuarterTurns(landscape = false, sheetForPrinter = true))
        assertEquals(0, sheetQuarterTurns(landscape = false, sheetForPrinter = false))
    }
}
