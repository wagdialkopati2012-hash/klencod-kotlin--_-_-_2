package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.TableView
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import com.example.ui.i18n.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.core.MoneyUtils
import com.example.core.UserRole
import com.example.data.excel.DuplicateResolution
import com.example.data.excel.ExcelImportSummary
import com.example.data.excel.ExcelParseResult
import com.example.data.local.ProductEntity
import com.example.data.repository.AuthenticatedUser
import com.example.ui.components.PremiumAccessDeniedCard
import com.example.ui.components.PremiumConfirmationDialog
import com.example.ui.components.PremiumEmptyState
import com.example.ui.components.PremiumGlassCard
import com.example.ui.components.PremiumPrimaryButton
import com.example.ui.components.PremiumProductCard
import com.example.ui.components.PremiumSearchBar
import com.example.ui.components.PremiumSecondaryButton
import com.example.ui.components.PremiumTextField
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerSoftBg
import com.example.ui.theme.Dimensions
import com.example.ui.theme.LocalLiquidGlassColors
import com.example.ui.theme.PremiumTeal
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessSoftBg
import com.example.ui.viewmodel.ProductSortOption
import com.example.ui.viewmodel.ProductsUiState
import com.example.ui.viewmodel.ProductsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
  user: AuthenticatedUser,
  viewModel: ProductsViewModel,
  uiState: ProductsUiState,
  openAddDialogInitially: Boolean = false,
  onConsumedInitialAdd: () -> Unit = {},
) {
  val canView = user.permissions.viewProducts || user.role == UserRole.SUPER_ADMIN
  val canAdd = user.permissions.addProduct || user.role == UserRole.SUPER_ADMIN
  val canEdit = user.permissions.editProduct || user.role == UserRole.SUPER_ADMIN
  val canDelete = user.permissions.deleteProduct || user.role == UserRole.SUPER_ADMIN
  val canImport = user.permissions.importExcel || user.role == UserRole.SUPER_ADMIN

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
  val glass = LocalLiquidGlassColors.current

  var showProductSheet by remember(openAddDialogInitially) {
    mutableStateOf(openAddDialogInitially && canAdd)
  }
  if (openAddDialogInitially) {
    onConsumedInitialAdd()
  }
  var editingProduct by remember { mutableStateOf<ProductEntity?>(null) }
  var deletingProduct by remember { mutableStateOf<ProductEntity?>(null) }
  var confirmBulkDelete by remember { mutableStateOf(false) }
  var sortMenuExpanded by remember { mutableStateOf(false) }

  val excelPickerLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri != null) {
        viewModel.parseExcelFile(context, uri)
      }
    }

  val excelTemplateExporter =
    rememberLauncherForActivityResult(
      ActivityResultContracts.CreateDocument(
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
      )
    ) { uri ->
      if (uri != null) {
        viewModel.exportSampleExcelToUri(context, uri)
      }
    }

  val isSelectionMode = uiState.selectedIds.isNotEmpty()

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
            text = "الأصناف",
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
              text = "${uiState.allProducts.size}",
              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }

        Box {
          IconButton(
            onClick = { sortMenuExpanded = true },
            modifier =
              Modifier.clip(RoundedCornerShape(14.dp))
                .background(glass.glassSurfaceElevated)
                .border(1.dp, glass.glassBorderTop, RoundedCornerShape(14.dp)),
          ) {
            Icon(
              imageVector = Icons.Rounded.Sort,
              contentDescription = "ترتيب الأصناف",
              tint = MaterialTheme.colorScheme.onSurface,
            )
          }
          DropdownMenu(
            expanded = sortMenuExpanded,
            onDismissRequest = { sortMenuExpanded = false },
          ) {
            ProductSortOption.entries.forEach { option ->
              DropdownMenuItem(
                text = {
                  Text(
                    text = option.label,
                    fontWeight =
                      if (uiState.sortOption == option) FontWeight.Bold else FontWeight.Normal,
                    color =
                      if (uiState.sortOption == option) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.onSurface,
                  )
                },
                onClick = {
                  sortMenuExpanded = false
                  viewModel.onSortChange(option)
                },
              )
            }
          }
        }
      }

      // Contextual multi-selection bar
      AnimatedVisibility(
        visible = isSelectionMode,
        enter = fadeIn() + slideInVertically { -20 },
        exit = fadeOut(),
      ) {
        PremiumGlassCard(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
          borderColorOverride = MaterialTheme.colorScheme.primary,
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "المحدد: ${uiState.selectedIds.size}",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.primary,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
              TextButton(onClick = { viewModel.selectAllVisible() }) {
                Icon(Icons.Rounded.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("تحديد الكل")
              }
              if (canDelete) {
                TextButton(onClick = { confirmBulkDelete = true }) {
                  Icon(
                    Icons.Rounded.DeleteOutline,
                    contentDescription = null,
                    tint = DangerRed,
                    modifier = Modifier.size(16.dp),
                  )
                  Spacer(Modifier.width(4.dp))
                  Text("حذف المحدد", color = DangerRed)
                }
              }
              IconButton(onClick = { viewModel.clearSelection() }) {
                Icon(Icons.Rounded.Close, contentDescription = "إلغاء التحديد")
              }
            }
          }
        }
      }

      // Search & Excel Import Action
      Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        PremiumSearchBar(
          query = uiState.searchQuery,
          onQueryChange = viewModel::onSearchChange,
          placeholder = "ابحث عن صنف بالاسم أو الرمز...",
          testTag = "product_search_input",
        )

        if (canImport) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            PremiumPrimaryButton(
              text = "استيراد ملف Excel",
              icon = Icons.Rounded.FileUpload,
              isLoading = uiState.isProcessingExcel,
              onClick = {
                excelPickerLauncher.launch(
                  arrayOf(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "application/octet-stream",
                  )
                )
              },
              modifier = Modifier.weight(1f),
              testTag = "import_excel_button",
            )
            IconButton(
              onClick = { excelTemplateExporter.launch("products_template.xlsx") },
              modifier =
                Modifier.size(50.dp)
                  .clip(RoundedCornerShape(16.dp))
                  .background(glass.glassSurfaceElevated)
                  .border(1.dp, glass.glassBorderTop, RoundedCornerShape(16.dp)),
            ) {
              Icon(
                imageVector = Icons.Rounded.Download,
                contentDescription = "تنزيل قالب Excel",
                tint = MaterialTheme.colorScheme.primary,
              )
            }
          }
        }

        AnimatedVisibility(visible = !uiState.feedbackMessage.isNullOrBlank()) {
          Surface(
            color = if (uiState.isError) DangerSoftBg else SuccessSoftBg,
            shape = RoundedCornerShape(14.dp),
            modifier =
              Modifier.fillMaxWidth().clickable { viewModel.clearFeedback() },
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

      if (uiState.filteredProducts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
          PremiumEmptyState(
            icon = Icons.Rounded.Inventory2,
            title = if (uiState.searchQuery.isNotEmpty()) "لا توجد نتائج مطابقة" else "قائمة الأصناف فارغة",
            description =
              if (uiState.searchQuery.isNotEmpty()) "جرّب البحث بكلمات أخرى"
              else "أضف الأصناف يدوياً أو استوردها من ملف Excel (.xlsx)",
            actionLabel = if (canAdd && uiState.searchQuery.isEmpty()) "إضافة صنف" else null,
            onAction = {
              editingProduct = null
              showProductSheet = true
            },
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
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          items(
            items = uiState.filteredProducts,
            key = { it.id },
          ) { product ->
            val selected = product.id in uiState.selectedIds
            PremiumProductCard(
              product = product,
              isSelectionMode = isSelectionMode,
              isSelected = selected,
              canEdit = canEdit,
              canDelete = canDelete,
              onClick = {
                if (isSelectionMode) {
                  viewModel.toggleSelection(product.id)
                } else if (canEdit) {
                  editingProduct = product
                  showProductSheet = true
                }
              },
              onLongClick = { viewModel.toggleSelection(product.id) },
              onEdit = {
                editingProduct = product
                showProductSheet = true
              },
              onDelete = { deletingProduct = product },
            )
          }
        }
      }
    }

    // Floating Action Button above floating nav
    if (canAdd) {
      FloatingActionButton(
        onClick = {
          editingProduct = null
          showProductSheet = true
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = PrimaryBlue,
        contentColor = Color.White,
        modifier =
          Modifier.align(Alignment.BottomEnd)
            .navigationBarsPadding()
            .padding(end = 18.dp, bottom = 92.dp)
            .shadow(12.dp, RoundedCornerShape(20.dp), ambientColor = PrimaryBlue)
            .testTag("add_product_fab"),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 16.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(Icons.Rounded.Add, contentDescription = "إضافة صنف")
          Spacer(Modifier.width(6.dp))
          Text("إضافة صنف", fontWeight = FontWeight.Bold)
        }
      }
    }
  }

  // Add / Edit Product Bottom Sheet
  if (showProductSheet) {
    ProductFormBottomSheet(
      existing = editingProduct,
      onDismiss = {
        showProductSheet = false
        editingProduct = null
      },
      onSave = { name, price, code ->
        val current = editingProduct
        if (current == null) {
          viewModel.addProduct(user, name, price, code)
        } else {
          viewModel.updateProduct(user, current, name, price, code)
        }
        showProductSheet = false
        editingProduct = null
      },
    )
  }

  // Delete single product confirmation
  PremiumConfirmationDialog(
    visible = deletingProduct != null,
    title = "حذف الصنف",
    message = "هل أنت متأكد من حذف الصنف \"${deletingProduct?.name.orEmpty()}\"؟ ستبقى الأسعار التاريخية في العروض المحفوظة دون تغيير.",
    confirmText = "حذف",
    onConfirm = {
      deletingProduct?.let { viewModel.deleteProduct(user, it) }
      deletingProduct = null
    },
    onDismiss = { deletingProduct = null },
  )

  // Bulk delete confirmation
  PremiumConfirmationDialog(
    visible = confirmBulkDelete,
    title = "حذف الأصناف المحددة",
    message = "هل تريد حذف ${uiState.selectedIds.size} من الأصناف المحددة نهائياً؟",
    confirmText = "حذف الكل",
    onConfirm = {
      viewModel.deleteSelected(user)
      confirmBulkDelete = false
    },
    onDismiss = { confirmBulkDelete = false },
  )

  // Excel Duplicate Resolution Dialog
  uiState.pendingExcelParse?.let { parsed ->
    ExcelDuplicateResolutionDialog(
      parseResult = parsed,
      duplicateCount = uiState.duplicateCountInParse,
      onSkipDuplicates = { viewModel.confirmExcelImport(user, DuplicateResolution.SKIP) },
      onUpdateDuplicates = { viewModel.confirmExcelImport(user, DuplicateResolution.UPDATE) },
      onDismiss = { viewModel.dismissExcelDialogs() },
    )
  }

  // Excel Import Summary Dialog
  uiState.importSummary?.let { summary ->
    ExcelImportSummaryDialog(
      summary = summary,
      onDismiss = { viewModel.dismissExcelDialogs() },
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductFormBottomSheet(
  existing: ProductEntity?,
  onDismiss: () -> Unit,
  onSave: (name: String, price: String, code: String) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var name by remember(existing) { mutableStateOf(existing?.name.orEmpty()) }
  var price by remember(existing) { mutableStateOf(existing?.priceUsd.orEmpty()) }
  var code by remember(existing) { mutableStateOf(existing?.code.orEmpty()) }
  var nameError by remember { mutableStateOf<String?>(null) }
  var priceError by remember { mutableStateOf<String?>(null) }

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
        text = if (existing == null) "إضافة صنف جديد" else "تعديل بيانات الصنف",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
      )

      PremiumTextField(
        value = name,
        onValueChange = {
          name = it
          nameError = null
        },
        label = "اسم الصنف",
        placeholder = "مثال: لابتوب عالي الأداء",
        leadingIcon = Icons.Rounded.Inventory2,
        isError = nameError != null,
        errorText = nameError,
        testTag = "product_name_input",
      )

      PremiumTextField(
        value = price,
        onValueChange = {
          price = it
          priceError = null
        },
        label = "السعر بالدولار (USD)",
        placeholder = "مثال: 1200",
        leadingIcon = Icons.Rounded.AttachMoney,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        isError = priceError != null,
        errorText = priceError,
        testTag = "product_price_input",
      )

      PremiumTextField(
        value = code,
        onValueChange = { code = it },
        label = "رمز الصنف (اختياري)",
        placeholder = "P001",
        leadingIcon = Icons.Rounded.QrCode2,
      )

      Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        PremiumSecondaryButton(
          text = "إلغاء",
          onClick = onDismiss,
          modifier = Modifier.weight(1f),
        )
        PremiumPrimaryButton(
          text = "حفظ",
          onClick = {
            val validName = name.trim().isNotEmpty()
            val parsedPrice = MoneyUtils.parseUsdOrNull(price)
            val validPrice = parsedPrice != null && parsedPrice.signum() > 0
            if (!validName) nameError = "يرجى إدخال اسم الصنف"
            if (!validPrice) priceError = "يرجى إدخال سعر صحيح أكبر من صفر"
            if (validName && validPrice) {
              onSave(name, price, code)
            }
          },
          modifier = Modifier.weight(1f),
          testTag = "save_product_button",
        )
      }
    }
  }
}

