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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
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
import io.github.wa_otomia.darkroom.ui.components.AmberTrack
import io.github.wa_otomia.darkroom.ui.components.IconCircleButton
import io.github.wa_otomia.darkroom.ui.components.NoticeCard
import io.github.wa_otomia.darkroom.ui.components.PaperButton
import io.github.wa_otomia.darkroom.ui.components.StatusPill
import io.github.wa_otomia.darkroom.ui.components.TonalButton
import io.github.wa_otomia.darkroom.ui.theme.Amber
import io.github.wa_otomia.darkroom.ui.theme.Danger
import io.github.wa_otomia.darkroom.ui.theme.MonoFont
import io.github.wa_otomia.darkroom.ui.theme.Ok
import io.github.wa_otomia.darkroom.ui.theme.PanelShape
import io.github.wa_otomia.darkroom.ui.theme.Paper
import io.github.wa_otomia.darkroom.ui.theme.PaperDim
import io.github.wa_otomia.darkroom.ui.theme.Surface2
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
 * Drawn on the 0.2.0 design system (SYUI020) inside the Surface1 "AI image processing" fold: no
 * card outline of its own, layers by fill (the code panel is Surface2). The experimental,
 * responsibility and billing notice is a titled [NoticeCard] that sits in full above the start
 * button in every state that shows it (R01); signed in and while the code is shown it is compact
 * (the title plus a "Details" toggle that expands the full text in place). Each state has at most
 * one [PaperButton] (its primary action); every other action is a [TonalButton], and copying the
 * code or an unlisted address is an [IconCircleButton]. Monospace is used only for the user code,
 * the verification address and the remaining time. Hard failures (failed, interrupted) are Danger,
 * "sign in again" and the approval caution Amber, "cancelled" PaperDim.
 *
 * The start button is the app's own primary button: no xAI or Grok mark, never styled as an
 * official "Sign in with" button. Side-effect free apart from the callbacks.
 *
 * TalkBack: each state swaps the button that was just tapped out of composition, and the flow
 * is asynchronous (the user is in the browser while the code runs down), so every state's
 * message is a polite live region: "Requesting…", the issued code (once, not the per-second
 * countdown), the failed / expired / denied / cancelled / interrupted line and "Signed in".
 */
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
    onCopyUri: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // R01: in full above the start button in every state that shows it (idle, failed,
        // cancelled, interrupted, re-login, and while requesting). Once a code is shown or the
        // account is signed in there is no start button, and the notice folds to its title.
        RiskNotice(compact = state is XaiLoginUiState.AwaitingUser || (signedIn && state !is XaiLoginUiState.Starting))

        when {
            state is XaiLoginUiState.Starting -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.settings_xai_starting),
                        color = Paper,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                    AmberTrack(percent = 0, indeterminate = true, modifier = Modifier.fillMaxWidth(), height = 3.dp)
                }
                TonalButton(
                    text = stringResource(R.string.settings_xai_cancel_login),
                    fillMaxWidth = false,
                    onClick = onCancel,
                )
            }
            state is XaiLoginUiState.AwaitingUser -> AwaitingUser(state, onCancel, onOpenUri, onCopyCode, onCopyUri)
            signedIn -> {
                // The Ok dot is the "ready" mark (never text); the label is Paper 13.40:1 on Surface2.
                StatusPill(
                    text = if (accountLabel.isNullOrBlank()) {
                        stringResource(R.string.settings_xai_signed_in_unknown)
                    } else {
                        stringResource(R.string.settings_xai_signed_in, accountLabel)
                    },
                    dot = Ok,
                    modifier = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
                )
                TonalButton(
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
                if (status != null) {
                    // Danger 10.67:1 for hard failures, Amber 8.10:1 for "sign in again",
                    // PaperDim 5.16:1 for a cancel the user chose (all on Surface1).
                    val color = when (state) {
                        is XaiLoginUiState.Failed, XaiLoginUiState.Interrupted -> Danger
                        XaiLoginUiState.Cancelled -> PaperDim
                        else -> Amber
                    }
                    StatusLine(status, color)
                }
                // With no saved client ID the button stays disabled; the reason sits under the
                // client ID field itself (SettingsScreen), not here.
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
    onCopyUri: (String) -> Unit,
) {
    val label = stringResource(R.string.settings_xai_code_label)
    val hyphen = stringResource(R.string.settings_xai_code_hyphen)
    // Read one character at a time: "W, D, J, B, hyphen, …".
    val spoken = state.userCode.map { if (it == '-') hyphen else it.toString() }.joinToString(", ")

    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(state.deadlineElapsedMs) {
        while (true) {
            now = SystemClock.elapsedRealtime()
            if (now >= state.deadlineElapsedMs) break
            delay(1_000)
        }
    }
    val remainingMs = state.deadlineElapsedMs - now
    val remaining = formatRemaining(remainingMs)

    // The code panel: a Surface2 block in the Surface1 fold (Paper 13.40:1, PaperDim 4.96:1).
    Column(
        Modifier
            .fillMaxWidth()
            .clip(PanelShape)
            .background(Surface2)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(label, color = PaperDim, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            // Large mono, kept left-to-right in RTL languages (R03). Announced once when it arrives;
            // the countdown below stays outside any live region so it is not re-read every second.
            SelectionContainer {
                Text(
                    state.userCode,
                    color = Paper,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = MonoFont,
                        textDirection = TextDirection.Ltr,
                    ),
                    modifier = Modifier.semantics {
                        contentDescription = "$label: $spoken"
                        liveRegion = LiveRegionMode.Polite
                    },
                )
            }
            IconCircleButton(
                icon = Icons.Outlined.ContentCopy,
                contentDescription = stringResource(R.string.settings_xai_copy_code),
                onClick = { onCopyCode(state.userCode) },
            )
        }
        Text(
            monoValues(stringResource(R.string.settings_xai_time_left, remaining), remaining),
            color = PaperDim,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
        // The remaining share of the code's lifetime; the text above carries it for TalkBack.
        Box(Modifier.fillMaxWidth().clearAndSetSemantics {}) {
            if (state.lifetimeMs > 0L) {
                AmberTrack(
                    percent = ((remainingMs.coerceAtLeast(0L) * 100L + state.lifetimeMs - 1) / state.lifetimeMs).toInt(),
                    indeterminate = false,
                    modifier = Modifier.fillMaxWidth(),
                    height = 3.dp,
                )
            } else {
                AmberTrack(percent = 0, indeterminate = true, modifier = Modifier.fillMaxWidth(), height = 3.dp)
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.settings_xai_verify_at), color = PaperDim, fontSize = 13.sp, lineHeight = 18.sp)
        if (state.openable) {
            VerificationAddress(state.verificationUri, Modifier.fillMaxWidth())
        } else {
            // Not opened for the user (R03): they open it themselves, so it can be copied as is.
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                VerificationAddress(state.verificationUri, Modifier.weight(1f).padding(end = 8.dp))
                IconCircleButton(
                    icon = Icons.Outlined.ContentCopy,
                    contentDescription = stringResource(R.string.settings_xai_copy_address),
                    onClick = { onCopyUri(state.verificationUri) },
                )
            }
        }
    }
    if (!state.openable) NoticeCard(text = stringResource(R.string.settings_xai_verify_unlisted_host))
    StatusLine(stringResource(R.string.settings_xai_only_if_you_started), Amber)

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
        TonalButton(
            text = stringResource(R.string.settings_xai_cancel_login),
            fillMaxWidth = false,
            onClick = onCancel,
        )
    }
}

