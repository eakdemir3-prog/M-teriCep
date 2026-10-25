package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.ui.graphics.toArgb
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.theme.isKomuta
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.Customer
import com.example.ui.components.AddCollectionDialog
import com.example.ui.components.AddDebtDialog
import com.example.ui.components.AddEditCustomerDialog
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.CsvImportPreviewDialog
import com.example.ui.components.EditTemplateDialog
import com.example.ui.components.MusteriCepBottomBar
import com.example.ui.components.QuickActionBottomSheet
import com.example.ui.components.QuickCalculatorDialog
import com.example.ui.components.SendWhatsAppDialog
import com.example.ui.components.TierLimitReachedDialog
import com.example.ui.screens.CollectionsScreen
import com.example.ui.screens.CustomerDetailScreen
import com.example.ui.screens.CustomersScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.PricingScreen
import com.example.ui.screens.ReportsAndExportScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WhatsAppTemplatesScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.DialogState
import com.example.viewmodel.MusteriCepViewModel
import com.example.viewmodel.Screen
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val viewModel: MusteriCepViewModel = viewModel()
      val loadedProfile by viewModel.loadedProfile.collectAsState()
      val profile = loadedProfile
      if (profile != null) {
        val komuta = profile.themeMode.isKomuta()
        LaunchedEffect(komuta) {
          val style = if (komuta) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
          else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
          enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
        }
        MyApplicationTheme(themeMode = profile.themeMode) {
          MusteriCepApp(viewModel)
        }
      }
    }
  }
}

