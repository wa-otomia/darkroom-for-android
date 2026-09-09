package app.darkroom.android.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.darkroom.android.R
import app.darkroom.android.core.SNAP_POSITION_DP
import app.darkroom.android.core.WatermarkAnchor
import app.darkroom.android.core.WatermarkBox
import app.darkroom.android.core.WatermarkSettings
import app.darkroom.android.core.largestFrameSize
import app.darkroom.android.core.safeInset
import app.darkroom.android.core.watermarkLayout
import app.darkroom.android.core.watermarkSnapGuides
import app.darkroom.android.data.imaging.WatermarkRenderer
import app.darkroom.android.ui.components.SnapDragState
import app.darkroom.android.ui.components.drawSnapGuides
import app.darkroom.android.ui.theme.Amber
import app.darkroom.android.ui.theme.PaperFaint
import app.darkroom.android.ui.theme.SurfacePanel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.io.File
import kotlin.math.roundToInt

@Composable
fun WatermarkPreviewEditor(
    settings: WatermarkSettings,
    photoFile: File?,
    dateText: String,
    onAnchorChange: (id: String, anchor: WatermarkAnchor) -> Unit,
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
        val (frameW, frameH) = with(density) {
            largestFrameSize(maxWidth.toPx(), 400.dp.toPx())
        }
        if (frameW <= 0f || frameH <= 0f) return@BoxWithConstraints
        val preview = when (draggingId) {
            "sns" -> settings.copy(
                sns = settings.sns.copy(anchor = WatermarkAnchor(liveCx / frameW, liveCy / frameH)),
            )
            "date" -> settings.copy(
                date = settings.date.copy(anchor = WatermarkAnchor(liveCx / frameW, liveCy / frameH)),
            )
            else -> settings
        }
        val boxes = watermarkLayout(preview, frameW, frameH, measureText, dateText)
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
                    onLive = { id, x, y ->
                        draggingId = id
                        liveCx = x
                        liveCy = y
                    },
                    onCommit = { id, anchor ->
                        onAnchorChangeState.value(id, anchor)
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
    val logo = box.logo
    Row(
        Modifier
            .offset { IntOffset(box.left.roundToInt(), box.top.roundToInt()) }
            .height(with(density) { box.height.coerceAtLeast(1f).toDp() })
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
                        val cx = if (frameW > 0f) snapped.x / frameW else current.cx / frameW
                        val cy = if (frameH > 0f) snapped.y / frameH else current.cy / frameH
                        snapState.value.end()
                        onCommitState.value(current.id, WatermarkAnchor(cx, cy))
                    },
                    onDragCancel = {
                        snapState.value.end()
                        onCancelState.value()
                    },
                )
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (logo != null && box.logoSize > 0f) {
            Icon(
                painter = painterResource(WatermarkRenderer.snsLogoRes(logo)),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(with(density) { box.logoSize.toDp() }),
            )
        }
        if (logo != null && box.text.isNotEmpty()) {
            Spacer(
                Modifier.width(with(density) { (box.textLeft - box.left - box.logoSize).coerceAtLeast(0f).toDp() }),
            )
        }
        if (box.text.isNotEmpty()) {
            Text(
                text = box.text,
                color = Color.White,
                fontSize = with(density) { box.textSize.toSp() },
                fontFamily = FontFamily.SansSerif,
                maxLines = 1,
                softWrap = false,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.65f),
                        offset = Offset(0f, box.textSize * 0.04f),
                        blurRadius = box.textSize * 0.14f,
                    ),
                ),
            )
        }
    }
}
