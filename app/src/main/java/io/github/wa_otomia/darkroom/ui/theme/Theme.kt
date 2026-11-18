package io.github.wa_otomia.darkroom.ui.theme

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Contrast ratios in this file are WCAG 2.x relative-luminance ratios. A
// translucent colour is first composited over the background it is listed
// against (sRGB, straight alpha), then measured.

val Room = Color(0xFF0E0D0B)
val Paper = Color(0xFFEFE6D4)
val Ink = Color(0xFF16140F)
val Amber = Color(0xFFD9A441)

/**
 * Secondary text. 55% Paper composites to: 5.23:1 on [Room], 5.16:1 on
 * [Surface1], 4.96:1 on [Surface2], 4.65:1 on [Surface3]. All clear 4.5:1 for
 * body text. Do not put it on [TonalPressed] (4.23:1) or on a photo.
 */
val PaperDim = Color(0x8CEFE6D4)

/**
 * Border and divider colour for controls whose outline is the only thing that
 * marks them as interactive (text fields, the outlined legacy chips).
 *
 * 40% alpha composites to 3.29:1 over [Room], 3.32:1 over [Surface1] and
 * 3.28:1 over [Surface2], which clears the 3:1 WCAG 1.4.11 floor for non-text
 * UI. The previous 25% alpha measured 1.99:1.
 */
val PaperFaint = Color(0x66EFE6D4)

/** Legacy 5% white wash. Prefer [Surface1]/[Surface2]; kept for unmigrated call sites. */
val SurfaceLow = Color(0x0DFFFFFF)

/**
 * [SurfaceLow] composited over [Room]. Use this when a panel must be opaque
 * (bottom sheets, overlay cards) instead of a 5% white wash. New code uses
 * [Surface1].
 */
val SurfacePanel = Color(0xFF1A1917)
val Danger = Color(0xFFF2B8B5)

/**
 * Purely decorative hairline, for separators that are never the sole
 * affordance of a control and are therefore exempt from the 3:1 floor.
 */
val PaperHairline = Color(0x33EFE6D4)

/**
 * Fill behind a pressed [io.github.wa_otomia.darkroom.ui.components.PaperButton]. A
 * ripple is close to invisible on the cream Paper fill, so the press is shown
 * by darkening the fill instead. Ink label text measures 11.92:1.
 */
val PaperPressed = Color(0xFFD9CFBC)

/**
 * Former fill behind a disabled PaperButton (55% Paper). Since 0.2.0 a
 * disabled PaperButton uses [Surface2] with a PaperDim label instead, so this
 * is unused by the app; it is kept so older call sites and branches compile.
 */
val PaperDisabled = Color(0x8CEFE6D4)

// ---------------------------------------------------------------------------
// 0.2.0 surface ladder (design D1). Layers are told apart by fill, not by
// outline: Room (page) < Surface1 (cards) < Surface2 (controls) < Surface3
// (secondary buttons, icon tiles). All three are opaque.
// ---------------------------------------------------------------------------

/**
 * Cards, card groups, drawers, bottom sheets, dialogs.
 * Paper on Surface1 14.70:1; PaperDim 5.16:1; Amber 8.10:1; Danger 10.67:1.
 */
val Surface1 = Color(0xFF171512)

/**
 * Control wells: segmented track, unselected chips/pills, status pills, the
 * floating navigation bar, text-field fill.
 * Paper on Surface2 13.40:1; PaperDim 4.96:1; Amber 7.38:1; Danger 9.73:1.
 */
val Surface2 = Color(0xFF211E19)

/**
 * Secondary (tonal) buttons, the selected segment, icon tiles, switch-off
 * track, slider inactive track.
 * Paper on Surface3 11.82:1; PaperDim 4.65:1; Amber 6.52:1.
 */
val Surface3 = Color(0xFF2C2821)

/**
 * Fill of a pressed tonal button / [Surface3] control. Paper on it 10.11:1.
 * PaperDim would be 4.23:1, so disabled tonal buttons use [Surface2] instead.
 */
val TonalPressed = Color(0xFF38332A)

