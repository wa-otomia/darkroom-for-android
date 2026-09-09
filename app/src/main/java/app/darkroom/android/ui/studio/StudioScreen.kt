package app.darkroom.android.ui.studio

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.darkroom.android.R
import app.darkroom.android.core.GENERATED_PHOTO_TAG
import app.darkroom.android.core.MAX_PRINT_ZOOM
import app.darkroom.android.core.MIN_PRINT_ZOOM
import app.darkroom.android.core.PRINT_ASPECT
import app.darkroom.android.core.PRINT_ASPECT_LANDSCAPE
import app.darkroom.android.core.PhotoMeta
import app.darkroom.android.core.SNAP_POSITION_DP
import app.darkroom.android.core.ViewportPlacement
import app.darkroom.android.core.defaultLandscape
import app.darkroom.android.core.editsInCreationOrder
import app.darkroom.android.core.isConfigured
import app.darkroom.android.core.exactQuarterTurns
import app.darkroom.android.core.formatCropSpec
import app.darkroom.android.core.isCancelledPrintMessage
import app.darkroom.android.core.isGenerated
import app.darkroom.android.core.listRelatedPhotos
import app.darkroom.android.core.newestVersionSource
import app.darkroom.android.core.normalizeRotationDegrees
import app.darkroom.android.core.parseCropSpec
import app.darkroom.android.core.resolveEditPresetName
import app.darkroom.android.core.resolveRootId
import app.darkroom.android.core.rotatePan
import app.darkroom.android.core.rotatedSize
import app.darkroom.android.core.sourceAfterVersionRemoved
import app.darkroom.android.core.studioVersionIngestName
import app.darkroom.android.core.versionLabel
import app.darkroom.android.data.catalog.CatalogRepository
import app.darkroom.android.data.imaging.ImagePipeline
import app.darkroom.android.data.jobs.AiJobs
import app.darkroom.android.data.jobs.AiKind
import app.darkroom.android.data.printer.PrintQueue
import app.darkroom.android.data.printer.StudioJobEvent
import app.darkroom.android.data.progress.JobKind
import app.darkroom.android.data.progress.JobProgress
import app.darkroom.android.data.progress.JobProgressTracker
import app.darkroom.android.data.progress.PrefsProgressStats
import app.darkroom.android.data.progress.exportPhases
import app.darkroom.android.data.progress.indeterminateUi
import app.darkroom.android.data.progress.toUi
import app.darkroom.android.data.settings.SettingsRepository
import app.darkroom.android.ui.UserErrorDialog
import app.darkroom.android.ui.components.AspectCropper
import app.darkroom.android.ui.components.DarkroomSnackbarHost
import app.darkroom.android.ui.components.GhostButton
import app.darkroom.android.ui.components.JobOverlayBar
import app.darkroom.android.ui.components.PaperButton
import app.darkroom.android.ui.components.PresetDropdown
import app.darkroom.android.ui.components.SectionLabel
import app.darkroom.android.ui.components.SnapDragState
import app.darkroom.android.ui.jobFieldKey
import app.darkroom.android.ui.localizedByteProgress
import app.darkroom.android.ui.localizedGeneratePhase
import app.darkroom.android.ui.localizedPrintPhase
import app.darkroom.android.ui.theme.Amber
import app.darkroom.android.ui.theme.MonoFont
import app.darkroom.android.ui.theme.Paper
import app.darkroom.android.ui.theme.PaperDim
import app.darkroom.android.ui.theme.Room
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
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
    var zoom by rememberSaveable(photoId) { mutableStateOf(1f) }
    var panX by rememberSaveable(photoId) { mutableStateOf(0f) }
    var panY by rememberSaveable(photoId) { mutableStateOf(0f) }
    var rotationDegrees by rememberSaveable(photoId) { mutableStateOf(0f) }
    var landscapeTouched by rememberSaveable(photoId) { mutableStateOf(false) }
    var landscape by rememberSaveable(photoId) {
        mutableStateOf(defaultLandscape(photo?.width ?: 0, photo?.height ?: 0))
    }
    LaunchedEffect(photoId, photo?.width, photo?.height) {
        val meta = photo ?: return@LaunchedEffect
        if (!landscapeTouched && meta.width > 0 && meta.height > 0) {
            landscape = defaultLandscape(meta.width, meta.height)
        }
    }
    var cropSpec by rememberSaveable(photoId, source) { mutableStateOf("") }
    var placement by remember(photoId, source) { mutableStateOf<ViewportPlacement?>(null) }
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

    val crop = remember(cropSpec) { parseCropSpec(cropSpec) }
    val myPrint = printJobs.find {
        it.photoId == photoId && jobFieldKey(it.state) in ACTIVE_PRINT_STATES
    }
    val generateJob = aiJobList.find { it.sourcePhotoId == photoId && it.kind == AiKind.Generate }
    val editJob = aiJobList.find { it.sourcePhotoId == photoId && it.kind == AiKind.Edit }
    val activeAi = editJob ?: generateJob
    val alreadyQueued = myPrint != null
    val generating = generateJob != null
    val editing = editJob != null
    val aiHidden = activeAi?.hidden == true
    val aiActive = generating || editing
    val aiBlocking = aiActive && !aiHidden
    val aiCancelledText = stringResource(R.string.ai_cancelled)
    val generatedText = stringResource(R.string.ai_generated)
    val viewGenerated = stringResource(R.string.ai_generated_view)
    val editDoneText = stringResource(R.string.edit_done)
    val versionFallback = stringResource(R.string.studio_version_fallback)
    val printDoneText = stringResource(R.string.print_done)
    val printQueuedText = stringResource(R.string.print_queued)
    var printStripCollapsed by rememberSaveable(photoId) { mutableStateOf(false) }
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
                    sourceOverride = e.editId
                    genError = null
                    launch { snackbar.showSnackbar(editDoneText) }
                }
                is StudioJobEvent.Failed -> {
                    if (e.action != "print") {
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
    val canGenerate = !aiActive && grokPub.keySet && hasInstructions
    val canPrintDirect = printerReady && !alreadyQueued
    val canPrintWithPreset = printerReady && grokPub.keySet && hasInstructions && !alreadyQueued
    val queuedIds = printJobs.filter { jobFieldKey(it.state) == "queued" }.map { it.id }
    val queueIndex = myPrint?.let { queuedIds.indexOf(it.id) }?.let { if (it >= 0) it + 1 else 0 } ?: 0
    LaunchedEffect(myPrint?.id) { printStripCollapsed = false }
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
            watermark = watermarkArmed,
        )
        scope.launch { snackbar.showSnackbar(printQueuedText) }
    }

    val startExport: () -> Unit = {
        if (!exporting) {
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
                        val jpeg = ImagePipeline.renderPrintJpeg(
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

    val composePanel: @Composable () -> Unit = {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column {
                SectionLabel(stringResource(R.string.studio_framing))
                FramingControls(
                    landscape = landscape,
                    rotationDegrees = rotationDegrees,
                    enabled = !cropLocked,
                    onOrientation = {
                        landscapeTouched = true
                        landscape = it
                    },
                    onRotate = onRotate,
                    onReset = {
                        zoom = 1f
                        panX = 0f
                        panY = 0f
                        rotationDegrees = 0f
                        landscapeTouched = false
                        val autoW = orientedW.takeIf { it > 0 } ?: photo.width
                        val autoH = orientedH.takeIf { it > 0 } ?: photo.height
                        landscape = defaultLandscape(autoW, autoH)
                        snapState.begin(0f, 0f, 0f)
                    },
                )
                if (landscape) {
                    StudioNote(stringResource(R.string.studio_landscape_note))
                }
            }
            Column {
                Text(stringResource(R.string.zoom), color = PaperDim)
                Slider(
                    value = zoom,
                    onValueChange = { zoom = it },
                    valueRange = MIN_PRINT_ZOOM..MAX_PRINT_ZOOM,
                    enabled = !cropLocked,
                    colors = SliderDefaults.colors(thumbColor = Amber, activeTrackColor = Amber),
                )
                if (cropLocked) {
                    StudioNote(stringResource(R.string.studio_crop_locked), tone = Amber)
                }
            }
            StudioWatermarkRow(
                checked = watermarkArmed,
                enabled = canWatermark && !cropLocked,
                hint = if (!canWatermark) stringResource(R.string.studio_watermark_unconfigured) else null,
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
                        onClick = { sourceOverride = "original" },
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
                            onClick = { sourceOverride = edit.id },
                            onLongClick = if (aiActive) {
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
            PaperButton(stringResource(R.string.studio_ai_generate_version), enabled = canGenerate) {
                genError = null
                aiJobs.enqueueEdit(photo.id, source, presetId, prompt)
            }
            GhostButton(stringResource(R.string.studio_ai_generate_to_gallery), enabled = canGenerate) {
                genError = null
                aiJobs.enqueueGenerate(photo.id, source, presetId, prompt)
            }
            StudioNote(stringResource(R.string.studio_ai_generate_hint), topPadding = 0)
        }
    }

    val outputPanel: @Composable () -> Unit = {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CopiesStepper(copies = copies, enabled = !cropLocked) { copies = it.coerceIn(1, 9) }
            PaperButton(stringResource(R.string.studio_add_to_print_queue), enabled = canPrintDirect) {
                startPrint(false)
            }
            GhostButton(stringResource(R.string.print_with_preset), enabled = canPrintWithPreset) {
                startPrint(true)
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
                "${working.width}×${working.height}",
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
    val showAiOverlay = aiBlocking
    val showExportOverlay = exporting && !exportStripCollapsed

    val topBar: @Composable () -> Unit = {
        StudioTopBar(
            filename = photo.filename,
            generated = photo.isGenerated(),
            busy = aiActive || alreadyQueued,
            onBack = onBack,
            onDelete = { pendingDelete = true },
        )
    }

    val jobIdExtra = printerJobId?.let { stringResource(R.string.print_job_id, it) }
    val printOverlayPhase = if (jobIdExtra.isNullOrBlank()) printLabel else "$printLabel · $jobIdExtra"

    val cropper: @Composable (Modifier) -> Unit = { cropModifier ->
        Box(cropModifier) {
            WatermarkOverlay(
                settings = watermarkSettings,
                photo = photo,
                landscape = landscape,
                visible = watermarkArmed,
                modifier = Modifier.fillMaxSize().background(Color.Black),
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
                    onCrop = { cropSpec = formatCropSpec(it) },
                    overlay = if (landscape) landscapeOverlay else portraitOverlay,
                    aspect = if (landscape) PRINT_ASPECT_LANDSCAPE.toFloat() else PRINT_ASPECT.toFloat(),
                    rotationDegrees = rotationDegrees,
                    onRotationChange = { rotationDegrees = it },
                    onPlacement = { placement = it },
                    // Lock only while a visible AI job is running. A queued print
                    // must not freeze framing — the crop was already captured at enqueue.
                    enabled = !cropLocked,
                    snapState = snapState,
                )
            }
            Column(Modifier.align(Alignment.TopCenter).fillMaxWidth()) {
                if (showAiOverlay && activeAi != null) {
                    JobOverlayBar(
                        phase = aiPhaseLabel,
                        ui = aiUi,
                        onHide = { aiJobs.hide(activeAi.id) },
                        onCancel = { aiJobs.cancel(activeAi.id) },
                    )
                }
                if (showExportOverlay) {
                    JobOverlayBar(
                        phase = exportLabel,
                        ui = exportUi,
                        onHide = { exportStripCollapsed = true },
                    )
                }
                val printJob = myPrint
                val printOverlayUi = printUi
                if (!printStripCollapsed && printJob != null && printOverlayUi != null) {
                    JobOverlayBar(
                        phase = printOverlayPhase,
                        ui = printOverlayUi,
                        onHide = { printStripCollapsed = true },
                        onCancel = { printQueue.cancel(printJob.id) },
                    )
                }
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
                            "${working.width}×${working.height}",
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

    UserErrorDialog(printError, onDismiss = { printError = null })
    UserErrorDialog(genError, onDismiss = { genError = null })

    if (pendingDelete) {
        AlertDialog(
            onDismissRequest = { pendingDelete = false },
            title = { Text(stringResource(R.string.delete_photo_title)) },
            text = { Text(photo.filename) },
            confirmButton = {
                TextButton(
                    enabled = !aiActive && !alreadyQueued,
                    onClick = {
                        val id = photo.id
                        pendingDelete = false
                        onBack()
                        // Deferred so the gallery can offer the same undo window it gives its own
                        // deletions; it renders the prompt off catalog.pendingDeletion.
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
                    enabled = !aiActive,
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
