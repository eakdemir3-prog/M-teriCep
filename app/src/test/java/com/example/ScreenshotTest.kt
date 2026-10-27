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
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.activity.ComponentActivity
import android.graphics.Canvas
import android.view.View
import org.robolectric.shadows.ShadowLooper
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
  @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

  abstract val tag: String
  /** Robolectric, qualifier verilen yapılandırmada Dialog ölçümünde takılıyor; diyaloglar varsayılan ekranda çekilir. */
  open val dialogs = false

  /** Açık pencerelerin kök görünümleri (etkinlik + varsa iletişim kutusu). */
  @Suppress("UNCHECKED_CAST")
  private fun rootViews(): List<View> {
    val wmg = Class.forName("android.view.WindowManagerGlobal")
    val inst = wmg.getMethod("getInstance").invoke(null)
    val f = wmg.getDeclaredField("mViews").apply { isAccessible = true }
    return (f.get(inst) as List<View>).toList()
  }

  private fun idle() {
    repeat(5) {
      ShadowLooper.idleMainLooper()
      rule.waitForIdle()
      Thread.sleep(40)
    }
  }

  private fun saveView(v: View, name: String) {
    val bmp = Bitmap.createBitmap(v.width.coerceAtLeast(1), v.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    v.draw(Canvas(bmp))
    val dir = File("build/shots").apply { mkdirs() }
    File(dir, "${tag}_$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
  }

  private fun save(@Suppress("UNUSED_PARAMETER") node: SemanticsNodeInteraction?, name: String) {
    idle()
    saveView(rule.activity.window.decorView, name)
  }

  private fun saveDialog(name: String) {
    idle()
    val dialog = rootViews().lastOrNull { it !== rule.activity.window.decorView } ?: rule.activity.window.decorView
    saveView(dialog, name)
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
    vm.loadDemoData()
    val deadline = System.currentTimeMillis() + 20_000
    while (System.currentTimeMillis() < deadline &&
      !(vm.allCustomersWithBalance.value.size >= 4 && vm.allCollections.value.isNotEmpty())) {
      ShadowLooper.idleMainLooper()
      Thread.sleep(50)
    }
    check(vm.allCustomersWithBalance.value.size >= 4) { "Örnek veri yüklenemedi" }
    return vm
  }

  @Test
  open fun screens() {
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
                Screen.Pricing -> com.example.ui.screens.PricingScreen(viewModel = vm)
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
      Screen.WhatsAppTemplates to "6_sablonlar",
      Screen.Pricing to "7_paketler"
    )
    for ((screen, name) in shots) {
      current = screen
      save(null, name)
    }
    val first = vm.allCustomersWithBalance.value.maxByOrNull { it.totalDebt }!!.customer.id
    vm.selectCustomer(first)
    val dl = System.currentTimeMillis() + 10_000
    while (System.currentTimeMillis() < dl && vm.selectedCustomerTransactions.value.isEmpty()) {
      ShadowLooper.idleMainLooper(); Thread.sleep(50)
    }
    current = Screen.CustomerDetail(first)
    save(null, "3_musteri_detay")
  }

  private fun idleLooperOnly() {
    repeat(20) { ShadowLooper.idleMainLooper(); Thread.sleep(25) }
  }

  private fun fakeCustomers() = listOf(
    com.example.model.CustomerWithBalance(
      customer = com.example.model.Customer(id = 1, name = "Ahmet Yılmaz (Yılmaz Market)", phone = "05325551020"),
      totalDebt = 6300.0, totalPaid = 2000.0, remainingBalance = 4300.0, overdueAmount = 2500.0
    ),
    com.example.model.CustomerWithBalance(
      customer = com.example.model.Customer(id = 2, name = "Mehmet Demir", phone = "05434443040"),
      totalDebt = 7500.0, totalPaid = 4000.0, remainingBalance = 3500.0
    )
  )

  @Test
  fun dialogCollection() {
    org.junit.Assume.assumeTrue(dialogs)
    val customers = fakeCustomers()
    rule.setContent {
      MyApplicationTheme(themeMode = AppThemeMode.DEFTER) {
        AddCollectionDialog(preselectedCustomerId = customers.first { it.remainingBalance > 0 }.customer.id, allCustomers = customers, onDismiss = {}, onSave = { _, _, _, _, _ -> })
      }
    }
    idleLooperOnly()
    val dialog = rootViews().lastOrNull { it !== rule.activity.window.decorView } ?: return
    saveView(dialog, "8_tahsilat_al")
  }

  @Test
  fun dialogDebt() {
    org.junit.Assume.assumeTrue(dialogs)
    val customers = fakeCustomers()
    rule.setContent {
      MyApplicationTheme(themeMode = AppThemeMode.DEFTER) {
        AddDebtDialog(preselectedCustomerId = customers.first().customer.id, allCustomers = customers, onDismiss = {}, onSave = { _, _, _, _, _, _ -> })
      }
    }
    idleLooperOnly()
    val dialog = rootViews().lastOrNull { it !== rule.activity.window.decorView } ?: return
    saveView(dialog, "7_borc_ekle")
  }

  @Test
  fun dialogDebtPicker() {
    org.junit.Assume.assumeTrue(dialogs)
    val customers = fakeCustomers()
    rule.setContent {
      MyApplicationTheme(themeMode = AppThemeMode.DEFTER) {
        AddDebtDialog(allCustomers = customers, onDismiss = {}, onSave = { _, _, _, _, _, _ -> })
      }
    }
    idleLooperOnly()
    val dialog = rootViews().lastOrNull { it !== rule.activity.window.decorView } ?: return
    saveView(dialog, "9_borc_ekle_secici")
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

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class ScreenshotDialogTest : ScreenshotBase() {
  override val tag = "d"
  override val dialogs = true
  override fun screens() {}
}
