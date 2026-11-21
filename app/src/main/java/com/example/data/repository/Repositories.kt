package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.core.MoneyUtils
import com.example.core.PasswordHasher
import com.example.core.UserPermissions
import com.example.core.UserRole
import com.example.data.excel.DuplicateResolution
import com.example.data.excel.ExcelImportSummary
import com.example.data.excel.ExcelParseResult
import com.example.data.local.AuditLogDao
import com.example.data.local.AuditLogEntity
import com.example.data.local.AuditOperationType
import com.example.data.local.CompanyInfoDao
import com.example.data.local.CompanyInfoEntity
import com.example.data.local.ProductDao
import com.example.data.local.ProductEntity
import com.example.data.local.QuotationDao
import com.example.data.local.QuotationEntity
import com.example.data.local.QuotationItemEntity
import com.example.data.local.QuotationStatus
import com.example.data.local.QuotationWithItems
import com.example.data.local.UserDao
import com.example.data.local.UserEntity
import com.example.data.prefs.AppPreferences
import java.io.File
import java.math.BigDecimal
import java.util.Calendar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class PermissionDeniedException(message: String) : SecurityException(message)

data class AuthenticatedUser(
  val entity: UserEntity,
  val role: UserRole,
  val permissions: UserPermissions,
)

fun UserEntity.toAuthenticatedUser(): AuthenticatedUser {
  val roleEnum = UserRole.fromString(role)
  val perms = UserPermissions.fromJson(permissionsJson, roleEnum)
  return AuthenticatedUser(entity = this, role = roleEnum, permissions = perms)
}

class AuditRepository(private val auditLogDao: AuditLogDao) {
  val allLogs: Flow<List<AuditLogEntity>> = auditLogDao.observeAllLogs()

  suspend fun log(
    actor: UserEntity,
    operation: AuditOperationType,
    entityName: String,
    recordId: String,
    description: String,
    previousValue: String? = null,
    newValue: String? = null,
  ) {
    auditLogDao.insert(
      AuditLogEntity(
        userId = actor.id,
        username = actor.username,
        operationType = operation.name,
        affectedEntity = entityName,
        affectedRecordId = recordId,
        description = description,
        previousValue = previousValue,
        newValue = newValue,
        timestamp = System.currentTimeMillis(),
      )
    )
  }
}

