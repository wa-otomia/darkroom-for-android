package app.darkroom.android.data.catalog

import android.content.Context
import app.darkroom.android.core.EditRecord
import app.darkroom.android.core.Framing
import app.darkroom.android.core.GENERATED_PHOTO_TAG
import app.darkroom.android.core.PhotoMeta
import app.darkroom.android.core.PrintFit
import app.darkroom.android.core.PrintRecord
import app.darkroom.android.core.framingFor
import app.darkroom.android.core.galleryThumbSource
import app.darkroom.android.core.toPlacement
import app.darkroom.android.core.withFraming
import app.darkroom.android.core.generatedPhotoFilename
import app.darkroom.android.core.hasJpegEoi
import app.darkroom.android.core.isJpegBytes
import app.darkroom.android.core.looksLikeJpeg
import app.darkroom.android.core.listRelatedPhotos
import app.darkroom.android.core.resolvePhotoSource
import app.darkroom.android.data.imaging.ImagePipeline
import app.darkroom.android.data.settings.ActivityLog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** A deletion waiting out its undo window; [ids] carries enough to render the prompt. */
data class PendingDeletion(
    val token: String,
    val ids: List<String>,
    val startedAtMs: Long,
    val graceMillis: Long,
)

data class DeleteProgress(val done: Int, val total: Int)

