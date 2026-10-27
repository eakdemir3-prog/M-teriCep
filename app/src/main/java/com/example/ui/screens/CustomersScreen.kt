package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CustomerFilter
import com.example.model.CustomerSort
import com.example.model.CustomerWithBalance
import com.example.ui.components.AppCard
import com.example.ui.components.EmptyStateView
import com.example.ui.components.InitialsAvatar
import com.example.ui.components.ScreenHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.LocalAppPalette
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.moneyStyle
import com.example.util.Formatters
import com.example.viewmodel.DialogState
import com.example.viewmodel.MusteriCepViewModel

@Composable
fun CustomersScreen(
  viewModel: MusteriCepViewModel,
  modifier: Modifier = Modifier
) {
  val customers by viewModel.filteredCustomers.collectAsState()
  val all by viewModel.allCustomersWithBalance.collectAsState()
  val currentFilter by viewModel.customerFilter.collectAsState()
  val currentSort by viewModel.customerSort.collectAsState()
  val searchQuery by viewModel.searchQuery.collectAsState()
  val palette = LocalAppPalette.current
  var showSortMenu by remember { mutableStateOf(false) }
  val activeCount = all.count { !it.customer.isArchived }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    item {
      ScreenHeader(title = "Müşteriler", subtitle = "$activeCount kayıtlı müşteri") {
        Box {
          IconButton(onClick = { showSortMenu = true }, modifier = Modifier.testTag("btn_customer_sort")) {
            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sırala", tint = MaterialTheme.colorScheme.onBackground)
          }
          DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
            listOf(
              CustomerSort.NAME_ASC to "İsme göre (A–Z)",
              CustomerSort.NAME_DESC to "İsme göre (Z–A)",
              CustomerSort.DEBT_DESC to "En çok borcu olan",
              CustomerSort.RECENT_ACTIVITY to "Son işleme göre"
            ).forEach { (sort, label) ->
              DropdownMenuItem(
                text = { Text(label) },
                trailingIcon = { if (currentSort == sort) Icon(Icons.Default.Check, contentDescription = null, tint = palette.ink) },
                onClick = { viewModel.customerSort.value = sort; showSortMenu = false }
              )
            }
          }
        }
        FilledIconButton(
          onClick = { viewModel.openDialog(DialogState.AddEditCustomer()) },
          colors = IconButtonDefaults.filledIconButtonColors(containerColor = palette.ink, contentColor = palette.onInk),
          modifier = Modifier.testTag("fab_add_customer")
        ) {
          Icon(Icons.Default.PersonAdd, contentDescription = "Müşteri ekle", modifier = Modifier.size(20.dp))
        }
      }
    }

    item {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { viewModel.searchQuery.value = it },
        placeholder = { Text("İsim, telefon veya not ara") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = palette.inactive) },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(onClick = { viewModel.searchQuery.value = "" }) { Icon(Icons.Default.Clear, contentDescription = "Temizle") }
          }
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
          unfocusedContainerColor = MaterialTheme.colorScheme.surface,
          focusedContainerColor = MaterialTheme.colorScheme.surface,
          unfocusedBorderColor = palette.cardBorder,
          focusedBorderColor = palette.ink
        ),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("input_search_customer")
      )
    }

    item {
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val filters = listOf(
          CustomerFilter.ALL to "Tümü",
          CustomerFilter.WITH_DEBT to "Borcu olanlar",
          CustomerFilter.OVERDUE to "Gecikmiş",
          CustomerFilter.ZERO_BALANCE to "Borcu olmayanlar",
          CustomerFilter.ARCHIVED to "Arşiv"
        )
        items(filters) { (f, label) ->
          FilterChip(
            selected = currentFilter == f,
            onClick = { viewModel.customerFilter.value = f },
            label = { Text(label, maxLines = 1, softWrap = false) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = palette.accentSoft,
              selectedLabelColor = palette.ink,
              containerColor = MaterialTheme.colorScheme.surface
            ),
            border = FilterChipDefaults.filterChipBorder(
              enabled = true,
              selected = currentFilter == f,
              borderColor = palette.cardBorder,
              selectedBorderColor = palette.ink.copy(alpha = 0.4f)
            )
          )
        }
      }
    }

    if (customers.isEmpty()) {
      item {
        EmptyStateView(
          icon = Icons.Default.People,
          title = if (searchQuery.isNotBlank()) "Sonuç bulunamadı" else if (all.isEmpty()) "Henüz müşteri yok" else "Bu filtrede müşteri yok",
          description = if (searchQuery.isNotBlank()) "Farklı bir isim veya numara deneyin." else "Müşteri ekleyerek borç ve tahsilatları takip etmeye başlayın.",
          actionButtonLabel = if (all.isEmpty()) "Müşteri ekle" else null,
          onActionClick = if (all.isEmpty()) ({ viewModel.openDialog(DialogState.AddEditCustomer()) }) else null
        )
      }
    } else {
      item {
        AppCard(padding = 0.dp) {
          customers.forEachIndexed { i, item ->
            if (i > 0) HorizontalDivider(color = palette.cardBorder, modifier = Modifier.padding(start = 68.dp))
            CustomerRow(
              item = item,
              onClick = { viewModel.selectCustomer(item.customer.id) },
              onWhatsApp = { viewModel.openDialog(DialogState.SendWhatsApp(item)) }
            )
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(24.dp)) }
  }
}

