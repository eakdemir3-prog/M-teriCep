package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.CollectionRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {
  @Query("SELECT * FROM collections ORDER BY date DESC")
  fun getAllCollections(): Flow<List<CollectionRecord>>

  @Query("SELECT * FROM collections WHERE customerId = :customerId ORDER BY date DESC")
  fun getCollectionsForCustomer(customerId: Long): Flow<List<CollectionRecord>>

  @Query("SELECT * FROM collections WHERE customerId = :customerId AND amount = :amount AND date > :afterTimestamp")
  suspend fun findRecentDuplicate(customerId: Long, amount: Double, afterTimestamp: Long): List<CollectionRecord>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCollection(collection: CollectionRecord): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(collections: List<CollectionRecord>): List<Long>

  @Update
  suspend fun updateCollection(collection: CollectionRecord)

  @Delete
  suspend fun deleteCollection(collection: CollectionRecord)

  @Query("DELETE FROM collections")
  suspend fun clearAll()
}
