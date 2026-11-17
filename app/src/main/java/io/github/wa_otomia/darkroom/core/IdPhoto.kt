package io.github.wa_otomia.darkroom.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlin.math.abs
import kotlin.math.ln

// ---------------------------------------------------------------------------------------------
// Sheet geometry (EDID020)
// ---------------------------------------------------------------------------------------------

/** Nominal 2×3 in paper: the 1560 px long edge of the landscape sheet is 76.2 mm. */
const val ID_PX_PER_MM: Double = 1560.0 / 76.2
const val ID_SHEET_WIDTH = 1560
const val ID_SHEET_HEIGHT = 1040
const val ID_GAP_PX = 10
const val ID_CUT_LINE_PX = 2
const val ID_CUT_LINE_COLOR: Int = 0xFFBDBDBD.toInt()

/** Below this margin (about 1 mm) the size note warns the printer may trim the edge. */
const val ID_EDGE_WARNING_PX = 20

fun idCellPx(mm: Double): Int = Math.round(mm * ID_PX_PER_MM).toInt()

data class IdPhotoPreset(val id: String, val widthMm: Double, val heightMm: Double)

/** Built-in sizes. Order and ids are stable: they are stored and shown in this order. */
val ID_PHOTO_PRESETS: List<IdPhotoPreset> = listOf(
    IdPhotoPreset("35x45", 35.0, 45.0),
    IdPhotoPreset("kr-visa", 35.0, 45.0),
    IdPhotoPreset("jp-visa", 45.0, 45.0),
    IdPhotoPreset("33x48", 33.0, 48.0),
    IdPhotoPreset("25x35", 25.0, 35.0),
    IdPhotoPreset("22x32", 22.0, 32.0),
    IdPhotoPreset("35x49", 35.0, 49.0),
    IdPhotoPreset("2x2in", 50.8, 50.8),
    IdPhotoPreset("50x70", 50.0, 70.0),
    IdPhotoPreset("30x40", 30.0, 40.0),
    IdPhotoPreset("24x30", 24.0, 30.0),
    IdPhotoPreset("40x60", 40.0, 60.0),
    IdPhotoPreset("35x50", 35.0, 50.0),
)

const val DEFAULT_ID_PHOTO_PRESET_ID = "35x45"

/** The preset with [id]; unknown or blank ids give the 35×45 default. */
fun idPhotoPreset(id: String?): IdPhotoPreset {
    val key = id?.trim()
    return ID_PHOTO_PRESETS.firstOrNull { it.id == key }
        ?: ID_PHOTO_PRESETS.first { it.id == DEFAULT_ID_PHOTO_PRESET_ID }
}

fun normalizeIdPhotoPresetId(id: String?): String = idPhotoPreset(id).id

data class IdRect(val left: Int, val top: Int, val width: Int, val height: Int) {
    val right: Int get() = left + width
    val bottom: Int get() = top + height
}

/**
 * Where the cells sit on the landscape sheet.
 *
 * [cellWidth]×[cellHeight] is the upright cell. When [rotated], every slot is
 * `cellHeight×cellWidth` and holds the upright cell turned 90° counter-clockwise.
 * [slots] are row-major. [cutLines] are the 2 px rings just outside each slot, clipped
 * to the sheet; they never cover slot pixels.
 */
data class IdPhotoLayout(
    val sheetWidth: Int,
    val sheetHeight: Int,
    val cellWidth: Int,
    val cellHeight: Int,
    val rotated: Boolean,
    val columns: Int,
    val rows: Int,
    val slots: List<IdRect>,
    val cutLines: List<IdRect>,
    val scale: Int,
) {
    val count: Int get() = slots.size

    /** Smallest of the four sheet margins around the grid, in this layout's px. */
    val minMarginPx: Int
        get() {
            if (slots.isEmpty()) return 0
            val left = slots.minOf { it.left }
            val top = slots.minOf { it.top }
            val right = sheetWidth - slots.maxOf { it.right }
            val bottom = sheetHeight - slots.maxOf { it.bottom }
            return minOf(minOf(left, top), minOf(right, bottom))
        }
}

