package com.example.core

import org.json.JSONObject

enum class UserRole(val arabicLabel: String) {
  SUPER_ADMIN("مدير النظام"),
  MANAGER("مشرف مناقصات"),
  USER("مستخدم عادي");

  companion object {
    fun fromString(value: String): UserRole =
      entries.firstOrNull { it.name == value } ?: USER
  }
}

data class UserPermissions(
  // 1. Company Info
  val viewCompany: Boolean = true,
  val editCompany: Boolean = false,
  // 2. Products
  val viewProducts: Boolean = true,
  val addProduct: Boolean = false,
  val editProduct: Boolean = false,
  val deleteProduct: Boolean = false,
  val importExcel: Boolean = false,
  // 3. Quotations
  val viewQuotations: Boolean = true,
  val createQuotation: Boolean = true,
  val editQuotation: Boolean = false,
  val deleteQuotation: Boolean = false,
  val exportPdf: Boolean = true,
  val viewAllUsersQuotations: Boolean = false,
  // 4. Reports
  val viewReports: Boolean = false,
  val viewFinancialTotals: Boolean = false,
  val exportReports: Boolean = false,
  // 5. Users
  val viewUsers: Boolean = false,
  val addUser: Boolean = false,
  val editUser: Boolean = false,
  val managePermissions: Boolean = false,
  val suspendOrDeleteUser: Boolean = false,
  // 6. System
  val viewAuditLog: Boolean = false,
  val backupDatabase: Boolean = false,
  val restoreDatabase: Boolean = false,
  val manageSettings: Boolean = true,
) {
  fun toJson(): String {
    return JSONObject().apply {
      put("viewCompany", viewCompany)
      put("editCompany", editCompany)
      put("viewProducts", viewProducts)
      put("addProduct", addProduct)
      put("editProduct", editProduct)
      put("deleteProduct", deleteProduct)
      put("importExcel", importExcel)
      put("viewQuotations", viewQuotations)
      put("createQuotation", createQuotation)
      put("editQuotation", editQuotation)
      put("deleteQuotation", deleteQuotation)
      put("exportPdf", exportPdf)
      put("viewAllUsersQuotations", viewAllUsersQuotations)
      put("viewReports", viewReports)
      put("viewFinancialTotals", viewFinancialTotals)
      put("exportReports", exportReports)
      put("viewUsers", viewUsers)
      put("addUser", addUser)
      put("editUser", editUser)
      put("managePermissions", managePermissions)
      put("suspendOrDeleteUser", suspendOrDeleteUser)
      put("viewAuditLog", viewAuditLog)
      put("backupDatabase", backupDatabase)
      put("restoreDatabase", restoreDatabase)
      put("manageSettings", manageSettings)
    }.toString()
  }

  companion object {
    fun allGranted(): UserPermissions =
      UserPermissions(
        viewCompany = true,
        editCompany = true,
        viewProducts = true,
        addProduct = true,
        editProduct = true,
        deleteProduct = true,
        importExcel = true,
        viewQuotations = true,
        createQuotation = true,
        editQuotation = true,
        deleteQuotation = true,
        exportPdf = true,
        viewAllUsersQuotations = true,
        viewReports = true,
        viewFinancialTotals = true,
        exportReports = true,
        viewUsers = true,
        addUser = true,
        editUser = true,
        managePermissions = true,
        suspendOrDeleteUser = true,
        viewAuditLog = true,
        backupDatabase = true,
        restoreDatabase = true,
        manageSettings = true,
      )

    fun forRole(role: UserRole): UserPermissions =
      when (role) {
        UserRole.SUPER_ADMIN -> allGranted()
        UserRole.MANAGER ->
          UserPermissions(
            viewCompany = true,
            editCompany = true,
            viewProducts = true,
            addProduct = true,
            editProduct = true,
            deleteProduct = true,
            importExcel = true,
            viewQuotations = true,
            createQuotation = true,
            editQuotation = true,
            deleteQuotation = false,
            exportPdf = true,
            viewAllUsersQuotations = true,
            viewReports = true,
            viewFinancialTotals = true,
            exportReports = true,
            viewUsers = false,
            addUser = false,
            editUser = false,
            managePermissions = false,
            suspendOrDeleteUser = false,
            viewAuditLog = true,
            backupDatabase = true,
            restoreDatabase = false,
            manageSettings = true,
          )
        UserRole.USER -> UserPermissions()
      }

    fun fromJson(json: String, role: UserRole = UserRole.USER): UserPermissions {
      if (role == UserRole.SUPER_ADMIN) return allGranted()
      return try {
        val obj = JSONObject(json)
        UserPermissions(
          viewCompany = obj.optBoolean("viewCompany", true),
          editCompany = obj.optBoolean("editCompany", false),
          viewProducts = obj.optBoolean("viewProducts", true),
          addProduct = obj.optBoolean("addProduct", false),
          editProduct = obj.optBoolean("editProduct", false),
          deleteProduct = obj.optBoolean("deleteProduct", false),
          importExcel = obj.optBoolean("importExcel", false),
          viewQuotations = obj.optBoolean("viewQuotations", true),
          createQuotation = obj.optBoolean("createQuotation", true),
          editQuotation = obj.optBoolean("editQuotation", false),
          deleteQuotation = obj.optBoolean("deleteQuotation", false),
          exportPdf = obj.optBoolean("exportPdf", true),
          viewAllUsersQuotations = obj.optBoolean("viewAllUsersQuotations", false),
          viewReports = obj.optBoolean("viewReports", false),
          viewFinancialTotals = obj.optBoolean("viewFinancialTotals", false),
          exportReports = obj.optBoolean("exportReports", false),
          viewUsers = obj.optBoolean("viewUsers", false),
          addUser = obj.optBoolean("addUser", false),
          editUser = obj.optBoolean("editUser", false),
          managePermissions = obj.optBoolean("managePermissions", false),
          suspendOrDeleteUser = obj.optBoolean("suspendOrDeleteUser", false),
          viewAuditLog = obj.optBoolean("viewAuditLog", false),
          backupDatabase = obj.optBoolean("backupDatabase", false),
          restoreDatabase = obj.optBoolean("restoreDatabase", false),
          manageSettings = obj.optBoolean("manageSettings", true),
        )
      } catch (_: Exception) {
        forRole(role)
      }
    }
  }
}
