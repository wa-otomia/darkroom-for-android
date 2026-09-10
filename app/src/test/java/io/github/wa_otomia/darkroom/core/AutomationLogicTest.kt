package io.github.wa_otomia.darkroom.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutomationLogicTest {
    private fun plan(
        autoEdit: Boolean = false,
        autoWatermark: Boolean = false,
        autoPrint: Boolean = false,
        presetId: String = "film",
        aiConfigured: Boolean = true,
        watermarkConfigured: Boolean = true,
        printerBound: Boolean = true,
    ) = planAutomation(
        autoEdit = autoEdit,
        autoWatermark = autoWatermark,
        autoPrint = autoPrint,
        presetId = presetId,
        aiConfigured = aiConfigured,
        watermarkConfigured = watermarkConfigured,
        printerBound = printerBound,
    )

    @Test
    fun allSwitchesOffDoesNothing() {
        val got = plan()
        assertFalse(got.edit)
        assertFalse(got.watermark)
        assertFalse(got.print)
        assertEquals(emptyList<String>(), got.skipped)
    }

    @Test
    fun editOnlyWhenReady() {
        val got = plan(autoEdit = true)
        assertEquals(AutomationPlan(edit = true, watermark = false, print = false, skipped = emptyList()), got)
    }

    @Test
    fun watermarkOnlyWhenReady() {
        val got = plan(autoWatermark = true)
        assertEquals(AutomationPlan(edit = false, watermark = true, print = false, skipped = emptyList()), got)
    }

    @Test
    fun printOnlyWhenReady() {
        val got = plan(autoPrint = true)
        assertEquals(AutomationPlan(edit = false, watermark = false, print = true, skipped = emptyList()), got)
    }

    @Test
    fun editAndWatermark() {
        val got = plan(autoEdit = true, autoWatermark = true)
        assertTrue(got.edit)
        assertTrue(got.watermark)
        assertFalse(got.print)
        assertTrue(got.skipped.isEmpty())
    }

    @Test
    fun editAndPrint() {
        val got = plan(autoEdit = true, autoPrint = true)
        assertTrue(got.edit)
        assertFalse(got.watermark)
        assertTrue(got.print)
        assertTrue(got.skipped.isEmpty())
    }

    @Test
    fun watermarkAndPrint() {
        val got = plan(autoWatermark = true, autoPrint = true)
        assertFalse(got.edit)
        assertTrue(got.watermark)
        assertTrue(got.print)
        assertTrue(got.skipped.isEmpty())
    }

    @Test
    fun allSwitchesOnWhenReady() {
        val got = plan(autoEdit = true, autoWatermark = true, autoPrint = true)
        assertEquals(AutomationPlan(edit = true, watermark = true, print = true, skipped = emptyList()), got)
    }

    @Test
    fun skipsEditWhenNoPreset() {
        val got = plan(autoEdit = true, presetId = AI_PRESET_NONE)
        assertFalse(got.edit)
        assertEquals(listOf(SKIP_EDIT_NO_PRESET), got.skipped)
    }

    @Test
    fun skipsEditWhenNoKey() {
        val got = plan(autoEdit = true, aiConfigured = false)
        assertFalse(got.edit)
        assertEquals(listOf(SKIP_EDIT_NO_KEY), got.skipped)
    }

    @Test
    fun skipsEditWhenNoPresetAndNoKey() {
        val got = plan(autoEdit = true, presetId = AI_PRESET_NONE, aiConfigured = false)
        assertFalse(got.edit)
        assertEquals(listOf(SKIP_EDIT_NO_PRESET, SKIP_EDIT_NO_KEY), got.skipped)
    }

    @Test
    fun skipsWatermarkWhenNotConfigured() {
        val got = plan(autoWatermark = true, watermarkConfigured = false)
        assertFalse(got.watermark)
        assertEquals(listOf(SKIP_WATERMARK_NO_CONFIG), got.skipped)
    }

    @Test
    fun skipsPrintWhenNoPrinter() {
        val got = plan(autoPrint = true, printerBound = false)
        assertFalse(got.print)
        assertEquals(listOf(SKIP_PRINT_NO_PRINTER), got.skipped)
    }

    @Test
    fun disabledStepsDoNotEmitSkipReasons() {
        val got = plan(
            autoEdit = false,
            autoWatermark = false,
            autoPrint = false,
            presetId = AI_PRESET_NONE,
            aiConfigured = false,
            watermarkConfigured = false,
            printerBound = false,
        )
        assertEquals(emptyList<String>(), got.skipped)
    }

    @Test
    fun laterStepsRunAfterEarlierSkips() {
        val got = plan(
            autoEdit = true,
            autoWatermark = true,
            autoPrint = true,
            presetId = AI_PRESET_NONE,
            aiConfigured = false,
            watermarkConfigured = false,
            printerBound = true,
        )
        assertFalse(got.edit)
        assertFalse(got.watermark)
        assertTrue(got.print)
        assertEquals(
            listOf(SKIP_EDIT_NO_PRESET, SKIP_EDIT_NO_KEY, SKIP_WATERMARK_NO_CONFIG),
            got.skipped,
        )
    }

    @Test
    fun eachSkipReasonCanAppearTogether() {
        val got = plan(
            autoEdit = true,
            autoWatermark = true,
            autoPrint = true,
            presetId = AI_PRESET_NONE,
            aiConfigured = false,
            watermarkConfigured = false,
            printerBound = false,
        )
        assertEquals(
            listOf(
                SKIP_EDIT_NO_PRESET,
                SKIP_EDIT_NO_KEY,
                SKIP_WATERMARK_NO_CONFIG,
                SKIP_PRINT_NO_PRINTER,
            ),
            got.skipped,
        )
        assertFalse(got.edit)
        assertFalse(got.watermark)
        assertFalse(got.print)
    }
}
