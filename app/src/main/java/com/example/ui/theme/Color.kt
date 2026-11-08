package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * MüşteriCep tasarım dili: "Minimal White & Emerald".
 * Tüm renkler burada tanımlanır; ekranlar renkleri doğrudan hex ile değil bu palet üzerinden okur.
 *
 * - ink         : ana yeşil vurgu (önemli düğmeler, seçili menü, bağlantılar)
 * - accent      : canlı yeşil (küçük vurgular, grafik)
 * - accentSoft  : açık yeşil zemin (seçili/olumlu küçük alanlar)
 * - success     : tahsilat / ödendi
 * - danger      : borç / gecikmiş
 * - warning     : vadesi yaklaşan / uyarı
 * - text        : ana metin, muted: ikincil metin, inactive: pasif simge
 */
data class AppPalette(
  val isDark: Boolean,
  val ink: Color,
  val onInk: Color,
  val accent: Color,
  val accentSoft: Color,
  val success: Color,
  val danger: Color,
  val dangerSoft: Color,
  val warning: Color,
  val warningSoft: Color,
  val text: Color,
  val muted: Color,
  val inactive: Color,
  val heroBackground: Color,
  val heroContent: Color,
  val heroMuted: Color,
  val cardBorder: Color,
  /** Marka dolgusu (limon yeşili): seçili sekme, + düğmesi, ana düğmeler. Üstündeki yazı onBrand. */
  val brand: Color = Color(0xFFC6F432),
  val onBrand: Color = Color(0xFF0E0F12)
)

/** Açık mod: %60 kırık beyaz, %30 gece siyahı, %10 limon yeşili (yazı gereken yerde koyu zeytin). */
val DefterPalette = AppPalette(
  isDark = false,
  ink = Color(0xFF4D7C0F),
  onInk = Color(0xFFFFFFFF),
  accent = Color(0xFF84CC16),
  accentSoft = Color(0xFFF1FBD2),
  success = Color(0xFF4D7C0F),
  danger = Color(0xFFDC2626),
  dangerSoft = Color(0xFFFEF2F2),
  warning = Color(0xFFD97706),
  warningSoft = Color(0xFFFFFBEB),
  text = Color(0xFF0E0F12),
  muted = Color(0xFF6B7280),
  inactive = Color(0xFF9CA3AF),
  heroBackground = Color(0xFF0E0F12),
  heroContent = Color(0xFFF5F7FA),
  heroMuted = Color(0xFF9CA3AF),
  cardBorder = Color(0xFFE6E8E3)
)

/** Koyu mod: %60 gece siyahı, %30 beyaz, %10 limon yeşili. */
val KomutaPalette = AppPalette(
  isDark = true,
  ink = Color(0xFFC6F432),
  onInk = Color(0xFF0E0F12),
  accent = Color(0xFFA3E635),
  accentSoft = Color(0xFF222A12),
  success = Color(0xFFC6F432),
  danger = Color(0xFFF87171),
  dangerSoft = Color(0xFF3A1A1C),
  warning = Color(0xFFFBBF24),
  warningSoft = Color(0xFF3A2C10),
  text = Color(0xFFF5F7FA),
  muted = Color(0xFF9CA3AF),
  inactive = Color(0xFF6B7280),
  heroBackground = Color(0xFF17191E),
  heroContent = Color(0xFFF5F7FA),
  heroMuted = Color(0xFF9CA3AF),
  cardBorder = Color(0xFF262A31)
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
