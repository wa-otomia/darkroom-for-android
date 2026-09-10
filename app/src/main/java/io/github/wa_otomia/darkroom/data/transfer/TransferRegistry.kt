package io.github.wa_otomia.darkroom.data.transfer

import io.github.wa_otomia.darkroom.core.isJpegName
import io.github.wa_otomia.darkroom.data.progress.JobKind
import io.github.wa_otomia.darkroom.data.progress.JobProgress
import io.github.wa_otomia.darkroom.data.progress.JobProgressTracker
import io.github.wa_otomia.darkroom.data.progress.ProgressStats
import io.github.wa_otomia.darkroom.data.progress.SizeSource
import io.github.wa_otomia.darkroom.data.progress.growEstimateIfExceeded
import io.github.wa_otomia.darkroom.data.progress.ingestPhases
import io.github.wa_otomia.darkroom.data.progress.resolveIncomingSize
import io.github.wa_otomia.darkroom.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

data class FtpSessionInfo(
    val id: String,
    val remote: String,
    val user: String,
    val authed: Boolean,
    val connectedAt: Long,
    val currentTransferId: String?,
    val currentFilename: String? = null,
    val currentBytes: Long = 0L,
    val lastCompletedBytes: Long? = null,
)

enum class TransferState { Receiving, Transcoding, Ingesting, Done, Failed }

data class IncomingTransfer(
    val id: String,
    val photoId: String,
    val filename: String,
    val bytes: Long,
    val total: Long?,
    val state: TransferState,
    val startedAt: Long,
    val error: String?,
    val updatedAt: Long = startedAt,
    val sizeSource: SizeSource = SizeSource.Unknown,
    val phase: String = "receiving",
    val progress: JobProgress? = null,
    val kind: JobKind = JobKind.FtpTransfer,
)

