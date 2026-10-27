package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.SouthWest
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DebtStatus
import com.example.model.TransactionItem
import com.example.ui.components.AppCard
import com.example.ui.components.EmptyStateView
import com.example.ui.components.IconBadge
import com.example.ui.components.InitialsAvatar
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.LocalAppPalette
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.moneyStyle
import com.example.util.Formatters
import com.example.util.Money
import com.example.viewmodel.DialogState
import com.example.viewmodel.MusteriCepViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
  viewModel: MusteriCepViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val customerWithBalance by viewModel.selectedCustomerWithBalance.collectAsState()
  val transactions by viewModel.selectedCustomerTransactions.collectAsState()
  val palette = LocalAppPalette.current
  var showMenu by remember { mutableStateOf(false) }

  BackHandler { viewModel.navigateBack() }

  val item = customerWithBalance
  if (item == null) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Text("Müşteri bulunamadı.", color = palette.muted)
    }
    return
  }
  val customer = item.customer

  // Her hareketten sonraki bakiye (eskiden yeniye borç − tahsilat)
  val balanceAfter = remember(transactions) {
    var running = 0L
    val map = HashMap<String, Double>()
    transactions
      .sortedWith(compareBy<TransactionItem> { it.date }.thenBy { if (it is TransactionItem.DebtItem) 0 else 1 }.thenBy { it.id })
      .forEach { t ->
        running += if (t is TransactionItem.DebtItem) Money.toCents(t.amount) else -Money.toCents(t.amount)
        map[key(t)] = Money.fromCents(running)
      }
    map
  }

  fun dial() {
    if (customer.phone.isBlank()) return
    try {
      context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + customer.phone.filter { it.isDigit() || it == '+' })))
    } catch (e: Exception) {
      viewModel.showMessage("Arama uygulaması açılamadı.")
    }
  }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    topBar = {
      TopAppBar(
        title = { Text("Müşteri", style = MaterialTheme.typography.titleMedium) },
        navigationIcon = {
          IconButton(onClick = { viewModel.navigateBack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri") }
        },
        actions = {
          IconButton(onClick = { viewModel.openDialog(DialogState.AddEditCustomer(customer)) }) {
            Icon(Icons.Default.Edit, contentDescription = "Düzenle")
          }
          Box {
            IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, contentDescription = "Diğer") }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
              DropdownMenuItem(
                text = { Text(if (customer.isArchived) "Arşivden çıkar" else "Arşive kaldır") },
                leadingIcon = { Icon(if (customer.isArchived) Icons.Default.Unarchive else Icons.Default.Archive, contentDescription = null) },
                onClick = { showMenu = false; viewModel.toggleArchiveCustomer(customer) }
              )
              DropdownMenuItem(
                text = { Text("Müşteriyi sil", color = palette.danger) },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = palette.danger) },
                onClick = {
                  showMenu = false
                  viewModel.openDialog(
                    DialogState.ConfirmDelete(
                      title = "Müşteriyi sil",
                      message = "${customer.name} ve tüm borç/tahsilat kayıtları kalıcı olarak silinecek. Bu işlem geri alınamaz.",
                      onConfirm = { viewModel.deleteCustomer(customer) }
                    )
                  )
                }
              )
            }
          }
        },
        windowInsets = WindowInsets(0, 0, 0, 0),
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
      )
    }
  ) { innerPadding ->
    LazyColumn(
      modifier = modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      // 1. Kimlik
      item {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
          InitialsAvatar(customer.name, size = 52.dp)
          Spacer(modifier = Modifier.width(14.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(customer.name, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
              if (customer.phone.isNotBlank()) Formatters.formatPhoneForDisplay(customer.phone) else "Telefon girilmemiş",
              style = MaterialTheme.typography.bodyMedium,
              color = palette.muted,
              maxLines = 1
            )
          }
          if (customer.phone.isNotBlank()) {
            IconButton(onClick = { dial() }) { Icon(Icons.Default.Call, contentDescription = "Ara", tint = palette.ink) }
          }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
          when {
            customer.isArchived -> StatusPill("Arşivde", palette.muted, MaterialTheme.colorScheme.surfaceVariant)
            item.overdueAmount > 0 -> StatusPill("Gecikmiş ödeme var", palette.danger, palette.dangerSoft)
            item.remainingBalance > 0 -> StatusPill("Borçlu", palette.warning, palette.warningSoft)
            item.remainingBalance < 0 -> StatusPill("Avansı var", palette.ink, palette.accentSoft)
            else -> StatusPill("Borcu yok", palette.success, palette.accentSoft)
          }
          if (customer.isDemo) StatusPill("Örnek kayıt", palette.warning, palette.warningSoft)
        }
      }

      // 2. Finansal özet
      item {
        AppCard {
          Text("Kalan bakiye", style = MaterialTheme.typography.labelLarge, color = palette.muted)
          Text(
            Formatters.formatCurrency(item.remainingBalance),
            style = moneyStyle(30, FontWeight.Bold),
            color = when {
              item.overdueAmount > 0 -> palette.danger
              item.remainingBalance > 0 -> MaterialTheme.colorScheme.onSurface
              else -> palette.success
            },
            maxLines = 1
          )
          if (item.overdueAmount > 0) {
            Text("Gecikmiş: ${Formatters.formatCurrency(item.overdueAmount)}", style = MaterialTheme.typography.bodySmall, color = palette.danger, fontWeight = FontWeight.Medium)
          } else if (item.nextDueDate != null && item.remainingBalance > 0) {
            Text("En yakın vade: ${Formatters.formatDate(item.nextDueDate)} • ${Formatters.dueLabel(item.nextDueDate)}", style = MaterialTheme.typography.bodySmall, color = palette.warning)
          }
          Spacer(modifier = Modifier.height(12.dp))
          HorizontalDivider(color = palette.cardBorder)
          Spacer(modifier = Modifier.height(12.dp))
          Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
              Text("Toplam borç", fontSize = 12.sp, color = palette.muted)
              Text(Formatters.formatCurrency(item.totalDebt), style = moneyStyle(15), maxLines = 1)
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
              Text("Toplam tahsilat", fontSize = 12.sp, color = palette.muted)
              Text(Formatters.formatCurrency(item.totalPaid), style = moneyStyle(15), color = palette.success, maxLines = 1)
            }
          }
        }
      }

      // 3. İşlemler
      item {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
          ActionButton("Tahsilat al", Icons.Default.Payments, filled = true, modifier = Modifier.weight(1f).testTag("btn_detail_add_collection")) {
            viewModel.openDialog(
              DialogState.AddCollection(
                preselectedCustomerId = customer.id,
                defaultAmount = if (item.remainingBalance > 0) item.remainingBalance else null
              )
            )
          }
          ActionButton("Borç ekle", Icons.AutoMirrored.Filled.ReceiptLong, filled = false, modifier = Modifier.weight(1f).testTag("btn_detail_add_debt")) {
            viewModel.openDialog(DialogState.AddDebt(preselectedCustomerId = customer.id))
          }
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
          onClick = { viewModel.openDialog(DialogState.SendWhatsApp(item)) },
          shape = RoundedCornerShape(12.dp),
          contentPadding = PaddingValues(vertical = 12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("btn_whatsapp_message")
        ) {
          Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = WhatsAppGreen, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("WhatsApp mesajı", color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
        }
      }

      // 4. İletişim ve not (varsa)
      if (customer.email.isNotBlank() || customer.address.isNotBlank() || customer.notes.isNotBlank()) {
        item {
          AppCard(padding = 14.dp) {
            if (customer.email.isNotBlank()) InfoLine(Icons.Default.Email, customer.email)
            if (customer.address.isNotBlank()) InfoLine(Icons.Default.LocationOn, customer.address)
            if (customer.notes.isNotBlank()) {
              Text("Not", fontSize = 12.sp, color = palette.muted, modifier = Modifier.padding(top = 4.dp))
              Text(customer.notes, style = MaterialTheme.typography.bodyMedium)
            }
          }
        }
      }

      // 5. İşlem geçmişi
      item { SectionHeader("İşlem geçmişi (${transactions.size})") }

      if (transactions.isEmpty()) {
        item {
          EmptyStateView(
            icon = Icons.AutoMirrored.Filled.ReceiptLong,
            title = "Henüz işlem yok",
            description = "Bu müşteriye ait borç veya tahsilat kaydı bulunmuyor.",
            actionButtonLabel = "Borç ekle",
            onActionClick = { viewModel.openDialog(DialogState.AddDebt(preselectedCustomerId = customer.id)) }
          )
        }
      } else {
        item {
          AppCard(padding = 0.dp) {
            transactions.forEachIndexed { i, t ->
              if (i > 0) HorizontalDivider(color = palette.cardBorder, modifier = Modifier.padding(start = 64.dp))
              TransactionRow(
                t = t,
                balance = balanceAfter[key(t)],
                onEdit = { if (t is TransactionItem.DebtItem) viewModel.editDebt(t.id) else viewModel.editCollection(t.id) },
                onDelete = {
                  val isDebt = t is TransactionItem.DebtItem
                  viewModel.openDialog(
                    DialogState.ConfirmDelete(
                      title = if (isDebt) "Borç kaydını sil" else "Tahsilatı sil",
                      message = "${Formatters.formatCurrency(t.amount)} tutarındaki kayıt silinsin mi? Hemen ardından \"Geri Al\" ile geri getirebilirsiniz.",
                      onConfirm = { if (isDebt) viewModel.deleteDebt(t.id) else viewModel.deleteCollection(t.id) }
                    )
                  )
                }
              )
            }
          }
        }
      }
    }
  }
}

