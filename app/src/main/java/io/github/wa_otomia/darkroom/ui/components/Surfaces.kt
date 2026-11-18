package io.github.wa_otomia.darkroom.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.wa_otomia.darkroom.R
import io.github.wa_otomia.darkroom.ui.theme.Amber
import io.github.wa_otomia.darkroom.ui.theme.AmberSoft
import io.github.wa_otomia.darkroom.ui.theme.CardGlow
import io.github.wa_otomia.darkroom.ui.theme.CardShape
import io.github.wa_otomia.darkroom.ui.theme.DarkroomMotion
import io.github.wa_otomia.darkroom.ui.theme.Ink
import io.github.wa_otomia.darkroom.ui.theme.LocalSystemAnimationsEnabled
import io.github.wa_otomia.darkroom.ui.theme.PanelShape
import io.github.wa_otomia.darkroom.ui.theme.Paper
import io.github.wa_otomia.darkroom.ui.theme.PaperDim
import io.github.wa_otomia.darkroom.ui.theme.PaperFaint
import io.github.wa_otomia.darkroom.ui.theme.RadiusIconTile
import io.github.wa_otomia.darkroom.ui.theme.Surface1
import io.github.wa_otomia.darkroom.ui.theme.Surface2
import io.github.wa_otomia.darkroom.ui.theme.Surface3

/** Smallest touch target we ship, per the Material accessibility guidance. */
internal val MinTouchTarget = 48.dp

/** Visual height of chips and pills; the touch box around them stays [MinTouchTarget]. */
internal val ChipVisualHeight = 36.dp

/**
 * Shape for controls whose height is a 48dp minimum but can grow when large
 * font sizes make their label wrap. A fixed 24dp radius is fully round at
 * 48dp and turns into a rounded rectangle when taller, so a wrapped label
 * never runs into the curve the way it would with a 50% stadium.
 */
internal val ControlShape: Shape = RoundedCornerShape(24.dp)

/** Same idea as [ControlShape] for the 36dp chips. */
internal val ChipShape: Shape = RoundedCornerShape(18.dp)

/** Gap between the segmented track and the selected segment block. */
private val SegmentInset = 3.dp
private val SegmentShape: Shape = RoundedCornerShape(21.dp)

/** Start padding of a [ListRow] (and of its text when it has no icon). */
val ListRowPadding: Dp = 14.dp
private val ListRowIconTile = 34.dp
private val ListRowGap = 12.dp

/** Where the text of a [ListRow] with a leading icon starts: 14 + 34 + 12. */
val ListRowTextIndent: Dp = ListRowPadding + ListRowIconTile + ListRowGap

// ---------------------------------------------------------------------------
// Motion
// ---------------------------------------------------------------------------

/**
 * Scales the element to [DarkroomMotion.PressedScale] while [interactionSource]
 * reports a press, over [DarkroomMotion.StateChangeMillis] (SYUI030-R04).
 *
 * Layout size and the touch target are unchanged: apply it *after* the
 * `clickable`/`selectable` modifier and before `clip`/`background`, so only the
 * drawn pill shrinks. When the system animator duration scale is 0
 * ([LocalSystemAnimationsEnabled] is false) it does nothing at all, and the
 * press is shown by the caller's fill change alone.
 */
fun Modifier.pressScale(interactionSource: InteractionSource): Modifier = composed {
    val animate = LocalSystemAnimationsEnabled.current
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && animate) DarkroomMotion.PressedScale else 1f,
        animationSpec = DarkroomMotion.stateChange(),
        label = "pressScale",
    )
    if (animate) {
        Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    } else {
        Modifier
    }
}

// ---------------------------------------------------------------------------
// Page structure
// ---------------------------------------------------------------------------

