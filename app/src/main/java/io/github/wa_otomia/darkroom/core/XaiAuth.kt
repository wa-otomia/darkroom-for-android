package io.github.wa_otomia.darkroom.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.net.URI
import java.util.Base64

/*
 * Experimental xAI subscription sign-in (AIPV050): the standard OAuth 2.0 Device Authorization
 * Grant (RFC 8628) against the endpoints published in xAI's OpenID discovery document. The app
 * ships no client ID: the user enters one they are authorized to use. Requests carry only
 * standard headers and the app's own identity; nothing here imitates another client.
 * Everything in this file is pure and JVM-testable.
 */

const val XAI_ISSUER = "https://auth.x.ai"
const val XAI_DISCOVERY_URL = "https://auth.x.ai/.well-known/openid-configuration"
const val XAI_AUTH_HOST = "auth.x.ai"
const val XAI_API_BASE = "https://api.x.ai/v1"
const val XAI_API_HOST = "api.x.ai"
const val XAI_SCOPE = "openid offline_access api:access"
const val GRANT_DEVICE_CODE = "urn:ietf:params:oauth:grant-type:device_code"
const val GRANT_REFRESH = "refresh_token"
const val DEVICE_DEFAULT_INTERVAL_MS = 5_000L
const val DEVICE_SLOW_DOWN_STEP_MS = 5_000L
const val DEVICE_BACKOFF_CAP_MS = 60_000L
const val DEVICE_MIN_LIFETIME_MS = 600_000L
const val TOKEN_REFRESH_WINDOW_MS = 5 * 60_000L

/** Upper bound for device-flow interval and expires_in, so seconds-to-ms never overflows. */
const val DEVICE_MAX_SECONDS = 86_400L

/** Upper bound for a token's expires_in (ten years). */
const val TOKEN_MAX_LIFETIME_S = 10 * 365 * 86_400L

private val XaiJson = Json { ignoreUnknownKeys = true }

private fun parseObject(body: String?): JsonObject? =
    body?.let { runCatching { XaiJson.parseToJsonElement(it) }.getOrNull() as? JsonObject }

