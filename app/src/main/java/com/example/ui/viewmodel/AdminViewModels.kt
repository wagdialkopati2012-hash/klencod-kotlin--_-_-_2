package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AppContainer
import com.example.core.DateUtils
import com.example.core.UserPermissions
import com.example.core.UserRole
import com.example.data.backup.StorageStats
import com.example.data.local.AuditLogEntity
import com.example.data.local.AuditOperationType
import com.example.data.local.QuotationWithItems
import com.example.data.local.UserEntity
import com.example.data.repository.AuthenticatedUser
import com.example.data.repository.PermissionDeniedException
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Calendar
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

enum class ReportTab(val label: String) {
  FINANCIAL("مالية"),
  BY_CUSTOMER("حسب العميل"),
  BY_USER("حسب المستخدم"),
}

enum class ReportPeriod(val label: String) {
  DAILY("تقرير يومي"),
  MONTHLY("تقرير شهري"),
  YEARLY("تقرير سنوي"),
}

data class ChartBarPoint(
  val label: String,
  val amountUsd: BigDecimal,
  val count: Int,
)

data class ReportsUiState(
  val selectedTab: ReportTab = ReportTab.FINANCIAL,
  val selectedPeriod: ReportPeriod = ReportPeriod.MONTHLY,
  val fromDateMillis: Long? = null,
  val toDateMillis: Long? = null,
  val customerFilter: String = "",
  val employeeFilter: String = "",
  val quotationNumberFilter: String = "",
  val totalQuotationsCount: Int = 0,
  val totalValueUsd: BigDecimal = BigDecimal.ZERO,
  val averageValueUsd: BigDecimal = BigDecimal.ZERO,
  val highestValueUsd: BigDecimal = BigDecimal.ZERO,
  val chartPoints: List<ChartBarPoint> = emptyList(),
  val customerBreakdown: List<ChartBarPoint> = emptyList(),
  val userBreakdown: List<ChartBarPoint> = emptyList(),
  val availableCustomers: List<String> = emptyList(),
  val availableEmployees: List<String> = emptyList(),
)

class ReportsViewModel(private val container: AppContainer) : ViewModel() {
  private val _tab = MutableStateFlow(ReportTab.FINANCIAL)
  private val _period = MutableStateFlow(ReportPeriod.MONTHLY)
  private val _fromDate = MutableStateFlow<Long?>(null)
  private val _toDate = MutableStateFlow<Long?>(null)
  private val _customer = MutableStateFlow("")
  private val _employee = MutableStateFlow("")
  private val _qNumber = MutableStateFlow("")

