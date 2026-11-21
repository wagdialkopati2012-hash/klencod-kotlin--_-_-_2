package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Functions
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import com.example.ui.i18n.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.DateUtils
import com.example.core.MoneyUtils
import com.example.core.UserRole
import com.example.data.repository.AuthenticatedUser
import com.example.ui.components.PremiumAccessDeniedCard
import com.example.ui.components.PremiumDatePickerModal
import com.example.ui.components.PremiumEmptyState
import com.example.ui.components.PremiumGlassCard
import com.example.ui.components.PremiumPrimaryButton
import com.example.ui.components.PremiumSecondaryButton
import com.example.ui.components.PremiumStatCard
import com.example.ui.components.PremiumTextField
import com.example.ui.theme.CyanSoftBg
import com.example.ui.theme.InfoSoftBg
import com.example.ui.theme.LocalLiquidGlassColors
import com.example.ui.theme.PremiumTeal
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.VioletAccent
import com.example.ui.theme.VioletSoftBg
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningSoftBg
import com.example.ui.viewmodel.ChartBarPoint
import com.example.ui.viewmodel.ReportPeriod
import com.example.ui.viewmodel.ReportTab
import com.example.ui.viewmodel.ReportsUiState
import com.example.ui.viewmodel.ReportsViewModel
import java.math.BigDecimal

