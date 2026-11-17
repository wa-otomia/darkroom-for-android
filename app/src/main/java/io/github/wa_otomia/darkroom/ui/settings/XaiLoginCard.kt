package io.github.wa_otomia.darkroom.ui.settings

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PersistableBundle
import android.os.SystemClock
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.wa_otomia.darkroom.R
import io.github.wa_otomia.darkroom.core.LoginFailure
import io.github.wa_otomia.darkroom.core.VerificationUriCheck
import io.github.wa_otomia.darkroom.core.checkVerificationUri
import io.github.wa_otomia.darkroom.data.ai.XaiCredentials
import io.github.wa_otomia.darkroom.data.ai.XaiLoginCoordinator
import io.github.wa_otomia.darkroom.data.ai.XaiLoginUiState
import io.github.wa_otomia.darkroom.ui.components.GhostButton
import io.github.wa_otomia.darkroom.ui.components.PaperButton
import io.github.wa_otomia.darkroom.ui.theme.Amber
import io.github.wa_otomia.darkroom.ui.theme.MonoFont
import io.github.wa_otomia.darkroom.ui.theme.Paper
import io.github.wa_otomia.darkroom.ui.theme.PaperDim
import io.github.wa_otomia.darkroom.ui.theme.PaperHairline
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.delay
import java.util.Locale

/** The app-wide xAI sign-in singletons, reached from Compose without widening the nav graph's signature. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface XaiUiEntryPoint {
    fun xaiLogin(): XaiLoginCoordinator
    fun xaiCredentials(): XaiCredentials
}

@Composable
internal fun rememberXaiUi(): XaiUiEntryPoint {
    val app = LocalContext.current.applicationContext
    return remember(app) { EntryPointAccessors.fromApplication(app, XaiUiEntryPoint::class.java) }
}

/**
 * Grok subscription sign-in card (AIPV050-R01..R06, R10). Everything shown comes from the app-wide
 * [XaiLoginUiState] and the public AI state; nothing here goes into saved-instance state, so a
 * recreated screen redraws the same code and countdown from memory.
 *
 * The start button is a plain app-themed button: no xAI or Grok mark, never styled as an official
 * "Sign in with" button. Side-effect free apart from the callbacks.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun XaiLoginCard(
    state: XaiLoginUiState,
    clientIdSaved: Boolean,
    signedIn: Boolean,
    accountLabel: String?,
    reloginRequired: Boolean,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    onSignOut: () -> Unit,
    onOpenUri: (String) -> Unit,
    onCopyCode: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .border(1.dp, PaperHairline, RoundedCornerShape(2.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // R01: always above the start button.
        Note(stringResource(R.string.settings_xai_experimental_notice), Amber)
        Note(stringResource(R.string.settings_xai_responsibility))
        Note(stringResource(R.string.settings_xai_billing_notice))

        when {
            state is XaiLoginUiState.Starting -> {
                Note(stringResource(R.string.settings_xai_starting), Paper)
                GhostButton(
                    text = stringResource(R.string.settings_xai_cancel_login),
                    fillMaxWidth = false,
                    onClick = onCancel,
                )
            }
            state is XaiLoginUiState.AwaitingUser -> AwaitingUser(state, onCancel, onOpenUri, onCopyCode)
            signedIn -> {
                Text(
                    if (accountLabel.isNullOrBlank()) {
                        stringResource(R.string.settings_xai_signed_in_unknown)
                    } else {
                        stringResource(R.string.settings_xai_signed_in, accountLabel)
                    },
                    color = Paper,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
                GhostButton(
                    text = stringResource(R.string.settings_xai_sign_out),
                    fillMaxWidth = false,
                    onClick = onSignOut,
                )
            }
            else -> {
                val status = when (state) {
                    is XaiLoginUiState.Failed -> stringResource(failureText(state.reason))
                    XaiLoginUiState.Cancelled -> stringResource(R.string.settings_xai_login_cancelled)
                    XaiLoginUiState.Interrupted -> stringResource(R.string.settings_xai_login_interrupted)
                    else -> if (reloginRequired) stringResource(R.string.settings_xai_relogin_required) else null
                }
                if (status != null) Note(status, Amber)
                if (!clientIdSaved) Note(stringResource(R.string.settings_xai_client_id_required))
                PaperButton(
                    text = stringResource(R.string.settings_xai_start_login),
                    enabled = clientIdSaved,
                    fillMaxWidth = false,
                    onClick = onStart,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AwaitingUser(
    state: XaiLoginUiState.AwaitingUser,
    onCancel: () -> Unit,
    onOpenUri: (String) -> Unit,
    onCopyCode: (String) -> Unit,
) {
    val label = stringResource(R.string.settings_xai_code_label)
    val hyphen = stringResource(R.string.settings_xai_code_hyphen)
    // Read one character at a time: "W, D, J, B, hyphen, …".
    val spoken = state.userCode.map { if (it == '-') hyphen else it.toString() }.joinToString(", ")
    Text(label, color = PaperDim, fontSize = 13.sp)
    SelectionContainer {
        Text(
            state.userCode,
            color = Paper,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = MonoFont,
                textDirection = TextDirection.Ltr,
            ),
            modifier = Modifier.semantics { contentDescription = "$label: $spoken" },
        )
    }
    Note(stringResource(R.string.settings_xai_verify_at), Paper)
    SelectionContainer {
        Text(
            state.verificationUri,
            color = Paper,
            style = TextStyle(fontFamily = MonoFont, fontSize = 14.sp, lineHeight = 20.sp, textDirection = TextDirection.Ltr),
        )
    }
    if (!state.openable) Note(stringResource(R.string.settings_xai_verify_unlisted_host), Amber)

    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(state.deadlineElapsedMs) {
        while (true) {
            now = SystemClock.elapsedRealtime()
            if (now >= state.deadlineElapsedMs) break
            delay(1_000)
        }
    }
    Note(stringResource(R.string.settings_xai_time_left, formatRemaining(state.deadlineElapsedMs - now)))
    Note(stringResource(R.string.settings_xai_only_if_you_started), Amber)

    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (state.openable) {
            PaperButton(
                text = stringResource(R.string.settings_xai_open_login_page),
                fillMaxWidth = false,
                onClick = { onOpenUri(state.verificationUri) },
            )
        }
        GhostButton(
            text = stringResource(R.string.settings_xai_copy_code),
            fillMaxWidth = false,
            onClick = { onCopyCode(state.userCode) },
        )
        GhostButton(
            text = stringResource(R.string.settings_xai_cancel_login),
            fillMaxWidth = false,
            onClick = onCancel,
        )
    }
}

@Composable
private fun Note(text: String, color: androidx.compose.ui.graphics.Color = PaperDim) {
    Text(text, color = color, fontSize = 13.sp, lineHeight = 18.sp)
}

/** Remaining time as m:ss, rounded up so a 900 s code starts at 15:00. */
internal fun formatRemaining(remainingMs: Long): String {
    val totalS = (remainingMs.coerceAtLeast(0L) + 999L) / 1000L
    return String.format(Locale.ROOT, "%d:%02d", totalS / 60, totalS % 60)
}