/**
 * The experimental, responsibility and billing notice (R01). In full: a titled Amber [NoticeCard]
 * (title Amber 8.10:1, detail PaperDim 5.16:1 on Surface1). Compact: the same card with only the
 * title and a "Details" toggle under it that expands the full text in place; collapsed by default,
 * kept across recreation. The toggle is its own TalkBack stop, a button with an expanded/collapsed
 * state; the notice text stays one stop.
 */
@Composable
private fun RiskNotice(compact: Boolean) {
    val title = stringResource(R.string.settings_xai_experimental_notice)
    val full = stringResource(R.string.settings_xai_responsibility) + "\n\n" +
        stringResource(R.string.settings_xai_billing_notice)
    if (!compact) {
        NoticeCard(title = title, text = full)
        return
    }
    var expanded by rememberSaveable { mutableStateOf(false) }
    val toggle: @Composable RowScope.() -> Unit = {
        DetailsToggle(expanded = expanded, onToggle = { expanded = !expanded })
    }
    if (expanded) {
        NoticeCard(title = title, text = full, actions = toggle)
    } else {
        // Without a title the text is the 14sp Amber message line, the same size and colour as
        // the full notice's title line.
        NoticeCard(text = title, actions = toggle)
    }
}

/**
 * A text toggle (Amber 14sp Medium label, 8.10:1 on Surface1, and an expand arrow). The touch box is
 * at least 48dp high but takes only the label line in the layout (24dp at 100 % font): it reaches
 * up to 12dp into the gap above and the card's 14dp padding below, so the compact notice does not
 * grow by a whole touch box.
 */
