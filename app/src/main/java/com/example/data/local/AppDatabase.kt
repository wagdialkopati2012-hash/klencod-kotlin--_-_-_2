package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
  entities = [
    UserEntity::class,
    CompanyInfoEntity::class,
    ProductEntity::class,
    QuotationEntity::class,
    QuotationItemEntity::class,
    AuditLogEntity::class,
  ],
  version = 1,
  exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun userDao(): UserDao
  abstract fun companyInfoDao(): CompanyInfoDao
  abstract fun productDao(): ProductDao
  abstract fun quotationDao(): QuotationDao
  abstract fun auditLogDao(): AuditLogDao

  companion object {
    const val DATABASE_NAME = "munakasat_tenders.db"

    @Volatile private var instance: AppDatabase? = null

    fun getInstance(context: Context): AppDatabase {
      return instance
        ?: synchronized(this) {
          instance
            ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME,
              )
              .fallbackToDestructiveMigration(dropAllTables = false)
              .build()
              .also { instance = it }
        }
    }
  }
}
