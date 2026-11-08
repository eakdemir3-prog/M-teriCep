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
  primary = Color(0xFF4D7C0F),
  onPrimary = Color.White,
  primaryContainer = Color(0xFFF1FBD2),
  onPrimaryContainer = Color(0xFF3F6212),
  secondary = Color(0xFF4D7C0F),
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFF1FBD2),
  onSecondaryContainer = Color(0xFF3F6212),
  tertiary = Color(0xFFD97706),
  onTertiary = Color.White,
  tertiaryContainer = Color(0xFFFFFBEB),
  onTertiaryContainer = Color(0xFF92400E),
  error = Color(0xFFDC2626),
  onError = Color.White,
  errorContainer = Color(0xFFFEF2F2),
  onErrorContainer = Color(0xFF991B1B),
  background = Color(0xFFF6F7F4),
  onBackground = Color(0xFF0E0F12),
  surface = Color(0xFFFFFFFF),
  onSurface = Color(0xFF0E0F12),
  surfaceVariant = Color(0xFFEEF0EA),
  onSurfaceVariant = Color(0xFF6B7280),
  surfaceTint = Color.Transparent,
  surfaceContainerLowest = Color(0xFFFFFFFF),
  surfaceContainerLow = Color(0xFFFFFFFF),
  surfaceContainer = Color(0xFFFFFFFF),
  surfaceContainerHigh = Color(0xFFFFFFFF),
  surfaceContainerHighest = Color(0xFFEEF0EA),
  outline = Color(0xFFD4D7CF),
  outlineVariant = Color(0xFFE6E8E3),
  inverseSurface = Color(0xFF0E0F12),
  inverseOnSurface = Color(0xFFF5F7FA),
  inversePrimary = Color(0xFFC6F432),
  scrim = Color(0xFF0E0F12)
)

private val DarkScheme = darkColorScheme(
  primary = Color(0xFFC6F432),
  onPrimary = Color(0xFF0E0F12),
  primaryContainer = Color(0xFF222A12),
  onPrimaryContainer = Color(0xFFE4FB9A),
  secondary = Color(0xFFC6F432),
  onSecondary = Color(0xFF0E0F12),
  secondaryContainer = Color(0xFF222A12),
  onSecondaryContainer = Color(0xFFE4FB9A),
  tertiary = Color(0xFFFBBF24),
  onTertiary = Color(0xFF2A1F05),
  tertiaryContainer = Color(0xFF3A2C10),
  onTertiaryContainer = Color(0xFFFDE68A),
  error = Color(0xFFF87171),
  onError = Color(0xFF2A0A0A),
  errorContainer = Color(0xFF3A1A1C),
  onErrorContainer = Color(0xFFFECACA),
  background = Color(0xFF0E0F12),
  onBackground = Color(0xFFF5F7FA),
  surface = Color(0xFF17191E),
  onSurface = Color(0xFFF5F7FA),
  surfaceVariant = Color(0xFF1F2228),
  onSurfaceVariant = Color(0xFF9CA3AF),
  surfaceTint = Color.Transparent,
  surfaceContainerLowest = Color(0xFF0E0F12),
  surfaceContainerLow = Color(0xFF17191E),
  surfaceContainer = Color(0xFF17191E),
  surfaceContainerHigh = Color(0xFF1C1F24),
  surfaceContainerHighest = Color(0xFF1F2228),
  outline = Color(0xFF3A3F47),
  outlineVariant = Color(0xFF262A31),
  inverseSurface = Color(0xFFF5F7FA),
  inverseOnSurface = Color(0xFF0E0F12),
  inversePrimary = Color(0xFF4D7C0F),
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
