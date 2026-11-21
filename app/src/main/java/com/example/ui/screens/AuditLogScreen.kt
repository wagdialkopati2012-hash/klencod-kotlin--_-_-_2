package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.ui.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.core.DateUtils
import com.example.core.UserRole
import com.example.data.local.AuditLogEntity
import com.example.data.local.AuditOperationType
import com.example.data.repository.AuthenticatedUser
import com.example.ui.components.PremiumAccessDeniedCard
import com.example.ui.components.PremiumDatePickerModal
import com.example.ui.components.PremiumEmptyState
import com.example.ui.components.PremiumGlassCard
import com.example.ui.components.PremiumPrimaryButton
import com.example.ui.components.PremiumSearchBar
import com.example.ui.components.PremiumStatusBadge
import com.example.ui.theme.CyanSoftBg
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerSoftBg
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.InfoSoftBg
import com.example.ui.theme.LocalLiquidGlassColors
import com.example.ui.theme.PremiumTeal
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessSoftBg
import com.example.ui.theme.VioletAccent
import com.example.ui.theme.VioletSoftBg
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningSoftBg
import com.example.ui.viewmodel.AuditLogUiState
import com.example.ui.viewmodel.AuditLogViewModel

@Composable
fun AuditLogScreen(
  user: AuthenticatedUser,
  viewModel: AuditLogViewModel,
  uiState: AuditLogUiState,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)

  val canView = user.permissions.viewAuditLog || user.role == UserRole.SUPER_ADMIN
  if (!canView) {
    Box(
      modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
      contentAlignment = Alignment.Center,
    ) {
      PremiumAccessDeniedCard(onBack = onBack)
    }
    return
  }

  val glass = LocalLiquidGlassColors.current
  var showDatePicker by remember { mutableStateOf(false) }
  var selectedLog by remember { mutableStateOf<AuditLogEntity?>(null) }

  Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
    // Header
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
          text = "سجل العمليات",
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        )
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        if (uiState.dateFilterMillis != null) {
          IconButton(onClick = { viewModel.onDateFilterChange(null) }) {
            Icon(Icons.Rounded.Close, contentDescription = "إزالة فلتر التاريخ")
          }
        }
        IconButton(
          onClick = { showDatePicker = true },
          modifier =
            Modifier.clip(RoundedCornerShape(14.dp))
              .background(
                if (uiState.dateFilterMillis != null) PrimaryBlue.copy(alpha = 0.16f)
                else glass.glassSurfaceElevated
              )
              .border(1.dp, glass.glassBorderTop, RoundedCornerShape(14.dp)),
        ) {
          Icon(Icons.Rounded.CalendarMonth, contentDescription = "تصفية بالتاريخ")
        }
      }
    }

    Column(
      modifier = Modifier.padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      PremiumSearchBar(
        query = uiState.usernameQuery,
        onQueryChange = viewModel::onUsernameChange,
        placeholder = "ابحث باسم المستخدم...",
      )

      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        FilterChip(
          selected = uiState.operationFilter == null,
          onClick = { viewModel.onOperationFilterChange(null) },
          label = { Text("الكل") },
        )
        AuditOperationType.entries.forEach { op ->
          FilterChip(
            selected = uiState.operationFilter == op,
            onClick = {
              viewModel.onOperationFilterChange(if (uiState.operationFilter == op) null else op)
            },
            label = { Text(op.arabicLabel) },
          )
        }
      }
    }

    Spacer(Modifier.height(8.dp))

    if (uiState.logs.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        PremiumEmptyState(
          icon = Icons.Rounded.History,
          title = "سجل العمليات فارغ",
          description = "سيتم تسجيل جميع العمليات والتغييرات تلقائياً مع حفظ القيم السابقة والجديدة",
        )
      }
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        items(uiState.logs, key = { it.id }) { log ->
          val op = AuditOperationType.fromString(log.operationType)
          val (accent, bg) =
            when (op) {
              AuditOperationType.CREATE, AuditOperationType.IMPORT -> SuccessGreen to SuccessSoftBg
              AuditOperationType.DELETE -> DangerRed to DangerSoftBg
              AuditOperationType.UPDATE, AuditOperationType.PERMISSION_CHANGE ->
                WarningAmber to WarningSoftBg
              AuditOperationType.BACKUP, AuditOperationType.RESTORE ->
                VioletAccent to VioletSoftBg
              AuditOperationType.LOGIN, AuditOperationType.LOGOUT ->
                PremiumTeal to CyanSoftBg
              AuditOperationType.EXPORT -> InfoBlue to InfoSoftBg
            }

          PremiumGlassCard(
            onClick = { selectedLog = log },
            contentPadding = PaddingValues(14.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                  modifier =
                    Modifier.size(10.dp)
                      .clip(RoundedCornerShape(5.dp))
                      .background(accent),
                )
                Spacer(Modifier.width(8.dp))
                PremiumStatusBadge(
                  text = op.arabicLabel,
                  textColor = accent,
                  backgroundColor = bg,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                  text = log.affectedEntity,
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Text(
                text = "@${log.username}",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = PrimaryBlue,
              )
            }
            Spacer(Modifier.height(6.dp))
            Text(
              text = log.description,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
              text = DateUtils.formatDateTime(log.timestamp),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }

  PremiumDatePickerModal(
    visible = showDatePicker,
    initialDateMillis = uiState.dateFilterMillis ?: System.currentTimeMillis(),
    onDateSelected = viewModel::onDateFilterChange,
    onDismiss = { showDatePicker = false },
  )

  selectedLog?.let { log ->
    Dialog(onDismissRequest = { selectedLog = null }) {
      Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(20.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text(
            text = "تفاصيل العملية",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
          )
          Text("المستخدم: ${log.username}")
          Text("النوع: ${AuditOperationType.fromString(log.operationType).arabicLabel}")
          Text("السجل المتأثر: ${log.affectedEntity} (#${log.affectedRecordId})")
          Text("الوقت: ${DateUtils.formatDateTime(log.timestamp)}")
          Text("الوصف: ${log.description}")
          if (!log.previousValue.isNullOrBlank()) {
            Surface(
              color = DangerSoftBg,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                text = "القيمة السابقة:\n${log.previousValue}",
                style = MaterialTheme.typography.bodySmall,
                color = DangerRed,
                modifier = Modifier.padding(10.dp),
              )
            }
          }
          if (!log.newValue.isNullOrBlank()) {
            Surface(
              color = SuccessSoftBg,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                text = "القيمة الجديدة:\n${log.newValue}",
                style = MaterialTheme.typography.bodySmall,
                color = SuccessGreen,
                modifier = Modifier.padding(10.dp),
              )
            }
          }
          Spacer(Modifier.height(4.dp))
          PremiumPrimaryButton(
            text = "إغلاق",
            onClick = { selectedLog = null },
            modifier = Modifier.fillMaxWidth(),
          )
        }
      }
    }
  }
}
