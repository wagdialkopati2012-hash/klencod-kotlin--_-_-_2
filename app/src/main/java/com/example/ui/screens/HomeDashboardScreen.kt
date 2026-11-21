package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddBox
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.BookmarkAdded
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.PostAdd
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.example.ui.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.DateUtils
import com.example.core.MoneyUtils
import com.example.data.local.QuotationStatus
import com.example.data.repository.AuthenticatedUser
import com.example.ui.components.PremiumEmptyState
import com.example.ui.components.PremiumGlassCard
import com.example.ui.components.PremiumGradientCard
import com.example.ui.components.PremiumPriceText
import com.example.ui.components.PremiumSectionHeader
import com.example.ui.components.PremiumStatCard
import com.example.ui.components.QuotationStatusChip
import com.example.ui.theme.CyanSoftBg
import com.example.ui.theme.Dimensions
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
import com.example.ui.viewmodel.DashboardUiState

@Composable
fun HomeDashboardScreen(
  user: AuthenticatedUser,
  dashboardState: DashboardUiState,
  onCreateQuotation: () -> Unit,
  onAddProduct: () -> Unit,
  onBrowseQuotations: () -> Unit,
  onOpenReports: () -> Unit,
  onOpenAuditLog: () -> Unit,
  onPreviewQuotation: (Long) -> Unit,
) {
  var visible by remember { mutableStateOf(false) }
  LaunchedEffect(Unit) { visible = true }

  val glass = LocalLiquidGlassColors.current
  val canViewTotals =
    user.permissions.viewFinancialTotals || user.role == com.example.core.UserRole.SUPER_ADMIN

  LazyColumn(
    modifier = Modifier.fillMaxSize().statusBarsPadding(),
    contentPadding =
      PaddingValues(
        start = 16.dp,
        end = 16.dp,
        top = 12.dp,
        bottom = Dimensions.BottomNavClearance,
      ),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    // Top Header
    item {
      Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f),
        ) {
          Box(
            modifier =
              Modifier.size(48.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(PrimaryBlue, PremiumTeal)))
                .border(1.5.dp, Color.White.copy(alpha = 0.7f), CircleShape),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = user.entity.fullName.take(1).uppercase(),
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
              color = Color.White,
            )
          }
          Spacer(Modifier.width(12.dp))
          Column {
            Text(
              text = "مرحباً ${user.entity.fullName}",
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onBackground,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = DateUtils.formatFullGregorianHeader(),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        if (user.permissions.viewAuditLog || user.role == com.example.core.UserRole.SUPER_ADMIN) {
          Box(
            modifier =
              Modifier.size(46.dp)
                .clip(CircleShape)
                .background(glass.glassSurfaceElevated)
                .border(1.dp, glass.glassBorderTop, CircleShape),
            contentAlignment = Alignment.Center,
          ) {
            IconButton(onClick = onOpenAuditLog) {
              Icon(
                imageVector = Icons.Rounded.NotificationsNone,
                contentDescription = "سجل العمليات",
                tint = MaterialTheme.colorScheme.onSurface,
              )
            }
          }
        }
      }
    }

    // Main Financial Hero Gradient Card
    item {
      AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { 24 },
      ) {
        PremiumGradientCard(
          gradientColors = listOf(glass.heroStart, RoyalBlue, glass.heroEnd),
          contentPadding = PaddingValues(20.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "إجمالي قيمة عروض الأسعار",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White.copy(alpha = 0.88f),
              )
              Spacer(Modifier.height(8.dp))
              Text(
                text =
                  if (canViewTotals) MoneyUtils.formatUsd(dashboardState.totalQuotationValueUsd)
                  else "مقيد بالصلاحيات",
                style =
                  MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp,
                  ),
                color = Color.White,
              )
              Spacer(Modifier.height(8.dp))
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                  Modifier.clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.16f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
              ) {
                Text(
                  text = "${dashboardState.totalQuotationsCount} عرض سعر مسجل",
                  style = MaterialTheme.typography.labelMedium,
                  color = Color.White,
                )
              }
            }

            Box(
              modifier =
                Modifier.size(56.dp)
                  .clip(RoundedCornerShape(18.dp))
                  .background(Color.White.copy(alpha = 0.18f))
                  .border(1.dp, Color.White.copy(alpha = 0.30f), RoundedCornerShape(18.dp)),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Rounded.TrendingUp,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(30.dp),
              )
            }
          }
        }
      }
    }

    // 2x2 Statistics Grid
    item {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          PremiumStatCard(
            label = "إجمالي الأصناف",
            value = dashboardState.totalProductsCount.toString(),
            icon = Icons.Rounded.Inventory2,
            accentColor = PremiumTeal,
            iconBgColor = CyanSoftBg,
            modifier = Modifier.weight(1f),
            onClick = onAddProduct,
          )
          PremiumStatCard(
            label = "عدد عروض الأسعار",
            value = dashboardState.totalQuotationsCount.toString(),
            icon = Icons.Rounded.Description,
            accentColor = VioletAccent,
            iconBgColor = VioletSoftBg,
            modifier = Modifier.weight(1f),
            onClick = onBrowseQuotations,
          )
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          PremiumStatCard(
            label = "العروض المحفوظة",
            value = dashboardState.savedQuotationsCount.toString(),
            icon = Icons.Rounded.BookmarkAdded,
            accentColor = PrimaryBlue,
            iconBgColor = InfoSoftBg,
            modifier = Modifier.weight(1f),
            onClick = onBrowseQuotations,
          )
          PremiumStatCard(
            label = "عدد العملاء",
            value = dashboardState.customersCount.toString(),
            icon = Icons.Rounded.Groups,
            accentColor = WarningAmber,
            iconBgColor = WarningSoftBg,
            modifier = Modifier.weight(1f),
            onClick = onBrowseQuotations,
          )
        }
      }
    }

    // Quick Actions Section
    item {
      Spacer(Modifier.height(2.dp))
      PremiumSectionHeader(title = "العمليات السريعة")
      Spacer(Modifier.height(10.dp))
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          QuickActionGlassTile(
            title = "إنشاء عرض سعر",
            icon = Icons.Rounded.PostAdd,
            accent = PrimaryBlue,
            enabled =
              user.permissions.createQuotation ||
                user.role == com.example.core.UserRole.SUPER_ADMIN,
            onClick = onCreateQuotation,
            modifier = Modifier.weight(1f).testTag("quick_create_quotation"),
          )
          QuickActionGlassTile(
            title = "إضافة صنف",
            icon = Icons.Rounded.AddBox,
            accent = PremiumTeal,
            enabled =
              user.permissions.addProduct || user.role == com.example.core.UserRole.SUPER_ADMIN,
            onClick = onAddProduct,
            modifier = Modifier.weight(1f).testTag("quick_add_product"),
          )
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          QuickActionGlassTile(
            title = "استعراض العروض",
            icon = Icons.Rounded.FolderOpen,
            accent = RoyalBlue,
            enabled =
              user.permissions.viewQuotations ||
                user.role == com.example.core.UserRole.SUPER_ADMIN,
            onClick = onBrowseQuotations,
            modifier = Modifier.weight(1f).testTag("quick_browse_quotations"),
          )
          QuickActionGlassTile(
            title = "التقارير",
            icon = Icons.Rounded.BarChart,
            accent = VioletAccent,
            enabled =
              user.permissions.viewReports || user.role == com.example.core.UserRole.SUPER_ADMIN,
            onClick = onOpenReports,
            modifier = Modifier.weight(1f).testTag("quick_reports"),
          )
        }
      }
    }

    // Recent Activity
    item {
      Spacer(Modifier.height(2.dp))
      PremiumSectionHeader(
        title = "أحدث عروض الأسعار",
        actionText = if (dashboardState.recentQuotations.isNotEmpty()) "عرض الكل" else null,
        onActionClick = onBrowseQuotations,
      )
    }

    if (dashboardState.recentQuotations.isEmpty()) {
      item {
        PremiumEmptyState(
          icon = Icons.Rounded.Description,
          title = "لا توجد عروض أسعار بعد",
          description = "ابدأ بإضافة الأصناف ثم أنشئ أول عرض سعر احترافي للعملاء",
          actionLabel =
            if (user.permissions.createQuotation) "إنشاء عرض سعر جديد" else null,
          onAction = onCreateQuotation,
        )
      }
    } else {
      items(
        items = dashboardState.recentQuotations,
        key = { it.quotation.id },
      ) { item ->
        val q = item.quotation
        PremiumGlassCard(
          onClick = { onPreviewQuotation(q.id) },
          contentPadding = PaddingValues(14.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Text(
                  text = q.customerName,
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f, fill = false),
                )
                QuotationStatusChip(status = QuotationStatus.fromString(q.status))
              }
              Spacer(Modifier.height(4.dp))
              Text(
                text = "${q.quotationNumber}  •  ${DateUtils.formatNumericDate(q.quotationDateMillis)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Spacer(Modifier.width(10.dp))
            if (canViewTotals) {
              PremiumPriceText(
                amountUsd = q.totalAmountUsd,
                color = SuccessGreen,
                fontSizeSp = 16,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun QuickActionGlassTile(
  title: String,
  icon: ImageVector,
  accent: Color,
  enabled: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  PremiumGlassCard(
    modifier = modifier,
    onClick = if (enabled) onClick else null,
    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier =
          Modifier.size(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
              if (enabled) accent.copy(alpha = 0.14f)
              else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
            ),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = if (enabled) accent else MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(20.dp),
        )
      }
      Spacer(Modifier.width(10.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color =
          if (enabled) MaterialTheme.colorScheme.onSurface
          else MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}
