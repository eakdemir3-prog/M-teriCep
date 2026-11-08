package com.example.ui.cloud

import androidx.compose.material.icons.filled.Star

import androidx.compose.material.icons.automirrored.filled.ArrowForward

import androidx.compose.ui.graphics.Color

import androidx.compose.foundation.background

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cloud.Appointment
import com.example.cloud.AppointmentStatus
import com.example.cloud.BusinessProfile
import com.example.cloud.ServiceItem
import com.example.cloud.WorkingDay
import com.example.model.AppThemeMode
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LocalAppPalette
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.RoseError
import com.example.ui.theme.Slate500
import com.example.ui.theme.moneyStyle
import com.example.util.Formatters
import com.example.viewmodel.CloudViewModel
import com.example.viewmodel.CustomerScreen
import kotlinx.coroutines.flow.collectLatest

/** Müşteri uygulaması: işletme bul, randevu al, randevularım, profil. */
@Composable
fun CustomerApp(cloud: CloudViewModel, themeMode: AppThemeMode, onThemeChange: (AppThemeMode) -> Unit) {
  val screen by cloud.customerScreen.collectAsState()
  val snackbar = remember { SnackbarHostState() }

  LaunchedEffect(Unit) { cloud.messages.collectLatest { snackbar.showSnackbar(it.text) } }
  BackHandler(enabled = screen !is CustomerScreen.Discover) { cloud.customerBack() }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets.safeDrawing,
    snackbarHost = { SnackbarHost(snackbar) },
    bottomBar = {
      if (screen !is CustomerScreen.BusinessDetail) {
        NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
          NavigationBarItem(
            selected = screen is CustomerScreen.Discover,
            onClick = { cloud.openCustomerTab(CustomerScreen.Discover) },
            icon = { Icon(Icons.Default.Search, contentDescription = null) },
            label = { Text("Randevu al") }
          )
          NavigationBarItem(
            selected = screen is CustomerScreen.MyAppointments,
            onClick = { cloud.openCustomerTab(CustomerScreen.MyAppointments) },
            icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
            label = { Text("Randevularım") }
          )
          NavigationBarItem(
            selected = screen is CustomerScreen.Profile,
            onClick = { cloud.openCustomerTab(CustomerScreen.Profile) },
            icon = { Icon(Icons.Default.Person, contentDescription = null) },
            label = { Text("Profil") }
          )
        }
      }
    }
  ) { padding ->
    val myBusiness by cloud.myBusiness.collectAsState()
    val ctx = LocalContext.current
    Column(modifier = Modifier.padding(padding)) {
      if (myBusiness != null) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(com.example.ui.theme.LocalAppPalette.current.text)
            .clickable { cloud.switchMode(ctx as? android.app.Activity, com.example.cloud.UserRole.BUSINESS) }
            .padding(horizontal = 16.dp, vertical = 9.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Müşteri modundasınız", color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
          Text("İşletmeme dön →", color = Color(0xFF5EEAD4), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
      }
    Box(modifier = Modifier.weight(1f)) {
      when (val s = screen) {
        is CustomerScreen.Discover -> DiscoverScreen(cloud)
        is CustomerScreen.MyAppointments -> MyAppointmentsScreen(cloud)
        is CustomerScreen.Profile -> CustomerProfileScreen(cloud, themeMode, onThemeChange)
        is CustomerScreen.BusinessDetail -> BookingScreen(cloud, s.businessId)
      }
    }
    }
  }
}

