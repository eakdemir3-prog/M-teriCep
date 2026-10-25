package com.example

import com.example.cloud.Slots
import com.example.cloud.WorkingDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class SlotsTest {

  private val dayStart: Long = Calendar.getInstance().apply {
    set(2030, Calendar.MARCH, 4, 0, 0, 0) // gelecekte bir pazartesi
    set(Calendar.MILLISECOND, 0)
  }.timeInMillis
  private val past = dayStart - 86_400_000L
  private val day = WorkingDay(dayOfWeek = 1, isOpen = true, openMinute = 9 * 60, closeMinute = 12 * 60)

  @Test
  fun `bos gunde acilistan kapanisa kadar saatler verilir`() {
    val slots = Slots.available(day, dayStart, 30, emptySet(), now = past)
    // 09:00 .. 11:30 arası 15 dk adımla: 11 başlangıç
    assertEquals(11, slots.size)
    assertEquals(Slots.atMinute(dayStart, 9 * 60), slots.first())
    assertEquals(Slots.atMinute(dayStart, 11 * 60 + 30), slots.last())
  }

  @Test
  fun `dolu bloklar ve cakisan saatler verilmez`() {
    val taken = Slots.blockKeys(Slots.atMinute(dayStart, 10 * 60), 30).toSet() // 10:00-10:30 dolu
    val slots = Slots.available(day, dayStart, 30, taken, now = past)
    assertFalse(slots.contains(Slots.atMinute(dayStart, 9 * 60 + 45))) // 9:45-10:15 çakışır
    assertFalse(slots.contains(Slots.atMinute(dayStart, 10 * 60)))
    assertFalse(slots.contains(Slots.atMinute(dayStart, 10 * 60 + 15)))
    assertTrue(slots.contains(Slots.atMinute(dayStart, 9 * 60 + 30)))
    assertTrue(slots.contains(Slots.atMinute(dayStart, 10 * 60 + 30)))
  }

  @Test
  fun `ogle arasina tasan randevu verilmez`() {
    val withBreak = day.copy(closeMinute = 14 * 60, breakStart = 12 * 60, breakEnd = 13 * 60)
    val slots = Slots.available(withBreak, dayStart, 60, emptySet(), now = past)
    assertFalse(slots.contains(Slots.atMinute(dayStart, 11 * 60 + 30)))
    assertFalse(slots.contains(Slots.atMinute(dayStart, 12 * 60)))
    assertTrue(slots.contains(Slots.atMinute(dayStart, 11 * 60)))
    assertTrue(slots.contains(Slots.atMinute(dayStart, 13 * 60)))
  }

  @Test
  fun `gecmis saatler ve kapali gun verilmez`() {
    val now = Slots.atMinute(dayStart, 10 * 60 + 5)
    val slots = Slots.available(day, dayStart, 30, emptySet(), now = now, minNoticeMinutes = 30)
    assertTrue(slots.all { it >= now + 30 * 60_000L })
    assertTrue(Slots.available(day.copy(isOpen = false), dayStart, 30, emptySet(), now = past).isEmpty())
  }

  @Test
  fun `45 dakikalik hizmet 3 blok kaplar`() {
    assertEquals(3, Slots.blockKeys(dayStart, 45).size)
    assertEquals(1, Slots.blockKeys(dayStart, 10).size)
  }
}
