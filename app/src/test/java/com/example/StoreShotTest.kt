package com.example

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.model.AppThemeMode
import com.example.model.CollectionRecord
import com.example.model.Customer
import com.example.model.Debt
import com.example.model.PaymentMethod
import com.example.ui.components.MusteriCepBottomBar
import com.example.ui.screens.CollectionsScreen
import com.example.ui.screens.CustomerDetailScreen
import com.example.ui.screens.CustomersScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ReportsAndExportScreen
import com.example.ui.screens.WhatsAppTemplatesScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MusteriCepViewModel
import com.example.viewmodel.Screen
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper

/** Play Store görselleri için gerçekçi (örnek olarak işaretlenmemiş) verilerle ekranlar. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class StoreShotTest {
  @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

  private fun idle() = repeat(6) { ShadowLooper.idleMainLooper(); rule.waitForIdle(); Thread.sleep(40) }

  private fun save(name: String) {
    idle()
    val v = rule.activity.window.decorView
    val bmp = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
    v.draw(Canvas(bmp))
    val dir = File("build/shots").apply { mkdirs() }
    File(dir, "store_$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
  }

  private fun seed(app: Application) = runBlocking {
    val db = AppDatabase.getDatabase(app)
    // Aynı süreçteki diğer testlerin örnek verileri karışmasın.
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { db.clearAllTables() }
    com.example.data.SettingsStore(app).setTier(com.example.model.SubscriptionTier.PRO)
    val c = db.customerDao(); val d = db.debtDao(); val k = db.collectionDao()
    val day = 86_400_000L
    val now = System.currentTimeMillis()
    fun cu(n: String, p: String, a: String = "", note: String = "") = Customer(name = n, phone = p, address = a, notes = note)
    val c1 = c.insertCustomer(cu("Elif Şahin", "0532 418 22 61", "Kadıköy/İstanbul", "Saç bakım paketi, aylık öder."))
    val c2 = c.insertCustomer(cu("Burak Aydın", "0543 902 15 37", "Üsküdar/İstanbul"))
    val c3 = c.insertCustomer(cu("Zeynep Koç", "0505 611 48 90", "Ataşehir/İstanbul", "Gelin paketi"))
    val c4 = c.insertCustomer(cu("Mert Yıldız", "0533 274 66 12"))
    val c5 = c.insertCustomer(cu("Selin Arslan", "0542 385 70 04"))
    val c6 = c.insertCustomer(cu("Deniz Kaplan", "0506 147 93 28"))
    val c7 = c.insertCustomer(cu("Ece Demirtaş", "0530 668 21 55"))

    d.insertDebt(Debt(customerId = c1, amount = 3600.0, date = now - 40 * day, dueDate = now - 6 * day, description = "Keratin bakım paketi (3 seans)", category = "Hizmet & İşçilik"))
    d.insertDebt(Debt(customerId = c1, amount = 900.0, date = now - 3 * day, dueDate = now + 4 * day, description = "Saç boyama", category = "Hizmet & İşçilik"))
    k.insertCollection(CollectionRecord(customerId = c1, amount = 1500.0, date = now - 20 * day, paymentMethod = PaymentMethod.CASH, notes = "1. taksit", createdAt = now - 20 * day))
    d.insertDebt(Debt(customerId = c2, amount = 2400.0, date = now - 12 * day, dueDate = now + 2 * day, description = "Sakal + cilt bakımı aylık paket", category = "Taksit"))
    k.insertCollection(CollectionRecord(customerId = c2, amount = 1200.0, date = now - 2 * 3_600_000L, paymentMethod = PaymentMethod.CREDIT_CARD, notes = "POS", createdAt = now - 2 * 3_600_000L))
    d.insertDebt(Debt(customerId = c3, amount = 15000.0, date = now - 25 * day, dueDate = now + 20 * day, description = "Gelin saçı ve makyaj paketi", category = "Hizmet & İşçilik"))
    k.insertCollection(CollectionRecord(customerId = c3, amount = 6000.0, date = now - day, paymentMethod = PaymentMethod.BANK_TRANSFER, notes = "Kapora", createdAt = now - day))
    d.insertDebt(Debt(customerId = c4, amount = 750.0, date = now - 9 * day, dueDate = now - 2 * day, description = "Saç kesimi + yıkama", category = "Hizmet & İşçilik"))
    d.insertDebt(Debt(customerId = c5, amount = 1800.0, date = now - 6 * day, dueDate = now + 6 * day, description = "Protez tırnak", category = "Hizmet & İşçilik"))
    k.insertCollection(CollectionRecord(customerId = c5, amount = 800.0, date = now - 5 * 3_600_000L, paymentMethod = PaymentMethod.CASH, notes = "Elden", createdAt = now - 5 * 3_600_000L))
    d.insertDebt(Debt(customerId = c6, amount = 1250.0, date = now - 14 * day, dueDate = now - 4 * day, description = "Ürün satışı: şampuan seti", category = "Ürün Satışı"))
    k.insertCollection(CollectionRecord(customerId = c6, amount = 1250.0, date = now - 8 * day, paymentMethod = PaymentMethod.BANK_TRANSFER, notes = "EFT", createdAt = now - 8 * day))
    d.insertDebt(Debt(customerId = c7, amount = 2200.0, date = now - 10 * day, dueDate = now + 1 * day, description = "Lazer epilasyon 2. seans", category = "Taksit"))
    k.insertCollection(CollectionRecord(customerId = c7, amount = 1000.0, date = now - 9 * day, paymentMethod = PaymentMethod.CREDIT_CARD, notes = "", createdAt = now - 9 * day))
    k.insertCollection(CollectionRecord(customerId = c3, amount = 2500.0, date = now - 10 * day, paymentMethod = PaymentMethod.CASH, notes = "", createdAt = now - 10 * day))
    c3
  }

  @Test
  fun store() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val detailId = seed(app)
    val vm = MusteriCepViewModel(app)
    vm.updateBusinessProfile("Lara Güzellik Salonu", "Emre", "0216 555 40 40", "İstanbul")
    val deadline = System.currentTimeMillis() + 20_000
    while (System.currentTimeMillis() < deadline &&
      !(vm.allCustomersWithBalance.value.size >= 7 && vm.allCollections.value.size >= 6 && vm.profile.value.isProfileComplete)) {
      ShadowLooper.idleMainLooper(); Thread.sleep(50)
    }
    var current: Screen by mutableStateOf(Screen.Dashboard)
    rule.setContent {
      MyApplicationTheme(themeMode = AppThemeMode.DEFTER) {
        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
          Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
              when (current) {
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
    for ((s, n) in listOf(
      Screen.Dashboard to "ana", Screen.Customers to "musteriler", Screen.Collections to "kasa",
      Screen.ReportsAndExport to "raporlar", Screen.WhatsAppTemplates to "sablonlar"
    )) { current = s; save(n) }
    vm.selectCustomer(detailId)
    val dl = System.currentTimeMillis() + 10_000
    while (System.currentTimeMillis() < dl && vm.selectedCustomerTransactions.value.isEmpty()) { ShadowLooper.idleMainLooper(); Thread.sleep(50) }
    current = Screen.CustomerDetail(detailId)
    save("detay")
  }
}
