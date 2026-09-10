package io.github.wa_otomia.darkroom.data.progress

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class JobProgressTracker(
    val jobId: String,
    val kind: JobKind,
    phases: List<WeightedPhase>,
    private val stats: ProgressStats,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    private var phases: MutableList<WeightedPhase> = adoptPhases(phases)
    private var currentPhase: String = this.phases.firstOrNull()?.id.orEmpty()
    private var phaseStartedAt: Long = clock()
    private val jobStartedAt: Long = phaseStartedAt
    private var bytesLoaded: Long? = null
    private var bytesTotal: Long? = null
    private var sizeSource: SizeSource = SizeSource.Unknown
    private var chunk: Int? = null
    private var chunkTotal: Int? = null
    private var jobState: String? = null
    private var error: String? = null
    private var finished: Boolean = false
    private var failed: Boolean = false
    private var phaseComplete: Boolean = false
    private var highWater: Float = 0f
    private var lastWithin: Float = 0f
    private var nestedWithin: Float? = null
    private var frozen: Boolean = false
    private val completed = linkedSetOf<String>()
    private var started: Boolean = false

    @Synchronized
    fun startPhase(
        id: String,
        bytesTotal: Long? = null,
        sizeSource: SizeSource = SizeSource.Unknown,
    ) {
        if (finished || failed) return
        val canonical = canonicalPhaseForKind(id)
        if (started && currentPhase == canonical && !phaseComplete) {
            if (bytesTotal != null) this.bytesTotal = bytesTotal
            if (sizeSource != SizeSource.Unknown) this.sizeSource = sizeSource
            return
        }
        if (started && isBackwardPhase(canonical)) return
        if (started && !phaseComplete && currentPhase.isNotEmpty()) {
            completePhaseLocked()
        }
        dropPhasesBetween(currentPhase, canonical)
        currentPhase = canonical
        started = true
        phaseComplete = false
        phaseStartedAt = clock()
        bytesLoaded = null
        this.bytesTotal = bytesTotal
        this.sizeSource = when {
            canonical == "receiving" -> sizeSource
            sizeSource != SizeSource.Unknown -> sizeSource
            else -> SizeSource.Unknown
        }
        chunk = null
        chunkTotal = null
        nestedWithin = null
        if (canonical != "printing") {
            jobState = null
            frozen = false
        }
        refreshLocked()
    }

    @Synchronized
    fun setBytes(loaded: Long, total: Long? = null, source: SizeSource? = null) {
        if (finished || failed) return
        bytesLoaded = loaded.coerceAtLeast(0L)
        source?.let { sizeSource = it }
        val base = total ?: bytesTotal
        bytesTotal = when {
            base != null && loaded > base && sizeSource != SizeSource.Allo && sizeSource != SizeSource.Unknown ->
                growEstimateIfExceeded(base, loaded)
            total != null -> total
            else -> bytesTotal
        }
        refreshLocked()
    }

    @Synchronized
    fun setChunks(chunk: Int, total: Int) {
        if (finished || failed) return
        this.chunk = chunk.coerceAtLeast(0)
        this.chunkTotal = total.coerceAtLeast(0)
        refreshLocked()
    }

    @Synchronized
    fun setJobState(state: String) {
        if (finished || failed) return
        jobState = state
        frozen = isPaperHalt(state)
        refreshLocked()
    }

    @Synchronized
    fun setNestedFraction(within: Float) {
        if (finished || failed) return
        nestedWithin = within.coerceIn(0f, PHASE_WITHIN_CAP)
        refreshLocked()
    }

    @Synchronized
    fun skipPhase(id: String) {
        if (finished || failed) return
        val canonical = canonicalPhaseForKind(id)
        if (canonical in completed) return
        if (currentPhase == canonical) {
            completePhaseLocked()
            return
        }
        if (phases.none { it.id == canonical }) return
        phases = adoptPhases(phases.filter { it.id != canonical })
        refreshLocked()
    }

    @Synchronized
    fun tick(now: Long = clock()): JobProgress {
        if (!finished && !failed && !frozen) {
            refreshLocked(now)
        }
        return snapshotLocked()
    }

    @Synchronized
    fun completePhase() {
        if (finished || failed) return
        completePhaseLocked()
    }

    @Synchronized
    fun fail(message: String) {
        if (finished) return
        error = message
        failed = true
        finished = false
        highWater = 0f
    }

    @Synchronized
    fun succeed() {
        if (failed) return
        if (!phaseComplete && started) completePhaseLocked()
        finished = true
        failed = false
        highWater = 1f
        lastWithin = 1f
        phaseComplete = true
    }

    @Synchronized
    fun snapshot(): JobProgress = snapshotLocked()

    @Synchronized
    fun toUi(phaseLabel: String, detail: String? = null): ProgressUi =
        snapshotLocked().toUi(phaseLabel, detail)

    @Synchronized
    fun isActive(): Boolean = started && !finished && !failed

    @Synchronized
    fun isTerminal(): Boolean = finished || failed

    fun launchTicks(
        scope: CoroutineScope,
        intervalMs: Long = TICK_INTERVAL_MS,
        onTick: (JobProgress) -> Unit = {},
    ): Job = scope.launch {
        while (!isTerminal()) {
            delay(intervalMs)
            if (!isActive()) continue
            onTick(tick())
        }
    }

    private fun completePhaseLocked() {
        if (!started) return
        if (phaseComplete && currentPhase in completed) {
            refreshLocked()
            return
        }
        val elapsed = (clock() - phaseStartedAt).coerceAtLeast(0L)
        stats.recordDuration(kind, currentPhase, elapsed)
        phaseComplete = true
        completed += currentPhase
        nestedWithin = null
        lastWithin = 1f
        refreshLocked()
        phases = adoptPhases(phases)
    }

    private fun dropPhasesBetween(fromId: String, toId: String) {
        if (fromId.isEmpty() || toId.isEmpty()) return
        val from = phases.indexOfFirst { it.id == fromId }
        val to = phases.indexOfFirst { it.id == toId }
        if (from < 0 || to < 0 || to <= from + 1) return
        val drop = phases.subList(from + 1, to).map { it.id }.toSet()
        phases = adoptPhases(phases.filter { it.id !in drop })
    }

    private fun isBackwardPhase(toId: String): Boolean {
        val from = phases.indexOfFirst { it.id == currentPhase }
        val to = phases.indexOfFirst { it.id == toId }
        return from >= 0 && to >= 0 && to < from
    }

    private fun adoptPhases(list: List<WeightedPhase>): MutableList<WeightedPhase> =
        durationWeightedPhases(kind, list, stats).toMutableList()

    private fun refreshLocked(now: Long = clock()) {
        if (finished) {
            highWater = 1f
            return
        }
        if (failed) {
            highWater = 0f
            return
        }
        val elapsed = (now - phaseStartedAt).coerceAtLeast(0L)
        val expected = stats.expectedDurationMs(kind, currentPhase)
        val raw = if (frozen) {
            lastWithin
        } else {
            phaseFraction(
                bytesLoaded = bytesLoaded,
                bytesTotal = bytesTotal,
                sizeKnown = sizeKnown(sizeSource, bytesTotal),
                chunk = chunk,
                chunkTotal = chunkTotal,
                elapsedMs = elapsed,
                expectedMs = expected,
                phaseComplete = phaseComplete,
                nestedWithin = nestedWithin,
            )
        }
        val stateHint = if (currentPhase == "printing" && !phaseComplete) {
            printStateFraction(jobState)
        } else {
            null
        }
        val within = if (phaseComplete) {
            1f
        } else {
            maxOf(raw, stateHint ?: 0f).coerceIn(0f, PHASE_WITHIN_CAP)
        }
        lastWithin = within
        highWater = if (finished) {
            1f
        } else {
            overallFraction(phases, currentPhase, within, highWater)
        }
    }

    private fun snapshotLocked(): JobProgress {
        val now = clock()
        return JobProgress(
            jobId = jobId,
            kind = kind,
            phase = currentPhase,
            phaseElapsedMs = (now - phaseStartedAt).coerceAtLeast(0L),
            jobElapsedMs = (now - jobStartedAt).coerceAtLeast(0L),
            bytesLoaded = bytesLoaded,
            bytesTotal = bytesTotal,
            sizeSource = sizeSource,
            chunk = chunk,
            chunkTotal = chunkTotal,
            jobState = jobState,
            error = error,
            finished = finished,
            failed = failed,
            fraction = if (finished) 1f else if (failed) 0f else highWater,
        )
    }

    private fun canonicalPhaseForKind(phase: String): String =
        if (kind == JobKind.Print) canonicalPhase(phase) else phase

    companion object {
        const val TICK_INTERVAL_MS = 250L
    }
}
