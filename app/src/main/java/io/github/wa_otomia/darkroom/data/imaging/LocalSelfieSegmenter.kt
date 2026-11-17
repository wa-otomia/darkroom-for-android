package io.github.wa_otomia.darkroom.data.imaging

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.SegmentationMask
import com.google.mlkit.vision.segmentation.Segmenter
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import io.github.wa_otomia.darkroom.core.ID_MASK_MSG_LOCAL_FAILED
import io.github.wa_otomia.darkroom.core.idMaskConfidenceLuma
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.nio.ByteBuffer
import java.util.concurrent.TimeoutException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On-device person segmentation for ID photo cut-outs (EDID040-R11), wrapping the bundled
 * Google ML Kit selfie segmenter. No Play services, network or API key is involved; the photo,
 * the confidence map and the mask stay in memory and are never logged.
 *
 * One job prepares and segments at a time in the process; other callers wait on [lock] before
 * they decode anything, so a waiting job holds no input bitmap (design D4).
 */
@Singleton
class LocalSelfieSegmenter @Inject constructor() {
    private val lock = Mutex()

    /**
     * Under the process-wide lock: builds the input with [prepare] (a software ARGB_8888 bitmap,
     * already upright; its own errors pass through unchanged), calls [onSegmenting], then segments
     * it and returns the mask luma at the mask's size, which is the input's size: confidence c maps
     * through [idMaskConfidenceLuma]. The input is recycled before this returns.
     *
     * Any segmentation failure but a cancellation throws [IllegalStateException] with
     * "[ID_MASK_MSG_LOCAL_FAILED]：<class>", since callers only catch [Exception]. That includes
     * native-library ([LinkageError]) and [OutOfMemoryError] errors on the calling thread or on
     * ML Kit's worker (model load, inference), which runs on an [ErrorTrappingExecutor], and a
     * result that never arrives within [TIMEOUT_MS]. A cancel closes the segmenter and drops any
     * late result.
     */
    suspend fun segment(prepare: () -> Bitmap, onSegmenting: () -> Unit): ImagePipeline.IdMaskDecoded =
        lock.withLock {
            val input = prepare()
            try {
                onSegmenting()
                infer(input)
            } finally {
                if (!input.isRecycled) input.recycle()
            }
        }

    private suspend fun infer(input: Bitmap): ImagePipeline.IdMaskDecoded {
        val result = CompletableDeferred<SegmentationMask>()
        var segmenter: Segmenter? = null
        try {
            // An Error on ML Kit's worker thread is not seen by this coroutine's catch: the
            // executor traps it and fails [result] instead of letting it reach the default
            // uncaught-exception handler (design D4).
            val worker = ErrorTrappingExecutor(Dispatchers.IO.asExecutor()) { t -> result.completeExceptionally(t) }
            val client = Segmentation.getClient(
                SelfieSegmenterOptions.Builder()
                    .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
                    .setExecutor(worker)
                    .build(),
            )
            segmenter = client
            val listeners = Dispatchers.Default.asExecutor()
            client.process(InputImage.fromBitmap(input, 0))
                .addOnSuccessListener(listeners) { mask -> result.complete(mask) }
                .addOnFailureListener(listeners) { e -> result.completeExceptionally(e) }
                .addOnCanceledListener(listeners) {
                    result.completeExceptionally(IllegalStateException("segmentation cancelled"))
                }
            val mask = withTimeoutOrNull(TIMEOUT_MS) { result.await() }
                ?: throw TimeoutException("segmentation timed out")
            return readMask(mask)
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            throw IllegalStateException("$ID_MASK_MSG_LOCAL_FAILED：${t.javaClass.simpleName}")
        } finally {
            runCatching { segmenter?.close() }
        }
    }

    private fun readMask(mask: SegmentationMask): ImagePipeline.IdMaskDecoded {
        val width = mask.width
        val height = mask.height
        if (width <= 0 || height <= 0) error("empty mask")
        val buffer: ByteBuffer = mask.buffer
        buffer.rewind()
        val count = width * height
        if (buffer.remaining() < count * Float.SIZE_BYTES) error("short mask buffer")
        val luma = ByteArray(count)
        for (i in 0 until count) luma[i] = idMaskConfidenceLuma(buffer.getFloat()).toByte()
        return ImagePipeline.IdMaskDecoded(luma, width, height)
    }

    private companion object {
        /** A result later than this (model load included) is a local failure, never a hung job. */
        const val TIMEOUT_MS = 60_000L
    }
}
