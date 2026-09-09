package app.darkroom.android.ui.studio

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import app.darkroom.android.core.PRINT_ASPECT
import app.darkroom.android.core.PRINT_ASPECT_LANDSCAPE
import app.darkroom.android.core.PhotoMeta
import app.darkroom.android.core.WatermarkBox
import app.darkroom.android.core.WatermarkSettings
import app.darkroom.android.core.formatWatermarkDate
import app.darkroom.android.core.largestFrameSize
import app.darkroom.android.core.watermarkLayout
import app.darkroom.android.data.imaging.WatermarkRenderer

/**
 * Read-only preview of [WatermarkSettings] over the Studio crop frame.
 *
 * Drawn with [Modifier.drawWithContent] on the same box as [content] so the
 * cropper still receives gestures. Geometry comes from [watermarkLayout].
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
    val logoPainter = painterResource(WatermarkRenderer.snsLogoRes(settings.sns.logo))
    Box(
        modifier
            .onSizeChanged { container = it }
            .drawWithContent {
                drawContent()
                if (!visible || frameW <= 0f || boxes.isEmpty()) return@drawWithContent
                val left = (size.width - frameW) / 2f
                val top = (size.height - frameH) / 2f
                for (box in boxes) {
                    drawWatermarkBox(
                        box = box,
                        frameLeft = left,
                        frameTop = top,
                        logoPainter = logoPainter,
                        measure = { text, sizePx, color ->
                            measurer.measure(
                                text = AnnotatedString(text),
                                style = TextStyle(
                                    fontSize = with(density) { sizePx.toSp() },
                                    fontFamily = FontFamily.SansSerif,
                                    color = color,
                                    shadow = Shadow(
                                        color = Color.Black.copy(alpha = 0.63f),
                                        offset = Offset(0f, sizePx * 0.04f),
                                        blurRadius = sizePx * 0.14f,
                                    ),
                                ),
                            )
                        },
                    )
                }
            },
    ) {
        content()
    }
}

private fun DrawScope.drawWatermarkBox(
    box: WatermarkBox,
    frameLeft: Float,
    frameTop: Float,
    logoPainter: Painter,
    measure: (String, Float, Color) -> TextLayoutResult,
) {
    val logo = box.logo
    if (logo != null && box.logoSize > 0f) {
        val logoTop = frameTop + box.top + (box.height - box.logoSize) / 2f
        translate(frameLeft + box.left, logoTop) {
            with(logoPainter) {
                draw(
                    Size(box.logoSize, box.logoSize),
                    colorFilter = ColorFilter.tint(Color.White),
                )
            }
        }
    }
    if (box.text.isEmpty() || box.textSize <= 0f) return
    val layout = measure(box.text, box.textSize, Color.White)
    drawText(
        textLayoutResult = layout,
        topLeft = Offset(
            frameLeft + box.textLeft,
            frameTop + box.textBaseline - layout.firstBaseline,
        ),
    )
}
