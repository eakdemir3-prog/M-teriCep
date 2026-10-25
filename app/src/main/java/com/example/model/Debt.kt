package com.example.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = "debts",
  foreignKeys = [
    ForeignKey(
      entity = Customer::class,
      parentColumns = ["id"],
      childColumns = ["customerId"],
      onDelete = ForeignKey.CASCADE
    )
  ],
  indices = [Index(value = ["customerId"])]
)
data class Debt(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val customerId: Long,
  val amount: Double,
  val date: Long = System.currentTimeMillis(),
  val dueDate: Long? = null,
  val description: String = "",
  val category: String = "Genel Satış",
  val createdAt: Long = System.currentTimeMillis()
)

enum class DebtStatus(val label: String) {
  UNPAID("Ödenmedi"),
  PARTIALLY_PAID("Kısmen Ödendi"),
  PAID("Ödendi"),
  OVERDUE("Vadesi Geçti")
}

object DebtCategories {
  val defaultCategories = listOf(
    "Genel Satış",
    "Ürün Satışı",
    "Hizmet & İşçilik",
    "Taksit",
    "Mal / Toptan Alım",
    "Açık Hesap",
    "Diğer"
  )
}
