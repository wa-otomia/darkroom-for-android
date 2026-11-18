package io.github.wa_otomia.darkroom.ui.io

import android.Manifest
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PersistableBundle
import android.provider.Settings
import android.text.format.DateUtils
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import io.github.wa_otomia.darkroom.R
import io.github.wa_otomia.darkroom.core.FtpBindTarget
import io.github.wa_otomia.darkroom.core.hasLocalNetworkPermission
import io.github.wa_otomia.darkroom.core.needsLocalNetworkPermission
import io.github.wa_otomia.darkroom.data.ftp.CameraFtpServer
import io.github.wa_otomia.darkroom.data.ftp.FtpInterfaces
import io.github.wa_otomia.darkroom.data.ftp.FtpForegroundService
import io.github.wa_otomia.darkroom.data.printer.PrintJob
import io.github.wa_otomia.darkroom.data.printer.PrintQueue
import io.github.wa_otomia.darkroom.data.settings.AppSettings
import io.github.wa_otomia.darkroom.data.transfer.FtpSessionInfo
import io.github.wa_otomia.darkroom.data.transfer.IncomingTransfer
import io.github.wa_otomia.darkroom.data.transfer.TransferState
import io.github.wa_otomia.darkroom.ui.components.AmberProgressBar
import io.github.wa_otomia.darkroom.ui.components.CardGroup
import io.github.wa_otomia.darkroom.ui.components.DarkroomCard
import io.github.wa_otomia.darkroom.ui.components.DarkroomSnackbarHost
import io.github.wa_otomia.darkroom.ui.components.GroupDivider
import io.github.wa_otomia.darkroom.ui.components.InlineConfirm
import io.github.wa_otomia.darkroom.ui.components.ListRow
import io.github.wa_otomia.darkroom.ui.components.ListRowChevron
import io.github.wa_otomia.darkroom.ui.components.ListRowPadding
import io.github.wa_otomia.darkroom.ui.components.ListRowTextIndent
import io.github.wa_otomia.darkroom.ui.components.NoticeCard
import io.github.wa_otomia.darkroom.ui.components.PageTitle
import io.github.wa_otomia.darkroom.ui.components.PaperButton
import io.github.wa_otomia.darkroom.ui.components.SectionLabel
import io.github.wa_otomia.darkroom.ui.components.StatusPill
import io.github.wa_otomia.darkroom.ui.components.TonalButton
import io.github.wa_otomia.darkroom.ui.components.cardGlowBrush
import io.github.wa_otomia.darkroom.ui.components.rememberInlineConfirmState
import io.github.wa_otomia.darkroom.ui.jobFieldKey
import io.github.wa_otomia.darkroom.ui.localizedUserError
import io.github.wa_otomia.darkroom.ui.navigation.LocalBottomBarClearance
import io.github.wa_otomia.darkroom.ui.transferProgressUi
import io.github.wa_otomia.darkroom.ui.theme.Amber
import io.github.wa_otomia.darkroom.ui.theme.AmberSoft
import io.github.wa_otomia.darkroom.ui.theme.MonoFont
import io.github.wa_otomia.darkroom.ui.theme.PanelShape
import io.github.wa_otomia.darkroom.ui.theme.Paper
import io.github.wa_otomia.darkroom.ui.theme.PaperDim
import io.github.wa_otomia.darkroom.ui.theme.Surface1
import io.github.wa_otomia.darkroom.ui.theme.Surface2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The service gives no failure callback, so treat a silent stretch as a failed start. */
private const val START_TIMEOUT_MS = 8_000L
private const val HOST_POLL_MS = 5_000L

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IoScreen(
    settings: AppSettings,
    sessions: List<FtpSessionInfo>,
    transfers: List<IncomingTransfer>,
    printQueue: PrintQueue,
    onDismissTransfer: (String) -> Unit,
    onOpenPrintQueue: () -> Unit,
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val listening by FtpForegroundService.listening.collectAsState()
    val serviceLocalNetworkBlocked by FtpForegroundService.localNetworkBlocked.collectAsState()
    val boundHosts by FtpForegroundService.boundHosts.collectAsState()
    val bindBlocked by FtpForegroundService.bindBlocked.collectAsState()
    // Running with nothing bound: either the port cannot be used, or there is no Wi-Fi yet.
    // The server never falls back to mobile data.
    val portBlocked = listening && boundHosts.isEmpty() && bindBlocked
    val waitingForWifi = listening && boundHosts.isEmpty() && !bindBlocked
    val listenPort by FtpForegroundService.listenPort.collectAsState()
    val listenUser by FtpForegroundService.listenUser.collectAsState()
    val listenPassword by FtpForegroundService.listenPassword.collectAsState()
    val listenPasvMin by FtpForegroundService.listenPasvMin.collectAsState()
    val listenPasvMax by FtpForegroundService.listenPasvMax.collectAsState()
    val printJobs by printQueue.printJobs.collectAsState(initial = emptyList())
    // Name rules only here (no system calls on the main thread); the resume loop below adds the
    // system's hotspot view off the main thread straight away.
    var hosts by remember { mutableStateOf(CameraFtpServer.wifiHostTargets()) }
    var refreshing by remember { mutableStateOf(false) }
    var resumed by remember { mutableStateOf(true) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var ftpInfoOpen by rememberSaveable { mutableStateOf(false) }
    var starting by rememberSaveable { mutableStateOf(false) }
    var startFailed by rememberSaveable { mutableStateOf(false) }
    var notifBlocked by rememberSaveable { mutableStateOf(false) }
    var localNetworkDenied by rememberSaveable { mutableStateOf(false) }
    var localNetworkAskBlocked by rememberSaveable { mutableStateOf(false) }
    val notifDenied = stringResource(R.string.ftp_notif_denied)
    val passwordLabel = stringResource(R.string.ftp_password)
    val copiedMessage = stringResource(R.string.copied_to_clipboard)

    // Copy feedback belongs beside the field that was copied, not in a snackbar at the bottom of
    // the screen. One state plus the label of the last copied field drives every row.
    val confirm = rememberInlineConfirmState()
    var confirmTarget by remember { mutableStateOf<String?>(null) }
    val confirmedLabel = if (confirm.visible) confirmTarget else null

    fun notifGranted(): Boolean = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

    fun localNetworkGranted(): Boolean = hasLocalNetworkPermission(context)

    fun startService() {
        startFailed = false
        starting = true
        FtpForegroundService.start(context)
    }

    val notify = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            notifBlocked = false
        } else if (Build.VERSION.SDK_INT >= 33) {
            // No rationale offered right after a denial means the user picked "don't ask again",
            // so stop asking and point at system settings instead.
            notifBlocked = activity == null ||
                !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)
            if (!notifBlocked) scope.launch { snackbar.showSnackbar(notifDenied) }
        }
        startService()
    }

    fun startAfterLocalNetwork() {
        if (Build.VERSION.SDK_INT >= 33 && !notifGranted() && !notifBlocked) {
            notify.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startService()
        }
    }

    val requestLocalNetwork = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            localNetworkDenied = false
            localNetworkAskBlocked = false
            startAfterLocalNetwork()
        } else {
            // Denied: never start. Same "don't ask again" detection as POST_NOTIFICATIONS.
            localNetworkDenied = true
            localNetworkAskBlocked = activity == null ||
                !ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.ACCESS_LOCAL_NETWORK,
                )
        }
    }

    fun onStartClicked() {
        if (needsLocalNetworkPermission(Build.VERSION.SDK_INT) && !localNetworkGranted()) {
            if (localNetworkAskBlocked) {
                localNetworkDenied = true
                return
            }
            requestLocalNetwork.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
            return
        }
        startAfterLocalNetwork()
    }

    fun refreshHosts() {
        scope.launch {
            refreshing = true
            hosts = withContext(Dispatchers.IO) {
                // A running server re-binds right away, so the rows it shows update too.
                FtpForegroundService.requestRebind()
                CameraFtpServer.wifiHostTargets(FtpInterfaces.policy(context))
            }
            refreshing = false
        }
    }

    val showPort = if (listening) listenPort else settings.ftpPort
    val showUser = if (listening) listenUser else settings.ftpUser
    val showPassword = if (listening) listenPassword else settings.ftpPassword
    val showPasvMin = if (listening) listenPasvMin else settings.ftpPasvMin
    val showPasvMax = if (listening) listenPasvMax else settings.ftpPasvMax

    LifecycleResumeEffect(Unit) {
        resumed = true
        onPauseOrDispose { resumed = false }
    }

    // Addresses change when the user switches Wi-Fi or turns the hotspot on while this screen is
    // open, so keep them current instead of only reading them once on resume.
    LaunchedEffect(resumed) {
        while (resumed) {
            hosts = withContext(Dispatchers.IO) { CameraFtpServer.wifiHostTargets(FtpInterfaces.policy(context)) }
            if (notifGranted()) notifBlocked = false
            if (localNetworkGranted()) {
                localNetworkDenied = false
                localNetworkAskBlocked = false
            }
            delay(HOST_POLL_MS)
        }
    }

    LaunchedEffect(listening, serviceLocalNetworkBlocked) {
        if (listening) {
            starting = false
            startFailed = false
        } else if (serviceLocalNetworkBlocked) {
            starting = false
            startFailed = false
            localNetworkDenied = true
        }
    }

    LaunchedEffect(starting) {
        if (!starting) return@LaunchedEffect
        delay(START_TIMEOUT_MS)
        if (starting) {
            starting = false
            startFailed = true
        }
    }

    fun copy(label: String, value: String, sensitive: Boolean = false, inlineConfirm: Boolean = true) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, value)
        if (sensitive) {
            clip.description.extras = PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
        }
        cm.setPrimaryClip(clip)
        // Android 13+ raises its own "copied" bubble; a second confirmation would double up.
        if (inlineConfirm && Build.VERSION.SDK_INT < 33) {
            confirmTarget = label
            scope.launch { confirm.show() }
        }
    }

    // Top-level tab: the floating nav bar overlays the bottom of this screen, so the scroll
    // content ends with this clearance and the snackbar is lifted above the bar.
    val clearance = LocalBottomBarClearance.current
    Scaffold(
        // containerColor/contentColor default to background/onBackground, i.e. Room/Paper.
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { DarkroomSnackbarHost(snackbar, Modifier.padding(bottom = clearance)) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            PageTitle(
                title = stringResource(R.string.io_title),
                modifier = Modifier.padding(top = 12.dp, bottom = 12.dp),
            )

            SectionLabel(stringResource(R.string.io_eyebrow))
            // FTP hero card: status and the values to type into the camera, all visible while
            // listening without expanding anything (IMNW010-R03). Only PASV sits under "more".
            DarkroomCard(
                brush = cardGlowBrush(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val active = listening || starting
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(Modifier.weight(1f)) {
                        StatusPill(
                            text = when {
                                portBlocked -> stringResource(R.string.ftp_port_unavailable)
                                waitingForWifi -> stringResource(R.string.ftp_waiting)
                                listening -> stringResource(R.string.ftp_on)
                                starting -> stringResource(R.string.ftp_starting)
                                else -> stringResource(R.string.ftp_off)
                            },
                            // Amber on AmberSoft 5.46–6.69:1; PaperDim on Surface2 4.96:1.
                            dot = if (active) Amber else PaperDim,
                            container = if (active) AmberSoft else Surface2,
                            contentColor = if (active) Amber else PaperDim,
                        )
                    }
                    when {
                        listening -> TonalButton(stringResource(R.string.ftp_stop), fillMaxWidth = false) {
                            FtpForegroundService.stop(context)
                        }
                        starting -> Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Amber,
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.size(10.dp))
                            PaperButton(stringResource(R.string.ftp_start), enabled = false, fillMaxWidth = false) { }
                        }
                        else -> PaperButton(stringResource(R.string.ftp_start), fillMaxWidth = false) { onStartClicked() }
                    }
                }

                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        stringResource(R.string.io_ftp_connection),
                        color = PaperDim,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.semantics { heading() },
                    )
                    HostRows(
                        // While running, show what is actually bound; otherwise (or when the
                        // port is blocked) the Wi-Fi addresses that would be used.
                        hosts = if (listening && !bindBlocked) boundHosts else hosts,
                        refreshing = refreshing,
                        confirmedLabel = confirmedLabel,
                        onRefresh = { refreshHosts() },
                    ) { label, ip -> copy(label, ip) }
                    FlowRow(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Credential(
                            label = stringResource(R.string.ftp_port),
                            value = showPort.toString(),
                            confirmedLabel = confirmedLabel,
                        ) { copy(it, showPort.toString()) }
                        Credential(
                            label = stringResource(R.string.ftp_user),
                            value = showUser,
                            confirmedLabel = confirmedLabel,
                        ) { copy(it, showUser) }
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            LabelWithConfirm(passwordLabel, confirmedLabel == passwordLabel)
                            Text(
                                if (passwordVisible) showPassword else "••••••••",
                                color = Paper,
                                fontFamily = MonoFont,
                                fontSize = 18.sp,
                                lineHeight = 24.sp,
                            )
                        }
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = stringResource(if (passwordVisible) R.string.hide_password else R.string.show_password),
                                tint = PaperDim,
                            )
                        }
                        IconButton(
                            onClick = {
                                copy(passwordLabel, showPassword, sensitive = true, inlineConfirm = false)
                                scope.launch { snackbar.showSnackbar(copiedMessage) }
                            },
                        ) {
                            Icon(
                                Icons.Outlined.ContentCopy,
                                contentDescription = stringResource(R.string.lib_io_copy_field, passwordLabel),
                                tint = PaperDim,
                            )
                        }
                    }
                }

                ConnectionFold(
                    title = stringResource(R.string.io_ui_more_details),
                    open = ftpInfoOpen,
                    onToggle = { ftpInfoOpen = !ftpInfoOpen },
                ) {
                    Credential(
                        label = stringResource(R.string.ftp_pasv),
                        value = "$showPasvMin–$showPasvMax",
                        confirmedLabel = confirmedLabel,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    ) { copy(it, "$showPasvMin-$showPasvMax") }
                }
            }
            val showLocalNetworkNotice = !localNetworkGranted() &&
                (localNetworkDenied || localNetworkAskBlocked || serviceLocalNetworkBlocked)
            if (showLocalNetworkNotice) {
                LocalNetworkBlockedNotice(
                    permanentlyBlocked = localNetworkAskBlocked,
                    onGrant = { requestLocalNetwork.launch(Manifest.permission.ACCESS_LOCAL_NETWORK) },
                    onOpenSettings = { openAppSettings(context) },
                )
            }
            if (notifBlocked) {
                NotificationBlockedNotice { openNotificationSettings(context) }
            }
            if (showPort < 1024) {
                NoticeCard(
                    text = stringResource(R.string.ftp_port21_hint),
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            // The setup instructions, moved from the page header to small print under the card.
            Text(
                stringResource(R.string.io_desc),
                style = MaterialTheme.typography.bodySmall,
                color = PaperDim,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 12.dp),
            )

            Spacer(Modifier.height(24.dp))
            CardGroup {
                PrintQueueRow(jobs = printJobs, onOpen = onOpenPrintQueue)
                GroupDivider()
                SessionsBlock(sessions = sessions, transfers = transfers)
            }
            Spacer(Modifier.height(24.dp))
            TransfersBlock(transfers = transfers, onDismiss = onDismissTransfer)
            Spacer(Modifier.height(clearance + 16.dp))
        }
    }
    if (startFailed) {
        AlertDialog(
            onDismissRequest = { startFailed = false },
            title = { Text(stringResource(R.string.job_error_title)) },
            text = { Text(stringResource(R.string.lib_io_start_failed)) },
            confirmButton = {
                TextButton(onClick = { startFailed = false }) {
                    Text(stringResource(R.string.common_back))
                }
            },
        )
    }
}