  fun observeReports(user: AuthenticatedUser): StateFlow<ReportsUiState> {
    return combine(
        container.quotationRepository.allQuotations,
        _tab,
        _period,
        _fromDate,
        _toDate,
        _customer,
        _employee,
        _qNumber,
      ) { values ->
        @Suppress("UNCHECKED_CAST")
        val all = values[0] as List<QuotationWithItems>
        val tab = values[1] as ReportTab
        val period = values[2] as ReportPeriod
        val from = values[3] as Long?
        val to = values[4] as Long?
        val cust = values[5] as String
        val emp = values[6] as String
        val qNum = values[7] as String

        val permitted =
          if (user.permissions.viewAllUsersQuotations || user.role == UserRole.SUPER_ADMIN) {
            all
          } else {
            all.filter { it.quotation.createdByUserId == user.entity.id }
          }

        val filtered =
          permitted.filter { item ->
            val q = item.quotation
            val okFrom = from == null || q.quotationDateMillis >= DateUtils.startOfDayMillis(from)
            val okTo = to == null || q.quotationDateMillis <= DateUtils.endOfDayMillis(to)
            val okCust = cust.isBlank() || q.customerName.contains(cust, ignoreCase = true)
            val okEmp = emp.isBlank() || q.createdByUsername.contains(emp, ignoreCase = true)
            val okNum = qNum.isBlank() || q.quotationNumber.contains(qNum, ignoreCase = true)
            okFrom && okTo && okCust && okEmp && okNum
          }

        val amounts = filtered.mapNotNull { it.quotation.totalAmountUsd.toBigDecimalOrNull() }
        val total = amounts.fold(BigDecimal.ZERO) { acc, bd -> acc.add(bd) }
        val avg =
          if (amounts.isNotEmpty()) {
            total.divide(BigDecimal(amounts.size), 2, RoundingMode.HALF_UP)
          } else {
            BigDecimal.ZERO
          }
        val highest = amounts.maxOrNull() ?: BigDecimal.ZERO

        val chartPoints = buildPeriodBars(filtered, period)
        val customerBars =
          filtered
            .groupBy { it.quotation.customerName }
            .map { (name, list) ->
              ChartBarPoint(
                label = name,
                amountUsd =
                  list
                    .mapNotNull { it.quotation.totalAmountUsd.toBigDecimalOrNull() }
                    .fold(BigDecimal.ZERO) { a, b -> a.add(b) },
                count = list.size,
              )
            }
            .sortedByDescending { it.amountUsd }

        val userBars =
          filtered
            .groupBy { it.quotation.createdByUsername.ifBlank { "غير معروف" } }
            .map { (uname, list) ->
              ChartBarPoint(
                label = uname,
                amountUsd =
                  list
                    .mapNotNull { it.quotation.totalAmountUsd.toBigDecimalOrNull() }
                    .fold(BigDecimal.ZERO) { a, b -> a.add(b) },
                count = list.size,
              )
            }
            .sortedByDescending { it.amountUsd }

        ReportsUiState(
          selectedTab = tab,
          selectedPeriod = period,
          fromDateMillis = from,
          toDateMillis = to,
          customerFilter = cust,
          employeeFilter = emp,
          quotationNumberFilter = qNum,
          totalQuotationsCount = filtered.size,
          totalValueUsd = total,
          averageValueUsd = avg,
          highestValueUsd = highest,
          chartPoints = chartPoints,
          customerBreakdown = customerBars,
          userBreakdown = userBars,
          availableCustomers = permitted.map { it.quotation.customerName }.distinct().sorted(),
          availableEmployees =
            permitted.map { it.quotation.createdByUsername }.filter { it.isNotBlank() }.distinct().sorted(),
        )
      }
      .flowOn(kotlinx.coroutines.Dispatchers.Default)
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReportsUiState(),
      )
  }

  private fun buildPeriodBars(
    items: List<QuotationWithItems>,
    period: ReportPeriod,
  ): List<ChartBarPoint> {
    if (items.isEmpty()) return emptyList()
    val cal = Calendar.getInstance()
    return when (period) {
      ReportPeriod.DAILY -> {
        items
          .groupBy { DateUtils.formatNumericDate(it.quotation.quotationDateMillis) }
          .toSortedMap()
          .entries
          .toList()
          .takeLast(7)
          .map { (day, list) ->
            ChartBarPoint(
              label = day.takeLast(5),
              amountUsd =
                list
                  .mapNotNull { it.quotation.totalAmountUsd.toBigDecimalOrNull() }
                  .fold(BigDecimal.ZERO) { a, b -> a.add(b) },
              count = list.size,
            )
          }
      }
      ReportPeriod.MONTHLY -> {
        items
          .groupBy { item ->
            cal.timeInMillis = item.quotation.quotationDateMillis
            val y = cal.get(Calendar.YEAR)
            val m = cal.get(Calendar.MONTH)
            Triple(y, m, DateUtils.formatMonthShort(m))
          }
          .entries
          .sortedWith(compareBy({ it.key.first }, { it.key.second }))
          .takeLast(6)
          .map { (key, list) ->
            ChartBarPoint(
              label = key.third,
              amountUsd =
                list
                  .mapNotNull { it.quotation.totalAmountUsd.toBigDecimalOrNull() }
                  .fold(BigDecimal.ZERO) { a, b -> a.add(b) },
              count = list.size,
            )
          }
      }
      ReportPeriod.YEARLY -> {
        items
          .groupBy { item ->
            cal.timeInMillis = item.quotation.quotationDateMillis
            cal.get(Calendar.YEAR)
          }
          .toSortedMap()
          .entries
          .map { (year, list) ->
            ChartBarPoint(
              label = year.toString(),
              amountUsd =
                list
                  .mapNotNull { it.quotation.totalAmountUsd.toBigDecimalOrNull() }
                  .fold(BigDecimal.ZERO) { a, b -> a.add(b) },
              count = list.size,
            )
          }
      }
    }
  }

  fun selectTab(tab: ReportTab) {
    _tab.value = tab
  }

  fun selectPeriod(period: ReportPeriod) {
    _period.value = period
  }

  fun applyFilters(
    from: Long?,
    to: Long?,
    customer: String,
    employee: String,
    quotationNumber: String,
  ) {
    _fromDate.value = from
    _toDate.value = to
    _customer.value = customer
    _employee.value = employee
    _qNumber.value = quotationNumber
  }

  fun clearFilters() {
    _fromDate.value = null
    _toDate.value = null
    _customer.value = ""
    _employee.value = ""
    _qNumber.value = ""
  }

  class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
      ReportsViewModel(container) as T
  }
}

