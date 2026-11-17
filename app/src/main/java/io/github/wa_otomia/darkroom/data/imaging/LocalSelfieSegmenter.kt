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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * On-device person segmentation for ID photo cut-outs (EDID040-R11), wrapping the bundled
 * Google ML Kit selfie segmenter. No Play services, network or API key is involved; the photo,
 * the confidence map and the mask stay in memory and are never logged.
 *
 * One inference runs at a time in the process; other callers wait on [lock].
 */
@Singleton
class LocalSelfieSegmenter @Inject constructor() {
    private val lock = Mutex()

    /**
     * Segments [input] (a software ARGB_8888 bitmap, already upright) and returns the mask luma at
     * the mask's size, which is [input]'s size: confidence c maps through [idMaskConfidenceLuma].
     * [input] is not recycled. Any failure but a cancellation, including native-library
     * ([LinkageError]) and [OutOfMemoryError] errors, throws [IllegalStateException] with
     * "[ID_MASK_MSG_LOCAL_FAILED]：<class>", since callers only catch [Exception].
     * A cancel closes the segmenter and drops any late result.
     */
    suspend fun segment(input: Bitmap): ImagePipeline.IdMaskDecoded = lock.withLock {
        var segmenter: Segmenter? = null
        try {
            val client = Segmentation.getClient(
                SelfieSegmenterOptions.Builder()
                    .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
                    .build(),
            )
            segmenter = client
            val mask = awaitMask(client, InputImage.fromBitmap(input, 0))
            readMask(mask)
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            throw IllegalStateException("$ID_MASK_MSG_LOCAL_FAILED：${t.javaClass.simpleName}")
        } finally {
            runCatching { segmenter?.close() }
        }
    }

    private suspend fun awaitMask(client: Segmenter, image: InputImage): SegmentationMask =
        suspendCancellableCoroutine { cont ->
            val executor = Dispatchers.Default.asExecutor()
            client.process(image)
                .addOnSuccessListener(executor) { mask -> if (cont.isActive) cont.resume(mask) }
                .addOnFailureListener(executor) { e -> if (cont.isActive) cont.resumeWithException(e) }
                .addOnCanceledListener(executor) {
                    if (cont.isActive) cont.resumeWithException(IllegalStateException("segmentation cancelled"))
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
}
