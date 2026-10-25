package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.RoseError

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickActionBottomSheet(
  onDismiss: () -> Unit,
  onAddDebt: () -> Unit,
  onAddCollection: () -> Unit,
  onAddCustomer: () -> Unit,
  onOpenCalculator: () -> Unit,
  onOpenTemplates: () -> Unit,
  onAddAppointment: (() -> Unit)? = null
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .padding(bottom = 32.dp)
    ) {
      Text(
        text = "Hızlı İşlemler",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = "Yapmak istediğiniz işlemi seçin",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Spacer(modifier = Modifier.height(20.dp))

      if (onAddAppointment != null) {
        QuickActionRow(
          title = "Randevu Ekle",
          subtitle = "Telefonla veya yüz yüze alınan randevuyu takvime yaz",
          icon = Icons.Default.Event,
          accentColor = NavyPrimary,
          testTag = "quick_action_add_appointment",
          onClick = {
            onDismiss()
            onAddAppointment()
          }
        )
        Spacer(modifier = Modifier.height(10.dp))
      }

      // 1. Hızlı Tahsilat Al (Yeşil)
      QuickActionRow(
        title = "Hızlı Tahsilat Al",
        subtitle = "Müşteriden yapılan ödemeyi anında kaydet",
        icon = Icons.Default.MonetizationOn,
        accentColor = EmeraldPrimary,
        testTag = "quick_action_add_collection",
        onClick = {
          onDismiss()
          onAddCollection()
        }
      )

      Spacer(modifier = Modifier.height(10.dp))

      // 2. Hızlı Borç Ekle (Kırmızı/Rose)
      QuickActionRow(
        title = "Yeni Borç Kaydet",
        subtitle = "Açık hesap, mal satışı veya hizmet borcu gir",
        icon = Icons.Default.ReceiptLong,
        accentColor = RoseError,
        testTag = "quick_action_add_debt",
        onClick = {
          onDismiss()
          onAddDebt()
        }
      )

      Spacer(modifier = Modifier.height(10.dp))

      // 3. Yeni Müşteri Ekle (Lacivert)
      QuickActionRow(
        title = "Yeni Müşteri Ekle",
        subtitle = "İsim, telefon ve adres bilgileriyle kaydet",
        icon = Icons.Default.PersonAdd,
        accentColor = NavyPrimary,
        testTag = "quick_action_add_customer",
        onClick = {
          onDismiss()
          onAddCustomer()
        }
      )

      Spacer(modifier = Modifier.height(10.dp))

      // 4. Hızlı Esnaf Hesap Makinesi (Amber)
      QuickActionRow(
        title = "Hızlı Hesap Makinesi",
        subtitle = "Tutarları topla, iskonto hesapla",
        icon = Icons.Default.Calculate,
        accentColor = AmberWarning,
        testTag = "quick_action_calculator",
        onClick = {
          onDismiss()
          onOpenCalculator()
        }
      )

      Spacer(modifier = Modifier.height(10.dp))

      // 5. WhatsApp Mesaj Şablonları
      QuickActionRow(
        title = "WhatsApp Şablonları",
        subtitle = "Hazır hatırlatma ve makbuz mesajlarını düzenle",
        icon = Icons.Default.Send,
        accentColor = Color(0xFF25D366),
        testTag = "quick_action_templates",
        onClick = {
          onDismiss()
          onOpenTemplates()
        }
      )
    }
  }
}

@Composable
private fun QuickActionRow(
  title: String,
  subtitle: String,
  icon: ImageVector,
  accentColor: Color,
  testTag: String,
  onClick: () -> Unit
) {
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .clickable(onClick = onClick)
      .testTag(testTag),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    shape = RoundedCornerShape(16.dp)
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(44.dp)
          .clip(CircleShape)
          .background(accentColor.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = accentColor,
          modifier = Modifier.size(24.dp)
        )
      }

      Spacer(modifier = Modifier.width(14.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
          fontSize = 15.sp,
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}