@Composable
private fun DetailsToggle(expanded: Boolean, onToggle: () -> Unit) {
    val stateText = stringResource(if (expanded) R.string.settings_expanded else R.string.settings_collapsed)
    Row(
        Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints.copy(minHeight = 0))
                // The label line (20sp, scaled with the font) plus a little, never less than 24dp.
                val label = maxOf(24.dp.roundToPx(), 20.sp.roundToPx() + 4.dp.roundToPx())
                val visible = minOf(placeable.height, label)
                layout(placeable.width, visible) { placeable.place(0, (visible - placeable.height) / 2) }
            }
            .heightIn(min = 48.dp)
            .clip(PanelShape)
            .clickable(role = Role.Button, onClick = onToggle)
            .semantics(mergeDescendants = true) { stateDescription = stateText }
            .padding(end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.settings_xai_notice_details),
            color = Amber,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Medium,
        )
        Icon(
            Icons.Outlined.ExpandMore,
            contentDescription = null,
            tint = Amber,
            modifier = Modifier
                .padding(start = 4.dp)
                .size(20.dp)
                .rotate(if (expanded) 180f else 0f),
        )
    }
}

/**
 * The verification address: Paper mono 14sp, left to right in every language, wrapped at any
 * character (a zero-width space after each one) so a long address never runs to the edge; TalkBack
 * reads the address itself.
 */
@Composable
private fun VerificationAddress(uri: String, modifier: Modifier = Modifier) {
    Text(
        breakAnywhere(uri),
        color = Paper,
        style = TextStyle(fontFamily = MonoFont, fontSize = 14.sp, lineHeight = 20.sp, textDirection = TextDirection.Ltr),
        modifier = modifier.semantics { contentDescription = uri },
    )
}

/** [text] with a zero-width space after every character: a line may break anywhere. */
internal fun breakAnywhere(text: String): String = buildString(text.length * 2) {
    text.forEach { append(it).append('\u200B') }
}

/**
 * A one-line status or caution inside the fold, without a box of its own: an 18dp icon and 14sp
 * text in [color] (Danger 10.67:1, Amber 8.10:1 or PaperDim 5.16:1 on Surface1), read as one TalkBack item and
 * announced politely when it appears or changes (it replaces the button the user just tapped).
 */
@Composable
private fun StatusLine(text: String, color: Color) {
    Row(
        Modifier.fillMaxWidth().semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            Icons.Outlined.Info,
            contentDescription = null,
            tint = color,
            modifier = Modifier.padding(top = 1.dp, end = 10.dp).size(18.dp),
        )
        Text(text, color = color, fontSize = 14.sp, lineHeight = 20.sp)
    }
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
 * Copies the verification address as plain text (not sensitive: it is a public page).
 * @return true when the app should confirm the copy itself (Android 12L and below; 13+ shows its own).
 */
internal fun copyXaiVerificationUri(context: Context, uri: String): Boolean {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return false
    clipboard.setPrimaryClip(ClipData.newPlainText("", uri))
    return Build.VERSION.SDK_INT < 33
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
