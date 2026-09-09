package app.darkroom.android.ui.components

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
import app.darkroom.android.core.MAX_PRINT_ZOOM
import app.darkroom.android.core.MIN_PRINT_ZOOM
import app.darkroom.android.core.PRINT_ASPECT
import app.darkroom.android.core.PixelCrop
import app.darkroom.android.core.SNAP_POSITION_DP
import app.darkroom.android.core.SNAP_ROTATION_DEG
import app.darkroom.android.core.ViewportPlacement
import app.darkroom.android.core.clampPan
import app.darkroom.android.core.coverScale
import app.darkroom.android.core.largestFrameSize
import app.darkroom.android.core.normalizeQuarterTurns
import app.darkroom.android.core.normalizeRotationDegrees
import app.darkroom.android.core.panSnapGuides
import app.darkroom.android.core.rotatePan
import app.darkroom.android.core.safeInset
import app.darkroom.android.core.viewportCrop
import app.darkroom.android.core.viewportPlacement
import app.darkroom.android.data.progress.indeterminateUi
import app.darkroom.android.ui.theme.Amber
import app.darkroom.android.ui.theme.Ink
import app.darkroom.android.ui.theme.Paper
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
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
 * [imageWidth] and [imageHeight] are the dimensions the image has *after* [quarterTurns]
 * when [onRotationChange] is null (legacy Studio). Once [onRotationChange] is wired they
 * are the unturned source, and [rotationDegrees] is the live angle.
 *
 * The crop reported by [onCrop] stays in that same space. [onPlacement] is the full
 * zoom + pan + rotation the printer should apply.
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
    onCrop: (PixelCrop) -> Unit,
    overlay: String,
    modifier: Modifier = Modifier,
    aspect: Float = PRINT_ASPECT.toFloat(),
    quarterTurns: Int = 0,
    rotationDegrees: Float = quarterTurns * 90f,
    onRotationChange: ((Float) -> Unit)? = null,
    onPlacement: (ViewportPlacement) -> Unit = {},
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

    val zoomState = rememberUpdatedState(zoom)
    val panState = rememberUpdatedState(Offset(panX, panY))
    val rotationState = rememberUpdatedState(rotationDegrees)
    val onZoomState = rememberUpdatedState(onZoomChange)
    val onPanState = rememberUpdatedState(onPanChange)
    val onRotationState = rememberUpdatedState(onRotationChange)
    val frameState = rememberUpdatedState(frameW to frameH)
    val imageState = rememberUpdatedState(imageWidth to imageHeight)
    val clampRotationState = rememberUpdatedState(clampRotation)
    val snapHolder = rememberUpdatedState(snap)
    val hapticHolder = rememberUpdatedState(haptic)
    var decoding by remember(model) { mutableStateOf(true) }
    val request = remember(model) {
        ImageRequest.Builder(context)
            .data(model)
            .size(Size.ORIGINAL)
            .crossfade(true)
            .build()
    }

    LaunchedEffect(zoom, panX, panY, frameW, frameH, imageWidth, imageHeight, model, clampRotation) {
        if (frameW <= 0f || frameH <= 0f || imageWidth <= 0 || imageHeight <= 0) return@LaunchedEffect
        val (x, y) = clampPan(panX, panY, zoom, frameW, frameH, imageWidth, imageHeight, clampRotation)
        if (x != panX || y != panY) {
            onPanChange(x, y)
            return@LaunchedEffect
        }
        val placement = viewportPlacement(
            frameW,
            frameH,
            imageWidth,
            imageHeight,
            zoom,
            x,
            y,
            clampRotation,
        )
        onPlacement(placement)
        onCrop(placement.toPixelCrop() ?: viewportCrop(frameW, frameH, imageWidth, imageHeight, zoom, x, y))
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
                            translationX = panX
                            translationY = panY
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
                    var pushed = panState.value
                    var live = pushed
                    detectSnappingTransformGestures(
                        onBegin = {
                            live = panState.value
                            pushed = live
                            val rot = if (onRotationState.value != null) rotationState.value else 0f
                            snapHolder.value.begin(live.x, live.y, rot)
                        },
                        onEnd = { snapHolder.value.end() },
                    ) { centroid, pan, zoomChange, rotation ->
                        if (panState.value != pushed) {
                            live = panState.value
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
                        live = Offset(x, y)
                        pushed = live
                        onZoomState.value(nextZoom)
                        onPanState.value(x, y)
                        onRotationState.value?.invoke(clampRot)
                    }
                },
        )
    }
}
