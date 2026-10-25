package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CustomerFilter
import com.example.model.CustomerSort
import com.example.ui.components.BalanceStatusBadge
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.RoseError
import com.example.ui.theme.Slate500
import com.example.util.Formatters
import com.example.viewmodel.DialogState
import com.example.viewmodel.MusteriCepViewModel

@Composable
fun CustomersScreen(
  viewModel: MusteriCepViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val customers by viewModel.filteredCustomers.collectAsState()
  val currentFilter by viewModel.customerFilter.collectAsState()
  val currentSort by viewModel.customerSort.collectAsState()
  val searchQuery by viewModel.searchQuery.collectAsState()

  var showSortMenu by remember { mutableStateOf(false) }

  Box(modifier = modifier.fillMaxSize()) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      item {
        Spacer(modifier = Modifier.height(8.dp))
        // Title & Sort Action
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "Müşteriler",
              style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "${customers.size} müşteri listeleniyor",
              style = MaterialTheme.typography.bodySmall,
              color = Slate500
            )
          }

          Box {
            IconButton(
              onClick = { showSortMenu = true },
              modifier = Modifier.testTag("btn_customer_sort")
            ) {
              Icon(Icons.Default.Sort, contentDescription = "Sırala", tint = NavyPrimary)
            }

            DropdownMenu(
              expanded = showSortMenu,
              onDismissRequest = { showSortMenu = false }
            ) {
              DropdownMenuItem(
                text = { Text("İsim (A - Z)") },
                onClick = {
                  viewModel.customerSort.value = CustomerSort.NAME_ASC
                  showSortMenu = false
                }
              )
              DropdownMenuItem(
                text = { Text("İsim (Z - A)") },
                onClick = {
                  viewModel.customerSort.value = CustomerSort.NAME_DESC
                  showSortMenu = false
                }
              )
              DropdownMenuItem(
                text = { Text("En Çok Borcu Olan") },
                onClick = {
                  viewModel.customerSort.value = CustomerSort.DEBT_DESC
                  showSortMenu = false
                }
              )
              DropdownMenuItem(
                text = { Text("Son İşlem Tarihi") },
                onClick = {
                  viewModel.customerSort.value = CustomerSort.RECENT_ACTIVITY
                  showSortMenu = false
                }
              )
            }
          }
        }
      }

      // Search Bar
      item {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { viewModel.searchQuery.value = it },
          placeholder = { Text("İsim, telefon veya not ara...") },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Slate500) },
          trailingIcon = {
            if (searchQuery.isNotEmpty()) {
              IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                Icon(Icons.Default.Clear, contentDescription = "Temizle")
              }
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_search_customer")
        )
      }

      // Filter Chips
      item {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          item {
            FilterChip(
              selected = currentFilter == CustomerFilter.ALL,
              onClick = { viewModel.customerFilter.value = CustomerFilter.ALL },
              label = { Text("Tümü") }
            )
          }
          item {
            FilterChip(
              selected = currentFilter == CustomerFilter.WITH_DEBT,
              onClick = { viewModel.customerFilter.value = CustomerFilter.WITH_DEBT },
              label = { Text("Borçlular") }
            )
          }
          item {
            FilterChip(
              selected = currentFilter == CustomerFilter.ZERO_BALANCE,
              onClick = { viewModel.customerFilter.value = CustomerFilter.ZERO_BALANCE },
              label = { Text("Bakiyesi Sıfır") }
            )
          }
          item {
            FilterChip(
              selected = currentFilter == CustomerFilter.ARCHIVED,
              onClick = { viewModel.customerFilter.value = CustomerFilter.ARCHIVED },
              label = { Text("Arşivlenenler") }
            )
          }
        }
      }

      // Empty State
      if (customers.isEmpty()) {
        item {
          EmptyStateView(
            icon = Icons.Default.Search,
            title = "Aramanıza uygun müşteri bulunamadı",
            description = "Farklı bir isim ya da filtre deneyebilir veya yeni bir müşteri ekleyebilirsiniz.",
            actionButtonLabel = "Yeni Müşteri Ekle",
            onActionClick = { viewModel.openDialog(DialogState.AddEditCustomer()) }
          )
        }
      }

      // Customer Cards
      items(customers, key = { it.customer.id }) { item ->
        val customer = item.customer
        ElevatedCard(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { viewModel.selectCustomer(customer.id) }
            .testTag("customer_card_${customer.id}")
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.Top
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                  modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                      if (item.remainingBalance > 0) RoseError.copy(alpha = 0.12f)
                      else EmeraldPrimary.copy(alpha = 0.12f)
                    ),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = customer.name.take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    color = if (item.remainingBalance > 0) RoseError else EmeraldPrimary,
                    fontSize = 18.sp
                  )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                  Text(
                    text = customer.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Text(
                    text = Formatters.formatPhoneForDisplay(customer.phone),
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate500
                  )
                }
              }

              BalanceStatusBadge(balance = item.remainingBalance, isArchived = customer.isArchived)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Financial Summary Row
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column {
                  Text("Kalan Bakiye", style = MaterialTheme.typography.labelSmall, color = Slate500)
                  Text(
                    text = Formatters.formatCurrency(item.remainingBalance),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (item.remainingBalance > 0) RoseError else EmeraldPrimary
                  )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  // Direct Dial Button
                  if (customer.phone.isNotBlank()) {
                    IconButton(
                      onClick = {
                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${customer.phone}"))
                        context.startActivity(dialIntent)
                      },
                      modifier = Modifier.size(36.dp)
                    ) {
                      Icon(Icons.Default.Call, contentDescription = "Ara", tint = NavyPrimary, modifier = Modifier.size(18.dp))
                    }
                  }

                  // Direct WhatsApp Button
                  if (customer.phone.isNotBlank()) {
                    IconButton(
                      onClick = { viewModel.openDialog(DialogState.SendWhatsApp(item)) },
                      modifier = Modifier.size(36.dp)
                    ) {
                      Icon(Icons.Default.Send, contentDescription = "WhatsApp", tint = Color(0xFF25D366), modifier = Modifier.size(18.dp))
                    }
                  }

                  // Quick Collection Button
                  IconButton(
                    onClick = {
                      viewModel.openDialog(
                        DialogState.AddCollection(
                          preselectedCustomerId = customer.id,
                          defaultAmount = if (item.remainingBalance > 0) item.remainingBalance else null
                        )
                      )
                    },
                    modifier = Modifier.size(36.dp)
                  ) {
                    Icon(Icons.Default.MonetizationOn, contentDescription = "Tahsilat Al", tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                  }
                }
              }
            }
          }
        }
      }

      // Bottom padding for bottom navigation bar
      item {
        Spacer(modifier = Modifier.height(84.dp))
      }
    }

    // Floating add customer button
    FloatingActionButton(
      onClick = { viewModel.openDialog(DialogState.AddEditCustomer()) },
      containerColor = NavyPrimary,
      contentColor = Color.White,
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(end = 20.dp, bottom = 80.dp)
        .testTag("fab_add_customer")
    ) {
      Icon(Icons.Default.PersonAdd, contentDescription = "Yeni Müşteri Ekle")
    }
  }
}
