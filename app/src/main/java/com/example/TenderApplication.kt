package com.example

import android.app.Application
import com.example.data.backup.BackupManager
import com.example.data.local.AppDatabase
import com.example.data.prefs.AppPreferences
import com.example.data.repository.AuditRepository
import com.example.data.repository.AuthRepository
import com.example.data.repository.CompanyRepository
import com.example.data.repository.ProductRepository
import com.example.data.repository.QuotationRepository

class AppContainer(application: Application) {
  val database: AppDatabase by lazy { AppDatabase.getInstance(application) }
  val preferences: AppPreferences by lazy { AppPreferences(application) }
  val auditRepository: AuditRepository by lazy { AuditRepository(database.auditLogDao()) }
  val authRepository: AuthRepository by lazy {
    AuthRepository(database.userDao(), preferences, auditRepository)
  }
  val companyRepository: CompanyRepository by lazy {
    CompanyRepository(application, database.companyInfoDao(), auditRepository)
  }
  val productRepository: ProductRepository by lazy {
    ProductRepository(database.productDao(), auditRepository)
  }
  val quotationRepository: QuotationRepository by lazy {
    QuotationRepository(database.quotationDao(), auditRepository)
  }
  val backupManager: BackupManager by lazy { BackupManager(application, database) }
}

class TenderApplication : Application() {
  lateinit var container: AppContainer
    private set

  override fun onCreate() {
    super.onCreate()
    container = AppContainer(this)
  }
}