data class UsersUiState(
  val users: List<UserEntity> = emptyList(),
  val searchQuery: String = "",
  val feedbackMessage: String? = null,
  val isError: Boolean = false,
)

class UsersViewModel(private val container: AppContainer) : ViewModel() {
  private val _search = MutableStateFlow("")
  private val _feedback = MutableStateFlow<Pair<String?, Boolean>>(null to false)

  val uiState: StateFlow<UsersUiState> =
    combine(container.authRepository.allUsers, _search, _feedback) { users, q, fb ->
        val query = q.trim().lowercase()
        val filtered =
          users.filter {
            query.isEmpty() ||
              it.username.lowercase().contains(query) ||
              it.fullName.lowercase().contains(query)
          }
        UsersUiState(
          users = filtered,
          searchQuery = q,
          feedbackMessage = fb.first,
          isError = fb.second,
        )
      }
      .flowOn(kotlinx.coroutines.Dispatchers.Default)
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UsersUiState(),
      )

  fun onSearchChange(q: String) {
    _search.value = q
  }

  fun clearFeedback() {
    _feedback.value = null to false
  }

  fun createUser(
    actor: AuthenticatedUser,
    username: String,
    fullName: String,
    password: String,
    role: UserRole,
  ) {
    viewModelScope.launch {
      runCatching { container.authRepository.createUser(actor, username, fullName, password, role) }
        .onSuccess { _feedback.value = "تمت إضافة المستخدم بنجاح" to false }
        .onFailure { e -> _feedback.value = (e.message ?: "تعذر إضافة المستخدم") to true }
    }
  }

  fun updateUser(
    actor: AuthenticatedUser,
    target: UserEntity,
    fullName: String,
    role: UserRole,
  ) {
    viewModelScope.launch {
      runCatching { container.authRepository.updateUserDetails(actor, target, fullName, role) }
        .onSuccess { _feedback.value = "تم تحديث بيانات المستخدم" to false }
        .onFailure { e -> _feedback.value = (e.message ?: "تعذر التحديث") to true }
    }
  }

  fun changePassword(actor: AuthenticatedUser, target: UserEntity, newPassword: String) {
    viewModelScope.launch {
      runCatching { container.authRepository.changeUserPassword(actor, target, newPassword) }
        .onSuccess { _feedback.value = "تم تغيير كلمة المرور بنجاح" to false }
        .onFailure { e -> _feedback.value = (e.message ?: "تعذر تغيير كلمة المرور") to true }
    }
  }

  fun toggleActive(actor: AuthenticatedUser, target: UserEntity) {
    viewModelScope.launch {
      runCatching {
          container.authRepository.setUserActiveStatus(actor, target, !target.isActive)
        }
        .onSuccess {
          _feedback.value =
            (if (!target.isActive) "تم تفعيل الحساب" else "تم تعليق الحساب") to false
        }
        .onFailure { e -> _feedback.value = (e.message ?: "تعذر تنفيذ العملية") to true }
    }
  }

  fun updatePermissions(
    actor: AuthenticatedUser,
    target: UserEntity,
    permissions: UserPermissions,
  ) {
    viewModelScope.launch {
      runCatching { container.authRepository.updatePermissions(actor, target, permissions) }
        .onSuccess { _feedback.value = "تم حفظ الصلاحيات بنجاح" to false }
        .onFailure { e -> _feedback.value = (e.message ?: "تعذر تحديث الصلاحيات") to true }
    }
  }

  fun deleteUser(actor: AuthenticatedUser, target: UserEntity) {
    viewModelScope.launch {
      runCatching { container.authRepository.deleteUser(actor, target) }
        .onSuccess { _feedback.value = "تم حذف المستخدم" to false }
        .onFailure { e -> _feedback.value = (e.message ?: "تعذر حذف المستخدم") to true }
    }
  }

  class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
      UsersViewModel(container) as T
  }
}

