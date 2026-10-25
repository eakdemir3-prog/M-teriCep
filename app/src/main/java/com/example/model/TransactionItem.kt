package com.example.model

sealed class TransactionItem {
  abstract val id: Long
  abstract val customerId: Long
  abstract val amount: Double
  abstract val date: Long
  abstract val notesOrDesc: String

  data class DebtItem(
    override val id: Long,
    override val customerId: Long,
    override val amount: Double,
    override val date: Long,
    override val notesOrDesc: String,
    val dueDate: Long?,
    val category: String,
    val status: DebtStatus
  ) : TransactionItem()

  data class CollectionItem(
    override val id: Long,
    override val customerId: Long,
    override val amount: Double,
    override val date: Long,
    override val notesOrDesc: String,
    val paymentMethod: PaymentMethod,
    val debtId: Long?
  ) : TransactionItem()
}