/** Upright and rotated grids are both tried; the rotated one wins only with strictly more cells. */
fun idPhotoLayout(preset: IdPhotoPreset): IdPhotoLayout {
    val cw = idCellPx(preset.widthMm)
    val ch = idCellPx(preset.heightMm)
    val uprightCols = idGridCount(ID_SHEET_WIDTH, cw)
    val uprightRows = idGridCount(ID_SHEET_HEIGHT, ch)
    val rotatedCols = idGridCount(ID_SHEET_WIDTH, ch)
    val rotatedRows = idGridCount(ID_SHEET_HEIGHT, cw)
    val rotated = rotatedCols * rotatedRows > uprightCols * uprightRows
    val columns = if (rotated) rotatedCols else uprightCols
    val rows = if (rotated) rotatedRows else uprightRows
    val slotW = if (rotated) ch else cw
    val slotH = if (rotated) cw else ch
    val slots = ArrayList<IdRect>(columns * rows)
    if (columns > 0 && rows > 0) {
        val gridW = columns * slotW + (columns - 1) * ID_GAP_PX
        val gridH = rows * slotH + (rows - 1) * ID_GAP_PX
        val left = (ID_SHEET_WIDTH - gridW) / 2
        val top = (ID_SHEET_HEIGHT - gridH) / 2
        for (r in 0 until rows) {
            for (c in 0 until columns) {
                slots += IdRect(left + c * (slotW + ID_GAP_PX), top + r * (slotH + ID_GAP_PX), slotW, slotH)
            }
        }
    }
    return IdPhotoLayout(
        sheetWidth = ID_SHEET_WIDTH,
        sheetHeight = ID_SHEET_HEIGHT,
        cellWidth = cw,
        cellHeight = ch,
        rotated = rotated,
        columns = columns,
        rows = rows,
        slots = slots,
        cutLines = idCutLines(slots, ID_SHEET_WIDTH, ID_SHEET_HEIGHT, ID_CUT_LINE_PX),
        scale = 1,
    )
}

private fun idGridCount(sheet: Int, cell: Int): Int =
    if (cell <= 0) 0 else (sheet + ID_GAP_PX) / (cell + ID_GAP_PX)

private fun idCutLines(slots: List<IdRect>, sheetW: Int, sheetH: Int, line: Int): List<IdRect> {
    val out = ArrayList<IdRect>(slots.size * 4)
    for (s in slots) {
        val ring = listOf(
            IdRect(s.left - line, s.top - line, s.width + 2 * line, line),
            IdRect(s.left - line, s.bottom, s.width + 2 * line, line),
            IdRect(s.left - line, s.top, line, s.height),
            IdRect(s.right, s.top, line, s.height),
        )
        for (r in ring) {
            val l = r.left.coerceAtLeast(0)
            val t = r.top.coerceAtLeast(0)
            val rr = r.right.coerceAtMost(sheetW)
            val b = r.bottom.coerceAtMost(sheetH)
            if (rr > l && b > t) out += IdRect(l, t, rr - l, b - t)
        }
    }
    return out
}

/** Every integer times [scale] (≥1), so export geometry is exactly twice the print geometry. */
fun IdPhotoLayout.scaled(scale: Int): IdPhotoLayout {
    val s = scale.coerceAtLeast(1)
    if (s == 1) return this
    fun IdRect.times() = IdRect(left * s, top * s, width * s, height * s)
    return copy(
        sheetWidth = sheetWidth * s,
        sheetHeight = sheetHeight * s,
        cellWidth = cellWidth * s,
        cellHeight = cellHeight * s,
        slots = slots.map { it.times() },
        cutLines = cutLines.map { it.times() },
        scale = this.scale * s,
    )
}

/** True when the smallest margin, measured at print scale, is under [ID_EDGE_WARNING_PX]. */
fun IdPhotoLayout.showsEdgeWarning(): Boolean =
    minMarginPx < ID_EDGE_WARNING_PX * scale.coerceAtLeast(1)

// ---------------------------------------------------------------------------------------------
// Shared pose (EDID030). Pan is a fraction of the upright cell width / height.
// ---------------------------------------------------------------------------------------------

