package com.example.ui.screens

import com.example.util.tr

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SouthWest
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.cloud.Appointment
import com.example.cloud.AppointmentStatus
import com.example.model.CustomerFilter
import com.example.model.DebtStatus
import com.example.ui.cloud.timeOf
import com.example.ui.components.AppCard
import com.example.ui.components.BarChart
import com.example.ui.components.EmptyStateView
import com.example.ui.components.IconBadge
import com.example.ui.components.QuickActionTile
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.LocalAppPalette
import com.example.ui.theme.moneyStyle
import com.example.util.Formatters
import com.example.util.Money
import com.example.viewmodel.DialogState
import com.example.viewmodel.MusteriCepViewModel
import com.example.viewmodel.Screen
import java.util.Calendar

/** Ana ekrandaki son işlemler satırı (borç veya tahsilat). */
private data class RecentTx(
  val key: String,
  val customerId: Long,
  val name: String,
  val isCollection: Boolean,
  val label: String,
  val date: Long,
  val sortKey: Long,
  val amount: Double,
  val balance: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
  viewModel: MusteriCepViewModel,
  modifier: Modifier = Modifier,
  appointmentsToday: List<Appointment>? = null,
  pendingCount: Int = 0
) {
  val summary by viewModel.dashboardSummary.collectAsState()
  val profile by viewModel.profile.collectAsState()
  val customers by viewModel.allCustomersWithBalance.collectAsState()
  val collections by viewModel.allCollections.collectAsState()
  val statuses by viewModel.debtStatuses.collectAsState()
  val hasDemo by viewModel.hasDemoData.collectAsState()
  val palette = LocalAppPalette.current
  var chartMode by rememberSaveable { mutableIntStateOf(0) } // 0 = hafta, 1 = ay

  val now = System.currentTimeMillis()
  val byId = remember(customers) { customers.associateBy { it.customer.id } }
  val overdue = statuses
    .filter { it.status == DebtStatus.OVERDUE && byId[it.debt.customerId]?.customer?.isArchived == false }
    .sortedBy { it.debt.dueDate }

  // Son işlemler: borç ve tahsilatlar birlikte, en yeniden eskiye
  val recent = remember(statuses, collections, customers) {
    val list = mutableListOf<RecentTx>()
    statuses.forEach { s ->
      val c = byId[s.debt.customerId] ?: return@forEach
      list += RecentTx(
        "d${s.debt.id}", c.customer.id, c.customer.name, false,
        s.debt.category.ifBlank { tr("Borç") }, s.debt.date, maxOf(s.debt.date, s.debt.createdAt), s.debt.amount, c.remainingBalance
      )
    }
    collections.forEach { col ->
      val c = byId[col.customerId] ?: return@forEach
      list += RecentTx(
        "c${col.id}", c.customer.id, c.customer.name, true,
        tr("Tahsilat • {0}", col.paymentMethod.label), col.date, maxOf(col.date, col.createdAt), col.amount, c.remainingBalance
      )
    }
    list.sortedByDescending { it.sortKey }.take(6)
  }

  // Tahsilat grafiği: haftalık (bu haftanın günleri) veya aylık (bu ayın haftaları)
  val chart = remember(collections, chartMode) { buildChart(collections.map { it.date to it.amount }, chartMode, now) }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    // 1. Üst alan: logo, işletme, bildirim ve profil
    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Image(
          painter = painterResource(R.drawable.ic_brand),
          contentDescription = null,
          modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            profile.displayBusinessName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            if (profile.ownerName.isNotBlank()) tr("Merhaba {0} • {1}", profile.ownerName.substringBefore(' '), Formatters.formatFriendlyDate(now))
            else Formatters.formatFriendlyDate(now),
            style = MaterialTheme.typography.bodySmall,
            color = palette.muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
        IconButton(onClick = {
          if (appointmentsToday != null) viewModel.navigate(Screen.Appointments) else viewModel.navigate(Screen.Settings)
        }) {
          Icon(Icons.Default.NotificationsNone, contentDescription = tr("Bildirimler"), tint = MaterialTheme.colorScheme.onBackground)
        }
        IconButton(onClick = { viewModel.navigate(Screen.Settings) }) {
          Icon(Icons.Default.Person, contentDescription = tr("Profil ve ayarlar"), tint = MaterialTheme.colorScheme.onBackground)
        }
      }
    }

    if (!profile.isProfileComplete) {
      item {
        NoticeRow(
          icon = Icons.Default.Storefront,
          tint = palette.ink,
          text = tr("İşletme adınızı girin; müşteriye giden mesajlarda görünür."),
          action = tr("Gir"),
          onAction = { viewModel.navigate(Screen.Settings) }
        )
      }
    }
    if (hasDemo) {
      item {
        NoticeRow(
          icon = Icons.Default.Science,
          tint = palette.warning,
          text = tr("Örnek veriler gösteriliyor."),
          action = tr("Örnekleri sil"),
          onAction = { viewModel.clearDemoData() }
        )
      }
    }

    // 2. Finansal özet
    item {
      AppCard(onClick = {
        viewModel.customerFilter.value = CustomerFilter.WITH_DEBT
        viewModel.navigate(Screen.Customers)
      }) {
        Text(tr("Toplam alacak"), style = MaterialTheme.typography.labelLarge, color = palette.muted)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          Formatters.formatCurrency(summary.totalReceivable),
          style = moneyStyle(30, FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1
        )
        Text(tr("{0} borçlu müşteri", summary.debtorCount), style = MaterialTheme.typography.bodySmall, color = palette.muted)
        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = palette.cardBorder)
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
          MiniStat(tr("Bu ay tahsil"), Formatters.formatCurrencyShort(summary.thisMonthCollected), palette.success, Modifier.weight(1f))
          MiniStat(tr("Gecikmiş"), Formatters.formatCurrencyShort(summary.overdueAmount), if (summary.overdueAmount > 0) palette.danger else palette.muted, Modifier.weight(1f))
          MiniStat(tr("Yaklaşan (7 gün)"), Formatters.formatCurrencyShort(summary.upcomingAmount), if (summary.upcomingAmount > 0) palette.warning else palette.muted, Modifier.weight(1f))
        }
      }
    }

    // 3. Hızlı işlemler
    item {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        QuickActionTile(Icons.Default.PersonAdd, tr("Müşteri\nekle"), Modifier.weight(1f)) { viewModel.openDialog(DialogState.AddEditCustomer()) }
        QuickActionTile(Icons.AutoMirrored.Filled.ReceiptLong, tr("Borç\nekle"), Modifier.weight(1f)) { viewModel.openDialog(DialogState.AddDebt()) }
        QuickActionTile(Icons.Default.Payments, tr("Tahsilat\nal"), Modifier.weight(1f)) { viewModel.openDialog(DialogState.AddCollection()) }
        QuickActionTile(Icons.Default.BarChart, tr("Raporlar"), Modifier.weight(1f)) { viewModel.navigate(Screen.ReportsAndExport) }
      }
    }

    // Bugünün randevuları (giriş yapılmışsa)
    if (appointmentsToday != null) {
      item {
        val active = appointmentsToday.filter { it.status.isActive || it.status == AppointmentStatus.COMPLETED }.sortedBy { it.start }
        val next = active.firstOrNull { it.end > now && it.status.isActive }
        AppCard(onClick = { viewModel.navigate(Screen.Appointments) }, padding = 14.dp) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Default.Event, palette.ink, palette.accentSoft)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(tr("Bugün {0} randevu", active.size), fontWeight = FontWeight.SemiBold, maxLines = 1)
              Text(
                when {
                  next != null -> tr("Sıradaki {0} • {1}", timeOf(next.start), next.customerName)
                  active.isNotEmpty() -> tr("Bugünkü randevular tamamlandı")
                  else -> tr("Takvime göz atın")
                },
                style = MaterialTheme.typography.bodySmall,
                color = palette.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
            if (pendingCount > 0) {
              Spacer(modifier = Modifier.width(8.dp))
              StatusPill(tr("{0} onay bekliyor", pendingCount), palette.warning, palette.warningSoft)
            }
          }
        }
      }
    }

    // 4. Tahsilat grafiği
    item {
      AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Column(modifier = Modifier.weight(1f)) {
            Text(tr("Tahsilatlar"), style = MaterialTheme.typography.titleSmall)
            Text(
              Formatters.formatCurrency(chart.total),
              style = moneyStyle(20, FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1
            )
          }
          SingleChoiceSegmentedButtonRow {
            listOf(tr("Hafta"), tr("Ay")).forEachIndexed { i, label ->
              SegmentedButton(
                selected = chartMode == i,
                onClick = { chartMode = i },
                shape = SegmentedButtonDefaults.itemShape(index = i, count = 2),
                icon = {},
                colors = SegmentedButtonDefaults.colors(
                  activeContainerColor = palette.accentSoft,
                  activeContentColor = palette.ink,
                  activeBorderColor = palette.cardBorder,
                  inactiveBorderColor = palette.cardBorder
                )
              ) { Text(label, fontSize = 12.sp, maxLines = 1) }
            }
          }
        }
        Spacer(modifier = Modifier.height(4.dp))
        ChangeLine(current = chart.total, previous = chart.previousTotal, periodName = if (chartMode == 0) tr("geçen hafta") else tr("geçen ay"))
        Spacer(modifier = Modifier.height(14.dp))
        if (chart.total <= 0.0 && chart.previousTotal <= 0.0) {
          Text(
            tr("Bu dönemde tahsilat yok. Tahsilat aldıkça grafik burada oluşur."),
            style = MaterialTheme.typography.bodySmall,
            color = palette.muted,
            modifier = Modifier.padding(vertical = 18.dp)
          )
        } else {
          BarChart(values = chart.values, labels = chart.labels, highlightIndex = chart.currentIndex)
        }
      }
    }

    if (customers.isEmpty()) {
      item {
        EmptyStateView(
          icon = Icons.Default.Person,
          title = tr("İlk müşterinizi ekleyin"),
          description = tr("Yukarıdaki \"Müşteri ekle\" ile başlayın. Denemek isterseniz örnek verileri yükleyebilirsiniz."),
          actionButtonLabel = tr("Örnek verilerle dene"),
          onActionClick = { viewModel.loadDemoData() }
        )
      }
    }

    // 5. Gecikmiş alacaklar (en fazla 3)
    if (overdue.isNotEmpty()) {
      item {
        SectionHeader(
          tr("Gecikmiş ödemeler"),
          color = palette.danger,
          action = tr("Tümü ({0})", overdue.size),
          onAction = {
            viewModel.customerFilter.value = CustomerFilter.OVERDUE
            viewModel.navigate(Screen.Customers)
          }
        )
      }
      item {
        AppCard(padding = 0.dp) {
          overdue.take(3).forEachIndexed { i, s ->
            val cwb = byId[s.debt.customerId]
            if (i > 0) HorizontalDivider(color = palette.cardBorder, modifier = Modifier.padding(horizontal = 16.dp))
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(cwb?.customer?.name ?: tr("Bilinmeyen müşteri"), fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(Formatters.dueLabel(s.debt.dueDate), style = MaterialTheme.typography.bodySmall, color = palette.danger, maxLines = 1)
              }
              Spacer(modifier = Modifier.width(8.dp))
              Text(Formatters.formatCurrency(s.remainingAmount), style = moneyStyle(14), color = palette.danger, maxLines = 1)
              if (cwb != null && cwb.customer.phone.isNotBlank()) {
                TextButton(onClick = { viewModel.openDialog(DialogState.SendWhatsApp(cwb)) }) {
                  Text(tr("Hatırlat"), color = palette.ink, maxLines = 1, softWrap = false)
                }
              }
            }
          }
        }
      }
    }

    // 6. Son işlemler
    if (recent.isNotEmpty()) {
      item {
        SectionHeader(tr("Son işlemler"), action = tr("Kasa"), onAction = { viewModel.navigate(Screen.Collections) })
      }
      item {
        AppCard(padding = 0.dp) {
          recent.forEachIndexed { i, tx ->
            if (i > 0) HorizontalDivider(color = palette.cardBorder, modifier = Modifier.padding(horizontal = 16.dp))
            RecentRow(tx) { viewModel.selectCustomer(tx.customerId) }
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(24.dp)) }
  }
}

