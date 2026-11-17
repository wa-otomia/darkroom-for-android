package io.github.wa_otomia.darkroom.core

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** What the encrypted token file holds (AIPV050-R07). */
class StoredXaiCredential(
    val tokens: XaiTokens,
    val tokenEndpoint: String,
    val revocationEndpoint: String?,
) {
    override fun toString() = "StoredXaiCredential(tokens=***, tokenEndpoint=$tokenEndpoint)"
}

interface XaiTokenStorePort {
    /** Null when signed out or when the stored credential is unreadable (which also deletes it). */
    fun load(): StoredXaiCredential?

    /** Atomic replace; throws when the credential could not be persisted. */
    fun save(c: StoredXaiCredential)

    fun delete()
}

sealed interface RefreshCall {
    data class Ok(val parsed: TokenParse.Ok) : RefreshCall

    /** [status] null means IO error or timeout. [errorCode] is only the OAuth "error" field. */
    data class Failed(val status: Int?, val errorCode: String?) : RefreshCall
}

fun interface XaiRefresher {
    suspend fun refresh(tokenEndpoint: String, clientId: String, refreshToken: String): RefreshCall
}

sealed interface CredentialResult {
    data class Token(val accessToken: String) : CredentialResult {
        override fun toString() = "Token(***)"
    }

    data object NotSignedIn : CredentialResult
    data object ReloginRequired : CredentialResult
    data object Transient : CredentialResult
    data object Rejected : CredentialResult
}

/** The user-facing message for a credential that could not be used. */
fun xaiCredentialMessage(result: CredentialResult): String = when (result) {
    CredentialResult.NotSignedIn -> XAI_MSG_NOT_SIGNED_IN
    CredentialResult.ReloginRequired -> XAI_MSG_RELOGIN
    CredentialResult.Transient -> XAI_MSG_REFRESH_NETWORK
    CredentialResult.Rejected -> XAI_MSG_REJECTED
    is CredentialResult.Token -> XAI_MSG_REJECTED
}

/** Thrown by [withXaiToken] when no usable access token exists or the retry was also rejected. */
class XaiCredentialException(val result: CredentialResult, message: String) : IllegalStateException(message)

/**
 * Single-flight refresh for every subscription caller (AIPV050-R08). All reads, refreshes,
 * persistence and sign-out run under one mutex, so concurrent callers share one refresh and a
 * sign-out never interleaves with a refresh that would write the tokens back.
 */
class XaiCredentialCoordinator(
    private val store: XaiTokenStorePort,
    private val refresher: XaiRefresher,
    private val clientId: () -> String,
    private val nowWallMs: () -> Long,
    private val onReloginRequired: () -> Unit,
) {
    private val mutex = Mutex()

    /**
     * A usable access token. With [rejected] (the token a request just got 401 for) a refresh is
     * forced unless another caller already replaced that token.
     */
    suspend fun accessToken(rejected: String? = null): CredentialResult = mutex.withLock {
        val current = store.load() ?: return@withLock CredentialResult.NotSignedIn
        val tokens = current.tokens
        if (rejected != null && tokens.accessToken != rejected) {
            return@withLock CredentialResult.Token(tokens.accessToken)
        }
        if (rejected == null && !needsRefresh(tokens.expiresAtMs, nowWallMs())) {
            return@withLock CredentialResult.Token(tokens.accessToken)
        }
        val call = try {
            refresher.refresh(current.tokenEndpoint, clientId(), tokens.refreshToken)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            RefreshCall.Failed(null, null)
        }
        when (call) {
            is RefreshCall.Ok -> {
                val parsed = call.parsed
                val next = StoredXaiCredential(
                    tokens = XaiTokens(
                        accessToken = parsed.accessToken,
                        refreshToken = parsed.refreshToken ?: tokens.refreshToken,
                        expiresAtMs = parsed.expiresAtMs,
                        accountLabel = parsed.accountLabel ?: tokens.accountLabel,
                    ),
                    tokenEndpoint = current.tokenEndpoint,
                    revocationEndpoint = current.revocationEndpoint,
                )
                // Persist before any request uses the new access token: a rotated refresh token
                // that was never stored cannot be used again.
                try {
                    store.save(next)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    return@withLock CredentialResult.Rejected
                }
                CredentialResult.Token(parsed.accessToken)
            }
            is RefreshCall.Failed -> when (classifyRefreshError(call.status, call.errorCode)) {
                RefreshFailure.RELOGIN_REQUIRED -> {
                    store.delete()
                    onReloginRequired()
                    CredentialResult.ReloginRequired
                }
                RefreshFailure.TRANSIENT -> CredentialResult.Transient
                RefreshFailure.REJECTED -> CredentialResult.Rejected
            }
        }
    }

    /** Reads then deletes the stored credential under the refresh lock; returns the in-memory copy. */
    suspend fun takeForSignOut(): StoredXaiCredential? = mutex.withLock {
        val current = store.load()
        store.delete()
        current
    }
}

sealed interface AuthAttempt<out T> {
    data class Done<T>(val value: T) : AuthAttempt<T>
    data object Unauthorized : AuthAttempt<Nothing>
}

/**
 * Runs [call] with an access token; a 401 ([AuthAttempt.Unauthorized]) forces one refresh and one
 * resend, never more. Throws [XaiCredentialException] with [messageFor]'s text when no token is
 * usable, or with the rejected message when the resend is also unauthorized.
 */
suspend fun <T> withXaiToken(
    coordinator: XaiCredentialCoordinator,
    messageFor: (CredentialResult) -> String = ::xaiCredentialMessage,
    call: suspend (String) -> AuthAttempt<T>,
): T {
    fun tokenOrThrow(result: CredentialResult): String = when (result) {
        is CredentialResult.Token -> result.accessToken
        else -> throw XaiCredentialException(result, messageFor(result))
    }
    val first = tokenOrThrow(coordinator.accessToken())
    when (val attempt = call(first)) {
        is AuthAttempt.Done -> return attempt.value
        AuthAttempt.Unauthorized -> Unit
    }
    val second = tokenOrThrow(coordinator.accessToken(rejected = first))
    return when (val attempt = call(second)) {
        is AuthAttempt.Done -> attempt.value
        AuthAttempt.Unauthorized ->
            throw XaiCredentialException(CredentialResult.Rejected, messageFor(CredentialResult.Rejected))
    }
}
