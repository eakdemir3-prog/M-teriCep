package com.example.ui.cloud

import com.example.util.tr

import androidx.compose.material.icons.filled.Star

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.cloud.Appointment
import com.example.cloud.AppointmentStatus
import com.example.cloud.BusinessProfile
import com.example.cloud.ServiceItem
import com.example.cloud.UserRole
import com.example.model.PaymentMethod
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LocalAppPalette
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.RoseError
import com.example.ui.theme.Slate500
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.moneyStyle
import com.example.util.Formatters
import com.example.util.WhatsAppHelper
import com.example.viewmodel.CloudViewModel

@Composable
fun BusinessAppointmentsScreen(
  cloud: CloudViewModel,
  onOpenBusinessProfile: () -> Unit,
  onAddAppointment: () -> Unit,
  onOpenPlans: () -> Unit = {}
) {
  val tier by cloud.tier.collectAsState()
  val monthCount by cloud.monthAppointmentCount.collectAsState()
  val context = LocalContext.current
  val user by cloud.firebaseUser.collectAsState()
  val business by cloud.myBusiness.collectAsState()
  val services by cloud.myServices.collectAsState()
  val pending by cloud.pendingAppointments.collectAsState()
  val dayList by cloud.dayAppointments.collectAsState()
  val selectedDay by cloud.selectedDay.collectAsState()
  val busy by cloud.busy.collectAsState()
  val palette = LocalAppPalette.current
  var ledgerFor by remember { mutableStateOf<Appointment?>(null) }
  val staffList by cloud.myStaff.collectAsState()
  var staffFilter by remember { mutableStateOf<String?>(null) }
  var notifyFor by remember { mutableStateOf<Pair<Appointment, AppointmentStatus>?>(null) }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      Spacer(modifier = Modifier.height(8.dp))
      Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
          Text(tr("Randevular"), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
          Text(
            business?.name ?: tr("Takvim ve randevu istekleri"),
            style = MaterialTheme.typography.bodySmall,
            color = Slate500
          )
        }
        if (user != null) {
          IconButton(onClick = onOpenBusinessProfile) {
            Icon(Icons.Default.Settings, contentDescription = tr("Randevu ayarları"), tint = NavyPrimary)
          }
        }
      }
    }

    // Giriş yapılmamış (sadece defter modu)
    if (user == null) {
      item {
        InfoCard(
          title = tr("Online randevu için giriş yapın"),
          text = tr("Müşterileriniz uygulamayı indirip işletmenizi seçerek randevu alabilsin. Giriş yapmak ücretsizdir; defterdeki kayıtlarınız olduğu gibi kalır."),
          action = if (cloud.isConfigured) tr("Google ile giriş yap") else null,
          onAction = { (context as? Activity)?.let { cloud.signIn(it, UserRole.BUSINESS) } }
        )
      }
      item { Spacer(modifier = Modifier.height(90.dp)) }
      return@LazyColumn
    }

    if (business == null) {
      item {
        InfoCard(
          title = tr("İşletme vitrininizi oluşturun"),
          text = tr("Adınız, çalışma saatleriniz ve hizmetleriniz (süre ve fiyat) girildiğinde müşterileriniz randevu alabilir."),
          action = tr("Şimdi oluştur"),
          onAction = onOpenBusinessProfile
        )
      }
    } else {
      if (services.none { it.isActive }) {
        item {
          InfoCard(
            title = tr("Hizmet ekleyin"),
            text = tr("Müşteri randevu alırken hizmet seçer (örn. Saç kesimi, 30 dk, 300 ₺). En az bir hizmet ekleyin."),
            action = tr("Hizmet ekle"),
            onAction = onOpenBusinessProfile
          )
        }
      }
      val limit = tier.monthlyAppointments
      val used = monthCount
      if (com.example.model.Billing.ENABLED && limit != com.example.model.UNLIMITED && used != null) {
        item {
          val palette = LocalAppPalette.current
          val full = used >= limit
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = if (full) palette.warningSoft else MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, if (full) palette.warning.copy(alpha = 0.4f) else palette.cardBorder),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tr("Bu ay: {0} / {1} randevu", used, limit), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                TextButton(onClick = onOpenPlans) { Text(tr("Paketler"), color = palette.ink) }
              }
              androidx.compose.material3.LinearProgressIndicator(
                progress = { (used.toFloat() / limit).coerceIn(0f, 1f) },
                color = if (full) palette.warning else palette.ink,
                trackColor = palette.cardBorder,
                modifier = Modifier.fillMaxWidth().height(6.dp)
              )
              if (full) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  tr("Sınır doldu: müşterileriniz bu ay online randevu alamıyor. Paketinizi yükseltince hemen açılır."),
                  style = MaterialTheme.typography.bodySmall, color = palette.warning
                )
              }
            }
          }
        }
      }
      item {
        val b0 = business!!
        val myReviews by cloud.myBusinessReviews.collectAsState()
        var showMine by remember { mutableStateOf(false) }
        Card(
          onClick = { cloud.loadMyBusinessReviews(); showMine = true },
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, LocalAppPalette.current.cardBorder),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Star, contentDescription = null, tint = StarGold, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(tr("Değerlendirmeler"), fontWeight = FontWeight.SemiBold)
              if (b0.ratingCount > 0) RatingBadge(b0.ratingAvg, b0.ratingCount)
              else Text(tr("Henüz yorum yok. Tamamlanan randevulardan sonra müşterileriniz puan verebilir."), style = MaterialTheme.typography.bodySmall, color = Slate500)
            }
            Text(tr("Gör"), color = LocalAppPalette.current.ink, fontWeight = FontWeight.SemiBold)
          }
        }
        if (showMine) ReviewsDialog(b0.name, b0.ratingAvg, b0.ratingCount, myReviews) { showMine = false }
      }
      item { JoinCodeCard(business!!) }
    }

    // Onay bekleyen istekler
    if (pending.isNotEmpty()) {
      item { SectionTitle(tr("Onay bekleyen istekler ({0})", pending.size), AmberWarning) }
      items(pending, key = { "p_" + it.id }) { a ->
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.5f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Text(tr("{0} • {1}", Formatters.formatShortDate(a.start), timeOf(a.start)), fontWeight = FontWeight.Bold)
            Text(tr("{0} • {1} ({2})", a.customerName, a.serviceName, durationText(a.durationMinutes)), style = MaterialTheme.typography.bodyMedium)
            if (a.note.isNotBlank()) Text(tr("Not: {0}", a.note), style = MaterialTheme.typography.bodySmall, color = Slate500)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Button(
                onClick = { cloud.setAppointmentStatus(a, AppointmentStatus.CONFIRMED); notifyFor = a to AppointmentStatus.CONFIRMED },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                modifier = Modifier.weight(1f)
              ) { Text(tr("Onayla")) }
              OutlinedButton(
                onClick = { cloud.setAppointmentStatus(a, AppointmentStatus.REJECTED); notifyFor = a to AppointmentStatus.REJECTED },
                modifier = Modifier.weight(1f)
              ) { Text("Reddet", color = RoseError) }
            }
          }
        }
      }
    }

    // Gün seçici
    item {
      SectionTitle(tr("Takvim"))
      Spacer(modifier = Modifier.height(6.dp))
      DayStrip(selectedDay = selectedDay, onSelect = { cloud.selectDay(it) }, days = 45, business = business, startOffset = -7)
    }

    if (staffList.isNotEmpty()) {
      item {
        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          item { FilterChip(selected = staffFilter == null, onClick = { staffFilter = null }, label = { Text(tr("Tüm personel")) }) }
          items(staffList, key = { "sf_" + it.id }) { st ->
            FilterChip(selected = staffFilter == st.id, onClick = { staffFilter = st.id }, label = { Text(st.name) })
          }
        }
      }
    }
    val visible = dayList.filter { it.status != AppointmentStatus.CANCELLED && it.status != AppointmentStatus.REJECTED }
      .filter { staffFilter == null || it.staffId == staffFilter }
    val cancelled = dayList.size - visible.size
    if (visible.isEmpty()) {
      item {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(Icons.Default.EventAvailable, contentDescription = null, tint = Slate500, modifier = Modifier.size(36.dp))
          Text(tr("{0} için randevu yok", Formatters.formatFriendlyDate(selectedDay)), style = MaterialTheme.typography.bodyMedium, color = Slate500)
          if (business != null && services.isNotEmpty()) {
            TextButton(onClick = onAddAppointment) { Text(tr("Randevu ekle")) }
          }
        }
      }
    } else {
      val revenue = visible.filter { it.status != AppointmentStatus.NO_SHOW }.sumOf { it.price }
      item {
        Text(
          tr("{0} randevu • tahmini {1}", visible.size, Formatters.formatCurrency(revenue)) + if (cancelled > 0) tr(" • {0} iptal", cancelled) else "",
          style = MaterialTheme.typography.bodySmall,
          color = Slate500
        )
      }
      items(visible, key = { "d_" + it.id }) { a ->
        BusinessAppointmentRow(
          a = a,
          onStatus = { st ->
            cloud.setAppointmentStatus(a, st)
            if (st == AppointmentStatus.CONFIRMED || st == AppointmentStatus.CANCELLED) notifyFor = a to st
          },
          onComplete = { ledgerFor = a },
          onCall = {
            if (a.customerPhone.isNotBlank()) {
              runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + a.customerPhone.filter { it.isDigit() || it == '+' }))) }
            }
          },
          onWhatsApp = {
            val msg = tr("Merhaba {0}, {1} saat {2} {3} randevunuzu hatırlatırız. {4}", a.customerName, Formatters.formatFriendlyDate(a.start), timeOf(a.start), a.serviceName, a.businessName)
            WhatsAppHelper.openWhatsAppChat(context, a.customerPhone, msg) { err -> android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_LONG).show() }
          }
        )
      }
    }
    item { Spacer(modifier = Modifier.height(if (busy) 120.dp else 90.dp)) }
  }

  notifyFor?.let { (a, st) ->
    if (a.customerPhone.isBlank()) {
      notifyFor = null
    } else {
      val msg = statusMessage(a, st)
      androidx.compose.material3.AlertDialog(
        onDismissRequest = { notifyFor = null },
        title = { Text(tr("Müşteriye bildirilsin mi?"), fontWeight = FontWeight.Bold) },
        text = {
          Column {
            Text(tr("{0} kişisine WhatsApp'tan şu mesaj hazırlanacak:", a.customerName), style = MaterialTheme.typography.bodySmall, color = Slate500)
            Spacer(modifier = Modifier.height(8.dp))
            androidx.compose.material3.Surface(shape = RoundedCornerShape(12.dp), color = LocalAppPalette.current.accentSoft) {
              Text(msg, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp))
            }
          }
        },
        confirmButton = {
          Button(
            onClick = {
              WhatsAppHelper.openWhatsAppChat(context, a.customerPhone, msg) { err ->
                android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_LONG).show()
              }
              notifyFor = null
            },
            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen)
          ) { Text(tr("WhatsApp'tan gönder")) }
        },
        dismissButton = { TextButton(onClick = { notifyFor = null }) { Text(tr("Gerek yok")) } }
      )
    }
  }

  ledgerFor?.let { a ->
    PostToLedgerDialog(
      appointment = a,
      onDismiss = { ledgerFor = null },
      onOnlyComplete = {
        cloud.setAppointmentStatus(a, AppointmentStatus.COMPLETED)
        ledgerFor = null
      },
      onPost = { paid, method ->
        cloud.postToLedger(a, paid, method)
        ledgerFor = null
      }
    )
  }
}

