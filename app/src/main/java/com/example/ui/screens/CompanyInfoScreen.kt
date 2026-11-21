package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Apartment
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.ui.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.core.UserRole
import com.example.data.repository.AuthenticatedUser
import com.example.ui.components.PremiumAccessDeniedCard
import com.example.ui.components.PremiumGlassCard
import com.example.ui.components.PremiumPrimaryButton
import com.example.ui.components.PremiumSecondaryButton
import com.example.ui.components.PremiumTextField
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerSoftBg
import com.example.ui.theme.Dimensions
import com.example.ui.theme.LocalLiquidGlassColors
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessSoftBg
import com.example.ui.viewmodel.CompanyViewModel
import java.io.File

@Composable
fun CompanyInfoScreen(
  user: AuthenticatedUser,
  viewModel: CompanyViewModel,
  uiState: com.example.ui.viewmodel.CompanyUiState,
) {
  val canView = user.permissions.viewCompany || user.role == UserRole.SUPER_ADMIN
  val canEdit = user.permissions.editCompany || user.role == UserRole.SUPER_ADMIN

  if (!canView) {
    Box(
      modifier = Modifier.fillMaxSize().statusBarsPadding(),
      contentAlignment = Alignment.Center,
    ) {
      PremiumAccessDeniedCard()
    }
    return
  }

  val photoPickerLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        viewModel.pickLogo(uri)
      }
    }

  val glass = LocalLiquidGlassColors.current
  val logoExists = !uiState.logoPath.isNullOrBlank() && File(uiState.logoPath).exists()

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
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Text(
      text = "بيانات الشركة",
      style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
      color = MaterialTheme.colorScheme.onBackground,
    )
    Text(
      text = "تظهر هذه البيانات والشعار في ترويسة عروض الأسعار وملفات PDF",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    // Company Logo Preview Card
    PremiumGlassCard(contentPadding = PaddingValues(18.dp)) {
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Box(
          modifier =
            Modifier.size(124.dp)
              .clip(RoundedCornerShape(26.dp))
              .background(glass.softBlueBg)
              .border(1.5.dp, glass.glassBorderTop, RoundedCornerShape(26.dp)),
          contentAlignment = Alignment.Center,
        ) {
          if (logoExists) {
            Image(
              painter = rememberAsyncImagePainter(model = File(uiState.logoPath!!)),
              contentDescription = "شعار الشركة",
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize(),
            )
          } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(
                imageVector = Icons.Rounded.Apartment,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp),
              )
              Spacer(Modifier.height(6.dp))
              Text(
                text = "لا يوجد شعار",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }

        if (canEdit) {
          Spacer(Modifier.height(14.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            PremiumSecondaryButton(
              text = if (logoExists) "تغيير الشعار" else "اختيار شعار",
              icon = Icons.Rounded.AddPhotoAlternate,
              onClick = {
                photoPickerLauncher.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
              },
              modifier = Modifier.weight(1f),
              testTag = "pick_logo_button",
            )
            if (logoExists) {
              PremiumSecondaryButton(
                text = "حذف الشعار",
                icon = Icons.Rounded.DeleteOutline,
                isDanger = true,
                onClick = { viewModel.removeLogo() },
                modifier = Modifier.weight(1f),
                testTag = "remove_logo_button",
              )
            }
          }
        }
      }
    }

    // Company Details Glass Form
    PremiumGlassCard(contentPadding = PaddingValues(18.dp)) {
      Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        PremiumTextField(
          value = uiState.companyName,
          onValueChange = viewModel::onNameChange,
          label = "اسم الشركة",
          placeholder = "مثال: شركة التقنية المتقدمة",
          leadingIcon = Icons.Rounded.Apartment,
          enabled = canEdit,
          testTag = "company_name_input",
        )
        PremiumTextField(
          value = uiState.companySubtitle,
          onValueChange = viewModel::onSubtitleChange,
          label = "الاسم الفرعي أو النشاط",
          placeholder = "مثال: للتجارة العامة والمناقصات",
          leadingIcon = Icons.Rounded.Subtitles,
          enabled = canEdit,
        )
        PremiumTextField(
          value = uiState.phone,
          onValueChange = viewModel::onPhoneChange,
          label = "رقم الهاتف",
          placeholder = "+966 5X XXX XXXX",
          leadingIcon = Icons.Rounded.Phone,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          enabled = canEdit,
          testTag = "company_phone_input",
        )
        PremiumTextField(
          value = uiState.address,
          onValueChange = viewModel::onAddressChange,
          label = "العنوان",
          placeholder = "المدينة، الحي، الشارع",
          leadingIcon = Icons.Rounded.LocationOn,
          enabled = canEdit,
          testTag = "company_address_input",
        )
        PremiumTextField(
          value = uiState.extraInfo,
          onValueChange = viewModel::onExtraChange,
          label = "معلومات إضافية",
          placeholder = "السجل التجاري، الرقم الضريبي، البريد الإلكتروني...",
          leadingIcon = Icons.Rounded.Info,
          singleLine = false,
          maxLines = 3,
          enabled = canEdit,
        )
      }
    }

    AnimatedVisibility(visible = !uiState.message.isNullOrBlank()) {
      Surface(
        color = if (uiState.isError) DangerSoftBg else SuccessSoftBg,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(
          text = uiState.message.orEmpty(),
          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
          color = if (uiState.isError) DangerRed else SuccessGreen,
          modifier = Modifier.padding(14.dp),
        )
      }
    }

    if (canEdit) {
      PremiumPrimaryButton(
        text = "حفظ بيانات الشركة",
        icon = Icons.Rounded.Save,
        isLoading = uiState.isSaving,
        onClick = { viewModel.save(user) },
        modifier = Modifier.fillMaxWidth(),
        testTag = "save_company_button",
      )
    }
  }
}
