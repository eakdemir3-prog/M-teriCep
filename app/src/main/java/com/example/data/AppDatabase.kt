package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.model.CollectionRecord
import com.example.model.Customer
import com.example.model.Debt
import com.example.model.WhatsAppTemplate

@Database(
  entities = [
    Customer::class,
    Debt::class,
    CollectionRecord::class,
    WhatsAppTemplate::class
  ],
  version = 2,
  exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun customerDao(): CustomerDao
  abstract fun debtDao(): DebtDao
  abstract fun collectionDao(): CollectionDao
  abstract fun templateDao(): TemplateDao

  companion object {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    /**
     * v1 -> v2: örnek veri işareti, kayıt güncelleme zamanı ve "silindi" işareti eklendi.
     * Kullanıcı verisi korunur. (Eski sürümdeki "fallbackToDestructiveMigration" her
     * şema değişikliğinde tüm kayıtları siliyordu; kaldırıldı.)
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE customers ADD COLUMN isDemo INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE debts ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE debts ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE collections ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE collections ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE debts SET updatedAt = createdAt")
        db.execSQL("UPDATE collections SET updatedAt = createdAt")
      }
    }

    fun getDatabase(context: Context): AppDatabase {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "mustericep_database"
        )
          .addMigrations(MIGRATION_1_2)
          .build()
          .also { INSTANCE = it }
      }
    }
  }
}
