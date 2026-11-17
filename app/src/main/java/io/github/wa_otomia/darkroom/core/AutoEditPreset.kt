package io.github.wa_otomia.darkroom.core

/**
 * The preset that import automation uses (AUTG040). It is stored apart from
 * [AiPresetStore.lastSelectedId], which stays the Studio and preset-library default.
 */

/** Effective automation preset: the stored ID if it is still in the library, else NONE. Never falls back. */
fun resolveAutoEditPresetId(stored: String?, presets: List<AiPreset>): String {
    val id = stored?.trim().orEmpty()
    return if (id.isNotEmpty() && presets.any { it.id == id }) id else AI_PRESET_NONE
}

/**
 * Startup reconcile, run after the built-in merge.
 * [stored] == null means the key was never written: migrate from [lastSelectedId] (AUTG040-R03).
 * Otherwise validate the stored value (AUTG040-R05). Idempotent: f(f(x)) == f(x).
 */
fun reconcileAutoEditPresetId(stored: String?, lastSelectedId: String, presets: List<AiPreset>): String =
    resolveAutoEditPresetId(stored ?: lastSelectedId, presets)

/** Value after deleting [deletedId] from the library (AUTG040-R04). */
fun autoEditPresetAfterDelete(current: String, deletedId: String): String =
    if (current == deletedId) AI_PRESET_NONE else current
