package io.github.wa_otomia.darkroom.ui.studio

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.BrandingWatermark
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Rotate90DegreesCcw
import androidx.compose.material.icons.outlined.Rotate90DegreesCw
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import io.github.wa_otomia.darkroom.R
import io.github.wa_otomia.darkroom.ui.components.ChipShape
import io.github.wa_otomia.darkroom.ui.components.ChipVisualHeight
import io.github.wa_otomia.darkroom.ui.components.DarkroomTextField
import io.github.wa_otomia.darkroom.ui.components.IconCircleButton
import io.github.wa_otomia.darkroom.ui.components.MinTouchTarget
import io.github.wa_otomia.darkroom.ui.components.SelectPill
import io.github.wa_otomia.darkroom.ui.components.TonalButton
import io.github.wa_otomia.darkroom.ui.components.darkroomSwitchColors
import io.github.wa_otomia.darkroom.ui.components.pressScale
import io.github.wa_otomia.darkroom.ui.theme.Amber
import io.github.wa_otomia.darkroom.ui.theme.Paper
import io.github.wa_otomia.darkroom.ui.theme.PaperDim
import io.github.wa_otomia.darkroom.ui.theme.PaperFaint
import io.github.wa_otomia.darkroom.ui.theme.Surface2
import io.github.wa_otomia.darkroom.ui.theme.Surface3

internal enum class StudioTab(val key: String, val labelRes: Int) {
    Compose("compose", R.string.studio_tab_compose),
    Process("process", R.string.studio_tab_process),
    Output("output", R.string.studio_tab_output),
    ;

    companion object {
        fun fromKey(key: String?): StudioTab =
            entries.find { it.key == key } ?: Compose
    }
}

/** Shared by the three tab panels so switching tabs does not resize the drawer. */
internal fun studioEqualPanelHeight(measuredHeights: IntArray, capPx: Int): Int {
    if (measuredHeights.isEmpty() || capPx <= 0) return 0
    var tallest = 0
    for (h in measuredHeights) if (h > tallest) tallest = h
    return tallest.coerceAtMost(capPx)
}

@Composable
internal fun StudioEqualHeightTabs(
    selected: StudioTab,
    maxHeight: Dp,
    modifier: Modifier = Modifier,
    compose: @Composable () -> Unit,
    process: @Composable () -> Unit,
    output: @Composable () -> Unit,
) {
    val maxPx = with(LocalDensity.current) { maxHeight.roundToPx() }
    SubcomposeLayout(modifier.fillMaxWidth()) { constraints ->
        val width = constraints.maxWidth
        val measureConstraints = Constraints(
            minWidth = width,
            maxWidth = width,
            minHeight = 0,
            maxHeight = Constraints.Infinity,
        )
        val heights = IntArray(StudioTab.entries.size)
        StudioTab.entries.forEachIndexed { i, tab ->
            val placeables = subcompose("measure-$tab") {
                Box(Modifier.fillMaxWidth()) {
                    when (tab) {
                        StudioTab.Compose -> compose()
                        StudioTab.Process -> process()
                        StudioTab.Output -> output()
                    }
                }
            }.map { it.measure(measureConstraints) }
            heights[i] = placeables.maxOfOrNull { it.height } ?: 0
        }
        val height = studioEqualPanelHeight(heights, maxPx).coerceAtMost(constraints.maxHeight)
        val selectedIndex = StudioTab.entries.indexOf(selected)
        val needsScroll = heights.getOrElse(selectedIndex) { 0 } > height
        val shown = subcompose("show-$selected-$height-$needsScroll") {
            val scroll = rememberScrollState()
            Column(
                Modifier
                    .fillMaxWidth()
                    .then(if (needsScroll) Modifier.verticalScroll(scroll) else Modifier),
            ) {
                when (selected) {
                    StudioTab.Compose -> compose()
                    StudioTab.Process -> process()
                    StudioTab.Output -> output()
                }
            }
        }.map {
            it.measure(
                Constraints(
                    minWidth = width,
                    maxWidth = width,
                    minHeight = height,
                    maxHeight = height,
                ),
            )
        }
        layout(width, height) {
            shown.forEach { it.place(0, 0) }
        }
    }
}