data class AuditLogUiState(
  val logs: List<AuditLogEntity> = emptyList(),
  val usernameQuery: String = "",
  val operationFilter: AuditOperationType? = null,
  val dateFilterMillis: Long? = null,
)

class AuditLogViewModel(private val container: AppContainer) : ViewModel() {
  private val _username = MutableStateFlow("")
  private val _opType = MutableStateFlow<AuditOperationType?>(null)
  private val _dateMillis = MutableStateFlow<Long?>(null)

  val uiState: StateFlow<AuditLogUiState> =
    combine(container.auditRepository.allLogs, _username, _opType, _dateMillis) {
        logs,
        uname,
        op,
        date ->
        val q = uname.trim().lowercase()
        val filtered =
          logs.filter { log ->
            val okUser = q.isEmpty() || log.username.lowercase().contains(q)
            val okOp = op == null || log.operationType == op.name
            val okDate =
              date == null ||
                (log.timestamp in DateUtils.startOfDayMillis(date)..DateUtils.endOfDayMillis(date))
            okUser && okOp && okDate
          }
        AuditLogUiState(
          logs = filtered,
          usernameQuery = uname,
          operationFilter = op,
          dateFilterMillis = date,
        )
      }
      .flowOn(kotlinx.coroutines.Dispatchers.Default)
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AuditLogUiState(),
      )

  fun onUsernameChange(v: String) {
    _username.value = v
  }

  fun onOperationFilterChange(op: AuditOperationType?) {
    _opType.value = op
  }

  fun onDateFilterChange(millis: Long?) {
    _dateMillis.value = millis
  }

  class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
      AuditLogViewModel(container) as T
  }
}

data class SettingsUiState(
  val storageStats: StorageStats = StorageStats(0L, 0L, 0L),
  val isBusy: Boolean = false,
  val feedbackMessage: String? = null,
  val isError: Boolean = false,
)

class SettingsViewModel(private val container: AppContainer) : ViewModel() {
  private val _uiState = MutableStateFlow(SettingsUiState())
  val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

  init {
    refreshStorage()
  }

  fun refreshStorage() {
    viewModelScope.launch {
      val stats = withContext(Dispatchers.IO) { container.backupManager.getStorageStats() }
      _uiState.update { it.copy(storageStats = stats) }
    }
  }

  fun clearCache() {
    viewModelScope.launch {
      val cleared = withContext(Dispatchers.IO) { container.backupManager.clearAppCache() }
      refreshStorage()
      _uiState.update {
        it.copy(
          feedbackMessage = "تم تنظيف الذاكرة المؤقتة (${cleared / 1024} كيلوبايت)",
          isError = false,
        )
      }
    }
  }

