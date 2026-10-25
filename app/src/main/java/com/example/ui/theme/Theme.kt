package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.example.model.AppThemeMode

private val DefterColorScheme = lightColorScheme(
  primary = Color(0xFF1F2A44),
  onPrimary = Color.White,
  primaryContainer = Color(0xFFE6E2D6),
  onPrimaryContainer = Color(0xFF1F2A44),
  secondary = Color(0xFF2E6B3A),
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFDDEBDC),
  onSecondaryContainer = Color(0xFF1C4626),
  tertiary = Color(0xFF9A6A12),
  onTertiary = Color.White,
  tertiaryContainer = Color(0xFFF4E4C4),
  onTertiaryContainer = Color(0xFF5C3F08),
  error = Color(0xFFA3402A),
  onError = Color.White,
  errorContainer = Color(0xFFF6DED6),
  onErrorContainer = Color(0xFF6B2416),
  background = Color(0xFFF6F1E7),
  onBackground = Color(0xFF1F2A44),
  surface = Color(0xFFFFFDF8),
  onSurface = Color(0xFF1F2A44),
  surfaceVariant = Color(0xFFEFE7D8),
  onSurfaceVariant = Color(0xFF6E6452),
  surfaceContainerLowest = Color(0xFFFFFFFF),
  surfaceContainerLow = Color(0xFFFBF7EF),
  surfaceContainer = Color(0xFFF8F2E6),
  surfaceContainerHigh = Color(0xFFF2EBDD),
  surfaceContainerHighest = Color(0xFFEDE5D5),
  outline = Color(0xFFCFC3AB),
  outlineVariant = Color(0xFFE4DAC6),
  inverseSurface = Color(0xFF1F2A44),
  inverseOnSurface = Color(0xFFF6F1E7)
)

private val KomutaColorScheme = darkColorScheme(
  primary = Color(0xFF7FB2FF),
  onPrimary = Color(0xFF0B1220),
  primaryContainer = Color(0xFF1A2A4A),
  onPrimaryContainer = Color(0xFFCFE0FF),
  secondary = Color(0xFF3DF5A0),
  onSecondary = Color(0xFF0B1220),
  secondaryContainer = Color(0xFF0F3A2A),
  onSecondaryContainer = Color(0xFFB4FBD8),
  tertiary = Color(0xFFF5B83D),
  onTertiary = Color(0xFF0B1220),
  tertiaryContainer = Color(0xFF3D2C08),
  onTertiaryContainer = Color(0xFFFCE3B0),
  error = Color(0xFFFF5C7A),
  onError = Color(0xFF0B1220),
  errorContainer = Color(0xFF4A1623),
  onErrorContainer = Color(0xFFFFD0D9),
  background = Color(0xFF0B1220),
  onBackground = Color(0xFFE6EDF7),
  surface = Color(0xFF111A2C),
  onSurface = Color(0xFFE6EDF7),
  surfaceVariant = Color(0xFF18233A),
  onSurfaceVariant = Color(0xFF9AA8C0),
  surfaceContainerLowest = Color(0xFF080D18),
  surfaceContainerLow = Color(0xFF0E1626),
  surfaceContainer = Color(0xFF111A2C),
  surfaceContainerHigh = Color(0xFF16213A),
  surfaceContainerHighest = Color(0xFF1B2843),
  outline = Color(0xFF34466A),
  outlineVariant = Color(0xFF22314D),
  inverseSurface = Color(0xFFE6EDF7),
  inverseOnSurface = Color(0xFF0B1220)
)

/** Seçili tema modu ve sistem ayarına göre koyu (Komuta Merkezi) tema kullanılıp kullanılmayacağı. */
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
  val komuta = themeMode.isKomuta()
  val palette = if (komuta) KomutaPalette else DefterPalette
  val colorScheme = if (komuta) KomutaColorScheme else DefterColorScheme

  CompositionLocalProvider(LocalAppPalette provides palette) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = Typography,
      content = content
    )
  }
}