class AuthRepository(
  private val userDao: UserDao,
  private val appPreferences: AppPreferences,
  private val auditRepository: AuditRepository,
) {
  val allUsers: Flow<List<UserEntity>> = userDao.observeAllUsers()

  fun observeUser(id: Long): Flow<UserEntity?> = userDao.observeById(id)

  suspend fun ensureInitialAdmin(): Boolean {
    if (userDao.countUsers() > 0) return false
    val salt = PasswordHasher.generateSalt()
    val hash = PasswordHasher.hashPassword("admin123", salt)
    val admin =
      UserEntity(
        username = "admin",
        fullName = "مدير النظام",
        passwordHash = hash,
        passwordSalt = salt,
        role = UserRole.SUPER_ADMIN.name,
        isActive = true,
        permissionsJson = UserPermissions.allGranted().toJson(),
      )
    userDao.insert(admin)
    return true
  }

  suspend fun isDefaultAdminCredentialsActive(): Boolean {
    val admin = userDao.findByUsername("admin") ?: return false
    return PasswordHasher.verifyPassword("admin123", admin.passwordSalt, admin.passwordHash)
  }

  suspend fun restoreActiveSession(): AuthenticatedUser? {
    val userId = appPreferences.loggedInUserId.firstOrNull() ?: return null
    val user = userDao.findById(userId) ?: return null
    if (!user.isActive) {
      appPreferences.setLoggedInUserId(null)
      return null
    }
    return user.toAuthenticatedUser()
  }

  suspend fun login(
    username: String,
    password: String,
    rememberUsername: Boolean,
  ): Result<AuthenticatedUser> {
    val trimmedUser = username.trim()
    if (trimmedUser.isEmpty() || password.isEmpty()) {
      return Result.failure(IllegalArgumentException("يرجى إدخال اسم المستخدم وكلمة المرور"))
    }
    val user =
      userDao.findByUsername(trimmedUser)
        ?: return Result.failure(IllegalArgumentException("اسم المستخدم أو كلمة المرور غير صحيحة"))

    if (!PasswordHasher.verifyPassword(password, user.passwordSalt, user.passwordHash)) {
      return Result.failure(IllegalArgumentException("اسم المستخدم أو كلمة المرور غير صحيحة"))
    }
    if (!user.isActive) {
      return Result.failure(
        IllegalStateException("تم تعليق هذا الحساب. يرجى التواصل مع مدير النظام")
      )
    }

    val now = System.currentTimeMillis()
    userDao.updateLastActivity(user.id, now)
    appPreferences.saveLoginPreference(rememberUsername, trimmedUser)
    appPreferences.setLoggedInUserId(user.id)

    val updatedUser = user.copy(lastActivityAt = now)
    auditRepository.log(
      actor = updatedUser,
      operation = AuditOperationType.LOGIN,
      entityName = "الجلسة",
      recordId = updatedUser.id.toString(),
      description = "تسجيل دخول المستخدم ${updatedUser.username}",
    )
    return Result.success(updatedUser.toAuthenticatedUser())
  }

  suspend fun logout(actor: AuthenticatedUser) {
    appPreferences.setLoggedInUserId(null)
    auditRepository.log(
      actor = actor.entity,
      operation = AuditOperationType.LOGOUT,
      entityName = "الجلسة",
      recordId = actor.entity.id.toString(),
      description = "تسجيل خروج المستخدم ${actor.entity.username}",
    )
  }

  suspend fun createUser(
    actor: AuthenticatedUser,
    username: String,
    fullName: String,
    password: String,
    role: UserRole,
  ): UserEntity {
    if (!actor.permissions.addUser && actor.role != UserRole.SUPER_ADMIN) {
      throw PermissionDeniedException("ليس لديك صلاحية إضافة مستخدمين")
    }
    val cleanUsername = username.trim()
    val cleanName = fullName.trim().ifEmpty { cleanUsername }
    require(cleanUsername.isNotEmpty()) { "اسم المستخدم مطلوب" }
    require(password.length >= 4) { "كلمة المرور يجب أن تكون 4 أحرف على الأقل" }
    if (userDao.findByUsername(cleanUsername) != null) {
      throw IllegalArgumentException("اسم المستخدم مستخدم مسبقاً")
    }
    val salt = PasswordHasher.generateSalt()
    val hash = PasswordHasher.hashPassword(password, salt)
    val perms = UserPermissions.forRole(role)
    val entity =
      UserEntity(
        username = cleanUsername,
        fullName = cleanName,
        passwordHash = hash,
        passwordSalt = salt,
        role = role.name,
        isActive = true,
        permissionsJson = perms.toJson(),
      )
    val id = userDao.insert(entity)
    val saved = entity.copy(id = id)
    auditRepository.log(
      actor = actor.entity,
      operation = AuditOperationType.CREATE,
      entityName = "مستخدم",
      recordId = id.toString(),
      description = "إنشاء مستخدم جديد: $cleanUsername (${role.arabicLabel})",
      newValue = "الاسم: $cleanName | الدور: ${role.arabicLabel}",
    )
    return saved
  }

  suspend fun updateUserDetails(
    actor: AuthenticatedUser,
    target: UserEntity,
    newFullName: String,
    newRole: UserRole,
  ) {
    if (!actor.permissions.editUser && actor.role != UserRole.SUPER_ADMIN) {
      throw PermissionDeniedException("ليس لديك صلاحية تعديل بيانات المستخدمين")
    }
    val targetCurrentRole = UserRole.fromString(target.role)
    val effectiveRole =
      if (targetCurrentRole == UserRole.SUPER_ADMIN) UserRole.SUPER_ADMIN else newRole
    val effectivePermissions =
      if (effectiveRole == UserRole.SUPER_ADMIN) UserPermissions.allGranted().toJson()
      else if (effectiveRole != targetCurrentRole) UserPermissions.forRole(effectiveRole).toJson()
      else target.permissionsJson

    val updated =
      target.copy(
        fullName = newFullName.trim().ifEmpty { target.username },
        role = effectiveRole.name,
        permissionsJson = effectivePermissions,
      )
    userDao.update(updated)
    auditRepository.log(
      actor = actor.entity,
      operation = AuditOperationType.UPDATE,
      entityName = "مستخدم",
      recordId = target.id.toString(),
      description = "تعديل بيانات المستخدم ${target.username}",
      previousValue = "${target.fullName} (${targetCurrentRole.arabicLabel})",
      newValue = "${updated.fullName} (${effectiveRole.arabicLabel})",
    )
  }

  suspend fun changeUserPassword(
    actor: AuthenticatedUser,
    target: UserEntity,
    newPassword: String,
  ) {
    if (!actor.permissions.editUser && actor.role != UserRole.SUPER_ADMIN && actor.entity.id != target.id) {
      throw PermissionDeniedException("ليس لديك صلاحية تغيير كلمة المرور")
    }
    require(newPassword.length >= 4) { "كلمة المرور يجب أن تكون 4 أحرف على الأقل" }
    val salt = PasswordHasher.generateSalt()
    val hash = PasswordHasher.hashPassword(newPassword, salt)
    userDao.update(target.copy(passwordSalt = salt, passwordHash = hash))
    auditRepository.log(
      actor = actor.entity,
      operation = AuditOperationType.UPDATE,
      entityName = "مستخدم",
      recordId = target.id.toString(),
      description = "تغيير كلمة المرور للمستخدم ${target.username}",
    )
  }

  suspend fun setUserActiveStatus(
    actor: AuthenticatedUser,
    target: UserEntity,
    isActive: Boolean,
  ) {
    if (!actor.permissions.suspendOrDeleteUser && actor.role != UserRole.SUPER_ADMIN) {
      throw PermissionDeniedException("ليس لديك صلاحية تفعيل أو تعليق المستخدمين")
    }
    if (UserRole.fromString(target.role) == UserRole.SUPER_ADMIN) {
      throw IllegalStateException("لا يمكن تعليق حساب مدير النظام الرئيسي")
    }
    if (actor.entity.id == target.id) {
      throw IllegalStateException("لا يمكنك تعليق حسابك الحالي")
    }
    userDao.update(target.copy(isActive = isActive))
    auditRepository.log(
      actor = actor.entity,
      operation = AuditOperationType.UPDATE,
      entityName = "مستخدم",
      recordId = target.id.toString(),
      description =
        if (isActive) "تفعيل حساب المستخدم ${target.username}" else "تعليق حساب المستخدم ${target.username}",
      previousValue = if (target.isActive) "نشط" else "معلق",
      newValue = if (isActive) "نشط" else "معلق",
    )
  }

  suspend fun updatePermissions(
    actor: AuthenticatedUser,
    target: UserEntity,
    newPermissions: UserPermissions,
  ) {
    if (!actor.permissions.managePermissions && actor.role != UserRole.SUPER_ADMIN) {
      throw PermissionDeniedException("ليس لديك صلاحية تعديل الصلاحيات")
    }
    if (UserRole.fromString(target.role) == UserRole.SUPER_ADMIN) {
      throw IllegalStateException("صلاحيات مدير النظام الرئيسي دائمة ولا يمكن تقليصها")
    }
    val json = newPermissions.toJson()
    userDao.update(target.copy(permissionsJson = json))
    auditRepository.log(
      actor = actor.entity,
      operation = AuditOperationType.PERMISSION_CHANGE,
      entityName = "صلاحيات المستخدم",
      recordId = target.id.toString(),
      description = "تحديث صلاحيات المستخدم ${target.username}",
      previousValue = target.permissionsJson,
      newValue = json,
    )
  }

  suspend fun deleteUser(actor: AuthenticatedUser, target: UserEntity) {
    if (!actor.permissions.suspendOrDeleteUser && actor.role != UserRole.SUPER_ADMIN) {
      throw PermissionDeniedException("ليس لديك صلاحية حذف المستخدمين")
    }
    if (UserRole.fromString(target.role) == UserRole.SUPER_ADMIN) {
      throw IllegalStateException("لا يمكن حذف حساب مدير النظام الرئيسي")
    }
    if (actor.entity.id == target.id) {
      throw IllegalStateException("لا يمكنك حذف حسابك الحالي")
    }
    userDao.deleteById(target.id)
    auditRepository.log(
      actor = actor.entity,
      operation = AuditOperationType.DELETE,
      entityName = "مستخدم",
      recordId = target.id.toString(),
      description = "حذف المستخدم ${target.username}",
      previousValue = "${target.fullName} (${target.username})",
    )
  }
}

