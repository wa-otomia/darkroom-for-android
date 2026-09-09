package app.darkroom.android.data.progress

import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.max

enum class JobKind { FtpTransfer, Print, Ai, Import, Share, Probe, BtScan, BtPair, FtpStart, StudioDecode, Export }

enum class SizeSource { Allo, SessionLast, Ema, Unknown }

data class WeightedPhase(
    val id: String,
    val weight: Float,
)

data class JobProgress(
    val jobId: String,
    val kind: JobKind,
    val phase: String,
    val phaseElapsedMs: Long,
    val jobElapsedMs: Long,
    val bytesLoaded: Long? = null,
    val bytesTotal: Long? = null,
    val sizeSource: SizeSource = SizeSource.Unknown,
    val chunk: Int? = null,
    val chunkTotal: Int? = null,
    val jobState: String? = null,
    val error: String? = null,
    val finished: Boolean = false,
    val failed: Boolean = false,
    val fraction: Float = 0f,
)

data class ProgressUi(
    val percent: Int?,
    val fill: Float,
    val phaseLabel: String,
    val detail: String? = null,
    val estimated: Boolean = false,
    val indeterminate: Boolean = false,
)

fun renormalize(phases: List<WeightedPhase>): List<WeightedPhase> {
    val sum = phases.sumOf { it.weight.toDouble() }.toFloat()
    if (sum <= 0f) return phases
    return phases.map { it.copy(weight = it.weight / sum) }
}

const val PHASE_WITHIN_CAP = 0.95f

fun ingestPhases(
    includeTranscode: Boolean,
    stats: ProgressStats? = null,
    kind: JobKind = JobKind.FtpTransfer,
): List<WeightedPhase> {
    val fallback = buildList {
        add(WeightedPhase("receiving", 0.70f))
        if (includeTranscode) add(WeightedPhase("transcoding", 0.20f))
        add(WeightedPhase("ingesting", 0.10f))
    }
    return durationWeightedPhases(kind, fallback, stats)
}

fun printPhases(includeEditing: Boolean, stats: ProgressStats? = null): List<WeightedPhase> {
    val fallback = buildList {
        if (includeEditing) add(WeightedPhase("editing", 0.45f))
        add(WeightedPhase("preparing", 0.08f))
        add(WeightedPhase("connecting", 0.10f))
        add(WeightedPhase("sending", 0.22f))
        add(WeightedPhase("printing", 0.15f))
    }
    return durationWeightedPhases(JobKind.Print, fallback, stats)
}

fun exportPhases(stats: ProgressStats? = null): List<WeightedPhase> = durationWeightedPhases(
    JobKind.Export,
    listOf(
        WeightedPhase("rendering", 0.70f),
        WeightedPhase("saving", 0.30f),
    ),
    stats,
)

fun aiPhases(stats: ProgressStats? = null): List<WeightedPhase> = durationWeightedPhases(
    JobKind.Ai,
    listOf(
        WeightedPhase("preparing", 0.08f),
        WeightedPhase("uploading", 0.17f),
        WeightedPhase("processing", 0.50f),
        WeightedPhase("downloading", 0.15f),
        WeightedPhase("saving", 0.10f),
    ),
    stats,
)

/** Cold-start constants unless [stats] has at least one learned EMA sample. */
fun durationWeightedPhases(
    kind: JobKind,
    fallback: List<WeightedPhase>,
    stats: ProgressStats?,
): List<WeightedPhase> {
    val normalized = renormalize(fallback)
    if (stats == null || fallback.none { stats.hasLearnedDuration(kind, it.id) }) {
        return normalized
    }
    return renormalize(
        fallback.map { phase ->
            WeightedPhase(
                id = phase.id,
                weight = stats.expectedDurationMs(kind, phase.id).toFloat().coerceAtLeast(1f),
            )
        },
    )
}

fun singlePhase(id: String): List<WeightedPhase> = listOf(WeightedPhase(id, 1f))

fun batchOverall(completedFiles: Int, fileCount: Int, currentFileOverall: Float): Float {
    if (fileCount <= 0) return 0f
    val raw = (completedFiles + currentFileOverall.coerceIn(0f, 1f)) / fileCount
    return raw.coerceIn(0f, 0.99f)
}

fun sizeKnown(source: SizeSource, bytesTotal: Long?): Boolean =
    bytesTotal != null && bytesTotal > 0L && source != SizeSource.Unknown

fun phaseFraction(
    bytesLoaded: Long?,
    bytesTotal: Long?,
    sizeKnown: Boolean,
    chunk: Int?,
    chunkTotal: Int?,
    elapsedMs: Long,
    expectedMs: Long,
    phaseComplete: Boolean,
    nestedWithin: Float? = null,
): Float {
    if (phaseComplete) return 1f
    if (nestedWithin != null) return nestedWithin.coerceIn(0f, PHASE_WITHIN_CAP)
    if (chunk != null && chunkTotal != null && chunkTotal > 0) {
        return (chunk.toFloat() / chunkTotal.toFloat()).coerceIn(0f, PHASE_WITHIN_CAP)
    }
    if (sizeKnown && bytesTotal != null && bytesTotal > 0L && bytesLoaded != null) {
        return (bytesLoaded.toFloat() / bytesTotal.toFloat()).coerceIn(0f, PHASE_WITHIN_CAP)
    }
    return timeBasedPhaseFraction(elapsedMs, expectedMs)
}

