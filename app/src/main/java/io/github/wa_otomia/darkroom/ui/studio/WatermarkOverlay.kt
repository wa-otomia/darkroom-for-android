package io.github.wa_otomia.darkroom.ui.studio

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import io.github.wa_otomia.darkroom.core.PRINT_ASPECT
import io.github.wa_otomia.darkroom.core.PRINT_ASPECT_LANDSCAPE
import io.github.wa_otomia.darkroom.core.PhotoMeta
import io.github.wa_otomia.darkroom.core.WatermarkSettings
import io.github.wa_otomia.darkroom.core.formatWatermarkDate
import io.github.wa_otomia.darkroom.core.largestFrameSize
import io.github.wa_otomia.darkroom.core.watermarkLayout
import io.github.wa_otomia.darkroom.ui.components.drawWatermarkMark
import io.github.wa_otomia.darkroom.ui.components.rememberLogoGlow

/**
 * Read-only preview of [WatermarkSettings] over the Studio crop frame.
 *
 * Drawn with [Modifier.drawWithContent] on the same box as [content] so the
 * cropper still receives gestures. Geometry comes from [watermarkLayout], which picks
 * the portrait or landscape anchors from the frame's orientation.
 */
@Composable
fun WatermarkOverlay(
    settings: WatermarkSettings,
    photo: PhotoMeta,
    landscape: Boolean,
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    var container by remember { mutableStateOf(IntSize.Zero) }
    val aspect = if (landscape) PRINT_ASPECT_LANDSCAPE.toFloat() else PRINT_ASPECT.toFloat()
    val (frameW, frameH) = largestFrameSize(container.width.toFloat(), container.height.toFloat(), aspect)
    val dateText = formatWatermarkDate(photo.createdAt, settings.date.includeTime)
    val measureText: (String, Float) -> Float = { text, sizePx ->
        if (text.isEmpty() || sizePx <= 0f) {
            0f
        } else {
            measurer.measure(
                text = AnnotatedString(text),
                style = TextStyle(
                    fontSize = with(density) { sizePx.toSp() },
                    fontFamily = FontFamily.SansSerif,
                ),
            ).size.width.toFloat()
        }
    }
    val boxes = remember(settings, frameW, frameH, dateText, visible) {
        if (!visible || frameW <= 0f || frameH <= 0f) {
            emptyList()
        } else {
            watermarkLayout(settings, frameW, frameH, measureText, dateText)
        }
    }
    val snsBox = boxes.firstOrNull { it.id == "sns" }
    val glow = rememberLogoGlow(snsBox?.logo, snsBox?.logoSize ?: 0f)
    Box(
        modifier
            .onSizeChanged { container = it }
            .drawWithContent {
                drawContent()
                if (!visible || frameW <= 0f || boxes.isEmpty()) return@drawWithContent
                val left = (size.width - frameW) / 2f
                val top = (size.height - frameH) / 2f
                for (box in boxes) {
                    drawWatermarkMark(
                        box = box,
                        frameLeft = left,
                        frameTop = top,
                        glow = glow,
                        measurer = measurer,
                        density = density,
                    )
                }
            },
    ) {
        content()
    }
}
