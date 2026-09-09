package app.darkroom.android.data.ai

import android.util.Base64
import app.darkroom.android.core.AiProvider
import app.darkroom.android.core.buildGrokEditJson
import app.darkroom.android.core.imagesEditsUrl
import app.darkroom.android.core.imagineErrorMessage
import app.darkroom.android.core.modelsListFallbackUrl
import app.darkroom.android.core.modelsListUrl
import app.darkroom.android.core.normalizeAiBaseUrl
import app.darkroom.android.core.parseImageEditResponse
import app.darkroom.android.core.parseModelIds
import app.darkroom.android.core.sanitizeAiSecrets
import app.darkroom.android.data.imaging.ImagePipeline
import app.darkroom.android.data.settings.ActivityLog
import app.darkroom.android.data.settings.SettingsRepository
import app.darkroom.android.di.ApplicationScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.Buffer
import okio.BufferedSink
import okio.ForwardingSink
import okio.buffer
import java.io.IOException
import java.io.InterruptedIOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val PROBE_TIMEOUT_S = 15L
private const val PROBE_BODY_LIMIT = 64L * 1024

sealed interface AiProbe {
    data class Ok(val models: List<String>, val elapsedMs: Long) : AiProbe
    data class Unauthorized(val status: Int) : AiProbe
    object NotFound : AiProbe
    data class HttpError(val status: Int) : AiProbe
    object Unreachable : AiProbe
    object Timeout : AiProbe
    object NoKey : AiProbe
    object InvalidUrl : AiProbe
}

data class AiProgress(
    val photoId: String,
    val phase: String,
    val elapsedMs: Long = 0,
    val loaded: Long? = null,
    val total: Long? = null,
    val percent: Int? = null,
    val error: String? = null,
)

