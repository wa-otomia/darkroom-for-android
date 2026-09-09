package app.darkroom.android.data.imaging

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import app.darkroom.android.R
import app.darkroom.android.core.PhotoMeta
import app.darkroom.android.core.SnsLogo
import app.darkroom.android.core.WatermarkBox
import app.darkroom.android.core.WatermarkSettings
import app.darkroom.android.core.formatWatermarkDate
import app.darkroom.android.core.watermarkLayout
import kotlin.math.ceil

/**
 * Draws [WatermarkSettings] onto an already-composed print sheet.
 *
 * Call after the photo is painted and, on the placement path, before the landscape
 * sheet quarter-turn so the marks stay upright relative to the photo.
 *
 * Marks are white with a soft dark shadow (no outline). The same blur/offset/alpha ratios
 * are used by the Compose previews so Settings, Studio and the printed sheet look alike.
 */
object WatermarkRenderer {
    /** Shadow blur radius as a fraction of the mark size. */
    const val GLOW_BLUR = 0.16f

    /** Shadow vertical offset as a fraction of the mark size. */
    const val GLOW_DY = 0.03f

    /** Shadow alpha (0–1). */
    const val GLOW_ALPHA = 0.6f

    /** Padding around a logo glyph so its blurred shadow is not clipped, as a fraction of size. */
    private const val GLOW_PAD = 0.4f

    fun apply(
        sheet: Bitmap,
        settings: WatermarkSettings,
        photo: PhotoMeta,
        frameW: Int,
        frameH: Int,
        context: Context? = null,
    ) {
        if (!settings.sns.enabled && !settings.date.enabled) return
        if (frameW <= 0 || frameH <= 0) return
        val canvas = Canvas(sheet)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = Typeface.SANS_SERIF
            isSubpixelText = true
        }
        val measure: (String, Float) -> Float = { text, size ->
            paint.textSize = size
            paint.measureText(text)
        }
        val dateText = formatWatermarkDate(
            createdAt = photo.createdAt,
            includeTime = settings.date.includeTime,
            fallbackIso = photo.ingestedAt,
        )
        val boxes = watermarkLayout(settings, frameW.toFloat(), frameH.toFloat(), measure, dateText)
        for (box in boxes) {
            drawBox(canvas, box, context, paint)
        }
    }

    /** Drawable for [logo], or `null` for [SnsLogo.NONE]. */
    fun snsLogoRes(logo: SnsLogo): Int? = when (logo) {
        SnsLogo.INSTAGRAM -> R.drawable.ic_sns_instagram
        SnsLogo.X -> R.drawable.ic_sns_x
        SnsLogo.FACEBOOK -> R.drawable.ic_sns_facebook
        SnsLogo.WEIBO -> R.drawable.ic_sns_weibo
        SnsLogo.NONE -> null
    }

    /** Pixels of transparent margin on every side of [logoGlowBitmap] for a glyph of [sizePx]. */
    fun glowPad(sizePx: Int): Int = ceil(sizePx.coerceAtLeast(1) * GLOW_PAD).toInt()

    /**
     * White [logo] glyph of [sizePx] with its soft shadow baked in, padded by [glowPad] on each
     * side. Draw it at `(left - pad, top - pad)`. Shared by the print path and the Compose
     * previews so the glow is identical everywhere. `null` for [SnsLogo.NONE].
     */
    fun logoGlowBitmap(context: Context, logo: SnsLogo, sizePx: Int): Bitmap? {
        val res = snsLogoRes(logo) ?: return null
        val size = sizePx.coerceAtLeast(1)
        val drawable = ContextCompat.getDrawable(context, res)?.mutate() ?: return null
        drawable.setTint(Color.WHITE)
        val glyph = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(Canvas(glyph))

        val pad = glowPad(size)
        val out = Bitmap.createBitmap(size + 2 * pad, size + 2 * pad, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val blurRadius = (size * GLOW_BLUR).coerceAtLeast(0.5f)
        val offset = IntArray(2)
        val shadowMask = glyph.extractAlpha(
            Paint().apply { maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL) },
            offset,
        )
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            color = Color.argb((GLOW_ALPHA * 255).toInt(), 0, 0, 0)
        }
        canvas.drawBitmap(
            shadowMask,
            (pad + offset[0]).toFloat(),
            pad + offset[1] + size * GLOW_DY,
            shadowPaint,
        )
        shadowMask.recycle()
        canvas.drawBitmap(glyph, pad.toFloat(), pad.toFloat(), null)
        glyph.recycle()
        return out
    }

    private fun drawBox(canvas: Canvas, box: WatermarkBox, context: Context?, textPaint: Paint) {
        val logo = box.logo
        if (logo != null && box.logoSize > 0f && context != null) {
            val size = box.logoSize.toInt().coerceAtLeast(1)
            val glow = logoGlowBitmap(context, logo, size)
            if (glow != null) {
                val pad = glowPad(size)
                val left = Math.round(box.left) - pad
                val top = Math.round(box.top + (box.height - box.logoSize) / 2f) - pad
                canvas.drawBitmap(glow, left.toFloat(), top.toFloat(), null)
                glow.recycle()
            }
        }
        if (box.text.isEmpty() || box.textSize <= 0f) return
        textPaint.textSize = box.textSize
        textPaint.style = Paint.Style.FILL
        textPaint.color = Color.WHITE
        textPaint.setShadowLayer(
            (box.textSize * GLOW_BLUR).coerceAtLeast(0.5f),
            0f,
            box.textSize * GLOW_DY,
            Color.argb((GLOW_ALPHA * 255).toInt(), 0, 0, 0),
        )
        canvas.drawText(box.text, box.textLeft, box.textBaseline, textPaint)
        textPaint.clearShadowLayer()
    }
}
