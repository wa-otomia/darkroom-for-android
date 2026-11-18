package io.github.wa_otomia.darkroom.ui.gallery

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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.wa_otomia.darkroom.R
import io.github.wa_otomia.darkroom.core.EditJobOverlay
import io.github.wa_otomia.darkroom.core.GalleryItem
import io.github.wa_otomia.darkroom.core.OverlayKind
import io.github.wa_otomia.darkroom.core.PendingKind
import io.github.wa_otomia.darkroom.core.PhotoMeta
import io.github.wa_otomia.darkroom.core.isGenerated
import io.github.wa_otomia.darkroom.core.PrintJobSnapshot
import io.github.wa_otomia.darkroom.core.fallbackImportName
import io.github.wa_otomia.darkroom.core.isJpegBytes
import io.github.wa_otomia.darkroom.core.isJpegName
import io.github.wa_otomia.darkroom.core.mergeGalleryItems
import io.github.wa_otomia.darkroom.data.automation.Automation
import io.github.wa_otomia.darkroom.data.catalog.CatalogRepository
import io.github.wa_otomia.darkroom.data.imaging.ImagePipeline
import io.github.wa_otomia.darkroom.data.jobs.AiJob
import io.github.wa_otomia.darkroom.data.printer.PrintJob
import io.github.wa_otomia.darkroom.data.printer.PrintProgress
import io.github.wa_otomia.darkroom.data.printer.PrintQueue
import io.github.wa_otomia.darkroom.data.progress.JobKind
import io.github.wa_otomia.darkroom.data.progress.JobProgressTracker
import io.github.wa_otomia.darkroom.data.progress.PrefsProgressStats
import io.github.wa_otomia.darkroom.data.progress.ProgressUi
import io.github.wa_otomia.darkroom.data.progress.SizeSource
import io.github.wa_otomia.darkroom.data.progress.batchOverall
import io.github.wa_otomia.darkroom.data.progress.failedProgressUi
import io.github.wa_otomia.darkroom.data.progress.indeterminateUi
import io.github.wa_otomia.darkroom.data.progress.ingestPhases
import io.github.wa_otomia.darkroom.data.progress.toUi
import io.github.wa_otomia.darkroom.data.transfer.IncomingTransfer
import io.github.wa_otomia.darkroom.ui.UserErrorDialog
import io.github.wa_otomia.darkroom.ui.components.AmberTrack
import io.github.wa_otomia.darkroom.ui.components.DarkroomCard
import io.github.wa_otomia.darkroom.ui.components.DarkroomSnackbarHost
import io.github.wa_otomia.darkroom.ui.components.DeleteUndoBar
import io.github.wa_otomia.darkroom.ui.components.IconCircleButton
import io.github.wa_otomia.darkroom.ui.components.JobOverlayBar
import io.github.wa_otomia.darkroom.ui.components.JobProgressStrip
import io.github.wa_otomia.darkroom.ui.components.JobScrim
import io.github.wa_otomia.darkroom.ui.components.PageTitle
import io.github.wa_otomia.darkroom.ui.components.StatusBadge
import io.github.wa_otomia.darkroom.ui.components.StatusChip
import io.github.wa_otomia.darkroom.ui.components.contentBlur
import io.github.wa_otomia.darkroom.ui.components.pressScale
import io.github.wa_otomia.darkroom.ui.navigation.LocalBottomBarClearance
import io.github.wa_otomia.darkroom.ui.jobFieldKey
import io.github.wa_otomia.darkroom.ui.localizedByteProgress
import io.github.wa_otomia.darkroom.ui.localizedGeneratePhase
import io.github.wa_otomia.darkroom.ui.localizedPrintPhase
import io.github.wa_otomia.darkroom.ui.localizedTransferPhase
import io.github.wa_otomia.darkroom.ui.theme.Amber
import io.github.wa_otomia.darkroom.ui.theme.Danger
import io.github.wa_otomia.darkroom.ui.theme.Ink
import io.github.wa_otomia.darkroom.ui.theme.MonoFont
import io.github.wa_otomia.darkroom.ui.theme.OnPhotoScrim
import io.github.wa_otomia.darkroom.ui.theme.PanelShape
import io.github.wa_otomia.darkroom.ui.theme.Paper
import io.github.wa_otomia.darkroom.ui.theme.PaperDim
import io.github.wa_otomia.darkroom.ui.theme.PaperPressed
import io.github.wa_otomia.darkroom.ui.theme.PhotoShape
import io.github.wa_otomia.darkroom.ui.theme.Room
import io.github.wa_otomia.darkroom.ui.theme.Surface1
import io.github.wa_otomia.darkroom.ui.theme.Surface3
import io.github.wa_otomia.darkroom.ui.userErrorRes
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

