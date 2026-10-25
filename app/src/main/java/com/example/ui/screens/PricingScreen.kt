package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SubscriptionTier
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.Slate500
import com.example.viewmodel.MusteriCepViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PricingScreen(
  viewModel: MusteriCepViewModel,
  modifier: Modifier = Modifier
) {
  val profile by viewModel.profile.collectAsState()
  val allCustomers by viewModel.allCustomersWithBalance.collectAsState()
  val realCount = allCustomers.count { !it.customer.isDemo }

  BackHandler {
    viewModel.navigateBack()
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Abonelik & Paketler", fontWeight = FontWeight.Bold) },
        navigationIcon = {
          IconButton(onClick = { viewModel.navigateBack() }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
      )
    }
  ) { innerPadding ->
    LazyColumn(
      modifier = modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      item {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Paketler",
          style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = "Kayıtlı müşteri (arşiv dahil): $realCount / ${if (profile.activeTier.maxCustomerLimit == Int.MAX_VALUE) "Sınırsız" else profile.activeTier.maxCustomerLimit} Müşteri",
          style = MaterialTheme.typography.bodyMedium,
          color = Slate500
        )
      }

      // Information pill about Google Play Billing
      item {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Info, contentDescription = null, tint = NavyPrimary)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                "Satın alma yakında",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = NavyPrimary
              )
              Text(
                "Ücretli paketler Google Play üzerinden satılacak. Bu sürümde ödeme alınmaz ve paket değiştirilemez; Ücretsiz paket tüm temel özellikleri içerir.",
                style = MaterialTheme.typography.bodySmall,
                color = Slate500
              )
            }
          }
        }
      }

      // Plan Cards
      items(SubscriptionTier.values()) { tier ->
        val isCurrent = profile.activeTier == tier
        val isPro = tier == SubscriptionTier.PRO

        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(
            containerColor = if (isPro) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface
          ),
          modifier = Modifier
            .fillMaxWidth()
            .then(
              if (isCurrent) Modifier.border(2.dp, EmeraldPrimary, RoundedCornerShape(18.dp))
              else if (isPro) Modifier.border(1.5.dp, NavyPrimary.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
              else Modifier
            )
            .testTag("pricing_card_${tier.name}")
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  text = tier.title,
                  style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                )
                Text(
                  text = if (tier.priceMonthly == 0) "Ücretsiz Kullanım" else "${tier.priceMonthly} TL / ay",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  color = if (tier.priceMonthly == 0) Slate500 else NavyPrimary
                )
              }

              Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isCurrent) EmeraldPrimary.copy(alpha = 0.15f) else AmberWarning.copy(alpha = 0.15f)
              ) {
                Text(
                  text = if (isCurrent) "Mevcut Paketiniz" else tier.badgeText,
                  color = if (isCurrent) EmeraldPrimary else AmberWarning,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Features list
            tier.features.forEach { feat ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Default.Check,
                  contentDescription = null,
                  tint = EmeraldPrimary,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = feat, style = MaterialTheme.typography.bodySmall)
              }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!isCurrent) {
              Button(
                onClick = { viewModel.requestUpgrade(tier) },
                colors = ButtonDefaults.buttonColors(
                  containerColor = if (isPro) NavyPrimary else MaterialTheme.colorScheme.surfaceVariant,
                  contentColor = if (isPro) com.example.ui.theme.LocalAppPalette.current.onInk else MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(
                  text = if (tier.priceMonthly == 0) "${tier.title} paket" else "${tier.title} • Yakında",
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }
      }

      item {
        Spacer(modifier = Modifier.height(40.dp))
      }
    }
  }
}
