package com.example.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
  tableName = "users",
  indices = [Index(value = ["username"], unique = true)],
)
data class UserEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val username: String,
  val fullName: String,
  val passwordHash: String,
  val passwordSalt: String,
  val role: String,
  val isActive: Boolean = true,
  val permissionsJson: String,
  val lastActivityAt: Long? = null,
  val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "company_info")
data class CompanyInfoEntity(
  @PrimaryKey val id: Int = 1,
  val companyName: String = "",
  val companySubtitle: String = "",
  val phone: String = "",
  val address: String = "",
  val extraInfo: String = "",
  val logoPath: String? = null,
  val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
  tableName = "products",
  indices = [Index(value = ["name"])],
)
data class ProductEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val code: String = "",
  val name: String,
  val priceUsd: String,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
  tableName = "quotations",
  indices = [Index(value = ["quotationNumber"], unique = true)],
)
data class QuotationEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val quotationNumber: String,
  val customerName: String,
  val quotationDateMillis: Long,
  val notes: String = "",
  val status: String = QuotationStatus.SAVED.name,
  val totalAmountUsd: String = "0.00",
  val createdByUserId: Long,
  val createdByUsername: String,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
  tableName = "quotation_items",
  foreignKeys = [
    ForeignKey(
      entity = QuotationEntity::class,
      parentColumns = ["id"],
      childColumns = ["quotationId"],
      onDelete = ForeignKey.CASCADE,
    )
  ],
  indices = [Index(value = ["quotationId"])],
)
data class QuotationItemEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val quotationId: Long,
  val productId: Long? = null,
  val productNameSnapshot: String,
  val unitPriceUsdSnapshot: String,
  val quantity: Int,
  val lineTotalUsd: String,
)

data class QuotationWithItems(
  @Embedded val quotation: QuotationEntity,
  @Relation(
    parentColumn = "id",
    entityColumn = "quotationId",
  )
  val items: List<QuotationItemEntity>,
)

@Entity(
  tableName = "audit_logs",
  indices = [Index(value = ["timestamp"]), Index(value = ["username"])],
)
data class AuditLogEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val userId: Long,
  val username: String,
  val operationType: String,
  val affectedEntity: String,
  val affectedRecordId: String,
  val description: String,
  val previousValue: String? = null,
  val newValue: String? = null,
  val timestamp: Long = System.currentTimeMillis(),
)

enum class QuotationStatus(val arabicLabel: String) {
  DRAFT("مسودة"),
  SAVED("محفوظ"),
  SENT("مرسل"),
  APPROVED("معتمد");

  companion object {
    fun fromString(value: String): QuotationStatus =
      entries.firstOrNull { it.name == value } ?: SAVED
  }
}

enum class AuditOperationType(val arabicLabel: String) {
  LOGIN("تسجيل دخول"),
  LOGOUT("تسجيل خروج"),
  CREATE("إنشاء"),
  UPDATE("تعديل"),
  DELETE("حذف"),
  IMPORT("استيراد"),
  EXPORT("تصدير"),
  BACKUP("نسخ احتياطي"),
  RESTORE("استعادة نسخة"),
  PERMISSION_CHANGE("تعديل صلاحيات");

  companion object {
    fun fromString(value: String): AuditOperationType =
      entries.firstOrNull { it.name == value } ?: UPDATE
  }
}
