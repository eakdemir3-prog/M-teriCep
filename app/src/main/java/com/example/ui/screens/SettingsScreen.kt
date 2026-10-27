package com.example.ui.screens

import androidx.compose.material.icons.filled.CalendarMonth

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.model.AppThemeMode
import com.example.reminder.DueReminderWorker
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LocalAppPalette
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.RoseError
import com.example.ui.theme.Slate500
import com.example.ui.theme.WhatsAppGreen
import com.example.util.FileShare
import com.example.util.Formatters
import com.example.viewmodel.DialogState
import com.example.viewmodel.MusteriCepViewModel
import com.example.viewmodel.CloudViewModel
import androidx.compose.material.icons.filled.AccountCircle
import com.example.viewmodel.Screen
import kotlinx.coroutines.launch

/** Gizlilik politikası GitHub Pages'te yayınlanınca bu adres güncellenmeli. */
private const val PRIVACY_POLICY_URL = "https://eakdemir3-prog.github.io/M-teriCep/gizlilik.html"

@Composable
fun SettingsScreen(
  viewModel: MusteriCepViewModel,
  cloud: CloudViewModel,
  modifier: Modifier = Modifier
) {
  val cloudUser by cloud.firebaseUser.collectAsState()
  var confirmDeleteAccount by remember { mutableStateOf(false) }
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val profile by viewModel.profile.collectAsState()
  val hasDemo by viewModel.hasDemoData.collectAsState()
  val customers by viewModel.allCustomersWithBalance.collectAsState()
  val palette = LocalAppPalette.current

  var showProfileEditDialog by remember { mutableStateOf(false) }
  var showKvkkDialog by remember { mutableStateOf(false) }

  // Bildirim izni (Android 13+)
  val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
    if (granted) viewModel.setNotifications(true)
    else viewModel.showMessage("Bildirim izni verilmedi. Uygulama çalışmaya devam eder; vadeleri Ana Sayfa'da görebilirsiniz.")
  }

  // Yedek dosyasını kaydet / aç
  val backupSaver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
    if (uri != null) scope.launch {
      try {
        FileShare.writeToUri(context, uri, viewModel.buildBackupJson())
        viewModel.showMessage("Yedek kaydedildi.")
      } catch (e: Exception) {
        viewModel.showMessage("Yedek kaydedilemedi: ${e.message}")
      }
    }
  }
  val backupOpener = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
    if (uri != null) {
      try {
        viewModel.confirmRestore(FileShare.readFromUri(context, uri))
      } catch (e: Exception) {
        viewModel.showMessage("Dosya okunamadı: ${e.message}")
      }
    }
  }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item {
      Spacer(modifier = Modifier.height(8.dp))
      Text("Ayarlar", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
      Text("İşletme bilgileri, görünüm, hatırlatıcılar ve yedekleme", style = MaterialTheme.typography.bodySmall, color = Slate500)
    }

    // 1. İşletme profili
    item {
      SettingsCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Store, contentDescription = null, tint = NavyPrimary)
          Spacer(modifier = Modifier.width(10.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(profile.businessName.ifBlank { "İşletme adı girilmedi" }, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            val sub = listOf(profile.ownerName, profile.city, profile.phone).filter { it.isNotBlank() }.joinToString(" • ")
            Text(sub.ifBlank { "WhatsApp mesajlarında işletme adınız kullanılır" }, style = MaterialTheme.typography.bodySmall, color = Slate500)
          }
          OutlinedButton(onClick = { showProfileEditDialog = true }, shape = RoundedCornerShape(10.dp)) {
            Text("Düzenle", fontSize = 12.sp)
          }
        }
      }
    }

    // Müşteri moduna geçiş (aynı hesapla başka işletmelerden randevu)
    if (cloud.isConfigured) item {
      val palette = com.example.ui.theme.LocalAppPalette.current
      androidx.compose.material3.Card(
        onClick = { cloud.switchMode(context as? android.app.Activity, com.example.cloud.UserRole.CUSTOMER) },
        shape = RoundedCornerShape(16.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = palette.accentSoft),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, palette.ink),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = palette.ink, modifier = Modifier.size(28.dp))
          Spacer(modifier = Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text("Randevu al", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
            Text("Başka işletmelerden randevu almak için müşteri moduna geçin. Aynı hesap, aynı bilgiler.", style = MaterialTheme.typography.bodySmall, color = Slate500)
          }
          Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = palette.ink)
        }
      }
    }

    // Hesap ve online randevu
    item {
      SettingsCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.AccountCircle, contentDescription = null, tint = NavyPrimary)
          Spacer(modifier = Modifier.width(10.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text("Hesap ve online randevu", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(
              cloudUser?.email ?: "Giriş yapılmadı — uygulama sadece defter olarak çalışıyor",
              style = MaterialTheme.typography.bodySmall,
              color = Slate500
            )
          }
        }
        Spacer(modifier = Modifier.height(10.dp))
        if (cloudUser == null) {
          Button(
            onClick = { (context as? android.app.Activity)?.let { cloud.signIn(it, com.example.cloud.UserRole.BUSINESS) } },
            enabled = cloud.isConfigured,
            modifier = Modifier.fillMaxWidth()
          ) { Text(if (cloud.isConfigured) "Google ile giriş yap" else "Online randevu yakında") }
        } else {
          OutlinedButton(onClick = { viewModel.navigate(Screen.BusinessProfile) }, modifier = Modifier.fillMaxWidth()) {
            Text("İşletme vitrini, saatler ve hizmetler")
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = { cloud.signOut() }, modifier = Modifier.weight(1f)) { Text("Çıkış yap") }
            TextButton(onClick = { confirmDeleteAccount = true }, modifier = Modifier.weight(1f)) { Text("Hesabı sil", color = RoseError) }
          }
        }
      }
    }

    // 2. Görünüm (tema)
    item {
      SettingsCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Palette, contentDescription = null, tint = NavyPrimary)
          Spacer(modifier = Modifier.width(10.dp))
          Text("Görünüm", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
          AppThemeMode.entries.forEach { mode ->
            FilterChip(
              selected = profile.themeMode == mode,
              onClick = { viewModel.setTheme(mode) },
              label = {
                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                  Text(mode.label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                  Text(mode.description, fontSize = 11.sp, color = Slate500)
                }
              }
            )
          }
        }
      }
    }

    // 3. Vade hatırlatıcı
    item {
      SettingsCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Notifications, contentDescription = null, tint = AmberWarning)
          Spacer(modifier = Modifier.width(10.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text("Vade hatırlatıcı", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text("Her sabah vadesi gelen ve geciken alacakları bildirir. Müşteriye kendiliğinden mesaj göndermez.", fontSize = 12.sp, color = Slate500)
          }
          Switch(
            checked = profile.notificationsEnabled,
            onCheckedChange = { enable ->
              if (enable && Build.VERSION.SDK_INT >= 33 && !DueReminderWorker.canNotify(context)) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
              } else if (enable && !DueReminderWorker.canNotify(context)) {
                viewModel.showMessage("Bildirimler telefon ayarlarından kapalı. Açmak için uygulama ayarlarına gidin.")
                openAppSettings(context)
              } else {
                viewModel.setNotifications(enable)
              }
            },
            modifier = Modifier.testTag("switch_reminders")
          )
        }
        if (profile.notificationsEnabled) {
          Spacer(modifier = Modifier.height(10.dp))
          Text("Ne zaman hatırlatılsın?", style = MaterialTheme.typography.labelMedium, color = Slate500)
          Spacer(modifier = Modifier.height(6.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            listOf(0 to "Vade günü", 1 to "1 gün önce", 3 to "3 gün önce", 7 to "1 hafta önce").forEach { (d, label) ->
              FilterChip(
                selected = profile.reminderDaysBefore == d,
                onClick = { viewModel.setReminderDays(d) },
                label = { Text(label, fontSize = 12.sp) }
              )
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
          TextButton(onClick = { viewModel.runReminderNow() }) { Text("Şimdi kontrol et") }
        }
      }
    }

    // 4. Modüller
    item {
      SettingsCard(padding = 6) {
        SettingsMenuItem(
          title = "WhatsApp mesaj şablonları",
          subtitle = "Hatırlatma ve teşekkür mesajlarını düzenleyin",
          icon = Icons.Default.Send,
          iconTint = WhatsAppGreen,
          onClick = { viewModel.navigate(Screen.WhatsAppTemplates) }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp), color = palette.cardBorder)
        SettingsMenuItem(
          title = "Raporlar ve Excel (CSV)",
          subtitle = "Dışa aktar, toplu müşteri içe aktar",
          icon = Icons.Default.FileDownload,
          iconTint = EmeraldPrimary,
          onClick = { viewModel.navigate(Screen.ReportsAndExport) }
        )
        if (com.example.model.Billing.ENABLED) {
        HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp), color = palette.cardBorder)
        SettingsMenuItem(
          title = "Paketler",
          subtitle = "${profile.activeTier.title} • ${if (profile.activeTier.maxCustomerLimit == Int.MAX_VALUE) "sınırsız" else profile.activeTier.maxCustomerLimit.toString()} müşteri",
          icon = Icons.Default.Paid,
          iconTint = AmberWarning,
          onClick = { viewModel.navigate(Screen.Pricing) }
        )
        }
      }
    }

    // 5. Yedekleme
    item {
      SettingsCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Backup, contentDescription = null, tint = EmeraldPrimary)
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text("Yedekleme", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text("Veriler sadece bu telefonda durur", style = MaterialTheme.typography.bodySmall, color = Slate500)
          }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          "Telefon kaybolursa veya değişirse kayıtlarınız gitmesin diye düzenli yedek alın. Yedek dosyasını Google Drive'a veya kendinize WhatsApp'tan gönderebilirsiniz.",
          style = MaterialTheme.typography.bodySmall,
          color = Slate500
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
          onClick = {
            scope.launch {
              FileShare.shareText(
                context,
                "MusteriCep_Yedek_${Formatters.formatDate(System.currentTimeMillis()).replace('.', '-')}.json",
                "application/json",
                viewModel.buildBackupJson(),
                "Yedeği gönder"
              )
            }
          },
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Yedek al ve gönder (Drive, WhatsApp...)")
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
          OutlinedButton(
            onClick = {
              backupSaver.launch("MusteriCep_Yedek_${Formatters.formatDate(System.currentTimeMillis()).replace('.', '-')}.json")
            },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f)
          ) {
            Text("Dosyaya kaydet", fontSize = 13.sp)
          }
          OutlinedButton(
            onClick = { backupOpener.launch(arrayOf("application/json", "application/octet-stream", "text/plain", "*/*")) },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Geri yükle", fontSize = 13.sp)
          }
        }
      }
    }

    // 6. Gizlilik
    item {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, palette.cardBorder),
        modifier = Modifier
          .fillMaxWidth()
          .clickable { showKvkkDialog = true }
      ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Security, contentDescription = null, tint = NavyPrimary)
          Spacer(modifier = Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text("KVKK ve gizlilik", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("Verileriniz nerede, nasıl saklanır", style = MaterialTheme.typography.bodySmall, color = Slate500)
          }
          Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Slate500, modifier = Modifier.size(18.dp))
        }
      }
    }

    // 7. Veri yönetimi
    item {
      SettingsCard {
        Text("Veri yönetimi", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(10.dp))
        if (hasDemo) {
          OutlinedButton(
            onClick = { viewModel.clearDemoData() },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Default.Science, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Örnek verileri sil", color = AmberWarning)
          }
        } else if (customers.none { !it.customer.isDemo }) {
          OutlinedButton(
            onClick = { viewModel.loadDemoData() },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("btn_load_sample_data")
          ) {
            Icon(Icons.Default.Science, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Örnek verilerle dene", color = NavyPrimary)
          }
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
          onClick = {
            viewModel.openDialog(
              DialogState.ConfirmAction(
                title = "Tüm verileri sil",
                message = "Bütün müşteriler, borçlar ve tahsilatlar bu telefondan kalıcı olarak silinecek. Bu işlem geri alınamaz. Önce yedek almanızı öneririz.",
                confirmLabel = "Hepsini sil",
                isDestructive = true,
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
          Text("Tüm verileri sil", color = RoseError)
        }
      }
    }

    item {
      Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("MüşteriCep ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelSmall, color = Slate500)
        Text("EA Vision Labs", style = MaterialTheme.typography.labelSmall, color = Slate500)
      }
      Spacer(modifier = Modifier.height(90.dp))
    }
  }

  if (showProfileEditDialog) {
    var name by remember { mutableStateOf(profile.businessName) }
    var owner by remember { mutableStateOf(profile.ownerName) }
    var phone by remember { mutableStateOf(profile.phone) }
    var city by remember { mutableStateOf(profile.city) }

    AlertDialog(
      onDismissRequest = { showProfileEditDialog = false },
      title = { Text("İşletme bilgileri", fontWeight = FontWeight.Bold) },
      text = {
        Column {
          OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("İşletme / dükkân adı") }, singleLine = true, modifier = Modifier.fillMaxWidth())
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(value = owner, onValueChange = { owner = it }, label = { Text("Yetkili adı") }, singleLine = true, modifier = Modifier.fillMaxWidth())
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("İşletme telefonu") }, singleLine = true, modifier = Modifier.fillMaxWidth())
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("Şehir") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
      },
      confirmButton = {
        Button(onClick = {
          viewModel.updateBusinessProfile(name, owner, phone, city)
          showProfileEditDialog = false
        }) { Text("Kaydet") }
      },
      dismissButton = {
        TextButton(onClick = { showProfileEditDialog = false }) { Text("İptal") }
      }
    )
  }

  if (confirmDeleteAccount) {
    AlertDialog(
      onDismissRequest = { confirmDeleteAccount = false },
      title = { Text("Hesap silinsin mi?", fontWeight = FontWeight.Bold) },
      text = { Text("Online hesabınız silinir, vitrininiz gizlenir ve yeni randevu alınamaz. Telefondaki borç/tahsilat defteriniz silinmez.") },
      confirmButton = { Button(onClick = { confirmDeleteAccount = false; cloud.deleteAccount() }) { Text("Hesabı sil") } },
      dismissButton = { TextButton(onClick = { confirmDeleteAccount = false }) { Text("Vazgeç") } }
    )
  }

  if (showKvkkDialog) {
    AlertDialog(
      onDismissRequest = { showKvkkDialog = false },
      title = { Text("KVKK ve gizlilik", fontWeight = FontWeight.Bold) },
      text = {
        Text(
          text = "6698 sayılı KVKK kapsamında bilgilendirme:\n\n" +
            "• Veri sorumlusu, uygulamaya müşteri bilgisi giren işletmedir.\n" +
            "• Borç ve tahsilat kayıtları yalnızca bu telefonda saklanır.\n" +
            "• Online randevu kullanılırsa işletme vitrini, hizmetler ve randevu bilgileri (müşteri adı, telefonu, randevu saati) " +
            "Google Firebase sunucularında saklanır ve yalnızca ilgili işletme ile müşteri tarafından görülür.\n" +
            "• Uygulama yapay zekâ servisi kullanmaz, verileri reklam için paylaşmaz.\n" +
            "• Telefonunuzun Google yedeklemesi açıksa uygulama verileri Android tarafından Google hesabınıza şifreli olarak yedeklenebilir.\n" +
            "• WhatsApp mesajları sizin onayınızla, sizin WhatsApp hesabınızdan gönderilir.\n" +
            "• Verilerinizi dilediğiniz an Excel (CSV) veya yedek dosyası olarak dışa aktarabilir, tamamen silebilirsiniz.\n" +
            "• Müşterilerinizin bilgilerini kaydetmeden önce onları bilgilendirmek sizin sorumluluğunuzdadır.\n\n" +
            "Bu metin bilgilendirme amaçlıdır, hukuki danışmanlık yerine geçmez.",
          style = MaterialTheme.typography.bodySmall
        )
      },
      confirmButton = {
        Button(onClick = { showKvkkDialog = false }) { Text("Kapat") }
      },
      dismissButton = {
        TextButton(onClick = {
          try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL)))
          } catch (e: Exception) {
            viewModel.showMessage("Tarayıcı açılamadı.")
          }
        }) { Text("Gizlilik politikası") }
      }
    )
  }
}

private fun openAppSettings(context: android.content.Context) {
  try {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
      putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
  } catch (e: Exception) {
    // Bazı cihazlarda yok; sessiz geç.
  }
}

@Composable
private fun SettingsCard(padding: Int = 16, content: @Composable () -> Unit) {
  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, LocalAppPalette.current.cardBorder),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(padding.dp)) { content() }
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
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
    Spacer(modifier = Modifier.width(14.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
      Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Slate500)
    }
    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Slate500, modifier = Modifier.size(16.dp))
  }
}
