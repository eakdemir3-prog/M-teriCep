package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.LocalAppPalette
import com.example.viewmodel.Screen

/**
 * Alt menü: 5 eşit sütun. Ortadaki "+" kendi sütununda durur; hiçbir yazının üstüne binmez
 * ve her ekran genişliğinde etiketler tek satırda kalır.
 */
@Composable
fun MusteriCepBottomBar(
  currentScreen: Screen,
  onNavigate: (Screen) -> Unit,
  onQuickActionClick: () -> Unit,
  pendingCount: Int = 0
) {
  val palette = LocalAppPalette.current
  Surface(
    modifier = Modifier.fillMaxWidth(),
    color = MaterialTheme.colorScheme.surface
  ) {
    Column(modifier = Modifier.navigationBarsPadding()) {
      HorizontalDivider(color = palette.cardBorder)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(68.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        NavCell(
          label = "Ana Sayfa",
          selectedIcon = Icons.Filled.Dashboard,
          unselectedIcon = Icons.Outlined.Dashboard,
          isSelected = currentScreen is Screen.Dashboard,
          testTag = "nav_dashboard",
          onClick = { onNavigate(Screen.Dashboard) }
        )
        NavCell(
          label = "Randevu",
          selectedIcon = Icons.Filled.CalendarMonth,
          unselectedIcon = Icons.Outlined.CalendarMonth,
          isSelected = currentScreen is Screen.Appointments,
          testTag = "nav_appointments",
          badge = pendingCount,
          onClick = { onNavigate(Screen.Appointments) }
        )

        // Ortadaki hızlı işlem düğmesi (kendi sütununda)
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight(),
          contentAlignment = Alignment.Center
        ) {
          Box(
            modifier = Modifier
              .size(52.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.secondary)
              .clickable(role = Role.Button, onClick = onQuickActionClick)
              .testTag("fab_quick_action"),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Add,
              contentDescription = "Hızlı işlem",
              tint = MaterialTheme.colorScheme.onSecondary,
              modifier = Modifier.size(28.dp)
            )
          }
        }

        NavCell(
          label = "Müşteriler",
          selectedIcon = Icons.Filled.People,
          unselectedIcon = Icons.Outlined.People,
          isSelected = currentScreen is Screen.Customers,
          testTag = "nav_customers",
          onClick = { onNavigate(Screen.Customers) }
        )
        NavCell(
          label = "Kasa",
          selectedIcon = Icons.Filled.AccountBalanceWallet,
          unselectedIcon = Icons.Outlined.AccountBalanceWallet,
          isSelected = currentScreen is Screen.Collections,
          testTag = "nav_collections",
          onClick = { onNavigate(Screen.Collections) }
        )
      }
    }
  }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.NavCell(
  label: String,
  selectedIcon: ImageVector,
  unselectedIcon: ImageVector,
  isSelected: Boolean,
  testTag: String,
  badge: Int = 0,
  onClick: () -> Unit
) {
  val tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
    modifier = Modifier
      .weight(1f)
      .fillMaxHeight()
      .clickable(role = Role.Tab, onClick = onClick)
      .testTag(testTag)
  ) {
    // Seçili sekme için yumuşak hap arka plan
    Box(
      modifier = Modifier
        .width(52.dp)
        .height(30.dp)
        .clip(RoundedCornerShape(15.dp))
        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = if (isSelected) selectedIcon else unselectedIcon,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(22.dp)
      )
      if (badge > 0) {
        Surface(
          shape = CircleShape,
          color = AmberWarning,
          border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surface),
          modifier = Modifier
            .align(Alignment.TopEnd)
            .offset(x = (-4).dp, y = (-2).dp)
        ) {
          Text(
            text = if (badge > 9) "9+" else badge.toString(),
            color = MaterialTheme.colorScheme.surface,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp)
          )
        }
      }
    }
    Spacer(modifier = Modifier.height(3.dp))
    Text(
      text = label,
      fontSize = 11.sp,
      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
      color = tint,
      maxLines = 1,
      softWrap = false,
      overflow = TextOverflow.Ellipsis
    )
  }
}
