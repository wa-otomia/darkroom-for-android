package app.darkroom.android.core

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WatermarkSettingsCodecTest {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun roundTripPreservesNonDefaultFields() {
        val original = WatermarkSettings(
            sns = SnsWatermark(
                enabled = true,
                logo = SnsLogo.WEIBO,
                handle = "@darkroom",
                scale = 0.06f,
                anchor = WatermarkAnchor(0.22f, 0.81f),
            ),
            date = DateWatermark(
                enabled = true,
                includeTime = true,
                scale = 0.045f,
                anchor = WatermarkAnchor(0.73f, 0.18f),
            ),
        )
        val encoded = json.encodeToString(WatermarkSettings.serializer(), original)
        val decoded = json.decodeFromString(WatermarkSettings.serializer(), encoded)
        assertEquals(original, decoded)
        assertTrue(encoded.contains("\"logo\":\"WEIBO\""))
        assertTrue(encoded.contains("\"handle\":\"@darkroom\""))
        assertTrue(encoded.contains("\"includeTime\":true"))
    }

    @Test
    fun missingOrCorruptFallsBackToDefaults() {
        assertEquals(WatermarkSettings(), decodeOrDefault(null))
        assertEquals(WatermarkSettings(), decodeOrDefault(""))
        assertEquals(WatermarkSettings(), decodeOrDefault("{"))
        assertEquals(WatermarkSettings(), decodeOrDefault("[]"))
        assertEquals(WatermarkSettings(), decodeOrDefault("not-json"))
        assertEquals(WatermarkSettings(), decodeOrDefault("{\"sns\":false}"))
    }

    @Test
    fun unknownKeysAreIgnored() {
        val decoded = json.decodeFromString(
            WatermarkSettings.serializer(),
            """{"sns":{"enabled":true,"logo":"X","extra":1},"date":{"includeTime":true,"unused":[]}}""",
        )
        assertTrue(decoded.sns.enabled)
        assertEquals(SnsLogo.X, decoded.sns.logo)
        assertTrue(decoded.date.includeTime)
        assertEquals(SnsWatermark().handle, decoded.sns.handle)
        assertEquals(DateWatermark().scale, decoded.date.scale, 0f)
    }

    @Test
    fun legacyJsonWithoutLandscapeAnchorsGetsDefaultsAndNoneLogoRoundTrips() {
        val legacy = """{"sns":{"enabled":true,"logo":"X","handle":"h","scale":0.04,"anchor":{"cx":0.2,"cy":0.9}},""" +
            """"date":{"enabled":true,"includeTime":false,"scale":0.03,"anchor":{"cx":0.8,"cy":0.9}}}"""
        val decoded = json.decodeFromString(WatermarkSettings.serializer(), legacy)
        assertEquals(WatermarkAnchor(0.2f, 0.9f), decoded.sns.anchor)
        assertEquals(SnsWatermark().landscapeAnchor, decoded.sns.landscapeAnchor)
        assertEquals(DateWatermark().landscapeAnchor, decoded.date.landscapeAnchor)

        val none = WatermarkSettings(sns = SnsWatermark(enabled = true, logo = SnsLogo.NONE, handle = "h"))
        val encoded = json.encodeToString(WatermarkSettings.serializer(), none)
        assertTrue(encoded.contains("\"logo\":\"NONE\""))
        assertEquals(none, json.decodeFromString(WatermarkSettings.serializer(), encoded))
    }

    private fun decodeOrDefault(raw: String?): WatermarkSettings {
        if (raw.isNullOrBlank()) return WatermarkSettings()
        return runCatching { json.decodeFromString(WatermarkSettings.serializer(), raw) }
            .getOrDefault(WatermarkSettings())
    }
}
