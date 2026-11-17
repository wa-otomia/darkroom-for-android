package io.github.wa_otomia.darkroom.ui

import io.github.wa_otomia.darkroom.R
import io.github.wa_otomia.darkroom.core.AiCredentialState
import io.github.wa_otomia.darkroom.core.ID_MASK_MSG_NOT_SIGNED_IN
import io.github.wa_otomia.darkroom.core.ID_MASK_MSG_RELOGIN
import io.github.wa_otomia.darkroom.core.LoginFailure
import io.github.wa_otomia.darkroom.core.XAI_MSG_FORBIDDEN
import io.github.wa_otomia.darkroom.core.XAI_MSG_NOT_SIGNED_IN
import io.github.wa_otomia.darkroom.core.XAI_MSG_QUOTA
import io.github.wa_otomia.darkroom.core.XAI_MSG_REFRESH_NETWORK
import io.github.wa_otomia.darkroom.core.XAI_MSG_REJECTED
import io.github.wa_otomia.darkroom.core.XAI_MSG_RELOGIN
import io.github.wa_otomia.darkroom.core.aiNotReadyMessage
import io.github.wa_otomia.darkroom.core.idMaskNotReadyMessage
import io.github.wa_otomia.darkroom.ui.settings.failureText
import io.github.wa_otomia.darkroom.ui.settings.formatRemaining
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UserErrorsXaiTest {
    @Test
    fun everyXaiMessageHasItsOwnString() {
        assertEquals(R.string.error_xai_not_signed_in, userErrorRes(XAI_MSG_NOT_SIGNED_IN))
        assertEquals(R.string.error_xai_relogin, userErrorRes(XAI_MSG_RELOGIN))
        assertEquals(R.string.error_xai_rejected, userErrorRes(XAI_MSG_REJECTED))
        assertEquals(R.string.error_xai_refresh_network, userErrorRes(XAI_MSG_REFRESH_NETWORK))
        assertEquals(R.string.error_xai_forbidden, userErrorRes(XAI_MSG_FORBIDDEN))
        assertEquals(R.string.error_xai_quota, userErrorRes(XAI_MSG_QUOTA))
    }

    @Test
    fun maskSubscriptionMessagesMapToTheSubscriptionStrings() {
        assertEquals(R.string.error_xai_not_signed_in, userErrorRes(ID_MASK_MSG_NOT_SIGNED_IN))
        assertEquals(R.string.error_xai_relogin, userErrorRes(ID_MASK_MSG_RELOGIN))
        assertEquals(R.string.error_xai_not_signed_in, userErrorRes(idMaskNotReadyMessage(AiCredentialState.NOT_SIGNED_IN)))
        assertEquals(R.string.error_xai_relogin, userErrorRes(idMaskNotReadyMessage(AiCredentialState.RELOGIN_REQUIRED)))
        assertEquals(R.string.id_error_mask_need_key, userErrorRes(idMaskNotReadyMessage(AiCredentialState.NO_KEY)))
    }

    @Test
    fun gateMessagesAreNotClaimedByTheKeyRule() {
        assertEquals(R.string.error_xai_not_signed_in, userErrorRes(aiNotReadyMessage(AiCredentialState.NOT_SIGNED_IN, "修图")))
        assertEquals(R.string.error_xai_relogin, userErrorRes(aiNotReadyMessage(AiCredentialState.RELOGIN_REQUIRED, "生成")))
        assertEquals(R.string.error_grok_key, userErrorRes(aiNotReadyMessage(AiCredentialState.NO_KEY, "修图")))
    }

    @Test
    fun wrappedMessagesStillMapAndShowNoRawDetail() {
        assertEquals(R.string.error_xai_forbidden, userErrorRes("java.lang.IllegalStateException: $XAI_MSG_FORBIDDEN"))
        assertEquals(R.string.error_xai_quota, userErrorRes("  $XAI_MSG_QUOTA\n"))
        assertNull(userErrorDetail(XAI_MSG_REJECTED))
    }

    @Test
    fun existingAiRulesUnchanged() {
        assertEquals(R.string.ds_error_grok_auth, userErrorRes("HTTP 403 Forbidden"))
        assertEquals(R.string.ds_error_grok_rate_limit, userErrorRes("Rate limit exceeded"))
        assertEquals(R.string.error_grok_key, userErrorRes("未配置 AI 密钥"))
    }

    @Test
    fun everyLoginFailureHasItsOwnText() {
        val texts = LoginFailure.entries.map(::failureText)
        assertEquals(LoginFailure.entries.size, texts.toSet().size)
        assertEquals(R.string.settings_xai_fail_invalid_client, failureText(LoginFailure.INVALID_CLIENT))
        assertEquals(R.string.settings_xai_fail_unsupported, failureText(LoginFailure.UNSUPPORTED))
    }

    @Test
    fun countdownRoundsUpAndNeverGoesNegative() {
        assertEquals("15:00", formatRemaining(900_000))
        assertEquals("10:00", formatRemaining(599_001))
        assertEquals("0:01", formatRemaining(1))
        assertEquals("0:00", formatRemaining(0))
        assertEquals("0:00", formatRemaining(-5_000))
    }
}
