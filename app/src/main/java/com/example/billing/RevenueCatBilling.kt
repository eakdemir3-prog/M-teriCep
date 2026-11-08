package com.example.billing

import android.app.Activity
import android.content.Context
import com.example.model.Billing
import com.example.model.SubscriptionTier
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.getCustomerInfoWith
import com.revenuecat.purchases.getProductsWith
import com.revenuecat.purchases.logInWith
import com.revenuecat.purchases.logOutWith
import com.revenuecat.purchases.models.GoogleReplacementMode
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.purchaseWith
import com.revenuecat.purchases.restorePurchasesWith

/**
 * RevenueCat ile abonelik: satın alımlar RevenueCat sunucusunda Google'a doğrulatılır ve
 * Google hesabına (Firebase kullanıcı kimliğine) bağlanır; telefon değişse de paket korunur.
 *
 * Paket, RevenueCat'teki yetkiler ("pro", "baslangic") veya etkin ürün kimliklerinden okunur.
 */
internal object RevenueCatBilling {
  private var configured = false
  private var products: Map<String, StoreProduct> = emptyMap()
  private var activeProductId: String? = null

  fun isAvailable(key: String) = key.startsWith("goog_")

  fun configure(context: Context, key: String, onInfo: (SubscriptionTier) -> Unit) {
    if (configured) return
    Purchases.configure(PurchasesConfiguration.Builder(context.applicationContext, key).build())
    Purchases.sharedInstance.updatedCustomerInfoListener =
      com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener { info -> onInfo(tierOf(info)) }
    configured = true
  }

  /** Firebase girişiyle aynı kimliği kullan: paket hesaba bağlı kalsın. */
  fun identify(uid: String?, onInfo: (SubscriptionTier) -> Unit) {
    if (!configured) return
    val p = Purchases.sharedInstance
    if (uid == null) {
      if (!p.isAnonymous) p.logOutWith(onError = { }, onSuccess = { onInfo(tierOf(it)) })
      return
    }
    if (p.appUserID == uid) return
    p.logInWith(uid, onError = { }, onSuccess = { info, _ -> onInfo(tierOf(info)) })
  }

  fun refresh(onInfo: (SubscriptionTier) -> Unit, onPrices: (Map<SubscriptionTier, TierPrices>) -> Unit) {
    if (!configured) return
    Purchases.sharedInstance.getCustomerInfoWith(onError = { }, onSuccess = { onInfo(tierOf(it)) })
    val ids = SubscriptionTier.entries.mapNotNull { it.productId }
    Purchases.sharedInstance.getProductsWith(ids, ProductType.SUBS, onError = { }) { list ->
      products = list.associateBy { it.id }
      onPrices(
        SubscriptionTier.entries.filter { it.productId != null }.associateWith { t ->
          TierPrices(
            monthly = products["${t.productId}:${Billing.BASE_MONTHLY}"]?.price?.formatted,
            yearly = products["${t.productId}:${Billing.BASE_YEARLY}"]?.price?.formatted
          )
        }
      )
    }
  }

  fun purchase(
    activity: Activity,
    tier: SubscriptionTier,
    yearly: Boolean,
    onDone: (SubscriptionTier) -> Unit,
    onError: (String) -> Unit
  ) {
    val id = "${tier.productId}:${if (yearly) Billing.BASE_YEARLY else Billing.BASE_MONTHLY}"
    val product = products[id] ?: run {
      onError("Paket bilgisi yüklenemedi. İnternet bağlantınızı kontrol edip tekrar deneyin.")
      return
    }
    val params = PurchaseParams.Builder(activity, product)
    activeProductId?.substringBefore(":")?.takeIf { it != tier.productId }?.let { old ->
      params.oldProductId(old).googleReplacementMode(GoogleReplacementMode.WITH_TIME_PRORATION)
    }
    Purchases.sharedInstance.purchaseWith(
      params.build(),
      onError = { error, userCancelled -> if (!userCancelled) onError("Satın alma tamamlanamadı: ${error.message}") },
      onSuccess = { _, info -> onDone(tierOf(info)) }
    )
  }

  fun restore(onDone: (SubscriptionTier) -> Unit, onError: (String) -> Unit) {
    if (!configured) return
    Purchases.sharedInstance.restorePurchasesWith(
      onError = { onError("Satın alımlar yüklenemedi: ${it.message}") },
      onSuccess = { onDone(tierOf(it)) }
    )
  }

  private fun tierOf(info: CustomerInfo): SubscriptionTier {
    val ent = info.entitlements.active.keys.map { it.lowercase() }
    val subs = info.activeSubscriptions
    activeProductId = subs.maxByOrNull { s -> SubscriptionTier.fromProductId(s.substringBefore(":"))?.rank ?: -1 }
    val fromProducts = subs.mapNotNull { SubscriptionTier.fromProductId(it.substringBefore(":")) }.maxByOrNull { it.rank }
    val fromEntitlements = when {
      "pro" in ent -> SubscriptionTier.PRO
      "baslangic" in ent || "starter" in ent -> SubscriptionTier.STARTER
      else -> null
    }
    return listOfNotNull(fromProducts, fromEntitlements).maxByOrNull { it.rank } ?: SubscriptionTier.FREE
  }
}
