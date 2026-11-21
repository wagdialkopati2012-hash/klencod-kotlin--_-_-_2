package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AppContainer
import com.example.core.DateUtils
import com.example.core.MoneyUtils
import com.example.core.UserRole
import com.example.data.local.ProductEntity
import com.example.data.local.QuotationStatus
import com.example.data.local.QuotationWithItems
import com.example.data.repository.AuthenticatedUser
import com.example.data.repository.QuotationLineInput
import java.math.BigDecimal
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuotationsListUiState(
  val quotations: List<QuotationWithItems> = emptyList(),
  val searchQuery: String = "",
  val statusFilter: QuotationStatus? = null,
  val customerFilter: String? = null,
  val startDateMillis: Long? = null,
  val endDateMillis: Long? = null,
  val availableCustomers: List<String> = emptyList(),
  val feedbackMessage: String? = null,
  val isError: Boolean = false,
)

class QuotationsListViewModel(private val container: AppContainer) : ViewModel() {
  private val _search = MutableStateFlow("")
  private val _status = MutableStateFlow<QuotationStatus?>(null)
  private val _customer = MutableStateFlow<String?>(null)
  private val _startDate = MutableStateFlow<Long?>(null)
  private val _endDate = MutableStateFlow<Long?>(null)
  private val _feedback = MutableStateFlow<Pair<String?, Boolean>>(null to false)

  fun observeList(user: AuthenticatedUser): StateFlow<QuotationsListUiState> {
    return combine(
        container.quotationRepository.allQuotations,
        _search,
        _status,
        _customer,
        _startDate,
        _endDate,
        _feedback,
      ) { values ->
        @Suppress("UNCHECKED_CAST")
        val all = values[0] as List<QuotationWithItems>
        val search = values[1] as String
        val status = values[2] as QuotationStatus?
        val customer = values[3] as String?
        val start = values[4] as Long?
        val end = values[5] as Long?
        @Suppress("UNCHECKED_CAST")
        val feedback = values[6] as Pair<String?, Boolean>

        val permitted =
          if (user.permissions.viewAllUsersQuotations || user.role == UserRole.SUPER_ADMIN) {
            all
          } else {
            all.filter { it.quotation.createdByUserId == user.entity.id }
          }

        val customers = permitted.map { it.quotation.customerName }.distinct().sorted()
        val q = search.trim().lowercase()

        val filtered =
          permitted.filter { item ->
            val quot = item.quotation
            val matchesQuery =
              q.isEmpty() ||
                quot.customerName.lowercase().contains(q) ||
                quot.quotationNumber.lowercase().contains(q)
            val matchesStatus = status == null || quot.status == status.name
            val matchesCustomer =
              customer.isNullOrBlank() || quot.customerName.equals(customer, ignoreCase = true)
            val matchesStart =
              start == null || quot.quotationDateMillis >= DateUtils.startOfDayMillis(start)
            val matchesEnd =
              end == null || quot.quotationDateMillis <= DateUtils.endOfDayMillis(end)
            matchesQuery && matchesStatus && matchesCustomer && matchesStart && matchesEnd
          }

        QuotationsListUiState(
          quotations = filtered,
          searchQuery = search,
          statusFilter = status,
          customerFilter = customer,
          startDateMillis = start,
          endDateMillis = end,
          availableCustomers = customers,
          feedbackMessage = feedback.first,
          isError = feedback.second,
        )
      }
      .flowOn(kotlinx.coroutines.Dispatchers.Default)
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = QuotationsListUiState(),
      )
  }

  fun onSearchChange(q: String) {
    _search.value = q
  }

  fun setFilters(
    status: QuotationStatus?,
    customer: String?,
    startDate: Long?,
    endDate: Long?,
  ) {
    _status.value = status
    _customer.value = customer
    _startDate.value = startDate
    _endDate.value = endDate
  }

  fun clearFilters() {
    _status.value = null
    _customer.value = null
    _startDate.value = null
    _endDate.value = null
  }

  fun clearFeedback() {
    _feedback.value = null to false
  }

  fun deleteQuotation(actor: AuthenticatedUser, item: QuotationWithItems) {
    viewModelScope.launch {
      runCatching { container.quotationRepository.deleteQuotation(actor, item.quotation) }
        .onSuccess {
          _feedback.value = "تم حذف عرض السعر ${item.quotation.quotationNumber}" to false
        }
        .onFailure { e ->
          _feedback.value = (e.message ?: "تعذر حذف عرض السعر") to true
        }
    }
  }

  fun updateStatus(
    actor: AuthenticatedUser,
    item: QuotationWithItems,
    status: QuotationStatus,
  ) {
    viewModelScope.launch {
      runCatching { container.quotationRepository.updateQuotationStatus(actor, item.quotation, status) }
        .onSuccess {
          _feedback.value = "تم تحديث حالة العرض إلى ${status.arabicLabel}" to false
        }
        .onFailure { e ->
          _feedback.value = (e.message ?: "تعذر تحديث الحالة") to true
        }
    }
  }

  class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
      QuotationsListViewModel(container) as T
  }
}

