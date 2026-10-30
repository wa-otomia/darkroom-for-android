package io.github.wa_otomia.darkroom.core

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WatermarkScaleSyncTest {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val apart = WatermarkSettings(
        sns = SnsWatermark(scale = 0.05f),
        date = DateWatermark(scale = 0.03f),
    )

    @Test
    fun syncIsOffByDefault() {
        assertFalse(WatermarkSettings().syncScale)
    }

    @Test
    fun withoutSyncEachSliderMovesOnlyItsOwnMark() {
        val sns = apart.withSnsScale(0.06f)
        assertEquals(0.06f, sns.sns.scale, 0f)
        assertEquals(0.03f, sns.date.scale, 0f)

        val date = apart.withDateScale(0.025f)
        assertEquals(0.05f, date.sns.scale, 0f)
        assertEquals(0.025f, date.date.scale, 0f)
    }

    @Test
    fun withSyncTheSnsSliderSetsBothMarks() {
        val next = apart.copy(syncScale = true).withSnsScale(0.06f)
        assertEquals(0.06f, next.sns.scale, 0f)
        assertEquals(0.06f, next.date.scale, 0f)
        assertTrue(next.syncScale)
    }

    @Test
    fun withSyncTheDateSliderSetsBothMarks() {
        val next = apart.copy(syncScale = true).withDateScale(0.025f)
        assertEquals(0.025f, next.sns.scale, 0f)
        assertEquals(0.025f, next.date.scale, 0f)
    }

    @Test
    fun switchingSyncOnDoesNotChangeEitherSizeUntilASliderMoves() {
        val on = apart.copy(syncScale = true)
        assertEquals(0.05f, on.sns.scale, 0f)
        assertEquals(0.03f, on.date.scale, 0f)
    }

    @Test
    fun syncedValueIsClampedOnceForBothMarks() {
        val high = apart.copy(syncScale = true).withSnsScale(1f)
        assertEquals(WATERMARK_SCALE_MAX, high.sns.scale, 0f)
        assertEquals(WATERMARK_SCALE_MAX, high.date.scale, 0f)

        val low = apart.copy(syncScale = true).withDateScale(0f)
        assertEquals(WATERMARK_SCALE_MIN, low.sns.scale, 0f)
        assertEquals(WATERMARK_SCALE_MIN, low.date.scale, 0f)
    }

    @Test
    fun syncLeavesEverythingElseAlone() {
        val base = WatermarkSettings(
            sns = SnsWatermark(enabled = true, logo = SnsLogo.X, handle = "@a", scale = 0.05f),
            date = DateWatermark(enabled = false, includeTime = true, scale = 0.03f),
            syncScale = true,
        )
        val next = base.withSnsScale(0.04f)
        assertEquals(base.sns.copy(scale = 0.04f), next.sns)
        assertEquals(base.date.copy(scale = 0.04f), next.date)
    }

    @Test
    fun syncSurvivesRoundTripAndOlderSavesDecodeAsOff() {
        val on = apart.copy(syncScale = true)
        val encoded = json.encodeToString(WatermarkSettings.serializer(), on)
        assertTrue(encoded.contains("\"syncScale\":true"))
        assertEquals(on, json.decodeFromString(WatermarkSettings.serializer(), encoded))

        val legacy = json.decodeFromString(
            WatermarkSettings.serializer(),
            """{"sns":{"enabled":true,"scale":0.05},"date":{"enabled":true,"scale":0.03}}""",
        )
        assertFalse(legacy.syncScale)
        assertEquals(0.05f, legacy.sns.scale, 0f)
        assertEquals(0.03f, legacy.date.scale, 0f)
    }
}