@Composable
private fun DiscoverScreen(cloud: CloudViewModel) {
  val profile by cloud.userProfile.collectAsState()
  val listed by cloud.listedBusinesses.collectAsState()
  val favorites by cloud.favoriteBusinesses.collectAsState()
  val busy by cloud.busy.collectAsState()
  var code by remember { mutableStateOf("") }
  var city by remember { mutableStateOf("") }
  var query by remember { mutableStateOf("") }
  var category by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(profile?.favoriteBusinessIds) { cloud.loadFavorites() }
  LaunchedEffect(Unit) { cloud.loadListed(null) }

  val filtered = listed.filter { b ->
    (query.isBlank() || b.name.contains(query, ignoreCase = true) || b.district.contains(query, ignoreCase = true)) &&
      (category == null || b.category == category)
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      Spacer(modifier = Modifier.height(8.dp))
      Text("Merhaba${profile?.displayName?.takeIf { it.isNotBlank() }?.let { ", " + it.substringBefore(' ') } ?: ""}", style = MaterialTheme.typography.headlineSmall)
      Text("Gittiğiniz işletmeyi bulun ve randevu alın", style = MaterialTheme.typography.bodySmall, color = Slate500)
    }

    // Kodla bul
    item {
      CardBox {
        Text("İşletme kodu ile bul", fontWeight = FontWeight.Bold)
        Text("İşletmenizin verdiği 6 haneli kodu girin.", style = MaterialTheme.typography.bodySmall, color = Slate500)
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          OutlinedTextField(
            value = code,
            onValueChange = { code = it.uppercase().filter { c -> c.isLetterOrDigit() }.take(8) },
            placeholder = { Text("Örn: K7M2PA") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            textStyle = moneyStyle(18),
            modifier = Modifier.weight(1f)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Button(onClick = { cloud.findByCode(code) }, enabled = code.length >= 4 && !busy) { Text("Bul") }
        }
      }
    }

    if (favorites.isNotEmpty()) {
      item { SectionTitle("İşletmelerim") }
      items(favorites, key = { "f_" + it.id }) { b -> BusinessRow(b, onClick = { cloud.openBusiness(b.id) }) }
    }

    item {
      SectionTitle("İşletme ara")
      Spacer(modifier = Modifier.height(6.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("Şehir") }, singleLine = true, modifier = Modifier.weight(1f))
        OutlinedButton(onClick = { cloud.loadListed(city.ifBlank { null }) }) { Text("Getir") }
      }
      Spacer(modifier = Modifier.height(6.dp))
      OutlinedTextField(
        value = query, onValueChange = { query = it }, placeholder = { Text("İsim veya ilçe") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }, singleLine = true, modifier = Modifier.fillMaxWidth()
      )
      Spacer(modifier = Modifier.height(6.dp))
      androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        item { FilterChip(selected = category == null, onClick = { category = null }, label = { Text("Tümü", fontSize = 12.sp) }) }
        items(listed.map { it.category }.filter { it.isNotBlank() }.distinct()) { c ->
          FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c, fontSize = 12.sp) })
        }
      }
    }
    if (filtered.isEmpty()) {
      item { Text("Listelenen işletme bulunamadı. İşletmenizden kodu isteyebilirsiniz.", style = MaterialTheme.typography.bodySmall, color = Slate500) }
    }
    items(filtered, key = { "l_" + it.id }) { b -> BusinessRow(b, onClick = { cloud.openBusiness(b.id) }) }
    item { Spacer(modifier = Modifier.height(24.dp)) }
  }
}

@Composable
private fun BusinessRow(b: BusinessProfile, onClick: () -> Unit) {
  val palette = LocalAppPalette.current
  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, palette.cardBorder),
    modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
  ) {
    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
      com.example.cloud.BusinessLogoImage(logo = b.logo, size = 46.dp)
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(b.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
          if (b.ratingCount > 0) {
            Spacer(modifier = Modifier.width(8.dp))
            RatingBadge(b.ratingAvg, b.ratingCount, 13.dp)
          }
        }
        Text(
          listOf(b.category, listOf(b.district, b.city).filter { it.isNotBlank() }.joinToString(", ")).filter { it.isNotBlank() }.joinToString(" • "),
          style = MaterialTheme.typography.bodySmall, color = Slate500, maxLines = 1
        )
      }
    }
  }
}

