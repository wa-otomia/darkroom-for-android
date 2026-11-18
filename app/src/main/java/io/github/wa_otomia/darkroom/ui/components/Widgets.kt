package io.github.wa_otomia.darkroom.ui.components

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.wa_otomia.darkroom.R
import io.github.wa_otomia.darkroom.core.undoRemainingFraction
import io.github.wa_otomia.darkroom.data.catalog.CatalogRepository
import io.github.wa_otomia.darkroom.data.catalog.PendingDeletion
import io.github.wa_otomia.darkroom.data.progress.ProgressUi
import io.github.wa_otomia.darkroom.ui.theme.Amber
import io.github.wa_otomia.darkroom.ui.theme.Danger
import io.github.wa_otomia.darkroom.ui.theme.FieldShape
import io.github.wa_otomia.darkroom.ui.theme.DarkroomMotion
import io.github.wa_otomia.darkroom.ui.theme.Ink
import io.github.wa_otomia.darkroom.ui.theme.MonoFont
import io.github.wa_otomia.darkroom.ui.theme.Ok
import io.github.wa_otomia.darkroom.ui.theme.PanelShape
import io.github.wa_otomia.darkroom.ui.theme.Paper
import io.github.wa_otomia.darkroom.ui.theme.PaperDim
import io.github.wa_otomia.darkroom.ui.theme.PaperDisabled
import io.github.wa_otomia.darkroom.ui.theme.PaperFaint
import io.github.wa_otomia.darkroom.ui.theme.PaperHairline
import io.github.wa_otomia.darkroom.ui.theme.PaperPressed
import io.github.wa_otomia.darkroom.ui.theme.PillShape
import io.github.wa_otomia.darkroom.ui.theme.Room
import io.github.wa_otomia.darkroom.ui.theme.SheetTopShape
import io.github.wa_otomia.darkroom.ui.theme.Surface1
import io.github.wa_otomia.darkroom.ui.theme.Surface2
import io.github.wa_otomia.darkroom.ui.theme.Surface3
import io.github.wa_otomia.darkroom.ui.theme.TonalPressed
import kotlinx.coroutines.delay

/** Height of the track in [AmberProgressBar] and the default [AmberTrack]. */
private val BarHeight = 6.dp

/** Hairline track used by pending cards and a collapsed [JobProgressStrip]. */
private val ThinBarHeight = 3.dp

/** Backdrop blur radius when a job overlay is up on API 31+. */
private val OverlayBlurRadius = 20.dp

/** Subtle darkening on top of the blurred content (API 31+ only). */
private val BackdropTint = Room.copy(alpha = 0.32f)

fun canBackdropBlur(): Boolean = Build.VERSION.SDK_INT >= 31

fun Modifier.contentBlur(blurred: Boolean): Modifier =
    if (blurred && canBackdropBlur()) blur(OverlayBlurRadius) else this

@Composable
fun OverlayTint(visible: Boolean, modifier: Modifier = Modifier) {
    if (visible && canBackdropBlur()) {
        Box(modifier.fillMaxSize().background(BackdropTint))
    }
}

/** [JobOverlayBar] fill: Room at 85% so the photo still reads through. */
private val OverlayBarFill = Room.copy(alpha = 0.85f)

/**
 * Colours for any Material text field (filled `TextField` or
 * `OutlinedTextField`, including dropdown anchors): [Surface2] fill, Paper
 * text (13.40:1), PaperDim label/placeholder/icons (4.96:1). The indicator
 * roles double as the outline of an `OutlinedTextField`: 1dp PaperFaint at
 * rest (3.28:1 on Surface2), Amber when focused (7.38:1), Danger on error
 * (9.73:1). Pair with `shape = FieldShape` (14dp) on an outlined field.
 */
