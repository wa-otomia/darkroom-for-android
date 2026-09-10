package io.github.wa_otomia.darkroom.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import io.github.wa_otomia.darkroom.core.GUIDE_CENTER_X
import io.github.wa_otomia.darkroom.core.GUIDE_FRAME_X_NEG
import io.github.wa_otomia.darkroom.core.GUIDE_FRAME_X_POS
import io.github.wa_otomia.darkroom.core.GUIDE_FRAME_Y_NEG
import io.github.wa_otomia.darkroom.core.GUIDE_FRAME_Y_POS
import io.github.wa_otomia.darkroom.core.GUIDE_SAFE_X_NEG
import io.github.wa_otomia.darkroom.core.GUIDE_SAFE_X_POS
import io.github.wa_otomia.darkroom.core.GUIDE_SAFE_Y_NEG
import io.github.wa_otomia.darkroom.core.GUIDE_SAFE_Y_POS
import io.github.wa_otomia.darkroom.core.SNAP_ROTATION_DEG
import io.github.wa_otomia.darkroom.core.SnapAxis
import io.github.wa_otomia.darkroom.core.SnapGuide
import io.github.wa_otomia.darkroom.core.SnapGuideKind
import io.github.wa_otomia.darkroom.core.rotationSnapGuides
import io.github.wa_otomia.darkroom.core.snapGuideKind
import kotlin.math.PI
import kotlin.math.abs

/** Result of one [SnapDragState.update] frame. [snappedIn] is true only on the inhale. */
data class SnapDragResult(
    val x: Float,
    val y: Float,
    val rotation: Float,
    val snappedIn: Boolean,
)

/**
 * Three-axis snapper (pan X/Y + rotation) for Studio photos and the watermark editor.
 *
 * Feed each frame's **finger deltas** (not the already-snapped object pose). Zoom is ignored
 * for snapping; the caller still applies [zoomChange] itself. Read [activeGuides] to draw
 * amber overlays. Call [begin] on pointer-down and [end] on lift so [SnapAxis.raw] resyncs.
 */
class SnapDragState(
    positionThresholdPx: Float,
    rotationThresholdDeg: Float = SNAP_ROTATION_DEG,
) {
    private val xAxis = SnapAxis(positionThresholdPx)
    private val yAxis = SnapAxis(positionThresholdPx)
    private val rotationAxis = SnapAxis(rotationThresholdDeg, wrap = 360f)

    val x: Float get() = xAxis.value
    val y: Float get() = yAxis.value
    val rotation: Float get() = rotationAxis.value

    /** Guides currently pinning an axis; observed by Compose for overlay redraw. */
    var activeGuides: List<SnapGuide> by mutableStateOf(emptyList())
        private set

    /** Resync every axis so `raw == value`. */
    fun begin() {
        xAxis.begin(xAxis.value)
        yAxis.begin(yAxis.value)
        rotationAxis.begin(rotationAxis.value)
        publishGuides()
    }

    fun begin(x: Float, y: Float, rotation: Float = 0f) {
        xAxis.begin(x)
        yAxis.begin(y)
        rotationAxis.begin(rotation)
        publishGuides()
    }

    /**
     * Align every axis to the pose actually written to UI state and clear pins.
     * Does not count as a snap-in. Call after clamp or when pan/rotation changed
     * outside [update] so the next frame's finger delta is relative to that pose.
     */
    fun sync(x: Float, y: Float, rotation: Float) {
        xAxis.sync(x)
        yAxis.sync(y)
        rotationAxis.sync(rotation)
        publishGuides()
    }

    /**
     * @param pan finger translation this frame (px)
     * @param zoomChange unused (pinch does not snap)
     * @param rotation finger rotation this frame (degrees)
     */
    @Suppress("UNUSED_PARAMETER")
    fun update(
        pan: Offset,
        zoomChange: Float,
        rotation: Float,
        guidesX: List<SnapGuide>,
        guidesY: List<SnapGuide>,
    ): SnapDragResult {
        val hx = xAxis.drag(pan.x, guidesX)
        val hy = yAxis.drag(pan.y, guidesY)
        val hr = rotationAxis.drag(rotation, rotationSnapGuides())
        publishGuides()
        return SnapDragResult(
            x = xAxis.value,
            y = yAxis.value,
            rotation = rotationAxis.value,
            snappedIn = hx || hy || hr,
        )
    }

    fun end() {
        begin()
    }

    private fun publishGuides() {
        activeGuides = listOfNotNull(xAxis.snapped, yAxis.snapped, rotationAxis.snapped)
    }
}

/**
 * [androidx.compose.foundation.gestures.detectTransformGestures] plus begin/end so
 * [SnapDragState] can reset `raw` on each pointer-down / lift.
 */