@Composable
private fun BookingScreen(cloud: CloudViewModel, businessId: String) {
  val context = LocalContext.current
  val business by cloud.viewedBusiness.collectAsState()
  val services by cloud.viewedServices.collectAsState()
  val slots by cloud.availableSlots.collectAsState()
  val profile by cloud.userProfile.collectAsState()
  val busy by cloud.busy.collectAsState()
  val reviews by cloud.viewedReviews.collectAsState()
  val allStaff by cloud.viewedStaff.collectAsState()
  /** null = "Fark etmez" */
  var staffSel by remember(businessId) { mutableStateOf<com.example.cloud.Staff?>(null) }
  val eligible = allStaff.filter { st -> service?.let { st.canDo(it.id) } ?: true }
  val staffChoice = when {
    eligible.isEmpty() -> emptyList()
    staffSel != null && eligible.any { it.id == staffSel?.id } -> listOf(staffSel!!)
    else -> eligible
  }
  var showReviews by remember(businessId) { mutableStateOf(false) }
  if (showReviews) {
    business?.let { bb -> ReviewsDialog(bb.name, bb.ratingAvg, bb.ratingCount, reviews) { showReviews = false } }
  }
  var service by remember(businessId) { mutableStateOf<ServiceItem?>(null) }
  var day by remember(businessId) { mutableStateOf(Formatters.startOfDay(System.currentTimeMillis())) }
  var time by remember(businessId) { mutableStateOf<Long?>(null) }
  var note by remember(businessId) { mutableStateOf("") }
  val isFav = profile?.favoriteBusinessIds?.contains(businessId) == true

  LaunchedEffect(businessId) { cloud.viewBusiness(businessId) }
  LaunchedEffect(services) { if (service == null || services.none { it.id == service?.id }) service = services.firstOrNull() }
  LaunchedEffect(business, service, day, staffChoice.map { it.id }) {
    time = null
    val b = business
    val s = service
    if (b != null && b.id == businessId && s != null) cloud.loadSlots(b, day, s.durationMinutes, staff = staffChoice)
  }

  Column(modifier = Modifier.fillMaxSize()) {
    TopBar(title = business?.name ?: "İşletme", onBack = { cloud.customerBack() }) {
      IconButton(onClick = { cloud.toggleFavorite(businessId) }) {
        Icon(if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = "Favori", tint = RoseError)
      }
    }
    val b = business
    if (b == null || b.id != businessId) {
      Text("Yükleniyor…", modifier = Modifier.padding(16.dp), color = Slate500)
    } else LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      item {
        CardBox {
          Row(verticalAlignment = Alignment.CenterVertically) {
            com.example.cloud.BusinessLogoImage(logo = b.logo, size = 60.dp, corner = 16.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(b.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
              if (b.category.isNotBlank()) Text(b.category, style = MaterialTheme.typography.labelMedium, color = Slate500)
              if (b.ratingCount > 0) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { showReviews = true }) {
                  RatingBadge(b.ratingAvg, b.ratingCount, 15.dp)
                  Text("  Yorumlar", fontSize = 13.sp, color = LocalAppPalette.current.ink, fontWeight = FontWeight.SemiBold)
                }
              }
            }
          }
          if (b.description.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(b.description, style = MaterialTheme.typography.bodyMedium)
          }
          val addr = com.example.cloud.BusinessLocation.addressText(b)
          if (addr.isNotBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
              Icon(Icons.Default.LocationOn, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(addr, style = MaterialTheme.typography.bodySmall)
            }
          }
          if (b.hasLocation || addr.isNotBlank() || b.phone.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
              if (b.hasLocation || addr.isNotBlank()) {
                Button(
                  onClick = {
                    runCatching {
                      context.startActivity(com.example.cloud.BusinessLocation.directionsIntent(b.latitude, b.longitude, listOf(b.name, addr).filter { it.isNotBlank() }.joinToString(", ")))
                    }
                  },
                  modifier = Modifier.weight(1f).height(44.dp), shape = RoundedCornerShape(12.dp)
                ) {
                  Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Yol tarifi", maxLines = 1)
                }
              }
              if (b.phone.isNotBlank()) {
                OutlinedButton(
                  onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + b.phone.filter { it.isDigit() || it == '+' }))) } },
                  modifier = Modifier.weight(1f).height(44.dp), shape = RoundedCornerShape(12.dp)
                ) {
                  Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Ara", maxLines = 1)
                }
              }
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
          Text(todayHoursText(b), style = MaterialTheme.typography.bodySmall, color = Slate500)
        }
      }

      item { SectionTitle("1. Hizmet seçin") }
      if (services.isEmpty()) {
        item { Text("Bu işletme henüz hizmet eklememiş.", color = Slate500) }
      }
      items(services, key = { "s_" + it.id }) { s ->
        val sel = service?.id == s.id
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(if (sel) 2.dp else 1.dp, if (sel) EmeraldPrimary else LocalAppPalette.current.cardBorder),
          modifier = Modifier.fillMaxWidth().clickable { service = s }
        ) {
          Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
              Text(s.name, fontWeight = FontWeight.SemiBold)
              Text(durationText(s.durationMinutes), style = MaterialTheme.typography.bodySmall, color = Slate500)
            }
            if (s.price > 0) Text(Formatters.formatCurrency(s.price), style = moneyStyle(15))
          }
        }
      }

      if (service != null && eligible.isNotEmpty()) {
        item {
          SectionTitle("Personel")
          Spacer(modifier = Modifier.height(6.dp))
          androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
              androidx.compose.material3.FilterChip(
                selected = staffSel == null, onClick = { staffSel = null },
                label = { Text("Fark etmez") }
              )
            }
            items(eligible, key = { it.id }) { st ->
              androidx.compose.material3.FilterChip(
                selected = staffSel?.id == st.id, onClick = { staffSel = st },
                label = { Text(st.name) }
              )
            }
          }
        }
      }

      if (service != null) {
        item {
          SectionTitle("2. Gün seçin")
          Spacer(modifier = Modifier.height(6.dp))
          DayStrip(selectedDay = day, onSelect = { day = it }, days = b.bookingWindowDays.coerceIn(1, 90), business = b)
        }
        item {
          SectionTitle("3. Saat seçin")
          Spacer(modifier = Modifier.height(6.dp))
          SlotGrid(
            slots = slots, selected = time, onSelect = { time = it },
            emptyText = if (!b.dayFor(day).isOpen) "İşletme bu gün kapalı." else "Bu gün için boş saat kalmadı. Başka bir gün seçin."
          )
        }
        item {
          OutlinedTextField(value = note, onValueChange = { note = it.take(200) }, label = { Text("İşletmeye not (isteğe bağlı)") }, modifier = Modifier.fillMaxWidth())
        }
        item {
          val t = time
          Button(
            onClick = { val s = service; if (s != null && t != null) cloud.book(b, s, t, note, staffChoice) { cloud.openCustomerTab(CustomerScreen.MyAppointments) } },
            enabled = t != null && !busy,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)
          ) {
            Text(
              if (t == null) "Saat seçin" else "${Formatters.formatShortDate(t)} ${timeOf(t)} • Randevu al",
              fontWeight = FontWeight.Bold
            )
          }
          Text(
            if (b.autoApprove) "Randevunuz hemen kesinleşir." else "İsteğiniz işletmeye gider; onaylanınca bildirim alırsınız.",
            style = MaterialTheme.typography.bodySmall, color = Slate500, modifier = Modifier.padding(top = 6.dp)
          )
          Spacer(modifier = Modifier.height(30.dp))
        }
      }
    }
  }
}

