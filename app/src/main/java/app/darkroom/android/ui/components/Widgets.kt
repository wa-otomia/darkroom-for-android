package app.darkroom.android.ui.components

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.darkroom.android.R
import app.darkroom.android.data.progress.ProgressUi
import app.darkroom.android.ui.theme.Amber
import app.darkroom.android.ui.theme.Danger
import app.darkroom.android.ui.theme.DarkroomMotion
import app.darkroom.android.ui.theme.Ink
import app.darkroom.android.ui.theme.MonoFont
import app.darkroom.android.ui.theme.Paper
import app.darkroom.android.ui.theme.PaperDim
import app.darkroom.android.ui.theme.PaperDisabled
import app.darkroom.android.ui.theme.PaperFaint
import app.darkroom.android.ui.theme.PaperHairline
import app.darkroom.android.ui.theme.PaperPressed
import app.darkroom.android.ui.theme.Room
import app.darkroom.android.ui.theme.SurfaceLow
import app.darkroom.android.ui.theme.SurfacePanel
import kotlinx.coroutines.delay

private val PillShape = RoundedCornerShape(999.dp)
private val EdgeShape = RoundedCornerShape(2.dp)

/** Smallest touch target we ship, per the Material accessibility guidance. */
private val MinTouchTarget = 48.dp

/** Height of the track in [AmberProgressBar]. */
private val BarHeight = 8.dp

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

/** Fully transparent Paper, so alpha cross-fades never dip through black. */
private val PaperClear = Paper.copy(alpha = 0f)

@Composable
fun darkroomTextFieldColors() = TextFieldDefaults.colors(
    focusedTextColor = Paper,
    unfocusedTextColor = Paper,
    disabledTextColor = PaperDim,
    errorTextColor = Paper,
    focusedContainerColor = SurfaceLow,
    unfocusedContainerColor = SurfaceLow,
    disabledContainerColor = SurfaceLow,
    errorContainerColor = SurfaceLow,
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
)

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
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { if (placeholder.isNotEmpty()) Text(placeholder) },
            modifier = Modifier.fillMaxWidth(),
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

@Composable
fun StatusChip(ok: Boolean, okText: String, badText: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val border by animateColorAsState(
        targetValue = when {
            pressed && ok -> Paper
            ok -> PaperFaint
            else -> Amber
        },
        animationSpec = DarkroomMotion.stateChange(),
        label = "statusChipBorder",
    )
    val content by animateColorAsState(
        targetValue = if (ok) PaperDim else Amber,
        animationSpec = DarkroomMotion.stateChange(),
        label = "statusChipContent",
    )
    val fill by animateColorAsState(
        targetValue = if (pressed) Paper.copy(alpha = 0.14f) else PaperClear,
        animationSpec = DarkroomMotion.stateChange(),
        label = "statusChipFill",
    )
    val state = stringResource(if (ok) R.string.a11y_state_ready else R.string.a11y_state_not_ready)
    Box(
        modifier = Modifier
            .heightIn(min = MinTouchTarget)
            .clip(PillShape)
            .background(fill)
            .border(1.dp, border, PillShape)
            // A bare Box reads as plain text to TalkBack, so the button role
            // and the ready/not-ready state have to be declared by hand.
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .semantics(mergeDescendants = true) { stateDescription = state },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (ok) okText else badText,
            color = content,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        )
    }
}

