package io.github.wa_otomia.darkroom.ui.presets

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.wa_otomia.darkroom.R
import io.github.wa_otomia.darkroom.core.AI_PRESET_CATEGORIES
import io.github.wa_otomia.darkroom.core.AI_PRESET_NONE
import io.github.wa_otomia.darkroom.core.AiPreset
import io.github.wa_otomia.darkroom.core.EDIT_PRESETS
import io.github.wa_otomia.darkroom.core.isSeedPresetId
import io.github.wa_otomia.darkroom.data.settings.SettingsRepository
import io.github.wa_otomia.darkroom.ui.UserErrorDialog
import io.github.wa_otomia.darkroom.ui.components.DarkroomSnackbarHost
import io.github.wa_otomia.darkroom.ui.components.DarkroomTextField
import io.github.wa_otomia.darkroom.ui.components.GhostButton
import io.github.wa_otomia.darkroom.ui.components.PaperButton
import io.github.wa_otomia.darkroom.ui.components.ScreenHeader
import io.github.wa_otomia.darkroom.ui.components.SectionLabel
import io.github.wa_otomia.darkroom.ui.components.StatusBadge
import io.github.wa_otomia.darkroom.ui.theme.Amber
import io.github.wa_otomia.darkroom.ui.theme.Danger
import io.github.wa_otomia.darkroom.ui.theme.Ink
import io.github.wa_otomia.darkroom.ui.theme.MonoFont
import io.github.wa_otomia.darkroom.ui.theme.Paper
import io.github.wa_otomia.darkroom.ui.theme.PaperDim
import io.github.wa_otomia.darkroom.ui.theme.PaperFaint
import io.github.wa_otomia.darkroom.ui.theme.PaperHairline
import io.github.wa_otomia.darkroom.ui.theme.Room
import kotlinx.coroutines.launch

