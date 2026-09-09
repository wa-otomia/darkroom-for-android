package app.darkroom.android.ui.settings

import android.Manifest
import android.app.Activity
import android.app.LocaleManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.LocaleList
import android.provider.Settings
import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.BrandingWatermark
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.WifiTethering
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.darkroom.android.BuildConfig
import app.darkroom.android.R
import app.darkroom.android.core.AI_PRESET_NONE
import app.darkroom.android.core.ActivityEntry
import app.darkroom.android.core.AiProvider
import app.darkroom.android.core.GROK_MODELS
import app.darkroom.android.core.PrintFit
import app.darkroom.android.core.SnsLogo
import app.darkroom.android.core.withAnchor
import app.darkroom.android.core.withDefaultAnchors
import app.darkroom.android.core.WATERMARK_SCALE_MAX
import app.darkroom.android.core.WATERMARK_SCALE_MIN
import app.darkroom.android.core.clampWatermarkScale
import app.darkroom.android.core.formatWatermarkDate
import app.darkroom.android.core.hasLocalNetworkPermission
import app.darkroom.android.core.isConfigured
import app.darkroom.android.core.normalizeAiBaseUrl
import app.darkroom.android.data.ai.AiImageClient
import app.darkroom.android.data.ai.AiProbe
import app.darkroom.android.data.catalog.CatalogRepository
import app.darkroom.android.data.ftp.FtpForegroundService
import app.darkroom.android.data.imaging.WatermarkRenderer
import app.darkroom.android.data.printer.NearbyDevice
import app.darkroom.android.data.printer.PrinterBluetooth
import app.darkroom.android.data.settings.ActivityLog
import app.darkroom.android.data.settings.SettingsRepository
import app.darkroom.android.data.progress.JobKind
import app.darkroom.android.data.progress.PrefsProgressStats
import app.darkroom.android.data.progress.indeterminateUi
import app.darkroom.android.ui.components.AmberTrack
import app.darkroom.android.ui.components.DarkroomSnackbarHost
import app.darkroom.android.ui.components.GhostButton
import app.darkroom.android.ui.components.InlineConfirm
import app.darkroom.android.ui.components.PaperButton
import app.darkroom.android.ui.components.PresetDropdown
import app.darkroom.android.ui.components.SectionLabel
import app.darkroom.android.ui.components.darkroomTextFieldColors
import app.darkroom.android.ui.components.rememberInlineConfirmState
import app.darkroom.android.ui.theme.Amber
import app.darkroom.android.ui.theme.Danger
import app.darkroom.android.ui.theme.Ink
import app.darkroom.android.ui.theme.MonoFont
import app.darkroom.android.ui.theme.Paper
import app.darkroom.android.ui.theme.PaperDim
import app.darkroom.android.ui.theme.PaperDisabled
import app.darkroom.android.ui.theme.PaperFaint
import app.darkroom.android.ui.theme.PaperHairline
import app.darkroom.android.ui.theme.Room
import app.darkroom.android.ui.theme.SurfaceLow
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.roundToInt

private const val FOLD_MS = 240
private const val HIGHLIGHT_MS = 1600L

/** Permission gate state for the Bluetooth scan button. */
private const val PERM_OK = 0
private const val PERM_DENIED = 1
private const val PERM_BLOCKED = 2