@Composable
private fun BusinessAppointmentRow(
  a: Appointment,
  onStatus: (AppointmentStatus) -> Unit,
  onComplete: () -> Unit,
  onCall: () -> Unit,
  onWhatsApp: () -> Unit
) {
  val palette = LocalAppPalette.current
  var menu by remember { mutableStateOf(false) }
  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, palette.cardBorder),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(modifier = Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
      Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(54.dp)) {
        Text(timeOf(a.start), style = moneyStyle(17), color = NavyPrimary)
        Text(timeOf(a.end), fontSize = 11.sp, color = Slate500)
      }
      Spacer(modifier = Modifier.width(10.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(a.customerName, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
          tr("{0} • {1}", a.serviceName, Formatters.formatCurrency(a.price)) + if (a.staffName.isNotBlank()) tr(" • {0}", a.staffName) else "",
          style = MaterialTheme.typography.bodySmall, color = Slate500, maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
          AppointmentStatusChip(a.status)
          if (a.ledgerPosted) Text(tr("deftere işlendi"), fontSize = 11.sp, color = EmeraldPrimary)
          if (a.customerUid.isBlank()) Text(tr("elle eklendi"), fontSize = 11.sp, color = Slate500)
        }
      }
      if (a.customerPhone.isNotBlank()) {
        IconButton(onClick = onWhatsApp) { Icon(Icons.Default.Share, contentDescription = "WhatsApp", tint = WhatsAppGreen) }
      }
      Column {
        IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, contentDescription = tr("İşlemler")) }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
          if (a.status == AppointmentStatus.PENDING) {
            DropdownMenuItem(text = { Text(tr("Onayla")) }, onClick = { menu = false; onStatus(AppointmentStatus.CONFIRMED) })
          }
          if (a.status.isActive || (a.status == AppointmentStatus.COMPLETED && !a.ledgerPosted)) {
            DropdownMenuItem(text = { Text(tr("Tamamlandı / Deftere işle")) }, onClick = { menu = false; onComplete() })
          }
          if (a.status.isActive) {
            DropdownMenuItem(text = { Text(tr("Gelmedi")) }, onClick = { menu = false; onStatus(AppointmentStatus.NO_SHOW) })
            DropdownMenuItem(text = { Text(tr("İptal et"), color = RoseError) }, onClick = { menu = false; onStatus(AppointmentStatus.CANCELLED) })
          }
          if (a.customerPhone.isNotBlank()) {
            DropdownMenuItem(text = { Text(tr("Ara")) }, leadingIcon = { Icon(Icons.Default.Call, contentDescription = null) }, onClick = { menu = false; onCall() })
          }
        }
      }
    }
  }
}

