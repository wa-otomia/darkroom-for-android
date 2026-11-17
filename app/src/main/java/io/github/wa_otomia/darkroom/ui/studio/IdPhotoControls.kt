package io.github.wa_otomia.darkroom.ui.studio

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.wa_otomia.darkroom.R
import io.github.wa_otomia.darkroom.core.ID_PHOTO_PRESETS
import io.github.wa_otomia.darkroom.core.IdBackground
import io.github.wa_otomia.darkroom.core.IdFeather
import io.github.wa_otomia.darkroom.core.IdPhotoLayout
import io.github.wa_otomia.darkroom.core.IdPhotoPreset
import io.github.wa_otomia.darkroom.core.idPhotoLayout
import io.github.wa_otomia.darkroom.core.idPhotoPreset
import io.github.wa_otomia.darkroom.core.showsEdgeWarning
import io.github.wa_otomia.darkroom.data.progress.ProgressUi
import io.github.wa_otomia.darkroom.ui.UserErrorMessage
import io.github.wa_otomia.darkroom.ui.components.AmberTrack
import io.github.wa_otomia.darkroom.ui.components.GhostButton
import io.github.wa_otomia.darkroom.ui.components.PaperButton
import io.github.wa_otomia.darkroom.ui.components.SectionLabel
import io.github.wa_otomia.darkroom.ui.components.darkroomTextFieldColors
import io.github.wa_otomia.darkroom.ui.theme.Amber
import io.github.wa_otomia.darkroom.ui.theme.Ink
import io.github.wa_otomia.darkroom.ui.theme.Paper
import io.github.wa_otomia.darkroom.ui.theme.PaperDim
import io.github.wa_otomia.darkroom.ui.theme.PaperFaint

// ---------------------------------------------------------------------------------------------
// Pure helpers (no Compose), kept here so the labels and the resource mapping stay in one place.
// ---------------------------------------------------------------------------------------------

/** "35×45" or "50.8×50.8": whole millimetres drop the decimal, others keep it (locale-stable). */
internal fun idPresetMm(preset: IdPhotoPreset): String = "${mmText(preset.widthMm)}×${mmText(preset.heightMm)}"

private fun mmText(mm: Double): String {
    val whole = Math.round(mm)
    return if (kotlin.math.abs(mm - whole) < 1e-9) whole.toString() else mm.toString()
}

/** Size label for the drawer header and the preview overlay, e.g. "35×45 mm · 1560×1040". */
internal fun idSheetLabel(preset: IdPhotoPreset, layout: IdPhotoLayout): String =
    "${idPresetMm(preset)} mm · ${layout.sheetWidth}×${layout.sheetHeight}"

@StringRes
internal fun idPresetPurposeRes(id: String): Int = when (id) {
    "33x48" -> R.string.studio_id_preset_33x48
    "25x35" -> R.string.studio_id_preset_25x35
    "22x32" -> R.string.studio_id_preset_22x32
    "35x49" -> R.string.studio_id_preset_35x49
    "2x2in" -> R.string.studio_id_preset_2x2in
    "50x70" -> R.string.studio_id_preset_50x70
    "30x40" -> R.string.studio_id_preset_30x40
    "24x30" -> R.string.studio_id_preset_24x30
    "40x60" -> R.string.studio_id_preset_40x60
    "35x50" -> R.string.studio_id_preset_35x50
    else -> R.string.studio_id_preset_35x45
}

@StringRes
internal fun idBackgroundNameRes(background: IdBackground): Int = when (background) {
    IdBackground.ORIGINAL -> R.string.studio_id_bg_original
    IdBackground.NAVY -> R.string.studio_id_bg_navy
    IdBackground.ID_BLUE -> R.string.studio_id_bg_id_blue
    IdBackground.RED -> R.string.studio_id_bg_red
    IdBackground.WHITE -> R.string.studio_id_bg_white
    IdBackground.LIGHT_GREY -> R.string.studio_id_bg_light_grey
    IdBackground.OFF_WHITE -> R.string.studio_id_bg_off_white
    IdBackground.PALE_BLUE -> R.string.studio_id_bg_pale_blue
}

@StringRes
internal fun idBackgroundUseRes(background: IdBackground): Int = when (background) {
    IdBackground.ORIGINAL -> R.string.studio_id_bg_original_use
    IdBackground.NAVY -> R.string.studio_id_bg_navy_use
    IdBackground.ID_BLUE -> R.string.studio_id_bg_id_blue_use
    IdBackground.RED -> R.string.studio_id_bg_red_use
    IdBackground.WHITE -> R.string.studio_id_bg_white_use
    IdBackground.LIGHT_GREY -> R.string.studio_id_bg_light_grey_use
    IdBackground.OFF_WHITE -> R.string.studio_id_bg_off_white_use
    IdBackground.PALE_BLUE -> R.string.studio_id_bg_pale_blue_use
}

