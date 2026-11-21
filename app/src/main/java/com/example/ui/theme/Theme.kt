package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat
import com.example.data.prefs.AppLanguage
import com.example.data.prefs.FontSizeOption
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.Translator
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.data.prefs.AppThemeStyle

@Immutable
data class LiquidGlassColors(
  val isDark: Boolean,
  val glassSurface: Color,
  val glassSurfaceElevated: Color,
  val glassBorderTop: Color,
  val glassBorderBottom: Color,
  val shadowColor: Color,
  val heroStart: Color,
  val heroEnd: Color,
  val tealAccent: Color,
  val primaryAccent: Color,
  val softBlueBg: Color,
  val divider: Color,
)

val LocalLiquidGlassColors =
  staticCompositionLocalOf {
    LiquidGlassColors(
      isDark = false,
      glassSurface = SurfaceWhite.copy(alpha = 0.82f),
      glassSurfaceElevated = SurfaceWhite.copy(alpha = 0.94f),
      glassBorderTop = Color.White.copy(alpha = 0.95f),
      glassBorderBottom = BorderLight.copy(alpha = 0.75f),
      shadowColor = DeepNavy.copy(alpha = 0.08f),
      heroStart = DeepNavy,
      heroEnd = RoyalBlue,
      tealAccent = PremiumTeal,
      primaryAccent = PrimaryBlue,
      softBlueBg = LightBlue,
      divider = BorderLight,
    )
  }

/** Colors of one selectable color style. */
@Immutable
data class ThemePalette(
  val primary: Color,
  val secondary: Color,
  val accent: Color,
  val deep: Color,
  val soft: Color,
  val accentSoft: Color,
)

fun AppThemeStyle.palette(): ThemePalette =
  when (this) {
    AppThemeStyle.LIQUID_ROYAL ->
      ThemePalette(
        Color(0xFF2563EB), Color(0xFF356AE6), Color(0xFF13B8A6),
        Color(0xFF172B4D), Color(0xFFEAF2FF), Color(0xFFE5FAF7),
      )
    AppThemeStyle.DEEP_NAVY ->
      ThemePalette(
        Color(0xFF1D3B7A), Color(0xFF2F5FC4), Color(0xFF0EA5E9),
        Color(0xFF0F1F3D), Color(0xFFE6EDFB), Color(0xFFE0F2FE),
      )
    AppThemeStyle.OCEAN_TEAL ->
      ThemePalette(
        Color(0xFF0D9488), Color(0xFF0E7490), Color(0xFF38BDF8),
        Color(0xFF134E4A), Color(0xFFE0F7F4), Color(0xFFE0F2FE),
      )
    AppThemeStyle.EMERALD ->
      ThemePalette(
        Color(0xFF059669), Color(0xFF047857), Color(0xFF34D399),
        Color(0xFF064E3B), Color(0xFFE6F8EF), Color(0xFFE7FAF1),
      )
    AppThemeStyle.INDIGO_SKY ->
      ThemePalette(
        Color(0xFF4F46E5), Color(0xFF4338CA), Color(0xFF06B6D4),
        Color(0xFF1E1B4B), Color(0xFFEEF0FF), Color(0xFFE0F7FA),
      )
    AppThemeStyle.VIOLET ->
      ThemePalette(
        Color(0xFF7C3AED), Color(0xFF6D28D9), Color(0xFFEC4899),
        Color(0xFF3B1D72), Color(0xFFF1EBFF), Color(0xFFFDE7F3),
      )
    AppThemeStyle.ROSE ->
      ThemePalette(
        Color(0xFFE11D48), Color(0xFFBE123C), Color(0xFFFB7185),
        Color(0xFF881337), Color(0xFFFFE8ED), Color(0xFFFFE4E9),
      )
    AppThemeStyle.SUNSET ->
      ThemePalette(
        Color(0xFFE8590C), Color(0xFFDC4A0A), Color(0xFFF59E0B),
        Color(0xFF7C2D12), Color(0xFFFFF1E6), Color(0xFFFFF4DD),
      )
    AppThemeStyle.GOLD ->
      ThemePalette(
        Color(0xFFB7791F), Color(0xFF9A6412), Color(0xFFEAB308),
        Color(0xFF5B3A0A), Color(0xFFFBF1DC), Color(0xFFFEF6D8),
      )
    AppThemeStyle.GRAPHITE ->
      ThemePalette(
        Color(0xFF475569), Color(0xFF334155), Color(0xFF64748B),
        Color(0xFF1E293B), Color(0xFFEEF1F5), Color(0xFFE8ECF1),
      )
  }