/**
 * A long filename would otherwise stretch the chip row, so the label is clipped.
 * The cap follows the font scale, which keeps the built-in labels ("Original",
 * "Edit 1") whole at 2x while still clipping filenames.
 */
@Composable
private fun chipMaxWidth() = 160.dp * LocalDensity.current.fontScale.coerceIn(1f, 2f)

/** 12dp thumbnail corners (D4); the selected one carries the only outline, 2dp Amber. */
private val VersionTileShape = RoundedCornerShape(12.dp)
private val VersionTileSize = 72.dp

/**
 * One entry in the single-choice strip that picks which version of *this* photo
 * the cropper shows. Carries selection semantics, so TalkBack reports which one
 * is loaded.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun StudioVersionTile(
    selected: Boolean,
    label: String,
    model: Any?,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    Column(
        modifier = Modifier
            .width(VersionTileSize)
            .semantics {
                this.role = Role.RadioButton
                this.selected = selected
            }
            .combinedClickable(
                enabled = enabled,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AsyncImage(
            model = model,
            contentDescription = label,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(VersionTileSize)
                .clip(VersionTileShape)
                .background(Surface2)
                .then(
                    if (selected) Modifier.border(2.dp, Amber, VersionTileShape) else Modifier,
                ),
        )
        Text(
            text = label,
            // Amber 8.10:1 and PaperDim 5.16:1 on Surface1.
            color = if (selected) Amber else if (enabled) PaperDim else PaperFaint,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            // Two lines so "Original" stays whole at 200% font scale; long names still ellipsize.
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        )
    }
}

/**
 * Leaves this photo for a sibling in the same lineage. Nothing here is ever
 * "selected" — the chip navigates away — so it is a plain button, and the full
 * label goes into the description because the visible one may be clipped.
 * Drawn as a 36dp Surface2 pill (Paper 13.40:1) inside a 48dp touch box.
 */
@Composable
internal fun StudioNavChip(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val description = stringResource(R.string.studio_open_photo, label)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .heightIn(min = MinTouchTarget)
            .widthIn(max = chipMaxWidth())
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .pressScale(interaction)
                .heightIn(min = ChipVisualHeight)
                .clip(ChipShape)
                .background(if (pressed && enabled) Surface3 else Surface2)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                color = if (enabled) Paper else PaperDim,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Label of a Studio switch row: 15sp Medium like a list row title, wraps at large font sizes. */
@Composable
private fun StudioSwitchLabel(text: String, enabled: Boolean, modifier: Modifier = Modifier) {
    Text(
        text,
        color = if (enabled) Paper else PaperDim,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier,
    )
}

@Composable
internal fun StudioSwitchRow(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    hint: String? = null,
    onCheckedChange: (Boolean) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                // Kept when disabled too, so TalkBack still hears the on/off state, the Switch
                // role and "disabled"; a disabled toggleable ignores taps.
                .toggleable(
                    value = checked,
                    enabled = enabled,
                    role = Role.Switch,
                    onValueChange = onCheckedChange,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StudioSwitchLabel(label, enabled, Modifier.weight(1f).padding(end = 12.dp))
            Switch(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled,
                colors = darkroomSwitchColors(),
            )
        }
        if (hint != null) {
            StudioNote(hint, tone = Amber, topPadding = 0)
        }
    }
}

@Composable
internal fun StudioWatermarkRow(
    checked: Boolean,
    enabled: Boolean,
    hint: String?,
    onCheckedChange: (Boolean) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                // Kept when disabled too (always so in ID photo mode), so TalkBack still hears
                // the on/off state, the Switch role and "disabled"; a disabled toggleable
                // ignores taps.
                .toggleable(
                    value = checked,
                    enabled = enabled,
                    role = Role.Switch,
                    onValueChange = onCheckedChange,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Decorative; the label next to it names the row.
            Icon(
                Icons.AutoMirrored.Outlined.BrandingWatermark,
                contentDescription = null,
                tint = PaperDim,
                modifier = Modifier.padding(end = 12.dp).size(20.dp),
            )
            StudioSwitchLabel(
                stringResource(R.string.studio_watermark),
                enabled,
                Modifier.weight(1f).padding(end = 12.dp),
            )
            Switch(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled,
                colors = darkroomSwitchColors(),
            )
        }
        if (hint != null) {
            StudioNote(hint, tone = Amber, topPadding = 0)
        }
    }
}

