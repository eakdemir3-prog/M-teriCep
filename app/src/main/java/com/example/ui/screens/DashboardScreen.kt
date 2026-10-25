package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CustomerFilter
import com.example.model.DebtStatus
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LocalAppPalette
import com.example.ui.theme.RoseError
import com.example.ui.theme.Slate500
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.moneyStyle
import com.example.util.Formatters
import com.example.viewmodel.DialogState
import com.example.viewmodel.MusteriCepViewModel
import com.example.viewmodel.Screen

@Composable
fun DashboardScreen(
  viewModel: MusteriCepViewModel,
  modifier: Modifier = Modifier,
  appointmentsToday: List<com.example.cloud.Appointment>? = null,
  pendingCount: Int = 0
) {
  val summary by viewModel.dashboardSummary.collectAsState()
  val profile by viewModel.profile.collectAsState()
  val customers by viewModel.allCustomersWithBalance.collectAsState()
  val collections by viewModel.allCollections.collectAsState()
  val statuses by viewModel.debtStatuses.collectAsState()
  val hasDemo by viewModel.hasDemoData.collectAsState()
  val palette = LocalAppPalette.current

  val now = System.currentTimeMillis()
  val byId = customers.associateBy { it.customer.id }
  val overdue = statuses
    .filter { it.status == DebtStatus.OVERDUE && byId[it.debt.customerId]?.customer?.isArchived == false }
    .sortedBy { it.debt.dueDate }
  val upcomingLimit = Formatters.startOfDay(now) + 8 * 86_400_000L
  val upcoming = statuses
    .filter {
      it.remainingAmount > 0 && it.status != DebtStatus.OVERDUE && it.debt.dueDate != null &&
        it.debt.dueDate < upcomingLimit && byId[it.debt.customerId]?.customer?.isArchived == false
    }
    .sortedBy { it.debt.dueDate }
  val topDebtors = customers.filter { it.remainingBalance > 0 && !it.customer.isArchived }
    .sortedByDescending { it.remainingBalance }
  val recentCollections = collections.take(5)

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    // 1. Başlık
    item {
      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = if (profile.ownerName.isNotBlank()) "Merhaba, ${profile.ownerName}" else Formatters.formatFriendlyDate(now),
            style = MaterialTheme.typography.bodySmall,
            color = Slate500
          )
          Text(
            text = profile.displayBusinessName,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
        androidx.compose.material3.IconButton(onClick = { viewModel.navigate(Screen.Settings) }) {
          Icon(androidx.compose.material.icons.Icons.Default.Settings, contentDescription = "Ayarlar", tint = palette.ink)
        }
      }
    }

    // Bugünün randevuları (giriş yapılmışsa)
    if (appointmentsToday != null) {
      item {
        val active = appointmentsToday.filter { it.status.isActive || it.status == com.example.cloud.AppointmentStatus.COMPLETED }
          .sortedBy { it.start }
        val next = active.firstOrNull { it.end > now && it.status.isActive }
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, palette.cardBorder),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { viewModel.navigate(Screen.Appointments) }
        ) {
          Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Event, contentDescription = null, tint = palette.ink)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text("Bugün ${active.size} randevu", fontWeight = FontWeight.SemiBold)
              Text(
                when {
                  next != null -> "Sıradaki: ${com.example.ui.cloud.timeOf(next.start)} • ${next.customerName}"
                  active.isNotEmpty() -> "Bugünkü randevular bitti"
                  else -> "Takvime göz atın"
                },
                style = MaterialTheme.typography.bodySmall,
                color = Slate500
              )
            }
            if (pendingCount > 0) {
              Surface(shape = RoundedCornerShape(10.dp), color = AmberWarning.copy(alpha = 0.15f)) {
                Text("$pendingCount onay bekliyor", color = AmberWarning, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
              }
            }
          }
        }
      }
    }

    // İşletme adı girilmemişse kısa yönlendirme
    if (!profile.isProfileComplete) {
      item {
        InfoBanner(
          icon = { Icon(Icons.Default.Store, contentDescription = null, tint = palette.ink) },
          text = "İşletme adınızı girin; WhatsApp mesajlarında bu ad görünür.",
          action = "Ayarlar",
          onAction = { viewModel.navigate(Screen.Settings) }
        )
      }
    }

    if (hasDemo) {
      item {
        InfoBanner(
          icon = { Icon(Icons.Default.Science, contentDescription = null, tint = AmberWarning) },
          text = "Şu an ÖRNEK veriler gösteriliyor. Gerçek müşterilerinizi eklemeden önce silebilirsiniz.",
          action = "Örnekleri sil",
          onAction = { viewModel.clearDemoData() }
        )
      }
    }

    // 2. Toplam alacak (ana kart)
    item {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = palette.heroBackground),
        border = BorderStroke(1.dp, palette.cardBorder),
        modifier = Modifier
          .fillMaxWidth()
          .clickable {
            viewModel.customerFilter.value = CustomerFilter.WITH_DEBT
            viewModel.navigate(Screen.Customers)
          }
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Text("Toplam alacak", style = MaterialTheme.typography.labelMedium, color = palette.heroMuted)
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = Formatters.formatCurrency(summary.totalReceivable),
            style = moneyStyle(32),
            color = if (palette.isDark) palette.success else palette.heroContent,
            maxLines = 1
          )
          Text(
            text = "${summary.debtorCount} borçlu müşteri",
            style = MaterialTheme.typography.bodySmall,
            color = palette.heroMuted
          )
          Spacer(modifier = Modifier.height(12.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            HeroPill(
              label = "Bu ay tahsilat",
              value = Formatters.formatCurrency(summary.thisMonthCollected),
              color = EmeraldPrimary,
              modifier = Modifier.weight(1f),
              onClick = { viewModel.navigate(Screen.Collections) }
            )
            HeroPill(
              label = "Vadesi geçen",
              value = Formatters.formatCurrency(summary.overdueAmount),
              color = RoseError,
              modifier = Modifier.weight(1f),
              onClick = {
                viewModel.customerFilter.value = CustomerFilter.OVERDUE
                viewModel.navigate(Screen.Customers)
              }
            )
          }
        }
      }
    }

    // 3. Dönem özeti
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, palette.cardBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 14.dp),
          horizontalArrangement = Arrangement.SpaceAround
        ) {
          PeriodSummaryItem("Bugün", Formatters.formatCurrency(summary.todayCollected))
          PeriodSummaryItem("Bu hafta", Formatters.formatCurrency(summary.thisWeekCollected))
          PeriodSummaryItem("Geçen ay", Formatters.formatCurrency(summary.lastMonthCollected))
        }
      }
    }

    if (customers.isEmpty()) {
      item {
        EmptyStateView(
          icon = Icons.Default.People,
          title = "İlk müşterinizi ekleyin",
          description = "Alttaki + düğmesinden müşteri, borç veya tahsilat ekleyebilirsiniz. Uygulamayı denemek isterseniz örnek verileri yükleyin.",
          actionButtonLabel = "Örnek verilerle dene",
          onActionClick = { viewModel.loadDemoData() }
        )
      }
    }

    // 4. Vadesi geçenler
    if (overdue.isNotEmpty()) {
      item {
        SectionHeader(
          title = "Vadesi geçenler (${overdue.size})",
          color = RoseError,
          action = "Tümü",
          onAction = {
            viewModel.customerFilter.value = CustomerFilter.OVERDUE
            viewModel.navigate(Screen.Customers)
          }
        )
      }
      items(overdue.take(5), key = { "o_" + it.debt.id }) { s ->
        val cwb = byId[s.debt.customerId]
        DueRow(
          name = cwb?.customer?.name ?: "Bilinmeyen müşteri",
          subtitle = "${Formatters.dueLabel(s.debt.dueDate)} • ${s.debt.category}",
          subtitleColor = RoseError,
          amount = Formatters.formatCurrency(s.remainingAmount),
          amountColor = RoseError,
          onClick = { cwb?.let { viewModel.selectCustomer(it.customer.id) } },
          onRemind = cwb?.takeIf { it.customer.phone.isNotBlank() }?.let { { viewModel.openDialog(DialogState.SendWhatsApp(it)) } }
        )
      }
    }

    // 5. Yaklaşan vadeler (7 gün)
    if (upcoming.isNotEmpty()) {
      item {
        SectionHeader(title = "Yaklaşan vadeler (7 gün)", color = AmberWarning)
      }
      items(upcoming.take(5), key = { "u_" + it.debt.id }) { s ->
        val cwb = byId[s.debt.customerId]
        DueRow(
          name = cwb?.customer?.name ?: "Bilinmeyen müşteri",
          subtitle = "${Formatters.dueLabel(s.debt.dueDate)} • ${Formatters.formatShortDate(s.debt.dueDate)}",
          subtitleColor = AmberWarning,
          amount = Formatters.formatCurrency(s.remainingAmount),
          amountColor = MaterialTheme.colorScheme.onSurface,
          onClick = { cwb?.let { viewModel.selectCustomer(it.customer.id) } },
          onRemind = cwb?.takeIf { it.customer.phone.isNotBlank() }?.let { { viewModel.openDialog(DialogState.SendWhatsApp(it)) } }
        )
      }
    }

    // 6. En çok borcu olanlar
    if (topDebtors.isNotEmpty()) {
      item {
        SectionHeader(
          title = "En çok borcu olanlar",
          action = "Tümü",
          onAction = {
            viewModel.customerFilter.value = CustomerFilter.WITH_DEBT
            viewModel.navigate(Screen.Customers)
          }
        )
      }
      items(topDebtors.take(4), key = { "d_" + it.customer.id }) { item ->
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, palette.cardBorder),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { viewModel.selectCustomer(item.customer.id) }
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = item.customer.name.take(1).uppercase(),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(item.customer.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
              Text(
                text = item.lastPaymentDate?.let { "Son ödeme: ${Formatters.formatShortDate(it)}" } ?: "Henüz ödeme yok",
                style = MaterialTheme.typography.bodySmall,
                color = Slate500
              )
            }
            Text(Formatters.formatCurrency(item.remainingBalance), style = moneyStyle(15), color = RoseError)
          }
        }
      }
    }

    // 7. Son tahsilatlar
    if (recentCollections.isNotEmpty()) {
      item {
        SectionHeader(title = "Son tahsilatlar", action = "Kasa", onAction = { viewModel.navigate(Screen.Collections) })
      }
      items(recentCollections, key = { "c_" + it.id }) { coll ->
        val cwb = byId[coll.customerId]
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, palette.cardBorder),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { cwb?.let { viewModel.selectCustomer(it.customer.id) } }
        ) {
          Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
              Text(cwb?.customer?.name ?: "Bilinmeyen müşteri", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
              Text(
                text = "${coll.paymentMethod.label} • ${Formatters.formatDate(coll.date)}",
                style = MaterialTheme.typography.bodySmall,
                color = Slate500
              )
            }
            Text("+" + Formatters.formatCurrency(coll.amount), style = moneyStyle(15), color = EmeraldPrimary)
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(90.dp)) }
  }
}

