package io.github.wa_otomia.darkroom.data.jobs

import io.github.wa_otomia.darkroom.core.AiJobSnapshot
import io.github.wa_otomia.darkroom.core.AppliedFraming
import io.github.wa_otomia.darkroom.core.GENERATED_PHOTO_TAG
import io.github.wa_otomia.darkroom.core.ID_MASK_MSG_NEED_KEY
import io.github.wa_otomia.darkroom.core.ID_MASK_MSG_VERSION_GONE
import io.github.wa_otomia.darkroom.core.combineGeneratePrompt
import io.github.wa_otomia.darkroom.core.generatedPhotoFilename
import io.github.wa_otomia.darkroom.core.resolvePhotoSource
import io.github.wa_otomia.darkroom.core.sanitizeAiSecrets
import io.github.wa_otomia.darkroom.data.catalog.CatalogRepository
import io.github.wa_otomia.darkroom.data.ai.AiImageClient
import io.github.wa_otomia.darkroom.data.imaging.ImagePipeline
import io.github.wa_otomia.darkroom.data.printer.StudioJobEvent
import io.github.wa_otomia.darkroom.data.progress.JobKind
import io.github.wa_otomia.darkroom.data.progress.JobProgress
import io.github.wa_otomia.darkroom.data.progress.JobProgressTracker
import io.github.wa_otomia.darkroom.data.progress.ProgressStats
import io.github.wa_otomia.darkroom.data.progress.aiPhases
import io.github.wa_otomia.darkroom.data.progress.applyAiProgress
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
     * Throws [IllegalStateException] with [ID_MASK_MSG_NEED_KEY] when no AI key is configured.
     *
     * The version key is frozen here, so switching versions later still stores the mask on the
     * originating version. The job never uses presets or extra prompts, never touches the last
     * preset, and never creates an edit or a photo: only [CatalogRepository.saveIdPhotoMask] commits.
     * A cancel before the commit discards any late result.
     */
    fun enqueueMask(photoId: String, source: String): String? {
        // D8 gate: no key means no job and no request. The job re-checks at execution time.
        if (!settings.aiConfigured()) error(ID_MASK_MSG_NEED_KEY)
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
        val tracker = JobProgressTracker(id, JobKind.Ai, aiPhases(stats), stats)
        tracker.startPhase("preparing")
        publish(id, tracker)
        val handle = appScope.launch(start = CoroutineStart.LAZY) {
            val ticker = tracker.launchTicks(this) { publish(id, tracker) }
            var committed = false
            // EDID040-R09: the last reply received, kept with the failure record whatever fails
            // after it arrives (undecodable, wrong aspect, not a matte, version gone).
            var reply: ByteArray? = null
            try {
                // EDID040-R09: a new attempt drops the version's last failure record.
                withContext(Dispatchers.IO) { runCatching { catalog.clearIdMaskFailure(photoId, versionKey) } }
                if (!settings.aiConfigured()) error(ID_MASK_MSG_NEED_KEY)
                val raw = withContext(Dispatchers.IO) {
                    val file = catalog.sourceFile(photoId, versionKey)
                    if (!file.isFile) error(ID_MASK_MSG_VERSION_GONE)
                    file.readBytes()
                }
                val response = grok.mask(raw, photoId) { progress ->
                    applyAiProgress(tracker, progress)
                    publish(id, tracker)
                }
                reply = response.bytes
                tracker.startPhase("saving")
                publish(id, tracker)
                val png = withContext(Dispatchers.Default) {
                    val decoded = ImagePipeline.decodeIdMask(response.bytes, response.upload)
                    ImagePipeline.encodeIdMaskPng(decoded)
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
            } catch (e: CancellationException) {
                // User cancel (or the app scope ending): no failure record (EDID040-R09).
                if (!committed) {
                    _jobs.update { list -> list.filter { it.id != id } }
                    _events.tryEmit(StudioJobEvent.Failed(photoId, AiKind.Mask.action, ""))
                }
            } catch (e: Exception) {
                val message = sanitizeAiSecrets(e.message ?: e.toString())
                if (!committed) {
                    // EDID040-R09: every other failure leaves a record (with the reply when one
                    // arrived) before it is reported, so the screen finds it on the Failed event.
                    // Keeping it never changes the job's result.
                    withContext(Dispatchers.IO + NonCancellable) {
                        runCatching { catalog.saveIdMaskFailure(photoId, versionKey, message, reply) }
                    }
                }
                tracker.fail(message)
                publish(id, tracker)
                _events.tryEmit(StudioJobEvent.Failed(photoId, AiKind.Mask.action, message))
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
                if (!settings.aiConfigured()) error("未配置 AI 钥匙。去设置页填入 API key 后再${if (kind == AiKind.Generate) "生成" else "修图"}。")
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
                val message = e.message ?: e.toString()
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