@Composable
private fun MiniStat(label: String, value: String, color: Color, modifier: Modifier) {
  Column(modifier = modifier) {
    Text(label, fontSize = 11.sp, color = LocalAppPalette.current.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
    Spacer(modifier = Modifier.height(2.dp))
    Text(value, style = moneyStyle(14), color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
  }
}

@Composable
private fun ChangeLine(current: Double, previous: Double, periodName: String) {
  val palette = LocalAppPalette.current
  val text: String
  val color: Color
  when {
    previous <= 0.0 && current <= 0.0 -> { text = tr("Henüz veri yok"); color = palette.muted }
    previous <= 0.0 -> { text = tr("{0} tahsilat yoktu", periodName); color = palette.muted }
    else -> {
      val pct = ((current - previous) / previous * 100).toInt()
      text = when {
        pct > 0 -> tr("%{0} artış ({1} {2})", pct, periodName, Formatters.formatCurrencyShort(previous))
        pct < 0 -> tr("%{0} azalış ({1} {2})", -pct, periodName, Formatters.formatCurrencyShort(previous))
        else -> tr("{0} ile aynı", periodName)
      }
      color = if (pct >= 0) palette.success else palette.danger
    }
  }
  Text(text, style = MaterialTheme.typography.bodySmall, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun RecentRow(tx: RecentTx, onClick: () -> Unit) {
  val palette = LocalAppPalette.current
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 2.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    androidx.compose.foundation.layout.Box(
      modifier = Modifier
        .padding(vertical = 10.dp)
    ) {
      IconBadge(
        icon = if (tx.isCollection) Icons.Default.SouthWest else Icons.Default.NorthEast,
        tint = if (tx.isCollection) palette.success else palette.danger,
        background = if (tx.isCollection) palette.accentSoft else palette.dangerSoft,
        size = 36.dp,
        iconSize = 18.dp
      )
    }
    Spacer(modifier = Modifier.width(12.dp))
    Column(
      modifier = Modifier
        .weight(1f)
        .padding(vertical = 10.dp)
    ) {
      Text(tx.name, fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
      Text(
        tr("{0} • {1}", tx.label, Formatters.formatShortDate(tx.date)),
        style = MaterialTheme.typography.bodySmall,
        color = palette.muted,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
    Spacer(modifier = Modifier.width(8.dp))
    Column(horizontalAlignment = Alignment.End) {
      Text(
        (if (tx.isCollection) "+" else "") + Formatters.formatCurrency(tx.amount),
        style = moneyStyle(14),
        color = if (tx.isCollection) palette.success else MaterialTheme.colorScheme.onSurface,
        maxLines = 1
      )
      Text(
        tr("Bakiye {0}", Formatters.formatCurrencyShort(tx.balance)),
        fontSize = 11.sp,
        color = palette.muted,
        maxLines = 1
      )
    }
  }
}

@Composable
private fun NoticeRow(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, text: String, action: String, onAction: () -> Unit) {
  val palette = LocalAppPalette.current
  AppCard(padding = 0.dp) {
    Row(modifier = Modifier.padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
      Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
      Spacer(modifier = Modifier.width(10.dp))
      Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
      TextButton(onClick = onAction) { Text(action, color = palette.ink, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false) }
    }
  }
}

/** Grafik verisi. */
private data class ChartData(
  val values: List<Double>,
  val labels: List<String>,
  val currentIndex: Int,
  val total: Double,
  val previousTotal: Double
)

private fun buildChart(items: List<Pair<Long, Double>>, mode: Int, now: Long): ChartData {
  val day = 86_400_000L
  return if (mode == 0) {
    val weekStart = Formatters.startOfWeek(now)
    val prevStart = weekStart - 7 * day
    val values = (0 until 7).map { i ->
      val s = dayStartPlus(weekStart, i)
      val e = dayStartPlus(weekStart, i + 1)
      Money.sum(items.filter { it.first in s until e }.map { it.second })
    }
    val todayIdx = ((Formatters.startOfDay(now) - weekStart) / day).toInt().coerceIn(0, 6)
    ChartData(
      values = values,
      labels = listOf(tr("Pzt"), tr("Sal"), tr("Çar"), tr("Per"), tr("Cum"), tr("Cmt"), tr("Paz")),
      currentIndex = todayIdx,
      total = Money.sum(values),
      previousTotal = Money.sum(items.filter { it.first in prevStart until weekStart }.map { it.second })
    )
  } else {
    val monthStart = Formatters.startOfMonth(now)
    val nextMonth = Calendar.getInstance().apply { timeInMillis = monthStart; add(Calendar.MONTH, 1) }.timeInMillis
    val prevMonth = Formatters.startOfMonth(monthStart - 1)
    // Ayın günleri 1-7, 8-14, 15-21, 22-28, 29-son şeklinde 5 dilime ayrılır
    val bounds = listOf(1, 8, 15, 22, 29).map { d -> Calendar.getInstance().apply { timeInMillis = monthStart; set(Calendar.DAY_OF_MONTH, d) }.timeInMillis }
    val values = bounds.mapIndexed { i, s ->
      val e = if (i + 1 < bounds.size) bounds[i + 1] else nextMonth
      if (s >= nextMonth) 0.0 else Money.sum(items.filter { it.first in s until e }.map { it.second })
    }
    val dom = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.DAY_OF_MONTH)
    ChartData(
      values = values,
      labels = listOf("1-7", "8-14", "15-21", "22-28", "29+"),
      currentIndex = ((dom - 1) / 7).coerceIn(0, 4),
      total = Money.sum(values),
      previousTotal = Money.sum(items.filter { it.first in prevMonth until monthStart }.map { it.second })
    )
  }
}

private fun dayStartPlus(base: Long, days: Int): Long =
  Calendar.getInstance().apply { timeInMillis = base; add(Calendar.DAY_OF_MONTH, days) }.timeInMillis