/** 12sp note under a control; PaperDim 5.16:1 on Surface1, Amber 8.10:1 for warnings. */
@Composable
internal fun StudioNote(text: String, tone: Color = PaperDim, topPadding: Int = 6) {
    Text(
        text = text,
        color = tone,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        modifier = Modifier.fillMaxWidth().padding(top = topPadding.dp, bottom = 4.dp),
    )
}

/**
 * The extra-prompt button next to the preset dropdown: a round tonal button, Amber icon once a
 * prompt is set, with the prompt's first line beside it.
 */
@Composable
internal fun StudioExtraPromptRow(
    prompt: String,
    enabled: Boolean,
    onEdit: () -> Unit,
) {
    val hasPrompt = prompt.isNotBlank()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(start = 8.dp),
    ) {
        IconCircleButton(
            icon = if (hasPrompt) Icons.Filled.Edit else Icons.Outlined.Edit,
            contentDescription = stringResource(R.string.studio_extra_prompt_edit),
            onClick = onEdit,
            enabled = enabled,
            contentColor = if (hasPrompt) Amber else Paper,
        )
        if (hasPrompt) {
            Text(
                prompt.trim(),
                color = PaperDim,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 140.dp),
            )
        }
    }
}

@Composable
internal fun ExtraPromptDialog(
    value: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var draft by remember { mutableStateOf(value) }
    LaunchedEffect(value) { draft = value }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.studio_extra_prompt)) },
        text = {
            Column {
                DarkroomTextField(
                    label = "",
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = stringResource(R.string.prompt_placeholder),
                    minLines = 3,
                )
                StudioNote(stringResource(R.string.studio_preset_stacks), topPadding = 0)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(draft) }) {
                Text(stringResource(R.string.common_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}

/**
 * Frame orientation plus the two quarter-turn buttons.
 *
 * [rotationDegrees] is the live angle, which a two-finger twist in the cropper may
 * have left off a right angle; [onRotate] steps it by whole quarters.
 *
 * Orientation and rotate/reset share one [FlowRow] so they sit on a single line
 * when width allows and wrap on narrow or large-font layouts.
 *
 * [orientationSelected] false shows neither orientation pill as selected while both stay
 * usable (ID photo mode, where tapping one leaves the mode).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FramingControls(
    landscape: Boolean,
    rotationDegrees: Float,
    enabled: Boolean,
    onOrientation: (Boolean) -> Unit,
    onRotate: (Int) -> Unit,
    onReset: (() -> Unit)? = null,
    orientationSelected: Boolean = true,
) {
    val shownDegrees = Math.round(rotationDegrees) % 360
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // The two orientation pills form one radio group; they wrap on their own at large
        // font sizes.
        FlowRow(
            Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SelectPill(
                selected = orientationSelected && !landscape,
                label = stringResource(R.string.studio_frame_portrait),
                onClick = { onOrientation(false) },
                enabled = enabled,
            )
            SelectPill(
                selected = orientationSelected && landscape,
                label = stringResource(R.string.studio_frame_landscape),
                onClick = { onOrientation(true) },
                enabled = enabled,
            )
        }
        // Counter-clockwise / clockwise quarter turns. Not auto-mirrored: the turn direction
        // is the same in RTL.
        IconCircleButton(
            icon = Icons.Outlined.Rotate90DegreesCcw,
            contentDescription = stringResource(R.string.studio_rotate_left),
            onClick = { onRotate(-1) },
            enabled = enabled,
        )
        IconCircleButton(
            icon = Icons.Outlined.Rotate90DegreesCw,
            contentDescription = stringResource(R.string.studio_rotate_right),
            onClick = { onRotate(1) },
            enabled = enabled,
        )
        if (onReset != null) {
            TonalButton(
                text = stringResource(R.string.studio_reset),
                enabled = enabled,
                fillMaxWidth = false,
                onClick = onReset,
            )
        }
        if (shownDegrees != 0) {
            Text(
                text = stringResource(R.string.studio_rotation_state, shownDegrees),
                color = Amber,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.align(Alignment.CenterVertically).padding(start = 4.dp),
            )
        }
    }
}