@Singleton
class AiImageClient @Inject constructor(
    private val settings: SettingsRepository,
    private val activityLog: ActivityLog,
    @Suppress("unused")
    @ApplicationScope private val appScope: CoroutineScope,
) {
    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(240, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    private val probeHttp: OkHttpClient by lazy {
        http.newBuilder()
            .connectTimeout(PROBE_TIMEOUT_S, TimeUnit.SECONDS)
            .readTimeout(PROBE_TIMEOUT_S, TimeUnit.SECONDS)
            .callTimeout(PROBE_TIMEOUT_S, TimeUnit.SECONDS)
            .build()
    }

    private val _progress = MutableSharedFlow<AiProgress>(extraBufferCapacity = 32)
    val progress: SharedFlow<AiProgress> = _progress

    suspend fun listModels(provider: AiProvider, baseUrl: String, apiKey: String): AiProbe =
        withContext(Dispatchers.IO) {
            val stored = settings.readAi().byProvider[provider]
            val key = apiKey.trim().ifEmpty { stored?.apiKey.orEmpty() }
            if (key.isBlank()) return@withContext AiProbe.NoKey
            val fallback = stored?.baseUrl ?: provider.defaultBaseUrl
            val normalized = normalizeAiBaseUrl(baseUrl, fallback)
            val primary = modelsListUrl(provider, normalized).toHttpUrlOrNull()
                ?: return@withContext AiProbe.InvalidUrl
            val startedAt = System.currentTimeMillis()
            fun elapsed() = System.currentTimeMillis() - startedAt
            val raw = probeUrl(primary.toString(), key) { body -> parseModelIds(provider, body) }
                ?: fallbackList(provider, normalized, key)
            val outcome = if (raw is AiProbe.Ok) raw.copy(elapsedMs = elapsed()) else raw
            activityLog.record(
                "ai",
                "listModels ${provider.id} GET -> ${probeLabel(outcome)} ${elapsed()}ms",
                status = if (outcome is AiProbe.Ok) "ok" else "error",
            )
            if (outcome is AiProbe.Ok) {
                settings.cacheModels(provider, outcome.models)
            }
            outcome
        }

    suspend fun edit(
        image: ByteArray,
        prompt: String,
        photoId: String,
        purpose: String = "edit",
        onProgress: (AiProgress) -> Unit = {},
    ): ByteArray =
        withContext(Dispatchers.IO) {
            val cfg = settings.readAi().activeSettings
            val provider = settings.readAi().active
            val t0 = System.currentTimeMillis()
            fun emit(phase: String, loaded: Long? = null, total: Long? = null, error: String? = null) {
                val elapsed = System.currentTimeMillis() - t0
                val pct = if (loaded != null && total != null && total > 0) ((100 * loaded) / total).toInt() else null
                val update = AiProgress(photoId, phase, elapsed, loaded, total, pct, error?.let(::sanitizeAiSecrets))
                onProgress(update)
                _progress.tryEmit(update)
            }
            if (cfg.apiKey.isBlank()) {
                emit("error", error = "未配置 AI 钥匙")
                error("未配置 AI 钥匙。去设置页填入 API key 后再修图。")
            }
            emit("preparing")
            val url = imagesEditsUrl(cfg.baseUrl, provider.defaultBaseUrl)
            activityLog.record(
                "ai",
                "POST $url ${provider.id} ${cfg.model} ${cfg.resolution}/${cfg.quality} photo=$photoId purpose=$purpose",
            )
            try {
                val upload = ImagePipeline.grokUploadJpeg(image)
                val requestBody = when (provider) {
                    AiProvider.GROK -> grokEditBody(cfg, prompt, upload) { written, total ->
                        emit("uploading", written, total)
                        if (written >= total && total > 0) emit("processing")
                    }
                    AiProvider.OPENAI -> openAiEditBody(cfg, prompt, upload) { written, total ->
                        emit("uploading", written, total)
                        if (written >= total && total > 0) emit("processing")
                    }
                }
                val req = Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer ${cfg.apiKey}")
                    .post(requestBody)
                    .build()
                val call = http.newCall(req)
                val cancelOnCompletion = currentCoroutineContext().job.invokeOnCompletion { cause ->
                    if (cause is CancellationException) call.cancel()
                }
                try {
                    call.awaitResponse().use { res ->
                        val raw = readBody(res) { phase, loaded, total, error -> emit(phase, loaded, total, error) }
                        val parsed = parseImageEditResponse(String(raw, Charsets.UTF_8))
                        if (!res.isSuccessful) error(imagineErrorMessage(parsed, res.code))
                        val rawImage = when {
                            parsed.b64.isNotBlank() -> Base64.decode(parsed.b64, Base64.DEFAULT)
                            parsed.url.isNotBlank() -> {
                                val imgCall = http.newCall(Request.Builder().url(parsed.url).build())
                                val cancelImg = currentCoroutineContext().job.invokeOnCompletion { cause ->
                                    if (cause is CancellationException) imgCall.cancel()
                                }
                                try {
                                    imgCall.awaitResponse().use { img ->
                                        if (!img.isSuccessful) error("无法下载返回的图片")
                                        readBody(img) { phase, loaded, total, error ->
                                            emit(phase, loaded, total, error)
                                        }
                                    }
                                } catch (e: CancellationException) {
                                    imgCall.cancel()
                                    throw e
                                } finally {
                                    cancelImg.dispose()
                                }
                            }
                            else -> error("没有返回图片")
                        }
                        emit("downloaded", rawImage.size.toLong(), rawImage.size.toLong())
                        activityLog.record("ai", "ok ${rawImage.size}B")
                        return@withContext rawImage
                    }
                } catch (e: CancellationException) {
                    call.cancel()
                    throw e
                } finally {
                    cancelOnCompletion.dispose()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (e is IOException && !currentCoroutineContext().isActive) {
                    throw CancellationException("cancelled", e)
                }
                val message = e.message ?: e.toString()
                emit("error", error = message)
                activityLog.record("ai", "failed", status = "error", error = sanitizeAiSecrets(message))
                throw e
            }
        }

    private fun grokEditBody(
        cfg: app.darkroom.android.core.ProviderSettings,
        prompt: String,
        upload: ByteArray,
        onBytes: (Long, Long) -> Unit,
    ): RequestBody {
        val dataUri = "data:image/jpeg;base64," + Base64.encodeToString(upload, Base64.NO_WRAP)
        val bytes = buildGrokEditJson(cfg.model, prompt, dataUri, cfg.resolution, cfg.quality)
            .toByteArray(Charsets.UTF_8)
        return CountingRequestBody(bytes.toRequestBody("application/json".toMediaType()), onBytes)
    }

    private fun openAiEditBody(
        cfg: app.darkroom.android.core.ProviderSettings,
        prompt: String,
        upload: ByteArray,
        onBytes: (Long, Long) -> Unit,
    ): RequestBody {
        val jpeg = "image/jpeg".toMediaType()
        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("model", cfg.model)
            .addFormDataPart("image", "image.jpg", upload.toRequestBody(jpeg))
            .addFormDataPart("prompt", prompt)
            .addFormDataPart("quality", cfg.quality)
            .addFormDataPart("size", "auto")
            .addFormDataPart("output_format", "jpeg")
            .addFormDataPart("n", "1")
            .build()
        return CountingRequestBody(multipart, onBytes)
    }

    private suspend fun fallbackList(provider: AiProvider, base: String, key: String): AiProbe {
        val fallback = modelsListFallbackUrl(provider, base)?.toHttpUrlOrNull()
            ?: return AiProbe.NotFound
        return probeUrl(fallback.toString(), key) { body -> parseModelIds(provider, body) }
            ?: AiProbe.NotFound
    }

    /**
     * Returns a verdict for [url], or null when Grok's primary listing route 404s so the caller
     * can try the OpenAI-compatible `/models` fallback.
     */
    private suspend fun probeUrl(url: String, key: String, parse: (String) -> List<String>): AiProbe? {
        val httpUrl = url.toHttpUrlOrNull() ?: return AiProbe.InvalidUrl
        val request = Request.Builder()
            .url(httpUrl)
            .header("Authorization", "Bearer $key")
            .build()
        return try {
            val res = probeHttp.newCall(request).awaitProbe()
            when {
                res.successful -> AiProbe.Ok(parse(res.body), 0)
                res.code == 404 -> null
                res.code == 401 || res.code == 403 -> AiProbe.Unauthorized(res.code)
                else -> AiProbe.HttpError(res.code)
            }
        } catch (e: IOException) {
            if (e is InterruptedIOException) AiProbe.Timeout else AiProbe.Unreachable
        }
    }
}

private class ProbeResponse(val code: Int, val successful: Boolean, val body: String)

private suspend fun Call.awaitResponse(): Response = suspendCancellableCoroutine { cont ->
    cont.invokeOnCancellation { runCatching { cancel() } }
    enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) {
            if (cont.isActive) {
                cont.resume(response)
            } else {
                response.close()
            }
        }

        override fun onFailure(call: Call, e: IOException) {
            if (cont.isActive) cont.resumeWithException(e)
        }
    })
}