data class IdPhotoPose(
    val zoom: Float = 1f,
    val panX: Float = 0f,
    val panY: Float = 0f,
    val rotation: Float = 0f,
)

/** First entry: zoom 1, pan 0, the right angle nearest the ordinary framing's rotation. */
fun idInitialPose(normalRotationDegrees: Float): IdPhotoPose {
    val rot = if (normalRotationDegrees.isFinite()) normalRotationDegrees else 0f
    return IdPhotoPose(rotation = normalizeRotationDegrees(Math.round(rot / 90f) * 90f))
}

/** The clamped placement of one upright cell, offsets in cell px. */
fun IdPhotoPose.toCellPlacement(
    cellWidth: Int,
    cellHeight: Int,
    imageWidth: Int,
    imageHeight: Int,
): ViewportPlacement = viewportPlacement(
    cellWidth.toFloat(),
    cellHeight.toFloat(),
    imageWidth,
    imageHeight,
    zoom,
    panX * cellWidth,
    panY * cellHeight,
    rotation,
)

/**
 * Zoom clamped, rotation normalized and pan clamped in cell px. Pan is left alone while the
 * cell or image size is unknown.
 */
fun IdPhotoPose.clamped(cellWidth: Int, cellHeight: Int, imageWidth: Int, imageHeight: Int): IdPhotoPose {
    val z = clampZoom(if (zoom.isFinite()) zoom else 1f)
    val rot = normalizeRotationDegrees(if (rotation.isFinite()) rotation else 0f)
    val px = if (panX.isFinite()) panX else 0f
    val py = if (panY.isFinite()) panY else 0f
    if (cellWidth <= 0 || cellHeight <= 0 || imageWidth <= 0 || imageHeight <= 0) {
        return IdPhotoPose(z, px, py, rot)
    }
    val cw = cellWidth.toFloat()
    val ch = cellHeight.toFloat()
    val (x, y) = clampPan(px * cw, py * ch, z, cw, ch, imageWidth, imageHeight, rot)
    return IdPhotoPose(z, x / cw, y / ch, rot)
}

/**
 * The rotate buttons, as in Studio's ordinary framing: land on the next right angle in
 * [direction] and turn the pan by the actual angle travelled. The pan turns in cell px, so a
 * non-square cell is not stretched.
 */
fun IdPhotoPose.rotatedQuarter(
    direction: Int,
    cellWidth: Int,
    cellHeight: Int,
    imageWidth: Int,
    imageHeight: Int,
): IdPhotoPose {
    val rot = if (rotation.isFinite()) rotation else 0f
    val target = normalizeRotationDegrees(Math.round((rot + direction * 90f) / 90f) * 90f)
    val cw = cellWidth.toFloat()
    val ch = cellHeight.toFloat()
    val (nx, ny) = if (cellWidth > 0 && cellHeight > 0) {
        val (x, y) = rotatePan(panX * cw, panY * ch, target - rot)
        x / cw to y / ch
    } else {
        panX to panY
    }
    return IdPhotoPose(zoom, nx, ny, target).clamped(cellWidth, cellHeight, imageWidth, imageHeight)
}

/**
 * Size change keeps zoom and rotation. The pan fraction is read in the [new] cell's px and
 * clamped there; fractions are cell-relative, so [old] does not change the result.
 */
@Suppress("UNUSED_PARAMETER")
fun IdPhotoPose.forPreset(
    old: IdPhotoLayout,
    new: IdPhotoLayout,
    imageWidth: Int,
    imageHeight: Int,
): IdPhotoPose = clamped(new.cellWidth, new.cellHeight, imageWidth, imageHeight)

/** A finger delta on the sheet in the upright cell's axes, so the image follows the finger. */
fun idSheetDeltaToCell(dx: Float, dy: Float, rotated: Boolean): Pair<Float, Float> =
    if (rotated) rotatePan(dx, dy, 1) else dx to dy

/**
 * The anchor for ([sheetX], [sheetY]): the point itself when a slot contains it, otherwise the
 * nearest point of the nearest slot. Returned as an offset from that slot's centre, in upright
 * cell px.
 */
