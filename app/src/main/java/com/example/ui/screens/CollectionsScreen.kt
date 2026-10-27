package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CollectionRecord
import com.example.model.PaymentMethod
import com.example.ui.components.AppCard
import com.example.ui.components.EmptyStateView
import com.example.ui.components.IconBadge
import com.example.ui.components.ScreenHeader
import com.example.ui.theme.LocalAppPalette
import com.example.ui.theme.moneyStyle
import com.example.util.Formatters
import com.example.util.Money
import com.example.viewmodel.DialogState
import com.example.viewmodel.MusteriCepViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionsScreen(
  viewModel: MusteriCepViewModel,
  modifier: Modifier = Modifier
) {
  val collections by viewModel.allCollections.collectAsState()
  val customers by viewModel.allCustomersWithBalance.collectAsState()
  val palette = LocalAppPalette.current
  var selectedMethod by remember { mutableStateOf<PaymentMethod?>(null) }
  var period by rememberSaveable { mutableStateOf(CollectionPeriod.MONTH) }

  val now = System.currentTimeMillis()
  val periodStart = when (period) {
    CollectionPeriod.TODAY -> Formatters.startOfDay(now)
    CollectionPeriod.WEEK -> Formatters.startOfWeek(now)
    CollectionPeriod.MONTH -> Formatters.startOfMonth(now)
    CollectionPeriod.ALL -> Long.MIN_VALUE
  }
  val names = remember(customers) { customers.associate { it.customer.id to it.customer.name } }
  val inPeriod = collections.filter { it.date >= periodStart }
  val filtered = if (selectedMethod == null) inPeriod else inPeriod.filter { it.paymentMethod == selectedMethod }
  val total = Money.sum(filtered.map { it.amount })
  fun sumOf(m: PaymentMethod) = Money.sum(inPeriod.filter { it.paymentMethod == m }.map { it.amount })

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      ScreenHeader(title = "Kasa", subtitle = "Müşterilerden alınan ödemeler") {
        FilledIconButton(
          onClick = { viewModel.openDialog(DialogState.AddCollection()) },
          colors = IconButtonDefaults.filledIconButtonColors(containerColor = palette.ink, contentColor = palette.onInk)
        ) {
          Icon(Icons.Default.Payments, contentDescription = "Tahsilat al", modifier = Modifier.size(20.dp))
        }
      }
    }

    item {
      AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              (if (selectedMethod != null) "${selectedMethod!!.label} • " else "") + period.title,
              style = MaterialTheme.typography.labelLarge,
              color = palette.muted,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(Formatters.formatCurrency(total), style = moneyStyle(28, FontWeight.Bold), color = palette.success, maxLines = 1)
            Text("${filtered.size} tahsilat", style = MaterialTheme.typography.bodySmall, color = palette.muted)
          }
        }
        Spacer(modifier = Modifier.height(12.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
          CollectionPeriod.entries.forEachIndexed { i, p ->
            SegmentedButton(
              selected = period == p,
              onClick = { period = p },
              shape = SegmentedButtonDefaults.itemShape(index = i, count = CollectionPeriod.entries.size),
              icon = {},
              colors = SegmentedButtonDefaults.colors(
                activeContainerColor = palette.accentSoft,
                activeContentColor = palette.ink,
                activeBorderColor = palette.cardBorder,
                inactiveBorderColor = palette.cardBorder
              )
            ) { Text(p.label, fontSize = 12.sp, maxLines = 1, softWrap = false) }
          }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
          MethodStat("Nakit", sumOf(PaymentMethod.CASH), Icons.Default.Payments, Modifier.weight(1f))
          MethodStat("Havale", sumOf(PaymentMethod.BANK_TRANSFER), Icons.Default.AccountBalance, Modifier.weight(1f))
          MethodStat("Kart", sumOf(PaymentMethod.CREDIT_CARD), Icons.Default.CreditCard, Modifier.weight(1f))
        }
      }
    }

    item {
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
          MethodChip("Tümü (${inPeriod.size})", selectedMethod == null) { selectedMethod = null }
        }
        items(PaymentMethod.entries.toList()) { m ->
          MethodChip("${m.label} (${inPeriod.count { it.paymentMethod == m }})", selectedMethod == m) { selectedMethod = m }
        }
      }
    }

    if (filtered.isEmpty()) {
      item {
        EmptyStateView(
          icon = Icons.Default.MonetizationOn,
          title = if (collections.isEmpty()) "Henüz tahsilat yok" else "Bu dönemde tahsilat yok",
          description = "Müşteriden ödeme aldığınızda buraya kaydedin.",
          actionButtonLabel = "Tahsilat al",
          onActionClick = { viewModel.openDialog(DialogState.AddCollection()) }
        )
      }
    } else {
      item {
        AppCard(padding = 0.dp) {
          filtered.forEachIndexed { i, coll ->
            if (i > 0) HorizontalDivider(color = palette.cardBorder, modifier = Modifier.padding(start = 64.dp))
            CollectionRow(
              coll = coll,
              name = names[coll.customerId] ?: "Bilinmeyen müşteri",
              onOpen = { if (names.containsKey(coll.customerId)) viewModel.selectCustomer(coll.customerId) },
              onEdit = { viewModel.editCollection(coll.id) },
              onDelete = {
                viewModel.openDialog(
                  DialogState.ConfirmDelete(
                    title = "Tahsilatı sil",
                    message = "${Formatters.formatCurrency(coll.amount)} tutarındaki tahsilat silinsin mi? Hemen ardından \"Geri Al\" ile geri getirebilirsiniz.",
                    onConfirm = { viewModel.deleteCollection(coll.id) }
                  )
                )
              }
            )
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(24.dp)) }
  }
}

