package com.example.cloud

import java.util.Calendar

/**
 * Uygun randevu saatlerinin hesaplanması (saf fonksiyonlar, test edilebilir).
 *
 * Takvim 15 dakikalık bloklara bölünür. Bir randevu, kapladığı her blok için bir
 * "kilit" kaydı oluşturur; aynı bloğa ikinci kilit sunucuda reddedilir. Böylece iki
 * müşteri aynı saate randevu alamaz.
 */
object Slots {
  const val BLOCK_MINUTES = 15

  /** Bir randevunun kapladığı blokların anahtarları: "202610251430" gibi. */
  fun blockKeys(start: Long, durationMinutes: Int): List<String> {
    val blocks = ((durationMinutes + BLOCK_MINUTES - 1) / BLOCK_MINUTES).coerceAtLeast(1)
    return (0 until blocks).map { i -> blockKey(start + i * BLOCK_MINUTES * 60_000L) }
  }

  /** Personelli işletmede kilit anahtarı personel önekiyle tutulur: "{personel}_202610251430". */
  fun lockKeys(start: Long, durationMinutes: Int, staffId: String): List<String> =
    blockKeys(start, durationMinutes).map { if (staffId.isBlank()) it else "${staffId}_$it" }

  /**
   * Blok, [staffId] için dolu mu? Öneksiz (işletme geneli) kilitler herkesi kapatır;
   * personelsiz bakışta ([staffId] boş) herhangi bir kilit bloğu kapatır.
   */
  fun isTaken(block: String, staffId: String, taken: Set<String>): Boolean =
    block in taken || (staffId.isNotBlank() && "${staffId}_$block" in taken) ||
      (staffId.isBlank() && taken.any { it.endsWith("_$block") })

  fun blockKey(timestamp: Long): String {
    val c = Calendar.getInstance().apply { timeInMillis = timestamp }
    return "%04d%02d%02d%02d%02d".format(
      c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH),
      c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE)
    )
  }

  fun atMinute(dayStart: Long, minuteOfDay: Int): Long = Calendar.getInstance().apply {
    timeInMillis = dayStart
    set(Calendar.HOUR_OF_DAY, minuteOfDay / 60)
    set(Calendar.MINUTE, minuteOfDay % 60)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
  }.timeInMillis

  /**
   * [dayStart] gününde [durationMinutes] süreli bir hizmet için boş başlangıç saatleri.
   * [takenBlocks] o gün dolu olan blok anahtarları. Geçmiş saatler ve [minNoticeMinutes]
   * içindeki saatler verilmez; öğle arasına taşan randevular verilmez.
   */
  fun available(
    day: WorkingDay,
    dayStart: Long,
    durationMinutes: Int,
    takenBlocks: Set<String>,
    now: Long,
    minNoticeMinutes: Int = 0,
    stepMinutes: Int = BLOCK_MINUTES,
    staffId: String = ""
  ): List<Long> {
    if (!day.isOpen || durationMinutes <= 0) return emptyList()
    val result = mutableListOf<Long>()
    var m = day.openMinute
    while (m + durationMinutes <= day.closeMinute) {
      val endM = m + durationMinutes
      val overlapsBreak = day.breakStart != null && day.breakEnd != null &&
        m < day.breakEnd && endM > day.breakStart
      val start = atMinute(dayStart, m)
      if (!overlapsBreak && start >= now + minNoticeMinutes * 60_000L) {
        val keys = blockKeys(start, durationMinutes)
        if (keys.none { isTaken(it, staffId, takenBlocks) }) result += start
      }
      m += stepMinutes
    }
    return result
  }
}
