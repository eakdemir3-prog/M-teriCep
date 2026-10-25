package com.example.ui.cloud

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cloud.AppointmentStatus
import com.example.cloud.BusinessProfile
import com.example.cloud.WorkingDay
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LocalAppPalette
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.RoseError
import com.example.ui.theme.Slate500
import com.example.util.Formatters
import java.util.Calendar

/** Yatay gün seçici (bugünden itibaren [days] gün). Kapalı günler soluk görünür. */
@Composable
fun DayStrip(
  selectedDay: Long,
  onSelect: (Long) -> Unit,
  days: Int = 30,
  business: BusinessProfile? = null,
  startOffset: Int = 0,
  badgeCounts: Map<Long, Int> = emptyMap()
) {
  val today = Formatters.startOfDay(System.currentTimeMillis())
  val list = remember(today, days, startOffset) {
    (startOffset until startOffset + days).map { i ->
      Calendar.getInstance().apply { timeInMillis = today; add(Calendar.DAY_OF_MONTH, i) }.timeInMillis
    }
  }
  val palette = LocalAppPalette.current
  val state = rememberLazyListState(initialFirstVisibleItemIndex = list.indexOf(selectedDay).coerceAtLeast(0))
  LazyRow(state = state, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    items(list) { day ->
      val cal = Calendar.getInstance().apply { timeInMillis = day }
      val dow = WorkingDay.fromCalendarDay(cal.get(Calendar.DAY_OF_WEEK))
      val open = business?.dayFor(day)?.isOpen ?: true
      val selected = day == selectedDay
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (selected) NavyPrimary else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (selected) NavyPrimary else palette.cardBorder),
        modifier = Modifier
          .width(56.dp)
          .clickable { onSelect(day) }
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.padding(vertical = 8.dp)
        ) {
          val content = if (selected) palette.onInk else if (open) MaterialTheme.colorScheme.onSurface else Slate500.copy(alpha = 0.5f)
          Text(
            if (day == today) "Bugün" else WorkingDay.SHORT_DAY_NAMES[dow - 1],
            fontSize = 11.sp,
            color = content
          )
          Text(cal.get(Calendar.DAY_OF_MONTH).toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = content)
          val count = badgeCounts[day] ?: 0
          Text(
            if (count > 0) "$count" else if (!open) "kapalı" else " ",
            fontSize = 10.sp,
            color = if (selected) palette.onInk else if (count > 0) AmberWarning else Slate500
          )
        }
      }
    }
  }
}

/** Boş saatlerin ızgarası. [slots] null ise yükleniyor. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SlotGrid(slots: List<Long>?, selected: Long?, onSelect: (Long) -> Unit, emptyText: String) {
  val palette = LocalAppPalette.current
  when {
    slots == null -> Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center) {
      CircularProgressIndicator()
    }
    slots.isEmpty() -> Text(emptyText, style = MaterialTheme.typography.bodySmall, color = Slate500, modifier = Modifier.padding(vertical = 8.dp))
    else -> FlowRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      slots.forEach { t ->
        val isSel = t == selected
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = if (isSel) EmeraldPrimary else MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, if (isSel) EmeraldPrimary else palette.cardBorder),
          modifier = Modifier.clickable { onSelect(t) }
        ) {
          Text(
            text = timeOf(t),
            fontWeight = FontWeight.SemiBold,
            color = if (isSel) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
          )
        }
      }
    }
  }
}

@Composable
fun AppointmentStatusChip(status: AppointmentStatus) {
  val color: Color = when (status) {
    AppointmentStatus.PENDING -> AmberWarning
    AppointmentStatus.CONFIRMED -> EmeraldPrimary
    AppointmentStatus.COMPLETED -> NavyPrimary
    AppointmentStatus.CANCELLED, AppointmentStatus.REJECTED, AppointmentStatus.NO_SHOW -> RoseError
  }
  Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.14f)) {
    Text(
      status.label,
      color = color,
      fontSize = 11.sp,
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
    )
  }
}

fun timeOf(t: Long): String {
  val c = Calendar.getInstance().apply { timeInMillis = t }
  return "%02d:%02d".format(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
}

fun durationText(minutes: Int): String = when {
  minutes < 60 -> "$minutes dk"
  minutes % 60 == 0 -> "${minutes / 60} saat"
  else -> "${minutes / 60} sa ${minutes % 60} dk"
}

@Composable
fun SectionTitle(text: String, color: Color = MaterialTheme.colorScheme.onBackground) {
  Spacer(modifier = Modifier.height(4.dp))
  Text(text, style = MaterialTheme.typography.titleMedium, color = color)
}