/**
 * The one page heading of a top-level screen: `headlineLarge` (28sp Bold,
 * Paper) with an optional [supporting] note (14sp, PaperDim) on the same
 * baseline, e.g. "Gallery  12 photos".
 *
 * Both parts are one paragraph, so at large font sizes the note wraps onto
 * the next line instead of being clipped. Marked as a heading for TalkBack.
 * Paper on Room 15.67:1; PaperDim on Room 5.23:1.
 */
@Composable
fun PageTitle(
    title: String,
    supporting: String? = null,
    modifier: Modifier = Modifier,
) {
    val text = buildAnnotatedString {
        append(title)
        if (!supporting.isNullOrBlank()) {
            append(' ')
            withStyle(SpanStyle(color = PaperDim, fontSize = 14.sp, fontWeight = FontWeight.Normal)) {
                append(' ')
                append(supporting)
            }
        }
    }
    Text(
        text = text,
        style = MaterialTheme.typography.headlineLarge,
        color = Paper,
        modifier = modifier.semantics { heading() },
    )
}

/**
 * A free-standing card: [Surface1], 22dp corners, no outline (SYUI020-R02).
 *
 * Pass [border] only for a selected/default state (1dp Amber) — never as a
 * container edge. [brush] paints over [color], e.g. [cardGlowBrush] for the
 * FTP hero card. With [onClick] the whole card is one button.
 * Paper on Surface1 14.70:1, PaperDim 5.16:1, Amber 8.10:1.
 */
@Composable
fun DarkroomCard(
    modifier: Modifier = Modifier,
    contentPadding: Dp = 16.dp,
    color: Color = Surface1,
    brush: Brush? = null,
    border: BorderStroke? = null,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(12.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(color)
            .then(if (brush != null) Modifier.background(brush) else Modifier)
            .then(if (border != null) Modifier.border(border, CardShape) else Modifier)
            .then(
                if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier,
            )
            .padding(contentPadding),
        verticalArrangement = verticalArrangement,
    ) {
        content()
    }
}

/**
 * Warm glow for a hero card: [CardGlow] at the top-start corner fading to
 * [Surface1] at 70%, mirrored in RTL. Paper on CardGlow 12.52:1, PaperDim
 * 4.79:1, Amber 6.90:1, so any text that is legal on Surface1 stays legal.
 */
@Composable
fun cardGlowBrush(): Brush {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    return Brush.linearGradient(
        0f to CardGlow,
        0.7f to Surface1,
        start = if (rtl) Offset(Float.POSITIVE_INFINITY, 0f) else Offset.Zero,
        end = if (rtl) Offset(0f, Float.POSITIVE_INFINITY) else Offset.Infinite,
    )
}

/**
 * A group of [ListRow]s on one [Surface1] card with 22dp corners, clipped so
 * row ripples and highlights follow the corners. Separate rows with
 * [GroupDivider]. [border] is for the settings deep-link highlight only.
 */
@Composable
fun CardGroup(
    modifier: Modifier = Modifier,
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(Surface1)
            .then(if (border != null) Modifier.border(border, CardShape) else Modifier),
    ) {
        content()
    }
}

/**
 * 1dp [Surface2] line between rows of a [CardGroup]. The default indent lines
 * it up with the text of a [ListRow] that has a leading icon; pass
 * [ListRowPadding] for rows without one. Decorative, exempt from 3:1.
 */
@Composable
fun GroupDivider(
    modifier: Modifier = Modifier,
    startIndent: Dp = ListRowTextIndent,
) {
    Box(
        modifier
            .padding(start = startIndent)
            .fillMaxWidth()
            .height(1.dp)
            .background(Surface2),
    )
}

/**
 * 34dp [Surface3] tile with an 11dp radius holding an Amber icon (6.52:1).
 * Used by [ListRow]; exposed for rows that need a custom layout.
 */
@Composable
fun IconTile(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = Amber,
) {
    Box(
        modifier
            .size(ListRowIconTile)
            .clip(RoundedCornerShape(RadiusIconTile))
            .background(Surface3),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
    }
}