@Composable
fun ReportsScreen(
  user: AuthenticatedUser,
  viewModel: ReportsViewModel,
  uiState: ReportsUiState,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)

  val isSuper = user.role == UserRole.SUPER_ADMIN
  val canViewReports = user.permissions.viewReports || isSuper
  val canViewFinancials = user.permissions.viewFinancialTotals || isSuper

  if (!canViewReports) {
    Box(
      modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
      contentAlignment = Alignment.Center,
    ) {
      PremiumAccessDeniedCard(onBack = onBack)
    }
    return
  }

  val glass = LocalLiquidGlassColors.current
  var showFilterSheet by remember { mutableStateOf(false) }

  Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
    // Top Header
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = onBack,
          modifier =
            Modifier.clip(RoundedCornerShape(14.dp))
              .background(glass.glassSurfaceElevated)
              .border(1.dp, glass.glassBorderTop, RoundedCornerShape(14.dp)),
        ) {
          Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "رجوع")
        }
        Spacer(Modifier.width(12.dp))
        Text(
          text = "التقارير والتحليلات",
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        )
      }

      IconButton(
        onClick = { showFilterSheet = true },
        modifier =
          Modifier.clip(RoundedCornerShape(14.dp))
            .background(glass.glassSurfaceElevated)
            .border(1.dp, glass.glassBorderTop, RoundedCornerShape(14.dp)),
      ) {
        Icon(Icons.Rounded.FilterList, contentDescription = "تصفية التقارير")
      }
    }

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // Tab pills (مالية / حسب العميل / حسب المستخدم)
      item {
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          ReportTab.entries.forEach { tab ->
            FilterChip(
              selected = uiState.selectedTab == tab,
              onClick = { viewModel.selectTab(tab) },
              label = { Text(tab.label, fontWeight = FontWeight.SemiBold) },
            )
          }
        }
      }

      // 4 Financial Summary Cards
      item {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            PremiumStatCard(
              label = "إجمالي عدد العروض",
              value = uiState.totalQuotationsCount.toString(),
              icon = Icons.Rounded.Description,
              accentColor = PrimaryBlue,
              iconBgColor = InfoSoftBg,
              modifier = Modifier.weight(1f),
            )
            PremiumStatCard(
              label = "إجمالي القيمة",
              value =
                if (canViewFinancials) MoneyUtils.formatUsd(uiState.totalValueUsd) else "مقيد",
              icon = Icons.Rounded.AttachMoney,
              accentColor = SuccessGreen,
              iconBgColor = CyanSoftBg,
              modifier = Modifier.weight(1f),
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            PremiumStatCard(
              label = "متوسط قيمة العرض",
              value =
                if (canViewFinancials) MoneyUtils.formatUsd(uiState.averageValueUsd) else "مقيد",
              icon = Icons.Rounded.Functions,
              accentColor = VioletAccent,
              iconBgColor = VioletSoftBg,
              modifier = Modifier.weight(1f),
            )
            PremiumStatCard(
              label = "أعلى عرض سعر",
              value =
                if (canViewFinancials) MoneyUtils.formatUsd(uiState.highestValueUsd) else "مقيد",
              icon = Icons.Rounded.Star,
              accentColor = WarningAmber,
              iconBgColor = WarningSoftBg,
              modifier = Modifier.weight(1f),
            )
          }
        }
      }

      when (uiState.selectedTab) {
        ReportTab.FINANCIAL -> {
          item {
            Row(
              modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              ReportPeriod.entries.forEach { period ->
                FilterChip(
                  selected = uiState.selectedPeriod == period,
                  onClick = { viewModel.selectPeriod(period) },
                  label = { Text(period.label) },
                )
              }
            }
          }
          item {
            if (!canViewFinancials) {
              PremiumAccessDeniedCard(
                title = "البيانات المالية مقيدة",
                message = "لا تملك صلاحية عرض المبالغ المالية التفصيلية في التقارير.",
              )
            } else if (uiState.chartPoints.isEmpty()) {
              PremiumEmptyState(
                icon = Icons.Rounded.BarChart,
                title = "لا توجد بيانات للرسم البياني",
                description = "أنشئ عروض أسعار لعرض الإحصائيات اليومية والشهرية والسنوية",
              )
            } else {
              PremiumBarChartCard(
                title = uiState.selectedPeriod.label,
                points = uiState.chartPoints,
              )
            }
          }
        }

        ReportTab.BY_CUSTOMER -> {
          if (uiState.customerBreakdown.isEmpty()) {
            item {
              PremiumEmptyState(
                icon = Icons.Rounded.BarChart,
                title = "لا توجد بيانات عملاء",
                description = "ستظهر مقارنة العملاء تلقائياً عند وجود عروض أسعار",
              )
            }
          } else {
            items(uiState.customerBreakdown) { point ->
              ComparisonRowCard(point = point, canViewFinancials = canViewFinancials)
            }
          }
        }

        ReportTab.BY_USER -> {
          if (uiState.userBreakdown.isEmpty()) {
            item {
              PremiumEmptyState(
                icon = Icons.Rounded.BarChart,
                title = "لا توجد بيانات مستخدمين",
                description = "ستظهر مقارنة المستخدمين تلقائياً عند وجود عروض أسعار",
              )
            }
          } else {
            items(uiState.userBreakdown) { point ->
              ComparisonRowCard(point = point, canViewFinancials = canViewFinancials)
            }
          }
        }
      }
    }
  }

  if (showFilterSheet) {
    ReportsFilterBottomSheet(
      currentFrom = uiState.fromDateMillis,
      currentTo = uiState.toDateMillis,
      currentCustomer = uiState.customerFilter,
      currentEmployee = uiState.employeeFilter,
      currentQNum = uiState.quotationNumberFilter,
      onApply = { f, t, c, e, q ->
        viewModel.applyFilters(f, t, c, e, q)
        showFilterSheet = false
      },
      onClear = {
        viewModel.clearFilters()
        showFilterSheet = false
      },
      onDismiss = { showFilterSheet = false },
    )
  }
}

