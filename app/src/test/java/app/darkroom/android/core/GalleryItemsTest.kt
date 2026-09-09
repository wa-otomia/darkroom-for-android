package app.darkroom.android.core

import app.darkroom.android.data.progress.JobKind
import app.darkroom.android.data.progress.JobProgress
import app.darkroom.android.data.transfer.IncomingTransfer
import app.darkroom.android.data.transfer.TransferState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GalleryItemsTest {
    private val existing = PhotoMeta("p1", "a.jpg", "", "", 1, 1, 1)

    @Test
    fun pendingRowsSitAboveCatalogPhotos() {
        val transfer = transfer(id = "t1", photoId = "incoming", filename = "shot.heic")
        val generate = generateJob(id = "g1", target = "gen-1", source = "p1")
        val items = mergeGalleryItems(listOf(existing), listOf(transfer), listOf(generate))
        assertEquals(listOf("incoming", "gen-1", "p1"), items.map { it.photoId })
        assertEquals(PendingKind.Transfer, (items[0] as GalleryItem.Pending).kind)
        assertEquals(PendingKind.Generate, (items[1] as GalleryItem.Pending).kind)
        assertTrue(items[2] is GalleryItem.Photo)
    }

    @Test
    fun roomRowDropsMatchingTransferAndGeneratePending() {
        val arrived = PhotoMeta("incoming", "shot.jpg", "", "", 1, 1, 1)
        val transfer = transfer(id = "t1", photoId = "incoming", filename = "shot.heic", state = TransferState.Done)
        val generate = generateJob(id = "g1", target = "incoming", source = "p1")
        val items = mergeGalleryItems(listOf(arrived), listOf(transfer), listOf(generate))
        assertEquals(listOf("incoming"), items.map { it.photoId })
        val photo = items.single() as GalleryItem.Photo
        assertEquals("shot.jpg", photo.meta.filename)
        assertNull(photo.overlay)
    }

    @Test
    fun editJobAttachesAsOverlayOnSourcePhoto() {
        val edit = AiJobSnapshot(
            id = "e1",
            kind = "edit",
            sourcePhotoId = "p1",
            targetPhotoId = "p1",
            phase = "processing",
            loaded = 10,
            total = 40,
            hidden = false,
            error = null,
        )
        val items = mergeGalleryItems(listOf(existing), emptyList<IncomingTransfer>(), listOf(edit))
        assertEquals(1, items.size)
        val photo = items.single() as GalleryItem.Photo
        val overlay = photo.overlay!!
        assertEquals("e1", overlay.jobId)
        assertEquals("processing", overlay.phase)
        assertEquals(0.25f, overlay.progress)
        assertEquals(true, overlay.cancelable)
    }

    @Test
    fun generatePendingCarriesSourceThumbAndIsCancelable() {
        val generate = generateJob(id = "g1", target = "gen-1", source = "p1", loaded = 20, total = 80)
        val pending = mergeGalleryItems(
            emptyList<PhotoMeta>(),
            emptyList<IncomingTransfer>(),
            listOf(generate),
        ).single() as GalleryItem.Pending
        assertEquals("gen-1", pending.photoId)
        assertEquals("p1", pending.sourceThumb)
        assertEquals("p1", pending.sourcePhotoId)
        assertEquals("g1", pending.cancelId)
        assertEquals(0.25f, pending.progress)
        assertEquals(true, pending.cancelable)
        assertNull(pending.filename)
    }

    @Test
    fun transferPendingExposesFilenameAndIndeterminateProgress() {
        val transfer = transfer(id = "t1", photoId = "incoming", filename = "DSC.heic", bytes = 128, total = null)
        val pending = mergeGalleryItems(
            emptyList<PhotoMeta>(),
            listOf(transfer),
            emptyList<AiJobSnapshot>(),
        ).single() as GalleryItem.Pending
        assertEquals("receiving", pending.phase)
        assertEquals("DSC.heic", pending.filename)
        assertNull(pending.progress)
        assertEquals(false, pending.cancelable)
    }

    @Test
    fun printJobInProgressAttachesOverlayWithKindAndFraction() {
        val progress = JobProgress(
            jobId = "pj1",
            kind = JobKind.Print,
            phase = "sending",
            phaseElapsedMs = 400,
            jobElapsedMs = 1_200,
            fraction = 0.42f,
        )
        val print = PrintJobSnapshot(
            id = "pj1",
            photoId = "p1",
            state = "running",
            phase = "sending",
            jobProgress = progress,
            createdAt = 10L,
        )
        val items = mergeGalleryItems(
            listOf(existing),
            emptyList<IncomingTransfer>(),
            emptyList<AiJobSnapshot>(),
            listOf(print),
        )
        val overlay = (items.single() as GalleryItem.Photo).overlay!!
        assertEquals("pj1", overlay.jobId)
        assertEquals(OverlayKind.Print, overlay.kind)
        assertEquals("sending", overlay.phase)
        assertEquals(0.42f, overlay.progress)
        assertEquals(progress, overlay.jobProgress)
    }

    @Test
    fun queuedPrintShowsOverlay() {
        val print = PrintJobSnapshot(
            id = "q1",
            photoId = "p1",
            state = "queued",
            phase = null,
            createdAt = 1L,
        )
        val overlay = (
            mergeGalleryItems(
                listOf(existing),
                emptyList<IncomingTransfer>(),
                emptyList<AiJobSnapshot>(),
                listOf(print),
            ).single() as GalleryItem.Photo
            ).overlay!!
        assertEquals(OverlayKind.Print, overlay.kind)
        assertEquals("queued", overlay.phase)
        assertNull(overlay.progress)
    }

    @Test
    fun finishedPrintLeavesNoOverlay() {
        val finished = JobProgress(
            jobId = "pj1",
            kind = JobKind.Print,
            phase = "done",
            phaseElapsedMs = 100,
            jobElapsedMs = 8_000,
            finished = true,
            fraction = 1f,
        )
        for (state in listOf("done", "failed", "cancelled")) {
            val print = PrintJobSnapshot(
                id = "pj1",
                photoId = "p1",
                state = state,
                phase = if (state == "done") "done" else "error",
                jobProgress = finished,
                createdAt = 10L,
            )
            val items = mergeGalleryItems(
                listOf(existing),
                emptyList<IncomingTransfer>(),
                emptyList<AiJobSnapshot>(),
                listOf(print),
            )
            assertNull((items.single() as GalleryItem.Photo).overlay)
        }
    }

    @Test
    fun runningPrintIsPreferredWhenSeveralJobsTargetTheSamePhoto() {
        val queued = PrintJobSnapshot(
            id = "q1",
            photoId = "p1",
            state = "queued",
            phase = null,
            createdAt = 1L,
        )
        val running = PrintJobSnapshot(
            id = "r1",
            photoId = "p1",
            state = "running",
            phase = "printing",
            jobProgress = JobProgress(
                jobId = "r1",
                kind = JobKind.Print,
                phase = "printing",
                phaseElapsedMs = 800,
                jobElapsedMs = 4_000,
                fraction = 0.77f,
            ),
            createdAt = 2L,
        )
        val items = mergeGalleryItems(
            listOf(existing),
            emptyList<IncomingTransfer>(),
            emptyList<AiJobSnapshot>(),
            listOf(queued, running),
        )
        val overlay = (items.single() as GalleryItem.Photo).overlay!!
        assertEquals("r1", overlay.jobId)
        assertEquals(OverlayKind.Print, overlay.kind)
        assertEquals(0.77f, overlay.progress)
    }

    @Test
    fun printDonePhaseHidesOverlayWhileRowIsStillRunning() {
        val print = PrintJobSnapshot(
            id = "pj1",
            photoId = "p1",
            state = "running",
            phase = "done",
            jobProgress = JobProgress(
                jobId = "pj1",
                kind = JobKind.Print,
                phase = "done",
                phaseElapsedMs = 10,
                jobElapsedMs = 8_000,
                finished = true,
                fraction = 1f,
            ),
            createdAt = 10L,
        )
        val items = mergeGalleryItems(
            listOf(existing),
            emptyList<IncomingTransfer>(),
            emptyList<AiJobSnapshot>(),
            listOf(print),
        )
        assertNull((items.single() as GalleryItem.Photo).overlay)
    }

    @Test
    fun finishedPrintDoesNotHideEditOverlay() {
        val edit = AiJobSnapshot(
            id = "e1",
            kind = "edit",
            sourcePhotoId = "p1",
            targetPhotoId = "p1",
            phase = "processing",
            loaded = 10,
            total = 40,
            hidden = false,
            error = null,
        )
        val done = PrintJobSnapshot(
            id = "pj1",
            photoId = "p1",
            state = "done",
            phase = "done",
            createdAt = 10L,
        )
        val items = mergeGalleryItems(
            listOf(existing),
            emptyList<IncomingTransfer>(),
            listOf(edit),
            listOf(done),
        )
        val overlay = (items.single() as GalleryItem.Photo).overlay!!
        assertEquals(OverlayKind.Edit, overlay.kind)
        assertEquals("e1", overlay.jobId)
        assertEquals(0.25f, overlay.progress)
    }

    private fun transfer(
        id: String,
        photoId: String,
        filename: String,
        bytes: Long = 0,
        total: Long? = 100,
        state: TransferState = TransferState.Receiving,
    ) = IncomingTransfer(
        id = id,
        photoId = photoId,
        filename = filename,
        bytes = bytes,
        total = total,
        state = state,
        startedAt = 1L,
        error = null,
    )

    private fun generateJob(
        id: String,
        target: String,
        source: String,
        loaded: Long? = null,
        total: Long? = null,
    ) = AiJobSnapshot(
        id = id,
        kind = "generate",
        sourcePhotoId = source,
        targetPhotoId = target,
        phase = "uploading",
        loaded = loaded,
        total = total,
        hidden = false,
        error = null,
    )
}
