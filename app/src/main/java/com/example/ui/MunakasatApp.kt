package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.AppContainer
import com.example.data.local.CompanyInfoEntity
import com.example.data.repository.AuthenticatedUser
import com.example.ui.components.BottomNavTab
import com.example.ui.components.LiquidGlassBackground
import com.example.ui.components.PremiumFloatingNavigation
import com.example.ui.components.PremiumLoadingIndicator
import com.example.ui.screens.AuditLogScreen
import com.example.ui.screens.CompanyInfoScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MoreHubScreen
import com.example.ui.screens.ProductsScreen
import com.example.ui.screens.QuotationEditorScreen
import com.example.ui.screens.QuotationPreviewScreen
import com.example.ui.screens.QuotationsListScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.UsersPermissionsScreen
import com.example.ui.theme.MunakasatTheme
import com.example.ui.viewmodel.AuditLogViewModel
import com.example.ui.viewmodel.CompanyViewModel
import com.example.ui.viewmodel.DashboardViewModel
import com.example.ui.viewmodel.ProductsViewModel
import com.example.ui.viewmodel.QuotationEditorViewModel
import com.example.ui.viewmodel.QuotationsListViewModel
import com.example.ui.viewmodel.ReportsViewModel
import com.example.ui.viewmodel.SessionUiState
import com.example.ui.viewmodel.SessionViewModel
import com.example.ui.viewmodel.SettingsViewModel
import com.example.ui.viewmodel.UsersViewModel

private object Routes {
  const val HOME = "home"
  const val COMPANY = "company"
  const val PRODUCTS = "products"
  const val QUOTATIONS = "quotations"
  const val MORE = "more"
  const val QUOTATION_EDITOR = "quotation_editor?id={id}"
  const val QUOTATION_PREVIEW = "quotation_preview/{id}"
  const val REPORTS = "reports"
  const val USERS = "users"
  const val AUDIT_LOG = "audit_log"
  const val SETTINGS = "settings"

  fun quotationEditor(id: Long? = null): String =
    if (id != null && id > 0L) "quotation_editor?id=$id" else "quotation_editor?id=-1"

  fun quotationPreview(id: Long): String = "quotation_preview/$id"
}

@Composable
fun MunakasatApp(container: AppContainer) {
  val sessionViewModel: SessionViewModel =
    viewModel(factory = SessionViewModel.Factory(container))
  val sessionState by sessionViewModel.uiState.collectAsStateWithLifecycle()

  val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
  val isDark =
    when (sessionState.themeMode) {
      com.example.data.prefs.ThemeMode.SYSTEM -> systemDark
      com.example.data.prefs.ThemeMode.LIGHT -> false
      com.example.data.prefs.ThemeMode.DARK -> true
    }

  MunakasatTheme(
    darkTheme = isDark,
    themeStyle = sessionState.themeStyle,
    amoled = sessionState.amoled,
    language = sessionState.language,
    fontSize = sessionState.fontSize,
  ) {
    AnimatedContent(
      targetState = Triple(sessionState.isInitializing, sessionState.currentUser != null, Unit),
      transitionSpec = { fadeIn() togetherWith fadeOut() },
      label = "authRoot",
    ) { (isInitializing, isAuthenticated, _) ->
      when {
        isInitializing -> {
          LiquidGlassBackground {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              PremiumLoadingIndicator(message = "جارٍ تجهيز نظام المناقصات...")
            }
          }
        }
        !isAuthenticated -> {
          LoginScreen(
            sessionState = sessionState,
            onLogin = sessionViewModel::login,
            onClearError = sessionViewModel::clearLoginError,
          )
        }
        else -> {
          val activeUser = sessionState.currentUser!!
          MainAuthenticatedShell(
            container = container,
            user = activeUser,
            sessionState = sessionState,
            sessionViewModel = sessionViewModel,
          )
        }
      }
    }
  }
}

