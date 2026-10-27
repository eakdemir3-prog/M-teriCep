package com.example.data

import androidx.room.withTransaction
import com.example.model.CollectionRecord
import com.example.model.Customer
import com.example.model.CustomerWithBalance
import com.example.model.Debt
import com.example.model.DebtWithStatus
import com.example.model.DefaultTemplates
import com.example.model.PaymentMethod
import com.example.model.TransactionItem
import com.example.model.WhatsAppTemplate
import com.example.util.BackupSnapshot
import com.example.util.Formatters
import com.example.util.Ledger
import com.example.util.Money
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/** Kayıt sonuçları: başarı, kullanıcı onayı gereken uyarı veya hata. */
sealed class SaveResult {
  data class Saved(val id: Long) : SaveResult()
  data class NeedsConfirmation(val reason: String) : SaveResult()
  data class Failed(val message: String) : SaveResult()
}

/** Tüm müşterilerin bakiyeleri ve tüm borçların güncel durumları (tek hesaplamada). */
data class LedgerSnapshot(
  val customers: List<CustomerWithBalance> = emptyList(),
  val debtStatuses: List<DebtWithStatus> = emptyList(),
  val collections: List<CollectionRecord> = emptyList()
)

class MusteriCepRepository(
  private val db: AppDatabase
) {
  private val customerDao = db.customerDao()
  private val debtDao = db.debtDao()
  private val collectionDao = db.collectionDao()
  private val templateDao = db.templateDao()

  val allCustomers: Flow<List<Customer>> = customerDao.getAllCustomers()
  val allDebts: Flow<List<Debt>> = debtDao.getAllDebts()
  val allCollections: Flow<List<CollectionRecord>> = collectionDao.getAllCollections()
  val allTemplates: Flow<List<WhatsAppTemplate>> = templateDao.getAllTemplates()

  val ledger: Flow<LedgerSnapshot> = combine(allCustomers, allDebts, allCollections) { customers, debts, collections ->
    val (balances, statuses) = Ledger.buildAll(customers, debts, collections)
    LedgerSnapshot(balances, statuses, collections)
  }

  val customersWithBalances: Flow<List<CustomerWithBalance>> = ledger.map { it.customers }

  // ---------------- Müşteri ----------------

  suspend fun getCustomer(id: Long): Customer? = customerDao.getCustomerById(id)

  suspend fun countRealCustomers(): Int = customerDao.countRealCustomers()

  suspend fun hasDemoData(): Boolean = customerDao.countDemoCustomers() > 0

  /** Aynı numara başka müşteride kayıtlı mı? (Numara biçimi farklı olsa da yakalar.) */
  suspend fun findCustomerByPhone(phone: String, excludeId: Long = 0): Customer? {
    val clean = Formatters.cleanPhoneNumber(phone)
    if (clean.length < 10) return null
    return customerDao.getAllCustomersOnce().firstOrNull {
      it.id != excludeId && Formatters.cleanPhoneNumber(it.phone) == clean
    }
  }

  suspend fun insertCustomer(customer: Customer): Long = customerDao.insertCustomer(customer.copy(id = 0))

  suspend fun updateCustomer(customer: Customer) = customerDao.updateCustomer(customer)

  suspend fun deleteCustomer(customer: Customer) = customerDao.deleteCustomer(customer)

  suspend fun setCustomerArchived(id: Long, isArchived: Boolean) = customerDao.setArchived(id, isArchived)

  // ---------------- Borç ----------------

  suspend fun insertDebt(debt: Debt): SaveResult {
    if (Money.toCents(debt.amount) <= 0) return SaveResult.Failed("Borç tutarı 0'dan büyük olmalıdır.")
    val now = System.currentTimeMillis()
    val id = debtDao.insertDebt(debt.copy(id = 0, amount = Money.round(debt.amount), createdAt = now, updatedAt = now))
    return SaveResult.Saved(id)
  }

  /** Düzenlemede ilk oluşturma zamanı korunur, sadece değişen alanlar güncellenir. */
  suspend fun updateDebt(debt: Debt): SaveResult {
    if (Money.toCents(debt.amount) <= 0) return SaveResult.Failed("Borç tutarı 0'dan büyük olmalıdır.")
    val existing = debtDao.getById(debt.id) ?: return SaveResult.Failed("Kayıt bulunamadı.")
    debtDao.updateDebt(
      existing.copy(
        amount = Money.round(debt.amount),
        date = debt.date,
        dueDate = debt.dueDate,
        description = debt.description,
        category = debt.category,
        updatedAt = System.currentTimeMillis()
      )
    )
    return SaveResult.Saved(debt.id)
  }

  suspend fun setDebtDeleted(id: Long, deleted: Boolean) = debtDao.setDeleted(id, deleted, System.currentTimeMillis())

  // ---------------- Tahsilat ----------------

  /**
   * Tahsilat kaydı. [force] false ise iki kontrol yapılır ve kullanıcıya sorulur:
   * 1) Son 2 dakikada aynı müşteriye aynı tutar girildiyse (çift kayıt şüphesi)
   * 2) Tutar kalan borçtan fazlaysa (müşteri alacaklı duruma düşer)
   */
  suspend fun insertCollection(collection: CollectionRecord, force: Boolean = false): SaveResult {
    val cents = Money.toCents(collection.amount)
    if (cents <= 0) return SaveResult.Failed("Tahsilat tutarı 0'dan büyük olmalıdır.")

    if (!force) {
      val recent = collectionDao.findRecentDuplicate(
        customerId = collection.customerId,
        amount = Money.round(collection.amount),
        createdAfter = System.currentTimeMillis() - 2 * 60_000
      )
      if (recent.isNotEmpty()) {
        return SaveResult.NeedsConfirmation(
          "Bu müşteriye aynı tutarda (${Formatters.formatCurrency(collection.amount)}) bir tahsilat az önce kaydedildi. " +
            "Aynı ödemeyi iki kez girmediğinizden emin misiniz?"
        )
      }
      val remaining = remainingFor(collection.customerId)
      if (cents > Money.toCents(remaining)) {
        val extra = Money.fromCents(cents - Money.toCents(maxOf(0.0, remaining)))
        return SaveResult.NeedsConfirmation(
          "Tahsilat, kalan borçtan ${Formatters.formatCurrency(extra)} fazla. " +
            "Fark müşterinin alacağı (avans) olarak görünecek. Devam edilsin mi?"
        )
      }
    }

    val now = System.currentTimeMillis()
    val id = collectionDao.insertCollection(
      collection.copy(id = 0, amount = Money.round(collection.amount), createdAt = now, updatedAt = now)
    )
    return SaveResult.Saved(id)
  }

  suspend fun updateCollection(collection: CollectionRecord): SaveResult {
    if (Money.toCents(collection.amount) <= 0) return SaveResult.Failed("Tahsilat tutarı 0'dan büyük olmalıdır.")
    val existing = collectionDao.getById(collection.id) ?: return SaveResult.Failed("Kayıt bulunamadı.")
    collectionDao.updateCollection(
      existing.copy(
        amount = Money.round(collection.amount),
        date = collection.date,
        paymentMethod = collection.paymentMethod,
        notes = collection.notes,
        updatedAt = System.currentTimeMillis()
      )
    )
    return SaveResult.Saved(collection.id)
  }

  suspend fun setCollectionDeleted(id: Long, deleted: Boolean) =
    collectionDao.setDeleted(id, deleted, System.currentTimeMillis())

  private suspend fun remainingFor(customerId: Long): Double {
    val debts = debtDao.getAllDebtsOnce().filter { it.customerId == customerId }
    val colls = collectionDao.getAllCollectionsOnce().filter { it.customerId == customerId }
    return Money.fromCents(debts.sumOf { Money.toCents(it.amount) } - colls.sumOf { Money.toCents(it.amount) })
  }

  // ---------------- Müşteri hareket geçmişi ----------------

  fun getTransactionsForCustomer(customerId: Long): Flow<List<TransactionItem>> {
    return combine(
      debtDao.getDebtsForCustomer(customerId),
      collectionDao.getCollectionsForCustomer(customerId)
    ) { debts, collections ->
      val statuses = Ledger.allocate(debts, collections)
      val items = mutableListOf<TransactionItem>()
      statuses.forEach { s ->
        items += TransactionItem.DebtItem(
          id = s.debt.id,
          customerId = s.debt.customerId,
          amount = s.debt.amount,
          date = s.debt.date,
          notesOrDesc = s.debt.description,
          dueDate = s.debt.dueDate,
          category = s.debt.category,
          status = s.status,
          remainingAmount = s.remainingAmount
        )
      }
      collections.forEach { c ->
        items += TransactionItem.CollectionItem(
          id = c.id,
          customerId = c.customerId,
          amount = c.amount,
          date = c.date,
          notesOrDesc = c.notes,
          paymentMethod = c.paymentMethod,
          debtId = c.debtId
        )
      }
      items.sortedWith(compareByDescending<TransactionItem> { it.date }.thenByDescending { it.id })
    }
  }

  suspend fun getDebt(id: Long): Debt? = debtDao.getById(id)
  suspend fun getCollection(id: Long): CollectionRecord? = collectionDao.getById(id)

  // ---------------- Şablonlar ----------------

  suspend fun updateTemplate(template: WhatsAppTemplate) = templateDao.updateTemplate(template)

  suspend fun ensureDefaultTemplates() {
    if (templateDao.count() == 0) {
      templateDao.insertAll(DefaultTemplates.getDefaults())
      return
    }
    // Kullanıcının değiştirmediği şablonların başlıkları yeni adlarla güncellenir.
    val defaults = DefaultTemplates.getDefaults().associateBy { it.key }
    templateDao.getAllOnce().forEach { t ->
      val d = defaults[t.key] ?: return@forEach
      if (!t.isCustom && t.title != d.title) templateDao.updateTemplate(t.copy(title = d.title))
    }
  }

  suspend fun resetTemplates() {
    db.withTransaction {
      templateDao.clearAll()
      templateDao.insertAll(DefaultTemplates.getDefaults())
    }
  }

  // ---------------- CSV içe aktarma ----------------

  /** Var olan müşterilerle aynı telefona sahip satırları ayıklar. */
  suspend fun existingPhones(): Set<String> =
    customerDao.getAllCustomersOnce().map { Formatters.cleanPhoneNumber(it.phone) }.filter { it.length >= 10 }.toSet()

  suspend fun insertCustomers(customers: List<Customer>): List<Long> =
    customerDao.insertAll(customers.map { it.copy(id = 0) })

  // ---------------- Yedekleme ----------------

  suspend fun snapshot(): BackupSnapshot = BackupSnapshot(
    customers = customerDao.getAllCustomersOnce().filter { !it.isDemo },
    debts = debtDao.getEverything(),
    collections = collectionDao.getEverything(),
    templates = templateDao.getAllOnce()
  )

  /** Yedekten geri yükleme: mevcut tüm kayıtların yerine yedektekiler konur (tek işlemde). */
  suspend fun restore(snapshot: BackupSnapshot) {
    db.withTransaction {
      collectionDao.clearAll()
      debtDao.clearAll()
      customerDao.clearAll()
      templateDao.clearAll()
      customerDao.insertAll(snapshot.customers)
      val ids = snapshot.customers.map { it.id }.toSet()
      debtDao.insertAll(snapshot.debts.filter { it.customerId in ids })
      collectionDao.insertAll(snapshot.collections.filter { it.customerId in ids })
      if (snapshot.templates.isNotEmpty()) templateDao.insertAll(snapshot.templates)
      else templateDao.insertAll(DefaultTemplates.getDefaults())
    }
  }

  // ---------------- Örnek veri ----------------

  /**
   * Örnek veriler sadece hiç gerçek müşteri yokken yüklenir ve "örnek" olarak işaretlenir.
   * Gerçek kayıtlar asla silinmez.
   */
  suspend fun loadSampleData(): Boolean {
    if (customerDao.countRealCustomers() > 0) return false
    db.withTransaction {
      customerDao.deleteDemoCustomers()
      val day = 86_400_000L
      val now = System.currentTimeMillis()

      fun c(name: String, phone: String, email: String, address: String, notes: String) =
        Customer(name = name, phone = phone, email = email, address = address, notes = notes, isDemo = true)

      val c1 = customerDao.insertCustomer(c("Ahmet Yılmaz (Yılmaz Market)", "0532 555 10 20", "", "Beyoğlu/İstanbul", "Cuma günleri öder."))
      val c2 = customerDao.insertCustomer(c("Mehmet Demir (Demir Oto Servis)", "0543 444 30 40", "", "Ümraniye/İstanbul", "Havale ile çalışır."))
      val c3 = customerDao.insertCustomer(c("Ayşe Kaya (Kaya Butik)", "0505 333 50 60", "", "Kadıköy/İstanbul", "Kartla düzenli öder."))
      val c4 = customerDao.insertCustomer(c("Canan Öztürk (Mimarlık Ofisi)", "0530 222 70 80", "", "Şişli/İstanbul", ""))

      debtDao.insertDebt(Debt(customerId = c1, amount = 4500.0, date = now - 15 * day, dueDate = now - 5 * day, description = "5 çuval un, 2 koli şeker", category = "Mal / Toptan Alım"))
      debtDao.insertDebt(Debt(customerId = c1, amount = 1800.0, date = now - 2 * day, dueDate = now + 7 * day, description = "Koli yağ ve bakliyat", category = "Ürün Satışı"))
      collectionDao.insertCollection(CollectionRecord(customerId = c1, amount = 2000.0, date = now - 3 * day, paymentMethod = PaymentMethod.CASH, notes = "Elden nakit", createdAt = now - 3 * day))

      debtDao.insertDebt(Debt(customerId = c2, amount = 7500.0, date = now - 7 * day, dueDate = now + 3 * day, description = "Motor yağı ve filtre seti", category = "Ürün Satışı"))
      collectionDao.insertCollection(CollectionRecord(customerId = c2, amount = 4000.0, date = now - day, paymentMethod = PaymentMethod.BANK_TRANSFER, notes = "EFT", createdAt = now - day))

      debtDao.insertDebt(Debt(customerId = c3, amount = 3200.0, date = now - 20 * day, dueDate = now - 10 * day, description = "Kumaş siparişi", category = "Ürün Satışı"))
      collectionDao.insertCollection(CollectionRecord(customerId = c3, amount = 3200.0, date = now - 9 * day, paymentMethod = PaymentMethod.CREDIT_CARD, notes = "POS tek çekim", createdAt = now - 9 * day))

      debtDao.insertDebt(Debt(customerId = c4, amount = 12500.0, date = now - day, dueDate = now + 14 * day, description = "Aydınlatma ve kablolama", category = "Hizmet & İşçilik"))
    }
    return true
  }

  suspend fun clearDemoData() = customerDao.deleteDemoCustomers()

  suspend fun clearAllData() {
    db.withTransaction {
      collectionDao.clearAll()
      debtDao.clearAll()
      customerDao.clearAll()
    }
  }
}
