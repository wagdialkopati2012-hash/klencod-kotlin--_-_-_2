package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AppContainer
import com.example.core.MoneyUtils
import com.example.data.excel.DuplicateResolution
import com.example.data.excel.ExcelImportSummary
import com.example.data.excel.ExcelParseResult
import com.example.data.excel.XlsxProductImporter
import com.example.data.local.CompanyInfoEntity
import com.example.data.local.ProductEntity
import com.example.data.local.QuotationStatus
import com.example.data.local.QuotationWithItems
import com.example.data.repository.AuthenticatedUser
import java.math.BigDecimal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DashboardUiState(
  val totalQuotationValueUsd: BigDecimal = BigDecimal.ZERO,
  val totalProductsCount: Int = 0,
  val totalQuotationsCount: Int = 0,
  val savedQuotationsCount: Int = 0,
  val customersCount: Int = 0,
  val recentQuotations: List<QuotationWithItems> = emptyList(),
  val recentLogsCount: Int = 0,
)

class DashboardViewModel(private val container: AppContainer) : ViewModel() {

  fun observeDashboard(user: AuthenticatedUser): StateFlow<DashboardUiState> {
    return combine(
        container.productRepository.allProducts,
        container.quotationRepository.allQuotations,
        container.auditRepository.allLogs,
      ) { products, allQuotations, logs ->
        val visibleQuotations =
          if (user.permissions.viewAllUsersQuotations ||
            user.role == com.example.core.UserRole.SUPER_ADMIN
          ) {
            allQuotations
          } else {
            allQuotations.filter { it.quotation.createdByUserId == user.entity.id }
          }

        val totalVal =
          visibleQuotations
            .mapNotNull { it.quotation.totalAmountUsd.toBigDecimalOrNull() }
            .fold(BigDecimal.ZERO) { acc, bd -> acc.add(bd) }

        val savedCount =
          visibleQuotations.count {
            it.quotation.status == QuotationStatus.SAVED.name ||
              it.quotation.status == QuotationStatus.APPROVED.name
          }

        val distinctCustomers =
          visibleQuotations.map { it.quotation.customerName.trim().lowercase() }.distinct().size

        DashboardUiState(
          totalQuotationValueUsd = totalVal,
          totalProductsCount = products.size,
          totalQuotationsCount = visibleQuotations.size,
          savedQuotationsCount = savedCount,
          customersCount = distinctCustomers,
          recentQuotations = visibleQuotations.take(5),
          recentLogsCount = logs.size,
        )
      }
      .flowOn(kotlinx.coroutines.Dispatchers.Default)
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState(),
      )
  }

  class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
      DashboardViewModel(container) as T
  }
}

data class CompanyUiState(
  val companyName: String = "",
  val companySubtitle: String = "",
  val phone: String = "",
  val address: String = "",
  val extraInfo: String = "",
  val logoPath: String? = null,
  val isSaving: Boolean = false,
  val message: String? = null,
  val isError: Boolean = false,
)

class CompanyViewModel(private val container: AppContainer) : ViewModel() {
  private val _uiState = MutableStateFlow(CompanyUiState())
  val uiState: StateFlow<CompanyUiState> = _uiState.asStateFlow()

  init {
    viewModelScope.launch {
      container.companyRepository.companyInfo.collect { info ->
        if (info != null) {
          _uiState.update {
            it.copy(
              companyName = info.companyName,
              companySubtitle = info.companySubtitle,
              phone = info.phone,
              address = info.address,
              extraInfo = info.extraInfo,
              logoPath = info.logoPath,
            )
          }
        }
      }
    }
  }

  fun onNameChange(v: String) = _uiState.update { it.copy(companyName = v) }
  fun onSubtitleChange(v: String) = _uiState.update { it.copy(companySubtitle = v) }
  fun onPhoneChange(v: String) = _uiState.update { it.copy(phone = v) }
  fun onAddressChange(v: String) = _uiState.update { it.copy(address = v) }
  fun onExtraChange(v: String) = _uiState.update { it.copy(extraInfo = v) }
  fun clearMessage() = _uiState.update { it.copy(message = null, isError = false) }

  fun pickLogo(uri: Uri) {
    viewModelScope.launch {
      runCatching {
          withContext(Dispatchers.IO) {
            container.companyRepository.copyPickedLogoToInternalStorage(uri)
          }
        }
        .onSuccess { newPath ->
          val old = _uiState.value.logoPath
          if (old != null && old != newPath) {
            container.companyRepository.deleteLogoFile(old)
          }
          _uiState.update { it.copy(logoPath = newPath, message = "تم تحديث معاينة الشعار — اضغط حفظ لاعتماده", isError = false) }
        }
        .onFailure { e ->
          _uiState.update { it.copy(message = e.message ?: "تعذر تحميل الصورة", isError = true) }
        }
    }
  }

