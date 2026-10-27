package com.example.model

/** Kullanıcının işletme bilgileri ve uygulama tercihleri (DataStore'da kalıcı saklanır). */
data class UserBusinessProfile(
  val businessName: String = "",
  val ownerName: String = "",
  val phone: String = "",
  val city: String = "",
  val currencySymbol: String = "₺",
  val notificationsEnabled: Boolean = false,
  /** Vadeden kaç gün önce hatırlatılsın. */
  val reminderDaysBefore: Int = 1,
  val activeTier: SubscriptionTier = SubscriptionTier.FREE,
  val themeMode: AppThemeMode = AppThemeMode.DEFTER
) {
  /** Mesajlarda ve başlıkta kullanılacak işletme adı (boşsa varsayılan). */
  val displayBusinessName: String get() = businessName.ifBlank { "İşletmem" }
  val isProfileComplete: Boolean get() = businessName.isNotBlank()
}

enum class AppThemeMode(val label: String, val description: String) {
  DEFTER("Açık", "Beyaz ve yeşil"),
  KOMUTA("Koyu", "Gece modu"),
  SISTEM("Sistem", "Telefona göre")
}
