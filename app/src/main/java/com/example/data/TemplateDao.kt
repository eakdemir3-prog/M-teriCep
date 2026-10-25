package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.WhatsAppTemplate
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateDao {
  @Query("SELECT * FROM whatsapp_templates ORDER BY id ASC")
  fun getAllTemplates(): Flow<List<WhatsAppTemplate>>

  @Query("SELECT * FROM whatsapp_templates WHERE `key` = :key LIMIT 1")
  suspend fun getTemplateByKey(key: String): WhatsAppTemplate?

  @Query("SELECT COUNT(*) FROM whatsapp_templates")
  suspend fun count(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(templates: List<WhatsAppTemplate>)

  @Query("SELECT * FROM whatsapp_templates ORDER BY id ASC")
  suspend fun getAllOnce(): List<WhatsAppTemplate>

  @Update
  suspend fun updateTemplate(template: WhatsAppTemplate)

  @Query("DELETE FROM whatsapp_templates")
  suspend fun clearAll()
}
