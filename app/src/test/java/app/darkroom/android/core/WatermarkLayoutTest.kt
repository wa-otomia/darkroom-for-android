package app.darkroom.android.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset

class WatermarkLayoutTest {
    private val measure: (String, Float) -> Float = { text, size -> text.length * size * 0.5f }

    @Test
    fun defaultSnsAnchorMapsToPixels() {
        val frameW = 1040f
        val frameH = 1560f
        val settings = WatermarkSettings(sns = SnsWatermark(enabled = true, handle = "x"))
        val boxes = watermarkLayout(settings, frameW, frameH, measure)
        assertEquals(1, boxes.size)
        val box = boxes[0]
        assertEquals("sns", box.id)
        assertEquals(SnsLogo.INSTAGRAM, box.logo)
        assertEquals(0.15f * frameW, box.cx, 0.5f)
        val inset = safeInset(frameW, frameH)
        assertEquals(frameH - inset - box.height / 2f, box.cy, 0.5f)
        assertInsideSafeArea(box, frameW, frameH)
        assertEquals(clampWatermarkScale(0.035f) * frameH, box.logoSize, 0.01f)
        assertEquals(box.logoSize, box.height, 0.01f)
        assertTrue(box.width > box.logoSize)
        assertEquals(box.left + box.logoSize + box.logoSize * 0.28f, box.textLeft, 0.5f)
    }

    @Test
    fun dateBoxUsesAnchorAndFormattedText() {
        val settings = WatermarkSettings(date = DateWatermark(enabled = true, includeTime = false))
        val boxes = watermarkLayout(settings, 1040f, 1560f, measure, dateText = "2024.01")
        assertEquals(1, boxes.size)
        val box = boxes[0]
        assertEquals("date", box.id)
        assertEquals(null, box.logo)
        assertEquals(0.85f * 1040f, box.cx, 0.5f)
        assertEquals(0.95f * 1560f, box.cy, 0.5f)
        assertEquals("2024.01", box.text)
        assertEquals(0f, box.logoSize, 0f)
        assertEquals(box.left, box.textLeft, 0.01f)
        assertInsideSafeArea(box, 1040f, 1560f)
    }

    @Test
    fun defaultSnsAnchorWithLongHandleStaysInsideSafeArea() {
        val frameW = 1040f
        val frameH = 1560f
        val settings = WatermarkSettings(sns = SnsWatermark(enabled = true, handle = "thirteenchars"))
        val box = watermarkLayout(settings, frameW, frameH, measure).single()
        val inset = safeInset(frameW, frameH)
        assertInsideSafeArea(box, frameW, frameH)
        assertEquals(inset, box.left, 0.5f)
        assertTrue(box.cx > 0.15f * frameW)
    }

    @Test
    fun dateWithTimeStaysInsideRightSafeEdge() {
        val frameW = 1040f
        val frameH = 1560f
        val settings = WatermarkSettings(date = DateWatermark(enabled = true, includeTime = true))
        val box = watermarkLayout(settings, frameW, frameH, measure, dateText = "2026.09.09 14:53").single()
        val inset = safeInset(frameW, frameH)
        assertInsideSafeArea(box, frameW, frameH)
        assertEquals(frameW - inset, box.right, 0.5f)
        assertTrue(box.cx < 0.85f * frameW)
    }

    @Test
    fun hugeScalePinsToInsetRatherThanGoingNegative() {
        val frameW = 200f
        val frameH = 300f
        val settings = WatermarkSettings(
            sns = SnsWatermark(enabled = true, scale = 0.5f, handle = "thirteenchars"),
        )
        val box = watermarkLayout(settings, frameW, frameH, measure).single()
        val inset = safeInset(frameW, frameH)
        assertTrue(box.width > frameW - 2f * inset)
        assertEquals(inset, box.left, 0.01f)
        assertTrue(box.left >= 0f)
        assertTrue(box.top + 0.01f >= inset)
    }

    @Test
    fun dragEndAnchorUsesClampedBoxCentre() {
        val frameW = 200f
        val frameH = 300f
        val width = 180f
        val height = 20f
        val inset = safeInset(frameW, frameH)
        val anchor = clampedWatermarkAnchor(
            centerX = 0.15f * frameW,
            centerY = 0.95f * frameH,
            width = width,
            height = height,
            frameW = frameW,
            frameH = frameH,
        )
        val left = anchor.cx * frameW - width / 2f
        val top = anchor.cy * frameH - height / 2f
        assertEquals(inset, left, 0.01f)
        assertTrue(top + height <= frameH - inset + 0.01f)
        assertTrue(left >= 0f)
    }

