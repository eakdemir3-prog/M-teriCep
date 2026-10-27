package com.example.model

import androidx.room.ColumnInfo
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
  val createdAt: Long = System.currentTimeMillis(),
  @ColumnInfo(defaultValue = "0")
  val updatedAt: Long = System.currentTimeMillis(),
  @ColumnInfo(defaultValue = "0")
  val isDeleted: Boolean = false
)

enum class PaymentMethod(val label: String, val shortLabel: String) {
  CASH("Nakit", "Nakit"),
  BANK_TRANSFER("Havale / EFT", "Havale"),
  CREDIT_CARD("Kredi / Banka Kartı", "Kart"),
  CHECK_PROMISSORY("Çek / Senet", "Çek/Senet"),
  OTHER("Diğer", "Diğer")
}
