package io.github.wa_otomia.darkroom.ui.studio

import io.github.wa_otomia.darkroom.core.ID_MASK_MSG_LOCAL_FAILED

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
    val featherInward: Boolean,
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

/** How the preview moves from the cut-out on screen to the one the current inputs want (D3). */
internal enum class CutoutTransition {
    /** The cut-out on screen already matches. */
    NONE,

    /** Background, feather, inward feather or mask revision changed: freeze until the new one lands. */
    FREEZE,

    /**
     * Only the feather scale changed (zoom step or cell size): keep the old cut-out, never freeze.
     * The cell size only rescales the feather radius, like the zoom step.
     */
    ZOOM_ONLY,

    /** No composite is wanted (original background or no mask): show the source at once. */
    ORIGINAL,
}

/**
 * [prev] is the key of the cut-out on screen (null when none), [next] the key the current inputs
 * want (null when the background is the original or there is no mask).
 */
internal fun cutoutTransition(prev: CutoutKey?, next: CutoutKey?): CutoutTransition = when {
    next == null -> CutoutTransition.ORIGINAL
    prev == next -> CutoutTransition.NONE
    prev != null && prev.copy(cellWidth = next.cellWidth, cellHeight = next.cellHeight, zoomStep = next.zoomStep) == next ->
        CutoutTransition.ZOOM_ONLY
    else -> CutoutTransition.FREEZE
}

/**
 * Whether the bounded retries given up for [gaveUp] still settle [key]: the same key, or one that
 * differs only in zoom step or cell size, so a pinch or size change after a give-up never brings
 * the freeze back (EDID040-R08).
 */
internal fun gaveUpCovers(gaveUp: CutoutKey?, key: CutoutKey?): Boolean {
    if (gaveUp == null || key == null) return false
    val transition = cutoutTransition(gaveUp, key)
    return transition == CutoutTransition.NONE || transition == CutoutTransition.ZOOM_ONLY
}

/** What the preview shows: the held cut-out or the source, and whether the controls are frozen. */
internal data class CutoutView(val showHeld: Boolean, val frozen: Boolean)

/**
 * The preview state for [key] while [held] is the finished cut-out kept on screen (D3,
 * EDID040-R08). A held cut-out of another source file is never shown. A freeze ends when the new
 * cut-out lands, when this mask revision is known missing or undecodable ([failedMask]; the
 * source is shown with the missing-mask note) or when the bounded retries for [key] are used up
 * ([gaveUp], also after a later zoom-only change; the source is shown rather than an old colour).
 * Without a known source size ([sizeKnown] false) no render can start, so nothing is frozen and
 * the source is shown until the size arrives.
 */
internal fun cutoutView(
    held: CutoutKey?,
    key: CutoutKey?,
    failedMask: CutoutKey?,
    gaveUp: CutoutKey?,
    sizeKnown: Boolean = true,
): CutoutView {
    val shown = held?.takeIf { key != null && it.file == key.file }
    return when (cutoutTransition(shown, key)) {
        CutoutTransition.ORIGINAL -> CutoutView(showHeld = false, frozen = false)
        CutoutTransition.NONE, CutoutTransition.ZOOM_ONLY -> CutoutView(showHeld = true, frozen = false)
        CutoutTransition.FREEZE -> {
            val settled = !sizeKnown ||
                key != null && ((failedMask != null && failedMask.sameMask(key)) || gaveUpCovers(gaveUp, key))
            CutoutView(showHeld = shown != null && !settled, frozen = !settled)
        }
    }
}

/** Backoff before re-rendering a cut-out after a transient (non-mask) failure. */
private val CUTOUT_RETRY_DELAYS_MS = longArrayOf(500L, 1_500L, 4_000L)

/**
 * Wait before retry number [attempt] (0-based) of a preview cut-out whose render failed for a
 * reason other than the mask (source decode, I/O, memory), or null once the bounded retries are
 * used up.
 */
internal fun cutoutRetryDelayMs(attempt: Int): Long? = CUTOUT_RETRY_DELAYS_MS.getOrNull(attempt)

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

/** Which label the cut-out button shows (EDID040-R02). */
internal enum class IdCutOutLabel {
    /** No mask yet, on-device cut-out off: "AI cut out person". */
    AI,

    /** No mask yet, on-device cut-out on: "Cut out person on device". */
    LOCAL,

    /** A mask exists: "Cut out again", whichever mode. */
    RECUT,
}

internal fun idCutOutLabel(hasMask: Boolean, local: Boolean): IdCutOutLabel = when {
    hasMask -> IdCutOutLabel.RECUT
    local -> IdCutOutLabel.LOCAL
    else -> IdCutOutLabel.AI
}

/**
 * Whether the cut-out can start (EDID040-R02/R10): an on-device cut-out needs no provider key; an
 * AI one needs [keySet]. Any job on this photo ([busy], hidden ones included) blocks both.
 */
internal fun idCanCutOut(local: Boolean, keySet: Boolean, busy: Boolean): Boolean = (local || keySet) && !busy

/** The "set an API key" hint and Settings entry show only for an AI cut-out without a key. */
internal fun idShowNeedKey(local: Boolean, keySet: Boolean): Boolean = !local && !keySet

/**
 * The on-device switch is disabled while this photo has any job or the preview composite is
 * building (EDID040-R10); the mode of a running job is frozen when it is queued anyway.
 */
internal fun idLocalSwitchEnabled(busy: Boolean, compositing: Boolean): Boolean = !busy && !compositing

/**
 * True when a failure record without an image came from the on-device cut-out, so the viewer
 * does not blame the AI for the missing image (EDID040-R11 keeps no image for such failures).
 * The record's [local] mode decides, whatever the message (an undecodable or deleted version
 * fails a local job without the local prefix); a record written before the mode was kept falls
 * back to the local failure prefix.
 */
internal fun idFailureIsLocal(local: Boolean, message: String): Boolean =
    local || message.trimStart().startsWith(ID_MASK_MSG_LOCAL_FAILED)
