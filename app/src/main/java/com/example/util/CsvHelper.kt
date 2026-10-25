package com.example.util

import com.example.model.CollectionRecord
import com.example.model.Customer
import com.example.model.CustomerWithBalance
import com.example.model.DebtWithStatus

data class CsvImportResult(
  val validCustomers: List<Customer>,
  val errors: List<String>,
  /** Uygulamada zaten kayıtlı olduğu için atlanacak satırlar. */
  val skippedDuplicates: List<String> = emptyList()
)

/**
 * Excel uyumlu CSV: Türkçe Excel noktalı virgül (;) ayırıcı ve virgüllü ondalık bekler,
 * Türkçe karakterler için dosya başına UTF-8 BOM eklenir.
 */
object CsvHelper {
  private const val UTF8_BOM = "﻿"
  private const val SEP = ';'

  // ---------------- Dışa aktarma ----------------

  fun exportCustomersToCsv(customers: List<CustomerWithBalance>): String {
    val sb = StringBuilder(UTF8_BOM)
    sb.appendRow("Müşteri No", "Ad Soyad / İşletme", "Telefon", "E-posta", "Adres", "Toplam Borç", "Toplam Tahsilat", "Kalan Bakiye", "Vadesi Geçen", "En Yakın Vade", "Durum", "Notlar")
    for (item in customers) {
      val c = item.customer
      val status = when {
        c.isArchived -> "Arşivde"
        item.overdueAmount > 0 -> "Vadesi geçmiş"
        item.remainingBalance > 0 -> "Borçlu"
        item.remainingBalance < 0 -> "Alacaklı (avans)"
        else -> "Bakiye yok"
      }
      sb.appendRow(
        c.id.toString(), c.name, c.phone, c.email, c.address,
        amount(item.totalDebt), amount(item.totalPaid), amount(item.remainingBalance), amount(item.overdueAmount),
        Formatters.formatDate(item.nextDueDate), status, c.notes
      )
    }
    return sb.toString()
  }

  fun exportAllTransactionsToCsv(
    customers: Map<Long, Customer>,
    debts: List<DebtWithStatus>,
    collections: List<CollectionRecord>
  ): String {
    val sb = StringBuilder(UTF8_BOM)
    sb.appendRow("Tarih", "İşlem Türü", "Müşteri", "Tutar", "Kalan", "Vade Tarihi", "Durum", "Kategori / Yöntem", "Açıklama / Not", "Kayıt No")
    val rows = mutableListOf<Pair<Long, List<String>>>()
    for (d in debts) {
      val name = customers[d.debt.customerId]?.name ?: "Bilinmeyen müşteri"
      rows += d.debt.date to listOf(
        Formatters.formatDate(d.debt.date), "Borç", name, amount(d.debt.amount), amount(d.remainingAmount),
        Formatters.formatDate(d.debt.dueDate), d.status.label, d.debt.category, d.debt.description, "B-${d.debt.id}"
      )
    }
    for (c in collections) {
      val name = customers[c.customerId]?.name ?: "Bilinmeyen müşteri"
      rows += c.date to listOf(
        Formatters.formatDate(c.date), "Tahsilat", name, amount(c.amount), "", "", "", c.paymentMethod.label, c.notes, "T-${c.id}"
      )
    }
    rows.sortedByDescending { it.first }.forEach { sb.appendRow(*it.second.toTypedArray()) }
    return sb.toString()
  }

  /** 4500.5 -> "4500,50" (Türkçe Excel sayı olarak tanır). */
  fun amount(value: Double): String = Money.toInput(value).let { if (it.contains(',')) it else "$it,00" }.let {
    if (value < 0 && !it.startsWith("-")) "-$it" else it
  }

  private fun StringBuilder.appendRow(vararg cells: String) {
    cells.forEachIndexed { i, cell ->
      if (i > 0) append(SEP)
      append(quote(cell))
    }
    append("\r\n")
  }

  /** Hücreyi tırnaklar; Excel'de formül olarak çalıştırılmasın diye =,+,-,@ ile başlayan metinlerin başına ' koyar. */
  private fun quote(raw: String): String {
    var v = raw.replace("\r", " ").replace("\n", " ")
    val looksNumeric = v.matches(Regex("^-?[0-9]+(,[0-9]+)?$"))
    if (!looksNumeric && v.isNotEmpty() && v[0] in charArrayOf('=', '+', '-', '@', '\t')) v = "'$v"
    return if (looksNumeric) v else "\"" + v.replace("\"", "\"\"") + "\""
  }

