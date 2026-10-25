package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldPrimary
import com.example.viewmodel.Screen

@Composable
fun MusteriCepBottomBar(
  currentScreen: Screen,
  onNavigate: (Screen) -> Unit,
  onQuickActionClick: () -> Unit
) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 8.dp,
    shadowElevation = 12.dp
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(64.dp)
          .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // 1. Ana Sayfa
        BottomNavItem(
          label = "Ana Sayfa",
          selectedIcon = Icons.Filled.Dashboard,
          unselectedIcon = Icons.Outlined.Dashboard,
          isSelected = currentScreen is Screen.Dashboard,
          testTag = "nav_dashboard",
          onClick = { onNavigate(Screen.Dashboard) }
        )

        // 2. Müşteriler
        BottomNavItem(
          label = "Müşteriler",
          selectedIcon = Icons.Filled.People,
          unselectedIcon = Icons.Outlined.People,
          isSelected = currentScreen is Screen.Customers,
          testTag = "nav_customers",
          onClick = { onNavigate(Screen.Customers) }
        )

        // Empty space for center elevated FAB
        Spacer(modifier = Modifier.size(56.dp))

        // 3. Tahsilatlar
        BottomNavItem(
          label = "Tahsilatlar",
          selectedIcon = Icons.Filled.AccountBalanceWallet,
          unselectedIcon = Icons.Outlined.AccountBalanceWallet,
          isSelected = currentScreen is Screen.Collections,
          testTag = "nav_collections",
          onClick = { onNavigate(Screen.Collections) }
        )

        // 4. Ayarlar
        BottomNavItem(
          label = "Ayarlar",
          selectedIcon = Icons.Filled.Settings,
          unselectedIcon = Icons.Outlined.Settings,
          isSelected = currentScreen is Screen.Settings,
          testTag = "nav_settings",
          onClick = { onNavigate(Screen.Settings) }
        )
      }

      // Elevated Center "+" Quick Action Button
      FloatingActionButton(
        onClick = onQuickActionClick,
        containerColor = EmeraldPrimary,
        contentColor = androidx.compose.ui.graphics.Color.White,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
        shape = CircleShape,
        modifier = Modifier
          .align(Alignment.Center)
          .offset(y = (-14).dp)
          .size(56.dp)
          .testTag("fab_quick_action")
      ) {
        Icon(
          imageVector = Icons.Default.Add,
          contentDescription = "Hızlı İşlem Menüsü",
          modifier = Modifier.size(28.dp)
        )
      }
    }
  }
}

@Composable
private fun BottomNavItem(
  label: String,
  selectedIcon: ImageVector,
  unselectedIcon: ImageVector,
  isSelected: Boolean,
  testTag: String,
  onClick: () -> Unit
) {
  val interactionSource = remember { MutableInteractionSource() }
  val tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
    modifier = Modifier
      .clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick
      )
      .padding(horizontal = 8.dp, vertical = 6.dp)
      .testTag(testTag)
  ) {
    Icon(
      imageVector = if (isSelected) selectedIcon else unselectedIcon,
      contentDescription = label,
      tint = tint,
      modifier = Modifier.size(24.dp)
    )
    Spacer(modifier = Modifier.height(2.dp))
    Text(
      text = label,
      fontSize = 11.sp,
      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
      color = tint
    )
  }
}