/** Mirrors the limits enforced by validateAiPresetFields. */
private const val TITLE_MAX = 80
private const val PROMPT_MAX = 4000

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PresetsScreen(settings: SettingsRepository) {
    val store by settings.presets.collectAsState()
    var title by rememberSaveable { mutableStateOf("") }
    var prompt by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("custom") }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var titleTouched by rememberSaveable { mutableStateOf(false) }
    var promptTouched by rememberSaveable { mutableStateOf(false) }
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    var pendingDeleteId by rememberSaveable { mutableStateOf<String?>(null) }
    var menuFor by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var editorError by remember { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val savedText = stringResource(R.string.preset_saved)
    val restoredText = stringResource(R.string.lib_presets_restored)
    val undoLabel = stringResource(R.string.lib_action_undo)

    val trimmedTitle = title.trim()
    val trimmedPrompt = prompt.trim()
    val titleError = when {
        trimmedTitle.length > TITLE_MAX -> R.string.error_preset_title_too_long
        titleTouched && trimmedTitle.isEmpty() -> R.string.error_preset_title_required
        else -> null
    }
    val promptError = when {
        trimmedPrompt.length > PROMPT_MAX -> R.string.error_preset_prompt_too_long
        promptTouched && trimmedPrompt.isEmpty() -> R.string.error_preset_prompt_required
        else -> null
    }
    val canSave = trimmedTitle.isNotEmpty() && trimmedTitle.length <= TITLE_MAX &&
        trimmedPrompt.isNotEmpty() && trimmedPrompt.length <= PROMPT_MAX

    val editingOriginal = editingId?.let { id -> store.presets.firstOrNull { it.id == id } }
    val dirty = if (editingOriginal == null) {
        title.isNotBlank() || prompt.isNotBlank() || category != "custom"
    } else {
        title != editingOriginal.title || prompt != editingOriginal.prompt || category != editingOriginal.category
    }

    fun openEditor(preset: AiPreset?) {
        if (preset == null) {
            editingId = null
            title = ""
            prompt = ""
            category = "custom"
        } else {
            editingId = preset.id
            title = preset.title
            prompt = preset.prompt
            category = preset.category
        }
        titleTouched = false
        promptTouched = false
        confirmDiscard = false
        error = null
        editorError = null
        showEditor = true
    }

    fun closeEditor() {
        confirmDiscard = false
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            showEditor = false
            editorError = null
        }
    }

    /** Cancel and swipe-to-dismiss both go through here so a draft is never dropped silently. */
    fun requestClose() {
        if (dirty) confirmDiscard = true else closeEditor()
    }

    // No snackbar: the badge animating in on the card is the feedback, right where the tap landed.
    fun setDefault(id: String) {
        runCatching { settings.setLastPreset(id) }.onFailure { error = it.message }
    }

    fun restoreBuiltin(preset: AiPreset) {
        val seed = EDIT_PRESETS.firstOrNull { it.id == preset.id } ?: return
        runCatching { settings.updatePreset(seed.id, seed.label, seed.prompt, seed.category) }
            .onSuccess {
                scope.launch {
                    val result = snackbar.showSnackbar(
                        message = restoredText,
                        actionLabel = undoLabel,
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        runCatching {
                            settings.updatePreset(preset.id, preset.title, preset.prompt, preset.category)
                        }.onFailure { error = it.message }
                    }
                }
            }
            .onFailure { error = it.message }
    }

    val defaultTitle = store.presets.firstOrNull { it.id == store.lastSelectedId }?.title
        ?: stringResource(R.string.lib_presets_default_none)

    Scaffold(
        // containerColor/contentColor default to background/onBackground, i.e. Room/Paper.
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { DarkroomSnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            ScreenHeader(
                eyebrow = stringResource(R.string.presets_eyebrow),
                title = stringResource(R.string.presets_title),
            )
            Text(stringResource(R.string.presets_desc), style = MaterialTheme.typography.bodyMedium, color = PaperDim, modifier = Modifier.padding(top = 8.dp, bottom = 16.dp))
            SectionLabel(stringResource(R.string.default_preset))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.lib_presets_default_current, defaultTitle),
                    color = Paper,
                    fontFamily = MonoFont,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                )
                if (store.lastSelectedId != AI_PRESET_NONE) {
                    GhostButton(stringResource(R.string.lib_presets_default_clear), fillMaxWidth = false) {
                        setDefault(AI_PRESET_NONE)
                    }
                }
            }
            Text(
                stringResource(R.string.lib_presets_default_hint),
                color = PaperDim,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 6.dp, bottom = 16.dp),
            )
            PaperButton(stringResource(R.string.add_preset)) { openEditor(null) }

            AI_PRESET_CATEGORIES.forEach { cat ->
                val items = store.presets.filter { it.category == cat }
                if (items.isEmpty()) return@forEach
                SectionLabel(categoryLabel(cat))
                items.forEach { preset ->
                    val builtin = isSeedPresetId(preset.id)
                    PresetCard(
                        preset = preset,
                        isDefault = store.lastSelectedId == preset.id,
                        canRestore = builtin && differsFromSeed(preset),
                        canDelete = !builtin,
                        menuOpen = menuFor == preset.id,
                        onMenuOpen = { menuFor = preset.id },
                        onMenuDismiss = { menuFor = null },
                        onEdit = { openEditor(preset) },
                        onSetDefault = { setDefault(preset.id) },
                        onRestore = { restoreBuiltin(preset) },
                        onDelete = { pendingDeleteId = preset.id },
                    )
                }
            }
        }
    }

    if (showEditor) {
        ModalBottomSheet(
            onDismissRequest = {
                // The gesture already hid the sheet; keep it out of the way until the user decides.
                if (dirty) confirmDiscard = true else closeEditor()
            },
            sheetState = sheetState,
            // Kept explicit: the sheet default is surfaceContainerLow (#141210), which would
            // lift the editor off the page background and change the current look.
            containerColor = Room,
            contentColor = Paper,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
                Text(
                    if (editingId == null) stringResource(R.string.add_preset) else stringResource(R.string.common_edit),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                CountedField(
                    label = stringResource(R.string.preset_title),
                    value = title,
                    count = trimmedTitle.length,
                    max = TITLE_MAX,
                    errorRes = titleError,
                ) {
                    title = it
                    titleTouched = true
                }
                SectionLabel(stringResource(R.string.lib_presets_category))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 16.dp),
                ) {
                    AI_PRESET_CATEGORIES.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(categoryLabel(cat)) },
                            colors = FilterChipDefaults.filterChipColors(
                                labelColor = Paper,
                                selectedContainerColor = Paper,
                                selectedLabelColor = Ink,
                                selectedLeadingIconColor = Ink,
                            ),
                        )
                    }
                }
                CountedField(
                    label = stringResource(R.string.preset_prompt),
                    value = prompt,
                    count = trimmedPrompt.length,
                    max = PROMPT_MAX,
                    errorRes = promptError,
                    minLines = 4,
                ) {
                    prompt = it
                    promptTouched = true
                }
                PaperButton(stringResource(R.string.common_save), enabled = canSave) {
                    val id = editingId
                    runCatching {
                        if (id == null) settings.createPreset(title, prompt, category)
                        else settings.updatePreset(id, title, prompt, category)
                    }.onSuccess {
                        editorError = null
                        closeEditor()
                        scope.launch { snackbar.showSnackbar(savedText) }
                    }.onFailure { editorError = it.message }
                }
                Box(Modifier.padding(top = 8.dp)) {
                    GhostButton(stringResource(R.string.common_cancel)) { requestClose() }
                }
            }
        }
    }

    if (showEditor && confirmDiscard) {
        AlertDialog(
            onDismissRequest = {
                confirmDiscard = false
                scope.launch { sheetState.show() }
            },
            title = { Text(stringResource(R.string.lib_presets_discard_title)) },
            text = { Text(stringResource(R.string.lib_presets_discard_desc)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    closeEditor()
                }) { Text(stringResource(R.string.lib_presets_discard_confirm), color = Danger) }
            },
            dismissButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    scope.launch { sheetState.show() }
                }) { Text(stringResource(R.string.lib_presets_keep_editing)) }
            },
        )
    }

    val pendingDelete = pendingDeleteId?.let { id -> store.presets.firstOrNull { it.id == id } }
    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text(stringResource(R.string.delete_preset_title)) },
            text = { Text(pendingDelete.title) },
            confirmButton = {
                TextButton(onClick = {
                    runCatching { settings.deletePreset(pendingDelete.id) }
                        .onFailure { error = it.message }
                    pendingDeleteId = null
                }) { Text(stringResource(R.string.common_delete), color = Danger) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }

    UserErrorDialog(error, onDismiss = { error = null })
    UserErrorDialog(editorError, onDismiss = { editorError = null })
}

