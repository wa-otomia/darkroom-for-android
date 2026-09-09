package app.darkroom.android.data.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import app.darkroom.android.core.AI_PRESET_NONE
import app.darkroom.android.core.AiPreset
import app.darkroom.android.core.AiPresetStore
import app.darkroom.android.core.AiProvider
import app.darkroom.android.core.AiSettings
import app.darkroom.android.core.DEFAULT_OPENAI_BASE_URL
import app.darkroom.android.core.DEFAULT_XAI_BASE_URL
import app.darkroom.android.core.PrintFit
import app.darkroom.android.core.ProviderSettings
import app.darkroom.android.core.PublicAiSettings
import app.darkroom.android.core.WatermarkSettings
import app.darkroom.android.core.defaultProviderSettings
import app.darkroom.android.core.mergeBuiltinPresets
import app.darkroom.android.core.mergeProviderSettings
import app.darkroom.android.core.newPresetId
import app.darkroom.android.core.normalizeAiBaseUrl
import app.darkroom.android.core.normalizeAiPresetStore
import app.darkroom.android.core.parseGrokResolution
import app.darkroom.android.core.parseQuality
import app.darkroom.android.core.parsePrintFit
import app.darkroom.android.core.printFitWire
import app.darkroom.android.core.seedAiPresets
import app.darkroom.android.core.toPublicAi
import app.darkroom.android.core.validateAiPresetFields
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

