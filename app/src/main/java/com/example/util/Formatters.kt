package com.example.util

import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Formatters {
  private val turkishLocale = Locale("tr", "TR")

  // SimpleDateFormat/DecimalFormat iş parçacığı güvenli değil; arka plan hatırlatıcısı da
  // kullandığı için her iş parçacığına ayrı nesne verilir.
  private val currencyFormat = ThreadLocal.withInitial {
    val symbols = DecimalFormatSymbols(turkishLocale).apply {
      groupingSeparator = '.'
      decimalSeparator = ','
    }
    DecimalFormat("#,##0.00 ₺", symbols).apply { roundingMode = RoundingMode.HALF_UP }
  }
  private val dateFormat = ThreadLocal.withInitial { SimpleDateFormat("dd.MM.yyyy", turkishLocale) }
  private val dateTimeFormat = ThreadLocal.withInitial { SimpleDateFormat("dd.MM.yyyy HH:mm", turkishLocale) }
  private val dayMonthFormat = ThreadLocal.withInitial { SimpleDateFormat("d MMMM yyyy", turkishLocale) }
  private val shortDayMonth = ThreadLocal.withInitial { SimpleDateFormat("d MMM", turkishLocale) }

  fun formatCurrency(amount: Double): String {
    val v = Money.round(amount)
    return currencyFormat.get()!!.format(if (Money.isZero(v)) 0.0 else v)
  }

  /** Dar alanlar için: kuruşu yoksa ondalıksız ("3.200 ₺"), varsa tam ("3.200,50 ₺"). */
  fun formatCurrencyShort(amount: Double): String {
    val full = formatCurrency(amount)
    return full.replace(",00 ₺", " ₺")
  }

  fun formatDate(timestamp: Long?): String {
    if (timestamp == null || timestamp == 0L) return "-"
    return dateFormat.get()!!.format(Date(timestamp))
  }

  fun formatDateTime(timestamp: Long?): String {
    if (timestamp == null || timestamp == 0L) return "-"
    return dateTimeFormat.get()!!.format(Date(timestamp))
  }

  fun formatFriendlyDate(timestamp: Long?): String {
    if (timestamp == null || timestamp == 0L) return "-"
    return dayMonthFormat.get()!!.format(Date(timestamp))
  }

  fun formatShortDate(timestamp: Long?): String {
    if (timestamp == null || timestamp == 0L) return "-"
    return shortDayMonth.get()!!.format(Date(timestamp))
  }

  /** "Bugün", "Yarın", "3 gün kaldı", "5 gün gecikti" gibi vade açıklaması. */
  fun dueLabel(dueDate: Long?, now: Long = System.currentTimeMillis()): String {
    if (dueDate == null) return "Vade yok"
    val days = daysBetween(now, dueDate)
    return when {
      days == 0 -> if (dueDate < now) "Bugün (vadesi geldi)" else "Bugün"
      days == 1 -> "Yarın"
      days > 1 -> "$days gün kaldı"
      days == -1 -> "1 gün gecikti"
      else -> "${-days} gün gecikti"
    }
  }

  /** İki zaman arasındaki takvim günü farkı (saatten bağımsız). */
  fun daysBetween(from: Long, to: Long): Int {
    val a = startOfDay(from)
    val b = startOfDay(to)
    return Math.round((b - a) / 86_400_000.0).toInt()
  }

  fun startOfDay(timestamp: Long): Long = Calendar.getInstance().apply {
    timeInMillis = timestamp
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
  }.timeInMillis

  /** Haftanın başı: Türkiye'de pazartesi. */
  fun startOfWeek(timestamp: Long): Long = Calendar.getInstance().apply {
    timeInMillis = startOfDay(timestamp)
    firstDayOfWeek = Calendar.MONDAY
    val diff = (get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
    add(Calendar.DAY_OF_MONTH, -diff)
  }.timeInMillis

  fun startOfMonth(timestamp: Long): Long = Calendar.getInstance().apply {
    timeInMillis = startOfDay(timestamp)
    set(Calendar.DAY_OF_MONTH, 1)
  }.timeInMillis

  /**
   * Telefonu WhatsApp'ın beklediği uluslararası biçime çevirir (Türkiye için 90 ile başlar).
   * "0532 123 45 67", "+90 532...", "532..." hepsi "905321234567" olur.
   */
  fun cleanPhoneNumber(rawPhone: String): String {
    val digits = rawPhone.filter { it.isDigit() }
    return when {
      digits.startsWith("0090") -> digits.substring(2)
      digits.startsWith("90") && digits.length == 12 -> digits
      digits.startsWith("0") && digits.length == 11 -> "90" + digits.substring(1)
      digits.length == 10 && digits.startsWith("5") -> "90$digits"
      digits.length == 10 -> "90$digits"
      else -> digits
    }
  }

  fun formatPhoneForDisplay(rawPhone: String): String {
    val digits = rawPhone.filter { it.isDigit() }
    val clean = when {
      digits.startsWith("90") && digits.length == 12 -> digits.substring(2)
      digits.startsWith("0") && digits.length == 11 -> digits.substring(1)
      else -> digits
    }
    if (clean.length == 10) {
      return "0 (${clean.substring(0, 3)}) ${clean.substring(3, 6)} ${clean.substring(6, 8)} ${clean.substring(8, 10)}"
    }
    return rawPhone
  }

  /** Dar alanlar için: "0532 123 45 67". */
  fun formatPhoneCompact(rawPhone: String): String {
    val digits = rawPhone.filter { it.isDigit() }
    val clean = when {
      digits.startsWith("90") && digits.length == 12 -> digits.substring(2)
      digits.startsWith("0") && digits.length == 11 -> digits.substring(1)
      else -> digits
    }
    return if (clean.length == 10) "0${clean.substring(0, 3)} ${clean.substring(3, 6)} ${clean.substring(6, 8)} ${clean.substring(8)}" else rawPhone
  }

  fun isValidEmail(email: String): Boolean =
    email.isBlank() || Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$").matches(email.trim())
}
