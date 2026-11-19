package io.github.wa_otomia.darkroom.data.jobs

import io.github.wa_otomia.darkroom.core.AiJobSnapshot
import io.github.wa_otomia.darkroom.core.AppliedFraming
import io.github.wa_otomia.darkroom.core.GENERATED_PHOTO_TAG
import io.github.wa_otomia.darkroom.core.ID_MASK_MSG_LOCAL_FAILED
import io.github.wa_otomia.darkroom.core.ID_MASK_MSG_NEED_KEY
import io.github.wa_otomia.darkroom.core.ID_MASK_MSG_VERSION_GONE
import io.github.wa_otomia.darkroom.core.combineGeneratePrompt
import io.github.wa_otomia.darkroom.core.generatedPhotoFilename
import io.github.wa_otomia.darkroom.core.IdMaskVerdict
import io.github.wa_otomia.darkroom.core.idMaskEndsAsCancel
import io.github.wa_otomia.darkroom.core.idMaskVerdictMessage
import io.github.wa_otomia.darkroom.core.resolvePhotoSource
import io.github.wa_otomia.darkroom.core.sanitizeAiSecrets
import io.github.wa_otomia.darkroom.core.validateLocalMaskLuma
import io.github.wa_otomia.darkroom.core.AiCredentialState
import io.github.wa_otomia.darkroom.core.aiNotReadyMessage
import io.github.wa_otomia.darkroom.core.idMaskNotReadyMessage
import io.github.wa_otomia.darkroom.data.catalog.CatalogRepository
import io.github.wa_otomia.darkroom.data.ai.AiImageClient
import io.github.wa_otomia.darkroom.data.imaging.ImagePipeline
import io.github.wa_otomia.darkroom.data.imaging.LocalSelfieSegmenter
import io.github.wa_otomia.darkroom.data.printer.StudioJobEvent
import io.github.wa_otomia.darkroom.data.progress.JobKind
import io.github.wa_otomia.darkroom.data.progress.JobProgress
import io.github.wa_otomia.darkroom.data.progress.JobProgressTracker
import io.github.wa_otomia.darkroom.data.progress.ProgressStats
import io.github.wa_otomia.darkroom.data.progress.aiPhases
import io.github.wa_otomia.darkroom.data.progress.applyAiProgress
import io.github.wa_otomia.darkroom.data.progress.localMaskPhases
import io.github.wa_otomia.darkroom.data.settings.SettingsRepository
import io.github.wa_otomia.darkroom.di.ApplicationScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

enum class AiKind { Generate, Edit, Mask }

data class AiJob(
    val id: String,
    val kind: AiKind,
    val sourcePhotoId: String,
    val source: String,
    val targetPhotoId: String,
    val phase: String,
    val loaded: Long?,
    val total: Long?,
    val hidden: Boolean,
    val startedAt: Long,
    val error: String?,
    val progress: JobProgress? = null,
    /** A [AiKind.Mask] job run on this device (EDID040-R11): no provider request, no upload. */
    val local: Boolean = false,
) {
    fun toSnapshot(): AiJobSnapshot = AiJobSnapshot(
        id = id,
        kind = when (kind) {
            AiKind.Generate -> "generate"
            AiKind.Edit -> "edit"
            AiKind.Mask -> "mask"
        },
        sourcePhotoId = sourcePhotoId,
        targetPhotoId = targetPhotoId,
        phase = phase,
        loaded = loaded,
        total = total,
        hidden = hidden,
        error = error,
        jobProgress = progress,
    )
}

