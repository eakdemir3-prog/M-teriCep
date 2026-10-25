package com.example.model

data class UserBusinessProfile(
  val businessName: String = "Örnek İşletmem",
  val ownerName: String = "İşletme Yetkilisi",
  val phone: String = "0555 123 45 67",
  val city: String = "İstanbul",
  val currencySymbol: String = "₺",
  val notificationsEnabled: Boolean = true,
  val activeTier: SubscriptionTier = SubscriptionTier.FREE
)