@Composable
private fun categoryLabel(id: String): String = stringResource(
    when (id) {
        "portrait" -> R.string.category_portrait
        "creative" -> R.string.category_creative
        else -> R.string.category_custom
    },
)

/** Built-in presets are editable, so offer a way back only once they have drifted. */
private fun differsFromSeed(preset: AiPreset): Boolean {
    val seed = EDIT_PRESETS.firstOrNull { it.id == preset.id } ?: return false
    return preset.title != seed.label || preset.prompt != seed.prompt || preset.category != seed.category
}

@Composable
private fun CountedField(
    label: String,
    value: String,
    count: Int,
    max: Int,
    errorRes: Int?,
    minLines: Int = 1,
    onValueChange: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, color = PaperDim, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Text(
                stringResource(R.string.lib_presets_char_count, count, max),
                color = if (count > max) Danger else PaperDim,
                fontFamily = MonoFont,
                fontSize = 12.sp,
            )
        }
        DarkroomTextField(
            label = "",
            value = value,
            minLines = minLines,
            onValueChange = onValueChange,
        )
        if (errorRes != null) {
            Text(
                stringResource(errorRes),
                color = Danger,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
    }
}

@Composable
private fun PresetCard(
    preset: AiPreset,
    isDefault: Boolean,
    canRestore: Boolean,
    canDelete: Boolean,
    menuOpen: Boolean,
    onMenuOpen: () -> Unit,
    onMenuDismiss: () -> Unit,
    onEdit: () -> Unit,
    onSetDefault: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
) {
    val editLabel = stringResource(R.string.common_edit)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            // Card frame: decorative, so a hairline. The pill inside it carries the affordance
            // weight, which keeps the control more prominent than its container.
            .border(1.dp, if (isDefault) Amber else PaperHairline, RoundedCornerShape(2.dp))
            .clickable(onClickLabel = editLabel, onClick = onEdit)
            .padding(start = 12.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                preset.title,
                color = Paper,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(end = 8.dp),
            )
            Box {
                IconButton(onClick = onMenuOpen) {
                    Icon(
                        Icons.Outlined.MoreVert,
                        contentDescription = stringResource(R.string.lib_presets_card_actions),
                        tint = PaperDim,
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = onMenuDismiss) {
                    DropdownMenuItem(
                        text = { Text(editLabel) },
                        onClick = {
                            onMenuDismiss()
                            onEdit()
                        },
                    )
                    if (canRestore) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.lib_presets_restore_builtin)) },
                            onClick = {
                                onMenuDismiss()
                                onRestore()
                            },
                        )
                    }
                    if (canDelete) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.common_delete), color = Danger) },
                            onClick = {
                                onMenuDismiss()
                                onDelete()
                            },
                        )
                    }
                }
            }
        }
        Text(
            preset.prompt,
            color = PaperDim,
            fontSize = 13.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp, end = 8.dp),
        )
        DefaultMarker(
            isDefault = isDefault,
            onSetDefault = onSetDefault,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

/**
 * Replaces the old "set as default" text button: the state and the control share one slot.
 *
 * The badge is kept in the composition with only [visible] flipping, so becoming the default
 * plays [StatusBadge]'s enter animation. That animation *is* the confirmation, which is why
 * setting a default no longer raises a snackbar.
 */
@Composable
private fun DefaultMarker(isDefault: Boolean, onSetDefault: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.heightIn(min = 48.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        StatusBadge(visible = isDefault, text = stringResource(R.string.default_badge))
        if (!isDefault) {
            SetDefaultPill(onSetDefault)
        }
    }
}

/** Outlined control, so its border is the sole affordance and stays at PaperFaint weight. */
@Composable
private fun SetDefaultPill(onSetDefault: () -> Unit) {
    val shape = RoundedCornerShape(999.dp)
    Row(
        Modifier
            .heightIn(min = 48.dp)
            .clip(shape)
            .border(1.dp, PaperFaint, shape)
            .clickable(onClick = onSetDefault)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Outlined.RadioButtonUnchecked,
            contentDescription = null,
            tint = PaperDim,
            modifier = Modifier.size(16.dp),
        )
        Text(
            stringResource(R.string.set_as_default),
            color = PaperDim,
            fontSize = 12.sp,
            maxLines = 1,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}
