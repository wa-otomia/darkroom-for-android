package io.github.wa_otomia.darkroom.ui.studio

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.wa_otomia.darkroom.R
import io.github.wa_otomia.darkroom.core.AppliedFraming
import io.github.wa_otomia.darkroom.core.DEFAULT_ID_PHOTO_PRESET_ID
import io.github.wa_otomia.darkroom.core.Framing
import io.github.wa_otomia.darkroom.core.GENERATED_PHOTO_TAG
import io.github.wa_otomia.darkroom.core.IdBackground
import io.github.wa_otomia.darkroom.core.IdFeather
import io.github.wa_otomia.darkroom.core.IdPhotoPose
import io.github.wa_otomia.darkroom.core.IdPhotoState
import io.github.wa_otomia.darkroom.core.MAX_PRINT_ZOOM
import io.github.wa_otomia.darkroom.core.MIN_PRINT_ZOOM
import io.github.wa_otomia.darkroom.core.PRINT_ASPECT
import io.github.wa_otomia.darkroom.core.PRINT_ASPECT_LANDSCAPE
import io.github.wa_otomia.darkroom.core.PhotoMeta
import io.github.wa_otomia.darkroom.core.SNAP_POSITION_DP
import io.github.wa_otomia.darkroom.core.defaultLandscape
import io.github.wa_otomia.darkroom.core.editsInCreationOrder
import io.github.wa_otomia.darkroom.core.framingVersionKey
import io.github.wa_otomia.darkroom.core.isConfigured
import io.github.wa_otomia.darkroom.core.exactQuarterTurns
import io.github.wa_otomia.darkroom.core.forPreset
import io.github.wa_otomia.darkroom.core.framingFor
import io.github.wa_otomia.darkroom.core.idBackground
import io.github.wa_otomia.darkroom.core.idFeather
import io.github.wa_otomia.darkroom.core.idInitialPose
import io.github.wa_otomia.darkroom.core.idMaskAiFeatherRequest
import io.github.wa_otomia.darkroom.core.idMaskRequired
import io.github.wa_otomia.darkroom.core.idPhotoFor
import io.github.wa_otomia.darkroom.core.idPhotoLayout
import io.github.wa_otomia.darkroom.core.idPhotoPreset
import io.github.wa_otomia.darkroom.core.isCancelledPrintMessage
import io.github.wa_otomia.darkroom.core.isGenerated
import io.github.wa_otomia.darkroom.core.listRelatedPhotos
import io.github.wa_otomia.darkroom.core.framingSheetSize
import io.github.wa_otomia.darkroom.core.liveFraming
import io.github.wa_otomia.darkroom.core.newestVersionSource
import io.github.wa_otomia.darkroom.core.normalizeRotationDegrees
import io.github.wa_otomia.darkroom.core.persistableFraming
import io.github.wa_otomia.darkroom.core.sheetPan
import io.github.wa_otomia.darkroom.core.studioCrop
import io.github.wa_otomia.darkroom.core.studioPlacement
import io.github.wa_otomia.darkroom.core.printImageSize
import io.github.wa_otomia.darkroom.core.resolveEditPresetName
import io.github.wa_otomia.darkroom.core.resolveRootId
import io.github.wa_otomia.darkroom.core.rotatePan
import io.github.wa_otomia.darkroom.core.rotatedQuarter
import io.github.wa_otomia.darkroom.core.rotatedSize
import io.github.wa_otomia.darkroom.core.sourceAfterVersionRemoved
import io.github.wa_otomia.darkroom.core.studioVersionIngestName
import io.github.wa_otomia.darkroom.core.versionLabel
import io.github.wa_otomia.darkroom.data.catalog.CatalogRepository
import io.github.wa_otomia.darkroom.data.catalog.IdMaskFailure
import io.github.wa_otomia.darkroom.data.imaging.ImagePipeline
import io.github.wa_otomia.darkroom.data.jobs.AiJobs
import io.github.wa_otomia.darkroom.data.jobs.AiKind
import io.github.wa_otomia.darkroom.data.printer.PrintQueue
import io.github.wa_otomia.darkroom.data.printer.StudioJobEvent
import io.github.wa_otomia.darkroom.data.progress.JobKind
import io.github.wa_otomia.darkroom.data.progress.JobProgress
import io.github.wa_otomia.darkroom.data.progress.JobProgressTracker
import io.github.wa_otomia.darkroom.data.progress.PrefsProgressStats
import io.github.wa_otomia.darkroom.data.progress.exportPhases
import io.github.wa_otomia.darkroom.data.progress.indeterminateUi
import io.github.wa_otomia.darkroom.data.progress.toUi
import io.github.wa_otomia.darkroom.data.settings.SettingsRepository
import io.github.wa_otomia.darkroom.ui.UserErrorDialog
import io.github.wa_otomia.darkroom.ui.components.AspectCropper
import io.github.wa_otomia.darkroom.ui.components.DarkroomSnackbarHost
import io.github.wa_otomia.darkroom.ui.components.DeleteUndoBar
import io.github.wa_otomia.darkroom.ui.components.GhostButton
import io.github.wa_otomia.darkroom.ui.components.JobProgressStrip
import io.github.wa_otomia.darkroom.ui.components.PaperButton
import io.github.wa_otomia.darkroom.ui.components.PresetDropdown
import io.github.wa_otomia.darkroom.ui.components.SectionLabel
import io.github.wa_otomia.darkroom.ui.components.SnapDragState
import io.github.wa_otomia.darkroom.ui.jobFieldKey
import io.github.wa_otomia.darkroom.ui.localizedByteProgress
import io.github.wa_otomia.darkroom.ui.localizedGeneratePhase
import io.github.wa_otomia.darkroom.ui.localizedPrintPhase
import io.github.wa_otomia.darkroom.ui.theme.Amber
import io.github.wa_otomia.darkroom.ui.theme.MonoFont
import io.github.wa_otomia.darkroom.ui.theme.Paper
import io.github.wa_otomia.darkroom.ui.theme.PaperDim
import io.github.wa_otomia.darkroom.ui.theme.Room
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val ACTIVE_PRINT_STATES = setOf("queued", "running")

