package io.github.wa_otomia.darkroom.data.progress

import io.github.wa_otomia.darkroom.data.ai.AiProgress

fun applyAiProgress(tracker: JobProgressTracker, progress: AiProgress) {
    when (progress.phase) {
        "error" -> tracker.fail(progress.error.orEmpty())
        "downloaded" -> {
            if (progress.loaded != null) {
                tracker.setBytes(progress.loaded, progress.total ?: progress.loaded, SizeSource.Allo)
            }
            if (tracker.snapshot().phase == "downloading") tracker.completePhase()
        }
        else -> {
            val current = tracker.snapshot().phase
            if (isEarlierAiPhase(progress.phase, current)) return
            val source = if (progress.total != null && progress.total > 0L) {
                SizeSource.Allo
            } else {
                SizeSource.Unknown
            }
            if (progress.phase != current) {
                tracker.startPhase(progress.phase, progress.total, source)
            }
            if (progress.loaded != null) {
                tracker.setBytes(progress.loaded, progress.total, source)
            }
        }
    }
}

private val AI_PHASE_ORDER = listOf("preparing", "uploading", "processing", "downloading", "saving")

internal fun isEarlierAiPhase(incoming: String, current: String): Boolean {
    val from = AI_PHASE_ORDER.indexOf(current)
    val to = AI_PHASE_ORDER.indexOf(incoming)
    return from >= 0 && to >= 0 && to < from
}
