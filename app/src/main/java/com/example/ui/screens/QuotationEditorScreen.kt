package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.NoteAlt
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.DateUtils
import com.example.core.MoneyUtils
import com.example.data.local.ProductEntity
import com.example.data.local.QuotationStatus
import com.example.data.repository.AuthenticatedUser
import com.example.ui.components.PremiumDatePickerModal
import com.example.ui.components.PremiumEmptyState
import com.example.ui.components.PremiumGlassCard
import com.example.ui.components.PremiumGradientCard
import com.example.ui.components.PremiumPriceText
import com.example.ui.components.PremiumPrimaryButton
import com.example.ui.components.PremiumSearchBar
import com.example.ui.components.PremiumSecondaryButton
import com.example.ui.components.PremiumSectionHeader
import com.example.ui.components.PremiumTextField
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerSoftBg
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.LocalLiquidGlassColors
import com.example.ui.theme.PremiumTeal
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.RoyalBlue
import com.example.ui.viewmodel.DraftQuotationLine
import com.example.ui.viewmodel.QuotationEditorUiState
import com.example.ui.viewmodel.QuotationEditorViewModel
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.layout.heightIn

@Composable
fun QuotationEditorScreen(
  user: AuthenticatedUser,
  viewModel: QuotationEditorViewModel,
  uiState: QuotationEditorUiState,
  onCancel: () -> Unit,
  onSavedAndPreview: (Long) -> Unit,
) {
  BackHandler(onBack = onCancel)

  var showDatePicker by remember { mutableStateOf(false) }
  var showProductPickerSheet by remember { mutableStateOf(false) }
  var showCustomerPickerSheet by remember { mutableStateOf(false) }
  val glass = LocalLiquidGlassColors.current

  Column(
    modifier =
      Modifier.fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()
        .imePadding(),
  ) {
    // Top Bar
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = onCancel,
          modifier =
            Modifier.clip(RoundedCornerShape(14.dp))
              .background(glass.glassSurfaceElevated)
              .border(1.dp, glass.glassBorderTop, RoundedCornerShape(14.dp)),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = "رجوع",
          )
        }
        Spacer(Modifier.width(12.dp))
        Text(
          text = if (uiState.editingId == null) "إنشاء عرض سعر" else "تعديل عرض السعر",
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onBackground,
        )
      }
    }

    // Scrollable Editor Body
    Column(
      modifier =
        Modifier.weight(1f)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // Section 1: Quotation Information
      PremiumSectionHeader(title = "معلومات العرض")
      PremiumGlassCard(contentPadding = PaddingValues(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          PremiumTextField(
            value = uiState.customerName,
            onValueChange = viewModel::onCustomerChange,
            label = "اسم العميل",
            placeholder = "مثال: شركة التقنية المتقدمة",
            leadingIcon = Icons.Rounded.Person,
            trailingContent =
              if (uiState.existingCustomers.isNotEmpty()) {
                {
                  Text(
                    text = "عملاء سابقون",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier =
                      Modifier.clip(RoundedCornerShape(10.dp))
                        .clickable { showCustomerPickerSheet = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                  )
                }
              } else null,
            testTag = "quotation_customer_input",
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            PremiumTextField(
              value = uiState.quotationNumber,
              onValueChange = viewModel::onNumberChange,
              label = "رقم العرض",
              leadingIcon = Icons.Rounded.Tag,
              modifier = Modifier.weight(1f),
              testTag = "quotation_number_input",
            )

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "تاريخ العرض",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp, start = 4.dp),
              )
              Row(
                modifier =
                  Modifier.fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(glass.glassSurfaceElevated)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    .clickable { showDatePicker = true }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(
                  imageVector = Icons.Rounded.CalendarMonth,
                  contentDescription = "تاريخ العرض",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(19.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                  text = DateUtils.formatNumericDate(uiState.quotationDateMillis),
                  style = MaterialTheme.typography.bodyMedium,
                  maxLines = 1,
                )
              }
            }
          }

          // Status chips
          Text(
            text = "حالة العرض",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            QuotationStatus.entries.forEach { st ->
              FilterChip(
                selected = uiState.status == st,
                onClick = { viewModel.onStatusChange(st) },
                label = { Text(st.arabicLabel) },
              )
            }
          }

          PremiumTextField(
            value = uiState.notes,
            onValueChange = viewModel::onNotesChange,
            label = "ملاحظات (اختيارية)",
            placeholder = "شروط التسليم، مدة الصلاحية...",
            leadingIcon = Icons.Rounded.NoteAlt,
            singleLine = false,
            maxLines = 3,
          )
        }
      }

      // Section 2: Add Products
      PremiumSectionHeader(
        title = "إضافة الأصناف",
        actionText = "+ اختيار أصناف",
        onActionClick = { showProductPickerSheet = true },
      )

      PremiumSecondaryButton(
        text = "بحث واختيار الأصناف (${uiState.availableProducts.size} متاح)",
        icon = Icons.Rounded.Add,
        onClick = { showProductPickerSheet = true },
        modifier = Modifier.fillMaxWidth(),
        testTag = "pick_products_button",
      )

      if (uiState.lines.isEmpty()) {
        PremiumEmptyState(
          icon = Icons.Rounded.Inventory2,
          title = "لم يتم اختيار أصناف بعد",
          description = "اضغط على زر اختيار الأصناف لإضافة بنود إلى عرض السعر",
        )
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          uiState.lines.forEach { line ->
            key(line.tempKey) {
            DraftLineCard(
              line = line,
              onQuantityChange = { viewModel.setQuantity(line.tempKey, it) },
              onIncrement = { viewModel.incrementQuantity(line.tempKey) },
              onDecrement = { viewModel.decrementQuantity(line.tempKey) },
              onRemove = { viewModel.removeLine(line.tempKey) },
            )
            }
          }
        }
      }

      // Section 3: Financial Summary
      PremiumSectionHeader(title = "الملخص المالي")
      PremiumGradientCard(
        gradientColors = listOf(DeepNavy, RoyalBlue, PremiumTeal),
        contentPadding = PaddingValues(18.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column {
            Text(
              text = "عدد الأصناف",
              style = MaterialTheme.typography.labelMedium,
              color = Color.White.copy(alpha = 0.8f),
            )
            Text(
              text = "${uiState.lines.size}",
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
              color = Color.White,
            )
          }
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = "إجمالي الكميات",
              style = MaterialTheme.typography.labelMedium,
              color = Color.White.copy(alpha = 0.8f),
            )
            Text(
              text = "${uiState.totalQuantity}",
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
              color = Color.White,
            )
          }
          Column(horizontalAlignment = Alignment.End) {
            Text(
              text = "الإجمالي الكلي",
              style = MaterialTheme.typography.labelMedium,
              color = Color.White.copy(alpha = 0.85f),
            )
            Text(
              text = MoneyUtils.formatUsd(uiState.totalAmountUsd),
              style =
                MaterialTheme.typography.headlineMedium.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 24.sp,
                ),
              color = Color.White,
            )
          }
        }
        Spacer(Modifier.height(10.dp))
        Text(
          text = MoneyUtils.amountInWords(uiState.totalAmountUsd),
          style = MaterialTheme.typography.bodySmall,
          color = Color.White.copy(alpha = 0.9f),
        )
      }

      AnimatedVisibility(visible = !uiState.errorMessage.isNullOrBlank()) {
        Surface(
          color = DangerSoftBg,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = uiState.errorMessage.orEmpty(),
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = DangerRed,
            modifier = Modifier.padding(12.dp),
          )
        }
      }

      Spacer(Modifier.height(8.dp))
    }

    // Section 4: Bottom Action Bar
    Surface(
      color = glass.glassSurfaceElevated,
      tonalElevation = 6.dp,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        PremiumSecondaryButton(
          text = "إلغاء",
          onClick = onCancel,
          modifier = Modifier.weight(0.8f),
        )
        PremiumSecondaryButton(
          text = "معاينة",
          icon = Icons.Rounded.Visibility,
          enabled = !uiState.isSaving,
          onClick = {
            viewModel.save(user) { savedId -> onSavedAndPreview(savedId) }
          },
          modifier = Modifier.weight(1f),
          testTag = "preview_quotation_button",
        )
        PremiumPrimaryButton(
          text = if (uiState.editingId == null) "حفظ" else "تحديث",
          icon = Icons.Rounded.Save,
          isLoading = uiState.isSaving,
          onClick = {
            viewModel.save(user) { onCancel() }
          },
          modifier = Modifier.weight(1f),
          testTag = "save_quotation_button",
        )
      }
    }
  }

  PremiumDatePickerModal(
    visible = showDatePicker,
    initialDateMillis = uiState.quotationDateMillis,
    onDateSelected = viewModel::onDateChange,
    onDismiss = { showDatePicker = false },
  )

  if (showProductPickerSheet) {
    ProductPickerBottomSheet(
      products = uiState.availableProducts,
      lines = uiState.lines,
      onSetQuantity = { product, qty -> viewModel.setProductQuantity(product, qty) },
      onDismiss = { showProductPickerSheet = false },
    )
  }

  if (showCustomerPickerSheet) {
    CustomerPickerBottomSheet(
      customers = uiState.existingCustomers,
      onSelectCustomer = {
        viewModel.onCustomerChange(it)
        showCustomerPickerSheet = false
      },
      onDismiss = { showCustomerPickerSheet = false },
    )
  }
}

