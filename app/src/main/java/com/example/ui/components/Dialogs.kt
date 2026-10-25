package com.example.ui.components

import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.Customer
import com.example.model.CustomerWithBalance
import com.example.model.Debt
import com.example.model.DebtCategories
import com.example.model.PaymentMethod
import com.example.model.WhatsAppTemplate
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.RoseError
import com.example.ui.theme.Slate500
import com.example.util.Formatters
import com.example.util.WhatsAppHelper
import java.util.Calendar

@Composable
fun AddEditCustomerDialog(
  existingCustomer: Customer? = null,
  allCustomers: List<CustomerWithBalance>,
  onDismiss: () -> Unit,
  onSave: (name: String, phone: String, email: String, address: String, notes: String) -> Unit
) {
  var name by remember { mutableStateOf(existingCustomer?.name ?: "") }
  var phone by remember { mutableStateOf(existingCustomer?.phone ?: "") }
  var email by remember { mutableStateOf(existingCustomer?.email ?: "") }
  var address by remember { mutableStateOf(existingCustomer?.address ?: "") }
  var notes by remember { mutableStateOf(existingCustomer?.notes ?: "") }

  var nameError by remember { mutableStateOf<String?>(null) }
  var phoneError by remember { mutableStateOf<String?>(null) }
  var duplicateWarning by remember { mutableStateOf<String?>(null) }

  // Check duplicate phone live
  LaunchedEffect(phone) {
    val clean = Formatters.cleanPhoneNumber(phone)
    if (clean.length >= 10) {
      val found = allCustomers.find {
        it.customer.id != (existingCustomer?.id ?: 0L) &&
          Formatters.cleanPhoneNumber(it.customer.phone) == clean
      }
      duplicateWarning = if (found != null) {
        "Dikkat: Bu telefon numarası '${found.customer.name}' müşterisinde de kayıtlıdır."
      } else null
    } else {
      duplicateWarning = null
    }
  }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp)
          .verticalScroll(rememberScrollState())
      ) {
        Text(
          text = if (existingCustomer == null) "Yeni Müşteri Ekle" else "Müşteriyi Düzenle",
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Ad Soyad / İşletme Adı (Zorunlu)
        OutlinedTextField(
          value = name,
          onValueChange = {
            name = it
            if (it.isNotBlank()) nameError = null
          },
          label = { Text("Ad Soyad / İşletme Adı *") },
          isError = nameError != null,
          supportingText = nameError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_customer_name")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Telefon
        OutlinedTextField(
          value = phone,
          onValueChange = { phone = it },
          label = { Text("Telefon Numarası (05xx)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_customer_phone")
        )

        if (duplicateWarning != null) {
          Spacer(modifier = Modifier.height(6.dp))
          PhoneDuplicateWarningCard(message = duplicateWarning!!)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // E-posta
        OutlinedTextField(
          value = email,
          onValueChange = { email = it },
          label = { Text("E-posta (İsteğe bağlı)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Adres
        OutlinedTextField(
          value = address,
          onValueChange = { address = it },
          label = { Text("Adres / Konum") },
          maxLines = 2,
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Notlar
        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("Notlar (Örn: Cuma günleri öder)") },
          maxLines = 3,
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          TextButton(onClick = onDismiss) {
            Text("Vazgeç")
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = {
              if (name.isBlank()) {
                nameError = "Ad Soyad alanı zorunludur"
                return@Button
              }
              onSave(name, phone, email, address, notes)
            },
            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
            modifier = Modifier.testTag("btn_save_customer")
          ) {
            Text("Kaydet")
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDebtDialog(
  preselectedCustomerId: Long? = null,
  allCustomers: List<CustomerWithBalance>,
  onDismiss: () -> Unit,
  onSave: (customerId: Long, amount: Double, dueDate: Long?, description: String, category: String) -> Unit
) {
  val context = LocalContext.current
  var selectedCustomerId by remember {
    mutableStateOf(preselectedCustomerId ?: allCustomers.firstOrNull()?.customer?.id ?: 0L)
  }
  var amountText by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var selectedCategory by remember { mutableStateOf(DebtCategories.defaultCategories.first()) }
  var dueDate by remember { mutableStateOf<Long?>(null) }
  var amountError by remember { mutableStateOf<String?>(null) }
  var expandedCustomerDropdown by remember { mutableStateOf(false) }

  val calendar = Calendar.getInstance()
  val datePickerDialog = DatePickerDialog(
    context,
    { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
      calendar.set(year, month, dayOfMonth, 23, 59, 59)
      dueDate = calendar.timeInMillis
    },
    calendar.get(Calendar.YEAR),
    calendar.get(Calendar.MONTH),
    calendar.get(Calendar.DAY_OF_MONTH)
  )

  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp)
          .verticalScroll(rememberScrollState())
      ) {
        Text(
          text = "Yeni Borç Kaydı Ekle",
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Customer Selection
        if (preselectedCustomerId == null) {
          ExposedDropdownMenuBox(
            expanded = expandedCustomerDropdown,
            onExpandedChange = { expandedCustomerDropdown = it }
          ) {
            val selectedCustomerName = allCustomers.find { it.customer.id == selectedCustomerId }?.customer?.name ?: "Müşteri Seçin"
            OutlinedTextField(
              value = selectedCustomerName,
              onValueChange = {},
              readOnly = true,
              label = { Text("Müşteri Seçin *") },
              trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCustomerDropdown) },
              modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
            )
            ExposedDropdownMenu(
              expanded = expandedCustomerDropdown,
              onDismissRequest = { expandedCustomerDropdown = false }
            ) {
              allCustomers.forEach { item ->
                DropdownMenuItem(
                  text = { Text(item.customer.name) },
                  onClick = {
                    selectedCustomerId = item.customer.id
                    expandedCustomerDropdown = false
                  }
                )
              }
            }
          }
          Spacer(modifier = Modifier.height(12.dp))
        }

        // Tutar
        OutlinedTextField(
          value = amountText,
          onValueChange = {
            amountText = it
            if (it.toDoubleOrNull() != null) amountError = null
          },
          label = { Text("Borç Tutarı (₺) *") },
          placeholder = { Text("0,00") },
          isError = amountError != null,
          supportingText = amountError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_debt_amount")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Vade Tarihi
        Text(
          text = "Vade Tarihi",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedButton(
            onClick = { datePickerDialog.show() },
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (dueDate == null) "Tarih Seç" else Formatters.formatDate(dueDate))
          }
          if (dueDate != null) {
            IconButton(onClick = { dueDate = null }) {
              Icon(Icons.Default.Close, contentDescription = "Temizle", tint = Slate500)
            }
          }
        }

        // Quick Due date chips
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          val now = System.currentTimeMillis()
          item {
            FilterChip(
              selected = false,
              onClick = { dueDate = now + (3 * 86_400_000L) },
              label = { Text("+3 Gün", fontSize = 11.sp) }
            )
          }
          item {
            FilterChip(
              selected = false,
              onClick = { dueDate = now + (7 * 86_400_000L) },
              label = { Text("+1 Hafta", fontSize = 11.sp) }
            )
          }
          item {
            FilterChip(
              selected = false,
              onClick = { dueDate = now + (15 * 86_400_000L) },
              label = { Text("+15 Gün", fontSize = 11.sp) }
            )
          }
          item {
            FilterChip(
              selected = false,
              onClick = { dueDate = now + (30 * 86_400_000L) },
              label = { Text("+1 Ay", fontSize = 11.sp) }
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Kategori
        Text(
          text = "Kategori",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(DebtCategories.defaultCategories) { cat ->
            FilterChip(
              selected = selectedCategory == cat,
              onClick = { selectedCategory = cat },
              label = { Text(cat, fontSize = 12.sp) }
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Açıklama
        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Açıklama (Ürün, fatura no, vb.)") },
          maxLines = 2,
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          TextButton(onClick = onDismiss) {
            Text("Vazgeç")
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = {
              val amt = amountText.replace(",", ".").toDoubleOrNull()
              if (amt == null || amt <= 0) {
                amountError = "Geçerli bir tutar girin"
                return@Button
              }
              if (selectedCustomerId <= 0L) {
                return@Button
              }
              onSave(selectedCustomerId, amt, dueDate, description, selectedCategory)
            },
            colors = ButtonDefaults.buttonColors(containerColor = RoseError),
            modifier = Modifier.testTag("btn_save_debt")
          ) {
            Text("Borcu Kaydet")
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCollectionDialog(
  preselectedCustomerId: Long? = null,
  defaultAmount: Double? = null,
  allCustomers: List<CustomerWithBalance>,
  onDismiss: () -> Unit,
  onSave: (customerId: Long, amount: Double, method: PaymentMethod, notes: String) -> Unit
) {
  var selectedCustomerId by remember {
    mutableStateOf(preselectedCustomerId ?: allCustomers.firstOrNull()?.customer?.id ?: 0L)
  }
  var amountText by remember {
    mutableStateOf(if (defaultAmount != null && defaultAmount > 0) defaultAmount.toString() else "")
  }
  var selectedMethod by remember { mutableStateOf(PaymentMethod.CASH) }
  var notes by remember { mutableStateOf("") }
  var amountError by remember { mutableStateOf<String?>(null) }
  var expandedCustomerDropdown by remember { mutableStateOf(false) }

  val targetCustomer = allCustomers.find { it.customer.id == selectedCustomerId }
  val remainingBalance = targetCustomer?.remainingBalance ?: 0.0

  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp)
          .verticalScroll(rememberScrollState())
      ) {
        Text(
          text = "Tahsilat Al",
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Customer Selection
        if (preselectedCustomerId == null) {
          ExposedDropdownMenuBox(
            expanded = expandedCustomerDropdown,
            onExpandedChange = { expandedCustomerDropdown = it }
          ) {
            val selectedCustomerName = targetCustomer?.customer?.name ?: "Müşteri Seçin"
            OutlinedTextField(
              value = selectedCustomerName,
              onValueChange = {},
              readOnly = true,
              label = { Text("Müşteri Seçin *") },
              trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCustomerDropdown) },
              modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
            )
            ExposedDropdownMenu(
              expanded = expandedCustomerDropdown,
              onDismissRequest = { expandedCustomerDropdown = false }
            ) {
              allCustomers.forEach { item ->
                DropdownMenuItem(
                  text = {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                      Text(item.customer.name)
                      Text(
                        Formatters.formatCurrency(item.remainingBalance),
                        color = if (item.remainingBalance > 0) RoseError else Slate500,
                        fontWeight = FontWeight.SemiBold
                      )
                    }
                  },
                  onClick = {
                    selectedCustomerId = item.customer.id
                    expandedCustomerDropdown = false
                  }
                )
              }
            }
          }
          Spacer(modifier = Modifier.height(10.dp))
        }

        // Remaining balance info pill
        if (targetCustomer != null) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("Kalan Güncel Bakiye:", style = MaterialTheme.typography.bodySmall)
              Text(
                text = Formatters.formatCurrency(remainingBalance),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = if (remainingBalance > 0) RoseError else EmeraldPrimary
              )
            }
          }
          Spacer(modifier = Modifier.height(10.dp))
        }

        // Tutar Girişi
        OutlinedTextField(
          value = amountText,
          onValueChange = {
            amountText = it
            if (it.toDoubleOrNull() != null) amountError = null
          },
          label = { Text("Tahsil Edilen Tutar (₺) *") },
          placeholder = { Text("0,00") },
          isError = amountError != null,
          supportingText = amountError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_collection_amount")
        )

        // Hızlı Tutar Ekleme Çipleri (Esnaf Dostu Verimlilik Özelliği)
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          if (remainingBalance > 0) {
            item {
              FilterChip(
                selected = false,
                onClick = { amountText = String.format("%.2f", remainingBalance).replace(",", ".") },
                label = { Text("Tam Bakiye (${Formatters.formatCurrency(remainingBalance)})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
              )
            }
          }
          item {
            FilterChip(
              selected = false,
              onClick = {
                val cur = amountText.replace(",", ".").toDoubleOrNull() ?: 0.0
                amountText = String.format("%.2f", cur + 100).replace(",", ".")
              },
              label = { Text("+100 ₺", fontSize = 11.sp) }
            )
          }
          item {
            FilterChip(
              selected = false,
              onClick = {
                val cur = amountText.replace(",", ".").toDoubleOrNull() ?: 0.0
                amountText = String.format("%.2f", cur + 500).replace(",", ".")
              },
              label = { Text("+500 ₺", fontSize = 11.sp) }
            )
          }
          item {
            FilterChip(
              selected = false,
              onClick = {
                val cur = amountText.replace(",", ".").toDoubleOrNull() ?: 0.0
                amountText = String.format("%.2f", cur + 1000).replace(",", ".")
              },
              label = { Text("+1.000 ₺", fontSize = 11.sp) }
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Ödeme Yöntemi
        Text(
          text = "Ödeme Yöntemi",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(PaymentMethod.values()) { method ->
            FilterChip(
              selected = selectedMethod == method,
              onClick = { selectedMethod = method },
              label = { Text(method.label, fontSize = 12.sp) }
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Notlar
        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("Açıklama / Makbuz Notu") },
          maxLines = 2,
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          TextButton(onClick = onDismiss) {
            Text("Vazgeç")
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = {
              val amt = amountText.replace(",", ".").toDoubleOrNull()
              if (amt == null || amt <= 0) {
                amountError = "Geçerli bir tutar girin"
                return@Button
              }
              if (selectedCustomerId <= 0L) {
                return@Button
              }
              onSave(selectedCustomerId, amt, selectedMethod, notes)
            },
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
            modifier = Modifier.testTag("btn_save_collection")
          ) {
            Text("Tahsilatı Kaydet")
          }
        }
      }
    }
  }
}

@Composable
fun SendWhatsAppDialog(
  customerWithBalance: CustomerWithBalance,
  businessName: String,
  templates: List<WhatsAppTemplate>,
  onDismiss: () -> Unit,
  onOpenTemplates: () -> Unit
) {
  val context = LocalContext.current
  val customer = customerWithBalance.customer
  var selectedTemplate by remember { mutableStateOf(templates.firstOrNull()) }
  var customizedMessage by remember { mutableStateOf("") }
  var copyFeedback by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  // Re-generate message whenever template changes
  LaunchedEffect(selectedTemplate, templates) {
    if (selectedTemplate == null && templates.isNotEmpty()) {
      selectedTemplate = templates.first()
    }
    selectedTemplate?.let { t ->
      customizedMessage = WhatsAppHelper.buildMessage(
        templateText = t.templateText,
        customerName = customer.name,
        businessName = businessName,
        totalDebt = customerWithBalance.totalDebt,
        totalPaid = customerWithBalance.totalPaid,
        remainingBalance = customerWithBalance.remainingBalance,
        dueDate = customerWithBalance.lastTransactionDate,
        lastPaymentAmount = null
      )
    }
  }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp)
          .verticalScroll(rememberScrollState())
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "WhatsApp Mesajı Gönder",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(Color(0xFF25D366).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Send,
              contentDescription = null,
              tint = Color(0xFF25D366),
              modifier = Modifier.size(20.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Alıcı Bilgisi
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text(customer.name, fontWeight = FontWeight.Bold)
            Text(
              "Tel: ${Formatters.formatPhoneForDisplay(customer.phone)} • Kalan Bakiye: ${Formatters.formatCurrency(customerWithBalance.remainingBalance)}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
          text = "Şablon Seçin",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(templates) { t ->
            FilterChip(
              selected = selectedTemplate?.id == t.id,
              onClick = { selectedTemplate = t },
              label = { Text(t.title, fontSize = 11.sp) }
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text = "Mesaj Önizleme (Düzenleyebilirsiniz):",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
          value = customizedMessage,
          onValueChange = { customizedMessage = it },
          maxLines = 6,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_whatsapp_preview")
        )

        if (errorMessage != null) {
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = errorMessage!!,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
          )
        }

        if (copyFeedback) {
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "✓ Mesaj panoya kopyalandı!",
            color = EmeraldPrimary,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold
          )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedButton(
            onClick = {
              WhatsAppHelper.copyToClipboard(context, "MusteriCep Mesaj", customizedMessage)
              copyFeedback = true
            }
          ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Kopyala")
          }

          Button(
            onClick = {
              WhatsAppHelper.openWhatsAppChat(
                context = context,
                rawPhone = customer.phone,
                message = customizedMessage,
                onError = { err -> errorMessage = err }
              )
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
            modifier = Modifier.testTag("btn_send_whatsapp")
          ) {
            Text("WhatsApp'ta Aç", color = Color.White)
          }
        }
      }
    }
  }
}

@Composable
fun EditTemplateDialog(
  template: WhatsAppTemplate,
  onDismiss: () -> Unit,
  onSave: (WhatsAppTemplate) -> Unit
) {
  var title by remember { mutableStateOf(template.title) }
  var text by remember { mutableStateOf(template.templateText) }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp)
          .verticalScroll(rememberScrollState())
      ) {
        Text(
          text = "Şablonu Düzenle",
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Şablon Başlığı") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text = "Değişken Ekle (Dokununca eklenir):",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          val variables = listOf(
            "{musteri_adi}" to "Müşteri Adı",
            "{kalan_bakiye}" to "Kalan Bakiye",
            "{toplam_borc}" to "Toplam Borç",
            "{vade_tarihi}" to "Vade Tarihi",
            "{son_tutar}" to "Son Tahsilat",
            "{isletme_adi}" to "İşletme Adı"
          )
          items(variables) { (token, label) ->
            FilterChip(
              selected = false,
              onClick = { text = "$text $token" },
              label = { Text(label, fontSize = 11.sp) }
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
          value = text,
          onValueChange = { text = it },
          label = { Text("Mesaj Metni") },
          maxLines = 6,
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          TextButton(onClick = onDismiss) {
            Text("Vazgeç")
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = {
              onSave(template.copy(title = title.trim(), templateText = text.trim()))
            },
            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
          ) {
            Text("Şablonu Kaydet")
          }
        }
      }
    }
  }
}

@Composable
fun QuickCalculatorDialog(
  onDismiss: () -> Unit,
  onApplyAmount: (Double) -> Unit
) {
  var display by remember { mutableStateOf("0") }
  var currentOp by remember { mutableStateOf<String?>(null) }
  var firstOperand by remember { mutableDoubleStateOf(0.0) }
  var shouldResetOnNextDigit by remember { mutableStateOf(false) }

  fun onDigit(d: String) {
    if (display == "0" || shouldResetOnNextDigit) {
      display = d
      shouldResetOnNextDigit = false
    } else {
      display += d
    }
  }

  fun onOp(op: String) {
    firstOperand = display.replace(",", ".").toDoubleOrNull() ?: 0.0
    currentOp = op
    shouldResetOnNextDigit = true
  }

  fun onEquals() {
    val second = display.replace(",", ".").toDoubleOrNull() ?: 0.0
    val res = when (currentOp) {
      "+" -> firstOperand + second
      "-" -> firstOperand - second
      "*" -> firstOperand * second
      "/" -> if (second != 0.0) firstOperand / second else 0.0
      else -> second
    }
    display = String.format("%.2f", res).replace(",", ".")
    currentOp = null
    shouldResetOnNextDigit = true
  }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Hızlı Esnaf Hesap Makinesi",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
          )
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Kapat")
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Display
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier.fillMaxWidth()
        ) {
          Box(
            modifier = Modifier
              .padding(16.dp)
              .fillMaxWidth(),
            contentAlignment = Alignment.CenterEnd
          ) {
            Text(
              text = "$display ₺",
              style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Keypad
        val keys = listOf(
          listOf("C", "±", "%", "/"),
          listOf("7", "8", "9", "*"),
          listOf("4", "5", "6", "-"),
          listOf("1", "2", "3", "+"),
          listOf("0", "00", ".", "=")
        )

        keys.forEach { row ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            row.forEach { key ->
              val isOp = key in listOf("/", "*", "-", "+", "=")
              val isClear = key == "C"

              Surface(
                modifier = Modifier
                  .weight(1f)
                  .height(48.dp)
                  .clip(RoundedCornerShape(10.dp))
                  .clickable {
                    when (key) {
                      "C" -> {
                        display = "0"
                        currentOp = null
                        firstOperand = 0.0
                      }
                      "±" -> {
                        val num = display.toDoubleOrNull() ?: 0.0
                        display = (-num).toString()
                      }
                      "%" -> {
                        val num = display.toDoubleOrNull() ?: 0.0
                        display = (num / 100.0).toString()
                      }
                      "+", "-", "*", "/" -> onOp(key)
                      "=" -> onEquals()
                      "." -> if (!display.contains(".")) display += "."
                      else -> onDigit(key)
                    }
                  },
                color = when {
                  isClear -> RoseError.copy(alpha = 0.15f)
                  isOp -> NavyPrimary.copy(alpha = 0.15f)
                  else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                },
                shape = RoundedCornerShape(10.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Text(
                    text = key,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = when {
                      isClear -> RoseError
                      isOp -> NavyPrimary
                      else -> MaterialTheme.colorScheme.onSurface
                    }
                  )
                }
              }
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
          onClick = {
            val amount = display.replace(",", ".").toDoubleOrNull() ?: 0.0
            onApplyAmount(amount)
            onDismiss()
          },
          colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Tutarı Tahsilat/Borç Alanına Aktar")
        }
      }
    }
  }
}

@Composable
fun CsvImportPreviewDialog(
  validCustomers: List<Customer>,
  errors: List<String>,
  onDismiss: () -> Unit,
  onConfirm: () -> Unit
) {
  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp)
      ) {
        Text(
          text = "CSV Müşteri İçe Aktarım Önizleme",
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = "Geçerli Kayıt: ${validCustomers.size}",
            color = EmeraldPrimary,
            fontWeight = FontWeight.Bold
          )
          if (errors.isNotEmpty()) {
            Text(
              text = "Hatalı Satır: ${errors.size}",
              color = RoseError,
              fontWeight = FontWeight.Bold
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Errors section
        if (errors.isNotEmpty()) {
          Card(
            colors = CardDefaults.cardColors(containerColor = RoseError.copy(alpha = 0.1f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Text("Hatalı Satırlar:", fontWeight = FontWeight.Bold, color = RoseError, fontSize = 12.sp)
              errors.take(3).forEach { err ->
                Text("• $err", fontSize = 11.sp, color = RoseError)
              }
              if (errors.size > 3) {
                Text("...ve ${errors.size - 3} hata daha", fontSize = 11.sp, color = RoseError)
              }
            }
          }
          Spacer(modifier = Modifier.height(10.dp))
        }

        // Preview list
        Text("Aktarılacak Müşteriler (İlk 5):", style = MaterialTheme.typography.labelSmall)
        Spacer(modifier = Modifier.height(6.dp))
        LazyColumn(modifier = Modifier.heightIn(max = 160.dp)) {
          items(validCustomers.take(5)) { c ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(c.name, fontSize = 13.sp, fontWeight = FontWeight.Medium)
              Text(c.phone, fontSize = 12.sp, color = Slate500)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          TextButton(onClick = onDismiss) {
            Text("Vazgeç")
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = onConfirm,
            enabled = validCustomers.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
          ) {
            Text("İçe Aktarmayı Başlat")
          }
        }
      }
    }
  }
}

@Composable
fun TierLimitReachedDialog(
  currentCount: Int,
  maxLimit: Int,
  onDismiss: () -> Unit,
  onViewPlans: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Star, contentDescription = null, tint = AmberWarning)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Müşteri Limiti Doldu", fontWeight = FontWeight.Bold)
      }
    },
    text = {
      Text("Ücretsiz pakette en fazla $maxLimit müşteri kaydedebilirsiniz. Şu an $currentCount müşteri kayıtlıdır. Daha fazla müşteri eklemek için paketleri inceleyebilirsiniz.")
    },
    confirmButton = {
      Button(
        onClick = {
          onDismiss()
          onViewPlans()
        },
        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
      ) {
        Text("Paketleri İncele")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Anladım")
      }
    }
  )
}
