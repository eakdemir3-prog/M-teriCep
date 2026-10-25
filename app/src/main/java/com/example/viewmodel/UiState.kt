package com.example.viewmodel

import com.example.model.Customer
import com.example.model.CustomerFilter
import com.example.model.CustomerSort
import com.example.model.CustomerWithBalance
import com.example.model.Debt
import com.example.model.CollectionRecord
import com.example.model.WhatsAppTemplate

sealed class Screen {
  object Dashboard : Screen()
  object Customers : Screen()
  object Collections : Screen()
  object Settings : Screen()
  data class CustomerDetail(val customerId: Long) : Screen()
  object WhatsAppTemplates : Screen()
  object ReportsAndExport : Screen()
  object Pricing : Screen()
}

sealed class DialogState {
  object None : DialogState()
  data class AddEditCustomer(val existingCustomer: Customer? = null) : DialogState()
  data class AddDebt(val preselectedCustomerId: Long? = null) : DialogState()
  data class AddCollection(val preselectedCustomerId: Long? = null, val defaultAmount: Double? = null) : DialogState()
  data class EditDebt(val debt: Debt) : DialogState()
  data class EditCollection(val collection: CollectionRecord) : DialogState()
  data class SendWhatsApp(val customerWithBalance: CustomerWithBalance) : DialogState()
  data class EditTemplate(val template: WhatsAppTemplate) : DialogState()
  data class CsvImportPreview(val validList: List<Customer>, val errors: List<String>) : DialogState()
  data class ConfirmDelete(
    val title: String,
    val message: String,
    val onConfirm: () -> Unit
  ) : DialogState()
  data class TierLimitReached(val currentCount: Int, val maxLimit: Int) : DialogState()
  data class QuickCalculator(val onApplyAmount: (Double) -> Unit) : DialogState()
}

data class DashboardSummary(
  val totalReceivable: Double = 0.0,
  val totalCollected: Double = 0.0,
  val overdueAmount: Double = 0.0,
  val activeCustomerCount: Int = 0,
  val overdueDebtsCount: Int = 0,
  val todayCollected: Double = 0.0,
  val thisWeekCollected: Double = 0.0,
  val thisMonthCollected: Double = 0.0
)
