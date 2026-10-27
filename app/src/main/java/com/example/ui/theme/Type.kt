package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Base = Typography()

val Typography = Typography(
  headlineLarge = Base.headlineLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
  headlineMedium = Base.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.25).sp),
  headlineSmall = Base.headlineSmall.copy(fontWeight = FontWeight.Bold),
  titleLarge = Base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
  titleMedium = Base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
  titleSmall = Base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
  bodyLarge = Base.bodyLarge.copy(lineHeight = 24.sp),
  bodyMedium = Base.bodyMedium,
  bodySmall = Base.bodySmall.copy(lineHeight = 18.sp),
  labelLarge = Base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
  labelMedium = Base.labelMedium,
  labelSmall = Base.labelSmall
)

/** Para tutarları: sistem yazı tipi, eşit genişlikte (tabular) rakamlar; alt alta hizalı durur. */
val MoneyFontFamily: FontFamily
  @Composable @ReadOnlyComposable get() = FontFamily.Default

@Composable
@ReadOnlyComposable
fun moneyStyle(size: Int, weight: FontWeight = FontWeight.SemiBold): TextStyle = TextStyle(
  fontFamily = MoneyFontFamily,
  fontWeight = weight,
  fontSize = size.sp,
  letterSpacing = if (size >= 24) (-0.5).sp else 0.sp,
  fontFeatureSettings = "tnum"
)