private fun key(t: TransactionItem) = (if (t is TransactionItem.DebtItem) "d" else "c") + t.id

@Composable
private fun ActionButton(label: String, icon: ImageVector, filled: Boolean, modifier: Modifier, onClick: () -> Unit) {
  val palette = LocalAppPalette.current
  if (filled) {
    Button(
      onClick = onClick,
      shape = RoundedCornerShape(12.dp),
      contentPadding = PaddingValues(vertical = 12.dp, horizontal = 8.dp),
      colors = ButtonDefaults.buttonColors(containerColor = palette.ink, contentColor = palette.onInk),
      modifier = modifier
    ) {
      Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(6.dp))
      Text(label, maxLines = 1, softWrap = false, fontWeight = FontWeight.SemiBold)
    }
  } else {
    OutlinedButton(
      onClick = onClick,
      shape = RoundedCornerShape(12.dp),
      contentPadding = PaddingValues(vertical = 12.dp, horizontal = 8.dp),
      modifier = modifier
    ) {
      Icon(icon, contentDescription = null, tint = palette.danger, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(6.dp))
      Text(label, maxLines = 1, softWrap = false, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
    }
  }
}

@Composable
private fun InfoLine(icon: ImageVector, text: String) {
  Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
    Icon(icon, contentDescription = null, tint = LocalAppPalette.current.inactive, modifier = Modifier.size(16.dp))
    Spacer(modifier = Modifier.width(8.dp))
    Text(text, style = MaterialTheme.typography.bodyMedium)
  }
}

