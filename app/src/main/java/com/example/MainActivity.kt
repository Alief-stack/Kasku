package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.firebase.FinanceRepository
import com.example.data.model.SavingsActionType
import com.example.data.model.SavingsGoal
import com.example.data.model.TransactionRecord
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.attemptAutoSignIn
import com.example.ui.auth.signOutUser
import com.example.ui.components.AddEditSavingsGoalSheet
import com.example.ui.components.AddEditTransactionSheet
import com.example.ui.components.SavingsMovementSheet
import com.example.ui.components.SetBudgetSheet
import com.example.ui.screens.BerandaScreen
import com.example.ui.screens.PengeluaranScreen
import com.example.ui.screens.RingkasanScreen
import com.example.ui.screens.TabunganScreen
import com.example.ui.state.FinanceViewModel
import com.example.ui.state.UiState
import com.example.ui.theme.KasKuTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

const val ROUTE_BERANDA = "beranda"
const val ROUTE_PENGELUARAN = "pengeluaran"
const val ROUTE_TABUNGAN = "tabungan"
const val ROUTE_RINGKASAN = "ringkasan"

private data class NavDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val kasKuDestinations = listOf(
    NavDestination(
        route = ROUTE_BERANDA,
        labelRes = R.string.nav_beranda,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        testTag = "nav_beranda"
    ),
    NavDestination(
        route = ROUTE_PENGELUARAN,
        labelRes = R.string.nav_pengeluaran,
        selectedIcon = Icons.Filled.ReceiptLong,
        unselectedIcon = Icons.Outlined.ReceiptLong,
        testTag = "nav_pengeluaran"
    ),
    NavDestination(
        route = ROUTE_TABUNGAN,
        labelRes = R.string.nav_tabungan,
        selectedIcon = Icons.Filled.Savings,
        unselectedIcon = Icons.Outlined.Savings,
        testTag = "nav_tabungan"
    ),
    NavDestination(
        route = ROUTE_RINGKASAN,
        labelRes = R.string.nav_ringkasan,
        selectedIcon = Icons.Filled.Assessment,
        unselectedIcon = Icons.Outlined.Assessment,
        testTag = "nav_ringkasan"
    )
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KasKuTheme {
                AppContent()
            }
        }
    }
}

@Composable
fun AppContent() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    LaunchedEffect(Unit) {
        if (Firebase.auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = { currentUser = Firebase.auth.currentUser },
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = { currentUser = Firebase.auth.currentUser }
        )
    } else {
        val uid = user.uid
        key(uid) {
            AuthenticatedKasKuApp(
                currentUserId = uid,
                userName = user.displayName,
                userEmail = user.email,
                onSignOut = {
                    signOutUser(
                        credentialManager = credentialManager,
                        scope = scope,
                        onSignOutComplete = { currentUser = null }
                    )
                }
            )
        }
    }
}

