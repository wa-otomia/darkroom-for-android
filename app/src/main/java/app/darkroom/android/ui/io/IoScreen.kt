package app.darkroom.android.ui.io

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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.NotificationsOff
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.darkroom.android.R
import app.darkroom.android.core.FtpBindTarget
import app.darkroom.android.core.hasLocalNetworkPermission
import app.darkroom.android.core.needsLocalNetworkPermission
import app.darkroom.android.data.ftp.CameraFtpServer
import app.darkroom.android.data.ftp.FtpForegroundService
import app.darkroom.android.data.printer.PrintJob
import app.darkroom.android.data.printer.PrintQueue
import app.darkroom.android.data.settings.AppSettings
import app.darkroom.android.data.transfer.FtpSessionInfo
import app.darkroom.android.data.transfer.IncomingTransfer
import app.darkroom.android.data.transfer.TransferState
import app.darkroom.android.ui.components.AmberProgressBar
import app.darkroom.android.ui.components.DarkroomSnackbarHost
import app.darkroom.android.ui.components.GhostButton
import app.darkroom.android.ui.components.InlineConfirm
import app.darkroom.android.ui.components.PaperButton
import app.darkroom.android.ui.components.SectionLabel
import app.darkroom.android.ui.components.rememberInlineConfirmState
import app.darkroom.android.ui.jobFieldKey
import app.darkroom.android.ui.localizedUserError
import app.darkroom.android.ui.transferProgressUi
import app.darkroom.android.ui.theme.Amber
import app.darkroom.android.ui.theme.MonoFont
import app.darkroom.android.ui.theme.Paper
import app.darkroom.android.ui.theme.PaperDim
import app.darkroom.android.ui.theme.PaperHairline
import app.darkroom.android.ui.theme.SurfaceLow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The service gives no failure callback, so treat a silent stretch as a failed start. */
private const val START_TIMEOUT_MS = 8_000L
private const val HOST_POLL_MS = 5_000L

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
    val listenPort by FtpForegroundService.listenPort.collectAsState()
    val listenUser by FtpForegroundService.listenUser.collectAsState()
    val listenPassword by FtpForegroundService.listenPassword.collectAsState()
    val listenPasvMin by FtpForegroundService.listenPasvMin.collectAsState()
    val listenPasvMax by FtpForegroundService.listenPasvMax.collectAsState()
    val printJobs by printQueue.printJobs.collectAsState(initial = emptyList())
    var hosts by remember { mutableStateOf(CameraFtpServer.wlanHostTargets()) }
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
            hosts = withContext(Dispatchers.IO) { CameraFtpServer.wlanHostTargets() }
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
            hosts = withContext(Dispatchers.IO) { CameraFtpServer.wlanHostTargets() }
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

    Scaffold(
        // containerColor/contentColor default to background/onBackground, i.e. Room/Paper.
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { DarkroomSnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(20.dp),
        ) {
            Text(stringResource(R.string.io_eyebrow), style = MaterialTheme.typography.labelSmall, color = Amber)
            Text(stringResource(R.string.io_title), style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(top = 4.dp))
            Text(stringResource(R.string.io_desc), style = MaterialTheme.typography.bodyMedium, color = PaperDim, modifier = Modifier.padding(top = 8.dp, bottom = 20.dp))

            PrintQueueRow(jobs = printJobs, onOpen = onOpenPrintQueue)
            Spacer(Modifier.height(24.dp))

            Column(
                Modifier
                    .fillMaxWidth()
                    .background(SurfaceLow)
                    // The card is a container, not a control, so its frame stays a hairline.
                    .border(1.dp, PaperHairline, RoundedCornerShape(2.dp))
                    .padding(20.dp),
            ) {
                Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Spacer(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (listening || starting) Amber else PaperDim),
                    )
                    Text(
                        when {
                            listening -> stringResource(R.string.ftp_on)
                            starting -> stringResource(R.string.ftp_starting)
                            else -> stringResource(R.string.ftp_off)
                        },
                        color = if (listening || starting) Amber else PaperDim,
                        fontFamily = MonoFont,
                        modifier = Modifier.padding(start = 8.dp).weight(1f),
                    )
                    when {
                        listening -> GhostButton(stringResource(R.string.ftp_stop), fillMaxWidth = false) {
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
                ConnectionFold(open = ftpInfoOpen, onToggle = { ftpInfoOpen = !ftpInfoOpen }) {
                    HostRows(
                        hosts = hosts,
                        refreshing = refreshing,
                        confirmedLabel = confirmedLabel,
                        onRefresh = { refreshHosts() },
                    ) { label, ip -> copy(label, ip) }
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
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            LabelWithConfirm(passwordLabel, confirmedLabel == passwordLabel)
                            Text(
                                if (passwordVisible) showPassword else "••••••••",
                                color = Paper,
                                fontFamily = MonoFont,
                                fontSize = 20.sp,
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
                    Spacer(Modifier.height(14.dp))
                    Credential(
                        label = stringResource(R.string.ftp_pasv),
                        value = "$showPasvMin–$showPasvMax",
                        confirmedLabel = confirmedLabel,
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
                Text(stringResource(R.string.ftp_port21_hint), color = Amber, modifier = Modifier.padding(top = 12.dp))
            }
            Spacer(Modifier.height(24.dp))
            SessionsBlock(sessions = sessions, transfers = transfers)
            Spacer(Modifier.height(24.dp))
            TransfersBlock(transfers = transfers, onDismiss = onDismissTransfer)
            Spacer(Modifier.height(16.dp))
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
    SectionLabel(stringResource(R.string.io_sessions))
    if (sessions.isEmpty()) {
        Text(stringResource(R.string.io_sessions_empty), color = PaperDim, fontSize = 13.sp)
        return
    }
    sessions.forEach { session ->
        val current = session.currentTransferId?.let { byId[it] }
        Column(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .border(1.dp, PaperHairline, RoundedCornerShape(2.dp))
                .padding(14.dp),
        ) {
            Text(session.remote, color = Paper, fontFamily = MonoFont, fontSize = 16.sp)
            Text(
                sessionSubtitle(session, now),
                color = PaperDim,
                fontFamily = MonoFont,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
            val sessionFile = current?.filename ?: session.currentFilename
            if (!sessionFile.isNullOrBlank()) {
                Text(
                    stringResource(R.string.io_session_file, sessionFile),
                    color = Amber,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 6.dp),
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
        Text(stringResource(R.string.io_transfers_empty), color = PaperDim, fontSize = 13.sp)
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
    AmberProgressBar(
        title = transfer.filename,
        ui = transferProgressUi(transfer),
        extra = {
            if (failed) {
                if (!errorText.isNullOrBlank()) {
                    Text(errorText, color = Amber, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    GhostButton(stringResource(R.string.io_transfer_dismiss), fillMaxWidth = false) {
                        onDismiss(transfer.id)
                    }
                }
            }
        },
    )
}

@Composable
private fun ConnectionFold(
    open: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val expandLabel = stringResource(R.string.expand_section)
    val collapseLabel = stringResource(R.string.collapse_section)
    val expandedState = stringResource(R.string.settings_expanded)
    val collapsedState = stringResource(R.string.settings_collapsed)
    val arrow by animateFloatAsState(targetValue = if (open) 180f else 0f, label = "ftpInfoArrow")
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
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
        ) {
            Text(
                stringResource(R.string.io_ftp_connection),
                color = Amber,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = PaperDim,
                modifier = Modifier.rotate(arrow),
            )
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
    Row(
        Modifier
            .fillMaxWidth()
            .border(1.dp, PaperHairline, RoundedCornerShape(2.dp))
            .clickable(onClick = onOpen)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.io_print_queue), color = Paper)
            Text(status, color = PaperDim, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
        }
        Icon(
            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = PaperDim,
        )
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
    val visible = hosts.filter { it.ip != "0.0.0.0" && it.iface != "*" }
    if (visible.isEmpty()) {
        Column(Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
            SectionLabel(hostLabel)
            Text(
                stringResource(R.string.lib_io_no_host_title),
                color = Amber,
                fontFamily = MonoFont,
                fontSize = 16.sp,
            )
            Text(
                stringResource(R.string.lib_io_no_host_desc),
                color = PaperDim,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 6.dp, bottom = 12.dp),
            )
        }
    } else {
        visible.forEach { target ->
            val label = "$hostLabel · ${target.iface}"
            Credential(label, target.ip, confirmedLabel) { onCopy(label, target.ip) }
        }
    }
    Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        GhostButton(
            stringResource(R.string.lib_io_refresh_hosts),
            enabled = !refreshing,
            fillMaxWidth = false,
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

@Composable
private fun Credential(
    label: String,
    value: String,
    confirmedLabel: String?,
    onCopy: (String) -> Unit,
) {
    val copyLabel = stringResource(R.string.lib_io_copy_field, label)
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = copyLabel) { onCopy(label) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            LabelWithConfirm(label, confirmedLabel == label)
            Text(value, color = Paper, fontFamily = MonoFont, fontSize = 20.sp)
        }
        IconButton(onClick = { onCopy(label) }) {
            Icon(Icons.Outlined.ContentCopy, contentDescription = copyLabel, tint = PaperDim)
        }
    }
    Spacer(Modifier.height(14.dp))
}

/**
 * Field label with room for the "copied" marker beside it. Putting the marker on the label line
 * keeps the value from being reflowed or ellipsised when the confirmation appears.
 */
@Composable
private fun LabelWithConfirm(label: String, confirmed: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        SectionLabel(label)
        InlineConfirm(
            visible = confirmed,
            text = stringResource(R.string.common_copied),
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
        )
    }
}

@Composable
private fun LocalNetworkBlockedNotice(
    permanentlyBlocked: Boolean,
    onGrant: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .border(1.dp, PaperHairline, RoundedCornerShape(2.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.WifiOff,
                contentDescription = null,
                tint = Amber,
                modifier = Modifier.size(18.dp),
            )
            Text(
                stringResource(R.string.ftp_local_network_title),
                color = Amber,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        Text(
            stringResource(R.string.ftp_local_network_desc),
            color = PaperDim,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
        )
        if (!permanentlyBlocked) {
            GhostButton(
                stringResource(R.string.ftp_local_network_grant),
                fillMaxWidth = false,
                onClick = onGrant,
            )
            Spacer(Modifier.height(8.dp))
        }
        GhostButton(
            stringResource(R.string.lib_io_open_settings),
            fillMaxWidth = false,
            onClick = onOpenSettings,
        )
    }
}

@Composable
private fun NotificationBlockedNotice(onOpenSettings: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .border(1.dp, PaperHairline, RoundedCornerShape(2.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.NotificationsOff,
                contentDescription = null,
                tint = Amber,
                modifier = Modifier.size(18.dp),
            )
            Text(
                stringResource(R.string.lib_io_notif_blocked_title),
                color = Amber,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        Text(
            stringResource(R.string.lib_io_notif_blocked_desc),
            color = PaperDim,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
        )
        GhostButton(
            stringResource(R.string.lib_io_open_settings),
            fillMaxWidth = false,
            onClick = onOpenSettings,
        )
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