private fun todayHoursText(b: BusinessProfile): String {
  val d = b.dayFor(System.currentTimeMillis())
  return if (!d.isOpen) "Bugün kapalı" else "Bugün ${WorkingDay.formatMinute(d.openMinute)} – ${WorkingDay.formatMinute(d.closeMinute)}"
}

@Composable
private fun MyAppointmentsScreen(cloud: CloudViewModel) {
  val list by cloud.myAppointments.collectAsState()
  val me by cloud.userProfile.collectAsState()
  var toCancel by remember { mutableStateOf<Appointment?>(null) }
  var toReview by remember { mutableStateOf<Appointment?>(null) }
  var justReviewed by remember { mutableStateOf(setOf<String>()) }
  val reviewed = (me?.reviewedAppointmentIds.orEmpty()).toSet() + justReviewed
  val now = System.currentTimeMillis()
  val upcoming = list.filter { it.end >= now && it.status.isActive }.sortedBy { it.start }
  val past = list.filter { !(it.end >= now && it.status.isActive) }.sortedByDescending { it.start }

  LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    item {
      Spacer(modifier = Modifier.height(8.dp))
      Text("Randevularım", style = MaterialTheme.typography.headlineSmall)
    }
    if (upcoming.isEmpty()) {
      item {
        CardBox {
          Text("Yaklaşan randevunuz yok.", fontWeight = FontWeight.SemiBold)
          TextButton(onClick = { cloud.openCustomerTab(CustomerScreen.Discover) }) { Text("Randevu al") }
        }
      }
    } else {
      item { SectionTitle("Yaklaşan") }
      items(upcoming, key = { "u_" + it.id }) { a -> CustomerAppointmentCard(a, onCancel = { toCancel = a }, onRebook = { cloud.openBusiness(a.businessId) }) }
    }
    if (past.isNotEmpty()) {
      item { SectionTitle("Geçmiş") }
      items(past.take(30), key = { "p_" + it.id }) { a ->
        val canReview = (a.status == AppointmentStatus.COMPLETED || (a.status == AppointmentStatus.CONFIRMED && a.end < now)) && a.customerUid.isNotBlank()
        CustomerAppointmentCard(
          a, onCancel = null, onRebook = { cloud.openBusiness(a.businessId) },
          reviewState = when {
            !canReview -> null
            a.id in reviewed -> true
            else -> false
          },
          onReview = { toReview = a }
        )
      }
    }
    item { Spacer(modifier = Modifier.height(24.dp)) }
  }

  toReview?.let { a ->
    WriteReviewDialog(
      businessName = a.businessName,
      serviceName = a.serviceName,
      onDismiss = { toReview = null },
      onSubmit = { rating, comment ->
        cloud.submitReview(a, rating, comment)
        justReviewed = justReviewed + a.id
        toReview = null
      }
    )
  }

  toCancel?.let { a ->
    AlertDialog(
      onDismissRequest = { toCancel = null },
      title = { Text("Randevu iptal edilsin mi?") },
      text = { Text("${a.businessName} • ${a.serviceName}\n${Formatters.formatFriendlyDate(a.start)} ${timeOf(a.start)}") },
      confirmButton = { Button(onClick = { cloud.cancelMyAppointment(a); toCancel = null }) { Text("İptal et") } },
      dismissButton = { TextButton(onClick = { toCancel = null }) { Text("Vazgeç") } }
    )
  }
}