/**
 * Amber at 16%: selected pill fill and deep-link highlight. Amber text on it
 * measures 6.69:1 over [Room], 6.10:1 over [Surface1], 5.46:1 over [Surface2];
 * Paper text 12.14:1 / 11.07:1 / 9.91:1. [PaperDim] on it is only 4.46:1 over
 * Surface1 and 4.17:1 over Surface2: not for text. Use Paper at 62% (5.24:1
 * over Surface1) for secondary text on this fill.
 */
val AmberSoft = Color(0x29D9A441)

/**
 * "Ready" status dot. Never used for text and never the only cue (the chip
 * also carries a label). 9.43:1 on [Surface1], 8.59:1 on [Surface2] — far
 * above the 3:1 non-text floor.
 */
val Ok = Color(0xFF9CC59A)

/**
 * Container for controls floating over a photo (Studio back/delete buttons,
 * size pill). Paper on Room 60% measures 4.12:1 even over a pure white
 * photo, 15.67:1 over Room; icon-only use needs 3:1.
 */
val OnPhotoScrim = Color(0x990E0D0B)

/**
 * Warm top-start tint of the FTP hero card gradient (fades to [Surface1] at
 * 70%). Paper on it 12.52:1, PaperDim 4.79:1, Amber 6.90:1.
 */
val CardGlow = Color(0xFF2B2314)

// ---------------------------------------------------------------------------
// Shapes (design D1). Every container in the UI uses one of these; 2.dp
// corners are retired. Crop frame, ID-photo sheet and watermark drawing are
// not containers and keep their own geometry.
// ---------------------------------------------------------------------------

/** Photos and thumbnails. */
val RadiusPhoto: Dp = 16.dp

/** Cards and card groups. */
val RadiusCard: Dp = 22.dp

/** Drawers, bottom sheets, dialogs (top corners). */
val RadiusSheet: Dp = 28.dp

/** Text fields, dropdown anchors, framed progress. */
val RadiusField: Dp = 14.dp

/** Icon tile inside a list row. */
val RadiusIconTile: Dp = 11.dp

/** Fully rounded ends: buttons, chips, pills, segmented controls, tracks. */
val PillShape: Shape = RoundedCornerShape(percent = 50)
val PhotoShape: Shape = RoundedCornerShape(RadiusPhoto)
val CardShape: Shape = RoundedCornerShape(RadiusCard)
val FieldShape: Shape = RoundedCornerShape(RadiusField)

/** Top-rounded sheet: Studio drawer, [io.github.wa_otomia.darkroom.ui.components.JobScrim] panel. */
val SheetTopShape: Shape = RoundedCornerShape(topStart = RadiusSheet, topEnd = RadiusSheet)

/** Snackbars, framed progress, notice cards. */
val PanelShape: Shape = RoundedCornerShape(16.dp)
val CircleControlShape: Shape = CircleShape

private val DarkroomShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

// ---------------------------------------------------------------------------
// Type. Since 0.2.0 every role is SansSerif in every language (SYUI020-R03).
// ---------------------------------------------------------------------------

/**
 * Display face. Was the platform serif until 0.1.x; 0.2.0 made headings sans
 * in every language, so this is now the same face as [BodyFont].
 */
val DisplayFont: FontFamily = FontFamily.SansSerif
val MonoFont = FontFamily.Monospace
val BodyFont = FontFamily.SansSerif

/** Kept for source compatibility; identical to [DisplayFont] since 0.2.0. */
val DisplayFontCjk: FontFamily = FontFamily.SansSerif

/**
 * Display face for code that sets a heading face by hand. Always SansSerif
 * since 0.2.0; kept so existing call sites compile.
 */
val LocalDisplayFontFamily = staticCompositionLocalOf<FontFamily> { DisplayFont }

/**
 * False when the user turned animations off (Developer options "Animator
 * duration scale" = off, or Accessibility "Remove animations", both of which
 * set `Settings.Global.ANIMATOR_DURATION_SCALE` to 0). Decorative motion such
 * as press-scaling checks this and stays still; state colour changes remain.
 */
val LocalSystemAnimationsEnabled = compositionLocalOf { true }

