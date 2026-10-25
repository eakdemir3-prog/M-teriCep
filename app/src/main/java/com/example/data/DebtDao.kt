package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.Debt
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtDao {
  @Query("SELECT * FROM debts ORDER BY date DESC")
  fun getAllDebts(): Flow<List<Debt>>

  @Query("SELECT * FROM debts WHERE customerId = :customerId ORDER BY date DESC")
  fun getDebtsForCustomer(customerId: Long): Flow<List<Debt>>

  @Query("SELECT * FROM debts WHERE dueDate IS NOT NULL AND dueDate < :currentTimestamp ORDER BY dueDate ASC")
  fun getOverdueDebts(currentTimestamp: Long): Flow<List<Debt>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertDebt(debt: Debt): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(debts: List<Debt>): List<Long>

  @Update
  suspend fun updateDebt(debt: Debt)

  @Delete
  suspend fun deleteDebt(debt: Debt)

  @Query("DELETE FROM debts")
  suspend fun clearAll()
}
