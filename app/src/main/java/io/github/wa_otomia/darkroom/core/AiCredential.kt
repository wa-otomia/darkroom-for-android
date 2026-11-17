package io.github.wa_otomia.darkroom.core

/** How Grok authenticates (AIPV010-R04). OpenAI is always [API_KEY]. */
enum class AiAuthMode(val id: String) {
    API_KEY("api_key"),
    SUBSCRIPTION("subscription"),
    ;

    companion object {
        /** Unknown or missing values (including data from before this setting existed) give [API_KEY]. */
        fun fromId(raw: String?): AiAuthMode =
            entries.firstOrNull { it.id == raw?.trim() } ?: API_KEY
    }
}

enum class AiCredentialState { READY, NO_KEY, NOT_SIGNED_IN, RELOGIN_REQUIRED }

fun effectiveAuthMode(provider: AiProvider, stored: AiAuthMode): AiAuthMode =
    if (provider == AiProvider.GROK) stored else AiAuthMode.API_KEY

/**
 * The AI readiness gate. No silent fallback between modes: a stored API key never makes the
 * subscription mode ready, and the re-login flag only matters while no credential is stored.
 */
fun credentialState(
    provider: AiProvider,
    mode: AiAuthMode,
    apiKey: String,
    hasSubscriptionCredential: Boolean,
    reloginRequired: Boolean,
): AiCredentialState = when (effectiveAuthMode(provider, mode)) {
    AiAuthMode.API_KEY -> if (apiKey.isNotBlank()) AiCredentialState.READY else AiCredentialState.NO_KEY
    AiAuthMode.SUBSCRIPTION -> when {
        hasSubscriptionCredential -> AiCredentialState.READY
        reloginRequired -> AiCredentialState.RELOGIN_REQUIRED
        else -> AiCredentialState.NOT_SIGNED_IN
    }
}

// Stable messages matched by UserErrors. None of them may contain "未配置 AI", which the older
// error_grok_key rule matches.
const val ID_MASK_MSG_NOT_SIGNED_IN = "抠图失败：xAI 订阅未登录"
const val ID_MASK_MSG_RELOGIN = "抠图失败：xAI 订阅需要重新登录"
const val XAI_MSG_NOT_SIGNED_IN = "xAI 订阅未登录。去设置页登录后再试。"
const val XAI_MSG_RELOGIN = "xAI 订阅登录已失效，请重新登录。"
const val XAI_MSG_REJECTED = "xAI 订阅凭据被拒绝，请稍后重试或重新登录。"
const val XAI_MSG_REFRESH_NETWORK = "xAI 订阅凭据刷新失败：网络不可用或服务暂时不可用。"
const val XAI_MSG_FORBIDDEN = "xAI 拒绝了订阅请求（403）：订阅层级不支持，或该账号或 client ID 未获 API 访问授权；可改用 API key。"
const val XAI_MSG_QUOTA = "xAI 订阅用量已用尽或受限（429）。"

/** Message for an AI gate that is not [AiCredentialState.READY]; [action] is e.g. "修图" or "生成". */
fun aiNotReadyMessage(state: AiCredentialState, action: String): String = when (state) {
    AiCredentialState.NO_KEY -> "未配置 AI 钥匙。去设置页填入 API key 后再$action。"
    AiCredentialState.NOT_SIGNED_IN -> XAI_MSG_NOT_SIGNED_IN
    AiCredentialState.RELOGIN_REQUIRED -> XAI_MSG_RELOGIN
    AiCredentialState.READY -> ""
}

/** The ID-photo mask variant of [aiNotReadyMessage] (EDID040-R02). */
fun idMaskNotReadyMessage(state: AiCredentialState): String = when (state) {
    AiCredentialState.NO_KEY -> ID_MASK_MSG_NEED_KEY
    AiCredentialState.NOT_SIGNED_IN -> ID_MASK_MSG_NOT_SIGNED_IN
    AiCredentialState.RELOGIN_REQUIRED -> ID_MASK_MSG_RELOGIN
    AiCredentialState.READY -> ""
}

/** Non-secret subscription facts carried into the public AI state. Never holds a token. */
data class SubscriptionSnapshot(
    val mode: AiAuthMode,
    val clientId: String,
    val hasCredential: Boolean,
    val reloginRequired: Boolean,
    val accountLabel: String?,
)
