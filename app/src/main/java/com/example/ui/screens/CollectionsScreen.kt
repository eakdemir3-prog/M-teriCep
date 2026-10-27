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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PaymentMethod
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.Slate500
import com.example.util.Formatters
import com.example.util.Money
import androidx.compose.foundation.horizontalScroll
import com.example.ui.theme.moneyStyle
import androidx.compose.material.icons.filled.Edit
import com.example.viewmodel.DialogState
import com.example.viewmodel.MusteriCepViewModel

@Composable
fun CollectionsScreen(
  viewModel: MusteriCepViewModel,
  modifier: Modifier = Modifier
) {
  val collections by viewModel.allCollections.collectAsState(initial = emptyList())
  val customers by viewModel.allCustomersWithBalance.collectAsState(initial = emptyList())
  var selectedMethodFilter by remember { mutableStateOf<PaymentMethod?>(null) }

  var period by remember { mutableStateOf(CollectionPeriod.MONTH) }
  val now = System.currentTimeMillis()
  val periodStart = when (period) {
    CollectionPeriod.TODAY -> Formatters.startOfDay(now)
    CollectionPeriod.WEEK -> Formatters.startOfWeek(now)
    CollectionPeriod.MONTH -> Formatters.startOfMonth(now)
    CollectionPeriod.ALL -> Long.MIN_VALUE
  }
  val inPeriod = collections.filter { it.date >= periodStart }

  val filteredCollections = if (selectedMethodFilter == null) {
    inPeriod
  } else {
    inPeriod.filter { it.paymentMethod == selectedMethodFilter }
  }

  val totalAmount = Money.sum(filteredCollections.map { it.amount })

  // Ödeme yöntemine göre dağılım (seçili dönem)
  val cashTotal = Money.sum(inPeriod.filter { it.paymentMethod == PaymentMethod.CASH }.map { it.amount })
  val bankTotal = Money.sum(inPeriod.filter { it.paymentMethod == PaymentMethod.BANK_TRANSFER }.map { it.amount })
  val cardTotal = Money.sum(inPeriod.filter { it.paymentMethod == PaymentMethod.CREDIT_CARD }.map { it.amount })

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = "Kasa",
        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = "Müşterilerden alınan ödemeler ve nakit akışı",
        style = MaterialTheme.typography.bodySmall,
        color = Slate500
      )
    }

    // Top Kasa Summary Card
    item {
      ElevatedCard(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Text("${period.label} alınan tahsilat", style = MaterialTheme.typography.labelMedium, color = Slate500)
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = Formatters.formatCurrency(totalAmount),
            style = moneyStyle(28),
            color = EmeraldPrimary
          )
          Spacer(modifier = Modifier.height(10.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            CollectionPeriod.entries.forEach { p ->
              FilterChip(
                selected = period == p,
                onClick = { period = p },
                label = {
                  Text(p.label, fontSize = 12.sp, maxLines = 1, softWrap = false, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                },
                modifier = Modifier.weight(1f)
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Breakdown pills
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            MethodPill("Nakit", Formatters.formatCurrencyShort(cashTotal), Icons.Default.Payments, Modifier.weight(1f))
            MethodPill("Havale/EFT", Formatters.formatCurrencyShort(bankTotal), Icons.Default.AccountBalance, Modifier.weight(1f))
            MethodPill("Kart", Formatters.formatCurrencyShort(cardTotal), Icons.Default.CreditCard, Modifier.weight(1f))
          }
        }
      }
    }

    // Method Filter Chips
    item {
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
          FilterChip(
            selected = selectedMethodFilter == null,
            onClick = { selectedMethodFilter = null },
            label = { Text("Tümü (${inPeriod.size})") }
          )
        }
        items(PaymentMethod.values()) { method ->
          val count = inPeriod.count { it.paymentMethod == method }
          FilterChip(
            selected = selectedMethodFilter == method,
            onClick = { selectedMethodFilter = method },
            label = { Text("${method.label} ($count)") }
          )
        }
      }
    }

    if (filteredCollections.isEmpty()) {
      item {
        EmptyStateView(
          icon = Icons.Default.MonetizationOn,
          title = if (collections.isEmpty()) "Henüz tahsilat yok" else "Bu dönemde tahsilat yok",
          description = "Müşterilerinizden ödeme aldığınızda hızlı tahsilat düğmesi ile buraya kaydedebilirsiniz.",
          actionButtonLabel = "Yeni Tahsilat Al",
          onActionClick = { viewModel.openDialog(DialogState.AddCollection()) }
        )
      }
    }

    items(filteredCollections, key = { it.id }) { coll ->
      val customer = customers.find { it.customer.id == coll.customerId }
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
          .fillMaxWidth()
          .clickable {
            customer?.let { viewModel.selectCustomer(it.customer.id) }
          }
          .testTag("collection_item_${coll.id}")
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
              modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(EmeraldPrimary.copy(alpha = 0.12f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = when (coll.paymentMethod) {
                  PaymentMethod.CASH -> Icons.Default.Payments
                  PaymentMethod.BANK_TRANSFER -> Icons.Default.AccountBalance
                  PaymentMethod.CREDIT_CARD -> Icons.Default.CreditCard
                  else -> Icons.Default.MonetizationOn
                },
                contentDescription = null,
                tint = EmeraldPrimary,
                modifier = Modifier.size(22.dp)
              )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
              Text(
                text = customer?.customer?.name ?: "Bilinmeyen Müşteri",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
              )
              Text(
                text = "${coll.paymentMethod.label} • ${Formatters.formatDate(coll.date)}",
                style = MaterialTheme.typography.bodySmall,
                color = Slate500
              )
              if (coll.notes.isNotBlank()) {
                Text(
                  maxLines = 1,
                  overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                  text = coll.notes,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }

          Spacer(modifier = Modifier.width(8.dp))
          Column(horizontalAlignment = Alignment.End) {
            Text(
              maxLines = 1,
              softWrap = false,
              text = "+" + Formatters.formatCurrency(coll.amount),
              style = moneyStyle(15),
              color = EmeraldPrimary
            )
            Row {
              IconButton(onClick = { viewModel.editCollection(coll.id) }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "Düzenle", tint = Slate500, modifier = Modifier.size(16.dp))
              }
              IconButton(
                onClick = {
                  viewModel.openDialog(
                    DialogState.ConfirmDelete(
                      title = "Tahsilatı Sil",
                      message = "${Formatters.formatCurrency(coll.amount)} tutarındaki tahsilat silinsin mi? Hemen ardından \"Geri Al\" ile geri getirebilirsiniz.",
                      onConfirm = { viewModel.deleteCollection(coll.id) }
                    )
                  )
                },
                modifier = Modifier.size(32.dp)
              ) {
                Icon(Icons.Default.Delete, contentDescription = "Sil", tint = Slate500, modifier = Modifier.size(16.dp))
              }
            }
          }
        }
      }
    }

    item {
      Spacer(modifier = Modifier.height(84.dp))
    }
  }
}

@Composable
private fun MethodPill(label: String, amount: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    modifier = modifier
  ) {
    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, fontSize = 11.sp, color = Slate500, maxLines = 1, softWrap = false, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
      }
      Spacer(modifier = Modifier.height(2.dp))
      Text(amount, style = moneyStyle(13), maxLines = 1, softWrap = false, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
    }
  }
}

private enum class CollectionPeriod(val label: String) {
  TODAY("Bugün"),
  WEEK("Bu hafta"),
  MONTH("Bu ay"),
  ALL("Tümü")
}
