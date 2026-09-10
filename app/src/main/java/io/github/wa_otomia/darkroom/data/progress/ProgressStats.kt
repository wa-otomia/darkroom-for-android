package io.github.wa_otomia.darkroom.data.progress

import android.content.Context

interface ProgressStats {
    fun expectedDurationMs(kind: JobKind, phase: String): Long
    fun recordDuration(kind: JobKind, phase: String, durationMs: Long)
    fun expectedBytes(kind: JobKind): Long?
    fun recordBytes(kind: JobKind, bytes: Long)
    fun hasLearnedDuration(kind: JobKind, phase: String): Boolean
}

fun seedDurationMs(kind: JobKind, phase: String): Long {
    val raw = SEEDS[kind to phase]
    if (raw != null) return raw
    val canonical = if (kind == JobKind.Print) canonicalPhase(phase) else phase
    return SEEDS[kind to canonical] ?: DEFAULT_SEED_MS
}

class PrefsProgressStats(
    context: Context,
) : ProgressStats {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun expectedDurationMs(kind: JobKind, phase: String): Long {
        val seed = seedDurationMs(kind, phase)
        val stored = prefs.getLong(durationKey(kind, phase), -1L)
        return if (stored > 0L) stored else seed
    }

    override fun recordDuration(kind: JobKind, phase: String, durationMs: Long) {
        val seed = seedDurationMs(kind, phase)
        if (!acceptDurationSample(seed, durationMs)) return
        val key = durationKey(kind, phase)
        val previous = prefs.getLong(key, seed).takeIf { it > 0L } ?: seed
        prefs.edit().putLong(key, emaNext(previous, durationMs)).apply()
    }

    override fun expectedBytes(kind: JobKind): Long? {
        val key = bytesKey(kind) ?: return null
        val stored = prefs.getLong(key, -1L)
        return stored.takeIf { it > 0L }
    }

    override fun recordBytes(kind: JobKind, bytes: Long) {
        if (bytes <= 0L) return
        val key = bytesKey(kind) ?: return
        val previous = prefs.getLong(key, 0L)
        val next = if (previous <= 0L) bytes else emaNext(previous, bytes)
        prefs.edit().putLong(key, next).apply()
    }

    override fun hasLearnedDuration(kind: JobKind, phase: String): Boolean =
        prefs.contains(durationKey(kind, phase))

    companion object {
        const val PREFS_NAME = "darkroom.progress"
        const val ALPHA = 0.30
        const val MIN_SAMPLE_MS = 80L
        const val MAX_SEED_MULTIPLIER = 10L
        const val DEFAULT_SEED_MS = 2_000L
    }
}

class MemoryProgressStats(
    private val durations: MutableMap<String, Long> = mutableMapOf(),
    private val bytes: MutableMap<String, Long> = mutableMapOf(),
) : ProgressStats {
    override fun expectedDurationMs(kind: JobKind, phase: String): Long {
        return durations[durationKey(kind, phase)] ?: seedDurationMs(kind, phase)
    }

    override fun recordDuration(kind: JobKind, phase: String, durationMs: Long) {
        val seed = seedDurationMs(kind, phase)
        if (!acceptDurationSample(seed, durationMs)) return
        val key = durationKey(kind, phase)
        val previous = durations[key] ?: seed
        durations[key] = emaNext(previous, durationMs)
    }

    override fun expectedBytes(kind: JobKind): Long? {
        val key = bytesKey(kind) ?: return null
        return bytes[key]?.takeIf { it > 0L }
    }

    override fun recordBytes(kind: JobKind, bytes: Long) {
        if (bytes <= 0L) return
        val key = bytesKey(kind) ?: return
        val previous = this.bytes[key] ?: 0L
        this.bytes[key] = if (previous <= 0L) bytes else emaNext(previous, bytes)
    }

    override fun hasLearnedDuration(kind: JobKind, phase: String): Boolean =
        durations.containsKey(durationKey(kind, phase))
}

internal fun durationKey(kind: JobKind, phase: String): String {
    val name = if (kind == JobKind.Print) canonicalPhase(phase) else phase
    return "emaMs.${kind.name.lowercase()}.$name"
}

internal fun bytesKey(kind: JobKind): String? = when (kind) {
    JobKind.FtpTransfer -> "emaBytes.ftp"
    JobKind.Import, JobKind.Share -> "emaBytes.import"
    else -> null
}

internal fun acceptDurationSample(seedMs: Long, durationMs: Long): Boolean {
    if (durationMs < PrefsProgressStats.MIN_SAMPLE_MS) return false
    return durationMs <= seedMs * PrefsProgressStats.MAX_SEED_MULTIPLIER
}

internal fun emaNext(previous: Long, sample: Long): Long =
    (PrefsProgressStats.ALPHA * sample + (1.0 - PrefsProgressStats.ALPHA) * previous)
        .toLong()
        .coerceAtLeast(1L)

private const val DEFAULT_SEED_MS = PrefsProgressStats.DEFAULT_SEED_MS

private val SEEDS: Map<Pair<JobKind, String>, Long> = mapOf(
    (JobKind.FtpTransfer to "receiving") to 4_000L,
    (JobKind.FtpTransfer to "transcoding") to 2_000L,
    (JobKind.FtpTransfer to "ingesting") to 1_000L,
    (JobKind.Print to "editing") to 25_000L,
    (JobKind.Print to "preparing") to 1_500L,
    (JobKind.Print to "connecting") to 6_000L,
    (JobKind.Print to "sending") to 8_000L,
    (JobKind.Print to "printing") to 40_000L,
    (JobKind.Ai to "preparing") to 800L,
    (JobKind.Ai to "uploading") to 3_000L,
    (JobKind.Ai to "processing") to 25_000L,
    (JobKind.Ai to "downloading") to 3_000L,
    (JobKind.Ai to "saving") to 1_000L,
    (JobKind.Import to "receiving") to 200L,
    (JobKind.Import to "reading") to 200L,
    (JobKind.Import to "transcoding") to 2_000L,
    (JobKind.Import to "ingesting") to 1_000L,
    (JobKind.Share to "receiving") to 200L,
    (JobKind.Share to "reading") to 200L,
    (JobKind.Share to "transcoding") to 2_000L,
    (JobKind.Share to "ingesting") to 1_000L,
    (JobKind.Probe to "probing") to 3_000L,
    (JobKind.BtScan to "scanning") to 12_000L,
    (JobKind.BtPair to "pairing") to 6_000L,
    (JobKind.FtpStart to "starting") to 2_000L,
    (JobKind.StudioDecode to "decoding") to 800L,
)
