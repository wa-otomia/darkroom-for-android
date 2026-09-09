package app.darkroom.android.ui.gallery

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.text.format.DateUtils
import androidx.core.content.FileProvider
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.darkroom.android.R
import app.darkroom.android.core.EditJobOverlay
import app.darkroom.android.core.GalleryItem
import app.darkroom.android.core.OverlayKind
import app.darkroom.android.core.PendingKind
import app.darkroom.android.core.PhotoMeta
import app.darkroom.android.core.PrintJobSnapshot
import app.darkroom.android.core.fallbackImportName
import app.darkroom.android.core.isJpegBytes
import app.darkroom.android.core.isJpegName
import app.darkroom.android.core.mergeGalleryItems
import app.darkroom.android.data.automation.Automation
import app.darkroom.android.data.catalog.CatalogRepository
import app.darkroom.android.data.imaging.ImagePipeline
import app.darkroom.android.data.jobs.AiJob
import app.darkroom.android.data.printer.PrintJob
import app.darkroom.android.data.printer.PrintProgress
import app.darkroom.android.data.printer.PrintQueue
import app.darkroom.android.data.progress.JobKind
import app.darkroom.android.data.progress.JobProgressTracker
import app.darkroom.android.data.progress.PrefsProgressStats
import app.darkroom.android.data.progress.ProgressUi
import app.darkroom.android.data.progress.SizeSource
import app.darkroom.android.data.progress.batchOverall
import app.darkroom.android.data.progress.failedProgressUi
import app.darkroom.android.data.progress.indeterminateUi
import app.darkroom.android.data.progress.ingestPhases
import app.darkroom.android.data.progress.toUi
import app.darkroom.android.data.transfer.IncomingTransfer
import app.darkroom.android.ui.UserErrorDialog
import app.darkroom.android.ui.components.AmberTrack
import app.darkroom.android.ui.components.DarkroomSnackbarHost
import app.darkroom.android.ui.components.DeleteUndoBar
import app.darkroom.android.ui.components.JobProgressStrip
import app.darkroom.android.ui.components.JobScrim
import app.darkroom.android.ui.components.StatusBadge
import app.darkroom.android.ui.components.StatusChip
import app.darkroom.android.ui.components.contentBlur
import app.darkroom.android.ui.theme.SurfacePanel
import app.darkroom.android.ui.jobFieldKey
import app.darkroom.android.ui.localizedByteProgress
import app.darkroom.android.ui.localizedGeneratePhase
import app.darkroom.android.ui.localizedPrintPhase
import app.darkroom.android.ui.localizedTransferPhase
import app.darkroom.android.ui.theme.Amber
import app.darkroom.android.ui.theme.Danger
import app.darkroom.android.ui.theme.Ink
import app.darkroom.android.ui.theme.MonoFont
import app.darkroom.android.ui.theme.Paper
import app.darkroom.android.ui.theme.PaperDim
import app.darkroom.android.ui.theme.PaperHairline
import app.darkroom.android.ui.theme.Room
import app.darkroom.android.ui.userErrorRes
import app.darkroom.android.ui.theme.SurfaceLow
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