@Composable
fun darkroomTextFieldColors() = TextFieldDefaults.colors(
    focusedTextColor = Paper,
    unfocusedTextColor = Paper,
    disabledTextColor = PaperDim,
    errorTextColor = Paper,
    focusedContainerColor = Surface2,
    unfocusedContainerColor = Surface2,
    disabledContainerColor = Surface2,
    errorContainerColor = Surface2,
    focusedIndicatorColor = Amber,
    unfocusedIndicatorColor = PaperFaint,
    disabledIndicatorColor = PaperHairline,
    errorIndicatorColor = Danger,
    cursorColor = Amber,
    errorCursorColor = Danger,
    focusedLabelColor = PaperDim,
    unfocusedLabelColor = PaperDim,
    errorLabelColor = Danger,
    focusedPlaceholderColor = PaperDim,
    unfocusedPlaceholderColor = PaperDim,
    focusedTrailingIconColor = PaperDim,
    unfocusedTrailingIconColor = PaperDim,
    errorTrailingIconColor = Danger,
    focusedLeadingIconColor = PaperDim,
    unfocusedLeadingIconColor = PaperDim,
)

/**
 * Outlined text field: [Surface2] fill, 14dp corners, label above the field
 * (PaperDim 14sp, 5.23:1 on Room / 5.16:1 on Surface1). See
 * [darkroomTextFieldColors] for the outline states. [secret] adds the
 * show/hide password toggle.
 */
