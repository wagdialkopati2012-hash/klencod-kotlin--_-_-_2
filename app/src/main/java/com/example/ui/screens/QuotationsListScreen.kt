package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import com.example.ui.i18n.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.DateUtils
import com.example.core.UserRole
import com.example.data.local.CompanyInfoEntity
import com.example.data.local.QuotationStatus
import com.example.data.local.QuotationWithItems
import com.example.data.pdf.QuotationPdfGenerator
import com.example.data.repository.AuthenticatedUser
import com.example.ui.components.PremiumAccessDeniedCard
import com.example.ui.components.PremiumConfirmationDialog
import com.example.ui.components.PremiumDatePickerModal
import com.example.ui.components.PremiumEmptyState
import com.example.ui.components.PremiumPrimaryButton
import com.example.ui.components.PremiumQuotationCard
import com.example.ui.components.PremiumSearchBar
import com.example.ui.components.PremiumSecondaryButton
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerSoftBg
import com.example.ui.theme.Dimensions
import com.example.ui.theme.LocalLiquidGlassColors
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessSoftBg
import com.example.ui.viewmodel.QuotationsListUiState
import com.example.ui.viewmodel.QuotationsListViewModel

@Composable
fun QuotationsListScreen(
  user: AuthenticatedUser,
  companyInfo: CompanyInfoEntity,
  viewModel: QuotationsListViewModel,
  uiState: QuotationsListUiState,
  onCreateNew: () -> Unit,
  onEditQuotation: (Long) -> Unit,
  onPreviewQuotation: (Long) -> Unit,
) {
  val canView = user.permissions.viewQuotations || user.role == UserRole.SUPER_ADMIN
  val canCreate = user.permissions.createQuotation || user.role == UserRole.SUPER_ADMIN
  val canEdit = user.permissions.editQuotation || user.role == UserRole.SUPER_ADMIN
  val canDelete = user.permissions.deleteQuotation || user.role == UserRole.SUPER_ADMIN
  val canExportPdf = user.permissions.exportPdf || user.role == UserRole.SUPER_ADMIN

  if (!canView) {
    Box(
      modifier = Modifier.fillMaxSize().statusBarsPadding(),
      contentAlignment = Alignment.Center,
    ) {
      PremiumAccessDeniedCard()
    }
    return
  }

  val context = LocalContext.current
  val shareScope = rememberCoroutineScope()
  val glass = LocalLiquidGlassColors.current
  var showFiltersSheet by remember { mutableStateOf(false) }
  var deletingItem by remember { mutableStateOf<QuotationWithItems?>(null) }

  val hasActiveFilters =
    uiState.statusFilter != null ||
      !uiState.customerFilter.isNullOrBlank() ||
      uiState.startDateMillis != null ||
      uiState.endDateMillis != null

  Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
    Column(modifier = Modifier.fillMaxSize()) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "عروض الأسعار",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
          )
          Spacer(Modifier.width(10.dp))
          Box(
            modifier =
              Modifier.clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                .padding(horizontal = 10.dp, vertical = 4.dp),
          ) {
            Text(
              text = "${uiState.quotations.size}",
              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }

        IconButton(
          onClick = { showFiltersSheet = true },
          modifier =
            Modifier.clip(RoundedCornerShape(14.dp))
              .background(
                if (hasActiveFilters) PrimaryBlue.copy(alpha = 0.16f)
                else glass.glassSurfaceElevated
              )
              .border(
                1.dp,
                if (hasActiveFilters) PrimaryBlue else glass.glassBorderTop,
                RoundedCornerShape(14.dp),
              ),
        ) {
          Icon(
            imageVector = Icons.Rounded.FilterList,
            contentDescription = "تصفية عروض الأسعار",
            tint = if (hasActiveFilters) PrimaryBlue else MaterialTheme.colorScheme.onSurface,
          )
        }
      }

      Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        PremiumSearchBar(
          query = uiState.searchQuery,
          onQueryChange = viewModel::onSearchChange,
          placeholder = "ابحث باسم العميل أو رقم العرض...",
          testTag = "quotation_search_input",
        )

        AnimatedVisibility(visible = !uiState.feedbackMessage.isNullOrBlank()) {
          Surface(
            color = if (uiState.isError) DangerSoftBg else SuccessSoftBg,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().clickable { viewModel.clearFeedback() },
          ) {
            Text(
              text = uiState.feedbackMessage.orEmpty(),
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
              color = if (uiState.isError) DangerRed else SuccessGreen,
              modifier = Modifier.padding(12.dp),
            )
          }
        }
      }

      Spacer(Modifier.height(10.dp))

      if (uiState.quotations.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
          PremiumEmptyState(
            icon = Icons.Rounded.Description,
            title = if (hasActiveFilters || uiState.searchQuery.isNotEmpty()) "لا توجد عروض مطابقة" else "لا توجد عروض أسعار محفوظة",
            description = "أنشئ عرض سعر جديداً واحفظه أو صدّره بصيغة PDF احترافية",
            actionLabel = if (canCreate) "إنشاء عرض سعر" else null,
            onAction = onCreateNew,
          )
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding =
            PaddingValues(
              start = 16.dp,
              end = 16.dp,
              top = 4.dp,
              bottom = Dimensions.BottomNavClearance + 76.dp,
            ),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          items(
            items = uiState.quotations,
            key = { it.quotation.id },
          ) { item ->
            PremiumQuotationCard(
              quotationWithItems = item,
              canEdit = canEdit,
              canDelete = canDelete,
              canExportPdf = canExportPdf,
              onPreview = { onPreviewQuotation(item.quotation.id) },
              onEdit = { onEditQuotation(item.quotation.id) },
              onDelete = { deletingItem = item },
              onExportPdf = { onPreviewQuotation(item.quotation.id) },
              onShare = {
                shareScope.launch {
                  val file =
                    withContext(Dispatchers.IO) {
                      runCatching {
                          QuotationPdfGenerator.generatePdfFile(context, item, companyInfo)
                        }
                        .getOrNull()
                    }
                  if (file != null) {
                    runCatching {
                      QuotationPdfGenerator.sharePdf(context, file, item.quotation.quotationNumber)
                    }
                  }
                }
              },
            )
          }
        }
      }
    }

    if (canCreate) {
      FloatingActionButton(
        onClick = onCreateNew,
        shape = RoundedCornerShape(20.dp),
        containerColor = PrimaryBlue,
        contentColor = Color.White,
        modifier =
          Modifier.align(Alignment.BottomEnd)
            .navigationBarsPadding()
            .padding(end = 18.dp, bottom = 92.dp)
            .shadow(12.dp, RoundedCornerShape(20.dp), ambientColor = PrimaryBlue)
            .testTag("create_quotation_fab"),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 16.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(Icons.Rounded.Add, contentDescription = "إنشاء عرض سعر")
          Spacer(Modifier.width(6.dp))
          Text("عرض جديد", fontWeight = FontWeight.Bold)
        }
      }
    }
  }

  if (showFiltersSheet) {
    QuotationFiltersBottomSheet(
      currentStatus = uiState.statusFilter,
      currentCustomer = uiState.customerFilter,
      currentStart = uiState.startDateMillis,
      currentEnd = uiState.endDateMillis,
      availableCustomers = uiState.availableCustomers,
      onApply = { status, cust, start, end ->
        viewModel.setFilters(status, cust, start, end)
        showFiltersSheet = false
      },
      onClear = {
        viewModel.clearFilters()
        showFiltersSheet = false
      },
      onDismiss = { showFiltersSheet = false },
    )
  }

  PremiumConfirmationDialog(
    visible = deletingItem != null,
    title = "حذف عرض السعر",
    message = "هل أنت متأكد من حذف عرض السعر \"${deletingItem?.quotation?.quotationNumber.orEmpty()}\"؟ لا يمكن التراجع عن هذا الإجراء.",
    confirmText = "حذف",
    onConfirm = {
      deletingItem?.let { viewModel.deleteQuotation(user, it) }
      deletingItem = null
    },
    onDismiss = { deletingItem = null },
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuotationFiltersBottomSheet(
  currentStatus: QuotationStatus?,
  currentCustomer: String?,
  currentStart: Long?,
  currentEnd: Long?,
  availableCustomers: List<String>,
  onApply: (QuotationStatus?, String?, Long?, Long?) -> Unit,
  onClear: () -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var selectedStatus by remember { mutableStateOf(currentStatus) }
  var selectedCustomer by remember { mutableStateOf(currentCustomer) }
  var startMillis by remember { mutableStateOf(currentStart) }
  var endMillis by remember { mutableStateOf(currentEnd) }
  var pickingStart by remember { mutableStateOf(false) }
  var pickingEnd by remember { mutableStateOf(false) }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Text(
        text = "تصفية عروض الأسعار",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
      )

      Text("حسب الحالة", style = MaterialTheme.typography.labelLarge)
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        FilterChip(
          selected = selectedStatus == null,
          onClick = { selectedStatus = null },
          label = { Text("الكل") },
        )
        QuotationStatus.entries.forEach { st ->
          FilterChip(
            selected = selectedStatus == st,
            onClick = { selectedStatus = if (selectedStatus == st) null else st },
            label = { Text(st.arabicLabel) },
          )
        }
      }

      if (availableCustomers.isNotEmpty()) {
        Text("حسب العميل", style = MaterialTheme.typography.labelLarge)
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          FilterChip(
            selected = selectedCustomer == null,
            onClick = { selectedCustomer = null },
            label = { Text("جميع العملاء") },
          )
          availableCustomers.forEach { cust ->
            FilterChip(
              selected = selectedCustomer == cust,
              onClick = { selectedCustomer = if (selectedCustomer == cust) null else cust },
              label = { Text(cust) },
            )
          }
        }
      }

      Text("النطاق الزمني", style = MaterialTheme.typography.labelLarge)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        PremiumSecondaryButton(
          text = startMillis?.let { "من: ${DateUtils.formatNumericDate(it)}" } ?: "من تاريخ",
          icon = Icons.Rounded.CalendarMonth,
          onClick = { pickingStart = true },
          modifier = Modifier.weight(1f),
        )
        PremiumSecondaryButton(
          text = endMillis?.let { "إلى: ${DateUtils.formatNumericDate(it)}" } ?: "إلى تاريخ",
          icon = Icons.Rounded.CalendarMonth,
          onClick = { pickingEnd = true },
          modifier = Modifier.weight(1f),
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        PremiumSecondaryButton(
          text = "مسح الكل",
          onClick = onClear,
          modifier = Modifier.weight(1f),
        )
        PremiumPrimaryButton(
          text = "تطبيق الفلتر",
          onClick = { onApply(selectedStatus, selectedCustomer, startMillis, endMillis) },
          modifier = Modifier.weight(1f),
        )
      }
    }
  }

  PremiumDatePickerModal(
    visible = pickingStart,
    initialDateMillis = startMillis ?: System.currentTimeMillis(),
    onDateSelected = { startMillis = it },
    onDismiss = { pickingStart = false },
  )
  PremiumDatePickerModal(
    visible = pickingEnd,
    initialDateMillis = endMillis ?: System.currentTimeMillis(),
    onDateSelected = { endMillis = it },
    onDismiss = { pickingEnd = false },
  )
}
