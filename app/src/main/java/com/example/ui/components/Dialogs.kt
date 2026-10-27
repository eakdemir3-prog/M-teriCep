package com.example.ui.components

import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import java.util.Locale
import com.example.model.CollectionRecord
import com.example.ui.theme.moneyStyle
import com.example.util.Money

@Composable
fun AddEditCustomerDialog(
  existingCustomer: Customer? = null,
  allCustomers: List<CustomerWithBalance>,
  onDismiss: () -> Unit,
  onSave: (name: String, phone: String, email: String, address: String, notes: String) -> Unit,
  saving: Boolean = false
) {
  var name by remember { mutableStateOf(existingCustomer?.name ?: "") }
  var phone by remember { mutableStateOf(existingCustomer?.phone ?: "") }
  var email by remember { mutableStateOf(existingCustomer?.email ?: "") }
  var address by remember { mutableStateOf(existingCustomer?.address ?: "") }
  var notes by remember { mutableStateOf(existingCustomer?.notes ?: "") }

  var nameError by remember { mutableStateOf<String?>(null) }
  var phoneError by remember { mutableStateOf<String?>(null) }
  var emailError by remember { mutableStateOf<String?>(null) }
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
          text = if (existingCustomer == null) "Müşteri ekle" else "Müşteriyi düzenle",
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
          onValueChange = { phone = it; phoneError = null },
          label = { Text("Telefon (WhatsApp için)") },
          placeholder = { Text("0532 123 45 67") },
          isError = phoneError != null,
          supportingText = phoneError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
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
          onValueChange = { email = it; emailError = null },
          label = { Text("E-posta (isteğe bağlı)") },
          isError = emailError != null,
          supportingText = emailError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
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
                nameError = "Ad soyad veya işletme adı zorunludur"
                return@Button
              }
              val digits = phone.filter { it.isDigit() }
              if (phone.isNotBlank() && digits.length < 10) {
                phoneError = "Numara eksik görünüyor (en az 10 hane)"
                return@Button
              }
              if (!Formatters.isValidEmail(email)) {
                emailError = "E-posta adresi geçersiz"
                return@Button
              }
              onSave(name, phone, email, address, notes)
            },
            enabled = !saving,
            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
            modifier = Modifier.testTag("btn_save_customer")
          ) {
            Text(if (saving) "Kaydediliyor…" else "Kaydet", maxLines = 1, softWrap = false)
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerPicker(
  allCustomers: List<CustomerWithBalance>,
  selectedCustomerId: Long,
  onSelect: (Long) -> Unit,
  showBalance: Boolean = false
) {
  var expanded by remember { mutableStateOf(false) }
  val selected = allCustomers.find { it.customer.id == selectedCustomerId }
  var query by remember(selectedCustomerId) { mutableStateOf(selected?.customer?.name ?: "") }
  val options = allCustomers
    .filter { !it.customer.isArchived || it.customer.id == selectedCustomerId }
    .filter { query.isBlank() || query == selected?.customer?.name || it.customer.name.contains(query, ignoreCase = true) }
    .take(50)

  ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
    OutlinedTextField(
      value = query,
      onValueChange = {
        query = it
        expanded = true
      },
      label = { Text("Müşteri *") },
      placeholder = { Text("İsim yazarak arayın") },
      singleLine = true,
      isError = selectedCustomerId <= 0L && query.isNotBlank() && options.isEmpty(),
      trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
      modifier = Modifier
        .menuAnchor()
        .fillMaxWidth()
    )
    ExposedDropdownMenu(expanded = expanded && options.isNotEmpty(), onDismissRequest = { expanded = false }) {
      options.forEach { item ->
        DropdownMenuItem(
          text = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text(item.customer.name, modifier = Modifier.weight(1f), maxLines = 1)
              if (showBalance) {
                Text(
                  Formatters.formatCurrency(item.remainingBalance),
                  color = if (item.remainingBalance > 0) RoseError else Slate500,
                  fontWeight = FontWeight.SemiBold
                )
              }
            }
          },
          onClick = {
            onSelect(item.customer.id)
            query = item.customer.name
            expanded = false
          }
        )
      }
    }
  }
}

@Composable
fun DatePickField(
  label: String,
  value: Long?,
  onChange: (Long?) -> Unit,
  allowClear: Boolean,
  endOfDay: Boolean,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    OutlinedButton(
      onClick = {
        val cal = Calendar.getInstance().apply { if (value != null) timeInMillis = value }
        DatePickerDialog(
          context,
          { _: DatePicker, y: Int, m: Int, d: Int ->
            val picked = Calendar.getInstance().apply {
              if (value != null) timeInMillis = value
              set(Calendar.YEAR, y)
              set(Calendar.MONTH, m)
              set(Calendar.DAY_OF_MONTH, d)
              if (endOfDay) {
                set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59)
              }
            }
            onChange(picked.timeInMillis)
          },
          cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        ).show()
      },
      modifier = Modifier.weight(1f)
    ) {
      Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        if (value == null) label else "$label: ${Formatters.formatDate(value)}",
        maxLines = 1,
        softWrap = false,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        fontSize = 14.sp
      )
    }
    if (allowClear && value != null) {
      IconButton(onClick = { onChange(null) }) {
        Icon(Icons.Default.Close, contentDescription = "Temizle", tint = Slate500)
      }
    }
  }
}

