package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Formatters {
  private val turkishLocale = Locale("tr", "TR")

  private val currencyFormat: DecimalFormat by lazy {
    val symbols = DecimalFormatSymbols(turkishLocale).apply {
      groupingSeparator = '.'
      decimalSeparator = ','
    }
    DecimalFormat("#,##0.00 ₺", symbols)
  }

  private val dateFormat = SimpleDateFormat("dd.MM.yyyy", turkishLocale)
  private val dateTimeFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", turkishLocale)
  private val dayMonthFormat = SimpleDateFormat("d MMMM yyyy", turkishLocale)

  fun formatCurrency(amount: Double): String {
    return currencyFormat.format(amount)
  }

  fun formatDate(timestamp: Long?): String {
    if (timestamp == null || timestamp == 0L) return "-"
    return dateFormat.format(Date(timestamp))
  }

  fun formatDateTime(timestamp: Long?): String {
    if (timestamp == null || timestamp == 0L) return "-"
    return dateTimeFormat.format(Date(timestamp))
  }

  fun formatFriendlyDate(timestamp: Long?): String {
    if (timestamp == null || timestamp == 0L) return "-"
    return dayMonthFormat.format(Date(timestamp))
  }

  fun cleanPhoneNumber(rawPhone: String): String {
    val digits = rawPhone.filter { it.isDigit() }
    return when {
      digits.startsWith("90") && digits.length >= 12 -> digits
      digits.startsWith("0") && digits.length == 11 -> "90" + digits.substring(1)
      digits.length == 10 -> "90$digits"
      else -> digits
    }
  }

  fun formatPhoneForDisplay(rawPhone: String): String {
    val digits = rawPhone.filter { it.isDigit() }
    val clean = if (digits.startsWith("90")) digits.substring(2) else if (digits.startsWith("0")) digits.substring(1) else digits
    if (clean.length == 10) {
      return "0 (${clean.substring(0, 3)}) ${clean.substring(3, 6)} ${clean.substring(6, 8)} ${clean.substring(8, 10)}"
    }
    return rawPhone
  }
}
