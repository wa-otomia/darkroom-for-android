package io.github.wa_otomia.darkroom.core

/**
 * Remaining undo-window fraction in `0f..1f`.
 *
 * `1` is a full window (nothing elapsed); `0` means the window has closed.
 * [elapsedMs] and [totalMs] are milliseconds.
 */
fun undoRemainingFraction(elapsedMs: Long, totalMs: Long): Float {
    if (totalMs <= 0L) return 0f
    if (elapsedMs <= 0L) return 1f
    return (1f - elapsedMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
}