@Singleton
class AiJobs @Inject constructor(
    private val catalog: CatalogRepository,
    private val settings: SettingsRepository,
    private val grok: AiImageClient,
    private val segmenter: LocalSelfieSegmenter,
    private val stats: ProgressStats,
    @ApplicationScope private val appScope: CoroutineScope,
) {
    private val _jobs = MutableStateFlow<List<AiJob>>(emptyList())
    val jobs: StateFlow<List<AiJob>> = _jobs

    private val _events = MutableSharedFlow<StudioJobEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<StudioJobEvent> = _events

    private val handles = ConcurrentHashMap<String, Job>()

    fun enqueueGenerate(
        photoId: String,
        source: String,
        presetId: String,
        prompt: String,
        appliedFraming: AppliedFraming? = null,
    ) {
        start(AiKind.Generate, photoId, source, presetId, prompt, appliedFraming)
    }

    fun enqueueEdit(
        photoId: String,
        source: String,
        presetId: String,
        prompt: String,
        appliedFraming: AppliedFraming? = null,
        // false for import automation: it must not move the Studio default (AUTG040-R02).
        rememberPreset: Boolean = true,
    ) {
        start(AiKind.Edit, photoId, source, presetId, prompt, appliedFraming, rememberPreset)
    }

    /**
     * Starts a person-matte job for one version (EDID040, AIJB010-R04). Returns the job id, or null
     * when any AI job row for [photoId] exists (hidden or still in its finished/failed tail).
     * Without [local], throws [IllegalStateException] with [ID_MASK_MSG_NEED_KEY] (or the
     * subscription variant from [idMaskNotReadyMessage]) when the AI credential is not ready.
     *
     * [local] (EDID040-R10/R11) is frozen here: the job segments on this device with
     * [LocalSelfieSegmenter], needs no key, sends no provider request and uploads nothing,
     * whatever the setting reads later. Either way the version key is frozen here, so switching
     * versions later still stores the mask on the originating version. The job never uses presets
     * or extra prompts, never touches the last preset, and never creates an edit or a photo: only
     * [CatalogRepository.saveIdPhotoMask] commits. A cancel before the commit discards any late
     * result.
     */
    fun enqueueMask(photoId: String, source: String, local: Boolean = false): String? {
        // D8 gate: no ready credential means no job and no request. The AI job re-checks at
        // execution time. A local cut-out never needs a credential (EDID040-R10/R11).
        if (!local) {
            settings.aiCredentialState().takeIf { it != AiCredentialState.READY }?.let { error(idMaskNotReadyMessage(it)) }
        }
        val versionKey = resolvePhotoSource(source)
        val id = UUID.randomUUID().toString()
        val row = AiJob(
            id = id,
            kind = AiKind.Mask,
            sourcePhotoId = photoId,
            source = versionKey,
            targetPhotoId = photoId,
            phase = "preparing",
            loaded = null,
            total = null,
            hidden = false,
            startedAt = System.currentTimeMillis(),
            error = null,
            local = local,
        )
        // Every writer of _jobs mutates through MutableStateFlow.update, so this gate and the
        // append are one atomic step and no concurrent tick can drop the new row.
        var added = false
        _jobs.update { current ->
            if (current.any { it.sourcePhotoId == photoId }) {
                added = false
                current
            } else {
                added = true
                current + row
            }
        }
        if (!added) return null
        val phases = if (local) localMaskPhases(stats) else aiPhases(stats)
        val tracker = JobProgressTracker(id, JobKind.Ai, phases, stats)
        tracker.startPhase("preparing")
        publish(id, tracker)
        val handle = appScope.launch(start = CoroutineStart.LAZY) {
            val ticker = tracker.launchTicks(this) { publish(id, tracker) }
            var committed = false
            // EDID040-R09: the last reply received, kept with the failure record whatever fails
            // after it arrives (undecodable, wrong aspect, not a matte, version gone). A local job
            // keeps its own mask only when the white-share check rejects it (EDID040-R11).
            var reply: ByteArray? = null
            try {
                // EDID040-R09: a new attempt drops the version's last failure record.
                withContext(Dispatchers.IO) { runCatching { catalog.clearIdMaskFailure(photoId, versionKey) } }
                if (!local) {
                    settings.aiCredentialState().takeIf { it != AiCredentialState.READY }?.let { error(idMaskNotReadyMessage(it)) }
                }
                val raw = withContext(Dispatchers.IO) {
                    val file = catalog.sourceFile(photoId, versionKey)
                    if (!file.isFile) error(ID_MASK_MSG_VERSION_GONE)
                    file.readBytes()
                }
                val png = if (local) {
                    runLocalMask(raw, tracker, id) { reply = it }
                } else {
                    val response = grok.mask(raw, photoId) { progress ->
                        applyAiProgress(tracker, progress)
                        publish(id, tracker)
                    }
                    reply = response.bytes
                    tracker.startPhase("saving")
                    publish(id, tracker)
                    withContext(Dispatchers.Default) {
                        val decoded = ImagePipeline.decodeIdMask(response.bytes, response.upload)
                        ImagePipeline.encodeIdMaskPng(decoded)
                    }
                }
                ensureActive()
                withContext(NonCancellable) {
                    if (!catalog.saveIdPhotoMask(photoId, versionKey, png)) error(ID_MASK_MSG_VERSION_GONE)
                    committed = true
                    withContext(Dispatchers.IO) { runCatching { catalog.clearIdMaskFailure(photoId, versionKey) } }
                    tracker.succeed()
                    publish(id, tracker)
                    _events.tryEmit(StudioJobEvent.MaskReady(photoId, versionKey))
                }
            } catch (e: Exception) {
                if (idMaskEndsAsCancel(e, isActive)) {
                    // User cancel (or the app scope ending), including one that lands while the
                    // reply is decoded and surfaces as the decode's own error: no failure record
                    // (EDID040-R09).
                    if (!committed) {
                        _jobs.update { list -> list.filter { it.id != id } }
                        _events.tryEmit(StudioJobEvent.Failed(photoId, AiKind.Mask.action, ""))
                    }
                } else {
                    val message = sanitizeAiSecrets(e.message ?: e.toString())
                    if (!committed) {
                        // EDID040-R09: every other failure leaves a record (with the reply when
                        // one arrived) before it is reported, so the screen finds it on the
                        // Failed event. Keeping it never changes the job's result.
                        withContext(Dispatchers.IO + NonCancellable) {
                            runCatching { catalog.saveIdMaskFailure(photoId, versionKey, message, reply, local) }
                        }
                    }
                    tracker.fail(message)
                    publish(id, tracker)
                    _events.tryEmit(StudioJobEvent.Failed(photoId, AiKind.Mask.action, message))
                }
            } finally {
                ticker.cancel()
                withContext(NonCancellable) {
                    delay(KEEP_MS)
                    _jobs.update { list -> list.filter { it.id != id } }
                    handles.remove(id)
                }
            }
        }
        handles[id] = handle
        handle.start()
        return id
    }

    /**
     * The local half of [enqueueMask] (EDID040-R11): upright bounded input, one on-device
     * inference, the white-share check, then the grey PNG to commit. [keepReply] receives the
     * PNG only when the check rejects it, so the failure record can show what was found.
     * Out-of-memory while preparing or encoding is a local failure, never a crash.
     */
    private suspend fun runLocalMask(
        raw: ByteArray,
        tracker: JobProgressTracker,
        id: String,
        keepReply: (ByteArray) -> Unit,
    ): ByteArray {
        // The input is decoded under the segmenter's lock and the phase moves to "segmenting" only
        // once it is held: a job waiting for another photo's inference stays in "preparing" and
        // holds no bitmap (design D4).
        val decoded = withContext(Dispatchers.Default) {
            segmenter.segment(
                prepare = { localOutOfMemory { ImagePipeline.localMaskInput(raw) } },
                onSegmenting = {
                    tracker.startPhase("segmenting")
                    publish(id, tracker)
                },
            )
        }
        tracker.startPhase("saving")
        publish(id, tracker)
        return withContext(Dispatchers.Default) {
            val png = localOutOfMemory { ImagePipeline.encodeIdMaskPng(decoded) }
            val verdict = validateLocalMaskLuma(decoded.luma)
            if (verdict != IdMaskVerdict.OK) {
                keepReply(png)
                error(idMaskVerdictMessage(verdict))
            }
            png
        }
    }

    /**
     * Runs [block], turning running out of memory (raw, or wrapped by ImagePipeline as its decode
     * message) into the local failure message (EDID040-R11).
     */
    private inline fun <T> localOutOfMemory(block: () -> T): T {
        try {
            return block()
        } catch (oom: OutOfMemoryError) {
            error("$ID_MASK_MSG_LOCAL_FAILED：${oom.javaClass.simpleName}")
        } catch (e: IllegalStateException) {
            val cause = e.cause
            if (cause is OutOfMemoryError) error("$ID_MASK_MSG_LOCAL_FAILED：${cause.javaClass.simpleName}")
            throw e
        }
    }

    fun cancel(id: String): Pair<Boolean, String> {
        val handle = handles[id] ?: return false to ""
        handle.cancel()
        return true to ""
    }

    fun hide(id: String) {
        update(id) { it.copy(hidden = true) }
    }

    private fun start(
        kind: AiKind,
        photoId: String,
        source: String,
        presetId: String,
        prompt: String,
        appliedFraming: AppliedFraming? = null,
        rememberPreset: Boolean = true,
    ) {
        val id = UUID.randomUUID().toString()
        val targetPhotoId = if (kind == AiKind.Generate) UUID.randomUUID().toString() else photoId
        val startedAt = System.currentTimeMillis()
        val row = AiJob(
            id = id,
            kind = kind,
            sourcePhotoId = photoId,
            source = source,
            targetPhotoId = targetPhotoId,
            phase = "preparing",
            loaded = null,
            total = null,
            hidden = false,
            startedAt = startedAt,
            error = null,
        )
        _jobs.update { it + row }
        val tracker = JobProgressTracker(id, JobKind.Ai, aiPhases(stats), stats)
        tracker.startPhase("preparing")
        publish(id, tracker)
        val handle = appScope.launch {
            val ticker = tracker.launchTicks(this) { publish(id, tracker) }
            try {
                if (presetId.isNotEmpty()) {
                    if (rememberPreset) settings.setLastPreset(presetId)
                    else if (settings.readPresets().presets.none { it.id == presetId }) error("预设不存在")
                }
                val store = settings.readPresets()
                val preset = store.presets.find { it.id == presetId }
                val presetPrompt = preset?.prompt.orEmpty()
                val combined = combineGeneratePrompt(presetPrompt, prompt)
                settings.aiCredentialState().takeIf { it != AiCredentialState.READY }?.let {
                    error(aiNotReadyMessage(it, if (kind == AiKind.Generate) "生成" else "修图"))
                }
                val file = catalog.sourceFile(photoId, resolvePhotoSource(source))
                val raw = file.readBytes()
                val upload = if (appliedFraming != null) {
                    ImagePipeline.cropToJpeg(
                        raw,
                        appliedFraming.crop,
                        appliedFraming.cropImageWidth,
                        appliedFraming.cropImageHeight,
                        rotateQuarters = appliedFraming.rotateQuarters,
                        landscape = appliedFraming.landscape,
                        rotationDegrees = appliedFraming.rotationDegrees,
                        placement = appliedFraming.placement,
                    )
                } else {
                    raw
                }
                val jpeg = grok.edit(upload, combined, photoId, purpose = kind.purpose) { progress ->
                    applyAiProgress(tracker, progress)
                    publish(id, tracker)
                }
                tracker.startPhase("saving")
                publish(id, tracker)
                when (kind) {
                    AiKind.Generate -> {
                        val from = catalog.get(photoId) ?: error("照片不存在")
                        val created = catalog.ingestBytes(
                            jpeg,
                            generatedPhotoFilename(from.filename),
                            kind = "studio",
                            id = targetPhotoId,
                            tags = listOf(GENERATED_PHOTO_TAG),
                        )
                        tracker.succeed()
                        publish(id, tracker)
                        _events.tryEmit(StudioJobEvent.Generated(photoId, created))
                    }
                    AiKind.Edit -> {
                        val editId = catalog.saveEdit(
                            photoId,
                            combined,
                            jpeg,
                            presetId = presetId,
                            presetTitle = preset?.title.orEmpty(),
                        ).id
                        tracker.succeed()
                        publish(id, tracker)
                        _events.tryEmit(StudioJobEvent.Edited(photoId, editId))
                    }
                    // Mask jobs run only through enqueueMask; start() is never called with this kind.
                    AiKind.Mask -> error("mask jobs use enqueueMask")
                }
            } catch (e: CancellationException) {
                _jobs.update { list -> list.filter { it.id != id } }
                _events.tryEmit(StudioJobEvent.Failed(photoId, kind.action, ""))
            } catch (e: Exception) {
                val message = sanitizeAiSecrets(e.message ?: e.toString())
                tracker.fail(message)
                publish(id, tracker)
                _events.tryEmit(StudioJobEvent.Failed(photoId, kind.action, message))
            } finally {
                ticker.cancel()
                withContext(NonCancellable) {
                    delay(KEEP_MS)
                    _jobs.update { list -> list.filter { it.id != id } }
                    handles.remove(id)
                }
            }
        }
        handles[id] = handle
    }

    private fun publish(id: String, tracker: JobProgressTracker) {
        val snap = tracker.snapshot()
        update(id) {
            it.copy(
                phase = if (snap.finished) "done" else if (snap.failed) "error" else snap.phase,
                loaded = snap.bytesLoaded,
                total = snap.bytesTotal,
                error = snap.error,
                progress = snap,
            )
        }
    }

    private fun update(id: String, transform: (AiJob) -> AiJob) {
        _jobs.update { list -> list.map { if (it.id == id) transform(it) else it } }
    }

    private val AiKind.purpose: String
        get() = when (this) {
            AiKind.Generate -> "generate"
            AiKind.Edit -> "edit"
            AiKind.Mask -> "mask"
        }

    private val AiKind.action: String
        get() = when (this) {
            AiKind.Generate -> "generate"
            AiKind.Edit -> "edit"
            AiKind.Mask -> "mask"
        }

    companion object {
        private const val KEEP_MS = 1_500L
    }
}
