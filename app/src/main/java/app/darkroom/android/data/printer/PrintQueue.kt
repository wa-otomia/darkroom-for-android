package app.darkroom.android.data.printer

import android.content.Context
import app.darkroom.android.core.OrientedSize
import app.darkroom.android.core.PhotoMeta
import app.darkroom.android.core.PixelCrop
import app.darkroom.android.core.PrintFit
import app.darkroom.android.core.PrintRecord
import app.darkroom.android.core.PrintRestoreAction
import app.darkroom.android.core.ViewportPlacement
import app.darkroom.android.core.combineGeneratePrompt
import app.darkroom.android.core.exactQuarterTurns
import app.darkroom.android.core.framingFor
import app.darkroom.android.core.PrintJobCancelFlags
import app.darkroom.android.core.resolveQueuedPose
import app.darkroom.android.core.mapPrintWorkerFailure
import app.darkroom.android.core.printImageSize
import app.darkroom.android.core.resolvePrintLandscape
import app.darkroom.android.core.restoreRunningPrintJob
import app.darkroom.android.core.shouldEditBeforePrint
import app.darkroom.android.data.catalog.CatalogRepository
import app.darkroom.android.data.catalog.PhotoDatabase
import app.darkroom.android.data.catalog.PrintJobEntity
import app.darkroom.android.data.ai.AiImageClient
import app.darkroom.android.data.imaging.ImagePipeline
import app.darkroom.android.data.jobs.AiJobs
import app.darkroom.android.data.progress.JobKind
import app.darkroom.android.data.progress.JobProgress
import app.darkroom.android.data.progress.JobProgressTracker
import app.darkroom.android.data.progress.ProgressStats
import app.darkroom.android.data.progress.aiPhases
import app.darkroom.android.data.progress.applyAiProgress
import app.darkroom.android.data.progress.printPhases
import app.darkroom.android.data.settings.ActivityLog
import app.darkroom.android.data.settings.SettingsRepository
import app.darkroom.android.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class PrintProgress(
    val photoId: String,
    val phase: String,
    val jobId: Int? = null,
    val jobState: String? = null,
    val elapsedMs: Long? = null,
    val chunk: Int? = null,
    val total: Int? = null,
    val percent: Int? = null,
    val error: String? = null,
    val progress: JobProgress? = null,
)

sealed class StudioJobEvent {
    abstract val photoId: String

    data class Generated(override val photoId: String, val created: PhotoMeta) : StudioJobEvent()
    data class Edited(override val photoId: String, val editId: String) : StudioJobEvent()
    data class PrintFinished(override val photoId: String) : StudioJobEvent()
    data class Failed(override val photoId: String, val action: String, val message: String?) : StudioJobEvent()
}

data class PrintJob(
    val id: String,
    val photoId: String,
    val source: String,
    val crop: PixelCrop?,
    val copies: Int,
    val presetId: String?,
    val prompt: String,
    val cropImageWidth: Int?,
    val cropImageHeight: Int?,
    val rotateQuarters: Int,
    val landscape: Boolean,
    val rotationDegrees: Float,
    val placement: ViewportPlacement?,
    val origin: String,
    val watermark: Boolean,
    val state: String,
    val phase: String?,
    val createdAt: Long,
    val startedAt: Long?,
    val finishedAt: Long?,
    val printerJobId: Int?,
    val jobState: String?,
    val error: String?,
)

