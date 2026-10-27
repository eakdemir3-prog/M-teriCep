package com.example.ui.cloud

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.cloud.UserRole
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LocalAppPalette
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.Slate500
import com.example.viewmodel.CloudViewModel

@Composable
fun WelcomeScreen(cloud: CloudViewModel) {
  val context = LocalContext.current
  val busy by cloud.busy.collectAsState()
  var role by remember { mutableStateOf<UserRole?>(null) }
  val palette = LocalAppPalette.current

  Box(
    modifier = Modifier
      .fillMaxSize()
      .safeDrawingPadding()
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Spacer(modifier = Modifier.height(24.dp))
      Image(
        painter = painterResource(R.drawable.ic_brand),
        contentDescription = null,
        modifier = Modifier.size(88.dp)
      )
      Spacer(modifier = Modifier.height(14.dp))
      Text("MüşteriCep", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
      Text(
        "Randevu, müşteri ve tahsilat tek cepte",
        style = MaterialTheme.typography.bodyMedium,
        color = Slate500,
        textAlign = TextAlign.Center
      )
      Spacer(modifier = Modifier.height(28.dp))

      Text("Nasıl kullanacaksınız?", style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth())
      Spacer(modifier = Modifier.height(10.dp))

      RoleCard(
        selected = role == UserRole.BUSINESS,
        icon = { Icon(Icons.Default.Storefront, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(30.dp)) },
        title = "İşletmeyim",
        text = "Randevularımı, müşterilerimi, veresiye ve tahsilatlarımı yöneteceğim.",
        onClick = { role = UserRole.BUSINESS }
      )
      Spacer(modifier = Modifier.height(10.dp))
      RoleCard(
        selected = role == UserRole.CUSTOMER,
        icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(30.dp)) },
        title = "Müşteriyim",
        text = "Gittiğim işletmeden kolayca randevu alacağım.",
        onClick = { role = UserRole.CUSTOMER }
      )

      Spacer(modifier = Modifier.height(24.dp))

      if (cloud.isConfigured) {
        Button(
          onClick = { role?.let { r -> context.findActivity()?.let { cloud.signIn(it, r) } } },
          enabled = role != null && !busy,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
        ) {
          if (busy) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = palette.onInk)
          } else {
            Text(
              when (role) {
                null -> "Önce yukarıdan seçin"
                else -> "Google ile devam et"
              },
              fontWeight = FontWeight.Bold,
              fontSize = 16.sp
            )
          }
        }
      } else {
        Text(
          "Randevu ve hesap özellikleri bu sürümde henüz aktif değil.",
          style = MaterialTheme.typography.bodySmall,
          color = Slate500,
          textAlign = TextAlign.Center
        )
      }

      if (role != UserRole.CUSTOMER) {
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = { cloud.continueLocalOnly() }) {
          Text("Giriş yapmadan sadece defteri kullan")
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
      Text(
        "Devam ederek KVKK aydınlatma metnini ve gizlilik politikasını kabul etmiş olursunuz.",
        style = MaterialTheme.typography.labelSmall,
        color = Slate500,
        textAlign = TextAlign.Center
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text("EA Vision Labs", style = MaterialTheme.typography.labelSmall, color = Slate500)
    }
  }
}

@Composable
private fun RoleCard(selected: Boolean, icon: @Composable () -> Unit, title: String, text: String, onClick: () -> Unit) {
  val palette = LocalAppPalette.current
  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) NavyPrimary else palette.cardBorder),
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
  ) {
    Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Start) {
      icon()
      Spacer(modifier = Modifier.width(14.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Text(text, style = MaterialTheme.typography.bodySmall, color = Slate500)
      }
    }
  }
}

private tailrec fun android.content.Context.findActivity(): Activity? = when (this) {
  is Activity -> this
  is android.content.ContextWrapper -> baseContext.findActivity()
  else -> null
}