  fun removeLogo() {
    val old = _uiState.value.logoPath
    container.companyRepository.deleteLogoFile(old)
    _uiState.update { it.copy(logoPath = null, message = "تم حذف الشعار — اضغط حفظ لتأكيد التغييرات", isError = false) }
  }

  fun save(actor: AuthenticatedUser) {
    val state = _uiState.value
    if (state.isSaving) return
    viewModelScope.launch {
      _uiState.update { it.copy(isSaving = true, message = null) }
      runCatching {
          container.companyRepository.saveCompanyInfo(
            actor = actor,
            companyName = state.companyName,
            companySubtitle = state.companySubtitle,
            phone = state.phone,
            address = state.address,
            extraInfo = state.extraInfo,
            logoPath = state.logoPath,
          )
        }
        .onSuccess {
          _uiState.update {
            it.copy(isSaving = false, message = "تم حفظ بيانات الشركة بنجاح", isError = false)
          }
        }
        .onFailure { e ->
          _uiState.update {
            it.copy(isSaving = false, message = e.message ?: "فشل الحفظ", isError = true)
          }
        }
    }
  }

  class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
      CompanyViewModel(container) as T
  }
}

enum class ProductSortOption(val label: String) {
  NEWEST("الأحدث أولاً"),
  NAME_ASC("الاسم (أ - ي)"),
  PRICE_HIGH("الأعلى سعراً"),
  PRICE_LOW("الأقل سعراً"),
}

data class ProductsUiState(
  val allProducts: List<ProductEntity> = emptyList(),
  val filteredProducts: List<ProductEntity> = emptyList(),
  val searchQuery: String = "",
  val sortOption: ProductSortOption = ProductSortOption.NEWEST,
  val selectedIds: Set<Long> = emptySet(),
  val pendingExcelParse: ExcelParseResult? = null,
  val duplicateCountInParse: Int = 0,
  val importSummary: ExcelImportSummary? = null,
  val isProcessingExcel: Boolean = false,
  val feedbackMessage: String? = null,
  val isError: Boolean = false,
)

class ProductsViewModel(private val container: AppContainer) : ViewModel() {
  private val _search = MutableStateFlow("")
  private val _sort = MutableStateFlow(ProductSortOption.NEWEST)
  private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
  private val _excelState = MutableStateFlow(ProductsUiState())

