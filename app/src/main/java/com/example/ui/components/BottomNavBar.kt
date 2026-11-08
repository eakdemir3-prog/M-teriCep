package com.example.ui.components

import com.example.util.tr

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.LocalAppPalette
import com.example.viewmodel.Screen

private data class NavTab(
  val label: String,
  val selectedIcon: ImageVector,
  val icon: ImageVector,
  val screen: Screen,
  val tag: String
)

/**
 * Yüzen kapsül alt menü (ZihinPark tarzı). Seçili sekme yeşil degrade bir kapsüle dönüşüp
 * adını gösterir; diğerleri yalnızca simgedir. Hızlı işlem "+" kapsülün sağında ayrı durur.
 */
@Composable
fun MusteriCepBottomBar(
  currentScreen: Screen,
  onNavigate: (Screen) -> Unit,
  onQuickActionClick: () -> Unit,
  pendingCount: Int = 0
) {
  val palette = LocalAppPalette.current
  val tabs = remember {
    listOf(
      NavTab(tr("Ana Sayfa"), Icons.Filled.Dashboard, Icons.Outlined.Dashboard, Screen.Dashboard, "nav_dashboard"),
      NavTab(tr("Randevu"), Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth, Screen.Appointments, "nav_appointments"),
      NavTab(tr("Müşteriler"), Icons.Filled.People, Icons.Outlined.People, Screen.Customers, "nav_customers"),
      NavTab(tr("Kasa"), Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet, Screen.Collections, "nav_collections")
    )
  }
  val gradient = Brush.horizontalGradient(listOf(palette.ink, palette.accent))
  val borderBrush = Brush.horizontalGradient(listOf(palette.accent.copy(alpha = 0.55f), palette.accent, palette.ink))

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .navigationBarsPadding()
      .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Surface(
      shape = RoundedCornerShape(32.dp),
      color = MaterialTheme.colorScheme.surface,
      shadowElevation = 10.dp,
      modifier = Modifier
        .weight(1f)
        .height(60.dp)
        .border(BorderStroke(1.5.dp, borderBrush), RoundedCornerShape(32.dp))
    ) {
      Row(
        modifier = Modifier
          .fillMaxHeight()
          .padding(horizontal = 5.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        tabs.forEach { tab ->
          val selected = when (tab.screen) {
            Screen.Dashboard -> currentScreen is Screen.Dashboard
            Screen.Appointments -> currentScreen is Screen.Appointments
            Screen.Customers -> currentScreen is Screen.Customers
            else -> currentScreen is Screen.Collections
          }
          NavPill(
            tab = tab,
            selected = selected,
            badge = if (tab.screen == Screen.Appointments) pendingCount else 0,
            gradient = gradient,
            onClick = { if (!selected) onNavigate(tab.screen) }
          )
        }
      }
    }

    Box(
      modifier = Modifier
        .size(64.dp)
        .shadow(10.dp, CircleShape, ambientColor = palette.accent, spotColor = palette.accent)
        .clip(CircleShape)
        .background(Brush.linearGradient(listOf(palette.ink, palette.accent)))
        .clickable(role = Role.Button, onClick = onQuickActionClick)
        .testTag("fab_quick_action")
        .semantics { contentDescription = tr("Hızlı işlem") },
      contentAlignment = Alignment.Center
    ) {
      Icon(Icons.Default.Add, contentDescription = null, tint = palette.onInk, modifier = Modifier.size(28.dp))
    }
  }
}

@Composable
private fun RowScope.NavPill(
  tab: NavTab,
  selected: Boolean,
  badge: Int,
  gradient: Brush,
  onClick: () -> Unit
) {
  val palette = LocalAppPalette.current
  // Seçili sekme yazısı kadar genişler; diğerleri kalan alanı eşit paylaşır
  Box(
    modifier = Modifier
      .then(if (selected) Modifier.animateContentSize(spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)) else Modifier.weight(1f))
      .height(50.dp)
      .clip(RoundedCornerShape(25.dp))
      .then(if (selected) Modifier.background(gradient) else Modifier)
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        role = Role.Tab,
        onClick = onClick
      )
      .testTag(tab.tag)
      .semantics {
        this.selected = selected
        contentDescription = tab.label + if (badge > 0) tr(", {0} bekleyen", badge) else ""
      }
      .then(if (selected) Modifier.padding(horizontal = 14.dp) else Modifier),
    contentAlignment = Alignment.Center
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box {
        Icon(
          imageVector = if (selected) tab.selectedIcon else tab.icon,
          contentDescription = null,
          tint = if (selected) palette.onInk else palette.inactive,
          modifier = Modifier.size(if (selected) 22.dp else 24.dp)
        )
        if (badge > 0 && !selected) {
          Surface(
            shape = CircleShape,
            color = AmberWarning,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surface),
            modifier = Modifier
              .align(Alignment.TopEnd)
              .offset(x = 9.dp, y = (-7).dp)
          ) {
            Text(
              text = if (badge > 9) "9+" else badge.toString(),
              color = Color.White,
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 4.dp)
            )
          }
        }
      }
      AnimatedVisibility(
        visible = selected,
        enter = fadeIn() + expandHorizontally(),
        exit = fadeOut() + shrinkHorizontally()
      ) {
        Row {
          Spacer(modifier = Modifier.width(6.dp))
          val fontScale = LocalDensity.current.fontScale
          Text(
            text = tab.label,
            color = palette.onInk,
            fontSize = (13f / fontScale.coerceAtLeast(1f)).sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false
          )
        }
      }
    }
  }
}