private data class AiDraft(
    val key: String = "",
    val url: String,
    val model: String,
    val resolution: String,
    val quality: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: SettingsRepository,
    bluetooth: PrinterBluetooth,
    activityLog: ActivityLog,
    grokClient: AiImageClient,
    catalog: CatalogRepository,
    section: String = "",
    onAbout: () -> Unit = {},
) {
    val cfg by settings.settings.collectAsState()
    val ai by settings.ai.collectAsState()
    val watermark by settings.watermark.collectAsState()
    val photos by catalog.photos.collectAsState(initial = emptyList())
    val presets by settings.presets.collectAsState()
    val logs by activityLog.entries.collectAsState()
    val ftpListening by FtpForegroundService.listening.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val progressStats = remember(context) { PrefsProgressStats(context.applicationContext) }
    val activity = remember(context) { context.findActivity() }
    val snackbar = remember { SnackbarHostState() }
    val timeFormatter = rememberLogTimeFormatter()

    // Draft state. Everything the user can type or expand survives an Activity restart, except the
    // two secrets: an API key and an FTP password in the saved-instance Bundle would be written to
    // the recents/saved-state store in plain text, which is a worse trade than retyping them.
    var formProviderId by rememberSaveable { mutableStateOf(ai.active.id) }
    val formProvider = AiProvider.fromId(formProviderId)
    val grokStored = ai.byProvider[AiProvider.GROK]
    val openaiStored = ai.byProvider[AiProvider.OPENAI]
    var grokDraft by remember {
        mutableStateOf(
            AiDraft(
                url = grokStored?.baseUrl ?: AiProvider.GROK.defaultBaseUrl,
                model = grokStored?.model ?: AiProvider.GROK.defaultModel,
                resolution = grokStored?.resolution ?: "1k",
                quality = grokStored?.quality ?: "medium",
            ),
        )
    }
    var openaiDraft by remember {
        mutableStateOf(
            AiDraft(
                url = openaiStored?.baseUrl ?: AiProvider.OPENAI.defaultBaseUrl,
                model = openaiStored?.model ?: AiProvider.OPENAI.defaultModel,
                resolution = "1k",
                quality = openaiStored?.quality ?: "medium",
            ),
        )
    }
    var modelMenuOpen by remember { mutableStateOf(false) }
    var modelRefreshFailed by remember { mutableStateOf(false) }
    var ftpUser by rememberSaveable { mutableStateOf(cfg.ftpUser) }
    var ftpPassword by remember { mutableStateOf("") }
    var ftpPort by rememberSaveable { mutableStateOf(cfg.ftpPort.toString()) }
    var pasvMin by rememberSaveable { mutableStateOf(cfg.ftpPasvMin.toString()) }
    var pasvMax by rememberSaveable { mutableStateOf(cfg.ftpPasvMax.toString()) }

    var portBlurred by rememberSaveable { mutableStateOf(false) }
    var passwordBlurred by rememberSaveable { mutableStateOf(false) }
    var pasvMinBlurred by rememberSaveable { mutableStateOf(false) }
    var pasvMaxBlurred by rememberSaveable { mutableStateOf(false) }
    var ftpSubmitted by rememberSaveable { mutableStateOf(false) }

    var openGrok by rememberSaveable { mutableStateOf(false) }
    var openPrinter by rememberSaveable { mutableStateOf(false) }
    var openFtp by rememberSaveable { mutableStateOf(false) }
    var openPrinting by rememberSaveable { mutableStateOf(false) }
    var openAutoPrint by rememberSaveable { mutableStateOf(false) }
    var openWatermark by rememberSaveable { mutableStateOf(false) }
    var openActivity by rememberSaveable { mutableStateOf(false) }
    var openLanguage by rememberSaveable { mutableStateOf(false) }

    var showClearKey by rememberSaveable { mutableStateOf(false) }
    var showClearLog by rememberSaveable { mutableStateOf(false) }
    var showFtpRestart by rememberSaveable { mutableStateOf(false) }
    var showReset by rememberSaveable { mutableStateOf(false) }
    var expandedLog by rememberSaveable { mutableStateOf("") }

    // Connection-test state is transient by design: a verdict that outlives the values it was
    // measured against would be misleading, so it is dropped on restart and on every draft edit.
    var probeJob by remember { mutableStateOf<Job?>(null) }
    var probeResult by remember { mutableStateOf<AiProbe?>(null) }
    var probeTarget by remember { mutableStateOf("") }
    var probeUsedSavedKey by remember { mutableStateOf(false) }

    // Saving is confirmed next to the button that did it rather than in a snackbar at the far edge
    // of the screen. The text is held alongside the state because the wording depends on what
    // actually landed on disk (a normalised URL, a restarted FTP service).
    val grokConfirm = rememberInlineConfirmState()
    var grokConfirmText by remember { mutableStateOf("") }
    val ftpConfirm = rememberInlineConfirmState()
    var ftpConfirmText by remember { mutableStateOf("") }

    // The UI locale is read back from the platform on every resume rather than cached in our own
    // store: LocaleManager is also what the system "App language" screen writes, so a returning
    // user sees whatever they picked there.
    var localeTag by remember { mutableStateOf(currentLocaleTag(context)) }

    var devices by remember { mutableStateOf(bluetooth.bonded()) }
    var scanJob by remember { mutableStateOf<Job?>(null) }
    var pairingMac by remember { mutableStateOf("") }
    var btEnabled by remember { mutableStateOf(bluetooth.isEnabled()) }
    var permState by rememberSaveable { mutableStateOf(PERM_OK) }
    var localNetworkMissing by remember { mutableStateOf(!hasLocalNetworkPermission(context)) }

    val scrollState = rememberScrollState()
    // Plain (non-observable) holders: these are rewritten on every scroll frame, so keeping them in
    // snapshot state would recompose the whole page while scrolling.
    val sectionTops = remember { mutableMapOf<String, Float>() }
    val viewportTop = remember { floatArrayOf(0f) }
    var highlighted by remember { mutableStateOf("") }
    var pendingScroll by remember { mutableStateOf("") }
    var scrollTick by remember { mutableStateOf(0) }

    val grokSaved = stringResource(R.string.grok_saved)
    val keyCleared = stringResource(R.string.settings_grok_key_cleared)
    val ftpSaved = stringResource(R.string.ftp_saved)
    val ftpSavedRestarted = stringResource(R.string.ftp_saved_restarted)
    val ftpSavedPending = stringResource(R.string.settings_ftp_saved_pending)
    val portErrorText = stringResource(R.string.error_ftp_port)
    val passwordErrorText = stringResource(R.string.error_ftp_password)
    val pasvRangeError = stringResource(R.string.settings_err_pasv_range)
    val pasvOrderError = stringResource(R.string.settings_err_pasv_order)
    val pasvSpanError = stringResource(R.string.settings_err_pasv_span)
    val pairFailedHint = stringResource(R.string.settings_pair_failed_hint)
    val unboundSummary = stringResource(R.string.settings_unbound)
    val resetDone = stringResource(R.string.settings_reset_done)
    val logCopied = stringResource(R.string.settings_log_copied)
    val logCleared = stringResource(R.string.settings_log_cleared)
    val logShareTitle = stringResource(R.string.settings_log_share_title)
    val needPrinterText = stringResource(R.string.settings_autoprint_need_printer)
    val needKeyText = stringResource(R.string.settings_automation_need_key)
    val needPresetText = stringResource(R.string.settings_automation_need_preset)
    val needWatermarkText = stringResource(R.string.settings_automation_need_watermark)

    val btPerms = if (Build.VERSION.SDK_INT >= 31) {
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    fun setOpen(key: String, value: Boolean) {
        when (key) {
            "grok", "ai" -> openGrok = value
            "printer" -> openPrinter = value
            "ftp" -> openFtp = value
            "printing" -> openPrinting = value
            "autoprint" -> openAutoPrint = value
            "watermark" -> openWatermark = value
            "activity" -> openActivity = value
            "language" -> openLanguage = value
        }
    }

    fun revealSection(key: String) {
        if (key.isBlank()) return
        setOpen(key, true)
        pendingScroll = key
        scrollTick += 1
    }

    // Setting the locale makes the platform recreate the activity. Every draft on this screen is
    // rememberSaveable, so it comes back through the saved-instance Bundle; the API key and FTP
    // password are deliberately not, and are cleared - both fields already treat blank as "keep
    // what is stored", so nothing is lost.
    //
    // LocaleManager is called directly rather than through AppCompatDelegate: the AppCompat entry
    // point resolves the system service through a Context it only ever receives from
    // AppCompatActivity, so with a ComponentActivity host it drops the call and returns normally.
    // That failure is silent - nothing is written and nothing throws.
    fun applyLocale(tag: String) {
        if (Build.VERSION.SDK_INT < 33) return
        val manager = context.getSystemService(LocaleManager::class.java) ?: return
        localeTag = tag
        manager.applicationLocales =
            if (tag.isEmpty()) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
    }

    fun openIntent(intent: Intent) {
        runCatching {
            if (activity != null) {
                activity.startActivity(intent)
            } else {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }

    fun startScan() {
        btEnabled = bluetooth.isEnabled()
        if (!btEnabled || scanJob != null) return
        val job = scope.launch(start = CoroutineStart.LAZY) {
            val started = System.currentTimeMillis()
            try {
                devices = bluetooth.scan()
            } catch (_: Exception) {
                // Cancelled by the user or refused by the adapter: keep whatever is already listed.
            } finally {
                progressStats.recordDuration(JobKind.BtScan, "scanning", System.currentTimeMillis() - started)
                scanJob = null
            }
        }
        scanJob = job
        job.start()
    }

    // The binding is written only after createBond() actually succeeds, otherwise the home screen
    // would report a ready printer that every print job then fails on.
    fun bindDevice(device: NearbyDevice) {
        if (pairingMac.isNotEmpty()) return
        pairingMac = device.mac
        scope.launch {
            val started = System.currentTimeMillis()
            val ok = runCatching { bluetooth.pair(device.mac) }.getOrDefault(false)
            progressStats.recordDuration(JobKind.BtPair, "pairing", System.currentTimeMillis() - started)
            if (ok) {
                settings.updateSettings { copy(printerMac = device.mac, printerName = device.name) }
            }
            pairingMac = ""
            // Success needs no snackbar: the row the user just tapped turns into "bound" with a
            // check next to it. A failure has nothing to show in place, so it still gets one.
            if (!ok) snackbar.showSnackbar(pairFailedHint)
        }
    }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        val denied = granted.filterValues { !it }.keys
        if (denied.isEmpty()) {
            permState = PERM_OK
            startScan()
        } else {
            val canAskAgain = activity != null && denied.any { activity.shouldShowRequestPermissionRationale(it) }
            permState = if (canAskAgain) PERM_DENIED else PERM_BLOCKED
        }
    }

    LifecycleResumeEffect(Unit) {
        btEnabled = bluetooth.isEnabled()
        localeTag = currentLocaleTag(context)
        localNetworkMissing = !hasLocalNetworkPermission(context)
        if (scanJob == null) {
            val bonded = bluetooth.bonded()
            devices = bonded + devices.filter { known -> bonded.none { it.mac == known.mac } }
        }
        onPauseOrDispose { }
    }

    LaunchedEffect(pendingScroll, scrollTick) {
        val key = pendingScroll
        if (key.isBlank()) return@LaunchedEffect
        highlighted = key
        delay(80)
        val top = sectionTops[key]
        if (top != null) {
            val target = (scrollState.value + top - viewportTop[0] - 12f)
                .coerceIn(0f, scrollState.maxValue.toFloat())
            scrollState.animateScrollTo(target.roundToInt())
        }
        delay(HIGHLIGHT_MS)
        highlighted = ""
        pendingScroll = ""
    }

    LaunchedEffect(section) { revealSection(section) }

    val currentDraft = if (formProvider == AiProvider.GROK) grokDraft else openaiDraft
    val storedForm = ai.byProvider[formProvider]

    LaunchedEffect(openGrok, formProvider, storedForm?.keySet, ai.cachedModels[formProvider]) {
        if (!openGrok) return@LaunchedEffect
        if (storedForm?.keySet != true) return@LaunchedEffect
        if (!ai.cachedModels[formProvider].isNullOrEmpty()) return@LaunchedEffect
        if (probeJob != null) return@LaunchedEffect
        probeJob = scope.launch {
            try {
                val result = grokClient.listModels(formProvider, currentDraft.url, currentDraft.key)
                modelRefreshFailed = result !is AiProbe.Ok
            } finally {
                probeJob = null
            }
        }
    }
    val grokDirty = currentDraft.url.trim() != storedForm?.baseUrl ||
        currentDraft.key.isNotBlank() ||
        currentDraft.model != storedForm?.model ||
        (formProvider == AiProvider.GROK && currentDraft.resolution != storedForm?.resolution) ||
        currentDraft.quality != storedForm?.quality
    val normalizedGrokUrl = normalizeAiBaseUrl(currentDraft.url, formProvider.defaultBaseUrl)

    val portValue = ftpPort.toIntOrNull()
    val minValue = pasvMin.toIntOrNull()
    val maxValue = pasvMax.toIntOrNull()
    val portError = if (portValue == null || portValue !in 1..65535) portErrorText else null
    val passwordError = if (ftpPassword.isNotEmpty() && ftpPassword.length < 8) passwordErrorText else null
    val pasvMinError = when {
        minValue == null || minValue !in 1024..65535 -> pasvRangeError
        maxValue != null && minValue >= maxValue -> pasvOrderError
        maxValue != null && maxValue - minValue + 1 < 4 -> pasvSpanError
        else -> null
    }
    val pasvMaxError = if (maxValue == null || maxValue !in 1024..65535) pasvRangeError else null
    val ftpInvalid = portError != null || passwordError != null || pasvMinError != null || pasvMaxError != null

    fun commitFtp(restart: Boolean) {
        val port = ftpPort.toIntOrNull() ?: return
        val min = pasvMin.toIntOrNull() ?: return
        val max = pasvMax.toIntOrNull() ?: return
        val wasListening = ftpListening
        // Bound outside the lambda: inside it, these names would also match AppSettings members.
        val userInput = ftpUser.ifBlank { "camera" }
        val passwordInput = ftpPassword
        settings.updateSettings {
            copy(
                ftpUser = userInput,
                ftpPassword = passwordInput.ifBlank { ftpPassword },
                ftpPort = port,
                ftpPasvMin = min,
                ftpPasvMax = max,
            )
        }
        val saved = settings.readSettings()
        ftpUser = saved.ftpUser
        ftpPort = saved.ftpPort.toString()
        pasvMin = saved.ftpPasvMin.toString()
        pasvMax = saved.ftpPasvMax.toString()
        ftpPassword = ""
        ftpSubmitted = false
        portBlurred = false
        passwordBlurred = false
        pasvMinBlurred = false
        pasvMaxBlurred = false
        if (restart) FtpForegroundService.restart(context)
        ftpConfirmText = when {
            restart -> ftpSavedRestarted
            wasListening -> ftpSavedPending
            else -> ftpSaved
        }
        scope.launch { ftpConfirm.show() }
    }

    val printerBound = cfg.printerMac.isNotBlank()
    val autoHasPreset = presets.lastSelectedId != AI_PRESET_NONE
    val autoAiReady = ai.keySet
    val autoParts = buildList {
        if (cfg.autoEdit) add(stringResource(R.string.chip_automation_edit))
        if (cfg.autoWatermark) add(stringResource(R.string.chip_automation_watermark))
        if (cfg.autoPrint) add(stringResource(R.string.chip_automation_print))
    }
    val autoSummary = if (autoParts.isEmpty()) {
        stringResource(R.string.settings_off)
    } else {
        autoParts.joinToString(" · ")
    }
    val watermarkParts = buildList {
        if (watermark.sns.enabled) add(stringResource(R.string.settings_watermark_sns))
        if (watermark.date.enabled) add(stringResource(R.string.settings_watermark_date))
    }
    val watermarkSummary = if (watermarkParts.isEmpty()) {
        stringResource(R.string.settings_off)
    } else {
        watermarkParts.joinToString(" · ")
    }
    var watermarkPreviewLandscape by rememberSaveable { mutableStateOf(false) }
    val newestPhoto = photos.firstOrNull()
    val watermarkPhotoFile = newestPhoto?.let { catalog.sourceFile(it.id, "original") }?.takeIf { it.exists() }
    val watermarkDateText = formatWatermarkDate(
        createdAt = newestPhoto?.createdAt.orEmpty(),
        includeTime = watermark.date.includeTime,
        fallbackIso = newestPhoto?.ingestedAt ?: Instant.now().toString(),
    )

    val grokSummary = stringResource(
        R.string.settings_summary_ai,
        if (ai.active == AiProvider.GROK) stringResource(R.string.settings_provider_grok) else stringResource(R.string.settings_provider_openai),
        ai.model,
        if (ai.active == AiProvider.GROK) ai.resolution else ai.quality,
    )
    val printerSummary = if (!printerBound) {
        unboundSummary
    } else {
        stringResource(R.string.settings_summary_printer, cfg.printerName.ifBlank { unboundSummary }, cfg.printerMac.takeLast(5))
    }
    val ftpSummary = stringResource(R.string.settings_summary_ftp, cfg.ftpPort, cfg.ftpUser)
    val printFitLabel = if (cfg.printFit == PrintFit.COVER) {
        stringResource(R.string.print_fit_cover)
    } else {
        stringResource(R.string.print_fit_contain)
    }
    val copiesText = pluralStringResource(R.plurals.copies_fmt, cfg.defaultCopies, cfg.defaultCopies)
    val printingSummary = stringResource(R.string.settings_printing_summary, copiesText, printFitLabel)
    val activitySummary = stringResource(R.string.settings_activity_summary, logs.size)

    // Each name is written the way that language writes it, not translated into the language
    // currently on screen: someone stuck in a language they cannot read has to be able to find
    // their own in the list.
    val languageSummary = when (localeTag) {
        "zh" -> stringResource(R.string.settings_lang_zh)
        "en" -> stringResource(R.string.settings_lang_en)
        "ja" -> stringResource(R.string.settings_lang_ja)
        "ko" -> stringResource(R.string.settings_lang_ko)
        else -> stringResource(R.string.settings_lang_system)
    }

    Scaffold(
        containerColor = Room,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { DarkroomSnackbarHost(snackbar) },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .onGloballyPositioned { viewportTop[0] = it.positionInRoot().y },
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(20.dp),
            ) {
                Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineLarge)
                Text(
                    stringResource(R.string.settings_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = PaperDim,
                    modifier = Modifier.padding(top = 8.dp, bottom = 20.dp),
                )

                SectionLabel(stringResource(R.string.settings_group_setup))

                Fold(
                    title = stringResource(R.string.settings_grok),
                    summary = grokSummary,
                    icon = Icons.Outlined.AutoAwesome,
                    open = openGrok,
                    highlighted = highlighted == "grok" || highlighted == "ai",
                    onToggle = { openGrok = !openGrok },
                    onTop = { sectionTops["grok"] = it },
                ) {
                    fun setCurrentDraft(next: AiDraft) {
                        if (formProvider == AiProvider.GROK) grokDraft = next else openaiDraft = next
                    }
                    val draft = currentDraft
                    val formKeySet = storedForm?.keySet == true
                    val qualities = if (formProvider == AiProvider.GROK) listOf("low", "medium") else listOf("low", "medium", "high")
                    val cached = ai.cachedModels[formProvider].orEmpty()
                    val modelChoices = (cached + draft.model + if (formProvider == AiProvider.GROK) GROK_MODELS else emptyList())
                        .filter { it.isNotBlank() }
                        .distinct()

                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                        listOf(AiProvider.GROK, AiProvider.OPENAI).forEachIndexed { index, provider ->
                            SegmentedButton(
                                selected = formProvider == provider,
                                onClick = {
                                    formProviderId = provider.id
                                    settings.setActiveProvider(provider)
                                    probeResult = null
                                    modelRefreshFailed = false
                                },
                                shape = SegmentedButtonDefaults.itemShape(index, 2),
                                colors = darkroomSegmentedColors(),
                            ) {
                                Text(
                                    stringResource(
                                        if (provider == AiProvider.GROK) {
                                            R.string.settings_provider_grok
                                        } else {
                                            R.string.settings_provider_openai
                                        },
                                    ),
                                )
                            }
                        }
                    }
                    Text(
                        stringResource(R.string.settings_grok_save_hint),
                        color = PaperDim,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                    SettingsField(
                        label = stringResource(R.string.grok_url),
                        value = draft.url,
                        supporting = if (normalizedGrokUrl != draft.url.trim()) {
                            stringResource(R.string.settings_grok_url_preview, normalizedGrokUrl)
                        } else {
                            null
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                        onValueChange = {
                            setCurrentDraft(draft.copy(url = it))
                            probeResult = null
                        },
                    )
                    SettingsField(
                        label = stringResource(R.string.grok_key),
                        value = draft.key,
                        placeholder = stringResource(
                            if (formProvider == AiProvider.GROK) {
                                R.string.grok_key_placeholder
                            } else {
                                R.string.settings_ai_key_placeholder_openai
                            },
                        ),
                        supporting = if (formKeySet) {
                            stringResource(R.string.settings_grok_key_present)
                        } else {
                            stringResource(
                                if (formProvider == AiProvider.GROK) {
                                    R.string.settings_grok_key_absent
                                } else {
                                    R.string.settings_ai_key_absent_openai
                                },
                            )
                        },
                        secret = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        onValueChange = {
                            setCurrentDraft(draft.copy(key = it))
                            probeResult = null
                        },
                    )
                    if (formKeySet) {
                        GhostButton(
                            text = stringResource(R.string.settings_grok_key_clear),
                            fillMaxWidth = false,
                            onClick = { showClearKey = true },
                        )
                    }

                    Text(
                        stringResource(R.string.grok_model),
                        color = PaperDim,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                    )
                    ExposedDropdownMenuBox(
                        expanded = modelMenuOpen,
                        onExpandedChange = { modelMenuOpen = it },
                    ) {
                        androidx.compose.material3.TextField(
                            value = draft.model,
                            onValueChange = {
                                setCurrentDraft(draft.copy(model = it))
                                probeResult = null
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(modelMenuOpen) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryEditable, true),
                            colors = darkroomTextFieldColors(),
                            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = MonoFont),
                        )
                        ExposedDropdownMenu(
                            expanded = modelMenuOpen,
                            onDismissRequest = { modelMenuOpen = false },
                        ) {
                            modelChoices.forEach { model ->
                                DropdownMenuItem(
                                    text = { Text(model, color = Paper, fontFamily = MonoFont) },
                                    onClick = {
                                        setCurrentDraft(draft.copy(model = model))
                                        modelMenuOpen = false
                                    },
                                )
                            }
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        GhostButton(
                            text = stringResource(R.string.settings_ai_refresh_models),
                            enabled = (draft.key.isNotBlank() || formKeySet) && probeJob == null,
                            fillMaxWidth = false,
                        ) {
                            probeUsedSavedKey = draft.key.isBlank()
                            probeTarget = normalizeAiBaseUrl(draft.url, formProvider.defaultBaseUrl)
                            probeResult = null
                            probeJob = scope.launch {
                                val started = System.currentTimeMillis()
                                try {
                                    val result = grokClient.listModels(formProvider, draft.url, draft.key)
                                    probeResult = result
                                    modelRefreshFailed = result !is AiProbe.Ok
                                } finally {
                                    progressStats.recordDuration(
                                        JobKind.Probe,
                                        "probing",
                                        System.currentTimeMillis() - started,
                                    )
                                    probeJob = null
                                }
                            }
                        }
                    }
                    if (modelRefreshFailed) {
                        Text(
                            stringResource(R.string.settings_ai_model_manual),
                            color = PaperDim,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }

                    Text(
                        stringResource(R.string.settings_ai_quality),
                        color = PaperDim,
                        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                    )
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                        qualities.forEachIndexed { index, quality ->
                            SegmentedButton(
                                selected = draft.quality == quality,
                                onClick = { setCurrentDraft(draft.copy(quality = quality)) },
                                shape = SegmentedButtonDefaults.itemShape(index, qualities.size),
                                colors = darkroomSegmentedColors(),
                            ) { Text(quality) }
                        }
                    }

                    if (formProvider == AiProvider.GROK) {
                        Text(
                            stringResource(R.string.grok_resolution),
                            color = PaperDim,
                            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                        )
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                            listOf("1k", "2k").forEachIndexed { index, res ->
                                SegmentedButton(
                                    selected = draft.resolution == res,
                                    onClick = { setCurrentDraft(draft.copy(resolution = res)) },
                                    shape = SegmentedButtonDefaults.itemShape(index, 2),
                                    colors = darkroomSegmentedColors(),
                                ) { Text(res) }
                            }
                        }
                    }

                    val saveGrok: () -> Unit = {
                        val typed = draft.url.trim()
                        settings.updateProvider(
                            formProvider,
                            draft.key.ifBlank { null },
                            draft.url,
                            draft.model,
                            if (formProvider == AiProvider.GROK) draft.resolution else null,
                            draft.quality,
                        )
                        val saved = settings.readAi().byProvider.getValue(formProvider)
                        val next = AiDraft(url = saved.baseUrl, model = saved.model, resolution = saved.resolution, quality = saved.quality)
                        setCurrentDraft(next)
                        grokConfirmText = if (saved.baseUrl != typed) {
                            context.getString(R.string.settings_grok_url_saved, saved.baseUrl)
                        } else {
                            grokSaved
                        }
                        probeResult = null
                        scope.launch { grokConfirm.show() }
                    }
                    if (grokDirty) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(Modifier.weight(1f)) {
                                PaperButton(text = stringResource(R.string.save_grok), onClick = saveGrok)
                            }
                            Box(Modifier.weight(1f)) {
                                GhostButton(text = stringResource(R.string.settings_discard)) {
                                    storedForm?.let {
                                        setCurrentDraft(
                                            AiDraft(
                                                url = it.baseUrl,
                                                model = it.model,
                                                resolution = it.resolution,
                                                quality = it.quality,
                                            ),
                                        )
                                    }
                                    probeResult = null
                                }
                            }
                        }
                        Text(
                            stringResource(R.string.settings_unsaved),
                            color = Amber,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    } else {
                        PaperButton(
                            text = stringResource(R.string.save_grok),
                            enabled = false,
                            fillMaxWidth = false,
                            onClick = saveGrok,
                        )
                    }
                    InlineConfirm(grokConfirm, grokConfirmText, Modifier.padding(top = 8.dp))

                    Row(
                        Modifier.fillMaxWidth().padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (probeJob == null) {
                            GhostButton(
                                text = stringResource(R.string.settings_grok_test),
                                enabled = draft.key.isNotBlank() || formKeySet,
                                fillMaxWidth = false,
                            ) {
                                probeTarget = normalizeAiBaseUrl(draft.url, formProvider.defaultBaseUrl)
                                probeUsedSavedKey = draft.key.isBlank()
                                probeResult = null
                                probeJob = scope.launch {
                                    val started = System.currentTimeMillis()
                                    try {
                                        probeResult = grokClient.listModels(formProvider, draft.url, draft.key)
                                        modelRefreshFailed = probeResult !is AiProbe.Ok
                                    } finally {
                                        progressStats.recordDuration(
                                            JobKind.Probe,
                                            "probing",
                                            System.currentTimeMillis() - started,
                                        )
                                        probeJob = null
                                    }
                                }
                            }
                        } else {
                            GhostButton(
                                text = stringResource(R.string.common_cancel),
                                fillMaxWidth = false,
                                onClick = { probeJob?.cancel() },
                            )
                        }
                    }
                    if (probeJob != null) {
                        TimedWaitBar(
                            label = stringResource(R.string.settings_grok_test_running),
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    probeResult?.let { probe ->
                        val modelMissing = probe is AiProbe.Ok &&
                            probe.models.isNotEmpty() &&
                            draft.model !in probe.models
                        val text = if (modelMissing) {
                            stringResource(R.string.settings_grok_test_model_missing, draft.model)
                        } else {
                            when (probe) {
                                is AiProbe.Ok -> stringResource(R.string.settings_grok_test_ok, probe.elapsedMs)
                                is AiProbe.Unauthorized -> stringResource(R.string.settings_grok_test_auth, probe.status)
                                is AiProbe.NotFound -> stringResource(R.string.settings_grok_test_notfound)
                                is AiProbe.HttpError -> stringResource(R.string.settings_grok_test_http, probe.status)
                                is AiProbe.Unreachable -> stringResource(R.string.settings_grok_test_unreachable)
                                is AiProbe.Timeout -> stringResource(R.string.settings_grok_test_timeout)
                                is AiProbe.NoKey -> stringResource(R.string.settings_grok_test_nokey)
                                is AiProbe.InvalidUrl -> stringResource(R.string.settings_grok_test_badurl)
                            }
                        }
                        val passed = probe is AiProbe.Ok && !modelMissing
                        val keySource = stringResource(
                            if (probeUsedSavedKey) {
                                R.string.settings_grok_test_key_saved
                            } else {
                                R.string.settings_grok_test_key_draft
                            },
                        )
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                                .border(1.dp, if (passed) PaperHairline else Amber, RoundedCornerShape(2.dp))
                                .padding(12.dp)
                                .semantics { liveRegion = LiveRegionMode.Polite },
                        ) {
                            Text(text, color = if (passed) Paper else Amber, fontSize = 13.sp, lineHeight = 18.sp)
                            Text(
                                stringResource(R.string.settings_grok_test_target, probeTarget, keySource),
                                color = PaperDim,
                                fontFamily = MonoFont,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                    }
                }

                Fold(
                    title = stringResource(R.string.settings_printer),
                    summary = printerSummary,
                    icon = Icons.Outlined.Print,
                    open = openPrinter,
                    highlighted = highlighted == "printer",
                    onToggle = { openPrinter = !openPrinter },
                    onTop = { sectionTops["printer"] = it },
                ) {
                    Text(
                        if (!printerBound) stringResource(R.string.chip_printer_off) else "${cfg.printerName} ${cfg.printerMac}",
                        fontFamily = MonoFont,
                        color = Paper,
                    )
                    if (printerBound) {
                        GhostButton(
                            text = stringResource(R.string.unbind_printer),
                            fillMaxWidth = false,
                        ) {
                            // No confirmation message: the line above turns into "not bound" and
                            // the section summary follows it, which says it better than a snackbar.
                            settings.updateSettings { copy(printerMac = "", printerName = "", autoPrint = false) }
                        }
                    }

                    if (!btEnabled) {
                        Notice(stringResource(R.string.settings_bt_off)) {
                            GhostButton(
                                text = stringResource(R.string.settings_bt_open),
                                fillMaxWidth = false,
                                onClick = { openIntent(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) },
                            )
                        }
                    }
                    if (permState != PERM_OK) {
                        Notice(
                            if (permState == PERM_BLOCKED) {
                                stringResource(R.string.settings_perm_denied_permanent)
                            } else {
                                stringResource(R.string.settings_perm_denied)
                            },
                        ) {
                            if (permState == PERM_DENIED) {
                                GhostButton(
                                    text = stringResource(R.string.settings_perm_retry),
                                    fillMaxWidth = false,
                                    onClick = { permLauncher.launch(btPerms) },
                                )
                            } else {
                                GhostButton(
                                    text = stringResource(R.string.settings_app_settings),
                                    fillMaxWidth = false,
                                    onClick = {
                                        openIntent(
                                            Intent(
                                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                                Uri.fromParts("package", context.packageName, null),
                                            ),
                                        )
                                    },
                                )
                            }
                        }
                    }

                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (scanJob == null) {
                            GhostButton(
                                text = stringResource(R.string.scan_nearby),
                                enabled = btEnabled,
                                fillMaxWidth = false,
                                onClick = { permLauncher.launch(btPerms) },
                            )
                        } else {
                            GhostButton(
                                text = stringResource(R.string.settings_scan_stop),
                                fillMaxWidth = false,
                                onClick = { scanJob?.cancel() },
                            )
                        }
                    }
                    if (scanJob != null) {
                        TimedWaitBar(
                            label = stringResource(R.string.phase_bt_scan),
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }

                    val paired = devices.filter { it.bonded }
                    val discovered = devices.filter { !it.bonded }
                    if (devices.isEmpty()) {
                        Text(stringResource(R.string.settings_devices_empty), color = PaperDim, fontSize = 13.sp)
                    }
                    if (paired.isNotEmpty()) {
                        Text(
                            stringResource(R.string.settings_devices_paired),
                            color = PaperDim,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            fontFamily = MonoFont,
                            modifier = Modifier.padding(top = 6.dp, bottom = 4.dp),
                        )
                        paired.forEach { device ->
                            DeviceRow(
                                device = device,
                                bound = device.mac == cfg.printerMac,
                                pairing = pairingMac == device.mac,
                                enabled = pairingMac.isEmpty(),
                            ) { bindDevice(device) }
                        }
                    }
                    if (discovered.isNotEmpty()) {
                        Text(
                            stringResource(R.string.settings_devices_found),
                            color = PaperDim,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            fontFamily = MonoFont,
                            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                        )
                        discovered.forEach { device ->
                            DeviceRow(
                                device = device,
                                bound = device.mac == cfg.printerMac,
                                pairing = pairingMac == device.mac,
                                enabled = pairingMac.isEmpty(),
                            ) { bindDevice(device) }
                        }
                    }
                }

                Fold(
                    title = stringResource(R.string.settings_ftp),
                    summary = ftpSummary,
                    icon = Icons.Outlined.WifiTethering,
                    open = openFtp,
                    highlighted = highlighted == "ftp",
                    onToggle = { openFtp = !openFtp },
                    onTop = { sectionTops["ftp"] = it },
                ) {
                    if (localNetworkMissing) {
                        Text(
                            stringResource(R.string.ftp_local_network_title),
                            color = Amber,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 12.dp),
                        )
                    }
                    SettingsField(
                        label = stringResource(R.string.ftp_user),
                        value = ftpUser,
                        supporting = stringResource(R.string.settings_ftp_user_hint),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        onValueChange = { ftpUser = it },
                    )
                    SettingsField(
                        label = stringResource(R.string.ftp_password),
                        value = ftpPassword,
                        placeholder = "••••••••",
                        supporting = if (passwordError != null && (passwordBlurred || ftpSubmitted)) {
                            passwordError
                        } else {
                            stringResource(R.string.settings_ftp_password_hint)
                        },
                        isError = passwordError != null && (passwordBlurred || ftpSubmitted),
                        secret = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                        onBlur = { passwordBlurred = true },
                        onValueChange = { ftpPassword = it },
                    )
                    SettingsField(
                        label = stringResource(R.string.ftp_port),
                        value = ftpPort,
                        supporting = when {
                            portError != null && (portBlurred || ftpSubmitted) -> portError
                            portValue != null && portValue < 1024 -> stringResource(R.string.ftp_port21_hint)
                            else -> null
                        },
                        isError = portError != null && (portBlurred || ftpSubmitted),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        onBlur = { portBlurred = true },
                        onValueChange = { ftpPort = it },
                    )
                    Text(
                        stringResource(R.string.settings_ftp_pasv_desc),
                        color = PaperDim,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    SettingsField(
                        label = stringResource(R.string.settings_ftp_pasv_min),
                        value = pasvMin,
                        supporting = if (pasvMinError != null && (pasvMinBlurred || ftpSubmitted)) pasvMinError else null,
                        isError = pasvMinError != null && (pasvMinBlurred || ftpSubmitted),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        onBlur = { pasvMinBlurred = true },
                        onValueChange = { pasvMin = it },
                    )
                    SettingsField(
                        label = stringResource(R.string.settings_ftp_pasv_max),
                        value = pasvMax,
                        supporting = if (pasvMaxError != null && (pasvMaxBlurred || ftpSubmitted)) pasvMaxError else null,
                        isError = pasvMaxError != null && (pasvMaxBlurred || ftpSubmitted),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        onBlur = { pasvMaxBlurred = true },
                        onValueChange = { pasvMax = it },
                    )
                    PaperButton(text = stringResource(R.string.common_save), fillMaxWidth = false) {
                        ftpSubmitted = true
                        if (!ftpInvalid) {
                            if (ftpListening) showFtpRestart = true else commitFtp(false)
                        }
                    }
                    InlineConfirm(ftpConfirm, ftpConfirmText, Modifier.padding(top = 8.dp))
                }

                SectionLabel(stringResource(R.string.settings_group_printing))

                Fold(
                    title = stringResource(R.string.settings_printing),
                    summary = printingSummary,
                    icon = Icons.Outlined.Tune,
                    open = openPrinting,
                    highlighted = highlighted == "printing",
                    onToggle = { openPrinting = !openPrinting },
                    onTop = { sectionTops["printing"] = it },
                ) {
                    Text(stringResource(R.string.settings_default_copies), color = Paper)
                    Text(
                        stringResource(R.string.settings_default_copies_desc),
                        color = PaperDim,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    CopiesStepper(cfg.defaultCopies) { next ->
                        settings.updateSettings { copy(defaultCopies = next) }
                    }
                    Text(
                        stringResource(R.string.print_fit),
                        color = PaperDim,
                        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                    )
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        val fits = listOf(
                            PrintFit.COVER to stringResource(R.string.print_fit_cover),
                            PrintFit.CONTAIN to stringResource(R.string.print_fit_contain),
                        )
                        fits.forEachIndexed { index, (fit, label) ->
                            SegmentedButton(
                                selected = cfg.printFit == fit,
                                onClick = { settings.updateSettings { copy(printFit = fit) } },
                                shape = SegmentedButtonDefaults.itemShape(index, fits.size),
                                colors = darkroomSegmentedColors(),
                            ) { Text(label) }
                        }
                    }
                }

                Fold(
                    title = stringResource(R.string.settings_automation),
                    summary = autoSummary,
                    icon = Icons.Outlined.Bolt,
                    open = openAutoPrint,
                    highlighted = highlighted == "autoprint",
                    onToggle = { openAutoPrint = !openAutoPrint },
                    onTop = { sectionTops["autoprint"] = it },
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .toggleable(
                                value = cfg.autoEdit,
                                role = Role.Switch,
                                onValueChange = { on -> settings.updateSettings { copy(autoEdit = on) } },
                            )
                            .semantics(mergeDescendants = true) {},
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("①", color = Amber, fontFamily = MonoFont, modifier = Modifier.padding(end = 10.dp))
                        Text(stringResource(R.string.settings_automation_step_edit), color = Paper, modifier = Modifier.weight(1f))
                        Switch(
                            checked = cfg.autoEdit,
                            onCheckedChange = null,
                            colors = SwitchDefaults.colors(checkedTrackColor = Amber),
                        )
                    }
                    PresetDropdown(
                        value = presets.lastSelectedId,
                        presets = presets.presets,
                        label = stringResource(R.string.settings_automation_preset),
                    ) { settings.setLastPreset(it) }
                    if (cfg.autoEdit && (!autoAiReady || !autoHasPreset)) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, bottom = 4.dp)
                                .border(1.dp, Amber.copy(alpha = 0.7f), RoundedCornerShape(2.dp))
                                .padding(12.dp),
                        ) {
                            Text(stringResource(R.string.settings_automation_blocked_edit), color = Amber, fontSize = 13.sp)
                            if (!autoAiReady) {
                                Text(
                                    needKeyText,
                                    color = Paper,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(top = 6.dp),
                                )
                                TextButton(onClick = { revealSection("grok") }) {
                                    Text(stringResource(R.string.settings_goto_ai), color = Amber)
                                }
                            }
                            if (!autoHasPreset) {
                                Text(
                                    needPresetText,
                                    color = Paper,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(top = 6.dp),
                                )
                            }
                        }
                    }
                    Text("│", color = PaperDim, fontFamily = MonoFont, modifier = Modifier.padding(start = 2.dp, top = 2.dp, bottom = 2.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .toggleable(
                                value = cfg.autoWatermark,
                                role = Role.Switch,
                                onValueChange = { on -> settings.updateSettings { copy(autoWatermark = on) } },
                            )
                            .semantics(mergeDescendants = true) {},
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("②", color = Amber, fontFamily = MonoFont, modifier = Modifier.padding(end = 10.dp))
                        Text(stringResource(R.string.settings_automation_step_watermark), color = Paper, modifier = Modifier.weight(1f))
                        Switch(
                            checked = cfg.autoWatermark,
                            onCheckedChange = null,
                            colors = SwitchDefaults.colors(checkedTrackColor = Amber),
                        )
                    }
                    if (!cfg.autoPrint) {
                        Text(
                            stringResource(R.string.settings_automation_watermark_print_only),
                            color = PaperDim,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
                        )
                    }
                    if (!watermark.isConfigured()) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, bottom = 4.dp)
                                .border(1.dp, Amber.copy(alpha = 0.7f), RoundedCornerShape(2.dp))
                                .padding(12.dp),
                        ) {
                            Text(stringResource(R.string.settings_automation_blocked_watermark), color = Amber, fontSize = 13.sp)
                            Text(
                                needWatermarkText,
                                color = Paper,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                            TextButton(onClick = { revealSection("watermark") }) {
                                Text(stringResource(R.string.settings_goto_watermark), color = Amber)
                            }
                        }
                    }
                    Text("│", color = PaperDim, fontFamily = MonoFont, modifier = Modifier.padding(start = 2.dp, top = 2.dp, bottom = 2.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .toggleable(
                                value = cfg.autoPrint,
                                role = Role.Switch,
                                onValueChange = { on ->
                                    when {
                                        !on -> settings.updateSettings { copy(autoPrint = false) }
                                        printerBound -> settings.updateSettings { copy(autoPrint = true) }
                                        else -> scope.launch { snackbar.showSnackbar(needPrinterText) }
                                    }
                                },
                            )
                            .semantics(mergeDescendants = true) {},
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("③", color = Amber, fontFamily = MonoFont, modifier = Modifier.padding(end = 10.dp))
                        Text(stringResource(R.string.settings_automation_step_print), color = Paper, modifier = Modifier.weight(1f))
                        Switch(
                            checked = cfg.autoPrint,
                            onCheckedChange = null,
                            colors = SwitchDefaults.colors(checkedTrackColor = Amber),
                        )
                    }
                    if (!printerBound) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 4.dp)
                                .border(1.dp, Amber.copy(alpha = 0.7f), RoundedCornerShape(2.dp))
                                .padding(12.dp),
                        ) {
                            Text(stringResource(R.string.settings_automation_blocked_print), color = Amber, fontSize = 13.sp)
                            Text(
                                needPrinterText,
                                color = Paper,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                            TextButton(onClick = { revealSection("printer") }) {
                                Text(stringResource(R.string.settings_goto_printer), color = Amber)
                            }
                        }
                    }
                }

                Fold(
                    title = stringResource(R.string.settings_watermark),
                    summary = watermarkSummary,
                    icon = Icons.Outlined.BrandingWatermark,
                    open = openWatermark,
                    highlighted = highlighted == "watermark",
                    onToggle = { openWatermark = !openWatermark },
                    onTop = { sectionTops["watermark"] = it },
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .toggleable(
                                value = watermark.sns.enabled,
                                role = Role.Switch,
                                onValueChange = { on ->
                                    settings.updateWatermark { copy(sns = sns.copy(enabled = on)) }
                                },
                            )
                            .semantics(mergeDescendants = true) {},
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.settings_watermark_sns), color = Paper, modifier = Modifier.weight(1f))
                        Switch(
                            checked = watermark.sns.enabled,
                            onCheckedChange = null,
                            colors = SwitchDefaults.colors(checkedTrackColor = Amber),
                        )
                    }
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        SnsLogo.entries.forEachIndexed { index, logo ->
                            SegmentedButton(
                                selected = watermark.sns.logo == logo,
                                onClick = { settings.updateWatermark { copy(sns = sns.copy(logo = logo)) } },
                                shape = SegmentedButtonDefaults.itemShape(index, SnsLogo.entries.size),
                                colors = darkroomSegmentedColors(),
                                icon = {},
                            ) {
                                val res = WatermarkRenderer.snsLogoRes(logo)
                                if (res != null) {
                                    Icon(
                                        painter = painterResource(res),
                                        contentDescription = stringResource(snsLogoLabel(logo)),
                                        modifier = Modifier.size(18.dp),
                                    )
                                } else {
                                    Text(
                                        stringResource(snsLogoLabel(logo)),
                                        maxLines = 1,
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                }
                            }
                        }
                    }
                    SettingsField(
                        label = stringResource(R.string.settings_watermark_handle),
                        value = watermark.sns.handle,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        onValueChange = { next ->
                            settings.updateWatermark { copy(sns = sns.copy(handle = next)) }
                        },
                    )
                    Text(
                        stringResource(R.string.settings_watermark_size),
                        color = PaperDim,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    Slider(
                        value = watermark.sns.scale,
                        onValueChange = { next ->
                            settings.updateWatermark { copy(sns = sns.copy(scale = clampWatermarkScale(next))) }
                        },
                        valueRange = WATERMARK_SCALE_MIN..WATERMARK_SCALE_MAX,
                        colors = SliderDefaults.colors(thumbColor = Amber, activeTrackColor = Amber),
                    )
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .padding(top = 8.dp)
                            .toggleable(
                                value = watermark.date.enabled,
                                role = Role.Switch,
                                onValueChange = { on ->
                                    settings.updateWatermark { copy(date = date.copy(enabled = on)) }
                                },
                            )
                            .semantics(mergeDescendants = true) {},
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.settings_watermark_date), color = Paper, modifier = Modifier.weight(1f))
                        Switch(
                            checked = watermark.date.enabled,
                            onCheckedChange = null,
                            colors = SwitchDefaults.colors(checkedTrackColor = Amber),
                        )
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .toggleable(
                                value = watermark.date.includeTime,
                                role = Role.Switch,
                                onValueChange = { on ->
                                    settings.updateWatermark { copy(date = date.copy(includeTime = on)) }
                                },
                            )
                            .semantics(mergeDescendants = true) {},
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.settings_watermark_include_time), color = Paper, modifier = Modifier.weight(1f))
                        Switch(
                            checked = watermark.date.includeTime,
                            onCheckedChange = null,
                            colors = SwitchDefaults.colors(checkedTrackColor = Amber),
                        )
                    }
                    Text(
                        stringResource(R.string.settings_watermark_size),
                        color = PaperDim,
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
                    )
                    Slider(
                        value = watermark.date.scale,
                        onValueChange = { next ->
                            settings.updateWatermark { copy(date = date.copy(scale = clampWatermarkScale(next))) }
                        },
                        valueRange = WATERMARK_SCALE_MIN..WATERMARK_SCALE_MAX,
                        colors = SliderDefaults.colors(thumbColor = Amber, activeTrackColor = Amber),
                    )
                    Text(
                        stringResource(R.string.settings_watermark_orientation_hint),
                        color = PaperDim,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
                    )
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        listOf(false, true).forEachIndexed { index, landscape ->
                            SegmentedButton(
                                selected = watermarkPreviewLandscape == landscape,
                                onClick = { watermarkPreviewLandscape = landscape },
                                shape = SegmentedButtonDefaults.itemShape(index, 2),
                                colors = darkroomSegmentedColors(),
                                icon = {},
                            ) {
                                Text(
                                    stringResource(
                                        if (landscape) R.string.studio_frame_landscape else R.string.studio_frame_portrait,
                                    ),
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                    WatermarkPreviewEditor(
                        settings = watermark,
                        photoFile = watermarkPhotoFile,
                        dateText = watermarkDateText,
                        landscape = watermarkPreviewLandscape,
                        onAnchorChange = { id, landscape, anchor ->
                            settings.updateWatermark {
                                when (id) {
                                    "sns" -> copy(sns = sns.withAnchor(landscape, anchor))
                                    "date" -> copy(date = date.withAnchor(landscape, anchor))
                                    else -> this
                                }
                            }
                        },
                        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                    )
                    GhostButton(text = stringResource(R.string.settings_watermark_reset)) {
                        settings.updateWatermark { withDefaultAnchors() }
                    }
                }

                SectionLabel(stringResource(R.string.settings_group_diagnostics))

                Fold(
                    title = stringResource(R.string.recent_activity),
                    summary = activitySummary,
                    icon = Icons.Outlined.History,
                    open = openActivity,
                    highlighted = highlighted == "activity",
                    onToggle = { openActivity = !openActivity },
                    onTop = { sectionTops["activity"] = it },
                ) {
                    if (logs.isEmpty()) {
                        Text(stringResource(R.string.activity_empty), color = PaperDim)
                    } else {
                        // Half the row each rather than hugging their labels: three ghost buttons
                        // never fit on one 360dp line, and at a 2x font scale even two would
                        // overflow if they sized themselves to their text.
                        Row(
                            Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(Modifier.weight(1f)) {
                                GhostButton(text = stringResource(R.string.settings_log_copy)) {
                                    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    manager.setPrimaryClip(ClipData.newPlainText(logShareTitle, buildLogReport(logs)))
                                    scope.launch { snackbar.showSnackbar(logCopied) }
                                }
                            }
                            Box(Modifier.weight(1f)) {
                                GhostButton(text = stringResource(R.string.settings_log_share)) {
                                    val send = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, logShareTitle)
                                        putExtra(Intent.EXTRA_TEXT, buildLogReport(logs))
                                    }
                                    openIntent(Intent.createChooser(send, logShareTitle))
                                }
                            }
                        }
                        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                            GhostButton(
                                text = stringResource(R.string.settings_log_clear),
                                fillMaxWidth = false,
                                onClick = { showClearLog = true },
                            )
                        }
                    }
                    val expandLabel = stringResource(R.string.settings_log_expand)
                    logs.take(20).forEach { entry ->
                        val failed = entry.status == "error" || !entry.error.isNullOrBlank()
                        val hasDetail = !entry.error.isNullOrBlank()
                        val open = expandedLog == entry.id
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .then(
                                    if (hasDetail) {
                                        Modifier.clickable(
                                            onClickLabel = expandLabel,
                                            role = Role.Button,
                                            onClick = { expandedLog = if (open) "" else entry.id },
                                        )
                                    } else {
                                        Modifier
                                    },
                                )
                                .semantics(mergeDescendants = true) {}
                                .padding(vertical = 3.dp),
                        ) {
                            Text(
                                "${formatLogTime(entry.at, timeFormatter)}  ${logKindLabel(entry.kind)}  ${entry.message}",
                                color = if (failed) Danger else PaperDim,
                                fontFamily = MonoFont,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                            )
                            if (open) {
                                // The raw exception text, deliberately not run through
                                // UserErrorMessage: this row exists to be copied into a bug report,
                                // and a friendly rewrite is the one thing that is no use there.
                                Text(
                                    entry.error.orEmpty(),
                                    color = Danger,
                                    fontFamily = MonoFont,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    modifier = Modifier.padding(top = 2.dp, start = 8.dp),
                                )
                            }
                        }
                    }
                }

                SectionLabel(stringResource(R.string.settings_group_app))

                Fold(
                    title = stringResource(R.string.settings_language),
                    summary = languageSummary,
                    icon = Icons.Outlined.Language,
                    open = openLanguage,
                    highlighted = highlighted == "language",
                    onToggle = { openLanguage = !openLanguage },
                    onTop = { sectionTops["language"] = it },
                ) {
                    if (Build.VERSION.SDK_INT >= 33) {
                        Text(
                            stringResource(R.string.settings_language_desc),
                            color = PaperDim,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        val choices = listOf(
                            "" to stringResource(R.string.settings_lang_system),
                            "zh" to stringResource(R.string.settings_lang_zh),
                            "en" to stringResource(R.string.settings_lang_en),
                            "ja" to stringResource(R.string.settings_lang_ja),
                            "ko" to stringResource(R.string.settings_lang_ko),
                        )
                        Column(Modifier.fillMaxWidth().selectableGroup()) {
                            choices.forEach { (tag, label) ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .selectable(
                                            selected = localeTag == tag,
                                            role = Role.RadioButton,
                                            onClick = { if (localeTag != tag) applyLocale(tag) },
                                        )
                                        .semantics(mergeDescendants = true) {},
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    RadioButton(
                                        selected = localeTag == tag,
                                        onClick = null,
                                        colors = RadioButtonDefaults.colors(selectedColor = Amber),
                                    )
                                    Text(label, color = Paper)
                                }
                            }
                        }
                    } else {
                        // Per-app locales are a platform feature from API 33 on. The only backport
                        // is AppCompat's, and it applies the locale through the delegates
                        // registered by AppCompatActivity, which this pure-Compose app does not
                        // use. Offering the picker here would set a preference that never takes
                        // effect, so the fold explains the situation instead.
                        Text(
                            stringResource(R.string.settings_language_unsupported),
                            color = PaperDim,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                        )
                    }
                }

                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                        .border(1.dp, PaperFaint, RoundedCornerShape(2.dp))
                        .heightIn(min = 48.dp)
                        .clickable(role = Role.Button, onClick = onAbout)
                        .semantics(mergeDescendants = true) {}
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Info, contentDescription = null, tint = PaperDim, modifier = Modifier.size(20.dp))
                    Column(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
                        Text(stringResource(R.string.settings_about), color = Amber)
                        Text(
                            stringResource(R.string.settings_about_summary),
                            color = PaperDim,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = PaperDim)
                }

                GhostButton(
                    text = stringResource(R.string.settings_reset),
                    onClick = { showReset = true },
                )
            }
        }
    }

    if (showClearKey) {
        AlertDialog(
            onDismissRequest = { showClearKey = false },
            title = { Text(stringResource(R.string.settings_grok_key_clear_title)) },
            text = { Text(stringResource(R.string.settings_grok_key_clear_text)) },
            confirmButton = {
                TextButton(onClick = {
                    settings.clearProviderKey(formProvider)
                    if (formProvider == AiProvider.GROK) {
                        grokDraft = grokDraft.copy(key = "")
                    } else {
                        openaiDraft = openaiDraft.copy(key = "")
                    }
                    probeResult = null
                    showClearKey = false
                    scope.launch { snackbar.showSnackbar(keyCleared) }
                }) { Text(stringResource(R.string.settings_clear)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearKey = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }

    if (showClearLog) {
        AlertDialog(
            onDismissRequest = { showClearLog = false },
            title = { Text(stringResource(R.string.settings_log_clear_title)) },
            text = { Text(stringResource(R.string.settings_log_clear_text)) },
            confirmButton = {
                TextButton(onClick = {
                    activityLog.clear()
                    expandedLog = ""
                    showClearLog = false
                    scope.launch { snackbar.showSnackbar(logCleared) }
                }) { Text(stringResource(R.string.settings_clear)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearLog = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }

    if (showFtpRestart) {
        AlertDialog(
            onDismissRequest = { showFtpRestart = false },
            title = { Text(stringResource(R.string.settings_ftp_restart_title)) },
            text = { Text(stringResource(R.string.settings_ftp_restart_text)) },
            // Three actions in a two-slot dialog. Stacked rather than side by side: Material lays
            // the button slots out in a flow row, so a column keeps every label at most one
            // dialog wide and nothing is cut off at a 2x font scale.
            confirmButton = {
                Column {
                    TextButton(onClick = {
                        showFtpRestart = false
                        commitFtp(true)
                    }) { Text(stringResource(R.string.settings_ftp_restart_confirm)) }
                    TextButton(onClick = {
                        showFtpRestart = false
                        commitFtp(false)
                    }) { Text(stringResource(R.string.settings_ftp_restart_skip)) }
                }
            },
            dismissButton = {
                TextButton(onClick = { showFtpRestart = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }

    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            title = { Text(stringResource(R.string.settings_reset_title)) },
            text = { Text(stringResource(R.string.settings_reset_text)) },
            confirmButton = {
                TextButton(onClick = {
                    settings.resetAll()
                    val nextCfg = settings.readSettings()
                    val nextAi = settings.readAi()
                    ftpUser = nextCfg.ftpUser
                    ftpPort = nextCfg.ftpPort.toString()
                    pasvMin = nextCfg.ftpPasvMin.toString()
                    pasvMax = nextCfg.ftpPasvMax.toString()
                    ftpPassword = ""
                    formProviderId = nextAi.active.id
                    nextAi.byProvider[AiProvider.GROK]?.let {
                        grokDraft = AiDraft(url = it.baseUrl, model = it.model, resolution = it.resolution, quality = it.quality)
                    }
                    nextAi.byProvider[AiProvider.OPENAI]?.let {
                        openaiDraft = AiDraft(url = it.baseUrl, model = it.model, resolution = it.resolution, quality = it.quality)
                    }
                    probeResult = null
                    showReset = false
                    scope.launch { snackbar.showSnackbar(resetDone) }
                }) { Text(stringResource(R.string.settings_reset_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showReset = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }
}

/**
 * Primary language subtag of the per-app locale, or "" when the app follows the system.
 *
 * Only the first preference and only its language part: the platform is free to store a picked
 * "zh" back as "zh-CN", and the picker has one row per language.
 */
private fun currentLocaleTag(context: Context): String {
    if (Build.VERSION.SDK_INT < 33) return ""
    val manager = context.getSystemService(LocaleManager::class.java) ?: return ""
    return manager.applicationLocales
        .toLanguageTags()
        .substringBefore(',')
        .substringBefore('-')
}

/**
 * The active segment is a Paper fill, so its label and check mark have to be
 * Ink, which measures 14.85:1 there. Paper on the Room fill of an inactive
 * segment measures 15.67:1.
 */
private fun snsLogoLabel(logo: SnsLogo): Int = when (logo) {
    SnsLogo.INSTAGRAM -> R.string.settings_watermark_logo_instagram
    SnsLogo.X -> R.string.settings_watermark_logo_x
    SnsLogo.FACEBOOK -> R.string.settings_watermark_logo_facebook
    SnsLogo.WEIBO -> R.string.settings_watermark_logo_weibo
    SnsLogo.NONE -> R.string.settings_watermark_logo_none
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun darkroomSegmentedColors() = SegmentedButtonDefaults.colors(
    activeContainerColor = Paper,
    activeContentColor = Ink,
    activeBorderColor = PaperFaint,
    inactiveContainerColor = Room,
    inactiveContentColor = Paper,
    inactiveBorderColor = PaperFaint,
    disabledActiveContainerColor = PaperDisabled,
    disabledActiveContentColor = Ink,
    disabledActiveBorderColor = PaperHairline,
    disabledInactiveContainerColor = Room,
    disabledInactiveContentColor = PaperDim,
    disabledInactiveBorderColor = PaperHairline,
)

@Composable
private fun Fold(
    title: String,
    summary: String,
    icon: ImageVector,
    open: Boolean,
    highlighted: Boolean,
    onToggle: () -> Unit,
    onTop: (Float) -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val expandLabel = stringResource(R.string.expand_section)
    val collapseLabel = stringResource(R.string.collapse_section)
    val expandedState = stringResource(R.string.settings_expanded)
    val collapsedState = stringResource(R.string.settings_collapsed)
    val borderColor by animateColorAsState(
        targetValue = if (highlighted) Amber else PaperFaint,
        animationSpec = tween(FOLD_MS),
        label = "foldBorder",
    )
    val arrow by animateFloatAsState(
        targetValue = if (open) 180f else 0f,
        animationSpec = tween(FOLD_MS),
        label = "foldArrow",
    )
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp)
            .onGloballyPositioned { onTop(it.positionInRoot().y) }
            .border(1.dp, borderColor, RoundedCornerShape(2.dp)),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable(
                    onClickLabel = if (open) collapseLabel else expandLabel,
                    role = Role.Button,
                    onClick = onToggle,
                )
                .semantics(mergeDescendants = true) {
                    stateDescription = if (open) expandedState else collapsedState
                }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (open) Amber else PaperDim,
                modifier = Modifier.size(20.dp),
            )
            Column(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
                Text(title, color = Amber)
                if (summary.isNotBlank()) {
                    Text(
                        summary,
                        color = PaperDim,
                        fontSize = 12.sp,
                        fontFamily = MonoFont,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            Icon(
                Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = PaperDim,
                modifier = Modifier.rotate(arrow),
            )
        }
        AnimatedVisibility(
            visible = open,
            enter = expandVertically(tween(FOLD_MS)) + fadeIn(tween(FOLD_MS)),
            exit = shrinkVertically(tween(FOLD_MS)) + fadeOut(tween(FOLD_MS)),
        ) {
            Column(
                Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                content = content,
            )
        }
    }
}

@Composable
private fun Notice(text: String, action: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .border(1.dp, Amber.copy(alpha = 0.7f), RoundedCornerShape(2.dp))
            .padding(12.dp),
    ) {
        Text(text, color = Amber, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
        action()
    }
}

@Composable
private fun DeviceRow(
    device: NearbyDevice,
    bound: Boolean,
    pairing: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val bindLabel = stringResource(R.string.settings_device_bind)
    val printerLike = stringResource(R.string.printer_like)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .border(1.dp, if (bound) Amber else PaperFaint, RoundedCornerShape(2.dp))
            .heightIn(min = 56.dp)
            .clickable(
                enabled = enabled && !bound,
                onClickLabel = bindLabel,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics(mergeDescendants = true) {}
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Outlined.Bluetooth, contentDescription = null, tint = PaperDim, modifier = Modifier.size(18.dp))
        Column(Modifier.weight(1f)) {
            Text(device.name, color = Paper)
            Text(
                device.mac + if (device.printerLike) " · $printerLike" else "",
                color = PaperDim,
                fontFamily = MonoFont,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            )
            device.rssi?.let { rssi ->
                val label = when {
                    rssi >= -60 -> stringResource(R.string.settings_signal_strong)
                    rssi >= -75 -> stringResource(R.string.settings_signal_medium)
                    else -> stringResource(R.string.settings_signal_weak)
                }
                Row(
                    Modifier.padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        Icons.Outlined.SignalCellularAlt,
                        contentDescription = null,
                        tint = if (rssi >= -75) Amber else PaperDim,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        stringResource(R.string.settings_signal_fmt, label, rssi),
                        color = PaperDim,
                        fontFamily = MonoFont,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                    )
                }
            }
        }
        when {
            pairing -> {
                Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.settings_device_pairing), color = Amber, fontSize = 12.sp)
                    TimedWaitBar(
                        label = "",
                        modifier = Modifier.padding(top = 6.dp).fillMaxWidth(0.35f),
                    )
                }
            }
            bound -> {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Amber, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.settings_device_bound), color = Amber, fontSize = 12.sp)
            }
            else -> {
                Text(bindLabel, color = if (enabled) Paper else PaperDim, fontSize = 12.sp)
                Icon(
                    Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = PaperDim,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun CopiesStepper(copies: Int, onChange: (Int) -> Unit) {
    val fewer = stringResource(R.string.copies_decrease)
    val more = stringResource(R.string.copies_increase)
    Row(
        Modifier.fillMaxWidth().padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Icons rather than "−" / "+" glyphs: IconButton is a fixed 48dp box, so at a 2x font
        // scale a 20sp glyph is clipped inside it, while a dp-sized icon is not.
        IconButton(
            enabled = copies > 1,
            onClick = { onChange(copies - 1) },
            modifier = Modifier.semantics { contentDescription = fewer },
        ) {
            Icon(
                Icons.Outlined.Remove,
                contentDescription = null,
                tint = if (copies > 1) Paper else PaperDim,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            "$copies",
            color = Paper,
            fontFamily = MonoFont,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        IconButton(
            enabled = copies < 9,
            onClick = { onChange(copies + 1) },
            modifier = Modifier.semantics { contentDescription = more },
        ) {
            Icon(
                Icons.Outlined.Add,
                contentDescription = null,
                tint = if (copies < 9) Paper else PaperDim,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun SettingsField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    supporting: String? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    secret: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
    onBlur: () -> Unit = {},
    onValueChange: (String) -> Unit,
) {
    var secretVisible by remember { mutableStateOf(false) }
    var wasFocused by remember { mutableStateOf(false) }
    val supportingSlot: @Composable (() -> Unit)? = if (supporting != null) {
        { Text(supporting, color = if (isError) Danger else PaperDim, fontSize = 12.sp) }
    } else {
        null
    }
    val secretToggle: @Composable (() -> Unit)? = if (secret) {
        {
            IconButton(onClick = { secretVisible = !secretVisible }) {
                Icon(
                    if (secretVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = stringResource(
                        if (secretVisible) R.string.hide_password else R.string.show_password,
                    ),
                    tint = PaperDim,
                )
            }
        }
    } else {
        null
    }
    Column(modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Text(
            label,
            color = if (isError) Danger else PaperDim,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { state ->
                    if (state.isFocused) {
                        wasFocused = true
                    } else if (wasFocused) {
                        wasFocused = false
                        onBlur()
                    }
                },
            enabled = enabled,
            singleLine = true,
            isError = isError,
            placeholder = { if (placeholder.isNotEmpty()) Text(placeholder) },
            supportingText = supportingSlot,
            visualTransformation = if (secret && !secretVisible) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            keyboardOptions = keyboardOptions,
            trailingIcon = secretToggle,
            colors = settingsFieldColors(),
        )
    }
}

@Composable
private fun settingsFieldColors(): TextFieldColors = TextFieldDefaults.colors(
    focusedTextColor = Paper,
    unfocusedTextColor = Paper,
    disabledTextColor = PaperDim,
    errorTextColor = Paper,
    focusedContainerColor = SurfaceLow,
    unfocusedContainerColor = SurfaceLow,
    disabledContainerColor = SurfaceLow,
    errorContainerColor = SurfaceLow,
    cursorColor = Amber,
    errorCursorColor = Danger,
    focusedIndicatorColor = Amber,
    unfocusedIndicatorColor = PaperFaint,
    disabledIndicatorColor = PaperFaint,
    errorIndicatorColor = Danger,
    focusedLabelColor = PaperDim,
    unfocusedLabelColor = PaperDim,
    errorLabelColor = Danger,
    focusedPlaceholderColor = PaperDim,
    unfocusedPlaceholderColor = PaperDim,
    errorPlaceholderColor = PaperDim,
    focusedSupportingTextColor = PaperDim,
    unfocusedSupportingTextColor = PaperDim,
    errorSupportingTextColor = Danger,
    focusedTrailingIconColor = PaperDim,
    unfocusedTrailingIconColor = PaperDim,
    errorTrailingIconColor = PaperDim,
)

/**
 * Locale-aware timestamp for the activity log: carries the year so entries do not collide across a
 * year boundary, and follows the 12/24-hour convention of the active locale.
 */
@Composable
private fun rememberLogTimeFormatter(): DateTimeFormatter {
    val context = LocalContext.current
    return remember(context) {
        val locales = context.resources.configuration.locales
        val locale = if (locales.isEmpty) Locale.getDefault() else locales[0]
        val pattern = DateFormat.getBestDateTimePattern(locale, "yMdjmm").replace('B', 'a')
        runCatching { DateTimeFormatter.ofPattern(pattern, locale) }
            .getOrElse { DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(locale) }
    }
}

private fun formatLogTime(iso: String, formatter: DateTimeFormatter): String = try {
    formatter.format(Instant.parse(iso).atZone(ZoneId.systemDefault()))
} catch (_: Exception) {
    ""
}

@Composable
private fun TimedWaitBar(label: String, modifier: Modifier = Modifier) {
    var elapsedMs by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        val started = System.currentTimeMillis()
        while (true) {
            elapsedMs = System.currentTimeMillis() - started
            delay(200)
        }
    }
    val elapsed = DateUtils.formatElapsedTime((elapsedMs / 1000L).coerceAtLeast(0L))
    Column(modifier.fillMaxWidth()) {
        if (label.isNotBlank()) {
            Text(label, color = PaperDim, fontSize = 12.sp, lineHeight = 16.sp)
        }
        AmberTrack(
            ui = indeterminateUi(label, elapsed),
            modifier = Modifier.fillMaxWidth().padding(top = if (label.isBlank()) 0.dp else 6.dp),
        )
        Text(
            stringResource(R.string.progress_elapsed_fmt, elapsed),
            color = PaperDim,
            fontFamily = MonoFont,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun logKindLabel(kind: String): String = when (kind) {
    "ai" -> stringResource(R.string.settings_kind_ai)
    "catalog" -> stringResource(R.string.settings_kind_catalog)
    "ftp" -> stringResource(R.string.settings_kind_ftp)
    "share" -> stringResource(R.string.settings_kind_share)
    "spp" -> stringResource(R.string.settings_kind_spp)
    else -> kind
}

private fun buildLogReport(entries: List<ActivityEntry>): String = buildString {
    append("Darkroom ").append(BuildConfig.VERSION_NAME).append(" (").append(BuildConfig.VERSION_CODE).append(")\n")
    append("Android ").append(Build.VERSION.RELEASE).append(" · ").append(Build.MODEL).append("\n\n")
    entries.take(100).forEach { entry ->
        append(entry.at).append("  ").append(entry.kind).append("  ").append(entry.status)
        append("  ").append(entry.message)
        val error = entry.error
        if (!error.isNullOrBlank()) append("  | ").append(error)
        append('\n')
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
