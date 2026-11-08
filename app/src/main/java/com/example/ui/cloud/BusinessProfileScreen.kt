package com.example.ui.cloud

import com.example.util.tr

import androidx.compose.foundation.rememberScrollState

import androidx.compose.foundation.verticalScroll

import com.example.model.allows

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.content.ContextCompat
import com.example.cloud.BusinessLocation
import com.example.cloud.BusinessLogo
import com.example.cloud.BusinessLogoImage
import kotlinx.coroutines.launch
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cloud.BusinessCategories
import com.example.cloud.BusinessProfile
import com.example.cloud.ServiceItem
import com.example.cloud.WorkingDay
import com.example.ui.theme.LocalAppPalette
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.RoseError
import com.example.ui.theme.Slate500
import com.example.util.Formatters
import com.example.util.Money
import com.example.viewmodel.CloudViewModel

/** İşletme vitrini, çalışma saatleri ve hizmetler. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessProfileScreen(
  cloud: CloudViewModel,
  defaultName: String,
  defaultCity: String,
  defaultPhone: String,
  onBack: () -> Unit,
  onOpenPlans: () -> Unit = {}
) {
  val existing by cloud.myBusiness.collectAsState()
  val services by cloud.myServices.collectAsState()
  val user by cloud.firebaseUser.collectAsState()
  val palette = LocalAppPalette.current

  BackHandler { onBack() }

  // Form durumu: işletme kaydı yüklendiğinde (veya ilk açılışta) doldurulur.
  val base = existing ?: BusinessProfile(
    id = user?.uid ?: "", ownerUid = user?.uid ?: "", name = defaultName, city = defaultCity, phone = defaultPhone
  )
  var name by remember(existing?.id) { mutableStateOf(base.name) }
  var category by remember(existing?.id) { mutableStateOf(base.category) }
  var city by remember(existing?.id) { mutableStateOf(base.city) }
  var district by remember(existing?.id) { mutableStateOf(base.district) }
  var address by remember(existing?.id) { mutableStateOf(base.address) }
  var phone by remember(existing?.id) { mutableStateOf(base.phone) }
  var description by remember(existing?.id) { mutableStateOf(base.description) }
  var listed by remember(existing?.id) { mutableStateOf(base.isListed) }
  var autoApprove by remember(existing?.id) { mutableStateOf(base.autoApprove) }
  var minNotice by remember(existing?.id) { mutableStateOf(base.minNoticeMinutes) }
  var window by remember(existing?.id) { mutableStateOf(base.bookingWindowDays) }
  val hours = remember(existing?.id) { mutableStateListOf<WorkingDay>().apply { addAll(base.workingHours.sortedBy { it.dayOfWeek }) } }
  var editService by remember { mutableStateOf<ServiceItem?>(null) }
  var editStaff by remember { mutableStateOf<com.example.cloud.Staff?>(null) }
  val staff by cloud.myStaff.collectAsState()
  val tierNow by cloud.tier.collectAsState()
  var logo by remember(existing?.id) { mutableStateOf(base.logo) }
  var lat by remember(existing?.id) { mutableStateOf(base.latitude) }
  var lng by remember(existing?.id) { mutableStateOf(base.longitude) }
  var logoBusy by remember { mutableStateOf(false) }
  var locBusy by remember { mutableStateOf(false) }
  var locMessage by remember { mutableStateOf<String?>(null) }
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  val pickLogo = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
    if (uri != null) {
      logoBusy = true
      scope.launch {
        val encoded = BusinessLogo.encode(context, uri)
        if (encoded != null) logo = encoded else cloud.notify(tr("Görsel okunamadı, başka bir görsel deneyin."))
        logoBusy = false
      }
    }
  }

  fun fetchLocation() {
    locBusy = true
    locMessage = null
    scope.launch {
      val p = BusinessLocation.current(context)
      if (p != null) {
        lat = p.first; lng = p.second
        locMessage = tr("Konum alındı. Kaydet'e basmayı unutmayın.")
      } else {
        locMessage = tr("Konum alınamadı. Telefonun konum (GPS) ayarı açık mı? Açık alanda tekrar deneyin.")
      }
      locBusy = false
    }
  }

  val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
    if (result.values.any { it }) fetchLocation()
    else locMessage = tr("Konum izni verilmedi. Ayarlar > Uygulamalar > MüşteriCep > İzinler'den açabilirsiniz.")
  }

  fun requestLocation() {
    val granted = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
      .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
    if (granted) fetchLocation()
    else locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
  }

  fun save() {
    cloud.saveBusiness(
      base.copy(
        name = name, category = category, city = city, district = district, address = address,
        phone = phone, description = description, isListed = listed, autoApprove = autoApprove,
        minNoticeMinutes = minNotice, bookingWindowDays = window, workingHours = hours.toList(),
        logo = logo, latitude = lat, longitude = lng
      )
    )
  }

  Scaffold(
    contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        title = { Text(tr("Randevu ayarları"), fontWeight = FontWeight.Bold) },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Geri")) } },
        actions = { TextButton(onClick = { save() }) { Text(tr("Kaydet"), fontWeight = FontWeight.Bold) } },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        windowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
      )
    }
  ) { padding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // 1. Vitrin bilgileri
      item {
        FormCard(tr("İşletme bilgileri")) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center) {
              BusinessLogoImage(logo = logo, size = 72.dp, corner = 18.dp)
              if (logoBusy) CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(tr("Logo"), fontWeight = FontWeight.SemiBold)
              Text(tr("Müşteriler sizi listede ve randevu ekranında logonuzla görür."), fontSize = 12.sp, color = Slate500)
              Row {
                TextButton(
                  onClick = { pickLogo.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                  enabled = !logoBusy,
                  contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 0.dp)
                ) {
                  Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(if (logo.isBlank()) tr("Logo seç") else tr("Değiştir"))
                }
                if (logo.isNotBlank()) {
                  Spacer(modifier = Modifier.width(12.dp))
                  TextButton(onClick = { logo = "" }) { Text(tr("Kaldır"), color = RoseError) }
                }
              }
            }
          }
          Spacer(modifier = Modifier.height(10.dp))
          OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(tr("İşletme adı *")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
          Spacer(modifier = Modifier.height(8.dp))
          Text(tr("Kategori"), style = MaterialTheme.typography.labelMedium, color = Slate500)
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(BusinessCategories.all) { c -> FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c, fontSize = 12.sp) }) }
          }
          Spacer(modifier = Modifier.height(8.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text(tr("Şehir")) }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(value = district, onValueChange = { district = it }, label = { Text(tr("İlçe")) }, singleLine = true, modifier = Modifier.weight(1f))
          }
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text(tr("Adres")) }, maxLines = 2, modifier = Modifier.fillMaxWidth())
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = phone, onValueChange = { phone = it }, label = { Text(tr("İşletme telefonu")) }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth()
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text(tr("Kısa tanıtım")) }, maxLines = 3, modifier = Modifier.fillMaxWidth())
        }
      }

      // 1b. Harita konumu
      item {
        FormCard(tr("Konum")) {
          Text(
            tr("İşletmenizdeyken \"Şu anki konumu kullan\"a basın. Randevu alan müşteriler tek dokunuşla yol tarifi alır."),
            fontSize = 12.sp, color = Slate500
          )
          Spacer(modifier = Modifier.height(10.dp))
          val la = lat
          val lo = lng
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              Icons.Default.LocationOn, contentDescription = null,
              tint = if (la != null) palette.success else palette.inactive, modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              if (la != null && lo != null) tr("Konum kayıtlı ({0})", BusinessLocation.format(la, lo)) else tr("Henüz konum eklenmedi"),
              fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f)
            )
          }
          locMessage?.let {
            Spacer(modifier = Modifier.height(6.dp))
            Text(it, fontSize = 12.sp, color = if (la != null && it.startsWith(tr("Konum alındı"))) palette.success else palette.warning)
          }
          Spacer(modifier = Modifier.height(10.dp))
          Button(
            onClick = { requestLocation() }, enabled = !locBusy,
            modifier = Modifier.fillMaxWidth().height(46.dp), shape = RoundedCornerShape(12.dp)
          ) {
            if (locBusy) {
              CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = palette.onInk)
              Spacer(modifier = Modifier.width(8.dp))
              Text(tr("Konum alınıyor…"))
            } else {
              Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(if (la == null) tr("Şu anki konumu kullan") else tr("Konumu güncelle"))
            }
          }
          if (la != null && lo != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
              OutlinedButton(
                onClick = { runCatching { context.startActivity(BusinessLocation.showIntent(la, lo, name.ifBlank { tr("İşletme") })) } },
                modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)
              ) {
                Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(tr("Haritada gör"), maxLines = 1)
              }
              OutlinedButton(
                onClick = { lat = null; lng = null; locMessage = null },
                modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)
              ) { Text(tr("Kaldır"), color = RoseError, maxLines = 1) }
            }
          }
        }
      }

      // 2. Randevu kuralları
      item {
        FormCard(tr("Randevu kuralları")) {
          SwitchRow(tr("Herkese açık listede görün"), tr("Kapalıysa müşteriler sizi sadece kodla bulur."), listed) { listed = it }
          HorizontalDivider(color = palette.cardBorder, modifier = Modifier.padding(vertical = 6.dp))
          SwitchRow(tr("Randevuları otomatik onayla"), tr("Kapalıysa her isteği siz onaylarsınız."), autoApprove) { autoApprove = it }
          HorizontalDivider(color = palette.cardBorder, modifier = Modifier.padding(vertical = 6.dp))
          Text(tr("En az ne kadar önceden randevu alınabilir?"), style = MaterialTheme.typography.labelMedium)
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(listOf(0 to tr("Hemen"), 30 to tr("30 dk"), 60 to tr("1 saat"), 120 to tr("2 saat"), 1440 to tr("1 gün"))) { (m, l) ->
              FilterChip(selected = minNotice == m, onClick = { minNotice = m }, label = { Text(l, fontSize = 12.sp) })
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
          Text(tr("En fazla kaç gün sonrasına?"), style = MaterialTheme.typography.labelMedium)
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(listOf(7, 14, 30, 60, 90)) { d ->
              FilterChip(selected = window == d, onClick = { window = d }, label = { Text(tr("{0} gün", d), fontSize = 12.sp) })
            }
          }
        }
      }

      // 3. Çalışma saatleri
      item {
        FormCard(tr("Çalışma saatleri")) {
          hours.forEachIndexed { index, d ->
            WorkingDayRow(d) { updated -> hours[index] = updated }
            if (index < hours.size - 1) HorizontalDivider(color = palette.cardBorder)
          }
        }
      }

      // 4. Hizmetler
      item {
        FormCard(tr("Hizmetler")) {
          if (existing == null) {
            Text(tr("Hizmet eklemek için önce üstten Kaydet'e basın."), style = MaterialTheme.typography.bodySmall, color = Slate500)
          } else {
            if (services.isEmpty()) {
              Text(tr("Henüz hizmet yok. Örn: Saç kesimi • 30 dk • 300 ₺"), style = MaterialTheme.typography.bodySmall, color = Slate500)
            }
            services.forEach { s ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { editService = s }
                  .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(s.name + if (!s.isActive) tr(" (kapalı)") else "", fontWeight = FontWeight.SemiBold)
                  Text(tr("{0} • {1}", durationText(s.durationMinutes), Formatters.formatCurrency(s.price)), style = MaterialTheme.typography.bodySmall, color = Slate500)
                }
                Icon(Icons.Default.Edit, contentDescription = tr("Düzenle"), tint = Slate500)
              }
            }
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedButton(onClick = { editService = ServiceItem(name = "", durationMinutes = 30, price = 0.0) }) {
              Icon(Icons.Default.Add, contentDescription = null)
              Spacer(modifier = Modifier.width(6.dp))
              Text(tr("Hizmet ekle"))
            }
          }
        }
      }

      // 5. Personel (İşletme paketi)
      item {
        FormCard(tr("Personel")) {
          val allowed = tierNow.allows(com.example.model.Feature.STAFF)
          if (!allowed) {
            Text(
              tr("Birden fazla çalışanınız varsa her birine ayrı takvim açın; müşteriler personel seçerek randevu alsın. Bu özellik İşletme paketinde."),
              style = MaterialTheme.typography.bodySmall, color = Slate500
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedButton(onClick = onOpenPlans) { Text(tr("İşletme paketini gör")) }
          } else if (existing == null) {
            Text(tr("Personel eklemek için önce üstten Kaydet'e basın."), style = MaterialTheme.typography.bodySmall, color = Slate500)
          } else {
            if (staff.isEmpty()) {
              Text(tr("Henüz personel yok. Personel eklemezseniz tek takvim kullanılır."), style = MaterialTheme.typography.bodySmall, color = Slate500)
            }
            staff.forEach { st ->
              Row(
                modifier = Modifier.fillMaxWidth().clickable { editStaff = st }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                com.example.ui.components.InitialsAvatar(st.name, size = 34.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(st.name + if (!st.isActive) tr(" (izinli)") else "", fontWeight = FontWeight.SemiBold)
                  Text(
                    if (st.serviceIds.isEmpty()) tr("Tüm hizmetler")
                    else services.filter { it.id in st.serviceIds }.joinToString(", ") { it.name },
                    style = MaterialTheme.typography.bodySmall, color = Slate500, maxLines = 1
                  )
                }
                Icon(Icons.Default.Edit, contentDescription = tr("Düzenle"), tint = Slate500)
              }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(tr("{0} / {1} personel", staff.size, tierNow.maxStaff), style = MaterialTheme.typography.bodySmall, color = Slate500)
            OutlinedButton(onClick = { editStaff = com.example.cloud.Staff(name = "") }, enabled = staff.size < tierNow.maxStaff) {
              Icon(Icons.Default.Add, contentDescription = null)
              Spacer(modifier = Modifier.width(6.dp))
              Text(tr("Personel ekle"))
            }
          }
        }
      }

      item {
        Button(onClick = { save() }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp)) {
          Text(tr("Kaydet"), fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(40.dp))
      }
    }
  }

  editStaff?.let { st ->
    StaffDialog(
      initial = st,
      services = services,
      onDismiss = { editStaff = null },
      onSave = { cloud.saveStaff(it); editStaff = null },
      onDelete = if (st.id.isNotBlank()) ({ cloud.deleteStaff(st); editStaff = null }) else null
    )
  }

  editService?.let { s ->
    ServiceDialog(
      initial = s,
      onDismiss = { editService = null },
      onSave = { cloud.saveService(it); editService = null },
      onDelete = if (s.id.isNotBlank()) ({ cloud.deleteService(s.id); editService = null }) else null
    )
  }
}

@Composable
private fun FormCard(title: String, content: @Composable () -> Unit) {
  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, LocalAppPalette.current.cardBorder),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = NavyPrimary)
      Spacer(modifier = Modifier.height(10.dp))
      content()
    }
  }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Column(modifier = Modifier.weight(1f)) {
      Text(title, fontWeight = FontWeight.Medium, fontSize = 14.sp)
      Text(subtitle, fontSize = 12.sp, color = Slate500)
    }
    Switch(checked = checked, onCheckedChange = onChange)
  }
}

@Composable
private fun WorkingDayRow(d: WorkingDay, onChange: (WorkingDay) -> Unit) {
  val context = LocalContext.current
  fun pick(initial: Int, cb: (Int) -> Unit) {
    TimePickerDialog(context, { _, h, m -> cb(h * 60 + m) }, initial / 60, initial % 60, true).show()
  }
  Column(modifier = Modifier.padding(vertical = 6.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(WorkingDay.DAY_NAMES[d.dayOfWeek - 1], fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
      if (d.isOpen) {
        TextButton(onClick = { pick(d.openMinute) { m -> onChange(d.copy(openMinute = m)) } }) { Text(WorkingDay.formatMinute(d.openMinute)) }
        Text("–")
        TextButton(onClick = { pick(d.closeMinute) { m -> onChange(d.copy(closeMinute = m)) } }) { Text(WorkingDay.formatMinute(d.closeMinute)) }
      } else {
        Text(tr("Kapalı"), color = Slate500, modifier = Modifier.padding(end = 8.dp))
      }
      Switch(checked = d.isOpen, onCheckedChange = { onChange(d.copy(isOpen = it)) })
    }
    if (d.isOpen) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(tr("Öğle arası:"), fontSize = 12.sp, color = Slate500)
        if (d.breakStart != null && d.breakEnd != null) {
          TextButton(onClick = { pick(d.breakStart) { m -> onChange(d.copy(breakStart = m)) } }) { Text(WorkingDay.formatMinute(d.breakStart), fontSize = 12.sp) }
          Text("–", fontSize = 12.sp)
          TextButton(onClick = { pick(d.breakEnd) { m -> onChange(d.copy(breakEnd = m)) } }) { Text(WorkingDay.formatMinute(d.breakEnd), fontSize = 12.sp) }
          TextButton(onClick = { onChange(d.copy(breakStart = null, breakEnd = null)) }) { Text(tr("Kaldır"), fontSize = 12.sp, color = RoseError) }
        } else {
          TextButton(onClick = { onChange(d.copy(breakStart = 12 * 60 + 30, breakEnd = 13 * 60 + 30)) }) { Text(tr("Ekle"), fontSize = 12.sp) }
        }
      }
    }
  }
}

@Composable
private fun ServiceDialog(initial: ServiceItem, onDismiss: () -> Unit, onSave: (ServiceItem) -> Unit, onDelete: (() -> Unit)?) {
  var name by remember { mutableStateOf(initial.name) }
  var duration by remember { mutableStateOf(initial.durationMinutes) }
  var price by remember { mutableStateOf(if (initial.price > 0) Money.toInput(initial.price) else "") }
  var active by remember { mutableStateOf(initial.isActive) }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (initial.id.isBlank()) tr("Yeni hizmet") else tr("Hizmeti düzenle"), fontWeight = FontWeight.Bold) },
    text = {
      Column {
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(tr("Hizmet adı")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        Text(tr("Süre"), style = MaterialTheme.typography.labelMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(listOf(15, 30, 45, 60, 90, 120, 180)) { m ->
            FilterChip(selected = duration == m, onClick = { duration = m }, label = { Text(durationText(m), fontSize = 12.sp) })
          }
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
          value = price, onValueChange = { price = it }, label = { Text(tr("Fiyat (₺)")) }, singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth()
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(tr("Randevuya açık"), modifier = Modifier.weight(1f))
          Switch(checked = active, onCheckedChange = { active = it })
        }
        if (onDelete != null) {
          TextButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = null, tint = RoseError)
            Text(tr("Hizmeti sil"), color = RoseError)
          }
        }
      }
    },
    confirmButton = {
      Button(onClick = {
        onSave(initial.copy(name = name, durationMinutes = duration, price = Money.parse(price) ?: 0.0, isActive = active))
      }) { Text(tr("Kaydet")) }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Vazgeç")) } }
  )
}

@Composable
private fun StaffDialog(
  initial: com.example.cloud.Staff,
  services: List<ServiceItem>,
  onDismiss: () -> Unit,
  onSave: (com.example.cloud.Staff) -> Unit,
  onDelete: (() -> Unit)?
) {
  var name by remember { mutableStateOf(initial.name) }
  var active by remember { mutableStateOf(initial.isActive) }
  var all by remember { mutableStateOf(initial.serviceIds.isEmpty()) }
  var picked by remember { mutableStateOf(initial.serviceIds.toSet()) }
  androidx.compose.material3.AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (initial.id.isBlank()) tr("Personel ekle") else tr("Personeli düzenle"), fontWeight = FontWeight.Bold) },
    text = {
      Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        OutlinedTextField(value = name, onValueChange = { name = it.take(40) }, label = { Text(tr("Ad (müşteriler görür)")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(tr("Randevu alıyor"), modifier = Modifier.weight(1f))
          androidx.compose.material3.Switch(checked = active, onCheckedChange = { active = it })
        }
        Text(tr("Kapalıysa (izin, tatil) müşteriler bu personeli göremez."), style = MaterialTheme.typography.bodySmall, color = Slate500)
        Spacer(modifier = Modifier.height(10.dp))
        Text(tr("Verdiği hizmetler"), fontWeight = FontWeight.SemiBold)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { all = !all }) {
          androidx.compose.material3.Checkbox(checked = all, onCheckedChange = { all = it })
          Text(tr("Tüm hizmetler"))
        }
        if (!all) services.forEach { sv ->
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { picked = if (sv.id in picked) picked - sv.id else picked + sv.id }) {
            androidx.compose.material3.Checkbox(checked = sv.id in picked, onCheckedChange = { picked = if (it) picked + sv.id else picked - sv.id })
            Text(sv.name)
          }
        }
        if (onDelete != null) {
          Spacer(modifier = Modifier.height(8.dp))
          TextButton(onClick = onDelete) { Text(tr("Personeli sil"), color = RoseError) }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(initial.copy(name = name.trim(), isActive = active, serviceIds = if (all) emptyList() else picked.toList())) },
        enabled = name.isNotBlank() && (all || picked.isNotEmpty())
      ) { Text(tr("Kaydet")) }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Vazgeç")) } }
  )
}
