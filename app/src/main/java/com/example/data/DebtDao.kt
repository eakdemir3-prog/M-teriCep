package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.Debt
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtDao {
  @Query("SELECT * FROM debts WHERE isDeleted = 0 ORDER BY date DESC, id DESC")
  fun getAllDebts(): Flow<List<Debt>>

  @Query("SELECT * FROM debts WHERE isDeleted = 0")
  suspend fun getAllDebtsOnce(): List<Debt>

  /** Yedekleme için silinmiş işaretli olanlar dahil hepsi. */
  @Query("SELECT * FROM debts")
  suspend fun getEverything(): List<Debt>

  @Query("SELECT * FROM debts WHERE customerId = :customerId AND isDeleted = 0 ORDER BY date DESC, id DESC")
  fun getDebtsForCustomer(customerId: Long): Flow<List<Debt>>

  @Query("SELECT * FROM debts WHERE id = :id LIMIT 1")
  suspend fun getById(id: Long): Debt?

  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insertDebt(debt: Debt): Long

  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insertAll(debts: List<Debt>): List<Long>

  @Update
  suspend fun updateDebt(debt: Debt)

  @Query("UPDATE debts SET isDeleted = :deleted, updatedAt = :now WHERE id = :id")
  suspend fun setDeleted(id: Long, deleted: Boolean, now: Long)

  @Query("DELETE FROM debts")
  suspend fun clearAll()
}