fun idCellAnchor(layout: IdPhotoLayout, sheetX: Float, sheetY: Float): Pair<Float, Float> {
    val slot = idAnchorSlot(layout, sheetX, sheetY) ?: return 0f to 0f
    val x = sheetX.coerceIn(slot.left.toFloat(), slot.right.toFloat())
    val y = sheetY.coerceIn(slot.top.toFloat(), slot.bottom.toFloat())
    val lx = x - (slot.left + slot.width / 2f)
    val ly = y - (slot.top + slot.height / 2f)
    return idSheetDeltaToCell(lx, ly, layout.rotated)
}

private fun idAnchorSlot(layout: IdPhotoLayout, x: Float, y: Float): IdRect? {
    var best: IdRect? = null
    var bestDist = Float.MAX_VALUE
    for (s in layout.slots) {
        val dx = maxOf(s.left - x, 0f, x - s.right)
        val dy = maxOf(s.top - y, 0f, y - s.bottom)
        val d = dx * dx + dy * dy
        if (d < bestDist) {
            bestDist = d
            best = s
        }
    }
    return best
}

/**
 * One frame of a whole-sheet gesture: zoom and rotate about the anchor point in the cell under
 * the centroid, then pan by the finger delta and clamp. No snapping. Coordinates are in
 * [layout]'s px.
 */
fun applyIdGesture(
    pose: IdPhotoPose,
    layout: IdPhotoLayout,
    imageWidth: Int,
    imageHeight: Int,
    centroidSheetX: Float,
    centroidSheetY: Float,
    panSheetDx: Float,
    panSheetDy: Float,
    zoomChange: Float,
    rotationDelta: Float,
): IdPhotoPose {
    val cw = layout.cellWidth.toFloat()
    val ch = layout.cellHeight.toFloat()
    if (cw <= 0f || ch <= 0f) return pose
    val z = clampZoom(if (pose.zoom.isFinite()) pose.zoom else 1f)
    val change = if (zoomChange.isFinite() && zoomChange > 0f) zoomChange else 1f
    val turn = if (rotationDelta.isFinite()) rotationDelta else 0f
    val liveX = pose.panX * cw
    val liveY = pose.panY * ch
    val nextZoom = clampZoom(z * change)
    val ratio = nextZoom / z
    val (fx, fy) = idCellAnchor(layout, centroidSheetX, centroidSheetY)
    val zoomedX = fx - (fx - liveX) * ratio
    val zoomedY = fy - (fy - liveY) * ratio
    val (rx, ry) = rotatePan(zoomedX - fx, zoomedY - fy, turn)
    val nextRotation = normalizeRotationDegrees(pose.rotation + turn)
    val (dx, dy) = idSheetDeltaToCell(panSheetDx, panSheetDy, layout.rotated)
    val x = rx + fx + dx
    val y = ry + fy + dy
    val (cx, cy) = if (imageWidth > 0 && imageHeight > 0) {
        clampPan(x, y, nextZoom, cw, ch, imageWidth, imageHeight, nextRotation)
    } else {
        x to y
    }
    return IdPhotoPose(nextZoom, cx / cw, cy / ch, nextRotation)
}

// ---------------------------------------------------------------------------------------------
// Background and feather (EDID040)
// ---------------------------------------------------------------------------------------------

enum class IdBackground(val id: String, val argb: Int?) {
    ORIGINAL("original", null),
    NAVY("navy", 0xFF1E4B91.toInt()),
    ID_BLUE("id_blue", 0xFF438EDB.toInt()),
    RED("red", 0xFFFF0000.toInt()),
    WHITE("white", 0xFFFFFFFF.toInt()),
    LIGHT_GREY("light_grey", 0xFFE5E5E5.toInt()),
    OFF_WHITE("off_white", 0xFFF2EEE3.toInt()),
    PALE_BLUE("pale_blue", 0xFFD6E4F0.toInt()),
    ;

    val needsMask: Boolean get() = argb != null
}

fun idBackground(id: String?): IdBackground {
    val key = id?.trim()
    return IdBackground.entries.firstOrNull { it.id == key } ?: IdBackground.ORIGINAL
}

val DEFAULT_ID_BACKGROUND_AFTER_MASK = IdBackground.NAVY

