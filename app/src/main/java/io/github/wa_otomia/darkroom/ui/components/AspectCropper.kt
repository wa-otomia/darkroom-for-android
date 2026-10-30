package io.github.wa_otomia.darkroom.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.wa_otomia.darkroom.core.MAX_PRINT_ZOOM
import io.github.wa_otomia.darkroom.core.MIN_PRINT_ZOOM
import io.github.wa_otomia.darkroom.core.PREVIEW_MAX_EDGE
import io.github.wa_otomia.darkroom.core.PRINT_ASPECT
import io.github.wa_otomia.darkroom.core.SNAP_POSITION_DP
import io.github.wa_otomia.darkroom.core.SNAP_ROTATION_DEG
import io.github.wa_otomia.darkroom.core.clampPan
import io.github.wa_otomia.darkroom.core.coverScale
import io.github.wa_otomia.darkroom.core.largestFrameSize
import io.github.wa_otomia.darkroom.core.normalizeQuarterTurns
import io.github.wa_otomia.darkroom.core.normalizeRotationDegrees
import io.github.wa_otomia.darkroom.core.panSnapGuides
import io.github.wa_otomia.darkroom.core.panUnitScale
import io.github.wa_otomia.darkroom.core.previewDecodeSize
import io.github.wa_otomia.darkroom.core.rotatePan
import io.github.wa_otomia.darkroom.core.safeInset
import io.github.wa_otomia.darkroom.data.progress.indeterminateUi
import io.github.wa_otomia.darkroom.ui.theme.Amber
import io.github.wa_otomia.darkroom.ui.theme.DarkroomMotion
import io.github.wa_otomia.darkroom.ui.theme.Ink
import io.github.wa_otomia.darkroom.ui.theme.Paper
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale
import coil.size.Size

/**
 * The crop frame outline, deliberately lighter than the shared `PaperFaint`.
 *
 * `PaperFaint` sits at 40% so a control's outline clears 3:1 against the Room
 * background, where the outline is the only thing marking the control. This
 * line is not that: it lies on top of the photograph, and the frame edge is
 * already carried by the 55% scrim outside it, so the hairline only has to
 * sharpen a boundary that reads on its own. At 40% the cream line competes with
 * the image for attention, which is the one thing a framing guide must not do.
 */
private val FrameHairline = Paper.copy(alpha = 0.3f)

/**
 * Rule-of-thirds grid shown while the image is being dragged. A cream line alone disappears on
 * a bright, airy photo and a dark one on a night shot, so each line is a faint dark stroke under
 * a lighter cream one; together they read on any image without competing with the amber snap
 * guides drawn on top.
 */
private val GridLine = Paper.copy(alpha = 0.55f)
private val GridShadow = Color.Black.copy(alpha = 0.28f)

/**
 * [imageWidth] and [imageHeight] are the dimensions the image has *after* [quarterTurns]
 * when [onRotationChange] is null (legacy Studio). Once [onRotationChange] is wired they
 * are the unturned source, and [rotationDegrees] is the live angle.
 *
 * [panX]/[panY] are measured in a frame of [panFrameWidth]×[panFrameHeight], which must have
 * the same [aspect] (Studio passes the print sheet). The on-screen frame changes size whenever
 * the layout around the cropper does (drawer, progress strips, rotation), so pan is converted to
 * screen pixels only to draw and to apply a gesture, and the same pose looks the same at every
 * size. Left at 0, pan is plain on-screen frame pixels. Out-of-range pan is clamped in pan units
 * and written back through [onPanChange]; the caller derives the placement from the same values.
 *
 * Pan and rotation go through [SnapDragState] (8 dp / 3°) before [clampPan]. Pass
 * [snapState] to share the same instance with a reset control; otherwise one is remembered here.
 */
