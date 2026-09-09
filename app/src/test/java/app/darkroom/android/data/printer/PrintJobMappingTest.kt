package app.darkroom.android.data.printer

import app.darkroom.android.data.catalog.PrintJobEntity
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrintJobMappingTest {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun toUiMapsWatermarkFlag() {
        assertTrue(entity(watermark = true).toUi(json).watermark)
        assertFalse(entity(watermark = false).toUi(json).watermark)
    }

    @Test
    fun entityDefaultsWatermarkOff() {
        val row = entity(watermark = false).copy()
        val omitted = PrintJobEntity(
            id = "job-1",
            photoId = "photo-1",
            source = "original",
            cropJson = null,
            copies = 1,
            presetId = null,
            prompt = "",
            cropImageWidth = null,
            cropImageHeight = null,
            rotateQuarters = 0,
            landscape = false,
            rotationDegrees = 0f,
            placementJson = null,
            origin = "manual",
            state = "queued",
            phase = null,
            createdAt = 1L,
            startedAt = null,
            finishedAt = null,
            printerJobId = null,
            jobState = null,
            error = null,
        )
        assertFalse(omitted.watermark)
        assertFalse(omitted.toUi(json).watermark)
        assertEquals("photo-1", row.toUi(json).photoId)
    }

    private fun entity(watermark: Boolean) = PrintJobEntity(
        id = "job-1",
        photoId = "photo-1",
        source = "original",
        cropJson = null,
        copies = 1,
        presetId = null,
        prompt = "",
        cropImageWidth = null,
        cropImageHeight = null,
        rotateQuarters = 0,
        landscape = false,
        rotationDegrees = 0f,
        placementJson = null,
        origin = "manual",
        watermark = watermark,
        state = "queued",
        phase = null,
        createdAt = 1L,
        startedAt = null,
        finishedAt = null,
        printerJobId = null,
        jobState = null,
        error = null,
    )
}