/** "本地抠图" starts off (EDID040-R10): AI cut-out stays the default. */
const val DEFAULT_ID_PHOTO_LOCAL_CUTOUT = false

enum class IdFeather(val id: String, val radiusMm: Double) {
    NONE("none", 0.0),
    WEAK("weak", 0.1),
    MEDIUM("medium", 0.25),
    STRONG("strong", 0.5),
    ;

    /** 0 / 2 / 5 / 10 px on the print sheet, times the output scale. */
    fun radiusPx(scale: Int = 1): Int = idCellPx(radiusMm) * scale.coerceAtLeast(1)
}

fun idFeather(id: String?): IdFeather {
    val key = id?.trim()
    return IdFeather.entries.firstOrNull { it.id == key } ?: IdFeather.WEAK
}

// ---------------------------------------------------------------------------------------------
// Per-version state and its storage inside framingsJson (LBVR020-R04)
// ---------------------------------------------------------------------------------------------

@Serializable
data class IdPhotoState(
    val active: Boolean = false,
    val presetId: String = DEFAULT_ID_PHOTO_PRESET_ID,
    val zoom: Float = 1f,
    val panX: Float = 0f,
    val panY: Float = 0f,
    val rotation: Float = 0f,
    val background: String = IdBackground.ORIGINAL.id,
    val feather: String = IdFeather.WEAK.id,
    val maskRev: String? = null,
    /** Shrink the mask by the feather radius before smoothing, so the blend stays inside (EDID040-R07). */
    val featherInward: Boolean = true,
) {
    val pose: IdPhotoPose get() = IdPhotoPose(zoom, panX, panY, rotation)

    fun withPose(p: IdPhotoPose): IdPhotoState =
        copy(zoom = p.zoom, panX = p.panX, panY = p.panY, rotation = p.rotation)

    val hasMask: Boolean get() = !maskRev.isNullOrBlank()

    /** Known ids, finite numbers, zoom in range, rotation in [0, 360), blank mask rev = none. */
    fun normalized(): IdPhotoState = IdPhotoState(
        active = active,
        presetId = normalizeIdPhotoPresetId(presetId),
        zoom = if (zoom.isFinite()) clampZoom(zoom) else 1f,
        panX = if (panX.isFinite()) panX else 0f,
        panY = if (panY.isFinite()) panY else 0f,
        rotation = if (rotation.isFinite()) normalizeRotationDegrees(rotation) else 0f,
        background = idBackground(background).id,
        feather = idFeather(feather).id,
        maskRev = maskRev?.trim()?.takeIf { it.isNotEmpty() },
        featherInward = featherInward,
    )
}

fun PhotoMeta.idPhotoFor(source: String?): IdPhotoState? = idPhotos[framingVersionKey(source)]

/** Whether the version [source] (the original or one of [PhotoMeta.edits]) still exists. */
fun PhotoMeta.hasVersion(source: String?): Boolean {
    val key = framingVersionKey(source)
    return key == ORIGINAL_VERSION_ID || edits.any { it.id == key }
}

/**
 * Names of `idmask-failed/<photoId>` folders whose photo has no catalog row: a reply kept for a
 * photo that has since been deleted (EDID040-R09).
 */
fun orphanIdMaskFailureDirs(dirNames: Collection<String>, knownIds: Set<String>): List<String> =
    dirNames.filter { it !in knownIds }

/**
 * A version's ID state after a Studio save of its toggle, size and pose (EDID010-R03), or null to
 * keep [old] as stored. Background, feather and the mask revision stay as they are. Without a
 * stored state an inactive save writes nothing unless [createIfMissing] says the version did
 * enter ID mode (its first entry is saved even when ID mode was left before any active save
 * landed); a version that never entered still starts from the initial pose (EDID010-R04).
 */
fun mergeIdPhotoPose(
    old: IdPhotoState?,
    active: Boolean,
    presetId: String,
    pose: IdPhotoPose,
    createIfMissing: Boolean,
): IdPhotoState? {
    if (old == null && !active && !createIfMissing) return null
    return (old ?: IdPhotoState()).copy(
        active = active,
        presetId = normalizeIdPhotoPresetId(presetId),
    ).withPose(pose)
}

