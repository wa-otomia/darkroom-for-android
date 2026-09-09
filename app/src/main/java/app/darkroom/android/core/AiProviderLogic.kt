package app.darkroom.android.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

const val DEFAULT_XAI_BASE_URL = "https://api.x.ai/v1"
const val DEFAULT_OPENAI_BASE_URL = "https://api.openai.com/v1"

private val AiJson = Json { ignoreUnknownKeys = true }

enum class AiProvider(val id: String, val defaultBaseUrl: String, val defaultModel: String) {
    GROK("grok", DEFAULT_XAI_BASE_URL, "grok-imagine-image-2.0"),
    OPENAI("openai", DEFAULT_OPENAI_BASE_URL, "gpt-image-1.5"),
    ;

    companion object {
        fun fromId(raw: String?): AiProvider =
            entries.firstOrNull { it.id.equals(raw?.trim().orEmpty(), ignoreCase = true) } ?: GROK
    }
}

val GROK_MODELS = listOf(
    "grok-imagine-image-2.0",
    "grok-imagine-image",
    "grok-imagine-image-quality",
)

data class ProviderSettings(
    val apiKey: String,
    val baseUrl: String,
    val model: String,
    val resolution: String,
    val quality: String,
)

data class AiSettings(
    val active: AiProvider,
    val byProvider: Map<AiProvider, ProviderSettings>,
) {
    val activeSettings: ProviderSettings
        get() = byProvider[active] ?: defaultProviderSettings(active)
}

data class PublicProviderSettings(
    val keySet: Boolean,
    val baseUrl: String,
    val model: String,
    val resolution: String,
    val quality: String,
)

data class PublicAiSettings(
    val active: AiProvider,
    val byProvider: Map<AiProvider, PublicProviderSettings>,
    val cachedModels: Map<AiProvider, List<String>> = emptyMap(),
) {
    val keySet: Boolean get() = byProvider[active]?.keySet == true
    val baseUrl: String get() = byProvider[active]?.baseUrl.orEmpty()
    val model: String get() = byProvider[active]?.model.orEmpty()
    val resolution: String get() = byProvider[active]?.resolution.orEmpty()
    val quality: String get() = byProvider[active]?.quality.orEmpty()
}

typealias GrokSettings = ProviderSettings
typealias PublicGrokSettings = PublicProviderSettings

fun defaultProviderSettings(provider: AiProvider): ProviderSettings = ProviderSettings(
    apiKey = "",
    baseUrl = provider.defaultBaseUrl,
    model = provider.defaultModel,
    resolution = "1k",
    quality = "medium",
)

fun parseGrokResolution(value: String?, fallback: String): String =
    if (value == "1k" || value == "2k") value else fallback

fun parseQuality(provider: AiProvider, value: String?, fallback: String): String {
    val allowed = when (provider) {
        AiProvider.GROK -> setOf("low", "medium")
        AiProvider.OPENAI -> setOf("low", "medium", "high")
    }
    return value?.takeIf { it in allowed } ?: fallback
}

fun parseGrokQuality(value: String?, fallback: String): String =
    parseQuality(AiProvider.GROK, value, fallback)

fun normalizeAiBaseUrl(value: String?, fallback: String): String {
    var raw = value?.trim().orEmpty()
    if (raw.isEmpty()) return fallback
    raw = raw.trimEnd('/')
    raw = raw.replace(Regex("/images/edits$", RegexOption.IGNORE_CASE), "")
    raw = raw.trimEnd('/')
    return raw.ifEmpty { fallback }
}

fun normalizeGrokBaseUrl(value: String?, fallback: String = DEFAULT_XAI_BASE_URL): String =
    normalizeAiBaseUrl(value, fallback)

fun imagesEditsUrl(baseUrl: String, fallback: String = DEFAULT_XAI_BASE_URL): String =
    "${normalizeAiBaseUrl(baseUrl, fallback)}/images/edits"

fun grokImagesEditsUrl(baseUrl: String): String = imagesEditsUrl(baseUrl, DEFAULT_XAI_BASE_URL)

fun modelsListUrl(provider: AiProvider, base: String): String {
    val normalized = normalizeAiBaseUrl(base, provider.defaultBaseUrl)
    return when (provider) {
        AiProvider.GROK -> "$normalized/image-generation-models"
        AiProvider.OPENAI -> "$normalized/models"
    }
}

fun modelsListFallbackUrl(provider: AiProvider, base: String): String? {
    if (provider != AiProvider.GROK) return null
    return "${normalizeAiBaseUrl(base, provider.defaultBaseUrl)}/models"
}

fun parseModelIds(provider: AiProvider, body: String): List<String> {
    val root = runCatching { AiJson.parseToJsonElement(body) }.getOrNull() as? JsonObject ?: return emptyList()
    return when (provider) {
        AiProvider.GROK -> parseGrokModelIds(root)
        AiProvider.OPENAI -> parseOpenAiModelIds(root)
    }
}