internal fun failureText(reason: LoginFailure): Int = when (reason) {
    LoginFailure.INVALID_CLIENT -> R.string.settings_xai_fail_invalid_client
    LoginFailure.FORBIDDEN -> R.string.settings_xai_fail_forbidden
    LoginFailure.UNAVAILABLE -> R.string.settings_xai_fail_unavailable
    LoginFailure.TEMPORARY -> R.string.settings_xai_fail_temporary
    LoginFailure.MALFORMED -> R.string.settings_xai_fail_malformed
    LoginFailure.DENIED -> R.string.settings_xai_fail_denied
    LoginFailure.EXPIRED -> R.string.settings_xai_fail_expired
    LoginFailure.FAILED -> R.string.settings_xai_fail_failed
    LoginFailure.UNSUPPORTED -> R.string.settings_xai_fail_unsupported
}

/**
 * Opens only the plain verification_uri (never verification_uri_complete) and only for an
 * allow-listed https x.ai host: Custom Tab first, then the system browser.
 * @return false when nothing could open it.
 */
internal fun openXaiVerificationPage(context: Context, uri: String): Boolean {
    if (checkVerificationUri(uri) != VerificationUriCheck.OPENABLE) return false
    val parsed = Uri.parse(uri)
    try {
        CustomTabsIntent.Builder().build().launchUrl(context, parsed)
        return true
    } catch (_: ActivityNotFoundException) {
        // No Custom Tabs browser; fall through to a plain browser.
    } catch (_: SecurityException) {
    }
    return try {
        context.startActivity(Intent(Intent.ACTION_VIEW, parsed).addCategory(Intent.CATEGORY_BROWSABLE))
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}

/**
 * Copies only the user code, flagged sensitive so the system clipboard preview hides it.
 * @return true when the app should confirm the copy itself (Android 12L and below; 13+ shows its own).
 */
internal fun copyXaiUserCode(context: Context, code: String): Boolean {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return false
    val clip = ClipData.newPlainText("", code)
    clip.description.extras = PersistableBundle().apply {
        val key = if (Build.VERSION.SDK_INT >= 33) ClipDescription.EXTRA_IS_SENSITIVE else "android.content.extra.IS_SENSITIVE"
        putBoolean(key, true)
    }
    clipboard.setPrimaryClip(clip)
    return Build.VERSION.SDK_INT < 33
}