@Composable
private fun SessionsBlock(sessions: List<FtpSessionInfo>, transfers: List<IncomingTransfer>) {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(sessions.size) {
        if (sessions.isEmpty()) return@LaunchedEffect
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }
    val byId = remember(transfers) { transfers.associateBy { it.id } }
    // Group row inside the print-queue/sessions CardGroup; it stays a heading for TalkBack.
    ListRow(
        title = stringResource(R.string.io_sessions),
        supporting = if (sessions.isEmpty()) stringResource(R.string.io_sessions_empty) else null,
        leadingIcon = Icons.Outlined.PhotoCamera,
        modifier = Modifier.semantics { heading() },
    )
    if (sessions.isEmpty()) {
        return
    }
    sessions.forEach { session ->
        val current = session.currentTransferId?.let { byId[it] }
        GroupDivider()
        // One session per row, text aligned with the group row's title above it.
        Column(
            Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {}
                .padding(start = ListRowTextIndent, end = ListRowPadding, top = 10.dp, bottom = 12.dp),
        ) {
            Text(session.remote, color = Paper, fontFamily = MonoFont, fontSize = 15.sp, lineHeight = 20.sp)
            Text(
                sessionSubtitle(session, now),
                color = PaperDim,
                fontFamily = MonoFont,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
            val sessionFile = current?.filename ?: session.currentFilename
            if (!sessionFile.isNullOrBlank()) {
                Text(
                    stringResource(R.string.io_session_file, sessionFile),
                    color = Amber,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun sessionSubtitle(session: FtpSessionInfo, nowMs: Long): String {
    val user = session.user.ifBlank { stringResource(R.string.io_session_anonymous) }
    val auth = if (session.authed) user else stringResource(R.string.io_session_signing_in)
    val elapsed = formatSessionElapsed(session.connectedAt, nowMs)
    return if (elapsed.isEmpty()) auth else "$auth · $elapsed"
}

@Composable
private fun TransfersBlock(transfers: List<IncomingTransfer>, onDismiss: (String) -> Unit) {
    val visible = transfers.filter { it.state != TransferState.Done }
    SectionLabel(stringResource(R.string.io_transfers))
    if (visible.isEmpty()) {
        Text(
            stringResource(R.string.io_transfers_empty),
            color = PaperDim,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(start = 4.dp),
        )
        return
    }
    visible.forEach { transfer ->
        TransferRow(transfer = transfer, onDismiss = onDismiss)
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun TransferRow(transfer: IncomingTransfer, onDismiss: (String) -> Unit) {
    val failed = transfer.state == TransferState.Failed
    val errorText = if (failed) {
        transfer.error?.takeIf { it.isNotBlank() }?.let { localizedUserError(it) }
    } else {
        null
    }
    DarkroomCard {
        AmberProgressBar(
            title = transfer.filename,
            ui = transferProgressUi(transfer),
            framed = false,
            extra = {
                if (failed) {
                    if (!errorText.isNullOrBlank()) {
                        Text(
                            errorText,
                            color = Amber,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        TonalButton(stringResource(R.string.io_transfer_dismiss), fillMaxWidth = false) {
                            onDismiss(transfer.id)
                        }
                    }
                }
            },
        )
    }
}

/** The "more" section of the FTP card (PASV range), collapsed by default. */
@Composable
private fun ConnectionFold(
    title: String,
    open: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val expandLabel = stringResource(R.string.expand_section)
    val collapseLabel = stringResource(R.string.collapse_section)
    val expandedState = stringResource(R.string.settings_expanded)
    val collapsedState = stringResource(R.string.settings_collapsed)
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable(
                    onClickLabel = if (open) collapseLabel else expandLabel,
                    role = Role.Button,
                    onClick = onToggle,
                )
                .semantics(mergeDescendants = true) {
                    stateDescription = if (open) expandedState else collapsedState
                }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                title,
                color = Amber,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            ListRowChevron(expanded = open)
        }
        AnimatedVisibility(
            visible = open,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun PrintQueueRow(jobs: List<PrintJob>, onOpen: () -> Unit) {
    val queued = jobs.count { jobFieldKey(it.state) == "queued" }
    val running = jobs.any { jobFieldKey(it.state) == "running" }
    val status = when {
        running && queued > 0 -> stringResource(R.string.io_print_queue_queued_printing, queued)
        running -> stringResource(R.string.print_queue_printing)
        queued > 0 -> stringResource(R.string.io_print_queue_queued, queued)
        else -> stringResource(R.string.io_print_queue_idle)
    }
    ListRow(
        title = stringResource(R.string.io_print_queue),
        supporting = status,
        leadingIcon = Icons.Outlined.Print,
        onClick = onOpen,
    ) {
        ListRowChevron()
    }
}

@Composable
private fun HostRows(
    hosts: List<FtpBindTarget>,
    refreshing: Boolean,
    confirmedLabel: String?,
    onRefresh: () -> Unit,
    onCopy: (String, String) -> Unit,
) {
    val hostLabel = stringResource(R.string.ftp_host)
    val visible = hosts
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (visible.isEmpty()) {
            Column(Modifier.fillMaxWidth()) {
                FieldLabel(hostLabel)
                Text(
                    stringResource(R.string.lib_io_no_host_title),
                    color = Amber,
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    stringResource(R.string.lib_io_no_host_desc),
                    color = PaperDim,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        } else {
            visible.forEach { target ->
                // "Host · wlan0": the interface name tells Wi-Fi from hotspot or Wi-Fi Direct.
                val label = "$hostLabel · ${target.iface}"
                val copyLabel = stringResource(R.string.lib_io_copy_field, label)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(onClickLabel = copyLabel) { onCopy(label, target.ip) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        LabelWithConfirm(label, confirmedLabel == label)
                        Text(
                            target.ip,
                            color = Paper,
                            fontFamily = MonoFont,
                            fontSize = 24.sp,
                            lineHeight = 30.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    IconButton(onClick = { onCopy(label, target.ip) }) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = copyLabel, tint = PaperDim)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TonalButton(
                stringResource(R.string.lib_io_refresh_hosts),
                enabled = !refreshing,
                fillMaxWidth = false,
                icon = Icons.Outlined.Refresh,
                onClick = onRefresh,
            )
            if (refreshing) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(start = 12.dp).size(16.dp),
                    color = Amber,
                    strokeWidth = 2.dp,
                )
            }
        }
    }
}

/**
 * One copyable connection value: label (with the "copied" marker) over a Mono value, plus a copy
 * button. The whole cell copies too. Sized to its content so port and user share a line; the
 * value column yields width first so the copy button is never squeezed out.
 */
@Composable
private fun Credential(
    label: String,
    value: String,
    confirmedLabel: String?,
    modifier: Modifier = Modifier,
    onCopy: (String) -> Unit,
) {
    val copyLabel = stringResource(R.string.lib_io_copy_field, label)
    Row(
        modifier.clickable(onClickLabel = copyLabel) { onCopy(label) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f, fill = false)) {
            LabelWithConfirm(label, confirmedLabel == label)
            Text(value, color = Paper, fontFamily = MonoFont, fontSize = 18.sp, lineHeight = 24.sp)
        }
        IconButton(onClick = { onCopy(label) }) {
            Icon(Icons.Outlined.ContentCopy, contentDescription = copyLabel, tint = PaperDim)
        }
    }
}

/** 12sp Medium PaperDim field label (5.16:1 on Surface1, 4.79:1 on the CardGlow corner). */
@Composable
private fun FieldLabel(text: String) {
    Text(
        text,
        color = PaperDim,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
    )
}

/**
 * Field label with room for the "copied" marker beside it. Putting the marker on the label line
 * keeps the value from being reflowed or ellipsised when the confirmation appears.
 */
@Composable
private fun LabelWithConfirm(label: String, confirmed: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        FieldLabel(label)
        InlineConfirm(
            visible = confirmed,
            text = stringResource(R.string.common_copied),
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun LocalNetworkBlockedNotice(
    permanentlyBlocked: Boolean,
    onGrant: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    TitledNotice(
        title = stringResource(R.string.ftp_local_network_title),
        description = stringResource(R.string.ftp_local_network_desc),
        icon = Icons.Outlined.WifiOff,
    ) {
        if (!permanentlyBlocked) {
            TonalButton(
                stringResource(R.string.ftp_local_network_grant),
                fillMaxWidth = false,
                onClick = onGrant,
            )
        }
        TonalButton(
            stringResource(R.string.lib_io_open_settings),
            fillMaxWidth = false,
            onClick = onOpenSettings,
        )
    }
}

@Composable
private fun NotificationBlockedNotice(onOpenSettings: () -> Unit) {
    TitledNotice(
        title = stringResource(R.string.lib_io_notif_blocked_title),
        description = stringResource(R.string.lib_io_notif_blocked_desc),
        icon = Icons.Outlined.NotificationsOff,
    ) {
        TonalButton(
            stringResource(R.string.lib_io_open_settings),
            fillMaxWidth = false,
            onClick = onOpenSettings,
        )
    }
}

/**
 * [io.github.wa_otomia.darkroom.ui.components.NoticeCard] look (Surface1, 16dp corners, 1dp Amber
 * 50% outline, Amber icon) with a separate Amber title and PaperDim description, which NoticeCard's
 * single text cannot carry. Amber on Surface1 8.10:1; PaperDim on Surface1 5.16:1.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TitledNotice(
    title: String,
    description: String,
    icon: ImageVector,
    actions: @Composable RowScope.() -> Unit,
) {
    Column(
        Modifier
            .padding(top = 12.dp)
            .fillMaxWidth()
            .clip(PanelShape)
            .background(Surface1)
            .border(1.dp, Amber.copy(alpha = 0.5f), PanelShape)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                icon,
                contentDescription = null,
                tint = Amber,
                modifier = Modifier.padding(top = 1.dp, end = 10.dp).size(18.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(title, color = Amber, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)
                Text(
                    description,
                    color = PaperDim,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        FlowRow(
            modifier = Modifier.padding(top = 10.dp, start = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            actions()
        }
    }
}

private fun formatSessionElapsed(connectedAt: Long, nowMs: Long): String {
    val seconds = ((nowMs - connectedAt) / 1000L).coerceAtLeast(0)
    return DateUtils.formatElapsedTime(seconds)
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        .setData(Uri.fromParts("package", context.packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivitySafely(intent)
}

private fun openNotificationSettings(context: Context) {
    val direct = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (context.startActivitySafely(direct)) return
    val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        .setData(Uri.fromParts("package", context.packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivitySafely(fallback)
}

private fun Context.startActivitySafely(intent: Intent): Boolean = runCatching {
    startActivity(intent)
    true
}.getOrDefault(false)