@Composable
fun DarkroomTextField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    minLines: Int = 1,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    secret: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions(
        imeAction = if (minLines > 1) ImeAction.Default else ImeAction.Done,
    ),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailingIcon: @Composable (() -> Unit)? = null,
    onValueChange: (String) -> Unit,
) {
    var secretVisible by remember { mutableStateOf(false) }
    val transformation = if (secret && !secretVisible) {
        PasswordVisualTransformation()
    } else {
        VisualTransformation.None
    }
    val secretToggle: @Composable (() -> Unit)? = if (secret) {
        {
            IconButton(onClick = { secretVisible = !secretVisible }) {
                Icon(
                    if (secretVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = stringResource(
                        if (secretVisible) R.string.hide_password else R.string.show_password,
                    ),
                    tint = PaperDim,
                )
            }
        }
    } else {
        trailingIcon
    }
    Column(modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        if (label.isNotEmpty()) {
            Text(
                label,
                color = PaperDim,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { if (placeholder.isNotEmpty()) Text(placeholder) },
            modifier = Modifier.fillMaxWidth(),
            shape = FieldShape,
            minLines = minLines,
            enabled = enabled,
            readOnly = readOnly,
            singleLine = minLines == 1 || secret,
            visualTransformation = transformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            trailingIcon = secretToggle,
            colors = darkroomTextFieldColors(),
        )
    }
}

/**
 * Tappable readiness pill (Gallery header): [Surface2] pill with a 6dp status
 * dot ([Ok] when ready, Amber when not) and the matching label — Paper when
 * ready (13.40:1 on Surface2), Amber when not (7.38:1). Pressed: [Surface3]
 * (Paper 11.82:1, Amber 6.52:1). 36dp visual inside a 48dp touch box; button
 * role plus a ready/not-ready state description, so the state never relies
 * on colour alone.
 */
@Composable
fun StatusChip(
    ok: Boolean,
    okText: String,
    badText: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val fill by animateColorAsState(
        targetValue = if (pressed) Surface3 else Surface2,
        animationSpec = DarkroomMotion.stateChange(),
        label = "statusChipFill",
    )
    val content by animateColorAsState(
        targetValue = if (ok) Paper else Amber,
        animationSpec = DarkroomMotion.stateChange(),
        label = "statusChipContent",
    )
    val dot by animateColorAsState(
        targetValue = if (ok) Ok else Amber,
        animationSpec = DarkroomMotion.stateChange(),
        label = "statusChipDot",
    )
    val state = stringResource(if (ok) R.string.a11y_state_ready else R.string.a11y_state_not_ready)
    Box(
        modifier = modifier
            .heightIn(min = MinTouchTarget)
            // A bare Box reads as plain text to TalkBack, so the button role
            // and the ready/not-ready state have to be declared by hand.
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics(mergeDescendants = true) { stateDescription = state },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier
                .pressScale(interaction)
                .heightIn(min = ChipVisualHeight)
                .clip(ChipShape)
                .background(fill)
                .padding(start = 12.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(6.dp).background(dot, CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (ok) okText else badText,
                color = content,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

/**
 * Version selector chip in Studio. Same look as [SelectPill] (selected:
 * AmberSoft fill, Amber label; unselected: Surface2, Paper label) but keeps
 * its historical button role with a selected state description.
 */
@Composable
fun VersionChip(
    active: Boolean,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    SelectPill(
        selected = active,
        label = label,
        onClick = onClick,
        modifier = modifier,
        role = Role.Button,
    )
}

/**
 * Primary action: Paper pill, Ink 15sp Medium label (14.85:1), at least 48dp
 * high. Pressed: [PaperPressed] fill (Ink 11.92:1) and a 0.97 press scale.
 * Disabled: [PaperDisabled] fill (Ink 4.96:1 over Room, 5.21:1 over Surface1).
 * Use one per screen or panel; everything else is a [TonalButton].
 * [icon] (18dp, optional) sits before the label. The label wraps at large
 * font sizes; the pill grows taller instead of clipping.
 */
@Composable
fun PaperButton(
    text: String,
    enabled: Boolean = true,
    fillMaxWidth: Boolean = true,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    // The default ripple is close to invisible on a cream fill, so the press is
    // shown by darkening the fill instead.
    val fill by animateColorAsState(
        targetValue = when {
            !enabled -> PaperDisabled
            pressed -> PaperPressed
            else -> Paper
        },
        animationSpec = DarkroomMotion.stateChange(),
        label = "paperButtonFill",
    )
    PillButton(
        text = text,
        icon = icon,
        fill = fill,
        content = Ink,
        enabled = enabled,
        fillMaxWidth = fillMaxWidth,
        interaction = interaction,
        modifier = modifier,
        onClick = onClick,
    )
}

/**
 * Secondary action. Since 0.2.0 this is the tonal button — a [Surface3] pill
 * with a Paper label (11.82:1), no outline — kept under its old name so
 * existing call sites restyle without edits. New code says [TonalButton].
 */
@Composable
fun GhostButton(
    text: String,
    enabled: Boolean = true,
    fillMaxWidth: Boolean = true,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    TonalButton(
        text = text,
        enabled = enabled,
        fillMaxWidth = fillMaxWidth,
        icon = icon,
        modifier = modifier,
        onClick = onClick,
    )
}

/**
 * Secondary action: [Surface3] pill, Paper 15sp Medium label (11.82:1 on
 * Surface3), no outline, at least 48dp high. Pressed: [TonalPressed] (Paper
 * 10.11:1) and a 0.97 press scale. Disabled: [Surface2] fill with a PaperDim
 * label (4.96:1).
 */
@Composable
fun TonalButton(
    text: String,
    enabled: Boolean = true,
    fillMaxWidth: Boolean = true,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val fill by animateColorAsState(
        targetValue = when {
            !enabled -> Surface2
            pressed -> TonalPressed
            else -> Surface3
        },
        animationSpec = DarkroomMotion.stateChange(),
        label = "tonalButtonFill",
    )
    // Disabled used to look identical to enabled: same Paper label, same border.
    val content by animateColorAsState(
        targetValue = if (enabled) Paper else PaperDim,
        animationSpec = DarkroomMotion.stateChange(),
        label = "tonalButtonContent",
    )
    PillButton(
        text = text,
        icon = icon,
        fill = fill,
        content = content,
        enabled = enabled,
        fillMaxWidth = fillMaxWidth,
        interaction = interaction,
        modifier = modifier,
        onClick = onClick,
    )
}

@Composable
private fun PillButton(
    text: String,
    icon: ImageVector?,
    fill: Color,
    content: Color,
    enabled: Boolean,
    fillMaxWidth: Boolean,
    interaction: MutableInteractionSource,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier
            .then(if (fillMaxWidth) Modifier.fillMaxWidth() else Modifier)
            .heightIn(min = MinTouchTarget)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .pressScale(interaction)
            .clip(ControlShape)
            .background(fill)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            color = content,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Job progress block: title (12sp Medium PaperDim, 5.16:1 on Surface1), phase
 * label (Paper 14.70:1), percent (Amber Mono 8.10:1), a 6dp [AmberTrack] and
 * optional detail. [framed] puts it on a [Surface1] panel with 16dp corners
 * and no outline; unframed it inherits the parent surface.
 */
@Composable
fun AmberProgressBar(
    title: String,
    ui: ProgressUi,
    extra: @Composable () -> Unit = {},
    framed: Boolean = true,
) {
    val showPercent = !ui.indeterminate && ui.percent != null
    Column(
        Modifier
            .fillMaxWidth()
            .then(
                if (framed) {
                    Modifier
                        .clip(PanelShape)
                        .background(Surface1)
                        .padding(16.dp)
                } else {
                    Modifier
                },
            ),
    ) {
        Text(
            title,
            color = PaperDim,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Medium,
        )
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                ui.phaseLabel,
                color = Paper,
                modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp),
            )
            if (showPercent) {
                Text(
                    percentLabel(ui),
                    color = Amber,
                    fontFamily = MonoFont,
                    fontSize = 12.sp,
                )
            }
        }
        AmberTrack(
            ui = ui,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        if (!ui.detail.isNullOrBlank()) {
            Text(
                ui.detail,
                color = PaperDim,
                fontFamily = MonoFont,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        extra()
    }
}

@Composable
fun AmberProgressBar(
    title: String,
    percent: Int,
    indeterminate: Boolean,
    label: String,
    extra: @Composable () -> Unit = {},
    framed: Boolean = true,
) {
    AmberProgressBar(
        title = title,
        ui = ProgressUi(
            percent = if (indeterminate) null else percent,
            fill = (percent.coerceIn(0, 100)) / 100f,
            phaseLabel = label,
            detail = null,
            estimated = false,
            indeterminate = indeterminate,
        ),
        extra = extra,
        framed = framed,
    )
}

/**
 * Rounded amber progress track: 6dp by default, pass `height = 3.dp` for the
 * thin variant on cards and strips. [Surface3] groove (Amber vs Surface3
 * 6.52:1, ≥ 3:1 for the indicator), round ends.
 */
@Composable
fun AmberTrack(
    ui: ProgressUi,
    modifier: Modifier = Modifier,
    height: Dp = BarHeight,
) {
    val terminal = !ui.indeterminate && ui.percent == null
    val percent = if (ui.indeterminate || terminal) 0 else (ui.fill * 100f).toInt().coerceIn(0, 100)
    AmberTrack(percent = percent, indeterminate = ui.indeterminate, modifier = modifier, height = height)
}

@Composable
fun AmberTrack(
    percent: Int,
    indeterminate: Boolean,
    modifier: Modifier = Modifier,
    height: Dp = BarHeight,
) {
    val clamped = percent.coerceIn(0, 100)
    BoxWithConstraints(
        modifier
            .height(height)
            .clip(PillShape)
            .background(Surface3)
            .then(
                if (indeterminate) {
                    Modifier.progressSemantics()
                } else {
                    Modifier.progressSemantics(clamped / 100f)
                },
            ),
    ) {
        val track = maxWidth
        if (indeterminate) {
            val slider = (track * 0.32f).coerceAtLeast(24.dp).coerceAtMost(track)
            val travel = track - slider
            val shift by rememberInfiniteTransition(label = "bar").animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(DarkroomMotion.SweepMillis, easing = DarkroomMotion.Standard),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "barShift",
            )
            Box(
                Modifier
                    .offset(x = travel * shift)
                    .width(slider)
                    .height(height)
                    .background(Amber.copy(alpha = 0.85f), PillShape),
            )
        } else {
            val target = clamped / 100f
            val fill = remember { Animatable(target) }
            LaunchedEffect(target) {
                if (target < fill.value) {
                    fill.snapTo(target)
                } else {
                    fill.animateTo(target, DarkroomMotion.valueChange())
                }
            }
            Box(
                Modifier
                    .width(track * fill.value.coerceIn(0f, 1f))
                    .height(height)
                    .background(Amber, PillShape),
            )
        }
    }
}

/**
 * Full-screen lock while a job runs. Pointer events are consumed so the
 * cropper, gallery, and FABs stay inert. The caller blurs the content behind
 * this overlay via [contentBlur]; this composable only tints on API 31+ and
 * never paints a cheap translucent wash on older APIs. The panel is a
 * bottom-anchored opaque [Surface1] sheet with 28dp top corners. Put cancel on the bar via [extra] or
 * [cancel]. [secondaryAction] follows [cancel] (hide, while the job keeps
 * running).
 */
@Composable
fun JobScrim(
    title: String,
    ui: ProgressUi,
    modifier: Modifier = Modifier,
    extra: @Composable () -> Unit = {},
    cancel: @Composable (() -> Unit)? = null,
    secondaryAction: @Composable (() -> Unit)? = null,
) {
    val a11y = stringResource(R.string.job_scrim_a11y)
    Box(
        modifier
            .fillMaxSize()
            .then(if (canBackdropBlur()) Modifier.background(BackdropTint) else Modifier)
            .semantics { contentDescription = a11y }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent().changes.forEach { it.consume() }
                    }
                }
            },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(SheetTopShape)
                .background(Surface1)
                .padding(20.dp),
        ) {
            AmberProgressBar(
                title = title,
                ui = ui,
                framed = false,
                extra = {
                    extra()
                    cancel?.invoke()
                    secondaryAction?.invoke()
                },
            )
        }
    }
}

@Composable
fun JobScrim(
    title: String,
    percent: Int,
    indeterminate: Boolean,
    label: String,
    modifier: Modifier = Modifier,
    extra: @Composable () -> Unit = {},
    cancel: @Composable (() -> Unit)? = null,
    secondaryAction: @Composable (() -> Unit)? = null,
) {
    JobScrim(
        title = title,
        ui = ProgressUi(
            percent = if (indeterminate) null else percent,
            fill = (percent.coerceIn(0, 100)) / 100f,
            phaseLabel = label,
            indeterminate = indeterminate,
        ),
        modifier = modifier,
        extra = extra,
        cancel = cancel,
        secondaryAction = secondaryAction,
    )
}

/**
 * Compact in-page job chrome on [Surface1]: title (PaperDim 5.16:1), phase
 * text (Paper 14.70:1), cancel / hide, and a 3dp amber track. [collapsed]
 * keeps only the track.
 */
@Composable
fun JobProgressStrip(
    title: String,
    ui: ProgressUi,
    collapsed: Boolean,
    modifier: Modifier = Modifier,
    extra: String? = ui.detail,
    onCancel: (() -> Unit)? = null,
    onHide: (() -> Unit)? = null,
) {
    val a11y = stringResource(R.string.job_scrim_a11y)
    Column(
        modifier
            .fillMaxWidth()
            .background(Surface1)
            .semantics { contentDescription = a11y },
    ) {
        if (!collapsed) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        title,
                        color = PaperDim,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        stripLabel(ui),
                        color = Paper,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!extra.isNullOrBlank()) {
                        Text(
                            extra,
                            color = PaperDim,
                            fontFamily = MonoFont,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
                if (onHide != null) {
                    IconButton(onClick = onHide) {
                        Icon(
                            Icons.Outlined.VisibilityOff,
                            contentDescription = stringResource(R.string.job_hide),
                            tint = Paper,
                        )
                    }
                }
                if (onCancel != null) {
                    IconButton(onClick = onCancel) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.common_cancel),
                            tint = Paper,
                        )
                    }
                }
            }
        }
        AmberTrack(
            ui = ui,
            modifier = Modifier.fillMaxWidth(),
            height = ThinBarHeight,
        )
    }
}

