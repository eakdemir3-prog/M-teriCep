package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalAppPalette

/** Uygulamanın standart kartı: beyaz zemin, ince kenarlık, 16 dp köşe, gölgesiz. */
@Composable
fun AppCard(
  modifier: Modifier = Modifier,
  padding: Dp = 16.dp,
  onClick: (() -> Unit)? = null,
  content: @Composable ColumnScope.() -> Unit
) {
  val palette = LocalAppPalette.current
  val shape = RoundedCornerShape(16.dp)
  val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  val border = BorderStroke(1.dp, palette.cardBorder)
  if (onClick != null) {
    Card(onClick = onClick, shape = shape, colors = colors, border = border, modifier = modifier.fillMaxWidth()) {
      Column(modifier = Modifier.padding(padding), content = content)
    }
  } else {
    Card(shape = shape, colors = colors, border = border, modifier = modifier.fillMaxWidth()) {
      Column(modifier = Modifier.padding(padding), content = content)
    }
  }
}

/** Bölüm başlığı + isteğe bağlı sağda "Tümü" bağlantısı. */
@Composable
fun SectionHeader(
  title: String,
  modifier: Modifier = Modifier,
  color: Color = MaterialTheme.colorScheme.onBackground,
  action: String? = null,
  onAction: (() -> Unit)? = null
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(top = 8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      title,
      style = MaterialTheme.typography.titleMedium,
      color = color,
      modifier = Modifier.weight(1f),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
    if (action != null && onAction != null) {
      TextButton(onClick = onAction) {
        Text(action, color = LocalAppPalette.current.ink, maxLines = 1, softWrap = false)
      }
    }
  }
}

/** Renkli küçük etiket (durum her zaman yazıyla da belirtilir, sadece renkle değil). */
@Composable
fun StatusPill(text: String, color: Color, background: Color) {
  Surface(shape = RoundedCornerShape(6.dp), color = background) {
    Text(
      text,
      color = color,
      fontSize = 11.sp,
      fontWeight = FontWeight.SemiBold,
      maxLines = 1,
      softWrap = false,
      modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
    )
  }
}

/** İsmin baş harfleriyle yuvarlak avatar. */
@Composable
fun InitialsAvatar(name: String, size: Dp = 40.dp, tint: Color? = null, background: Color? = null) {
  val palette = LocalAppPalette.current
  val initials = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() && it[0].isLetter() }
    .take(2).joinToString("") { it.take(1) }.uppercase(java.util.Locale("tr", "TR")).ifBlank { "?" }
  Box(
    modifier = Modifier
      .size(size)
      .clip(CircleShape)
      .background(background ?: palette.accentSoft),
    contentAlignment = Alignment.Center
  ) {
    Text(initials, color = tint ?: palette.ink, fontWeight = FontWeight.SemiBold, fontSize = (size.value * 0.36f).sp)
  }
}

/** Yuvarlak zeminli simge (hızlı işlem, liste başı). */
@Composable
fun IconBadge(icon: ImageVector, tint: Color, background: Color, size: Dp = 40.dp, iconSize: Dp = 20.dp) {
  Box(
    modifier = Modifier
      .size(size)
      .clip(RoundedCornerShape(12.dp))
      .background(background),
    contentAlignment = Alignment.Center
  ) {
    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
  }
}

/** Hızlı işlem kutucuğu: simge + kısa (en fazla 2 satır) açıklama. Genişliği dışarıdan verilir. */
@Composable
fun QuickActionTile(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
  val palette = LocalAppPalette.current
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifier
      .clip(RoundedCornerShape(14.dp))
      .clickable(role = Role.Button, onClick = onClick)
      .padding(vertical = 8.dp)
  ) {
    IconBadge(icon = icon, tint = palette.ink, background = palette.accentSoft, size = 48.dp, iconSize = 22.dp)
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      label,
      fontSize = 12.sp,
      lineHeight = 15.sp,
      fontWeight = FontWeight.Medium,
      textAlign = TextAlign.Center,
      color = MaterialTheme.colorScheme.onSurface,
      minLines = 2,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis
    )
  }
}

/** Basit çubuk grafik (gerçek verilerle). Değerler 0 ise ince taban çizgisi görünür. */
@Composable
fun BarChart(
  values: List<Double>,
  labels: List<String>,
  highlightIndex: Int = -1,
  modifier: Modifier = Modifier,
  height: Dp = 120.dp
) {
  val palette = LocalAppPalette.current
  val max = (values.maxOrNull() ?: 0.0).coerceAtLeast(1.0)
  val barColor = palette.accent
  val dimColor = palette.accent.copy(alpha = 0.35f)
  val baseColor = palette.cardBorder
  Column(modifier = modifier.fillMaxWidth()) {
    Canvas(
      modifier = Modifier
        .fillMaxWidth()
        .height(height)
    ) {
      val n = values.size.coerceAtLeast(1)
      val slot = size.width / n
      val barW = (slot * 0.52f).coerceAtMost(28.dp.toPx())
      drawLine(baseColor, Offset(0f, size.height - 0.5f), Offset(size.width, size.height - 0.5f), strokeWidth = 1.dp.toPx())
      values.forEachIndexed { i, v ->
        val h = ((v / max) * (size.height - 4.dp.toPx())).toFloat()
        if (h > 0f) {
          val x = i * slot + (slot - barW) / 2f
          drawRoundRect(
            color = if (highlightIndex < 0 || i == highlightIndex) barColor else dimColor,
            topLeft = Offset(x, size.height - h),
            size = Size(barW, h),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
          )
        }
      }
    }
    Spacer(modifier = Modifier.height(6.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
      labels.forEachIndexed { i, l ->
        Text(
          l,
          fontSize = 10.sp,
          color = if (i == highlightIndex) palette.ink else palette.muted,
          fontWeight = if (i == highlightIndex) FontWeight.SemiBold else FontWeight.Normal,
          textAlign = TextAlign.Center,
          maxLines = 1,
          softWrap = false,
          modifier = Modifier.weight(1f)
        )
      }
    }
  }
}

/** Ekran üst başlığı: büyük başlık + alt satır + sağda isteğe bağlı eylemler. */
@Composable
fun ScreenHeader(title: String, subtitle: String? = null, actions: @Composable () -> Unit = {}) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 12.dp, bottom = 4.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
      if (subtitle != null) {
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = LocalAppPalette.current.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
    }
    Spacer(modifier = Modifier.width(8.dp))
    actions()
  }
}
