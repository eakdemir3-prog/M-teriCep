package com.example.util

import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Para hesapları için yardımcılar. Tutarlar veritabanında ondalık (₺) olarak durur ama
 * tüm toplama/çıkarma işlemleri kuruş (Long) üzerinden yapılır; böylece kayan nokta
 * yuvarlama hataları (0,1 + 0,2 = 0,30000000000000004 gibi) bakiyeye yansımaz.
 */
object Money {
  /** ₺ tutarı kuruşa çevirir (yarım kuruş yukarı yuvarlanır). */
  fun toCents(amount: Double): Long = (amount * 100.0).roundToLong()

  fun fromCents(cents: Long): Double = cents / 100.0

  /** Tutarı 2 ondalığa yuvarlar. */
  fun round(amount: Double): Double = fromCents(toCents(amount))

  fun sum(amounts: Iterable<Double>): Double = fromCents(amounts.sumOf { toCents(it) })

  fun isZero(amount: Double): Boolean = abs(toCents(amount)) == 0L

  /**
   * Kullanıcının yazdığı tutarı çözer. Türkçe ve İngilizce yazımların hepsini kabul eder:
   * "1.250,50"  "1250,50"  "1250.50"  "1,250.50"  "1.250"  "₺ 2.500"  "2500 TL".
   * Geçersiz, sıfır veya negatif değerler için null döner.
   */
  fun parse(raw: String): Double? {
    var s = raw.trim()
      .replace("₺", "")
      .replace("TL", "", ignoreCase = true)
      .replace(" ", "")
      .replace(" ", "")
    if (s.isEmpty()) return null
    if (s.any { !it.isDigit() && it != '.' && it != ',' }) return null

    val lastDot = s.lastIndexOf('.')
    val lastComma = s.lastIndexOf(',')

    s = when {
      lastDot >= 0 && lastComma >= 0 -> {
        // İkisi de var: en sondaki ondalık ayırıcıdır.
        if (lastComma > lastDot) s.replace(".", "").replace(',', '.')
        else s.replace(",", "")
      }
      lastComma >= 0 -> {
        // Tek virgül Türkçede ondalıktır ("250,50"); birden fazla virgül binliktir ("1,250,000").
        if (s.count { it == ',' } == 1) s.replace(',', '.') else s.replace(",", "")
      }
      lastDot >= 0 -> {
        val parts = s.split('.')
        // "1.250" veya "1.250.000" -> binlik ayırıcı (Türkçe yazım)
        val thousands = parts.size > 2 || (parts.size == 2 && parts[1].length == 3)
        if (thousands && parts.drop(1).all { it.length == 3 }) s.replace(".", "") else if (parts.size == 2) s else return null
      }
      else -> s
    }

    val value = s.toDoubleOrNull() ?: return null
    if (value.isNaN() || value.isInfinite()) return null
    val rounded = round(value)
    return if (rounded > 0.0) rounded else null
  }

  /** Bir tutarı düzenleme alanına yazmak için Türkçe biçimde verir: 2500 -> "2500", 12.5 -> "12,50". */
  fun toInput(amount: Double): String {
    val cents = toCents(amount)
    val whole = cents / 100
    val frac = abs(cents % 100)
    return if (frac == 0L) whole.toString() else "$whole," + frac.toString().padStart(2, '0')
  }
}
