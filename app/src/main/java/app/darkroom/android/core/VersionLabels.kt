package app.darkroom.android.core

/** Numbered chip label: `1-人像精修`. Blank names fall back to [fallback]. */
fun versionLabel(index: Int, presetName: String?, fallback: String = "修图"): String {
    val name = presetName?.trim().orEmpty().ifEmpty { fallback }
    return "$index-$name"
}

fun resolvePhotoSource(source: String?): String =
    source?.trim()?.takeIf { it.isNotEmpty() } ?: "original"

fun editsInCreationOrder(edits: List<EditRecord>): List<EditRecord> =
    edits.sortedWith(compareBy<EditRecord> { it.createdAt }.thenBy { it.id })

fun newestVersionSource(edits: List<EditRecord>): String =
    edits.maxByOrNull { it.createdAt }?.id ?: "original"

fun defaultStudioSource(photo: PhotoMeta?): String =
    newestVersionSource(photo?.edits.orEmpty())

fun sourceAfterVersionRemoved(
    edits: List<EditRecord>,
    removedId: String,
    currentSource: String,
): String {
    val current = resolvePhotoSource(currentSource)
    if (current != removedId) return current
    return newestVersionSource(edits.filter { it.id != removedId })
}

fun resolveEditPresetName(
    presetTitle: String?,
    presetId: String?,
    fallback: String,
    titleForId: (String) -> String? = { null },
): String {
    val title = presetTitle?.trim().orEmpty()
    if (title.isNotEmpty()) return title
    val id = presetId?.trim().orEmpty()
    if (id.isNotEmpty()) {
        val resolved = titleForId(id)?.trim().orEmpty()
        if (resolved.isNotEmpty()) return resolved
    }
    return fallback
}

fun studioVersionIngestName(originalFilename: String, versionIndex: Int): String {
    val name = originalFilename.substringAfterLast('/')
    val stem = name.substringBeforeLast('.').trim().ifEmpty { "photo" }
    return "$stem-v$versionIndex.jpg"
}