/** Asymptotic within-phase fraction: creeps toward [PHASE_WITHIN_CAP], never reaches it. */
fun timeBasedPhaseFraction(elapsedMs: Long, expectedMs: Long): Float {
    if (elapsedMs <= 0L) return 0f
    val expected = expectedMs.toDouble().coerceAtLeast(1.0)
    val raw = PHASE_WITHIN_CAP * (1.0 - exp(-elapsedMs.toDouble() / expected))
    return raw.toFloat().coerceIn(0f, PHASE_WITHIN_CAP)
}

fun overallFraction(
    phases: List<WeightedPhase>,
    currentPhase: String,
    within: Float,
    priorHighWater: Float,
): Float {
    if (phases.isEmpty()) return priorHighWater.coerceIn(0f, 0.99f)
    var done = 0f
    var seen = false
    for (phase in phases) {
        if (phase.id == currentPhase) {
            val capped = if (within >= 1f) 1f else within.coerceIn(0f, PHASE_WITHIN_CAP)
            done += phase.weight * capped
            seen = true
            break
        }
        done += phase.weight
    }
    if (!seen) done = within.coerceIn(0f, 1f)
    return maxOf(priorHighWater, done.coerceAtMost(0.99f))
}

fun resolveIncomingSize(
    allo: Long?,
    sessionLastBytes: Long?,
    emaBytes: Long?,
): Pair<Long?, SizeSource> = when {
    allo != null && allo > 0L -> allo to SizeSource.Allo
    sessionLastBytes != null && sessionLastBytes > 0L -> sessionLastBytes to SizeSource.SessionLast
    emaBytes != null && emaBytes > 0L -> emaBytes to SizeSource.Ema
    else -> null to SizeSource.Unknown
}

fun growEstimateIfExceeded(estimate: Long, received: Long): Long {
    if (received <= estimate) return estimate
    val boosted = (estimate * 1.15).toLong()
    val room = ceil(received / 0.85).toLong()
    return max(boosted, room)
}

fun canonicalPhase(phase: String): String =
    if (phase == "uploading") "sending" else phase

fun isPaperHalt(state: String?): Boolean {
    val key = state?.lowercase()?.substringAfterLast('.').orEmpty()
    return key in PAPER_HALT_STATES
}

fun printStateFraction(state: String?): Float? {
    val key = state?.lowercase()?.substringAfterLast('.').orEmpty()
    return PRINT_STATE_FRACTION[key]
}

fun JobProgress.toUi(phaseLabel: String, detail: String? = null): ProgressUi {
    if (failed) {
        return failedProgressUi(phaseLabel, detail ?: error)
    }
    if (finished) {
        return ProgressUi(
            percent = 100,
            fill = 1f,
            phaseLabel = phaseLabel,
            detail = detail,
            estimated = false,
            indeterminate = false,
        )
    }
    val waitKind = kind in WAIT_KINDS
    val ftpUnknown = kind == JobKind.FtpTransfer &&
        phase == "receiving" &&
        sizeSource == SizeSource.Unknown
    val indeterminate = waitKind || ftpUnknown
    val estimated = !indeterminate &&
        (sizeSource == SizeSource.SessionLast || sizeSource == SizeSource.Ema)
    val percent = if (indeterminate) null else (fraction * 100f).toInt().coerceIn(0, 99)
    return ProgressUi(
        percent = percent,
        fill = if (indeterminate) 0f else fraction.coerceIn(0f, 0.99f),
        phaseLabel = phaseLabel,
        detail = detail,
        estimated = estimated && percent != null,
        indeterminate = indeterminate,
    )
}

fun indeterminateUi(phaseLabel: String, detail: String? = null): ProgressUi = ProgressUi(
    percent = null,
    fill = 0f,
    phaseLabel = phaseLabel,
    detail = detail,
    estimated = false,
    indeterminate = true,
)

/** Terminal Failed / cancelled: status text only, no percent, static hairline. */
fun failedProgressUi(phaseLabel: String, detail: String? = null): ProgressUi = ProgressUi(
    percent = null,
    fill = 0f,
    phaseLabel = phaseLabel,
    detail = detail,
    estimated = false,
    indeterminate = false,
)

private val WAIT_KINDS = setOf(
    JobKind.Probe,
    JobKind.BtScan,
    JobKind.BtPair,
    JobKind.FtpStart,
    JobKind.StudioDecode,
)

private val PRINT_STATE_FRACTION = mapOf(
    "downloading" to 0.20f,
    "printing" to 0.55f,
    "queued" to 0.72f,
    "waiting" to 0.82f,
)

private val PAPER_HALT_STATES = setOf(
    "no_paper",
    "out_of_paper",
    "paper_empty",
    "paper_out",
    "lack_paper",
    "load_paper",
    "pending",
)