@Composable
private fun DraftLineCard(
  line: DraftQuotationLine,
  onQuantityChange: (Int) -> Unit,
  onIncrement: () -> Unit,
  onDecrement: () -> Unit,
  onRemove: () -> Unit,
) {
  val glass = LocalLiquidGlassColors.current
  PremiumGlassCard(contentPadding = PaddingValues(14.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = line.productName,
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
        Text(
          text = "سعر الوحدة: ${MoneyUtils.formatUsd(line.unitPriceUsd)}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      IconButton(onClick = onRemove, modifier = Modifier.size(34.dp)) {
        Icon(
          imageVector = Icons.Rounded.Close,
          contentDescription = "إزالة الصنف",
          tint = DangerRed,
          modifier = Modifier.size(18.dp),
        )
      }
    }

    Spacer(Modifier.height(10.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      // Quantity stepper
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
          Modifier.clip(RoundedCornerShape(14.dp))
            .background(glass.softBlueBg)
            .padding(horizontal = 4.dp, vertical = 2.dp),
      ) {
        IconButton(onClick = onDecrement, modifier = Modifier.size(36.dp)) {
          Icon(
            imageVector = Icons.Rounded.Remove,
            contentDescription = "إنقاص الكمية",
            tint = PrimaryBlue,
            modifier = Modifier.size(18.dp),
          )
        }
        QuantityInputField(
          quantity = line.quantity,
          onQuantityChange = onQuantityChange,
        )
        IconButton(onClick = onIncrement, modifier = Modifier.size(36.dp)) {
          Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = "زيادة الكمية",
            tint = PrimaryBlue,
            modifier = Modifier.size(18.dp),
          )
        }
      }

    }
  }
}

