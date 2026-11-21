package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.core.DateUtils
import com.example.core.UserPermissions
import com.example.core.UserRole
import com.example.data.local.UserEntity
import com.example.data.repository.AuthenticatedUser
import com.example.ui.components.PremiumAccessDeniedCard
import com.example.ui.components.PremiumConfirmationDialog
import com.example.ui.components.PremiumGlassCard
import com.example.ui.components.PremiumPrimaryButton
import com.example.ui.components.PremiumSearchBar
import com.example.ui.components.PremiumSecondaryButton
import com.example.ui.components.PremiumStatusBadge
import com.example.ui.components.PremiumTextField
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerSoftBg
import com.example.ui.theme.LocalLiquidGlassColors
import com.example.ui.theme.PremiumTeal
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessSoftBg
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningSoftBg
import com.example.ui.viewmodel.UsersUiState
import com.example.ui.viewmodel.UsersViewModel

@Composable
fun UsersPermissionsScreen(
  user: AuthenticatedUser,
  viewModel: UsersViewModel,
  uiState: UsersUiState,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)

  val isSuper = user.role == UserRole.SUPER_ADMIN
  val canView = user.permissions.viewUsers || isSuper
  val canAdd = user.permissions.addUser || isSuper
  val canEdit = user.permissions.editUser || isSuper
  val canPerms = user.permissions.managePermissions || isSuper
  val canSuspendOrDelete = user.permissions.suspendOrDeleteUser || isSuper

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
  var showUserForm by remember { mutableStateOf(false) }
  var editingUser by remember { mutableStateOf<UserEntity?>(null) }
  var passwordTarget by remember { mutableStateOf<UserEntity?>(null) }
  var permissionsTarget by remember { mutableStateOf<UserEntity?>(null) }
  var deletingUser by remember { mutableStateOf<UserEntity?>(null) }

  Box(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
    Column(modifier = Modifier.fillMaxSize()) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
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
          text = "المستخدمون والصلاحيات",
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        )
      }

      Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        PremiumSearchBar(
          query = uiState.searchQuery,
          onQueryChange = viewModel::onSearchChange,
          placeholder = "ابحث عن مستخدم...",
        )

        AnimatedVisibility(visible = !uiState.feedbackMessage.isNullOrBlank()) {
          Surface(
            color = if (uiState.isError) DangerSoftBg else SuccessSoftBg,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().clickable { viewModel.clearFeedback() },
          ) {
            Text(
              text = uiState.feedbackMessage.orEmpty(),
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
              color = if (uiState.isError) DangerRed else SuccessGreen,
              modifier = Modifier.padding(12.dp),
            )
          }
        }
      }

      Spacer(Modifier.height(10.dp))

      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 92.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        items(uiState.users, key = { it.id }) { target ->
          val targetRole = UserRole.fromString(target.role)
          val isTargetSuper = targetRole == UserRole.SUPER_ADMIN
          var menuOpen by remember { mutableStateOf(false) }

          PremiumGlassCard(
            onClick =
              if (canPerms) {
                { permissionsTarget = target }
              } else null,
            contentPadding = PaddingValues(14.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Box(
                modifier =
                  Modifier.size(46.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(PrimaryBlue, PremiumTeal))),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = target.fullName.take(1).uppercase(),
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  color = Color.White,
                )
              }
              Spacer(Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = target.fullName,
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                  text = "@${target.username}  •  ${targetRole.arabicLabel}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (target.lastActivityAt != null) {
                  Text(
                    text = "آخر نشاط: ${DateUtils.formatDateTime(target.lastActivityAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }

              PremiumStatusBadge(
                text = if (target.isActive) "نشط" else "معلق",
                textColor = if (target.isActive) SuccessGreen else WarningAmber,
                backgroundColor = if (target.isActive) SuccessSoftBg else WarningSoftBg,
              )

              if (canEdit || canPerms || canSuspendOrDelete) {
                Box {
                  IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "خيارات المستخدم")
                  }
                  DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                  ) {
                    if (canPerms) {
                      DropdownMenuItem(
                        text = { Text("تعديل الصلاحيات") },
                        leadingIcon = { Icon(Icons.Rounded.Security, contentDescription = null) },
                        onClick = {
                          menuOpen = false
                          permissionsTarget = target
                        },
                      )
                    }
                    if (canEdit) {
                      DropdownMenuItem(
                        text = { Text("تعديل البيانات") },
                        leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                        onClick = {
                          menuOpen = false
                          editingUser = target
                          showUserForm = true
                        },
                      )
                      DropdownMenuItem(
                        text = { Text("تغيير كلمة المرور") },
                        leadingIcon = { Icon(Icons.Rounded.Key, contentDescription = null) },
                        onClick = {
                          menuOpen = false
                          passwordTarget = target
                        },
                      )
                    }
                    if (canSuspendOrDelete && !isTargetSuper && target.id != user.entity.id) {
                      DropdownMenuItem(
                        text = { Text(if (target.isActive) "تعليق الحساب" else "تفعيل الحساب") },
                        leadingIcon = {
                          Icon(
                            if (target.isActive) Icons.Rounded.Block else Icons.Rounded.CheckCircle,
                            contentDescription = null,
                          )
                        },
                        onClick = {
                          menuOpen = false
                          viewModel.toggleActive(user, target)
                        },
                      )
                      DropdownMenuItem(
                        text = { Text("حذف المستخدم", color = DangerRed) },
                        leadingIcon = {
                          Icon(
                            Icons.Rounded.DeleteOutline,
                            contentDescription = null,
                            tint = DangerRed,
                          )
                        },
                        onClick = {
                          menuOpen = false
                          deletingUser = target
                        },
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    if (canAdd) {
      FloatingActionButton(
        onClick = {
          editingUser = null
          showUserForm = true
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = PrimaryBlue,
        contentColor = Color.White,
        modifier =
          Modifier.align(Alignment.BottomEnd)
            .padding(18.dp)
            .shadow(12.dp, RoundedCornerShape(20.dp))
            .testTag("add_user_fab"),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 16.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(Icons.Rounded.Add, contentDescription = "إضافة مستخدم")
          Spacer(Modifier.width(6.dp))
          Text("إضافة مستخدم", fontWeight = FontWeight.Bold)
        }
      }
    }
  }

  if (showUserForm) {
    UserFormBottomSheet(
      existing = editingUser,
      onDismiss = {
        showUserForm = false
        editingUser = null
      },
      onSaveNew = { uname, fname, pass, role ->
        viewModel.createUser(user, uname, fname, pass, role)
        showUserForm = false
      },
      onSaveEdit = { target, fname, role ->
        viewModel.updateUser(user, target, fname, role)
        showUserForm = false
        editingUser = null
      },
    )
  }

  passwordTarget?.let { target ->
    ChangePasswordBottomSheet(
      target = target,
      onDismiss = { passwordTarget = null },
      onSave = { newPass ->
        viewModel.changePassword(user, target, newPass)
        passwordTarget = null
      },
    )
  }

  permissionsTarget?.let { target ->
    PermissionsEditorBottomSheet(
      target = target,
      onDismiss = { permissionsTarget = null },
      onSave = { newPerms ->
        viewModel.updatePermissions(user, target, newPerms)
        permissionsTarget = null
      },
    )
  }

  PremiumConfirmationDialog(
    visible = deletingUser != null,
    title = "حذف المستخدم",
    message = "هل أنت متأكد من حذف المستخدم \"${deletingUser?.fullName.orEmpty()}\"؟",
    confirmText = "حذف",
    onConfirm = {
      deletingUser?.let { viewModel.deleteUser(user, it) }
      deletingUser = null
    },
    onDismiss = { deletingUser = null },
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserFormBottomSheet(
  existing: UserEntity?,
  onDismiss: () -> Unit,
  onSaveNew: (username: String, fullName: String, password: String, role: UserRole) -> Unit,
  onSaveEdit: (target: UserEntity, fullName: String, role: UserRole) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var username by remember(existing) { mutableStateOf(existing?.username.orEmpty()) }
  var fullName by remember(existing) { mutableStateOf(existing?.fullName.orEmpty()) }
  var password by remember { mutableStateOf("") }
  var role by remember(existing) {
    mutableStateOf(existing?.let { UserRole.fromString(it.role) } ?: UserRole.USER)
  }
  val isExistingSuper = existing != null && UserRole.fromString(existing.role) == UserRole.SUPER_ADMIN

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
        text = if (existing == null) "إضافة مستخدم جديد" else "تعديل بيانات المستخدم",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
      )

      if (existing == null) {
        PremiumTextField(
          value = username,
          onValueChange = { username = it },
          label = "اسم المستخدم",
          leadingIcon = Icons.Rounded.Person,
        )
      }
      PremiumTextField(
        value = fullName,
        onValueChange = { fullName = it },
        label = "الاسم الكامل",
        leadingIcon = Icons.Rounded.Person,
      )
      if (existing == null) {
        PremiumTextField(
          value = password,
          onValueChange = { password = it },
          label = "كلمة المرور",
          leadingIcon = Icons.Rounded.Lock,
          visualTransformation = PasswordVisualTransformation(),
        )
      }

      if (!isExistingSuper) {
        Text("الدور الوظيفي", style = MaterialTheme.typography.labelLarge)
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          UserRole.entries.forEach { r ->
            FilterChip(
              selected = role == r,
              onClick = { role = r },
              label = { Text(r.arabicLabel) },
            )
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        PremiumSecondaryButton(text = "إلغاء", onClick = onDismiss, modifier = Modifier.weight(1f))
        PremiumPrimaryButton(
          text = "حفظ",
          onClick = {
            if (existing == null) {
              onSaveNew(username, fullName, password, role)
            } else {
              onSaveEdit(existing, fullName, role)
            }
          },
          modifier = Modifier.weight(1f),
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChangePasswordBottomSheet(
  target: UserEntity,
  onDismiss: () -> Unit,
  onSave: (String) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState()
  var newPassword by remember { mutableStateOf("") }
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
        text = "تغيير كلمة المرور (${target.fullName})",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
      )
      PremiumTextField(
        value = newPassword,
        onValueChange = { newPassword = it },
        label = "كلمة المرور الجديدة",
        leadingIcon = Icons.Rounded.Key,
        visualTransformation = PasswordVisualTransformation(),
      )
      Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        PremiumSecondaryButton(text = "إلغاء", onClick = onDismiss, modifier = Modifier.weight(1f))
        PremiumPrimaryButton(
          text = "حفظ",
          onClick = { onSave(newPassword) },
          modifier = Modifier.weight(1f),
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PermissionsEditorBottomSheet(
  target: UserEntity,
  onDismiss: () -> Unit,
  onSave: (UserPermissions) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val role = UserRole.fromString(target.role)
  val isSuper = role == UserRole.SUPER_ADMIN
  var perms by remember(target) {
    mutableStateOf(UserPermissions.fromJson(target.permissionsJson, role))
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 18.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text(
        text = "صلاحيات: ${target.fullName}",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
      )
      if (isSuper) {
        PremiumStatusBadge(
          text = "مدير النظام يمتلك كافة الصلاحيات بشكل دائم",
          textColor = PrimaryBlue,
          backgroundColor = PrimaryBlue.copy(alpha = 0.12f),
        )
      }

      // 1. Company Info
      ExpandablePermissionCategory(
        title = "1. صلاحيات بيانات الشركة",
        isLocked = isSuper,
        items =
          listOf(
            Triple("عرض بيانات الشركة", perms.viewCompany) { v ->
              perms = perms.copy(viewCompany = v)
            },
            Triple("تعديل بيانات والشعار", perms.editCompany) { v ->
              perms = perms.copy(editCompany = v)
            },
          ),
      )

      // 2. Products
      ExpandablePermissionCategory(
        title = "2. صلاحيات الأصناف",
        isLocked = isSuper,
        items =
          listOf(
            Triple("عرض الأصناف", perms.viewProducts) { v -> perms = perms.copy(viewProducts = v) },
            Triple("إضافة صنف", perms.addProduct) { v -> perms = perms.copy(addProduct = v) },
            Triple("تعديل صنف", perms.editProduct) { v -> perms = perms.copy(editProduct = v) },
            Triple("حذف أصناف", perms.deleteProduct) { v -> perms = perms.copy(deleteProduct = v) },
            Triple("استيراد Excel", perms.importExcel) { v -> perms = perms.copy(importExcel = v) },
          ),
      )

      // 3. Quotations
      ExpandablePermissionCategory(
        title = "3. صلاحيات عروض الأسعار",
        isLocked = isSuper,
        items =
          listOf(
            Triple("عرض العروض", perms.viewQuotations) { v ->
              perms = perms.copy(viewQuotations = v)
            },
            Triple("إنشاء عرض سعر", perms.createQuotation) { v ->
              perms = perms.copy(createQuotation = v)
            },
            Triple("تعديل عرض سعر", perms.editQuotation) { v ->
              perms = perms.copy(editQuotation = v)
            },
            Triple("حذف عرض سعر", perms.deleteQuotation) { v ->
              perms = perms.copy(deleteQuotation = v)
            },
            Triple("تصدير ومشاركة PDF", perms.exportPdf) { v -> perms = perms.copy(exportPdf = v) },
            Triple("عرض عروض جميع الموظفين", perms.viewAllUsersQuotations) { v ->
              perms = perms.copy(viewAllUsersQuotations = v)
            },
          ),
      )

      // 4. Reports
      ExpandablePermissionCategory(
        title = "4. صلاحيات التقارير",
        isLocked = isSuper,
        items =
          listOf(
            Triple("الوصول للتقارير", perms.viewReports) { v ->
              perms = perms.copy(viewReports = v)
            },
            Triple("عرض الإجماليات المالية", perms.viewFinancialTotals) { v ->
              perms = perms.copy(viewFinancialTotals = v)
            },
            Triple("تصدير التقارير", perms.exportReports) { v ->
              perms = perms.copy(exportReports = v)
            },
          ),
      )

      // 5. Users
      ExpandablePermissionCategory(
        title = "5. صلاحيات المستخدمين",
        isLocked = isSuper,
        items =
          listOf(
            Triple("عرض المستخدمين", perms.viewUsers) { v -> perms = perms.copy(viewUsers = v) },
            Triple("إضافة مستخدم", perms.addUser) { v -> perms = perms.copy(addUser = v) },
            Triple("تعديل مستخدم", perms.editUser) { v -> perms = perms.copy(editUser = v) },
            Triple("إدارة الصلاحيات", perms.managePermissions) { v ->
              perms = perms.copy(managePermissions = v)
            },
            Triple("تعليق أو حذف مستخدم", perms.suspendOrDeleteUser) { v ->
              perms = perms.copy(suspendOrDeleteUser = v)
            },
          ),
      )

      // 6. System
      ExpandablePermissionCategory(
        title = "6. صلاحيات إدارة النظام",
        isLocked = isSuper,
        items =
          listOf(
            Triple("عرض سجل العمليات", perms.viewAuditLog) { v ->
              perms = perms.copy(viewAuditLog = v)
            },
            Triple("إنشاء نسخ احتياطي", perms.backupDatabase) { v ->
              perms = perms.copy(backupDatabase = v)
            },
            Triple("استعادة نسخة احتياطية", perms.restoreDatabase) { v ->
              perms = perms.copy(restoreDatabase = v)
            },
            Triple("إدارة الإعدادات", perms.manageSettings) { v ->
              perms = perms.copy(manageSettings = v)
            },
          ),
      )

      Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        PremiumSecondaryButton(text = "إغلاق", onClick = onDismiss, modifier = Modifier.weight(1f))
        if (!isSuper) {
          PremiumPrimaryButton(
            text = "حفظ الصلاحيات",
            onClick = { onSave(perms) },
            modifier = Modifier.weight(1f),
          )
        }
      }
    }
  }
}

@Composable
private fun ExpandablePermissionCategory(
  title: String,
  isLocked: Boolean,
  items: List<Triple<String, Boolean, (Boolean) -> Unit>>,
) {
  var expanded by remember { mutableStateOf(false) }
  val enabledCount = items.count { it.second }

  PremiumGlassCard(
    onClick = { expanded = !expanded },
    contentPadding = PaddingValues(14.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        )
        Text(
          text = "$enabledCount من ${items.size} مفعلة",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Icon(
        imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
        contentDescription = null,
      )
    }

    AnimatedVisibility(visible = expanded) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        items.forEach { (label, checked, onChanged) ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = label,
              style = MaterialTheme.typography.bodyMedium,
              modifier = Modifier.weight(1f),
            )
            Switch(
              checked = checked,
              enabled = !isLocked,
              onCheckedChange = onChanged,
            )
          }
        }
      }
    }
  }
}
