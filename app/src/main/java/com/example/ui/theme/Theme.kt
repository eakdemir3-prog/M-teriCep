package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
  primary = Color(0xFF60A5FA),
  onPrimary = Color(0xFF0F172A),
  primaryContainer = NavyContainerDark,
  onPrimaryContainer = Color(0xFFDBEAFE),
  secondary = EmeraldLight,
  onSecondary = Color(0xFF064E3B),
  secondaryContainer = EmeraldContainerDark,
  onSecondaryContainer = Color(0xFFA7F3D0),
  tertiary = Color(0xFFFBBF24),
  onTertiary = Color(0xFF451A03),
  error = Color(0xFFF87171),
  onError = Color(0xFF450A0A),
  background = DarkSurface,
  onBackground = Color(0xFFF1F5F9),
  surface = DarkSurfaceVariant,
  onSurface = Color(0xFFF8FAFC),
  surfaceVariant = Color(0xFF374151),
  onSurfaceVariant = Color(0xFFCBD5E1),
  outline = Color(0xFF6B7280)
)

private val LightColorScheme = lightColorScheme(
  primary = NavyPrimary,
  onPrimary = Color.White,
  primaryContainer = NavyContainerLight,
  onPrimaryContainer = NavyPrimary,
  secondary = EmeraldPrimary,
  onSecondary = Color.White,
  secondaryContainer = EmeraldContainerLight,
  onSecondaryContainer = EmeraldPrimary,
  tertiary = AmberWarning,
  onTertiary = Color.White,
  tertiaryContainer = AmberContainerLight,
  onTertiaryContainer = AmberWarning,
  error = RoseError,
  onError = Color.White,
  errorContainer = RoseContainerLight,
  onErrorContainer = RoseError,
  background = Slate50,
  onBackground = Slate900,
  surface = Color.White,
  onSurface = Slate900,
  surfaceVariant = Slate100,
  onSurfaceVariant = Slate700,
  outline = Slate200
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Keep consistent Navy & Emerald branding
  content: @Composable () -> Unit,
) {
  val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
