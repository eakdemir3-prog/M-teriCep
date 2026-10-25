package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CustomerWithBalance
import com.example.ui.components.EmptyStateView
import com.example.ui.components.StatCard
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.RoseError
import com.example.ui.theme.Slate500
import com.example.util.Formatters
import com.example.viewmodel.DialogState
import com.example.viewmodel.MusteriCepViewModel
import com.example.viewmodel.Screen

@Composable
fun DashboardScreen(
  viewModel: MusteriCepViewModel,
  modifier: Modifier = Modifier
) {
  val summary by viewModel.dashboardSummary.collectAsState()
  val profile by viewModel.profile.collectAsState()
  val customers by viewModel.allCustomersWithBalance.collectAsState(initial = emptyList())
  val collections by viewModel.allCollections.collectAsState(initial = emptyList())
  val debts by viewModel.allDebts.collectAsState(initial = emptyList())

  val now = System.currentTimeMillis()
  val debtorCustomers = customers.filter { it.remainingBalance > 0 }.sortedByDescending { it.remainingBalance }
  val overdueDebts = debts.filter { it.dueDate != null && it.dueDate < now }
  val recentCollections = collections.take(5)

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // 1. Header Banner
    item {
      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = profile.businessName,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "Hoş geldiniz, ${profile.ownerName} • ${Formatters.formatFriendlyDate(System.currentTimeMillis())}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        // Tier Badge
        Surface(
          shape = RoundedCornerShape(20.dp),
          color = MaterialTheme.colorScheme.primaryContainer,
          modifier = Modifier.clickable { viewModel.navigate(Screen.Pricing) }
        ) {
          Text(
            text = profile.activeTier.title,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
          )
        }
      }
    }

    // 2. Summary Stat Cards (2x2 Grid)
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Toplam Alacak
        StatCard(
          title = "Toplam Alacak",
          value = Formatters.formatCurrency(summary.totalReceivable),
          icon = Icons.Default.ReceiptLong,
          accentColor = NavyPrimary,
          subtitle = "${debtorCustomers.size} borçlu müşteri",
          modifier = Modifier.weight(1f),
          onClick = {
            viewModel.customerFilter.value = com.example.model.CustomerFilter.WITH_DEBT
            viewModel.navigate(Screen.Customers)
          }
        )

        // Tahsil Edilen
        StatCard(
          title = "Toplam Tahsilat",
          value = Formatters.formatCurrency(summary.totalCollected),
          icon = Icons.Default.MonetizationOn,
          accentColor = EmeraldPrimary,
          subtitle = "${collections.size} işlem",
          modifier = Modifier.weight(1f),
          onClick = { viewModel.navigate(Screen.Collections) }
        )
      }
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Vadesi Geçen
        StatCard(
          title = "Vadesi Geçen",
          value = Formatters.formatCurrency(summary.overdueAmount),
          icon = Icons.Default.ErrorOutline,
          accentColor = RoseError,
          subtitle = "${summary.overdueDebtsCount} kayıt gecikti",
          modifier = Modifier.weight(1f)
        )

        // Aktif Müşteri
        StatCard(
          title = "Kayıtlı Müşteri",
          value = "${summary.activeCustomerCount}",
          icon = Icons.Default.People,
          accentColor = AmberWarning,
          subtitle = "Paket: ${profile.activeTier.title}",
          modifier = Modifier.weight(1f),
          onClick = {
            viewModel.customerFilter.value = com.example.model.CustomerFilter.ALL
            viewModel.navigate(Screen.Customers)
          }
        )
      }
    }

    // 3. Günlük Kasa & Tahsilat Performansı Kartı
    item {
      ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.TrendingUp, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Tahsilat Performansı", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            TextButton(onClick = { viewModel.navigate(Screen.Collections) }) {
              Text("Tüm Kasa", fontSize = 12.sp)
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
          ) {
            PeriodSummaryItem("Bugün", Formatters.formatCurrency(summary.todayCollected), EmeraldPrimary)
            PeriodSummaryItem("Bu Hafta", Formatters.formatCurrency(summary.thisWeekCollected), NavyPrimary)
            PeriodSummaryItem("Bu Ay", Formatters.formatCurrency(summary.thisMonthCollected), AmberWarning)
          }
        }
      }
    }

    // Empty state helper if no customers
    if (customers.isEmpty()) {
      item {
        EmptyStateView(
          icon = Icons.Default.People,
          title = "Henüz müşteri kaydı bulunmuyor",
          description = "Uygulamayı hemen keşfetmek için örnek esnaf verilerini yükleyebilir veya ilk müşterinizi ekleyebilirsiniz.",
          actionButtonLabel = "Örnek Esnaf Verisi Yükle",
          onActionClick = { viewModel.loadDemoData() }
        )
      }
    }

    // 4. Vadesi Geçmiş & Hatırlatma Bekleyenler
    if (overdueDebts.isNotEmpty()) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Vadesi Geçen Alacaklar (${overdueDebts.size})",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = RoseError
          )
        }
      }

      items(overdueDebts.take(4)) { debt ->
        val customer = customers.find { it.customer.id == debt.customerId }
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { customer?.let { viewModel.selectCustomer(it.customer.id) } }
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = customer?.customer?.name ?: "Bilinmeyen Müşteri",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
              )
              Text(
                text = "${debt.category} • Vade: ${Formatters.formatDate(debt.dueDate)}",
                style = MaterialTheme.typography.bodySmall,
                color = RoseError
              )
            }

            Column(horizontalAlignment = Alignment.End) {
              Text(
                text = Formatters.formatCurrency(debt.amount),
                fontWeight = FontWeight.ExtraBold,
                color = RoseError,
                fontSize = 15.sp
              )
              Spacer(modifier = Modifier.height(4.dp))
              // Direct WhatsApp button
              if (customer != null) {
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = Color(0xFF25D366).copy(alpha = 0.15f),
                  modifier = Modifier.clickable {
                    viewModel.openDialog(DialogState.SendWhatsApp(customer))
                  }
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(
                      imageVector = Icons.Default.Send,
                      contentDescription = null,
                      tint = Color(0xFF25D366),
                      modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Hatırlat", color = Color(0xFF25D366), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                  }
                }
              }
            }
          }
        }
      }
    }

    // 5. En Çok Borcu Olan Müşteriler
    if (debtorCustomers.isNotEmpty()) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "En Çok Borcu Olan Müşteriler",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
          )
          TextButton(onClick = {
            viewModel.customerFilter.value = com.example.model.CustomerFilter.WITH_DEBT
            viewModel.navigate(Screen.Customers)
          }) {
            Text("Tümünü Gör")
          }
        }
      }

      items(debtorCustomers.take(4)) { item ->
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { viewModel.selectCustomer(item.customer.id) }
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              Box(
                modifier = Modifier
                  .size(40.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = item.customer.name.take(1).uppercase(),
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary,
                  fontSize = 16.sp
                )
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = item.customer.name,
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp
                )
                Text(
                  text = Formatters.formatPhoneForDisplay(item.customer.phone),
                  style = MaterialTheme.typography.bodySmall,
                  color = Slate500
                )
              }
            }

            Column(horizontalAlignment = Alignment.End) {
              Text(
                text = Formatters.formatCurrency(item.remainingBalance),
                fontWeight = FontWeight.ExtraBold,
                color = RoseError,
                fontSize = 15.sp
              )
              Text(
                text = "Kalan Bakiye",
                style = MaterialTheme.typography.labelSmall,
                color = Slate500
              )
            }
          }
        }
      }
    }

    // 6. Son Tahsilatlar
    if (recentCollections.isNotEmpty()) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Son Tahsilatlar",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
          )
          TextButton(onClick = { viewModel.navigate(Screen.Collections) }) {
            Text("Kasa Detayı")
          }
        }
      }

      items(recentCollections) { coll ->
        val customer = customers.find { it.customer.id == coll.customerId }
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = customer?.customer?.name ?: "Bilinmeyen Müşteri",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
              )
              Text(
                text = "${coll.paymentMethod.label} • ${Formatters.formatDateTime(coll.date)}",
                style = MaterialTheme.typography.bodySmall,
                color = Slate500
              )
            }
            Text(
              text = "+${Formatters.formatCurrency(coll.amount)}",
              fontWeight = FontWeight.ExtraBold,
              color = EmeraldPrimary,
              fontSize = 15.sp
            )
          }
        }
      }
    }

    // Bottom spacing
    item {
      Spacer(modifier = Modifier.height(80.dp))
    }
  }
}

@Composable
private fun PeriodSummaryItem(label: String, value: String, color: Color) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(label, style = MaterialTheme.typography.bodySmall, color = Slate500)
    Spacer(modifier = Modifier.height(2.dp))
    Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = color)
  }
}
