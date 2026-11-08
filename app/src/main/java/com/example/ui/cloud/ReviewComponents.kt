package com.example.ui.cloud

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cloud.Review
import com.example.ui.theme.LocalAppPalette
import com.example.util.Formatters
import java.util.Locale

val StarGold = Color(0xFFF59E0B)

fun ratingText(avg: Double): String = String.format(Locale("tr", "TR"), "%.1f", avg)

/** "★ 4,8 (12)" özeti. Hiç yorum yoksa hiçbir şey çizmez. */
@Composable
fun RatingBadge(avg: Double, count: Int, size: Dp = 14.dp) {
  if (count <= 0) return
  Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.semantics { contentDescription = "Puan ${ratingText(avg)}, $count yorum" }) {
    Icon(Icons.Filled.Star, contentDescription = null, tint = StarGold, modifier = Modifier.size(size))
    Spacer(modifier = Modifier.width(3.dp))
    Text(ratingText(avg), fontWeight = FontWeight.Bold, fontSize = (size.value - 1).sp)
    Text(" ($count)", color = LocalAppPalette.current.muted, fontSize = (size.value - 2).sp)
  }
}

@Composable
fun Stars(rating: Int, size: Dp = 14.dp) {
  Row {
    (1..5).forEach { i ->
      Icon(
        if (i <= rating) Icons.Filled.Star else Icons.Outlined.StarOutline,
        contentDescription = null,
        tint = if (i <= rating) StarGold else LocalAppPalette.current.inactive,
        modifier = Modifier.size(size)
      )
    }
  }
}

@Composable
fun ReviewItem(r: Review) {
  Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(r.customerName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
      Stars(r.rating, 13.dp)
    }
    val meta = listOf(r.serviceName, if (r.createdAt > 0) Formatters.formatShortDate(r.createdAt) else "").filter { it.isNotBlank() }.joinToString(" • ")
    if (meta.isNotBlank()) Text(meta, fontSize = 11.sp, color = LocalAppPalette.current.muted)
    if (r.comment.isNotBlank()) {
      Spacer(modifier = Modifier.height(4.dp))
      Text(r.comment, style = MaterialTheme.typography.bodyMedium)
    }
  }
}

@Composable
fun ReviewsDialog(title: String, avg: Double, count: Int, reviews: List<Review>, onDismiss: () -> Unit) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Column {
        Text(title, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        RatingBadge(avg, count, 16.dp)
      }
    },
    text = {
      if (reviews.isEmpty()) Text("Henüz yorum yok.")
      else LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
        items(reviews, key = { it.id }) { r ->
          ReviewItem(r)
          HorizontalDivider(color = LocalAppPalette.current.cardBorder)
        }
      }
    },
    confirmButton = { TextButton(onClick = onDismiss) { Text("Kapat") } }
  )
}

/** Müşterinin puan ve yorum yazdığı pencere. */
@Composable
fun WriteReviewDialog(businessName: String, serviceName: String, onDismiss: () -> Unit, onSubmit: (Int, String) -> Unit) {
  var rating by remember { mutableIntStateOf(0) }
  var comment by remember { mutableStateOf("") }
  val labels = listOf("", "Çok kötü", "Kötü", "İdare eder", "İyi", "Harika")
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("$businessName nasıldı?", fontWeight = FontWeight.Bold) },
    text = {
      Column {
        Text(serviceName, style = MaterialTheme.typography.bodySmall, color = LocalAppPalette.current.muted)
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
          (1..5).forEach { i ->
            Icon(
              if (i <= rating) Icons.Filled.Star else Icons.Outlined.StarOutline,
              contentDescription = "$i yıldız",
              tint = if (i <= rating) StarGold else LocalAppPalette.current.inactive,
              modifier = Modifier.size(40.dp).clickable { rating = i }
            )
          }
        }
        Text(labels[rating], fontWeight = FontWeight.SemiBold, color = StarGold, modifier = Modifier.padding(top = 4.dp).heightIn(min = 20.dp))
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
          value = comment,
          onValueChange = { if (it.length <= 300) comment = it },
          label = { Text("Yorumunuz (isteğe bağlı)") },
          supportingText = { Text("${comment.length}/300") },
          minLines = 3,
          modifier = Modifier.fillMaxWidth()
        )
        Text("Yorumunuz adınızın baş harfiyle (ör. Ayşe K.) herkese açık görünür.", fontSize = 11.sp, color = LocalAppPalette.current.muted)
      }
    },
    confirmButton = {
      Button(onClick = { onSubmit(rating, comment) }, enabled = rating > 0) { Text("Gönder") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } }
  )
}
