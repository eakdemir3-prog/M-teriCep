package com.example.viewmodel

import com.example.model.CollectionRecord
import com.example.model.Customer
import com.example.model.CustomerWithBalance
import com.example.model.Debt
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
  object Appointments : Screen()
  object BusinessProfile : Screen()
}

sealed class DialogState {
  object None : DialogState()
  data class AddEditCustomer(val existingCustomer: Customer? = null) : DialogState()

  /** [existing] doluysa düzenleme modunda açılır. */
  data class AddDebt(val preselectedCustomerId: Long? = null, val existing: Debt? = null) : DialogState()

  data class AddCollection(
    val preselectedCustomerId: Long? = null,
    val defaultAmount: Double? = null,
    val existing: CollectionRecord? = null
  ) : DialogState()

  data class SendWhatsApp(val customerWithBalance: CustomerWithBalance) : DialogState()
  data class EditTemplate(val template: WhatsAppTemplate) : DialogState()
  data class CsvImportPreview(
    val validList: List<Customer>,
    val errors: List<String>,
    val skipped: List<String> = emptyList()
  ) : DialogState()

  data class ConfirmDelete(
    val title: String,
    val message: String,
    val onConfirm: () -> Unit
  ) : DialogState()

  /** Genel onay penceresi (fazla ödeme, çift kayıt şüphesi, geri yükleme...). */
  data class ConfirmAction(
    val title: String,
    val message: String,
    val confirmLabel: String,
    val isDestructive: Boolean = false,
    val onConfirm: () -> Unit
  ) : DialogState()

  data class TierLimitReached(val currentCount: Int, val maxLimit: Int) : DialogState()
  data class QuickCalculator(val onApplyAmount: (Double) -> Unit) : DialogState()
}

/** Ekranın altında gösterilen kısa bilgi; isteğe bağlı "Geri Al" gibi bir düğmesi olabilir. */
data class UiMessage(
  val text: String,
  val actionLabel: String? = null,
  val onAction: (() -> Unit)? = null
)

data class DashboardSummary(
  val totalReceivable: Double = 0.0,
  val totalCollected: Double = 0.0,
  val overdueAmount: Double = 0.0,
  val activeCustomerCount: Int = 0,
  val overdueDebtsCount: Int = 0,
  val overdueCustomerCount: Int = 0,
  val debtorCount: Int = 0,
  val upcomingAmount: Double = 0.0,
  val upcomingCount: Int = 0,
  val todayCollected: Double = 0.0,
  val thisWeekCollected: Double = 0.0,
  val thisMonthCollected: Double = 0.0,
  val lastMonthCollected: Double = 0.0
)