  val uiState: StateFlow<ProductsUiState> =
    combine(
        container.productRepository.allProducts,
        _search,
        _sort,
        _selectedIds,
        _excelState,
      ) { products, query, sort, selected, extra ->
        val q = query.trim().lowercase()
        val filtered =
          products
            .filter { p ->
              q.isEmpty() ||
                p.name.lowercase().contains(q) ||
                p.code.lowercase().contains(q) ||
                p.priceUsd.contains(q)
            }
            .let { list ->
              when (sort) {
                ProductSortOption.NEWEST -> list.sortedByDescending { it.id }
                ProductSortOption.NAME_ASC -> list.sortedBy { it.name.lowercase() }
                ProductSortOption.PRICE_HIGH ->
                  list.sortedByDescending { it.priceUsd.toBigDecimalOrNull() ?: BigDecimal.ZERO }
                ProductSortOption.PRICE_LOW ->
                  list.sortedBy { it.priceUsd.toBigDecimalOrNull() ?: BigDecimal.ZERO }
              }
            }
        extra.copy(
          allProducts = products,
          filteredProducts = filtered,
          searchQuery = query,
          sortOption = sort,
          selectedIds = selected.intersect(products.map { it.id }.toSet()),
        )
      }
      .flowOn(kotlinx.coroutines.Dispatchers.Default)
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProductsUiState(),
      )

  fun onSearchChange(q: String) {
    _search.value = q
  }

  fun onSortChange(sort: ProductSortOption) {
    _sort.value = sort
  }

  fun toggleSelection(productId: Long) {
    _selectedIds.update { current ->
      if (productId in current) current - productId else current + productId
    }
  }

  fun selectAllVisible() {
    _selectedIds.value = uiState.value.filteredProducts.map { it.id }.toSet()
  }

  fun clearSelection() {
    _selectedIds.value = emptySet()
  }

  fun clearFeedback() {
    _excelState.update { it.copy(feedbackMessage = null, isError = false) }
  }

  fun addProduct(actor: AuthenticatedUser, name: String, price: String, code: String = "") {
    viewModelScope.launch {
      runCatching { container.productRepository.addProduct(actor, name, price, code) }
        .onSuccess {
          _excelState.update { it.copy(feedbackMessage = "تمت إضافة الصنف بنجاح", isError = false) }
        }
        .onFailure { e ->
          _excelState.update { it.copy(feedbackMessage = e.message ?: "تعذر إضافة الصنف", isError = true) }
        }
    }
  }

  fun updateProduct(
    actor: AuthenticatedUser,
    product: ProductEntity,
    newName: String,
    newPrice: String,
    newCode: String,
  ) {
    viewModelScope.launch {
      runCatching {
          container.productRepository.updateProduct(actor, product, newName, newPrice, newCode)
        }
        .onSuccess {
          _excelState.update { it.copy(feedbackMessage = "تم تحديث الصنف بنجاح", isError = false) }
        }
        .onFailure { e ->
          _excelState.update { it.copy(feedbackMessage = e.message ?: "تعذر تحديث الصنف", isError = true) }
        }
    }
  }

  fun deleteProduct(actor: AuthenticatedUser, product: ProductEntity) {
    viewModelScope.launch {
      runCatching { container.productRepository.deleteProduct(actor, product) }
        .onSuccess {
          _excelState.update { it.copy(feedbackMessage = "تم حذف الصنف", isError = false) }
        }
        .onFailure { e ->
          _excelState.update { it.copy(feedbackMessage = e.message ?: "تعذر حذف الصنف", isError = true) }
        }
    }
  }

  fun deleteSelected(actor: AuthenticatedUser) {
    val ids = _selectedIds.value.toList()
    if (ids.isEmpty()) return
    viewModelScope.launch {
      runCatching { container.productRepository.deleteProducts(actor, ids) }
        .onSuccess {
          _selectedIds.value = emptySet()
          _excelState.update {
            it.copy(feedbackMessage = "تم حذف ${ids.size} من الأصناف", isError = false)
          }
        }
        .onFailure { e ->
          _excelState.update { it.copy(feedbackMessage = e.message ?: "تعذر الحذف", isError = true) }
        }
    }
  }

  fun parseExcelFile(context: Context, uri: Uri) {
    viewModelScope.launch {
      _excelState.update { it.copy(isProcessingExcel = true, feedbackMessage = null) }
      runCatching {
          withContext(Dispatchers.IO) { XlsxProductImporter.parseFromUri(context, uri) }
        }
        .onSuccess { parsed ->
          val existingNames =
            uiState.value.allProducts.map { it.name.trim().lowercase() }.toSet()
          val dupCount = parsed.validRows.count { it.name.trim().lowercase() in existingNames }
          _excelState.update {
            it.copy(
              isProcessingExcel = false,
              pendingExcelParse = parsed,
              duplicateCountInParse = dupCount,
            )
          }
        }
        .onFailure { e ->
          _excelState.update {
            it.copy(
              isProcessingExcel = false,
              feedbackMessage = e.message ?: "تعذر قراءة ملف Excel",
              isError = true,
            )
          }
        }
    }
  }

  fun confirmExcelImport(actor: AuthenticatedUser, resolution: DuplicateResolution) {
    val parsed = _excelState.value.pendingExcelParse ?: return
    viewModelScope.launch {
      _excelState.update { it.copy(isProcessingExcel = true) }
      runCatching {
          container.productRepository.importParsedExcel(actor, parsed, resolution)
        }
        .onSuccess { summary ->
          _excelState.update {
            it.copy(
              isProcessingExcel = false,
              pendingExcelParse = null,
              importSummary = summary,
            )
          }
        }
        .onFailure { e ->
          _excelState.update {
            it.copy(
              isProcessingExcel = false,
              pendingExcelParse = null,
              feedbackMessage = e.message ?: "فشل الاستيراد",
              isError = true,
            )
          }
        }
    }
  }

  fun dismissExcelDialogs() {
    _excelState.update {
      it.copy(pendingExcelParse = null, importSummary = null, duplicateCountInParse = 0)
    }
  }

  fun exportSampleExcelToUri(context: Context, uri: Uri) {
    viewModelScope.launch {
      runCatching {
          withContext(Dispatchers.IO) {
            val bytes =
              XlsxProductImporter.buildWorkbookBytes(
                listOf(
                  "لابتوب عالي الأداء" to "1200",
                  "طابعة مكتبية ليزرية" to "450",
                  "شاشة عرض 27 بوصة" to "320",
                )
              )
            context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
          }
        }
        .onSuccess {
          _excelState.update {
            it.copy(feedbackMessage = "تم تصدير نموذج Excel جاهز للاستخدام", isError = false)
          }
        }
        .onFailure { e ->
          _excelState.update {
            it.copy(feedbackMessage = e.message ?: "فشل تصدير النموذج", isError = true)
          }
        }
    }
  }

  class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
      ProductsViewModel(container) as T
  }
}