suspend fun PointerInputScope.detectSnappingTransformGestures(
    onBegin: () -> Unit,
    onEnd: () -> Unit,
    onGesture: (centroid: Offset, pan: Offset, zoom: Float, rotation: Float) -> Unit,
) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        onBegin()
        try {
            var rotation = 0f
            var zoom = 1f
            var pan = Offset.Zero
            var pastTouchSlop = false
            val touchSlop = viewConfiguration.touchSlop
            do {
                val event = awaitPointerEvent()
                val canceled = event.changes.fastAny { it.isConsumed }
                if (!canceled) {
                    val zoomChange = event.calculateZoom()
                    val rotationChange = event.calculateRotation()
                    val panChange = event.calculatePan()
                    if (!pastTouchSlop) {
                        zoom *= zoomChange
                        rotation += rotationChange
                        pan += panChange
                        val centroidSize = event.calculateCentroidSize(useCurrent = false)
                        val zoomMotion = abs(1 - zoom) * centroidSize
                        val rotationMotion = abs(rotation * PI.toFloat() * centroidSize / 180f)
                        val panMotion = pan.getDistance()
                        if (zoomMotion > touchSlop || rotationMotion > touchSlop || panMotion > touchSlop) {
                            pastTouchSlop = true
                        }
                    }
                    if (pastTouchSlop) {
                        val centroid = event.calculateCentroid(useCurrent = false)
                        if (rotationChange != 0f || zoomChange != 1f || panChange != Offset.Zero) {
                            onGesture(centroid, panChange, zoomChange, rotationChange)
                        }
                        event.changes.fastForEach {
                            if (it.positionChanged()) it.consume()
                        }
                    }
                }
            } while (!canceled && event.changes.any { it.pressed })
        } finally {
            onEnd()
        }
    }
}

/**
 * Amber overlay for active snap guides. Centre lines span the frame; safe / frame-edge
 * guides are short ticks on the matching inset or edge.
 */
fun DrawScope.drawSnapGuides(
    guides: List<SnapGuide>,
    frameLeft: Float,
    frameTop: Float,
    frameW: Float,
    frameH: Float,
    inset: Float,
    color: Color,
    strokeWidth: Float,
) {
    if (guides.isEmpty() || frameW <= 0f || frameH <= 0f) return
    val tick = minOf(frameW, frameH) * 0.12f
    val drawn = HashSet<String>()
    for (guide in guides) {
        val kind = snapGuideKind(guide.id)
        if (kind == SnapGuideKind.ROTATION) continue
        if (!drawn.add(guide.id)) continue
        when (kind) {
            SnapGuideKind.CENTER -> if (guide.id == GUIDE_CENTER_X) {
                drawLine(
                    color,
                    Offset(frameLeft + frameW / 2f, frameTop),
                    Offset(frameLeft + frameW / 2f, frameTop + frameH),
                    strokeWidth,
                )
            } else {
                drawLine(
                    color,
                    Offset(frameLeft, frameTop + frameH / 2f),
                    Offset(frameLeft + frameW, frameTop + frameH / 2f),
                    strokeWidth,
                )
            }
            SnapGuideKind.SAFE, SnapGuideKind.FRAME -> {
                val useInset = kind == SnapGuideKind.SAFE
                val edge = if (useInset) inset else 0f
                when (guide.id) {
                    GUIDE_SAFE_X_NEG, GUIDE_FRAME_X_NEG -> drawLine(
                        color,
                        Offset(frameLeft + edge, frameTop + frameH / 2f - tick),
                        Offset(frameLeft + edge, frameTop + frameH / 2f + tick),
                        strokeWidth,
                    )
                    GUIDE_SAFE_X_POS, GUIDE_FRAME_X_POS -> drawLine(
                        color,
                        Offset(frameLeft + frameW - edge, frameTop + frameH / 2f - tick),
                        Offset(frameLeft + frameW - edge, frameTop + frameH / 2f + tick),
                        strokeWidth,
                    )
                    GUIDE_SAFE_Y_NEG, GUIDE_FRAME_Y_NEG -> drawLine(
                        color,
                        Offset(frameLeft + frameW / 2f - tick, frameTop + edge),
                        Offset(frameLeft + frameW / 2f + tick, frameTop + edge),
                        strokeWidth,
                    )
                    GUIDE_SAFE_Y_POS, GUIDE_FRAME_Y_POS -> drawLine(
                        color,
                        Offset(frameLeft + frameW / 2f - tick, frameTop + frameH - edge),
                        Offset(frameLeft + frameW / 2f + tick, frameTop + frameH - edge),
                        strokeWidth,
                    )
                }
            }
            SnapGuideKind.ROTATION -> Unit
        }
    }
}
