package io.github.wa_otomia.darkroom.ui.studio

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as GeometrySize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale
import coil.size.Size
import io.github.wa_otomia.darkroom.R
import io.github.wa_otomia.darkroom.core.ID_CUT_LINE_COLOR
import io.github.wa_otomia.darkroom.core.ID_MASK_MSG_MISSING
import io.github.wa_otomia.darkroom.core.IdBackground
import io.github.wa_otomia.darkroom.core.IdFeather
import io.github.wa_otomia.darkroom.core.IdPhotoLayout
import io.github.wa_otomia.darkroom.core.IdPhotoPose
import io.github.wa_otomia.darkroom.core.MAX_PRINT_ZOOM
import io.github.wa_otomia.darkroom.core.MIN_PRINT_ZOOM
import io.github.wa_otomia.darkroom.core.PREVIEW_MAX_EDGE
import io.github.wa_otomia.darkroom.core.clampPan
import io.github.wa_otomia.darkroom.core.clamped
import io.github.wa_otomia.darkroom.core.coverScale
import io.github.wa_otomia.darkroom.core.idCellAnchor
import io.github.wa_otomia.darkroom.core.idFeatherSourcePx
import io.github.wa_otomia.darkroom.core.idSheetDeltaToCell
import io.github.wa_otomia.darkroom.core.largestFrameSize
import io.github.wa_otomia.darkroom.core.normalizeRotationDegrees
import io.github.wa_otomia.darkroom.core.panSnapGuides
import io.github.wa_otomia.darkroom.core.previewDecodeSize
import io.github.wa_otomia.darkroom.core.rotatePan
import io.github.wa_otomia.darkroom.core.safeInset
import io.github.wa_otomia.darkroom.data.catalog.CatalogRepository
import io.github.wa_otomia.darkroom.data.imaging.ImagePipeline
import io.github.wa_otomia.darkroom.data.progress.indeterminateUi
import io.github.wa_otomia.darkroom.ui.components.AmberTrack
import io.github.wa_otomia.darkroom.ui.components.FrameChipShape
import io.github.wa_otomia.darkroom.ui.components.SnapDragState
import io.github.wa_otomia.darkroom.ui.components.detectSnappingTransformGestures
import io.github.wa_otomia.darkroom.ui.components.drawSnapGuides
import io.github.wa_otomia.darkroom.ui.theme.Amber
import io.github.wa_otomia.darkroom.ui.theme.Ink
import io.github.wa_otomia.darkroom.ui.theme.Paper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs

/** Gaps and edges of the sheet are white on paper and in the preview (EDID020-R04). */
private val SheetWhite = Color.White
private val CutLine = Color(ID_CUT_LINE_COLOR)
private val SheetHairline = Paper.copy(alpha = 0.3f)

/** Zoom steps the preview cut-out is recomputed at; the feather radius depends on zoom. */
private const val CUTOUT_ZOOM_STEP = 0.1f
private const val CUTOUT_ZOOM_DEBOUNCE_MS = 200L

/**
 * The ID photo sheet (EDID020/EDID030): the white 3:2 landscape sheet fitted to the container,
 * grey cut lines, and every slot showing the same cell pose. Rotated slots hold the upright cell
 * turned 90° counter-clockwise, as the renderer does.
 *
 * The whole container is the gesture surface. The pinch/twist anchor is the matching point in
 * the cell under the centroid (or the nearest cell), finger deltas are mapped into the upright
 * cell's axes so the image follows the finger in every slot, and pan/rotation snap per cell
 * through [snapState]. [pose] pan is a fraction of the upright cell; it never holds screen px.
 *
 * [cutout] replaces the decoded source when the background is swapped: an orientation-corrected
 * composite of the same aspect. [cellBackground] fills blank cell area (zoom < 1, free angle).
 */
