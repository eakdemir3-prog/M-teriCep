package com.example.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = "collections",
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
data class CollectionRecord(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val customerId: Long,
  val debtId: Long? = null,
  val amount: Double,
  val date: Long = System.currentTimeMillis(),
  val paymentMethod: PaymentMethod = PaymentMethod.CASH,
  val notes: String = "",
  val createdAt: Long = System.currentTimeMillis()
)

enum class PaymentMethod(val label: String) {
  CASH("Nakit"),
  BANK_TRANSFER("Havale / EFT"),
  CREDIT_CARD("Kredi / Banka Kartı"),
  CHECK_PROMISSORY("Çek / Senet"),
  OTHER("Diğer")
}
