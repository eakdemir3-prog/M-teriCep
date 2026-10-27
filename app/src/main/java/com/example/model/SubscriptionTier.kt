package com.example.model

/** Google Play abonelikleri açık mı? Kapalıysa hiçbir sınır uygulanmaz. */
object Billing {
  const val ENABLED = true
  const val BASE_MONTHLY = "aylik"
  const val BASE_YEARLY = "yillik"
}

const val UNLIMITED = Int.MAX_VALUE

/**
 * Paketler. Sınırlar yalnızca işletme hesabı için geçerlidir; randevu alan müşteriler hiçbir
 * zaman ödeme yapmaz. Fiyatlar Play Console'daki aboneliklerden okunur; buradakiler yedek metindir.
 */
enum class SubscriptionTier(
  val title: String,
  val rank: Int,
  /** Play Console'daki abonelik ürün kimliği (ücretsiz pakette yok). */
  val productId: String?,
  val fallbackMonthly: String,
  val fallbackYearly: String,
  val maxCustomerLimit: Int,
  val monthlyAppointments: Int,
  val maxServices: Int,
  val badgeText: String
) {
  FREE("Ücretsiz", 0, null, "0 ₺", "0 ₺", 25, 30, 3, "Başlamak için"),
  STARTER("Başlangıç", 1, "mustericep_baslangic", "149 ₺", "1.490 ₺", 150, 300, 15, "Popüler"),
  PRO("Pro", 2, "mustericep_pro", "299 ₺", "2.990 ₺", UNLIMITED, UNLIMITED, UNLIMITED, "En kapsamlı");

  fun limitText(n: Int): String = if (n == UNLIMITED) "Sınırsız" else n.toString()

  val features: List<String>
    get() = listOf(
      "${limitText(maxCustomerLimit)} müşteri kaydı",
      "Ayda ${if (monthlyAppointments == UNLIMITED) "sınırsız" else monthlyAppointments.toString()} randevu",
      "${limitText(maxServices)} hizmet",
      "Borç, tahsilat ve kasa takibi",
      "Logo, harita konumu ve yol tarifi",
      "Yedekleme ve geri yükleme"
    ) + when (this) {
      FREE -> listOf("5 hazır WhatsApp şablonu", "Bu ayın raporları")
      STARTER -> listOf("WhatsApp şablonlarını düzenleme", "Tüm dönem raporları", "CSV (Excel) dışa aktarma", "Otomatik vade hatırlatıcısı")
      PRO -> listOf("WhatsApp şablonlarını düzenleme", "Tüm dönem raporları", "CSV dışa ve toplu içe aktarma", "Otomatik vade hatırlatıcısı", "İşletme listesinde üst sırada")
    }

  companion object {
    fun fromProductId(id: String): SubscriptionTier? = entries.firstOrNull { it.productId == id }
  }
}

/** Paket gerektiren özellikler. */
enum class Feature(val label: String, val required: SubscriptionTier) {
  EDIT_TEMPLATES("WhatsApp şablonlarını düzenleme", SubscriptionTier.STARTER),
  ALL_REPORTS("Tüm dönem raporları", SubscriptionTier.STARTER),
  CSV_EXPORT("CSV (Excel) dışa aktarma", SubscriptionTier.STARTER),
  CSV_IMPORT("CSV ile toplu müşteri içe aktarma", SubscriptionTier.PRO),
  DUE_REMINDERS("Otomatik vade hatırlatıcısı", SubscriptionTier.STARTER),
  MORE_CUSTOMERS("Daha fazla müşteri", SubscriptionTier.STARTER),
  MORE_SERVICES("Daha fazla hizmet", SubscriptionTier.STARTER),
  MORE_APPOINTMENTS("Daha fazla aylık randevu", SubscriptionTier.STARTER);
}

fun SubscriptionTier.allows(f: Feature): Boolean = !Billing.ENABLED || rank >= f.required.rank
