package com.example.cloud

import java.util.Calendar

enum class UserRole { BUSINESS, CUSTOMER }

data class UserProfile(
  val uid: String,
  val role: UserRole?,
  val displayName: String = "",
  val email: String = "",
  val phone: String = "",
  val favoriteBusinessIds: List<String> = emptyList()
)

/** Haftanın bir günü için çalışma saatleri. dayOfWeek: 1 = Pazartesi ... 7 = Pazar. Saatler dakika cinsinden (09:00 = 540). */
data class WorkingDay(
  val dayOfWeek: Int,
  val isOpen: Boolean,
  val openMinute: Int = 9 * 60,
  val closeMinute: Int = 18 * 60,
  /** Öğle arası (yoksa null). */
  val breakStart: Int? = null,
  val breakEnd: Int? = null
) {
  companion object {
    val DAY_NAMES = listOf("Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi", "Pazar")
    val SHORT_DAY_NAMES = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")

    fun defaultWeek(): List<WorkingDay> = (1..7).map { d ->
      WorkingDay(dayOfWeek = d, isOpen = d <= 6, openMinute = 9 * 60, closeMinute = 19 * 60)
    }

    /** Calendar.DAY_OF_WEEK (Pazar=1) -> 1=Pazartesi ... 7=Pazar */
    fun fromCalendarDay(calendarDay: Int): Int = if (calendarDay == Calendar.SUNDAY) 7 else calendarDay - 1

    fun formatMinute(m: Int): String = "%02d:%02d".format(m / 60, m % 60)
  }
}

data class ServiceItem(
  val id: String = "",
  val name: String,
  val durationMinutes: Int,
  val price: Double,
  val isActive: Boolean = true
)

data class BusinessProfile(
  val id: String,
  val ownerUid: String,
  val name: String,
  val category: String = "",
  val city: String = "",
  val district: String = "",
  val address: String = "",
  val phone: String = "",
  val description: String = "",
  /** Herkese açık listede görünsün mü? */
  val isListed: Boolean = true,
  /** Randevular işletme onayı olmadan kesinleşsin mi? */
  val autoApprove: Boolean = false,
  val joinCode: String = "",
  val workingHours: List<WorkingDay> = WorkingDay.defaultWeek(),
  /** Müşteri en fazla kaç gün sonrasına randevu alabilir. */
  val bookingWindowDays: Int = 30,
  /** Randevu en az kaç saat önceden alınabilir / iptal edilebilir. */
  val minNoticeMinutes: Int = 60,
  /** Küçük JPEG logo (base64). Boşsa simge gösterilir. */
  val logo: String = "",
  /** Harita konumu (müşterilere yol tarifi için). */
  val latitude: Double? = null,
  val longitude: Double? = null
) {
  val hasLocation: Boolean get() = latitude != null && longitude != null
  fun dayFor(timestamp: Long): WorkingDay {
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val d = WorkingDay.fromCalendarDay(cal.get(Calendar.DAY_OF_WEEK))
    return workingHours.firstOrNull { it.dayOfWeek == d } ?: WorkingDay(d, isOpen = false)
  }
}

enum class AppointmentStatus(val label: String) {
  PENDING("Onay bekliyor"),
  CONFIRMED("Onaylandı"),
  COMPLETED("Tamamlandı"),
  CANCELLED("İptal edildi"),
  REJECTED("Reddedildi"),
  NO_SHOW("Gelmedi");

  val isActive: Boolean get() = this == PENDING || this == CONFIRMED
}

data class Appointment(
  val id: String = "",
  val businessId: String,
  val businessName: String,
  /** Uygulamayı kullanan müşteri ise uid; işletmenin elle eklediği randevularda boş. */
  val customerUid: String = "",
  val customerName: String,
  val customerPhone: String = "",
  val serviceId: String = "",
  val serviceName: String,
  val durationMinutes: Int,
  val price: Double,
  val start: Long,
  val status: AppointmentStatus,
  val note: String = "",
  val cancelledBy: String = "",
  val dayKey: String = dayKeyOf(start),
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
  /** İşletme bu randevuyu defterine (borç/tahsilat) işledi mi? */
  val ledgerPosted: Boolean = false,
  /** Randevu alınırken işletmenin adresi ve konumu (yol tarifi için). */
  val businessAddress: String = "",
  val businessLat: Double? = null,
  val businessLng: Double? = null
) {
  val end: Long get() = start + durationMinutes * 60_000L
}

/** "20261025" biçiminde gün anahtarı (Firestore'da bileşik indeks gerektirmeden sorgu için). */
fun dayKeyOf(timestamp: Long): String {
  val c = Calendar.getInstance().apply { timeInMillis = timestamp }
  return "%04d%02d%02d".format(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
}

object BusinessCategories {
  val all = listOf(
    "Kuaför / Berber", "Güzellik / Bakım", "Diş / Sağlık", "Diyetisyen", "Veteriner",
    "Oto Servis / Yıkama", "Danışmanlık", "Eğitim / Kurs", "Spor / Fitness", "Fotoğraf", "Diğer"
  )
}
