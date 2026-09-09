package app.darkroom.android.core

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
