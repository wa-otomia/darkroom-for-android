package io.github.wa_otomia.darkroom.core

import kotlin.math.abs
import kotlinx.serialization.Serializable

/** Stable [PhotoMeta.framings] key for the unedited original. */
const val ORIGINAL_VERSION_ID = "original"

@Serializable
data class Framing(
    val landscape: Boolean,
    val zoom: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val rotationDegrees: Float = 0f,
    val frameWidth: Float = 0f,
    val frameHeight: Float = 0f,
    val imageWidth: Int = 0,
    val imageHeight: Int = 0,
)

/** Live Studio crop sent with an AI job so the model sees the framed cut, not the full file. */
data class AppliedFraming(
    val crop: PixelCrop? = null,
    val cropImageWidth: Int? = null,
    val cropImageHeight: Int? = null,
    val rotateQuarters: Int = 0,
    val landscape: Boolean = false,
    val rotationDegrees: Float = 0f,
    val placement: ViewportPlacement? = null,
)

data class ResolvedJobPose(
    val landscape: Boolean?,
    val rotationDegrees: Float,
    val placement: ViewportPlacement?,
)

fun framingVersionKey(source: String?): String = resolvePhotoSource(source)

fun framingSheetSize(landscape: Boolean): Pair<Float, Float> =
    if (landscape) PRINT_HEIGHT.toFloat() to PRINT_WIDTH.toFloat()
    else PRINT_WIDTH.toFloat() to PRINT_HEIGHT.toFloat()

/** Maps a saved pan into another cropper/print frame of a different pixel size. */
fun Framing.panInFrame(frameWidth: Float, frameHeight: Float): Pair<Float, Float> {
    if (this.frameWidth <= 0f || this.frameHeight <= 0f || frameWidth <= 0f || frameHeight <= 0f) {
        return offsetX to offsetY
    }
    return (offsetX * frameWidth / this.frameWidth) to (offsetY * frameHeight / this.frameHeight)
}

fun Framing.toPlacement(): ViewportPlacement {
    val (fallbackW, fallbackH) = framingSheetSize(landscape)
    return ViewportPlacement(
        zoom = clampZoom(zoom),
        offsetX = offsetX,
        offsetY = offsetY,
        rotationDegrees = normalizeRotationDegrees(rotationDegrees),
        frameWidth = if (frameWidth > 0f) frameWidth else fallbackW,
        frameHeight = if (frameHeight > 0f) frameHeight else fallbackH,
        imageWidth = imageWidth,
        imageHeight = imageHeight,
    )
}

fun ViewportPlacement.toFraming(landscape: Boolean): Framing = Framing(
    landscape = landscape,
    zoom = zoom,
    offsetX = offsetX,
    offsetY = offsetY,
    rotationDegrees = rotationDegrees,
    frameWidth = frameWidth,
    frameHeight = frameHeight,
    imageWidth = imageWidth,
    imageHeight = imageHeight,
)

fun PhotoMeta.framingFor(source: String?): Framing? = framings[framingVersionKey(source)]

fun PhotoMeta.withFraming(source: String?, framing: Framing?): PhotoMeta {
    val key = framingVersionKey(source)
    if (framing == null && key !in framings) return this
    val next = framings.toMutableMap()
    if (framing == null) next.remove(key) else next[key] = framing
    return copy(framings = next)
}

/** Framing of the version the gallery tile already shows (newest edit, else original). */
fun galleryTileFraming(photo: PhotoMeta, hasEditThumb: (String) -> Boolean = { true }): Framing? =
    photo.framingFor(galleryThumbSource(photo, hasEditThumb))

fun Framing.matchesAutoDefault(
    imageWidth: Int = this.imageWidth,
    imageHeight: Int = this.imageHeight,
): Boolean {
    if (abs(zoom - 1f) > 0.002f) return false
    if (abs(offsetX) > 0.5f || abs(offsetY) > 0.5f) return false
    val rot = normalizeRotationDegrees(rotationDegrees)
    if (rot > 0.5f && abs(rot - 360f) > 0.5f) return false
    return landscape == defaultLandscape(imageWidth, imageHeight)
}

fun liveFraming(
    landscape: Boolean,
    zoom: Float,
    offsetX: Float,
    offsetY: Float,
    rotationDegrees: Float,
    placement: ViewportPlacement?,
    imageWidth: Int,
    imageHeight: Int,
): Framing {
    if (placement != null) {
        return placement.toFraming(landscape).copy(
            zoom = zoom,
            rotationDegrees = rotationDegrees,
            landscape = landscape,
        )
    }
    val (fw, fh) = framingSheetSize(landscape)
    return Framing(
        landscape = landscape,
        zoom = zoom,
        offsetX = offsetX,
        offsetY = offsetY,
        rotationDegrees = rotationDegrees,
        frameWidth = fw,
        frameHeight = fh,
        imageWidth = imageWidth,
        imageHeight = imageHeight,
    )
}

fun persistableFraming(
    framing: Framing,
    imageWidth: Int = framing.imageWidth,
    imageHeight: Int = framing.imageHeight,
): Framing? = framing.takeUnless { it.matchesAutoDefault(imageWidth, imageHeight) }

/**
 * Gallery/automation enqueue passes no explicit pose; Studio print always does.
 * Saved framing is applied only when the caller left landscape and placement unset.
 */
fun resolveQueuedPose(
    saved: Framing?,
    explicitLandscape: Boolean?,
    explicitRotationDegrees: Float,
    explicitPlacement: ViewportPlacement?,
): ResolvedJobPose {
    val useSaved = saved != null && explicitLandscape == null && explicitPlacement == null
    if (useSaved) {
        return ResolvedJobPose(
            landscape = saved.landscape,
            rotationDegrees = saved.rotationDegrees,
            placement = saved.toPlacement(),
        )
    }
    return ResolvedJobPose(
        landscape = explicitLandscape,
        rotationDegrees = explicitRotationDegrees,
        placement = explicitPlacement,
    )
}
