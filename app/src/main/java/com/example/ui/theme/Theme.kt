package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.model.AppThemeMode

private val LightScheme = lightColorScheme(
  primary = Color(0xFF0F766E),
  onPrimary = Color.White,
  primaryContainer = Color(0xFFECFDF5),
  onPrimaryContainer = Color(0xFF0F766E),
  secondary = Color(0xFF0F766E),
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFECFDF5),
  onSecondaryContainer = Color(0xFF0F766E),
  tertiary = Color(0xFFD97706),
  onTertiary = Color.White,
  tertiaryContainer = Color(0xFFFFFBEB),
  onTertiaryContainer = Color(0xFF92400E),
  error = Color(0xFFDC2626),
  onError = Color.White,
  errorContainer = Color(0xFFFEF2F2),
  onErrorContainer = Color(0xFF991B1B),
  background = Color(0xFFF5F7FA),
  onBackground = Color(0xFF172033),
  surface = Color(0xFFFFFFFF),
  onSurface = Color(0xFF172033),
  surfaceVariant = Color(0xFFF1F4F8),
  onSurfaceVariant = Color(0xFF64748B),
  surfaceTint = Color.Transparent,
  surfaceContainerLowest = Color(0xFFFFFFFF),
  surfaceContainerLow = Color(0xFFFFFFFF),
  surfaceContainer = Color(0xFFFFFFFF),
  surfaceContainerHigh = Color(0xFFFFFFFF),
  surfaceContainerHighest = Color(0xFFF1F4F8),
  outline = Color(0xFFCBD5E1),
  outlineVariant = Color(0xFFE5EAF0),
  inverseSurface = Color(0xFF172033),
  inverseOnSurface = Color(0xFFF5F7FA),
  inversePrimary = Color(0xFF34D399),
  scrim = Color(0xFF0B1220)
)

private val DarkScheme = darkColorScheme(
  primary = Color(0xFF34D399),
  onPrimary = Color(0xFF052E2B),
  primaryContainer = Color(0xFF0F2E2A),
  onPrimaryContainer = Color(0xFFA7F3D0),
  secondary = Color(0xFF34D399),
  onSecondary = Color(0xFF052E2B),
  secondaryContainer = Color(0xFF0F2E2A),
  onSecondaryContainer = Color(0xFFA7F3D0),
  tertiary = Color(0xFFFBBF24),
  onTertiary = Color(0xFF2A1F05),
  tertiaryContainer = Color(0xFF3A2C10),
  onTertiaryContainer = Color(0xFFFDE68A),
  error = Color(0xFFF87171),
  onError = Color(0xFF2A0A0A),
  errorContainer = Color(0xFF3A1A1C),
  onErrorContainer = Color(0xFFFECACA),
  background = Color(0xFF0F1419),
  onBackground = Color(0xFFE6EBF2),
  surface = Color(0xFF161D26),
  onSurface = Color(0xFFE6EBF2),
  surfaceVariant = Color(0xFF1E2833),
  onSurfaceVariant = Color(0xFF94A3B8),
  surfaceTint = Color.Transparent,
  surfaceContainerLowest = Color(0xFF0F1419),
  surfaceContainerLow = Color(0xFF161D26),
  surfaceContainer = Color(0xFF161D26),
  surfaceContainerHigh = Color(0xFF1B2430),
  surfaceContainerHighest = Color(0xFF1E2833),
  outline = Color(0xFF334155),
  outlineVariant = Color(0xFF263241),
  inverseSurface = Color(0xFFE6EBF2),
  inverseOnSurface = Color(0xFF0F1419),
  inversePrimary = Color(0xFF0F766E),
  scrim = Color(0xFF000000)
)

/** Kart köşeleri 14–20 dp: sade, oyuncak gibi değil. */
private val AppShapes = Shapes(
  extraSmall = RoundedCornerShape(8.dp),
  small = RoundedCornerShape(10.dp),
  medium = RoundedCornerShape(14.dp),
  large = RoundedCornerShape(18.dp),
  extraLarge = RoundedCornerShape(22.dp)
)

/** Seçili tema modu ve sistem ayarına göre koyu tema kullanılıp kullanılmayacağı. */
@Composable
fun AppThemeMode.isKomuta(): Boolean = when (this) {
  AppThemeMode.DEFTER -> false
  AppThemeMode.KOMUTA -> true
  AppThemeMode.SISTEM -> isSystemInDarkTheme()
}

@Composable
fun MyApplicationTheme(
  themeMode: AppThemeMode = AppThemeMode.DEFTER,
  content: @Composable () -> Unit,
) {
  val dark = themeMode.isKomuta()
  CompositionLocalProvider(LocalAppPalette provides if (dark) KomutaPalette else DefterPalette) {
    MaterialTheme(
      colorScheme = if (dark) DarkScheme else LightScheme,
      typography = Typography,
      shapes = AppShapes,
      content = content
    )
  }
}