class CompanyRepository(
  private val context: Context,
  private val companyInfoDao: CompanyInfoDao,
  private val auditRepository: AuditRepository,
) {
  val companyInfo: Flow<CompanyInfoEntity?> = companyInfoDao.observeCompanyInfo()

  suspend fun getOnce(): CompanyInfoEntity {
    return companyInfoDao.getCompanyInfoOnce()
      ?: CompanyInfoEntity(
        id = 1,
        companyName = "شركة التقنية المتقدمة",
        companySubtitle = "Advanced Technology Co.",
        phone = "",
        address = "",
        extraInfo = "",
      )
  }

  suspend fun saveCompanyInfo(
    actor: AuthenticatedUser,
    companyName: String,
    companySubtitle: String,
    phone: String,
    address: String,
    extraInfo: String,
    logoPath: String?,
  ) {
    if (!actor.permissions.editCompany && actor.role != UserRole.SUPER_ADMIN) {
      throw PermissionDeniedException("ليس لديك صلاحية تعديل بيانات الشركة")
    }
    val previous = companyInfoDao.getCompanyInfoOnce()
    val updated =
      CompanyInfoEntity(
        id = 1,
        companyName = companyName.trim(),
        companySubtitle = companySubtitle.trim(),
        phone = phone.trim(),
        address = address.trim(),
        extraInfo = extraInfo.trim(),
        logoPath = logoPath,
        updatedAt = System.currentTimeMillis(),
      )
    companyInfoDao.upsert(updated)
    auditRepository.log(
      actor = actor.entity,
      operation = AuditOperationType.UPDATE,
      entityName = "بيانات الشركة",
      recordId = "1",
      description = "تحديث بيانات الشركة (${updated.companyName})",
      previousValue = previous?.let { "${it.companyName} | ${it.phone}" },
      newValue = "${updated.companyName} | ${updated.phone}",
    )
  }

  fun copyPickedLogoToInternalStorage(uri: Uri): String {
    val logosDir = File(context.filesDir, "logos").apply { mkdirs() }
    val targetFile = File(logosDir, "company_logo_${System.currentTimeMillis()}.png")
    context.contentResolver.openInputStream(uri)?.use { input ->
      targetFile.outputStream().use { output -> input.copyTo(output) }
    } ?: throw IllegalStateException("تعذر قراءة صورة الشعار المحددة")
    return targetFile.absolutePath
  }

  fun deleteLogoFile(path: String?) {
    if (path.isNullOrBlank()) return
    runCatching { File(path).delete() }
  }
}