/** Replaces the version's ID state; null removes it. Ordinary framings are untouched. */
fun PhotoMeta.withIdPhoto(source: String?, state: IdPhotoState?): PhotoMeta {
    val key = framingVersionKey(source)
    if (state == null && key !in idPhotos) return this
    val next = idPhotos.toMutableMap()
    if (state == null) next.remove(key) else next[key] = state
    return copy(idPhotos = next)
}

private const val ID_PHOTO_JSON_KEY = "idPhoto"
private const val FRAMING_REQUIRED_KEY = "landscape"
private val versionStateJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

/**
 * `{"original": {<Framing fields>, "idPhoto": {...}}, "e1": {"idPhoto": {...}}}`.
 * Without any ID state the output is exactly the legacy framings JSON.
 */
fun encodeVersionStates(framings: Map<String, Framing>, idPhotos: Map<String, IdPhotoState>): String {
    if (idPhotos.isEmpty()) return versionStateJson.encodeToString(framings)
    val keys = LinkedHashSet<String>(framings.size + idPhotos.size)
    keys += framings.keys
    keys += idPhotos.keys
    val entries = LinkedHashMap<String, JsonElement>(keys.size)
    for (key in keys) {
        val fields = LinkedHashMap<String, JsonElement>()
        framings[key]?.let { framing ->
            fields.putAll(versionStateJson.encodeToJsonElement(Framing.serializer(), framing) as JsonObject)
        }
        idPhotos[key]?.let { state ->
            fields[ID_PHOTO_JSON_KEY] =
                versionStateJson.encodeToJsonElement(IdPhotoState.serializer(), state.normalized())
        }
        entries[key] = JsonObject(fields)
    }
    return JsonObject(entries).toString()
}

/**
 * Tolerant per entry: an entry is a [Framing] only when it has `landscape`, and its `idPhoto`
 * is decoded on its own, so a damaged ID state drops only that version's ID state. Text that
 * is not a JSON object gives two empty maps.
 */
fun decodeVersionStates(raw: String): Pair<Map<String, Framing>, Map<String, IdPhotoState>> {
    val root = runCatching { versionStateJson.parseToJsonElement(raw) }.getOrNull() as? JsonObject
        ?: return emptyMap<String, Framing>() to emptyMap()
    val framings = LinkedHashMap<String, Framing>()
    val idPhotos = LinkedHashMap<String, IdPhotoState>()
    for ((key, element) in root) {
        val entry = element as? JsonObject ?: continue
        if (FRAMING_REQUIRED_KEY in entry) {
            runCatching {
                versionStateJson.decodeFromJsonElement(Framing.serializer(), JsonObject(entry - ID_PHOTO_JSON_KEY))
            }.getOrNull()?.let { framings[key] = it }
        }
        entry[ID_PHOTO_JSON_KEY]?.let { id ->
            runCatching { versionStateJson.decodeFromJsonElement(IdPhotoState.serializer(), id) }
                .getOrNull()
                ?.normalized()
                ?.let { idPhotos[key] = it }
        }
    }
    return framings to idPhotos
}

/** Preset prints never use the ID sheet; manual ones do when the version's ID mode is on. */
fun idPrintApplies(state: IdPhotoState?, usedPreset: Boolean): Boolean = !usedPreset && state?.active == true

fun idMaskRequired(state: IdPhotoState?): Boolean = state != null && idBackground(state.background).needsMask

// ---------------------------------------------------------------------------------------------
// Messages thrown by the data layer and mapped in UserErrors
// ---------------------------------------------------------------------------------------------

const val ID_MASK_MSG_ASPECT = "抠图失败：蒙版比例与上传图不符"
const val ID_MASK_MSG_NOT_BINARY = "抠图失败：返回图不是黑白蒙版"
const val ID_MASK_MSG_NO_PERSON = "抠图失败：未识别到人像"
const val ID_MASK_MSG_NO_BACKGROUND = "抠图失败：未分离出背景"
const val ID_MASK_MSG_UNDECODABLE = "抠图失败：无法解码返回图"
const val ID_MASK_MSG_NEED_KEY = "抠图失败：未配置 AI 密钥"
const val ID_MASK_MSG_VERSION_GONE = "抠图失败：该版本已删除"
const val ID_MASK_MSG_MISSING = "证件照蒙版缺失或损坏"
const val ID_MASK_MSG_BACKGROUND_KEPT = "抠图失败：返回图保留了原背景，不是蒙版"
const val ID_MASK_MSG_SHADED = "抠图失败：返回图是带明暗的灰度图，不是蒙版"

