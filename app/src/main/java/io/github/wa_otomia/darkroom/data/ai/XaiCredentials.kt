package io.github.wa_otomia.darkroom.data.ai

import io.github.wa_otomia.darkroom.core.AuthAttempt
import io.github.wa_otomia.darkroom.core.CredentialResult
import io.github.wa_otomia.darkroom.core.XaiCredentialCoordinator
import io.github.wa_otomia.darkroom.core.withXaiToken
import io.github.wa_otomia.darkroom.core.xaiCredentialMessage
import io.github.wa_otomia.darkroom.data.settings.SettingsRepository
import io.github.wa_otomia.darkroom.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The one entry point for using, signing out of and resetting the xAI subscription credential.
 * Every subscription request (settings probe, AI jobs, print edits, ID-photo masks, automation)
 * goes through [withToken] and therefore through one shared refresh lock (AIPV050-R08).
 */
@Singleton
class XaiCredentials @Inject constructor(
    private val settings: SettingsRepository,
    private val store: KeystoreXaiTokenStore,
    private val http: XaiAuthHttp,
    private val login: XaiLoginCoordinator,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val coordinator = XaiCredentialCoordinator(
        store = store,
        refresher = http,
        clientId = { settings.grokClientId() },
        nowWallMs = System::currentTimeMillis,
        onReloginRequired = { settings.setGrokReloginRequired(true) },
    )

    fun hasCredential(): Boolean = store.hasCredential()

    suspend fun <T> withToken(
        messageFor: (CredentialResult) -> String = ::xaiCredentialMessage,
        call: suspend (String) -> AuthAttempt<T>,
    ): T = withXaiToken(coordinator, messageFor, call)

    /**
     * Local sign-out first (AIPV050-R10): cancel any sign-in, take and delete the tokens under the
     * refresh lock, clear the re-login flag and publish. Revocation then runs best effort in the
     * background with the client ID read before anything else changes. Keeps API key and client ID.
     */
    suspend fun signOut() {
        val clientId = settings.grokClientId()
        login.cancel()
        val taken = coordinator.takeForSignOut()
        settings.setGrokReloginRequired(false) // also republishes the public AI state
        val revocation = taken?.revocationEndpoint ?: return
        val refreshToken = taken.tokens.refreshToken
        scope.launch { http.revoke(revocation, clientId, refreshToken) }
    }

    /** Called by the reset flow before SettingsRepository.resetAll(), which then clears the client ID. */
    suspend fun clearForReset() = signOut()
}