    @Test
    fun landscapeFrameUsesLandscapeAnchorsAndLongEdgeScale() {
        val frameW = 1560f
        val frameH = 1040f
        val settings = WatermarkSettings(
            sns = SnsWatermark(
                enabled = true,
                handle = "x",
                anchor = WatermarkAnchor(0.5f, 0.5f),
                landscapeAnchor = WatermarkAnchor(0.3f, 0.4f),
            ),
            date = DateWatermark(
                enabled = true,
                anchor = WatermarkAnchor(0.5f, 0.5f),
                landscapeAnchor = WatermarkAnchor(0.7f, 0.6f),
            ),
        )
        assertTrue(isLandscapeFrame(frameW, frameH))
        val boxes = watermarkLayout(settings, frameW, frameH, measure, dateText = "2024.01.15")
        val sns = boxes.first { it.id == "sns" }
        val date = boxes.first { it.id == "date" }
        assertEquals(0.3f * frameW, sns.cx, 0.5f)
        assertEquals(0.4f * frameH, sns.cy, 0.5f)
        assertEquals(0.7f * frameW, date.cx, 0.5f)
        assertEquals(0.6f * frameH, date.cy, 0.5f)
        // Same physical size as on the portrait sheet: scale × long edge, not × frame height.
        assertEquals(0.035f * frameW, sns.logoSize, 0.01f)
        assertEquals(0.03f * frameW, date.textSize, 0.01f)
        val portrait = watermarkLayout(settings, frameH, frameW, measure, dateText = "2024.01.15")
        assertEquals(sns.logoSize, portrait.first { it.id == "sns" }.logoSize, 0.01f)
        assertEquals(0.5f * frameH, portrait.first { it.id == "sns" }.cx, 0.5f)
    }

    @Test
    fun landscapeDefaultsStayInsideSafeArea() {
        val settings = WatermarkSettings(
            sns = SnsWatermark(enabled = true, handle = "thirteenchars"),
            date = DateWatermark(enabled = true, includeTime = true),
        )
        val boxes = watermarkLayout(settings, 1560f, 1040f, measure, dateText = "2026.09.09 14:53")
        boxes.forEach { assertInsideSafeArea(it, 1560f, 1040f) }
        assertTrue(boxes.first { it.id == "sns" }.cx < boxes.first { it.id == "date" }.cx)
    }

    @Test
    fun noneLogoDrawsHandleOnly() {
        val settings = WatermarkSettings(sns = SnsWatermark(enabled = true, logo = SnsLogo.NONE, handle = "abc"))
        val box = watermarkLayout(settings, 1040f, 1560f, measure).single()
        assertEquals(null, box.logo)
        assertEquals(0f, box.logoSize, 0f)
        assertEquals(box.left, box.textLeft, 0.01f)
        assertEquals(box.textSize, box.height, 0.01f)
        assertEquals(measure("abc", box.textSize), box.width, 0.01f)
    }

    @Test
    fun textBaselineCentresMixedCaseOnLogo() {
        val settings = WatermarkSettings(sns = SnsWatermark(enabled = true, handle = "abc"))
        val box = watermarkLayout(settings, 1040f, 1560f, measure).single()
        assertEquals(box.top + box.height * WATERMARK_TEXT_BASELINE, box.textBaseline, 0.01f)
        assertEquals(0.83f, WATERMARK_TEXT_BASELINE, 0f)
    }

    @Test
    fun withDefaultAnchorsResetsBothOrientationsOnly() {
        val custom = WatermarkSettings(
            sns = SnsWatermark(
                enabled = true,
                logo = SnsLogo.X,
                handle = "h",
                scale = 0.07f,
                anchor = WatermarkAnchor(0.5f, 0.5f),
                landscapeAnchor = WatermarkAnchor(0.4f, 0.4f),
            ),
            date = DateWatermark(
                enabled = true,
                includeTime = true,
                scale = 0.06f,
                anchor = WatermarkAnchor(0.5f, 0.5f),
                landscapeAnchor = WatermarkAnchor(0.6f, 0.6f),
            ),
        )
        val reset = custom.withDefaultAnchors()
        assertEquals(SnsWatermark().anchor, reset.sns.anchor)
        assertEquals(SnsWatermark().landscapeAnchor, reset.sns.landscapeAnchor)
        assertEquals(DateWatermark().anchor, reset.date.anchor)
        assertEquals(DateWatermark().landscapeAnchor, reset.date.landscapeAnchor)
        assertEquals(0.07f, reset.sns.scale, 0f)
        assertEquals(0.06f, reset.date.scale, 0f)
        assertEquals(SnsLogo.X, reset.sns.logo)
        assertEquals("h", reset.sns.handle)
        assertTrue(reset.sns.enabled && reset.date.enabled && reset.date.includeTime)
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

    private fun assertInsideSafeArea(box: WatermarkBox, frameW: Float, frameH: Float) {
        val inset = safeInset(frameW, frameH)
        assertTrue(box.left + 0.01f >= inset)
        assertTrue(box.right <= frameW - inset + 0.01f)
        assertTrue(box.top + 0.01f >= inset)
        assertTrue(box.bottom <= frameH - inset + 0.01f)
    }
}
