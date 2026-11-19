package io.github.wa_otomia.darkroom.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiCredentialTest {
    @Test
    fun apiKeyModeOnlyLooksAtKey() {
        for (provider in AiProvider.entries) {
            assertEquals(AiCredentialState.READY, credentialState(provider, AiAuthMode.API_KEY, "k", false, false))
            assertEquals(AiCredentialState.NO_KEY, credentialState(provider, AiAuthMode.API_KEY, " ", true, true))
        }
    }

    @Test
    fun openAiIgnoresSubscriptionMode() {
        assertEquals(AiAuthMode.API_KEY, effectiveAuthMode(AiProvider.OPENAI, AiAuthMode.SUBSCRIPTION))
        assertEquals(AiAuthMode.SUBSCRIPTION, effectiveAuthMode(AiProvider.GROK, AiAuthMode.SUBSCRIPTION))
        assertEquals(AiCredentialState.READY, credentialState(AiProvider.OPENAI, AiAuthMode.SUBSCRIPTION, "sk-x", false, true))
        assertEquals(AiCredentialState.NO_KEY, credentialState(AiProvider.OPENAI, AiAuthMode.SUBSCRIPTION, "", true, false))
    }

    @Test
    fun subscriptionModeNeverFallsBackToKey() {
        val sub = AiAuthMode.SUBSCRIPTION
        assertEquals(AiCredentialState.NOT_SIGNED_IN, credentialState(AiProvider.GROK, sub, "xai-key", false, false))
        assertEquals(AiCredentialState.RELOGIN_REQUIRED, credentialState(AiProvider.GROK, sub, "xai-key", false, true))
        assertEquals(AiCredentialState.READY, credentialState(AiProvider.GROK, sub, "", true, false))
        // The re-login flag only matters while no credential is stored.
        assertEquals(AiCredentialState.READY, credentialState(AiProvider.GROK, sub, "", true, true))
    }

    @Test
    fun authModeFromId() {
        assertEquals(AiAuthMode.API_KEY, AiAuthMode.fromId(null))
        assertEquals(AiAuthMode.API_KEY, AiAuthMode.fromId(""))
        assertEquals(AiAuthMode.API_KEY, AiAuthMode.fromId("oauth"))
        assertEquals(AiAuthMode.API_KEY, AiAuthMode.fromId("api_key"))
        assertEquals(AiAuthMode.SUBSCRIPTION, AiAuthMode.fromId("subscription"))
    }

    @Test
    fun notReadyMessages() {
        assertEquals("未配置 AI 钥匙。去设置页填入 API key 后再修图。", aiNotReadyMessage(AiCredentialState.NO_KEY, "修图"))
        assertEquals(XAI_MSG_NOT_SIGNED_IN, aiNotReadyMessage(AiCredentialState.NOT_SIGNED_IN, "修图"))
        assertEquals(XAI_MSG_RELOGIN, aiNotReadyMessage(AiCredentialState.RELOGIN_REQUIRED, "生成"))
        assertEquals(ID_MASK_MSG_NEED_KEY, idMaskNotReadyMessage(AiCredentialState.NO_KEY))
        assertEquals(ID_MASK_MSG_NOT_SIGNED_IN, idMaskNotReadyMessage(AiCredentialState.NOT_SIGNED_IN))
        assertEquals(ID_MASK_MSG_RELOGIN, idMaskNotReadyMessage(AiCredentialState.RELOGIN_REQUIRED))
    }

    @Test
    fun subscriptionMessagesAvoidTheApiKeyNeedle() {
        val messages = listOf(
            aiNotReadyMessage(AiCredentialState.NOT_SIGNED_IN, "修图"),
            aiNotReadyMessage(AiCredentialState.RELOGIN_REQUIRED, "修图"),
            ID_MASK_MSG_NOT_SIGNED_IN,
            ID_MASK_MSG_RELOGIN,
            XAI_MSG_REJECTED,
            XAI_MSG_REFRESH_NETWORK,
            XAI_MSG_FORBIDDEN,
            XAI_MSG_QUOTA,
        )
        for (m in messages) {
            assertFalse(m, m.contains("未配置 AI", ignoreCase = true))
            assertFalse(m, m.contains("未配置 ai"))
        }
        assertTrue(messages.toSet().size == messages.size)
    }
}
