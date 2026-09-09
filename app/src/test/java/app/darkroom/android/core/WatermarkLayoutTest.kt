package app.darkroom.android.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset

class WatermarkLayoutTest {
    private val measure: (String, Float) -> Float = { text, size -> text.length * size * 0.5f }

    @Test
    fun defaultSnsAnchorMapsToPixels() {
        val settings = WatermarkSettings(sns = SnsWatermark(enabled = true, handle = "darkroom"))
        val boxes = watermarkLayout(settings, 1040f, 1560f, measure)
        assertEquals(1, boxes.size)
        val box = boxes[0]
        assertEquals("sns", box.id)
        assertEquals(SnsLogo.INSTAGRAM, box.logo)
        assertEquals(0.15f * 1040f, box.cx, 0.5f)
        assertEquals(0.95f * 1560f, box.cy, 0.5f)
        assertEquals(clampWatermarkScale(0.035f) * 1560f, box.logoSize, 0.01f)
        assertEquals(box.logoSize, box.height, 0.01f)
        assertTrue(box.width > box.logoSize)
        assertEquals(box.left + box.logoSize + box.logoSize * 0.28f, box.textLeft, 0.5f)
    }

    @Test
    fun dateBoxUsesAnchorAndFormattedText() {
        val settings = WatermarkSettings(date = DateWatermark(enabled = true, includeTime = false))
        val boxes = watermarkLayout(settings, 1040f, 1560f, measure, dateText = "2024.01.15")
        assertEquals(1, boxes.size)
        val box = boxes[0]
        assertEquals("date", box.id)
        assertEquals(null, box.logo)
        assertEquals(0.85f * 1040f, box.cx, 0.5f)
        assertEquals(0.95f * 1560f, box.cy, 0.5f)
        assertEquals("2024.01.15", box.text)
        assertEquals(0f, box.logoSize, 0f)
        assertEquals(box.left, box.textLeft, 0.01f)
    }

    @Test
    fun bothMarksLayoutWhenEnabled() {
        val settings = WatermarkSettings(
            sns = SnsWatermark(enabled = true, handle = "x"),
            date = DateWatermark(enabled = true),
        )
        val boxes = watermarkLayout(settings, 200f, 300f, measure, dateText = "2024.01.15")
        assertEquals(listOf("sns", "date"), boxes.map { it.id })
    }

    @Test
    fun disabledMarksAreOmitted() {
        assertTrue(watermarkLayout(WatermarkSettings(), 1040f, 1560f, measure).isEmpty())
    }

    @Test
    fun scaleIsClampedToBounds() {
        val tiny = WatermarkSettings(sns = SnsWatermark(enabled = true, scale = 0.005f, handle = ""))
        val huge = WatermarkSettings(sns = SnsWatermark(enabled = true, scale = 0.5f, handle = ""))
        val t = watermarkLayout(tiny, 1000f, 1000f, measure).single()
        val h = watermarkLayout(huge, 1000f, 1000f, measure).single()
        assertEquals(WATERMARK_SCALE_MIN * 1000f, t.height, 0.01f)
        assertEquals(WATERMARK_SCALE_MAX * 1000f, h.height, 0.01f)
        assertEquals(0.02f, WATERMARK_SCALE_MIN, 0f)
        assertEquals(0.08f, WATERMARK_SCALE_MAX, 0f)
    }

    @Test
    fun safeLineGuidesUseBoxHalfSize() {
        val box = WatermarkBox(
            id = "sns",
            left = 100f,
            top = 200f,
            width = 80f,
            height = 40f,
        )
        val (xs, ys) = watermarkSnapGuides(box, 1040f, 1560f, inset = 52f)
        assertEquals(520f, xs.first { it.id == GUIDE_CENTER_X }.value, 0.01f)
        assertEquals(780f, ys.first { it.id == GUIDE_CENTER_Y }.value, 0.01f)
        assertEquals(52f + 40f, xs.first { it.id == GUIDE_SAFE_X_NEG }.value, 0.01f)
        assertEquals(1040f - 52f - 40f, xs.first { it.id == GUIDE_SAFE_X_POS }.value, 0.01f)
        assertEquals(52f + 20f, ys.first { it.id == GUIDE_SAFE_Y_NEG }.value, 0.01f)
        assertEquals(1560f - 52f - 20f, ys.first { it.id == GUIDE_SAFE_Y_POS }.value, 0.01f)
    }

    @Test
    fun dateFormattingUsesIsoWithFallback() {
        val utc = ZoneOffset.UTC
        assertEquals("2024.01.15", formatWatermarkDate("2024-01-15T14:30:00Z", false, zone = utc))
        assertEquals("2024.01.15 14:30", formatWatermarkDate("2024-01-15T14:30:00Z", true, zone = utc))
        assertEquals(
            "2023.12.31",
            formatWatermarkDate("not-a-date", false, fallbackIso = "2023-12-31T23:00:00Z", zone = utc),
        )
        assertEquals("", formatWatermarkDate("nope", false, fallbackIso = "also-nope", zone = utc))
        assertEquals("2024.06.01", formatWatermarkDate("2024-06-01", false, zone = utc))
        assertEquals("2024.06.01 08:09", formatWatermarkDate("2024:06:01 08:09:00", true, zone = utc))
    }

    @Test
    fun settingsDefaultsAndConfiguredFlag() {
        val empty = WatermarkSettings()
        assertFalseConfigured(empty)
        assertEquals(SnsLogo.INSTAGRAM, empty.sns.logo)
        assertEquals(0.15f, empty.sns.anchor.cx, 0f)
        assertEquals(0.95f, empty.sns.anchor.cy, 0f)
        assertEquals(0.85f, empty.date.anchor.cx, 0f)
        assertTrue(WatermarkSettings(sns = SnsWatermark(enabled = true)).isConfigured())
        assertTrue(WatermarkSettings(date = DateWatermark(enabled = true)).isConfigured())
    }

    private fun assertFalseConfigured(settings: WatermarkSettings) {
        assertEquals(false, settings.isConfigured())
    }
}
