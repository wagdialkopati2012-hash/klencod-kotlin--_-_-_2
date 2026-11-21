package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import com.example.ui.i18n.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.core.MoneyUtils
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerSoftBg
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.Dimensions
import com.example.ui.theme.LocalLiquidGlassColors
import com.example.ui.theme.PremiumTeal
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.RoyalBlue
import java.math.BigDecimal

@Composable
fun LiquidGlassBackground(
  modifier: Modifier = Modifier,
  content: @Composable BoxScope.() -> Unit,
) {
  val glass = LocalLiquidGlassColors.current
  val bgColor = MaterialTheme.colorScheme.background
  // Background glows follow the selected color style (cached, drawn once per size/theme).
  val glowPrimary = PrimaryBlue.copy(alpha = if (glass.isDark) 0.26f else 0.16f)
  val glowAccent = PremiumTeal.copy(alpha = if (glass.isDark) 0.15f else 0.12f)
  val glowBottom = PrimaryBlue.copy(alpha = if (glass.isDark) 0.16f else 0.09f)

  Box(modifier = modifier.fillMaxSize().background(bgColor)) {
    Box(
      modifier =
        Modifier.fillMaxSize().drawWithCache {
          val topRightGlow =
            Brush.radialGradient(
              colors = listOf(glowPrimary, Color.Transparent),
              center = Offset(size.width * 0.85f, size.height * 0.08f),
              radius = size.width * 0.85f,
            )
          val midLeftGlow =
            Brush.radialGradient(
              colors = listOf(glowAccent, Color.Transparent),
              center = Offset(size.width * 0.12f, size.height * 0.38f),
              radius = size.width * 0.72f,
            )
          val bottomGlow =
            Brush.radialGradient(
              colors = listOf(glowBottom, Color.Transparent),
              center = Offset(size.width * 0.75f, size.height * 0.85f),
              radius = size.width * 0.85f,
            )
          onDrawBehind {
            drawRect(topRightGlow)
            drawRect(midLeftGlow)
            drawRect(bottomGlow)
          }
        }
    )
    content()
  }
}

@Composable
fun PremiumGlassCard(
  modifier: Modifier = Modifier,
  cornerRadius: Dp = Dimensions.CardCorner,
  contentPadding: PaddingValues = PaddingValues(16.dp),
  onClick: (() -> Unit)? = null,
  elevated: Boolean = false,
  borderColorOverride: Color? = null,
  content: @Composable ColumnScope.() -> Unit,
) {
  val glass = LocalLiquidGlassColors.current
  val shape = RoundedCornerShape(cornerRadius)
  val interactionSource = remember { MutableInteractionSource() }
  // Only clickable cards pay for the press animation; plain cards stay completely static.
  val pressedState = if (onClick != null) interactionSource.collectIsPressedAsState() else null
  val scaleState =
    if (onClick != null) {
      animateFloatAsState(
        targetValue = if (pressedState?.value == true) 0.985f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow),
        label = "glassCardScale",
      )
    } else {
      null
    }

  val surfaceColor = if (elevated) glass.glassSurfaceElevated else glass.glassSurface
  val borderBrush =
    if (borderColorOverride != null) {
      SolidColor(borderColorOverride)
    } else {
      Brush.linearGradient(listOf(glass.glassBorderTop, glass.glassBorderBottom))
    }

  val baseModifier =
    modifier
      .then(
        if (scaleState != null) {
          Modifier.graphicsLayer {
            val sc = scaleState.value
            scaleX = sc
            scaleY = sc
          }
        } else {
          Modifier
        }
      )
      .clip(shape)
      .background(surfaceColor)
      .border(width = 1.dp, brush = borderBrush, shape = shape)

  val clickableModifier =
    if (onClick != null) {
      baseModifier.clickable(
        interactionSource = interactionSource,
        indication = androidx.compose.material3.ripple(),
        onClick = onClick,
      )
    } else {
      baseModifier
    }

  Column(
    modifier = clickableModifier.padding(contentPadding),
    content = content,
  )
}