private fun parseGrokModelIds(root: JsonObject): List<String> {
    val models = root["models"] as? JsonArray
    if (models != null) {
        return models.flatMap { element ->
            val obj = element as? JsonObject ?: return@flatMap emptyList()
            val id = obj["id"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
            val aliases = (obj["aliases"] as? JsonArray)?.mapNotNull { alias ->
                alias.jsonPrimitive.contentOrNull?.takeIf { it.isNotBlank() }
            }.orEmpty()
            listOfNotNull(id) + aliases
        }.distinct()
    }
    val data = root["data"] as? JsonArray ?: return emptyList()
    return data.mapNotNull { element ->
        (element as? JsonObject)?.get("id")?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
    }.distinct()
}

private fun parseOpenAiModelIds(root: JsonObject): List<String> {
    val data = root["data"] as? JsonArray ?: return emptyList()
    return data.mapNotNull { element ->
        (element as? JsonObject)?.get("id")?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
    }.filter(::isOpenAiImageModel).distinct()
}

fun isOpenAiImageModel(id: String): Boolean {
    val key = id.lowercase()
    return key.contains("gpt-image") ||
        key.contains("chatgpt-image") ||
        key.contains("dall-e") ||
        key.contains("image")
}

fun mergeProviderSettings(
    provider: AiProvider,
    stored: ProviderSettings?,
    env: ProviderSettings,
): ProviderSettings {
    val storedKey = stored?.apiKey?.trim().orEmpty()
    val storedBase = stored?.baseUrl?.trim().orEmpty()
    val storedModel = stored?.model?.trim().orEmpty()
    return ProviderSettings(
        apiKey = storedKey.ifEmpty { env.apiKey },
        baseUrl = normalizeAiBaseUrl(storedBase.ifEmpty { env.baseUrl }, env.baseUrl),
        model = storedModel.ifEmpty { env.model },
        resolution = stored?.resolution?.let { parseGrokResolution(it, env.resolution) } ?: env.resolution,
        quality = stored?.quality?.let { parseQuality(provider, it, env.quality) } ?: env.quality,
    )
}

fun mergeGrokSettings(stored: GrokSettings?, env: GrokSettings): GrokSettings =
    mergeProviderSettings(AiProvider.GROK, stored, env)

fun toPublicProvider(settings: ProviderSettings) = PublicProviderSettings(
    keySet = settings.apiKey.isNotBlank(),
    baseUrl = settings.baseUrl,
    model = settings.model,
    resolution = settings.resolution,
    quality = settings.quality,
)

fun toPublicAi(
    settings: AiSettings,
    cachedModels: Map<AiProvider, List<String>> = emptyMap(),
) = PublicAiSettings(
    active = settings.active,
    byProvider = AiProvider.entries.associateWith { provider ->
        toPublicProvider(settings.byProvider[provider] ?: defaultProviderSettings(provider))
    },
    cachedModels = cachedModels,
)

fun toPublicGrok(settings: GrokSettings) = toPublicProvider(settings)

fun buildGrokEditJson(
    model: String,
    prompt: String,
    dataUri: String,
    resolution: String,
    quality: String,
): String {
    val image = JsonObject(
        mapOf(
            "url" to JsonPrimitive(dataUri),
            "type" to JsonPrimitive("image_url"),
        ),
    )
    return AiJson.encodeToString(
        JsonObject.serializer(),
        JsonObject(
            mapOf(
                "model" to JsonPrimitive(model),
                "prompt" to JsonPrimitive(prompt),
                "image" to image,
                "aspect_ratio" to JsonPrimitive("auto"),
                "resolution" to JsonPrimitive(resolution),
                "quality" to JsonPrimitive(quality),
                "n" to JsonPrimitive(1),
                "response_format" to JsonPrimitive("b64_json"),
            ),
        ),
    )
}

data class ImageEditResult(
    val b64: String = "",
    val url: String = "",
    val error: String = "",
)

fun parseImageEditResponse(body: String): ImageEditResult {
    val root = runCatching { AiJson.parseToJsonElement(body) }.getOrNull() as? JsonObject
        ?: return ImageEditResult()
    val err = root["error"]
    val error = when (err) {
        is JsonPrimitive -> err.contentOrNull.orEmpty()
        is JsonObject -> err["message"]?.jsonPrimitive?.contentOrNull.orEmpty()
        else -> ""
    }
    val item = (root["data"] as? JsonArray)?.firstOrNull() as? JsonObject
    return ImageEditResult(
        b64 = item?.get("b64_json")?.jsonPrimitive?.contentOrNull.orEmpty(),
        url = item?.get("url")?.jsonPrimitive?.contentOrNull.orEmpty(),
        error = error,
    )
}

fun imagineErrorMessage(parsed: ImageEditResult, status: Int): String =
    parsed.error.ifBlank { "修图失败 ($status)" }

fun sanitizeAiSecrets(text: String): String {
    return text
        .replace(Regex("(bearer\\s+)\\S+", RegexOption.IGNORE_CASE), "$1********")
        .replace(Regex("((?:api[_-]?key|authorization)\\s*[:=]\\s*)\\S+", RegexOption.IGNORE_CASE), "$1********")
        .replace(Regex("\\bxai-[A-Za-z0-9_-]{8,}\\b"), "xai-********")
        .replace(Regex("\\bsk-[A-Za-z0-9_-]{8,}\\b"), "sk-********")
}

fun formatByteProgress(
    loaded: Long?,
    total: Long?,
    megabyteLabel: String = "MB",
    kilobyteLabel: String = "KB",
    byteLabel: String = "B",
): String {
    if (loaded == null && total == null) return ""
    fun fmt(n: Long): String = when {
        n >= 1_000_000 -> "${"%.1f".format(n / 1_000_000.0)} $megabyteLabel"
        n >= 1000 -> "${n / 1000} $kilobyteLabel"
        else -> "$n $byteLabel"
    }
    return when {
        loaded != null && total != null && total > 0 -> "${fmt(loaded)} / ${fmt(total)}"
        loaded != null -> fmt(loaded)
        else -> fmt(total!!)
    }
}