/**
 * Compact phase + fraction bar used on gallery tiles and over the Studio photo.
 * [onHide] only dismisses the chrome for this job; [onCancel] stops the job.
 * Room at 85% over the photo: Paper text measures 10.44:1 even over a pure
 * white photo.
 */
@Composable
fun JobOverlayBar(
    phase: String,
    ui: ProgressUi,
    modifier: Modifier = Modifier,
    onHide: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null,
) {
    val estimated = stringResource(R.string.progress_estimated)
    Column(
        modifier
            .background(OverlayBarFill),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (ui.estimated) "$phase · $estimated" else phase,
                color = Paper,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            )
            if (onHide != null) {
                IconButton(onClick = onHide) {
                    Icon(
                        Icons.Outlined.VisibilityOff,
                        contentDescription = stringResource(R.string.job_hide),
                        tint = Paper,
                    )
                }
            }
            if (onCancel != null) {
                IconButton(onClick = onCancel) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.common_cancel),
                        tint = Paper,
                    )
                }
            }
        }
        AmberTrack(
            ui = ui,
            modifier = Modifier.fillMaxWidth(),
            height = ThinBarHeight,
        )
    }
}

@Composable
fun JobProgressStrip(
    title: String,
    percent: Int,
    indeterminate: Boolean,
    label: String,
    collapsed: Boolean,
    modifier: Modifier = Modifier,
    extra: String? = null,
    onCancel: (() -> Unit)? = null,
    onHide: (() -> Unit)? = null,
) {
    JobProgressStrip(
        title = title,
        ui = ProgressUi(
            percent = if (indeterminate) null else percent,
            fill = (percent.coerceIn(0, 100)) / 100f,
            phaseLabel = label,
            detail = extra,
            indeterminate = indeterminate,
        ),
        collapsed = collapsed,
        modifier = modifier,
        extra = extra,
        onCancel = onCancel,
        onHide = onHide,
    )
}

