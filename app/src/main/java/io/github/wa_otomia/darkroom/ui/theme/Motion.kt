package io.github.wa_otomia.darkroom.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween

/**
 * Shared motion tokens for the hand-rolled controls in
 * [io.github.wa_otomia.darkroom.ui.components].
 *
 * Durations and easing mirror the Material 3 defaults that the stock controls
 * already animate with (Switch, Slider, FilterChip, SegmentedButton), so a
 * screen that mixes both kinds of control no longer looks like two different
 * design languages.
 */
object DarkroomMotion {
    /** Material 3 `short4`: colour and alpha changes on chips and buttons. */
    const val StateChangeMillis = 200

    /** Material 3 `medium2`: a value moving, such as a progress bar advancing. */
    const val ValueChangeMillis = 300

    /** One leg of the indeterminate progress sweep, played in both directions. */
    const val SweepMillis = 1000

    /** How long an inline confirmation stays on screen before it fades out. */
    const val ConfirmVisibleMillis = 1600L

    /** Material 3 `emphasized`/standard easing. */
    val Standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** Material 3 `standard decelerate`, for content that enters. */
    val Decelerate: Easing = CubicBezierEasing(0f, 0f, 0f, 1f)

    fun <T> stateChange(): FiniteAnimationSpec<T> = tween(StateChangeMillis, easing = Standard)

    fun <T> valueChange(): FiniteAnimationSpec<T> = tween(ValueChangeMillis, easing = Standard)

    fun <T> enter(): FiniteAnimationSpec<T> = tween(StateChangeMillis, easing = Decelerate)
}