@Composable
private fun CustomerAppointmentCard(
  a: Appointment,
  onCancel: (() -> Unit)?,
  onRebook: () -> Unit,
  reviewState: Boolean? = null,
  onReview: () -> Unit = {}
) {
  val palette = LocalAppPalette.current
  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, palette.cardBorder),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
          Text(a.businessName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
          Text(
            "${a.serviceName} • ${durationText(a.durationMinutes)}" + if (a.staffName.isNotBlank()) " • ${a.staffName}" else "",
            style = MaterialTheme.typography.bodySmall, color = Slate500
          )
        }
        AppointmentStatusChip(a.status)
      }
      Spacer(modifier = Modifier.height(6.dp))
      Text("${Formatters.formatFriendlyDate(a.start)} • ${timeOf(a.start)}", style = moneyStyle(15), color = NavyPrimary)
      Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
        if (onCancel != null && (a.businessLat != null || a.businessAddress.isNotBlank())) {
          val context = LocalContext.current
          TextButton(onClick = {
            runCatching {
              context.startActivity(com.example.cloud.BusinessLocation.directionsIntent(a.businessLat, a.businessLng, listOf(a.businessName, a.businessAddress).filter { it.isNotBlank() }.joinToString(", ")))
            }
          }) {
            Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Yol tarifi")
          }
        }
        if (onCancel != null) TextButton(onClick = onCancel) { Text("İptal et", color = RoseError) }
        when (reviewState) {
          false -> TextButton(onClick = onReview) {
            Icon(Icons.Default.Star, contentDescription = null, tint = StarGold, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Değerlendir")
          }
          true -> Text("Değerlendirildi ✓", fontSize = 12.sp, color = palette.success, modifier = Modifier.align(Alignment.CenterVertically).padding(horizontal = 8.dp))
          null -> Unit
        }
        TextButton(onClick = onRebook) { Text(if (onCancel != null) "İşletme" else "Tekrar al") }
      }
    }
  }
}

