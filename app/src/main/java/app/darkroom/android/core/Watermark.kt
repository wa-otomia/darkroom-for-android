package app.darkroom.android.core

import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** SNS mark drawn to the left of the handle. [NONE] prints the handle alone. */
@Serializable
enum class SnsLogo { INSTAGRAM, X, FACEBOOK, WEIBO, NONE }

/**
 * Centre of a watermark box, normalized to the paper frame (`0..1`).
 * `(0.15, 0.95)` is the lower-left safe corner on a 2:3 portrait sheet.
 */
@Serializable
data class WatermarkAnchor(val cx: Float, val cy: Float)

/**
 * Handle + logo. [scale] is text/logo height as a fraction of the frame's **long edge**
 * (clamped 0.02–0.08) so a mark is the same physical size on a portrait and a landscape sheet.
 * [anchor] positions the 2:3 portrait frame, [landscapeAnchor] the 3:2 landscape frame.
 */
@Serializable
data class SnsWatermark(
    val enabled: Boolean = false,
    val logo: SnsLogo = SnsLogo.INSTAGRAM,
    val handle: String = "",
    val scale: Float = 0.035f,
    val anchor: WatermarkAnchor = WatermarkAnchor(0.15f, 0.95f),
    val landscapeAnchor: WatermarkAnchor = WatermarkAnchor(0.12f, 0.93f),
)

/** Capture-date mark. [includeTime] switches `yyyy.MM.dd` ↔ `yyyy.MM.dd HH:mm`. */
@Serializable
data class DateWatermark(
    val enabled: Boolean = false,
    val includeTime: Boolean = false,
    val scale: Float = 0.03f,
    val anchor: WatermarkAnchor = WatermarkAnchor(0.85f, 0.95f),
    val landscapeAnchor: WatermarkAnchor = WatermarkAnchor(0.88f, 0.93f),
)

/** A frame wider than tall is the landscape sheet (before the print quarter-turn). */
fun isLandscapeFrame(frameW: Float, frameH: Float): Boolean = frameW > frameH

fun SnsWatermark.anchorFor(landscape: Boolean): WatermarkAnchor = if (landscape) landscapeAnchor else anchor
fun DateWatermark.anchorFor(landscape: Boolean): WatermarkAnchor = if (landscape) landscapeAnchor else anchor

fun SnsWatermark.withAnchor(landscape: Boolean, value: WatermarkAnchor): SnsWatermark =
    if (landscape) copy(landscapeAnchor = value) else copy(anchor = value)

fun DateWatermark.withAnchor(landscape: Boolean, value: WatermarkAnchor): DateWatermark =
    if (landscape) copy(landscapeAnchor = value) else copy(anchor = value)

/** Both orientations back to their default positions; size, logo, handle and switches untouched. */
fun WatermarkSettings.withDefaultAnchors(): WatermarkSettings {
    val sns0 = SnsWatermark()
    val date0 = DateWatermark()
    return copy(
        sns = sns.copy(anchor = sns0.anchor, landscapeAnchor = sns0.landscapeAnchor),
        date = date.copy(anchor = date0.anchor, landscapeAnchor = date0.landscapeAnchor),
    )
}

/**
 * Baseline of the mark text as a fraction of the box height. Sans-serif caps rise ~0.71 em and
 * x-height ~0.53 em above the baseline, so 0.83 puts the visual centre of a mixed-case handle on
 * the logo's centre line instead of the line box centre (which sits the text too low).
 */
const val WATERMARK_TEXT_BASELINE = 0.83f

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
    val landscape = isLandscapeFrame(frameW, frameH)
    val boxes = ArrayList<WatermarkBox>(2)
    if (settings.sns.enabled) {
        boxes += layoutWatermark(
            id = "sns",
            logo = settings.sns.logo.takeIf { it != SnsLogo.NONE },
            text = settings.sns.handle,
            scale = settings.sns.scale,
            anchor = settings.sns.anchorFor(landscape),
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
            anchor = settings.date.anchorFor(landscape),
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
    val textSize = clampWatermarkScale(scale) * maxOf(frameW, frameH)
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
    val textBaseline = top + height * WATERMARK_TEXT_BASELINE
    return clampWatermarkBox(
        WatermarkBox(
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
        ),
        frameW,
        frameH,
    )
}

/**
 * Pins [box] so every edge stays inside the safe inset. A box wider/taller than the
 * remaining span is locked to the left/top inset rather than going negative.
 */
fun clampWatermarkBox(box: WatermarkBox, frameW: Float, frameH: Float): WatermarkBox {
    if (frameW <= 0f || frameH <= 0f) return box
    val inset = safeInset(frameW, frameH)
    val minLeft = inset
    val maxLeft = frameW - inset - box.width
    val minTop = inset
    val maxTop = frameH - inset - box.height
    val left = if (maxLeft < minLeft) minLeft else box.left.coerceIn(minLeft, maxLeft)
    val top = if (maxTop < minTop) minTop else box.top.coerceIn(minTop, maxTop)
    val dx = left - box.left
    val dy = top - box.top
    if (dx == 0f && dy == 0f) return box
    return box.copy(
        left = left,
        top = top,
        textLeft = box.textLeft + dx,
        textBaseline = box.textBaseline + dy,
    )
}

/** Normalised centre of the box that [watermarkLayout] would produce for this drag. */
fun clampedWatermarkAnchor(
    centerX: Float,
    centerY: Float,
    width: Float,
    height: Float,
    frameW: Float,
    frameH: Float,
): WatermarkAnchor {
    val clamped = clampWatermarkBox(
        WatermarkBox(
            id = "",
            left = centerX - width / 2f,
            top = centerY - height / 2f,
            width = width,
            height = height,
        ),
        frameW,
        frameH,
    )
    return WatermarkAnchor(
        cx = if (frameW > 0f) clamped.cx / frameW else 0f,
        cy = if (frameH > 0f) clamped.cy / frameH else 0f,
    )
}
