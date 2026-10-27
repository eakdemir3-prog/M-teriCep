package com.example.billing

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.example.data.SettingsStore
import com.example.model.Billing
import com.example.model.SubscriptionTier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Bir paketin Play'den gelen fiyatları (yerel para birimiyle). */
data class TierPrices(val monthly: String?, val yearly: String?)

/**
 * Google Play abonelikleri. Etkin paket Play'deki satın alımlardan hesaplanır ve
 * SettingsStore'a yazılır; uygulamanın geri kalanı paketi oradan okur.
 * Play'e ulaşılamazsa (internet yok vb.) son bilinen paket korunur.
 */
object BillingManager : PurchasesUpdatedListener {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
  private var client: BillingClient? = null
  private var settings: SettingsStore? = null
  private var appContext: Context? = null

  private val _prices = MutableStateFlow<Map<SubscriptionTier, TierPrices>>(emptyMap())
  val prices: StateFlow<Map<SubscriptionTier, TierPrices>> = _prices.asStateFlow()

  private val _ready = MutableStateFlow(false)
  val ready: StateFlow<Boolean> = _ready.asStateFlow()

  private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
  val messages: SharedFlow<String> = _messages.asSharedFlow()

  private var details: Map<String, ProductDetails> = emptyMap()
  private var currentPurchase: Purchase? = null

  fun init(context: Context) {
    if (!Billing.ENABLED || client != null) return
    appContext = context.applicationContext
    settings = SettingsStore(context.applicationContext)
    client = BillingClient.newBuilder(context.applicationContext)
      .setListener(this)
      .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
      .build()
    connect()
  }

  private fun connect() {
    val c = client ?: return
    if (c.isReady) { scope.launch { refresh() }; return }
    c.startConnection(object : BillingClientStateListener {
      override fun onBillingSetupFinished(result: BillingResult) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
          _ready.value = true
          scope.launch { refresh() }
        }
      }
      override fun onBillingServiceDisconnected() { _ready.value = false }
    })
  }

  /** Fiyatları ve mevcut abonelikleri Play'den yeniden okur. */
  suspend fun refresh() {
    val c = client ?: return
    if (!c.isReady) { connect(); return }
    val products = SubscriptionTier.entries.mapNotNull { it.productId }.map {
      QueryProductDetailsParams.Product.newBuilder().setProductId(it).setProductType(BillingClient.ProductType.SUBS).build()
    }
    val pd = runCatching { c.queryProductDetails(QueryProductDetailsParams.newBuilder().setProductList(products).build()) }.getOrNull()
    if (pd?.billingResult?.responseCode == BillingClient.BillingResponseCode.OK) {
      details = pd.productDetailsList.orEmpty().associateBy { it.productId }
      _prices.value = SubscriptionTier.entries.filter { it.productId != null }.associateWith { t ->
        val d = details[t.productId]
        TierPrices(priceOf(d, Billing.BASE_MONTHLY), priceOf(d, Billing.BASE_YEARLY))
      }
    }
    val pr = runCatching {
      c.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build())
    }.getOrNull() ?: return
    if (pr.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
      handlePurchases(pr.purchasesList, fromQuery = true)
    }
  }

  private fun priceOf(d: ProductDetails?, basePlan: String): String? {
    val offer = d?.subscriptionOfferDetails?.filter { it.basePlanId == basePlan }
      ?.minByOrNull { if (it.offerId == null) 0 else 1 } ?: return null
    return offer.pricingPhases.pricingPhaseList.lastOrNull()?.formattedPrice
  }

  /** Satın alma ekranını açar. Var olan abonelik varsa yeni pakete geçiş yapılır. */
  fun purchase(activity: Activity, tier: SubscriptionTier, yearly: Boolean) {
    val c = client
    val d = tier.productId?.let { details[it] }
    if (c == null || !c.isReady || d == null) {
      _messages.tryEmit("Google Play'e bağlanılamadı. İnternet bağlantınızı kontrol edip tekrar deneyin.")
      connect()
      return
    }
    val base = if (yearly) Billing.BASE_YEARLY else Billing.BASE_MONTHLY
    val offer = d.subscriptionOfferDetails?.filter { it.basePlanId == base }?.minByOrNull { if (it.offerId == null) 1 else 0 }
    if (offer == null) {
      _messages.tryEmit("Bu paket şu an satın alınamıyor. Lütfen daha sonra tekrar deneyin.")
      return
    }
    val params = BillingFlowParams.newBuilder().setProductDetailsParamsList(
      listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(d).setOfferToken(offer.offerToken).build())
    )
    currentPurchase?.let { old ->
      params.setSubscriptionUpdateParams(
        BillingFlowParams.SubscriptionUpdateParams.newBuilder()
          .setOldPurchaseToken(old.purchaseToken)
          .setSubscriptionReplacementMode(BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.WITH_TIME_PRORATION)
          .build()
      )
    }
    c.launchBillingFlow(activity, params.build())
  }

  override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
    when (result.responseCode) {
      BillingClient.BillingResponseCode.OK -> scope.launch { handlePurchases(purchases.orEmpty(), fromQuery = false) }
      BillingClient.BillingResponseCode.USER_CANCELED -> Unit
      BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> scope.launch { refresh() }
      else -> _messages.tryEmit("Satın alma tamamlanamadı (kod ${result.responseCode}). Tekrar deneyin.")
    }
  }

  private suspend fun handlePurchases(list: List<Purchase>, fromQuery: Boolean) {
    val c = client ?: return
    var best: SubscriptionTier = SubscriptionTier.FREE
    var bestPurchase: Purchase? = null
    var pending = false
    for (p in list) {
      if (p.purchaseState == Purchase.PurchaseState.PENDING) { pending = true; continue }
      if (p.purchaseState != Purchase.PurchaseState.PURCHASED) continue
      if (!p.isAcknowledged) {
        runCatching { c.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build()) }
      }
      val tier = p.products.mapNotNull { SubscriptionTier.fromProductId(it) }.maxByOrNull { it.rank } ?: continue
      if (tier.rank > best.rank) { best = tier; bestPurchase = p }
    }
    if (!fromQuery && pending) _messages.tryEmit("Ödemeniz onay bekliyor. Onaylanınca paketiniz açılacak.")
    if (fromQuery || list.isNotEmpty()) {
      currentPurchase = bestPurchase
      val before = settings?.currentTier()
      settings?.setTier(best)
      if (!fromQuery && best != SubscriptionTier.FREE && before != best) _messages.tryEmit("${best.title} paketiniz açıldı. Teşekkürler!")
    }
  }

  /** Play Store'daki abonelik yönetimi sayfası (iptal, ödeme yöntemi). */
  fun manageIntent(tier: SubscriptionTier?): Intent {
    val pkg = appContext?.packageName ?: "com.eavisionlabs.mustericep"
    val url = if (tier?.productId != null) "https://play.google.com/store/account/subscriptions?sku=${tier.productId}&package=$pkg"
    else "https://play.google.com/store/account/subscriptions"
    return Intent(Intent.ACTION_VIEW, Uri.parse(url))
  }
}