private val DarkroomColors: ColorScheme = darkColorScheme(
    primary = Paper,
    onPrimary = Ink,
    primaryContainer = Paper,
    onPrimaryContainer = Ink,
    secondary = Amber,
    onSecondary = Ink,
    secondaryContainer = Paper,
    onSecondaryContainer = Ink,
    tertiary = Amber,
    onTertiary = Ink,
    tertiaryContainer = Color(0xFF3A2E15),
    onTertiaryContainer = Paper,
    background = Room,
    onBackground = Paper,
    surface = Room,
    onSurface = Paper,
    surfaceVariant = Surface2,
    onSurfaceVariant = PaperDim,
    // Left unset, the container roles fall back to the Material baseline
    // purple-greys, which show up in menus, sheets and snackbars. Mapped to
    // the darkroom ladder by what each Material component paints with:
    //   surfaceContainerLow     -> ModalBottomSheet, NavigationDrawer: Surface1
    //   surfaceContainer        -> DropdownMenu, NavigationBar:        Surface2
    //   surfaceContainerHigh    -> AlertDialog, DatePicker:            Surface1
    //   surfaceContainerHighest -> filled TextField/Card, Switch off:  Surface3
    // "High" is deliberately darker than the default container: design D1
    // puts dialogs on Surface1 with menus on Surface2.
    surfaceDim = Room,
    surfaceBright = Surface3,
    surfaceContainerLowest = Room,
    surfaceContainerLow = Surface1,
    surfaceContainer = Surface2,
    surfaceContainerHigh = Surface1,
    surfaceContainerHighest = Surface3,
    inverseSurface = Paper,
    inverseOnSurface = Ink,
    outline = PaperFaint,
    outlineVariant = PaperHairline,
    scrim = Color(0xFF000000),
    error = Danger,
    onError = Ink,
    errorContainer = Color(0xFF4A2321),
    onErrorContainer = Danger,
)

/**
 * Every style carries an explicit `lineHeight`. Without one, a CJK glyph run
 * falls back to the font's own metrics, which differ per ROM, so the same
 * screen breaks differently on different devices. No style goes below 12sp:
 * smaller text does not survive an accessibility review.
 *
 * No style carries a `color`. `Text` resolves its colour as `color`, then
 * `style.color`, and only then `LocalContentColor`, so a colour set here
 * outranks the content colour every Material container provides — including
 * the containers that exist to invert it. Text that wants to be dimmed or
 * accented asks for it where it is written.
 *
 * Page titles use `headlineLarge` (28sp Bold); all other headings SemiBold.
 * Monospace is not a theme role: technical values (IP, port, size, file name,
 * count, log) ask for [MonoFont] explicitly.
 */
private val DarkroomTypography = Typography(
    displayLarge = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.SemiBold, fontSize = 40.sp, lineHeight = 48.sp),
    displayMedium = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, lineHeight = 42.sp),
    displaySmall = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 36.sp),
    headlineLarge = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp),
    headlineMedium = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    headlineSmall = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleLarge = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = BodyFont, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = BodyFont, fontSize = 14.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontFamily = BodyFont, fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
)

@Composable
fun DarkroomTheme(content: @Composable () -> Unit) {
    val animationsEnabled = rememberSystemAnimationsEnabled()
    CompositionLocalProvider(
        LocalDisplayFontFamily provides DisplayFont,
        LocalSystemAnimationsEnabled provides animationsEnabled,
    ) {
        MaterialTheme(
            colorScheme = DarkroomColors,
            typography = DarkroomTypography,
            shapes = DarkroomShapes,
            content = content,
        )
    }
}

/**
 * Reads `Settings.Global.ANIMATOR_DURATION_SCALE` and follows changes while
 * the app is open (the setting can be flipped from Quick Settings without
 * recreating the activity).
 */
@Composable
private fun rememberSystemAnimationsEnabled(): Boolean {
    if (LocalInspectionMode.current) return true
    val resolver = LocalContext.current.applicationContext.contentResolver
    var enabled by remember(resolver) { mutableStateOf(animatorScale(resolver) != 0f) }
    DisposableEffect(resolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                enabled = animatorScale(resolver) != 0f
            }
        }
        resolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            false,
            observer,
        )
        // The value may have changed between the first read and registering.
        enabled = animatorScale(resolver) != 0f
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    return enabled
}

private fun animatorScale(resolver: android.content.ContentResolver): Float =
    runCatching {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    }.getOrDefault(1f)