@Singleton
class CatalogRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: PhotoDatabase,
    private val activityLog: ActivityLog,
) {
    private val library: File get() = File(context.filesDir, "library")
    private val inbox: File get() = File(context.filesDir, "inbox")

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Ids hidden from [photos] because a deletion is waiting for its undo window to close. */
    private val hiddenIds = MutableStateFlow<Set<String>>(emptySet())
    private val pendingIds = mutableMapOf<String, List<String>>()
    private val pendingJobs = mutableMapOf<String, Job>()

    private val _pendingDeletion = MutableStateFlow<PendingDeletion?>(null)

    /**
     * The deletion currently offering an undo, or null.
     *
     * Lives here rather than in the gallery because a deletion started from the photo detail
     * screen pops straight back to the gallery: the screen that has to offer the undo is never
     * the screen that asked for the delete. Whoever renders the prompt reads this and answers
     * with [undoDelete] or [commitDeleteNow].
     *
     * At most one deletion is advertised at a time. [scheduleDelete] commits the previous one,
     * since its prompt is about to be replaced on screen anyway.
     */
    val pendingDeletion: StateFlow<PendingDeletion?> = _pendingDeletion.asStateFlow()

    private val _deleteProgress = MutableStateFlow<DeleteProgress?>(null)
    val deleteProgress: StateFlow<DeleteProgress?> = _deleteProgress.asStateFlow()

    val photos: Flow<List<PhotoMeta>> = combine(db.photoDao().observeAll(), hiddenIds) { rows, hidden ->
        val all = rows.map { it.toMeta() }
        if (hidden.isEmpty()) all else all.filter { it.id !in hidden }
    }

    fun inboxDir(): File = inbox.apply { mkdirs() }

    fun originalFile(id: String) = File(library, "$id/original.jpg")
    fun thumbFile(id: String, source: String = "original"): File {
        val resolved = resolvePhotoSource(source)
        return if (resolved == "original") {
            File(library, "$id/thumb.jpg")
        } else {
            File(library, "$id/edits/$resolved-thumb.jpg")
        }
    }
    fun editFile(id: String, editId: String) = File(library, "$id/edits/$editId.jpg")

    fun sourceFile(id: String, source: String): File {
        val resolved = resolvePhotoSource(source)
        return if (resolved == "original") originalFile(id) else editFile(id, resolved)
    }

    fun framedThumbFile(id: String) = File(library, "$id/framed-thumb.jpg")

    fun galleryThumbFile(photo: PhotoMeta): File {
        val source = galleryThumbSource(photo) { thumbFile(photo.id, it).exists() }
        val framed = framedThumbFile(photo.id)
        if (photo.framingFor(source) != null && framed.exists()) return framed
        return thumbFile(photo.id, source)
    }

    /** Edit/original thumb, or the full file when the thumb has not been written yet. */
    fun versionThumbFile(id: String, source: String): File {
        val thumb = thumbFile(id, source)
        return if (thumb.exists()) thumb else sourceFile(id, source)
    }

    suspend fun get(id: String): PhotoMeta? = db.photoDao().get(id)?.toMeta()

    suspend fun list(): List<PhotoMeta> = db.photoDao().list().map { it.toMeta() }

    suspend fun related(id: String): List<PhotoMeta> = listRelatedPhotos(list(), id)

    suspend fun ingestBytes(
        bytes: ByteArray,
        originalName: String,
        kind: String = "import",
        id: String = UUID.randomUUID().toString(),
        onPhase: ((String) -> Unit)? = null,
        tags: List<String> = emptyList(),
    ): PhotoMeta = withContext(Dispatchers.IO) {
        val stored = if (isJpegBytes(bytes) || looksLikeJpeg(bytes)) {
            if (!hasJpegEoi(bytes)) error("文件不完整")
            bytes
        } else {
            onPhase?.invoke("transcoding")
            try {
                ImagePipeline.decodeToJpeg(bytes)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                error("无法解码图片")
            }
        }
        onPhase?.invoke("ingesting")
        library.mkdirs()
        val dir = File(library, id)
        File(dir, "edits").mkdirs()
        val info = ImagePipeline.probe(stored)
        originalFile(id).writeBytes(stored)
        ImagePipeline.writeFile(thumbFile(id), ImagePipeline.thumbnailJpeg(stored))
        val now = Instant.now().toString()
        val meta = PhotoMeta(
            id = id,
            filename = storedJpegFilename(originalName),
            createdAt = now,
            ingestedAt = now,
            width = info.width,
            height = info.height,
            bytes = stored.size.toLong(),
            tags = tags,
        )
        db.photoDao().upsert(meta.toEntity())
        activityLog.record(kind, "ingest ${meta.filename} → $id")
        meta
    }

    suspend fun ingestFile(
        file: File,
        kind: String = "ftp",
        id: String = UUID.randomUUID().toString(),
        onPhase: ((String) -> Unit)? = null,
    ): PhotoMeta {
        try {
            return ingestBytes(file.readBytes(), file.name, kind, id, onPhase)
        } finally {
            file.delete()
        }
    }

    suspend fun saveEdit(
        photoId: String,
        prompt: String,
        jpeg: ByteArray,
        presetId: String = "",
        presetTitle: String = "",
    ): EditRecord = withContext(Dispatchers.IO) {
        val current = get(photoId) ?: error("照片不存在")
        val stored = storedAiJpeg(jpeg, "修图结果不是 JPEG")
        val id = UUID.randomUUID().toString()
        ImagePipeline.writeFile(editFile(photoId, id), stored)
        ImagePipeline.writeFile(thumbFile(photoId, id), ImagePipeline.thumbnailJpeg(stored))
        val info = ImagePipeline.probe(stored)
        val record = EditRecord(
            id = id,
            prompt = prompt,
            createdAt = Instant.now().toString(),
            filename = "$id.jpg",
            width = info.width,
            height = info.height,
            presetId = presetId,
            presetTitle = presetTitle,
        )
        val next = current.copy(edits = current.edits + record)
        refreshGalleryFramedThumb(next)
        db.photoDao().upsert(next.toEntity())
        record
    }

    suspend fun deleteEdit(photoId: String, editId: String) = withContext(Dispatchers.IO) {
        val resolved = resolvePhotoSource(editId)
        if (resolved == "original") return@withContext
        val current = get(photoId) ?: return@withContext
        if (current.edits.none { it.id == resolved }) return@withContext
        val next = current.copy(
            edits = current.edits.filter { it.id != resolved },
            framings = current.framings - resolved,
        )
        refreshGalleryFramedThumb(next)
        db.photoDao().upsert(next.toEntity())
        editFile(photoId, resolved).delete()
        thumbFile(photoId, resolved).delete()
        activityLog.record("catalog", "delete edit $resolved of $photoId")
    }

    fun persistFraming(photoId: String, source: String, framing: Framing?) {
        scope.launch { saveFraming(photoId, source, framing) }
    }

    suspend fun saveFraming(photoId: String, source: String, framing: Framing?) = withContext(Dispatchers.IO) {
        val current = get(photoId) ?: return@withContext
        val next = current.withFraming(source, framing)
        val display = galleryThumbSource(next) { thumbFile(next.id, it).exists() }
        val framedMissing = next.framingFor(display) != null && !framedThumbFile(next.id).exists()
        if (next.framings == current.framings && !framedMissing) return@withContext
        refreshGalleryFramedThumb(next)
        if (next.framings != current.framings) {
            db.photoDao().upsert(next.toEntity())
        }
    }

    private fun refreshGalleryFramedThumb(photo: PhotoMeta) {
        val source = galleryThumbSource(photo) { thumbFile(photo.id, it).exists() }
        val dest = framedThumbFile(photo.id)
        val framing = photo.framingFor(source)
        if (framing == null) {
            dest.delete()
            return
        }
        val file = sourceFile(photo.id, source)
        if (!file.exists()) {
            dest.delete()
            return
        }
        runCatching {
            val jpeg = ImagePipeline.renderPrintJpeg(
                file.readBytes(),
                crop = null,
                fit = PrintFit.COVER,
                landscape = framing.landscape,
                rotationDegrees = framing.rotationDegrees,
                placement = framing.toPlacement(),
                watermark = null,
                sheetForPrinter = false,
            )
            ImagePipeline.writeFile(dest, ImagePipeline.thumbnailJpeg(jpeg))
        }
    }

    suspend fun saveGenerated(
        fromPhotoId: String,
        jpeg: ByteArray,
        prompt: String,
        id: String = UUID.randomUUID().toString(),
    ): PhotoMeta =
        withContext(Dispatchers.IO) {
            val stored = storedAiJpeg(jpeg, "生成结果不是 JPEG")
            val from = get(fromPhotoId) ?: error("照片不存在")
            val family = list()
            val rootId = app.darkroom.android.core.resolveRootId(family, fromPhotoId)
            File(library, "$id/edits").mkdirs()
            val info = ImagePipeline.probe(stored)
            originalFile(id).writeBytes(stored)
            ImagePipeline.writeFile(thumbFile(id), ImagePipeline.thumbnailJpeg(stored))
            val now = Instant.now().toString()
            val meta = PhotoMeta(
                id = id,
                filename = generatedPhotoFilename(from.filename),
                createdAt = now,
                ingestedAt = now,
                width = info.width,
                height = info.height,
                bytes = stored.size.toLong(),
                parentId = rootId,
                generatePrompt = prompt,
                tags = listOf(GENERATED_PHOTO_TAG),
            )
            db.photoDao().upsert(meta.toEntity())
            activityLog.record("ai", "generate $id from $fromPhotoId")
            meta
        }

    /** Archive name: original stem plus `.jpg` after HEIC/PNG/WebP transcode. */
    private fun storedJpegFilename(originalName: String): String {
        val name = File(originalName).name
        val stem = name.substringBeforeLast('.').trim().ifEmpty { "photo" }
        return "$stem.jpg"
    }

    /**
     * Grok stills are transcoded here so every generate / in-place edit /
     * auto-print preset lands as real JPEG bytes under a `.jpg` name.
     */
    private fun storedAiJpeg(bytes: ByteArray, notJpeg: String): ByteArray = try {
        ImagePipeline.ensureStoredJpeg(bytes)
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        error(notJpeg)
    }

    suspend fun recordPrint(photoId: String, record: PrintRecord) {
        val current = get(photoId) ?: return
        db.photoDao().upsert(current.copy(prints = current.prints + record).toEntity())
    }

    /** Deletes immediately, with no undo. Prefer [scheduleDelete] anywhere a user can see it. */
    suspend fun delete(id: String) {
        deleteNow(id)
    }

    /**
     * Hides [ids] from [photos] straight away and deletes them for real once the undo window
     * closes. Returns a token for [undoDelete] / [commitDeleteNow], or null when [ids] is empty.
     *
     * The timer runs on a repository-owned scope, so navigating away from the caller still commits
     * the deletion. If the process dies while a deletion is pending, the photos simply reappear:
     * nothing is written to disk, so there are no orphan files and no rows pointing at missing files.
     */
    fun scheduleDelete(ids: Collection<String>, graceMillis: Long = UNDO_GRACE_MS): String? {
        val target = ids.filter { it.isNotBlank() }.distinct()
        if (target.isEmpty()) return null
        // Let the previous batch through rather than leaving it on its timer with no undo
        // affordance: its prompt is about to be replaced on screen. It commits by token, so it
        // still deletes its own ids and cannot be stranded by the swap below.
        _pendingDeletion.value?.token?.let { commitDeleteNow(it) }
        val token = UUID.randomUUID().toString()
        val job = scope.launch(start = CoroutineStart.LAZY) {
            delay(graceMillis)
            withContext(NonCancellable) { runDelete(token, cancelTimer = false) }
        }
        register(token, target, job, graceMillis)
        job.start()
        return token
    }

    /** Brings a pending deletion back. Returns false when it already went through. */
    fun undoDelete(token: String): Boolean {
        val ids = takePending(token, cancelTimer = true) ?: return false
        unhide(ids)
        activityLog.record("catalog", "undo delete ${ids.size}")
        return true
    }

    /** Commits a pending deletion without waiting for the timer. Safe to call twice. */
    fun commitDeleteNow(token: String) {
        scope.launch { withContext(NonCancellable) { runDelete(token, cancelTimer = true) } }
    }

    /**
     * Removes library folders that no catalog row points at, which is how a crash between the
     * file delete and the row delete gets cleaned up. Folders touched within
     * [ORPHAN_MIN_AGE_MS] are left alone so an ingest in flight is never destroyed.
     */
    suspend fun pruneOrphans(): Int = withContext(Dispatchers.IO) {
        val dirs = library.listFiles()?.filter { it.isDirectory }.orEmpty()
        if (dirs.isEmpty()) return@withContext 0
        val known = db.photoDao().list().mapTo(HashSet()) { it.id }
        val cutoff = System.currentTimeMillis() - ORPHAN_MIN_AGE_MS
        var removed = 0
        for (dir in dirs) {
            if (dir.name in known) continue
            if (dir.lastModified() > cutoff) continue
            if (dir.deleteRecursively()) removed++
        }
        if (removed > 0) activityLog.record("catalog", "prune $removed orphan folders")
        removed
    }

    /**
     * Drops leftover inbox uploads that never ingested (decode failure, process
     * death). Files newer than [INBOX_MAX_AGE_MS] are left for an in-flight STOR.
     */
    suspend fun pruneStaleInbox(): Int = withContext(Dispatchers.IO) {
        inbox.mkdirs()
        val stale = selectStaleInboxFiles(inbox.listFiles(), System.currentTimeMillis(), INBOX_MAX_AGE_MS)
        var removed = 0
        for (file in stale) {
            if (file.delete()) removed++
        }
        if (removed > 0) activityLog.record("catalog", "prune $removed stale inbox files")
        removed
    }

    private suspend fun runDelete(token: String, cancelTimer: Boolean) {
        val ids = takePending(token, cancelTimer) ?: return
        val show = ids.size >= DELETE_PROGRESS_MIN
        try {
            ids.forEachIndexed { index, id ->
                runCatching { deleteNow(id) }
                if (show) _deleteProgress.value = DeleteProgress(index + 1, ids.size)
            }
        } finally {
            if (show) _deleteProgress.value = null
            unhide(ids)
        }
    }

    private suspend fun deleteNow(id: String) {
        withContext(Dispatchers.IO) {
            // Files first: a crash in between leaves a row with no files, which shows up as a
            // failed thumbnail the user can delete again. The other order would leak files
            // invisibly; pruneOrphans() mops up whatever slips through.
            File(library, id).deleteRecursively()
            db.photoDao().delete(id)
            activityLog.record("catalog", "delete $id")
        }
    }

    @Synchronized
    private fun register(token: String, ids: List<String>, job: Job, graceMillis: Long) {
        pendingIds[token] = ids
        pendingJobs[token] = job
        hiddenIds.value = hiddenIds.value + ids
        _pendingDeletion.value = PendingDeletion(
            token = token,
            ids = ids,
            startedAtMs = System.currentTimeMillis(),
            graceMillis = graceMillis,
        )
    }

    @Synchronized
    private fun takePending(token: String, cancelTimer: Boolean): List<String>? {
        val job = pendingJobs.remove(token)
        if (cancelTimer) job?.cancel()
        // Only retract the prompt if it is still this batch's; a newer batch may own it by now.
        if (_pendingDeletion.value?.token == token) _pendingDeletion.value = null
        return pendingIds.remove(token)
    }

    @Synchronized
    private fun unhide(ids: List<String>) {
        hiddenIds.value = hiddenIds.value - ids.toSet()
    }

    companion object {
        /** Safety net for when the caller is torn down before it can commit or undo. */
        const val UNDO_GRACE_MS = 15_000L
        const val INBOX_MAX_AGE_MS = 10 * 60 * 1000L
        private const val ORPHAN_MIN_AGE_MS = 10 * 60 * 1000L
        private const val DELETE_PROGRESS_MIN = 10
    }
}

/** Inbox files whose last-modified is at least [maxAgeMs] before [nowMs]. */
fun selectStaleInboxFiles(files: Array<File>?, nowMs: Long, maxAgeMs: Long): List<File> {
    if (files == null || files.isEmpty()) return emptyList()
    val cutoff = nowMs - maxAgeMs
    return files.filter { it.isFile && it.lastModified() <= cutoff }
}