@Composable
private fun MethodChip(label: String, selected: Boolean, onClick: () -> Unit) {
  val palette = LocalAppPalette.current
  FilterChip(
    selected = selected,
    onClick = onClick,
    label = { Text(label, maxLines = 1, softWrap = false) },
    colors = FilterChipDefaults.filterChipColors(
      selectedContainerColor = palette.accentSoft,
      selectedLabelColor = palette.ink,
      containerColor = MaterialTheme.colorScheme.surface
    ),
    border = FilterChipDefaults.filterChipBorder(
      enabled = true, selected = selected,
      borderColor = palette.cardBorder, selectedBorderColor = palette.ink.copy(alpha = 0.4f)
    )
  )
}

@Composable
private fun MethodStat(label: String, amount: Double, icon: ImageVector, modifier: Modifier) {
  val palette = LocalAppPalette.current
  Column(
    modifier = modifier
      .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
      .background(MaterialTheme.colorScheme.background)
      .padding(horizontal = 10.dp, vertical = 8.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(icon, contentDescription = null, tint = palette.inactive, modifier = Modifier.size(14.dp))
      Spacer(modifier = Modifier.width(4.dp))
      Text(label, fontSize = 11.sp, color = palette.muted, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
    }
    Spacer(modifier = Modifier.height(2.dp))
    Text(Formatters.formatCurrencyShort(amount), style = moneyStyle(13), maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
  }
}

@Composable
private fun CollectionRow(coll: CollectionRecord, name: String, onOpen: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
  val palette = LocalAppPalette.current
  var menu by remember { mutableStateOf(false) }
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onOpen)
      .padding(start = 14.dp, top = 10.dp, bottom = 10.dp)
      .testTag("collection_item_${coll.id}"),
    verticalAlignment = Alignment.CenterVertically
  ) {
    IconBadge(
      icon = when (coll.paymentMethod) {
        PaymentMethod.CASH -> Icons.Default.Payments
        PaymentMethod.BANK_TRANSFER -> Icons.Default.AccountBalance
        PaymentMethod.CREDIT_CARD -> Icons.Default.CreditCard
        else -> Icons.Default.MonetizationOn
      },
      tint = palette.success,
      background = palette.accentSoft,
      size = 36.dp,
      iconSize = 18.dp
    )
    Spacer(modifier = Modifier.width(12.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(name, fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
      Text(
        listOf(coll.paymentMethod.label, Formatters.formatDate(coll.date), coll.notes).filter { it.isNotBlank() }.joinToString(" • "),
        fontSize = 12.sp,
        color = palette.muted,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
    Spacer(modifier = Modifier.width(8.dp))
    Text("+" + Formatters.formatCurrency(coll.amount), style = moneyStyle(14), color = palette.success, maxLines = 1)
    Box {
      IconButton(onClick = { menu = true }, modifier = Modifier.size(40.dp)) {
        Icon(Icons.Default.MoreVert, contentDescription = "Seçenekler", tint = palette.inactive, modifier = Modifier.size(18.dp))
      }
      DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
        DropdownMenuItem(text = { Text("Düzenle") }, leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }, onClick = { menu = false; onEdit() })
        DropdownMenuItem(
          text = { Text("Sil", color = palette.danger) },
          leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = palette.danger) },
          onClick = { menu = false; onDelete() }
        )
      }
    }
  }
}

private enum class CollectionPeriod(val label: String, val title: String) {
  TODAY("Bugün", "Bugün alınan"),
  WEEK("Hafta", "Bu hafta alınan"),
  MONTH("Ay", "Bu ay alınan"),
  ALL("Tümü", "Toplam alınan")
}