@Composable
private fun percentLabel(ui: ProgressUi): String {
    val pct = ui.percent ?: return ""
    return if (ui.estimated) {
        "$pct% · ${stringResource(R.string.progress_estimated)}"
    } else {
        "$pct%"
    }
}

@Composable
private fun stripLabel(ui: ProgressUi): String {
    return if (ui.estimated) {
        "${ui.phaseLabel} · ${stringResource(R.string.progress_estimated)}"
    } else {
        ui.phaseLabel
    }
}

/**
 * Undo window chrome in the [JobProgressStrip] slot: same [Surface1] panel,
 * type, and amber track, with a countdown that empties over the grace period.
 * Message Paper 14.70:1; "Undo" Amber 8.10:1.
 */
@Composable
fun DeleteUndoBar(
    pending: PendingDeletion?,
    onUndo: (String) -> Unit,
    onDeleteNow: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val last = remember { mutableStateOf<PendingDeletion?>(null) }
    SideEffect {
        if (pending != null) last.value = pending
    }
    val shown = pending ?: last.value
    val message = if (shown != null) {
        stringResource(R.string.lib_gallery_deleted, shown.ids.size)
    } else {
        ""
    }
    val undoLabel = stringResource(R.string.lib_action_undo)
    val deleteNowLabel = stringResource(R.string.lib_action_delete_now)
    AnimatedVisibility(
        visible = pending != null,
        modifier = modifier.fillMaxWidth(),
        enter = slideInVertically(animationSpec = DarkroomMotion.enter()) { -it } +
            expandVertically(animationSpec = DarkroomMotion.enter()) +
            fadeIn(animationSpec = DarkroomMotion.enter()),
        exit = slideOutVertically(animationSpec = DarkroomMotion.stateChange()) { -it } +
            shrinkVertically(animationSpec = DarkroomMotion.stateChange()) +
            fadeOut(animationSpec = DarkroomMotion.stateChange()),
    ) {
        val current = shown ?: return@AnimatedVisibility
        val fill = rememberUndoFill(current.startedAtMs, current.graceMillis)
        Column(
            Modifier
                .fillMaxWidth()
                .background(Surface1)
                .semantics { liveRegion = LiveRegionMode.Polite },
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    message,
                    color = Paper,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                )
                TextButton(onClick = { onDeleteNow(current.token) }) {
                    Text(deleteNowLabel, color = Paper, fontSize = 14.sp, lineHeight = 20.sp)
                }
                TextButton(onClick = { onUndo(current.token) }) {
                    Text(undoLabel, color = Amber, fontSize = 14.sp, lineHeight = 20.sp)
                }
            }
            AmberTrack(
                ui = ProgressUi(
                    percent = (fill * 100f).toInt().coerceIn(0, 100),
                    fill = fill,
                    phaseLabel = message,
                    indeterminate = false,
                ),
                modifier = Modifier.fillMaxWidth(),
                height = ThinBarHeight,
            )
        }
    }
}

