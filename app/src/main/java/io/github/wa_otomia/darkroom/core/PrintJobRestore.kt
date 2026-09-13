package io.github.wa_otomia.darkroom.core

enum class PrintRestoreAction { Requeue, FailInterrupted }

/**
 * How to treat a `running` print_jobs row after a process death.
 *
 * Phases at or after the printer starts receiving bytes must not be replayed
 * (the sheet may already be out). Earlier phases are safe to queue again.
 */
fun restoreRunningPrintJob(phase: String?, remoteJobCreated: Boolean = false): PrintRestoreAction {
    if (remoteJobCreated) return PrintRestoreAction.FailInterrupted
    val key = phase?.trim()?.lowercase().orEmpty()
    if (key.isEmpty()) return PrintRestoreAction.Requeue
    return if (key in INTERRUPTED_PRINT_PHASES) {
        PrintRestoreAction.FailInterrupted
    } else {
        PrintRestoreAction.Requeue
    }
}

private val INTERRUPTED_PRINT_PHASES = setOf(
    "waiting_for_user",
    "resuming",
    "canceling",
    "outcome_unknown",
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
 * Before remote creation, local cancellation can settle the row. After remote
 * creation, only [cancelConfirmed] settles a cancellation. Intent, disconnects,
 * timeout and coroutine cancellation do not establish the device outcome.
 */
fun mapPrintWorkerFailure(
    cancellation: Boolean,
    cancelRequested: Boolean,
    message: String?,
    remoteJobCreated: Boolean = false,
    cancelConfirmed: Boolean = false,
): PrintWorkerOutcome {
    if (remoteJobCreated && !cancelConfirmed) return PrintWorkerOutcome(
        state = "failed", error = message ?: "Remote printer outcome unknown; no automatic reprint",
    )
    val cancelled = cancelConfirmed || cancellation ||
        cancelRequested ||
        (!message.isNullOrBlank() && isCancelledPrintMessage(message))
    return if (cancelled) {
        PrintWorkerOutcome(state = "cancelled", error = null)
    } else {
        PrintWorkerOutcome(state = "failed", error = message)
    }
}
