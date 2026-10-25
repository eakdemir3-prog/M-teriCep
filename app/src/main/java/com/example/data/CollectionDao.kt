package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.CollectionRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {
  @Query("SELECT * FROM collections WHERE isDeleted = 0 ORDER BY date DESC, id DESC")
  fun getAllCollections(): Flow<List<CollectionRecord>>

  @Query("SELECT * FROM collections WHERE isDeleted = 0")
  suspend fun getAllCollectionsOnce(): List<CollectionRecord>

  @Query("SELECT * FROM collections")
  suspend fun getEverything(): List<CollectionRecord>

  @Query("SELECT * FROM collections WHERE customerId = :customerId AND isDeleted = 0 ORDER BY date DESC, id DESC")
  fun getCollectionsForCustomer(customerId: Long): Flow<List<CollectionRecord>>

  @Query("SELECT * FROM collections WHERE id = :id LIMIT 1")
  suspend fun getById(id: Long): CollectionRecord?

  /** Çift kayıt kontrolü: aynı müşteriye, aynı tutarda, yakın zamanda girilmiş tahsilat. */
  @Query(
    "SELECT * FROM collections WHERE customerId = :customerId AND isDeleted = 0 " +
      "AND ABS(amount - :amount) < 0.005 AND createdAt > :createdAfter"
  )
  suspend fun findRecentDuplicate(customerId: Long, amount: Double, createdAfter: Long): List<CollectionRecord>

  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insertCollection(collection: CollectionRecord): Long

  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insertAll(collections: List<CollectionRecord>): List<Long>

  @Update
  suspend fun updateCollection(collection: CollectionRecord)

  @Query("UPDATE collections SET isDeleted = :deleted, updatedAt = :now WHERE id = :id")
  suspend fun setDeleted(id: Long, deleted: Boolean, now: Long)

  @Query("DELETE FROM collections")
  suspend fun clearAll()
}