@Composable
private fun rememberUndoFill(startedAtMs: Long, totalMs: Long): Float {
    val window = if (totalMs > 0L) totalMs else CatalogRepository.UNDO_GRACE_MS
    val fill = remember(startedAtMs, window) {
        Animatable(undoRemainingFraction(System.currentTimeMillis() - startedAtMs, window))
    }
    LaunchedEffect(startedAtMs, window) {
        val elapsed = (System.currentTimeMillis() - startedAtMs).coerceAtLeast(0L)
        fill.snapTo(undoRemainingFraction(elapsed, window))
        val leftover = (window - elapsed).coerceAtLeast(0L)
        if (leftover > 0L) {
            fill.animateTo(
                0f,
                tween(
                    durationMillis = leftover.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                    easing = LinearEasing,
                ),
            )
        }
    }
    return fill.value
}

/**
 * Legacy page header: amber `labelSmall` eyebrow, then the `headlineLarge`
 * title 4.dp below it. Superseded by [PageTitle] (design D2); kept until every
 * screen has migrated, then removed.
 */
@Composable
fun ScreenHeader(
    eyebrow: String,
    title: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(eyebrow, style = MaterialTheme.typography.labelSmall, color = Amber)
        Text(
            title,
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/**
 * Small group heading above a card or list: 12sp Medium sans, PaperDim
 * (5.23:1 on Room), original casing, no letter-spacing, 4dp start inset so it
 * lines up inside a 22dp card corner. Marked as a heading for TalkBack.
 */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = PaperDim,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier
            .padding(start = 4.dp, bottom = 8.dp)
            .semantics { heading() },
    )
}