private fun JsonObject.string(key: String): String? =
    (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull

/** Accepts a JSON number or a numeric string; null when absent or not numeric. */
private fun JsonObject.number(key: String): Long? {
    val p = this[key] as? JsonPrimitive ?: return null
    val raw = p.contentOrNull?.trim() ?: return null
    return raw.toLongOrNull() ?: raw.toDoubleOrNull()?.takeIf { it.isFinite() }?.toLong()
}

// ---------------------------------------------------------------------------------------------
// Discovery and host checks
// ---------------------------------------------------------------------------------------------

data class XaiEndpoints(val deviceAuthorization: String, val token: String, val revocation: String?)

private fun httpsHostIs(url: String, host: String): Boolean {
    val uri = runCatching { URI(url) }.getOrNull() ?: return false
    return uri.scheme?.lowercase() == "https" &&
        uri.host?.equals(host, ignoreCase = true) == true &&
        uri.rawUserInfo == null &&
        (uri.port == -1 || uri.port == 443) &&
        uri.rawFragment == null
}

/** https, host exactly auth.x.ai (no subdomain), no user info, default port, no fragment. */
fun isAllowedAuthEndpoint(url: String): Boolean = httpsHostIs(url, XAI_AUTH_HOST)

/** The only host a subscription access token may be sent to (AIPV060-R02). */
fun isAllowedApiUrl(url: String): Boolean = httpsHostIs(url, XAI_API_HOST)

/**
 * Reads only issuer, device_authorization_endpoint, token_endpoint, grant_types_supported and
 * revocation_endpoint. Null when the document is unusable for device sign-in.
 */
fun parseXaiDiscovery(body: String): XaiEndpoints? {
    val root = parseObject(body) ?: return null
    val issuer = root.string("issuer") ?: return null
    if (issuer != XAI_ISSUER && issuer != "$XAI_ISSUER/") return null
    val device = root.string("device_authorization_endpoint")?.takeIf(::isAllowedAuthEndpoint) ?: return null
    val token = root.string("token_endpoint")?.takeIf(::isAllowedAuthEndpoint) ?: return null
    val grants = root["grant_types_supported"]
    if (grants != null) {
        val list = (grants as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull } ?: return null
        if (GRANT_DEVICE_CODE !in list) return null
    }
    val revocation = root.string("revocation_endpoint")?.takeIf(::isAllowedAuthEndpoint)
    return XaiEndpoints(deviceAuthorization = device, token = token, revocation = revocation)
}

// ---------------------------------------------------------------------------------------------
// Device authorization response and user code
// ---------------------------------------------------------------------------------------------

data class DeviceAuthorization(
    val deviceCode: String,
    val userCode: String,
    val verificationUri: String,
    val expiresInS: Long?,
    val intervalS: Long?,
) {
    override fun toString() = "DeviceAuthorization(userCode=***, deviceCode=***, uri=$verificationUri)"
}

enum class VerificationUriCheck { OPENABLE, DISPLAY_ONLY, REJECT }

sealed interface DeviceAuthParse {
    data class Ok(val auth: DeviceAuthorization, val uriCheck: VerificationUriCheck) : DeviceAuthParse

    /** Missing fields, invalid JSON, invalid user code or a verification URI that is not https. */
    data object Malformed : DeviceAuthParse
}

private val USER_CODE = Regex("^[A-Za-z0-9-]{1,64}$")

fun isValidUserCode(code: String): Boolean = USER_CODE.matches(code)

/** https on x.ai or a subdomain → openable; other https → shown only; anything else → rejected. */
fun checkVerificationUri(url: String): VerificationUriCheck {
    val uri = runCatching { URI(url) }.getOrNull() ?: return VerificationUriCheck.REJECT
    if (uri.scheme?.lowercase() != "https") return VerificationUriCheck.REJECT
    val host = uri.host?.lowercase() ?: return VerificationUriCheck.REJECT
    val xai = host == "x.ai" || host.endsWith(".x.ai")
    return if (xai && uri.rawUserInfo == null) VerificationUriCheck.OPENABLE else VerificationUriCheck.DISPLAY_ONLY
}

/** verification_uri_complete is deliberately ignored: only the bare verification_uri is ever opened. */
fun parseDeviceAuthorization(body: String): DeviceAuthParse {
    val root = parseObject(body) ?: return DeviceAuthParse.Malformed
    val deviceCode = root.string("device_code")?.takeIf { it.isNotBlank() } ?: return DeviceAuthParse.Malformed
    val userCode = root.string("user_code")?.takeIf(::isValidUserCode) ?: return DeviceAuthParse.Malformed
    val uri = root.string("verification_uri")?.takeIf { it.isNotBlank() } ?: return DeviceAuthParse.Malformed
    val check = checkVerificationUri(uri)
    if (check == VerificationUriCheck.REJECT) return DeviceAuthParse.Malformed
    return DeviceAuthParse.Ok(
        DeviceAuthorization(
            deviceCode = deviceCode,
            userCode = userCode,
            verificationUri = uri,
            expiresInS = root.number("expires_in")?.takeIf { it > 0 },
            intervalS = root.number("interval")?.takeIf { it > 0 },
        ),
        check,
    )
}

// ---------------------------------------------------------------------------------------------
// Errors
// ---------------------------------------------------------------------------------------------

enum class LoginFailure { INVALID_CLIENT, FORBIDDEN, UNAVAILABLE, TEMPORARY, MALFORMED, DENIED, EXPIRED, FAILED, UNSUPPORTED }

/** Reads only the "error" field; error_description is never read. */
fun parseOAuthErrorCode(body: String?): String? = parseObject(body)?.string("error")?.takeIf { it.isNotBlank() }

private fun isTransientStatus(status: Int?): Boolean = status == null || status == 429 || status in 500..599

@Suppress("UNUSED_PARAMETER")
fun classifyDeviceCodeHttpError(status: Int?, errorCode: String?): LoginFailure = when {
    isTransientStatus(status) -> LoginFailure.TEMPORARY
    status == 400 || status == 401 -> LoginFailure.INVALID_CLIENT
    status == 403 -> LoginFailure.FORBIDDEN
    status == 404 -> LoginFailure.UNAVAILABLE
    else -> LoginFailure.FAILED
}

// ---------------------------------------------------------------------------------------------
// RFC 8628 polling (monotonic time supplied by the caller)
// ---------------------------------------------------------------------------------------------

/** [deadlineMs] is on the caller's monotonic clock (elapsedRealtime). */
data class PollState(val intervalMs: Long, val deadlineMs: Long)

sealed interface PollOutcome {
    data object Pending : PollOutcome
    data object SlowDown : PollOutcome

    /** IO error, timeout, 429 or 5xx. */
    data object Transient : PollOutcome
    data class Success(val tokens: TokenParse.Ok) : PollOutcome {
        override fun toString() = "Success(***)"
    }
    data class Stop(val reason: LoginFailure) : PollOutcome
}

sealed interface PollStep {
    data class Wait(val delayMs: Long, val state: PollState) : PollStep
    data class Done(val reason: LoginFailure) : PollStep
}

fun initialPollState(auth: DeviceAuthorization, nowMs: Long): PollState {
    val interval = auth.intervalS?.takeIf { it > 0 }?.let { it.coerceAtMost(DEVICE_MAX_SECONDS) * 1000 }
        ?: DEVICE_DEFAULT_INTERVAL_MS
    val lifetime = maxOf((auth.expiresInS ?: 0L).coerceIn(0, DEVICE_MAX_SECONDS) * 1000, DEVICE_MIN_LIFETIME_MS)
    return PollState(intervalMs = interval, deadlineMs = nowMs + lifetime)
}

private fun waitOrExpire(state: PollState, nowMs: Long): PollStep {
    val remaining = state.deadlineMs - nowMs
    if (remaining <= 0) return PollStep.Done(LoginFailure.EXPIRED)
    // A non-positive interval would poll back to back; fall back to the RFC 8628 default.
    val safe = if (state.intervalMs > 0) state else state.copy(intervalMs = DEVICE_DEFAULT_INTERVAL_MS)
    return PollStep.Wait(minOf(safe.intervalMs, remaining), safe)
}

/** The first request is also preceded by one interval. */
fun firstPollStep(state: PollState, nowMs: Long): PollStep = waitOrExpire(state, nowMs)

fun classifyTokenPoll(status: Int?, body: String?, nowWallMs: Long): PollOutcome = when {
    status != null && status in 200..299 ->
        when (val parsed = parseTokenResponse(body.orEmpty(), nowWallMs, requireRefresh = true)) {
            is TokenParse.Ok -> PollOutcome.Success(parsed)
            TokenParse.Incomplete -> PollOutcome.Stop(LoginFailure.MALFORMED)
        }
    status == 400 || status == 401 -> when (parseOAuthErrorCode(body)) {
        "authorization_pending" -> PollOutcome.Pending
        "slow_down" -> PollOutcome.SlowDown
        "access_denied" -> PollOutcome.Stop(LoginFailure.DENIED)
        "expired_token" -> PollOutcome.Stop(LoginFailure.EXPIRED)
        "invalid_client", "unauthorized_client", "invalid_scope" -> PollOutcome.Stop(LoginFailure.INVALID_CLIENT)
        else -> PollOutcome.Stop(LoginFailure.FAILED)
    }
    status == 403 -> PollOutcome.Stop(LoginFailure.FORBIDDEN)
    status == 404 -> PollOutcome.Stop(LoginFailure.UNAVAILABLE)
    isTransientStatus(status) -> PollOutcome.Transient
    else -> PollOutcome.Stop(LoginFailure.FAILED)
}

/** The grown interval sticks for every later poll; neither slow_down nor backoff falls back. */
fun nextPollStep(state: PollState, outcome: PollOutcome, nowMs: Long): PollStep {
    val interval = when (outcome) {
        PollOutcome.Pending -> state.intervalMs
        PollOutcome.SlowDown -> state.intervalMs + DEVICE_SLOW_DOWN_STEP_MS
        PollOutcome.Transient -> maxOf(state.intervalMs, minOf(state.intervalMs * 2, DEVICE_BACKOFF_CAP_MS))
        is PollOutcome.Stop -> return PollStep.Done(outcome.reason)
        is PollOutcome.Success -> throw IllegalArgumentException("success ends polling")
    }
    return waitOrExpire(state.copy(intervalMs = interval), nowMs)
}

// ---------------------------------------------------------------------------------------------
// Tokens
// ---------------------------------------------------------------------------------------------

class XaiTokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtMs: Long?,
    val accountLabel: String?,
) {
    override fun toString() = "XaiTokens(access=***, refresh=***, expiresAtMs=$expiresAtMs)"
}