@Singleton
class PrintQueue @Inject constructor(
    private val catalog: CatalogRepository,
    private val settings: SettingsRepository,
    private val grok: AiImageClient,
    private val bluetooth: PrinterBluetooth,
    private val activityLog: ActivityLog,
    private val db: PhotoDatabase,
    private val aiJobs: AiJobs,
    private val stats: ProgressStats,
    @ApplicationScope private val appScope: CoroutineScope,
    @ApplicationContext private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val mutex = Mutex()
    private val rowMutex = Mutex()
    private val wake = Channel<Unit>(Channel.CONFLATED)
    private val cancelFlags = PrintJobCancelFlags()

    private var activePrinter: XiaomiPrinter? = null
    private var activeRowId: String? = null
    private var currentWork: Job? = null
    private var currentJobId: String? = null
    private var lastPersistedPhase: String? = null
    private var lastPersistedJobState: String? = null
    private var printTracker: JobProgressTracker? = null
    private var tickJob: Job? = null

    private val _progress = MutableSharedFlow<PrintProgress>(extraBufferCapacity = 64)
    val progress: SharedFlow<PrintProgress> = _progress

    private val _runningProgress = MutableStateFlow<PrintProgress?>(null)
    val runningProgress: StateFlow<PrintProgress?> = _runningProgress

    private val _jobs = MutableSharedFlow<StudioJobEvent>(extraBufferCapacity = 16)
    val jobs: SharedFlow<StudioJobEvent> = _jobs

    val printJobs: Flow<List<PrintJob>> = db.printJobDao().observeAll().map { rows ->
        rows.sortedBy { it.createdAt }.map { it.toUi(json) }
    }

    init {
        appScope.launch {
            for (ignored in wake) drain()
        }
    }

    fun restore() {
        appScope.launch {
            for (row in db.printJobDao().listRunning()) {
                when (restoreRunningPrintJob(row.phase)) {
                    PrintRestoreAction.Requeue -> persist(
                        row.copy(
                            state = STATE_QUEUED,
                            phase = null,
                            startedAt = null,
                            printerJobId = null,
                            jobState = null,
                            error = null,
                        ),
                    )
                    PrintRestoreAction.FailInterrupted -> persist(
                        row.copy(
                            state = STATE_FAILED,
                            error = "中断",
                            finishedAt = System.currentTimeMillis(),
                        ),
                    )
                }
            }
            kick()
        }
    }

    fun enqueuePrint(
        photoId: String,
        source: String,
        crop: PixelCrop?,
        copies: Int?,
        presetId: String?,
        prompt: String = "",
        cropImageWidth: Int? = null,
        cropImageHeight: Int? = null,
        rotateQuarters: Int = 0,
        landscape: Boolean? = null,
        rotationDegrees: Float = rotateQuarters * 90f,
        placement: ViewportPlacement? = null,
        watermark: Boolean = false,
    ) {
        enqueue(
            photoId = photoId,
            source = source,
            crop = crop,
            copies = copies,
            presetId = presetId,
            prompt = prompt,
            cropImageWidth = cropImageWidth,
            cropImageHeight = cropImageHeight,
            rotateQuarters = rotateQuarters,
            landscape = landscape,
            rotationDegrees = rotationDegrees,
            placement = placement,
            origin = ORIGIN_MANUAL,
            watermark = watermark,
        )
    }

    fun enqueue(
        photoId: String,
        source: String,
        crop: PixelCrop? = null,
        copies: Int? = null,
        presetId: String? = null,
        prompt: String = "",
        cropImageWidth: Int? = null,
        cropImageHeight: Int? = null,
        rotateQuarters: Int = 0,
        landscape: Boolean? = null,
        rotationDegrees: Float = rotateQuarters * 90f,
        placement: ViewportPlacement? = null,
        origin: String = ORIGIN_MANUAL,
        watermark: Boolean = false,
    ) {
        appScope.launch {
            val copiesN = (copies ?: settings.readSettings().defaultCopies).coerceIn(1, 9)
            val photo = catalog.get(photoId)
            val pose = resolveQueuedPose(
                saved = photo?.framingFor(source),
                explicitLandscape = landscape,
                explicitRotationDegrees = rotationDegrees,
                explicitPlacement = placement,
            )
            val resolvedLandscape = pose.landscape ?: resolveEnqueueLandscape(photoId, source, null)
            persist(
                PrintJobEntity(
                    id = UUID.randomUUID().toString(),
                    photoId = photoId,
                    source = source,
                    cropJson = crop?.let { json.encodeToString(PixelCrop.serializer(), it) },
                    copies = copiesN,
                    presetId = presetId,
                    prompt = prompt,
                    cropImageWidth = cropImageWidth,
                    cropImageHeight = cropImageHeight,
                    rotateQuarters = exactQuarterTurns(pose.rotationDegrees) ?: rotateQuarters,
                    landscape = resolvedLandscape,
                    rotationDegrees = pose.rotationDegrees,
                    placementJson = pose.placement?.let { encodePlacement(json, it) },
                    origin = origin,
                    watermark = watermark,
                    state = STATE_QUEUED,
                    phase = null,
                    createdAt = System.currentTimeMillis(),
                    startedAt = null,
                    finishedAt = null,
                    printerJobId = null,
                    jobState = null,
                    error = null,
                ),
            )
            kick()
        }
    }

    @Deprecated("Use AiJobs.enqueueGenerate")
    fun enqueueGenerate(photoId: String, source: String, presetId: String, prompt: String) {
        aiJobs.enqueueGenerate(photoId, source, presetId, prompt)
    }

    @Deprecated("Use AiJobs.enqueueEdit")
    fun enqueueEdit(photoId: String, source: String, presetId: String, prompt: String) {
        aiJobs.enqueueEdit(photoId, source, presetId, prompt)
    }

    fun cancel(jobId: String): Pair<Boolean, String> {
        appScope.launch { cancelJob(jobId) }
        return true to "已请求取消"
    }

    fun remove(id: String) {
        appScope.launch { db.printJobDao().delete(id) }
    }

    fun cancel(jobId: Int): Pair<Boolean, String> {
        val running = currentJobId
        if (running != null) cancelFlags.request(running)
        val printer = activePrinter
        if (printer != null) printer.requestCancel(jobId)
        if (running != null) {
            currentWork?.cancel()
            return true to "已请求取消"
        }
        return if (printer != null) true to "已请求取消" else false to "没有进行中的打印"
    }

    fun retry(id: String) {
        appScope.launch {
            val old = db.printJobDao().get(id) ?: return@launch
            persist(
                old.copy(
                    id = UUID.randomUUID().toString(),
                    state = STATE_QUEUED,
                    phase = null,
                    createdAt = System.currentTimeMillis(),
                    startedAt = null,
                    finishedAt = null,
                    printerJobId = null,
                    jobState = null,
                    error = null,
                ),
            )
            db.printJobDao().delete(id)
            kick()
        }
    }

    fun clearFinished() {
        appScope.launch {
            db.printJobDao().deleteWhereState(listOf(STATE_FAILED, STATE_CANCELLED, STATE_DONE))
        }
    }

    suspend fun applyPreset(photoId: String, source: String, presetId: String): String {
        val preset = settings.readPresets().presets.find { it.id == presetId } ?: error("预设不存在")
        if (!settings.aiConfigured()) error("未配置 AI 钥匙。去设置页填入 API key 后再修图。")
        val file = catalog.sourceFile(photoId, source)
        val jpeg = grok.edit(file.readBytes(), preset.prompt, photoId, "edit")
        return catalog.saveEdit(
            photoId,
            preset.prompt,
            jpeg,
            presetId = presetId,
            presetTitle = preset.title,
        ).id
    }

    suspend fun generate(photoId: String, source: String, presetId: String, prompt: String): PhotoMeta {
        val store = settings.readPresets()
        if (presetId.isNotEmpty()) settings.setLastPreset(presetId)
        val presetPrompt = store.presets.find { it.id == presetId }?.prompt.orEmpty()
        val combined = combineGeneratePrompt(presetPrompt, prompt)
        if (!settings.aiConfigured()) error("未配置 AI 钥匙。去设置页填入 API key 后再生成。")
        val file = catalog.sourceFile(photoId, source)
        val jpeg = grok.edit(file.readBytes(), combined, photoId, "generate")
        return catalog.saveGenerated(photoId, jpeg, combined)
    }

    suspend fun editInPlace(photoId: String, source: String, presetId: String, prompt: String): String {
        val store = settings.readPresets()
        val preset = store.presets.find { it.id == presetId }
        val presetPrompt = preset?.prompt.orEmpty()
        val combined = combineGeneratePrompt(presetPrompt, prompt)
        if (!settings.aiConfigured()) error("未配置 AI 钥匙。去设置页填入 API key 后再修图。")
        val file = catalog.sourceFile(photoId, source)
        val jpeg = grok.edit(file.readBytes(), combined, photoId, "edit")
        return catalog.saveEdit(
            photoId,
            combined,
            jpeg,
            presetId = presetId,
            presetTitle = preset?.title.orEmpty(),
        ).id
    }

    suspend fun printPhoto(
        photoId: String,
        source: String,
        crop: PixelCrop?,
        copies: Int?,
        presetId: String?,
        prompt: String = "",
        cropImageWidth: Int? = null,
        cropImageHeight: Int? = null,
        rotateQuarters: Int = 0,
        landscape: Boolean = false,
        rotationDegrees: Float = rotateQuarters * 90f,
        placement: ViewportPlacement? = null,
        watermark: Boolean = false,
    ): Pair<Int, String> = mutex.withLock {
        val t0 = System.currentTimeMillis()
        var usedSource = source
        var jobId = 0
        var printer: XiaomiPrinter? = null
        val cfg = settings.readSettings()
        val usedPreset = shouldEditBeforePrint(presetId, prompt)
        val tracker = JobProgressTracker(
            jobId = activeRowId ?: photoId,
            kind = JobKind.Print,
            phases = printPhases(includeEditing = usedPreset, stats = stats),
            stats = stats,
        )
        printTracker = tracker
        ensureTicker(tracker)
        try {
            throwIfCancelled()
            if (usedPreset) {
                tracker.startPhase("editing")
                emit(photoId, "editing")
                if (!settings.aiConfigured()) error("未配置 AI 钥匙。去设置页填入 API key 后再修图。")
                val store = settings.readPresets()
                val preset = store.presets.find { it.id == presetId }
                val presetPrompt = preset?.prompt.orEmpty()
                val combined = combineGeneratePrompt(presetPrompt, prompt)
                val srcBytes = catalog.sourceFile(photoId, source).readBytes()
                val cropped = ImagePipeline.cropToJpeg(
                    srcBytes,
                    crop,
                    cropImageWidth,
                    cropImageHeight,
                    rotateQuarters = rotateQuarters,
                    landscape = landscape,
                    rotationDegrees = rotationDegrees,
                    placement = placement,
                )
                throwIfCancelled()
                val nest = JobProgressTracker("$photoId-edit", JobKind.Ai, aiPhases(stats), stats)
                val nestTicks = nest.launchTicks(appScope) { snap ->
                    tracker.setNestedFraction(snap.fraction)
                    emit(photoId, "editing")
                }
                val jpeg = try {
                    grok.edit(cropped, combined, photoId, "edit") { grokProgress ->
                        applyAiProgress(nest, grokProgress)
                        tracker.setNestedFraction(nest.snapshot().fraction)
                        emit(photoId, "editing")
                    }
                } finally {
                    nestTicks.cancel()
                }
                usedSource = catalog.saveEdit(
                    photoId,
                    combined,
                    jpeg,
                    presetId = presetId.orEmpty(),
                    presetTitle = preset?.title.orEmpty(),
                ).id
                tracker.completePhase()
            }
            throwIfCancelled()
            tracker.startPhase("preparing")
            emit(photoId, "preparing")
            val copiesN = (copies ?: cfg.defaultCopies).coerceIn(1, 9)
            val bytes = catalog.sourceFile(photoId, usedSource).readBytes()
            val mark = if (watermark) settings.readWatermark() else null
            val photoMeta = catalog.get(photoId)
            val jpeg = if (usedPreset) {
                ImagePipeline.renderPrintJpeg(
                    bytes,
                    crop = null,
                    PrintFit.COVER,
                    landscape = landscape,
                    watermark = mark,
                    photo = photoMeta,
                    context = context,
                )
            } else {
                ImagePipeline.renderPrintJpeg(
                    bytes,
                    crop,
                    cfg.printFit,
                    cropImageWidth = cropImageWidth,
                    cropImageHeight = cropImageHeight,
                    rotateQuarters = rotateQuarters,
                    landscape = landscape,
                    rotationDegrees = rotationDegrees,
                    placement = placement,
                    watermark = mark,
                    photo = photoMeta,
                    context = context,
                )
            }
            throwIfCancelled()
            tracker.startPhase("connecting")
            emit(photoId, "connecting")
            if (cfg.printerMac.isBlank()) error("打印机未绑")
            val pipe = withContext(Dispatchers.IO) { bluetooth.connect(cfg.printerMac) }
            printer = XiaomiPrinter(pipe, readTimeout = 30_000)
            activePrinter = printer
            withContext(Dispatchers.IO) {
                printer.connect()
                printer.checkPaperReady()
            }
            throwIfCancelled()
            tracker.startPhase("sending")
            emit(photoId, "sending")
            jobId = withContext(Dispatchers.IO) {
                printer.printJpeg(jpeg, copiesN) { chunk, total ->
                    tracker.setChunks(chunk, total)
                    emit(
                        photoId,
                        "uploading",
                        jobId = jobId.takeIf { it != 0 },
                        jobState = "downloading",
                        chunk = chunk,
                        total = total,
                        percent = if (total > 0) (100 * chunk) / total else null,
                        elapsedMs = System.currentTimeMillis() - t0,
                    )
                }
            }
            tracker.completePhase()
            tracker.startPhase("printing")
            emit(photoId, "printing", jobId = jobId, jobState = "downloading")
            val done = withContext(Dispatchers.IO) {
                printer.waitUntilDone(jobId, 180_000, 2000) { state, _, elapsed ->
                    tracker.setJobState(state)
                    emit(photoId, "printing", jobId = jobId, jobState = state, elapsedMs = elapsed)
                }
            }
            val jobState = done["job_state"]?.toString() ?: "finished"
            catalog.recordPrint(
                photoId,
                PrintRecord(
                    at = Instant.now().toString(),
                    jobId = jobId,
                    jobState = jobState,
                    source = usedSource,
                    crop = if (usedPreset) null else crop,
                    copies = copiesN,
                ),
            )
            tracker.succeed()
            emit(photoId, "done", jobId = jobId, jobState = jobState, elapsedMs = System.currentTimeMillis() - t0)
            val sent = printer.lastSendStats?.let { " · upload ${it.elapsedMs} ms, %.1f KB/s".format(it.kbPerSec) }.orEmpty()
            activityLog.record("spp", "job $jobId $jobState$sent")
            jobId to jobState
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (cancelFlags.isRequested(activeRowId)) {
                throw CancellationException(e.message, e)
            }
            val message = e.message ?: e.toString()
            catalog.recordPrint(
                photoId,
                PrintRecord(
                    at = Instant.now().toString(),
                    jobId = jobId.takeIf { it != 0 },
                    source = usedSource,
                    crop = if (usedPreset) null else crop,
                    copies = copies ?: cfg.defaultCopies,
                    error = message,
                ),
            )
            tracker.fail(message)
            emit(photoId, "error", jobId = jobId.takeIf { it != 0 }, error = message)
            activityLog.record("spp", "print failed", status = "error", error = message)
            throw e
        } finally {
            try {
                printer?.disconnect()
            } catch (_: Exception) {
            }
            activePrinter = null
            printTracker = null
            tickJob?.cancel()
            tickJob = null
        }
    }

    private suspend fun drain() {
        while (true) {
            val running = takeNextQueued() ?: return
            supervisorScope {
                val work = launch { runQueued(running) }
                currentWork = work
                currentJobId = running.id
                try {
                    work.join()
                } finally {
                    currentWork = null
                    currentJobId = null
                }
            }
        }
    }

    private suspend fun runQueued(row: PrintJobEntity) {
        activeRowId = row.id
        lastPersistedPhase = row.phase
        lastPersistedJobState = row.jobState
        try {
            printPhoto(
                photoId = row.photoId,
                source = row.source,
                crop = decodeCrop(json, row.cropJson),
                copies = row.copies,
                presetId = row.presetId,
                prompt = row.prompt,
                cropImageWidth = row.cropImageWidth,
                cropImageHeight = row.cropImageHeight,
                rotateQuarters = row.rotateQuarters,
                landscape = row.landscape,
                rotationDegrees = row.rotationDegrees,
                placement = decodePlacement(json, row.placementJson),
                watermark = row.watermark,
            )
            val doneAt = System.currentTimeMillis()
            patchRow(row.id) { it.copy(state = STATE_DONE, phase = "done", finishedAt = doneAt, error = null) }
            _jobs.tryEmit(StudioJobEvent.PrintFinished(row.photoId))
            try {
                delay(DONE_KEEP_MS)
            } catch (_: CancellationException) {
            }
            withContext(NonCancellable) { db.printJobDao().delete(row.id) }
        } catch (e: CancellationException) {
            withContext(NonCancellable) {
                val outcome = mapPrintWorkerFailure(
                    cancellation = true,
                    cancelRequested = cancelFlags.isRequested(row.id),
                    message = e.message,
                )
                patchRow(row.id) {
                    if (it.state == STATE_RUNNING) {
                        it.copy(
                            state = outcome.state,
                            error = outcome.error,
                            finishedAt = System.currentTimeMillis(),
                        )
                    } else {
                        it
                    }
                }
            }
        } catch (e: Exception) {
            val message = e.message ?: e.toString()
            val outcome = mapPrintWorkerFailure(
                cancellation = false,
                cancelRequested = cancelFlags.isRequested(row.id),
                message = message,
            )
            withContext(NonCancellable) {
                patchRow(row.id) {
                    it.copy(
                        state = outcome.state,
                        phase = if (outcome.state == STATE_CANCELLED) it.phase else "error",
                        error = outcome.error,
                        finishedAt = System.currentTimeMillis(),
                    )
                }
            }
            if (outcome.state == STATE_FAILED) {
                _jobs.tryEmit(StudioJobEvent.Failed(row.photoId, "print", outcome.error))
            }
        } finally {
            activeRowId = null
            lastPersistedPhase = null
            lastPersistedJobState = null
            cancelFlags.clear(row.id)
            if (_runningProgress.value?.photoId == row.photoId) _runningProgress.value = null
        }
    }

    private suspend fun takeNextQueued(): PrintJobEntity? = rowMutex.withLock {
        val next = db.printJobDao().nextQueued() ?: return@withLock null
        val running = next.copy(state = STATE_RUNNING, startedAt = System.currentTimeMillis())
        db.printJobDao().upsert(running)
        running
    }

    private suspend fun cancelJob(jobId: String) {
        val running = rowMutex.withLock {
            val row = db.printJobDao().get(jobId) ?: return
            when (row.state) {
                STATE_QUEUED -> {
                    db.printJobDao().upsert(
                        row.copy(state = STATE_CANCELLED, error = null, finishedAt = System.currentTimeMillis()),
                    )
                    false
                }
                STATE_RUNNING -> {
                    cancelFlags.request(jobId)
                    activePrinter?.requestCancel(row.printerJobId ?: 0)
                    true
                }
                else -> false
            }
        }
        if (running && currentJobId == jobId) currentWork?.cancel()
    }

    private fun kick() {
        wake.trySend(Unit)
    }

    private suspend fun persist(row: PrintJobEntity) {
        rowMutex.withLock { db.printJobDao().upsert(row) }
    }

    private suspend fun patchRow(id: String, transform: (PrintJobEntity) -> PrintJobEntity) {
        rowMutex.withLock {
            val current = db.printJobDao().get(id) ?: return
            db.printJobDao().upsert(transform(current))
        }
    }

    private fun throwIfCancelled() {
        if (cancelFlags.isRequested(activeRowId ?: currentJobId)) error("已取消")
    }

    private suspend fun resolveEnqueueLandscape(
        photoId: String,
        source: String,
        explicit: Boolean?,
    ): Boolean {
        if (explicit != null) return explicit
        val photo = catalog.get(photoId)
        val fromMeta = printImageSize(photo, source)
        if (fromMeta != null) return resolvePrintLandscape(null, fromMeta)
        val probed = withContext(Dispatchers.IO) {
            val file = catalog.sourceFile(photoId, source)
            if (!file.exists()) return@withContext null
            val info = ImagePipeline.probe(file.readBytes())
            OrientedSize(info.width, info.height).takeIf { it.width > 0 && it.height > 0 }
        }
        return resolvePrintLandscape(null, probed)
    }

    private fun emit(
        photoId: String,
        phase: String,
        jobId: Int? = null,
        jobState: String? = null,
        elapsedMs: Long? = null,
        chunk: Int? = null,
        total: Int? = null,
        percent: Int? = null,
        error: String? = null,
    ) {
        val snap = printTracker?.tick()
        val update = PrintProgress(photoId, phase, jobId, jobState, elapsedMs, chunk, total, percent, error, snap)
        _runningProgress.value = update
        _progress.tryEmit(update)
        val rowId = activeRowId ?: return
        val phaseChanged = phase != lastPersistedPhase || jobState != lastPersistedJobState
        val chunkOnly = chunk != null && !phaseChanged
        if (chunkOnly) return
        lastPersistedPhase = phase
        lastPersistedJobState = jobState
        appScope.launch {
            patchRow(rowId) {
                it.copy(
                    phase = phase,
                    printerJobId = jobId ?: it.printerJobId,
                    jobState = jobState ?: it.jobState,
                    error = error,
                )
            }
        }
    }

    private fun ensureTicker(tracker: JobProgressTracker) {
        tickJob?.cancel()
        tickJob = tracker.launchTicks(appScope) { snap ->
            val current = _runningProgress.value ?: return@launchTicks
            _runningProgress.value = current.copy(
                progress = snap,
                elapsedMs = snap.jobElapsedMs,
                jobState = snap.jobState ?: current.jobState,
            )
        }
    }

    companion object {
        const val ORIGIN_MANUAL = "manual"
        const val ORIGIN_AUTO = "auto"
        const val STATE_QUEUED = "queued"
        const val STATE_RUNNING = "running"
        const val STATE_DONE = "done"
        const val STATE_FAILED = "failed"
        const val STATE_CANCELLED = "cancelled"
        private const val DONE_KEEP_MS = 2_000L
    }
}

