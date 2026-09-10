package io.github.wa_otomia.darkroom.core

/** Album-export sheet. Landscape stays 3:2; it is not turned onto the portrait printer sheet. */
fun exportSheetSize(landscape: Boolean, outputScale: Int = 1): OrientedSize {
    val scale = outputScale.coerceAtLeast(1)
    val w = PRINT_WIDTH * scale
    val h = PRINT_HEIGHT * scale
    return if (landscape) OrientedSize(h, w) else OrientedSize(w, h)
}

/** Extra clockwise quarters used only when a landscape frame must fill the portrait printer sheet. */
fun sheetQuarterTurns(landscape: Boolean, sheetForPrinter: Boolean): Int =
    if (landscape && sheetForPrinter) PRINT_SHEET_QUARTER_TURNS else 0
