package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.Customer
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
  @Query("SELECT * FROM customers ORDER BY name ASC")
  fun getAllCustomers(): Flow<List<Customer>>

  @Query("SELECT * FROM customers ORDER BY name ASC")
  suspend fun getAllCustomersOnce(): List<Customer>

  @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
  suspend fun getCustomerById(id: Long): Customer?

  /** Paket limiti için: örnek veriler hariç tüm müşteriler (arşivdekiler dahil). */
  @Query("SELECT COUNT(*) FROM customers WHERE isDemo = 0")
  suspend fun countRealCustomers(): Int

  @Query("SELECT COUNT(*) FROM customers WHERE isDemo = 1")
  suspend fun countDemoCustomers(): Int

  // Yeni kayıt: çakışmada eskisini SİLMEMESİ için ABORT (REPLACE, bağlı borçları da silerdi).
  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insertCustomer(customer: Customer): Long

  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insertAll(customers: List<Customer>): List<Long>

  @Update
  suspend fun updateCustomer(customer: Customer)

  @Query("UPDATE customers SET isArchived = :isArchived WHERE id = :id")
  suspend fun setArchived(id: Long, isArchived: Boolean)

  @Delete
  suspend fun deleteCustomer(customer: Customer)

  @Query("DELETE FROM customers WHERE isDemo = 1")
  suspend fun deleteDemoCustomers()

  @Query("DELETE FROM customers")
  suspend fun clearAll()
}