@Serializable
private data class PlacementWire(
    val zoom: Float,
    val offsetX: Float,
    val offsetY: Float,
    val rotationDegrees: Float,
    val frameWidth: Float,
    val frameHeight: Float,
    val imageWidth: Int,
    val imageHeight: Int,
)

private fun encodePlacement(json: Json, placement: ViewportPlacement): String = json.encodeToString(
    PlacementWire.serializer(),
    PlacementWire(
        zoom = placement.zoom,
        offsetX = placement.offsetX,
        offsetY = placement.offsetY,
        rotationDegrees = placement.rotationDegrees,
        frameWidth = placement.frameWidth,
        frameHeight = placement.frameHeight,
        imageWidth = placement.imageWidth,
        imageHeight = placement.imageHeight,
    ),
)

private fun decodePlacement(json: Json, raw: String?): ViewportPlacement? {
    val wire = raw?.let { runCatching { json.decodeFromString(PlacementWire.serializer(), it) }.getOrNull() }
        ?: return null
    return ViewportPlacement(
        zoom = wire.zoom,
        offsetX = wire.offsetX,
        offsetY = wire.offsetY,
        rotationDegrees = wire.rotationDegrees,
        frameWidth = wire.frameWidth,
        frameHeight = wire.frameHeight,
        imageWidth = wire.imageWidth,
        imageHeight = wire.imageHeight,
    )
}

private fun decodeCrop(json: Json, raw: String?): PixelCrop? =
    raw?.let { runCatching { json.decodeFromString(PixelCrop.serializer(), it) }.getOrNull() }

internal fun PrintJobEntity.toUi(json: Json) = PrintJob(
    id = id,
    photoId = photoId,
    source = source,
    crop = decodeCrop(json, cropJson),
    copies = copies,
    presetId = presetId,
    prompt = prompt,
    cropImageWidth = cropImageWidth,
    cropImageHeight = cropImageHeight,
    rotateQuarters = rotateQuarters,
    landscape = landscape,
    rotationDegrees = rotationDegrees,
    placement = decodePlacement(json, placementJson),
    origin = origin,
    watermark = watermark,
    state = state,
    phase = phase,
    createdAt = createdAt,
    startedAt = startedAt,
    finishedAt = finishedAt,
    printerJobId = printerJobId,
    jobState = jobState,
    error = error,
)
