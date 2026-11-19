package io.github.wa_otomia.darkroom.data.ai

import android.os.SystemClock
import io.github.wa_otomia.darkroom.core.AiAuthMode
import io.github.wa_otomia.darkroom.core.DeviceAuthParse
import io.github.wa_otomia.darkroom.core.LoginFailure
import io.github.wa_otomia.darkroom.core.PollOutcome
import io.github.wa_otomia.darkroom.core.PollStep
import io.github.wa_otomia.darkroom.core.StoredXaiCredential
import io.github.wa_otomia.darkroom.core.VerificationUriCheck
import io.github.wa_otomia.darkroom.core.XaiTokens
import io.github.wa_otomia.darkroom.core.classifyDeviceCodeHttpError
import io.github.wa_otomia.darkroom.core.classifyTokenPoll
import io.github.wa_otomia.darkroom.core.firstPollStep
import io.github.wa_otomia.darkroom.core.initialPollState
import io.github.wa_otomia.darkroom.core.nextPollStep
import io.github.wa_otomia.darkroom.core.parseDeviceAuthorization
import io.github.wa_otomia.darkroom.core.parseOAuthErrorCode
import io.github.wa_otomia.darkroom.data.settings.SettingsRepository
import io.github.wa_otomia.darkroom.di.ApplicationScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** In-memory only: never put into saved-instance state, an Intent or a log. Holds no device code. */
sealed interface XaiLoginUiState {
    data object Idle : XaiLoginUiState
    data object Starting : XaiLoginUiState
    data class AwaitingUser(
        val userCode: String,
        val verificationUri: String,
        val openable: Boolean,
        /** On the SystemClock.elapsedRealtime() clock. */
        val deadlineElapsedMs: Long,
        /** The code's whole lifetime (deadline minus start), only to draw the countdown track; 0 = unknown. */
        val lifetimeMs: Long = 0L,
    ) : XaiLoginUiState {
        override fun toString() = "AwaitingUser(***)"
    }
    data class SignedIn(val accountLabel: String?) : XaiLoginUiState
    data class Failed(val reason: LoginFailure) : XaiLoginUiState
    data object Cancelled : XaiLoginUiState
    data object Interrupted : XaiLoginUiState
}

/**
 * The single app-wide device sign-in attempt (AIPV050-R02..R06). Survives leaving the settings
 * screen; a persisted in-progress flag reveals a process death during sign-in on the next start.
 * Every state write and the final token save are tied to the attempt's generation, so nothing
 * from a cancelled or replaced attempt is published or stored.
 */