/** On-device cut-out failure (EDID040-R11); the thrown message adds "：<exception class>". */
const val ID_MASK_MSG_LOCAL_FAILED = "本地抠图失败"

// ---------------------------------------------------------------------------------------------
// Failed cut-out record (EDID040-R09)
// ---------------------------------------------------------------------------------------------

/** Longest failure reason kept with a failed cut-out. */
const val ID_MASK_FAILURE_MESSAGE_MAX = 500

/**
 * What a version keeps after a mask job fails for any reason but a user cancel: the sanitized
 * reason, when it failed (epoch ms), whether the reply bytes were kept next to it and whether the
 * job was an on-device cut-out ([local], EDID040-R11; absent in older records, read as false).
 * Stored as `idmask-failed/<photoId>/<versionKey>.json` in the app cache; its presence is the record.
 */
@Serializable
data class IdMaskFailureRecord(
    val message: String,
    val at: Long,
    val hasImage: Boolean,
    val local: Boolean = false,
)

/** [message] with secrets redacted (AIPV040-R03), trimmed and cut to [ID_MASK_FAILURE_MESSAGE_MAX] chars. */
fun idMaskFailureMessage(message: String?): String {
    val clean = sanitizeAiSecrets(message.orEmpty()).trim()
    if (clean.length <= ID_MASK_FAILURE_MESSAGE_MAX) return clean
    // Never leave half of a surrogate pair at the cut.
    val end = if (Character.isHighSurrogate(clean[ID_MASK_FAILURE_MESSAGE_MAX - 1])) {
        ID_MASK_FAILURE_MESSAGE_MAX - 1
    } else {
        ID_MASK_FAILURE_MESSAGE_MAX
    }
    return clean.substring(0, end)
}

/**
 * The reply bytes a failed cut-out keeps: any reply that arrived, as is, whatever went wrong
 * after it (undecodable, wrong aspect, not a matte, version gone). Null when none arrived.
 */
fun idMaskFailureReply(reply: ByteArray?): ByteArray? = reply?.takeIf { it.isNotEmpty() }

/**
 * True when a cut-out job that threw [error] ends as a user cancel (no failure record): the
 * error is a cancellation, or the job was already cancelled ([jobActive] false). A cancel that
 * lands while a reply is decoded can surface as the decode's own error instead of a
 * cancellation, so the job's state decides (EDID040-R09).
 */
fun idMaskEndsAsCancel(error: Throwable, jobActive: Boolean): Boolean =
    error is kotlin.coroutines.cancellation.CancellationException || !jobActive

/**
 * The record of a failed cut-out with [message], at [at], given the [reply] bytes received;
 * [local] when the job ran on device.
 */
fun idMaskFailureRecord(message: String?, reply: ByteArray?, at: Long, local: Boolean = false): IdMaskFailureRecord =
    IdMaskFailureRecord(idMaskFailureMessage(message), at, idMaskFailureReply(reply) != null, local)

private val idMaskFailureJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

fun encodeIdMaskFailureRecord(record: IdMaskFailureRecord): String =
    idMaskFailureJson.encodeToString(IdMaskFailureRecord.serializer(), record)

/** The stored record, or null when [raw] is missing, not JSON or lacks a field. */
fun parseIdMaskFailureRecord(raw: String?): IdMaskFailureRecord? {
    if (raw.isNullOrBlank()) return null
    val record = runCatching { idMaskFailureJson.decodeFromString(IdMaskFailureRecord.serializer(), raw) }
        .getOrNull() ?: return null
    // Re-cleaned on read: the file is outside the app's control once written.
    return record.copy(message = idMaskFailureMessage(record.message))
}

