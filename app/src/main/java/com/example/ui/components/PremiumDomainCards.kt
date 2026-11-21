package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.example.ui.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.DateUtils
import com.example.core.MoneyUtils
import com.example.data.local.ProductEntity
import com.example.data.local.QuotationStatus
import com.example.data.local.QuotationWithItems
import com.example.ui.theme.CyanSoftBg
import com.example.ui.theme.DangerRed
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.InfoSoftBg
import com.example.ui.theme.LocalLiquidGlassColors
import com.example.ui.theme.PremiumTeal
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessSoftBg
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningSoftBg

@Composable
fun PremiumStatCard(
  label: String,
  value: String,
  icon: ImageVector,
  accentColor: Color,
  iconBgColor: Color,
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null,
) {
  val glass = LocalLiquidGlassColors.current
  val effectiveIconBg = if (glass.isDark) accentColor.copy(alpha = 0.22f) else iconBgColor

  PremiumGlassCard(
    modifier = modifier,
    onClick = onClick,
    contentPadding = PaddingValues(14.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f),
      )
      Spacer(Modifier.width(6.dp))
      Box(
        modifier =
          Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(effectiveIconBg),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = accentColor,
          modifier = Modifier.size(19.dp),
        )
      }
    }
    Spacer(Modifier.height(10.dp))
    Text(
      text = value,
      style =
        MaterialTheme.typography.headlineMedium.copy(
          fontWeight = FontWeight.Bold,
          fontSize = 24.sp,
        ),
      color = MaterialTheme.colorScheme.onSurface,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
fun PremiumStatusBadge(
  text: String,
  textColor: Color,
  backgroundColor: Color,
  modifier: Modifier = Modifier,
) {
  val glass = LocalLiquidGlassColors.current
  val bg = if (glass.isDark) textColor.copy(alpha = 0.18f) else backgroundColor
  Box(
    modifier =
      modifier
        .clip(RoundedCornerShape(10.dp))
        .background(bg)
        .padding(horizontal = 10.dp, vertical = 4.dp),
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
      color = textColor,
      maxLines = 1,
    )
  }
}

@Composable
fun QuotationStatusChip(status: QuotationStatus, modifier: Modifier = Modifier) {
  val (fg, bg) =
    when (status) {
      QuotationStatus.APPROVED -> SuccessGreen to SuccessSoftBg
      QuotationStatus.SENT -> InfoBlue to InfoSoftBg
      QuotationStatus.SAVED -> PremiumTeal to CyanSoftBg
      QuotationStatus.DRAFT -> WarningAmber to WarningSoftBg
    }
  PremiumStatusBadge(text = status.arabicLabel, textColor = fg, backgroundColor = bg, modifier = modifier)
}

@Composable
fun PremiumProductCard(
  product: ProductEntity,
  isSelectionMode: Boolean,
  isSelected: Boolean,
  canEdit: Boolean,
  canDelete: Boolean,
  onClick: () -> Unit,
  onLongClick: () -> Unit,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var menuExpanded by remember { mutableStateOf(false) }

  PremiumGlassCard(
    modifier = modifier.fillMaxWidth(),
    borderColorOverride =
      if (isSelected) MaterialTheme.colorScheme.primary else null,
    onClick = onClick,
    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      if (isSelectionMode) {
        Icon(
          imageVector =
            if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
          contentDescription = if (isSelected) "محدد" else "غير محدد",
          tint =
            if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(26.dp).clickable { onClick() },
        )
        Spacer(Modifier.width(10.dp))
      } else {
        Box(
          modifier =
            Modifier.size(46.dp)
              .clip(RoundedCornerShape(14.dp))
              .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
              .clickable { onLongClick() },
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Rounded.Inventory2,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
          )
        }
        Spacer(Modifier.width(12.dp))
      }

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = product.name,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text(
            text = product.code.ifBlank { "#${product.id}" },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          PremiumPriceText(
            amountUsd = product.priceUsd,
            color = PremiumTeal,
            fontSizeSp = 15,
          )
        }
      }

      if (!isSelectionMode && (canEdit || canDelete)) {
        Box {
          IconButton(onClick = { menuExpanded = true }) {
            Icon(
              imageVector = Icons.Rounded.MoreVert,
              contentDescription = "خيارات الصنف",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
          ) {
            if (canEdit) {
              DropdownMenuItem(
                text = { Text("تعديل") },
                leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                onClick = {
                  menuExpanded = false
                  onEdit()
                },
              )
            }
            if (canDelete) {
              DropdownMenuItem(
                text = { Text("حذف", color = DangerRed) },
                leadingIcon = {
                  Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = DangerRed)
                },
                onClick = {
                  menuExpanded = false
                  onDelete()
                },
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun PremiumQuotationCard(
  quotationWithItems: QuotationWithItems,
  canEdit: Boolean,
  canDelete: Boolean,
  canExportPdf: Boolean,
  onPreview: () -> Unit,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
  onExportPdf: () -> Unit,
  onShare: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val q = quotationWithItems.quotation
  val status = QuotationStatus.fromString(q.status)
  var menuExpanded by remember { mutableStateOf(false) }

  PremiumGlassCard(
    modifier = modifier.fillMaxWidth(),
    onClick = onPreview,
    contentPadding = PaddingValues(16.dp),
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
            text = q.quotationNumber,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = PrimaryBlue,
          )
          QuotationStatusChip(status = status)
        }
        Spacer(Modifier.height(4.dp))
        Text(
          text = q.customerName,
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Box {
        IconButton(onClick = { menuExpanded = true }) {
          Icon(
            imageVector = Icons.Rounded.MoreVert,
            contentDescription = "خيارات عرض السعر",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        DropdownMenu(
          expanded = menuExpanded,
          onDismissRequest = { menuExpanded = false },
        ) {
          DropdownMenuItem(
            text = { Text("معاينة") },
            leadingIcon = { Icon(Icons.Rounded.Visibility, contentDescription = null) },
            onClick = {
              menuExpanded = false
              onPreview()
            },
          )
          if (canEdit) {
            DropdownMenuItem(
              text = { Text("تعديل") },
              leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
              onClick = {
                menuExpanded = false
                onEdit()
              },
            )
          }
          if (canExportPdf) {
            DropdownMenuItem(
              text = { Text("تصدير PDF") },
              leadingIcon = { Icon(Icons.Rounded.PictureAsPdf, contentDescription = null) },
              onClick = {
                menuExpanded = false
                onExportPdf()
              },
            )
            DropdownMenuItem(
              text = { Text("مشاركة") },
              leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = null) },
              onClick = {
                menuExpanded = false
                onShare()
              },
            )
          }
          if (canDelete) {
            DropdownMenuItem(
              text = { Text("حذف", color = DangerRed) },
              leadingIcon = {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = DangerRed)
              },
              onClick = {
                menuExpanded = false
                onDelete()
              },
            )
          }
        }
      }
    }

    Spacer(Modifier.height(10.dp))
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .height(1.dp)
          .background(LocalLiquidGlassColors.current.divider),
    )
    Spacer(Modifier.height(10.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column {
        Text(
          text = "التاريخ: ${DateUtils.formatNumericDate(q.quotationDateMillis)}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = "بواسطة: ${q.createdByUsername}  •  ${quotationWithItems.items.size} أصناف",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      PremiumPriceText(
        amountUsd = q.totalAmountUsd,
        color = SuccessGreen,
        fontSizeSp = 18,
      )
    }
  }
}
