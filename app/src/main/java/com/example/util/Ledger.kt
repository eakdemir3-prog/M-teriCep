package com.example.util

import com.example.model.CollectionRecord
import com.example.model.Customer
import com.example.model.CustomerWithBalance
import com.example.model.Debt
import com.example.model.DebtStatus
import com.example.model.DebtWithStatus

/**
 * Cari hesap hesaplamaları (saf fonksiyonlar, birim test edilebilir).
 *
 * Kural: Müşterinin yaptığı tüm ödemeler, en eski borçtan başlayarak sırayla borçlara
 * dağıtılır (FIFO). Böylece "hangi borç ödendi, hangisi vadesi geçmiş" doğru çıkar.
 * Tüm toplamlar kuruş (Long) üzerinden yapılır.
 */
object Ledger {

  /** Bir müşterinin borçlarının güncel durumu. Dönen liste en eski borçtan en yeniye sıralıdır. */
  fun allocate(
    debts: List<Debt>,
    collections: List<CollectionRecord>,
    now: Long = System.currentTimeMillis()
  ): List<DebtWithStatus> {
    var paidPool = collections.filter { !it.isDeleted }.sumOf { Money.toCents(it.amount) }
    val ordered = debts.filter { !it.isDeleted }.sortedWith(compareBy<Debt> { it.date }.thenBy { it.id })

    return ordered.map { debt ->
      val amount = Money.toCents(debt.amount)
      val paid = minOf(amount, maxOf(0L, paidPool))
      paidPool -= paid
      val remaining = amount - paid
      val status = when {
        remaining <= 0L -> DebtStatus.PAID
        debt.dueDate != null && debt.dueDate < now -> DebtStatus.OVERDUE
        paid > 0L -> DebtStatus.PARTIALLY_PAID
        else -> DebtStatus.UNPAID
      }
      DebtWithStatus(
        debt = debt,
        paidAmount = Money.fromCents(paid),
        remainingAmount = Money.fromCents(remaining),
        status = status
      )
    }
  }

  fun customerBalance(
    customer: Customer,
    debts: List<Debt>,
    collections: List<CollectionRecord>,
    now: Long = System.currentTimeMillis()
  ): CustomerWithBalance {
    val activeDebts = debts.filter { !it.isDeleted }
    val activeCollections = collections.filter { !it.isDeleted }

    val totalDebt = activeDebts.sumOf { Money.toCents(it.amount) }
    val totalPaid = activeCollections.sumOf { Money.toCents(it.amount) }
    val statuses = allocate(activeDebts, activeCollections, now)

    val overdue = statuses.filter { it.status == DebtStatus.OVERDUE }
    val open = statuses.filter { it.remainingAmount > 0.0 && it.debt.dueDate != null }

    val lastCollection = activeCollections.maxWithOrNull(compareBy<CollectionRecord> { it.date }.thenBy { it.id })
    val lastActivity = listOfNotNull(activeDebts.maxOfOrNull { it.date }, lastCollection?.date).maxOrNull()

    return CustomerWithBalance(
      customer = customer,
      totalDebt = Money.fromCents(totalDebt),
      totalPaid = Money.fromCents(totalPaid),
      remainingBalance = Money.fromCents(totalDebt - totalPaid),
      overdueDebtCount = overdue.size,
      lastTransactionDate = lastActivity,
      overdueAmount = Money.fromCents(overdue.sumOf { Money.toCents(it.remainingAmount) }),
      nextDueDate = open.minOfOrNull { it.debt.dueDate!! },
      lastPaymentAmount = lastCollection?.amount,
      lastPaymentDate = lastCollection?.date
    )
  }

  /** Tüm müşteriler için bakiye ve borç durumları (tek geçişte, müşteriye göre gruplanarak). */
  fun buildAll(
    customers: List<Customer>,
    debts: List<Debt>,
    collections: List<CollectionRecord>,
    now: Long = System.currentTimeMillis()
  ): Pair<List<CustomerWithBalance>, List<DebtWithStatus>> {
    val debtsByCustomer = debts.filter { !it.isDeleted }.groupBy { it.customerId }
    val collsByCustomer = collections.filter { !it.isDeleted }.groupBy { it.customerId }
    val balances = ArrayList<CustomerWithBalance>(customers.size)
    val statuses = ArrayList<DebtWithStatus>()
    for (c in customers) {
      val d = debtsByCustomer[c.id].orEmpty()
      val col = collsByCustomer[c.id].orEmpty()
      balances += customerBalance(c, d, col, now)
      statuses += allocate(d, col, now)
    }
    return balances to statuses
  }
}