sealed interface TokenParse {
    data class Ok(
        val accessToken: String,
        val refreshToken: String?,
        val expiresAtMs: Long?,
        val accountLabel: String?,
    ) : TokenParse {
        override fun toString() = "TokenParse.Ok(***)"
    }

    data object Incomplete : TokenParse
}

fun parseTokenResponse(body: String, nowWallMs: Long, requireRefresh: Boolean): TokenParse {
    val root = parseObject(body) ?: return TokenParse.Incomplete
    val access = root.string("access_token")?.takeIf { it.isNotBlank() } ?: return TokenParse.Incomplete
    val type = root["token_type"]
    if (type != null) {
        val t = (type as? JsonPrimitive)?.contentOrNull
        if (t == null || !t.equals("bearer", ignoreCase = true)) return TokenParse.Incomplete
    }
    val refresh = root.string("refresh_token")?.takeIf { it.isNotBlank() }
    if (requireRefresh && refresh == null) return TokenParse.Incomplete
    val expiresIn = root.number("expires_in")?.takeIf { it > 0 }?.coerceAtMost(TOKEN_MAX_LIFETIME_S)
    val expiresAt = if (expiresIn != null) nowWallMs + expiresIn * 1000 else jwtExpiryMs(access)
    return TokenParse.Ok(
        accessToken = access,
        refreshToken = refresh,
        expiresAtMs = expiresAt,
        accountLabel = idTokenAccountLabel(root.string("id_token")),
    )
}