@Composable
private fun JoinCodeCard(business: BusinessProfile) {
  val context = LocalContext.current
  val palette = LocalAppPalette.current
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = palette.heroBackground),
    border = BorderStroke(1.dp, palette.cardBorder),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
      Column(modifier = Modifier.weight(1f)) {
        Text(tr("Müşteri kodunuz"), style = MaterialTheme.typography.labelMedium, color = palette.heroMuted)
        Text(business.joinCode.ifBlank { "------" }, style = moneyStyle(26), color = if (palette.isDark) palette.success else palette.ink)
        Text(
          if (business.isListed) tr("Listede görünüyorsunuz; kodla da bulunabilirsiniz.") else tr("Listede gizlisiniz; müşteriler sadece kodla bulur."),
          style = MaterialTheme.typography.bodySmall,
          color = palette.heroMuted
        )
      }
      OutlinedButton(onClick = {
        val text = tr("{0} için online randevu: MüşteriCep uygulamasını indirin, \"Müşteriyim\" ile giriş yapın ve {1} kodunu girin.\n", business.name, business.joinCode) +
          "https://play.google.com/store/apps/details?id=${context.packageName}"
        val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }
        runCatching { context.startActivity(Intent.createChooser(send, tr("Kodu paylaş"))) }
      }) {
        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(tr("Paylaş"))
      }
    }
  }
}

