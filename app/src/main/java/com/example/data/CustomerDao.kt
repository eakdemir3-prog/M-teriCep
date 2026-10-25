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

  @Query("SELECT * FROM customers WHERE isArchived = 0 ORDER BY name ASC")
  fun getActiveCustomers(): Flow<List<Customer>>

  @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
  fun getCustomerById(id: Long): Flow<Customer?>

  @Query("SELECT * FROM customers WHERE phone = :phone AND id != :excludeId LIMIT 1")
  suspend fun findCustomerByPhone(phone: String, excludeId: Long = 0): Customer?

  @Query("SELECT COUNT(*) FROM customers WHERE isArchived = 0")
  fun getActiveCustomerCount(): Flow<Int>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCustomer(customer: Customer): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(customers: List<Customer>): List<Long>

  @Update
  suspend fun updateCustomer(customer: Customer)

  @Query("UPDATE customers SET isArchived = :isArchived WHERE id = :id")
  suspend fun setArchived(id: Long, isArchived: Boolean)

  @Delete
  suspend fun deleteCustomer(customer: Customer)

  @Query("DELETE FROM customers")
  suspend fun clearAll()
}
