package io.github.wa_otomia.darkroom.core

const val SKIP_EDIT_NO_KEY = "edit:no_key"
const val SKIP_EDIT_NO_PRESET = "edit:no_preset"
const val SKIP_WATERMARK_NO_CONFIG = "watermark:no_config"
const val SKIP_WATERMARK_NO_PRINT = "watermark:no_print"
const val SKIP_PRINT_NO_PRINTER = "print:no_printer"

data class AutomationPlan(
    val edit: Boolean,
    val watermark: Boolean,
    val print: Boolean,
    val skipped: List<String>,
)

/**
 * Step-by-step ingest pipeline: ① AI edit → ② watermark → ③ print.
 * A disabled switch is omitted. An enabled switch whose conditions fail is
 * skipped with a reason string; later steps still run.
 */
fun planAutomation(
    autoEdit: Boolean,
    autoWatermark: Boolean,
    autoPrint: Boolean,
    presetId: String,
    aiConfigured: Boolean,
    watermarkConfigured: Boolean,
    printerBound: Boolean,
): AutomationPlan {
    val skipped = mutableListOf<String>()

    val hasPreset = presetId != AI_PRESET_NONE
    val edit = if (!autoEdit) {
        false
    } else {
        if (!hasPreset) skipped += SKIP_EDIT_NO_PRESET
        if (!aiConfigured) skipped += SKIP_EDIT_NO_KEY
        hasPreset && aiConfigured
    }

    val watermark = if (!autoWatermark) {
        false
    } else if (!watermarkConfigured) {
        skipped += SKIP_WATERMARK_NO_CONFIG
        false
    } else {
        true
    }

    val print = if (!autoPrint) {
        false
    } else if (!printerBound) {
        skipped += SKIP_PRINT_NO_PRINTER
        false
    } else {
        true
    }

    return AutomationPlan(edit = edit, watermark = watermark, print = print, skipped = skipped)
}