@Composable
fun AspectCropper(
    model: Any?,
    imageWidth: Int,
    imageHeight: Int,
    zoom: Float,
    panX: Float,
    panY: Float,
    onZoomChange: (Float) -> Unit,
    onPanChange: (Float, Float) -> Unit,
    overlay: String,
    modifier: Modifier = Modifier,
    aspect: Float = PRINT_ASPECT.toFloat(),
    quarterTurns: Int = 0,
    rotationDegrees: Float = quarterTurns * 90f,
    onRotationChange: ((Float) -> Unit)? = null,
    panFrameWidth: Float = 0f,
    panFrameHeight: Float = 0f,
    enabled: Boolean = true,
    note: String? = null,
    snapState: SnapDragState? = null,
) {
    val context = LocalContext.current
    var container by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val positionThresholdPx = with(density) { SNAP_POSITION_DP.dp.toPx() }
    val localSnap = remember(positionThresholdPx) { SnapDragState(positionThresholdPx, SNAP_ROTATION_DEG) }
    val snap = snapState ?: localSnap
    val (frameW, frameH) = largestFrameSize(container.width.toFloat(), container.height.toFloat(), aspect)
    val legacyTurns = if (onRotationChange == null) normalizeQuarterTurns(quarterTurns) else 0
    val rawWidth = if (legacyTurns % 2 == 1) imageHeight else imageWidth
    val rawHeight = if (legacyTurns % 2 == 1) imageWidth else imageHeight
    val clampRotation = if (onRotationChange == null) 0f else rotationDegrees
    val ownPanFrame = panFrameWidth > 0f && panFrameHeight > 0f
    val spaceW = if (ownPanFrame) panFrameWidth else frameW
    val spaceH = if (ownPanFrame) panFrameHeight else frameH
    // Screen px per pan unit, from the width only: the two frames share the aspect, and one
    // factor keeps x and y in the same unit so rotating a pan stays a rotation.
    val pxPerUnit = panUnitScale(frameW, spaceW)

    val zoomState = rememberUpdatedState(zoom)
    val panState = rememberUpdatedState(Offset(panX, panY))
    val rotationState = rememberUpdatedState(rotationDegrees)
    val onZoomState = rememberUpdatedState(onZoomChange)
    val onPanState = rememberUpdatedState(onPanChange)
    val onRotationState = rememberUpdatedState(onRotationChange)
    val frameState = rememberUpdatedState(frameW to frameH)
    val spaceState = rememberUpdatedState(spaceW to spaceH)
    val pxPerUnitState = rememberUpdatedState(pxPerUnit)
    val imageState = rememberUpdatedState(imageWidth to imageHeight)
    val clampRotationState = rememberUpdatedState(clampRotation)
    val snapHolder = rememberUpdatedState(snap)
    val hapticHolder = rememberUpdatedState(haptic)
    // True from the first movement past touch slop until the fingers lift; a tap never shows it.
    var dragging by remember { mutableStateOf(false) }
    val gridAlpha = remember { Animatable(0f) }
    LaunchedEffect(dragging) {
        if (dragging) {
            gridAlpha.animateTo(1f, DarkroomMotion.enter())
        } else if (gridAlpha.value > 0f) {
            // A new drag inside the hold or the fade restarts this effect and cancels the fade.
            delay(DarkroomMotion.GuideHoldMillis)
            gridAlpha.animateTo(0f, DarkroomMotion.valueChange())
        }
    }
    // rawWidth/rawHeight are EXIF-corrected and not yet quarter-turned: Coil's decode space.
    // Size.ORIGINAL decodes the full raster; a portrait A7CR software bitmap exceeds 100 MiB.
    val preview = previewDecodeSize(rawWidth, rawHeight, PREVIEW_MAX_EDGE)
    var decoding by remember(model, preview.width, preview.height) { mutableStateOf(true) }
    val request = remember(model, preview.width, preview.height) {
        ImageRequest.Builder(context)
            .data(model)
            .size(Size(preview.width, preview.height))
            .precision(Precision.EXACT)
            .scale(Scale.FIT)
            .memoryCacheKey("${model}:${preview.width}x${preview.height}")
            .crossfade(true)
            .build()
    }

    // Clamp in pan units against the pan frame. clampPan scales linearly with the frame, so this
    // is the same bound at any on-screen size: resizing the preview never moves or trims the pan,
    // and an in-range value comes back unchanged (no write-back loop).
    LaunchedEffect(zoom, panX, panY, spaceW, spaceH, imageWidth, imageHeight, model, clampRotation) {
        if (spaceW <= 0f || spaceH <= 0f || imageWidth <= 0 || imageHeight <= 0) return@LaunchedEffect
        val (x, y) = clampPan(panX, panY, zoom, spaceW, spaceH, imageWidth, imageHeight, clampRotation)
        if (x != panX || y != panY) onPanState.value(x, y)
    }

    Box(
        modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { container = it },
        contentAlignment = Alignment.Center,
    ) {
        if (frameW > 0f && frameH > 0f && imageWidth > 0 && imageHeight > 0) {
            val cover = coverScale(frameW, frameH, imageWidth, imageHeight)
            val baseW = rawWidth * cover
            val baseH = rawHeight * cover
            Box(
                Modifier
                    .size(
                        width = with(density) { frameW.toDp() },
                        height = with(density) { frameH.toDp() },
                    )
                    .clipToBounds()
                    .background(Paper),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = request,
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    onState = { state ->
                        decoding = state is AsyncImagePainter.State.Loading ||
                            state is AsyncImagePainter.State.Empty
                    },
                    modifier = Modifier
                        .requiredSize(
                            width = with(density) { baseW.toDp() },
                            height = with(density) { baseH.toDp() },
                        )
                        .graphicsLayer {
                            scaleX = zoom
                            scaleY = zoom
                            rotationZ = rotationDegrees
                            translationX = panX * pxPerUnit
                            translationY = panY * pxPerUnit
                        },
                )
            }
            val guides = snap.activeGuides
            val inset = safeInset(frameW, frameH)
            val guideStroke = with(density) { 1.dp.toPx() }
            Canvas(Modifier.fillMaxSize()) {
                val left = (size.width - frameW) / 2f
                val top = (size.height - frameH) / 2f
                val hole = Path().apply {
                    fillType = PathFillType.EvenOdd
                    addRect(Rect(0f, 0f, size.width, size.height))
                    addRect(Rect(left, top, left + frameW, top + frameH))
                }
                drawPath(hole, Color.Black.copy(alpha = 0.55f))
                val grid = gridAlpha.value
                if (grid > 0f) {
                    drawThirdsGrid(left, top, frameW, frameH, grid, guideStroke)
                }
                drawSnapGuides(
                    guides = guides,
                    frameLeft = left,
                    frameTop = top,
                    frameW = frameW,
                    frameH = frameH,
                    inset = inset,
                    color = Amber,
                    strokeWidth = guideStroke,
                )
            }
            Box(
                Modifier
                    .size(
                        width = with(density) { frameW.toDp() },
                        height = with(density) { frameH.toDp() },
                    )
                    .border(1.dp, if (enabled) FrameHairline else Amber.copy(alpha = 0.7f)),
            ) {
                Text(
                    overlay,
                    color = Paper.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    letterSpacing = 1.4.sp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .background(Ink.copy(alpha = 0.72f), RoundedCornerShape(2.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
                if (decoding) {
                    AmberTrack(
                        ui = indeterminateUi(""),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth(),
                        height = 3.dp,
                    )
                }
                if (note != null) {
                    Text(
                        note,
                        color = Amber,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        letterSpacing = 1.4.sp,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp)
                            .background(Ink.copy(alpha = 0.82f), RoundedCornerShape(2.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(model, enabled) {
                    if (!enabled) return@pointerInput
                    // pushed: pan units last sent or seen. live: the same pose in screen px, which
                    // is what the centroid, snap and clamp math below works in.
                    var pushed = panState.value
                    var pushedScale = pxPerUnitState.value
                    var live = pushed * pushedScale
                    detectSnappingTransformGestures(
                        onBegin = {
                            pushed = panState.value
                            pushedScale = pxPerUnitState.value
                            live = pushed * pushedScale
                            val rot = if (onRotationState.value != null) rotationState.value else 0f
                            snapHolder.value.begin(live.x, live.y, rot)
                        },
                        onEnd = {
                            dragging = false
                            snapHolder.value.end()
                        },
                    ) { centroid, pan, zoomChange, rotation ->
                        dragging = true
                        // Re-read when the pan was changed elsewhere (clamp, rotate button) or the
                        // frame resized mid-gesture (drawer settling), so px and units agree.
                        val unit = pxPerUnitState.value
                        if (panState.value != pushed || unit != pushedScale) {
                            pushed = panState.value
                            pushedScale = unit
                            live = pushed * unit
                            val currentRotation =
                                if (onRotationState.value != null) rotationState.value else 0f
                            snapHolder.value.sync(live.x, live.y, currentRotation)
                        }
                        val (fw, fh) = frameState.value
                        val (iw, ih) = imageState.value
                        val z = zoomState.value
                        val nextZoom = (z * zoomChange).coerceIn(MIN_PRINT_ZOOM, MAX_PRINT_ZOOM)
                        val ratio = if (z > 0f) nextZoom / z else 1f
                        val fx = centroid.x - size.width / 2f
                        val fy = centroid.y - size.height / 2f
                        val zoomedX = fx - (fx - live.x) * ratio
                        val zoomedY = fy - (fy - live.y) * ratio
                        val rotateBy = if (onRotationState.value != null) rotation else 0f
                        val (rx, ry) = rotatePan(zoomedX - fx, zoomedY - fy, rotateBy)
                        val rotatedX = rx + fx
                        val rotatedY = ry + fy
                        val nextRotation = if (onRotationState.value != null) {
                            normalizeRotationDegrees(rotationState.value + rotation)
                        } else {
                            rotationState.value
                        }
                        val snapRot = if (onRotationState.value != null) nextRotation else clampRotationState.value
                        val inset = safeInset(fw, fh)
                        val (guidesX, guidesY) = panSnapGuides(fw, fh, iw, ih, nextZoom, snapRot, inset)
                        val snap = snapHolder.value
                        val snapped = snap.update(
                            pan = Offset(rotatedX + pan.x - snap.x, rotatedY + pan.y - snap.y),
                            zoomChange = zoomChange,
                            rotation = if (onRotationState.value != null) {
                                snap.rotation.let { current ->
                                    val delta = nextRotation - current
                                    val wrapped = ((delta + 540f) % 360f) - 180f
                                    wrapped
                                }
                            } else {
                                0f
                            },
                            guidesX = guidesX,
                            guidesY = guidesY,
                        )
                        val clampRot = if (onRotationState.value != null) snapped.rotation else clampRotationState.value
                        val (x, y) = clampPan(
                            snapped.x,
                            snapped.y,
                            nextZoom,
                            fw,
                            fh,
                            iw,
                            ih,
                            clampRot,
                        )
                        if (x != snapped.x || y != snapped.y || clampRot != snapped.rotation) {
                            snap.sync(x, y, clampRot)
                        }
                        if (snapped.snappedIn) {
                            hapticHolder.value.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        // Back to pan units, clamped there too so the clamp effect sees an
                        // in-range value and does not nudge it by a float ulp.
                        val (sw, sh) = spaceState.value
                        val (ux, uy) = if (sw > 0f && sh > 0f) {
                            clampPan(x / unit, y / unit, nextZoom, sw, sh, iw, ih, clampRot)
                        } else {
                            x to y
                        }
                        live = Offset(x, y)
                        pushed = Offset(ux, uy)
                        onZoomState.value(nextZoom)
                        onPanState.value(ux, uy)
                        onRotationState.value?.invoke(clampRot)
                    }
                },
        )
    }
}

/** Two vertical and two horizontal lines at the thirds of the frame, faded by [alpha]. */
private fun DrawScope.drawThirdsGrid(
    left: Float,
    top: Float,
    frameW: Float,
    frameH: Float,
    alpha: Float,
    strokeWidth: Float,
) {
    if (frameW <= 0f || frameH <= 0f) return
    val shadow = GridShadow.copy(alpha = GridShadow.alpha * alpha)
    val line = GridLine.copy(alpha = GridLine.alpha * alpha)
    for (i in 1..2) {
        val x = left + frameW * i / 3f
        val y = top + frameH * i / 3f
        drawLine(shadow, Offset(x, top), Offset(x, top + frameH), strokeWidth * 2f)
        drawLine(shadow, Offset(left, y), Offset(left + frameW, y), strokeWidth * 2f)
        drawLine(line, Offset(x, top), Offset(x, top + frameH), strokeWidth)
        drawLine(line, Offset(left, y), Offset(left + frameW, y), strokeWidth)
    }
}
