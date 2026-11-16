package com.example

import com.example.cloud.BusinessProfile
import com.example.cloud.WorkingDay
import com.example.cloud.dayKeyOf
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ClosedDatesTest {
  @Test
  fun closedDateIsNotOpen() {
    val c = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 18, 10, 0, 0) } // Çarşamba
    val open = BusinessProfile(id = "b", ownerUid = "b", name = "X", workingHours = WorkingDay.defaultWeek())
    assertTrue(open.dayFor(c.timeInMillis).isOpen)
    val closed = open.copy(closedDates = listOf(dayKeyOf(c.timeInMillis)))
    assertFalse(closed.dayFor(c.timeInMillis).isOpen)
    // Ertesi gün etkilenmez
    c.add(Calendar.DAY_OF_MONTH, 1)
    assertTrue(closed.dayFor(c.timeInMillis).isOpen)
  }
}
