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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.darkroom.android.R
import app.darkroom.android.ui.components.GhostButton
import app.darkroom.android.ui.theme.Amber
import app.darkroom.android.ui.theme.Ink
import app.darkroom.android.ui.theme.Paper
import app.darkroom.android.ui.theme.PaperDim
import app.darkroom.android.ui.theme.PaperFaint

/**
 * A long filename would otherwise stretch the chip row, so the label is clipped.
 * The cap follows the font scale, which keeps the built-in labels ("Original",
 * "Edit 1") whole at 2x while still clipping filenames.
 */
@Composable
private fun chipMaxWidth() = 160.dp * LocalDensity.current.fontScale.coerceIn(1f, 2f)

/**
 * One entry in the single-choice strip that picks which version of *this* photo
 * the cropper shows. Carries selection semantics, so TalkBack reports which one
 * is loaded.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun StudioVersionChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    Box(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .widthIn(max = chipMaxWidth())
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) Paper else Color.Transparent)
            .then(if (selected) Modifier else Modifier.border(1.dp, PaperFaint, RoundedCornerShape(999.dp)))
            .semantics {
                this.role = Role.RadioButton
                this.selected = selected
            }
            .combinedClickable(
                enabled = enabled,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) Ink else if (enabled) Paper else PaperDim,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
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

/**
 * Frame orientation plus the two quarter-turn buttons.
 *
 * [rotationDegrees] is the live angle, which a two-finger twist in the cropper may
 * have left off a right angle; [onRotate] steps it by whole quarters.
 *
 * Both rows flow: at large font scales the labels outgrow one line, and wrapping
 * onto a second is better than clipping a control.
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
    Column(Modifier.fillMaxWidth()) {
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
        }
        FlowRow(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
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
