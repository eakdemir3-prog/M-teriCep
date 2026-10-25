package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.ui.theme.Slate500
import com.example.viewmodel.DialogState
import com.example.viewmodel.MusteriCepViewModel
import com.example.viewmodel.Screen

@Composable
fun SettingsScreen(
  viewModel: MusteriCepViewModel,
  modifier: Modifier = Modifier
) {
  val profile by viewModel.profile.collectAsState()
  var showProfileEditDialog by remember { mutableStateOf(false) }
  var showKvkkDialog by remember { mutableStateOf(false) }
  var showFirebaseInfoDialog by remember { mutableStateOf(false) }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item {
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = "Ayarlar & Profil",
        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = "İşletme bilgileri, veri yedekleme ve güvenlik ayarları",
        style = MaterialTheme.typography.bodySmall,
        color = Slate500
      )
    }

    // 1. İşletme Profili Kartı
    item {
      ElevatedCard(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Store, contentDescription = null, tint = NavyPrimary)
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(profile.businessName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Yetkili: ${profile.ownerName} • ${profile.city}", style = MaterialTheme.typography.bodySmall, color = Slate500)
              }
            }

            OutlinedButton(
              onClick = { showProfileEditDialog = true },
              shape = RoundedCornerShape(10.dp)
            ) {
              Text("Düzenle", fontSize = 12.sp)
            }
          }

          Spacer(modifier = Modifier.height(12.dp))
          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
          Spacer(modifier = Modifier.height(10.dp))

          // Notifications Switch
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Notifications, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text("Vade Hatırlatıcıları", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                Text("Günü gelen alacaklar için uyarı ver", fontSize = 11.sp, color = Slate500)
              }
            }

            Switch(
              checked = profile.notificationsEnabled,
              onCheckedChange = {
                viewModel.updateBusinessProfile(
                  profile.businessName,
                  profile.ownerName,
                  profile.phone,
                  profile.city,
                  it
                )
              }
            )
          }
        }
      }
    }

    // 2. Modüller ve Hızlı Menü
    item {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(6.dp)) {
          SettingsMenuItem(
            title = "WhatsApp Mesaj Şablonları",
            subtitle = "5 hazır şablonu özelleştirin",
            icon = Icons.Default.Send,
            iconTint = Color(0xFF25D366),
            onClick = { viewModel.navigate(Screen.WhatsAppTemplates) }
          )

          HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

          SettingsMenuItem(
            title = "Raporlar & Excel (CSV) Aktarımı",
            subtitle = "Finansal özet, müşteri dışa/içe aktarma",
            icon = Icons.Default.FileDownload,
            iconTint = EmeraldPrimary,
            onClick = { viewModel.navigate(Screen.ReportsAndExport) }
          )

          HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

          SettingsMenuItem(
            title = "Abonelik ve Paket Limitleri",
            subtitle = "Mevcut: ${profile.activeTier.title} Planı (${profile.activeTier.maxCustomerLimit} Müşteri)",
            icon = Icons.Default.Paid,
            iconTint = AmberWarning,
            onClick = { viewModel.navigate(Screen.Pricing) }
          )
        }
      }
    }

    // 3. Bulut Senkronizasyon (Firebase) Durum Paneli (CRITICAL HONEST PROMPT REQUIREMENT)
    item {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CloudDone, contentDescription = null, tint = EmeraldPrimary)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text("Veri Saklama & Bulut Durumu", fontWeight = FontWeight.Bold, fontSize = 15.sp)
              Text("Cihazda Güvenli Yerel SQLite (Room)", style = MaterialTheme.typography.bodySmall, color = EmeraldPrimary)
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          Text(
            text = "Tüm müşteri, borç ve tahsilat kayıtlarınız cihazınızda yüksek güvenlikli yerel veritabanında saklanır. İnternet bağlantısına ihtiyaç duymadan %100 çevrimdışı çalışır.",
            style = MaterialTheme.typography.bodySmall,
            color = Slate500
          )

          Spacer(modifier = Modifier.height(12.dp))

          OutlinedButton(
            onClick = { showFirebaseInfoDialog = true },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Default.CloudQueue, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Çoklu Cihaz & Firebase Kılavuzu")
          }
        }
      }
    }

    // 4. KVKK & Gizlilik Politikası
    item {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
          .fillMaxWidth()
          .clickable { showKvkkDialog = true }
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.Security, contentDescription = null, tint = NavyPrimary)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text("KVKK & Gizlilik Bildirimi", fontWeight = FontWeight.Bold, fontSize = 14.sp)
              Text("Kişisel ve finansal veri işleme şartları", style = MaterialTheme.typography.bodySmall, color = Slate500)
            }
          }
          Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Slate500, modifier = Modifier.size(18.dp))
        }
      }
    }

    // 5. Veri Yönetimi & Sıfırlama
    item {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text("Veri Yönetimi & Test İşlemleri", fontWeight = FontWeight.Bold, fontSize = 14.sp)
          Spacer(modifier = Modifier.height(10.dp))

          // Load Demo Data
          Button(
            onClick = { viewModel.loadDemoData() },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("btn_load_sample_data")
          ) {
            Icon(Icons.Default.Refresh, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Örnek Esnaf Verisi Yükle", color = NavyPrimary)
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Clear all
          OutlinedButton(
            onClick = {
              viewModel.openDialog(
                DialogState.ConfirmDelete(
                  title = "Tüm Verileri Sıfırla",
                  message = "Tüm müşteri kayıtları, borç ve tahsilat hareketleri kalıcı olarak silinecektir. Devam etmek istiyor musunuz?",
                  onConfirm = { viewModel.clearAllData() }
                )
              )
            },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("btn_clear_all_data")
          ) {
            Icon(Icons.Default.DeleteForever, contentDescription = null, tint = RoseError, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Tüm Verileri Temizle & Sıfırla", color = RoseError)
          }
        }
      }
    }

    item {
      Spacer(modifier = Modifier.height(84.dp))
    }
  }

  // Dialog: Edit Business Profile
  if (showProfileEditDialog) {
    var name by remember { mutableStateOf(profile.businessName) }
    var owner by remember { mutableStateOf(profile.ownerName) }
    var phone by remember { mutableStateOf(profile.phone) }
    var city by remember { mutableStateOf(profile.city) }

    AlertDialog(
      onDismissRequest = { showProfileEditDialog = false },
      title = { Text("İşletme Profilini Düzenle", fontWeight = FontWeight.Bold) },
      text = {
        Column {
          OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("İşletme / Dükkan Adı") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = owner,
            onValueChange = { owner = it },
            label = { Text("Yetkili Adı") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("İşletme Telefonu") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = city,
            onValueChange = { city = it },
            label = { Text("Şehir") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.updateBusinessProfile(name, owner, phone, city, profile.notificationsEnabled)
            showProfileEditDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
        ) {
          Text("Kaydet")
        }
      },
      dismissButton = {
        TextButton(onClick = { showProfileEditDialog = false }) {
          Text("İptal")
        }
      }
    )
  }

  // Dialog: Firebase Configuration Transparency
  if (showFirebaseInfoDialog) {
    AlertDialog(
      onDismissRequest = { showFirebaseInfoDialog = false },
      title = { Text("Firebase Bulut Entegrasyonu", fontWeight = FontWeight.Bold) },
      text = {
        Column {
          Text(
            text = "MüşteriCep, esnafımızın verilerini güvenceye almak için varsayılan olarak %100 yerel şifrelenmiş SQLite/Room altyapısında çalışmaktadır.\n\n" +
              "Birden fazla telefonda veya çalışanlarla bulut senkronizasyonu kurmak için:\n" +
              "1. Firebase Console üzerinde bir Firestore projesi oluşturulur.\n" +
              "2. Google Sign-In ve Firestore kuralları (kullanıcı kimliğine göre kısıtlanmış) tanımlanır.\n" +
              "3. Yapılandırma dosyası (google-services.json) projeye eklenir.\n\n" +
              "Şu anki yerel modda verileriniz tamamen sizin cihazınızdadır ve üçüncü taraflara aktarılmaz.",
            style = MaterialTheme.typography.bodySmall
          )
        }
      },
      confirmButton = {
        Button(onClick = { showFirebaseInfoDialog = false }) {
          Text("Anladım")
        }
      }
    )
  }

  // Dialog: KVKK & Privacy
  if (showKvkkDialog) {
    AlertDialog(
      onDismissRequest = { showKvkkDialog = false },
      title = { Text("KVKK & Gizlilik Politikası", fontWeight = FontWeight.Bold) },
      text = {
        Column {
          Text(
            text = "6698 sayılı Kişisel Verilerin Korunması Kanunu (KVKK) Kapsamında:\n\n" +
              "• Veri Sorumlusu: Uygulamayı kullanan işletme yetkilisidir.\n" +
              "• Veri İşleme Amacı: Müşteri borç ve tahsilat takibinin yapılması ve fatura/ödeme hatırlatmalarının WhatsApp aracılığıyla iletilmesi.\n" +
              "• Verilerin Saklanması: Girilen tüm müşteri verileri kullanıcının kendi cihazında güvenle depolanır. Harici yapay zekâ veya üçüncü taraf sunuculara otomatik olarak aktarılmaz.\n" +
              "• Haklarınız: Verilerinizi Excel (CSV) olarak dışa aktarabilir, silebilir ve istediğiniz zaman sıfırlayabilirsiniz.\n\n" +
              "*İşbu metin bilgilendirme amaçlı olup yasal danışmanlık yerine geçmez.",
            style = MaterialTheme.typography.bodySmall
          )
        }
      },
      confirmButton = {
        Button(onClick = { showKvkkDialog = false }) {
          Text("Kapat")
        }
      }
    )
  }
}

@Composable
private fun SettingsMenuItem(
  title: String,
  subtitle: String,
  icon: ImageVector,
  iconTint: Color,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(14.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
      Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
      Spacer(modifier = Modifier.width(14.dp))
      Column {
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Slate500)
      }
    }
    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Slate500, modifier = Modifier.size(16.dp))
  }
}
