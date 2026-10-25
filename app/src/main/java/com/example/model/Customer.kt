package com.example.model

import androidx.room.ColumnInfo
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
  val createdAt: Long = System.currentTimeMillis(),
  /** Örnek (demo) veri mi? Gerçek kayıtlarla karışmasın diye işaretlenir. */
  @ColumnInfo(defaultValue = "0")
  val isDemo: Boolean = false
)

data class CustomerWithBalance(
  val customer: Customer,
  val totalDebt: Double,
  val totalPaid: Double,
  val remainingBalance: Double,
  val overdueDebtCount: Int = 0,
  val lastTransactionDate: Long? = null,
  /** Vadesi geçmiş ve henüz ödenmemiş kısım. */
  val overdueAmount: Double = 0.0,
  /** Ödenmemiş borçlar arasındaki en yakın vade tarihi. */
  val nextDueDate: Long? = null,
  val lastPaymentAmount: Double? = null,
  val lastPaymentDate: Long? = null
)

enum class CustomerFilter {
  ALL,
  WITH_DEBT,
  OVERDUE,
  ZERO_BALANCE,
  ARCHIVED
}

enum class CustomerSort {
  NAME_ASC,
  NAME_DESC,
  DEBT_DESC,
  RECENT_ACTIVITY
}