@Composable
private fun TransactionRow(t: TransactionItem, balance: Double?, onEdit: () -> Unit, onDelete: () -> Unit) {
  val palette = LocalAppPalette.current
  val isDebt = t is TransactionItem.DebtItem
  var menu by remember { mutableStateOf(false) }
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(start = 14.dp, end = 0.dp, top = 12.dp, bottom = 12.dp),
    verticalAlignment = Alignment.Top
  ) {
    IconBadge(
      icon = if (isDebt) Icons.Default.NorthEast else Icons.Default.SouthWest,
      tint = if (isDebt) palette.danger else palette.success,
      background = if (isDebt) palette.dangerSoft else palette.accentSoft,
      size = 36.dp,
      iconSize = 18.dp
    )
    Spacer(modifier = Modifier.width(12.dp))
    Column(modifier = Modifier.weight(1f)) {
      val title = when (t) {
        is TransactionItem.DebtItem -> "Borç • ${t.category}"
        is TransactionItem.CollectionItem -> "Tahsilat • ${t.paymentMethod.label}"
      }
      Text(title, fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
      Text(Formatters.formatDate(t.date), fontSize = 12.sp, color = palette.muted)
      if (t.notesOrDesc.isNotBlank()) {
        Text(t.notesOrDesc, fontSize = 12.sp, color = palette.muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
      }
      if (t is TransactionItem.DebtItem) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
          when (t.status) {
            DebtStatus.PAID -> StatusPill("Ödendi", palette.success, palette.accentSoft)
            DebtStatus.PARTIALLY_PAID -> StatusPill("Kısmen ödendi", palette.warning, palette.warningSoft)
            DebtStatus.OVERDUE -> StatusPill("Gecikmiş", palette.danger, palette.dangerSoft)
            DebtStatus.UNPAID -> StatusPill("Ödenmedi", palette.muted, MaterialTheme.colorScheme.surfaceVariant)
          }
          if (t.dueDate != null && t.status != DebtStatus.PAID) {
            Text(
              "Vade ${Formatters.formatShortDate(t.dueDate)}",
              fontSize = 11.sp,
              color = if (t.status == DebtStatus.OVERDUE) palette.danger else palette.muted,
              maxLines = 1
            )
          }
        }
      }
    }
    Spacer(modifier = Modifier.width(8.dp))
    Column(horizontalAlignment = Alignment.End) {
      Text(
        (if (isDebt) "" else "+") + Formatters.formatCurrency(t.amount),
        style = moneyStyle(14),
        color = if (isDebt) MaterialTheme.colorScheme.onSurface else palette.success,
        maxLines = 1
      )
      if (t is TransactionItem.DebtItem && t.remainingAmount > 0 && t.remainingAmount < t.amount) {
        Text("Kalan ${Formatters.formatCurrencyShort(t.remainingAmount)}", fontSize = 11.sp, color = palette.warning, maxLines = 1)
      }
      if (balance != null) {
        Text("Bakiye ${Formatters.formatCurrencyShort(balance)}", fontSize = 11.sp, color = palette.muted, maxLines = 1)
      }
    }
    Box {
      IconButton(onClick = { menu = true }, modifier = Modifier.size(36.dp)) {
        Icon(Icons.Default.MoreVert, contentDescription = "İşlem seçenekleri", tint = palette.inactive, modifier = Modifier.size(18.dp))
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
