package io.github.wa_otomia.darkroom.data.catalog

import android.content.Context
import io.github.wa_otomia.darkroom.core.EditRecord
import io.github.wa_otomia.darkroom.core.Framing
import io.github.wa_otomia.darkroom.core.GENERATED_PHOTO_TAG
import io.github.wa_otomia.darkroom.core.ID_MASK_MSG_MISSING
import io.github.wa_otomia.darkroom.core.IdBackground
import io.github.wa_otomia.darkroom.core.IdFeather
import io.github.wa_otomia.darkroom.core.IdPhotoPose
import io.github.wa_otomia.darkroom.core.IdPhotoState
import io.github.wa_otomia.darkroom.core.PhotoMeta
import io.github.wa_otomia.darkroom.core.PrintFit
import io.github.wa_otomia.darkroom.core.PrintRecord
import io.github.wa_otomia.darkroom.core.ORIGINAL_VERSION_ID
import io.github.wa_otomia.darkroom.core.framingFor
import io.github.wa_otomia.darkroom.core.framingVersionKey
import io.github.wa_otomia.darkroom.core.idBackground
import io.github.wa_otomia.darkroom.core.idFeather
import io.github.wa_otomia.darkroom.core.idMaskRequired
import io.github.wa_otomia.darkroom.core.idPhotoFor
import io.github.wa_otomia.darkroom.core.normalizeIdPhotoPresetId
import io.github.wa_otomia.darkroom.core.withIdPhoto
import io.github.wa_otomia.darkroom.core.galleryThumbSource
import io.github.wa_otomia.darkroom.core.toPlacement
import io.github.wa_otomia.darkroom.core.withFraming
import io.github.wa_otomia.darkroom.core.generatedPhotoFilename
import io.github.wa_otomia.darkroom.core.hasJpegEoi
import io.github.wa_otomia.darkroom.core.isJpegBytes
import io.github.wa_otomia.darkroom.core.looksLikeJpeg
import io.github.wa_otomia.darkroom.core.listRelatedPhotos
import io.github.wa_otomia.darkroom.core.resolvePhotoSource
import io.github.wa_otomia.darkroom.data.imaging.ImagePipeline
import io.github.wa_otomia.darkroom.data.settings.ActivityLog
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

    /**
     * Serialises every read-modify-replace of an existing photo row: Studio's framing and ID pose
     * saves, the background / feather taps, the mask job's commit, new edits, print records,
     * version deletion and photo deletion. Never held across a network call.
     */
    private val rowLock = Mutex()

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
        if (hasFramedThumb(photo, source) && framed.exists()) return framed
        return thumbFile(photo.id, source)
    }

    /** The display version gets a framed thumb when it has a saved framing or its ID mode is on. */
    private fun hasFramedThumb(photo: PhotoMeta, source: String): Boolean =
        photo.framingFor(source) != null || photo.idPhotoFor(source)?.active == true

    /** `library/<photoId>/idphoto/<versionKey>-mask-<rev>.png`. */
    fun idPhotoMaskFile(photoId: String, source: String, rev: String): File =
        File(idPhotoDir(photoId), "${idMaskPrefix(framingVersionKey(source))}$rev.png")

    private fun idPhotoDir(photoId: String) = File(library, "$photoId/idphoto")

    private fun idMaskPrefix(versionKey: String) = "$versionKey-mask-"

    /** The mask bytes for [maskRev], or null when there is no rev or the file is missing or empty. */
    fun readIdPhotoMask(photoId: String, source: String, maskRev: String?): ByteArray? {
        val rev = maskRev?.trim()?.takeIf { it.isNotEmpty() && '/' !in it && '\\' !in it } ?: return null
        val file = idPhotoMaskFile(photoId, source, rev)
        if (!file.isFile) return null
        return runCatching { file.readBytes() }.getOrNull()?.takeIf { it.isNotEmpty() }
    }

    /** Edit/original thumb, or the full file when the thumb has not been written yet. */
    fun versionThumbFile(id: String, source: String): File {
        val thumb = thumbFile(id, source)
        return if (thumb.exists()) thumb else sourceFile(id, source)
    }

    suspend fun get(id: String): PhotoMeta? = db.photoDao().get(id)?.toMeta()

    /**
     * The row plus the bytes of [source]'s stored mask revision (null when it has none or the
     * file is missing), read together under [rowLock]. A re-cut deletes the superseded file
     * right after its commit, so a job that freezes the ID state must read the mask in the same
     * step (PRRD020-R04: a later re-cut does not affect a job that already started).
     */
    suspend fun getWithIdMask(photoId: String, source: String): Pair<PhotoMeta?, ByteArray?> =
        withContext(Dispatchers.IO) {
            rowLock.withLock {
                val photo = get(photoId)
                val rev = photo?.idPhotoFor(source)?.maskRev
                photo to rev?.let { runCatching { readIdPhotoMask(photoId, source, it) }.getOrNull() }
            }
        }

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
        get(photoId) ?: error("照片不存在")
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
        // The files are written outside the lock; the row is re-read under it so a mask commit
        // or ID / framing write that landed meanwhile is merged, not overwritten.
        rowLock.withLock {
            val current = get(photoId)
            if (current == null) {
                editFile(photoId, id).delete()
                thumbFile(photoId, id).delete()
                error("照片不存在")
            }
            val next = current.copy(edits = current.edits + record)
            refreshGalleryFramedThumb(next)
            db.photoDao().upsert(next.toEntity())
        }
        record
    }

    suspend fun deleteEdit(photoId: String, editId: String) = withContext(Dispatchers.IO) {
        rowLock.withLock {
            val resolved = resolvePhotoSource(editId)
            if (resolved == "original") return@withLock
            val current = get(photoId) ?: return@withLock
            if (current.edits.none { it.id == resolved }) return@withLock
            val next = current.copy(
                edits = current.edits.filter { it.id != resolved },
                framings = current.framings - resolved,
                idPhotos = current.idPhotos - resolved,
            )
            refreshGalleryFramedThumb(next)
            db.photoDao().upsert(next.toEntity())
            editFile(photoId, resolved).delete()
            thumbFile(photoId, resolved).delete()
            deleteIdMasks(photoId, resolved)
            activityLog.record("catalog", "delete edit $resolved of $photoId")
        }
    }

    fun persistFraming(photoId: String, source: String, framing: Framing?) {
        scope.launch { saveFraming(photoId, source, framing) }
    }

    suspend fun saveFraming(photoId: String, source: String, framing: Framing?) = withContext(Dispatchers.IO) {
        rowLock.withLock {
            val current = get(photoId) ?: return@withLock
            val next = current.withFraming(source, framing)
            val display = galleryThumbSource(next) { thumbFile(next.id, it).exists() }
            val framedMissing = hasFramedThumb(next, display) && !framedThumbFile(next.id).exists()
            if (next.framings == current.framings && !framedMissing) return@withLock
            refreshGalleryFramedThumb(next)
            if (next.framings != current.framings) {
                db.photoDao().upsert(next.toEntity())
            }
        }
    }

    /**
     * Studio's ID mode toggle, size and pose for one version (EDID010-R03). Merges only these
     * fields: background, feather and the mask revision are left exactly as stored. The state is
     * kept even when [active] is false, so leaving and re-entering ID mode restores it. An
     * inactive save for a version that never entered ID mode writes nothing, so its first entry
     * still starts from the initial pose (EDID010-R04).
     */
    suspend fun saveIdPhotoPose(photoId: String, source: String, active: Boolean, presetId: String, pose: IdPhotoPose) {
        updateIdPhoto(photoId, source) { old ->
            if (old == null && !active) return@updateIdPhoto null
            (old ?: IdPhotoState()).copy(
                active = active,
                presetId = normalizeIdPhotoPresetId(presetId),
            ).withPose(pose)
        }
    }

    /** Fire-and-forget [saveIdPhotoPose] on the repository scope, so it outlives the caller. */
    fun persistIdPhotoPose(photoId: String, source: String, active: Boolean, presetId: String, pose: IdPhotoPose) {
        scope.launch { saveIdPhotoPose(photoId, source, active, presetId, pose) }
    }

    suspend fun setIdPhotoBackground(photoId: String, source: String, background: String) {
        val id = idBackground(background).id
        updateIdPhoto(photoId, source) { old -> (old ?: IdPhotoState()).copy(background = id) }
    }

    suspend fun setIdPhotoFeather(photoId: String, source: String, feather: String) {
        val id = idFeather(feather).id
        updateIdPhoto(photoId, source) { old -> (old ?: IdPhotoState()).copy(feather = id) }
    }

    /**
     * Stores a validated mask for [source] as a new revision and points the version's ID state
     * at it. The first mask of a version also selects navy and weak feathering (EDID040-R04/R07);
     * a later one keeps the user's choices.
     *
     * Returns false, writing nothing, when the photo or the version no longer exists. The file is
     * written to a temp name and renamed before the row changes; the previous revision's file is
     * deleted only after the row points at the new one.
     */
    suspend fun saveIdPhotoMask(photoId: String, source: String, png: ByteArray): Boolean = withContext(Dispatchers.IO) {
        rowLock.withLock {
            val current = get(photoId) ?: return@withLock false
            val key = framingVersionKey(source)
            if (key != ORIGINAL_VERSION_ID && current.edits.none { it.id == key }) return@withLock false
            val rev = UUID.randomUUID().toString().replace("-", "").take(MASK_REV_LENGTH)
            val file = idPhotoMaskFile(photoId, key, rev)
            writeAtomically(file, png)
            val old = current.idPhotoFor(key)
            val firstMask = old?.hasMask != true
            val base = old ?: IdPhotoState()
            val state = base.copy(
                maskRev = rev,
                background = if (firstMask) IdBackground.NAVY.id else base.background,
                feather = if (firstMask) IdFeather.WEAK.id else base.feather,
            ).normalized()
            val next = current.withIdPhoto(key, state)
            try {
                if (state.active && key == displaySource(next)) refreshGalleryFramedThumb(next)
                db.photoDao().upsert(next.toEntity())
            } catch (e: Throwable) {
                file.delete()
                throw e
            }
            old?.maskRev?.takeIf { it != rev }?.let { idPhotoMaskFile(photoId, key, it).delete() }
            activityLog.record("catalog", "save id photo mask $key of $photoId")
            true
        }
    }

    /**
     * Read-modify-replace of one version's ID state under [rowLock]; [transform] returning null
     * keeps the stored state. Upserts only on a change.
     * The gallery's framed thumb is re-rendered first (so the row emission sees the new file)
     * when the change can show on it: the version is the display version and ID mode is or was
     * on, or the thumb it needs is missing.
     */
    private suspend fun updateIdPhoto(
        photoId: String,
        source: String,
        transform: (IdPhotoState?) -> IdPhotoState?,
    ) = withContext(Dispatchers.IO) {
        rowLock.withLock {
            val current = get(photoId) ?: return@withLock
            val key = framingVersionKey(source)
            val old = current.idPhotoFor(key)
            val state = transform(old)?.normalized() ?: old
            val next = current.withIdPhoto(key, state)
            val display = displaySource(next)
            val changed = state != old
            val framedMissing = hasFramedThumb(next, display) && !framedThumbFile(next.id).exists()
            if (!changed && !framedMissing) return@withLock
            val showsOnThumb = key == display && (old?.active == true || state?.active == true)
            if ((changed && showsOnThumb) || framedMissing) refreshGalleryFramedThumb(next)
            if (changed) db.photoDao().upsert(next.toEntity())
        }
    }

    private fun displaySource(photo: PhotoMeta): String =
        galleryThumbSource(photo) { thumbFile(photo.id, it).exists() }

    /** Temp file plus rename, so a crash never leaves a half-written file under the final name. */
    private fun writeAtomically(file: File, bytes: ByteArray) {
        val dir = checkNotNull(file.parentFile)
        dir.mkdirs()
        val temp = File(dir, "${file.name}.tmp")
        try {
            temp.writeBytes(bytes)
            if (!temp.renameTo(file)) error("无法保存证件照蒙版")
        } catch (e: Throwable) {
            temp.delete()
            throw e
        }
    }

    /** Every mask revision of [versionKey], current or left behind. */
    private fun deleteIdMasks(photoId: String, versionKey: String) {
        val prefix = idMaskPrefix(versionKey)
        idPhotoDir(photoId).listFiles()?.forEach { file ->
            if (file.isFile && file.name.startsWith(prefix)) file.delete()
        }
    }

    private fun refreshGalleryFramedThumb(photo: PhotoMeta) {
        val source = galleryThumbSource(photo) { thumbFile(photo.id, it).exists() }
        val dest = framedThumbFile(photo.id)
        val idState = photo.idPhotoFor(source)?.takeIf { it.active }
        val framing = photo.framingFor(source)
        if (idState == null && framing == null) {
            dest.delete()
            return
        }
        val file = sourceFile(photo.id, source)
        if (!file.exists()) {
            dest.delete()
            return
        }
        runCatching {
            val bytes = file.readBytes()
            val jpeg = if (idState != null) {
                renderIdThumbSheet(photo.id, source, idState, bytes)
            } else {
                ImagePipeline.renderPrintJpeg(
                    bytes,
                    crop = null,
                    fit = PrintFit.COVER,
                    landscape = framing!!.landscape,
                    rotationDegrees = framing.rotationDegrees,
                    placement = framing.toPlacement(),
                    watermark = null,
                    sheetForPrinter = false,
                )
            }
            ImagePipeline.writeFile(dest, ImagePipeline.thumbnailJpeg(jpeg))
        }
    }

    /**
     * The ID sheet on the portrait printer sheet, so the 2:3 gallery card shows all of it. A
     * missing or broken mask shows the layout with the original background rather than a stale
     * thumb; printing and export still refuse it.
     */
    private fun renderIdThumbSheet(photoId: String, source: String, state: IdPhotoState, bytes: ByteArray): ByteArray {
        val mask = if (idMaskRequired(state)) readIdPhotoMask(photoId, source, state.maskRev) else null
        return try {
            ImagePipeline.renderIdPhotoSheetJpeg(bytes, state, mask, outputScale = 1, sheetForPrinter = true)
        } catch (e: IllegalStateException) {
            if (e.message != ID_MASK_MSG_MISSING) throw e
            ImagePipeline.renderIdPhotoSheetJpeg(
                bytes,
                state.copy(background = IdBackground.ORIGINAL.id),
                null,
                outputScale = 1,
                sheetForPrinter = true,
            )
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
            val rootId = io.github.wa_otomia.darkroom.core.resolveRootId(family, fromPhotoId)
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
        withContext(Dispatchers.IO) {
            rowLock.withLock {
                val current = get(photoId) ?: return@withLock
                db.photoDao().upsert(current.copy(prints = current.prints + record).toEntity())
            }
        }
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
            // Under rowLock so a late per-version write (a mask commit in particular) cannot
            // recreate the folder or the row after the photo is gone.
            rowLock.withLock {
                // Files first: a crash in between leaves a row with no files, which shows up as a
                // failed thumbnail the user can delete again. The other order would leak files
                // invisibly; pruneOrphans() mops up whatever slips through.
                File(library, id).deleteRecursively()
                db.photoDao().delete(id)
            }
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
        private const val MASK_REV_LENGTH = 12
    }
}

/** Inbox files whose last-modified is at least [maxAgeMs] before [nowMs]. */
fun selectStaleInboxFiles(files: Array<File>?, nowMs: Long, maxAgeMs: Long): List<File> {
    if (files == null || files.isEmpty()) return emptyList()
    val cutoff = nowMs - maxAgeMs
    return files.filter { it.isFile && it.lastModified() <= cutoff }
}
