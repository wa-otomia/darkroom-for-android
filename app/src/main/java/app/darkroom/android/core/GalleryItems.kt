package app.darkroom.android.core

import app.darkroom.android.data.jobs.AiJob
import app.darkroom.android.data.progress.JobProgress
import app.darkroom.android.data.transfer.IncomingTransfer
import app.darkroom.android.data.transfer.TransferState

data class AiJobSnapshot(
    val id: String,
    val kind: String,
    val sourcePhotoId: String,
    val targetPhotoId: String,
    val phase: String,
    val loaded: Long?,
    val total: Long?,
    val hidden: Boolean,
    val error: String?,
    val jobProgress: JobProgress? = null,
)

enum class PendingKind { Transfer, Generate }

data class OverlayInfo(
    val jobId: String,
    val phase: String,
    val loaded: Long? = null,
    val total: Long? = null,
    val progress: Float? = null,
    val cancelable: Boolean = true,
    val error: String? = null,
    val jobProgress: JobProgress? = null,
)

typealias EditJobOverlay = OverlayInfo

sealed class GalleryItem {
    abstract val photoId: String

    data class Pending(
        override val photoId: String,
        val kind: PendingKind,
        val sourceThumb: String? = null,
        val sourcePhotoId: String? = sourceThumb,
        val progress: Float? = null,
        val phase: String,
        val cancelable: Boolean,
        val filename: String? = null,
        val error: String? = null,
        val loaded: Long? = null,
        val total: Long? = null,
        val dismissId: String? = null,
        val cancelId: String? = null,
        val jobProgress: JobProgress? = null,
    ) : GalleryItem()

    data class Photo(
        val meta: PhotoMeta,
        val overlay: OverlayInfo?,
    ) : GalleryItem() {
        override val photoId: String get() = meta.id
    }
}

@JvmName("mergeGalleryItemsFromAiJobs")
fun mergeGalleryItems(
    photos: List<PhotoMeta>,
    transfers: List<IncomingTransfer>,
    aiJobs: List<AiJob>,
): List<GalleryItem> = mergeGalleryItems(photos, transfers, aiJobs.map { it.toSnapshot() })

/**
 * Gallery rows: in-flight transfers and generate jobs first, then catalog photos.
 *
 * A Room row always wins the [GalleryItem.photoId] slot so a completed ingest
 * does not sit next to its own placeholder. Edit jobs never take a row; they
 * hang on the source photo as [OverlayInfo].
 */
fun mergeGalleryItems(
    photos: List<PhotoMeta>,
    transfers: List<IncomingTransfer>,
    aiJobs: List<AiJobSnapshot>,
): List<GalleryItem> {
    val occupied = photos.mapTo(HashSet()) { it.id }
    val pending = ArrayList<GalleryItem.Pending>()

    fun addPending(item: GalleryItem.Pending) {
        if (item.photoId in occupied) return
        occupied += item.photoId
        pending += item
    }

    for (transfer in transfers) {
        addPending(
            GalleryItem.Pending(
                photoId = transfer.photoId,
                kind = PendingKind.Transfer,
                sourceThumb = null,
                sourcePhotoId = null,
                progress = transfer.progress?.fraction ?: progressFraction(transfer.bytes, transfer.total),
                phase = transfer.progress?.phase ?: transferPhase(transfer.state),
                cancelable = false,
                filename = transfer.filename,
                error = transfer.error,
                loaded = transfer.bytes,
                total = transfer.total,
                dismissId = if (transfer.state == TransferState.Failed) transfer.id else null,
                cancelId = null,
                jobProgress = transfer.progress,
            ),
        )
    }

    for (job in aiJobs) {
        if (!job.kind.equals("generate", ignoreCase = true)) continue
        addPending(
            GalleryItem.Pending(
                photoId = job.targetPhotoId,
                kind = PendingKind.Generate,
                sourceThumb = job.sourcePhotoId,
                sourcePhotoId = job.sourcePhotoId,
                progress = job.jobProgress?.fraction ?: progressFraction(job.loaded, job.total),
                phase = job.phase,
                cancelable = true,
                filename = null,
                error = job.error,
                loaded = job.loaded,
                total = job.total,
                dismissId = null,
                cancelId = job.id,
                jobProgress = job.jobProgress,
            ),
        )
    }

    val overlayByPhoto = LinkedHashMap<String, OverlayInfo>()
    for (job in aiJobs) {
        if (!job.kind.equals("edit", ignoreCase = true)) continue
        overlayByPhoto[job.sourcePhotoId] = OverlayInfo(
            jobId = job.id,
            phase = job.phase,
            loaded = job.loaded,
            total = job.total,
            progress = job.jobProgress?.fraction ?: progressFraction(job.loaded, job.total),
            cancelable = true,
            error = job.error,
            jobProgress = job.jobProgress,
        )
    }

    val photoItems = photos.map { meta ->
        GalleryItem.Photo(meta = meta, overlay = overlayByPhoto[meta.id])
    }
    return pending + photoItems
}

private fun transferPhase(state: TransferState): String = when (state) {
    TransferState.Receiving -> "receiving"
    TransferState.Transcoding -> "transcoding"
    TransferState.Ingesting -> "ingesting"
    TransferState.Done -> "done"
    TransferState.Failed -> "failed"
}

private fun progressFraction(loaded: Long?, total: Long?): Float? {
    if (loaded == null || total == null || total <= 0L) return null
    return (loaded.toDouble() / total.toDouble()).toFloat().coerceIn(0f, 1f)
}
