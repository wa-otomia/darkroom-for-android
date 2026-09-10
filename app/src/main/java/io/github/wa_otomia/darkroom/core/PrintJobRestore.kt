package io.github.wa_otomia.darkroom.core

enum class PrintRestoreAction { Requeue, FailInterrupted }

/**
 * How to treat a `running` print_jobs row after a process death.
 *
 * Phases at or after the printer starts receiving bytes must not be replayed
 * (the sheet may already be out). Earlier phases are safe to queue again.
 */
fun restoreRunningPrintJob(phase: String?): PrintRestoreAction {
    val key = phase?.trim()?.lowercase().orEmpty()
    if (key.isEmpty()) return PrintRestoreAction.Requeue
    return if (key in INTERRUPTED_PRINT_PHASES) {
        PrintRestoreAction.FailInterrupted
    } else {
        PrintRestoreAction.Requeue
    }
}

private val INTERRUPTED_PRINT_PHASES = setOf(
    "sending",
    "uploading",
    "printing",
    "done",
    "error",
)

data class PrintWorkerOutcome(val state: String, val error: String?)

/** Per-job cancel marks. A later job must not clear an earlier job's flag. */
class PrintJobCancelFlags {
    private val ids = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    fun request(jobId: String) {
        if (jobId.isNotBlank()) ids.add(jobId)
    }

    fun isRequested(jobId: String?): Boolean = jobId != null && ids.contains(jobId)

    fun clear(jobId: String) {
        ids.remove(jobId)
    }
}

/** Empty / 「已取消」 / `cancelled` — Studio must not treat these as a failure dialog. */
fun isCancelledPrintMessage(message: String?): Boolean {
    if (message.isNullOrBlank()) return true
    val t = message.trim()
    return t == "已取消" || t.equals("cancelled", ignoreCase = true)
}

/**
 * Maps a finished print-job attempt to the persisted row.
 *
 * Cancellation (coroutine cancel, [cancelRequested], or the `已取消` guard)
 * becomes `cancelled` with no error text. Every other exception is `failed`.
 */
fun mapPrintWorkerFailure(
    cancellation: Boolean,
    cancelRequested: Boolean,
    message: String?,
): PrintWorkerOutcome {
    val cancelled = cancellation ||
        cancelRequested ||
        (!message.isNullOrBlank() && isCancelledPrintMessage(message))
    return if (cancelled) {
        PrintWorkerOutcome(state = "cancelled", error = null)
    } else {
        PrintWorkerOutcome(state = "failed", error = message)
    }
}
