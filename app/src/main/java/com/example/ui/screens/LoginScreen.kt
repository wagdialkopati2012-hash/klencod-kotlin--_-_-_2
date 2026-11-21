package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.components.PremiumPrimaryButton
import com.example.ui.components.PremiumTextField
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerSoftBg
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.LightBlue
import com.example.ui.theme.PremiumTeal
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.RoyalBlue
import com.example.ui.viewmodel.SessionUiState

@Composable
fun LoginScreen(
  sessionState: SessionUiState,
  onLogin: (username: String, password: String, remember: Boolean) -> Unit,
  onClearError: () -> Unit,
) {
  var username by remember(sessionState.rememberedUsername) {
    mutableStateOf(sessionState.rememberedUsername)
  }
  var password by remember { mutableStateOf("") }
  var passwordVisible by remember { mutableStateOf(false) }
  var rememberMe by remember(sessionState.rememberChecked) {
    mutableStateOf(sessionState.rememberChecked)
  }
  var showCard by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) { showCard = true }

  val infiniteTransition = rememberInfiniteTransition(label = "loginBg")
  val drift by
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = 1f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 7000, easing = FastOutSlowInEasing),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "orbDrift",
    )

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(DeepNavy, Color(0xFF1A3A6E), Color(0xFF234E8C), Color(0xFFF0F5FF))
          )
        )
  ) {
    // Subtle animated Liquid Glass background orbs
    Canvas(modifier = Modifier.fillMaxSize()) {
      drawCircle(
        brush =
          Brush.radialGradient(
            colors = listOf(RoyalBlue.copy(alpha = 0.55f), Color.Transparent),
          ),
        radius = size.width * 0.65f,
        center = Offset(size.width * (0.2f + 0.12f * drift), size.height * 0.16f),
      )
      drawCircle(
        brush =
          Brush.radialGradient(
            colors = listOf(PremiumTeal.copy(alpha = 0.35f), Color.Transparent),
          ),
        radius = size.width * 0.55f,
        center = Offset(size.width * (0.85f - 0.1f * drift), size.height * 0.42f),
      )
    }

    Column(
      modifier =
        Modifier.fillMaxSize()
          .safeDrawingPadding()
          .imePadding()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 22.dp, vertical = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      // Animated Logo Emblem
      AnimatedVisibility(
        visible = showCard,
        enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { -40 },
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Box(
            modifier =
              Modifier.size(84.dp)
                .shadow(18.dp, RoundedCornerShape(26.dp), ambientColor = PrimaryBlue)
                .clip(RoundedCornerShape(26.dp))
                .background(Brush.linearGradient(listOf(RoyalBlue, PremiumTeal)))
                .border(1.5.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(26.dp)),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Rounded.Description,
              contentDescription = "شعار نظام المناقصات",
              tint = Color.White,
              modifier = Modifier.size(44.dp),
            )
          }
          Spacer(Modifier.height(16.dp))
          Text(
            text = "نظام المناقصات",
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
          )
          Spacer(Modifier.height(4.dp))
          Text(
            text = "مرحباً بك في نظام المناقصات",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.85f),
          )
        }
      }

      Spacer(Modifier.height(26.dp))

      // Glass Login Card
      AnimatedVisibility(
        visible = showCard,
        enter = fadeIn(tween(600, delayMillis = 120)) + slideInVertically(tween(600)) { 60 },
      ) {
        Column(
          modifier =
            Modifier.widthIn(max = 460.dp)
              .fillMaxWidth()
              .shadow(
                elevation = 20.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = DeepNavy.copy(alpha = 0.25f),
              )
              .clip(RoundedCornerShape(28.dp))
              .background(Color.White.copy(alpha = 0.90f))
              .border(
                width = 1.2.dp,
                brush =
                  Brush.linearGradient(
                    listOf(Color.White, Color.White.copy(alpha = 0.55f))
                  ),
                shape = RoundedCornerShape(28.dp),
              )
              .padding(22.dp),
        ) {
          Text(
            text = "تسجيل الدخول",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = DeepNavy,
          )
          Spacer(Modifier.height(4.dp))
          Text(
            text = "أدخل بيانات حسابك للمتابعة إلى لوحة التحكم",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF64748B),
          )
          Spacer(Modifier.height(18.dp))

          PremiumTextField(
            value = username,
            onValueChange = {
              username = it
              onClearError()
            },
            label = "اسم المستخدم",
            placeholder = "أدخل اسم المستخدم",
            leadingIcon = Icons.Rounded.Person,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            testTag = "username_input",
          )

          Spacer(Modifier.height(14.dp))

          PremiumTextField(
            value = password,
            onValueChange = {
              password = it
              onClearError()
            },
            label = "كلمة المرور",
            placeholder = "••••••••",
            leadingIcon = Icons.Rounded.Lock,
            visualTransformation =
              if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions =
              KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
              ),
            keyboardActions =
              KeyboardActions(onDone = { onLogin(username, password, rememberMe) }),
            trailingContent = {
              IconButton(
                onClick = { passwordVisible = !passwordVisible },
                modifier = Modifier.size(34.dp),
              ) {
                Icon(
                  imageVector =
                    if (passwordVisible) Icons.Rounded.VisibilityOff
                    else Icons.Rounded.Visibility,
                  contentDescription = if (passwordVisible) "إخفاء كلمة المرور" else "إظهار كلمة المرور",
                  tint = Color(0xFF64748B),
                )
              }
            },
            testTag = "password_input",
          )

          Spacer(Modifier.height(10.dp))

          Row(
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { rememberMe = !rememberMe }
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Checkbox(
              checked = rememberMe,
              onCheckedChange = { rememberMe = it },
              colors = CheckboxDefaults.colors(checkedColor = PrimaryBlue),
            )
            Text(
              text = "تذكرني",
              style = MaterialTheme.typography.bodyMedium,
              color = DeepNavy,
            )
          }

          AnimatedVisibility(visible = !sessionState.loginError.isNullOrBlank()) {
            Surface(
              color = DangerSoftBg,
              shape = RoundedCornerShape(14.dp),
              border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.35f)),
              modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
              Text(
                text = sessionState.loginError.orEmpty(),
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = DangerRed,
                modifier = Modifier.padding(12.dp),
              )
            }
          }

          Spacer(Modifier.height(18.dp))

          PremiumPrimaryButton(
            text = "دخول",
            icon = Icons.AutoMirrored.Rounded.Login,
            isLoading = sessionState.isLoggingIn,
            onClick = { onLogin(username, password, rememberMe) },
            modifier = Modifier.fillMaxWidth(),
            testTag = "login_button",
          )

          if (sessionState.isDefaultAdminActive) {
            Spacer(Modifier.height(14.dp))
            Surface(
              color = LightBlue,
              shape = RoundedCornerShape(14.dp),
              border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.25f)),
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(14.dp))
                  .clickable {
                    username = "admin"
                    password = "admin123"
                    onClearError()
                  },
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(
                  imageVector = Icons.Rounded.Info,
                  contentDescription = null,
                  tint = PrimaryBlue,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                  text = "بيانات المدير الافتراضية: admin / admin123 (اضغط للتعبئة)",
                  style = MaterialTheme.typography.labelMedium,
                  color = DeepNavy,
                )
              }
            }
          }
        }
      }

      Spacer(Modifier.height(22.dp))
      Text(
        text = "إصدار v1.0.0  •  بيئة عمل محلية آمنة",
        style = MaterialTheme.typography.labelSmall,
        color = DeepNavy.copy(alpha = 0.65f),
        textAlign = TextAlign.Center,
      )
    }
  }
}
