package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Uygulamanın anlamsal renk paleti. Her tema (Esnaf Defteri / Komuta Merkezi) kendi
 * paletini sağlar; ekranlar renkleri doğrudan hex ile değil bu palet üzerinden okur.
 *
 * - ink      : marka / vurgu rengi (başlıklar, seçili öğeler)
 * - success  : tahsilat, ödendi
 * - danger   : borç, gecikmiş
 * - warning  : vadesi yaklaşan, uyarı
 * - muted    : ikincil metin
 */
data class AppPalette(
  val isDark: Boolean,
  val ink: Color,
  val onInk: Color,
  val success: Color,
  val danger: Color,
  val warning: Color,
  val muted: Color,
  val heroBackground: Color,
  val heroContent: Color,
  val heroMuted: Color,
  val cardBorder: Color
)

val DefterPalette = AppPalette(
  isDark = false,
  ink = Color(0xFF1F2A44),
  onInk = Color(0xFFFFFFFF),
  success = Color(0xFF2E6B3A),
  danger = Color(0xFFA3402A),
  warning = Color(0xFF9A6A12),
  muted = Color(0xFF6E6452),
  heroBackground = Color(0xFFFFFDF8),
  heroContent = Color(0xFF1F2A44),
  heroMuted = Color(0xFF7A6F5C),
  cardBorder = Color(0xFFE4DAC6)
)

val KomutaPalette = AppPalette(
  isDark = true,
  ink = Color(0xFF7FB2FF),
  onInk = Color(0xFF0B1220),
  success = Color(0xFF3DF5A0),
  danger = Color(0xFFFF5C7A),
  warning = Color(0xFFF5B83D),
  muted = Color(0xFF8592AB),
  heroBackground = Color(0xFF111A2C),
  heroContent = Color(0xFFE6EDF7),
  heroMuted = Color(0xFF8592AB),
  cardBorder = Color(0xFF22314D)
)

val LocalAppPalette = staticCompositionLocalOf { DefterPalette }

/** WhatsApp marka yeşili (tema bağımsız). */
val WhatsAppGreen = Color(0xFF25D366)

// Eski isimler korunuyor: tüm ekranlar bunları kullanıyor, artık aktif temaya göre değişiyorlar.
val NavyPrimary: Color
  @Composable @ReadOnlyComposable get() = LocalAppPalette.current.ink

val NavyDark: Color
  @Composable @ReadOnlyComposable get() = LocalAppPalette.current.ink

val EmeraldPrimary: Color
  @Composable @ReadOnlyComposable get() = LocalAppPalette.current.success

val RoseError: Color
  @Composable @ReadOnlyComposable get() = LocalAppPalette.current.danger

val AmberWarning: Color
  @Composable @ReadOnlyComposable get() = LocalAppPalette.current.warning

val Slate500: Color
  @Composable @ReadOnlyComposable get() = LocalAppPalette.current.muted
