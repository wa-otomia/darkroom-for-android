package io.github.wa_otomia.darkroom.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.wa_otomia.darkroom.R
import io.github.wa_otomia.darkroom.core.PRINT_ASPECT
import io.github.wa_otomia.darkroom.core.PRINT_ASPECT_LANDSCAPE
import io.github.wa_otomia.darkroom.core.SNAP_POSITION_DP
import io.github.wa_otomia.darkroom.core.WatermarkAnchor
import io.github.wa_otomia.darkroom.core.WatermarkBox
import io.github.wa_otomia.darkroom.core.WatermarkSettings
import io.github.wa_otomia.darkroom.core.clampedWatermarkAnchor
import io.github.wa_otomia.darkroom.core.largestFrameSize
import io.github.wa_otomia.darkroom.core.safeInset
import io.github.wa_otomia.darkroom.core.watermarkLayout
import io.github.wa_otomia.darkroom.core.watermarkSnapGuides
import io.github.wa_otomia.darkroom.core.withAnchor
import io.github.wa_otomia.darkroom.ui.components.LogoGlow
import io.github.wa_otomia.darkroom.ui.components.SnapDragState
import io.github.wa_otomia.darkroom.ui.components.drawSnapGuides
import io.github.wa_otomia.darkroom.ui.components.drawWatermarkMark
import io.github.wa_otomia.darkroom.ui.components.rememberLogoGlow
import io.github.wa_otomia.darkroom.ui.theme.Amber
import io.github.wa_otomia.darkroom.ui.theme.PaperFaint
import io.github.wa_otomia.darkroom.ui.theme.SurfacePanel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.io.File
import kotlin.math.roundToInt

/**
 * Draggable watermark preview on a paper frame. [landscape] shows the 3:2 sheet and edits the
 * landscape anchors; otherwise the 2:3 sheet and the portrait anchors. [onAnchorChange] receives
 * the orientation that was being edited so the caller writes the matching field.
 */
