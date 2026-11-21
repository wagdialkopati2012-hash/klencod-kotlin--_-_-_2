package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
  @Query("SELECT * FROM users ORDER BY id ASC")
  fun observeAllUsers(): Flow<List<UserEntity>>

  @Query("SELECT * FROM users ORDER BY id ASC")
  suspend fun getAllUsersOnce(): List<UserEntity>

  @Query("SELECT * FROM users WHERE LOWER(username) = LOWER(:username) LIMIT 1")
  suspend fun findByUsername(username: String): UserEntity?

  @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
  suspend fun findById(id: Long): UserEntity?

  @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
  fun observeById(id: Long): Flow<UserEntity?>

  @Query("SELECT COUNT(*) FROM users")
  suspend fun countUsers(): Int

  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insert(user: UserEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAllReplace(users: List<UserEntity>)

  @Update
  suspend fun update(user: UserEntity)

  @Query("UPDATE users SET lastActivityAt = :timestamp WHERE id = :userId")
  suspend fun updateLastActivity(userId: Long, timestamp: Long)

  @Query("DELETE FROM users WHERE id = :id")
  suspend fun deleteById(id: Long)

  @Query("DELETE FROM users")
  suspend fun deleteAll()
}

@Dao
interface CompanyInfoDao {
  @Query("SELECT * FROM company_info WHERE id = 1 LIMIT 1")
  fun observeCompanyInfo(): Flow<CompanyInfoEntity?>

  @Query("SELECT * FROM company_info WHERE id = 1 LIMIT 1")
  suspend fun getCompanyInfoOnce(): CompanyInfoEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(info: CompanyInfoEntity)

  @Query("DELETE FROM company_info")
  suspend fun deleteAll()
}

@Dao
interface ProductDao {
  @Query("SELECT * FROM products ORDER BY id DESC")
  fun observeAllProducts(): Flow<List<ProductEntity>>

  @Query("SELECT * FROM products ORDER BY id ASC")
  suspend fun getAllProductsOnce(): List<ProductEntity>

  @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
  suspend fun findById(id: Long): ProductEntity?

  @Query("SELECT * FROM products WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name)) LIMIT 1")
  suspend fun findByNormalizedName(name: String): ProductEntity?

  @Query("SELECT COUNT(*) FROM products")
  fun observeProductCount(): Flow<Int>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(product: ProductEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(products: List<ProductEntity>)

  @Update
  suspend fun update(product: ProductEntity)

  @Query("DELETE FROM products WHERE id = :id")
  suspend fun deleteById(id: Long)

  @Query("DELETE FROM products WHERE id IN (:ids)")
  suspend fun deleteByIds(ids: List<Long>)

  @Query("DELETE FROM products")
  suspend fun deleteAll()
}

@Dao
interface QuotationDao {
  @Transaction
  @Query("SELECT * FROM quotations ORDER BY quotationDateMillis DESC, id DESC")
  fun observeAllQuotationsWithItems(): Flow<List<QuotationWithItems>>

  @Transaction
  @Query("SELECT * FROM quotations WHERE id = :id LIMIT 1")
  fun observeQuotationWithItems(id: Long): Flow<QuotationWithItems?>

  @Transaction
  @Query("SELECT * FROM quotations WHERE id = :id LIMIT 1")
  suspend fun getQuotationWithItemsOnce(id: Long): QuotationWithItems?

  @Query("SELECT * FROM quotations ORDER BY id ASC")
  suspend fun getAllQuotationsOnce(): List<QuotationEntity>

  @Query("SELECT * FROM quotation_items ORDER BY id ASC")
  suspend fun getAllQuotationItemsOnce(): List<QuotationItemEntity>

  @Query("SELECT * FROM quotations WHERE quotationNumber = :number LIMIT 1")
  suspend fun findByNumber(number: String): QuotationEntity?

  @Query("SELECT COUNT(*) FROM quotations")
  suspend fun countQuotations(): Int

  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insertQuotation(quotation: QuotationEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertQuotationReplace(quotation: QuotationEntity): Long

  @Update
  suspend fun updateQuotation(quotation: QuotationEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertItems(items: List<QuotationItemEntity>)

  @Query("DELETE FROM quotation_items WHERE quotationId = :quotationId")
  suspend fun deleteItemsByQuotationId(quotationId: Long)

  @Query("DELETE FROM quotations WHERE id = :id")
  suspend fun deleteQuotationById(id: Long)

  @Query("DELETE FROM quotation_items")
  suspend fun deleteAllItems()

  @Query("DELETE FROM quotations")
  suspend fun deleteAllQuotations()

  @Transaction
  suspend fun saveQuotationWithItems(
    quotation: QuotationEntity,
    items: List<QuotationItemEntity>,
    isUpdate: Boolean,
  ): Long {
    val qId =
      if (isUpdate) {
        updateQuotation(quotation)
        deleteItemsByQuotationId(quotation.id)
        quotation.id
      } else {
        insertQuotation(quotation)
      }
    val attachedItems = items.map { it.copy(id = 0, quotationId = qId) }
    if (attachedItems.isNotEmpty()) {
      insertItems(attachedItems)
    }
    return qId
  }
}

@Dao
interface AuditLogDao {
  @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC, id DESC")
  fun observeAllLogs(): Flow<List<AuditLogEntity>>

  @Query("SELECT * FROM audit_logs ORDER BY timestamp ASC, id ASC")
  suspend fun getAllLogsOnce(): List<AuditLogEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(log: AuditLogEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(logs: List<AuditLogEntity>)

  @Query("DELETE FROM audit_logs")
  suspend fun deleteAll()
}
