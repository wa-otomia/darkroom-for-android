package io.github.wa_otomia.darkroom.ui.io

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.wa_otomia.darkroom.R
import io.github.wa_otomia.darkroom.core.PhotoMeta
import io.github.wa_otomia.darkroom.data.catalog.CatalogRepository
import io.github.wa_otomia.darkroom.data.printer.PrintJob
import io.github.wa_otomia.darkroom.data.printer.PrintProgress
import io.github.wa_otomia.darkroom.data.printer.PrintQueue
import io.github.wa_otomia.darkroom.data.progress.ProgressUi
import io.github.wa_otomia.darkroom.data.progress.indeterminateUi
import io.github.wa_otomia.darkroom.data.progress.toUi
import io.github.wa_otomia.darkroom.ui.components.AmberProgressBar
import io.github.wa_otomia.darkroom.ui.components.ProPrinterPanel
import io.github.wa_otomia.darkroom.ui.components.GhostButton
import io.github.wa_otomia.darkroom.ui.jobFieldKey
import io.github.wa_otomia.darkroom.ui.localizedPrintPhase
import io.github.wa_otomia.darkroom.ui.localizedUserError
import io.github.wa_otomia.darkroom.ui.theme.Amber
import io.github.wa_otomia.darkroom.ui.theme.MonoFont
import io.github.wa_otomia.darkroom.ui.theme.Paper
import io.github.wa_otomia.darkroom.ui.theme.PaperDim
import io.github.wa_otomia.darkroom.ui.theme.PaperHairline
import io.github.wa_otomia.darkroom.ui.theme.Room
import io.github.wa_otomia.darkroom.ui.theme.SurfaceLow
import coil.compose.AsyncImage
import coil.request.ImageRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintQueueScreen(
    catalog: CatalogRepository,
    printQueue: PrintQueue,
    photos: List<PhotoMeta>,
    onBack: () -> Unit,
) {
    val jobs by printQueue.printJobs.collectAsState(initial = emptyList())
    val runningProgress by printQueue.runningProgress.collectAsState(initial = null)
    val visible = remember(jobs) {
        jobs.filter { jobFieldKey(it.state) != "done" }
            .sortedWith(compareBy({ printSortKey(it) }, { it.createdAt }, { it.id }))
    }
    val queuedIds = remember(visible) {
        visible.filter { jobFieldKey(it.state) == "queued" }.map { it.id }
    }
    val canClear = visible.any { jobFieldKey(it.state) in FINISHED_STATES && it.phase != "outcome_unknown" }
    val byPhoto = remember(photos) { photos.associateBy { it.id } }

    Scaffold(
        containerColor = Room,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.print_queue_title), color = Paper) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                            tint = Paper,
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
        },
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ProPrinterPanel(printQueue, visible.firstOrNull { jobFieldKey(it.state) == "running" }?.id)
            }
            item {
                GhostButton(
                    stringResource(R.string.print_queue_clear_failed),
                    enabled = canClear,
                ) { printQueue.clearFinished() }
            }
            if (visible.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.print_queue_empty),
                        color = PaperDim,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                }
            }
            itemsIndexed(visible, key = { _, job -> job.id }) { _, job ->
                val state = jobFieldKey(job.state)
                val queueIndex = queuedIds.indexOf(job.id).let { if (it >= 0) it + 1 else 0 }
                val photo = byPhoto[job.photoId]
                PrintJobRow(
                    job = job,
                    state = state,
                    queueIndex = queueIndex,
                    filename = photo?.filename ?: job.photoId,
                    thumb = catalog.thumbFile(job.photoId),
                    progress = runningProgress?.takeIf { it.photoId == job.photoId && state == "running" },
                    onCancel = { printQueue.cancel(job.id) },
                    onRetry = { printQueue.retry(job.id) },
                    onRemove = { printQueue.remove(job.id) },
                )
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun PrintJobRow(
    job: PrintJob,
    state: String,
    queueIndex: Int,
    filename: String,
    thumb: java.io.File,
    progress: PrintProgress?,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onRemove: () -> Unit,
) {
    val context = LocalContext.current
    val origin = if (jobFieldKey(job.origin) == "auto") {
        stringResource(R.string.print_queue_origin_auto)
    } else {
        stringResource(R.string.print_queue_origin_manual)
    }
    val phase = progress?.phase ?: job.phase
    val jobState = progress?.jobState ?: job.jobState
    val status = when (state) {
        "queued" -> stringResource(R.string.print_queue_queued_fmt, queueIndex)
        "running" -> if (!phase.isNullOrBlank()) {
            localizedPrintPhase(phase, jobState)
        } else {
            stringResource(R.string.print_queue_printing)
        }
        "cancelled" -> stringResource(R.string.job_state_cancelled)
        "failed" -> stringResource(R.string.job_state_failed)
        else -> state
    }
    Column(
        Modifier
            .fillMaxWidth()
            .border(1.dp, PaperHairline, RoundedCornerShape(2.dp))
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .width(40.dp)
                    .height(60.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(SurfaceLow),
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(thumb).crossfade(true).build(),
                    contentDescription = filename,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    filename,
                    color = Paper,
                    fontFamily = MonoFont,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    origin,
                    color = PaperDim,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    status,
                    color = if (state == "failed") Amber else PaperDim,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        when (state) {
            "running" -> {
                val ui = progress?.progress?.toUi(status)
                    ?: ProgressUi(
                        percent = null,
                        fill = 0f,
                        phaseLabel = status,
                        indeterminate = true,
                    )
                Spacer(Modifier.height(10.dp))
                AmberProgressBar(
                    title = stringResource(R.string.progress_print),
                    ui = ui,
                )
                Spacer(Modifier.height(10.dp))
                GhostButton(stringResource(R.string.common_cancel), fillMaxWidth = false, onClick = onCancel)
            }
            "queued" -> {
                Spacer(Modifier.height(10.dp))
                AmberProgressBar(
                    title = stringResource(R.string.progress_print),
                    ui = indeterminateUi(status),
                )
                Spacer(Modifier.height(10.dp))
                GhostButton(stringResource(R.string.common_cancel), fillMaxWidth = false, onClick = onCancel)
            }
            "failed", "cancelled" -> {
                val error = job.error?.let { localizedUserError(it) }
                if (!error.isNullOrBlank()) {
                    Text(error, color = Amber, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }
                Row(
                    Modifier.padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GhostButton(stringResource(R.string.print_queue_retry), fillMaxWidth = false, enabled = job.phase != "outcome_unknown", onClick = onRetry)
                    GhostButton(stringResource(R.string.print_queue_remove), fillMaxWidth = false, enabled = job.phase != "outcome_unknown", onClick = onRemove)
                }
            }
        }
    }
}

private val FINISHED_STATES = setOf("failed", "cancelled")

private fun printSortKey(job: PrintJob): Int = when (jobFieldKey(job.state)) {
    "running" -> 0
    "queued" -> 1
    "failed" -> 2
    "cancelled" -> 3
    else -> 4
}