@Composable
internal fun IdPhotoSheetPreview(
    model: Any?,
    imageWidth: Int,
    imageHeight: Int,
    layout: IdPhotoLayout,
    pose: IdPhotoPose,
    onPoseChange: (IdPhotoPose) -> Unit,
    cellBackground: Color,
    cutout: ImageBitmap?,
    overlay: String,
    description: String,
    snapState: SnapDragState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    note: String? = null,
    compositing: Boolean = false,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    var container by remember { mutableStateOf(IntSize.Zero) }
    val sheetAspect = layout.sheetWidth.toFloat() / layout.sheetHeight.coerceAtLeast(1)
    val (frameW, frameH) = largestFrameSize(container.width.toFloat(), container.height.toFloat(), sheetAspect)
    // Screen px per layout px. Layout coordinates (print px) are what the core helpers speak.
    val scale = if (layout.sheetWidth > 0) frameW / layout.sheetWidth else 0f
    val cellW = layout.cellWidth * scale
    val cellH = layout.cellHeight * scale

    val poseState = rememberUpdatedState(pose)
    val onPoseState = rememberUpdatedState(onPoseChange)
    val layoutState = rememberUpdatedState(layout)
    val scaleState = rememberUpdatedState(scale)
    val frameState = rememberUpdatedState(frameW to frameH)
    val imageState = rememberUpdatedState(imageWidth to imageHeight)
    val snapHolder = rememberUpdatedState(snapState)
    val hapticHolder = rememberUpdatedState(haptic)

    // Out-of-range pan (smaller zoom, other size) is clamped in cell px and written back, the way
    // AspectCropper does. The tolerance keeps float round trips from echoing back and forth.
    LaunchedEffect(pose, layout.cellWidth, layout.cellHeight, imageWidth, imageHeight) {
        if (imageWidth <= 0 || imageHeight <= 0) return@LaunchedEffect
        val c = pose.clamped(layout.cellWidth, layout.cellHeight, imageWidth, imageHeight)
        if (!c.sameAs(pose)) onPoseState.value(c)
    }

    val preview = previewDecodeSize(imageWidth, imageHeight, PREVIEW_MAX_EDGE)
    var decoding by remember(model, preview.width, preview.height) { mutableStateOf(true) }
    val request = remember(model, preview.width, preview.height) {
        ImageRequest.Builder(context)
            .data(model)
            .size(Size(preview.width, preview.height))
            .precision(Precision.EXACT)
            .scale(Scale.FIT)
            // Same key as AspectCropper, so switching modes reuses the decoded preview.
            .memoryCacheKey("${model}:${preview.width}x${preview.height}")
            .crossfade(true)
            .build()
    }
    // One painter for every slot: Coil 2 does not merge identical in-flight requests, so a
    // per-slot AsyncImage decodes the source once per slot on a cold cache. Not created while
    // the cut-out stands in, so the source is not decoded for nothing, nor before the oriented
    // size is known (same gate as AspectCropper), so the request never uses the fallback box.
    val sourcePainter = if (cutout == null && imageWidth > 0 && imageHeight > 0) {
        rememberAsyncImagePainter(
            model = request,
            onState = { state ->
                decoding = state is AsyncImagePainter.State.Loading ||
                    state is AsyncImagePainter.State.Empty
            },
            contentScale = ContentScale.FillBounds,
        )
    } else {
        null
    }

    Box(
        modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { container = it }
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        if (frameW > 0f && frameH > 0f) {
            val frameWDp = with(density) { frameW.toDp() }
            val frameHDp = with(density) { frameH.toDp() }
            Box(Modifier.size(frameWDp, frameHDp).background(SheetWhite)) {
                // Sheet geometry is physical (print px), so it never mirrors in RTL.
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Box(Modifier.fillMaxSize()) {
                        Canvas(Modifier.fillMaxSize()) {
                            for (line in layout.cutLines) {
                                drawRect(
                                    CutLine,
                                    topLeft = Offset(line.left * scale, line.top * scale),
                                    size = GeometrySize(
                                        (line.width * scale).coerceAtLeast(1f),
                                        (line.height * scale).coerceAtLeast(1f),
                                    ),
                                )
                            }
                        }
                        if (imageWidth > 0 && imageHeight > 0 && cellW > 0f && cellH > 0f) {
                            val cover = coverScale(cellW, cellH, imageWidth, imageHeight)
                            val baseWDp = with(density) { (imageWidth * cover).toDp() }
                            val baseHDp = with(density) { (imageHeight * cover).toDp() }
                            val cellWDp = with(density) { cellW.toDp() }
                            val cellHDp = with(density) { cellH.toDp() }
                            for (slot in layout.slots) {
                                val left = slot.left * scale
                                val top = slot.top * scale
                                Box(
                                    Modifier
                                        .absoluteOffset { IntOffset(Math.round(left), Math.round(top)) }
                                        .size(
                                            with(density) { (slot.width * scale).toDp() },
                                            with(density) { (slot.height * scale).toDp() },
                                        )
                                        .clipToBounds()
                                        .background(cellBackground),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Box(
                                        Modifier
                                            .requiredSize(cellWDp, cellHDp)
                                            .graphicsLayer {
                                                if (layout.rotated) rotationZ = -90f
                                                clip = true
                                            }
                                            .background(cellBackground),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        val imageModifier = Modifier
                                            .requiredSize(baseWDp, baseHDp)
                                            .graphicsLayer {
                                                scaleX = pose.zoom
                                                scaleY = pose.zoom
                                                rotationZ = pose.rotation
                                                translationX = pose.panX * cellW
                                                translationY = pose.panY * cellH
                                            }
                                        if (cutout != null) {
                                            Image(
                                                bitmap = cutout,
                                                contentDescription = null,
                                                contentScale = ContentScale.FillBounds,
                                                modifier = imageModifier,
                                            )
                                        } else if (sourcePainter != null) {
                                            Image(
                                                painter = sourcePainter,
                                                contentDescription = null,
                                                contentScale = ContentScale.FillBounds,
                                                modifier = imageModifier,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        val guides = snapState.activeGuides
                        val guideStroke = with(density) { 1.dp.toPx() }
                        Canvas(Modifier.fillMaxSize()) {
                            if (guides.isEmpty() || cellW <= 0f || cellH <= 0f) return@Canvas
                            for (slot in layout.slots) {
                                val l = slot.left * scale
                                val t = slot.top * scale
                                val w = slot.width * scale
                                val h = slot.height * scale
                                val cx = l + w / 2f
                                val cy = t + h / 2f
                                clipRect(l, t, l + w, t + h) {
                                    rotate(if (layout.rotated) -90f else 0f, pivot = Offset(cx, cy)) {
                                        drawSnapGuides(
                                            guides = guides,
                                            frameLeft = cx - cellW / 2f,
                                            frameTop = cy - cellH / 2f,
                                            frameW = cellW,
                                            frameH = cellH,
                                            inset = safeInset(cellW, cellH),
                                            color = Amber,
                                            strokeWidth = guideStroke,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // The chips sit on the photo grid, not on the white sheet margin: the grid's own edges
            // (physical, like the sheet) plus the 10dp inset AspectCropper gives its frame chips.
            val gridLeftDp = with(density) { ((layout.slots.minOfOrNull { it.left } ?: 0) * scale).toDp() }
            val gridTopDp = with(density) { ((layout.slots.minOfOrNull { it.top } ?: 0) * scale).toDp() }
            val gridRightDp = with(density) {
                ((layout.sheetWidth - (layout.slots.maxOfOrNull { it.right } ?: layout.sheetWidth)) * scale).toDp()
            }
            val gridBottomDp = with(density) {
                ((layout.sheetHeight - (layout.slots.maxOfOrNull { it.bottom } ?: layout.sheetHeight)) * scale).toDp()
            }
            Box(
                Modifier
                    .size(frameWDp, frameHDp)
                    .border(1.dp, if (enabled) SheetHairline else Amber.copy(alpha = 0.7f)),
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .absolutePadding(left = gridLeftDp, top = gridTopDp, right = gridRightDp, bottom = gridBottomDp),
                ) {
                    Text(
                        overlay,
                        color = Paper.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                            .background(Ink.copy(alpha = 0.72f), FrameChipShape)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                    if (note != null) {
                        Text(
                            note,
                            color = Amber,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(10.dp)
                                .background(Ink.copy(alpha = 0.82f), FrameChipShape)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
                if (decoding && cutout == null) {
                    AmberTrack(
                        ui = indeterminateUi(""),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth(),
                        height = 3.dp,
                    )
                }
                if (compositing) {
                    // EDID040-R08: the controls are frozen until the new composite lands.
                    val busy = stringResource(R.string.studio_id_compositing)
                    CircularProgressIndicator(
                        color = Amber,
                        strokeWidth = 3.dp,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(40.dp)
                            .semantics { contentDescription = busy },
                    )
                }
            }
        }
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(model, enabled) {
                    if (!enabled) return@pointerInput
                    // pushed: the pose last sent or seen. live: its pan in on-screen cell px, the
                    // unit the anchor, snap and clamp math below works in.
                    var pushed = poseState.value
                    var pushedScale = scaleState.value
                    var pushedLayout = layoutState.value
                    fun livePan(p: IdPhotoPose, lay: IdPhotoLayout, s: Float) =
                        Offset(p.panX * lay.cellWidth * s, p.panY * lay.cellHeight * s)
                    var live = livePan(pushed, pushedLayout, pushedScale)
                    detectSnappingTransformGestures(
                        onBegin = {
                            pushed = poseState.value
                            pushedScale = scaleState.value
                            pushedLayout = layoutState.value
                            live = livePan(pushed, pushedLayout, pushedScale)
                            snapHolder.value.begin(live.x, live.y, pushed.rotation)
                        },
                        onEnd = { snapHolder.value.end() },
                    ) { centroid, pan, zoomChange, rotation ->
                        val lay = layoutState.value
                        val s = scaleState.value
                        val (iw, ih) = imageState.value
                        if (s <= 0f || lay.cellWidth <= 0 || lay.cellHeight <= 0) return@detectSnappingTransformGestures
                        val cw = lay.cellWidth * s
                        val ch = lay.cellHeight * s
                        // Re-read when the pose changed elsewhere (slider, rotate button, clamp,
                        // size change) or the sheet resized mid-gesture.
                        val current = poseState.value
                        if (current != pushed || s != pushedScale || lay != pushedLayout) {
                            pushed = current
                            pushedScale = s
                            pushedLayout = lay
                            live = livePan(current, lay, s)
                            snapHolder.value.sync(live.x, live.y, current.rotation)
                        }
                        val z = current.zoom
                        val nextZoom = (z * zoomChange).coerceIn(MIN_PRINT_ZOOM, MAX_PRINT_ZOOM)
                        val ratio = if (z > 0f) nextZoom / z else 1f
                        // Centroid in layout px of the sheet, then the anchor in that cell.
                        val (fw, fh) = frameState.value
                        val sheetLeft = (size.width - fw) / 2f
                        val sheetTop = (size.height - fh) / 2f
                        val (ax, ay) = idCellAnchor(lay, (centroid.x - sheetLeft) / s, (centroid.y - sheetTop) / s)
                        val fx = ax * s
                        val fy = ay * s
                        val zoomedX = fx - (fx - live.x) * ratio
                        val zoomedY = fy - (fy - live.y) * ratio
                        val (rx, ry) = rotatePan(zoomedX - fx, zoomedY - fy, rotation)
                        val rotatedX = rx + fx
                        val rotatedY = ry + fy
                        val nextRotation = normalizeRotationDegrees(current.rotation + rotation)
                        val (dx, dy) = idSheetDeltaToCell(pan.x, pan.y, lay.rotated)
                        val (guidesX, guidesY) = panSnapGuides(cw, ch, iw, ih, nextZoom, nextRotation, safeInset(cw, ch))
                        val snap = snapHolder.value
                        val snapped = snap.update(
                            pan = Offset(rotatedX + dx - snap.x, rotatedY + dy - snap.y),
                            zoomChange = zoomChange,
                            rotation = ((nextRotation - snap.rotation + 540f) % 360f) - 180f,
                            guidesX = guidesX,
                            guidesY = guidesY,
                        )
                        val clampRot = snapped.rotation
                        val (x, y) = if (iw > 0 && ih > 0) {
                            clampPan(snapped.x, snapped.y, nextZoom, cw, ch, iw, ih, clampRot)
                        } else {
                            snapped.x to snapped.y
                        }
                        if (x != snapped.x || y != snapped.y) snap.sync(x, y, clampRot)
                        if (snapped.snappedIn) {
                            hapticHolder.value.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        val next = IdPhotoPose(nextZoom, x / cw, y / ch, clampRot)
                        live = Offset(x, y)
                        pushed = next
                        onPoseState.value(next)
                    }
                },
        )
    }
}

private fun IdPhotoPose.sameAs(other: IdPhotoPose): Boolean =
    abs(zoom - other.zoom) < 1e-4f &&
        abs(panX - other.panX) < 1e-5f &&
        abs(panY - other.panY) < 1e-5f &&
        abs(rotation - other.rotation) < 1e-3f

/**
 * What [rememberIdPreviewCutout] has for the current inputs. [argb] is the background of [bitmap]
 * (it may still be the previous colour while [compositing]). [compositing] means a new cut-out is
 * being built and the ID controls are frozen (EDID040-R08).
 */
internal data class IdPreviewCutout(
    val bitmap: ImageBitmap?,
    val argb: Int?,
    val failed: Boolean,
    val compositing: Boolean,
)

/** Last render request of one preview; plain (not snapshot) state, read only by the effect. */
private class CutoutRequestMemo {
    var lastRequested: CutoutKey? = null
}

/**
 * One cut-out render at a time. The blocking decode and composite cannot be interrupted, and
 * withContext returns only after its block has finished, so holding this lock across it makes a
 * newer render wait for a cancelled one instead of running beside it.
 */
private val cutoutRenderLock = Mutex()

/**
 * The orientation-corrected background swap for the preview (D7), built off the main thread by
 * [ImagePipeline.renderIdPreviewCutout] and cached per (source, mask revision, colour, feather,
 * inward feather, zoom step). A zoom step change is debounced (also before the first cut-out is
 * ready) and renders run one at a time. The last finished cut-out stays on screen until the new
 * one lands, so a colour or feather change never flashes the source (EDID040-R08); such a change
 * reports [IdPreviewCutout.compositing] until then, a zoom-only change never does. A mask revision
 * found missing or undecodable is not retried until the revision changes, and stays reported
 * across zoom, feather and size changes.
 * Null bitmap when the background is the original, or while the first cut-out is built.
 * Nothing here is saved state: bitmaps never reach the saved-instance bundle.
 */
@Composable
internal fun rememberIdPreviewCutout(
    catalog: CatalogRepository,
    photoId: String,
    source: String,
    imageFile: File,
    maskRev: String?,
    background: IdBackground,
    feather: IdFeather,
    featherInward: Boolean,
    maskOffset: Int,
    zoom: Float,
    layout: IdPhotoLayout,
    imageWidth: Int,
    imageHeight: Int,
): IdPreviewCutout {
    val argb = background.argb
    val key = if (argb != null && !maskRev.isNullOrBlank()) {
        CutoutKey(
            file = imageFile.path,
            maskRev = maskRev,
            argb = argb,
            feather = feather.id,
            featherInward = featherInward,
            maskOffset = maskOffset,
            cellWidth = layout.cellWidth,
            cellHeight = layout.cellHeight,
            zoomStep = Math.round(zoom / CUTOUT_ZOOM_STEP),
        )
    } else {
        null
    }
    var held by remember(photoId, source) { mutableStateOf<Pair<CutoutKey, ImageBitmap>?>(null) }
    var failedKey by remember(photoId, source) { mutableStateOf<CutoutKey?>(null) }
    // Key whose transient retries are used up: it no longer holds the controls frozen.
    var gaveUpKey by remember(photoId, source) { mutableStateOf<CutoutKey?>(null) }
    val memo = remember(photoId, source) { CutoutRequestMemo() }
    LaunchedEffect(key, imageWidth, imageHeight) {
        if (key == null) {
            held = null
            // A give-up only covers the key itself and zoom/size-only steps right after it; coming
            // back from the original background composites afresh and freezes again (EDID040-R08).
            gaveUpKey = null
            return@LaunchedEffect
        }
        // The feather radius in source px needs the image size; wait for the probe.
        if (imageWidth <= 0 || imageHeight <= 0) return@LaunchedEffect
        val start = cutoutStart(key, held?.first, memo.lastRequested, failedKey)
        if (start == CutoutStart.Skip) return@LaunchedEffect
        memo.lastRequested = key
        // A zoom-only step after a give-up keeps it: the retry runs without freezing the controls.
        if (!gaveUpCovers(gaveUpKey, key)) gaveUpKey = null
        if (start == CutoutStart.Debounced) delay(CUTOUT_ZOOM_DEBOUNCE_MS)
        val featherSourcePx = idFeatherSourcePx(
            feather.radiusPx(1),
            key.cellWidth,
            key.cellHeight,
            imageWidth,
            imageHeight,
            key.zoomStep * CUTOUT_ZOOM_STEP,
        )
        // EDID040-R14: the print-px edge shift in source px, with its sign.
        val offsetSourcePx = if (key.maskOffset == 0) {
            0f
        } else {
            val magnitude = idFeatherSourcePx(
                Math.abs(key.maskOffset),
                key.cellWidth,
                key.cellHeight,
                imageWidth,
                imageHeight,
                key.zoomStep * CUTOUT_ZOOM_STEP,
            )
            if (key.maskOffset < 0) -magnitude else magnitude
        }
        var attempt = 0
        while (true) {
            try {
                val bitmap = cutoutRenderLock.withLock {
                    withContext(Dispatchers.IO) {
                        val mask = catalog.readIdPhotoMask(photoId, source, key.maskRev) ?: error(ID_MASK_MSG_MISSING)
                        ensureActive()
                        val bytes = imageFile.readBytes()
                        ensureActive()
                        ImagePipeline.renderIdPreviewCutout(
                            bytes,
                            mask,
                            key.argb,
                            featherSourcePx,
                            featherInward = key.featherInward,
                            offsetSourcePx = offsetSourcePx,
                        ).asImageBitmap()
                    }
                }
                held = key to bitmap
                failedKey = null
                return@LaunchedEffect
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                // Only a missing or undecodable mask is reported as such (it also blocks print and
                // export). A failed mask revision is remembered by (file, revision), so zoom, feather
                // and size changes neither hide the note nor retry.
                if (e.message == ID_MASK_MSG_MISSING) {
                    held = null
                    failedKey = key
                    return@LaunchedEffect
                }
                // A source decode failure, an I/O error or running out of memory is transient: no
                // note, the held cut-out stays (so the next zoom-only change is still debounced), and
                // the same key is rendered again after a short, bounded backoff, since nothing else
                // re-runs this effect while the inputs are unchanged.
                val wait = cutoutRetryDelayMs(attempt++)
                if (wait == null) {
                    gaveUpKey = key
                    return@LaunchedEffect
                }
                delay(wait)
            }
        }
    }
    val current = held
    // The held cut-out stays on screen while a newer one is computed (frozen unless only the
    // zoom step or cell size moved); another source file never borrows it.
    // Without a known source size no render can start, so the controls are never frozen for it.
    val view = cutoutView(current?.first, key, failedKey, gaveUpKey, sizeKnown = imageWidth > 0 && imageHeight > 0)
    val shown = current?.takeIf { view.showHeld }
    val failed = key != null && failedKey?.sameMask(key) == true
    return IdPreviewCutout(
        bitmap = shown?.second,
        argb = shown?.first?.argb,
        failed = failed,
        compositing = view.frozen,
    )
}