@Composable
fun VersionChip(active: Boolean, label: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val fill by animateColorAsState(
        targetValue = when {
            active -> Paper
            pressed -> Paper.copy(alpha = 0.14f)
            else -> PaperClear
        },
        animationSpec = DarkroomMotion.stateChange(),
        label = "versionChipFill",
    )
    val border by animateColorAsState(
        targetValue = when {
            active -> PaperClear
            pressed -> Paper
            else -> PaperFaint
        },
        animationSpec = DarkroomMotion.stateChange(),
        label = "versionChipBorder",
    )
    val content by animateColorAsState(
        targetValue = if (active) Ink else Paper,
        animationSpec = DarkroomMotion.stateChange(),
        label = "versionChipContent",
    )
    val state = stringResource(
        if (active) R.string.a11y_state_selected else R.string.a11y_state_not_selected,
    )
    Box(
        modifier = Modifier
            .heightIn(min = MinTouchTarget)
            .clip(PillShape)
            .background(fill)
            .border(1.dp, border, PillShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .semantics(mergeDescendants = true) {
                selected = active
                stateDescription = state
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, color = content, fontSize = 12.sp, lineHeight = 16.sp)
    }
}

@Composable
fun PaperButton(
    text: String,
    enabled: Boolean = true,
    fillMaxWidth: Boolean = true,
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
    Text(
        text = text,
        color = Ink,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .then(if (fillMaxWidth) Modifier.fillMaxWidth() else Modifier)
            .heightIn(min = MinTouchTarget)
            .clip(EdgeShape)
            .background(fill)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

@Composable
fun GhostButton(
    text: String,
    enabled: Boolean = true,
    fillMaxWidth: Boolean = true,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val border by animateColorAsState(
        targetValue = when {
            !enabled -> PaperHairline
            pressed -> Paper
            else -> PaperFaint
        },
        animationSpec = DarkroomMotion.stateChange(),
        label = "ghostButtonBorder",
    )
    val fill by animateColorAsState(
        targetValue = if (pressed && enabled) Paper.copy(alpha = 0.14f) else PaperClear,
        animationSpec = DarkroomMotion.stateChange(),
        label = "ghostButtonFill",
    )
    // Disabled used to look identical to enabled: same Paper label, same border.
    val content by animateColorAsState(
        targetValue = if (enabled) Paper else PaperDim,
        animationSpec = DarkroomMotion.stateChange(),
        label = "ghostButtonContent",
    )
    Text(
        text = text,
        color = content,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .then(if (fillMaxWidth) Modifier.fillMaxWidth() else Modifier)
            .heightIn(min = MinTouchTarget)
            .clip(EdgeShape)
            .background(fill)
            .border(1.dp, border, EdgeShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

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
                        .background(SurfaceLow)
                        .border(1.dp, PaperHairline, EdgeShape)
                        .padding(12.dp)
                } else {
                    Modifier
                },
            ),
    ) {
        Text(
            title.uppercase(),
            color = PaperDim,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 1.6.sp,
            fontFamily = MonoFont,
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
            .background(SurfaceLow)
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
 * bottom-anchored opaque sheet. Put cancel on the bar via [extra] or
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
                .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                .background(SurfacePanel)
                .border(
                    1.dp,
                    PaperHairline,
                    RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp),
                )
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
 * Compact in-page job chrome: phase text, cancel / hide, and a slim amber
 * track. [collapsed] keeps only the track.
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
            .background(SurfacePanel)
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
                        title.uppercase(),
                        color = PaperDim,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        letterSpacing = 1.6.sp,
                        fontFamily = MonoFont,
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
 * Full-width undo affordance, pinned to the bottom of its parent. Hairline
 * on top, message left, amber undo on the right.
 */
@Composable
fun DeleteUndoBar(
    message: String,
    undoLabel: String,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(Room)
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(PaperHairline))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                message,
                color = Paper,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.weight(1f).padding(end = 8.dp),
            )
            TextButton(onClick = onUndo) {
                Text(undoLabel, color = Amber, fontSize = 14.sp, lineHeight = 20.sp)
            }
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = PaperDim,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 2.sp,
        fontFamily = MonoFont,
        modifier = Modifier.padding(bottom = 8.dp),
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
 */
@Composable
fun DarkroomSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(hostState, modifier) { data ->
        Snackbar(
            snackbarData = data,
            shape = EdgeShape,
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
            text = text.uppercase(),
            color = Ink,
            fontFamily = MonoFont,
            // 12sp is the floor; Ink on Amber measures 8.18:1.
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 1.2.sp,
            modifier = Modifier
                .clip(PillShape)
                .background(Amber)
                .padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}
