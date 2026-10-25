package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.MusteriCepRepository
import com.example.model.CollectionRecord
import com.example.model.Customer
import com.example.model.CustomerFilter
import com.example.model.CustomerSort
import com.example.model.CustomerWithBalance
import com.example.model.Debt
import com.example.model.PaymentMethod
import com.example.model.SubscriptionTier
import com.example.model.TransactionItem
import com.example.model.UserBusinessProfile
import com.example.model.WhatsAppTemplate
import com.example.util.CsvHelper
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class MusteriCepViewModel(application: Application) : AndroidViewModel(application) {

  private val repository: MusteriCepRepository
  init {
    val database = AppDatabase.getDatabase(application, viewModelScope)
    repository = MusteriCepRepository(
      database.customerDao(),
      database.debtDao(),
      database.collectionDao(),
      database.templateDao()
    )
    viewModelScope.launch {
      repository.ensureDefaultTemplates()
    }
  }

  // Navigation Stack
  private val _screenStack = MutableStateFlow<List<Screen>>(listOf(Screen.Dashboard))
  val currentScreen: StateFlow<Screen> = _screenStack.combine(flowOf(Unit)) { stack, _ ->
    stack.lastOrNull() ?: Screen.Dashboard
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Screen.Dashboard)

  fun navigate(screen: Screen) {
    val current = _screenStack.value
    // If navigating to a primary tab, clear the stack to avoid deep tab nests
    val isPrimaryTab = screen is Screen.Dashboard || screen is Screen.Customers || screen is Screen.Collections || screen is Screen.Settings
    if (isPrimaryTab) {
      _screenStack.value = listOf(screen)
    } else {
      _screenStack.value = current + screen
    }
  }

  fun navigateBack(): Boolean {
    val current = _screenStack.value
    if (current.size > 1) {
      _screenStack.value = current.dropLast(1)
      return true
    }
    return false
  }

  // Dialog State
  private val _dialogState = MutableStateFlow<DialogState>(DialogState.None)
  val dialogState: StateFlow<DialogState> = _dialogState.asStateFlow()

  fun openDialog(dialog: DialogState) {
    _dialogState.value = dialog
  }

  fun dismissDialog() {
    _dialogState.value = DialogState.None
  }

  // Feedback Notifications
  private val _snackbar = MutableSharedFlow<String>()
  val snackbar: SharedFlow<String> = _snackbar.asSharedFlow()

  fun showMessage(msg: String) {
    viewModelScope.launch {
      _snackbar.emit(msg)
    }
  }

  // User Profile & Settings
  private val _profile = MutableStateFlow(UserBusinessProfile())
  val profile: StateFlow<UserBusinessProfile> = _profile.asStateFlow()

  fun updateBusinessProfile(name: String, owner: String, phone: String, city: String, notifications: Boolean) {
    _profile.value = _profile.value.copy(
      businessName = name,
      ownerName = owner,
      phone = phone,
      city = city,
      notificationsEnabled = notifications
    )
    showMessage("İşletme profili güncellendi.")
  }

  fun setActiveTier(tier: SubscriptionTier) {
    _profile.value = _profile.value.copy(activeTier = tier)
    showMessage("${tier.title} paketine geçildi.")
  }

  // Filter & Search
  val searchQuery = MutableStateFlow("")
  val customerFilter = MutableStateFlow(CustomerFilter.ALL)
  val customerSort = MutableStateFlow(CustomerSort.NAME_ASC)

  // Data streams from repository
  val allCustomersWithBalance = repository.customersWithBalances
  val allDebts = repository.allDebts
  val allCollections = repository.allCollections
  val allTemplates = repository.allTemplates

  // Filtered & Sorted Customer list
  val filteredCustomers: StateFlow<List<CustomerWithBalance>> = combine(
    repository.customersWithBalances,
    searchQuery,
    customerFilter,
    customerSort
  ) { list, query, filter, sort ->
    var filtered = list.filter { item ->
      val c = item.customer
      val matchesQuery = query.isBlank() ||
        c.name.contains(query, ignoreCase = true) ||
        c.phone.contains(query) ||
        c.notes.contains(query, ignoreCase = true)

      val matchesFilter = when (filter) {
        CustomerFilter.ALL -> !c.isArchived
        CustomerFilter.WITH_DEBT -> !c.isArchived && item.remainingBalance > 0
        CustomerFilter.ZERO_BALANCE -> !c.isArchived && item.remainingBalance <= 0
        CustomerFilter.ARCHIVED -> c.isArchived
      }

      matchesQuery && matchesFilter
    }

    filtered = when (sort) {
      CustomerSort.NAME_ASC -> filtered.sortedBy { it.customer.name.lowercase() }
      CustomerSort.NAME_DESC -> filtered.sortedByDescending { it.customer.name.lowercase() }
      CustomerSort.DEBT_DESC -> filtered.sortedByDescending { it.remainingBalance }
      CustomerSort.RECENT_ACTIVITY -> filtered.sortedByDescending { it.lastTransactionDate ?: 0L }
    }

    filtered
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // Dashboard Stats
  val dashboardSummary: StateFlow<DashboardSummary> = combine(
    repository.customersWithBalances,
    repository.allDebts,
    repository.allCollections
  ) { customers, debts, collections ->
    val now = System.currentTimeMillis()
    val cal = Calendar.getInstance()

    // Today start
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val todayStart = cal.timeInMillis

    // This week start
    cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
    val weekStart = cal.timeInMillis

    // This month start
    cal.set(Calendar.DAY_OF_MONTH, 1)
    val monthStart = cal.timeInMillis

    val totalRec = customers.filter { !it.customer.isArchived }.sumOf { maxOf(0.0, it.remainingBalance) }
    val totalColl = collections.sumOf { it.amount }

    // Overdue debts
    val overdueDebts = debts.filter { it.dueDate != null && it.dueDate < now }
    val overdueSum = overdueDebts.sumOf { it.amount }

    val todayColl = collections.filter { it.date >= todayStart }.sumOf { it.amount }
    val weekColl = collections.filter { it.date >= weekStart }.sumOf { it.amount }
    val monthColl = collections.filter { it.date >= monthStart }.sumOf { it.amount }

    DashboardSummary(
      totalReceivable = totalRec,
      totalCollected = totalColl,
      overdueAmount = overdueSum,
      activeCustomerCount = customers.count { !it.customer.isArchived },
      overdueDebtsCount = overdueDebts.size,
      todayCollected = todayColl,
      thisWeekCollected = weekColl,
      thisMonthCollected = monthColl
    )
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardSummary())

  // Customer Detail flow
  private val _selectedCustomerId = MutableStateFlow<Long?>(null)
  fun selectCustomer(id: Long) {
    _selectedCustomerId.value = id
    navigate(Screen.CustomerDetail(id))
  }

  val selectedCustomerWithBalance: StateFlow<CustomerWithBalance?> = combine(
    repository.customersWithBalances,
    _selectedCustomerId
  ) { list, id ->
    if (id == null) null else list.find { it.customer.id == id }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  val selectedCustomerTransactions: StateFlow<List<TransactionItem>> = _selectedCustomerId
    .flatMapLatest { id ->
      if (id == null) flowOf(emptyList()) else repository.getTransactionsForCustomer(id)
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // Customer Actions
  fun saveCustomer(
    name: String,
    phone: String,
    email: String,
    address: String,
    notes: String,
    existingId: Long = 0,
    onSuccess: () -> Unit
  ) {
    if (name.isBlank()) {
      showMessage("Lütfen müşteri adını giriniz.")
      return
    }

    // Check tier limits
    val activeCount = dashboardSummary.value.activeCustomerCount
    val tier = profile.value.activeTier
    if (existingId == 0L && activeCount >= tier.maxCustomerLimit) {
      openDialog(DialogState.TierLimitReached(activeCount, tier.maxCustomerLimit))
      return
    }

    viewModelScope.launch {
      val customer = Customer(
        id = existingId,
        name = name.trim(),
        phone = phone.trim(),
        email = email.trim(),
        address = address.trim(),
        notes = notes.trim()
      )
      if (existingId == 0L) {
        repository.insertCustomer(customer)
        showMessage("Yeni müşteri eklendi: $name")
      } else {
        repository.updateCustomer(customer)
        showMessage("Müşteri güncellendi.")
      }
      dismissDialog()
      onSuccess()
    }
  }

  fun toggleArchiveCustomer(customer: Customer) {
    viewModelScope.launch {
      val newStatus = !customer.isArchived
      repository.setCustomerArchived(customer.id, newStatus)
      showMessage(if (newStatus) "Müşteri arşivlendi." else "Müşteri arşivden çıkarıldı.")
    }
  }

  fun deleteCustomer(customer: Customer) {
    viewModelScope.launch {
      repository.deleteCustomer(customer)
      showMessage("${customer.name} ve tüm hareketleri silindi.")
      dismissDialog()
      if (currentScreen.value is Screen.CustomerDetail) {
        navigateBack()
      }
    }
  }

  // Debt Actions
  fun saveDebt(
    customerId: Long,
    amount: Double,
    dueDate: Long?,
    description: String,
    category: String,
    existingId: Long = 0,
    onSuccess: () -> Unit
  ) {
    if (amount <= 0) {
      showMessage("Borç tutarı 0'dan büyük olmalıdır.")
      return
    }
    viewModelScope.launch {
      val debt = Debt(
        id = existingId,
        customerId = customerId,
        amount = amount,
        dueDate = dueDate,
        description = description.trim(),
        category = category
      )
      if (existingId == 0L) {
        repository.insertDebt(debt)
        showMessage("Yeni borç kaydı eklendi.")
      } else {
        repository.updateDebt(debt)
        showMessage("Borç kaydı güncellendi.")
      }
      dismissDialog()
      onSuccess()
    }
  }

  fun deleteDebt(debt: Debt) {
    viewModelScope.launch {
      repository.deleteDebt(debt)
      showMessage("Borç kaydı silindi.")
      dismissDialog()
    }
  }

  // Collection Actions
  fun saveCollection(
    customerId: Long,
    amount: Double,
    paymentMethod: PaymentMethod,
    notes: String,
    existingId: Long = 0,
    onSuccess: () -> Unit
  ) {
    if (amount <= 0) {
      showMessage("Tahsilat tutarı 0'dan büyük olmalıdır.")
      return
    }
    viewModelScope.launch {
      val coll = CollectionRecord(
        id = existingId,
        customerId = customerId,
        amount = amount,
        paymentMethod = paymentMethod,
        notes = notes.trim()
      )
      if (existingId == 0L) {
        val result = repository.insertCollection(coll)
        result.onSuccess {
          showMessage("Tahsilat başarıyla kaydedildi.")
          dismissDialog()
          onSuccess()
        }.onFailure { err ->
          showMessage(err.message ?: "Tahsilat kaydedilemedi.")
        }
      } else {
        repository.updateCollection(coll)
        showMessage("Tahsilat güncellendi.")
        dismissDialog()
        onSuccess()
      }
    }
  }

  fun deleteCollection(collection: CollectionRecord) {
    viewModelScope.launch {
      repository.deleteCollection(collection)
      showMessage("Tahsilat kaydı silindi.")
      dismissDialog()
    }
  }

  // WhatsApp Templates
  fun updateTemplate(template: WhatsAppTemplate) {
    viewModelScope.launch {
      repository.updateTemplate(template)
      showMessage("Şablon güncellendi.")
      dismissDialog()
    }
  }

  // CSV Import & Export
  fun parseAndPreviewCsv(csvContent: String) {
    val result = CsvHelper.parseCustomerCsv(csvContent)
    if (result.validCustomers.isEmpty() && result.errors.isNotEmpty()) {
      showMessage("CSV okunamadı: ${result.errors.first()}")
    } else {
      openDialog(DialogState.CsvImportPreview(result.validCustomers, result.errors))
    }
  }

  fun executeCsvImport(validCustomers: List<Customer>) {
    viewModelScope.launch {
      val tier = profile.value.activeTier
      val currentTotal = dashboardSummary.value.activeCustomerCount
      val availableSlots = tier.maxCustomerLimit - currentTotal

      val toImport = if (validCustomers.size > availableSlots && tier.maxCustomerLimit != Int.MAX_VALUE) {
        validCustomers.take(maxOf(0, availableSlots))
      } else {
        validCustomers
      }

      repository.insertCustomers(toImport)
      val skipped = validCustomers.size - toImport.size
      if (skipped > 0) {
        showMessage("${toImport.size} müşteri içe aktarıldı. Paket limiti nedeniyle $skipped kayıt aktarılamadı.")
      } else {
        showMessage("${toImport.size} müşteri başarıyla içe aktarıldı.")
      }
      dismissDialog()
    }
  }

  // Sample data & reset
  fun loadDemoData() {
    viewModelScope.launch {
      repository.loadSampleData()
      showMessage("Örnek esnaf verileri başarıyla yüklendi!")
    }
  }

  fun clearAllData() {
    viewModelScope.launch {
      repository.clearAllData()
      showMessage("Tüm kayıtlar silindi ve sıfırlandı.")
      dismissDialog()
    }
  }
}