@Composable
private fun InfoCard(title: String, text: String, action: String?, onAction: () -> Unit) {
  val palette = LocalAppPalette.current
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, palette.cardBorder),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
      Spacer(modifier = Modifier.height(4.dp))
      Text(text, style = MaterialTheme.typography.bodySmall, color = Slate500)
      if (action != null) {
        Spacer(modifier = Modifier.height(10.dp))
        Button(onClick = onAction) { Text(action) }
      }
    }
  }
}

@Composable
fun PostToLedgerDialog(
  appointment: Appointment,
  onDismiss: () -> Unit,
  onOnlyComplete: () -> Unit,
  onPost: (paid: Boolean, method: PaymentMethod) -> Unit
) {
  var method by remember { mutableStateOf(PaymentMethod.CASH) }
  Dialog(onDismissRequest = onDismiss) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
      Column(modifier = Modifier.padding(20.dp)) {
        Text(tr("Randevu tamamlandı"), style = MaterialTheme.typography.titleLarge)
        Text(
          tr("{0} • {1} • {2}", appointment.customerName, appointment.serviceName, Formatters.formatCurrency(appointment.price)),
          style = MaterialTheme.typography.bodyMedium,
          color = Slate500
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(tr("Ödeme alındıysa yöntemini seçin:"), style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(PaymentMethod.entries.toList()) { m ->
            FilterChip(selected = method == m, onClick = { method = m }, label = { Text(m.label, fontSize = 12.sp) })
          }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Button(
          onClick = { onPost(true, method) },
          colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
          modifier = Modifier.fillMaxWidth()
        ) { Text(tr("Ödendi — tahsilat olarak işle")) }
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedButton(onClick = { onPost(false, method) }, modifier = Modifier.fillMaxWidth()) {
          Text(tr("Ödenmedi — veresiye (borç) yaz"), color = RoseError)
        }
        TextButton(onClick = onOnlyComplete, modifier = Modifier.fillMaxWidth()) {
          Text(tr("Deftere işlemeden sadece tamamla"))
        }
      }
    }
  }
}