private suspend fun Call.awaitProbe(): ProbeResponse = suspendCancellableCoroutine { cont ->
    cont.invokeOnCancellation { runCatching { cancel() } }
    enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) {
            val snapshot = response.use {
                ProbeResponse(
                    code = it.code,
                    successful = it.isSuccessful,
                    body = runCatching { it.peekBody(PROBE_BODY_LIMIT).string() }.getOrDefault(""),
                )
            }
            cont.resume(snapshot)
        }

        override fun onFailure(call: Call, e: IOException) {
            cont.resumeWithException(e)
        }
    })
}

private suspend fun readBody(
    res: Response,
    emit: (String, Long?, Long?, String?) -> Unit,
): ByteArray {
    val body = res.body
    val declared = body.contentLength().takeIf { it >= 0L }
    emit("downloading", 0L, declared, null)
    val source = body.source()
    val out = Buffer()
    var loaded = 0L
    var lastAt = 0L
    while (!source.exhausted()) {
        currentCoroutineContext().ensureActive()
        val n = source.read(out, DOWNLOAD_CHUNK)
        if (n < 0L) break
        loaded += n
        val now = System.currentTimeMillis()
        if (now - lastAt >= PROGRESS_MIN_MS || n >= DOWNLOAD_CHUNK) {
            emit("downloading", loaded, declared, null)
            lastAt = now
        }
    }
    currentCoroutineContext().ensureActive()
    val raw = out.readByteArray()
    emit("downloading", raw.size.toLong(), declared ?: raw.size.toLong(), null)
    return raw
}

internal class CountingRequestBody(
    private val delegate: RequestBody,
    private val onBytes: (written: Long, total: Long) -> Unit,
) : RequestBody() {
    override fun contentType() = delegate.contentType()
    override fun contentLength() = delegate.contentLength()
    override fun writeTo(sink: BufferedSink) {
        val total = contentLength().coerceAtLeast(0L)
        val counting = object : ForwardingSink(sink) {
            private var written = 0L
            override fun write(source: Buffer, byteCount: Long) {
                super.write(source, byteCount)
                written += byteCount
                onBytes(written, total)
            }
        }
        val buffered = counting.buffer()
        delegate.writeTo(buffered)
        buffered.flush()
        if (total > 0L) onBytes(total, total)
    }
}

private const val DOWNLOAD_CHUNK = 64L * 1024
private const val PROGRESS_MIN_MS = 100L

private fun probeLabel(probe: AiProbe): String = when (probe) {
    is AiProbe.Ok -> "ok ${probe.models.size} models"
    is AiProbe.Unauthorized -> "unauthorized ${probe.status}"
    is AiProbe.NotFound -> "not_found"
    is AiProbe.HttpError -> "http ${probe.status}"
    is AiProbe.Unreachable -> "unreachable"
    is AiProbe.Timeout -> "timeout"
    is AiProbe.NoKey -> "no_key"
    is AiProbe.InvalidUrl -> "invalid_url"
}
