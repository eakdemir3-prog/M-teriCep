package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.billing.BillingManager
import com.example.model.SubscriptionTier
import com.example.model.UNLIMITED
import com.example.ui.components.AppCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.LocalAppPalette
import com.example.ui.theme.moneyStyle
import com.example.viewmodel.MusteriCepViewModel
import kotlinx.coroutines.launch

private tailrec fun Context.findActivity(): Activity? = when (this) {
  is Activity -> this
  is ContextWrapper -> baseContext.findActivity()
  else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PricingScreen(
  viewModel: MusteriCepViewModel,
  modifier: Modifier = Modifier
) {
  val profile by viewModel.profile.collectAsState()
  val allCustomers by viewModel.allCustomersWithBalance.collectAsState()
  val prices by BillingManager.prices.collectAsState()
  val realCount = allCustomers.count { !it.customer.isDemo }
  val current = profile.activeTier
  val palette = LocalAppPalette.current
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var yearly by rememberSaveable { mutableStateOf(false) }

  BackHandler { viewModel.navigateBack() }
  LaunchedEffect(Unit) { BillingManager.refresh() }

  Scaffold(
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        title = { Text("Paketler", fontWeight = FontWeight.Bold) },
        navigationIcon = {
          IconButton(onClick = { viewModel.navigateBack() }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
          }
        },
        windowInsets = WindowInsets(0, 0, 0, 0),
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
      )
    }
  ) { innerPadding ->
    LazyColumn(
      modifier = modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // Mevcut paket ve kullanım
      item {
        AppCard {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
              Text("Mevcut paketiniz", style = MaterialTheme.typography.labelMedium, color = palette.muted)
              Text(current.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            StatusPill("Aktif", palette.success, palette.accentSoft)
          }
          Spacer(modifier = Modifier.height(10.dp))
          val limit = current.maxCustomerLimit
          Text(
            "Müşteri: $realCount / ${if (limit == UNLIMITED) "sınırsız" else limit}",
            style = MaterialTheme.typography.bodyMedium
          )
          if (limit != UNLIMITED) {
            Spacer(modifier = Modifier.height(6.dp))
            androidx.compose.material3.LinearProgressIndicator(
              progress = { (realCount.toFloat() / limit).coerceIn(0f, 1f) },
              color = if (realCount >= limit) palette.danger else palette.ink,
              trackColor = palette.cardBorder,
              modifier = Modifier.fillMaxWidth().height(6.dp)
            )
          }
        }
      }

      // Aylık / yıllık
      item {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
          SegmentedButton(
            selected = !yearly, onClick = { yearly = false },
            shape = SegmentedButtonDefaults.itemShape(0, 2),
            colors = SegmentedButtonDefaults.colors(activeContainerColor = palette.accentSoft, activeContentColor = palette.ink),
            icon = {}
          ) { Text("Aylık") }
          SegmentedButton(
            selected = yearly, onClick = { yearly = true },
            shape = SegmentedButtonDefaults.itemShape(1, 2),
            colors = SegmentedButtonDefaults.colors(activeContainerColor = palette.accentSoft, activeContentColor = palette.ink),
            icon = {}
          ) { Text("Yıllık • 2 ay bedava") }
        }
      }

      items(SubscriptionTier.entries.toList(), key = { it.name }) { tier ->
        val isCurrent = current == tier
        val highlight = tier == SubscriptionTier.PRO
        val p = prices[tier]
        val priceText = when {
          tier.productId == null -> "0 ₺"
          yearly -> p?.yearly ?: tier.fallbackYearly
          else -> p?.monthly ?: tier.fallbackMonthly
        }
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(
            if (isCurrent || highlight) 2.dp else 1.dp,
            when { isCurrent -> palette.success; highlight -> palette.ink.copy(alpha = 0.5f); else -> palette.cardBorder }
          ),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(tier.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
              if (isCurrent) StatusPill("Mevcut paket", palette.success, palette.accentSoft)
              else StatusPill(tier.badgeText, palette.warning, palette.warningSoft)
            }
            Row(verticalAlignment = Alignment.Bottom) {
              Text(priceText, style = moneyStyle(26), color = palette.ink)
              if (tier.productId != null) {
                Text(if (yearly) " / yıl" else " / ay", style = MaterialTheme.typography.bodyMedium, color = palette.muted, modifier = Modifier.padding(bottom = 3.dp))
              }
            }
            Spacer(modifier = Modifier.height(12.dp))
            tier.features.forEach { feat ->
              Row(modifier = Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Check, contentDescription = null, tint = palette.success, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(feat, style = MaterialTheme.typography.bodySmall)
              }
            }
            if (tier.productId != null && !isCurrent) {
              Spacer(modifier = Modifier.height(14.dp))
              val upgrade = tier.rank > current.rank
              Button(
                onClick = {
                  val act = context.findActivity()
                  if (act != null) BillingManager.purchase(act, tier, yearly)
                },
                colors = ButtonDefaults.buttonColors(
                  containerColor = if (upgrade) palette.ink else MaterialTheme.colorScheme.surfaceVariant,
                  contentColor = if (upgrade) palette.onInk else MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
              ) {
                Text(if (upgrade) "${tier.title} paketine geç" else "${tier.title} paketine düş", fontWeight = FontWeight.SemiBold)
              }
            }
          }
        }
      }

      item {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
          OutlinedButton(
            onClick = { scope.launch { BillingManager.refresh(); viewModel.showMessage("Satın alımlar kontrol edildi.") } },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f)
          ) { Text("Satın alımları yükle", maxLines = 1, fontSize = 13.sp) }
          OutlinedButton(
            onClick = { runCatching { context.startActivity(BillingManager.manageIntent(current.takeIf { it.productId != null })) } },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f)
          ) { Text("Aboneliği yönet", maxLines = 1, fontSize = 13.sp) }
        }
      }

      item {
        Text(
          "Ödeme Google Play hesabınızdan alınır. Abonelik, siz iptal edene kadar her dönem sonunda otomatik yenilenir. " +
            "Play Store > Ödemeler ve abonelikler bölümünden istediğiniz zaman iptal edebilirsiniz; iptal ettiğinizde paketiniz dönem sonuna kadar açık kalır. " +
            "Paket değiştirdiğinizde kalan süreniz yeni pakete orantılı olarak aktarılır.",
          style = MaterialTheme.typography.bodySmall,
          color = palette.muted
        )
        Spacer(modifier = Modifier.height(40.dp))
      }
    }
  }
}
