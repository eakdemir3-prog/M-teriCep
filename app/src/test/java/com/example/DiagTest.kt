package com.example

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class DiagTest {
  @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

  private fun fake() = listOf(
    com.example.model.CustomerWithBalance(
      customer = com.example.model.Customer(id = 1, name = "Ahmet", phone = "05325551020"),
      totalDebt = 6300.0, totalPaid = 2000.0, remainingBalance = 4300.0
    )
  )

  @Test fun a_trivialDialog() {
    rule.setContent { MyApplicationTheme { Dialog(onDismissRequest = {}) { Text("x") } } }
  }

  @Test fun b_debtEmpty() {
    rule.setContent { MyApplicationTheme { com.example.ui.components.AddDebtDialog(allCustomers = emptyList(), preselectedCustomerId = 1L, onDismiss = {}, onSave = { _, _, _, _, _, _ -> }) } }
  }

  @Test fun c_debtFake() {
    rule.setContent { MyApplicationTheme { com.example.ui.components.AddDebtDialog(allCustomers = fake(), preselectedCustomerId = 1L, onDismiss = {}, onSave = { _, _, _, _, _, _ -> }) } }
  }

  @Test fun d_collEmpty() {
    rule.setContent { MyApplicationTheme { com.example.ui.components.AddCollectionDialog(allCustomers = emptyList(), preselectedCustomerId = 1L, onDismiss = {}, onSave = { _, _, _, _, _ -> }) } }
  }
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class DiagDefaultTest {
  @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
  @Test fun b_debtEmpty() {
    rule.setContent { MyApplicationTheme { com.example.ui.components.AddDebtDialog(allCustomers = emptyList(), preselectedCustomerId = 1L, onDismiss = {}, onSave = { _, _, _, _, _, _ -> }) } }
  }
}