@Composable
private fun HeroPill(label: String, value: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
  val palette = LocalAppPalette.current
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = color.copy(alpha = if (palette.isDark) 0.10f else 0.08f),
    border = BorderStroke(1.dp, color.copy(alpha = 0.25f)),
    modifier = modifier.clickable(onClick = onClick)
  ) {
    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
      Text(label, fontSize = 11.sp, color = palette.heroMuted)
      Text(value, style = moneyStyle(14), color = color, maxLines = 1)
    }
  }
}

@Composable
private fun PeriodSummaryItem(label: String, value: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(label, style = MaterialTheme.typography.bodySmall, color = Slate500)
    Spacer(modifier = Modifier.height(2.dp))
    Text(value, style = moneyStyle(14), color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
  }
}

@Composable
private fun SectionHeader(
  title: String,
  color: Color = MaterialTheme.colorScheme.onBackground,
  action: String? = null,
  onAction: (() -> Unit)? = null
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 6.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(title, style = MaterialTheme.typography.titleMedium, color = color)
    if (action != null && onAction != null) {
      TextButton(onClick = onAction) { Text(action) }
    }
  }
}

@Composable
private fun DueRow(
  name: String,
  subtitle: String,
  subtitleColor: Color,
  amount: String,
  amountColor: Color,
  onClick: () -> Unit,
  onRemind: (() -> Unit)?
) {
  val palette = LocalAppPalette.current
  Card(
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, palette.cardBorder),
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
  ) {
    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
      Column(modifier = Modifier.weight(1f)) {
        Text(name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = subtitleColor, maxLines = 1)
      }
      Column(horizontalAlignment = Alignment.End) {
        Text(amount, style = moneyStyle(15), color = amountColor)
        if (onRemind != null) {
          Spacer(modifier = Modifier.height(4.dp))
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = WhatsAppGreen.copy(alpha = 0.15f),
            modifier = Modifier.clickable(onClick = onRemind)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.Send, contentDescription = null, tint = WhatsAppGreen, modifier = Modifier.size(12.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Hatırlat", color = WhatsAppGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun InfoBanner(icon: @Composable () -> Unit, text: String, action: String, onAction: () -> Unit) {
  val palette = LocalAppPalette.current
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surfaceVariant,
    border = BorderStroke(1.dp, palette.cardBorder),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
      icon()
      Spacer(modifier = Modifier.width(10.dp))
      Text(text, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
      OutlinedButton(onClick = onAction, modifier = Modifier.padding(start = 6.dp)) {
        Text(action, fontSize = 12.sp)
      }
    }
  }
}