@Singleton
class XaiLoginCoordinator @Inject constructor(
    private val settings: SettingsRepository,
    private val store: KeystoreXaiTokenStore,
    private val http: XaiAuthHttp,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val lock = Any()
    private val _state = MutableStateFlow<XaiLoginUiState>(XaiLoginUiState.Idle)
    val state: StateFlow<XaiLoginUiState> = _state

    private var job: Job? = null
    private var generation = 0L

    init {
        if (settings.isLoginInProgress()) {
            settings.setLoginInProgress(false)
            _state.value = XaiLoginUiState.Interrupted
        }
    }

    /** Starts a new attempt (cancelling any running one). Does nothing without a saved client ID. */
    fun start() {
        val clientId = settings.grokClientId()
        if (clientId.isBlank() || settings.grokAuthMode() != AiAuthMode.SUBSCRIPTION) return
        synchronized(lock) {
            job?.cancel()
            val gen = ++generation
            settings.setLoginInProgress(true)
            _state.value = XaiLoginUiState.Starting
            job = scope.launch {
                try {
                    run(gen, clientId)
                } finally {
                    withContext(NonCancellable) {
                        synchronized(lock) {
                            if (gen == generation) {
                                job = null
                                settings.setLoginInProgress(false)
                            }
                        }
                    }
                }
            }
        }
    }

    /** Stops polling at once; nothing is saved afterwards even if the user approves in the browser. */
    fun cancel() {
        synchronized(lock) {
            val running = job ?: return
            job = null
            generation++
            running.cancel()
            settings.setLoginInProgress(false)
            _state.value = XaiLoginUiState.Cancelled
        }
    }

    /** Clears a terminal state back to [XaiLoginUiState.Idle]. */
    fun acknowledge() {
        synchronized(lock) {
            when (_state.value) {
                is XaiLoginUiState.Failed,
                is XaiLoginUiState.SignedIn,
                XaiLoginUiState.Cancelled,
                XaiLoginUiState.Interrupted,
                -> _state.value = XaiLoginUiState.Idle
                else -> Unit
            }
        }
    }

    private fun publish(gen: Long, next: XaiLoginUiState) {
        synchronized(lock) { if (gen == generation) _state.value = next }
    }

    private fun fail(gen: Long, step: String, reason: LoginFailure) {
        http.logStep(step, reason.name)
        publish(gen, XaiLoginUiState.Failed(reason))
    }

    private suspend fun run(gen: Long, clientId: String) {
        val ep = http.fetchDiscovery() ?: return fail(gen, "discovery", LoginFailure.UNSUPPORTED)
        currentCoroutineContext().ensureActive()
        val res = http.requestDeviceCode(ep, clientId)
        if (!res.successful) {
            return fail(gen, "device", classifyDeviceCodeHttpError(res.status, parseOAuthErrorCode(res.body)))
        }
        val parsed = parseDeviceAuthorization(res.body.orEmpty()) as? DeviceAuthParse.Ok
            ?: return fail(gen, "device", LoginFailure.MALFORMED)
        http.logStep("device", "ok")
        val auth = parsed.auth
        val startedMs = SystemClock.elapsedRealtime()
        var poll = initialPollState(auth, startedMs)
        publish(
            gen,
            XaiLoginUiState.AwaitingUser(
                userCode = auth.userCode,
                verificationUri = auth.verificationUri,
                openable = parsed.uriCheck == VerificationUriCheck.OPENABLE,
                deadlineElapsedMs = poll.deadlineMs,
                lifetimeMs = poll.deadlineMs - startedMs,
            ),
        )
        var step = firstPollStep(poll, SystemClock.elapsedRealtime())
        while (true) {
            when (val s = step) {
                is PollStep.Done -> return fail(gen, "poll", s.reason)
                is PollStep.Wait -> {
                    delay(s.delayMs)
                    poll = s.state
                }
            }
            if (SystemClock.elapsedRealtime() >= poll.deadlineMs) return fail(gen, "poll", LoginFailure.EXPIRED)
            val r = http.pollToken(ep, clientId, auth.deviceCode)
            val outcome = classifyTokenPoll(r.status, r.body, System.currentTimeMillis())
            if (outcome is PollOutcome.Success) {
                val ok = outcome.tokens
                val credential = StoredXaiCredential(
                    tokens = XaiTokens(
                        accessToken = ok.accessToken,
                        refreshToken = checkNotNull(ok.refreshToken),
                        expiresAtMs = ok.expiresAtMs,
                        accountLabel = ok.accountLabel,
                    ),
                    tokenEndpoint = ep.token,
                    revocationEndpoint = ep.revocation,
                )
                currentCoroutineContext().ensureActive()
                val saved = try {
                    synchronized(lock) {
                        if (gen != generation) throw CancellationException("login replaced")
                        store.save(credential)
                    }
                    true
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    false
                }
                if (!saved) return fail(gen, "save", LoginFailure.FAILED)
                http.logStep("poll", "ok")
                settings.setGrokReloginRequired(false) // also republishes the public AI state
                publish(gen, XaiLoginUiState.SignedIn(ok.accountLabel))
                return
            }
            step = nextPollStep(poll, outcome, SystemClock.elapsedRealtime())
        }
    }
}
