package io.github.wa_otomia.darkroom.ui.studio

/*
 * Pure state rules behind Studio's ID photo UI, kept free of Android and Compose so the JVM
 * tests can cover them.
 */

/** Inputs of one preview cut-out render (see rememberIdPreviewCutout). */
internal data class CutoutKey(
    val file: String,
    val maskRev: String,
    val argb: Int,
    val feather: String,
    val cellWidth: Int,
    val cellHeight: Int,
    val zoomStep: Int,
)

/** Same source file and mask revision: a mask that failed once fails again for this key. */
internal fun CutoutKey.sameMask(other: CutoutKey): Boolean = file == other.file && maskRev == other.maskRev

internal enum class CutoutStart {
    /** The held cut-out already matches, or this mask revision is known to be missing/undecodable. */
    Skip,

    /** The zoom is moving: wait for it to settle before rendering. */
    Debounced,

    /** A colour, feather, size or mask change: render at once. */
    Now,
}

/**
 * How a cut-out render for [key] starts. [held] is the key of the cut-out on screen, [lastRequested]
 * the key of the previous render request (finished or not) and [failedMask] the key whose mask was
 * found missing or undecodable. A zoom step that differs from the last request, or a held cut-out
 * that differs only in zoom step, is debounced, also while no cut-out is held yet, so a pinch
 * never starts a render per step.
 */
internal fun cutoutStart(
    key: CutoutKey,
    held: CutoutKey?,
    lastRequested: CutoutKey?,
    failedMask: CutoutKey?,
): CutoutStart {
    if (held == key) return CutoutStart.Skip
    if (failedMask != null && failedMask.sameMask(key)) return CutoutStart.Skip
    val zoomMoving = (lastRequested != null && lastRequested.zoomStep != key.zoomStep) ||
        (held != null && held.copy(zoomStep = key.zoomStep) == key)
    return if (zoomMoving) CutoutStart.Debounced else CutoutStart.Now
}

/**
 * A tapped background or feather shown before the catalog write lands. [value] stays until every
 * write started from a tap has completed and the row shows the latest tap, so an earlier write
 * landing after a later tap never flips the preview (or an export) back to the older choice.
 * [seq] is the token of the latest tap; [inFlight] counts writes not yet completed.
 */
internal data class PendingIdChoice(
    val value: String? = null,
    val seq: Int = 0,
    val inFlight: Int = 0,
) {
    /** A new tap; the returned state's [seq] is its token. */
    fun begin(choice: String): PendingIdChoice = copy(value = choice, seq = seq + 1, inFlight = inFlight + 1)

    /** The write for [token] completed. A failure of the latest tap drops the optimistic value. */
    fun finish(token: Int, ok: Boolean): PendingIdChoice = copy(
        value = if (!ok && token == seq) null else value,
        inFlight = (inFlight - 1).coerceAtLeast(0),
    )

    /** Clears the value once no write is in flight and the row already holds it. */
    fun settle(stored: String?): PendingIdChoice =
        if (value != null && inFlight == 0 && value == stored) copy(value = null) else this
}

/**
 * The mask failure to show for the version Studio is on: only one recorded for that version
 * ([errorVersion], a framing version key), never another version's.
 */
internal fun maskErrorFor(error: String?, errorVersion: String?, currentVersion: String): String? =
    error?.takeIf { it.isNotBlank() && errorVersion == currentVersion }