private const val MAX_LISTED_FILES = 6

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun GalleryScreen(
    photos: List<PhotoMeta>,
    transfers: List<IncomingTransfer>,
    aiJobs: List<AiJob>,
    grokReady: Boolean,
    printerReady: Boolean,
    autoEdit: Boolean,
    autoWatermark: Boolean,
    autoPrint: Boolean,
    catalog: CatalogRepository,
    automation: Automation,
    printQueue: PrintQueue,
    reselect: Flow<Unit> = emptyFlow(),
    onOpen: (String) -> Unit,
    onDismissTransfer: (String) -> Unit,
    onCancelAi: (String) -> Unit,
    onSettings: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val grid = rememberLazyGridState()
    val progressStats = remember(context) { PrefsProgressStats(context.applicationContext) }

    var busy by remember { mutableStateOf(false) }
    var importDone by remember { mutableStateOf(0) }
    var importTotal by remember { mutableStateOf(0) }
    var importPhase by remember { mutableStateOf("receiving") }
    var importFileFraction by remember { mutableStateOf(0f) }
    var importIndeterminate by remember { mutableStateOf(false) }
    var importReport by remember { mutableStateOf<ImportReport?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    var selection by rememberSaveable(stateSaver = IdSetSaver) { mutableStateOf(emptySet<String>()) }
    var seenTopId by rememberSaveable { mutableStateOf<String?>(null) }
    var newArrivals by rememberSaveable { mutableStateOf(0) }

    val printJobs by printQueue.printJobs.collectAsState(initial = emptyList())
    val runningProgress by printQueue.runningProgress.collectAsState(initial = null)
    val printSnapshots = remember(printJobs, runningProgress) {
        printJobs.map { it.toGallerySnapshot(runningProgress) }
    }
    val items = remember(photos, transfers, aiJobs, printSnapshots) {
        mergeGalleryItems(photos, transfers, aiJobs, printSnapshots)
    }
    val photoItems = remember(items) { items.filterIsInstance<GalleryItem.Photo>() }
    val selectionMode = selection.isNotEmpty()
    val deletedTemplate = stringResource(R.string.lib_gallery_deleted)
    val undoLabel = stringResource(R.string.lib_action_undo)
    val noCameraText = stringResource(R.string.gallery_no_camera_app)
    val snackbar = remember { SnackbarHostState() }
    var pendingCapture by remember { mutableStateOf<File?>(null) }

    suspend fun runImport(uris: List<Uri>, kind: String) {
        if (uris.isEmpty()) return
        busy = true
        error = null
        importDone = 0
        importTotal = uris.size
        val failures = mutableListOf<ImportOutcome.Failed>()
        val converted = mutableListOf<String>()
        var imported = 0
        try {
            uris.forEach { uri ->
                val name = displayName(context, uri)
                val size = querySize(context, uri)
                val includeTranscode = !isJpegName(name)
                val fileTracker = JobProgressTracker(
                    jobId = uri.toString(),
                    kind = JobKind.Import,
                    phases = ingestPhases(includeTranscode, progressStats, JobKind.Import),
                    stats = progressStats,
                )
                val source = if (size != null && size > 0L) SizeSource.Allo else SizeSource.Unknown
                fileTracker.startPhase("receiving", size, source)
                importPhase = "receiving"
                importFileFraction = 0f
                importIndeterminate = source == SizeSource.Unknown && uris.size == 1
                coroutineScope {
                    val ticker = fileTracker.launchTicks(this) { snap ->
                        importPhase = snap.phase
                        importFileFraction = snap.fraction
                        importIndeterminate = snap.sizeSource == SizeSource.Unknown &&
                            snap.phase == "receiving" &&
                            uris.size == 1
                    }
                    try {
                        when (
                            val outcome = withContext(Dispatchers.IO) {
                                importOne(context, catalog, uri, kind) { phase, loaded, total ->
                                    if (phase != fileTracker.snapshot().phase) {
                                        fileTracker.startPhase(phase, total, source)
                                    }
                                    if (loaded != null) fileTracker.setBytes(loaded, total)
                                }
                            }
                        ) {
                            is ImportOutcome.Stored -> {
                                imported++
                                runCatching { automation.onIngested(outcome.photoId, kind) }
                            }
                            is ImportOutcome.Converted -> {
                                imported++
                                converted += outcome.name
                                runCatching { automation.onIngested(outcome.photoId, kind) }
                            }
                            is ImportOutcome.Failed -> failures += outcome
                        }
                        fileTracker.succeed()
                    } finally {
                        ticker.cancel()
                    }
                }
                importDone++
                importFileFraction = 0f
                importIndeterminate = false
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message
        } finally {
            busy = false
            importDone = 0
            importTotal = 0
            importFileFraction = 0f
            importIndeterminate = false
        }
        // Conversion is lossy, so it is reported even when nothing failed.
        if (failures.isNotEmpty() || converted.isNotEmpty()) {
            importReport = ImportReport(imported, converted.toList(), failures.toList())
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch { runImport(uris, "import") }
    }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val file = pendingCapture
        pendingCapture = null
        scope.launch {
            try {
                if (success && file != null && file.length() > 0L) {
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
                    runImport(listOf(uri), "camera")
                }
            } finally {
                file?.delete()
            }
        }
    }
    fun launchCamera() {
        if (busy) return
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(context.packageManager) == null) {
            scope.launch { snackbar.showSnackbar(noCameraText) }
            return
        }
        val dir = File(context.cacheDir, "capture").apply { mkdirs() }
        val file = File(dir, "${UUID.randomUUID()}.jpg")
        runCatching { file.createNewFile() }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        pendingCapture = file
        takePicture.launch(uri)
    }

    // A crash between the file delete and the row delete leaves a folder behind; clean it up once
    // per visit rather than letting it accumulate.
    LaunchedEffect(Unit) {
        runCatching { catalog.pruneOrphans() }
        runCatching { catalog.pruneStaleInbox() }
    }

    // Drop ids that left the catalog (deleted elsewhere, or undone) so the count stays honest.
    // Pending cards are never selectable.
    LaunchedEffect(photoItems) {
        if (selection.isEmpty()) return@LaunchedEffect
        val live = photoItems.mapTo(HashSet<String>()) { it.photoId }
        val next = selection.filterTo(LinkedHashSet<String>()) { it in live }
        if (next.size != selection.size) selection = next
    }

    val atTop by remember { derivedStateOf { grid.firstVisibleItemIndex == 0 && grid.firstVisibleItemScrollOffset < 4 } }
    val topId = photos.firstOrNull()?.id
    LaunchedEffect(topId, photos.size) {
        if (topId == null) {
            seenTopId = null
            newArrivals = 0
            return@LaunchedEffect
        }
        val previous = seenTopId
        if (previous == null || previous == topId) {
            seenTopId = topId
            return@LaunchedEffect
        }
        // How many photos landed above the newest one we had already shown.
        val added = photos.indexOfFirst { it.id == previous }
        if (added <= 0) {
            // The previous top is gone (deleted), so there is nothing new to announce.
            seenTopId = topId
            newArrivals = 0
            return@LaunchedEffect
        }
        if (atTop && !selectionMode) {
            seenTopId = topId
            newArrivals = 0
            grid.animateScrollToItem(0)
        } else {
            newArrivals = added
        }
    }

    // Scrolling back up by hand answers the prompt just as well as tapping it.
    LaunchedEffect(atTop) {
        if (atTop && newArrivals > 0) {
            seenTopId = photos.firstOrNull()?.id
            newArrivals = 0
        }
    }

    // Re-tapping the tab that is already open arrives from the navigation layer.
    val latestPhotos by rememberUpdatedState(photos)
    LaunchedEffect(reselect, grid) {
        reselect.collect {
            // Returning to the top answers the new-photo prompt, so retire it rather than
            // leaving it floating over the newest row.
            seenTopId = latestPhotos.firstOrNull()?.id
            newArrivals = 0
            grid.animateScrollToItem(0)
        }
    }

    // The undo prompt follows the repository rather than this screen's own state, because a
    // deletion started from the photo detail screen pops straight back here: the screen that has
    // to offer the undo is never the one that asked for the delete. The 15 s commit lives on
    // the repository timer — this bar only offers undo.
    val pendingDeletion by catalog.pendingDeletion.collectAsState()
    val deleteProgress by catalog.deleteProgress.collectAsState()

    fun deleteSelected() {
        val ids = selection.toList()
        if (ids.isEmpty()) return
        selection = emptySet()
        catalog.scheduleDelete(ids)
    }

    BackHandler(enabled = selectionMode) { selection = emptySet() }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val columns = when {
            maxWidth >= 840.dp -> 4
            maxWidth >= 600.dp -> 3
            else -> 2
        }
        Scaffold(
            // containerColor/contentColor default to background/onBackground, i.e. Room/Paper.
            contentWindowInsets = WindowInsets(0),
            topBar = {
                if (selectionMode) {
                    SelectionBar(
                        count = selection.size,
                        allSelected = photoItems.isNotEmpty() && selection.size == photoItems.size,
                        onExit = { selection = emptySet() },
                        onSelectAll = { selection = photoItems.mapTo(LinkedHashSet<String>()) { it.photoId } },
                        onDelete = { deleteSelected() },
                    )
                }
            },
            snackbarHost = {
                deleteProgress?.let { deleting ->
                    val fraction = if (deleting.total > 0) deleting.done.toFloat() / deleting.total else 0f
                    JobProgressStrip(
                        title = stringResource(R.string.progress_delete),
                        ui = ProgressUi(
                            percent = (fraction * 100).toInt().coerceIn(0, 100),
                            fill = fraction,
                            phaseLabel = stringResource(R.string.progress_delete_fmt, deleting.done, deleting.total),
                            indeterminate = false,
                        ),
                        collapsed = false,
                    )
                }
                pendingDeletion?.let { pending ->
                    DeleteUndoBar(
                        message = deletedTemplate.format(pending.ids.size),
                        undoLabel = undoLabel,
                        onUndo = { catalog.undoDelete(pending.token) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                DarkroomSnackbarHost(snackbar)
            },
            floatingActionButton = {
                if (!selectionMode) {
                    Column(horizontalAlignment = Alignment.End) {
                        SmallFloatingActionButton(
                            onClick = { if (!busy) launchCamera() },
                            containerColor = Paper,
                            contentColor = Ink,
                        ) {
                            Icon(
                                Icons.Outlined.PhotoCamera,
                                contentDescription = stringResource(R.string.gallery_take_photo),
                            )
                        }
                        Spacer(Modifier.size(12.dp))
                        ExtendedFloatingActionButton(
                            text = { Text(if (busy) stringResource(R.string.importing) else stringResource(R.string.manual_import)) },
                            icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                            containerColor = Paper,
                            contentColor = Ink,
                            onClick = { if (!busy) picker.launch("image/*") },
                        )
                    }
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding).contentBlur(busy)) {
                LazyVerticalGrid(
                    state = grid,
                    columns = GridCells.Fixed(columns),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 168.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        GalleryHeader(
                            count = items.size,
                            empty = items.isEmpty(),
                            grokReady = grokReady,
                            printerReady = printerReady,
                            autoEdit = autoEdit,
                            autoWatermark = autoWatermark,
                            autoPrint = autoPrint,
                            onSettings = onSettings,
                        )
                    }
                    items(items, key = { it.photoId }) { item ->
                        when (item) {
                            is GalleryItem.Pending -> PhotoCard(
                                photo = null,
                                thumb = catalog.thumbFile(item.photoId),
                                pending = item,
                                sourceThumb = item.sourcePhotoId?.let { catalog.thumbFile(it) },
                                overlay = null,
                                selectionMode = selectionMode,
                                selected = false,
                                modifier = Modifier.animateItem(),
                                onOpen = {},
                                onToggle = {},
                                onEnterSelection = {},
                                onDismissPending = { item.dismissId?.let(onDismissTransfer) },
                                onCancelPending = { item.cancelId?.let(onCancelAi) },
                                onCancelOverlay = {},
                            )
                            is GalleryItem.Photo -> {
                                val selected = item.photoId in selection
                                PhotoCard(
                                    photo = item.meta,
                                    thumb = catalog.thumbFile(item.photoId),
                                    pending = null,
                                    sourceThumb = null,
                                    overlay = item.overlay,
                                    selectionMode = selectionMode,
                                    selected = selected,
                                    modifier = Modifier.animateItem(),
                                    onOpen = { onOpen(item.photoId) },
                                    onToggle = {
                                        selection = if (selected) selection - item.photoId else selection + item.photoId
                                    },
                                    onEnterSelection = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        selection = selection + item.photoId
                                    },
                                    onDismissPending = {},
                                    onCancelPending = {},
                                    onCancelOverlay = {
                                        val overlay = item.overlay ?: return@PhotoCard
                                        when (overlay.kind) {
                                            OverlayKind.Print -> printQueue.cancel(overlay.jobId)
                                            OverlayKind.Edit -> onCancelAi(overlay.jobId)
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
                if (newArrivals > 0) {
                    NewArrivalsPill(
                        count = newArrivals,
                        // Horizontal insets keep the pill inside the screen at a large font scale.
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 12.dp, start = 20.dp, end = 20.dp),
                    ) {
                        seenTopId = photos.firstOrNull()?.id
                        newArrivals = 0
                        scope.launch { grid.animateScrollToItem(0) }
                    }
                }
            }
        }
        if (busy) {
            val overall = if (importTotal <= 0) {
                0f
            } else {
                batchOverall(importDone, importTotal, importFileFraction)
            }
            val phase = localizedTransferPhase(importPhase)
            JobScrim(
                title = stringResource(R.string.lib_gallery_import_title),
                ui = ProgressUi(
                    percent = if (importIndeterminate) null else (overall * 100).toInt().coerceIn(0, 99),
                    fill = overall,
                    phaseLabel = phase,
                    detail = stringResource(R.string.lib_gallery_import_label, importDone, importTotal),
                    indeterminate = importIndeterminate || importTotal == 0,
                ),
            )
        }
    }

    importReport?.let { report ->
        ImportReportDialog(report) { importReport = null }
    }
    UserErrorDialog(error, onDismiss = { error = null })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GalleryHeader(
    count: Int,
    empty: Boolean,
    grokReady: Boolean,
    printerReady: Boolean,
    autoEdit: Boolean,
    autoWatermark: Boolean,
    autoPrint: Boolean,
    onSettings: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Text(stringResource(R.string.gallery_inbox_live), style = MaterialTheme.typography.labelSmall, color = Amber)
        Text(
            if (empty) stringResource(R.string.gallery_waiting) else stringResource(R.string.gallery_count, count),
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            stringResource(R.string.gallery_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = PaperDim,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            stringResource(R.string.lib_gallery_import_formats),
            color = PaperDim,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 6.dp),
        )
        if (count > 0) {
            Text(
                stringResource(R.string.lib_gallery_long_press_hint),
                color = PaperDim,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 16.dp),
        ) {
            StatusChip(grokReady, stringResource(R.string.chip_grok_ok), stringResource(R.string.chip_grok_off)) { onSettings("grok") }
            StatusChip(printerReady, stringResource(R.string.chip_printer_ok), stringResource(R.string.chip_printer_off)) { onSettings("printer") }
            val autoOn = autoEdit || autoWatermark || autoPrint
            val autoParts = buildList {
                if (autoEdit) add(stringResource(R.string.chip_automation_edit))
                if (autoWatermark) add(stringResource(R.string.chip_automation_watermark))
                if (autoPrint) add(stringResource(R.string.chip_automation_print))
            }
            val autoOnLabel = if (autoParts.isEmpty()) {
                stringResource(R.string.chip_automation_off)
            } else {
                stringResource(R.string.chip_automation, autoParts.joinToString(" · "))
            }
            StatusChip(
                autoOn,
                autoOnLabel,
                stringResource(R.string.chip_automation_off),
            ) { onSettings("autoprint") }
        }
        if (empty) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    // Decorative frame, not an affordance, so it stays at hairline weight.
                    .border(1.dp, PaperHairline, RoundedCornerShape(2.dp))
                    .padding(28.dp),
            ) {
                Column {
                    Text(stringResource(R.string.empty_title), style = MaterialTheme.typography.headlineMedium)
                    Text(stringResource(R.string.empty_desc), style = MaterialTheme.typography.bodyMedium, color = PaperDim, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoCard(
    photo: PhotoMeta?,
    thumb: File,
    pending: GalleryItem.Pending?,
    sourceThumb: File?,
    overlay: EditJobOverlay?,
    selectionMode: Boolean,
    selected: Boolean,
    modifier: Modifier,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onEnterSelection: () -> Unit,
    onDismissPending: () -> Unit,
    onCancelPending: () -> Unit,
    onCancelOverlay: () -> Unit,
) {
    val context = LocalContext.current
    val request = remember(thumb) {
        ImageRequest.Builder(context).data(thumb).crossfade(220).build()
    }
    var thumbState by remember(thumb) { mutableStateOf(ThumbState.LOADING) }
    val timeLabel = remember(photo?.ingestedAt, context) {
        photo?.ingestedAt?.let { photoTimeLabel(context, it) }.orEmpty()
    }
    val failedPending = pending != null && (!pending.error.isNullOrBlank() || pending.dismissId != null)
    val selectable = photo != null && pending == null

    Column(
        modifier
            .clip(RoundedCornerShape(2.dp))
            .background(SurfaceLow)
            // The photo is the affordance here, so the unselected outline is a hairline: at
            // PaperFaint it competes with the image it frames.
            .border(1.dp, if (selected) Amber else PaperHairline, RoundedCornerShape(2.dp))
            .combinedClickable(
                onClick = {
                    when {
                        failedPending -> onDismissPending()
                        selectionMode && selectable -> onToggle()
                        selectable -> onOpen()
                    }
                },
                onLongClick = {
                    if (selectable) {
                        if (selectionMode) onToggle() else onEnterSelection()
                    }
                },
            ),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .background(SurfaceLow),
        ) {
            Crossfade(
                targetState = pending != null,
                animationSpec = tween(220),
                label = "galleryPending",
            ) { isPending ->
                if (isPending && pending != null) {
                    PendingFace(pending = pending, sourceThumb = sourceThumb)
                } else if (photo != null) {
                    AsyncImage(
                        model = request,
                        contentDescription = photo.filename,
                        contentScale = ContentScale.Crop,
                        onState = { state ->
                            thumbState = when (state) {
                                is AsyncImagePainter.State.Success -> ThumbState.OK
                                is AsyncImagePainter.State.Error -> ThumbState.FAILED
                                else -> ThumbState.LOADING
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(Modifier.fillMaxSize().background(SurfaceLow))
                }
            }
            // Keeps the badge and the selection mark readable on bright photos.
            if (pending == null) {
                Box(
                    Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Room.copy(alpha = 0.5f),
                                0.35f to Color.Transparent,
                            ),
                        ),
                )
            }
            if (selected) {
                Box(Modifier.matchParentSize().background(Room.copy(alpha = 0.4f)))
            }
            if (pending == null && thumbState == ThumbState.FAILED) {
                Column(
                    Modifier
                        .matchParentSize()
                        .background(Room.copy(alpha = 0.88f))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        Icons.Outlined.BrokenImage,
                        contentDescription = null,
                        tint = PaperDim,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        stringResource(R.string.lib_gallery_thumb_failed),
                        color = PaperDim,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
            if (photo != null && pending == null) {
                StatusBadge(
                    visible = photo.edits.isNotEmpty() || photo.parentId != null,
                    text = if (photo.edits.isNotEmpty()) {
                        stringResource(R.string.badge_edited)
                    } else {
                        stringResource(R.string.generated_prefix).trim(' ', '·')
                    },
                    modifier = Modifier.align(Alignment.TopStart).padding(6.dp),
                )
            }
            if (selectionMode && selectable) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (selected) Amber else Room.copy(alpha = 0.6f))
                        .border(1.dp, if (selected) Amber else Paper, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected) {
                        Icon(
                            Icons.Outlined.Check,
                            contentDescription = null,
                            tint = Ink,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            } else if (overlay != null && !selectionMode) {
                IconButton(
                    onClick = onCancelOverlay,
                    modifier = Modifier.align(Alignment.TopEnd),
                ) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.common_cancel),
                        tint = Paper,
                    )
                }
            }
            if (pending != null) {
                PendingFooter(
                    pending = pending,
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                    onCancel = onCancelPending,
                )
            } else if (overlay != null) {
                OverlayBar(
                    overlay = overlay,
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                )
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)) {
            val name = pending?.filename
                ?: pending?.let { stringResource(if (it.kind == PendingKind.Generate) R.string.generating else R.string.importing) }
                ?: photo?.filename.orEmpty()
            val prefix = if (photo?.parentId != null && pending == null) {
                stringResource(R.string.generated_prefix)
            } else {
                ""
            }
            Text(
                prefix + name,
                color = Paper,
                fontSize = 12.sp,
                fontFamily = MonoFont,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                timeLabel,
                color = PaperDim,
                fontSize = 12.sp,
                fontFamily = MonoFont,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun PendingFace(pending: GalleryItem.Pending, sourceThumb: File?) {
    Box(Modifier.fillMaxSize().background(SurfaceLow)) {
        if (pending.kind == PendingKind.Generate && sourceThumb != null) {
            AsyncImage(
                model = sourceThumb,
                contentDescription = pending.filename,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (Build.VERSION.SDK_INT >= 31) Modifier.blur(18.dp) else Modifier),
            )
            if (Build.VERSION.SDK_INT >= 31) {
                Box(Modifier.matchParentSize().background(Room.copy(alpha = 0.32f)))
            }
        } else {
            Box(Modifier.fillMaxSize().background(SurfaceLow), contentAlignment = Alignment.Center) {
                Text(
                    pending.filename ?: stringResource(R.string.importing),
                    color = PaperDim,
                    fontFamily = MonoFont,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }
        if (!pending.error.isNullOrBlank()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .background(SurfacePanel)
                    .padding(10.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    stringResource(R.string.transfer_phase_failed),
                    color = Amber,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                )
                Text(
                    pending.error,
                    color = PaperDim,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun PendingFooter(
    pending: GalleryItem.Pending,
    modifier: Modifier,
    onCancel: () -> Unit,
) {
    val failed = !pending.error.isNullOrBlank() || pending.dismissId != null
    val phase = if (pending.kind == PendingKind.Transfer) {
        localizedTransferPhase(pending.phase)
    } else {
        localizedGeneratePhase(pending.phase)
    }
    val detail = localizedByteProgress(pending.loaded, pending.total).ifEmpty { null }
    val ui = pending.jobProgress?.toUi(phase, detail)
        ?: if (failed) {
            failedProgressUi(phase, detail)
        } else {
            indeterminateUi(phase, detail)
        }
    val estimated = stringResource(R.string.progress_estimated)
    Column(
        modifier
            .background(SurfacePanel)
            .border(1.dp, PaperHairline),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 8.dp, end = 2.dp, top = 6.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                when {
                    failed -> stringResource(R.string.transfer_phase_failed)
                    ui.estimated -> "$phase · $estimated"
                    else -> phase
                },
                color = if (failed) Amber else Paper,
                fontSize = 12.sp,
                fontFamily = MonoFont,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (pending.cancelable) {
                IconButton(onClick = onCancel) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.common_cancel),
                        tint = Paper,
                    )
                }
            }
        }
        AmberTrack(
            ui = if (failed) ui.copy(indeterminate = false, percent = null, fill = 0f) else ui,
            modifier = Modifier.fillMaxWidth(),
            height = 3.dp,
        )
    }
}

@Composable
private fun OverlayBar(overlay: EditJobOverlay, modifier: Modifier) {
    val phase = if (overlay.kind == OverlayKind.Print) {
        localizedPrintPhase(overlay.phase, overlay.jobProgress?.jobState)
    } else {
        localizedGeneratePhase(overlay.phase)
    }
    val detail = localizedByteProgress(overlay.loaded, overlay.total).ifEmpty { null }
    val ui = overlay.jobProgress?.toUi(phase, detail) ?: indeterminateUi(phase, detail)
    val estimated = stringResource(R.string.progress_estimated)
    Column(
        modifier
            .background(SurfacePanel)
            .border(1.dp, PaperHairline),
    ) {
        Text(
            if (ui.estimated) "$phase · $estimated" else phase,
            color = Paper,
            fontSize = 12.sp,
            fontFamily = MonoFont,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
        )
        AmberTrack(
            ui = ui,
            modifier = Modifier.fillMaxWidth(),
            height = 3.dp,
        )
    }
}

@Composable
private fun SelectionBar(
    count: Int,
    allSelected: Boolean,
    onExit: () -> Unit,
    onSelectAll: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().background(SurfaceLow)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onExit) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.lib_gallery_selection_exit),
                    tint = Paper,
                )
            }
            Text(
                stringResource(R.string.lib_gallery_selection_count, count),
                color = Paper,
                fontFamily = MonoFont,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = 4.dp),
            )
            if (!allSelected) {
                IconButton(onClick = onSelectAll) {
                    Icon(
                        Icons.Outlined.SelectAll,
                        contentDescription = stringResource(R.string.lib_gallery_select_all),
                        tint = Paper,
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.common_delete),
                    tint = Danger,
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(PaperHairline))
    }
}

@Composable
private fun NewArrivalsPill(count: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Paper)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Outlined.ArrowUpward,
            contentDescription = null,
            tint = Ink,
            modifier = Modifier.size(16.dp),
        )
        Text(
            stringResource(R.string.lib_gallery_new_photos, count),
            color = Ink,
            fontSize = 13.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun ImportReportDialog(report: ImportReport, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.lib_gallery_import_result_title)) },
        text = {
            Column {
                Text(stringResource(R.string.lib_gallery_import_ok, report.imported), color = PaperDim)
                if (report.converted.isNotEmpty()) {
                    Text(
                        stringResource(R.string.lib_gallery_import_converted_head, report.converted.size),
                        color = Paper,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    report.converted.take(MAX_LISTED_FILES).forEach { name ->
                        ReportLine(name)
                    }
                    MoreLine(report.converted.size - MAX_LISTED_FILES)
                }
                if (report.failures.isNotEmpty()) {
                    Text(
                        stringResource(R.string.lib_gallery_import_failed_head, report.failures.size),
                        color = Danger,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    report.failures.take(MAX_LISTED_FILES).forEach { failure ->
                        val reason = stringResource(failure.reason)
                        ReportLine(stringResource(R.string.lib_gallery_import_failed_item, failure.name, reason))
                    }
                    MoreLine(report.failures.size - MAX_LISTED_FILES)
                }
                Text(
                    stringResource(R.string.lib_gallery_import_formats),
                    color = PaperDim,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_back)) }
        },
    )
}

@Composable
private fun ReportLine(text: String) {
    Text(
        text,
        color = PaperDim,
        fontSize = 12.sp,
        fontFamily = MonoFont,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun MoreLine(extra: Int) {
    if (extra <= 0) return
    Text(
        stringResource(R.string.lib_gallery_import_more, extra),
        color = PaperDim,
        fontSize = 12.sp,
        modifier = Modifier.padding(top = 4.dp),
    )
}

private enum class ThumbState { LOADING, OK, FAILED }

private sealed interface ImportOutcome {
    /** Already JPEG, archived byte-for-byte. */
    data class Stored(val photoId: String) : ImportOutcome

    /** Transcoded to JPEG, so the pixels were re-encoded and possibly scaled down. */
    data class Converted(val name: String, val photoId: String) : ImportOutcome

    data class Failed(val name: String, val reason: Int) : ImportOutcome
}

private data class ImportReport(
    val imported: Int,
    val converted: List<String>,
    val failures: List<ImportOutcome.Failed>,
)

private val IdSetSaver: Saver<Set<String>, Any> = listSaver<Set<String>, String>(
    save = { it.toList() },
    restore = { it.toSet() },
)

private suspend fun importOne(
    context: Context,
    catalog: CatalogRepository,
    uri: Uri,
    kind: String = "import",
    onProgress: (phase: String, loaded: Long?, total: Long?) -> Unit = { _, _, _ -> },
): ImportOutcome {
    val name = displayName(context, uri)
    val size = querySize(context, uri)
    val bytes = try {
        readUriBytes(context, uri) { loaded, total ->
            onProgress("receiving", loaded, total ?: size)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }
    if (bytes == null || bytes.isEmpty()) return ImportOutcome.Failed(name, R.string.lib_gallery_reason_unreadable)
    val jpeg = if (isJpegBytes(bytes)) {
        bytes
    } else {
        onProgress("transcoding", null, null)
        try {
            ImagePipeline.decodeToJpeg(bytes)
        } catch (e: CancellationException) {
            throw e
        } catch (_: OutOfMemoryError) {
            return ImportOutcome.Failed(name, R.string.lib_gallery_reason_too_large)
        } catch (_: Exception) {
            return ImportOutcome.Failed(name, R.string.lib_gallery_reason_unsupported)
        }
    }
    return try {
        onProgress("ingesting", null, null)
        val photo = catalog.ingestBytes(jpeg, jpegName(name), kind)
        if (jpeg === bytes) ImportOutcome.Stored(photo.id) else ImportOutcome.Converted(name, photo.id)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        ImportOutcome.Failed(name, userErrorRes(e.message) ?: R.string.ds_error_generic)
    }
}

private fun querySize(context: Context, uri: Uri): Long? {
    return runCatching {
        context.contentResolver
            .query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getLong(0).takeIf { it > 0L } else null
            }
    }.getOrNull()
}

private fun readUriBytes(
    context: Context,
    uri: Uri,
    onBytes: (loaded: Long, total: Long?) -> Unit,
): ByteArray? {
    val total = querySize(context, uri)
    val input = context.contentResolver.openInputStream(uri) ?: return null
    return input.use { stream ->
        val out = java.io.ByteArrayOutputStream()
        val buf = ByteArray(64 * 1024)
        var loaded = 0L
        while (true) {
            val n = stream.read(buf)
            if (n < 0) break
            out.write(buf, 0, n)
            loaded += n
            onBytes(loaded, total)
        }
        out.toByteArray()
    }
}

private fun displayName(context: Context, uri: Uri): String {
    val provided = queryDisplayName(context, uri, OpenableColumns.DISPLAY_NAME)
        ?: queryDisplayName(context, uri, MediaStore.MediaColumns.DISPLAY_NAME)
    return fallbackImportName(provided, System.currentTimeMillis())
}

private fun queryDisplayName(context: Context, uri: Uri, column: String): String? {
    return runCatching {
        context.contentResolver
            .query(uri, arrayOf(column), null, null, null)
            ?.use { cursor ->
                val idx = cursor.getColumnIndex(column)
                if (idx >= 0 && cursor.moveToFirst()) {
                    cursor.getString(idx)?.trim()?.takeIf { it.isNotBlank() }
                } else {
                    null
                }
            }
    }.getOrNull()
}

/** The bytes are JPEG by this point, either originally or after transcoding, so only the
 * extension can still be wrong — `IMG_0001.HEIC` is stored as `IMG_0001.jpg`. */
private fun jpegName(name: String): String {
    if (isJpegName(name)) return name
    val base = name.substringBeforeLast('.', name).ifBlank { "import" }
    return "$base.jpg"
}

/**
 * Locale-aware and always distinguishable across years: time of day for today, an abbreviated
 * date within this year, and a date carrying the year for anything older.
 */
private fun photoTimeLabel(context: Context, iso: String): String {
    val instant = try {
        Instant.parse(iso)
    } catch (_: Exception) {
        return ""
    }
    val zone = ZoneId.systemDefault()
    val taken = instant.atZone(zone)
    val now = ZonedDateTime.now(zone)
    val flags = when {
        taken.toLocalDate() == now.toLocalDate() -> DateUtils.FORMAT_SHOW_TIME
        taken.year == now.year ->
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH or DateUtils.FORMAT_NO_YEAR
        else ->
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH or DateUtils.FORMAT_SHOW_YEAR
    }
    return DateUtils.formatDateTime(context, instant.toEpochMilli(), flags)
}

private fun PrintJob.toGallerySnapshot(running: PrintProgress?): PrintJobSnapshot {
    val live = running?.takeIf { it.photoId == photoId && jobFieldKey(state) == "running" }
    return PrintJobSnapshot(
        id = id,
        photoId = photoId,
        state = state,
        phase = live?.phase ?: phase,
        jobProgress = live?.progress,
        createdAt = createdAt,
        error = live?.error ?: error,
    )
}