@Composable
private fun ExcelDuplicateResolutionDialog(
  parseResult: ExcelParseResult,
  duplicateCount: Int,
  onSkipDuplicates: () -> Unit,
  onUpdateDuplicates: () -> Unit,
  onDismiss: () -> Unit,
) {
  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(26.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 8.dp,
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Rounded.TableView,
            contentDescription = null,
            tint = PrimaryBlue,
            modifier = Modifier.size(28.dp),
          )
          Spacer(Modifier.width(10.dp))
          Text(
            text = "معاينة استيراد Excel",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
          )
        }
        Text(
          text =
            "تمت قراءة ${parseResult.totalRowsScanned} صفاً: ${parseResult.validRows.size} صالحة، ${parseResult.errors.size} غير صالحة، و$duplicateCount صنف مكرر.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (duplicateCount > 0) {
          Text(
            text = "كيف تود معالجة الأصناف المكررة؟",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
          )
          PremiumPrimaryButton(
            text = "تحديث أسعار المكرر ($duplicateCount)",
            onClick = onUpdateDuplicates,
            modifier = Modifier.fillMaxWidth(),
          )
          PremiumSecondaryButton(
            text = "تخطي الأصناف المكررة",
            onClick = onSkipDuplicates,
            modifier = Modifier.fillMaxWidth(),
          )
        } else {
          PremiumPrimaryButton(
            text = "استيراد ${parseResult.validRows.size} صنف",
            onClick = onSkipDuplicates,
            modifier = Modifier.fillMaxWidth(),
          )
        }
        TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally)) {
          Text("إلغاء")
        }
      }
    }
  }
}