@Composable
private fun AuthenticatedKasKuApp(
    currentUserId: String,
    userName: String?,
    userEmail: String?,
    onSignOut: () -> Unit
) {
    val viewModel: FinanceViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                FinanceViewModel(FinanceRepository(app), currentUserId)
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()
    val selectedMonthKey by viewModel.selectedMonthKey.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_BERANDA

    // Modal sheet states
    var showTransactionSheet by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<TransactionRecord?>(null) }

    var showSavingsGoalSheet by remember { mutableStateOf(false) }
    var editingSavingsGoal by remember { mutableStateOf<SavingsGoal?>(null) }

    var movementTargetGoal by remember { mutableStateOf<SavingsGoal?>(null) }
    var movementInitialAction by remember { mutableStateOf(SavingsActionType.DEPOSIT) }

    var showBudgetSheet by remember { mutableStateOf(false) }
    var budgetInitialCategory by remember { mutableStateOf<String?>(null) }
    var budgetInitialLimit by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val navigateToRoute: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        kasKuDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToRoute(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes), maxLines = 1) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                if (currentRoute != ROUTE_RINGKASAN && uiState is UiState.Success) {
                    FloatingActionButton(
                        onClick = {
                            if (currentRoute == ROUTE_TABUNGAN) {
                                editingSavingsGoal = null
                                showSavingsGoalSheet = true
                            } else {
                                editingTransaction = null
                                showTransactionSheet = true
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("main_fab")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = if (currentRoute == ROUTE_TABUNGAN) {
                                stringResource(R.string.cd_add_savings_goal)
                            } else {
                                stringResource(R.string.cd_add_transaction)
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxHeight()
                            .statusBarsPadding()
                    ) {
                        Spacer(modifier = Modifier.padding(top = 8.dp))
                        Icon(
                            imageVector = Icons.Filled.AccountBalanceWallet,
                            contentDescription = stringResource(R.string.app_name),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.padding(top = 16.dp))
                        kasKuDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToRoute(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                Box(
                    contentAlignment = Alignment.TopCenter,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 720.dp)
                    ) {
                        when (val state = uiState) {
                            is UiState.Loading -> {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator()
                                    Spacer(modifier = Modifier.padding(top = 12.dp))
                                    Text(
                                        text = "Memuat catatan keuangan...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            is UiState.Error -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.errorContainer,
                                        shape = MaterialTheme.shapes.large,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(20.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "Tidak dapat memuat data",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                            Text(
                                                text = state.message,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                    }
                                }
                            }

                            is UiState.Success -> {
                                val data = state.data
                                NavHost(
                                    navController = navController,
                                    startDestination = ROUTE_BERANDA
                                ) {
                                    composable(ROUTE_BERANDA) {
                                        BerandaScreen(
                                            data = data,
                                            userName = userName,
                                            onPreviousMonth = viewModel::previousMonth,
                                            onNextMonth = viewModel::nextMonth,
                                            onAddExpenseClick = {
                                                editingTransaction = null
                                                showTransactionSheet = true
                                            },
                                            onAddSavingsClick = {
                                                editingSavingsGoal = null
                                                showSavingsGoalSheet = true
                                            },
                                            onDepositToGoal = { goal ->
                                                movementTargetGoal = goal
                                                movementInitialAction = SavingsActionType.DEPOSIT
                                            },
                                            onEditTransaction = { tx ->
                                                editingTransaction = tx
                                                showTransactionSheet = true
                                            },
                                            onOpenExpensesTab = { navigateToRoute(ROUTE_PENGELUARAN) },
                                            onOpenSavingsTab = { navigateToRoute(ROUTE_TABUNGAN) }
                                        )
                                    }

                                    composable(ROUTE_PENGELUARAN) {
                                        PengeluaranScreen(
                                            data = data,
                                            onPreviousMonth = viewModel::previousMonth,
                                            onNextMonth = viewModel::nextMonth,
                                            onEditTransaction = { tx ->
                                                editingTransaction = tx
                                                showTransactionSheet = true
                                            },
                                            onDeleteTransaction = viewModel::deleteTransaction,
                                            onOpenSetBudget = { category, limit ->
                                                budgetInitialCategory = category
                                                budgetInitialLimit = limit
                                                showBudgetSheet = true
                                            },
                                            onDeleteBudget = viewModel::deleteBudget
                                        )
                                    }

                                    composable(ROUTE_TABUNGAN) {
                                        TabunganScreen(
                                            data = data,
                                            onCreateGoal = {
                                                editingSavingsGoal = null
                                                showSavingsGoalSheet = true
                                            },
                                            onEditGoal = { goal ->
                                                editingSavingsGoal = goal
                                                showSavingsGoalSheet = true
                                            },
                                            onDeleteGoal = viewModel::deleteSavingsGoal,
                                            onDeposit = { goal ->
                                                movementTargetGoal = goal
                                                movementInitialAction = SavingsActionType.DEPOSIT
                                            },
                                            onWithdraw = { goal ->
                                                movementTargetGoal = goal
                                                movementInitialAction = SavingsActionType.WITHDRAW
                                            }
                                        )
                                    }

                                    composable(ROUTE_RINGKASAN) {
                                        RingkasanScreen(
                                            data = data,
                                            userName = userName,
                                            userEmail = userEmail,
                                            onPreviousMonth = viewModel::previousMonth,
                                            onNextMonth = viewModel::nextMonth,
                                            onSignOutClick = onSignOut
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

    if (showTransactionSheet) {
        val currentTx = editingTransaction
        AddEditTransactionSheet(
            existing = currentTx,
            isSubmitting = isSubmitting,
            onDismiss = { showTransactionSheet = false },
            onSave = { title, amount, type, category, note, occurredAt ->
                if (currentTx == null) {
                    viewModel.addTransaction(title, amount, type, category, note, occurredAt) {
                        showTransactionSheet = false
                    }
                } else {
                    viewModel.updateTransaction(currentTx.id, title, amount, type, category, note, occurredAt) {
                        showTransactionSheet = false
                    }
                }
            }
        )
    }

    if (showSavingsGoalSheet) {
        val currentGoal = editingSavingsGoal
        AddEditSavingsGoalSheet(
            existing = currentGoal,
            isSubmitting = isSubmitting,
            onDismiss = { showSavingsGoalSheet = false },
            onSave = { name, target, initial, iconKey, colorHex ->
                if (currentGoal == null) {
                    viewModel.addSavingsGoal(name, target, initial, iconKey, colorHex) {
                        showSavingsGoalSheet = false
                    }
                } else {
                    viewModel.updateSavingsGoal(currentGoal.id, name, target, iconKey, colorHex) {
                        showSavingsGoalSheet = false
                    }
                }
            }
        )
    }

    movementTargetGoal?.let { goal ->
        SavingsMovementSheet(
            goal = goal,
            initialAction = movementInitialAction,
            isSubmitting = isSubmitting,
            onDismiss = { movementTargetGoal = null },
            onConfirm = { amount, actionType, note ->
                viewModel.recordSavingsMovement(goal, amount, actionType, note) {
                    movementTargetGoal = null
                }
            }
        )
    }

    if (showBudgetSheet) {
        SetBudgetSheet(
            monthKey = selectedMonthKey,
            initialCategory = budgetInitialCategory,
            initialLimit = budgetInitialLimit,
            isSubmitting = isSubmitting,
            onDismiss = { showBudgetSheet = false },
            onSave = { category, limit ->
                viewModel.upsertBudget(category, limit) {
                    showBudgetSheet = false
                }
            }
        )
    }
}