/** Unverified JWT payload; used only for display and refresh timing, never for authorization. */
private fun jwtPayload(token: String): JsonObject? {
    val parts = token.split('.')
    if (parts.size != 3 || parts[1].isEmpty()) return null
    val seg = parts[1]
    val padded = seg + "=".repeat((4 - seg.length % 4) % 4)
    val bytes = runCatching { Base64.getUrlDecoder().decode(padded) }.getOrNull() ?: return null
    return parseObject(String(bytes, Charsets.UTF_8))
}

fun jwtExpiryMs(token: String): Long? {
    val exp = jwtPayload(token)?.number("exp")?.takeIf { it > 0 } ?: return null
    return if (exp > Long.MAX_VALUE / 1000) null else exp * 1000
}

fun idTokenAccountLabel(idToken: String?): String? {
    val payload = idToken?.let(::jwtPayload) ?: return null
    return (payload.string("email") ?: payload.string("sub"))?.trim()?.takeIf { it.isNotEmpty() }?.take(256)
}

/** Unknown expiry never pre-refreshes; a 401 forces the refresh instead. */
fun needsRefresh(expiresAtMs: Long?, nowWallMs: Long): Boolean =
    expiresAtMs != null && expiresAtMs - nowWallMs <= TOKEN_REFRESH_WINDOW_MS

// ---------------------------------------------------------------------------------------------
// Refresh classification and request forms
// ---------------------------------------------------------------------------------------------

enum class RefreshFailure { TRANSIENT, RELOGIN_REQUIRED, REJECTED }

fun classifyRefreshError(status: Int?, errorCode: String?): RefreshFailure = when {
    isTransientStatus(status) -> RefreshFailure.TRANSIENT
    status == 401 -> RefreshFailure.RELOGIN_REQUIRED
    status == 400 && errorCode == "invalid_grant" -> RefreshFailure.RELOGIN_REQUIRED
    else -> RefreshFailure.REJECTED
}

fun deviceAuthorizationForm(clientId: String): List<Pair<String, String>> = listOf(
    "client_id" to clientId,
    "scope" to XAI_SCOPE,
)

fun devicePollForm(clientId: String, deviceCode: String): List<Pair<String, String>> = listOf(
    "grant_type" to GRANT_DEVICE_CODE,
    "device_code" to deviceCode,
    "client_id" to clientId,
)

fun refreshForm(clientId: String, refreshToken: String): List<Pair<String, String>> = listOf(
    "grant_type" to GRANT_REFRESH,
    "refresh_token" to refreshToken,
    "client_id" to clientId,
)

fun revocationForm(clientId: String, refreshToken: String): List<Pair<String, String>> = listOf(
    "token" to refreshToken,
    "token_type_hint" to "refresh_token",
    "client_id" to clientId,
)