private fun endOfDayPlus(days: Int): Long = Calendar.getInstance().apply {
  add(Calendar.DAY_OF_MONTH, days)
  set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 0)
}.timeInMillis

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddDebtDialog(
  preselectedCustomerId: Long? = null,
  existing: Debt? = null,
  allCustomers: List<CustomerWithBalance>,
  onDismiss: () -> Unit,
  onSave: (customerId: Long, amount: Double, date: Long, dueDate: Long?, description: String, category: String) -> Unit,
  saving: Boolean = false
) {
  val isEdit = existing != null
  var selectedCustomerId by remember { mutableLongStateOf(existing?.customerId ?: preselectedCustomerId ?: 0L) }
  var amountText by remember { mutableStateOf(existing?.let { Money.toInput(it.amount) } ?: "") }
  var description by remember { mutableStateOf(existing?.description ?: "") }
  var selectedCategory by remember { mutableStateOf(existing?.category ?: DebtCategories.defaultCategories.first()) }
  var date by remember { mutableLongStateOf(existing?.date ?: System.currentTimeMillis()) }
  var dueDate by remember { mutableStateOf(existing?.dueDate) }
  var amountError by remember { mutableStateOf<String?>(null) }
  var customerError by remember { mutableStateOf<String?>(null) }
  val categories = remember(selectedCategory) {
    if (selectedCategory in DebtCategories.defaultCategories) DebtCategories.defaultCategories
    else DebtCategories.defaultCategories + selectedCategory
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
          text = if (isEdit) "Borcu düzenle" else "Borç ekle",
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (preselectedCustomerId == null && !isEdit) {
          CustomerPicker(
            allCustomers = allCustomers,
            selectedCustomerId = selectedCustomerId,
            onSelect = { selectedCustomerId = it; customerError = null }
          )
          customerError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
          }
          Spacer(modifier = Modifier.height(12.dp))
        } else {
          val name = allCustomers.find { it.customer.id == selectedCustomerId }?.customer?.name
          if (name != null) {
            Text(name, fontWeight = FontWeight.SemiBold, color = Slate500)
            Spacer(modifier = Modifier.height(10.dp))
          }
        }

        OutlinedTextField(
          value = amountText,
          onValueChange = {
            amountText = it
            if (Money.parse(it) != null) amountError = null
          },
          label = { Text("Borç Tutarı (₺) *") },
          placeholder = { Text("Örn: 1.250,50") },
          isError = amountError != null,
          supportingText = {
            val parsed = Money.parse(amountText)
            when {
              amountError != null -> Text(amountError!!, color = MaterialTheme.colorScheme.error)
              parsed != null -> Text(Formatters.formatCurrency(parsed))
            }
          },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          textStyle = moneyStyle(20),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_debt_amount")
        )

        Spacer(modifier = Modifier.height(6.dp))
        DatePickField(label = "Tarih", value = date, onChange = { if (it != null) date = it }, allowClear = false, endOfDay = false)

        Spacer(modifier = Modifier.height(10.dp))
        Text("Vade Tarihi", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(6.dp))
        DatePickField(label = "Vade", value = dueDate, onChange = { dueDate = it }, allowClear = true, endOfDay = true)
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf(3 to "+3 gün", 7 to "+1 hafta", 15 to "+15 gün", 30 to "+1 ay").forEach { (days, label) ->
            FilterChip(selected = false, onClick = { dueDate = endOfDayPlus(days) }, label = { Text(label, fontSize = 12.sp) })
          }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text("Kategori", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          categories.forEach { cat ->
            FilterChip(
              selected = selectedCategory == cat,
              onClick = { selectedCategory = cat },
              label = { Text(cat, fontSize = 12.sp) }
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Açıklama (ürün, fatura no...)") },
          maxLines = 3,
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
          TextButton(onClick = onDismiss) { Text("Vazgeç", maxLines = 1, softWrap = false) }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = {
              val amt = Money.parse(amountText)
              if (amt == null) {
                amountError = "Geçerli bir tutar girin (örn: 1.250,50)"
                return@Button
              }
              if (selectedCustomerId <= 0L) {
                customerError = "Listeden bir müşteri seçin"
                return@Button
              }
              onSave(selectedCustomerId, amt, date, dueDate, description, selectedCategory)
            },
            enabled = !saving,
            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
            modifier = Modifier.testTag("btn_save_debt")
          ) {
            Text(if (saving) "Kaydediliyor…" else if (isEdit) "Güncelle" else "Kaydet", maxLines = 1, softWrap = false)
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddCollectionDialog(
  preselectedCustomerId: Long? = null,
  defaultAmount: Double? = null,
  existing: CollectionRecord? = null,
  allCustomers: List<CustomerWithBalance>,
  onDismiss: () -> Unit,
  onSave: (customerId: Long, amount: Double, date: Long, method: PaymentMethod, notes: String) -> Unit,
  saving: Boolean = false
) {
  val isEdit = existing != null
  var selectedCustomerId by remember { mutableLongStateOf(existing?.customerId ?: preselectedCustomerId ?: 0L) }
  var amountText by remember {
    mutableStateOf(
      when {
        existing != null -> Money.toInput(existing.amount)
        defaultAmount != null && defaultAmount > 0 -> Money.toInput(defaultAmount)
        else -> ""
      }
    )
  }
  var selectedMethod by remember { mutableStateOf(existing?.paymentMethod ?: PaymentMethod.CASH) }
  var notes by remember { mutableStateOf(existing?.notes ?: "") }
  var date by remember { mutableLongStateOf(existing?.date ?: System.currentTimeMillis()) }
  var amountError by remember { mutableStateOf<String?>(null) }
  var customerError by remember { mutableStateOf<String?>(null) }

  val targetCustomer = allCustomers.find { it.customer.id == selectedCustomerId }
  val remainingBalance = targetCustomer?.remainingBalance ?: 0.0

  fun addToAmount(delta: Double) {
    val cur = Money.parse(amountText) ?: 0.0
    amountText = Money.toInput(cur + delta)
    amountError = null
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
          text = if (isEdit) "Tahsilatı düzenle" else "Tahsilat al",
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (preselectedCustomerId == null && !isEdit) {
          CustomerPicker(
            allCustomers = allCustomers,
            selectedCustomerId = selectedCustomerId,
            onSelect = { selectedCustomerId = it; customerError = null },
            showBalance = true
          )
          customerError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
          }
          Spacer(modifier = Modifier.height(10.dp))
        } else if (targetCustomer != null) {
          Text(targetCustomer.customer.name, fontWeight = FontWeight.SemiBold, color = Slate500)
          Spacer(modifier = Modifier.height(8.dp))
        }

        if (targetCustomer != null && !isEdit) {
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
              Text("Kalan bakiye", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
              Text(
                text = Formatters.formatCurrency(remainingBalance),
                style = moneyStyle(16),
                color = if (remainingBalance > 0) RoseError else EmeraldPrimary
              )
            }
          }
          Spacer(modifier = Modifier.height(10.dp))
        }

        OutlinedTextField(
          value = amountText,
          onValueChange = {
            amountText = it
            if (Money.parse(it) != null) amountError = null
          },
          label = { Text("Alınan Tutar (₺) *") },
          placeholder = { Text("Örn: 500") },
          isError = amountError != null,
          supportingText = {
            val parsed = Money.parse(amountText)
            when {
              amountError != null -> Text(amountError!!, color = MaterialTheme.colorScheme.error)
              parsed != null && !isEdit && targetCustomer != null -> {
                val after = remainingBalance - parsed
                Text(
                  if (after >= -0.004) "İşlem sonrası kalan: ${Formatters.formatCurrency(after)}"
                  else "Fazla ödeme: ${Formatters.formatCurrency(-after)} avans olarak kalır"
                )
              }
              parsed != null -> Text(Formatters.formatCurrency(parsed))
            }
          },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          textStyle = moneyStyle(20),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_collection_amount")
        )

        if (!isEdit) {
          FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (remainingBalance > 0) {
              FilterChip(
                selected = false,
                onClick = { amountText = Money.toInput(remainingBalance); amountError = null },
                label = { Text("Tamamı (${Formatters.formatCurrency(remainingBalance)})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
              )
            }
            listOf(100.0, 500.0, 1000.0).forEach { delta ->
              FilterChip(
                selected = false,
                onClick = { addToAmount(delta) },
                label = { Text("+" + Formatters.formatCurrency(delta).replace(",00", ""), fontSize = 12.sp) }
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))
        DatePickField(label = "Tarih", value = date, onChange = { if (it != null) date = it }, allowClear = false, endOfDay = false)

        Spacer(modifier = Modifier.height(10.dp))
        Text("Ödeme Yöntemi", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          PaymentMethod.entries.forEach { method ->
            FilterChip(
              selected = selectedMethod == method,
              onClick = { selectedMethod = method },
              label = { Text(method.label, fontSize = 12.sp) }
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("Not (makbuz no, banka...)") },
          maxLines = 2,
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
          TextButton(onClick = onDismiss) { Text("Vazgeç", maxLines = 1, softWrap = false) }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = {
              val amt = Money.parse(amountText)
              if (amt == null) {
                amountError = "Geçerli bir tutar girin (örn: 500 veya 1.250,50)"
                return@Button
              }
              if (selectedCustomerId <= 0L) {
                customerError = "Listeden bir müşteri seçin"
                return@Button
              }
              onSave(selectedCustomerId, amt, date, selectedMethod, notes)
            },
            enabled = !saving,
            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
            modifier = Modifier.testTag("btn_save_collection")
          ) {
            Text(if (saving) "Kaydediliyor…" else if (isEdit) "Güncelle" else "Kaydet", maxLines = 1, softWrap = false)
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
      customizedMessage = WhatsAppHelper.buildMessage(t.templateText, customerWithBalance, businessName)
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
            "{gecikmis_tutar}" to "Gecikmiş Tutar",
            "{vade_tarihi}" to "En Yakın Vade",
            "{son_tutar}" to "Son Ödeme",
            "{toplam_borc}" to "Toplam Borç",
            "{toplam_tahsilat}" to "Toplam Tahsilat",
            "{isletme_adi}" to "İşletme Adı",
            "{tarih}" to "Bugünün Tarihi"
          )
          items(variables) { (token, label) ->
            FilterChip(
              selected = false,
              onClick = { text = if (text.isBlank() || text.endsWith(" ")) text + token else "$text $token" },
              label = { Text(label, fontSize = 11.sp) }
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
          value = text,
          onValueChange = { text = it },
          label = { Text("Mesaj Metni") },
          minLines = 4,
          maxLines = 8,
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text("Önizleme (örnek müşteri ile):", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
          text = WhatsAppHelper.buildMessage(
            templateText = text, customerName = "Ahmet Yılmaz", businessName = "İşletmem",
            totalDebt = 6300.0, totalPaid = 2000.0, remainingBalance = 4300.0,
            dueDate = System.currentTimeMillis() + 3 * 86_400_000L, lastPaymentAmount = 2000.0, overdueAmount = 2500.0
          ),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
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
              onSave(template.copy(title = title.trim().ifBlank { template.title }, templateText = text.trim()))
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

  fun evaluate() {
    val second = display.replace(",", ".").toDoubleOrNull() ?: 0.0
    val res = when (currentOp) {
      "+" -> firstOperand + second
      "-" -> firstOperand - second
      "*" -> firstOperand * second
      "/" -> if (second != 0.0) firstOperand / second else 0.0
      else -> second
    }
    display = String.format(Locale.US, "%.2f", res).trimEnd('0').trimEnd('.')
    currentOp = null
    shouldResetOnNextDigit = true
  }

  fun onOp(op: String) {
    if (currentOp != null && !shouldResetOnNextDigit) evaluate()
    firstOperand = display.toDoubleOrNull() ?: 0.0
    currentOp = op
    shouldResetOnNextDigit = true
  }

  fun onEquals() = evaluate()

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
              text = (display.toDoubleOrNull()?.let { Formatters.formatCurrency(it) } ?: "$display ₺"),
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
                        display = String.format(Locale.US, "%.2f", -num).trimEnd('0').trimEnd('.')
                      }
                      "%" -> {
                        // "1000 + 10%" gibi: ilk sayının yüzdesi
                        val num = display.toDoubleOrNull() ?: 0.0
                        val base = if (currentOp != null) firstOperand else 1.0
                        display = String.format(Locale.US, "%.2f", base * num / 100.0).trimEnd('0').trimEnd('.')
                        shouldResetOnNextDigit = true
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
            val amount = Money.round(display.toDoubleOrNull() ?: 0.0)
            if (amount > 0) onApplyAmount(amount) else onDismiss()
          },
          colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Bu tutarla tahsilat al")
        }
      }
    }
  }
}

@Composable
fun CsvImportPreviewDialog(
  validCustomers: List<Customer>,
  errors: List<String>,
  skipped: List<String> = emptyList(),
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

        if (skipped.isNotEmpty()) {
          Text(
            text = "Zaten kayıtlı olduğu için atlanacak: ${skipped.size}",
            color = AmberWarning,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
          )
          skipped.take(3).forEach { Text("• $it", fontSize = 11.sp, color = Slate500) }
          Spacer(modifier = Modifier.height(10.dp))
        }

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
      Text("Mevcut paketinizde en fazla $maxLimit müşteri kaydedebilirsiniz (arşivdekiler dahil). Şu an $currentCount müşteri kayıtlı. Daha fazlası için paketleri inceleyebilirsiniz.")
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