@Composable
private fun PremiumBarChartCard(
  title: String,
  points: List<ChartBarPoint>,
) {
  val maxVal = points.maxOfOrNull { it.amountUsd }?.max(BigDecimal.ONE) ?: BigDecimal.ONE

  PremiumGlassCard(contentPadding = PaddingValues(18.dp)) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
    )
    Spacer(Modifier.height(16.dp))
    Row(
      modifier = Modifier.fillMaxWidth().height(175.dp),
      horizontalArrangement = Arrangement.SpaceEvenly,
      verticalAlignment = Alignment.Bottom,
    ) {
      points.forEach { pt ->
        val rawFraction =
          (pt.amountUsd.toFloat() / maxVal.toFloat()).coerceIn(0.08f, 1f)
        val animatedFraction by
          animateFloatAsState(
            targetValue = rawFraction,
            animationSpec = spring(dampingRatio = 0.7f),
            label = "bar_${pt.label}",
          )
        Column(
          modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Bottom,
        ) {
          Text(
            text = "${pt.count}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(Modifier.height(4.dp))
          Box(
            modifier = Modifier.width(26.dp).height(115.dp),
            contentAlignment = Alignment.BottomCenter,
          ) {
            Box(
              modifier =
                Modifier.fillMaxWidth()
                  .fillMaxHeight(animatedFraction)
                  .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                  .background(
                    Brush.verticalGradient(listOf(PrimaryBlue, RoyalBlue, PremiumTeal))
                  ),
            )
          }
          Spacer(Modifier.height(6.dp))
          Text(
            text = pt.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}

@Composable
private fun ComparisonRowCard(
  point: ChartBarPoint,
  canViewFinancials: Boolean,
) {
  PremiumGlassCard(contentPadding = PaddingValues(14.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = point.label,
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = "${point.count} عروض أسعار",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Text(
        text = if (canViewFinancials) MoneyUtils.formatUsd(point.amountUsd) else "مقيد",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = SuccessGreen,
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportsFilterBottomSheet(
  currentFrom: Long?,
  currentTo: Long?,
  currentCustomer: String,
  currentEmployee: String,
  currentQNum: String,
  onApply: (Long?, Long?, String, String, String) -> Unit,
  onClear: () -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var from by remember { mutableStateOf(currentFrom) }
  var to by remember { mutableStateOf(currentTo) }
  var customer by remember { mutableStateOf(currentCustomer) }
  var employee by remember { mutableStateOf(currentEmployee) }
  var qNum by remember { mutableStateOf(currentQNum) }
  var pickingFrom by remember { mutableStateOf(false) }
  var pickingTo by remember { mutableStateOf(false) }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = "تصفية التقارير",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        PremiumSecondaryButton(
          text = from?.let { "من: ${DateUtils.formatNumericDate(it)}" } ?: "من تاريخ",
          icon = Icons.Rounded.CalendarMonth,
          onClick = { pickingFrom = true },
          modifier = Modifier.weight(1f),
        )
        PremiumSecondaryButton(
          text = to?.let { "إلى: ${DateUtils.formatNumericDate(it)}" } ?: "إلى تاريخ",
          icon = Icons.Rounded.CalendarMonth,
          onClick = { pickingTo = true },
          modifier = Modifier.weight(1f),
        )
      }

      PremiumTextField(
        value = customer,
        onValueChange = { customer = it },
        label = "اسم العميل",
        placeholder = "تصفية باسم العميل...",
      )
      PremiumTextField(
        value = employee,
        onValueChange = { employee = it },
        label = "الموظف",
        placeholder = "اسم المستخدم...",
      )
      PremiumTextField(
        value = qNum,
        onValueChange = { qNum = it },
        label = "رقم العرض",
        placeholder = "QU-...",
      )

      Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        PremiumSecondaryButton(
          text = "مسح",
          onClick = onClear,
          modifier = Modifier.weight(1f),
        )
        PremiumPrimaryButton(
          text = "تطبيق",
          onClick = { onApply(from, to, customer, employee, qNum) },
          modifier = Modifier.weight(1f),
        )
      }
    }
  }

  PremiumDatePickerModal(
    visible = pickingFrom,
    initialDateMillis = from ?: System.currentTimeMillis(),
    onDateSelected = { from = it },
    onDismiss = { pickingFrom = false },
  )
  PremiumDatePickerModal(
    visible = pickingTo,
    initialDateMillis = to ?: System.currentTimeMillis(),
    onDateSelected = { to = it },
    onDismiss = { pickingTo = false },
  )
}