/** The selected-tab panel may never take more than this share of the phone layout. */
private const val PANEL_MAX_FRACTION = 0.42f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioScreen(
    photoId: String,
    photos: List<PhotoMeta>,
    catalog: CatalogRepository,
    settings: SettingsRepository,
    printQueue: PrintQueue,
    aiJobs: AiJobs,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onSettings: (String) -> Unit,
    onOpenGallery: (String) -> Unit = {},
) {
    val photo = photos.find { it.id == photoId }
    val presets by settings.presets.collectAsState()
    val grokPub by settings.ai.collectAsState()
    val appSettings by settings.settings.collectAsState()
    // An app setting, not per version (EDID040-R10); read at the tap to freeze the job's mode.
    val idLocalCutout by settings.idPhotoLocalCutoutFlow.collectAsState()
    // EDID040-R12: also an app setting, read at the tap with the level to freeze the request.
    val idAiFeather by settings.idPhotoAiFeatherFlow.collectAsState()
    val scope = rememberCoroutineScope()
    val printJobs by printQueue.printJobs.collectAsState(initial = emptyList())
    val runningProgress by printQueue.runningProgress.collectAsState(initial = null)
    val aiJobList by aiJobs.jobs.collectAsState()
    var sourceOverride by rememberSaveable(photoId) { mutableStateOf<String?>(null) }
    val source = run {
        val edits = photo?.edits.orEmpty()
        val raw = sourceOverride ?: newestVersionSource(edits)
        if (raw != "original" && edits.none { it.id == raw }) newestVersionSource(edits) else raw
    }
    val haptics = LocalHapticFeedback.current
    var zoom by rememberSaveable(photoId, source) { mutableStateOf(1f) }
    // Pan is in print-sheet pixels (framingSheetSize(landscape)), never on-screen pixels: the
    // preview resizes with the drawer, the progress strips and rotation, and a pose stored in
    // screen pixels would mean a different composition at every size.
    var panX by rememberSaveable(photoId, source) { mutableStateOf(0f) }
    var panY by rememberSaveable(photoId, source) { mutableStateOf(0f) }
    var rotationDegrees by rememberSaveable(photoId, source) { mutableStateOf(0f) }
    var landscapeTouched by rememberSaveable(photoId, source) { mutableStateOf(false) }
    var landscape by rememberSaveable(photoId, source) {
        mutableStateOf(defaultLandscape(photo?.width ?: 0, photo?.height ?: 0))
    }
    var framingReady by remember(photoId, source) { mutableStateOf(false) }
    // The last composed version's pose, for saves that run outside composition (leaving the
    // screen, an edit job switching the version).
    val poseRef = remember { mutableStateOf(FramingPoseSnap("", "", false, null)) }
    val idPoseRef = remember { mutableStateOf(IdPoseSnap("", "", false, false, DEFAULT_ID_PHOTO_PRESET_ID, IdPhotoPose())) }
    // The loaded row, written back unchanged until the image size is known (an edit without
    // stored dimensions waits for a probe), so leaving early cannot rewrite it half-resolved.
    var restoreFraming by remember(photoId, source) { mutableStateOf<Framing?>(null) }
    // ID photo mode (EDID010-R03). Studio owns only the toggle, the size and the shared pose,
    // as primitives for the saved-instance bundle. Background, feather and the mask revision
    // are read live from the row (photo.idPhotoFor(source)) and written straight to the
    // catalog, so the mask job's commit is never overwritten by a stale copy held here.
    // idKnown: this version has ID state (loaded or entered this session); until then nothing
    // is saved and the first entry starts from the initial pose (EDID010-R04).
    var idActive by rememberSaveable(photoId, source) { mutableStateOf(false) }
    var idKnown by rememberSaveable(photoId, source) { mutableStateOf(false) }
    var idPresetId by rememberSaveable(photoId, source) { mutableStateOf(DEFAULT_ID_PHOTO_PRESET_ID) }
    var idZoom by rememberSaveable(photoId, source) { mutableStateOf(1f) }
    var idPanX by rememberSaveable(photoId, source) { mutableStateOf(0f) }
    var idPanY by rememberSaveable(photoId, source) { mutableStateOf(0f) }
    var idRotation by rememberSaveable(photoId, source) { mutableStateOf(0f) }
    // A tapped background / feather until the row catches up: the catalog write re-renders the
    // gallery thumb before the row changes, and the preview must follow the tap at once. Kept
    // until every write started by a tap has completed (PendingIdChoice), never cleared because
    // an older row value happens to equal the latest tap.
    var idBgPending by remember(photoId, source) { mutableStateOf(PendingIdChoice()) }
    var idFeatherPending by remember(photoId, source) { mutableStateOf(PendingIdChoice()) }
    var idInwardPending by remember(photoId, source) { mutableStateOf(PendingIdChoice()) }
    // EDID040-R08: the preview is building a new composite; the ID controls wait for it.
    // Reported by the preview while it is composed, false for every new version.
    var idCompositing by remember(photoId, source) { mutableStateOf(false) }
    // EDID040-R09: this version's last failed cut-out, read from the cache record; re-read on
    // screen open, version switch, every record write/clear and every mask Ready/Failed event.
    var idMaskFailure by remember(photoId, source) { mutableStateOf<IdMaskFailure?>(null) }
    var idMaskFailureCheck by remember(photoId) { mutableStateOf(0) }
    var idCutoutResultOpen by remember(photoId, source) { mutableStateOf(false) }
    // Mask job failure for the ID section; already sanitized by AiJobs. Recorded with the version
    // (framing version key) it belongs to and shown only on that version.
    var maskError by rememberSaveable(photoId) { mutableStateOf<String?>(null) }
    var maskErrorVersion by rememberSaveable(photoId) { mutableStateOf<String?>(null) }
    // Version the last cut-out was started on, for a failure whose job row is already gone.
    var maskCutVersion by rememberSaveable(photoId) { mutableStateOf<String?>(null) }
    var useCurrentFraming by rememberSaveable { mutableStateOf(true) }
    var prompt by rememberSaveable(photoId) { mutableStateOf("") }
    var extraPromptOpen by rememberSaveable(photoId) { mutableStateOf(false) }
    var presetId by rememberSaveable(photoId) { mutableStateOf(presets.lastSelectedId) }
    var tabKey by rememberSaveable { mutableStateOf(StudioTab.Compose.key) }
    val tab = StudioTab.fromKey(tabKey)
    var drawerExpanded by rememberSaveable { mutableStateOf(true) }
    var copies by rememberSaveable(photoId) { mutableStateOf(appSettings.defaultCopies) }
    var printError by rememberSaveable(photoId) { mutableStateOf<String?>(null) }
    var genError by rememberSaveable(photoId) { mutableStateOf<String?>(null) }
    var pendingDelete by rememberSaveable(photoId) { mutableStateOf(false) }
    var versionMenuId by rememberSaveable(photoId) { mutableStateOf<String?>(null) }
    var versionDeleteId by rememberSaveable(photoId) { mutableStateOf<String?>(null) }
    var sourceWidth by rememberSaveable(photoId, source) { mutableStateOf(0) }
    var sourceHeight by rememberSaveable(photoId, source) { mutableStateOf(0) }
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val density = LocalDensity.current
    val pendingDeletion by catalog.pendingDeletion.collectAsState()
    val watermarkSettings by settings.watermark.collectAsState()
    var watermarkOn by rememberSaveable(photoId) { mutableStateOf(watermarkSettings.isConfigured()) }
    val canWatermark = watermarkSettings.isConfigured()
    val watermarkArmed = watermarkOn && canWatermark
    val snapThresholdPx = with(density) { SNAP_POSITION_DP.dp.toPx() }
    val snapState = remember(snapThresholdPx) { SnapDragState(snapThresholdPx) }
    val progressStats = remember(context) { PrefsProgressStats(context.applicationContext) }
    var exporting by rememberSaveable(photoId) { mutableStateOf(false) }
    var exportProgress by remember { mutableStateOf<JobProgress?>(null) }
    val exportedText = stringResource(R.string.studio_exported)
    val shareText = stringResource(R.string.studio_share)
    val exportFailedText = stringResource(R.string.studio_export_failed)

    val myPrint = printJobs.find {
        it.photoId == photoId && jobFieldKey(it.state) in ACTIVE_PRINT_STATES
    }
    val generateJob = aiJobList.find { it.sourcePhotoId == photoId && it.kind == AiKind.Generate }
    val editJob = aiJobList.find { it.sourcePhotoId == photoId && it.kind == AiKind.Edit }
    // The mask job never locks framing (EDID040-R06) but counts as busy for every other gate.
    val maskJob = aiJobList.find { it.sourcePhotoId == photoId && it.kind == AiKind.Mask }
    val activeAi = editJob ?: generateJob
    val alreadyQueued = myPrint != null
    val generating = generateJob != null
    val editing = editJob != null
    val aiHidden = activeAi?.hidden == true
    val aiActive = generating || editing
    val aiBlocking = aiActive && !aiHidden
    val aiBusy = aiActive || maskJob != null
    val aiCancelledText = stringResource(R.string.ai_cancelled)
    val maskReadyText = stringResource(R.string.studio_id_mask_ready)
    val generatedText = stringResource(R.string.ai_generated)
    val viewGenerated = stringResource(R.string.ai_generated_view)
    val editDoneText = stringResource(R.string.edit_done)
    val versionFallback = stringResource(R.string.studio_version_fallback)
    val printDoneText = stringResource(R.string.print_done)
    val printQueuedText = stringResource(R.string.print_queued)
    var printStripCollapsed by rememberSaveable(photoId) { mutableStateOf(false) }
    var aiStripCollapsed by rememberSaveable(photoId) { mutableStateOf(false) }
    var exportStripCollapsed by rememberSaveable(photoId) { mutableStateOf(false) }
    val myProgress = runningProgress?.takeIf { it.photoId == photoId }
    val printPhase = myProgress?.phase ?: myPrint?.phase
    val printState = myProgress?.jobState ?: myPrint?.jobState
    val printerJobId = myProgress?.jobId ?: myPrint?.printerJobId
    val genPhase = activeAi?.phase?.let { jobFieldKey(it).ifBlank { it.toString() } }
    val genLoaded = activeAi?.loaded
    val genTotal = activeAi?.total

    LaunchedEffect(photoId) {
        printQueue.jobs.collect { e ->
            if (e.photoId != photoId) return@collect
            when (e) {
                is StudioJobEvent.PrintFinished -> {
                    printError = null
                    launch { snackbar.showSnackbar(printDoneText) }
                }
                is StudioJobEvent.Failed -> {
                    if (e.action == "print") {
                        val rowCancelled = printJobs.any {
                            it.photoId == photoId && jobFieldKey(it.state) == "cancelled"
                        }
                        if (!rowCancelled && !isCancelledPrintMessage(e.message)) {
                            printError = e.message
                        }
                    }
                }
                else -> Unit
            }
        }
    }
    LaunchedEffect(photoId) {
        aiJobs.events.collect { e ->
            if (e.photoId != photoId) return@collect
            when (e) {
                is StudioJobEvent.Generated -> {
                    genError = null
                    val hidden = aiJobs.jobs.value.find {
                        it.targetPhotoId == e.created.id ||
                            (it.sourcePhotoId == e.photoId && it.kind == AiKind.Generate)
                    }?.hidden == true
                    if (!hidden && photoId == e.photoId) {
                        onOpenGallery(e.created.id)
                    } else {
                        launch {
                            val result = snackbar.showSnackbar(
                                message = generatedText,
                                actionLabel = viewGenerated,
                                duration = SnackbarDuration.Long,
                            )
                            if (result == SnackbarResult.ActionPerformed) onOpenGallery(e.created.id)
                        }
                    }
                }
                is StudioJobEvent.Edited -> {
                    // A version switch like selectSource: the version being left is saved now,
                    // since its debounced framing / ID saves are cancelled by the switch.
                    val snap = poseRef.value
                    if (snap.ready && snap.photoId == photoId && snap.source != e.editId) {
                        catalog.persistFraming(snap.photoId, snap.source, snap.framing)
                    }
                    val id = idPoseRef.value
                    if (id.due && id.photoId == photoId && id.source != e.editId) {
                        catalog.persistIdPhotoPose(id.photoId, id.source, id.active, id.presetId, id.pose, createIfMissing = true)
                    }
                    sourceOverride = e.editId
                    genError = null
                    launch { snackbar.showSnackbar(editDoneText) }
                }
                is StudioJobEvent.MaskReady -> {
                    idMaskFailureCheck++
                    if (maskErrorVersion == framingVersionKey(e.source)) {
                        maskError = null
                        maskErrorVersion = null
                    }
                    launch { snackbar.showSnackbar(maskReadyText) }
                }
                is StudioJobEvent.Failed -> {
                    if (e.action == "mask") {
                        idMaskFailureCheck++
                        // Shown in the ID section, not the generic dialog; a cancel is blank.
                        if (e.message.isNullOrBlank()) {
                            launch { snackbar.showSnackbar(aiCancelledText) }
                        } else {
                            // The failed row stays listed for a moment and names its version.
                            val failedVersion = aiJobs.jobs.value.find {
                                it.sourcePhotoId == photoId && it.kind == AiKind.Mask
                            }?.source ?: maskCutVersion
                            maskError = e.message
                            maskErrorVersion = failedVersion?.let { framingVersionKey(it) }
                        }
                    } else if (e.action != "print") {
                        if (e.message.isNullOrBlank()) {
                            launch { snackbar.showSnackbar(aiCancelledText) }
                        } else {
                            genError = e.message
                        }
                    }
                }
                else -> Unit
            }
        }
    }

    if (photo == null) {
        Column(Modifier.padding(24.dp)) {
            TextButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = null,
                    tint = Amber,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.back_gallery), color = Amber)
            }
            Text(stringResource(R.string.not_found), style = MaterialTheme.typography.headlineMedium, color = Paper)
        }
        return
    }

    val family = listRelatedPhotos(photos, photo.id)
    val rootId = resolveRootId(photos, photo.id)
    val imageFile = catalog.sourceFile(photo.id, source)
    val cropLocked = aiBlocking
    val printerReady = appSettings.printerMac.isNotBlank()
    val hasInstructions = presetId.isNotEmpty() || prompt.isNotBlank()
    val canGenerate = !aiBusy && grokPub.keySet && hasInstructions
    val canPrintDirect = printerReady && !alreadyQueued
    val canPrintWithPreset = printerReady && grokPub.keySet && hasInstructions && !alreadyQueued
    val queuedIds = printJobs.filter { jobFieldKey(it.state) == "queued" }.map { it.id }
    val queueIndex = myPrint?.let { queuedIds.indexOf(it.id) }?.let { if (it >= 0) it + 1 else 0 } ?: 0
    LaunchedEffect(myPrint?.id) { printStripCollapsed = false }
    // While an edit runs, the shown version is pinned: otherwise the new edit row would switch
    // "newest" by itself and drop the pending saves of the version being left. The Edited event
    // then switches through the save above.
    LaunchedEffect(editJob?.id) {
        if (editJob != null && sourceOverride == null) sourceOverride = source
    }
    LaunchedEffect(activeAi?.id) { aiStripCollapsed = false }
    LaunchedEffect(exporting) { if (exporting) exportStripCollapsed = false }
    val portraitOverlay = stringResource(R.string.crop_overlay)
    val landscapeOverlay = stringResource(R.string.studio_crop_overlay_landscape)

    LaunchedEffect(photoId, source, photo, imageFile) {
        if (source == "original" || source.isBlank()) {
            sourceWidth = photo.width
            sourceHeight = photo.height
            return@LaunchedEffect
        }
        val edit = photo.edits.find { it.id == source }
        if (edit != null && edit.width > 0 && edit.height > 0) {
            sourceWidth = edit.width
            sourceHeight = edit.height
            return@LaunchedEffect
        }
        if (imageFile.exists()) {
            val info = withContext(Dispatchers.IO) { ImagePipeline.probe(imageFile.readBytes()) }
            sourceWidth = info.width
            sourceHeight = info.height
        } else {
            sourceWidth = 0
            sourceHeight = 0
        }
    }

    val orientedW = if (sourceWidth > 0) sourceWidth else if (source == "original") photo.width else 0
    val orientedH = if (sourceHeight > 0) sourceHeight else if (source == "original") photo.height else 0
    val quarterTurns = exactQuarterTurns(rotationDegrees) ?: 0
    // Size the crop rect is expressed in: the source turned by whole quarters. At a
    // free angle the crop is not used (placement is), so the unturned size stands in.
    val working = remember(orientedW, orientedH, quarterTurns) {
        rotatedSize(orientedW, orientedH, quarterTurns)
    }

    LaunchedEffect(photoId, source) {
        framingReady = false
        // Waits for this screen's own fire-and-forget saves (leave, rotate, version switch) and
        // reads under the row lock, so a save still on its way is never loaded as older state and
        // then written back over itself by the debounced save.
        val stored = catalog.getSettled(photoId)
        val saved = stored?.framingFor(source) ?: photo.framingFor(source)
        val savedId = stored?.idPhotoFor(source) ?: photo.idPhotoFor(source)
        restoreFraming = saved
        if (saved != null) {
            // One snapshot, no suspension: the cropper must never clamp the new pan against
            // the old zoom or orientation. Older rows were measured on the on-screen frame of
            // the time and convert to sheet pixels here; no layout is needed.
            val (sheetPanX, sheetPanY) = saved.sheetPan()
            zoom = saved.zoom
            panX = sheetPanX
            panY = sheetPanY
            rotationDegrees = saved.rotationDegrees
            landscape = saved.landscape
            landscapeTouched = true
        } else {
            zoom = 1f
            panX = 0f
            panY = 0f
            rotationDegrees = 0f
            landscapeTouched = false
            val size = printImageSize(photo, source)
            landscape = defaultLandscape(size?.width ?: 0, size?.height ?: 0)
        }
        // Same snapshot: a version whose ID state is on opens straight into ID mode.
        if (savedId != null) {
            idKnown = true
            idActive = savedId.active
            idPresetId = savedId.presetId
            idZoom = savedId.zoom
            idPanX = savedId.panX
            idPanY = savedId.panY
            idRotation = savedId.rotation
        } else if (!idKnown) {
            // Skipped when idKnown came back from the saved-instance bundle (the values are keyed
            // on this version): a first entry whose write from the previous composition has not
            // landed yet. Keeping it lets the next save converge with that write, instead of the
            // screen dropping ID mode while the row turns it on.
            idActive = false
            idPresetId = DEFAULT_ID_PHOTO_PRESET_ID
            idZoom = 1f
            idPanX = 0f
            idPanY = 0f
            idRotation = 0f
        }
        framingReady = true
    }
    val sizeKnown = orientedW > 0 && orientedH > 0
    LaunchedEffect(photoId, source, restoreFraming, sizeKnown) {
        if (restoreFraming != null && sizeKnown) restoreFraming = null
    }
    val (sheetW, sheetH) = framingSheetSize(landscape)
    // Derived from state alone, so it does not change when only the preview size does: the
    // drawer opening, a strip appearing or a rotation re-saves nothing and re-renders nothing.
    val placement = remember(landscape, orientedW, orientedH, zoom, panX, panY, rotationDegrees) {
        studioPlacement(landscape, orientedW, orientedH, zoom, panX, panY, rotationDegrees)
    }
    val crop = remember(placement) { studioCrop(placement) }

    val currentFraming = liveFraming(
        landscape = landscape,
        zoom = zoom,
        offsetX = placement?.offsetX ?: panX,
        offsetY = placement?.offsetY ?: panY,
        rotationDegrees = rotationDegrees,
        placement = placement,
        imageWidth = orientedW,
        imageHeight = orientedH,
    )
    val framingToPersist: Framing? =
        restoreFraming ?: persistableFraming(currentFraming, orientedW, orientedH)
    val idPose = IdPhotoPose(idZoom, idPanX, idPanY, idRotation)
    // Saved with the ordinary framing's cadence: only for a version that has ID state.
    val idSaveDue = framingReady && idKnown
    SideEffect {
        poseRef.value = FramingPoseSnap(
            photoId = photo.id,
            source = source,
            ready = framingReady,
            framing = framingToPersist,
        )
        idPoseRef.value = IdPoseSnap(
            photoId = photo.id,
            source = source,
            due = idSaveDue,
            active = idActive,
            presetId = idPresetId,
            pose = idPose,
        )
    }
    DisposableEffect(Unit) {
        onDispose {
            val snap = poseRef.value
            if (snap.ready) catalog.persistFraming(snap.photoId, snap.source, snap.framing)
            val id = idPoseRef.value
            if (id.due) catalog.persistIdPhotoPose(id.photoId, id.source, id.active, id.presetId, id.pose, createIfMissing = true)
        }
    }
    LaunchedEffect(photoId, source, zoom, panX, panY, rotationDegrees, landscape, placement, framingReady, restoreFraming) {
        if (!framingReady || restoreFraming != null) return@LaunchedEffect
        delay(300)
        catalog.saveFraming(
            photo.id,
            source,
            persistableFraming(currentFraming, orientedW, orientedH),
        )
    }
    // Every ID save below runs only once idKnown is set (the version has ID state or entered ID
    // mode here), so an inactive save may create the entry: leaving ID mode before the first
    // debounced save landed must still keep the chosen size and pose (EDID010-R03).
    LaunchedEffect(photoId, source, idSaveDue, idActive, idPresetId, idZoom, idPanX, idPanY, idRotation) {
        if (!idSaveDue) return@LaunchedEffect
        delay(300)
        catalog.saveIdPhotoPose(photo.id, source, idActive, idPresetId, idPose, createIfMissing = true)
    }

    val selectSource: (String) -> Unit = { next ->
        if (next != source) {
            if (framingReady) {
                catalog.persistFraming(photo.id, source, framingToPersist)
            }
            if (idSaveDue) {
                catalog.persistIdPhotoPose(photo.id, source, idActive, idPresetId, idPose, createIfMissing = true)
            }
            sourceOverride = next
        }
    }

    val idPreset = idPhotoPreset(idPresetId)
    val idLayout = remember(idPreset) { idPhotoLayout(idPreset) }
    val idStored = photo.idPhotoFor(source)
    val idHasMask = idStored?.hasMask == true
    val idBg = idBackground(idBgPending.value ?: idStored?.background)
    val idFeatherChoice = idFeather(idFeatherPending.value ?: idStored?.feather)
    val idFeatherInward = idInwardPending.value?.toBooleanStrictOrNull() ?: idStored?.featherInward ?: true
    // Backstop for the load above: a first entry written by a composition that is already gone
    // (left and reopened at once) can land after the load found nothing. Adopt it when it shows,
    // so the screen never shows ordinary framing while the row (and so a print) has ID mode on.
    LaunchedEffect(photoId, source, framingReady, idStored != null) {
        val late = idStored ?: return@LaunchedEffect
        if (!framingReady || idKnown) return@LaunchedEffect
        idKnown = true
        idActive = late.active
        idPresetId = late.presetId
        idZoom = late.zoom
        idPanX = late.panX
        idPanY = late.panY
        idRotation = late.rotation
    }
    LaunchedEffect(idStored?.background, idBgPending) {
        idBgPending = idBgPending.settle(idStored?.background)
    }
    LaunchedEffect(idStored?.feather, idFeatherPending) {
        idFeatherPending = idFeatherPending.settle(idStored?.feather)
    }
    LaunchedEffect(idStored?.featherInward, idInwardPending) {
        idInwardPending = idInwardPending.settle(idStored?.featherInward?.toString())
    }
    val setIdPose: (IdPhotoPose) -> Unit = { p ->
        idZoom = p.zoom
        idPanX = p.panX
        idPanY = p.panY
        idRotation = p.rotation
    }
    // EDID010-R01/R04: the first entry of a version starts at zoom 1, pan 0 and the right angle
    // nearest the ordinary rotation, with the size last chosen anywhere. Nothing here touches the
    // ordinary pose, so leaving restores it exactly.
    // Ignored until the version's load has applied its framing and ID state: a first entry
    // before then would take the default rotation, and the load would overwrite the toggle.
    val onIdToggle: (Boolean) -> Unit = { on ->
        if (framingReady) {
            if (on && !idKnown) {
                setIdPose(idInitialPose(rotationDegrees))
                idPresetId = settings.lastIdPhotoPreset()
                idKnown = true
            }
            idActive = on
        }
    }
    // EDID030-R01: a size change keeps zoom and rotation and re-clamps the pan in the new cell.
    val onIdPreset: (String) -> Unit = { id ->
        val next = idPhotoPreset(id)
        if (next.id != idPreset.id) {
            setIdPose(idPose.forPreset(idLayout, idPhotoLayout(next), orientedW, orientedH))
            idPresetId = next.id
        }
        settings.setLastIdPhotoPreset(next.id)
    }
    val onIdRotate: (Int) -> Unit = { delta ->
        setIdPose(idPose.rotatedQuarter(delta, idLayout.cellWidth, idLayout.cellHeight, orientedW, orientedH))
    }
    // EDID010-R04: only the ID pose; size, mask, background and the ordinary framing stay.
    val onIdReset: () -> Unit = {
        val reset = IdPhotoPose()
        setIdPose(reset)
        snapState.begin(0f, 0f, 0f)
        catalog.persistIdPhotoPose(photo.id, source, idActive, idPresetId, reset, createIfMissing = true)
    }
    val onCutOut: () -> Unit = {
        val version = framingVersionKey(source)
        maskError = null
        maskErrorVersion = null
        maskCutVersion = version
        try {
            // The mode is frozen into the job here (EDID040-R10): what the switch shows now.
            // EDID040-R13: so is the AI feather, with the level and size on screen now.
            val aiFeather = idMaskAiFeatherRequest(
                enabled = idAiFeather,
                local = idLocalCutout,
                feather = idFeatherChoice,
                inward = idFeatherInward,
                cellWidth = idLayout.cellWidth,
                cellHeight = idLayout.cellHeight,
                imageWidth = orientedW,
                imageHeight = orientedH,
                zoom = idZoom,
            )
            aiJobs.enqueueMask(photo.id, source, local = idLocalCutout, aiFeather = aiFeather)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            maskError = e.message ?: e.toString()
            maskErrorVersion = version
        }
    }
    // Written at once, not with the pose debounce (D2). A tap must land even if Studio closes
    // right after it; a failure of the latest tap drops the optimistic choice and reports.
    val writeIdChoice: (suspend () -> Unit, (Boolean) -> Unit) -> Unit = { write, onDone ->
        scope.launch {
            var ok = false
            try {
                withContext(NonCancellable) { write() }
                ok = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                genError = e.message ?: e.toString()
            } finally {
                onDone(ok)
            }
        }
    }
    val onIdBackground: (IdBackground) -> Unit = { bg ->
        val id = photo.id
        val src = source
        idBgPending = idBgPending.begin(bg.id)
        val token = idBgPending.seq
        writeIdChoice({ catalog.setIdPhotoBackground(id, src, bg.id) }) { ok ->
            idBgPending = idBgPending.finish(token, ok)
        }
    }
    val onIdFeather: (IdFeather) -> Unit = { f ->
        val id = photo.id
        val src = source
        idFeatherPending = idFeatherPending.begin(f.id)
        val token = idFeatherPending.seq
        writeIdChoice({ catalog.setIdPhotoFeather(id, src, f.id) }) { ok ->
            idFeatherPending = idFeatherPending.finish(token, ok)
        }
    }
    val onIdFeatherInward: (Boolean) -> Unit = { inward ->
        val id = photo.id
        val src = source
        idInwardPending = idInwardPending.begin(inward.toString())
        val token = idInwardPending.seq
        writeIdChoice({ catalog.setIdPhotoFeatherInward(id, src, inward) }) { ok ->
            idInwardPending = idInwardPending.finish(token, ok)
        }
    }

    val appliedAiFraming: AppliedFraming? =
        if (useCurrentFraming) {
            AppliedFraming(
                crop = crop,
                cropImageWidth = working.width.takeIf { it > 0 },
                cropImageHeight = working.height.takeIf { it > 0 },
                rotateQuarters = quarterTurns,
                landscape = landscape,
                rotationDegrees = rotationDegrees,
                placement = placement,
            )
        } else {
            null
        }

    // The buttons land on a right angle even after a two-finger twist, so one tap
    // straightens a tilted frame instead of carrying the tilt round.
    val onRotate: (Int) -> Unit = { delta ->
        val target = normalizeRotationDegrees(Math.round((rotationDegrees + delta * 90f) / 90f) * 90f)
        val (nx, ny) = rotatePan(panX, panY, target - rotationDegrees)
        panX = nx
        panY = ny
        rotationDegrees = target
    }

    // Direct print drops the preset and the typed prompt so it never edits.
    val startPrint: (Boolean) -> Unit = { edit ->
        printError = null
        printStripCollapsed = false
        // The ID toggle, size and pose reach the row before the job does, so execution reads
        // what is on screen, including a just-left ID mode (PRRD020-R04).
        val idSave: (suspend () -> Unit)? = if (idSaveDue) {
            val id = photo.id
            val src = source
            val active = idActive
            val preset = idPresetId
            val pose = idPose
            ({ catalog.saveIdPhotoPose(id, src, active, preset, pose, createIfMissing = true) })
        } else {
            null
        }
        printQueue.enqueuePrint(
            photo.id,
            source,
            crop,
            copies,
            if (edit) presetId.ifEmpty { null } else null,
            if (edit) prompt else "",
            working.width.takeIf { it > 0 },
            working.height.takeIf { it > 0 },
            rotateQuarters = quarterTurns,
            landscape = landscape,
            rotationDegrees = rotationDegrees,
            placement = placement,
            watermark = watermarkArmed && !idActive,
            framing = framingToPersist,
            beforeEnqueue = idSave,
        )
        scope.launch { snackbar.showSnackbar(printQueuedText) }
    }

    val startExport: () -> Unit = {
        if (!exporting) {
            // EDEX010-R04: the live ID state with the on-screen background, feather and inward
            // choice (pending taps included) and the row's mask.
            val idExport: IdPhotoState? = if (idActive) {
                (idStored ?: IdPhotoState())
                    .copy(
                        active = true,
                        presetId = idPresetId,
                        background = idBg.id,
                        feather = idFeatherChoice.id,
                        featherInward = idFeatherInward,
                    )
                    .withPose(idPose)
            } else {
                null
            }
            exporting = true
            exportStripCollapsed = false
            exportProgress = null
            val tracker = JobProgressTracker(
                jobId = "export-$photoId",
                kind = JobKind.Export,
                phases = exportPhases(progressStats),
                stats = progressStats,
            )
            val ticks = tracker.launchTicks(scope) { exportProgress = it }
            scope.launch {
                try {
                    tracker.startPhase("rendering")
                    val uri = withContext(Dispatchers.IO) {
                        val bytes = imageFile.readBytes()
                        val jpeg = if (idExport != null) {
                            // A missing mask throws before decoding, so no MediaStore row is made.
                            // The row's current revision and its bytes are read in one step, so a
                            // re-cut that committed after the tap cannot delete them in between.
                            val mask = if (idMaskRequired(idExport)) {
                                catalog.getWithIdMask(photo.id, source).second
                            } else {
                                null
                            }
                            ImagePipeline.renderIdPhotoSheetJpeg(
                                bytes,
                                idExport,
                                mask,
                                outputScale = 2,
                                sheetForPrinter = false,
                            )
                        } else {
                            ImagePipeline.renderPrintJpeg(
                                bytes,
                                crop,
                                appSettings.printFit,
                                cropImageWidth = working.width.takeIf { it > 0 },
                                cropImageHeight = working.height.takeIf { it > 0 },
                                rotateQuarters = quarterTurns,
                                landscape = landscape,
                                rotationDegrees = rotationDegrees,
                                placement = placement,
                                watermark = if (watermarkArmed) settings.readWatermark() else null,
                                photo = photo,
                                outputScale = 2,
                                context = context.applicationContext,
                                sheetForPrinter = false,
                            )
                        }
                        tracker.startPhase("saving")
                        writeExportJpeg(context.applicationContext, jpeg, exportDisplayName(photo))
                    }
                    tracker.succeed()
                    exportProgress = tracker.tick()
                    val result = snackbar.showSnackbar(
                        message = exportedText,
                        actionLabel = shareText,
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        shareExported(context, uri)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    tracker.fail(e.message ?: e.toString())
                    snackbar.showSnackbar(exportFailedText)
                } finally {
                    ticks.cancel()
                    exporting = false
                    exportProgress = null
                }
            }
        }
    }

    // Mask job progress for the ID section (the top AI strip shows only generate/edit).
    val maskPhaseLabel = maskJob?.phase
        ?.let { jobFieldKey(it).ifBlank { it.toString() } }
        ?.let { localizedGeneratePhase(it) }
        .orEmpty()
    val maskBytes = localizedByteProgress(maskJob?.loaded, maskJob?.total).ifEmpty { null }
    val maskUi = maskJob?.let { it.progress?.toUi(maskPhaseLabel, maskBytes) ?: indeterminateUi(maskPhaseLabel, maskBytes) }
    // Progress and Cancel show on every version and outside ID mode too, since the job gates
    // Generate and deletes everywhere (EDID040-R06); another version's job is named by label.
    val currentVersionKey = framingVersionKey(source)
    val maskJobUi = maskJob?.let { job ->
        IdMaskJobUi(maskUi ?: indeterminateUi(maskPhaseLabel)) { aiJobs.cancel(job.id) }
    }
    val maskJobElsewhereLabel = maskJob?.takeIf { it.source != currentVersionKey }?.let { job ->
        if (job.source == framingVersionKey("original")) {
            stringResource(R.string.original)
        } else {
            val ordered = editsInCreationOrder(photo.edits)
            val index = ordered.indexOfFirst { it.id == job.source }
            val edit = ordered.getOrNull(index)
            if (edit == null) {
                versionFallback
            } else {
                versionLabel(
                    index + 1,
                    resolveEditPresetName(edit.presetTitle, edit.presetId, versionFallback) { id ->
                        presets.presets.find { it.id == id }?.title
                    },
                    versionFallback,
                )
            }
        }
    }
    val maskErrorHere = maskErrorFor(maskError, maskErrorVersion, currentVersionKey)
    // The mask job writes the record before it reports the failure and clears it when a new
    // attempt starts or succeeds; each write or clear signals, so the entry follows the record.
    LaunchedEffect(photo.id, source, idMaskFailureCheck) {
        catalog.idMaskFailureChanges().collectLatest {
            val read = withContext(Dispatchers.IO) {
                runCatching { catalog.readIdMaskFailure(photo.id, source) }.getOrNull()
            }
            idMaskFailure = read
            if (read == null) idCutoutResultOpen = false
        }
    }

    val composePanel: @Composable () -> Unit = {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column {
                SectionLabel(stringResource(R.string.studio_framing))
                FramingControls(
                    landscape = landscape,
                    rotationDegrees = if (idActive) idRotation else rotationDegrees,
                    enabled = !cropLocked,
                    // EDID010-R01: in ID mode neither chip shows selected; either one leaves ID
                    // mode and then follows the ordinary orientation rule.
                    orientationSelected = !idActive,
                    onOrientation = {
                        if (idActive) idActive = false
                        landscapeTouched = true
                        landscape = it
                    },
                    onRotate = if (idActive) onIdRotate else onRotate,
                    onReset = if (idActive) {
                        onIdReset
                    } else {
                        {
                            restoreFraming = null
                            zoom = 1f
                            panX = 0f
                            panY = 0f
                            rotationDegrees = 0f
                            landscapeTouched = false
                            val autoW = orientedW.takeIf { it > 0 } ?: photo.width
                            val autoH = orientedH.takeIf { it > 0 } ?: photo.height
                            landscape = defaultLandscape(autoW, autoH)
                            snapState.begin(0f, 0f, 0f)
                            catalog.persistFraming(photo.id, source, null)
                        }
                    },
                )
                StudioToggleChip(
                    label = stringResource(R.string.studio_id_photo),
                    checked = idActive,
                    enabled = !cropLocked && framingReady,
                    modifier = Modifier.padding(top = 6.dp),
                    onCheckedChange = onIdToggle,
                )
                if (idActive) {
                    IdPhotoSizeSection(
                        presetId = idPreset.id,
                        enabled = !cropLocked && framingReady,
                        onPresetChange = onIdPreset,
                    )
                }
                // The ID sheet is always landscape, turned onto the portrait paper.
                if (landscape || idActive) {
                    StudioNote(stringResource(R.string.studio_landscape_note))
                }
            }
            Column {
                Text(stringResource(R.string.zoom), color = PaperDim)
                Slider(
                    value = if (idActive) idZoom else zoom,
                    onValueChange = { if (idActive) idZoom = it else zoom = it },
                    valueRange = MIN_PRINT_ZOOM..MAX_PRINT_ZOOM,
                    enabled = !cropLocked,
                    colors = SliderDefaults.colors(thumbColor = Amber, activeTrackColor = Amber),
                )
                if (cropLocked) {
                    StudioNote(stringResource(R.string.studio_crop_locked), tone = Amber)
                }
            }
            // EDID010-R05: off in ID mode without touching the session switch, which returns
            // as it was when ID mode ends.
            StudioWatermarkRow(
                checked = watermarkArmed && !idActive,
                enabled = canWatermark && !cropLocked && !idActive,
                hint = when {
                    idActive -> stringResource(R.string.studio_id_no_watermark)
                    !canWatermark -> stringResource(R.string.studio_watermark_unconfigured)
                    else -> null
                },
            ) { watermarkOn = it }
        }
    }

    val processPanel: @Composable () -> Unit = {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column {
                SectionLabel(stringResource(R.string.version))
                val siblings = family.filter { it.id != photo.id }
                if (siblings.isNotEmpty()) {
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        siblings.forEach { p ->
                            StudioNavChip(
                                label = if (p.id == rootId) stringResource(R.string.original) else p.filename,
                                onClick = { onOpen(p.id) },
                            )
                        }
                    }
                }
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StudioVersionTile(
                        selected = source == "original",
                        label = stringResource(R.string.original),
                        model = catalog.versionThumbFile(photo.id, "original"),
                        onClick = { selectSource("original") },
                    )
                    editsInCreationOrder(photo.edits).forEachIndexed { i, edit ->
                        val presetName = resolveEditPresetName(
                            edit.presetTitle,
                            edit.presetId,
                            versionFallback,
                        ) { id -> presets.presets.find { it.id == id }?.title }
                        StudioVersionTile(
                            selected = source == edit.id,
                            label = versionLabel(i + 1, presetName, versionFallback),
                            model = catalog.versionThumbFile(photo.id, edit.id),
                            onClick = { selectSource(edit.id) },
                            onLongClick = if (aiBusy) {
                                null
                            } else {
                                {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    versionMenuId = edit.id
                                }
                            },
                        )
                    }
                }
            }

            if (idActive) {
                IdPhotoProcessSection(
                    keySet = grokPub.keySet,
                    local = idLocalCutout,
                    localSwitchEnabled = idLocalSwitchEnabled(busy = aiBusy, compositing = idCompositing),
                    aiFeather = idAiFeather,
                    aiFeatherSwitchEnabled = idAiFeatherSwitchEnabled(
                        local = idLocalCutout,
                        busy = aiBusy,
                        compositing = idCompositing,
                    ),
                    bakedFeather = idStored?.aiFeatherLevel,
                    hasMask = idHasMask,
                    background = idBg,
                    feather = idFeatherChoice,
                    featherInward = idFeatherInward,
                    compositing = idCompositing,
                    canCutOut = idCanCutOut(local = idLocalCutout, keySet = grokPub.keySet, busy = aiBusy),
                    maskJob = maskJobUi,
                    maskJobElsewhere = maskJobElsewhereLabel,
                    maskError = maskErrorHere,
                    onCutOut = onCutOut,
                    onLocalChange = { settings.setIdPhotoLocalCutout(it) },
                    onAiFeatherChange = { settings.setIdPhotoAiFeather(it) },
                    onOpenSettings = { onSettings("grok") },
                    onBackground = onIdBackground,
                    onFeather = onIdFeather,
                    onFeatherInward = onIdFeatherInward,
                    onViewCutoutResult = if (idMaskFailure != null) {
                        { idCutoutResultOpen = true }
                    } else {
                        null
                    },
                )
            } else {
                if (maskJobUi != null) {
                    IdMaskJobRow(maskJobUi, maskJobElsewhereLabel)
                }
                Column {
                    SectionLabel(stringResource(R.string.grok_title))
                    if (!grokPub.keySet) {
                        Text(stringResource(R.string.grok_need_key), color = Amber, modifier = Modifier.padding(bottom = 8.dp))
                        TextButton(onClick = { onSettings("grok") }) { Text(stringResource(R.string.nav_settings), color = Amber) }
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Box(Modifier.weight(1f)) {
                            PresetDropdown(presetId, presets.presets, enabled = !cropLocked) {
                                presetId = it
                                settings.setLastPreset(it)
                            }
                        }
                        StudioExtraPromptRow(
                            prompt = prompt,
                            enabled = !cropLocked,
                            onEdit = { extraPromptOpen = true },
                        )
                    }
                }
                StudioSwitchRow(
                    label = stringResource(R.string.studio_ai_use_framing),
                    checked = useCurrentFraming,
                    enabled = !cropLocked,
                ) { useCurrentFraming = it }
                PaperButton(stringResource(R.string.studio_ai_generate_version), enabled = canGenerate) {
                    genError = null
                    aiJobs.enqueueEdit(photo.id, source, presetId, prompt, appliedAiFraming)
                }
                GhostButton(stringResource(R.string.studio_ai_generate_to_gallery), enabled = canGenerate) {
                    genError = null
                    aiJobs.enqueueGenerate(photo.id, source, presetId, prompt, appliedAiFraming)
                }
                StudioNote(stringResource(R.string.studio_ai_generate_hint), topPadding = 0)
            }
        }
    }

    val outputPanel: @Composable () -> Unit = {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CopiesStepper(copies = copies, enabled = !cropLocked) { copies = it.coerceIn(1, 9) }
            PaperButton(stringResource(R.string.studio_add_to_print_queue), enabled = canPrintDirect) {
                startPrint(false)
            }
            GhostButton(stringResource(R.string.print_with_preset), enabled = canPrintWithPreset && !idActive) {
                startPrint(true)
            }
            if (idActive) {
                StudioNote(stringResource(R.string.studio_id_preset_print_off), topPadding = 0)
            }
            if (alreadyQueued) {
                StudioNote(stringResource(R.string.print_queue_already_queued), tone = Amber, topPadding = 0)
            }
            if (!printerReady) {
                Column {
                    Text(stringResource(R.string.print_need_printer), color = Amber)
                    TextButton(onClick = { onSettings("printer") }) { Text(stringResource(R.string.nav_settings), color = Amber) }
                }
            }
            GhostButton(stringResource(R.string.studio_export), enabled = !exporting && !cropLocked) {
                startExport()
            }
        }
    }

    // In ID mode the header names the size and the sheet instead of the ordinary crop.
    val sizeLabel = if (idActive) idSheetLabel(idPreset, idLayout) else "${working.width}×${working.height}"

    val onTabSelect: (StudioTab) -> Unit = { clicked ->
        if (clicked == tab && drawerExpanded) {
            drawerExpanded = false
        } else {
            tabKey = clicked.key
            drawerExpanded = true
        }
    }

    val drawerPanel: @Composable (maxHeight: Dp) -> Unit = { panelCap ->
        Column(Modifier.fillMaxWidth()) {
            Text(
                sizeLabel,
                fontFamily = MonoFont,
                color = PaperDim,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            StudioEqualHeightTabs(
                selected = tab,
                maxHeight = panelCap,
                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 12.dp),
                compose = composePanel,
                process = processPanel,
                output = outputPanel,
            )
        }
    }

    val printQueued = myPrint != null && jobFieldKey(myPrint.state) == "queued"
    val printLabel = when {
        myPrint == null -> ""
        printQueued && queueIndex > 0 -> stringResource(R.string.print_queue_queued_fmt, queueIndex)
        printQueued -> stringResource(R.string.job_state_queued)
        printPhase != null -> localizedPrintPhase(printPhase, printState)
        else -> stringResource(R.string.printing)
    }
    val printUi = when {
        myPrint == null -> null
        printQueued || myProgress?.progress == null && printPhase == null -> indeterminateUi(printLabel)
        myProgress?.progress != null -> myProgress.progress.toUi(printLabel)
        else -> indeterminateUi(printLabel)
    }
    val aiPhaseLabel = if (genPhase != null) {
        localizedGeneratePhase(genPhase)
    } else {
        stringResource(if (editing) R.string.editing else R.string.generating)
    }
    val aiBytes = localizedByteProgress(genLoaded, genTotal).ifEmpty { null }
    val aiUi = activeAi?.progress?.toUi(aiPhaseLabel, aiBytes) ?: indeterminateUi(aiPhaseLabel, aiBytes)
    val exportPhase = exportProgress?.phase ?: "rendering"
    val exportLabel = if (exportPhase == "saving") {
        stringResource(R.string.phase_export_saving)
    } else {
        stringResource(R.string.phase_export_rendering)
    }
    val exportUi = exportProgress?.toUi(exportLabel) ?: indeterminateUi(exportLabel)

    val topBar: @Composable () -> Unit = {
        Column {
            StudioTopBar(
                filename = photo.filename,
                generated = photo.isGenerated(),
                busy = aiBusy || alreadyQueued,
                onBack = onBack,
                onDelete = { pendingDelete = true },
            )
            DeleteUndoBar(
                pending = pendingDeletion,
                onUndo = { catalog.undoDelete(it) },
                onDeleteNow = { catalog.commitDeleteNow(it) },
            )
            if (myPrint != null && printUi != null) {
                JobProgressStrip(
                    title = stringResource(R.string.progress_print),
                    ui = printUi,
                    collapsed = printStripCollapsed,
                    extra = printerJobId?.let { stringResource(R.string.print_job_id, it) },
                    onCancel = { printQueue.cancel(myPrint.id) },
                    onHide = { printStripCollapsed = true },
                )
            }
            activeAi?.let { job ->
                JobProgressStrip(
                    title = stringResource(R.string.progress_generate),
                    ui = aiUi,
                    collapsed = aiStripCollapsed,
                    extra = aiBytes,
                    onCancel = { aiJobs.cancel(job.id) },
                    onHide = {
                        aiJobs.hide(job.id)
                        aiStripCollapsed = true
                    },
                )
            }
            if (exporting) {
                JobProgressStrip(
                    title = stringResource(R.string.progress_export),
                    ui = exportUi,
                    collapsed = exportStripCollapsed,
                    onHide = { exportStripCollapsed = true },
                )
            }
        }
    }

    val cropper: @Composable (Modifier) -> Unit = { cropModifier ->
        if (idActive) {
            // EDID030: the whole sheet, no watermark overlay (EDID010-R05).
            val cutout = rememberIdPreviewCutout(
                catalog = catalog,
                photoId = photo.id,
                source = source,
                imageFile = imageFile,
                maskRev = idStored?.maskRev,
                background = idBg,
                // EDID040-R13: an AI-feathered mask gets no on-device feather.
                // The inward switch is fixed too, so toggling it never recomposites the same image.
                feather = if (idStored?.aiFeatherLevel != null) IdFeather.NONE else idFeatherChoice,
                featherInward = if (idStored?.aiFeatherLevel != null) true else idFeatherInward,
                zoom = idZoom,
                layout = idLayout,
                imageWidth = orientedW,
                imageHeight = orientedH,
            )
            val perSheet = pluralStringResource(R.plurals.studio_id_per_sheet, idLayout.count, idLayout.count)
            val swapped = idBg.argb?.takeIf { idHasMask }
            // Reported up for the process tab; reset when the preview leaves (ID mode off).
            val compositing = cutout.compositing
            DisposableEffect(compositing) {
                idCompositing = compositing
                onDispose { idCompositing = false }
            }
            IdPhotoSheetPreview(
                model = imageFile,
                imageWidth = orientedW,
                imageHeight = orientedH,
                layout = idLayout,
                pose = idPose,
                onPoseChange = setIdPose,
                // The blank cell area follows the composite on screen, so it keeps the old colour
                // until the new composite lands.
                cellBackground = (cutout.argb ?: swapped)?.let { Color(it) } ?: Color.White,
                cutout = cutout.bitmap,
                overlay = idSheetLabel(idPreset, idLayout),
                description = stringResource(
                    R.string.studio_id_sheet_a11y,
                    "${idSheetLabel(idPreset, idLayout)} · $perSheet",
                ),
                snapState = snapState,
                modifier = cropModifier,
                enabled = !cropLocked && !compositing,
                note = if (cutout.failed) stringResource(R.string.studio_id_mask_missing) else null,
                compositing = compositing,
            )
        } else {
            WatermarkOverlay(
                settings = watermarkSettings,
                photo = photo,
                landscape = landscape,
                visible = watermarkArmed,
                modifier = cropModifier.background(Color.Black),
            ) {
                AspectCropper(
                    model = imageFile,
                    imageWidth = orientedW,
                    imageHeight = orientedH,
                    zoom = zoom,
                    panX = panX,
                    panY = panY,
                    onZoomChange = { zoom = it },
                    onPanChange = { x, y -> panX = x; panY = y },
                    overlay = if (landscape) landscapeOverlay else portraitOverlay,
                    aspect = if (landscape) PRINT_ASPECT_LANDSCAPE.toFloat() else PRINT_ASPECT.toFloat(),
                    rotationDegrees = rotationDegrees,
                    onRotationChange = { rotationDegrees = it },
                    panFrameWidth = sheetW,
                    panFrameHeight = sheetH,
                    // Lock only while a visible AI job is running. A queued print
                    // must not freeze framing — execution reads the latest saved pose,
                    // and enqueue saves the on-screen pose before the row can run.
                    enabled = !cropLocked,
                    snapState = snapState,
                )
            }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(Room)) {
        if (maxWidth >= 600.dp) {
            val panelWidth = (maxWidth * 0.36f).coerceIn(320.dp, 440.dp)
            Scaffold(
                containerColor = Room,
                contentWindowInsets = WindowInsets(0),
                topBar = topBar,
                snackbarHost = { DarkroomSnackbarHost(snackbar) },
            ) { padding ->
                Row(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .navigationBarsPadding()
                        .imePadding(),
                ) {
                    cropper(Modifier.weight(1f).fillMaxHeight())
                    Column(
                        Modifier
                            .width(panelWidth)
                            .fillMaxHeight()
                            .background(Room)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        Text(
                            sizeLabel,
                            fontFamily = MonoFont,
                            color = PaperDim,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        )
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .padding(bottom = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            when (tab) {
                                StudioTab.Compose -> composePanel()
                                StudioTab.Process -> processPanel()
                                StudioTab.Output -> outputPanel()
                            }
                        }
                        StudioTabBar(
                            selected = tab,
                            onSelect = { tabKey = it.key },
                        )
                    }
                }
            }
        } else {
            val panelCap = maxHeight * PANEL_MAX_FRACTION
            Scaffold(
                containerColor = Room,
                contentWindowInsets = WindowInsets(0),
                topBar = topBar,
                snackbarHost = { DarkroomSnackbarHost(snackbar) },
            ) { padding ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .imePadding(),
                ) {
                    cropper(Modifier.weight(1f).fillMaxWidth())
                    StudioControlsDrawer(
                        expanded = drawerExpanded,
                        onExpandedChange = { drawerExpanded = it },
                        panel = { drawerPanel(panelCap) },
                        tabBar = {
                            StudioTabBar(
                                selected = tab,
                                onSelect = onTabSelect,
                                includeNavigationBars = true,
                            )
                        },
                    )
                }
            }
        }
    }

    if (extraPromptOpen) {
        ExtraPromptDialog(
            value = prompt,
            onDismiss = { extraPromptOpen = false },
            onConfirm = {
                prompt = it
                extraPromptOpen = false
            },
        )
    }

    val cutoutResult = idMaskFailure
    if (idCutoutResultOpen && cutoutResult != null) {
        IdCutoutResultViewer(
            failure = cutoutResult,
            onDismiss = { idCutoutResultOpen = false },
        )
    }

    UserErrorDialog(printError, onDismiss = { printError = null })
    UserErrorDialog(genError, onDismiss = { genError = null })

    if (pendingDelete) {
        AlertDialog(
            onDismissRequest = { pendingDelete = false },
            title = { Text(stringResource(R.string.delete_photo_title)) },
            text = { Text(photo.filename) },
            confirmButton = {
                TextButton(
                    enabled = !aiBusy && !alreadyQueued,
                    onClick = {
                        val id = photo.id
                        pendingDelete = false
                        onBack()
                        // Deferred so the gallery (or this top bar, if we stay here) can offer
                        // the same undo window; the prompt reads catalog.pendingDeletion.
                        catalog.scheduleDelete(listOf(id))
                    },
                ) { Text(stringResource(R.string.common_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }

    LaunchedEffect(versionMenuId, photo.edits) {
        val id = versionMenuId ?: return@LaunchedEffect
        if (photo.edits.none { it.id == id }) versionMenuId = null
    }
    val menuEdit = versionMenuId?.let { id -> photo.edits.find { it.id == id } }
    menuEdit?.let { edit ->
        val menuIndex = editsInCreationOrder(photo.edits).indexOfFirst { it.id == edit.id }
        val menuName = resolveEditPresetName(
            edit.presetTitle,
            edit.presetId,
            versionFallback,
        ) { id -> presets.presets.find { it.id == id }?.title }
        val menuLabel = if (menuIndex >= 0) {
            versionLabel(menuIndex + 1, menuName, versionFallback)
        } else {
            menuName
        }
        AlertDialog(
            onDismissRequest = { versionMenuId = null },
            title = { Text(menuLabel) },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            versionMenuId = null
                            versionDeleteId = edit.id
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.studio_delete_version), modifier = Modifier.fillMaxWidth())
                    }
                    TextButton(
                        onClick = {
                            val editId = edit.id
                            versionMenuId = null
                            scope.launch {
                                try {
                                    val created = withContext(Dispatchers.IO) {
                                        val file = catalog.sourceFile(photo.id, editId)
                                        val index = editsInCreationOrder(photo.edits)
                                            .indexOfFirst { it.id == editId } + 1
                                        catalog.ingestBytes(
                                            file.readBytes(),
                                            studioVersionIngestName(photo.filename, index.coerceAtLeast(1)),
                                            kind = "studio",
                                            tags = listOf(GENERATED_PHOTO_TAG),
                                        )
                                    }
                                    onOpenGallery(created.id)
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    genError = e.message ?: e.toString()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.studio_add_version_to_gallery), modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { versionMenuId = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    versionDeleteId?.let { editId ->
        AlertDialog(
            onDismissRequest = { versionDeleteId = null },
            title = { Text(stringResource(R.string.studio_delete_version_title)) },
            confirmButton = {
                TextButton(
                    enabled = !aiBusy,
                    onClick = {
                        val id = editId
                        versionDeleteId = null
                        sourceOverride = sourceAfterVersionRemoved(photo.edits, id, source)
                        scope.launch {
                            try {
                                catalog.deleteEdit(photo.id, id)
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                genError = e.message ?: e.toString()
                            }
                        }
                    },
                ) { Text(stringResource(R.string.common_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { versionDeleteId = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudioTopBar(
    filename: String,
    generated: Boolean,
    busy: Boolean,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onCancelHiddenJob: (() -> Unit)? = null,
) {
    TopAppBar(
        title = {
            Column {
                if (generated) {
                    Text(
                        stringResource(R.string.photo_tag_generated),
                        color = Amber,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        fontFamily = MonoFont,
                        letterSpacing = 1.2.sp,
                    )
                }
                Text(filename, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Paper)
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.common_back), tint = Paper)
            }
        },
        actions = {
            if (onCancelHiddenJob != null) {
                IconButton(onClick = onCancelHiddenJob) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.common_cancel),
                        tint = Paper,
                    )
                }
            }
            IconButton(onClick = onDelete, enabled = !busy) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.common_delete),
                    tint = if (busy) PaperDim else Paper,
                )
            }
        },
        windowInsets = WindowInsets(0),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Room,
            titleContentColor = Paper,
            navigationIconContentColor = Paper,
            actionIconContentColor = Paper,
        ),
    )
}

@Composable
private fun CopiesStepper(copies: Int, enabled: Boolean, onChange: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.copies), color = PaperDim, modifier = Modifier.weight(1f))
        val fewer = stringResource(R.string.copies_decrease)
        val more = stringResource(R.string.copies_increase)
        IconButton(
            enabled = enabled && copies > 1,
            onClick = { onChange(copies - 1) },
            modifier = Modifier.semantics { contentDescription = fewer },
        ) {
            Text("−", color = if (enabled && copies > 1) Paper else PaperDim, fontSize = 20.sp)
        }
        Text("$copies", color = Paper, fontFamily = MonoFont, modifier = Modifier.padding(horizontal = 4.dp))
        IconButton(
            enabled = enabled && copies < 9,
            onClick = { onChange(copies + 1) },
            modifier = Modifier.semantics { contentDescription = more },
        ) {
            Text("+", color = if (enabled && copies < 9) Paper else PaperDim, fontSize = 20.sp)
        }
    }
}

private data class FramingPoseSnap(
    val photoId: String,
    val source: String,
    val ready: Boolean,
    val framing: Framing?,
)

private data class IdPoseSnap(
    val photoId: String,
    val source: String,
    val due: Boolean,
    val active: Boolean,
    val presetId: String,
    val pose: IdPhotoPose,
)

private fun exportDisplayName(photo: PhotoMeta): String {
    val base = photo.filename
        .substringAfterLast('/')
        .substringBeforeLast('.')
        .ifBlank { photo.id }
        .replace(Regex("[^A-Za-z0-9._-]+"), "_")
    return "$base-export.jpg"
}

private fun writeExportJpeg(context: Context, jpeg: ByteArray, displayName: String): Uri {
    val resolver = context.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Darkroom")
        put(MediaStore.Images.Media.IS_PENDING, 1)
    }
    val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    val uri = resolver.insert(collection, values) ?: error("export insert failed")
    try {
        resolver.openOutputStream(uri)?.use { it.write(jpeg) } ?: error("export write failed")
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return uri
    } catch (e: Exception) {
        runCatching { resolver.delete(uri, null, null) }
        throw e
    }
}

private fun shareExported(context: Context, uri: Uri) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "image/jpeg"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        clipData = ClipData.newUri(context.contentResolver, "", uri)
    }
    context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