  // ---------------- İçe aktarma ----------------

  /**
   * Beklenen sütunlar: Ad Soyad; Telefon; E-posta; Adres; Not (ilk iki zorunlu).
   * Ayırıcı ; veya , olabilir; tırnak içindeki ayırıcılar hücreyi bölmez.
   * [existingPhones] uygulamada zaten kayıtlı numaralar (temiz biçimde).
   */
  fun parseCustomerCsv(csvContent: String, existingPhones: Set<String> = emptySet()): CsvImportResult {
    val valid = mutableListOf<Customer>()
    val errors = mutableListOf<String>()
    val skipped = mutableListOf<String>()

    val content = csvContent.removePrefix(UTF8_BOM)
    if (content.isBlank()) return CsvImportResult(emptyList(), listOf("CSV dosyası boş."))

    val firstLine = content.lineSequence().firstOrNull { it.isNotBlank() } ?: ""
    val delimiter = if (firstLine.count { it == ';' } >= firstLine.count { it == ',' } && firstLine.contains(';')) ';' else if (firstLine.contains(',')) ',' else ';'
    val rows = tokenize(content, delimiter)
    if (rows.isEmpty()) return CsvImportResult(emptyList(), listOf("CSV dosyası boş."))

    val header = rows.first().map { it.trim().lowercase() }
    val hasHeader = header.any { it.contains("telefon") || it.startsWith("ad") || it.contains("isim") || it.contains("müşteri") }
    val start = if (hasHeader) 1 else 0

    val seenInFile = mutableSetOf<String>()
    for (i in start until rows.size) {
      val lineNo = i + 1
      val cells = rows[i].map { it.trim() }
      if (cells.all { it.isBlank() }) continue

      val name = cells.getOrElse(0) { "" }
      val rawPhone = cells.getOrElse(1) { "" }
      if (name.isBlank()) {
        errors += "Satır $lineNo: Müşteri adı boş."
        continue
      }
      val phone = Formatters.cleanPhoneNumber(rawPhone)
      if (phone.length < 10) {
        errors += "Satır $lineNo: '$name' için telefon geçersiz ($rawPhone)."
        continue
      }
      if (phone in existingPhones) {
        skipped += "Satır $lineNo: '$name' zaten kayıtlı (aynı telefon)."
        continue
      }
      if (!seenInFile.add(phone)) {
        skipped += "Satır $lineNo: '$name' dosyada iki kez geçiyor."
        continue
      }
      valid += Customer(
        name = name,
        phone = phone,
        email = cells.getOrElse(2) { "" },
        address = cells.getOrElse(3) { "" },
        notes = cells.getOrElse(4) { "" }
      )
    }
    return CsvImportResult(valid, errors, skipped)
  }

  /** RFC 4180 uyumlu basit CSV okuyucu (tırnak içi ayırıcı, "" kaçışı ve satır sonları). */
  fun tokenize(text: String, delimiter: Char): List<List<String>> {
    val rows = mutableListOf<List<String>>()
    var row = mutableListOf<String>()
    val cell = StringBuilder()
    var inQuotes = false
    var i = 0
    while (i < text.length) {
      val ch = text[i]
      if (inQuotes) {
        if (ch == '"') {
          if (i + 1 < text.length && text[i + 1] == '"') { cell.append('"'); i++ } else inQuotes = false
        } else cell.append(ch)
      } else {
        when (ch) {
          '"' -> inQuotes = true
          delimiter -> { row.add(cell.toString()); cell.setLength(0) }
          '\r' -> {}
          '\n' -> {
            row.add(cell.toString()); cell.setLength(0)
            rows.add(row); row = mutableListOf()
          }
          else -> cell.append(ch)
        }
      }
      i++
    }
    if (cell.isNotEmpty() || row.isNotEmpty()) {
      row.add(cell.toString())
      rows.add(row)
    }
    return rows.filter { r -> r.any { it.isNotBlank() } }
  }

  /** İçe aktarma için örnek şablon dosyası. */
  fun sampleTemplate(): String = UTF8_BOM +
    "Ad Soyad;Telefon;E-posta;Adres;Not\r\n" +
    "\"Ali Koç\";\"0532 111 22 33\";\"ali@ornek.com\";\"Kadıköy\";\"Cuma öder\"\r\n"
}