@Composable
private fun ExcelImportSummaryDialog(
  summary: ExcelImportSummary,
  onDismiss: () -> Unit,
) {
  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(26.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 8.dp,
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = SuccessGreen,
            modifier = Modifier.size(28.dp),
          )
          Spacer(Modifier.width(10.dp))
          Text(
            text = "ملخص استيراد Excel",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
          )
        }
        Text("إجمالي الصفوف المقروءة: ${summary.totalRowsScanned}")
        Text("أصناف مضافة جديدة: ${summary.addedCount}", color = SuccessGreen, fontWeight = FontWeight.Bold)
        Text("أصناف محدثة: ${summary.updatedCount}", color = RoyalBlue)
        Text("مكررة تم تخطيها: ${summary.skippedDuplicatesCount}")
        if (summary.invalidCount > 0) {
          Text("صفوف غير صالحة: ${summary.invalidCount}", color = DangerRed)
          summary.errors.take(3).forEach { err ->
            Text(
              text = "• صف ${err.rowNumber}: ${err.reason}",
              style = MaterialTheme.typography.bodySmall,
              color = DangerRed,
            )
          }
        }
        Spacer(Modifier.height(6.dp))
        PremiumPrimaryButton(
          text = "إغلاق",
          onClick = onDismiss,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    }
  }
}