@Composable
fun MunakasatTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  themeStyle: AppThemeStyle = AppThemeStyle.LIQUID_ROYAL,
  amoled: Boolean = false,
  language: AppLanguage = AppLanguage.AR,
  fontSize: FontSizeOption = FontSizeOption.NORMAL,
  content: @Composable () -> Unit,
) {
  Translator.language = language
  val palette = themeStyle.palette()
  val lift = if (darkTheme) 0.22f else 0f
  val primary = lerp(palette.primary, Color.White, lift)
  val secondary = lerp(palette.secondary, Color.White, lift)
  val accent = lerp(palette.accent, Color.White, if (darkTheme) 0.08f else 0f)

  // Publish the brand colors so every screen that reads PrimaryBlue / PremiumTeal ... follows them.
  ActiveTheme.primary = primary
  ActiveTheme.secondary = secondary
  ActiveTheme.accent = accent
  ActiveTheme.deep = palette.deep
  ActiveTheme.soft = palette.soft
  ActiveTheme.accentSoft = palette.accentSoft

  val darkBackground = if (amoled) Color.Black else AppBackgroundDark
  val darkSurface = if (amoled) Color(0xFF0A0A0E) else SurfaceDark
  val darkSurfaceElevated = if (amoled) Color(0xFF15151C) else SurfaceElevatedDark
  val darkBorder = if (amoled) Color(0xFF26262F) else BorderDark

  val colorScheme =
    if (darkTheme) {
      darkColorScheme(
        primary = primary,
        onPrimary = Color.White,
        primaryContainer = darkSurfaceElevated,
        onPrimaryContainer = lerp(primary, Color.White, 0.65f),
        secondary = accent,
        onSecondary = Color.White,
        secondaryContainer = lerp(darkSurfaceElevated, accent, 0.18f),
        onSecondaryContainer = lerp(accent, Color.White, 0.7f),
        tertiary = InfoBlue,
        background = darkBackground,
        onBackground = MainTextDark,
        surface = darkSurface,
        onSurface = MainTextDark,
        surfaceVariant = darkSurfaceElevated,
        onSurfaceVariant = SecondaryTextDark,
        outline = darkBorder,
        outlineVariant = darkBorder.copy(alpha = 0.6f),
        error = DangerRed,
        onError = Color.White,
      )
    } else {
      lightColorScheme(
        primary = primary,
        onPrimary = Color.White,
        primaryContainer = palette.soft,
        onPrimaryContainer = palette.deep,
        secondary = accent,
        onSecondary = Color.White,
        secondaryContainer = palette.accentSoft,
        onSecondaryContainer = palette.deep,
        tertiary = InfoBlue,
        background = lerp(AppBackgroundLight, palette.soft, 0.35f),
        onBackground = MainTextLight,
        surface = SurfaceWhite,
        onSurface = MainTextLight,
        surfaceVariant = palette.soft.copy(alpha = 0.55f),
        onSurfaceVariant = SecondaryTextLight,
        outline = BorderLight,
        outlineVariant = BorderLight,
        error = DangerRed,
        onError = Color.White,
      )
    }

  val glassColors =
    if (darkTheme) {
      LiquidGlassColors(
        isDark = true,
        glassSurface = darkSurface.copy(alpha = 0.90f),
        glassSurfaceElevated = darkSurfaceElevated.copy(alpha = 0.94f),
        glassBorderTop = Color.White.copy(alpha = 0.14f),
        glassBorderBottom = darkBorder.copy(alpha = 0.65f),
        shadowColor = Color.Black.copy(alpha = 0.35f),
        heroStart = lerp(palette.deep, primary, 0.45f),
        heroEnd = lerp(primary, accent, 0.55f),
        tealAccent = accent,
        primaryAccent = primary,
        softBlueBg = darkSurfaceElevated,
        divider = darkBorder,
      )
    } else {
      LiquidGlassColors(
        isDark = false,
        glassSurface = SurfaceWhite.copy(alpha = 0.92f),
        glassSurfaceElevated = SurfaceWhite.copy(alpha = 0.96f),
        glassBorderTop = Color.White.copy(alpha = 0.98f),
        glassBorderBottom = BorderLight.copy(alpha = 0.80f),
        shadowColor = palette.deep.copy(alpha = 0.08f),
        heroStart = palette.secondary,
        heroEnd = accent,
        tealAccent = accent,
        primaryAccent = primary,
        softBlueBg = palette.soft,
        divider = BorderLight,
      )
    }

  // Keep status/navigation bar icons readable against the chosen light/dark appearance.
  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as? Activity)?.window
      if (window != null) {
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = !darkTheme
        controller.isAppearanceLightNavigationBars = !darkTheme
      }
    }
  }

  val baseDensity = LocalDensity.current
  val density =
    androidx.compose.runtime.remember(baseDensity, fontSize) {
      Density(baseDensity.density, baseDensity.fontScale * fontSize.scale)
    }

  CompositionLocalProvider(
    LocalLayoutDirection provides
      (if (language == AppLanguage.EN) LayoutDirection.Ltr else LayoutDirection.Rtl),
    LocalAppLanguage provides language,
    LocalDensity provides density,
    LocalLiquidGlassColors provides glassColors,
  ) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = AppTypography,
      shapes = AppShapes,
      content = content,
    )
  }
}