/** Both FABs, the extended one included, use 18dp corners (D4). */
private val FabShape = RoundedCornerShape(18.dp)

/** Scaffold FAB spacing (16dp) + FAB height (56dp) + a 16dp gap over the last row. */
private val FabRowClearance = 88.dp

/** The undo bar and delete progress as inset [PanelShape] panels, matching [SelectionBar]. */
private val TopStripModifier = Modifier
    .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 4.dp)
    .clip(PanelShape)

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
    focusPhotoId: String? = null,
    onFocusConsumed: () -> Unit = {},
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
    var highlightPhotoId by rememberSaveable { mutableStateOf<String?>(null) }

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

    val latestItems by rememberUpdatedState(items)
    LaunchedEffect(focusPhotoId, items) {
        val id = focusPhotoId ?: return@LaunchedEffect
        val index = latestItems.indexOfFirst { it.photoId == id }
        if (index < 0) return@LaunchedEffect
        highlightPhotoId = id
        seenTopId = latestPhotos.firstOrNull()?.id
        newArrivals = 0
        grid.animateScrollToItem(index + 1)
        onFocusConsumed()
    }

    // The undo prompt follows the repository rather than this screen's own state, because a
    // deletion started from the photo detail screen pops straight back here: the screen that has
    // to offer the undo is never the one that asked for the delete. The 15 s commit lives on
    // the repository timer — this strip offers undo, delete-now, and a countdown.
    val pendingDeletion by catalog.pendingDeletion.collectAsState()
    val deleteProgress by catalog.deleteProgress.collectAsState()

    fun deleteSelected() {
        val ids = selection.toList()
        if (ids.isEmpty()) return
        selection = emptySet()
        catalog.scheduleDelete(ids)
    }

    BackHandler(enabled = selectionMode) { selection = emptySet() }

    val clearance = LocalBottomBarClearance.current

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
                Column(Modifier.fillMaxWidth()) {
                    if (selectionMode) {
                        SelectionBar(
                            count = selection.size,
                            allSelected = photoItems.isNotEmpty() && selection.size == photoItems.size,
                            onExit = { selection = emptySet() },
                            onSelectAll = { selection = photoItems.mapTo(LinkedHashSet<String>()) { it.photoId } },
                            onDelete = { deleteSelected() },
                        )
                    }
                    // Inset panels like the selection bar above, so the stack reads as one family.
                    DeleteUndoBar(
                        pending = pendingDeletion,
                        onUndo = { catalog.undoDelete(it) },
                        onDeleteNow = { catalog.commitDeleteNow(it) },
                        modifier = TopStripModifier,
                    )
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
                            modifier = TopStripModifier,
                        )
                    }
                }
            },
            // With the FABs showing, the Scaffold already stacks the snackbar above them (and they
            // sit above the floating bar); without them it has to clear the bar itself.
            snackbarHost = {
                DarkroomSnackbarHost(
                    snackbar,
                    Modifier.padding(bottom = if (selectionMode) clearance else 0.dp),
                )
            },
            floatingActionButton = {
                if (!selectionMode) {
                    Row(
                        // The Scaffold adds its own 16dp above this, so the FABs float 16dp
                        // over the navigation pill.
                        modifier = Modifier.padding(bottom = clearance),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FloatingActionButton(
                            onClick = { if (!busy) launchCamera() },
                            shape = FabShape,
                            // Paper on Surface3 11.82:1.
                            containerColor = Surface3,
                            contentColor = Paper,
                        ) {
                            Icon(
                                Icons.Outlined.PhotoCamera,
                                contentDescription = stringResource(R.string.gallery_take_photo),
                            )
                        }
                        ExtendedFloatingActionButton(
                            text = { Text(if (busy) stringResource(R.string.importing) else stringResource(R.string.manual_import)) },
                            icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                            shape = FabShape,
                            // Ink on Paper 14.85:1.
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
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    // The bottom inset clears the floating navigation pill plus the FAB row above
                    // it, so the last row can scroll fully into view.
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 8.dp,
                        bottom = clearance + FabRowClearance,
                    ),
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
                                sourceThumb = item.sourcePhotoId?.let { sid ->
                                    photos.find { it.id == sid }?.let(catalog::galleryThumbFile)
                                        ?: catalog.thumbFile(sid)
                                },
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
                                    thumb = catalog.galleryThumbFile(item.meta),
                                    pending = null,
                                    sourceThumb = null,
                                    overlay = item.overlay,
                                    selectionMode = selectionMode,
                                    selected = selected,
                                    highlighted = item.photoId == highlightPhotoId,
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
                    // Usage hint, shown once there is something to tap. It sits under the grid so
                    // nothing but the title and status pills stands between the header and the
                    // photos.
                    if (items.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }, key = "gallery_footer_hint") {
                            Text(
                                stringResource(R.string.lib_gallery_long_press_hint),
                                color = PaperDim,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 12.dp),
                            )
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
                // JobScrim lifts its content by LocalBottomBarClearance, above the floating pill.
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
    Column(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
        PageTitle(
            title = stringResource(R.string.nav_gallery),
            supporting = if (empty) {
                stringResource(R.string.gallery_waiting)
            } else {
                pluralStringResource(R.plurals.gallery_count, count, count)
            },
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
        )
        // StatusChip draws a 36dp pill in a 48dp touch box, so the rows need no extra gap.
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
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
            // The setup and import notes only matter before the first photo; once there is
            // one, the grid follows the status pills directly (SYUI020-R05).
            DarkroomCard(
                modifier = Modifier.padding(top = 12.dp),
                contentPadding = 20.dp,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    stringResource(R.string.empty_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Paper,
                    modifier = Modifier.semantics { heading() },
                )
                // PaperDim on Surface1 5.16:1.
                Text(stringResource(R.string.empty_desc), style = MaterialTheme.typography.bodyMedium, color = PaperDim)
                Text(stringResource(R.string.gallery_desc), style = MaterialTheme.typography.bodyMedium, color = PaperDim)
                Text(
                    stringResource(R.string.lib_gallery_import_formats),
                    style = MaterialTheme.typography.bodySmall,
                    color = PaperDim,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun PhotoCard(
    photo: PhotoMeta?,
    thumb: File,
    pending: GalleryItem.Pending?,
    sourceThumb: File?,
    overlay: EditJobOverlay?,
    selectionMode: Boolean,
    selected: Boolean,
    highlighted: Boolean = false,
    modifier: Modifier,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onEnterSelection: () -> Unit,
    onDismissPending: () -> Unit,
    onCancelPending: () -> Unit,
    onCancelOverlay: () -> Unit,
) {
    val context = LocalContext.current
    val thumbStamp = thumb.lastModified()
    val request = remember(thumb.path, thumbStamp) {
        ImageRequest.Builder(context)
            .data(thumb)
            .memoryCacheKey("${thumb.path}:$thumbStamp")
            .diskCacheKey("${thumb.path}:$thumbStamp")
            .crossfade(220)
            .build()
    }
    var thumbState by remember(thumb) { mutableStateOf(ThumbState.LOADING) }
    val timeLabel = remember(photo?.ingestedAt, context) {
        photo?.ingestedAt?.let { photoTimeLabel(context, it) }.orEmpty()
    }
    val failedPending = pending != null && (!pending.error.isNullOrBlank() || pending.dismissId != null)
    val selectable = photo != null && pending == null
    val selectedText = stringResource(R.string.a11y_state_selected)
    val notSelectedText = stringResource(R.string.a11y_state_not_selected)
    val selectActionLabel = stringResource(R.string.gallery_a11y_select_photo)
    // Caption text. A failed placeholder without a file name shows no caption: its face and
    // footer already say it failed, and "Generating…" over a failure would contradict them.
    val captionName: String? = when {
        pending != null -> pending.filename
            ?: if (failedPending) {
                null
            } else {
                stringResource(if (pending.kind == PendingKind.Generate) R.string.generating else R.string.importing)
            }
        else -> photo?.filename.orEmpty()
    }
    // Mono only for a real file name; the "Generating…"/"Importing…" fallbacks are prose.
    val captionIsFileName = pending?.filename != null || pending == null
    val captionPrefix = if (photo?.parentId != null && pending == null) {
        stringResource(R.string.generated_prefix)
    } else {
        ""
    }
    // Height of the caption + footer stack, so a pending face keeps its centred text above it.
    val density = LocalDensity.current
    var bottomStackHeight by remember { mutableStateOf(0.dp) }

    // The tile is the 2:3 image frame itself (LBBR010-R01): no outline and no caption bar. Only a
    // selected or located photo gets a frame, 2dp Amber, drawn over the photo's edge.
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(2f / 3f)
            .clip(PhotoShape)
            .background(Surface1)
            .then(if (selected || highlighted) Modifier.border(2.dp, Amber, PhotoShape) else Modifier)
            .combinedClickable(
                onLongClickLabel = if (selectable && !selectionMode) selectActionLabel else null,
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
            )
            .then(
                if (selectionMode && selectable) {
                    Modifier.semantics { stateDescription = if (selected) selectedText else notSelectedText }
                } else {
                    Modifier
                },
            ),
    ) {
        Crossfade(
            targetState = pending != null,
            animationSpec = tween(220),
            label = "galleryPending",
        ) { isPending ->
            if (isPending && pending != null) {
                PendingFace(
                    pending = pending,
                    sourceThumb = sourceThumb,
                    bottomReserve = bottomStackHeight,
                    captionName = captionName,
                )
            } else if (photo != null) {
                AsyncImage(
                    model = request,
                    // Decorative within the tile: the caption below already carries the file name.
                    contentDescription = null,
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
                Box(Modifier.fillMaxSize().background(Surface1))
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
                // PaperDim on Room 88% over white ≥ 4.6:1.
                Text(
                    stringResource(R.string.lib_gallery_thumb_failed),
                    color = PaperDim,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
        if (photo != null && pending == null) {
            FlowRow(
                modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                StatusBadge(
                    visible = photo.edits.isNotEmpty(),
                    text = stringResource(R.string.badge_edited),
                )
                StatusBadge(
                    visible = photo.isGenerated(),
                    text = stringResource(R.string.photo_tag_generated),
                )
            }
        }
        if (selectionMode && selectable) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (selected) Amber else OnPhotoScrim)
                    .then(if (selected) Modifier else Modifier.border(2.dp, Paper, CircleShape)),
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
            IconCircleButton(
                icon = Icons.Outlined.Close,
                contentDescription = stringResource(R.string.common_cancel),
                onClick = onCancelOverlay,
                modifier = Modifier.align(Alignment.TopEnd).padding(2.dp),
                container = OnPhotoScrim,
                size = 36.dp,
            )
        }
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .onSizeChanged { bottomStackHeight = with(density) { it.height.toDp() } },
        ) {
            if (captionName != null) {
                PhotoCaption(
                    prefix = captionPrefix,
                    name = captionName,
                    nameIsFileName = captionIsFileName,
                    time = timeLabel,
                )
            }
            if (pending != null) {
                PendingFooter(
                    pending = pending,
                    modifier = Modifier.fillMaxWidth(),
                    onCancel = onCancelPending,
                )
            } else if (overlay != null) {
                OverlayBar(
                    overlay = overlay,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** Height of the transparent-to-Room-72% ramp at the top of [PhotoCaption]; text starts at 20dp. */
private val CaptionScrimRamp = 16.dp

/**
 * File name and time laid over the bottom of the photo. Only a real file name is mono
 * (a technical value); the prefix and status fallbacks stay sans. The scrim reaches Room 72% at
 * a fixed [CaptionScrimRamp], above where the text starts, so however tall the text grows at a
 * large font scale, Paper sits on at least Room 72% (≥ 6:1 even over a white photo).
 */
@Composable
private fun PhotoCaption(prefix: String, name: String, nameIsFileName: Boolean, time: String) {
    val text = remember(prefix, name, nameIsFileName) {
        buildAnnotatedString {
            append(prefix)
            if (nameIsFileName) {
                withStyle(SpanStyle(fontFamily = MonoFont)) { append(name) }
            } else {
                append(name)
            }
        }
    }
    Column(
        Modifier
            .fillMaxWidth()
            .drawWithCache {
                val stop = (CaptionScrimRamp.toPx() / size.height).coerceIn(0f, 1f)
                val scrim = Brush.verticalGradient(
                    0f to Color.Transparent,
                    stop to Room.copy(alpha = 0.72f),
                    1f to Room.copy(alpha = 0.88f),
                )
                onDrawBehind { drawRect(scrim) }
            }
            .padding(start = 10.dp, end = 10.dp, top = 20.dp, bottom = 8.dp),
    ) {
        Text(
            text,
            color = Paper,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (time.isNotEmpty()) {
            Text(
                time,
                color = Paper,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

/**
 * Face of a placeholder tile. [bottomReserve] is the height of the caption and footer drawn over
 * its bottom; the centred text keeps clear of it so a failure reason is never covered.
 * [captionName] is what the caption shows, so the centred name is not announced twice.
 */
@Composable
private fun PendingFace(
    pending: GalleryItem.Pending,
    sourceThumb: File?,
    bottomReserve: Dp,
    captionName: String?,
) {
    Box(Modifier.fillMaxSize().background(Surface1)) {
        if (pending.kind == PendingKind.Generate && sourceThumb != null) {
            AsyncImage(
                model = sourceThumb,
                // Decorative: the caption carries the name.
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (Build.VERSION.SDK_INT >= 31) Modifier.blur(18.dp) else Modifier),
            )
            if (Build.VERSION.SDK_INT >= 31) {
                Box(Modifier.matchParentSize().background(Room.copy(alpha = 0.32f)))
            }
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Surface1)
                    .padding(bottom = bottomReserve),
                contentAlignment = Alignment.Center,
            ) {
                val faceText = pending.filename ?: stringResource(R.string.importing)
                Text(
                    faceText,
                    color = PaperDim,
                    fontFamily = if (pending.filename != null) MonoFont else null,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(12.dp)
                        .then(if (faceText == captionName) Modifier.clearAndSetSemantics {} else Modifier),
                )
            }
        }
        if (!pending.error.isNullOrBlank()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .background(Surface1)
                    .padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = bottomReserve + 4.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    stringResource(R.string.transfer_phase_failed),
                    color = Amber,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                )
                Text(
                    pending.error,
                    color = PaperDim,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
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
    Column(modifier.background(Surface1)) {
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
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
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
    JobOverlayBar(phase = phase, ui = ui, modifier = modifier)
}

@Composable
private fun SelectionBar(
    count: Int,
    allSelected: Boolean,
    onExit: () -> Unit,
    onSelectAll: () -> Unit,
    onDelete: () -> Unit,
) {
    // A Surface1 panel set off from the page by fill alone; no divider underneath.
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 4.dp)
            .clip(PanelShape)
            .background(Surface1)
            .padding(horizontal = 4.dp, vertical = 4.dp),
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
            style = MaterialTheme.typography.titleMedium,
            color = Paper,
            maxLines = 2,
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
        // Danger on Surface1 10.67:1.
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Outlined.Delete,
                contentDescription = stringResource(R.string.common_delete),
                tint = Danger,
            )
        }
    }
}

@Composable
private fun NewArrivalsPill(count: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    // Ink on Paper 14.85:1, on PaperPressed 11.92:1. Fixed 24dp corners rather than a stadium so
    // a wrapped label at a large font scale never meets the curve.
    Row(
        modifier
            .heightIn(min = 48.dp)
            .shadow(6.dp, NewArrivalsShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .pressScale(interaction)
            .clip(NewArrivalsShape)
            .background(if (pressed) PaperPressed else Paper)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Outlined.ArrowUpward,
            contentDescription = null,
            tint = Ink,
            modifier = Modifier.size(18.dp),
        )
        Text(
            stringResource(R.string.lib_gallery_new_photos, count),
            color = Ink,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

private val NewArrivalsShape = RoundedCornerShape(24.dp)

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
                        ReportLine(name, fileName = name)
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
                        ReportLine(
                            stringResource(R.string.lib_gallery_import_failed_item, failure.name, reason),
                            fileName = failure.name,
                        )
                    }
                    MoreLine(report.failures.size - MAX_LISTED_FILES)
                }
                Text(
                    stringResource(R.string.lib_gallery_import_formats),
                    style = MaterialTheme.typography.bodySmall,
                    color = PaperDim,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_back), color = Paper) }
        },
    )
}

/** A report line; only [fileName] inside [text] is mono, the localized reason stays sans. */
@Composable
private fun ReportLine(text: String, fileName: String) {
    val styled = remember(text, fileName) {
        buildAnnotatedString {
            append(text)
            val start = if (fileName.isEmpty()) -1 else text.indexOf(fileName)
            if (start >= 0) addStyle(SpanStyle(fontFamily = MonoFont), start, start + fileName.length)
        }
    }
    Text(
        styled,
        color = PaperDim,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun MoreLine(extra: Int) {
    if (extra <= 0) return
    Text(
        stringResource(R.string.lib_gallery_import_more, extra),
        color = PaperDim,
        style = MaterialTheme.typography.bodySmall,
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