/**
 * One row of a [CardGroup]: optional [IconTile], [title] (15sp Medium Paper,
 * 14.70:1 on Surface1) over optional [supporting] (13sp PaperDim, 5.16:1),
 * and an end-aligned [trailing] slot (switch, value, [ListRowChevron]).
 *
 * Min height 56dp, 14dp horizontal padding. With [onClick] the row is one
 * merged button; without it the row is still merged so TalkBack reads title
 * and summary together. Text wraps at any font size. Add semantics such as an
 * expanded/collapsed `stateDescription`, or an [AmberSoft] highlight
 * background, through [modifier]: it is applied to the same node, outside the
 * padding.
 */
@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    leadingIcon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(
                if (onClick != null) {
                    Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                } else {
                    Modifier.semantics(mergeDescendants = true) {}
                },
            )
            .padding(horizontal = ListRowPadding, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ListRowGap),
    ) {
        if (leadingIcon != null) {
            IconTile(leadingIcon, tint = if (enabled) Amber else PaperDim)
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = if (enabled) Paper else PaperDim,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
            )
            if (!supporting.isNullOrBlank()) {
                Text(
                    supporting,
                    color = PaperDim,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        trailing?.invoke(this)
    }
}

/**
 * Trailing affordance for a [ListRow]. `expanded == null` draws a
 * navigate-forward chevron (auto-mirrored in RTL); true/false draws an
 * expand arrow that turns over when [expanded]. Decorative: the row carries
 * the semantics. PaperFaint on Surface1 3.32:1 (non-text, ≥ 3:1).
 */
@Composable
fun ListRowChevron(expanded: Boolean? = null, modifier: Modifier = Modifier) {
    if (expanded == null) {
        Icon(
            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = PaperFaint,
            modifier = modifier.size(20.dp),
        )
    } else {
        val turn by animateFloatAsState(
            targetValue = if (expanded) 180f else 0f,
            animationSpec = DarkroomMotion.stateChange(),
            label = "chevronTurn",
        )
        Icon(
            Icons.Outlined.ExpandMore,
            contentDescription = null,
            tint = PaperFaint,
            modifier = modifier.size(20.dp).rotate(turn),
        )
    }
}

/**
 * Inline warning or hint: [Surface1] fill, 16dp corners, 1dp outline at 50% of
 * [color], with icon and text in [color]. Amber on Surface1 8.10:1; Danger on
 * Surface1 10.67:1. The 50% outline (2.93:1) is decorative — icon and text
 * carry the meaning. [actions] (e.g. a [TonalButton]) flow below the text and
 * wrap at large font sizes.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NoticeCard(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = Icons.Outlined.Info,
    color: Color = Amber,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(PanelShape)
            .background(Surface1)
            .border(1.dp, color.copy(alpha = 0.5f), PanelShape)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.padding(top = 1.dp, end = 10.dp).size(18.dp),
                )
            }
            Text(text, color = color, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.weight(1f))
        }
        if (actions != null) {
            FlowRow(
                modifier = Modifier.padding(top = 10.dp, start = if (icon != null) 28.dp else 0.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                actions()
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Selection controls
// ---------------------------------------------------------------------------

/**
 * Choice chip. Selected: [AmberSoft] fill, Amber label (6.69:1 over Room,
 * 6.10:1 over Surface1). Unselected: [Surface2] fill, Paper label (13.40:1);
 * pressed: [Surface3] (Paper 11.82:1). Disabled labels are PaperDim (4.96:1 on
 * Surface2).
 *
 * 36dp pill inside a 48dp touch box. Announces [role] (radio button by
 * default; use `Role.Checkbox` for multi-select filters, `Role.Tab` for tabs)
 * and the selected/not selected state. Put a row of them in a parent with
 * `Modifier.selectableGroup()` when they are mutually exclusive.
 */
@Composable
fun SelectPill(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
    role: Role = Role.RadioButton,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val fill by animateColorAsState(
        targetValue = when {
            selected -> AmberSoft
            pressed && enabled -> Surface3
            else -> Surface2
        },
        animationSpec = DarkroomMotion.stateChange(),
        label = "selectPillFill",
    )
    val content by animateColorAsState(
        targetValue = when {
            !enabled -> PaperDim
            selected -> Amber
            else -> Paper
        },
        animationSpec = DarkroomMotion.stateChange(),
        label = "selectPillContent",
    )
    val state = stringResource(
        if (selected) R.string.a11y_state_selected else R.string.a11y_state_not_selected,
    )
    Box(
        modifier
            .heightIn(min = MinTouchTarget)
            .widthIn(min = MinTouchTarget)
            .selectable(
                selected = selected,
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = role,
                onClick = onClick,
            )
            .semantics { stateDescription = state },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier
                .pressScale(interaction)
                .heightIn(min = ChipVisualHeight)
                .clip(ChipShape)
                .background(fill)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) {
                Icon(leadingIcon, contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(label, color = content, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium)
        }
    }
}

/**
 * Segmented control: [Surface2] track; the selected segment is a [Surface3]
 * block with a Paper label (11.82:1), the others PaperDim (4.96:1 on Surface2).
 *
 * Each segment is a 48dp-high touch cell spanning the full track height; the
 * selected block is drawn 3dp inside it. Segments share the width equally and
 * labels wrap (never clip) at large font sizes; all segments grow to the
 * tallest one. The track is a `selectableGroup`; each segment announces
 * [role] (`Role.RadioButton` for a setting, `Role.Tab` when it switches
 * panels) and its selected state.
 *
 * [icons] (same size as [options], null entries allowed) adds an 18dp icon
 * before each label; with [iconOnly] the label is not drawn and becomes the
 * icon's content description.
 */
@Composable
fun SegmentedPills(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    icons: List<Painter?>? = null,
    iconOnly: Boolean = false,
    enabled: Boolean = true,
    role: Role = Role.RadioButton,
) {
    Row(
        modifier
            .height(IntrinsicSize.Min)
            .clip(ControlShape)
            .background(Surface2)
            .selectableGroup(),
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            val fill by animateColorAsState(
                targetValue = when {
                    selected -> Surface3
                    pressed && enabled -> Surface3.copy(alpha = 0.5f)
                    else -> Surface3.copy(alpha = 0f)
                },
                animationSpec = DarkroomMotion.stateChange(),
                label = "segmentFill",
            )
            val content by animateColorAsState(
                targetValue = if (selected && enabled) Paper else PaperDim,
                animationSpec = DarkroomMotion.stateChange(),
                label = "segmentContent",
            )
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .heightIn(min = MinTouchTarget)
                    .selectable(
                        selected = selected,
                        interactionSource = interaction,
                        indication = null,
                        enabled = enabled,
                        role = role,
                        onClick = { onSelect(index) },
                    )
                    .padding(SegmentInset),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .pressScale(interaction)
                        .clip(SegmentShape)
                        .background(fill)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val icon = icons?.getOrNull(index)
                    if (icon != null) {
                        Icon(
                            icon,
                            contentDescription = if (iconOnly) label else null,
                            tint = content,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    if (!iconOnly || icon == null) {
                        if (icon != null) Spacer(Modifier.width(6.dp))
                        Text(
                            label,
                            color = content,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Round icon button: a [size] (default 40dp) circle centred in a 48dp touch
 * box. [container] defaults to [Surface3] (Paper icon 11.82:1); over a photo
 * pass [io.github.wa_otomia.darkroom.ui.theme.OnPhotoScrim] (Paper icon
 * ≥ 4.12:1 even over white). Pressed: the fill moves 12% toward Paper and the
 * circle scales to 0.97. Always give a [contentDescription] unless a visible
 * label next to it says the same thing.
 */
@Composable
fun IconCircleButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    container: Color = Surface3,
    contentColor: Color = Paper,
    size: Dp = 40.dp,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val fill by animateColorAsState(
        targetValue = if (pressed && enabled) lerp(container, Paper, 0.12f) else container,
        animationSpec = DarkroomMotion.stateChange(),
        label = "iconCircleFill",
    )
    Box(
        modifier
            .size(if (size > MinTouchTarget) size else MinTouchTarget)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .pressScale(interaction)
                .size(size)
                .clip(CircleShape)
                .background(fill),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (enabled) contentColor else contentColor.copy(alpha = 0.38f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * Non-interactive status pill: a 6dp dot and a label on a 36dp-high
 * [container]. Use for a state shown next to an action ("Listening"). For a
 * tappable status use [StatusChip]. Paper on Surface2 13.40:1; Amber on
 * AmberSoft 5.46–6.69:1 depending on the backdrop.
 */
@Composable
fun StatusPill(
    text: String,
    modifier: Modifier = Modifier,
    dot: Color? = null,
    container: Color = Surface2,
    contentColor: Color = Paper,
) {
    Row(
        modifier
            .heightIn(min = ChipVisualHeight)
            .clip(ChipShape)
            .background(container)
            .padding(start = if (dot != null) 12.dp else 14.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dot != null) {
            Box(Modifier.size(6.dp).background(dot, CircleShape))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = contentColor, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium)
    }
}

// ---------------------------------------------------------------------------
// Stock Material control colours
// ---------------------------------------------------------------------------

/**
 * Switch: on = Amber track with an Ink thumb (8.18:1); off = [Surface3] track,
 * no outline, PaperDim thumb (4.65:1 on Surface3, 5.16:1 on Surface1). The
 * off track itself is only 1.24:1 against Surface1; the thumb is what marks
 * the control and its state, and it clears 3:1.
 */
@Composable
fun darkroomSwitchColors(): SwitchColors = SwitchDefaults.colors(
    checkedThumbColor = Ink,
    checkedTrackColor = Amber,
    checkedBorderColor = Amber,
    checkedIconColor = Amber,
    uncheckedThumbColor = PaperDim,
    uncheckedTrackColor = Surface3,
    uncheckedBorderColor = Color.Transparent,
    uncheckedIconColor = Surface3,
    disabledCheckedThumbColor = Ink,
    disabledCheckedTrackColor = Amber.copy(alpha = 0.38f),
    disabledCheckedBorderColor = Color.Transparent,
    disabledCheckedIconColor = Amber.copy(alpha = 0.38f),
    disabledUncheckedThumbColor = Paper.copy(alpha = 0.3f),
    disabledUncheckedTrackColor = Surface2,
    disabledUncheckedBorderColor = Color.Transparent,
    disabledUncheckedIconColor = Surface2,
)

/**
 * Slider: Amber active track, [Surface3] inactive track (Amber vs Surface3
 * 6.52:1), Paper thumb (11.82:1 on Surface3, 14.70:1 on Surface1).
 */
@Composable
fun darkroomSliderColors(): SliderColors = SliderDefaults.colors(
    thumbColor = Paper,
    activeTrackColor = Amber,
    activeTickColor = Ink,
    inactiveTrackColor = Surface3,
    inactiveTickColor = PaperDim,
    disabledThumbColor = Paper.copy(alpha = 0.38f),
    disabledActiveTrackColor = Amber.copy(alpha = 0.38f),
    disabledActiveTickColor = Ink.copy(alpha = 0.38f),
    disabledInactiveTrackColor = Surface2,
    disabledInactiveTickColor = PaperDim.copy(alpha = 0.2f),
)

/** Drag handle for a sheet or drawer: 32×4dp PaperFaint bar (3.32:1 on Surface1). Decorative. */
@Composable
fun SheetGrabber(modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(width = 32.dp, height = 4.dp)
            .clip(CircleShape)
            .background(PaperFaint)
            .clearAndSetSemantics {},
    )
}
