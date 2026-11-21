package com.example.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * Brand colors that follow the selected color style. They are Compose state, so every screen that
 * reads PrimaryBlue / RoyalBlue / PremiumTeal ... recomposes automatically when the style changes.
 */
object ActiveTheme {
  var primary by mutableStateOf(Color(0xFF2563EB))
  var secondary by mutableStateOf(Color(0xFF356AE6))
  var accent by mutableStateOf(Color(0xFF13B8A6))
  var deep by mutableStateOf(Color(0xFF172B4D))
  var soft by mutableStateOf(Color(0xFFEAF2FF))
  var accentSoft by mutableStateOf(Color(0xFFE5FAF7))
}

// Core Brand & Light Palette
val PrimaryBlue: Color get() = ActiveTheme.primary
val DeepNavy: Color get() = ActiveTheme.deep
val RoyalBlue: Color get() = ActiveTheme.secondary
val PremiumTeal: Color get() = ActiveTheme.accent
val LightBlue: Color get() = ActiveTheme.soft
val AppBackgroundLight = Color(0xFFF5F7FC)
val SurfaceWhite = Color(0xFFFFFFFF)
val MainTextLight = Color(0xFF172033)
val SecondaryTextLight = Color(0xFF7B8497)
val BorderLight = Color(0xFFE7ECF4)

// Dark Palette
val AppBackgroundDark = Color(0xFF0B1324)
val SurfaceDark = Color(0xFF142036)
val SurfaceElevatedDark = Color(0xFF1B2B47)
val MainTextDark = Color(0xFFF1F5FB)
val SecondaryTextDark = Color(0xFF9AA7BE)
val BorderDark = Color(0xFF263859)

// Semantic Colors
val SuccessGreen = Color(0xFF16A36A)
val SuccessSoftBg = Color(0xFFE7F8F0)
val WarningAmber = Color(0xFFF0A42B)
val WarningSoftBg = Color(0xFFFFF4E3)
val DangerRed = Color(0xFFE5484D)
val DangerSoftBg = Color(0xFFFEECEC)
val InfoBlue = Color(0xFF3984E8)
val InfoSoftBg = Color(0xFFEBF3FF)

// Soft Stat Accents
val VioletSoftBg = Color(0xFFF2EEFF)
val VioletAccent = Color(0xFF7C3AED)
val CyanSoftBg: Color get() = ActiveTheme.accentSoft
