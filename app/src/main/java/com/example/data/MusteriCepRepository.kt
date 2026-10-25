package com.example.data

import com.example.model.CollectionRecord
import com.example.model.Customer
import com.example.model.CustomerWithBalance
import com.example.model.Debt
import com.example.model.DebtStatus
import com.example.model.DefaultTemplates
import com.example.model.PaymentMethod
import com.example.model.TransactionItem
import com.example.model.WhatsAppTemplate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.Calendar

class MusteriCepRepository(
  private val customerDao: CustomerDao,
  private val debtDao: DebtDao,
  private val collectionDao: CollectionDao,
  private val templateDao: TemplateDao
) {

  val allCustomers: Flow<List<Customer>> = customerDao.getAllCustomers()
  val activeCustomers: Flow<List<Customer>> = customerDao.getActiveCustomers()
  val allDebts: Flow<List<Debt>> = debtDao.getAllDebts()
  val allCollections: Flow<List<CollectionRecord>> = collectionDao.getAllCollections()
  val allTemplates: Flow<List<WhatsAppTemplate>> = templateDao.getAllTemplates()

  // Combined real-time Customer with Balance calculations
  val customersWithBalances: Flow<List<CustomerWithBalance>> = combine(
    allCustomers,
    allDebts,
    allCollections
  ) { customers, debts, collections ->
    val now = System.currentTimeMillis()
    customers.map { customer ->
      val customerDebts = debts.filter { it.customerId == customer.id }
      val customerCollections = collections.filter { it.customerId == customer.id }

      val totalDebt = customerDebts.sumOf { it.amount }
      val totalPaid = customerCollections.sumOf { it.amount }
      val balance = totalDebt - totalPaid

      val overdueCount = customerDebts.count { it.dueDate != null && it.dueDate < now && balance > 0 }

      val lastDebtDate = customerDebts.maxOfOrNull { it.date }
      val lastCollectionDate = customerCollections.maxOfOrNull { it.date }
      val lastActivity = listOfNotNull(lastDebtDate, lastCollectionDate).maxOrNull()

      CustomerWithBalance(
        customer = customer,
        totalDebt = totalDebt,
        totalPaid = totalPaid,
        remainingBalance = if (balance < 0.001 && balance > -0.001) 0.0 else balance,
        overdueDebtCount = overdueCount,
        lastTransactionDate = lastActivity
      )
    }
  }

  suspend fun findCustomerByPhone(phone: String, excludeId: Long = 0): Customer? {
    return customerDao.findCustomerByPhone(phone, excludeId)
  }

  suspend fun insertCustomer(customer: Customer): Long {
    return customerDao.insertCustomer(customer)
  }

  suspend fun insertCustomers(customers: List<Customer>): List<Long> {
    return customerDao.insertAll(customers)
  }

  suspend fun updateCustomer(customer: Customer) {
    customerDao.updateCustomer(customer)
  }

  suspend fun deleteCustomer(customer: Customer) {
    customerDao.deleteCustomer(customer)
  }

  suspend fun setCustomerArchived(id: Long, isArchived: Boolean) {
    customerDao.setArchived(id, isArchived)
  }

  // Debt operations
  suspend fun insertDebt(debt: Debt): Long {
    require(debt.amount > 0) { "Borç tutarı 0'dan büyük olmalıdır." }
    return debtDao.insertDebt(debt)
  }

  suspend fun updateDebt(debt: Debt) {
    debtDao.updateDebt(debt)
  }

  suspend fun deleteDebt(debt: Debt) {
    debtDao.deleteDebt(debt)
  }

  // Collection operations with duplicate checking
  suspend fun insertCollection(collection: CollectionRecord): Result<Long> {
    if (collection.amount <= 0) {
      return Result.failure(IllegalArgumentException("Tahsilat tutarı 0'dan büyük olmalıdır."))
    }

    // Check duplicate in last 60 seconds
    val sixtySecondsAgo = System.currentTimeMillis() - 60_000
    val recent = collectionDao.findRecentDuplicate(
      customerId = collection.customerId,
      amount = collection.amount,
      afterTimestamp = sixtySecondsAgo
    )
    if (recent.isNotEmpty()) {
      return Result.failure(IllegalStateException("Bu müşteri için aynı tutarda tahsilat son 1 dakika içinde kaydedildi. Çift kaydı önlemek için kontrol ediniz."))
    }

    val id = collectionDao.insertCollection(collection)
    return Result.success(id)
  }

  suspend fun updateCollection(collection: CollectionRecord) {
    collectionDao.updateCollection(collection)
  }

  suspend fun deleteCollection(collection: CollectionRecord) {
    collectionDao.deleteCollection(collection)
  }

  // Timeline history for customer
  fun getTransactionsForCustomer(customerId: Long): Flow<List<TransactionItem>> {
    return combine(
      debtDao.getDebtsForCustomer(customerId),
      collectionDao.getCollectionsForCustomer(customerId)
    ) { debts, collections ->
      val now = System.currentTimeMillis()
      val items = mutableListOf<TransactionItem>()

      // Total collected amount to allocate toward debts
      var runningPaid = collections.sumOf { it.amount }

      // Map debts with calculated status
      debts.forEach { debt ->
        val status = when {
          runningPaid >= debt.amount -> {
            runningPaid -= debt.amount
            DebtStatus.PAID
          }
          runningPaid > 0 -> {
            runningPaid = 0.0
            DebtStatus.PARTIALLY_PAID
          }
          debt.dueDate != null && debt.dueDate < now -> DebtStatus.OVERDUE
          else -> DebtStatus.UNPAID
        }

        items.add(
          TransactionItem.DebtItem(
            id = debt.id,
            customerId = debt.customerId,
            amount = debt.amount,
            date = debt.date,
            notesOrDesc = debt.description,
            dueDate = debt.dueDate,
            category = debt.category,
            status = status
          )
        )
      }

      collections.forEach { coll ->
        items.add(
          TransactionItem.CollectionItem(
            id = coll.id,
            customerId = coll.customerId,
            amount = coll.amount,
            date = coll.date,
            notesOrDesc = coll.notes,
            paymentMethod = coll.paymentMethod,
            debtId = coll.debtId
          )
        )
      }

      // Sort chronological descending (newest first)
      items.sortedByDescending { it.date }
    }
  }

  suspend fun updateTemplate(template: WhatsAppTemplate) {
    templateDao.updateTemplate(template)
  }

  suspend fun ensureDefaultTemplates() {
    if (templateDao.count() == 0) {
      templateDao.insertAll(DefaultTemplates.getDefaults())
    }
  }

  // Demo Sample Data for instant onboarding & testing
  suspend fun loadSampleData() {
    // Clear first to ensure pristine sample state
    customerDao.clearAll()
    debtDao.clearAll()
    collectionDao.clearAll()

    val cal = Calendar.getInstance()
    val now = cal.timeInMillis

    // 1. Örnek Müşteri 1: Ahmet Yılmaz (Borçlu, Vadesi Geçmiş)
    val c1Id = customerDao.insertCustomer(
      Customer(
        name = "Ahmet Yılmaz (Yılmaz Market)",
        phone = "0532 555 10 20",
        email = "ahmet@yilmazmarket.com",
        address = "İstiklal Cad. No: 42, Beyoğlu/İstanbul",
        notes = "Haftalık toptan un ve şeker alımı yapar. Cuma günleri öder."
      )
    )

    // 2. Örnek Müşteri 2: Mehmet Demir (Kısmen Ödemiş)
    val c2Id = customerDao.insertCustomer(
      Customer(
        name = "Mehmet Demir (Demir Oto Servis)",
        phone = "0543 444 30 40",
        email = "demir@otoservis.com",
        address = "Sanayi Sitesi 3. Blok No: 12, Ümraniye/İstanbul",
        notes = "Yedek parça alımları. Havale ile çalışır."
      )
    )

    // 3. Örnek Müşteri 3: Ayşe Kaya (Bakiyesi Sıfır, Düzenli Müşteri)
    val c3Id = customerDao.insertCustomer(
      Customer(
        name = "Ayşe Kaya (Kaya Butik)",
        phone = "0505 333 50 60",
        email = "ayse@kayabutik.com",
        address = "Bağdat Caddesi No: 110, Kadıköy/İstanbul",
        notes = "Kredi kartı ile düzenli ödeme yapar."
      )
    )

    // 4. Örnek Müşteri 4: Canan Öztürk (Yeni Müşteri)
    val c4Id = customerDao.insertCustomer(
      Customer(
        name = "Canan Öztürk (Mimarlık Ofisi)",
        phone = "0530 222 70 80",
        email = "canan@ozturkmimarlik.com",
        address = "Levent Loft D: 8, Şişli/İstanbul",
        notes = "Ofis tadilat malzemeleri temin edildi."
      )
    )

    // Debts & Collections for c1 (Ahmet Yılmaz)
    val pastDueDate = now - (5 * 86_400_000L) // 5 days ago
    debtDao.insertDebt(
      Debt(
        customerId = c1Id,
        amount = 4500.0,
        date = now - (15 * 86_400_000L),
        dueDate = pastDueDate,
        description = "5 Çuval Un ve 2 Koli Şeker",
        category = "Mal / Toptan Alım"
      )
    )
    debtDao.insertDebt(
      Debt(
        customerId = c1Id,
        amount = 1800.0,
        date = now - (2 * 86_400_000L),
        dueDate = now + (7 * 86_400_000L),
        description = "Koli Yağ ve Bakliyat",
        category = "Ürün Satışı"
      )
    )
    collectionDao.insertCollection(
      CollectionRecord(
        customerId = c1Id,
        amount = 2000.0,
        date = now - (3 * 86_400_000L),
        paymentMethod = PaymentMethod.CASH,
        notes = "Dükkanda elden nakit alındı."
      )
    )

    // Debts & Collections for c2 (Mehmet Demir)
    val futureDueDate = now + (10 * 86_400_000L)
    debtDao.insertDebt(
      Debt(
        customerId = c2Id,
        amount = 7500.0,
        date = now - (7 * 86_400_000L),
        dueDate = futureDueDate,
        description = "Motor Yağları ve Filtre Seti",
        category = "Ürün Satışı"
      )
    )
    collectionDao.insertCollection(
      CollectionRecord(
        customerId = c2Id,
        amount = 4000.0,
        date = now - (1 * 86_400_000L),
        paymentMethod = PaymentMethod.BANK_TRANSFER,
        notes = "İş Bankası hesabına EFT geldi."
      )
    )

    // Debts & Collections for c3 (Ayşe Kaya - Tamamen Kapalı)
    debtDao.insertDebt(
      Debt(
        customerId = c3Id,
        amount = 3200.0,
        date = now - (20 * 86_400_000L),
        dueDate = now - (10 * 86_400_000L),
        description = "Yazlık Kumaş Siparişi",
        category = "Ürün Satışı"
      )
    )
    collectionDao.insertCollection(
      CollectionRecord(
        customerId = c3Id,
        amount = 3200.0,
        date = now - (9 * 86_400_000L),
        paymentMethod = PaymentMethod.CREDIT_CARD,
        notes = "POS cihazı tek çekim."
      )
    )

    // Debts for c4 (Canan Öztürk - Yeni Borç)
    debtDao.insertDebt(
      Debt(
        customerId = c4Id,
        amount = 12500.0,
        date = now - (1 * 86_400_000L),
        dueDate = now + (14 * 86_400_000L),
        description = "Aydınlatma Armatürleri ve Kablolama",
        category = "Hizmet & İşçilik"
      )
    )
  }

  suspend fun clearAllData() {
    customerDao.clearAll()
    debtDao.clearAll()
    collectionDao.clearAll()
  }
}