@Singleton
class TransferRegistry @Inject constructor(
    @ApplicationScope private val scope: CoroutineScope,
    private val stats: ProgressStats,
) {
    private val _sessions = MutableStateFlow<List<FtpSessionInfo>>(emptyList())
    private val clock = MutableStateFlow(System.currentTimeMillis())
    private val trackers = ConcurrentHashMap<String, JobProgressTracker>()
    private val sessionByTransfer = ConcurrentHashMap<String, String>()
    private val receiveClosed = ConcurrentHashMap.newKeySet<String>()
    private val lastCompletedBySession = ConcurrentHashMap<String, Long>()

    /**
     * Visible FTP sessions: authenticated, or still within [UNAUTH_VISIBLE_MS]
     * of connect (so a camera that is mid-login is shown, then dropped).
     */
    val sessions: StateFlow<List<FtpSessionInfo>> = combine(_sessions, clock) { list, now ->
        list.filter { it.authed || now - it.connectedAt < UNAUTH_VISIBLE_MS }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val _transfers = MutableStateFlow<List<IncomingTransfer>>(emptyList())
    val transfers: StateFlow<List<IncomingTransfer>> = _transfers.asStateFlow()

    init {
        scope.launch {
            while (true) {
                delay(STALE_CHECK_MS)
                val now = System.currentTimeMillis()
                clock.value = now
                failStaleReceiving(now)
            }
        }
        scope.launch {
            while (true) {
                delay(JobProgressTracker.TICK_INTERVAL_MS)
                if (trackers.isEmpty()) continue
                trackers.keys.toList().forEach { id ->
                    val tracker = trackers[id] ?: return@forEach
                    if (tracker.isActive()) {
                        tracker.tick()
                        publish(id)
                    }
                }
            }
        }
    }

    fun sessionOpened(id: String, remote: String) {
        val info = FtpSessionInfo(
            id = id,
            remote = remote,
            user = "",
            authed = false,
            connectedAt = System.currentTimeMillis(),
            currentTransferId = null,
            lastCompletedBytes = lastCompletedBySession[id],
        )
        _sessions.update { list -> list.filterNot { it.id == id } + info }
    }

    fun sessionAuthed(id: String, user: String) {
        _sessions.update { list ->
            list.map { if (it.id == id) it.copy(user = user, authed = true) else it }
        }
    }

    fun sessionClosed(id: String) {
        _sessions.update { list -> list.filterNot { it.id == id } }
    }

    fun transferStarted(
        sessionId: String,
        filename: String,
        total: Long?,
        bytes: Long = 0L,
        state: TransferState = TransferState.Receiving,
    ): IncomingTransfer = begin(
        sessionId = sessionId,
        filename = filename,
        allo = total,
        bytes = bytes,
        state = state,
        kind = JobKind.FtpTransfer,
    )

    fun beginLocalIngest(
        filename: String,
        total: Long?,
        kind: JobKind = JobKind.Share,
        photoId: String = UUID.randomUUID().toString(),
        bytes: Long = 0L,
    ): IncomingTransfer = begin(
        sessionId = null,
        filename = filename,
        allo = total,
        bytes = bytes,
        state = TransferState.Receiving,
        kind = kind,
        photoId = photoId,
    )

    fun sessionActivity(sessionId: String, filename: String, bytes: Long) {
        _sessions.update { list ->
            list.map {
                if (it.id == sessionId) it.copy(currentFilename = filename, currentBytes = bytes) else it
            }
        }
    }

    fun transferProgress(id: String, bytes: Long, total: Long?) {
        val now = System.currentTimeMillis()
        val tracker = trackers[id]
        if (tracker != null) {
            val snap = tracker.snapshot()
            val incoming = total ?: snap.bytesTotal
            val grown = if (
                incoming != null &&
                snap.sizeSource != SizeSource.Allo &&
                snap.sizeSource != SizeSource.Unknown
            ) {
                growEstimateIfExceeded(incoming, bytes)
            } else {
                incoming
            }
            tracker.setBytes(bytes, grown)
            publish(id)
            return
        }
        _transfers.update { list ->
            list.map { row ->
                if (row.id != id) {
                    row
                } else {
                    row.copy(bytes = bytes, total = total ?: row.total, updatedAt = now)
                }
            }
        }
    }

    fun receiveFinished(id: String, received: Long? = null) {
        if (!receiveClosed.add(id)) return
        val tracker = trackers[id]
        val got = received ?: tracker?.snapshot()?.bytesLoaded ?: row(id)?.bytes ?: 0L
        if (tracker != null) {
            val snap = tracker.snapshot()
            tracker.setBytes(got, snap.bytesTotal)
            if (snap.phase == "receiving") tracker.completePhase()
            stats.recordBytes(snap.kind, got)
        }
        val sessionId = sessionByTransfer[id]
        if (sessionId != null && got > 0L) {
            lastCompletedBySession[sessionId] = got
            _sessions.update { list ->
                list.map {
                    if (it.id == sessionId) it.copy(lastCompletedBytes = got) else it
                }
            }
        }
        publish(id)
    }

    fun transcoding(id: String) {
        val tracker = trackers[id]
        if (tracker != null) {
            if (tracker.snapshot().phase != "transcoding") tracker.startPhase("transcoding")
            publish(id)
        } else {
            setState(id, TransferState.Transcoding)
        }
    }

    fun ingesting(id: String) {
        val tracker = trackers[id]
        if (tracker != null) {
            val phase = tracker.snapshot().phase
            if (phase == "receiving") tracker.completePhase()
            if (phase != "ingesting" && phase != "transcoding") {
                tracker.skipPhase("transcoding")
            }
            if (tracker.snapshot().phase != "ingesting") tracker.startPhase("ingesting")
            publish(id)
        } else {
            setState(id, TransferState.Ingesting)
        }
    }

    fun done(id: String) {
        trackers[id]?.succeed()
        setState(id, TransferState.Done, error = null)
        publish(id)
        clearSessionTransfer(id)
        scope.launch {
            delay(DONE_RETAIN_MS)
            _transfers.update { list ->
                list.filterNot { it.id == id && it.state == TransferState.Done }
            }
            trackers.remove(id)
            sessionByTransfer.remove(id)
            receiveClosed.remove(id)
        }
    }

    fun failed(id: String, error: String) {
        if (id.isEmpty()) return
        trackers[id]?.fail(error)
        setState(id, TransferState.Failed, error = error)
        publish(id)
        clearSessionTransfer(id)
    }

    fun dismiss(id: String) {
        _transfers.update { list -> list.filterNot { it.id == id } }
        trackers.remove(id)
        sessionByTransfer.remove(id)
        receiveClosed.remove(id)
        clearSessionTransfer(id)
    }

    private fun begin(
        sessionId: String?,
        filename: String,
        allo: Long?,
        bytes: Long,
        state: TransferState,
        kind: JobKind,
        photoId: String = UUID.randomUUID().toString(),
    ): IncomingTransfer {
        val now = System.currentTimeMillis()
        val sessionLast = sessionId?.let { lastCompletedBySession[it] }
            ?: sessionId?.let { sid -> _sessions.value.find { it.id == sid }?.lastCompletedBytes }
        val (estimate, source) = resolveIncomingSize(allo, sessionLast, stats.expectedBytes(kind))
        val includeTranscode = !isJpegName(filename)
        val id = UUID.randomUUID().toString()
        val tracker = JobProgressTracker(
            jobId = id,
            kind = kind,
            phases = ingestPhases(includeTranscode, stats, kind),
            stats = stats,
        )
        val startPhase = when (state) {
            TransferState.Transcoding -> "transcoding"
            TransferState.Ingesting -> "ingesting"
            else -> "receiving"
        }
        tracker.startPhase(startPhase, estimate, source)
        if (bytes > 0L) tracker.setBytes(bytes, estimate, source)
        trackers[id] = tracker
        if (sessionId != null) sessionByTransfer[id] = sessionId
        val snap = tracker.snapshot()
        val transfer = IncomingTransfer(
            id = id,
            photoId = photoId,
            filename = filename,
            bytes = bytes,
            total = snap.bytesTotal,
            state = state,
            startedAt = now,
            error = null,
            updatedAt = now,
            sizeSource = snap.sizeSource,
            phase = snap.phase,
            progress = snap,
            kind = kind,
        )
        _transfers.update { it + transfer }
        if (sessionId != null) {
            _sessions.update { list ->
                list.map {
                    if (it.id == sessionId) {
                        it.copy(
                            currentTransferId = transfer.id,
                            currentFilename = filename,
                            currentBytes = bytes,
                        )
                    } else {
                        it
                    }
                }
            }
        }
        return transfer
    }

    private fun failStaleReceiving(now: Long) {
        val stale = _transfers.value.filter { isStaleReceiving(it.state, it.updatedAt, now) }
        stale.forEach { failed(it.id, "传输中断") }
    }

    private fun setState(id: String, state: TransferState, error: String? = null) {
        val now = System.currentTimeMillis()
        _transfers.update { list ->
            list.map { row ->
                if (row.id != id) row else row.copy(state = state, error = error, updatedAt = now)
            }
        }
    }

    private fun publish(id: String) {
        val tracker = trackers[id]
        val snap = tracker?.snapshot()
        val now = System.currentTimeMillis()
        _transfers.update { list ->
            list.map { row ->
                if (row.id != id) {
                    row
                } else {
                    val nextState = when {
                        row.state == TransferState.Failed || snap?.failed == true -> TransferState.Failed
                        row.state == TransferState.Done || snap?.finished == true -> TransferState.Done
                        snap?.phase == "transcoding" -> TransferState.Transcoding
                        snap?.phase == "ingesting" -> TransferState.Ingesting
                        else -> row.state
                    }
                    row.copy(
                        bytes = snap?.bytesLoaded ?: row.bytes,
                        total = snap?.bytesTotal,
                        sizeSource = snap?.sizeSource ?: row.sizeSource,
                        phase = snap?.phase ?: row.phase,
                        progress = snap,
                        state = nextState,
                        error = snap?.error ?: row.error,
                        updatedAt = now,
                    )
                }
            }
        }
    }

    private fun row(id: String): IncomingTransfer? = _transfers.value.find { it.id == id }

    private fun clearSessionTransfer(transferId: String) {
        _sessions.update { list ->
            list.map { session ->
                if (session.currentTransferId == transferId) {
                    session.copy(currentTransferId = null)
                } else {
                    session
                }
            }
        }
    }

    companion object {
        const val DONE_RETAIN_MS = 1_500L
        const val UNAUTH_VISIBLE_MS = 30_000L
        const val STALE_RECEIVING_MS = 180_000L
        private const val STALE_CHECK_MS = 15_000L
    }
}

fun isStaleReceiving(
    state: TransferState,
    updatedAt: Long,
    now: Long,
    staleMs: Long = TransferRegistry.STALE_RECEIVING_MS,
): Boolean = state == TransferState.Receiving && now - updatedAt > staleMs
