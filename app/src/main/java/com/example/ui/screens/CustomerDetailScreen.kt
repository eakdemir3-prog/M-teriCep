package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Debt
import com.example.model.CollectionRecord
import com.example.model.TransactionItem
import com.example.ui.components.BalanceStatusBadge
import com.example.ui.components.EmptyStateView
import com.example.ui.components.StatusBadge
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.RoseError
import com.example.ui.theme.Slate500
import com.example.util.Formatters
import com.example.ui.theme.moneyStyle
import com.example.ui.theme.AmberWarning
import com.example.model.DebtStatus
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
  val profile by viewModel.profile.collectAsState()

  var showMenu by remember { mutableStateOf(false) }

  // Handle Android back button
  BackHandler {
    viewModel.navigateBack()
  }

  if (customerWithBalance == null) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Text("Müşteri bulunamadı.")
    }
    return
  }

  val item = customerWithBalance!!
  val customer = item.customer

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = customer.name,
            maxLines = 1,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
          )
        },
        navigationIcon = {
          IconButton(onClick = { viewModel.navigateBack() }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
          }
        },
        actions = {
          IconButton(onClick = { showMenu = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "Seçenekler")
          }

          DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
          ) {
            DropdownMenuItem(
              text = { Text("Müşteriyi Düzenle") },
              leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
              onClick = {
                showMenu = false
                viewModel.openDialog(DialogState.AddEditCustomer(customer))
              }
            )
            DropdownMenuItem(
              text = { Text(if (customer.isArchived) "Arşivden Çıkar" else "Arşivle") },
              leadingIcon = {
                Icon(
                  if (customer.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                  contentDescription = null
                )
              },
              onClick = {
                showMenu = false
                viewModel.toggleArchiveCustomer(customer)
              }
            )
            DropdownMenuItem(
              text = { Text("Müşteriyi Sil", color = RoseError) },
              leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = RoseError) },
              onClick = {
                showMenu = false
                viewModel.openDialog(
                  DialogState.ConfirmDelete(
                    title = "Müşteriyi Sil",
                    message = "${customer.name} müşterisini ve müşteriye ait tüm borç/tahsilat kayıtlarını silmek istediğinizden emin misiniz? Bu işlem geri alınamaz.",
                    onConfirm = { viewModel.deleteCustomer(customer) }
                  )
                )
              }
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
      )
    }
  ) { innerPadding ->
    LazyColumn(
      modifier = modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // 1. Balance Summary Card
      item {
        Spacer(modifier = Modifier.height(4.dp))
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(
            containerColor = if (item.remainingBalance > 0) RoseError.copy(alpha = 0.08f)
            else EmeraldPrimary.copy(alpha = 0.08f)
          ),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Kalan Güncel Bakiye",
                style = MaterialTheme.typography.labelMedium,
                color = Slate500
              )
              BalanceStatusBadge(balance = item.remainingBalance, isArchived = customer.isArchived)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
              text = Formatters.formatCurrency(item.remainingBalance),
              style = moneyStyle(30),
              color = if (item.remainingBalance > 0) RoseError else EmeraldPrimary
            )
            if (item.overdueAmount > 0) {
              Text(
                text = "Vadesi geçen: ${Formatters.formatCurrency(item.overdueAmount)}",
                style = MaterialTheme.typography.bodySmall,
                color = RoseError,
                fontWeight = FontWeight.SemiBold
              )
            } else if (item.nextDueDate != null && item.remainingBalance > 0) {
              Text(
                text = "En yakın vade: ${Formatters.formatDate(item.nextDueDate)} (${Formatters.dueLabel(item.nextDueDate)})",
                style = MaterialTheme.typography.bodySmall,
                color = AmberWarning
              )
            }
            if (customer.isDemo) {
              Spacer(modifier = Modifier.height(6.dp))
              com.example.ui.components.DemoBadge()
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Column {
                Text("Toplam Borç", style = MaterialTheme.typography.bodySmall, color = Slate500)
                Text(
                  text = Formatters.formatCurrency(item.totalDebt),
                  style = moneyStyle(15),
                  color = MaterialTheme.colorScheme.onSurface
                )
              }

              Column(horizontalAlignment = Alignment.End) {
                Text("Toplam Tahsilat", style = MaterialTheme.typography.bodySmall, color = Slate500)
                Text(
                  text = Formatters.formatCurrency(item.totalPaid),
                  style = moneyStyle(15),
                  color = EmeraldPrimary
                )
              }
            }
          }
        }
      }

      // 2. Action Buttons (Tahsilat Al, Borç Ekle, WhatsApp)
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // Tahsilat Al (Yeşil)
          Button(
            onClick = {
              viewModel.openDialog(
                DialogState.AddCollection(
                  preselectedCustomerId = customer.id,
                  defaultAmount = if (item.remainingBalance > 0) item.remainingBalance else null
                )
              )
            },
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("btn_detail_add_collection")
          ) {
            Icon(Icons.Default.MonetizationOn, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Tahsilat Al", fontSize = 13.sp)
          }

          // Yeni Borç (Kırmızı)
          OutlinedButton(
            onClick = {
              viewModel.openDialog(DialogState.AddDebt(preselectedCustomerId = customer.id))
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("btn_detail_add_debt")
          ) {
            Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = RoseError, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Borç Ekle", color = RoseError, fontSize = 13.sp)
          }
        }
      }

      // 3. WhatsApp Mesaj Gönder Düğmesi (MANDATORY PROMPT REQUIREMENT)
      item {
        Button(
          onClick = {
            viewModel.openDialog(DialogState.SendWhatsApp(item))
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("btn_whatsapp_message")
        ) {
          Icon(Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("WhatsApp ile Mesaj Gönder", color = Color.White, fontWeight = FontWeight.Bold)
        }
      }

      // 4. Müşteri İletişim Bilgileri Kartı
      item {
        ElevatedCard(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text("İletişim & Notlar", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(10.dp))

            // Phone
            if (customer.phone.isNotBlank()) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable {
                    val dial = Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + customer.phone.filter { it.isDigit() || it == '+' }))
                    try { context.startActivity(dial) } catch (e: Exception) { viewModel.showMessage("Arama uygulaması açılamadı.") }
                  }
                  .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(Icons.Default.Call, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                  text = Formatters.formatPhoneForDisplay(customer.phone),
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                  color = NavyPrimary
                )
              }
            }

            // Email
            if (customer.email.isNotBlank()) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(Icons.Default.Email, contentDescription = null, tint = Slate500, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = customer.email, style = MaterialTheme.typography.bodyMedium)
              }
            }

            // Address
            if (customer.address.isNotBlank()) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Slate500, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = customer.address, style = MaterialTheme.typography.bodyMedium)
              }
            }

            // Notes
            if (customer.notes.isNotBlank()) {
              Spacer(modifier = Modifier.height(6.dp))
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(
                  text = "Not: ${customer.notes}",
                  style = MaterialTheme.typography.bodySmall,
                  modifier = Modifier.padding(10.dp)
                )
              }
            }
          }
        }
      }

      // 5. Tarih Sırasıyla Tüm Finansal Hareketler (Ledger Timeline)
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.History, contentDescription = null, tint = NavyPrimary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "İşlem Geçmişi (${transactions.size})",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
          }
        }
      }

      if (transactions.isEmpty()) {
        item {
          EmptyStateView(
            icon = Icons.Default.ReceiptLong,
            title = "Henüz işlem kaydı yok",
            description = "Bu müşteriye ait borç veya tahsilat kaydı bulunmamaktadır.",
            actionButtonLabel = "Borç Ekle",
            onActionClick = {
              viewModel.openDialog(DialogState.AddDebt(preselectedCustomerId = customer.id))
            }
          )
        }
      }

      items(transactions, key = { it.javaClass.simpleName + "_" + it.id }) { txn ->
        when (txn) {
          is TransactionItem.DebtItem -> {
            Card(
              shape = RoundedCornerShape(12.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.Top
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                      modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(RoseError.copy(alpha = 0.12f)),
                      contentAlignment = Alignment.Center
                    ) {
                      Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = RoseError, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                      Text(txn.category, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                      Text(Formatters.formatDate(txn.date), style = MaterialTheme.typography.bodySmall, color = Slate500)
                    }
                  }

                  Column(horizontalAlignment = Alignment.End) {
                    Text(
                      text = Formatters.formatCurrency(txn.amount),
                      style = moneyStyle(15),
                      color = RoseError
                    )
                    if (txn.status == DebtStatus.PARTIALLY_PAID || (txn.status == DebtStatus.OVERDUE && txn.remainingAmount < txn.amount)) {
                      Text(
                        text = "Kalan ${Formatters.formatCurrency(txn.remainingAmount)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate500
                      )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    StatusBadge(status = txn.status)
                  }
                }

                if (txn.notesOrDesc.isNotBlank()) {
                  Spacer(modifier = Modifier.height(6.dp))
                  Text(
                    text = txn.notesOrDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }

                if (txn.dueDate != null) {
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = "Vade: ${Formatters.formatDate(txn.dueDate)}" +
                      if (txn.status != DebtStatus.PAID) " • ${Formatters.dueLabel(txn.dueDate)}" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = when (txn.status) {
                      DebtStatus.OVERDUE -> RoseError
                      DebtStatus.PAID -> Slate500
                      else -> AmberWarning
                    }
                  )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                  IconButton(onClick = { viewModel.editDebt(txn.id) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Düzenle", tint = Slate500, modifier = Modifier.size(16.dp))
                  }
                  IconButton(
                    onClick = {
                      viewModel.openDialog(
                        DialogState.ConfirmDelete(
                          title = "Borç Kaydını Sil",
                          message = "${Formatters.formatCurrency(txn.amount)} tutarındaki borç kaydı silinsin mi? Hemen ardından \"Geri Al\" ile geri getirebilirsiniz.",
                          onConfirm = { viewModel.deleteDebt(txn.id) }
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

          is TransactionItem.CollectionItem -> {
            Card(
              shape = RoundedCornerShape(12.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.Top
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                      modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(EmeraldPrimary.copy(alpha = 0.12f)),
                      contentAlignment = Alignment.Center
                    ) {
                      Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                      Text("Tahsilat (${txn.paymentMethod.label})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                      Text(Formatters.formatDate(txn.date), style = MaterialTheme.typography.bodySmall, color = Slate500)
                    }
                  }

                  Text(
                    text = "+" + Formatters.formatCurrency(txn.amount),
                    style = moneyStyle(15),
                    color = EmeraldPrimary
                  )
                }

                if (txn.notesOrDesc.isNotBlank()) {
                  Spacer(modifier = Modifier.height(6.dp))
                  Text(
                    text = txn.notesOrDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                  IconButton(onClick = { viewModel.editCollection(txn.id) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Düzenle", tint = Slate500, modifier = Modifier.size(16.dp))
                  }
                  IconButton(
                    onClick = {
                      viewModel.openDialog(
                        DialogState.ConfirmDelete(
                          title = "Tahsilat Kaydını Sil",
                          message = "${Formatters.formatCurrency(txn.amount)} tutarındaki tahsilat silinsin mi? Hemen ardından \"Geri Al\" ile geri getirebilirsiniz.",
                          onConfirm = { viewModel.deleteCollection(txn.id) }
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
      }

      item {
        Spacer(modifier = Modifier.height(40.dp))
      }
    }
  }
}