@Composable
fun MusteriCepApp(
  viewModel: MusteriCepViewModel
) {
  val currentScreen by viewModel.currentScreen.collectAsState()
  val dialogState by viewModel.dialogState.collectAsState()
  val allCustomers by viewModel.allCustomersWithBalance.collectAsState()
  val templates by viewModel.allTemplates.collectAsState(initial = emptyList())
  val profile by viewModel.profile.collectAsState()

  val snackbarHostState = remember { SnackbarHostState() }
  var showQuickActionSheet by remember { mutableStateOf(false) }

  // ViewModel'den gelen kısa mesajlar (bazılarında "Geri Al" düğmesi var)
  LaunchedEffect(Unit) {
    viewModel.messages.collectLatest { msg ->
      val result = snackbarHostState.showSnackbar(
        message = msg.text,
        actionLabel = msg.actionLabel,
        withDismissAction = msg.actionLabel == null,
        duration = if (msg.actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short
      )
      if (result == SnackbarResult.ActionPerformed) msg.onAction?.invoke()
    }
  }

  // Sistem geri tuşu: alt sayfalardan geri, ana sekmelerden Ana Sayfa'ya
  BackHandler(enabled = currentScreen !is Screen.Dashboard) {
    viewModel.navigateBack()
  }

  val isPrimaryTab = currentScreen is Screen.Dashboard ||
    currentScreen is Screen.Customers ||
    currentScreen is Screen.Collections ||
    currentScreen is Screen.Settings

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets.safeDrawing,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    bottomBar = {
      if (isPrimaryTab) {
        MusteriCepBottomBar(
          currentScreen = currentScreen,
          onNavigate = { viewModel.navigate(it) },
          onQuickActionClick = { showQuickActionSheet = true }
        )
      }
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      AnimatedContent(
        targetState = currentScreen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "ScreenTransition"
      ) { screen ->
        when (screen) {
          is Screen.Dashboard -> DashboardScreen(viewModel = viewModel)
          is Screen.Customers -> CustomersScreen(viewModel = viewModel)
          is Screen.Collections -> CollectionsScreen(viewModel = viewModel)
          is Screen.Settings -> SettingsScreen(viewModel = viewModel)
          is Screen.CustomerDetail -> CustomerDetailScreen(viewModel = viewModel)
          is Screen.WhatsAppTemplates -> WhatsAppTemplatesScreen(viewModel = viewModel)
          is Screen.ReportsAndExport -> ReportsAndExportScreen(viewModel = viewModel)
          is Screen.Pricing -> PricingScreen(viewModel = viewModel)
        }
      }
    }
  }

  // Quick Action Bottom Sheet (from center FAB)
  if (showQuickActionSheet) {
    QuickActionBottomSheet(
      onDismiss = { showQuickActionSheet = false },
      onAddDebt = { viewModel.openDialog(DialogState.AddDebt()) },
      onAddCollection = { viewModel.openDialog(DialogState.AddCollection()) },
      onAddCustomer = { viewModel.openDialog(DialogState.AddEditCustomer()) },
      onOpenCalculator = {
        viewModel.openDialog(
          DialogState.QuickCalculator(
            onApplyAmount = { amt ->
              viewModel.openDialog(DialogState.AddCollection(defaultAmount = amt))
            }
          )
        )
      },
      onOpenTemplates = { viewModel.navigate(Screen.WhatsAppTemplates) }
    )
  }

  // Global Dialogs Rendering
  when (val state = dialogState) {
    is DialogState.None -> { /* No dialog */ }

    is DialogState.AddEditCustomer -> {
      AddEditCustomerDialog(
        existingCustomer = state.existingCustomer,
        allCustomers = allCustomers,
        onDismiss = { viewModel.dismissDialog() },
        onSave = { name, phone, email, address, notes ->
          viewModel.saveCustomer(
            name = name,
            phone = phone,
            email = email,
            address = address,
            notes = notes,
            existing = state.existingCustomer
          )
        }
      )
    }

    is DialogState.AddDebt -> {
      AddDebtDialog(
        preselectedCustomerId = state.preselectedCustomerId,
        existing = state.existing,
        allCustomers = allCustomers,
        onDismiss = { viewModel.dismissDialog() },
        onSave = { customerId, amount, date, dueDate, description, category ->
          viewModel.saveDebt(
            customerId = customerId,
            amount = amount,
            date = date,
            dueDate = dueDate,
            description = description,
            category = category,
            existingId = state.existing?.id ?: 0L
          )
        }
      )
    }

    is DialogState.AddCollection -> {
      AddCollectionDialog(
        preselectedCustomerId = state.preselectedCustomerId,
        defaultAmount = state.defaultAmount,
        existing = state.existing,
        allCustomers = allCustomers,
        onDismiss = { viewModel.dismissDialog() },
        onSave = { customerId, amount, date, method, notes ->
          viewModel.saveCollection(
            customerId = customerId,
            amount = amount,
            date = date,
            paymentMethod = method,
            notes = notes,
            existingId = state.existing?.id ?: 0L
          )
        }
      )
    }

    is DialogState.SendWhatsApp -> {
      SendWhatsAppDialog(
        customerWithBalance = state.customerWithBalance,
        businessName = profile.displayBusinessName,
        templates = templates,
        onDismiss = { viewModel.dismissDialog() },
        onOpenTemplates = {
          viewModel.dismissDialog()
          viewModel.navigate(Screen.WhatsAppTemplates)
        }
      )
    }

    is DialogState.EditTemplate -> {
      EditTemplateDialog(
        template = state.template,
        onDismiss = { viewModel.dismissDialog() },
        onSave = { updated -> viewModel.updateTemplate(updated) }
      )
    }

    is DialogState.QuickCalculator -> {
      QuickCalculatorDialog(
        onDismiss = { viewModel.dismissDialog() },
        onApplyAmount = state.onApplyAmount
      )
    }

    is DialogState.CsvImportPreview -> {
      CsvImportPreviewDialog(
        validCustomers = state.validList,
        errors = state.errors,
        skipped = state.skipped,
        onDismiss = { viewModel.dismissDialog() },
        onConfirm = { viewModel.executeCsvImport(state.validList) }
      )
    }

    is DialogState.ConfirmDelete -> {
      ConfirmDeleteDialog(
        title = state.title,
        message = state.message,
        onConfirm = state.onConfirm,
        onDismiss = { viewModel.dismissDialog() }
      )
    }

    is DialogState.ConfirmAction -> {
      ConfirmActionDialog(
        title = state.title,
        message = state.message,
        confirmLabel = state.confirmLabel,
        isDestructive = state.isDestructive,
        onConfirm = state.onConfirm,
        onDismiss = { viewModel.dismissDialog() }
      )
    }

    is DialogState.TierLimitReached -> {
      TierLimitReachedDialog(
        currentCount = state.currentCount,
        maxLimit = state.maxLimit,
        onDismiss = { viewModel.dismissDialog() },
        onViewPlans = { viewModel.navigate(Screen.Pricing) }
      )
    }

  }
}
