package com.example

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.window.Dialog
import com.example.ui.components.DatePickField
import com.example.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class DiagTest {
  @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

  @Test fun a_trivialDialog() {
    rule.setContent { MyApplicationTheme { Dialog(onDismissRequest = {}) { Text("x") } } }
  }

  @Test fun b_datePickInline() {
    rule.setContent { MyApplicationTheme { Column { DatePickField("Tarih", System.currentTimeMillis(), {}, false, false) } } }
  }

  @Test fun c_moneyField() {
    rule.setContent {
      MyApplicationTheme {
        androidx.compose.material3.OutlinedTextField(value = "12", onValueChange = {}, textStyle = com.example.ui.theme.moneyStyle(20),
          supportingText = { Text("a") })
      }
    }
  }
}
