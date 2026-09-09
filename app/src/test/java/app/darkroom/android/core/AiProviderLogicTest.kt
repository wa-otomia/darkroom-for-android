package app.darkroom.android.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiProviderLogicTest {
    private val grokEnv = ProviderSettings(
        apiKey = "env-key",
        baseUrl = "https://proxy.example/v1",
        model = "grok-imagine-image",
        resolution = "2k",
        quality = "low",
    )
    private val openaiEnv = ProviderSettings(
        apiKey = "sk-envkey",
        baseUrl = "https://openai.example/v1",
        model = "gpt-image-1.5",
        resolution = "1k",
        quality = "medium",
    )

    @Test
    fun mergeFallsBackToEnv() {
        assertEquals(grokEnv, mergeProviderSettings(AiProvider.GROK, null, grokEnv))
        assertEquals(
            grokEnv,
            mergeProviderSettings(
                AiProvider.GROK,
                ProviderSettings("  ", "  ", "", "2k", "low"),
                grokEnv,
            ),
        )
        assertEquals(openaiEnv, mergeProviderSettings(AiProvider.OPENAI, null, openaiEnv))
    }

    @Test
    fun mergePrefersStored() {
        assertEquals(
            ProviderSettings(
                apiKey = "file-key",
                baseUrl = DEFAULT_XAI_BASE_URL,
                model = "grok-imagine-image-2.0",
                resolution = "1k",
                quality = "medium",
            ),
            mergeProviderSettings(
                AiProvider.GROK,
                ProviderSettings(
                    apiKey = "file-key",
                    baseUrl = "https://api.x.ai/v1/",
                    model = "grok-imagine-image-2.0",
                    resolution = "1k",
                    quality = "medium",
                ),
                grokEnv,
            ),
        )
    }

    @Test
    fun qualityValidationPerProvider() {
        assertEquals("medium", parseQuality(AiProvider.GROK, "high", "medium"))
        assertEquals("low", parseQuality(AiProvider.GROK, "low", "medium"))
        assertEquals("medium", parseQuality(AiProvider.GROK, "medium", "low"))
        assertEquals("high", parseQuality(AiProvider.OPENAI, "high", "medium"))
        assertEquals("low", parseQuality(AiProvider.OPENAI, "low", "medium"))
        assertEquals("medium", parseQuality(AiProvider.OPENAI, "ultra", "medium"))
        assertEquals("1k", parseGrokResolution("4k", "1k"))
        assertEquals(
            grokEnv.copy(resolution = "2k", quality = "low"),
            mergeProviderSettings(
                AiProvider.GROK,
                grokEnv.copy(resolution = "4k", quality = "high"),
                grokEnv,
            ),
        )
        assertEquals(
            openaiEnv.copy(quality = "high"),
            mergeProviderSettings(
                AiProvider.OPENAI,
                openaiEnv.copy(quality = "high"),
                openaiEnv,
            ),
        )
    }

    @Test
    fun publicViewHidesKey() {
        val pub = toPublicProvider(
            ProviderSettings("secret-key", "https://api.x.ai/v1", "grok-imagine-image-2.0", "1k", "medium"),
        )
        assertEquals(true, pub.keySet)
        assertFalse(pub.toString().contains("secret-key"))
        assertFalse(toPublicProvider(grokEnv.copy(apiKey = "")).keySet)
        val ai = toPublicAi(
            AiSettings(
                AiProvider.OPENAI,
                mapOf(
                    AiProvider.GROK to grokEnv.copy(apiKey = ""),
                    AiProvider.OPENAI to openaiEnv,
                ),
            ),
        )
        assertTrue(ai.keySet)
        assertEquals(AiProvider.OPENAI, ai.active)
        assertEquals(openaiEnv.model, ai.model)
    }

    @Test
    fun normalizeBaseUrlBothProviders() {
        assertEquals(DEFAULT_XAI_BASE_URL, normalizeAiBaseUrl("", DEFAULT_XAI_BASE_URL))
        assertEquals(DEFAULT_XAI_BASE_URL, normalizeAiBaseUrl("  ", DEFAULT_XAI_BASE_URL))
        assertEquals(DEFAULT_XAI_BASE_URL, normalizeAiBaseUrl(null, DEFAULT_XAI_BASE_URL))
        assertEquals(DEFAULT_XAI_BASE_URL, normalizeAiBaseUrl("https://api.x.ai/v1/", DEFAULT_XAI_BASE_URL))
        assertEquals(DEFAULT_XAI_BASE_URL, normalizeAiBaseUrl("https://api.x.ai/v1/images/edits", DEFAULT_XAI_BASE_URL))
        assertEquals(
            DEFAULT_OPENAI_BASE_URL,
            normalizeAiBaseUrl("https://api.openai.com/v1/images/edits", DEFAULT_OPENAI_BASE_URL),
        )
        assertEquals(
            DEFAULT_OPENAI_BASE_URL,
            normalizeAiBaseUrl("https://api.openai.com/v1/", DEFAULT_OPENAI_BASE_URL),
        )
        assertEquals(
            "https://proxy.example/v1/images/edits",
            imagesEditsUrl("https://proxy.example/v1/", DEFAULT_XAI_BASE_URL),
        )
        assertEquals(
            "https://api.x.ai/v1/image-generation-models",
            modelsListUrl(AiProvider.GROK, "https://api.x.ai/v1/"),
        )
        assertEquals(
            "https://api.x.ai/v1/models",
            modelsListFallbackUrl(AiProvider.GROK, "https://api.x.ai/v1/"),
        )
        assertEquals(
            "https://api.openai.com/v1/models",
            modelsListUrl(AiProvider.OPENAI, "https://api.openai.com/v1/"),
        )
        assertEquals(null, modelsListFallbackUrl(AiProvider.OPENAI, DEFAULT_OPENAI_BASE_URL))
        assertEquals(DEFAULT_XAI_BASE_URL, normalizeGrokBaseUrl("https://api.x.ai/v1/images/edits"))
        assertEquals("https://proxy.example/v1/images/edits", grokImagesEditsUrl("https://proxy.example/v1/"))
    }

    @Test
    fun parseGrokModelsArrayWithAliases() {
        val body = """
            {
              "models": [
                {
                  "id": "grok-imagine-image-2.0",
                  "aliases": ["grok-imagine-image", "imagine-2"]
                },
                { "id": "grok-imagine-image-quality" }
              ]
            }
        """.trimIndent()
        assertEquals(
            listOf("grok-imagine-image-2.0", "grok-imagine-image", "imagine-2", "grok-imagine-image-quality"),
            parseModelIds(AiProvider.GROK, body),
        )
    }

    @Test
    fun parseGrokDataArray() {
        val body = """
            {
              "data": [
                { "id": "grok-imagine-image-2.0" },
                { "id": "grok-imagine-image" }
              ]
            }
        """.trimIndent()
        assertEquals(
            listOf("grok-imagine-image-2.0", "grok-imagine-image"),
            parseModelIds(AiProvider.GROK, body),
        )
    }

    @Test
    fun parseOpenAiImageModelsOnly() {
        val body = """
            {
              "data": [
                { "id": "gpt-image-1.5" },
                { "id": "chatgpt-image-latest" },
                { "id": "dall-e-3" },
                { "id": "gpt-4o" },
                { "id": "tts-1" },
                { "id": "sora-image-preview" }
              ]
            }
        """.trimIndent()
        assertEquals(
            listOf("gpt-image-1.5", "chatgpt-image-latest", "dall-e-3", "sora-image-preview"),
            parseModelIds(AiProvider.OPENAI, body),
        )
    }

    @Test
    fun parseModelIdsIgnoresInvalidJson() {
        assertEquals(emptyList<String>(), parseModelIds(AiProvider.GROK, "not-json"))
        assertEquals(emptyList<String>(), parseModelIds(AiProvider.OPENAI, "{}"))
    }

    @Test
    fun sanitizeSecretsRedactsXaiAndSk() {
        assertTrue(sanitizeAiSecrets("Authorization: Bearer xai-abcdefghijk").contains("********"))
        assertFalse(sanitizeAiSecrets("xai-abcdefghijk").contains("xai-abcdefghijk"))
        assertFalse(sanitizeAiSecrets("sk-abcdefghijk").contains("sk-abcdefghijk"))
        assertTrue(sanitizeAiSecrets("key=sk-abcdefghijk").contains("sk-********"))
    }

    @Test
    fun formatByteProgressUsesPassedUnitLabels() {
        assertEquals("", formatByteProgress(null, null))
        assertEquals("512 B / 1 KB", formatByteProgress(512, 1024))
        assertEquals("1.5 MB / 3.0 MB", formatByteProgress(1_500_000, 3_000_000))
        assertEquals("2.0 兆 / 4.0 兆", formatByteProgress(2_000_000, 4_000_000, megabyteLabel = "兆"))
    }

    @Test
    fun compatGrokAliasesStillWork() {
        assertEquals(grokEnv, mergeGrokSettings(null, grokEnv))
        assertEquals("medium", parseGrokQuality("high", "medium"))
    }
}
