package app.darkroom.android.core

import kotlin.math.abs
import kotlin.math.sign

/** Position snap radius in dp; convert with density before constructing [SnapAxis]. */
const val SNAP_POSITION_DP = 8f

/** Rotation snap radius in degrees. */
const val SNAP_ROTATION_DEG = 3f

/** Safe-line inset as a fraction of the frame's short edge. */
const val SAFE_INSET_FRACTION = 0.05f

const val GUIDE_CENTER_X = "center-x"
const val GUIDE_CENTER_Y = "center-y"
const val GUIDE_FRAME_X_POS = "frame-x-pos"
const val GUIDE_FRAME_X_NEG = "frame-x-neg"
const val GUIDE_FRAME_Y_POS = "frame-y-pos"
const val GUIDE_FRAME_Y_NEG = "frame-y-neg"
const val GUIDE_SAFE_X_POS = "safe-x-pos"
const val GUIDE_SAFE_X_NEG = "safe-x-neg"
const val GUIDE_SAFE_Y_POS = "safe-y-pos"
const val GUIDE_SAFE_Y_NEG = "safe-y-neg"
const val GUIDE_ROT_0 = "rot-0"
const val GUIDE_ROT_90 = "rot-90"
const val GUIDE_ROT_180 = "rot-180"
const val GUIDE_ROT_270 = "rot-270"

/** How a [SnapGuide.id] should be drawn (full center line vs short edge tick). */
enum class SnapGuideKind { CENTER, SAFE, FRAME, ROTATION }

fun snapGuideKind(id: String): SnapGuideKind = when {
    id.startsWith("center") -> SnapGuideKind.CENTER
    id.startsWith("safe") -> SnapGuideKind.SAFE
    id.startsWith("frame") -> SnapGuideKind.FRAME
    else -> SnapGuideKind.ROTATION
}

/** Safe-line inset in the same units as [frameW]/[frameH]. */
fun safeInset(frameW: Float, frameH: Float): Float =
    SAFE_INSET_FRACTION * minOf(frameW, frameH).coerceAtLeast(0f)

/**
 * One snap target on a single axis. [value] is the axis coordinate (px offset or degrees);
 * [id] selects the overlay stroke the UI should draw.
 */
data class SnapGuide(val id: String, val value: Float)

/**
 * Dead-zone snapper for one axis.
 *
 * [raw] is the finger-driven virtual position; [value] is the object position written to UI
 * state. While unsnapped they stay equal. On snap-in [value] pins to the guide and [raw] keeps
 * accumulating the finger. Leaving the dead zone exits continuously from the guide edge
 * (`guide ± threshold`) so the object does not jump to the finger.
 *
 * @param threshold snap radius (px or degrees)
 * @param wrap period for a cyclic axis; pass `360` for rotation so distances use the shortest arc
 */
class SnapAxis(
    private val threshold: Float,
    private val wrap: Float? = null,
) {
    /** Object position (what the UI displays / writes back). */
    var value: Float = 0f
        private set

    /** Finger-driven virtual position. Equal to [value] except while pinned to a guide. */
    var raw: Float = 0f
        private set

    var snapped: SnapGuide? = null
        private set

    /** Reset both positions to [current] and clear any pin. Call on pointer-down and after an external reset. */
    fun begin(current: Float) {
        sync(current)
    }

    /**
     * Align [value] and [raw] to [current] and clear any pin. Does not count as a snap-in.
     * Call when the written pose came from clamp or an external change, not from [drag].
     */
    fun sync(current: Float) {
        value = if (wrap != null) norm(current) else current
        raw = value
        snapped = null
    }

    /**
     * Signed distance from [from] to [to]. On a wrap axis this is the shortest arc in
     * `(-wrap/2, wrap/2]`.
     */
    fun dist(from: Float, to: Float): Float {
        if (wrap == null) return to - from
        val period = wrap
        var d = (to - from) % period
        if (d > period / 2f) d -= period
        if (d <= -period / 2f) d += period
        return d
    }

    /** Wrap-axis values are normalized to `[0, wrap)`. */
    fun norm(v: Float): Float {
        val period = wrap ?: return v
        var x = v % period
        if (x < 0f) x += period
        return x
    }

    /**
     * Advance the finger by [delta] and maybe inhale onto [guides].
     *
     * @return `true` on the snap-in frame only (for a one-shot haptic). Stay-pinned and exit
     * return `false`. [delta] `== 0` never snaps in (e.g. a zoom-only frame).
     */
    fun drag(delta: Float, guides: List<SnapGuide>): Boolean {
        raw = norm(raw + delta)
        val g = snapped
        if (g != null) {
            val d = dist(g.value, raw)
            if (abs(d) <= threshold) {
                value = g.value
                return false
            }
            // Exit from the guide edge, then re-sync raw so lag does not accumulate.
            value = norm(g.value + d - threshold * sign(d))
            raw = value
            snapped = null
            return false
        }
        value = raw
        if (delta == 0f) return false
        val hit = guides
            .filter { c ->
                val d = dist(value, c.value)
                // Landing exactly on the guide (d == 0) still counts as moving toward it,
                // so a wrap jump such as 357° + 3° pins and later exits to 1°.
                abs(d) <= threshold && (d == 0f || sign(d) == sign(delta))
            }
            .minByOrNull { c -> abs(dist(value, c.value)) }
            ?: return false
        snapped = hit
        value = hit.value
        return true
    }
}