/**
 * The single snackbar visual for the app.
 *
 * The cream fill means every slot has to be named: `contentColor` covers the
 * message, `actionColor` the action label, `actionContentColor` anything else
 * in the action slot, and `dismissActionContentColor` the close icon. Left to
 * the Material defaults the last two resolve through `inversePrimary`, which
 * this scheme never sets. Ink measures 14.85:1 on Paper; Amber, the accent
 * used for actions everywhere else, would be 1.81:1 and is not an option.
 * 16dp corners ([PanelShape]).
 */
@Composable
fun DarkroomSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(hostState, modifier) { data ->
        Snackbar(
            snackbarData = data,
            shape = PanelShape,
            containerColor = Paper,
            contentColor = Ink,
            actionColor = Ink,
            actionContentColor = Ink,
            dismissActionContentColor = Ink,
        )
    }
}

/**
 * Drives an [InlineConfirm] marker.
 *
 * Meant to replace a snackbar for feedback that belongs next to the control the
 * user just touched ("saved", "set as default"). Call it exactly the way a
 * snackbar was called: `scope.launch { confirm.show() }`.
 */
@Stable
class InlineConfirmState internal constructor() {
    var visible by mutableStateOf(false)
        private set

    suspend fun show(durationMillis: Long = DarkroomMotion.ConfirmVisibleMillis) {
        visible = true
        try {
            delay(durationMillis)
        } finally {
            visible = false
        }
    }
}

@Composable
fun rememberInlineConfirmState(): InlineConfirmState = remember { InlineConfirmState() }

/**
 * Short-lived confirmation shown in place, beside the control that changed.
 *
 * The row is a polite live region, so TalkBack still announces the result the
 * way a snackbar did.
 */
@Composable
fun InlineConfirm(visible: Boolean, text: String, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(DarkroomMotion.enter()) +
            scaleIn(animationSpec = DarkroomMotion.enter(), initialScale = 0.85f),
        exit = fadeOut(DarkroomMotion.stateChange()),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        ) {
            Icon(
                Icons.Outlined.Check,
                contentDescription = null,
                tint = Amber,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text,
                color = Amber,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }
}

@Composable
fun InlineConfirm(state: InlineConfirmState, text: String, modifier: Modifier = Modifier) {
    InlineConfirm(visible = state.visible, text = text, modifier = modifier)
}

/**
 * Badge that animates in and out when a persistent state flips, so a change
 * like "this preset is now the default" is visible where it happened instead of
 * only in a snackbar at the bottom of the screen.
 */
@Composable
fun StatusBadge(visible: Boolean, text: String, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(DarkroomMotion.enter()) +
            scaleIn(animationSpec = DarkroomMotion.enter(), initialScale = 0.8f),
        exit = fadeOut(DarkroomMotion.stateChange()) +
            scaleOut(animationSpec = DarkroomMotion.stateChange(), targetScale = 0.8f),
    ) {
        Text(
            text = text,
            color = Ink,
            // 12sp is the floor; Ink on Amber measures 8.18:1.
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clip(PillShape)
                .background(Amber)
                .padding(horizontal = 10.dp, vertical = 3.dp),
        )
    }
}
