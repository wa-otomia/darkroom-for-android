package io.github.wa_otomia.darkroom.core

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class XaiCredentialCoordinatorTest {
    private val endpoint = "https://auth.x.ai/oauth2/token"
    private val revoke = "https://auth.x.ai/oauth2/revoke"

    private class FakeStore(var current: StoredXaiCredential?, val events: MutableList<String>) : XaiTokenStorePort {
        var saves = 0
        var deletes = 0
        override fun load(): StoredXaiCredential? = current
        override fun save(c: StoredXaiCredential) {
            saves++
            events += "save:${c.tokens.accessToken}"
            current = c
        }
        override fun delete() {
            deletes++
            events += "delete"
            current = null
        }
    }

    private class FakeRefresher(
        val events: MutableList<String>,
        var gate: CompletableDeferred<Unit>? = null,
        var next: () -> RefreshCall,
    ) : XaiRefresher {
        var calls = 0
        val seenClientIds = mutableListOf<String>()
        val seenRefreshTokens = mutableListOf<String>()
        override suspend fun refresh(tokenEndpoint: String, clientId: String, refreshToken: String): RefreshCall {
            calls++
            seenClientIds += clientId
            seenRefreshTokens += refreshToken
            gate?.await()
            events += "refresh"
            return next()
        }
    }

    private fun stored(access: String, refresh: String = "rt-old", expiresAt: Long? = 0L, label: String? = "a@example.com") =
        StoredXaiCredential(XaiTokens(access, refresh, expiresAt, label), endpoint, revoke)

    private fun ok(access: String, refresh: String? = "rt-new", label: String? = null) =
        RefreshCall.Ok(TokenParse.Ok(access, refresh, 10_000_000L, label))

    private class Harness(store: StoredXaiCredential?, next: () -> RefreshCall) {
        val events = mutableListOf<String>()
        val store = FakeStore(store, events)
        val refresher = FakeRefresher(events, next = next)
        var reloginCallbacks = 0
        val coordinator = XaiCredentialCoordinator(
            store = this.store,
            refresher = refresher,
            clientId = { "test-client-id" },
            nowWallMs = { 1_000L },
            onReloginRequired = { reloginCallbacks++ },
        )
    }

    @Test
    fun notSignedInWithoutCredential() = runTest {
        val h = Harness(null) { error("no refresh") }
        assertEquals(CredentialResult.NotSignedIn, h.coordinator.accessToken())
        assertEquals(0, h.refresher.calls)
    }

    @Test
    fun freshTokenIsUsedWithoutRefresh() = runTest {
        val h = Harness(stored("at-1", expiresAt = 1_000L + TOKEN_REFRESH_WINDOW_MS + 1)) { error("no refresh") }
        assertEquals(CredentialResult.Token("at-1"), h.coordinator.accessToken())
        val unknownExpiry = Harness(stored("at-1", expiresAt = null)) { error("no refresh") }
        assertEquals(CredentialResult.Token("at-1"), unknownExpiry.coordinator.accessToken())
    }

    @Test
    fun concurrentCallersShareOneRefresh() = runTest {
        val h = Harness(stored("at-old")) { ok("at-new") }
        val gate = CompletableDeferred<Unit>()
        h.refresher.gate = gate
        val a = async { h.coordinator.accessToken() }
        val b = async { h.coordinator.accessToken() }
        testScheduler.advanceUntilIdle()
        gate.complete(Unit)
        assertEquals(CredentialResult.Token("at-new"), a.await())
        assertEquals(CredentialResult.Token("at-new"), b.await())
        assertEquals(1, h.refresher.calls)
        assertEquals(listOf("test-client-id"), h.refresher.seenClientIds)
        assertEquals(listOf("rt-old"), h.refresher.seenRefreshTokens)
    }

    @Test
    fun saveHappensBeforeTokenIsReturned() = runTest {
        val h = Harness(stored("at-old")) { ok("at-new") }
        val result = h.coordinator.accessToken()
        h.events += "returned"
        assertEquals(CredentialResult.Token("at-new"), result)
        assertEquals(listOf("refresh", "save:at-new", "returned"), h.events)
    }

    @Test
    fun missingRefreshTokenKeepsOldAndIdTokenUpdatesLabel() = runTest {
        val h = Harness(stored("at-old", refresh = "rt-old", label = "a@example.com")) { ok("at-new", refresh = null) }
        h.coordinator.accessToken()
        assertEquals("rt-old", h.store.current?.tokens?.refreshToken)
        assertEquals("a@example.com", h.store.current?.tokens?.accountLabel)
        assertEquals(endpoint, h.store.current?.tokenEndpoint)
        assertEquals(revoke, h.store.current?.revocationEndpoint)

        val h2 = Harness(stored("at-old", label = "a@example.com")) { ok("at-new", refresh = "rt-2", label = "b@example.com") }
        h2.coordinator.accessToken()
        assertEquals("rt-2", h2.store.current?.tokens?.refreshToken)
        assertEquals("b@example.com", h2.store.current?.tokens?.accountLabel)
    }

    @Test
    fun unauthorizedForcesOneRefreshAndOneResend() = runTest {
        val h = Harness(stored("at-1", expiresAt = null)) { ok("at-2") }
        val sent = mutableListOf<String>()
        val value = withXaiToken(h.coordinator) { token ->
            sent += token
            if (token == "at-1") AuthAttempt.Unauthorized else AuthAttempt.Done("image")
        }
        assertEquals("image", value)
        assertEquals(listOf("at-1", "at-2"), sent)
        assertEquals(1, h.refresher.calls)
    }

    @Test
    fun secondUnauthorizedFailsAsRejected() = runTest {
        val h = Harness(stored("at-1", expiresAt = null)) { ok("at-2") }
        var calls = 0
        try {
            withXaiToken(h.coordinator) { _ ->
                calls++
                AuthAttempt.Unauthorized
            }
            fail("expected rejection")
        } catch (e: XaiCredentialException) {
            assertEquals(XAI_MSG_REJECTED, e.message)
            assertEquals(CredentialResult.Rejected, e.result)
        }
        assertEquals(2, calls)
        assertEquals(1, h.refresher.calls)
    }

    @Test
    fun rejectedTokenAlreadyReplacedIsNotRefreshedAgain() = runTest {
        val h = Harness(stored("at-2", expiresAt = null)) { error("no refresh") }
        assertEquals(CredentialResult.Token("at-2"), h.coordinator.accessToken(rejected = "at-1"))
        assertEquals(0, h.refresher.calls)
    }

    @Test
    fun reloginRequiredDeletesAndNotifies() = runTest {
        for (failure in listOf(RefreshCall.Failed(400, "invalid_grant"), RefreshCall.Failed(401, null))) {
            val h = Harness(stored("at-old")) { failure }
            assertEquals(CredentialResult.ReloginRequired, h.coordinator.accessToken())
            assertNull(h.store.current)
            assertEquals(1, h.store.deletes)
            assertEquals(1, h.reloginCallbacks)
        }
    }

    @Test
    fun transientKeepsTokens() = runTest {
        for (failure in listOf(RefreshCall.Failed(429, null), RefreshCall.Failed(null, null))) {
            val h = Harness(stored("at-old")) { failure }
            assertEquals(CredentialResult.Transient, h.coordinator.accessToken())
            assertNotNull(h.store.current)
            assertEquals(0, h.store.deletes)
            assertEquals(0, h.reloginCallbacks)
        }
        val thrown = Harness(stored("at-old")) { throw java.io.IOException("offline") }
        assertEquals(CredentialResult.Transient, thrown.coordinator.accessToken())
    }

    @Test
    fun rejectedKeepsTokens() = runTest {
        for (failure in listOf(RefreshCall.Failed(403, null), RefreshCall.Failed(400, "invalid_client"))) {
            val h = Harness(stored("at-old")) { failure }
            assertEquals(CredentialResult.Rejected, h.coordinator.accessToken())
            assertNotNull(h.store.current)
            assertEquals(0, h.store.deletes)
            assertEquals(0, h.reloginCallbacks)
        }
    }

    @Test
    fun credentialFailuresMapToMessages() = runTest {
        val h = Harness(null) { error("no refresh") }
        try {
            withXaiToken(h.coordinator) { AuthAttempt.Done(Unit) }
            fail("expected not signed in")
        } catch (e: XaiCredentialException) {
            assertEquals(XAI_MSG_NOT_SIGNED_IN, e.message)
        }
        try {
            withXaiToken(h.coordinator, messageFor = { if (it == CredentialResult.NotSignedIn) ID_MASK_MSG_NOT_SIGNED_IN else "" }) {
                AuthAttempt.Done(Unit)
            }
            fail("expected not signed in")
        } catch (e: XaiCredentialException) {
            assertEquals(ID_MASK_MSG_NOT_SIGNED_IN, e.message)
        }
        assertEquals(XAI_MSG_RELOGIN, xaiCredentialMessage(CredentialResult.ReloginRequired))
        assertEquals(XAI_MSG_REFRESH_NETWORK, xaiCredentialMessage(CredentialResult.Transient))
    }

    @Test
    fun takeForSignOutReturnsCopyAndDeletes() = runTest {
        val h = Harness(stored("at-1", refresh = "rt-1")) { error("no refresh") }
        val taken = h.coordinator.takeForSignOut()
        assertEquals("rt-1", taken?.tokens?.refreshToken)
        assertEquals(revoke, taken?.revocationEndpoint)
        assertNull(h.store.current)
        assertEquals(CredentialResult.NotSignedIn, h.coordinator.accessToken())
    }

    @Test
    fun publicTypesDoNotLeakTokens() {
        assertFalse(CredentialResult.Token("at-secret").toString().contains("at-secret"))
        assertFalse(stored("at-secret", refresh = "rt-secret").toString().contains("secret"))
        assertTrue(stored("at-secret").toString().contains(endpoint))
    }
}
