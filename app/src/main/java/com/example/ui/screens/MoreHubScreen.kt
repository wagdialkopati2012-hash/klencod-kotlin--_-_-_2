package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.ManageAccounts
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.example.ui.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.UserRole
import com.example.data.repository.AuthenticatedUser
import com.example.ui.components.PremiumGlassCard
import com.example.ui.theme.Dimensions
import com.example.ui.theme.PremiumTeal
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.VioletAccent
import com.example.ui.theme.WarningAmber

@Composable
fun MoreHubScreen(
  user: AuthenticatedUser,
  onOpenReports: () -> Unit,
  onOpenUsers: () -> Unit,
  onOpenAuditLog: () -> Unit,
  onOpenSettings: () -> Unit,
) {
  val isSuper = user.role == UserRole.SUPER_ADMIN
  val canReports = user.permissions.viewReports || isSuper
  val canUsers = user.permissions.viewUsers || isSuper
  val canAudit = user.permissions.viewAuditLog || isSuper
  val canSettings = user.permissions.manageSettings || isSuper

  Column(
    modifier =
      Modifier.fillMaxSize()
        .statusBarsPadding()
        .verticalScroll(rememberScrollState())
        .padding(
          start = 16.dp,
          end = 16.dp,
          top = 12.dp,
          bottom = Dimensions.BottomNavClearance,
        ),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Text(
      text = "المزيد",
      style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
      color = MaterialTheme.colorScheme.onBackground,
    )
    Text(
      text = "الأدوات التحليلية، إدارة الصلاحيات، سجل العمليات، وإعدادات النظام",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(4.dp))

    MoreMenuCard(
      title = "التقارير",
      subtitle = "الإحصائيات المالية، الرسوم البيانية، وتقارير العملاء والمستخدمين",
      icon = Icons.Rounded.BarChart,
      accentColor = PrimaryBlue,
      enabled = canReports,
      onClick = onOpenReports,
      testTag = "more_reports",
    )

    MoreMenuCard(
      title = "المستخدمون والصلاحيات",
      subtitle = "إدارة الحسابات، الأدوار، ومصفوفة الصلاحيات التفصيلية",
      icon = Icons.Rounded.ManageAccounts,
      accentColor = PremiumTeal,
      enabled = canUsers,
      onClick = onOpenUsers,
      testTag = "more_users",
    )

    MoreMenuCard(
      title = "سجل العمليات",
      subtitle = "تتبع جميع التعديلات، تسجيلات الدخول، وعمليات الاستيراد والنسخ",
      icon = Icons.Rounded.History,
      accentColor = VioletAccent,
      enabled = canAudit,
      onClick = onOpenAuditLog,
      testTag = "more_audit",
    )

    MoreMenuCard(
      title = "الإعدادات",
      subtitle = "المظهر، النسخ الاحتياطي، استعادة قاعدة البيانات، وتصدير البيانات",
      icon = Icons.Rounded.Settings,
      accentColor = WarningAmber,
      enabled = canSettings,
      onClick = onOpenSettings,
      testTag = "more_settings",
    )
  }
}

@Composable
private fun MoreMenuCard(
  title: String,
  subtitle: String,
  icon: ImageVector,
  accentColor: Color,
  enabled: Boolean,
  onClick: () -> Unit,
  testTag: String,
) {
  PremiumGlassCard(
    modifier = Modifier.fillMaxWidth().testTag(testTag),
    onClick = if (enabled) onClick else null,
    contentPadding = PaddingValues(16.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier =
          Modifier.size(48.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(
              if (enabled) accentColor.copy(alpha = 0.14f)
              else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
            ),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = if (enabled) icon else Icons.Rounded.Lock,
          contentDescription = null,
          tint = if (enabled) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(24.dp),
        )
      }
      Spacer(Modifier.width(14.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color =
            if (enabled) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(2.dp))
        Text(
          text = if (enabled) subtitle else "يتطلب صلاحية إدارية للوصول",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      if (enabled) {
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}
