package app.darkroom.android.core

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val IMPORT_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")

/**
 * Resolves a picker/share display name to something we can ingest.
 *
 * Photo-picker content URIs often surface a MediaStore `_id` (`"265"`) instead of the
 * real filename; those get a timestamped `import-yyyyMMdd-HHmmss.jpg` fallback.
 */
fun fallbackImportName(
    rawName: String?,
    now: Long,
    zone: ZoneId = ZoneId.systemDefault(),
): String {
    val cleaned = rawName?.replace('\\', '/')?.substringAfterLast('/')?.trim().orEmpty()
    val stem = cleaned.substringBeforeLast('.', cleaned)
    val numeric = stem.isNotEmpty() && stem.all { it.isDigit() }
    if (cleaned.isEmpty() || numeric) {
        val stamp = Instant.ofEpochMilli(now).atZone(zone).format(IMPORT_STAMP)
        return "import-$stamp.jpg"
    }
    return cleaned
}