@Composable
fun WatermarkPreviewEditor(
    settings: WatermarkSettings,
    photoFile: File?,
    dateText: String,
    landscape: Boolean,
    onAnchorChange: (id: String, landscape: Boolean, anchor: WatermarkAnchor) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val textMeasurer = rememberTextMeasurer()
    val previewLabel = stringResource(R.string.settings_watermark_preview)
    val thresholdPx = with(density) { SNAP_POSITION_DP.dp.toPx() }
    val snsSnap = remember(thresholdPx) { SnapDragState(positionThresholdPx = thresholdPx) }
    val dateSnap = remember(thresholdPx) { SnapDragState(positionThresholdPx = thresholdPx) }
    var draggingId by remember { mutableStateOf<String?>(null) }
    var liveCx by remember { mutableFloatStateOf(0f) }
    var liveCy by remember { mutableFloatStateOf(0f) }
    val onAnchorChangeState = rememberUpdatedState(onAnchorChange)
    val hapticState = rememberUpdatedState(haptic)
    val measureText: (String, Float) -> Float = remember(textMeasurer, density) {
        { text, fontPx ->
            textMeasurer.measure(
                text = text,
                style = TextStyle(
                    fontSize = with(density) { fontPx.toSp() },
                    fontFamily = FontFamily.SansSerif,
                ),
            ).size.width.toFloat()
        }
    }

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val aspect = if (landscape) PRINT_ASPECT_LANDSCAPE.toFloat() else PRINT_ASPECT.toFloat()
        val (frameW, frameH) = with(density) {
            largestFrameSize(maxWidth.toPx(), 400.dp.toPx(), aspect)
        }
        if (frameW <= 0f || frameH <= 0f) return@BoxWithConstraints
        val liveAnchor = WatermarkAnchor(liveCx / frameW, liveCy / frameH)
        val preview = when (draggingId) {
            "sns" -> settings.copy(sns = settings.sns.withAnchor(landscape, liveAnchor))
            "date" -> settings.copy(date = settings.date.withAnchor(landscape, liveAnchor))
            else -> settings
        }
        val boxes = watermarkLayout(preview, frameW, frameH, measureText, dateText)
        val snsBox = boxes.firstOrNull { it.id == "sns" }
        val glow = rememberLogoGlow(snsBox?.logo, snsBox?.logoSize ?: 0f)
        val inset = safeInset(frameW, frameH)
        val guideStroke = with(density) { 1.dp.toPx() }
        val activeGuides = snsSnap.activeGuides + dateSnap.activeGuides
        val request = remember(photoFile) {
            photoFile?.let {
                ImageRequest.Builder(context)
                    .data(it)
                    .crossfade(true)
                    .build()
            }
        }

        Box(
            Modifier
                .size(with(density) { frameW.toDp() }, with(density) { frameH.toDp() })
                .align(Alignment.Center)
                .clipToBounds()
                .background(SurfacePanel)
                .border(1.dp, PaperFaint, RoundedCornerShape(2.dp))
                .semantics { contentDescription = previewLabel },
        ) {
            if (request != null) {
                AsyncImage(
                    model = request,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Canvas(Modifier.fillMaxSize()) {
                drawSnapGuides(
                    guides = activeGuides,
                    frameLeft = 0f,
                    frameTop = 0f,
                    frameW = frameW,
                    frameH = frameH,
                    inset = inset,
                    color = Amber,
                    strokeWidth = guideStroke,
                )
            }
            boxes.forEach { box ->
                val snap = if (box.id == "sns") snsSnap else dateSnap
                WatermarkPreviewMark(
                    box = box,
                    frameW = frameW,
                    frameH = frameH,
                    inset = inset,
                    snap = snap,
                    glow = if (box.id == "sns") glow else null,
                    measurer = textMeasurer,
                    onLive = { id, x, y ->
                        draggingId = id
                        liveCx = x
                        liveCy = y
                    },
                    onCommit = { id, anchor ->
                        onAnchorChangeState.value(id, landscape, anchor)
                        draggingId = null
                    },
                    onCancel = { draggingId = null },
                    onSnappedIn = {
                        hapticState.value.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                )
            }
        }
    }
}

@Composable
private fun WatermarkPreviewMark(
    box: WatermarkBox,
    frameW: Float,
    frameH: Float,
    inset: Float,
    snap: SnapDragState,
    glow: LogoGlow?,
    measurer: TextMeasurer,
    onLive: (id: String, x: Float, y: Float) -> Unit,
    onCommit: (id: String, anchor: WatermarkAnchor) -> Unit,
    onCancel: () -> Unit,
    onSnappedIn: () -> Unit,
) {
    val density = LocalDensity.current
    val boxState = rememberUpdatedState(box)
    val snapState = rememberUpdatedState(snap)
    val onLiveState = rememberUpdatedState(onLive)
    val onCommitState = rememberUpdatedState(onCommit)
    val onCancelState = rememberUpdatedState(onCancel)
    val onSnappedInState = rememberUpdatedState(onSnappedIn)
    // The box is the touch target; the mark itself (glow padding, descenders) is drawn
    // unclipped around it and clipped only by the frame.
    Box(
        Modifier
            .offset { IntOffset(box.left.roundToInt(), box.top.roundToInt()) }
            .size(
                with(density) { box.width.coerceAtLeast(1f).toDp() },
                with(density) { box.height.coerceAtLeast(1f).toDp() },
            )
            .drawBehind {
                drawWatermarkMark(
                    box = box,
                    frameLeft = -box.left,
                    frameTop = -box.top,
                    glow = glow,
                    measurer = measurer,
                    density = density,
                )
            }
            .pointerInput(box.id, frameW, frameH, inset) {
                detectDragGestures(
                    onDragStart = {
                        val current = boxState.value
                        snapState.value.begin(current.cx, current.cy)
                        onLiveState.value(current.id, current.cx, current.cy)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val current = boxState.value
                        val (guidesX, guidesY) = watermarkSnapGuides(current, frameW, frameH, inset)
                        val result = snapState.value.update(
                            pan = dragAmount,
                            zoomChange = 1f,
                            rotation = 0f,
                            guidesX = guidesX,
                            guidesY = guidesY,
                        )
                        onLiveState.value(current.id, result.x, result.y)
                        if (result.snappedIn) onSnappedInState.value()
                    },
                    onDragEnd = {
                        val current = boxState.value
                        val snapped = snapState.value
                        val anchor = clampedWatermarkAnchor(
                            centerX = snapped.x,
                            centerY = snapped.y,
                            width = current.width,
                            height = current.height,
                            frameW = frameW,
                            frameH = frameH,
                        )
                        snapState.value.end()
                        onCommitState.value(current.id, anchor)
                    },
                    onDragCancel = {
                        snapState.value.end()
                        onCancelState.value()
                    },
                )
            },
    )
}
