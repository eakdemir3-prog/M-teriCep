package com.example

import com.example.model.CollectionRecord
import com.example.model.Customer
import com.example.model.Debt
import com.example.model.DebtStatus
import com.example.util.CsvHelper
import com.example.util.Formatters
import com.example.util.Ledger
import com.example.util.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LedgerAndMoneyTest {

  private val day = 86_400_000L
  private val now = 1_800_000_000_000L
  private val customer = Customer(id = 1, name = "Test", phone = "05321234567")

  private fun debt(id: Long, amount: Double, daysAgo: Int, dueInDays: Int?) = Debt(
    id = id, customerId = 1, amount = amount, date = now - daysAgo * day,
    dueDate = dueInDays?.let { now + it * day }
  )

  private fun pay(id: Long, amount: Double, daysAgo: Int = 0) = CollectionRecord(
    id = id, customerId = 1, amount = amount, date = now - daysAgo * day
  )

  // ---------------- Tutar girişi ----------------

  @Test
  fun `turkce ve ingilizce tutar yazimlari dogru okunur`() {
    assertEquals(1250.50, Money.parse("1.250,50")!!, 0.0)
    assertEquals(1250.50, Money.parse("1250,50")!!, 0.0)
    assertEquals(1250.50, Money.parse("1250.50")!!, 0.0)
    assertEquals(1250.50, Money.parse("1,250.50")!!, 0.0)
    assertEquals(1250.0, Money.parse("1.250")!!, 0.0)
    assertEquals(1_250_000.0, Money.parse("1.250.000")!!, 0.0)
    assertEquals(2500.0, Money.parse("₺ 2.500")!!, 0.0)
    assertEquals(2500.0, Money.parse("2500 TL")!!, 0.0)
    assertEquals(12.5, Money.parse("12,5")!!, 0.0)
  }

  @Test
  fun `gecersiz tutarlar reddedilir`() {
    assertNull(Money.parse(""))
    assertNull(Money.parse("abc"))
    assertNull(Money.parse("0"))
    assertNull(Money.parse("-50"))
    assertNull(Money.parse("1.2.3"))
  }

  @Test
  fun `kurus hesabi kayan nokta hatasi yapmaz`() {
    assertEquals(0.3, Money.sum(listOf(0.1, 0.2)), 0.0)
    assertEquals("12,50", Money.toInput(12.5))
    assertEquals("2500", Money.toInput(2500.0))
    assertTrue(Formatters.formatCurrency(1250.5).contains("1.250,50"))
  }

  // ---------------- Borç dağıtımı ----------------

  @Test
  fun `odemeler en eski borca once dusulur`() {
    val debts = listOf(
      debt(1, 4500.0, daysAgo = 15, dueInDays = -5), // eski, vadesi geçmiş
      debt(2, 1800.0, daysAgo = 2, dueInDays = 7) // yeni
    )
    val result = Ledger.allocate(debts, listOf(pay(1, 2000.0)), now)
    val old = result.first { it.debt.id == 1L }
    val new = result.first { it.debt.id == 2L }
    assertEquals(2500.0, old.remainingAmount, 0.0)
    assertEquals(DebtStatus.OVERDUE, old.status)
    assertEquals(1800.0, new.remainingAmount, 0.0)
    assertEquals(DebtStatus.UNPAID, new.status)
  }

  @Test
  fun `kismi odeme ve tam odeme durumlari`() {
    val debts = listOf(debt(1, 1000.0, 10, 5), debt(2, 1000.0, 5, 10))
    val result = Ledger.allocate(debts, listOf(pay(1, 600.0), pay(2, 600.0)), now)
    assertEquals(DebtStatus.PAID, result[0].status)
    assertEquals(DebtStatus.PARTIALLY_PAID, result[1].status)
    assertEquals(800.0, result[1].remainingAmount, 0.0)
  }

  @Test
  fun `odenmis borc vadesi gecse de gecikmis sayilmaz`() {
    val balance = Ledger.customerBalance(customer, listOf(debt(1, 3200.0, 20, -10)), listOf(pay(1, 3200.0)), now)
    assertEquals(0.0, balance.remainingBalance, 0.0)
    assertEquals(0.0, balance.overdueAmount, 0.0)
    assertEquals(0, balance.overdueDebtCount)
  }

  @Test
  fun `bakiye ve gecikmis tutar dogru hesaplanir`() {
    val debts = listOf(debt(1, 4500.0, 15, -5), debt(2, 1800.0, 2, 7))
    val b = Ledger.customerBalance(customer, debts, listOf(pay(1, 2000.0, daysAgo = 3)), now)
    assertEquals(6300.0, b.totalDebt, 0.0)
    assertEquals(2000.0, b.totalPaid, 0.0)
    assertEquals(4300.0, b.remainingBalance, 0.0)
    assertEquals(2500.0, b.overdueAmount, 0.0)
    // En yakın vade: ödenmemiş en eski vade (gecikmiş olan)
    assertEquals(now - 5 * day, b.nextDueDate)
    assertEquals(2000.0, b.lastPaymentAmount!!, 0.0)
  }

  @Test
  fun `silinen kayitlar hesaba katilmaz`() {
    val debts = listOf(debt(1, 1000.0, 5, null), debt(2, 500.0, 3, null).copy(isDeleted = true))
    val colls = listOf(pay(1, 200.0), pay(2, 1000.0).copy(isDeleted = true))
    val b = Ledger.customerBalance(customer, debts, colls, now)
    assertEquals(800.0, b.remainingBalance, 0.0)
  }

  @Test
  fun `fazla odeme eksi bakiye yani avans olur`() {
    val b = Ledger.customerBalance(customer, listOf(debt(1, 1000.0, 5, null)), listOf(pay(1, 1200.0)), now)
    assertEquals(-200.0, b.remainingBalance, 0.0)
    assertEquals(DebtStatus.PAID, Ledger.allocate(listOf(debt(1, 1000.0, 5, null)), listOf(pay(1, 1200.0)), now)[0].status)
  }

  // ---------------- CSV ----------------

  @Test
  fun `csv tirnak icindeki noktali virgulu bolmez ve tekrar edenleri atlar`() {
    val csv = "Ad Soyad;Telefon;E-posta;Adres;Not\n" +
      "\"Demir Oto\";\"0543 111 22 33\";\"\";\"Sanayi Sit.; 3. Blok\";\"Cuma\"\n" +
      "Ayşe;05051112233;;;\n" +
      "Ayşe Tekrar;0505 111 22 33;;;\n" +
      "Var Olan;0532 999 88 77;;;\n"
    val result = CsvHelper.parseCustomerCsv(csv, existingPhones = setOf("905329998877"))
    assertEquals(2, result.validCustomers.size)
    assertEquals("Sanayi Sit.; 3. Blok", result.validCustomers[0].address)
    assertEquals(2, result.skippedDuplicates.size)
  }

  @Test
  fun `csv disa aktarimda tutar excel bicimindedir`() {
    assertEquals("4500,00", CsvHelper.amount(4500.0))
    assertEquals("12,50", CsvHelper.amount(12.5))
    assertEquals("-200,00", CsvHelper.amount(-200.0))
  }

  @Test
  fun `hafta baslangici pazartesidir`() {
    val start = Formatters.startOfWeek(now)
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = start }
    assertEquals(java.util.Calendar.MONDAY, cal.get(java.util.Calendar.DAY_OF_WEEK))
    assertTrue(start <= now)
    assertTrue(now - start < 7 * day)
  }

  @Test
  fun `ay baslangici haftadan bagimsiz hesaplanir`() {
    val start = Formatters.startOfMonth(now)
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = start }
    val nowCal = java.util.Calendar.getInstance().apply { timeInMillis = now }
    assertEquals(1, cal.get(java.util.Calendar.DAY_OF_MONTH))
    assertEquals(nowCal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.MONTH))
  }
}
