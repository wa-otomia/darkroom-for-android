package io.github.wa_otomia.darkroom.data.ai

import io.github.wa_otomia.darkroom.BuildConfig
import io.github.wa_otomia.darkroom.core.RefreshCall
import io.github.wa_otomia.darkroom.core.TokenParse
import io.github.wa_otomia.darkroom.core.XAI_DISCOVERY_URL
import io.github.wa_otomia.darkroom.core.XaiEndpoints
import io.github.wa_otomia.darkroom.core.XaiRefresher
import io.github.wa_otomia.darkroom.core.classifyRefreshError
import io.github.wa_otomia.darkroom.core.deviceAuthorizationForm
import io.github.wa_otomia.darkroom.core.devicePollForm
import io.github.wa_otomia.darkroom.core.isAllowedAuthEndpoint
import io.github.wa_otomia.darkroom.core.parseOAuthErrorCode
import io.github.wa_otomia.darkroom.core.parseTokenResponse
import io.github.wa_otomia.darkroom.core.parseXaiDiscovery
import io.github.wa_otomia.darkroom.core.refreshForm
import io.github.wa_otomia.darkroom.core.revocationForm
import io.github.wa_otomia.darkroom.data.settings.ActivityLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/** [status] null means IO error or timeout. [body] is at most 64 KiB and is never logged. */
class HttpResult(val status: Int?, val body: String?) {
    val successful: Boolean get() = status != null && status in 200..299
    override fun toString() = "HttpResult(status=$status)"
}

/**
 * HTTP for the xAI sign-in endpoints only (AIPV050-R02). Its own client: redirects are never
 * followed, every URL must pass [isAllowedAuthEndpoint], and requests carry only Accept, the form
 * content type and this app's own User-Agent. No response body is ever written to a log.
 */
@Singleton
class XaiAuthHttp @Inject constructor(
    private val activityLog: ActivityLog,
) : XaiRefresher {
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    private val revokeHttp: OkHttpClient by lazy {
        http.newBuilder().callTimeout(5, TimeUnit.SECONDS).build()
    }

    private val userAgent = "darkroom-android/${BuildConfig.VERSION_NAME}"

    suspend fun fetchDiscovery(): XaiEndpoints? {
        val result = send(http, XAI_DISCOVERY_URL, form = null)
        val endpoints = if (result.successful) parseXaiDiscovery(result.body.orEmpty()) else null
        log("discovery", if (endpoints != null) "ok" else "UNSUPPORTED")
        return endpoints
    }

    suspend fun requestDeviceCode(ep: XaiEndpoints, clientId: String): HttpResult =
        send(http, ep.deviceAuthorization, deviceAuthorizationForm(clientId))

    suspend fun pollToken(ep: XaiEndpoints, clientId: String, deviceCode: String): HttpResult =
        send(http, ep.token, devicePollForm(clientId, deviceCode))

    override suspend fun refresh(tokenEndpoint: String, clientId: String, refreshToken: String): RefreshCall {
        val result = send(http, tokenEndpoint, refreshForm(clientId, refreshToken))
        val call = if (result.successful) {
            when (val parsed = parseTokenResponse(result.body.orEmpty(), System.currentTimeMillis(), requireRefresh = false)) {
                is TokenParse.Ok -> RefreshCall.Ok(parsed)
                TokenParse.Incomplete -> RefreshCall.Failed(result.status, null)
            }
        } else {
            RefreshCall.Failed(result.status, parseOAuthErrorCode(result.body))
        }
        log(
            "refresh",
            when (call) {
                is RefreshCall.Ok -> "ok"
                is RefreshCall.Failed -> classifyRefreshError(call.status, call.errorCode).name
            },
        )
        return call
    }

    /** RFC 7009 best effort with a short timeout; never retried, every failure swallowed. */
    suspend fun revoke(revocationEndpoint: String, clientId: String, refreshToken: String) {
        val ok = try {
            send(revokeHttp, revocationEndpoint, revocationForm(clientId, refreshToken)).successful
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            false
        }
        log("revoke", if (ok) "ok" else "failed")
    }

    fun logStep(step: String, outcome: String) = log(step, outcome)

    private fun log(step: String, outcome: String) {
        activityLog.record("ai", "xai-auth $step -> $outcome", status = if (outcome == "ok") "ok" else "error")
    }

    /** GET when [form] is null, otherwise a form POST. Refuses any URL that is not an allowed auth endpoint. */
    private suspend fun send(client: OkHttpClient, url: String, form: List<Pair<String, String>>?): HttpResult {
        if (!isAllowedAuthEndpoint(url)) return HttpResult(-1, null)
        val builder = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("User-Agent", userAgent)
        if (form != null) {
            val body = FormBody.Builder().apply { form.forEach { (k, v) -> add(k, v) } }.build()
            builder.post(body)
        }
        return client.newCall(builder.build()).awaitResult()
    }
}

private const val AUTH_BODY_LIMIT = 64L * 1024

private suspend fun Call.awaitResult(): HttpResult = suspendCancellableCoroutine { cont ->
    cont.invokeOnCancellation { runCatching { cancel() } }
    enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) {
            val result = response.use {
                HttpResult(it.code, runCatching { it.peekBody(AUTH_BODY_LIMIT).string() }.getOrNull())
            }
            if (cont.isActive) cont.resume(result)
        }

        override fun onFailure(call: Call, e: IOException) {
            if (cont.isActive) cont.resume(HttpResult(null, null))
        }
    })
}
