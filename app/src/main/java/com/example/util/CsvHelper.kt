package com.example.util

import com.example.model.Customer
import com.example.model.CustomerWithBalance
import com.example.model.Debt
import com.example.model.CollectionRecord

data class CsvImportResult(
  val validCustomers: List<Customer>,
  val errors: List<String>
)

object CsvHelper {
  private const val UTF8_BOM = "\uFEFF"

  fun exportCustomersToCsv(customers: List<CustomerWithBalance>): String {
    val sb = StringBuilder()
    sb.append(UTF8_BOM)
    // Excel in Turkish locale prefers ';' as delimiter
    sb.append("Müşteri ID;Ad Soyad / İşletme;Telefon;E-posta;Adres;Toplam Borç;Toplam Tahsilat;Kalan Bakiye;Durum;Notlar\n")

    for (item in customers) {
      val c = item.customer
      val status = if (c.isArchived) "Arşivlendi" else if (item.remainingBalance > 0) "Borçlu" else "Bakiyesi Yok"
      sb.append("${c.id};")
      sb.append("\"${escapeCsv(c.name)}\";")
      sb.append("\"${escapeCsv(c.phone)}\";")
      sb.append("\"${escapeCsv(c.email)}\";")
      sb.append("\"${escapeCsv(c.address)}\";")
      sb.append("${item.totalDebt};")
      sb.append("${item.totalPaid};")
      sb.append("${item.remainingBalance};")
      sb.append("\"$status\";")
      sb.append("\"${escapeCsv(c.notes)}\"\n")
    }

    return sb.toString()
  }

  fun exportAllTransactionsToCsv(
    customers: Map<Long, Customer>,
    debts: List<Debt>,
    collections: List<CollectionRecord>
  ): String {
    val sb = StringBuilder()
    sb.append(UTF8_BOM)
    sb.append("İşlem Türü;İşlem ID;Müşteri Adı;Tutar;Tarih;Vade Tarihi;Kategori / Yöntem;Açıklama / Not\n")

    for (d in debts) {
      val cName = customers[d.customerId]?.name ?: "Bilinmeyen Müşteri"
      sb.append("Borç;${d.id};\"${escapeCsv(cName)}\";${d.amount};")
      sb.append("${Formatters.formatDate(d.date)};${Formatters.formatDate(d.dueDate)};")
      sb.append("\"${escapeCsv(d.category)}\";\"${escapeCsv(d.description)}\"\n")
    }

    for (c in collections) {
      val cName = customers[c.customerId]?.name ?: "Bilinmeyen Müşteri"
      sb.append("Tahsilat;${c.id};\"${escapeCsv(cName)}\";${c.amount};")
      sb.append("${Formatters.formatDate(c.date)};-;\"${escapeCsv(c.paymentMethod.label)}\";\"${escapeCsv(c.notes)}\"\n")
    }

    return sb.toString()
  }

  fun parseCustomerCsv(csvContent: String): CsvImportResult {
    val validList = mutableListOf<Customer>()
    val errorList = mutableListOf<String>()

    val cleanContent = csvContent.removePrefix(UTF8_BOM)
    val lines = cleanContent.lines().filter { it.isNotBlank() }

    if (lines.isEmpty()) {
      return CsvImportResult(emptyList(), listOf("CSV dosyası boş."))
    }

    // Determine delimiter (semicolon or comma)
    val firstLine = lines.first()
    val delimiter = if (firstLine.contains(";")) ";" else ","

    // Skip header line if detected
    val startIndex = if (firstLine.contains("Ad", ignoreCase = true) || firstLine.contains("İsim", ignoreCase = true) || firstLine.contains("Telefon", ignoreCase = true)) 1 else 0

    for (i in startIndex until lines.size) {
      val lineNum = i + 1
      val line = lines[i]
      val tokens = line.split(delimiter).map { it.trim().removeSurrounding("\"") }

      if (tokens.isEmpty() || tokens.all { it.isBlank() }) continue

      val name = tokens.getOrNull(0) ?: ""
      val phone = tokens.getOrNull(1) ?: ""
      val email = tokens.getOrNull(2) ?: ""
      val address = tokens.getOrNull(3) ?: ""
      val notes = tokens.getOrNull(4) ?: ""

      if (name.isBlank()) {
        errorList.add("Satır $lineNum: Müşteri adı/işletme adı boş olamaz.")
        continue
      }

      val cleanPhone = Formatters.cleanPhoneNumber(phone)
      if (cleanPhone.length < 10) {
        errorList.add("Satır $lineNum: '$name' için telefon numarası geçersiz ($phone). En az 10 hane olmalıdır.")
        continue
      }

      validList.add(
        Customer(
          name = name,
          phone = cleanPhone,
          email = email,
          address = address,
          notes = notes
        )
      )
    }

    return CsvImportResult(validList, errorList)
  }

  private fun escapeCsv(text: String): String {
    return text.replace("\"", "\"\"").replace("\n", " ")
  }
}