/** İşletmenin elle randevu eklemesi (telefonla/yüz yüze gelen istekler). */
@Composable
fun ManualAppointmentDialog(cloud: CloudViewModel, onDismiss: () -> Unit) {
  val business by cloud.myBusiness.collectAsState()
  val services by cloud.myServices.collectAsState()
  val slots by cloud.availableSlots.collectAsState()
  val active = services.filter { it.isActive }
  var name by remember { mutableStateOf("") }
  var phone by remember { mutableStateOf("") }
  var note by remember { mutableStateOf("") }
  var service by remember { mutableStateOf<ServiceItem?>(active.firstOrNull()) }
  var day by remember { mutableStateOf(Formatters.startOfDay(System.currentTimeMillis())) }
  var time by remember { mutableStateOf<Long?>(null) }
  val staffAll by cloud.myStaff.collectAsState()
  val staffFor = staffAll.filter { st -> st.isActive && service?.let { st.canDo(it.id) } ?: true }
  var staffPick by remember { mutableStateOf<com.example.cloud.Staff?>(null) }
  val staffChoice = when {
    staffFor.isEmpty() -> emptyList()
    staffPick != null && staffFor.any { it.id == staffPick?.id } -> listOf(staffPick!!)
    else -> staffFor
  }

  LaunchedEffect(service, day, business, staffChoice.map { it.id }) {
    val b = business
    val s = service
    time = null
    if (b != null && s != null) cloud.loadSlots(b, day, s.durationMinutes, applyNotice = false, staff = staffChoice)
  }

  Dialog(onDismissRequest = onDismiss) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
      Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
        Text(tr("Randevu ekle"), style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(10.dp))
        if (business == null || active.isEmpty()) {
          Text(tr("Önce Randevular > Ayarlar'dan işletme bilgilerini ve en az bir hizmeti girin."), color = Slate500)
          TextButton(onClick = onDismiss) { Text(tr("Tamam")) }
        } else {
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(tr("Müşteri adı *")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
          value = phone, onValueChange = { phone = it }, label = { Text(tr("Telefon")) }, singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(tr("Hizmet"), style = MaterialTheme.typography.labelMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(active) { s ->
            FilterChip(selected = service?.id == s.id, onClick = { service = s }, label = { Text(tr("{0} • {1}", s.name, durationText(s.durationMinutes)), fontSize = 12.sp) })
          }
        }
        if (staffFor.isNotEmpty()) {
          Spacer(modifier = Modifier.height(10.dp))
          Text(tr("Personel"), style = MaterialTheme.typography.labelMedium)
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item { FilterChip(selected = staffPick == null, onClick = { staffPick = null }, label = { Text(tr("Fark etmez"), fontSize = 12.sp) }) }
            items(staffFor, key = { it.id }) { st ->
              FilterChip(selected = staffPick?.id == st.id, onClick = { staffPick = st }, label = { Text(st.name, fontSize = 12.sp) })
            }
          }
        }
        Spacer(modifier = Modifier.height(10.dp))
        DayStrip(selectedDay = day, onSelect = { day = it }, days = 60, business = business)
        Spacer(modifier = Modifier.height(10.dp))
        SlotGrid(slots = slots, selected = time, onSelect = { time = it }, emptyText = tr("Bu gün için boş saat yok (kapalı veya dolu)."))
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text(tr("Not")) }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
          TextButton(onClick = onDismiss) { Text(tr("Vazgeç")) }
          Button(
            onClick = {
              val s = service ?: return@Button
              val t = time ?: return@Button
              cloud.addManualAppointment(name, phone, s, t, note, staffChoice)
              onDismiss()
            },
            enabled = name.isNotBlank() && time != null && service != null
          ) { Text(tr("Kaydet")) }
        }
        }
      }
    }
  }
}

/** İşletmenin müşteriye göndereceği durum mesajı (WhatsApp). */
private fun statusMessage(a: Appointment, st: AppointmentStatus): String {
  val whenText = tr("{0} saat {1}", Formatters.formatFriendlyDate(a.start), timeOf(a.start))
  return when (st) {
    AppointmentStatus.CONFIRMED ->
      tr("Merhaba {0}, {1} {2} randevunuz onaylandı. Sizi bekliyoruz. {3}", a.customerName, whenText, a.serviceName, a.businessName) +
        if (a.businessAddress.isNotBlank()) "\nAdres: ${a.businessAddress}" else ""
    AppointmentStatus.REJECTED ->
      tr("Merhaba {0}, {1} için {2} randevu isteğinizi maalesef karşılayamıyoruz. Uygulamadan başka bir saat seçebilirsiniz. {3}", a.customerName, whenText, a.serviceName, a.businessName)
    AppointmentStatus.CANCELLED ->
      tr("Merhaba {0}, {1} {2} randevunuz iptal edildi. Yeni randevu için uygulamadan uygun bir saat seçebilirsiniz. {3}", a.customerName, whenText, a.serviceName, a.businessName)
    else -> tr("Merhaba {0}, {1} {2} randevunuzla ilgili bilgilendirme. {3}", a.customerName, whenText, a.serviceName, a.businessName)
  }
}
