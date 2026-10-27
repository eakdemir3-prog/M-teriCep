package com.example

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.example.model.AppThemeMode
import com.example.ui.components.AddCollectionDialog
import com.example.ui.components.AddDebtDialog
import com.example.ui.components.MusteriCepBottomBar
import com.example.ui.screens.CollectionsScreen
import com.example.ui.screens.CustomerDetailScreen
import com.example.ui.screens.CustomersScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ReportsAndExportScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WhatsAppTemplatesScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.CloudViewModel
import com.example.viewmodel.MusteriCepViewModel
import com.example.viewmodel.Screen
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Ekran görüntüsü üretir (app/build/shots). Tasarımdaki taşma ve kaymaları farklı
 * telefon genişliklerinde gözle kontrol etmek için CI'da çalışır.
 */
abstract class ScreenshotBase {
  @get:Rule val rule = createComposeRule()

  abstract val tag: String

  private fun save(node: SemanticsNodeInteraction, name: String) {
    val bmp: Bitmap = node.captureToImage().asAndroidBitmap()
    val dir = File("build/shots").apply { mkdirs() }
    File(dir, "${tag}_$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
  }

  private fun frame(vm: MusteriCepViewModel, screen: Screen, content: @Composable () -> Unit) {
    rule.setContent {
      MyApplicationTheme(themeMode = AppThemeMode.DEFTER) {
        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
          Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) { content() }
            MusteriCepBottomBar(currentScreen = screen, onNavigate = {}, onQuickActionClick = {}, pendingCount = 2)
          }
        }
      }
    }
  }

  private fun viewModel(): MusteriCepViewModel {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val vm = MusteriCepViewModel(app)
    vm.clearAllData()
    rule.waitForIdle()
    vm.loadDemoData()
    rule.waitUntil(timeoutMillis = 15_000) {
      Thread.sleep(50)
      vm.allCustomersWithBalance.value.size >= 4 && vm.allCollections.value.isNotEmpty()
    }
    return vm
  }

  @Test
  fun screens() {
    val vm = viewModel()
    var current: Screen by androidx.compose.runtime.mutableStateOf(Screen.Dashboard)
    rule.setContent {
      MyApplicationTheme(themeMode = AppThemeMode.DEFTER) {
        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
          Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
              when (val s = current) {
                Screen.Dashboard -> DashboardScreen(viewModel = vm)
                Screen.Customers -> CustomersScreen(viewModel = vm)
                Screen.Collections -> CollectionsScreen(viewModel = vm)
                Screen.ReportsAndExport -> ReportsAndExportScreen(viewModel = vm)
                Screen.WhatsAppTemplates -> WhatsAppTemplatesScreen(viewModel = vm)
                is Screen.CustomerDetail -> CustomerDetailScreen(viewModel = vm)
                else -> DashboardScreen(viewModel = vm)
              }
            }
            MusteriCepBottomBar(currentScreen = current, onNavigate = {}, onQuickActionClick = {}, pendingCount = 2)
          }
        }
      }
    }
    val shots = listOf(
      Screen.Dashboard to "1_ana",
      Screen.Customers to "2_musteriler",
      Screen.Collections to "4_kasa",
      Screen.ReportsAndExport to "5_raporlar",
      Screen.WhatsAppTemplates to "6_sablonlar"
    )
    for ((screen, name) in shots) {
      current = screen
      rule.waitForIdle()
      Thread.sleep(300)
      rule.waitForIdle()
      save(rule.onRoot(), name)
    }
    val first = vm.allCustomersWithBalance.value.maxByOrNull { it.totalDebt }!!.customer.id
    vm.selectCustomer(first)
    rule.waitUntil(10_000) { Thread.sleep(50); vm.selectedCustomerTransactions.value.isNotEmpty() }
    current = Screen.CustomerDetail(first)
    rule.waitForIdle()
    Thread.sleep(300)
    rule.waitForIdle()
    save(rule.onRoot(), "3_musteri_detay")
  }

  @Test
  fun dialogs() {
    val vm = viewModel()
    val customers = vm.allCustomersWithBalance.value
    var which by androidx.compose.runtime.mutableIntStateOf(0)
    rule.setContent {
      MyApplicationTheme(themeMode = AppThemeMode.DEFTER) {
        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
          if (which == 0) {
            AddDebtDialog(allCustomers = customers, onDismiss = {}, onSave = { _, _, _, _, _, _ -> })
          } else {
            AddCollectionDialog(preselectedCustomerId = customers.first { it.remainingBalance > 0 }.customer.id, allCustomers = customers, onDismiss = {}, onSave = { _, _, _, _, _ -> })
          }
        }
      }
    }
    rule.waitForIdle()
    save(rule.onAllNodes(isRoot())[1], "7_borc_ekle")
    which = 1
    rule.waitForIdle()
    save(rule.onAllNodes(isRoot())[1], "8_tahsilat_al")
  }
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h760dp-xhdpi")
class ScreenshotSmallTest : ScreenshotBase() {
  override val tag = "k360"
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class ScreenshotNormalTest : ScreenshotBase() {
  override val tag = "n411"
}