// ---------------------------------------------------------------------------------------------
// AI mask request (AIPV030-R04)
// ---------------------------------------------------------------------------------------------

/**
 * Fixed instruction for the matte request (AIPV030-R04). Presets and extra prompts never join it.
 * One sentence per literal, joined by exactly one space.
 */
const val ID_PHOTO_MASK_PROMPT: String =
    "Task: produce an alpha matte for replacing the background of a portrait or ID photo. " +
        "This is a segmentation task, not a photo edit: do not relight, recolour, restyle or re-render the photo. " +
        "Output only the matte, at exactly the same width, height, framing and composition as the input; " +
        "do not crop, pad, shift, rotate, resize or redraw anything, so that every matte pixel lines up with the same pixel of the input. " +
        "Foreground is the main person only: hair, face, ears, neck, shoulders, arms and hands if visible, " +
        "worn clothing and worn accessories such as glasses, earrings, hats and headscarves. " +
        "Background is everything else, even where it touches or overlaps the person: walls, floors, ceilings, sky, doors, windows, " +
        "furniture including chairs, chair backs, headrests, sofas and cushions, shelves, hanging clothes and coats, bags, objects, " +
        "other people, shadows cast on the background, and any border or padding. " +
        "Background seen through gaps in the person is background too: between hair strands and locks, inside curls and loose ends, " +
        "between the hair and the face, neck or shoulders, and between the arms and the body. " +
        "Paint the background pure black (#000000), even where it is white or light-coloured in the photo and even inside those gaps. " +
        "Paint the person as one flat, solid, pure white (#FFFFFF) silhouette with no shading, lighting, gradients, texture, " +
        "facial features or outlines inside it; eyes, mouth, glasses, hair texture and clothing details must not be visible. " +
        "Keep the edge tight to the person, with no glow, halo or margin around it. " +
        "Only where fine hair strands or flyaways overlap the background, use grey levels to show partial transparency " +
        "(lighter means more hair, darker means more background); only dense hair with no background showing through " +
        "stays solid white, and everywhere else make a clean, sharp edge. " +
        "If no person is visible, return an all-black image. " +
        "Use only black, white and that grey hair transition: no colours, text, watermarks or other content."

val OPENAI_MASK_CANVASES: List<Pair<Int, Int>> = listOf(1024 to 1024, 1536 to 1024, 1024 to 1536)

/** The supported canvas whose aspect is closest to [width]:[height] on a log scale. */
fun openAiMaskCanvas(width: Int, height: Int): Pair<Int, Int> {
    if (width <= 0 || height <= 0) return OPENAI_MASK_CANVASES.first()
    val aspect = width.toDouble() / height
    return OPENAI_MASK_CANVASES.minBy { (cw, ch) -> abs(ln(aspect / (cw.toDouble() / ch))) }
}

/** [width]×[height] fitted into the canvas without upscaling, centred, in whole px. */
fun fitMaskContent(width: Int, height: Int, canvasWidth: Int, canvasHeight: Int): IdRect {
    if (width <= 0 || height <= 0 || canvasWidth <= 0 || canvasHeight <= 0) {
        return IdRect(0, 0, canvasWidth.coerceAtLeast(0), canvasHeight.coerceAtLeast(0))
    }
    val scale = minOf(1.0, canvasWidth.toDouble() / width, canvasHeight.toDouble() / height)
    val w = Math.round(width * scale).toInt().coerceIn(1, canvasWidth)
    val h = Math.round(height * scale).toInt().coerceIn(1, canvasHeight)
    return IdRect((canvasWidth - w) / 2, (canvasHeight - h) / 2, w, h)
}

/** AI upload size: long edge at most [AI_UPLOAD_MAX_EDGE], never upscaled. */
fun aiUploadSize(width: Int, height: Int): OrientedSize {
    val longEdge = maxOf(width, height)
    val scale = if (longEdge <= 0) 1f else minOf(1f, AI_UPLOAD_MAX_EDGE.toFloat() / longEdge)
    return OrientedSize(
        (width * scale).toInt().coerceAtLeast(1),
        (height * scale).toInt().coerceAtLeast(1),
    )
}