@Composable
private fun CustomerProfileScreen(cloud: CloudViewModel, themeMode: AppThemeMode, onThemeChange: (AppThemeMode) -> Unit) {
  val profile by cloud.userProfile.collectAsState()
  val user by cloud.firebaseUser.collectAsState()
  var name by remember(profile?.uid) { mutableStateOf(profile?.displayName ?: "") }
  var phone by remember(profile?.uid) { mutableStateOf(profile?.phone ?: "") }
  var confirmDelete by remember { mutableStateOf(false) }

  LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    item {
      Spacer(modifier = Modifier.height(8.dp))
      Text("Profil", style = MaterialTheme.typography.headlineSmall)
      Text(user?.email ?: "", style = MaterialTheme.typography.bodySmall, color = Slate500)
    }
    item {
      CardBox {
        Text("Randevu bilgileriniz", fontWeight = FontWeight.Bold)
        Text("İşletme sizi bu bilgilerle görür ve gerekirse arar.", style = MaterialTheme.typography.bodySmall, color = Slate500)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Ad soyad") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
          value = phone, onValueChange = { phone = it }, label = { Text("Telefon *") }, placeholder = { Text("0532 123 45 67") },
          singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { cloud.saveCustomerProfile(name, phone) }) { Text("Kaydet") }
      }
    }
    item {
      val palette = LocalAppPalette.current
      val hasBusiness = cloud.myBusiness.collectAsState().value != null
      val ctx = LocalContext.current
      Card(
        onClick = { cloud.switchMode(ctx as? android.app.Activity, com.example.cloud.UserRole.BUSINESS) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = palette.accentSoft),
        border = BorderStroke(1.5.dp, palette.ink),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Storefront, contentDescription = null, tint = palette.ink, modifier = Modifier.size(28.dp))
          Spacer(modifier = Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(if (hasBusiness) "İşletmeme dön" else "İşletmem var", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(
              if (hasBusiness) "İşletme moduna geç: takvim, müşteriler, tahsilat"
              else "İşletme moduna geçin; ücretsiz vitrin açın, randevu ve tahsilat yönetin. Aldığınız randevular kaybolmaz.",
              style = MaterialTheme.typography.bodySmall, color = Slate500
            )
          }
          Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = palette.ink)
        }
      }
    }
    item {
      CardBox {
        Text("Görünüm", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          AppThemeMode.entries.forEach { m ->
            FilterChip(selected = themeMode == m, onClick = { onThemeChange(m) }, label = { Text(m.label, fontSize = 12.sp) })
          }
        }
      }
    }
    item {
      CardBox {
        OutlinedButton(onClick = { cloud.signOut() }, modifier = Modifier.fillMaxWidth()) { Text("Çıkış yap") }
        TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) { Text("Hesabımı sil", color = RoseError) }
      }
    }
    item {
      Text("MüşteriCep • EA Vision Labs", style = MaterialTheme.typography.labelSmall, color = Slate500, modifier = Modifier.fillMaxWidth().padding(8.dp))
    }
  }

  if (confirmDelete) {
    AlertDialog(
      onDismissRequest = { confirmDelete = false },
      title = { Text("Hesap silinsin mi?") },
      text = { Text("Giriş bilgileriniz ve profiliniz silinir. Geçmiş randevu kayıtları işletmelerin defterinde kalabilir.") },
      confirmButton = { Button(onClick = { confirmDelete = false; cloud.deleteAccount() }) { Text("Hesabı sil") } },
      dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Vazgeç") } }
    )
  }
}

@Composable
private fun CardBox(content: @Composable () -> Unit) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, LocalAppPalette.current.cardBorder),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(16.dp)) { content() }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(title: String, onBack: () -> Unit, actions: @Composable () -> Unit = {}) {
  TopAppBar(
    title = { Text(title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri") } },
    actions = { actions() },
    windowInsets = WindowInsets(0, 0, 0, 0),
    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
  )
}
