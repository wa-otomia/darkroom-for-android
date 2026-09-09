package app.darkroom.android.data.jobs

import app.darkroom.android.core.AiJobSnapshot
import app.darkroom.android.core.combineGeneratePrompt
import app.darkroom.android.core.generatedPhotoFilename
import app.darkroom.android.core.resolvePhotoSource
import app.darkroom.android.data.catalog.CatalogRepository
import app.darkroom.android.data.ai.AiImageClient
import app.darkroom.android.data.printer.StudioJobEvent
import app.darkroom.android.data.progress.JobKind
import app.darkroom.android.data.progress.JobProgress
import app.darkroom.android.data.progress.JobProgressTracker
import app.darkroom.android.data.progress.ProgressStats
import app.darkroom.android.data.progress.aiPhases
import app.darkroom.android.data.progress.applyAiProgress
import app.darkroom.android.data.settings.SettingsRepository
import app.darkroom.android.di.ApplicationScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

enum class AiKind { Generate, Edit }

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

    fun enqueueGenerate(photoId: String, source: String, presetId: String, prompt: String) {
        start(AiKind.Generate, photoId, source, presetId, prompt)
    }

    fun enqueueEdit(photoId: String, source: String, presetId: String, prompt: String) {
        start(AiKind.Edit, photoId, source, presetId, prompt)
    }

    fun cancel(id: String): Pair<Boolean, String> {
        val handle = handles[id] ?: return false to ""
        handle.cancel()
        return true to ""
    }

    fun hide(id: String) {
        update(id) { it.copy(hidden = true) }
    }

    private fun start(kind: AiKind, photoId: String, source: String, presetId: String, prompt: String) {
        val id = UUID.randomUUID().toString()
        val targetPhotoId = if (kind == AiKind.Generate) UUID.randomUUID().toString() else photoId
        val startedAt = System.currentTimeMillis()
        _jobs.value = _jobs.value + AiJob(
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
        val tracker = JobProgressTracker(id, JobKind.Ai, aiPhases(stats), stats)
        tracker.startPhase("preparing")
        publish(id, tracker)
        val handle = appScope.launch {
            val ticker = tracker.launchTicks(this) { publish(id, tracker) }
            try {
                if (presetId.isNotEmpty()) settings.setLastPreset(presetId)
                val store = settings.readPresets()
                val preset = store.presets.find { it.id == presetId }
                val presetPrompt = preset?.prompt.orEmpty()
                val combined = combineGeneratePrompt(presetPrompt, prompt)
                if (!settings.aiConfigured()) error("未配置 AI 钥匙。去设置页填入 API key 后再${if (kind == AiKind.Generate) "生成" else "修图"}。")
                val file = catalog.sourceFile(photoId, resolvePhotoSource(source))
                val jpeg = grok.edit(file.readBytes(), combined, photoId, purpose = kind.purpose) { progress ->
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
                }
            } catch (e: CancellationException) {
                _jobs.value = _jobs.value.filter { it.id != id }
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
                    _jobs.value = _jobs.value.filter { it.id != id }
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
        _jobs.value = _jobs.value.map { if (it.id == id) transform(it) else it }
    }

    private val AiKind.purpose: String
        get() = if (this == AiKind.Generate) "generate" else "edit"

    private val AiKind.action: String
        get() = if (this == AiKind.Generate) "generate" else "edit"

    companion object {
        private const val KEEP_MS = 1_500L
    }
}
