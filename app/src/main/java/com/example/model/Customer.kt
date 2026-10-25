package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class Customer(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val name: String,
  val phone: String,
  val email: String = "",
  val address: String = "",
  val notes: String = "",
  val isArchived: Boolean = false,
  val createdAt: Long = System.currentTimeMillis()
)

data class CustomerWithBalance(
  val customer: Customer,
  val totalDebt: Double,
  val totalPaid: Double,
  val remainingBalance: Double,
  val overdueDebtCount: Int = 0,
  val lastTransactionDate: Long? = null
)

enum class CustomerFilter {
  ALL,
  WITH_DEBT,
  ZERO_BALANCE,
  ARCHIVED
}

enum class CustomerSort {
  NAME_ASC,
  NAME_DESC,
  DEBT_DESC,
  RECENT_ACTIVITY
}
