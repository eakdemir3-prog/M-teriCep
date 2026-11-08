package com.example.model

import com.example.util.tr

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
  val badgeText: String,
  /** Personel sayısı (0 = personel özelliği yok, tek takvim). */
  val maxStaff: Int = 0
) {
  FREE(tr("Ücretsiz"), 0, null, "0 ₺", "0 ₺", 25, 30, 3, tr("Başlamak için")),
  STARTER(tr("Başlangıç"), 1, "mustericep_baslangic", "149 ₺", "1.490 ₺", 150, 300, 15, tr("Popüler")),
  PRO(tr("Pro"), 2, "mustericep_pro", "299 ₺", "2.990 ₺", UNLIMITED, UNLIMITED, UNLIMITED, tr("En kapsamlı")),
  BUSINESS(tr("İşletme"), 3, "mustericep_isletme", "499 ₺", "4.990 ₺", UNLIMITED, UNLIMITED, UNLIMITED, tr("Ekipler için"), maxStaff = 10);

  fun limitText(n: Int): String = if (n == UNLIMITED) tr("Sınırsız") else n.toString()

  val features: List<String>
    get() = listOf(
      tr("{0} müşteri kaydı", limitText(maxCustomerLimit)),
      tr("Ayda {0} randevu", if (monthlyAppointments == UNLIMITED) "sınırsız" else monthlyAppointments.toString()),
      tr("{0} hizmet", limitText(maxServices)),
      tr("Borç, tahsilat ve kasa takibi"),
      tr("Logo, harita konumu ve yol tarifi"),
      tr("Yedekleme ve geri yükleme")
    ) + when (this) {
      FREE -> listOf(tr("5 hazır WhatsApp şablonu"), tr("Bu ayın raporları"))
      STARTER -> listOf(tr("WhatsApp şablonlarını düzenleme"), tr("Tüm dönem raporları"), tr("CSV (Excel) dışa aktarma"), tr("Otomatik vade hatırlatıcısı"))
      PRO -> listOf(tr("WhatsApp şablonlarını düzenleme"), tr("Tüm dönem raporları"), tr("CSV dışa ve toplu içe aktarma"), tr("Otomatik vade hatırlatıcısı"), tr("İşletme listesinde üst sırada"))
      BUSINESS -> listOf(tr("Pro'daki her şey"), tr("10 personele kadar ayrı takvim"), tr("Müşteri personel seçerek randevu alır"), tr("Personel bazlı randevu listesi"), tr("İşletme listesinde üst sırada"))
    }

  companion object {
    fun fromProductId(id: String): SubscriptionTier? = entries.firstOrNull { it.productId == id }
  }
}

/** Paket gerektiren özellikler. */
enum class Feature(val label: String, val required: SubscriptionTier) {
  EDIT_TEMPLATES(tr("WhatsApp şablonlarını düzenleme"), SubscriptionTier.STARTER),
  ALL_REPORTS(tr("Tüm dönem raporları"), SubscriptionTier.STARTER),
  CSV_EXPORT(tr("CSV (Excel) dışa aktarma"), SubscriptionTier.STARTER),
  CSV_IMPORT(tr("CSV ile toplu müşteri içe aktarma"), SubscriptionTier.PRO),
  DUE_REMINDERS(tr("Otomatik vade hatırlatıcısı"), SubscriptionTier.STARTER),
  MORE_CUSTOMERS(tr("Daha fazla müşteri"), SubscriptionTier.STARTER),
  MORE_SERVICES(tr("Daha fazla hizmet"), SubscriptionTier.STARTER),
  MORE_APPOINTMENTS(tr("Daha fazla aylık randevu"), SubscriptionTier.STARTER),
  STAFF(tr("Personel ve ayrı takvimler"), SubscriptionTier.BUSINESS);
}

fun SubscriptionTier.allows(f: Feature): Boolean = !Billing.ENABLED || rank >= f.required.rank
