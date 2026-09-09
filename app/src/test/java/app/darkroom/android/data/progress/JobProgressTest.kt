package app.darkroom.android.data.progress

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JobProgressTest {
    @Test
    fun jpegSkipTranscodeRenormalizesWeights() {
        val phases = ingestPhases(includeTranscode = false)
        assertEquals(2, phases.size)
        assertEquals("receiving", phases[0].id)
        assertEquals("ingesting", phases[1].id)
        assertEquals(0.875f, phases[0].weight, 0.001f)
        assertEquals(0.125f, phases[1].weight, 0.001f)
    }

    @Test
    fun overallFractionNeverRegresses() {
        val phases = ingestPhases(includeTranscode = true)
        val first = overallFraction(phases, "receiving", 0.80f, 0f)
        val second = overallFraction(phases, "receiving", 0.20f, first)
        assertTrue(first > 0f)
        assertEquals(first, second, 0.0001f)
    }

    @Test
    fun overallFractionCapsUntilSucceed() {
        val phases = aiPhases()
        val almost = overallFraction(phases, "saving", 1f, 0f)
        assertTrue(almost <= 0.99f)
    }

    @Test
    fun phaseFractionCapsAt95UntilComplete() {
        val timed = phaseFraction(null, null, false, null, null, 60_000, 1_000, phaseComplete = false)
        assertEquals(0.95f, timed, 0.0001f)
        val bytes = phaseFraction(100, 100, true, null, null, 0, 1_000, phaseComplete = false)
        assertEquals(0.95f, bytes, 0.0001f)
        val chunks = phaseFraction(null, null, false, 10, 10, 0, 1_000, phaseComplete = false)
        assertEquals(0.95f, chunks, 0.0001f)
        assertEquals(1f, phaseFraction(100, 100, true, 10, 10, 5_000, 1_000, phaseComplete = true), 0.0001f)
    }

    @Test
    fun growEstimateKeepsReceiveBelowOne() {
        assertEquals(100L, growEstimateIfExceeded(100, 50))
        val grown = growEstimateIfExceeded(100, 120)
        assertTrue(grown > 120)
        assertTrue(120f / grown <= 0.85f + 0.001f)
    }

    @Test
    fun resolveIncomingSizePrefersAlloThenSessionThenEma() {
        assertEquals(100L to SizeSource.Allo, resolveIncomingSize(100, 50, 40))
        assertEquals(50L to SizeSource.SessionLast, resolveIncomingSize(null, 50, 40))
        assertEquals(40L to SizeSource.Ema, resolveIncomingSize(null, null, 40))
        assertEquals(null to SizeSource.Unknown, resolveIncomingSize(null, null, null))
        assertEquals(null to SizeSource.Unknown, resolveIncomingSize(0, 0, 0))
    }

    @Test
    fun progressUiHidesPercentOnTerminalFailure() {
        val failed = JobProgress(
            jobId = "t1",
            kind = JobKind.FtpTransfer,
            phase = "failed",
            phaseElapsedMs = 200,
            jobElapsedMs = 200,
            error = "无法解码图片",
            failed = true,
            fraction = 0.4f,
        ).toUi("Failed")
        assertNull(failed.percent)
        assertFalse(failed.indeterminate)
        assertEquals(0f, failed.fill, 0.0001f)
        assertEquals("无法解码图片", failed.detail)

        val cancelled = failedProgressUi("Cancelled")
        assertNull(cancelled.percent)
        assertFalse(cancelled.indeterminate)
        assertEquals(0f, cancelled.fill, 0.0001f)
    }

    @Test
    fun progressUiHidesPercentWhenIndeterminate() {
        val unknown = JobProgress(
            jobId = "t1",
            kind = JobKind.FtpTransfer,
            phase = "receiving",
            phaseElapsedMs = 200,
            jobElapsedMs = 200,
            bytesLoaded = 12_000,
            sizeSource = SizeSource.Unknown,
            fraction = 0.2f,
        ).toUi("Receiving", "12 KB")
        assertTrue(unknown.indeterminate)
        assertNull(unknown.percent)
        assertFalse(unknown.estimated)

        val probe = JobProgress(
            jobId = "p1",
            kind = JobKind.Probe,
            phase = "probing",
            phaseElapsedMs = 400,
            jobElapsedMs = 400,
            fraction = 0.4f,
        ).toUi("Testing")
        assertTrue(probe.indeterminate)
        assertNull(probe.percent)
    }

    @Test
    fun progressUiMarksEstimateAndShowsPercent() {
        val alloProgress = JobProgress(
            jobId = "t1",
            kind = JobKind.FtpTransfer,
            phase = "receiving",
            phaseElapsedMs = 200,
            jobElapsedMs = 200,
            bytesLoaded = 50,
            bytesTotal = 100,
            sizeSource = SizeSource.Allo,
            fraction = 0.50f,
        )
        val allo = alloProgress.toUi("Receiving")
        assertEquals(50, allo.percent)
        assertFalse(allo.estimated)
        assertFalse(allo.indeterminate)

        val ema = alloProgress.copy(sizeSource = SizeSource.Ema).toUi("Receiving")
        assertEquals(50, ema.percent)
        assertTrue(ema.estimated)
    }

    @Test
    fun succeedReaches100AndPrintFreezesOnNoPaper() {
        var now = 1_000L
        val stats = MemoryProgressStats()
        val tracker = JobProgressTracker(
            jobId = "print-1",
            kind = JobKind.Print,
            phases = printPhases(includeEditing = false),
            stats = stats,
            clock = { now },
        )
        tracker.startPhase("printing")
        now += 5_000L
        val moving = tracker.tick(now).fraction
        assertTrue(moving > 0f)
        tracker.setJobState("no_paper")
        now += 20_000L
        val frozen = tracker.tick(now).fraction
        assertEquals(moving, frozen, 0.0001f)
        tracker.succeed()
        val ui = tracker.toUi("Done")
        assertEquals(100, ui.percent)
        assertFalse(ui.indeterminate)
        assertEquals(1f, ui.fill, 0.0001f)
    }

    @Test
    fun aiReaches100OnlyAfterSaving() {
        var now = 0L
        val tracker = JobProgressTracker(
            jobId = "ai-1",
            kind = JobKind.Ai,
            phases = aiPhases(),
            stats = MemoryProgressStats(),
            clock = { now },
        )
        tracker.startPhase("preparing")
        tracker.startPhase("uploading", 100, SizeSource.Allo)
        tracker.setBytes(100, 100)
        tracker.startPhase("processing")
        now += 1_000L
        tracker.tick(now)
        tracker.startPhase("downloading", 50, SizeSource.Allo)
        tracker.setBytes(50, 50)
        tracker.startPhase("saving")
        val before = tracker.toUi("Saving")
        assertTrue((before.percent ?: 0) < 100)
        assertFalse(before.indeterminate)
        tracker.succeed()
        assertEquals(100, tracker.toUi("Done").percent)
    }

    @Test
    fun emaIgnoresOutliersAndUpdates() {
        val stats = MemoryProgressStats()
        stats.recordDuration(JobKind.Ai, "processing", 10)
        assertEquals(seedDurationMs(JobKind.Ai, "processing"), stats.expectedDurationMs(JobKind.Ai, "processing"))
        stats.recordDuration(JobKind.Ai, "processing", 1_000_000)
        assertEquals(seedDurationMs(JobKind.Ai, "processing"), stats.expectedDurationMs(JobKind.Ai, "processing"))
        stats.recordDuration(JobKind.Ai, "processing", 10_000)
        val next = stats.expectedDurationMs(JobKind.Ai, "processing")
        assertTrue(next < seedDurationMs(JobKind.Ai, "processing"))
        assertTrue(next > 10_000)
    }

    @Test
    fun timeBasedFractionIsAsymptotic() {
        val expected = 25_000L
        assertEquals(0f, timeBasedPhaseFraction(0, expected), 0.0001f)
        val atExpected = timeBasedPhaseFraction(expected, expected)
        assertEquals(0.95f * (1f - kotlin.math.exp(-1.0).toFloat()), atExpected, 0.0001f)
        val atFive = timeBasedPhaseFraction(5 * expected, expected)
        assertTrue(atFive < PHASE_WITHIN_CAP)
        assertTrue(atFive > atExpected)
        val processingWeight = aiPhases().first { it.id == "processing" }.weight
        val overall = overallFraction(aiPhases(), "processing", atFive, 0f)
        val prior = 0.08f + 0.17f
        assertTrue(overall - prior < PHASE_WITHIN_CAP * processingWeight)
    }

    @Test
    fun perPhaseCapKeepsProcessingBelowFullWeight() {
        val phases = aiPhases()
        val capped = overallFraction(phases, "processing", PHASE_WITHIN_CAP, 0f)
        assertEquals(0.08f + 0.17f + 0.50f * PHASE_WITHIN_CAP, capped, 0.0001f)
        assertTrue(capped < 0.75f)
        val complete = overallFraction(phases, "processing", 1f, 0f)
        assertEquals(0.75f, complete, 0.0001f)
    }

    @Test
    fun emaUpdatePersistsRoundTripViaFakeStore() {
        val store = mutableMapOf<String, Long>()
        val writer = MemoryProgressStats(store)
        assertFalse(writer.hasLearnedDuration(JobKind.Ai, "processing"))
        assertEquals(25_000L, writer.expectedDurationMs(JobKind.Ai, "processing"))
        writer.recordDuration(JobKind.Ai, "processing", 40_000)
        assertTrue(writer.hasLearnedDuration(JobKind.Ai, "processing"))
        val learned = writer.expectedDurationMs(JobKind.Ai, "processing")
        assertEquals(emaNext(25_000L, 40_000L), learned)
        val reader = MemoryProgressStats(store)
        assertTrue(reader.hasLearnedDuration(JobKind.Ai, "processing"))
        assertEquals(learned, reader.expectedDurationMs(JobKind.Ai, "processing"))
    }

    @Test
    fun weightsRenormalizeFromLearnedDurations() {
        val cold = aiPhases()
        assertEquals(0.50f, cold.first { it.id == "processing" }.weight, 0.0001f)
        val stats = MemoryProgressStats()
        stats.recordDuration(JobKind.Ai, "preparing", 800)
        stats.recordDuration(JobKind.Ai, "uploading", 3_000)
        stats.recordDuration(JobKind.Ai, "processing", 40_000)
        stats.recordDuration(JobKind.Ai, "downloading", 3_000)
        stats.recordDuration(JobKind.Ai, "saving", 1_000)
        val learned = durationWeightedPhases(JobKind.Ai, cold, stats)
        val processing = learned.first { it.id == "processing" }
        assertTrue(processing.weight > 0.70f)
        assertEquals(1f, learned.sumOf { it.weight.toDouble() }.toFloat(), 0.001f)
        val tracker = JobProgressTracker("ai-w", JobKind.Ai, cold, stats)
        tracker.startPhase("processing")
        val atStart = tracker.snapshot().fraction
        assertTrue(atStart < 0.30f)
    }

    @Test
    fun fractionIsMonotonicAcrossPhaseChange() {
        var now = 0L
        val tracker = JobProgressTracker(
            jobId = "ai-mono",
            kind = JobKind.Ai,
            phases = aiPhases(),
            stats = MemoryProgressStats(),
            clock = { now },
        )
        tracker.startPhase("preparing")
        now += 400L
        val preparing = tracker.tick(now).fraction
        tracker.startPhase("uploading", 100, SizeSource.Allo)
        val enteredUpload = tracker.tick(now).fraction
        assertTrue(enteredUpload + 0.0001f >= preparing)
        tracker.setBytes(40, 100)
        val midUpload = tracker.tick(now).fraction
        assertTrue(midUpload >= enteredUpload)
        tracker.startPhase("processing")
        val enteredProcessing = tracker.tick(now).fraction
        assertTrue(enteredProcessing + 0.0001f >= midUpload)
        now += 8_000L
        val later = tracker.tick(now).fraction
        assertTrue(later > enteredProcessing)
        assertTrue(later < 0.75f)
    }

    @Test
    fun processingDoesNotPinWhenUploadEventRepeats() {
        var now = 0L
        val stats = MemoryProgressStats()
        val tracker = JobProgressTracker(
            jobId = "ai-reg",
            kind = JobKind.Ai,
            phases = aiPhases(),
            stats = stats,
            clock = { now },
        )
        tracker.startPhase("preparing")
        tracker.startPhase("uploading", 100, SizeSource.Allo)
        now += 3_000L
        tracker.setBytes(100, 100)
        tracker.startPhase("processing")
        val entered = tracker.snapshot().fraction
        tracker.startPhase("uploading", 100, SizeSource.Allo)
        tracker.startPhase("processing")
        assertEquals("processing", tracker.snapshot().phase)
        assertEquals(entered, tracker.snapshot().fraction, 0.0001f)
        now += 1_000L
        val mid = tracker.tick(now).fraction
        now += 8_000L
        val later = tracker.tick(now).fraction
        assertTrue(later > mid)
        assertTrue(later < 0.75f)
        tracker.startPhase("downloading")
        assertTrue(stats.hasLearnedDuration(JobKind.Ai, "processing"))
    }

    @Test
    fun aiUploadEventAfterProcessingDoesNotSaturate() {
        var now = 0L
        val tracker = JobProgressTracker(
            jobId = "ai-edit",
            kind = JobKind.Ai,
            phases = aiPhases(),
            stats = MemoryProgressStats(),
            clock = { now },
        )
        applyAiProgress(tracker, ai("preparing"))
        applyAiProgress(tracker, ai("uploading", loaded = 100, total = 100))
        applyAiProgress(tracker, ai("processing"))
        applyAiProgress(tracker, ai("uploading", loaded = 100, total = 100))
        applyAiProgress(tracker, ai("processing"))
        assertEquals("processing", tracker.snapshot().phase)
        now += 2_000L
        val mid = tracker.tick(now).fraction
        now += 10_000L
        val later = tracker.tick(now).fraction
        assertTrue(later > mid)
        assertTrue(later < 0.08f + 0.17f + 0.50f * PHASE_WITHIN_CAP)
    }

    @Test
    fun seedDurationsMatchColdStartDefaults() {
        assertEquals(25_000L, seedDurationMs(JobKind.Ai, "processing"))
        assertEquals(3_000L, seedDurationMs(JobKind.Ai, "uploading"))
        assertEquals(3_000L, seedDurationMs(JobKind.Ai, "downloading"))
        assertEquals(1_000L, seedDurationMs(JobKind.Ai, "saving"))
        assertEquals(6_000L, seedDurationMs(JobKind.Print, "connecting"))
        assertEquals(40_000L, seedDurationMs(JobKind.Print, "printing"))
        assertEquals(2_000L, seedDurationMs(JobKind.FtpTransfer, "transcoding"))
        assertEquals(1_000L, seedDurationMs(JobKind.FtpTransfer, "ingesting"))
    }

    private fun ai(
        phase: String,
        loaded: Long? = null,
        total: Long? = null,
    ) = app.darkroom.android.data.ai.AiProgress(
        photoId = "p",
        phase = phase,
        loaded = loaded,
        total = total,
    )
}