data class DraftQuotationLine(
  val tempKey: String,
  val productId: Long?,
  val productName: String,
  val unitPriceUsd: BigDecimal,
  val quantity: Int,
) {
  val lineTotal: BigDecimal
    get() = MoneyUtils.calculateLineTotal(unitPriceUsd, quantity)
}

data class QuotationEditorUiState(
  val editingId: Long? = null,
  val quotationNumber: String = "",
  val customerName: String = "",
  val quotationDateMillis: Long = System.currentTimeMillis(),
  val notes: String = "",
  val status: QuotationStatus = QuotationStatus.SAVED,
  val lines: List<DraftQuotationLine> = emptyList(),
  val availableProducts: List<ProductEntity> = emptyList(),
  val existingCustomers: List<String> = emptyList(),
  val isSaving: Boolean = false,
  val errorMessage: String? = null,
  val savedSuccessId: Long? = null,
) {
  val totalQuantity: Int
    get() = lines.sumOf { it.quantity }

  val totalAmountUsd: BigDecimal
    get() = MoneyUtils.calculateQuotationTotal(lines.map { it.lineTotal })
}

class QuotationEditorViewModel(private val container: AppContainer) : ViewModel() {
  private val _uiState = MutableStateFlow(QuotationEditorUiState())
  val uiState: StateFlow<QuotationEditorUiState> = _uiState.asStateFlow()

  init {
    viewModelScope.launch {
      combine(
          container.productRepository.allProducts,
          container.quotationRepository.allQuotations,
        ) { products, quotations ->
          products to quotations.map { it.quotation.customerName }.distinct().sorted()
        }
        .collect { (products, customers) ->
          _uiState.update {
            it.copy(availableProducts = products, existingCustomers = customers)
          }
        }
    }
  }

  fun initialize(quotationId: Long?) {
    viewModelScope.launch {
      if (quotationId != null && quotationId > 0L) {
        val existing = container.quotationRepository.getOnce(quotationId)
        if (existing != null) {
          _uiState.update {
            it.copy(
              editingId = existing.quotation.id,
              quotationNumber = existing.quotation.quotationNumber,
              customerName = existing.quotation.customerName,
              quotationDateMillis = existing.quotation.quotationDateMillis,
              notes = existing.quotation.notes,
              status = QuotationStatus.fromString(existing.quotation.status),
              lines =
                existing.items.mapIndexed { idx, item ->
                  DraftQuotationLine(
                    tempKey = "existing_${item.id}_$idx",
                    productId = item.productId,
                    productName = item.productNameSnapshot,
                    unitPriceUsd =
                      item.unitPriceUsdSnapshot.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                    quantity = item.quantity,
                  )
                },
              errorMessage = null,
              savedSuccessId = null,
            )
          }
          return@launch
        }
      }
      val nextNum = container.quotationRepository.generateNextQuotationNumber()
      _uiState.update {
        QuotationEditorUiState(
          editingId = null,
          quotationNumber = nextNum,
          customerName = "",
          quotationDateMillis = System.currentTimeMillis(),
          notes = "",
          status = QuotationStatus.SAVED,
          lines = emptyList(),
          availableProducts = it.availableProducts,
          existingCustomers = it.existingCustomers,
        )
      }
    }
  }

  fun onCustomerChange(v: String) = _uiState.update { it.copy(customerName = v, errorMessage = null) }
  fun onNumberChange(v: String) = _uiState.update { it.copy(quotationNumber = v, errorMessage = null) }
  fun onDateChange(millis: Long) = _uiState.update { it.copy(quotationDateMillis = millis) }
  fun onNotesChange(v: String) = _uiState.update { it.copy(notes = v) }
  fun onStatusChange(s: QuotationStatus) = _uiState.update { it.copy(status = s) }
  fun clearError() = _uiState.update { it.copy(errorMessage = null) }
  fun clearSavedEvent() = _uiState.update { it.copy(savedSuccessId = null) }

