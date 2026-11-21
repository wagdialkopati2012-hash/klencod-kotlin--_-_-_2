package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.rounded.Apartment
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.example.ui.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Dimensions
import com.example.ui.theme.LocalLiquidGlassColors
import com.example.ui.theme.RoyalBlue

enum class BottomNavTab(
  val route: String,
  val label: String,
  val activeIcon: ImageVector,
  val inactiveIcon: ImageVector,
  val testTag: String,
) {
  HOME("home", "الرئيسية", Icons.Rounded.Home, Icons.Outlined.Home, "nav_home"),
  COMPANY("company", "بيانات الشركة", Icons.Rounded.Apartment, Icons.Outlined.Apartment, "nav_company"),
  PRODUCTS("products", "الأصناف", Icons.Rounded.Inventory2, Icons.Outlined.Inventory2, "nav_products"),
  QUOTATIONS("quotations", "عروض الأسعار", Icons.Rounded.Description, Icons.Outlined.Description, "nav_quotations"),
  MORE("more", "المزيد", Icons.Rounded.MoreHoriz, Icons.Outlined.MoreHoriz, "nav_more"),
}

@Composable
fun PremiumFloatingNavigation(
  selectedTab: BottomNavTab,
  onSelectTab: (BottomNavTab) -> Unit,
  modifier: Modifier = Modifier,
) {
  val glass = LocalLiquidGlassColors.current
  val capsuleShape = RoundedCornerShape(Dimensions.NavCapsuleCorner)

  Box(
    modifier =
      modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        .padding(horizontal = 14.dp, vertical = 10.dp),
    contentAlignment = Alignment.Center,
  ) {
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .shadow(
            elevation = 16.dp,
            shape = capsuleShape,
            ambientColor = glass.shadowColor,
            spotColor = glass.shadowColor,
          )
          .clip(capsuleShape)
          .background(glass.glassSurfaceElevated)
          .border(
            width = 1.dp,
            brush = Brush.linearGradient(listOf(glass.glassBorderTop, glass.glassBorderBottom)),
            shape = capsuleShape,
          )
          .padding(horizontal = 6.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      // In RTL layout direction, index 0 renders on the rightmost side:
      // 1. الرئيسية -> 2. بيانات الشركة -> 3. الأصناف -> 4. عروض الأسعار -> 5. المزيد
      BottomNavTab.entries.forEach { tab ->
        val isSelected = tab == selectedTab
        val scale by
          animateFloatAsState(
            targetValue = if (isSelected) 1.08f else 0.96f,
            animationSpec = spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow),
            label = "navScale_${tab.name}",
          )
        val iconElevation by
          animateDpAsState(
            targetValue = if (isSelected) 6.dp else 0.dp,
            animationSpec = spring(stiffness = Spring.StiffnessMedium),
            label = "navElev_${tab.name}",
          )
        val labelColor by
          animateColorAsState(
            targetValue =
              if (isSelected) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.onSurfaceVariant,
            label = "navLabel_${tab.name}",
          )
        val iconTint by
          animateColorAsState(
            targetValue =
              if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            label = "navIcon_${tab.name}",
          )

        val interactionSource = remember { MutableInteractionSource() }
        Column(
          modifier =
            Modifier.weight(1f)
              .testTag(tab.testTag)
              .clip(RoundedCornerShape(22.dp))
              .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(bounded = true),
                onClick = { onSelectTab(tab) },
              )
              .padding(vertical = 4.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
        ) {
          Box(
            modifier =
              Modifier.graphicsLayer {
                  scaleX = scale
                  scaleY = scale
                }
                .shadow(
                  elevation = iconElevation,
                  shape = RoundedCornerShape(16.dp),
                  ambientColor = glass.primaryAccent.copy(alpha = 0.35f),
                  spotColor = glass.primaryAccent.copy(alpha = 0.35f),
                )
                .clip(RoundedCornerShape(16.dp))
                .background(
                  brush =
                    if (isSelected) {
                      Brush.linearGradient(listOf(glass.primaryAccent, RoyalBlue, glass.tealAccent))
                    } else {
                      Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                    }
                )
                .padding(horizontal = 14.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = if (isSelected) tab.activeIcon else tab.inactiveIcon,
              contentDescription = tab.label,
              tint = iconTint,
              modifier = Modifier.size(21.dp),
            )
          }
          Spacer(Modifier.height(2.dp))
          Text(
            text = tab.label,
            style =
              MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
              ),
            color = labelColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}
