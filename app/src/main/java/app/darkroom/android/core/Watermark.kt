package app.darkroom.android.core

import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** SNS mark drawn to the left of the handle. */
@Serializable
enum class SnsLogo { INSTAGRAM, X, FACEBOOK, WEIBO }

/**
 * Centre of a watermark box, normalized to the paper frame (`0..1`).
 * `(0.15, 0.95)` is the lower-left safe corner on a 2:3 portrait sheet.
 */
@Serializable
data class WatermarkAnchor(val cx: Float, val cy: Float)

/** Handle + logo. [scale] is text/logo height as a fraction of frame height (clamped 0.02–0.08). */
@Serializable
data class SnsWatermark(
    val enabled: Boolean = false,
    val logo: SnsLogo = SnsLogo.INSTAGRAM,
    val handle: String = "",
    val scale: Float = 0.035f,
    val anchor: WatermarkAnchor = WatermarkAnchor(0.15f, 0.95f),
)

/** Capture-date mark. [includeTime] switches `yyyy.MM.dd` ↔ `yyyy.MM.dd HH:mm`. */
@Serializable
data class DateWatermark(
    val enabled: Boolean = false,
    val includeTime: Boolean = false,
    val scale: Float = 0.03f,
    val anchor: WatermarkAnchor = WatermarkAnchor(0.85f, 0.95f),
)

/** Persisted watermark configuration. Overlay happens only at print/export time. */
@Serializable
data class WatermarkSettings(
    val sns: SnsWatermark = SnsWatermark(),
    val date: DateWatermark = DateWatermark(),
)

/** True when automation / Studio should treat a watermark as configured. */
fun WatermarkSettings.isConfigured(): Boolean = sns.enabled || date.enabled

const val WATERMARK_SCALE_MIN = 0.02f
const val WATERMARK_SCALE_MAX = 0.08f

/** Pixel box shared by preview, snap guides, and [app.darkroom.android.data.imaging.WatermarkRenderer]. */
data class WatermarkBox(
    val id: String,
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
    val logo: SnsLogo? = null,
    val logoSize: Float = 0f,
    val text: String = "",
    val textSize: Float = 0f,
    val textLeft: Float = 0f,
    val textBaseline: Float = 0f,
) {
    val right: Float get() = left + width
    val bottom: Float get() = top + height
    val cx: Float get() = left + width / 2f
    val cy: Float get() = top + height / 2f
}

fun clampWatermarkScale(scale: Float): Float = scale.coerceIn(WATERMARK_SCALE_MIN, WATERMARK_SCALE_MAX)

/**
 * Pixel boxes for every enabled watermark.
 *
 * [measureText] is `(text, fontPx) -> widthPx` so Compose and Android [android.graphics.Paint]
 * can share the same geometry. Pass [dateText] (already formatted) when the date mark is on;
 * leave it empty to size an empty date box.
 */
fun watermarkLayout(
    settings: WatermarkSettings,
    frameW: Float,
    frameH: Float,
    measureText: (String, Float) -> Float,
    dateText: String = "",
): List<WatermarkBox> {
    if (frameW <= 0f || frameH <= 0f) return emptyList()
    val boxes = ArrayList<WatermarkBox>(2)
    if (settings.sns.enabled) {
        boxes += layoutWatermark(
            id = "sns",
            logo = settings.sns.logo,
            text = settings.sns.handle,
            scale = settings.sns.scale,
            anchor = settings.sns.anchor,
            frameW = frameW,
            frameH = frameH,
            measureText = measureText,
        )
    }
    if (settings.date.enabled) {
        boxes += layoutWatermark(
            id = "date",
            logo = null,
            text = dateText,
            scale = settings.date.scale,
            anchor = settings.date.anchor,
            frameW = frameW,
            frameH = frameH,
            measureText = measureText,
        )
    }
    return boxes
}

/**
 * Snap targets for a watermark box centre, in frame pixels:
 * vertical/horizontal midlines, plus box-edge-to-safe-line
 * (`inset + w/2`, `frame − inset − w/2`, and the same on y).
 */
