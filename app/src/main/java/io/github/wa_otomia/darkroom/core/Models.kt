package io.github.wa_otomia.darkroom.core

import kotlinx.serialization.Serializable

@Serializable
data class PixelCrop(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
)

@Serializable
data class NormalizedCrop(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
)

@Serializable
data class EditRecord(
    val id: String,
    val prompt: String,
    val createdAt: String,
    val filename: String,
    val width: Int = 0,
    val height: Int = 0,
    val presetId: String = "",
    val presetTitle: String = "",
)

@Serializable
data class PrintRecord(
    val at: String,
    val jobId: Int? = null,
    val jobState: String? = null,
    val source: String,
    val crop: PixelCrop? = null,
    val copies: Int,
    val error: String? = null,
)

@Serializable
data class PhotoMeta(
    val id: String,
    val filename: String,
    val createdAt: String,
    val ingestedAt: String,
    val width: Int,
    val height: Int,
    val bytes: Long,
    val edits: List<EditRecord> = emptyList(),
    val prints: List<PrintRecord> = emptyList(),
    val parentId: String? = null,
    val generatePrompt: String? = null,
    val tags: List<String> = emptyList(),
    /** Per-version crop pose. Key is [ORIGINAL_VERSION_ID] or an edit id. Missing = auto defaults. */
    val framings: Map<String, Framing> = emptyMap(),
)

/** Catalog tag written when a Studio result is saved as its own gallery photo. */
const val GENERATED_PHOTO_TAG = "generated"

fun PhotoMeta.isGenerated(): Boolean =
    GENERATED_PHOTO_TAG in tags || parentId != null

enum class PrintFit {
    COVER,
    CONTAIN,
}

@Serializable
data class ActivityEntry(
    val id: String,
    val at: String,
    val kind: String,
    val message: String,
    val status: String = "ok",
    val error: String? = null,
)