@Composable
private fun CustomerRow(item: CustomerWithBalance, onClick: () -> Unit, onWhatsApp: () -> Unit) {
  val palette = LocalAppPalette.current
  val c = item.customer
  val (statusText, statusColor, statusBg) = when {
    c.isArchived -> Triple("Arşivde", palette.muted, MaterialTheme.colorScheme.surfaceVariant)
    item.overdueAmount > 0 -> Triple("Gecikmiş", palette.danger, palette.dangerSoft)
    item.remainingBalance > 0 -> Triple("Borçlu", palette.warning, palette.warningSoft)
    item.remainingBalance < 0 -> Triple("Avans", palette.ink, palette.accentSoft)
    else -> Triple("Borcu yok", palette.success, palette.accentSoft)
  }
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp)
      .testTag("customer_card_${c.id}"),
    verticalAlignment = Alignment.CenterVertically
  ) {
    InitialsAvatar(c.name, size = 40.dp)
    Spacer(modifier = Modifier.width(12.dp))
    Column(modifier = Modifier.weight(1f)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          c.name,
          fontWeight = FontWeight.SemiBold,
          fontSize = 15.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f, fill = false)
        )
        if (c.isDemo) {
          Spacer(modifier = Modifier.width(6.dp))
          StatusPill("Örnek", palette.warning, palette.warningSoft)
        }
      }
      Text(
        listOfNotNull(
          Formatters.formatPhoneForDisplay(c.phone).takeIf { c.phone.isNotBlank() },
          item.lastTransactionDate?.let { "Son işlem ${Formatters.formatShortDate(it)}" }
        ).joinToString(" • ").ifBlank { "Telefon yok" },
        style = MaterialTheme.typography.bodySmall,
        color = palette.muted,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
    Spacer(modifier = Modifier.width(8.dp))
    Column(horizontalAlignment = Alignment.End) {
      Text(
        Formatters.formatCurrency(item.remainingBalance),
        style = moneyStyle(14),
        color = when {
          item.overdueAmount > 0 -> palette.danger
          item.remainingBalance > 0 -> MaterialTheme.colorScheme.onSurface
          else -> palette.muted
        },
        maxLines = 1
      )
      Spacer(modifier = Modifier.height(3.dp))
      StatusPill(statusText, statusColor, statusBg)
    }
    if (c.phone.isNotBlank() && item.remainingBalance > 0) {
      IconButton(onClick = onWhatsApp, modifier = Modifier.size(40.dp)) {
        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "WhatsApp ile hatırlat", tint = WhatsAppGreen, modifier = Modifier.size(20.dp))
      }
    } else {
      Spacer(modifier = Modifier.width(12.dp))
    }
  }
}