/** Quantity box the user can type into directly (e.g. 100), accepting Arabic or English digits. */
@Composable
private fun QuantityInputField(
  quantity: Int,
  onQuantityChange: (Int) -> Unit,
) {
  val focusManager = LocalFocusManager.current
  var text by remember { mutableStateOf(quantity.toString()) }
  var hadFocus by remember { mutableStateOf(false) }

  // Keep the box in sync when the +/- buttons change the quantity.
  LaunchedEffect(quantity) {
    val typed = text.toIntOrNull() ?: 0
    if (typed != quantity) text = quantity.toString()
  }

  BasicTextField(
    value = text,
    onValueChange = { raw ->
      val digits = MoneyUtils.normalizeDigits(raw).filter { it in '0'..'9' }.take(7).trimStart('0')
      text = digits
      onQuantityChange(digits.toIntOrNull() ?: 0)
    },
    modifier =
      Modifier.width(76.dp).onFocusChanged { state ->
        if (hadFocus && !state.isFocused && text.isEmpty()) {
          text = "1"
          onQuantityChange(1)
        }
        hadFocus = state.isFocused
      },
    singleLine = true,
    textStyle =
      MaterialTheme.typography.titleMedium.copy(
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
      ),
    cursorBrush = SolidColor(PrimaryBlue),
    keyboardOptions =
      KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
    decorationBox = { innerTextField ->
      Box(
        modifier = Modifier.height(36.dp).fillMaxWidth(),
        contentAlignment = Alignment.Center,
      ) {
        innerTextField()
      }
    },
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductPickerBottomSheet(
  products: List<ProductEntity>,
  lines: List<DraftQuotationLine>,
  onSetQuantity: (ProductEntity, Int) -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var query by remember { mutableStateOf("") }
  val filtered =
    remember(products, query) {
      val q = query.trim().lowercase()
      if (q.isEmpty()) products
      else products.filter { it.name.lowercase().contains(q) || it.code.lowercase().contains(q) }
    }
  val quantities = remember(lines) { lines.associate { (it.productId ?: -1L) to it.quantity } }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().imePadding().padding(horizontal = 18.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "اختيار الأصناف",
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        )
        PremiumSecondaryButton(text = "تم", onClick = onDismiss)
      }

      Text(
        text = "اكتب الكمية لكل صنف ثم انتقل للصنف التالي",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      PremiumSearchBar(
        query = query,
        onQueryChange = { query = it },
        placeholder = "ابحث عن صنف لإضافته...",
      )

      if (filtered.isEmpty()) {
        PremiumEmptyState(
          icon = Icons.Rounded.Inventory2,
          title = "لا توجد أصناف",
          description = "أضف أصنافاً من شاشة الأصناف أولاً",
        )
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
          contentPadding = PaddingValues(bottom = 20.dp),
        ) {
          items(filtered, key = { it.id }) { product ->
            ProductPickerRow(
              product = product,
              currentQuantity = quantities[product.id] ?: 0,
              onSetQuantity = { qty -> onSetQuantity(product, qty) },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ProductPickerRow(
  product: ProductEntity,
  currentQuantity: Int,
  onSetQuantity: (Int) -> Unit,
) {
  val focusManager = LocalFocusManager.current
  val focusRequester = remember { FocusRequester() }
  // The box starts EMPTY for items that are not in the quotation yet.
  var text by remember(product.id) { mutableStateOf(if (currentQuantity > 0) currentQuantity.toString() else "") }
  val selected = currentQuantity > 0

  PremiumGlassCard(
    onClick = { focusRequester.requestFocus() },
    borderColorOverride = if (selected) PrimaryBlue else null,
    contentPadding = PaddingValues(12.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = product.name,
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = "${product.code}  •  ${MoneyUtils.formatUsdString(product.priceUsd)}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Spacer(Modifier.width(10.dp))
      BasicTextField(
        value = text,
        onValueChange = { raw ->
          val digits =
            MoneyUtils.normalizeDigits(raw).filter { it in '0'..'9' }.take(7).trimStart('0')
          text = digits
          onSetQuantity(digits.toIntOrNull() ?: 0)
        },
        modifier = Modifier.width(92.dp).focusRequester(focusRequester),
        singleLine = true,
        textStyle =
          MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
          ),
        cursorBrush = SolidColor(PrimaryBlue),
        keyboardOptions =
          KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        decorationBox = { innerTextField ->
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                  if (selected) PrimaryBlue.copy(alpha = 0.12f) else LocalLiquidGlassColors.current.softBlueBg
                )
                .border(
                  width = if (selected) 1.5.dp else 1.dp,
                  color = if (selected) PrimaryBlue else MaterialTheme.colorScheme.outline,
                  shape = RoundedCornerShape(12.dp),
                ),
            contentAlignment = Alignment.Center,
          ) {
            if (text.isEmpty()) {
              Text(
                text = "الكمية",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
              )
            }
            innerTextField()
          }
        },
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomerPickerBottomSheet(
  customers: List<String>,
  onSelectCustomer: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState()
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text(
        text = "اختر عميلاً سابقاً",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
      )
      LazyColumn(
        modifier = Modifier.fillMaxWidth().height(280.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 20.dp),
      ) {
        items(customers) { customer ->
          PremiumGlassCard(
            onClick = { onSelectCustomer(customer) },
            contentPadding = PaddingValues(14.dp),
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Rounded.Person,
                contentDescription = null,
                tint = PrimaryBlue,
              )
              Spacer(Modifier.width(10.dp))
              Text(
                text = customer,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
              )
            }
          }
        }
      }
    }
  }
}