class ProductRepository(
  private val productDao: ProductDao,
  private val auditRepository: AuditRepository,
) {
  val allProducts: Flow<List<ProductEntity>> = productDao.observeAllProducts()

  suspend fun addProduct(
    actor: AuthenticatedUser,
    name: String,
    priceInput: String,
    customCode: String = "",
  ): ProductEntity {
    if (!actor.permissions.addProduct && actor.role != UserRole.SUPER_ADMIN) {
      throw PermissionDeniedException("ليس لديك صلاحية إضافة الأصناف")
    }
    val cleanName = name.trim()
    require(cleanName.isNotEmpty()) { "اسم الصنف مطلوب" }
    val price =
      MoneyUtils.parseUsdOrNull(priceInput)
        ?: throw IllegalArgumentException("السعر بالدولار غير صالح")
    require(price > BigDecimal.ZERO) { "يجب أن يكون السعر أكبر من صفر" }

    val existingCount = productDao.getAllProductsOnce().size
    val code = customCode.trim().ifEmpty { "P%03d".format(existingCount + 1) }
    val now = System.currentTimeMillis()
    val entity =
      ProductEntity(
        code = code,
        name = cleanName,
        priceUsd = price.toPlainString(),
        createdAt = now,
        updatedAt = now,
      )
    val id = productDao.insert(entity)
    auditRepository.log(
      actor = actor.entity,
      operation = AuditOperationType.CREATE,
      entityName = "صنف",
      recordId = id.toString(),
      description = "إضافة صنف جديد: $cleanName",
      newValue = "$cleanName — ${MoneyUtils.formatUsd(price)}",
    )
    return entity.copy(id = id)
  }

  suspend fun updateProduct(
    actor: AuthenticatedUser,
    product: ProductEntity,
    newName: String,
    newPriceInput: String,
    newCode: String = product.code,
  ) {
    if (!actor.permissions.editProduct && actor.role != UserRole.SUPER_ADMIN) {
      throw PermissionDeniedException("ليس لديك صلاحية تعديل الأصناف")
    }
    val cleanName = newName.trim()
    require(cleanName.isNotEmpty()) { "اسم الصنف مطلوب" }
    val price =
      MoneyUtils.parseUsdOrNull(newPriceInput)
        ?: throw IllegalArgumentException("السعر بالدولار غير صالح")
    require(price > BigDecimal.ZERO) { "يجب أن يكون السعر أكبر من صفر" }

    val updated =
      product.copy(
        code = newCode.trim().ifEmpty { product.code },
        name = cleanName,
        priceUsd = price.toPlainString(),
        updatedAt = System.currentTimeMillis(),
      )
    productDao.update(updated)
    auditRepository.log(
      actor = actor.entity,
      operation = AuditOperationType.UPDATE,
      entityName = "صنف",
      recordId = product.id.toString(),
      description = "تعديل الصنف: $cleanName",
      previousValue = "${product.name} — ${MoneyUtils.formatUsdString(product.priceUsd)}",
      newValue = "${updated.name} — ${MoneyUtils.formatUsd(price)}",
    )
  }

  suspend fun deleteProduct(actor: AuthenticatedUser, product: ProductEntity) {
    if (!actor.permissions.deleteProduct && actor.role != UserRole.SUPER_ADMIN) {
      throw PermissionDeniedException("ليس لديك صلاحية حذف الأصناف")
    }
    productDao.deleteById(product.id)
    auditRepository.log(
      actor = actor.entity,
      operation = AuditOperationType.DELETE,
      entityName = "صنف",
      recordId = product.id.toString(),
      description = "حذف الصنف: ${product.name}",
      previousValue = "${product.name} — ${MoneyUtils.formatUsdString(product.priceUsd)}",
    )
  }

  suspend fun deleteProducts(actor: AuthenticatedUser, ids: List<Long>) {
    if (!actor.permissions.deleteProduct && actor.role != UserRole.SUPER_ADMIN) {
      throw PermissionDeniedException("ليس لديك صلاحية حذف الأصناف")
    }
    if (ids.isEmpty()) return
    productDao.deleteByIds(ids)
    auditRepository.log(
      actor = actor.entity,
      operation = AuditOperationType.DELETE,
      entityName = "الأصناف",
      recordId = ids.joinToString(","),
      description = "حذف جماعي لعدد ${ids.size} من الأصناف",
    )
  }

  suspend fun importParsedExcel(
    actor: AuthenticatedUser,
    parseResult: ExcelParseResult,
    resolution: DuplicateResolution,
  ): ExcelImportSummary {
    if (!actor.permissions.importExcel && actor.role != UserRole.SUPER_ADMIN) {
      throw PermissionDeniedException("ليس لديك صلاحية استيراد ملفات Excel")
    }
    var added = 0
    var updated = 0
    var skipped = 0
    var nextCodeIndex = productDao.getAllProductsOnce().size + 1

    val now = System.currentTimeMillis()
    for (row in parseResult.validRows) {
      val existing = productDao.findByNormalizedName(row.name)
      if (existing != null) {
        when (resolution) {
          DuplicateResolution.SKIP -> skipped++
          DuplicateResolution.UPDATE -> {
            productDao.update(
              existing.copy(
                priceUsd = row.priceUsd.toPlainString(),
                updatedAt = now,
              )
            )
            updated++
          }
        }
      } else {
        productDao.insert(
          ProductEntity(
            code = "P%03d".format(nextCodeIndex++),
            name = row.name,
            priceUsd = row.priceUsd.toPlainString(),
            createdAt = now,
            updatedAt = now,
          )
        )
        added++
      }
    }

    auditRepository.log(
      actor = actor.entity,
      operation = AuditOperationType.IMPORT,
      entityName = "الأصناف",
      recordId = "excel",
      description = "استيراد ملف Excel: إضافة $added، تحديث $updated، تخطي $skipped",
      newValue = "المقروء: ${parseResult.totalRowsScanned} | أخطاء: ${parseResult.errors.size}",
    )

    return ExcelImportSummary(
      totalRowsScanned = parseResult.totalRowsScanned,
      addedCount = added,
      updatedCount = updated,
      skippedDuplicatesCount = skipped,
      invalidCount = parseResult.errors.size,
      errors = parseResult.errors,
    )
  }
}

