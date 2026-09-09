package app.darkroom.android.ui.studio

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.darkroom.android.R
import app.darkroom.android.ui.components.DarkroomTextField
import app.darkroom.android.ui.components.GhostButton
import app.darkroom.android.ui.theme.Amber
import app.darkroom.android.ui.theme.Ink
import app.darkroom.android.ui.theme.Paper
import app.darkroom.android.ui.theme.PaperDim
import app.darkroom.android.ui.theme.PaperFaint
import app.darkroom.android.ui.theme.PaperHairline
import app.darkroom.android.ui.theme.SurfaceLow
import coil.compose.AsyncImage

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

@Composable
internal fun StudioTabBar(
    selected: StudioTab,
    onSelect: (StudioTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .background(SurfaceLow),
    ) {
        StudioTab.entries.forEach { tab ->
            val active = tab == selected
            val label = stringResource(tab.labelRes)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .semantics {
                        this.role = Role.Tab
                        this.selected = active
                    }
                    .clickable(role = Role.Tab) { onSelect(tab) }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = label,
                    color = if (active) Amber else PaperDim,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Box(
                    Modifier
                        .padding(top = 6.dp)
                        .width(24.dp)
                        .height(2.dp)
                        .background(if (active) Amber else Color.Transparent),
                )
            }
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

private val VersionTileShape = RoundedCornerShape(8.dp)
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
                .background(SurfaceLow)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) Amber else PaperHairline,
                    shape = VersionTileShape,
                ),
        )
        Text(
            text = label,
            color = if (selected) Amber else if (enabled) PaperDim else PaperFaint,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        )
    }
}

/**
 * Leaves this photo for a sibling in the same lineage. Nothing here is ever
 * "selected" — the chip navigates away — so it is a plain button, and the full
 * label goes into the description because the visible one may be clipped.
 */
@Composable
internal fun StudioNavChip(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val description = stringResource(R.string.studio_open_photo, label)
    Box(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .widthIn(max = chipMaxWidth())
            .clip(RoundedCornerShape(999.dp))
            .border(1.dp, PaperFaint, RoundedCornerShape(999.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (enabled) Paper else PaperDim,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
                .then(
                    if (enabled) {
                        Modifier.toggleable(
                            value = checked,
                            role = Role.Switch,
                            onValueChange = onCheckedChange,
                        )
                    } else {
                        Modifier
                    },
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.studio_watermark),
                color = if (enabled) Paper else PaperDim,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled,
                colors = SwitchDefaults.colors(checkedTrackColor = Amber),
            )
        }
        if (hint != null) {
            StudioNote(hint, tone = Amber, topPadding = 0)
        }
    }
}

@Composable
internal fun StudioNote(text: String, tone: Color = PaperDim, topPadding: Int = 6) {
    Text(
        text = text,
        color = tone,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        modifier = Modifier.fillMaxWidth().padding(top = topPadding.dp, bottom = 4.dp),
    )
}

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
    ) {
        IconButton(
            onClick = onEdit,
            enabled = enabled,
        ) {
            Icon(
                imageVector = if (hasPrompt) Icons.Filled.Edit else Icons.Outlined.Edit,
                contentDescription = stringResource(R.string.studio_extra_prompt_edit),
                tint = when {
                    !enabled -> PaperDim
                    hasPrompt -> Amber
                    else -> Paper
                },
            )
        }
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
) {
    val shownDegrees = Math.round(rotationDegrees) % 360
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ToggleChip(
            label = stringResource(R.string.studio_frame_portrait),
            active = !landscape,
            enabled = enabled,
            onClick = { onOrientation(false) },
        )
        ToggleChip(
            label = stringResource(R.string.studio_frame_landscape),
            active = landscape,
            enabled = enabled,
            onClick = { onOrientation(true) },
        )
        GlyphButton(
            glyph = "↺",
            description = stringResource(R.string.studio_rotate_left),
            enabled = enabled,
            onClick = { onRotate(-1) },
        )
        GlyphButton(
            glyph = "↻",
            description = stringResource(R.string.studio_rotate_right),
            enabled = enabled,
            onClick = { onRotate(1) },
        )
        if (onReset != null) {
            GhostButton(
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

@Composable
private fun ToggleChip(label: String, active: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(if (active) Amber.copy(alpha = if (enabled) 0.9f else 0.4f) else Color.Transparent)
            .then(if (active) Modifier else Modifier.border(1.dp, PaperFaint, RoundedCornerShape(2.dp)))
            .selectable(
                selected = active,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (active) Ink else if (enabled) Paper else PaperDim,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
    }
}

/**
 * The glyph grows with the font scale, so the box takes 48dp as a floor rather
 * than a fixed size and lets the content push it out from there.
 */
@Composable
private fun GlyphButton(glyph: String, description: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clip(RoundedCornerShape(2.dp))
            .border(1.dp, PaperFaint, RoundedCornerShape(2.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }
            .padding(horizontal = 12.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            glyph,
            color = if (enabled) Paper else PaperDim,
            fontSize = 20.sp,
            lineHeight = 26.sp,
        )
    }
}