data class AppSettings(
    val printerMac: String = "",
    val printerName: String = "",
    val defaultCopies: Int = 1,
    val autoPrint: Boolean = false,
    val autoEdit: Boolean = false,
    val autoWatermark: Boolean = false,
    val printFit: PrintFit = PrintFit.COVER,
    val ftpUser: String = "camera",
    val ftpPassword: String = "",
    val ftpPort: Int = 2121,
    val ftpPasvMin: Int = 50000,
    val ftpPasvMax: Int = 50010,
    val ftpAutoStart: Boolean = false,
    val keepAlive: Boolean = false,
)

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "darkroom.secure",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )
    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings
    private val _presets = MutableStateFlow(seedAiPresets())
    val presets: StateFlow<AiPresetStore> = _presets
    private val _ai = MutableStateFlow(toPublicAi(AiSettings(AiProvider.GROK, emptyMap())))
    val ai: StateFlow<PublicAiSettings> = _ai
    private val _watermark = MutableStateFlow(WatermarkSettings())
    val watermark: StateFlow<WatermarkSettings> = _watermark

    init {
        if (prefs.getString(KEY_FTP_PASSWORD, "").isNullOrBlank()) {
            prefs.edit()
                .putString(KEY_FTP_USER, "camera")
                .putString(KEY_FTP_PASSWORD, generatePassword())
                .putInt(KEY_FTP_PORT, 2121)
                .apply()
        }
        if (prefs.getString(KEY_PRESETS, null) == null) {
            prefs.edit().putString(KEY_PRESETS, json.encodeToString(seedAiPresets())).apply()
        } else {
            val merged = mergeBuiltinPresets(normalizeAiPresetStore(readPresetStoreRaw()))
            prefs.edit().putString(KEY_PRESETS, json.encodeToString(merged)).apply()
        }
        if (!prefs.contains(KEY_AUTO_EDIT)) {
            val lastSelectedId = readPresets().lastSelectedId
            val migrateEdit = prefs.getBoolean(KEY_AUTO_PRINT, false) && lastSelectedId != AI_PRESET_NONE
            prefs.edit().putBoolean(KEY_AUTO_EDIT, migrateEdit).apply()
        }
        _settings.value = readSettings()
        _presets.value = readPresets()
        _watermark.value = readWatermark()
        publishAi()
    }

    fun readSettings(): AppSettings = AppSettings(
        printerMac = prefs.getString(KEY_PRINTER_MAC, "").orEmpty(),
        printerName = prefs.getString(KEY_PRINTER_NAME, "").orEmpty(),
        defaultCopies = prefs.getInt(KEY_COPIES, 1).coerceIn(1, 9),
        autoPrint = prefs.getBoolean(KEY_AUTO_PRINT, false),
        autoEdit = prefs.getBoolean(KEY_AUTO_EDIT, false),
        autoWatermark = prefs.getBoolean(KEY_AUTO_WATERMARK, false),
        printFit = parsePrintFit(prefs.getString(KEY_PRINT_FIT, "cover")),
        ftpUser = prefs.getString(KEY_FTP_USER, "camera").orEmpty().ifBlank { "camera" },
        ftpPassword = prefs.getString(KEY_FTP_PASSWORD, "").orEmpty(),
        ftpPort = prefs.getInt(KEY_FTP_PORT, 2121),
        ftpPasvMin = prefs.getInt(KEY_PASV_MIN, 50000),
        ftpPasvMax = prefs.getInt(KEY_PASV_MAX, 50010),
        ftpAutoStart = prefs.getBoolean(KEY_FTP_AUTO_START, false),
        keepAlive = prefs.getBoolean(KEY_KEEP_ALIVE, false),
    )

    fun updateSettings(patch: AppSettings.() -> AppSettings) {
        val next = _settings.value.patch()
        prefs.edit()
            .putString(KEY_PRINTER_MAC, next.printerMac)
            .putString(KEY_PRINTER_NAME, next.printerName)
            .putInt(KEY_COPIES, next.defaultCopies.coerceIn(1, 9))
            .putBoolean(KEY_AUTO_PRINT, next.autoPrint)
            .putBoolean(KEY_AUTO_EDIT, next.autoEdit)
            .putBoolean(KEY_AUTO_WATERMARK, next.autoWatermark)
            .putString(KEY_PRINT_FIT, printFitWire(next.printFit))
            .putString(KEY_FTP_USER, next.ftpUser.ifBlank { "camera" })
            .putString(KEY_FTP_PASSWORD, if (next.ftpPassword.length >= 8) next.ftpPassword else _settings.value.ftpPassword)
            .putInt(KEY_FTP_PORT, next.ftpPort)
            .putInt(KEY_PASV_MIN, next.ftpPasvMin)
            .putInt(KEY_PASV_MAX, next.ftpPasvMax)
            .putBoolean(KEY_FTP_AUTO_START, next.ftpAutoStart)
            .putBoolean(KEY_KEEP_ALIVE, next.keepAlive)
            .apply()
        _settings.value = readSettings()
    }

    fun readAi(): AiSettings {
        val grok = mergeProviderSettings(
            AiProvider.GROK,
            ProviderSettings(
                apiKey = prefs.getString(KEY_GROK_KEY, "").orEmpty(),
                baseUrl = prefs.getString(KEY_GROK_URL, "").orEmpty(),
                model = prefs.getString(KEY_GROK_MODEL, "").orEmpty(),
                resolution = prefs.getString(KEY_GROK_RES, "1k").orEmpty(),
                quality = prefs.getString(KEY_GROK_QUALITY, "medium").orEmpty(),
            ),
            defaultProviderSettings(AiProvider.GROK),
        )
        val openai = mergeProviderSettings(
            AiProvider.OPENAI,
            ProviderSettings(
                apiKey = prefs.getString(KEY_OPENAI_KEY, "").orEmpty(),
                baseUrl = prefs.getString(KEY_OPENAI_URL, "").orEmpty(),
                model = prefs.getString(KEY_OPENAI_MODEL, "").orEmpty(),
                resolution = "1k",
                quality = prefs.getString(KEY_OPENAI_QUALITY, "medium").orEmpty(),
            ),
            defaultProviderSettings(AiProvider.OPENAI),
        )
        return AiSettings(
            active = AiProvider.fromId(prefs.getString(KEY_AI_PROVIDER, AiProvider.GROK.id)),
            byProvider = mapOf(AiProvider.GROK to grok, AiProvider.OPENAI to openai),
        )
    }

    fun aiConfigured(): Boolean = readAi().activeSettings.apiKey.isNotBlank()

    fun updateProvider(
        provider: AiProvider,
        apiKey: String?,
        baseUrl: String?,
        model: String?,
        resolution: String?,
        quality: String?,
    ) {
        val prev = readAi().byProvider[provider] ?: defaultProviderSettings(provider)
        val next = ProviderSettings(
            apiKey = apiKey?.trim()?.ifEmpty { prev.apiKey } ?: prev.apiKey,
            baseUrl = if (baseUrl != null) normalizeAiBaseUrl(baseUrl, provider.defaultBaseUrl) else prev.baseUrl,
            model = model?.trim()?.ifEmpty { prev.model } ?: prev.model,
            resolution = if (provider == AiProvider.GROK) parseGrokResolution(resolution, prev.resolution) else prev.resolution,
            quality = parseQuality(provider, quality, prev.quality),
        )
        val editor = prefs.edit()
        when (provider) {
            AiProvider.GROK -> editor
                .putString(KEY_GROK_KEY, next.apiKey)
                .putString(KEY_GROK_URL, next.baseUrl)
                .putString(KEY_GROK_MODEL, next.model)
                .putString(KEY_GROK_RES, next.resolution)
                .putString(KEY_GROK_QUALITY, next.quality)
            AiProvider.OPENAI -> editor
                .putString(KEY_OPENAI_KEY, next.apiKey)
                .putString(KEY_OPENAI_URL, next.baseUrl)
                .putString(KEY_OPENAI_MODEL, next.model)
                .putString(KEY_OPENAI_QUALITY, next.quality)
        }
        editor.apply()
        publishAi()
    }

    fun clearProviderKey(provider: AiProvider) {
        val key = if (provider == AiProvider.GROK) KEY_GROK_KEY else KEY_OPENAI_KEY
        prefs.edit().putString(key, "").apply()
        publishAi()
    }

    fun setActiveProvider(provider: AiProvider) {
        prefs.edit().putString(KEY_AI_PROVIDER, provider.id).apply()
        publishAi()
    }

    fun cacheModels(provider: AiProvider, models: List<String>) {
        val key = if (provider == AiProvider.GROK) KEY_GROK_MODELS else KEY_OPENAI_MODELS
        prefs.edit().putString(key, json.encodeToString(models)).apply()
        publishAi()
    }

    fun cachedModels(provider: AiProvider): List<String> {
        val raw = prefs.getString(
            if (provider == AiProvider.GROK) KEY_GROK_MODELS else KEY_OPENAI_MODELS,
            null,
        ) ?: return emptyList()
        return runCatching { json.decodeFromString<List<String>>(raw) }.getOrDefault(emptyList())
            .filter { it.isNotBlank() }
    }

    fun clearGrokKey() = clearProviderKey(AiProvider.GROK)

    fun readWatermark(): WatermarkSettings {
        val raw = prefs.getString(KEY_WATERMARK, null) ?: return WatermarkSettings()
        return runCatching { json.decodeFromString<WatermarkSettings>(raw) }.getOrDefault(WatermarkSettings())
    }

    fun updateWatermark(patch: WatermarkSettings.() -> WatermarkSettings) {
        val next = _watermark.value.patch()
        prefs.edit().putString(KEY_WATERMARK, json.encodeToString(next)).apply()
        _watermark.value = readWatermark()
    }

    private fun publishAi() {
        val settings = readAi()
        val cached = AiProvider.entries.associateWith { cachedModels(it) }
        _ai.value = toPublicAi(settings, cached)
    }

    /** Restores device configuration to factory defaults. AI presets and the photo catalog are left untouched. */
    fun resetAll() {
        prefs.edit()
            .putString(KEY_PRINTER_MAC, "")
            .putString(KEY_PRINTER_NAME, "")
            .putInt(KEY_COPIES, 1)
            .putBoolean(KEY_AUTO_PRINT, false)
            .putBoolean(KEY_AUTO_EDIT, false)
            .putBoolean(KEY_AUTO_WATERMARK, false)
            .putString(KEY_PRINT_FIT, printFitWire(PrintFit.COVER))
            .putString(KEY_FTP_USER, "camera")
            .putString(KEY_FTP_PASSWORD, generatePassword())
            .putInt(KEY_FTP_PORT, 2121)
            .putInt(KEY_PASV_MIN, 50000)
            .putInt(KEY_PASV_MAX, 50010)
            .putBoolean(KEY_FTP_AUTO_START, false)
            .putBoolean(KEY_KEEP_ALIVE, false)
            .putString(KEY_GROK_KEY, "")
            .putString(KEY_GROK_URL, DEFAULT_XAI_BASE_URL)
            .putString(KEY_GROK_MODEL, AiProvider.GROK.defaultModel)
            .putString(KEY_GROK_RES, "1k")
            .putString(KEY_GROK_QUALITY, "medium")
            .putString(KEY_OPENAI_KEY, "")
            .putString(KEY_OPENAI_URL, DEFAULT_OPENAI_BASE_URL)
            .putString(KEY_OPENAI_MODEL, AiProvider.OPENAI.defaultModel)
            .putString(KEY_OPENAI_QUALITY, "medium")
            .putString(KEY_AI_PROVIDER, AiProvider.GROK.id)
            .putString(KEY_GROK_MODELS, "[]")
            .putString(KEY_OPENAI_MODELS, "[]")
            .putString(KEY_WATERMARK, json.encodeToString(WatermarkSettings()))
            .apply()
        _settings.value = readSettings()
        _watermark.value = readWatermark()
        publishAi()
    }

    fun readPresets(): AiPresetStore = mergeBuiltinPresets(normalizeAiPresetStore(readPresetStoreRaw()))

    private fun readPresetStoreRaw(): AiPresetStore? {
        val raw = prefs.getString(KEY_PRESETS, null) ?: return null
        return runCatching { json.decodeFromString<AiPresetStore>(raw) }.getOrNull()
    }

    private fun writePresets(store: AiPresetStore): AiPresetStore {
        prefs.edit().putString(KEY_PRESETS, json.encodeToString(store)).apply()
        _presets.value = store
        return store
    }

    fun setLastPreset(id: String): AiPresetStore {
        val store = readPresets()
        val last = id.trim()
        if (last.isNotEmpty() && store.presets.none { it.id == last }) error("预设不存在")
        return writePresets(store.copy(lastSelectedId = last))
    }

    fun createPreset(title: String, prompt: String, category: String): AiPresetStore {
        val fields = validateAiPresetFields(title, prompt, category)
        val store = readPresets()
        return writePresets(store.copy(presets = store.presets + AiPreset(newPresetId(), fields.title, fields.prompt, fields.category)))
    }

    fun updatePreset(id: String, title: String, prompt: String, category: String): AiPresetStore {
        val store = readPresets()
        val item = store.presets.find { it.id == id } ?: error("预设不存在")
        val fields = validateAiPresetFields(title, prompt, category)
        return writePresets(
            store.copy(
                presets = store.presets.map {
                    if (it.id == id) item.copy(title = fields.title, prompt = fields.prompt, category = fields.category) else it
                },
            ),
        )
    }

    fun deletePreset(id: String): AiPresetStore {
        val store = readPresets()
        val next = store.presets.filter { it.id != id }
        if (next.size == store.presets.size) error("预设不存在")
        return writePresets(
            store.copy(
                presets = next,
                lastSelectedId = if (store.lastSelectedId == id) AI_PRESET_NONE else store.lastSelectedId,
            ),
        )
    }

    companion object {
        private const val GROK_DEFAULT_MODEL = "grok-imagine-image-2.0"
        private const val KEY_PRINTER_MAC = "printer_mac"
        private const val KEY_PRINTER_NAME = "printer_name"
        private const val KEY_COPIES = "copies"
        private const val KEY_AUTO_PRINT = "auto_print"
        private const val KEY_AUTO_EDIT = "auto_edit"
        private const val KEY_AUTO_WATERMARK = "auto_watermark"
        private const val KEY_PRINT_FIT = "print_fit"
        private const val KEY_FTP_USER = "ftp_user"
        private const val KEY_FTP_PASSWORD = "ftp_password"
        private const val KEY_FTP_PORT = "ftp_port"
        private const val KEY_PASV_MIN = "pasv_min"
        private const val KEY_PASV_MAX = "pasv_max"
        private const val KEY_FTP_AUTO_START = "ftp_auto_start"
        private const val KEY_KEEP_ALIVE = "keep_alive"
        // No language key: the UI locale lives in AppCompatDelegate /
        // LocaleManager only. Keeping a copy here would be a second source of
        // truth that the Android 13+ system language picker never writes to,
        // and the two would drift apart the first time it is used.
        private const val KEY_GROK_KEY = "grok_key"
        private const val KEY_GROK_URL = "grok_url"
        private const val KEY_GROK_MODEL = "grok_model"
        private const val KEY_GROK_RES = "grok_res"
        private const val KEY_GROK_QUALITY = "grok_quality"
        private const val KEY_OPENAI_KEY = "openai_key"
        private const val KEY_OPENAI_URL = "openai_url"
        private const val KEY_OPENAI_MODEL = "openai_model"
        private const val KEY_OPENAI_QUALITY = "openai_quality"
        private const val KEY_AI_PROVIDER = "ai_provider"
        private const val KEY_GROK_MODELS = "grok_models"
        private const val KEY_OPENAI_MODELS = "openai_models"
        private const val KEY_WATERMARK = "watermark"
        private const val KEY_PRESETS = "presets"

        fun generatePassword(): String {
            val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789"
            val rng = SecureRandom()
            return buildString(10) { repeat(10) { append(alphabet[rng.nextInt(alphabet.length)]) } }
        }
    }
}
