package com.example.model

/**
 * Google Play Faturalandırma henüz bağlı değil. Kapalıyken müşteri sınırı uygulanmaz ve
 * paketler menüsü gizlenir; böylece kimse satın alamayacağı bir sınıra takılmaz.
 */
object Billing {
  const val ENABLED = false
}

enum class SubscriptionTier(
  val title: String,
  val priceMonthly: Int,
  val maxCustomerLimit: Int, // Int.MAX_VALUE for unlimited
  val badgeText: String,
  val features: List<String>
) {
  FREE(
    title = "Ücretsiz",
    priceMonthly = 0,
    maxCustomerLimit = 25,
    badgeText = "Mevcut Paket",
    features = listOf(
      "En fazla 25 müşteri kaydı",
      "Temel borç ve tahsilat takibi",
      "5 hazır WhatsApp şablonu",
      "Cihazda güvenli yerel depolama"
    )
  ),
  STARTER(
    title = "Başlangıç",
    priceMonthly = 149,
    maxCustomerLimit = 100,
    badgeText = "Popüler",
    features = listOf(
      "100 müşteri kaydı kapasitesi",
      "Sınırsız WhatsApp mesaj şablonu",
      "Excel (CSV) dışa aktarma",
      "Gecikmiş vade hatırlatıcıları",
      "E-posta destek"
    )
  ),
  PRO(
    title = "Pro",
    priceMonthly = 299,
    maxCustomerLimit = Int.MAX_VALUE,
    badgeText = "En Çok Tercih Edilen",
    features = listOf(
      "Sınırsız müşteri kaydı",
      "Detaylı finansal & nakit akışı raporları",
      "Excel (CSV) içe ve dışa aktarma",
      "Özelleştirilebilir kasa kategorileri",
      "Öncelikli WhatsApp destek hattı"
    )
  ),
  BUSINESS(
    title = "İşletme",
    priceMonthly = 499,
    maxCustomerLimit = Int.MAX_VALUE,
    badgeText = "Kurumsal",
    features = listOf(
      "Sınırsız müşteri ve işlem hacmi",
      "Çoklu cihaz Firebase bulut senkronizasyonu hazırlığı",
      "Otomatik yedekleme ve arşivleme",
      "Özel muhasebe format desteği",
      "7/24 VIP telefon desteği"
    )
  );

  companion object {
    fun getTierForCustomerCount(count: Int, activeTier: SubscriptionTier): Boolean {
      return count < activeTier.maxCustomerLimit
    }
  }
}
