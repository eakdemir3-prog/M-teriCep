package com.example.viewmodel

import com.example.model.allows
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.AppDatabase
import com.example.data.LedgerSnapshot
import com.example.data.MusteriCepRepository
import com.example.data.SaveResult
import com.example.data.SettingsStore
import com.example.model.AppThemeMode
import com.example.model.CollectionRecord
import com.example.model.Customer
import com.example.model.CustomerFilter
import com.example.model.CustomerSort
import com.example.model.CustomerWithBalance
import com.example.model.Debt
import com.example.model.DebtStatus
import com.example.model.DebtWithStatus
import com.example.model.PaymentMethod
import com.example.model.SubscriptionTier
import com.example.model.TransactionItem
import com.example.model.UserBusinessProfile
import com.example.model.WhatsAppTemplate
import com.example.reminder.DueReminderWorker
import com.example.util.BackupManager
import com.example.util.CsvHelper
import com.example.util.Formatters
import com.example.util.Money
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.Collator
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class MusteriCepViewModel(application: Application) : AndroidViewModel(application) {

  private val repository = MusteriCepRepository(AppDatabase.getDatabase(application))
  private val settings = SettingsStore(application)
  private val trCollator: Collator = Collator.getInstance(Locale("tr", "TR")).apply { strength = Collator.PRIMARY }

  init {
    viewModelScope.launch { repository.ensureDefaultTemplates() }
  }

  // ---------------- Gezinme ----------------

  private val _screenStack = MutableStateFlow<List<Screen>>(listOf(Screen.Dashboard))
  val currentScreen: StateFlow<Screen> = _screenStack
    .map { it.lastOrNull() ?: Screen.Dashboard }
    .stateIn(viewModelScope, SharingStarted.Eagerly, Screen.Dashboard)

  fun navigate(screen: Screen) {
    val isPrimaryTab = screen is Screen.Dashboard || screen is Screen.Customers ||
      screen is Screen.Collections || screen is Screen.Settings || screen is Screen.Appointments
    _screenStack.value = if (isPrimaryTab) listOf(screen) else _screenStack.value + screen
  }

  /** Geri tuşu: alt sayfadan bir önceki sayfaya, ana sekmelerden Ana Sayfa'ya döner. */
  fun navigateBack(): Boolean {
    val stack = _screenStack.value
    return when {
      stack.size > 1 -> { _screenStack.value = stack.dropLast(1); true }
      stack.lastOrNull() !is Screen.Dashboard -> { _screenStack.value = listOf(Screen.Dashboard); true }
      else -> false
    }
  }

  // ---------------- Pencereler ve mesajlar ----------------

  private val _dialogState = MutableStateFlow<DialogState>(DialogState.None)
  val dialogState: StateFlow<DialogState> = _dialogState.asStateFlow()

  fun openDialog(dialog: DialogState) { _dialogState.value = dialog }
  fun dismissDialog() { _dialogState.value = DialogState.None }

  private val _messages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 4)
  val messages: SharedFlow<UiMessage> = _messages.asSharedFlow()

  fun showMessage(msg: String) { _messages.tryEmit(UiMessage(msg)) }

  private fun showUndo(msg: String, undo: () -> Unit) {
    _messages.tryEmit(UiMessage(msg, "Geri Al", undo))
  }

  // ---------------- Profil ve ayarlar ----------------

  /** DataStore'dan okunana kadar null (ilk açılışta yanlış tema yanıp sönmesin diye). */
  val loadedProfile: StateFlow<UserBusinessProfile?> =
    settings.profile.stateIn(viewModelScope, SharingStarted.Eagerly, null)

  val profile: StateFlow<UserBusinessProfile> = loadedProfile
    .map { it ?: UserBusinessProfile() }
    .stateIn(viewModelScope, SharingStarted.Eagerly, UserBusinessProfile())

  fun updateBusinessProfile(name: String, owner: String, phone: String, city: String) {
    viewModelScope.launch {
      settings.updateBusiness(name, owner, phone, city)
      showMessage("İşletme bilgileri kaydedildi.")
    }
  }

  fun setTheme(mode: AppThemeMode) {
    viewModelScope.launch { settings.setTheme(mode) }
  }

  /** Bildirim izni ekranda istendikten sonra çağrılır. */
  fun setNotifications(enabled: Boolean) {
    if (enabled && !requireFeature(com.example.model.Feature.DUE_REMINDERS)) return
    viewModelScope.launch {
      settings.setNotifications(enabled)
      val ctx = getApplication<Application>()
      if (enabled) {
        DueReminderWorker.schedule(ctx)
        showMessage("Vade hatırlatıcı açıldı. Her sabah 09:00 civarı kontrol edilir.")
      } else {
        DueReminderWorker.cancel(ctx)
        showMessage("Vade hatırlatıcı kapatıldı.")
      }
    }
  }

  fun setReminderDays(days: Int) {
    viewModelScope.launch { settings.setReminderDays(days) }
  }

  /** Hatırlatıcıyı hemen bir kez çalıştırır (deneme için). */
  fun runReminderNow() {
    val ctx = getApplication<Application>()
    if (!DueReminderWorker.canNotify(ctx)) {
      showMessage("Bildirim izni kapalı. Telefon ayarlarından MüşteriCep bildirimlerine izin verin.")
      return
    }
    WorkManager.getInstance(ctx).enqueue(OneTimeWorkRequestBuilder<DueReminderWorker>().build())
    showMessage("Kontrol edildi. Vadesi gelen alacak varsa bildirim gelecek.")
  }

  /** Özellik mevcut pakette varsa true; yoksa paket önerisi penceresini açar. */
  fun requireFeature(f: com.example.model.Feature): Boolean {
    if (profile.value.activeTier.allows(f)) return true
    openDialog(DialogState.FeatureLocked(f))
    return false
  }

  // ---------------- Veri akışları ----------------

  val ledger: StateFlow<LedgerSnapshot> =
    repository.ledger.stateIn(viewModelScope, SharingStarted.Eagerly, LedgerSnapshot())

  val allCustomersWithBalance: StateFlow<List<CustomerWithBalance>> = ledger
    .map { it.customers }
    .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  val debtStatuses: StateFlow<List<DebtWithStatus>> = ledger
    .map { it.debtStatuses }
    .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  val allDebts: Flow<List<Debt>> = repository.allDebts
  val allCollections: StateFlow<List<CollectionRecord>> = ledger
    .map { it.collections }
    .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
  val allTemplates: Flow<List<WhatsAppTemplate>> = repository.allTemplates

  val hasDemoData: StateFlow<Boolean> = ledger
    .map { snap -> snap.customers.any { it.customer.isDemo } }
    .stateIn(viewModelScope, SharingStarted.Eagerly, false)

  // ---------------- Arama ve filtre ----------------

  val searchQuery = MutableStateFlow("")
  val customerFilter = MutableStateFlow(CustomerFilter.ALL)
  val customerSort = MutableStateFlow(CustomerSort.NAME_ASC)

  val filteredCustomers: StateFlow<List<CustomerWithBalance>> = combine(
    allCustomersWithBalance, searchQuery, customerFilter, customerSort
  ) { list, query, filter, sort ->
    val q = query.trim().lowercase(Locale("tr", "TR"))
    val qDigits = query.filter { it.isDigit() }
    val filtered = list.filter { item ->
      val c = item.customer
      val matchesQuery = q.isBlank() ||
        c.name.lowercase(Locale("tr", "TR")).contains(q) ||
        c.notes.lowercase(Locale("tr", "TR")).contains(q) ||
        (qDigits.length >= 3 && c.phone.filter { it.isDigit() }.contains(qDigits))
      val matchesFilter = when (filter) {
        CustomerFilter.ALL -> !c.isArchived
        CustomerFilter.WITH_DEBT -> !c.isArchived && item.remainingBalance > 0
        CustomerFilter.OVERDUE -> !c.isArchived && item.overdueAmount > 0
        CustomerFilter.ZERO_BALANCE -> !c.isArchived && item.remainingBalance <= 0
        CustomerFilter.ARCHIVED -> c.isArchived
      }
      matchesQuery && matchesFilter
    }
    when (sort) {
      CustomerSort.NAME_ASC -> filtered.sortedWith { a, b -> trCollator.compare(a.customer.name, b.customer.name) }
      CustomerSort.NAME_DESC -> filtered.sortedWith { a, b -> trCollator.compare(b.customer.name, a.customer.name) }
      CustomerSort.DEBT_DESC -> filtered.sortedByDescending { it.remainingBalance }
      CustomerSort.RECENT_ACTIVITY -> filtered.sortedByDescending { it.lastTransactionDate ?: 0L }
    }
  }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  // ---------------- Ana sayfa özeti ----------------

  val dashboardSummary: StateFlow<DashboardSummary> = ledger.map { snap ->
    val now = System.currentTimeMillis()
    val todayStart = Formatters.startOfDay(now)
    val weekStart = Formatters.startOfWeek(now)
    val monthStart = Formatters.startOfMonth(now)
    val lastMonthStart = Formatters.startOfMonth(monthStart - 1)
    val upcomingLimit = todayStart + 8 * 86_400_000L

    fun sum(items: List<CollectionRecord>) = Money.sum(items.map { it.amount })

    val open = snap.debtStatuses.filter { it.remainingAmount > 0 }
    val overdue = open.filter { it.status == DebtStatus.OVERDUE }
    val upcoming = open.filter { it.status != DebtStatus.OVERDUE && it.debt.dueDate != null && it.debt.dueDate < upcomingLimit }

    DashboardSummary(
      totalReceivable = Money.sum(snap.customers.map { maxOf(0.0, it.remainingBalance) }),
      totalCollected = sum(snap.collections),
      overdueAmount = Money.sum(overdue.map { it.remainingAmount }),
      activeCustomerCount = snap.customers.count { !it.customer.isArchived },
      overdueDebtsCount = overdue.size,
      overdueCustomerCount = overdue.map { it.debt.customerId }.distinct().size,
      debtorCount = snap.customers.count { it.remainingBalance > 0 },
      upcomingAmount = Money.sum(upcoming.map { it.remainingAmount }),
      upcomingCount = upcoming.size,
      todayCollected = sum(snap.collections.filter { it.date >= todayStart }),
      thisWeekCollected = sum(snap.collections.filter { it.date >= weekStart }),
      thisMonthCollected = sum(snap.collections.filter { it.date >= monthStart }),
      lastMonthCollected = sum(snap.collections.filter { it.date in lastMonthStart until monthStart })
    )
  }.stateIn(viewModelScope, SharingStarted.Eagerly, DashboardSummary())

  // ---------------- Müşteri detayı ----------------

  private val _selectedCustomerId = MutableStateFlow<Long?>(null)

  fun selectCustomer(id: Long) {
    _selectedCustomerId.value = id
    navigate(Screen.CustomerDetail(id))
  }

  val selectedCustomerWithBalance: StateFlow<CustomerWithBalance?> =
    combine(allCustomersWithBalance, _selectedCustomerId) { list, id ->
      if (id == null) null else list.find { it.customer.id == id }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

  val selectedCustomerTransactions: StateFlow<List<TransactionItem>> = _selectedCustomerId
    .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else repository.getTransactionsForCustomer(id) }
    .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  // ---------------- Kayıt kilidi ----------------

  /** Kaydet'e art arda basılınca aynı kaydın iki kez oluşmasını önler. */
  private val _saving = MutableStateFlow(false)
  val saving: StateFlow<Boolean> = _saving.asStateFlow()

  private fun guardedSave(block: suspend () -> Unit) {
    if (_saving.value) return
    _saving.value = true
    viewModelScope.launch {
      try {
        block()
      } catch (e: Exception) {
        showMessage("Kaydedilemedi: ${e.message ?: "bilinmeyen hata"}")
      } finally {
        _saving.value = false
      }
    }
  }

  // ---------------- Müşteri işlemleri ----------------

  fun saveCustomer(
    name: String,
    phone: String,
    email: String,
    address: String,
    notes: String,
    existing: Customer? = null
  ) {
    if (name.isBlank()) {
      showMessage("Müşteri adı zorunludur.")
      return
    }
    guardedSave {
      if (existing == null) {
        val tier = profile.value.activeTier
        val count = repository.countRealCustomers()
        if (com.example.model.Billing.ENABLED && count >= tier.maxCustomerLimit) {
          openDialog(DialogState.TierLimitReached(count, tier.maxCustomerLimit))
          return@guardedSave
        }
        repository.insertCustomer(
          Customer(name = name.trim(), phone = phone.trim(), email = email.trim(), address = address.trim(), notes = notes.trim())
        )
        showMessage("Müşteri eklendi: ${name.trim()}")
      } else {
        // Arşiv durumu, oluşturma tarihi ve örnek veri işareti korunur.
        repository.updateCustomer(
          existing.copy(name = name.trim(), phone = phone.trim(), email = email.trim(), address = address.trim(), notes = notes.trim())
        )
        showMessage("Müşteri güncellendi.")
      }
      dismissDialog()
    }
  }

  fun toggleArchiveCustomer(customer: Customer) {
    viewModelScope.launch {
      val newStatus = !customer.isArchived
      repository.setCustomerArchived(customer.id, newStatus)
      showMessage(if (newStatus) "Müşteri arşive kaldırıldı." else "Müşteri arşivden çıkarıldı.")
    }
  }

  fun deleteCustomer(customer: Customer) {
    viewModelScope.launch {
      dismissDialog()
      if (currentScreen.value is Screen.CustomerDetail) navigateBack()
      repository.deleteCustomer(customer)
      showMessage("${customer.name} ve tüm hareketleri silindi.")
    }
  }

  // ---------------- Borç işlemleri ----------------

  fun saveDebt(
    customerId: Long,
    amount: Double,
    date: Long,
    dueDate: Long?,
    description: String,
    category: String,
    existingId: Long = 0
  ) {
    if (customerId <= 0) { showMessage("Lütfen müşteri seçin."); return }
    if (Money.toCents(amount) <= 0) { showMessage("Tutar 0'dan büyük olmalıdır."); return }
    guardedSave {
      val debt = Debt(
        id = existingId, customerId = customerId, amount = amount, date = date,
        dueDate = dueDate, description = description.trim(), category = category
      )
      val result = if (existingId == 0L) repository.insertDebt(debt) else repository.updateDebt(debt)
      when (result) {
        is SaveResult.Saved -> {
          dismissDialog()
          showMessage(if (existingId == 0L) "Borç kaydedildi: ${Formatters.formatCurrency(amount)}" else "Borç kaydı güncellendi.")
        }
        is SaveResult.Failed -> showMessage(result.message)
        is SaveResult.NeedsConfirmation -> showMessage(result.reason)
      }
    }
  }

  fun editDebt(id: Long) {
    viewModelScope.launch {
      repository.getDebt(id)?.let { openDialog(DialogState.AddDebt(it.customerId, existing = it)) }
    }
  }

  fun deleteDebt(id: Long) {
    viewModelScope.launch {
      repository.setDebtDeleted(id, true)
      dismissDialog()
      showUndo("Borç kaydı silindi.") {
        viewModelScope.launch { repository.setDebtDeleted(id, false) }
      }
    }
  }

  // ---------------- Tahsilat işlemleri ----------------

  fun saveCollection(
    customerId: Long,
    amount: Double,
    date: Long,
    paymentMethod: PaymentMethod,
    notes: String,
    existingId: Long = 0,
    force: Boolean = false
  ) {
    if (customerId <= 0) { showMessage("Lütfen müşteri seçin."); return }
    if (Money.toCents(amount) <= 0) { showMessage("Tutar 0'dan büyük olmalıdır."); return }
    guardedSave {
      val record = CollectionRecord(
        id = existingId, customerId = customerId, amount = amount, date = date,
        paymentMethod = paymentMethod, notes = notes.trim()
      )
      val result = if (existingId == 0L) repository.insertCollection(record, force) else repository.updateCollection(record)
      when (result) {
        is SaveResult.Saved -> {
          dismissDialog()
          showMessage(if (existingId == 0L) "Tahsilat kaydedildi: ${Formatters.formatCurrency(amount)}" else "Tahsilat güncellendi.")
        }
        is SaveResult.Failed -> showMessage(result.message)
        is SaveResult.NeedsConfirmation -> openDialog(
          DialogState.ConfirmAction(
            title = "Kontrol edin",
            message = result.reason,
            confirmLabel = "Yine de kaydet",
            onConfirm = { saveCollection(customerId, amount, date, paymentMethod, notes, existingId, force = true) }
          )
        )
      }
    }
  }

  fun editCollection(id: Long) {
    viewModelScope.launch {
      repository.getCollection(id)?.let {
        openDialog(DialogState.AddCollection(preselectedCustomerId = it.customerId, existing = it))
      }
    }
  }

  fun deleteCollection(id: Long) {
    viewModelScope.launch {
      repository.setCollectionDeleted(id, true)
      dismissDialog()
      showUndo("Tahsilat kaydı silindi.") {
        viewModelScope.launch { repository.setCollectionDeleted(id, false) }
      }
    }
  }

  // ---------------- WhatsApp şablonları ----------------

  fun updateTemplate(template: WhatsAppTemplate) {
    if (!requireFeature(com.example.model.Feature.EDIT_TEMPLATES)) return
    if (template.templateText.isBlank()) { showMessage("Mesaj metni boş olamaz."); return }
    viewModelScope.launch {
      repository.updateTemplate(template.copy(isCustom = true))
      showMessage("Şablon kaydedildi.")
      dismissDialog()
    }
  }

  fun resetTemplates() {
    viewModelScope.launch {
      repository.resetTemplates()
      dismissDialog()
      showMessage("Şablonlar varsayılana döndürüldü.")
    }
  }

  // ---------------- CSV ----------------

  fun parseAndPreviewCsv(csvContent: String) {
    viewModelScope.launch {
      val result = CsvHelper.parseCustomerCsv(csvContent, repository.existingPhones())
      if (result.validCustomers.isEmpty() && result.skippedDuplicates.isEmpty() && result.errors.isNotEmpty()) {
        showMessage("CSV okunamadı: ${result.errors.first()}")
      } else {
        openDialog(DialogState.CsvImportPreview(result.validCustomers, result.errors, result.skippedDuplicates))
      }
    }
  }

  fun executeCsvImport(validCustomers: List<Customer>) {
    if (!requireFeature(com.example.model.Feature.CSV_IMPORT)) return
    viewModelScope.launch {
      val tier = profile.value.activeTier
      val available = if (!com.example.model.Billing.ENABLED || tier.maxCustomerLimit == com.example.model.UNLIMITED) Int.MAX_VALUE
        else (tier.maxCustomerLimit - repository.countRealCustomers()).coerceAtLeast(0)
      val toImport = validCustomers.take(available)
      if (toImport.isNotEmpty()) repository.insertCustomers(toImport)
      val skipped = validCustomers.size - toImport.size
      dismissDialog()
      showMessage(
        if (skipped > 0) "${toImport.size} müşteri eklendi. Paket limiti nedeniyle $skipped kayıt eklenemedi."
        else "${toImport.size} müşteri içe aktarıldı."
      )
    }
  }

  fun exportCustomersCsv(): String = CsvHelper.exportCustomersToCsv(
    allCustomersWithBalance.value.filter { !it.customer.isDemo }
  )

  fun exportTransactionsCsv(): String {
    val snap = ledger.value
    val real = snap.customers.filter { !it.customer.isDemo }.associate { it.customer.id to it.customer }
    return CsvHelper.exportAllTransactionsToCsv(
      real,
      snap.debtStatuses.filter { it.debt.customerId in real },
      snap.collections.filter { it.customerId in real }
    )
  }

  // ---------------- Yedekleme ----------------

  suspend fun buildBackupJson(): String =
    BackupManager.toJson(repository.snapshot().copy(profile = profile.value))

  fun confirmRestore(json: String) {
    val snapshot = try {
      BackupManager.fromJson(json)
    } catch (e: Exception) {
      showMessage(e.message ?: "Yedek dosyası okunamadı.")
      return
    }
    openDialog(
      DialogState.ConfirmAction(
        title = "Yedekten geri yükle",
        message = "Yedek tarihi: ${Formatters.formatDateTime(snapshot.createdAt)}\n" +
          "${snapshot.customers.size} müşteri, ${snapshot.debts.count { !it.isDeleted }} borç, " +
          "${snapshot.collections.count { !it.isDeleted }} tahsilat.\n\n" +
          "Telefondaki mevcut kayıtların YERİNE yedektekiler konacak. Devam edilsin mi?",
        confirmLabel = "Geri yükle",
        isDestructive = true,
        onConfirm = {
          viewModelScope.launch {
            try {
              repository.restore(snapshot)
              snapshot.profile?.let { settings.restore(it) }
              dismissDialog()
              showMessage("Yedek geri yüklendi: ${snapshot.customers.size} müşteri.")
            } catch (e: Exception) {
              dismissDialog()
              showMessage("Geri yükleme başarısız: ${e.message}")
            }
          }
        }
      )
    )
  }

  // ---------------- Örnek veri ve sıfırlama ----------------

  fun loadDemoData() {
    viewModelScope.launch {
      if (repository.loadSampleData()) {
        showMessage("Örnek veriler yüklendi. İstediğinizde Ayarlar'dan silebilirsiniz.")
      } else {
        showMessage("Gerçek müşteri kaydınız olduğu için örnek veri yüklenmedi.")
      }
    }
  }

  fun clearDemoData() {
    viewModelScope.launch {
      repository.clearDemoData()
      showMessage("Örnek veriler silindi.")
    }
  }

  fun clearAllData() {
    viewModelScope.launch {
      repository.clearAllData()
      dismissDialog()
      navigate(Screen.Dashboard)
      showMessage("Tüm müşteri ve hareket kayıtları silindi.")
    }
  }
}
