package app.darkroom.android.core

fun generatedPhotoFilename(sourceFilename: String): String {
    val stem = sourceFilename.substringBeforeLast('.').trim().ifEmpty { "photo" }
    return "$stem-生成.jpg"
}

fun combineGeneratePrompt(presetPrompt: String, customPrompt: String): String {
    val preset = presetPrompt.trim()
    val custom = customPrompt.trim()
    if (preset.isEmpty() && custom.isEmpty()) {
        throw IllegalArgumentException("请选择预设或填写补充提示词")
    }
    if (preset.isNotEmpty() && custom.isNotEmpty()) {
        return "$preset\n\n补充要求：\n$custom"
    }
    return preset.ifEmpty { custom }
}

/**
 * Whether a print request carries any Grok instruction. Mirrors the
 * preset-or-prompt rule of [combineGeneratePrompt], so "edit and print" with
 * only a free-text prompt still edits, while a bare direct print never does.
 */
fun shouldEditBeforePrint(presetId: String?, prompt: String): Boolean =
    (!presetId.isNullOrEmpty() && presetId != AI_PRESET_NONE) || prompt.isNotBlank()

fun resolveRootId(
    photos: Iterable<PhotoMeta>,
    photoId: String,
): String {
    val byId = photos.associateBy { it.id }
    val seen = mutableSetOf<String>()
    var id = photoId
    while (true) {
        if (!seen.add(id)) return id
        val node = byId[id] ?: return id
        val parent = node.parentId
        if (parent.isNullOrEmpty() || parent == id) return id
        if (!byId.containsKey(parent)) return parent
        id = parent
    }
}

fun listRelatedPhotos(photos: List<PhotoMeta>, photoId: String): List<PhotoMeta> {
    val rootId = resolveRootId(photos, photoId)
    return photos.filter { resolveRootId(photos, it.id) == rootId }
}
