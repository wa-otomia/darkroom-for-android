package app.darkroom.android.data.imaging

import android.content.Context
import android.graphics.Bitmap
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

/**
 * Draws [WatermarkSettings] onto an already-composed print sheet.
 *
 * Call after the photo is painted and, on the placement path, before the landscape
 * sheet quarter-turn so the marks stay upright relative to the photo.
 */
object WatermarkRenderer {
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

    fun snsLogoRes(logo: SnsLogo): Int = when (logo) {
        SnsLogo.INSTAGRAM -> R.drawable.ic_sns_instagram
        SnsLogo.X -> R.drawable.ic_sns_x
        SnsLogo.FACEBOOK -> R.drawable.ic_sns_facebook
        SnsLogo.WEIBO -> R.drawable.ic_sns_weibo
    }

    private fun drawBox(canvas: Canvas, box: WatermarkBox, context: Context?, textPaint: Paint) {
        val logo = box.logo
        if (logo != null && box.logoSize > 0f && context != null) {
            val drawable = ContextCompat.getDrawable(context, snsLogoRes(logo))?.mutate()
            if (drawable != null) {
                drawable.setTint(Color.WHITE)
                val size = box.logoSize.toInt().coerceAtLeast(1)
                val left = Math.round(box.left)
                val top = Math.round(box.top + (box.height - box.logoSize) / 2f)
                drawable.setBounds(left, top, left + size, top + size)
                drawable.draw(canvas)
            }
        }
        if (box.text.isEmpty() || box.textSize <= 0f) return
        textPaint.textSize = box.textSize
        textPaint.style = Paint.Style.FILL
        textPaint.color = Color.WHITE
        val stroke = Paint(textPaint).apply {
            style = Paint.Style.STROKE
            strokeWidth = (box.textSize * 0.08f).coerceAtLeast(1f)
            color = Color.argb(180, 0, 0, 0)
            strokeJoin = Paint.Join.ROUND
        }
        textPaint.setShadowLayer(
            box.textSize * 0.14f,
            0f,
            box.textSize * 0.04f,
            Color.argb(160, 0, 0, 0),
        )
        canvas.drawText(box.text, box.textLeft, box.textBaseline, stroke)
        canvas.drawText(box.text, box.textLeft, box.textBaseline, textPaint)
        textPaint.clearShadowLayer()
    }
}
