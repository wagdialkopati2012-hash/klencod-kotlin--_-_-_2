package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.UserPermissions
import com.example.core.UserRole
import com.example.data.backup.BackupManager
import com.example.data.excel.DuplicateResolution
import com.example.data.excel.XlsxProductImporter
import com.example.data.local.AppDatabase
import com.example.data.local.CompanyInfoEntity
import com.example.data.local.QuotationStatus
import com.example.data.pdf.QuotationPdfGenerator
import com.example.data.prefs.AppPreferences
import com.example.data.repository.AuditRepository
import com.example.data.repository.AuthRepository
import com.example.data.repository.CompanyRepository
import com.example.data.repository.PermissionDeniedException
import com.example.data.repository.ProductRepository
import com.example.data.repository.QuotationLineInput
import com.example.data.repository.QuotationRepository
import java.io.ByteArrayInputStream
import java.math.BigDecimal
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MunakasatRobolectricTest {

  private lateinit var context: Context
  private lateinit var db: AppDatabase
  private lateinit var auditRepo: AuditRepository
  private lateinit var authRepo: AuthRepository
  private lateinit var productRepo: ProductRepository
  private lateinit var quotationRepo: QuotationRepository
  private lateinit var companyRepo: CompanyRepository
  private lateinit var backupManager: BackupManager

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    val prefs = AppPreferences(context)
    auditRepo = AuditRepository(db.auditLogDao())
    authRepo = AuthRepository(db.userDao(), prefs, auditRepo)
    productRepo = ProductRepository(db.productDao(), auditRepo)
    quotationRepo = QuotationRepository(db.quotationDao(), auditRepo)
    companyRepo = CompanyRepository(context, db.companyInfoDao(), auditRepo)
    backupManager = BackupManager(context, db)
  }

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun `permissions json roundtrip preserves granular flags`() {
    val original =
      UserPermissions(
        viewCompany = true,
        editCompany = true,
        viewProducts = true,
        addProduct = true,
        importExcel = true,
        viewReports = true,
        viewFinancialTotals = false,
      )
    val parsed = UserPermissions.fromJson(original.toJson(), UserRole.USER)
    assertEquals(original, parsed)
  }

  @Test
  fun `full business workflow preserves historical prices, permissions, excel import, pdf, and backup`() = runTest {
    // 1. Seed initial admin and authenticate
    assertTrue(authRepo.ensureInitialAdmin())
    val adminLogin = authRepo.login("admin", "admin123", rememberUsername = true)
    assertTrue(adminLogin.isSuccess)
    val admin = adminLogin.getOrThrow()

    // 2. Create a standard user and verify permission denial for adding products
    val normalEntity = authRepo.createUser(admin, "sara", "سارة علي", "pass1234", UserRole.USER)
    val saraLogin = authRepo.login("sara", "pass1234", false).getOrThrow()
    var denied = false
    try {
      productRepo.addProduct(saraLogin, "صنف غير مصرح", "50")
    } catch (_: PermissionDeniedException) {
      denied = true
    }
    assertTrue(denied)

    // 3. Suspend user and verify login is blocked
    authRepo.setUserActiveStatus(admin, normalEntity, false)
    val suspendedAttempt = authRepo.login("sara", "pass1234", false)
    assertTrue(suspendedAttempt.isFailure)

    // 4. Add products + Import XLSX
    val laptop = productRepo.addProduct(admin, "لابتوب عالي الأداء", "1200")
    val xlsxBytes =
      XlsxProductImporter.buildWorkbookBytes(
        listOf("لابتوب عالي الأداء" to "1350", "طابعة احترافية" to "450")
      )
    val parsed = XlsxProductImporter.parseFromStream(ByteArrayInputStream(xlsxBytes))
    assertEquals(2, parsed.validRows.size)
    val summary = productRepo.importParsedExcel(admin, parsed, DuplicateResolution.SKIP)
    assertEquals(1, summary.addedCount)
    assertEquals(1, summary.skippedDuplicatesCount)

    // 5. Create quotation and verify historical price snapshot remains unchanged after product price update
    val qId =
      quotationRepo.saveQuotation(
        actor = admin,
        editingId = null,
        quotationNumber = "QU-2026-0001",
        customerName = "شركة التقنية المتقدمة",
        quotationDateMillis = System.currentTimeMillis(),
        notes = "تسليم فوري",
        status = QuotationStatus.SAVED,
        lines =
          listOf(
            QuotationLineInput(
              productId = laptop.id,
              productName = laptop.name,
              unitPriceUsd = BigDecimal("1200.00"),
              quantity = 2,
            )
          ),
      )

    productRepo.updateProduct(admin, laptop, laptop.name, "1500", laptop.code)
    val savedQuotation = quotationRepo.getOnce(qId)
    assertNotNull(savedQuotation)
    assertEquals("2400.00", savedQuotation!!.quotation.totalAmountUsd)
    assertEquals("1200.00", savedQuotation.items.first().unitPriceUsdSnapshot)

    // 6. Verify A4 quotation page rendering produces valid A4 bitmaps
    val company = CompanyInfoEntity(id = 1, companyName = "شركة النخبة", phone = "0500000000", address = "الرياض")
    val a4Bitmaps = QuotationPdfGenerator.renderPageBitmapsDirect(context, savedQuotation, company, 1.0f)
    assertEquals(1, a4Bitmaps.size)
    assertEquals(QuotationPdfGenerator.A4_WIDTH, a4Bitmaps.first().width)
    assertEquals(QuotationPdfGenerator.A4_HEIGHT, a4Bitmaps.first().height)
    a4Bitmaps.forEach { it.recycle() }

    // 7. Verify backup export & restore
    companyRepo.saveCompanyInfo(admin, "شركة النخبة", "Elite Co.", "0500000000", "الرياض", "", null)
    val jsonBackup = backupManager.exportFullBackupJson()
    val restoreResult = backupManager.restoreFromJson(jsonBackup)
    assertEquals(2, restoreResult.productsRestored)
    assertEquals(1, restoreResult.quotationsRestored)
    assertTrue(auditRepo.allLogs.first().isNotEmpty())
  }
}
