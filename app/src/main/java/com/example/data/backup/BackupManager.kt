package com.example.data.backup

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.AuditLogEntity
import com.example.data.local.CompanyInfoEntity
import com.example.data.local.ProductEntity
import com.example.data.local.QuotationEntity
import com.example.data.local.QuotationItemEntity
import com.example.data.local.UserEntity
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

data class BackupRestoreResult(
  val usersRestored: Int,
  val productsRestored: Int,
  val quotationsRestored: Int,
  val logsRestored: Int,
)

data class StorageStats(
  val databaseSizeBytes: Long,
  val cacheSizeBytes: Long,
  val logosSizeBytes: Long,
)

class BackupManager(
  private val context: Context,
  private val db: AppDatabase,
) {

  suspend fun exportFullBackupJson(): String {
    val users = db.userDao().getAllUsersOnce()
    val company = db.companyInfoDao().getCompanyInfoOnce()
    val products = db.productDao().getAllProductsOnce()
    val quotations = db.quotationDao().getAllQuotationsOnce()
    val quotationItems = db.quotationDao().getAllQuotationItemsOnce()
    val auditLogs = db.auditLogDao().getAllLogsOnce()

    val root = JSONObject()
    root.put("schemaVersion", 1)
    root.put("exportedAt", System.currentTimeMillis())

    val usersArr = JSONArray()
    users.forEach { u ->
      usersArr.put(
        JSONObject().apply {
          put("id", u.id)
          put("username", u.username)
          put("fullName", u.fullName)
          put("passwordHash", u.passwordHash)
          put("passwordSalt", u.passwordSalt)
          put("role", u.role)
          put("isActive", u.isActive)
          put("permissionsJson", u.permissionsJson)
          put("lastActivityAt", u.lastActivityAt ?: JSONObject.NULL)
          put("createdAt", u.createdAt)
        }
      )
    }
    root.put("users", usersArr)

    if (company != null) {
      val logoBase64 =
        company.logoPath
          ?.takeIf { it.isNotBlank() && File(it).exists() }
          ?.let { Base64.encodeToString(File(it).readBytes(), Base64.NO_WRAP) }
      root.put(
        "companyInfo",
        JSONObject().apply {
          put("id", company.id)
          put("companyName", company.companyName)
          put("companySubtitle", company.companySubtitle)
          put("phone", company.phone)
          put("address", company.address)
          put("extraInfo", company.extraInfo)
          put("logoBase64", logoBase64 ?: JSONObject.NULL)
          put("updatedAt", company.updatedAt)
        },
      )
    }

    val productsArr = JSONArray()
    products.forEach { p ->
      productsArr.put(
        JSONObject().apply {
          put("id", p.id)
          put("code", p.code)
          put("name", p.name)
          put("priceUsd", p.priceUsd)
          put("createdAt", p.createdAt)
          put("updatedAt", p.updatedAt)
        }
      )
    }
    root.put("products", productsArr)

    val quotationsArr = JSONArray()
    quotations.forEach { q ->
      quotationsArr.put(
        JSONObject().apply {
          put("id", q.id)
          put("quotationNumber", q.quotationNumber)
          put("customerName", q.customerName)
          put("quotationDateMillis", q.quotationDateMillis)
          put("notes", q.notes)
          put("status", q.status)
          put("totalAmountUsd", q.totalAmountUsd)
          put("createdByUserId", q.createdByUserId)
          put("createdByUsername", q.createdByUsername)
          put("createdAt", q.createdAt)
          put("updatedAt", q.updatedAt)
        }
      )
    }
    root.put("quotations", quotationsArr)

    val itemsArr = JSONArray()
    quotationItems.forEach { item ->
      itemsArr.put(
        JSONObject().apply {
          put("id", item.id)
          put("quotationId", item.quotationId)
          put("productId", item.productId ?: JSONObject.NULL)
          put("productNameSnapshot", item.productNameSnapshot)
          put("unitPriceUsdSnapshot", item.unitPriceUsdSnapshot)
          put("quantity", item.quantity)
          put("lineTotalUsd", item.lineTotalUsd)
        }
      )
    }
    root.put("quotationItems", itemsArr)

    val logsArr = JSONArray()
    auditLogs.forEach { log ->
      logsArr.put(
        JSONObject().apply {
          put("id", log.id)
          put("userId", log.userId)
          put("username", log.username)
          put("operationType", log.operationType)
          put("affectedEntity", log.affectedEntity)
          put("affectedRecordId", log.affectedRecordId)
          put("description", log.description)
          put("previousValue", log.previousValue ?: JSONObject.NULL)
          put("newValue", log.newValue ?: JSONObject.NULL)
          put("timestamp", log.timestamp)
        }
      )
    }
    root.put("auditLogs", logsArr)

    return root.toString(2)
  }

  suspend fun writeBackupToUri(uri: Uri) {
    val json = exportFullBackupJson()
    context.contentResolver.openOutputStream(uri)?.use { os ->
      os.write(json.toByteArray(Charsets.UTF_8))
    } ?: throw IllegalStateException("تعذر الكتابة إلى مسار النسخ الاحتياطي")
  }

  suspend fun restoreBackupFromUri(uri: Uri): BackupRestoreResult {
    val content =
      context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
        ?: throw IllegalStateException("تعذر قراءة ملف النسخة الاحتياطية")
    return restoreFromJson(content)
  }

  suspend fun restoreFromJson(jsonText: String): BackupRestoreResult {
    val root = JSONObject(jsonText)
    if (!root.has("users") || !root.has("products") || !root.has("quotations")) {
      throw IllegalArgumentException("بنية ملف النسخة الاحتياطية غير صالحة")
    }

    val usersArr = root.getJSONArray("users")
    val users =
      (0 until usersArr.length()).map { i ->
        val obj = usersArr.getJSONObject(i)
        UserEntity(
          id = obj.getLong("id"),
          username = obj.getString("username"),
          fullName = obj.optString("fullName", obj.getString("username")),
          passwordHash = obj.getString("passwordHash"),
          passwordSalt = obj.getString("passwordSalt"),
          role = obj.getString("role"),
          isActive = obj.optBoolean("isActive", true),
          permissionsJson = obj.getString("permissionsJson"),
          lastActivityAt =
            if (obj.isNull("lastActivityAt")) null else obj.optLong("lastActivityAt"),
          createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
        )
      }
    require(users.isNotEmpty()) { "النسخة الاحتياطية لا تحتوي على أي مستخدمين" }

    val companyObj = root.optJSONObject("companyInfo")
    val companyEntity =
      companyObj?.let { obj ->
        val logoBase64 = if (obj.isNull("logoBase64")) null else obj.optString("logoBase64")
        val restoredLogoPath =
          logoBase64?.takeIf { it.isNotBlank() }?.let { b64 ->
            val logosDir = File(context.filesDir, "logos").apply { mkdirs() }
            val file = File(logosDir, "company_logo_${System.currentTimeMillis()}.png")
            file.writeBytes(Base64.decode(b64, Base64.DEFAULT))
            file.absolutePath
          }
        CompanyInfoEntity(
          id = 1,
          companyName = obj.optString("companyName", ""),
          companySubtitle = obj.optString("companySubtitle", ""),
          phone = obj.optString("phone", ""),
          address = obj.optString("address", ""),
          extraInfo = obj.optString("extraInfo", ""),
          logoPath = restoredLogoPath,
          updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
        )
      }

    val productsArr = root.getJSONArray("products")
    val products =
      (0 until productsArr.length()).map { i ->
        val obj = productsArr.getJSONObject(i)
        ProductEntity(
          id = obj.getLong("id"),
          code = obj.optString("code", ""),
          name = obj.getString("name"),
          priceUsd = obj.getString("priceUsd"),
          createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
          updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
        )
      }

    val quotationsArr = root.getJSONArray("quotations")
    val quotations =
      (0 until quotationsArr.length()).map { i ->
        val obj = quotationsArr.getJSONObject(i)
        QuotationEntity(
          id = obj.getLong("id"),
          quotationNumber = obj.getString("quotationNumber"),
          customerName = obj.getString("customerName"),
          quotationDateMillis = obj.getLong("quotationDateMillis"),
          notes = obj.optString("notes", ""),
          status = obj.optString("status", "SAVED"),
          totalAmountUsd = obj.getString("totalAmountUsd"),
          createdByUserId = obj.getLong("createdByUserId"),
          createdByUsername = obj.optString("createdByUsername", ""),
          createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
          updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
        )
      }

    val itemsArr = root.optJSONArray("quotationItems") ?: JSONArray()
    val items =
      (0 until itemsArr.length()).map { i ->
        val obj = itemsArr.getJSONObject(i)
        QuotationItemEntity(
          id = obj.getLong("id"),
          quotationId = obj.getLong("quotationId"),
          productId = if (obj.isNull("productId")) null else obj.optLong("productId"),
          productNameSnapshot = obj.getString("productNameSnapshot"),
          unitPriceUsdSnapshot = obj.getString("unitPriceUsdSnapshot"),
          quantity = obj.getInt("quantity"),
          lineTotalUsd = obj.getString("lineTotalUsd"),
        )
      }

    val logsArr = root.optJSONArray("auditLogs") ?: JSONArray()
    val logs =
      (0 until logsArr.length()).map { i ->
        val obj = logsArr.getJSONObject(i)
        AuditLogEntity(
          id = obj.getLong("id"),
          userId = obj.getLong("userId"),
          username = obj.getString("username"),
          operationType = obj.getString("operationType"),
          affectedEntity = obj.getString("affectedEntity"),
          affectedRecordId = obj.getString("affectedRecordId"),
          description = obj.getString("description"),
          previousValue = if (obj.isNull("previousValue")) null else obj.optString("previousValue"),
          newValue = if (obj.isNull("newValue")) null else obj.optString("newValue"),
          timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
        )
      }

    db.withTransaction {
      db.quotationDao().deleteAllItems()
      db.quotationDao().deleteAllQuotations()
      db.productDao().deleteAll()
      db.auditLogDao().deleteAll()
      db.userDao().deleteAll()
      db.companyInfoDao().deleteAll()

      db.userDao().insertAllReplace(users)
      if (companyEntity != null) {
        db.companyInfoDao().upsert(companyEntity)
      }
      if (products.isNotEmpty()) {
        db.productDao().insertAll(products)
      }
      quotations.forEach { db.quotationDao().insertQuotationReplace(it) }
      if (items.isNotEmpty()) {
        db.quotationDao().insertItems(items)
      }
      if (logs.isNotEmpty()) {
        db.auditLogDao().insertAll(logs)
      }
    }

    return BackupRestoreResult(
      usersRestored = users.size,
      productsRestored = products.size,
      quotationsRestored = quotations.size,
      logsRestored = logs.size,
    )
  }

  suspend fun exportDataCsvToUri(uri: Uri) {
    val products = db.productDao().getAllProductsOnce()
    val quotations = db.quotationDao().getAllQuotationsOnce()
    val sb = StringBuilder()
    sb.append("\uFEFF") // UTF-8 BOM for Arabic Excel compatibility
    sb.appendLine("=== الأصناف ===")
    sb.appendLine("الرمز,اسم الصنف,السعر بالدولار")
    products.forEach { p ->
      sb.appendLine("\"${p.code}\",\"${p.name.replace("\"", "\"\"")}\",${p.priceUsd}")
    }
    sb.appendLine()
    sb.appendLine("=== عروض الأسعار ===")
    sb.appendLine("رقم العرض,اسم العميل,التاريخ,الحالة,الإجمالي بالدولار,بواسطة")
    quotations.forEach { q ->
      sb.appendLine(
        "\"${q.quotationNumber}\",\"${q.customerName.replace("\"", "\"\"")}\",${q.quotationDateMillis},${q.status},${q.totalAmountUsd},\"${q.createdByUsername}\""
      )
    }
    context.contentResolver.openOutputStream(uri)?.use { os ->
      os.write(sb.toString().toByteArray(Charsets.UTF_8))
    } ?: throw IllegalStateException("تعذر تصدير البيانات")
  }

  fun getStorageStats(): StorageStats {
    val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
    val dbWal = File("${dbFile.absolutePath}-wal")
    val dbShm = File("${dbFile.absolutePath}-shm")
    val dbBytes =
      (if (dbFile.exists()) dbFile.length() else 0L) +
        (if (dbWal.exists()) dbWal.length() else 0L) +
        (if (dbShm.exists()) dbShm.length() else 0L)

    val cacheBytes = dirSize(context.cacheDir)
    val logosBytes = dirSize(File(context.filesDir, "logos"))
    return StorageStats(
      databaseSizeBytes = dbBytes,
      cacheSizeBytes = cacheBytes,
      logosSizeBytes = logosBytes,
    )
  }

  fun clearAppCache(): Long {
    val before = dirSize(context.cacheDir)
    context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
    return before
  }

  private fun dirSize(dir: File): Long {
    if (!dir.exists()) return 0L
    var total = 0L
    dir.listFiles()?.forEach { file ->
      total += if (file.isDirectory) dirSize(file) else file.length()
    }
    return total
  }
}
