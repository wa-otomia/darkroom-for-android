package app.darkroom.android.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import app.darkroom.android.core.SnsLogo
import app.darkroom.android.core.WatermarkBox
import app.darkroom.android.data.imaging.WatermarkRenderer
import kotlin.math.roundToInt

/** Logo glyph with its shadow baked in (see [WatermarkRenderer.logoGlowBitmap]) plus its padding. */
class LogoGlow(val image: ImageBitmap, val pad: Int)

/** Rebuilt only when [logo] or the integer [sizePx] changes; `null` for no logo. */
@Composable
fun rememberLogoGlow(logo: SnsLogo?, sizePx: Float): LogoGlow? {
    val context = LocalContext.current
    val size = sizePx.roundToInt()
    return remember(logo, size, context) {
        if (logo == null || logo == SnsLogo.NONE || size <= 0) {
            null
        } else {
            WatermarkRenderer.logoGlowBitmap(context, logo, size)?.let {
                LogoGlow(it.asImageBitmap(), WatermarkRenderer.glowPad(size))
            }
        }
    }
}

/** Text style matching the print path: white sans-serif with the renderer's soft shadow ratios. */
fun watermarkTextStyle(sizePx: Float, density: Density): TextStyle = TextStyle(
    fontSize = with(density) { sizePx.toSp() },
    fontFamily = FontFamily.SansSerif,
    color = Color.White,
    shadow = Shadow(
        color = Color.Black.copy(alpha = WatermarkRenderer.GLOW_ALPHA),
        offset = Offset(0f, sizePx * WatermarkRenderer.GLOW_DY),
        blurRadius = (sizePx * WatermarkRenderer.GLOW_BLUR).coerceAtLeast(0.5f),
    ),
)

/**
 * Draws one [WatermarkBox] (geometry from `watermarkLayout`) with the frame's top-left at
 * ([frameLeft], [frameTop]) in this scope. Glyph and text share the print renderer's baseline
 * and shadow so previews match the sheet.
 */
fun DrawScope.drawWatermarkMark(
    box: WatermarkBox,
    frameLeft: Float,
    frameTop: Float,
    glow: LogoGlow?,
    measurer: TextMeasurer,
    density: Density,
) {
    if (glow != null && box.logo != null && box.logoSize > 0f) {
        val logoTop = box.top + (box.height - box.logoSize) / 2f
        drawImage(
            image = glow.image,
            topLeft = Offset(
                frameLeft + box.left - glow.pad,
                frameTop + logoTop - glow.pad,
            ),
        )
    }
    if (box.text.isEmpty() || box.textSize <= 0f) return
    val layout = measurer.measure(
        text = AnnotatedString(box.text),
        style = watermarkTextStyle(box.textSize, density),
        softWrap = false,
        maxLines = 1,
    )
    drawText(
        textLayoutResult = layout,
        topLeft = Offset(
            frameLeft + box.textLeft,
            frameTop + box.textBaseline - layout.firstBaseline,
        ),
    )
}