  fun addProduct(product: ProductEntity) {
    val price = product.priceUsd.toBigDecimalOrNull() ?: BigDecimal.ZERO
    _uiState.update { state ->
      val existingIdx = state.lines.indexOfFirst { it.productId == product.id }
      val newLines =
        if (existingIdx >= 0) {
          state.lines.mapIndexed { idx, line ->
            if (idx == existingIdx) line.copy(quantity = line.quantity + 1) else line
          }
        } else {
          state.lines +
            DraftQuotationLine(
              tempKey = "p_${product.id}_${System.nanoTime()}",
              productId = product.id,
              productName = product.name,
              unitPriceUsd = price,
              quantity = 1,
            )
        }
      state.copy(lines = newLines, errorMessage = null)
    }
  }

  /** Sets an exact quantity typed by the user (0 while the field is empty; save requires >= 1). */
  fun setQuantity(tempKey: String, quantity: Int) {
    val safe = quantity.coerceIn(0, MAX_QUANTITY)
    _uiState.update { state ->
      state.copy(
        lines =
          state.lines.map { line ->
            if (line.tempKey == tempKey) line.copy(quantity = safe) else line
          },
        errorMessage = null,
      )
    }
  }

  /**
   * Used by the item picker: the quantity typed next to an item. A quantity >= 1 adds the item (or
   * updates it); an empty / 0 quantity removes it again. The quantity always starts empty.
   */
  fun setProductQuantity(product: ProductEntity, quantity: Int) {
    val safe = quantity.coerceIn(0, MAX_QUANTITY)
    val price = product.priceUsd.toBigDecimalOrNull() ?: BigDecimal.ZERO
    _uiState.update { state ->
      val existingIdx = state.lines.indexOfFirst { it.productId == product.id }
      val newLines =
        when {
          safe <= 0 && existingIdx >= 0 -> state.lines.filterIndexed { i, _ -> i != existingIdx }
          safe <= 0 -> state.lines
          existingIdx >= 0 ->
            state.lines.mapIndexed { i, line ->
              if (i == existingIdx) line.copy(quantity = safe) else line
            }
          else ->
            state.lines +
              DraftQuotationLine(
                tempKey = "p_${product.id}_${System.nanoTime()}",
                productId = product.id,
                productName = product.name,
                unitPriceUsd = price,
                quantity = safe,
              )
        }
      state.copy(lines = newLines, errorMessage = null)
    }
  }

  fun incrementQuantity(tempKey: String) {
    _uiState.update { state ->
      state.copy(
        lines =
          state.lines.map { line ->
            if (line.tempKey == tempKey) {
              line.copy(quantity = (line.quantity + 1).coerceAtMost(MAX_QUANTITY))
            } else {
              line
            }
          }
      )
    }
  }

  fun decrementQuantity(tempKey: String) {
    _uiState.update { state ->
      state.copy(
        lines =
          state.lines.map { line ->
            if (line.tempKey == tempKey && line.quantity > 1) {
              line.copy(quantity = line.quantity - 1)
            } else {
              line
            }
          }
      )
    }
  }

  fun removeLine(tempKey: String) {
    _uiState.update { state ->
      state.copy(lines = state.lines.filterNot { it.tempKey == tempKey })
    }
  }

  fun save(actor: AuthenticatedUser, onSavedNavigateToPreview: ((Long) -> Unit)? = null) {
    val state = _uiState.value
    if (state.isSaving) return
    viewModelScope.launch {
      _uiState.update { it.copy(isSaving = true, errorMessage = null) }
      runCatching {
          container.quotationRepository.saveQuotation(
            actor = actor,
            editingId = state.editingId,
            quotationNumber = state.quotationNumber,
            customerName = state.customerName,
            quotationDateMillis = state.quotationDateMillis,
            notes = state.notes,
            status = state.status,
            lines =
              state.lines.map {
                QuotationLineInput(
                  productId = it.productId,
                  productName = it.productName,
                  unitPriceUsd = it.unitPriceUsd,
                  quantity = it.quantity,
                )
              },
          )
        }
        .onSuccess { savedId ->
          _uiState.update {
            it.copy(
              isSaving = false,
              editingId = savedId,
              savedSuccessId = savedId,
            )
          }
          onSavedNavigateToPreview?.invoke(savedId)
        }
        .onFailure { e ->
          _uiState.update {
            it.copy(
              isSaving = false,
              errorMessage = e.message ?: "تعذر حفظ عرض السعر",
            )
          }
        }
    }
  }

  companion object {
    const val MAX_QUANTITY = 9_999_999
  }

  class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
      QuotationEditorViewModel(container) as T
  }
}