@Composable
fun PremiumGradientCard(
  modifier: Modifier = Modifier,
  cornerRadius: Dp = Dimensions.CardCornerLarge,
  gradientColors: List<Color>? = null,
  contentPadding: PaddingValues = PaddingValues(20.dp),
  content: @Composable ColumnScope.() -> Unit,
) {
  val glass = LocalLiquidGlassColors.current
  val shape = RoundedCornerShape(cornerRadius)
  val colors = gradientColors ?: listOf(DeepNavy, RoyalBlue, glass.tealAccent)

  Column(
    modifier =
      modifier
        .shadow(
          elevation = 14.dp,
          shape = shape,
          ambientColor = PrimaryBlue.copy(alpha = 0.25f),
          spotColor = PrimaryBlue.copy(alpha = 0.30f),
        )
        .clip(shape)
        .background(Brush.linearGradient(colors))
        .border(
          width = 1.dp,
          brush =
            Brush.linearGradient(
              listOf(Color.White.copy(alpha = 0.45f), Color.White.copy(alpha = 0.12f))
            ),
          shape = shape,
        )
        .padding(contentPadding),
    content = content,
  )
}

@Composable
fun PremiumPrimaryButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  icon: ImageVector? = null,
  enabled: Boolean = true,
  isLoading: Boolean = false,
  testTag: String? = null,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val scale by
    animateFloatAsState(
      targetValue = if (isPressed && enabled) 0.97f else 1f,
      animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium),
      label = "primaryBtnScale",
    )
  val shape = RoundedCornerShape(Dimensions.ButtonCorner)
  val glass = LocalLiquidGlassColors.current

  val finalModifier =
    modifier
      .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
      .heightIn(min = 50.dp)
      .graphicsLayer {
        scaleX = scale
        scaleY = scale
      }
      .shadow(
        elevation = if (enabled) 8.dp else 0.dp,
        shape = shape,
        ambientColor = glass.primaryAccent.copy(alpha = 0.30f),
        spotColor = glass.primaryAccent.copy(alpha = 0.35f),
      )

  Button(
    onClick = onClick,
    enabled = enabled && !isLoading,
    interactionSource = interactionSource,
    shape = shape,
    colors =
      ButtonDefaults.buttonColors(
        containerColor = Color.Transparent,
        contentColor = Color.White,
        disabledContainerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
      ),
    contentPadding = PaddingValues(0.dp),
    modifier = finalModifier,
  ) {
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .heightIn(min = 50.dp)
          .background(
            brush =
              if (enabled) {
                Brush.horizontalGradient(listOf(glass.primaryAccent, RoyalBlue, glass.tealAccent))
              } else {
                SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
              },
            shape = shape,
          )
          .border(
            width = 1.dp,
            color = Color.White.copy(alpha = if (enabled) 0.32f else 0.1f),
            shape = shape,
          )
          .padding(horizontal = 18.dp, vertical = 12.dp),
      contentAlignment = Alignment.Center,
    ) {
      if (isLoading) {
        CircularProgressIndicator(
          modifier = Modifier.size(22.dp),
          color = Color.White,
          strokeWidth = 2.4.dp,
        )
      } else {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center,
        ) {
          if (icon != null) {
            Icon(
              imageVector = icon,
              contentDescription = null,
              modifier = Modifier.size(19.dp),
            )
            Spacer(Modifier.width(8.dp))
          }
          Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}

@Composable
fun PremiumSecondaryButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  icon: ImageVector? = null,
  enabled: Boolean = true,
  isDanger: Boolean = false,
  testTag: String? = null,
) {
  val glass = LocalLiquidGlassColors.current
  val shape = RoundedCornerShape(Dimensions.ButtonCorner)
  val tint = if (isDanger) DangerRed else MaterialTheme.colorScheme.primary

  OutlinedButton(
    onClick = onClick,
    enabled = enabled,
    shape = shape,
    border = BorderStroke(1.dp, tint.copy(alpha = if (enabled) 0.40f else 0.18f)),
    colors =
      ButtonDefaults.outlinedButtonColors(
        containerColor =
          if (isDanger) DangerSoftBg.copy(alpha = if (glass.isDark) 0.16f else 0.85f)
          else glass.glassSurfaceElevated,
        contentColor = tint,
      ),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    modifier =
      modifier
        .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
        .heightIn(min = 48.dp),
  ) {
    if (icon != null) {
      Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(Modifier.width(8.dp))
    }
    Text(
      text = text,
      style = MaterialTheme.typography.labelLarge,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
fun PremiumGlassButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  icon: ImageVector? = null,
  enabled: Boolean = true,
  testTag: String? = null,
) {
  PremiumSecondaryButton(
    text = text,
    onClick = onClick,
    modifier = modifier,
    icon = icon,
    enabled = enabled,
    testTag = testTag,
  )
}

@Composable
fun PremiumTextField(
  value: String,
  onValueChange: (String) -> Unit,
  label: String,
  modifier: Modifier = Modifier,
  placeholder: String = "",
  leadingIcon: ImageVector? = null,
  trailingContent: @Composable (() -> Unit)? = null,
  singleLine: Boolean = true,
  maxLines: Int = if (singleLine) 1 else 4,
  enabled: Boolean = true,
  isError: Boolean = false,
  errorText: String? = null,
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  keyboardActions: KeyboardActions = KeyboardActions.Default,
  visualTransformation: VisualTransformation = VisualTransformation.None,
  testTag: String? = null,
) {
  val glass = LocalLiquidGlassColors.current
  val interactionSource = remember { MutableInteractionSource() }
  val isFocused by interactionSource.collectIsFocusedAsState()
  val borderColor by
    animateColorAsState(
      targetValue =
        when {
          isError -> DangerRed
          isFocused -> MaterialTheme.colorScheme.primary
          else -> MaterialTheme.colorScheme.outline
        },
      label = "inputBorder",
    )
  val borderWidth = if (isFocused || isError) 1.5.dp else 1.dp
  val shape = RoundedCornerShape(Dimensions.InputCorner)

  Column(modifier = modifier) {
    if (label.isNotEmpty()) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color =
          if (isError) DangerRed
          else if (isFocused) MaterialTheme.colorScheme.primary
          else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 6.dp, start = 4.dp),
      )
    }

    Row(
      modifier =
        Modifier.fillMaxWidth()
          .heightIn(min = 52.dp)
          .clip(shape)
          .background(glass.glassSurfaceElevated)
          .border(width = borderWidth, color = borderColor, shape = shape)
          .padding(horizontal = 14.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      if (leadingIcon != null) {
        Icon(
          imageVector = leadingIcon,
          contentDescription = null,
          tint =
            if (isFocused) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
      }

      Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
        if (value.isEmpty() && placeholder.isNotEmpty()) {
          Text(
            text = placeholder,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        BasicTextField(
          value = value,
          onValueChange = onValueChange,
          enabled = enabled,
          singleLine = singleLine,
          maxLines = maxLines,
          interactionSource = interactionSource,
          keyboardOptions = keyboardOptions,
          keyboardActions = keyboardActions,
          visualTransformation = visualTransformation,
          cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
          textStyle =
            LocalTextStyle.current.copy(
              fontFamily = MaterialTheme.typography.bodyLarge.fontFamily,
              fontSize = 15.sp,
              color =
                if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant,
            ),
          modifier =
            Modifier.fillMaxWidth().then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        )
      }

      if (trailingContent != null) {
        Spacer(Modifier.width(8.dp))
        trailingContent()
      }
    }

    AnimatedVisibility(visible = isError && !errorText.isNullOrBlank()) {
      Text(
        text = errorText.orEmpty(),
        style = MaterialTheme.typography.bodySmall,
        color = DangerRed,
        modifier = Modifier.padding(top = 4.dp, start = 4.dp),
      )
    }
  }
}

@Composable
fun PremiumSearchBar(
  query: String,
  onQueryChange: (String) -> Unit,
  placeholder: String,
  modifier: Modifier = Modifier,
  testTag: String? = null,
) {
  PremiumTextField(
    value = query,
    onValueChange = onQueryChange,
    label = "",
    placeholder = placeholder,
    leadingIcon = Icons.Rounded.Search,
    trailingContent =
      if (query.isNotEmpty()) {
        {
          IconButton(
            onClick = { onQueryChange("") },
            modifier = Modifier.size(32.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.Close,
              contentDescription = "مسح البحث",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(18.dp),
            )
          }
        }
      } else null,
    modifier = modifier,
    testTag = testTag,
  )
}

@Composable
fun PremiumSectionHeader(
  title: String,
  modifier: Modifier = Modifier,
  subtitle: String? = null,
  actionText: String? = null,
  onActionClick: (() -> Unit)? = null,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onBackground,
      )
      if (!subtitle.isNullOrBlank()) {
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
    if (actionText != null && onActionClick != null) {
      TextButton(onClick = onActionClick) {
        Text(
          text = actionText,
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary,
        )
      }
    }
  }
}

@Composable
fun PremiumEmptyState(
  icon: ImageVector,
  title: String,
  description: String,
  modifier: Modifier = Modifier,
  actionLabel: String? = null,
  onAction: (() -> Unit)? = null,
) {
  PremiumGlassCard(
    modifier = modifier.fillMaxWidth(),
    contentPadding = PaddingValues(28.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Box(
        modifier =
          Modifier.size(64.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(32.dp),
        )
      }
      Spacer(Modifier.height(14.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
      )
      Spacer(Modifier.height(6.dp))
      Text(
        text = description,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
      if (actionLabel != null && onAction != null) {
        Spacer(Modifier.height(16.dp))
        PremiumPrimaryButton(
          text = actionLabel,
          onClick = onAction,
          modifier = Modifier.fillMaxWidth(0.7f),
        )
      }
    }
  }
}

@Composable
fun PremiumLoadingIndicator(
  message: String = "جارٍ التحميل...",
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxWidth().padding(32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(12.dp))
    Text(
      text = message,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
fun PremiumPriceText(
  amountUsd: String,
  modifier: Modifier = Modifier,
  color: Color = MaterialTheme.colorScheme.primary,
  fontSizeSp: Int = 16,
  fontWeight: FontWeight = FontWeight.Bold,
) {
  val bd = amountUsd.toBigDecimalOrNull() ?: BigDecimal.ZERO
  Text(
    text = MoneyUtils.formatUsd(bd),
    style =
      MaterialTheme.typography.titleMedium.copy(
        fontSize = fontSizeSp.sp,
        fontWeight = fontWeight,
      ),
    color = color,
    modifier = modifier,
    maxLines = 1,
  )
}

@Composable
fun PremiumConfirmationDialog(
  visible: Boolean,
  title: String,
  message: String,
  confirmText: String = "تأكيد",
  cancelText: String = "إلغاء",
  isDestructive: Boolean = true,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
) {
  if (!visible) return
  val glass = LocalLiquidGlassColors.current
  Dialog(onDismissRequest = onDismiss) {
    AnimatedVisibility(
      visible = true,
      enter = fadeIn() + scaleIn(initialScale = 0.92f),
      exit = fadeOut(),
    ) {
      Surface(
        shape = RoundedCornerShape(26.dp),
        color = glass.glassSurfaceElevated,
        tonalElevation = 8.dp,
        border = BorderStroke(1.dp, glass.glassBorderTop),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(22.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Box(
            modifier =
              Modifier.size(54.dp)
                .clip(CircleShape)
                .background(
                  if (isDestructive) DangerSoftBg
                  else MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                ),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Rounded.WarningAmber,
              contentDescription = null,
              tint = if (isDestructive) DangerRed else MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(28.dp),
            )
          }
          Spacer(Modifier.height(14.dp))
          Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
          )
          Spacer(Modifier.height(8.dp))
          Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
          )
          Spacer(Modifier.height(20.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            PremiumSecondaryButton(
              text = cancelText,
              onClick = onDismiss,
              modifier = Modifier.weight(1f),
            )
            if (isDestructive) {
              Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(Dimensions.ButtonCorner),
                colors =
                  ButtonDefaults.buttonColors(
                    containerColor = DangerRed,
                    contentColor = Color.White,
                  ),
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
              ) {
                Text(confirmText, fontWeight = FontWeight.Bold)
              }
            } else {
              PremiumPrimaryButton(
                text = confirmText,
                onClick = onConfirm,
                modifier = Modifier.weight(1f),
              )
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumDatePickerModal(
  visible: Boolean,
  initialDateMillis: Long,
  onDateSelected: (Long) -> Unit,
  onDismiss: () -> Unit,
) {
  if (!visible) return
  val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialDateMillis)
  DatePickerDialog(
    onDismissRequest = onDismiss,
    confirmButton = {
      TextButton(
        onClick = {
          pickerState.selectedDateMillis?.let { onDateSelected(it) }
          onDismiss()
        }
      ) {
        Text("اختيار", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("إلغاء")
      }
    },
  ) {
    DatePicker(state = pickerState)
  }
}

@Composable
fun PremiumAccessDeniedCard(
  title: String = "الوصول مقيد",
  message: String = "لا تملك الصلاحية اللازمة لعرض هذا القسم. تواصل مع مدير النظام لتفعيل الصلاحية.",
  onBack: (() -> Unit)? = null,
) {
  PremiumGlassCard(
    modifier = Modifier.fillMaxWidth().padding(16.dp),
    contentPadding = PaddingValues(24.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Box(
        modifier =
          Modifier.size(64.dp).clip(CircleShape).background(DangerSoftBg),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Rounded.Lock,
          contentDescription = null,
          tint = DangerRed,
          modifier = Modifier.size(30.dp),
        )
      }
      Spacer(Modifier.height(14.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
      )
      Spacer(Modifier.height(6.dp))
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
      if (onBack != null) {
        Spacer(Modifier.height(16.dp))
        PremiumSecondaryButton(text = "رجوع", onClick = onBack)
      }
    }
  }
}