  fun createBackup(actor: AuthenticatedUser, uri: Uri) {
    viewModelScope.launch {
      if (!actor.permissions.backupDatabase && actor.role != UserRole.SUPER_ADMIN) {
        _uiState.update {
          it.copy(feedbackMessage = "ليس لديك صلاحية إنشاء نسخ احتياطي", isError = true)
        }
        return@launch
      }
      _uiState.update { it.copy(isBusy = true) }
      runCatching {
          withContext(Dispatchers.IO) { container.backupManager.writeBackupToUri(uri) }
          container.auditRepository.log(
            actor = actor.entity,
            operation = AuditOperationType.BACKUP,
            entityName = "قاعدة البيانات",
            recordId = "full_backup",
            description = "إنشاء نسخة احتياطية كاملة لقاعدة البيانات",
          )
        }
        .onSuccess {
          refreshStorage()
          _uiState.update {
            it.copy(isBusy = false, feedbackMessage = "تم حفظ النسخة الاحتياطية بنجاح", isError = false)
          }
        }
        .onFailure { e ->
          _uiState.update {
            it.copy(isBusy = false, feedbackMessage = e.message ?: "فشل النسخ الاحتياطي", isError = true)
          }
        }
    }
  }

  fun restoreBackup(actor: AuthenticatedUser, uri: Uri) {
    viewModelScope.launch {
      if (!actor.permissions.restoreDatabase && actor.role != UserRole.SUPER_ADMIN) {
        _uiState.update {
          it.copy(feedbackMessage = "ليس لديك صلاحية استعادة قاعدة البيانات", isError = true)
        }
        return@launch
      }
      _uiState.update { it.copy(isBusy = true) }
      runCatching {
          val res =
            withContext(Dispatchers.IO) { container.backupManager.restoreBackupFromUri(uri) }
          container.auditRepository.log(
            actor = actor.entity,
            operation = AuditOperationType.RESTORE,
            entityName = "قاعدة البيانات",
            recordId = "full_restore",
            description =
              "استعادة نسخة احتياطية (${res.productsRestored} صنف، ${res.quotationsRestored} عرض سعر)",
          )
          res
        }
        .onSuccess { res ->
          refreshStorage()
          _uiState.update {
            it.copy(
              isBusy = false,
              feedbackMessage =
                "تمت الاستعادة بنجاح: ${res.productsRestored} صنف و ${res.quotationsRestored} عرض سعر",
              isError = false,
            )
          }
        }
        .onFailure { e ->
          _uiState.update {
            it.copy(isBusy = false, feedbackMessage = e.message ?: "فشل استعادة النسخة", isError = true)
          }
        }
    }
  }

  fun exportCsv(actor: AuthenticatedUser, uri: Uri) {
    viewModelScope.launch {
      _uiState.update { it.copy(isBusy = true) }
      runCatching {
          withContext(Dispatchers.IO) { container.backupManager.exportDataCsvToUri(uri) }
          container.auditRepository.log(
            actor = actor.entity,
            operation = AuditOperationType.EXPORT,
            entityName = "البيانات",
            recordId = "csv",
            description = "تصدير الأصناف وعروض الأسعار بصيغة CSV",
          )
        }
        .onSuccess {
          _uiState.update {
            it.copy(isBusy = false, feedbackMessage = "تم تصدير البيانات بصيغة CSV بنجاح", isError = false)
          }
        }
        .onFailure { e ->
          _uiState.update {
            it.copy(isBusy = false, feedbackMessage = e.message ?: "تعذر التصدير", isError = true)
          }
        }
    }
  }

  fun clearFeedback() {
    _uiState.update { it.copy(feedbackMessage = null, isError = false) }
  }

  class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
      SettingsViewModel(container) as T
  }
}