@StringRes
internal fun idFeatherLabelRes(feather: IdFeather): Int = when (feather) {
    IdFeather.NONE -> R.string.studio_id_feather_none
    IdFeather.WEAK -> R.string.studio_id_feather_weak
    IdFeather.MEDIUM -> R.string.studio_id_feather_medium
    IdFeather.STRONG -> R.string.studio_id_feather_strong
}

// ---------------------------------------------------------------------------------------------
// Compose tab (EDID010-R02, EDID020-R05)
// ---------------------------------------------------------------------------------------------

/**
 * Size selector plus the notes that belong next to it: the chosen size's use, the edge warning
 * when the grid sits under 1 mm from the paper edge, and the issuing-authority disclaimer.
 * Side-effect free: it is composed twice by [StudioEqualHeightTabs].
 */
@Composable
internal fun IdPhotoSizeSection(
    presetId: String,
    enabled: Boolean,
    onPresetChange: (String) -> Unit,
) {
    val preset = idPhotoPreset(presetId)
    val layout = remember(preset) { idPhotoLayout(preset) }
    Column(Modifier.fillMaxWidth()) {
        IdPhotoSizeDropdown(presetId = preset.id, enabled = enabled, onChange = onPresetChange)
        StudioNote(stringResource(idPresetPurposeRes(preset.id)), topPadding = 0)
        if (layout.showsEdgeWarning()) {
            StudioNote(stringResource(R.string.studio_id_edge_warning), tone = Amber, topPadding = 0)
        }
        StudioNote(stringResource(R.string.studio_id_authority_note), topPadding = 0)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IdPhotoSizeDropdown(
    presetId: String,
    enabled: Boolean,
    onChange: (String) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val selected = idPhotoPreset(presetId)
    val selectedLayout = remember(selected) { idPhotoLayout(selected) }
    val perSheet = pluralStringResource(R.plurals.studio_id_per_sheet, selectedLayout.count, selectedLayout.count)
    ExposedDropdownMenuBox(expanded = open, onExpandedChange = { if (enabled) open = it }) {
        TextField(
            value = "${idPresetMm(selected)} mm · $perSheet",
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            singleLine = true,
            label = { Text(stringResource(R.string.studio_id_size), color = PaperDim) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(open) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp)
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled),
            colors = darkroomTextFieldColors(),
        )
        val menuMaxHeight = with(LocalDensity.current) {
            (LocalWindowInfo.current.containerSize.height / 2).toDp()
        }
        ExposedDropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            modifier = Modifier.heightIn(max = menuMaxHeight),
        ) {
            ID_PHOTO_PRESETS.forEach { preset ->
                val count = idPhotoLayout(preset).count
                DropdownMenuItem(
                    text = {
                        Column(Modifier.padding(vertical = 4.dp)) {
                            Text(
                                "${idPresetMm(preset)} mm · " +
                                    pluralStringResource(R.plurals.studio_id_per_sheet, count, count),
                                color = if (preset.id == selected.id) Amber else Paper,
                                fontSize = 14.sp,
                                lineHeight = 19.sp,
                            )
                            Text(
                                stringResource(idPresetPurposeRes(preset.id)),
                                color = PaperDim,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                            )
                        }
                    },
                    onClick = {
                        onChange(preset.id)
                        open = false
                    },
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Process tab (EDID040)
// ---------------------------------------------------------------------------------------------

/** Mask job chrome shown inside the section; null when no mask job runs for this photo. */
internal data class IdMaskJobUi(val ui: ProgressUi, val onCancel: () -> Unit)

/**
 * Replaces the AI generate block in ID mode: what the AI does, the cut-out button and its gate,
 * the running job, the last failure, the palette and the feather selector.
 * Side-effect free: every action is a callback.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun IdPhotoProcessSection(
    keySet: Boolean,
    hasMask: Boolean,
    background: IdBackground,
    feather: IdFeather,
    canCutOut: Boolean,
    maskJob: IdMaskJobUi?,
    maskError: String?,
    onCutOut: () -> Unit,
    onOpenSettings: () -> Unit,
    onBackground: (IdBackground) -> Unit,
    onFeather: (IdFeather) -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.fillMaxWidth()) {
            SectionLabel(stringResource(R.string.studio_id_section))
            StudioNote(stringResource(R.string.studio_id_explain), topPadding = 0)
            if (!keySet) {
                Text(
                    stringResource(R.string.grok_need_key),
                    color = Amber,
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
                )
                TextButton(onClick = onOpenSettings) {
                    Text(stringResource(R.string.nav_settings), color = Amber)
                }
            }
        }
        PaperButton(
            stringResource(if (hasMask) R.string.studio_id_recut else R.string.studio_id_cutout),
            enabled = canCutOut,
            onClick = onCutOut,
        )
        if (maskJob != null) {
            Column(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.studio_id_cutting),
                            color = Paper,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                        )
                        Text(
                            listOfNotNull(maskJob.ui.phaseLabel.ifBlank { null }, maskJob.ui.detail)
                                .joinToString(" · "),
                            color = PaperDim,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                        )
                    }
                    GhostButton(
                        stringResource(R.string.common_cancel),
                        fillMaxWidth = false,
                        onClick = maskJob.onCancel,
                    )
                }
                AmberTrack(ui = maskJob.ui, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
            }
        }
        if (!maskError.isNullOrBlank()) {
            UserErrorMessage(maskError)
        }

        val paletteEnabled = hasMask && maskJob == null
        Column(Modifier.fillMaxWidth()) {
            SectionLabel(stringResource(R.string.studio_id_background))
            if (!hasMask) {
                StudioNote(stringResource(R.string.studio_id_need_mask), topPadding = 0)
            }
            Column(Modifier.fillMaxWidth().selectableGroup()) {
                IdBackground.entries.forEach { option ->
                    IdBackgroundOption(
                        option = option,
                        selected = hasMask && option == background,
                        enabled = paletteEnabled,
                        onClick = { onBackground(option) },
                    )
                }
            }
            StudioNote(stringResource(R.string.studio_id_authority_note))
        }

        val featherEnabled = paletteEnabled && background.needsMask
        Column(Modifier.fillMaxWidth()) {
            SectionLabel(stringResource(R.string.studio_id_feather))
            FlowRow(
                Modifier.fillMaxWidth().selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                IdFeather.entries.forEach { option ->
                    IdFeatherChip(
                        label = stringResource(idFeatherLabelRes(option)),
                        selected = hasMask && option == feather,
                        enabled = featherEnabled,
                        onClick = { onFeather(option) },
                    )
                }
            }
        }
    }
}

/**
 * One palette row: swatch, name and use. The row is one radio button for TalkBack, which reads
 * the name, the use and the selected state together (EDID040-R04).
 */
@Composable
private fun IdBackgroundOption(
    option: IdBackground,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val state = stringResource(if (selected) R.string.a11y_state_selected else R.string.a11y_state_not_selected)
    val shape = RoundedCornerShape(2.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(shape)
            .background(if (selected) Amber.copy(alpha = if (enabled) 0.18f else 0.08f) else Color.Transparent)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .semantics(mergeDescendants = true) { stateDescription = state }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IdSwatch(option, enabled = enabled, selected = selected)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(idBackgroundNameRes(option)),
                color = if (enabled) Paper else PaperDim,
                fontSize = 14.sp,
                lineHeight = 19.sp,
            )
            Text(
                stringResource(idBackgroundUseRes(option)),
                color = PaperDim,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            )
        }
    }
}

/** Colour chip; "original" is drawn as a struck-through empty chip, never by colour alone. */
@Composable
private fun IdSwatch(option: IdBackground, enabled: Boolean, selected: Boolean) {
    val shape = RoundedCornerShape(2.dp)
    val alpha = if (enabled) 1f else 0.45f
    Box(
        Modifier
            .size(28.dp)
            .clip(shape)
            .background(option.argb?.let { Color(it).copy(alpha = alpha) } ?: Color.Transparent)
            .border(if (selected) 2.dp else 1.dp, if (selected) Amber else PaperFaint, shape),
    ) {
        if (option.argb == null) {
            val stroke = PaperDim.copy(alpha = PaperDim.alpha * alpha)
            Canvas(Modifier.matchParentSize()) {
                drawLine(stroke, Offset(size.width * 0.2f, size.height * 0.8f), Offset(size.width * 0.8f, size.height * 0.2f), 2.dp.toPx())
            }
        }
    }
}

@Composable
private fun IdFeatherChip(label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(2.dp)
    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clip(shape)
            .background(if (selected) Amber.copy(alpha = if (enabled) 0.9f else 0.4f) else Color.Transparent)
            .then(if (selected) Modifier else Modifier.border(1.dp, PaperFaint, shape))
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) Ink else if (enabled) Paper else PaperDim,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
    }
}
