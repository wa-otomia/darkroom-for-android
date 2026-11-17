package io.github.wa_otomia.darkroom.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class XaiAuthTest {
    private val clientId = "test-client-id"

    private fun fakeJwt(payloadJson: String): String {
        val enc = Base64.getUrlEncoder().withoutPadding()
        val header = enc.encodeToString("""{"alg":"none"}""".toByteArray())
        val payload = enc.encodeToString(payloadJson.toByteArray())
        return "$header.$payload.c2lnbmF0dXJl"
    }

    private fun discovery(
        issuer: String = "https://auth.x.ai",
        device: String? = "https://auth.x.ai/oauth2/device/code",
        token: String = "https://auth.x.ai/oauth2/token",
        revocation: String? = "https://auth.x.ai/oauth2/revoke",
        grants: String? = """["authorization_code","refresh_token","urn:ietf:params:oauth:grant-type:device_code"]""",
    ): String = buildString {
        append("{\"issuer\":\"").append(issuer).append('"')
        append(",\"authorization_endpoint\":\"https://auth.x.ai/oauth2/auth\"")
        if (device != null) append(",\"device_authorization_endpoint\":\"").append(device).append('"')
        append(",\"token_endpoint\":\"").append(token).append('"')
        if (revocation != null) append(",\"revocation_endpoint\":\"").append(revocation).append('"')
        if (grants != null) append(",\"grant_types_supported\":").append(grants)
        append('}')
    }

    // --- discovery -----------------------------------------------------------------------------

    @Test
    fun discoveryRealShapeWithoutRegistrationEndpoint() {
        val ep = parseXaiDiscovery(discovery())
        assertEquals(
            XaiEndpoints(
                "https://auth.x.ai/oauth2/device/code",
                "https://auth.x.ai/oauth2/token",
                "https://auth.x.ai/oauth2/revoke",
            ),
            ep,
        )
        assertEquals("https://auth.x.ai/oauth2/token", parseXaiDiscovery(discovery(issuer = "https://auth.x.ai/"))?.token)
        assertEquals("https://auth.x.ai/oauth2/token", parseXaiDiscovery(discovery(grants = null))?.token)
    }

    @Test
    fun discoveryRejectsWrongIssuerHostsAndGrants() {
        assertNull(parseXaiDiscovery(discovery(issuer = "https://accounts.x.ai")))
        assertNull(parseXaiDiscovery(discovery(device = "https://sub.auth.x.ai/device")))
        assertNull(parseXaiDiscovery(discovery(device = "https://evil.example/device")))
        assertNull(parseXaiDiscovery(discovery(token = "http://auth.x.ai/oauth2/token")))
        assertNull(parseXaiDiscovery(discovery(token = "https://auth.x.ai:8443/oauth2/token")))
        assertNull(parseXaiDiscovery(discovery(device = null)))
        assertNull(parseXaiDiscovery(discovery(grants = """["authorization_code","refresh_token"]""")))
        assertNull(parseXaiDiscovery("not json"))
    }

    @Test
    fun discoveryBadRevocationOnlyDropsRevocation() {
        val ep = parseXaiDiscovery(discovery(revocation = "https://evil.example/revoke"))
        assertEquals("https://auth.x.ai/oauth2/device/code", ep?.deviceAuthorization)
        assertNull(ep?.revocation)
        assertNull(parseXaiDiscovery(discovery(revocation = null))?.revocation)
    }

    @Test
    fun authEndpointAndApiHostChecks() {
        assertTrue(isAllowedAuthEndpoint("https://auth.x.ai/oauth2/token"))
        assertTrue(isAllowedAuthEndpoint("https://AUTH.X.AI:443/oauth2/token"))
        assertFalse(isAllowedAuthEndpoint("https://user@auth.x.ai/oauth2/token"))
        assertFalse(isAllowedAuthEndpoint("https://auth.x.ai/oauth2/token#frag"))
        assertFalse(isAllowedAuthEndpoint("https://auth.x.ai.evil.com/token"))
        assertFalse(isAllowedAuthEndpoint("::not a url"))
        assertTrue(isAllowedApiUrl("$XAI_API_BASE/images/edits"))
        assertFalse(isAllowedApiUrl("https://proxy.example.com/v1/images/edits"))
    }

    // --- device authorization ------------------------------------------------------------------

    @Test
    fun verificationUriChecks() {
        assertEquals(VerificationUriCheck.OPENABLE, checkVerificationUri("https://accounts.x.ai/device"))
        assertEquals(VerificationUriCheck.OPENABLE, checkVerificationUri("https://x.ai/device"))
        assertEquals(VerificationUriCheck.DISPLAY_ONLY, checkVerificationUri("https://grok.com/device"))
        assertEquals(VerificationUriCheck.DISPLAY_ONLY, checkVerificationUri("https://x.ai.evil.com/device"))
        assertEquals(VerificationUriCheck.REJECT, checkVerificationUri("http://example.com/device"))
        assertEquals(VerificationUriCheck.REJECT, checkVerificationUri("javascript:alert(1)"))
    }

    @Test
    fun userCodeCharacters() {
        assertTrue(isValidUserCode("WDJB-MJHT"))
        assertTrue(isValidUserCode("ABCD-EFGH"))
        assertFalse(isValidUserCode("ABCD EFGH<"))
        assertFalse(isValidUserCode(""))
        assertFalse(isValidUserCode("ＡＢＣＤ"))
    }

    @Test
    fun deviceAuthorizationIgnoresCompleteUriAndAcceptsStringNumbers() {
        val body = """{"device_code":"dc-1","user_code":"ABCD-EFGH","verification_uri":"https://accounts.x.ai/device",
            "verification_uri_complete":"https://accounts.x.ai/device?user_code=ABCD-EFGH","expires_in":"900","interval":"7"}"""
        val ok = parseDeviceAuthorization(body) as DeviceAuthParse.Ok
        assertEquals("https://accounts.x.ai/device", ok.auth.verificationUri)
        assertEquals("ABCD-EFGH", ok.auth.userCode)
        assertEquals(900L, ok.auth.expiresInS)
        assertEquals(7L, ok.auth.intervalS)
        assertEquals(VerificationUriCheck.OPENABLE, ok.uriCheck)
        assertFalse(ok.auth.toString().contains("ABCD-EFGH"))
        assertFalse(ok.auth.toString().contains("dc-1"))
    }

    @Test
    fun deviceAuthorizationMalformedCases() {
        val base = """"device_code":"dc-1","verification_uri":"https://accounts.x.ai/device""""
        assertEquals(DeviceAuthParse.Malformed, parseDeviceAuthorization("""{$base,"user_code":"ABCD EFGH<"}"""))
        assertEquals(DeviceAuthParse.Malformed, parseDeviceAuthorization("""{$base}"""))
        assertEquals(
            DeviceAuthParse.Malformed,
            parseDeviceAuthorization("""{"device_code":"dc-1","user_code":"ABCD-EFGH","verification_uri":"http://example.com/device"}"""),
        )
        assertEquals(DeviceAuthParse.Malformed, parseDeviceAuthorization("<html>"))
        val displayOnly = parseDeviceAuthorization(
            """{"device_code":"dc-1","user_code":"ABCD-EFGH","verification_uri":"https://grok.com/device","interval":0}""",
        ) as DeviceAuthParse.Ok
        assertEquals(VerificationUriCheck.DISPLAY_ONLY, displayOnly.uriCheck)
        assertNull(displayOnly.auth.intervalS)
    }

    @Test
    fun deviceCodeHttpErrors() {
        assertEquals(LoginFailure.INVALID_CLIENT, classifyDeviceCodeHttpError(400, "invalid_client"))
        assertEquals(LoginFailure.INVALID_CLIENT, classifyDeviceCodeHttpError(401, "invalid_client"))
        assertEquals(LoginFailure.FORBIDDEN, classifyDeviceCodeHttpError(403, null))
        assertEquals(LoginFailure.UNAVAILABLE, classifyDeviceCodeHttpError(404, null))
        assertEquals(LoginFailure.TEMPORARY, classifyDeviceCodeHttpError(429, null))
        assertEquals(LoginFailure.TEMPORARY, classifyDeviceCodeHttpError(503, null))
        assertEquals(LoginFailure.TEMPORARY, classifyDeviceCodeHttpError(null, null))
        assertEquals(LoginFailure.FAILED, classifyDeviceCodeHttpError(302, null))
    }

    @Test
    fun oauthErrorCodeReadsOnlyErrorField() {
        assertEquals("invalid_client", parseOAuthErrorCode("""{"error":"invalid_client","error_description":"secret detail"}"""))
        assertNull(parseOAuthErrorCode("""{"error_description":"secret detail"}"""))
        assertNull(parseOAuthErrorCode(null))
        assertNull(parseOAuthErrorCode("oops"))
    }

    // --- polling -------------------------------------------------------------------------------

    private fun auth(expiresIn: Long? = 900, interval: Long? = 5) =
        DeviceAuthorization("dc-1", "ABCD-EFGH", "https://accounts.x.ai/device", expiresIn, interval)

    private fun waitOf(step: PollStep): PollStep.Wait = step as PollStep.Wait

    @Test
    fun firstPollWaitsOneInterval() {
        val s = initialPollState(auth(), nowMs = 1_000)
        assertEquals(5_000, s.intervalMs)
        assertEquals(1_000 + 900_000, s.deadlineMs)
        assertEquals(5_000, waitOf(firstPollStep(s, 1_000)).delayMs)
        val defaults = initialPollState(auth(expiresIn = null, interval = null), nowMs = 0)
        assertEquals(DEVICE_DEFAULT_INTERVAL_MS, defaults.intervalMs)
        assertEquals(600_000, defaults.deadlineMs)
    }

    @Test
    fun deadlineIsAtLeastTenMinutes() {
        assertEquals(600_000, initialPollState(auth(expiresIn = 300), 0).deadlineMs)
        assertEquals(900_000, initialPollState(auth(expiresIn = 900), 0).deadlineMs)
    }

    @Test
    fun slowDownAccumulates() {
        var s = initialPollState(auth(), 0)
        var step = waitOf(nextPollStep(s, PollOutcome.Pending, 5_000))
        assertEquals(5_000, step.delayMs)
        s = step.state
        step = waitOf(nextPollStep(s, PollOutcome.SlowDown, 10_000))
        assertEquals(10_000, step.delayMs)
        s = step.state
        step = waitOf(nextPollStep(s, PollOutcome.Pending, 20_000))
        assertEquals(10_000, step.delayMs)
        s = step.state
        step = waitOf(nextPollStep(s, PollOutcome.SlowDown, 30_000))
        assertEquals(15_000, step.delayMs)
    }

    @Test
    fun transientBacksOffToCap() {
        var s = initialPollState(auth(), 0)
        val delays = mutableListOf<Long>()
        repeat(6) {
            val w = waitOf(nextPollStep(s, PollOutcome.Transient, 1_000))
            delays += w.delayMs
            s = w.state
        }
        assertEquals(listOf(10_000L, 20_000L, 40_000L, 60_000L, 60_000L, 60_000L), delays)
        // Pending after backoff keeps the grown interval.
        assertEquals(60_000, waitOf(nextPollStep(s, PollOutcome.Pending, 1_000)).delayMs)
    }

    @Test
    fun waitIsClippedToDeadlineAndExpires() {
        val s = initialPollState(auth(expiresIn = 300), 0)
        assertEquals(2_000, waitOf(nextPollStep(s, PollOutcome.Pending, 598_000)).delayMs)
        assertEquals(PollStep.Done(LoginFailure.EXPIRED), nextPollStep(s, PollOutcome.Pending, 600_000))
        assertEquals(PollStep.Done(LoginFailure.EXPIRED), firstPollStep(s, 700_000))
    }

    @Test
    fun stopOutcomesEndPolling() {
        val s = initialPollState(auth(), 0)
        assertEquals(PollStep.Done(LoginFailure.DENIED), nextPollStep(s, PollOutcome.Stop(LoginFailure.DENIED), 1))
    }

    @Test
    fun classifyTokenPollErrors() {
        fun err(code: String) = """{"error":"$code","error_description":"x"}"""
        assertEquals(PollOutcome.Pending, classifyTokenPoll(400, err("authorization_pending"), 0))
        assertEquals(PollOutcome.SlowDown, classifyTokenPoll(400, err("slow_down"), 0))
        assertEquals(PollOutcome.Stop(LoginFailure.DENIED), classifyTokenPoll(400, err("access_denied"), 0))
        assertEquals(PollOutcome.Stop(LoginFailure.EXPIRED), classifyTokenPoll(400, err("expired_token"), 0))
        assertEquals(PollOutcome.Stop(LoginFailure.INVALID_CLIENT), classifyTokenPoll(401, err("invalid_client"), 0))
        assertEquals(PollOutcome.Stop(LoginFailure.INVALID_CLIENT), classifyTokenPoll(400, err("unauthorized_client"), 0))
        assertEquals(PollOutcome.Stop(LoginFailure.INVALID_CLIENT), classifyTokenPoll(400, err("invalid_scope"), 0))
        assertEquals(PollOutcome.Stop(LoginFailure.FAILED), classifyTokenPoll(400, err("something_new"), 0))
        assertEquals(PollOutcome.Stop(LoginFailure.FORBIDDEN), classifyTokenPoll(403, null, 0))
        assertEquals(PollOutcome.Transient, classifyTokenPoll(null, null, 0))
        assertEquals(PollOutcome.Transient, classifyTokenPoll(429, null, 0))
        assertEquals(PollOutcome.Transient, classifyTokenPoll(502, null, 0))
        assertEquals(PollOutcome.Stop(LoginFailure.FAILED), classifyTokenPoll(302, null, 0))
    }

    @Test
    fun classifyTokenPollSuccessNeedsRefreshToken() {
        val ok = classifyTokenPoll(200, """{"access_token":"at-1","refresh_token":"rt-1","expires_in":3600}""", 1_000)
        assertTrue(ok is PollOutcome.Success)
        assertEquals(1_000 + 3_600_000L, (ok as PollOutcome.Success).tokens.expiresAtMs)
        assertEquals(
            PollOutcome.Stop(LoginFailure.MALFORMED),
            classifyTokenPoll(200, """{"access_token":"at-1","expires_in":3600}""", 0),
        )
    }

    // --- tokens --------------------------------------------------------------------------------

    @Test
    fun tokenResponseRules() {
        assertEquals(TokenParse.Incomplete, parseTokenResponse("""{"access_token":"at-1"}""", 0, requireRefresh = true))
        val refreshOk = parseTokenResponse("""{"access_token":"at-1"}""", 0, requireRefresh = false) as TokenParse.Ok
        assertNull(refreshOk.refreshToken)
        assertNull(refreshOk.expiresAtMs)
        assertNull(refreshOk.accountLabel)
        assertEquals(
            TokenParse.Incomplete,
            parseTokenResponse("""{"access_token":"at-1","refresh_token":"rt","token_type":"mac"}""", 0, true),
        )
        assertTrue(parseTokenResponse("""{"access_token":"at-1","refresh_token":"rt","token_type":"BEARER"}""", 0, true) is TokenParse.Ok)
        assertEquals(TokenParse.Incomplete, parseTokenResponse("""{"access_token":""}""", 0, false))
        assertEquals(TokenParse.Incomplete, parseTokenResponse("garbage", 0, false))
    }

    @Test
    fun expiresInWinsOverJwtExp() {
        val jwt = fakeJwt("""{"exp":2000,"sub":"u"}""")
        val withExpiresIn = parseTokenResponse("""{"access_token":"$jwt","expires_in":60}""", 10_000, false) as TokenParse.Ok
        assertEquals(70_000L, withExpiresIn.expiresAtMs)
        val fromJwt = parseTokenResponse("""{"access_token":"$jwt"}""", 10_000, false) as TokenParse.Ok
        assertEquals(2_000_000L, fromJwt.expiresAtMs)
        assertNull(jwtExpiryMs("not-a-jwt"))
        assertNull(jwtExpiryMs("a.%%%.c"))
    }

    @Test
    fun accountLabelFromIdToken() {
        assertEquals("a@example.com", idTokenAccountLabel(fakeJwt("""{"email":"a@example.com","sub":"user-1"}""")))
        assertEquals("user-1", idTokenAccountLabel(fakeJwt("""{"sub":"user-1"}""")))
        assertNull(idTokenAccountLabel(fakeJwt("""{"name":"x"}""")))
        assertNull(idTokenAccountLabel(null))
        val idToken = fakeJwt("""{"email":"a@example.com"}""")
        val parsed = parseTokenResponse(
            """{"access_token":"at-1","refresh_token":"rt-1","id_token":"$idToken"}""",
            0,
            true,
        ) as TokenParse.Ok
        assertEquals("a@example.com", parsed.accountLabel)
    }

    @Test
    fun toStringNeverShowsTokens() {
        val ok = TokenParse.Ok("at-secret", "rt-secret", 1L, "a@example.com")
        assertFalse(ok.toString().contains("secret"))
        val tokens = XaiTokens("at-secret", "rt-secret", 1L, "a@example.com")
        assertFalse(tokens.toString().contains("secret"))
        assertFalse(PollOutcome.Success(ok).toString().contains("secret"))
    }

    @Test
    fun refreshWindow() {
        assertFalse(needsRefresh(null, 0))
        assertTrue(needsRefresh(TOKEN_REFRESH_WINDOW_MS, 0))
        assertFalse(needsRefresh(TOKEN_REFRESH_WINDOW_MS + 1, 0))
        assertTrue(needsRefresh(0, 1_000))
    }

    @Test
    fun refreshErrorTable() {
        assertEquals(RefreshFailure.TRANSIENT, classifyRefreshError(null, null))
        assertEquals(RefreshFailure.TRANSIENT, classifyRefreshError(429, null))
        assertEquals(RefreshFailure.TRANSIENT, classifyRefreshError(500, null))
        assertEquals(RefreshFailure.TRANSIENT, classifyRefreshError(599, null))
        assertEquals(RefreshFailure.RELOGIN_REQUIRED, classifyRefreshError(401, null))
        assertEquals(RefreshFailure.RELOGIN_REQUIRED, classifyRefreshError(401, "invalid_client"))
        assertEquals(RefreshFailure.RELOGIN_REQUIRED, classifyRefreshError(400, "invalid_grant"))
        assertEquals(RefreshFailure.REJECTED, classifyRefreshError(400, "invalid_client"))
        assertEquals(RefreshFailure.REJECTED, classifyRefreshError(400, "unsupported_grant_type"))
        assertEquals(RefreshFailure.REJECTED, classifyRefreshError(403, null))
        assertEquals(RefreshFailure.REJECTED, classifyRefreshError(200, null))
    }

    // --- forms ---------------------------------------------------------------------------------

    @Test
    fun formsContainOnlyStandardFields() {
        assertEquals(
            listOf("client_id" to clientId, "scope" to "openid offline_access api:access"),
            deviceAuthorizationForm(clientId),
        )
        assertEquals(
            listOf(
                "grant_type" to "urn:ietf:params:oauth:grant-type:device_code",
                "device_code" to "dc-1",
                "client_id" to clientId,
            ),
            devicePollForm(clientId, "dc-1"),
        )
        assertEquals(
            listOf("grant_type" to "refresh_token", "refresh_token" to "rt-1", "client_id" to clientId),
            refreshForm(clientId, "rt-1"),
        )
        assertEquals(
            listOf("token" to "rt-1", "token_type_hint" to "refresh_token", "client_id" to clientId),
            revocationForm(clientId, "rt-1"),
        )
        // The exact lists above also prove no extension parameter is ever added.
    }

    // --- sanitizer -----------------------------------------------------------------------------

    @Test
    fun sanitizerMasksTokenFieldsAndJwts() {
        val jwt = fakeJwt("""{"sub":"user-1","exp":2000}""")
        val raw = """{"refresh_token":"abc123","note":"$jwt"}"""
        val out = sanitizeAiSecrets(raw)
        assertFalse(out.contains("abc123"))
        assertFalse(out.contains(jwt))
        assertTrue(out.contains("\"refresh_token\""))
        assertTrue(out.contains("eyJ********"))
        val form = sanitizeAiSecrets("device_code=dc-secret&user_code=ABCD-EFGH&access_token=at-secret")
        assertFalse(form.contains("dc-secret"))
        assertFalse(form.contains("ABCD-EFGH"))
        assertFalse(form.contains("at-secret"))
        assertTrue(form.contains("device_code="))
        assertFalse(sanitizeAiSecrets("id_token: xyz987").contains("xyz987"))
        // Older patterns still apply.
        assertFalse(sanitizeAiSecrets("Authorization: Bearer xai-abcdefghijk").contains("xai-abcdefghijk"))
        assertTrue(sanitizeAiSecrets("修图失败 (500)") == "修图失败 (500)")
    }
}
