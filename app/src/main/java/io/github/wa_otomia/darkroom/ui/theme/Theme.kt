package io.github.wa_otomia.darkroom.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.sp

val Room = Color(0xFF0E0D0B)
val Paper = Color(0xFFEFE6D4)
val Ink = Color(0xFF16140F)
val Amber = Color(0xFFD9A441)
val PaperDim = Color(0x8CEFE6D4)

/**
 * Border and divider colour for controls whose outline is the only thing that
 * marks them as interactive (GhostButton, the outlined chips, text fields).
 *
 * 40% alpha composites to 3.29:1 over [Room], which clears the 3:1 WCAG 1.4.11
 * floor for non-text UI. The previous 25% alpha measured 1.99:1.
 */
val PaperFaint = Color(0x66EFE6D4)

val SurfaceLow = Color(0x0DFFFFFF)

/**
 * [SurfaceLow] composited over [Room]. Use this when a panel must be opaque
 * (bottom sheets, overlay cards) instead of a 5% white wash.
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
 * by darkening the fill instead. Ink label text stays at 12.29:1.
 */
val PaperPressed = Color(0xFFD9CFBC)

/**
 * Fill behind a disabled [io.github.wa_otomia.darkroom.ui.components.PaperButton].
 * 55% alpha keeps the Ink label at 4.96:1; the previous 40% measured 3.12:1.
 */
val PaperDisabled = Color(0x8CEFE6D4)

val DisplayFont = FontFamily.Serif
val MonoFont = FontFamily.Monospace
val BodyFont = FontFamily.SansSerif

/**
 * Display face used when the UI language is Chinese, Japanese or Korean.
 *
 * Most CJK ROMs ship no CJK serif, so [DisplayFont] silently falls back to the
 * system sans for CJK glyphs while keeping the serif for Latin ones. A heading
 * that mixes both then renders in two faces with different weights and
 * baselines. Asking for sans on both sides keeps mixed-script headings
 * consistent; the lost serif flavour is compensated with a heavier weight (see
 * [CjkDisplayWeight]).
 */
val DisplayFontCjk = FontFamily.SansSerif

/** Serif at Medium reads as heavy as sans at SemiBold at the same size. */
private val LatinDisplayWeight = FontWeight.Medium
private val CjkDisplayWeight = FontWeight.SemiBold

private val CjkLanguages = setOf("zh", "ja", "ko")

/**
 * Display face picked for the active UI language. Code that sets
 * `fontFamily = DisplayFont` by hand should read this instead so it follows the
 * locale like [MaterialTheme.typography] does.
 */
val LocalDisplayFontFamily = staticCompositionLocalOf<FontFamily> { DisplayFont }

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
    surfaceVariant = Color(0xFF1A1814),
    onSurfaceVariant = PaperDim,
    // Left unset, the container roles fall back to the Material baseline
    // purple-greys, which show up in menus, sheets and snackbars.
    surfaceDim = Color(0xFF0A0908),
    surfaceBright = Color(0xFF262319),
    surfaceContainerLowest = Color(0xFF090807),
    surfaceContainerLow = Color(0xFF141210),
    surfaceContainer = Color(0xFF1A1814),
    surfaceContainerHigh = Color(0xFF201E19),
    surfaceContainerHighest = Color(0xFF26231D),
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
 */
private fun darkroomTypography(display: FontFamily, displayWeight: FontWeight) = Typography(
    displayLarge = TextStyle(fontFamily = display, fontWeight = displayWeight, fontSize = 40.sp, lineHeight = 48.sp),
    displayMedium = TextStyle(fontFamily = display, fontWeight = displayWeight, fontSize = 34.sp, lineHeight = 42.sp),
    displaySmall = TextStyle(fontFamily = display, fontWeight = displayWeight, fontSize = 28.sp, lineHeight = 36.sp),
    headlineLarge = TextStyle(fontFamily = display, fontWeight = displayWeight, fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = display, fontWeight = displayWeight, fontSize = 24.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontFamily = display, fontWeight = displayWeight, fontSize = 20.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = display, fontWeight = displayWeight, fontSize = 22.sp, lineHeight = 30.sp),
    titleMedium = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = BodyFont, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = BodyFont, fontSize = 14.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontFamily = MonoFont, fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = MonoFont, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 2.sp),
)

private val LatinTypography = darkroomTypography(DisplayFont, LatinDisplayWeight)
private val CjkTypography = darkroomTypography(DisplayFontCjk, CjkDisplayWeight)

@Composable
fun DarkroomTheme(content: @Composable () -> Unit) {
    // A locale change recreates the activity, so reading the platform locale
    // once here is enough to pick the display face for the whole tree.
    val cjk = Locale.current.language in CjkLanguages
    val display = if (cjk) DisplayFontCjk else DisplayFont
    CompositionLocalProvider(LocalDisplayFontFamily provides display) {
        MaterialTheme(
            colorScheme = DarkroomColors,
            typography = if (cjk) CjkTypography else LatinTypography,
            content = content,
        )
    }
}