@Composable
private fun MainAuthenticatedShell(
  container: AppContainer,
  user: AuthenticatedUser,
  sessionState: SessionUiState,
  sessionViewModel: SessionViewModel,
) {
  val navController = rememberNavController()
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route ?: Routes.HOME

  val dashboardVm: DashboardViewModel =
    viewModel(factory = DashboardViewModel.Factory(container))
  val companyVm: CompanyViewModel = viewModel(factory = CompanyViewModel.Factory(container))
  val productsVm: ProductsViewModel = viewModel(factory = ProductsViewModel.Factory(container))
  val quotationsListVm: QuotationsListViewModel =
    viewModel(factory = QuotationsListViewModel.Factory(container))
  val quotationEditorVm: QuotationEditorViewModel =
    viewModel(factory = QuotationEditorViewModel.Factory(container))
  val reportsVm: ReportsViewModel = viewModel(factory = ReportsViewModel.Factory(container))
  val usersVm: UsersViewModel = viewModel(factory = UsersViewModel.Factory(container))
  val auditVm: AuditLogViewModel = viewModel(factory = AuditLogViewModel.Factory(container))
  val settingsVm: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory(container))

  // Flows are only COLLECTED inside the visible destination, so hidden screens don't keep
  // querying / recomposing in the background (a big smoothness win).
  val dashboardFlow =
    remember(user.entity.id, user.entity.permissionsJson) { dashboardVm.observeDashboard(user) }
  val quotationsListFlow =
    remember(user.entity.id, user.entity.permissionsJson) { quotationsListVm.observeList(user) }
  val reportsFlow =
    remember(user.entity.id, user.entity.permissionsJson) { reportsVm.observeReports(user) }

  val companyEntityFlow by
    container.companyRepository.companyInfo.collectAsStateWithLifecycle(initialValue = null)
  val companyEntity =
    companyEntityFlow
      ?: CompanyInfoEntity(
        id = 1,
        companyName = "شركة التقنية المتقدمة",
        companySubtitle = "للتجارة العامة والمناقصات",
      )

  var openAddProductOnNextVisit by remember { mutableStateOf(false) }

  val topLevelRoutes =
    setOf(Routes.HOME, Routes.COMPANY, Routes.PRODUCTS, Routes.QUOTATIONS, Routes.MORE)
  val showFloatingNav = currentRoute in topLevelRoutes

  val selectedTab =
    when (currentRoute) {
      Routes.HOME -> BottomNavTab.HOME
      Routes.COMPANY -> BottomNavTab.COMPANY
      Routes.PRODUCTS -> BottomNavTab.PRODUCTS
      Routes.QUOTATIONS -> BottomNavTab.QUOTATIONS
      Routes.MORE -> BottomNavTab.MORE
      else -> BottomNavTab.MORE
    }

  fun navigateToTab(tab: BottomNavTab) {
    navController.navigate(tab.route) {
      popUpTo(navController.graph.findStartDestination().id) { saveState = true }
      launchSingleTop = true
      restoreState = true
    }
  }

  LiquidGlassBackground {
    Box(modifier = Modifier.fillMaxSize()) {
      NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        enterTransition = { fadeIn(tween(170)) },
        exitTransition = { fadeOut(tween(110)) },
        popEnterTransition = { fadeIn(tween(170)) },
        popExitTransition = { fadeOut(tween(110)) },
      ) {
        composable(Routes.HOME) {
          val dashboardState by dashboardFlow.collectAsStateWithLifecycle()
          HomeDashboardScreen(
            user = user,
            dashboardState = dashboardState,
            onCreateQuotation = {
              quotationEditorVm.initialize(null)
              navController.navigate(Routes.quotationEditor())
            },
            onAddProduct = {
              openAddProductOnNextVisit = true
              navigateToTab(BottomNavTab.PRODUCTS)
            },
            onBrowseQuotations = { navigateToTab(BottomNavTab.QUOTATIONS) },
            onOpenReports = { navController.navigate(Routes.REPORTS) },
            onOpenAuditLog = { navController.navigate(Routes.AUDIT_LOG) },
            onPreviewQuotation = { id -> navController.navigate(Routes.quotationPreview(id)) },
          )
        }

        composable(Routes.COMPANY) {
          val companyState by companyVm.uiState.collectAsStateWithLifecycle()
          CompanyInfoScreen(
            user = user,
            viewModel = companyVm,
            uiState = companyState,
          )
        }

        composable(Routes.PRODUCTS) {
          val productsState by productsVm.uiState.collectAsStateWithLifecycle()
          ProductsScreen(
            user = user,
            viewModel = productsVm,
            uiState = productsState,
            openAddDialogInitially = openAddProductOnNextVisit,
            onConsumedInitialAdd = { openAddProductOnNextVisit = false },
          )
        }

        composable(Routes.QUOTATIONS) {
          val quotationsListState by quotationsListFlow.collectAsStateWithLifecycle()
          QuotationsListScreen(
            user = user,
            companyInfo = companyEntity,
            viewModel = quotationsListVm,
            uiState = quotationsListState,
            onCreateNew = {
              quotationEditorVm.initialize(null)
              navController.navigate(Routes.quotationEditor())
            },
            onEditQuotation = { id ->
              quotationEditorVm.initialize(id)
              navController.navigate(Routes.quotationEditor(id))
            },
            onPreviewQuotation = { id -> navController.navigate(Routes.quotationPreview(id)) },
          )
        }

        composable(Routes.MORE) {
          MoreHubScreen(
            user = user,
            onOpenReports = { navController.navigate(Routes.REPORTS) },
            onOpenUsers = { navController.navigate(Routes.USERS) },
            onOpenAuditLog = { navController.navigate(Routes.AUDIT_LOG) },
            onOpenSettings = { navController.navigate(Routes.SETTINGS) },
          )
        }

        composable(
          route = Routes.QUOTATION_EDITOR,
          arguments =
            listOf(
              navArgument("id") {
                type = NavType.LongType
                defaultValue = -1L
              }
            ),
        ) { backStackEntry ->
          val editorState by quotationEditorVm.uiState.collectAsStateWithLifecycle()
          val argId = backStackEntry.arguments?.getLong("id") ?: -1L
          LaunchedEffect(argId) {
            if (argId > 0L && editorState.editingId != argId) {
              quotationEditorVm.initialize(argId)
            }
          }
          QuotationEditorScreen(
            user = user,
            viewModel = quotationEditorVm,
            uiState = editorState,
            onCancel = { navController.popBackStack() },
            onSavedAndPreview = { savedId ->
              navController.popBackStack()
              navController.navigate(Routes.quotationPreview(savedId))
            },
          )
        }

        composable(
          route = Routes.QUOTATION_PREVIEW,
          arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) { backStackEntry ->
          val qId = backStackEntry.arguments?.getLong("id") ?: 0L
          val quotationWithItems by
            remember(qId) { container.quotationRepository.observeQuotation(qId) }
              .collectAsStateWithLifecycle(initialValue = null)
          QuotationPreviewScreen(
            user = user,
            quotationWithItems = quotationWithItems,
            companyInfo = companyEntity,
            onBack = { navController.popBackStack() },
          )
        }

        composable(Routes.REPORTS) {
          val reportsState by reportsFlow.collectAsStateWithLifecycle()
          ReportsScreen(
            user = user,
            viewModel = reportsVm,
            uiState = reportsState,
            onBack = { navController.popBackStack() },
          )
        }

        composable(Routes.USERS) {
          val usersState by usersVm.uiState.collectAsStateWithLifecycle()
          UsersPermissionsScreen(
            user = user,
            viewModel = usersVm,
            uiState = usersState,
            onBack = { navController.popBackStack() },
          )
        }

        composable(Routes.AUDIT_LOG) {
          val auditState by auditVm.uiState.collectAsStateWithLifecycle()
          AuditLogScreen(
            user = user,
            viewModel = auditVm,
            uiState = auditState,
            onBack = { navController.popBackStack() },
          )
        }

        composable(Routes.SETTINGS) {
          val settingsState by settingsVm.uiState.collectAsStateWithLifecycle()
          SettingsScreen(
            user = user,
            sessionState = sessionState,
            settingsViewModel = settingsVm,
            settingsState = settingsState,
            onSetThemeMode = sessionViewModel::setThemeMode,
            onSetAmoled = sessionViewModel::setAmoled,
            onSetThemeStyle = sessionViewModel::setThemeStyle,
            onSetLanguage = sessionViewModel::setLanguage,
            onSetFontSize = sessionViewModel::setFontSize,
            onLogout = sessionViewModel::logout,
            onBack = { navController.popBackStack() },
          )
        }
      }

      if (showFloatingNav) {
        PremiumFloatingNavigation(
          selectedTab = selectedTab,
          onSelectTab = ::navigateToTab,
          modifier = Modifier.align(Alignment.BottomCenter),
        )
      }
    }
  }
}