data class QuotationLineInput(
  val productId: Long?,
  val productName: String,
  val unitPriceUsd: BigDecimal,
  val quantity: Int,
)

class QuotationRepository(
  private val quotationDao: QuotationDao,
  private val auditRepository: AuditRepository,
) {
  val allQuotations: Flow<List<QuotationWithItems>> =
    quotationDao.observeAllQuotationsWithItems()

  fun observeQuotation(id: Long): Flow<QuotationWithItems?> =
    quotationDao.observeQuotationWithItems(id)

  suspend fun getOnce(id: Long): QuotationWithItems? =
    quotationDao.getQuotationWithItemsOnce(id)

  /** Quotation numbers are plain sequential numbers: 1, 2, 3 ... */
  suspend fun generateNextQuotationNumber(): String {
    val highest =
      quotationDao
        .getAllQuotationsOnce()
        .mapNotNull { MoneyUtils.normalizeDigits(it.quotationNumber.trim()).toLongOrNull() }
        .maxOrNull() ?: 0L
    var next = highest + 1
    while (quotationDao.findByNumber(next.toString()) != null) next++
    return next.toString()
  }

  suspend fun saveQuotation(
    actor: AuthenticatedUser,
    editingId: Long?,
    quotationNumber: String,
    customerName: String,
    quotationDateMillis: Long,
    notes: String,
    status: QuotationStatus,
    lines: List<QuotationLineInput>,
  ): Long {
    val isUpdate = editingId != null && editingId > 0L
    if (isUpdate) {
      if (!actor.permissions.editQuotation && actor.role != UserRole.SUPER_ADMIN) {
        throw PermissionDeniedException("ليس لديك صلاحية تعديل عروض الأسعار")
      }
    } else {
      if (!actor.permissions.createQuotation && actor.role != UserRole.SUPER_ADMIN) {
        throw PermissionDeniedException("ليس لديك صلاحية إنشاء عروض الأسعار")
      }
    }

    val cleanNumber = quotationNumber.trim()
    val cleanCustomer = customerName.trim()
    require(cleanNumber.isNotEmpty()) { "رقم عرض السعر مطلوب" }
    require(cleanCustomer.isNotEmpty()) { "اسم العميل مطلوب" }
    require(lines.isNotEmpty()) { "يرجى إضافة صنف واحد على الأقل إلى عرض السعر" }

    val existingByNum = quotationDao.findByNumber(cleanNumber)
    if (existingByNum != null && (!isUpdate || existingByNum.id != editingId)) {
      throw IllegalArgumentException("رقم عرض السعر ($cleanNumber) موجود مسبقاً")
    }

    val itemEntities =
      lines.map { line ->
        require(line.quantity > 0) { "الكمية يجب أن تكون 1 على الأقل" }
        val lineTotal = MoneyUtils.calculateLineTotal(line.unitPriceUsd, line.quantity)
        QuotationItemEntity(
          quotationId = editingId ?: 0L,
          productId = line.productId,
          productNameSnapshot = line.productName,
          unitPriceUsdSnapshot = line.unitPriceUsd.toPlainString(),
          quantity = line.quantity,
          lineTotalUsd = lineTotal.toPlainString(),
        )
      }

    val total =
      MoneyUtils.calculateQuotationTotal(
        itemEntities.map { it.lineTotalUsd.toBigDecimal() }
      )

    val now = System.currentTimeMillis()
    val previous = if (isUpdate) quotationDao.getQuotationWithItemsOnce(editingId!!) else null

    val quotationEntity =
      if (isUpdate && previous != null) {
        previous.quotation.copy(
          quotationNumber = cleanNumber,
          customerName = cleanCustomer,
          quotationDateMillis = quotationDateMillis,
          notes = notes.trim(),
          status = status.name,
          totalAmountUsd = total.toPlainString(),
          updatedAt = now,
        )
      } else {
        QuotationEntity(
          quotationNumber = cleanNumber,
          customerName = cleanCustomer,
          quotationDateMillis = quotationDateMillis,
          notes = notes.trim(),
          status = status.name,
          totalAmountUsd = total.toPlainString(),
          createdByUserId = actor.entity.id,
          createdByUsername = actor.entity.username,
          createdAt = now,
          updatedAt = now,
        )
      }

    val savedId =
      quotationDao.saveQuotationWithItems(
        quotation = quotationEntity,
        items = itemEntities,
        isUpdate = isUpdate,
      )

    auditRepository.log(
      actor = actor.entity,
      operation = if (isUpdate) AuditOperationType.UPDATE else AuditOperationType.CREATE,
      entityName = "عرض سعر",
      recordId = savedId.toString(),
      description =
        if (isUpdate) "تعديل عرض السعر $cleanNumber للعميل $cleanCustomer"
        else "إنشاء عرض سعر $cleanNumber للعميل $cleanCustomer",
      previousValue =
        previous?.let {
          "${it.quotation.customerName} — ${MoneyUtils.formatUsdString(it.quotation.totalAmountUsd)}"
        },
      newValue = "$cleanCustomer — ${MoneyUtils.formatUsd(total)} (${lines.size} أصناف)",
    )

    return savedId
  }

  suspend fun updateQuotationStatus(
    actor: AuthenticatedUser,
    quotation: QuotationEntity,
    newStatus: QuotationStatus,
  ) {
    if (!actor.permissions.editQuotation && actor.role != UserRole.SUPER_ADMIN) {
      throw PermissionDeniedException("ليس لديك صلاحية تعديل حالة عرض السعر")
    }
    quotationDao.updateQuotation(
      quotation.copy(status = newStatus.name, updatedAt = System.currentTimeMillis())
    )
    auditRepository.log(
      actor = actor.entity,
      operation = AuditOperationType.UPDATE,
      entityName = "عرض سعر",
      recordId = quotation.id.toString(),
      description = "تغيير حالة العرض ${quotation.quotationNumber}",
      previousValue = QuotationStatus.fromString(quotation.status).arabicLabel,
      newValue = newStatus.arabicLabel,
    )
  }

  suspend fun deleteQuotation(actor: AuthenticatedUser, quotation: QuotationEntity) {
    if (!actor.permissions.deleteQuotation && actor.role != UserRole.SUPER_ADMIN) {
      throw PermissionDeniedException("ليس لديك صلاحية حذف عروض الأسعار")
    }
    quotationDao.deleteQuotationById(quotation.id)
    auditRepository.log(
      actor = actor.entity,
      operation = AuditOperationType.DELETE,
      entityName = "عرض سعر",
      recordId = quotation.id.toString(),
      description = "حذف عرض السعر ${quotation.quotationNumber}",
      previousValue =
        "${quotation.customerName} — ${MoneyUtils.formatUsdString(quotation.totalAmountUsd)}",
    )
  }
}
