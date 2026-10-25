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

  val filteredCollections = if (selectedMethodFilter == null) {
    collections
  } else {
    collections.filter { it.paymentMethod == selectedMethodFilter }
  }

  val totalAmount = filteredCollections.sumOf { it.amount }

  // Method breakdown totals
  val cashTotal = collections.filter { it.paymentMethod == PaymentMethod.CASH }.sumOf { it.amount }
  val bankTotal = collections.filter { it.paymentMethod == PaymentMethod.BANK_TRANSFER }.sumOf { it.amount }
  val cardTotal = collections.filter { it.paymentMethod == PaymentMethod.CREDIT_CARD }.sumOf { it.amount }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = "Tahsilatlar & Kasa",
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
          Text("Toplam Alınan Tahsilat", style = MaterialTheme.typography.labelMedium, color = Slate500)
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = Formatters.formatCurrency(totalAmount),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = EmeraldPrimary
          )

          Spacer(modifier = Modifier.height(14.dp))

          // Breakdown pills
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            MethodPill("Nakit", Formatters.formatCurrency(cashTotal), Icons.Default.Payments)
            MethodPill("Havale/EFT", Formatters.formatCurrency(bankTotal), Icons.Default.AccountBalance)
            MethodPill("Kart", Formatters.formatCurrency(cardTotal), Icons.Default.CreditCard)
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
            label = { Text("Tümü (${collections.size})") }
          )
        }
        items(PaymentMethod.values()) { method ->
          val count = collections.count { it.paymentMethod == method }
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
          title = "Tahsilat kaydı bulunmuyor",
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
                .size(42.dp)
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
                fontSize = 14.sp
              )
              Text(
                text = "${coll.paymentMethod.label} • ${Formatters.formatDateTime(coll.date)}",
                style = MaterialTheme.typography.bodySmall,
                color = Slate500
              )
              if (coll.notes.isNotBlank()) {
                Text(
                  text = coll.notes,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }

          Column(horizontalAlignment = Alignment.End) {
            Text(
              text = "+" + Formatters.formatCurrency(coll.amount),
              fontWeight = FontWeight.ExtraBold,
              color = EmeraldPrimary,
              fontSize = 15.sp
            )
            IconButton(
              onClick = {
                viewModel.openDialog(
                  DialogState.ConfirmDelete(
                    title = "Tahsilatı Sil",
                    message = "${Formatters.formatCurrency(coll.amount)} tutarındaki tahsilatı silmek istediğinize emin misiniz?",
                    onConfirm = { viewModel.deleteCollection(coll) }
                  )
                )
              },
              modifier = Modifier.size(28.dp)
            ) {
              Icon(Icons.Default.Delete, contentDescription = "Sil", tint = Slate500, modifier = Modifier.size(16.dp))
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
private fun MethodPill(label: String, amount: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    modifier = Modifier.padding(horizontal = 2.dp)
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(icon, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(14.dp))
      Spacer(modifier = Modifier.width(4.dp))
      Column {
        Text(label, fontSize = 10.sp, color = Slate500)
        Text(amount, fontSize = 11.sp, fontWeight = FontWeight.Bold)
      }
    }
  }
}
