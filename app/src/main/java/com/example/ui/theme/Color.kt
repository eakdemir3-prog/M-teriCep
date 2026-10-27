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
  val cardBorder: Color
)

/** Varsayılan: beyaz kartlar, açık gri zemin, zümrüt yeşili vurgu. */
val DefterPalette = AppPalette(
  isDark = false,
  ink = Color(0xFF0F766E),
  onInk = Color(0xFFFFFFFF),
  accent = Color(0xFF10B981),
  accentSoft = Color(0xFFECFDF5),
  success = Color(0xFF16A34A),
  danger = Color(0xFFDC2626),
  dangerSoft = Color(0xFFFEF2F2),
  warning = Color(0xFFD97706),
  warningSoft = Color(0xFFFFFBEB),
  text = Color(0xFF172033),
  muted = Color(0xFF64748B),
  inactive = Color(0xFF94A3B8),
  heroBackground = Color(0xFFFFFFFF),
  heroContent = Color(0xFF172033),
  heroMuted = Color(0xFF64748B),
  cardBorder = Color(0xFFE5EAF0)
)

/** Koyu mod: aynı dil, gece için. */
val KomutaPalette = AppPalette(
  isDark = true,
  ink = Color(0xFF34D399),
  onInk = Color(0xFF052E2B),
  accent = Color(0xFF10B981),
  accentSoft = Color(0xFF0F2E2A),
  success = Color(0xFF4ADE80),
  danger = Color(0xFFF87171),
  dangerSoft = Color(0xFF3A1A1C),
  warning = Color(0xFFFBBF24),
  warningSoft = Color(0xFF3A2C10),
  text = Color(0xFFE6EBF2),
  muted = Color(0xFF94A3B8),
  inactive = Color(0xFF64748B),
  heroBackground = Color(0xFF161D26),
  heroContent = Color(0xFFE6EBF2),
  heroMuted = Color(0xFF94A3B8),
  cardBorder = Color(0xFF263241)
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