fun watermarkSnapGuides(
    box: WatermarkBox,
    frameW: Float,
    frameH: Float,
    inset: Float,
): Pair<List<SnapGuide>, List<SnapGuide>> {
    val halfW = box.width / 2f
    val halfH = box.height / 2f
    val x = listOf(
        SnapGuide(GUIDE_CENTER_X, frameW / 2f),
        SnapGuide(GUIDE_SAFE_X_NEG, inset + halfW),
        SnapGuide(GUIDE_SAFE_X_POS, frameW - inset - halfW),
    )
    val y = listOf(
        SnapGuide(GUIDE_CENTER_Y, frameH / 2f),
        SnapGuide(GUIDE_SAFE_Y_NEG, inset + halfH),
        SnapGuide(GUIDE_SAFE_Y_POS, frameH - inset - halfH),
    )
    return x to y
}

private val EXIF_LOCAL = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")
private val EXIF_DATE = DateTimeFormatter.ofPattern("yyyy:MM:dd")

/**
 * Formats an ISO-8601 (or EXIF) timestamp as `yyyy.MM.dd` / `yyyy.MM.dd HH:mm`.
 * Tries [createdAt] first, then [fallbackIso]. Returns `""` if neither parses.
 *
 * Tests should pass an explicit [zone] (typically [ZoneOffset.UTC]); the default is the
 * device zone so a print matches the wall-clock date the photo was taken.
 */
fun formatWatermarkDate(
    createdAt: String,
    includeTime: Boolean,
    fallbackIso: String? = null,
    zone: ZoneId = ZoneId.systemDefault(),
): String {
    val instant = parseWatermarkInstant(createdAt) ?: fallbackIso?.let { parseWatermarkInstant(it) }
        ?: return ""
    val zoned = instant.atZone(zone)
    val date = "%04d.%02d.%02d".format(zoned.year, zoned.monthValue, zoned.dayOfMonth)
    if (!includeTime) return date
    return "$date %02d:%02d".format(zoned.hour, zoned.minute)
}

fun parseWatermarkInstant(raw: String): Instant? {
    val s = raw.trim()
    if (s.isEmpty()) return null
    return tryParse { Instant.parse(s) }
        ?: tryParse { OffsetDateTime.parse(s).toInstant() }
        ?: tryParse { LocalDateTime.parse(s).toInstant(ZoneOffset.UTC) }
        ?: tryParse { LocalDate.parse(s).atStartOfDay(ZoneOffset.UTC).toInstant() }
        ?: tryParse { LocalDateTime.parse(s, EXIF_LOCAL).toInstant(ZoneOffset.UTC) }
        ?: tryParse { LocalDate.parse(s, EXIF_DATE).atStartOfDay(ZoneOffset.UTC).toInstant() }
}

private inline fun <T> tryParse(block: () -> T): T? = try {
    block()
} catch (_: Exception) {
    null
}

private fun layoutWatermark(
    id: String,
    logo: SnsLogo?,
    text: String,
    scale: Float,
    anchor: WatermarkAnchor,
    frameW: Float,
    frameH: Float,
    measureText: (String, Float) -> Float,
): WatermarkBox {
    val textSize = clampWatermarkScale(scale) * frameH
    val logoSize = if (logo != null) textSize else 0f
    val textWidth = if (text.isEmpty()) 0f else measureText(text, textSize).coerceAtLeast(0f)
    val gap = if (logoSize > 0f && textWidth > 0f) textSize * 0.28f else 0f
    val width = logoSize + gap + textWidth
    val height = when {
        logoSize > 0f -> logoSize
        textSize > 0f -> textSize
        else -> 0f
    }
    val cx = anchor.cx * frameW
    val cy = anchor.cy * frameH
    val left = cx - width / 2f
    val top = cy - height / 2f
    val textLeft = left + logoSize + gap
    val textBaseline = top + height * 0.78f
    return WatermarkBox(
        id = id,
        left = left,
        top = top,
        width = width,
        height = height,
        logo = logo,
        logoSize = logoSize,
        text = text,
        textSize = textSize,
        textLeft = textLeft,
        textBaseline = textBaseline,
    )
}
