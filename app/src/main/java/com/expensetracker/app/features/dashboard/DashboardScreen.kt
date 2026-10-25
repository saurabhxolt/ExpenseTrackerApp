package com.expensetracker.app.features.dashboard

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import com.expensetracker.app.core.security.LocalPrivacyMode
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.expensetracker.app.core.database.entity.TransactionEntity
import com.expensetracker.app.core.ui.theme.DarkCard
import com.expensetracker.app.core.ui.theme.GreenSuccess
import com.expensetracker.app.core.ui.theme.PrimaryBlue
import com.expensetracker.app.core.ui.theme.RedExpense
import com.expensetracker.app.core.ui.theme.TextPrimary
import com.expensetracker.app.core.ui.theme.TextSecondary
import com.expensetracker.app.core.database.entity.RecurringIncomeEntity
import com.expensetracker.app.core.database.entity.InsurancePolicyEntity
import com.expensetracker.app.core.analysis.SalaryProvisioningPlan
import com.expensetracker.app.core.analysis.PolicyProvisioningItem
import com.expensetracker.app.features.subscriptions.MarkIncomeReceivedDialog
import com.expensetracker.app.features.subscriptions.SalaryStashDialog
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DashboardRoute(
    viewModel: DashboardViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val registeredCards by viewModel.registeredCards.collectAsState()
    val savingsGoals by viewModel.savingsGoals.collectAsState()
    val recurringIncomes by viewModel.recurringIncomes.collectAsState()
    val insurancePolicies by viewModel.insurancePolicies.collectAsState()
    val salaryProvisioningPlan by viewModel.salaryProvisioningPlan.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showQuickCashLog by remember { mutableStateOf(false) }
    var showReceiptScanner by remember { mutableStateOf(false) }
    var selectedTransactionDetails by remember { mutableStateOf<TransactionEntity?>(null) }
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var incomeToMarkReceived by remember { mutableStateOf<RecurringIncomeEntity?>(null) }
    var policyToStash by remember { mutableStateOf<PolicyProvisioningItem?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        viewModel.refreshPermissionState(context)
        val isSmsGranted = permissionsMap[Manifest.permission.READ_SMS] == true ||
                           com.expensetracker.app.core.utils.PermissionUtils.isSmsPermissionGranted(context)
        if (isSmsGranted) {
            viewModel.triggerSmsInboxScan(context)
        }
    }

    val requestPermissionsAndScan = {
        val perms = mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
        smsPermissionLauncher.launch(perms)

        if (!com.expensetracker.app.core.utils.PermissionUtils.isNotificationListenerGranted(context)) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshPermissionState(context)
        if (!com.expensetracker.app.core.utils.PermissionUtils.isSmsPermissionGranted(context)) {
            val perms = mutableListOf(
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS
            ).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }.toTypedArray()
            smsPermissionLauncher.launch(perms)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissionState(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = PrimaryBlue,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Transaction")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            DashboardScreen(
                uiState = uiState,
                savingsGoals = savingsGoals,
                recurringIncomes = recurringIncomes,
                insurancePolicies = insurancePolicies,
                salaryProvisioningPlan = salaryProvisioningPlan,
                onPeriodFilterSelected = { viewModel.setPeriodFilter(it) },
                onMonthYearSelected = { viewModel.setMonthYearFilter(it) },
                onSortOrderSelected = { viewModel.setSortOrder(it) },
                onTransactionClick = { selectedTransactionDetails = it },
                onEditTransaction = { editingTransaction = it },
                onDeleteTransaction = { trx ->
                    viewModel.deleteTransaction(trx)
                    scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = "Transaction deleted",
                            actionLabel = "Undo",
                            duration = SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.restoreLastDeletedTransaction()
                        }
                    }
                },
                onScanInbox = { viewModel.triggerSmsInboxScan(context) },
                onRequestPermissions = { requestPermissionsAndScan() },
                onQuickCashClick = { showQuickCashLog = true },
                onScanReceiptClick = { showReceiptScanner = true },
                onMarkIncomeReceivedClick = { incomeToMarkReceived = it },
                onStashPolicyClick = { policyToStash = it }
            )

            incomeToMarkReceived?.let { income ->
                MarkIncomeReceivedDialog(
                    income = income,
                    onDismiss = { incomeToMarkReceived = null },
                    onConfirm = { recordInLedger ->
                        viewModel.markIncomeReceived(income, recordInLedger)
                        incomeToMarkReceived = null
                    }
                )
            }

            policyToStash?.let { item ->
                SalaryStashDialog(
                    item = item,
                    onDismiss = { policyToStash = null },
                    onConfirm = { jarId, amount, recordInLedger ->
                        viewModel.stashSalaryToJar(jarId, amount, recordInLedger)
                        policyToStash = null
                    }
                )
            }

            if (showReceiptScanner) {
                com.expensetracker.app.features.ocr.ReceiptScannerDialog(
                    ocrParser = viewModel.receiptOcrParser,
                    registeredCards = registeredCards,
                    onDismiss = { showReceiptScanner = false },
                    onSaveTransaction = { amount, merchant, category, timestamp, cardLast4 ->
                        viewModel.addTransaction(
                            merchant = merchant,
                            amount = amount,
                            type = "DEBIT",
                            category = category,
                            timestamp = timestamp,
                            accountDigits = cardLast4 ?: ""
                        )
                        scope.launch {
                            snackbarHostState.showSnackbar("Saved ₹${amount.toInt()} from receipt! 🧾")
                        }
                    }
                )
            }

            if (showQuickCashLog) {
                com.expensetracker.app.features.quicklog.QuickCashLogDialog(
                    onDismiss = { showQuickCashLog = false },
                    onSaveCashSpend = { amount, category, merchant, note ->
                        viewModel.saveCashSpend(amount, category, merchant, note)
                        scope.launch {
                            snackbarHostState.showSnackbar("Cash spend ₹${amount.toInt()} logged! 💵")
                        }
                    }
                )
            }

            if (showAddDialog) {
                AddTransactionDialog(
                    availableCategories = uiState.categories,
                    registeredCards = registeredCards,
                    onDismiss = { showAddDialog = false },
                    onConfirm = { merchant, amount, type, category, timestamp, accountDigits ->
                        viewModel.addTransaction(
                            merchant = merchant,
                            amount = amount,
                            type = type,
                            category = category,
                            timestamp = timestamp,
                            accountDigits = accountDigits
                        )
                        showAddDialog = false
                    }
                )
            }

            selectedTransactionDetails?.let { trx ->
                TransactionDetailsDialog(
                    transaction = trx,
                    allTransactions = uiState.transactions,
                    onDismiss = { selectedTransactionDetails = null },
                    onEdit = {
                        editingTransaction = trx
                        selectedTransactionDetails = null
                    },
                    onDelete = {
                        viewModel.deleteTransaction(trx)
                        selectedTransactionDetails = null
                        scope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = "Transaction deleted",
                                actionLabel = "Undo",
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                viewModel.restoreLastDeletedTransaction()
                            }
                        }
                    },
                    onOpenSmsApp = {
                        openMessagesApp(context)
                    }
                )
            }

            editingTransaction?.let { trx ->
                EditTransactionDialog(
                    transaction = trx,
                    availableCategories = uiState.categories,
                    onDismiss = { editingTransaction = null },
                    onConfirm = { merchant, category, amount, type ->
                        viewModel.updateTransaction(trx, merchant, category, amount, type)
                        editingTransaction = null
                    }
                )
            }
        }
    }
}

@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    savingsGoals: List<com.expensetracker.app.core.database.entity.SavingsGoalEntity> = emptyList(),
    recurringIncomes: List<RecurringIncomeEntity> = emptyList(),
    insurancePolicies: List<InsurancePolicyEntity> = emptyList(),
    salaryProvisioningPlan: SalaryProvisioningPlan? = null,
    onPeriodFilterSelected: (PeriodFilter) -> Unit = {},
    onMonthYearSelected: (String) -> Unit = {},
    onSortOrderSelected: (String) -> Unit = {},
    onTransactionClick: (TransactionEntity) -> Unit = {},
    onEditTransaction: (TransactionEntity) -> Unit = {},
    onDeleteTransaction: (TransactionEntity) -> Unit = {},
    onScanInbox: () -> Unit = {},
    onRequestPermissions: () -> Unit = {},
    onQuickCashClick: () -> Unit = {},
    onScanReceiptClick: () -> Unit = {},
    onMarkIncomeReceivedClick: (RecurringIncomeEntity) -> Unit = {},
    onStashPolicyClick: (PolicyProvisioningItem) -> Unit = {}
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    val dateFormat = remember { SimpleDateFormat("EEEE, dd MMM", Locale.getDefault()) }
                    val todayFormatted = remember { dateFormat.format(Date()) }
                    Text(
                        text = todayFormatted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Dashboard",
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = onQuickCashClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981).copy(alpha = 0.15f),
                            contentColor = Color(0xFF10B981)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f)),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("💵 Cash", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = onScanReceiptClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryBlue.copy(alpha = 0.15f),
                            contentColor = PrimaryBlue
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.3f)),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("🧾 Scan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    if (uiState.isScanning) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = PrimaryBlue,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Scanning...", style = MaterialTheme.typography.bodyMedium)
                        }
                    } else if (uiState.isAutoTrackingEnabled) {
                        IconButton(onClick = onScanInbox) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Scan SMS Inbox",
                                tint = PrimaryBlue
                            )
                        }
                    }
                }
            }
        }

        // Unified Collapsible Filter Bar
        item {
            com.expensetracker.app.core.ui.components.UnifiedFilterBar(
                periodOption = uiState.selectedPeriodFilter.label,
                periodOptions = PeriodFilter.values().map { it.label },
                onPeriodSelected = { label ->
                    val filter = PeriodFilter.values().find { it.label == label } ?: PeriodFilter.THIS_MONTH
                    onPeriodFilterSelected(filter)
                }
            )
        }

        // Permission Banner
        item {
            AnimatedVisibility(visible = !uiState.isAutoTrackingEnabled) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onRequestPermissions() },
                    colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Enable Auto-Tracking ⚡",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Tap to grant SMS & Notification permissions to auto-scan current month & live alerts.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Dynamic Executive Financial Snapshot & Health Hero Card
        item {
            val isPositive = uiState.netCashflow >= 0
            val isDark = androidx.compose.foundation.isSystemInDarkTheme()
            val cashflowColor = if (isPositive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error

            val healthStatus = when {
                uiState.savingsRate >= 30.0 -> "Optimal 🟢"
                uiState.savingsRate > 0.0 -> "Balanced 🟡"
                else -> "High Outflow 🔴"
            }
            val healthColor = when {
                uiState.savingsRate >= 30.0 -> GreenSuccess
                uiState.savingsRate > 0.0 -> Color(0xFFF59E0B)
                else -> MaterialTheme.colorScheme.error
            }

            val gradientStart = if (isPositive) {
                if (isDark) Color(0xFF0C243B) else Color(0xFFE8F3FF)
            } else {
                if (isDark) Color(0xFF3B1523) else Color(0xFFFFECEF)
            }
            val gradientEnd = if (isDark) Color(0xFF141E28) else Color(0xFFF8FAFC)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(gradientStart, gradientEnd)
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        // Top Header Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "NET CASHFLOW",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = uiState.selectedPeriodFilter.label,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }

                            Surface(
                                color = healthColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(20.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, healthColor.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(healthColor, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = healthStatus,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = healthColor
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Large Net Cashflow Amount
                        AnimatedCurrencyText(
                            value = uiState.netCashflow,
                            prefix = if (isPositive) "+₹" else "-₹",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black
                            ),
                            color = cashflowColor
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Savings Rate & Progress Bar
                        if (uiState.totalIncome > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Savings: ${String.format(Locale.getDefault(), "%.1f", uiState.savingsRate)}%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PrimaryBlue
                                )
                                Text(
                                    text = "Avg Pace: ₹${String.format(Locale.getDefault(), "%.0f", uiState.avgDailySpend)}/day",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { (uiState.savingsRate.toFloat() / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = PrimaryBlue,
                                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Sleek Embedded 3-Pill Breakdown Ribbon
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.5f else 0.7f),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Inflow
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                    Text("Income 💰", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    AnimatedCurrencyText(
                                        value = uiState.totalIncome,
                                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .height(24.dp)
                                        .width(1.dp)
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                )

                                // Outflow
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                    Text("Expenses 💸", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    AnimatedCurrencyText(
                                        value = uiState.totalExpenses,
                                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .height(24.dp)
                                        .width(1.dp)
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                )

                                // Investments
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                    Text("Invested 📈", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    AnimatedCurrencyText(
                                        value = uiState.totalInvestments,
                                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                                        color = PrimaryBlue
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ⚡ Daily Spending Radar & Real-Time Pacing Card
        item {
            val isPrivacy = LocalPrivacyMode.current
            val radarStatus = when {
                uiState.safeToSpendToday >= 500.0 -> "On Track 🟢"
                uiState.safeToSpendToday > 0.0 -> "Pacing Tight 🟡"
                else -> "Cap Reached 🔴"
            }
            val radarColor = when {
                uiState.safeToSpendToday >= 500.0 -> GreenSuccess
                uiState.safeToSpendToday > 0.0 -> Color(0xFFF59E0B)
                else -> MaterialTheme.colorScheme.error
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(PrimaryBlue.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "⚡", fontSize = 20.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Daily Spending Radar",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${uiState.daysRemainingInMonth} days remaining in cycle",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            color = radarColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = radarStatus,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = radarColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "Safe-to-Spend Today",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            AnimatedCurrencyText(
                                value = uiState.safeToSpendToday,
                                prefix = "₹",
                                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 26.sp, fontWeight = FontWeight.ExtraBold),
                                color = if (uiState.safeToSpendToday > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (uiState.hasBudgetSet) {
                                    if (isPrivacy) "Budget: ₹•••••" else "Budget: ₹${String.format(Locale.getDefault(), "%,.0f", uiState.totalBudgetLimit)}"
                                } else {
                                    "Paced from income"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryBlue
                            )
                            Text(
                                text = if (uiState.todayExpenses > 0) "Today: ₹${String.format(Locale.getDefault(), "%.0f", uiState.todayExpenses)}" else "No spend today yet",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 4-cell matrix: Spent Today | Avg Daily Spend | Unspent Pool | Bills Reserved
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Spent Today 📅", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isPrivacy) "₹•••••" else "₹${String.format(Locale.getDefault(), "%.2f", uiState.todayExpenses)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.todayExpenses > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Avg Daily 📊", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isPrivacy) "₹•••••" else "₹${String.format(Locale.getDefault(), "%.1f", uiState.avgDailySpend)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Unspent Pool", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isPrivacy) "₹•••••" else "₹${String.format(Locale.getDefault(), "%.2f", uiState.remainingMonthlyBudget)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Bills Reserved", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isPrivacy) "₹•••••" else "₹${String.format(Locale.getDefault(), "%.2f", uiState.totalPendingBillsThisMonth)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Upcoming Mandates & Monthly Bills Commitment Card
        if (uiState.totalMonthlyCommitted > 0 || uiState.upcomingSubscriptions.isNotEmpty()) {
            item {
                val isPrivacy = LocalPrivacyMode.current
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Monthly Mandates & Subscriptions 🔄", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.weight(1f))
                            Text(
                                if (isPrivacy) "Total ₹•••••/mo" else "Total ₹${String.format(Locale.getDefault(), "%,.0f", uiState.totalMonthlyCommitted)}/mo",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isPrivacy) "Paid: ₹••••• 🟢" else "Paid: ₹${String.format(Locale.getDefault(), "%,.0f", uiState.totalPaidCommitted)} 🟢", fontSize = 12.sp, color = GreenSuccess, fontWeight = FontWeight.Medium)
                            Text(if (isPrivacy) "Pending: ₹••••• ⏳" else "Pending: ₹${String.format(Locale.getDefault(), "%,.0f", uiState.totalPendingCommitted)} ⏳", fontSize = 12.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }

        val activeIncomes = recurringIncomes.filter { it.isActive }
        val hasInflows = activeIncomes.isNotEmpty()
        val hasPolicies = insurancePolicies.isNotEmpty()
        val hasJars = savingsGoals.isNotEmpty()

        // 🎯 Financial Horizons: Shown as a single sleek action hub when nothing is set up yet
        if (!hasInflows && !hasPolicies && !hasJars) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(PrimaryBlue.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🎯", fontSize = 18.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Financial Horizons & Planning",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Automate salary day allocations, policy stashes & jars",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GreenSuccess.copy(alpha = 0.25f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("💰", fontSize = 20.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Inflows", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Text("Salary/Rent", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Surface(
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.25f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("🛡️", fontSize = 20.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Policy Vault", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Text("LIC/Mediclaim", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Surface(
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.25f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("🏺", fontSize = 20.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Savings Jars", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Text("Sinking Funds", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 💰 Scheduled Inflows & Salary Day Radar Card (Rendered only when active)
        if (hasInflows) {
            item {
                val isPrivacy = LocalPrivacyMode.current
                val totalExpectedInflow = activeIncomes.sumOf { it.amount }
                val cal = Calendar.getInstance()
                val currentMonth = cal.get(Calendar.MONTH)
                val currentYear = cal.get(Calendar.YEAR)

                val realizedThisCycle = activeIncomes.filter { income ->
                    income.lastReceivedDateMs?.let { lastMs ->
                        val c = Calendar.getInstance().apply { timeInMillis = lastMs }
                        c.get(Calendar.MONTH) == currentMonth && c.get(Calendar.YEAR) == currentYear
                    } ?: false
                }.sumOf { it.amount }

                val pendingInflow = (totalExpectedInflow - realizedThisCycle).coerceAtLeast(0.0)
                val inflowProgress = if (totalExpectedInflow > 0) (realizedThisCycle / totalExpectedInflow).toFloat().coerceIn(0f, 1f) else 0f

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GreenSuccess.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(GreenSuccess.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("💰", fontSize = 18.sp)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Scheduled Inflows & Salary",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${activeIncomes.size} stream(s) • ${(inflowProgress * 100).toInt()}% realized",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (pendingInflow == 0.0) GreenSuccess.copy(alpha = 0.15f) else PrimaryBlue.copy(alpha = 0.15f)
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = if (pendingInflow == 0.0) "All Realized 🟢" else if (isPrivacy) "₹•••••" else "Pending: ₹${String.format(Locale.getDefault(), "%,.0f", pendingInflow)}",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (pendingInflow == 0.0) GreenSuccess else PrimaryBlue
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = if (isPrivacy) "Realized: ₹•••••" else "Realized: ₹${String.format(Locale.getDefault(), "%,.0f", realizedThisCycle)} 🟢",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = GreenSuccess
                            )
                            Text(
                                text = if (isPrivacy) "Expected: ₹•••••" else "Total: ₹${String.format(Locale.getDefault(), "%,.0f", totalExpectedInflow)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { inflowProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = GreenSuccess,
                            trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(activeIncomes) { inc ->
                                val isReceived = inc.lastReceivedDateMs?.let { lastMs ->
                                    val c = Calendar.getInstance().apply { timeInMillis = lastMs }
                                    c.get(Calendar.MONTH) == currentMonth && c.get(Calendar.YEAR) == currentYear
                                } ?: false

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(if (inc.isSalary) "🏢" else "💰", fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(inc.name, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                            Text(
                                                text = if (isPrivacy) "₹•••••" else "+₹${inc.amount.toInt()} (${if (isReceived) "Realized ✓" else "Due Day ${inc.expectedDayOfMonth}"})",
                                                fontSize = 9.sp,
                                                color = if (isReceived) GreenSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (!isReceived) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = GreenSuccess.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.clickable { onMarkIncomeReceivedClick(inc) }
                                            ) {
                                                Text(
                                                    "✓ Receive",
                                                    color = GreenSuccess,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
        }

        // 🛡️ Annual Policy Vault & Salary Day Stash Card (Rendered only when active)
        if (hasPolicies) {
            item {
                val isPrivacy = LocalPrivacyMode.current
                val plan = salaryProvisioningPlan
                val totalMonthlyPace = plan?.totalRecommendedStash ?: 0.0

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryBlue.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🛡️", fontSize = 18.sp)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Annual Policies & Salary Stash",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${insurancePolicies.size} policies • ₹${totalMonthlyPace.toInt()}/mo stash pace",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (totalMonthlyPace > 0) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.15f)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = if (isPrivacy) "₹•••••/mo" else "₹${String.format(Locale.getDefault(), "%,.0f", totalMonthlyPace)}/mo",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryBlue
                                    )
                                }
                            }
                        }

                        if (plan != null && plan.items.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Salary Day Stash Allocation 💡",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(plan.items) { item ->
                                    val p = item.policy
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                when (p.policyType) {
                                                    "HEALTH" -> "🏥"
                                                    "TERM" -> "🛡️"
                                                    "CAR" -> "🚗"
                                                    "BIKE" -> "🏍️"
                                                    else -> "📜"
                                                },
                                                fontSize = 14.sp
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column {
                                                Text(p.policyName, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                                Text(
                                                    text = if (item.isFunded) {
                                                        "Fully Funded 🟢"
                                                    } else {
                                                        if (isPrivacy) "Pace: ₹•••••/mo" else "Pace: ₹${item.recommendedMonthlyStash.toInt()}/mo"
                                                    },
                                                    fontSize = 9.sp,
                                                    color = if (item.isFunded) GreenSuccess else PrimaryBlue
                                                )
                                            }

                                            if (item.linkedJar != null && !item.isFunded && item.recommendedMonthlyStash > 0) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    color = PrimaryBlue.copy(alpha = 0.15f),
                                                    shape = RoundedCornerShape(6.dp),
                                                    modifier = Modifier.clickable { onStashPolicyClick(item) }
                                                ) {
                                                    Text(
                                                        "Stash ₹${item.recommendedMonthlyStash.toInt()}",
                                                        color = PrimaryBlue,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
            }
        }

        // 🏺 Visual Sinking Funds (Interactive Savings Jars) Overview (Rendered only when active)
        if (hasJars) {
            item {
                val isPrivacy = LocalPrivacyMode.current
                val totalSaved = savingsGoals.sumOf { it.currentAmount }
                val totalTarget = savingsGoals.sumOf { it.targetAmount }
                val overallProgress = if (totalTarget > 0) (totalSaved / totalTarget).toFloat().coerceIn(0f, 1f) else 0f

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(GreenSuccess.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🏺", fontSize = 18.sp)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Savings Jars (Sinking Funds)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${savingsGoals.size} active jars • ${(overallProgress * 100).toInt()}% achieved",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Card(
                                colors = CardDefaults.cardColors(containerColor = GreenSuccess.copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = if (isPrivacy) "₹•••••" else "₹${String.format(Locale.getDefault(), "%,.0f", totalSaved)}",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GreenSuccess
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { overallProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = GreenSuccess,
                            trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(savingsGoals) { goal ->
                                val goalProgress = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(goal.emoji.ifBlank { "🎯" }, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(goal.title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                            Text(
                                                text = if (isPrivacy) "₹•••••" else "₹${goal.currentAmount.toInt()} (${(goalProgress * 100).toInt()}%)",
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
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

        // Section Title
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Transactions",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                ) {
                    Text(
                        text = "${uiState.transactions.size} records • ${uiState.selectedPeriodFilter.label}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (uiState.transactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Transactions in Last 5 Days",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        } else {
            items(uiState.transactions, key = { it.id }) { transaction ->
                TransactionItem(
                    transaction = transaction,
                    onClick = { onTransactionClick(transaction) },
                    onEdit = { onEditTransaction(transaction) },
                    onDelete = { onDeleteTransaction(transaction) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun TransactionItem(
    transaction: TransactionEntity,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val dateStr = remember(transaction.timestamp) { dateFormat.format(Date(transaction.timestamp)) }
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    val emoji = remember(transaction.category) {
        com.expensetracker.app.core.ui.theme.CategoryEmojiProvider.getCategoryEmoji(transaction.category)
    }
    val categoryColor = remember(transaction.category) {
        com.expensetracker.app.core.ui.theme.CategoryColorProvider.getCategoryColor(transaction.category)
    }
    val avatarBg = categoryColor.copy(alpha = 0.15f)

    val isCredit = transaction.type == "CREDIT"
    val isInvestment = transaction.type == "INVESTMENT" || transaction.category.equals("Investments", ignoreCase = true) || transaction.category.equals("Investment", ignoreCase = true)
    val isTransfer = transaction.type == "TRANSFER"
    
    val amountPrefix = when {
        isCredit -> "+"
        isInvestment -> ""
        isTransfer -> "↔ "
        else -> "-"
    }
    val amountColor = when {
        isCredit -> MaterialTheme.colorScheme.tertiary
        isInvestment -> PrimaryBlue
        isTransfer -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.error
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(avatarBg, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = emoji, fontSize = 20.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = transaction.merchant,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${transaction.category} • $dateStr",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                val isPrivacy = LocalPrivacyMode.current
                val displayedAmount = if (isPrivacy) "${amountPrefix}₹•••••" else "${amountPrefix}₹${String.format("%.2f", transaction.amount)}"
                Card(
                    colors = CardDefaults.cardColors(containerColor = amountColor.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = displayedAmount,
                        fontWeight = FontWeight.Bold,
                        color = amountColor,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Transaction",
                        tint = PrimaryBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Transaction",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionDetailsDialog(
    transaction: TransactionEntity,
    allTransactions: List<TransactionEntity> = emptyList(),
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenSmsApp: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMMM yyyy, hh:mm:ss a", Locale.getDefault()) }
    val fullDateStr = remember(transaction.timestamp) { dateFormat.format(Date(transaction.timestamp)) }
    val priceHike = remember(transaction, allTransactions) {
        if (allTransactions.isNotEmpty()) {
            com.expensetracker.app.core.analysis.PriceCreepDetector.detectPriceHikeForTransaction(transaction, allTransactions)
        } else null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Transaction Details",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = transaction.merchant,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = transaction.category,
                                fontSize = 14.sp,
                                color = PrimaryBlue,
                                fontWeight = FontWeight.SemiBold
                            )
                            val typeLabel = when (transaction.type) {
                                "CREDIT" -> "Income"
                                "TRANSFER" -> "Transfer / CC Bill"
                                "INVESTMENT" -> "Investment"
                                else -> "Expense"
                            }
                            val typeBg = when (transaction.type) {
                                "CREDIT" -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                                "TRANSFER" -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                "INVESTMENT" -> PrimaryBlue.copy(alpha = 0.15f)
                                else -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                            }
                            val typeColor = when (transaction.type) {
                                "CREDIT" -> MaterialTheme.colorScheme.tertiary
                                "TRANSFER" -> MaterialTheme.colorScheme.secondary
                                "INVESTMENT" -> PrimaryBlue
                                else -> MaterialTheme.colorScheme.error
                            }
                            Box(
                                modifier = Modifier
                                    .background(typeBg, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(text = typeLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = typeColor)
                            }
                        }
                    }
                    val amountPrefix = when (transaction.type) {
                        "CREDIT" -> "+"
                        "TRANSFER" -> "↔ "
                        "INVESTMENT" -> ""
                        else -> "-"
                    }
                    val amountColor = when (transaction.type) {
                        "CREDIT" -> MaterialTheme.colorScheme.tertiary
                        "TRANSFER" -> MaterialTheme.colorScheme.secondary
                        "INVESTMENT" -> PrimaryBlue
                        else -> MaterialTheme.colorScheme.error
                    }
                    Text(
                        text = "$amountPrefix₹${String.format("%.2f", transaction.amount)}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = amountColor,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Date & Time: $fullDateStr",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                priceHike?.let { hike ->
                    val isPrivacy = LocalPrivacyMode.current
                    androidx.compose.material3.Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📈", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            val hikeMsg = if (isPrivacy) {
                                "Price Hike Detected: Amount is higher (+${hike.percentageIncrease}%) than prior payments."
                            } else {
                                "Price Hike Detected: +₹${String.format("%.0f", hike.difference)} (+${hike.percentageIncrease}%) vs previous payment of ₹${String.format("%.0f", hike.previousAmount)}."
                            }
                            Text(
                                text = hikeMsg,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                // 🏛️ Tax Deductible Section Indicator
                val detectedSection = remember(transaction) {
                    transaction.taxDeductibleSection ?: com.expensetracker.app.core.analysis.TaxDeductionAnalyzer.inferSection(transaction.merchant, transaction.category, transaction.rawText)
                }
                detectedSection?.let { section ->
                    androidx.compose.material3.Surface(
                        color = PrimaryBlue.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🏛️", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Tax Exempt: ${com.expensetracker.app.core.analysis.TaxDeductionAnalyzer.getSectionDisplayName(section)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue
                            )
                        }
                    }
                }

                if (transaction.rawText.isNotBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Original SMS / Raw Statement Text:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryBlue
                                )
                                if (!transaction.isManual) {
                                    Text(
                                        text = "Open App ✉️",
                                        fontSize = 11.sp,
                                        color = PrimaryBlue,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable { onOpenSmsApp() }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = transaction.rawText,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                if (transaction.transactionHash.isNotBlank()) {
                    Text(
                        text = "Ref / Hash: ${transaction.transactionHash}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        onDelete()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete 🗑️")
                }
                Button(
                    onClick = onEdit,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkCard)
                ) {
                    Text("Edit ✏️")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

fun openMessagesApp(context: Context) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("sms:")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open Messages app", Toast.LENGTH_SHORT).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun AddTransactionDialog(
    availableCategories: List<String>,
    registeredCards: List<com.expensetracker.app.core.database.entity.CreditCardEntity> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (merchant: String, amount: Double, type: String, category: String, timestamp: Long, accountDigits: String) -> Unit
) {
    var merchant by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("DEBIT") }
    var selectedCategory by remember { mutableStateOf(availableCategories.firstOrNull() ?: "Food & Dining") }
    var selectedTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }
    var selectedCardLast4 by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedTimestamp)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { ms ->
                            selectedTimestamp = ms
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK", fontWeight = FontWeight.Bold, color = PrimaryBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Add Transaction", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(
                        onClick = { selectedType = "DEBIT" },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedType == "DEBIT") RedExpense else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selectedType == "DEBIT") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("Expense", fontSize = 11.sp)
                    }
                    Button(
                        onClick = {
                            selectedType = "CREDIT"
                            selectedCategory = "Income"
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedType == "CREDIT") GreenSuccess else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selectedType == "CREDIT") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("Income", fontSize = 11.sp)
                    }
                    Button(
                        onClick = {
                            selectedType = "INVESTMENT"
                            selectedCategory = "Investments"
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedType == "INVESTMENT") PrimaryBlue else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selectedType == "INVESTMENT") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("Investment", fontSize = 11.sp)
                    }
                }

                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("Merchant / Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dateFormat.format(Date(selectedTimestamp)),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Transaction Date 📅") },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(imageVector = Icons.Default.CalendarToday, contentDescription = "Pick Date", tint = PrimaryBlue)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                )

                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        availableCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    selectedCategory = cat
                                    if (cat.equals("Investments", ignoreCase = true) || cat.equals("Investment", ignoreCase = true)) {
                                        selectedType = "INVESTMENT"
                                    } else if (cat.equals("Income", ignoreCase = true)) {
                                        selectedType = "CREDIT"
                                    }
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                // Payment Method / Card Selector
                if (registeredCards.isNotEmpty() && selectedType == "DEBIT") {
                    Text(
                        text = "Payment Method / Linked Card",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    androidx.compose.foundation.layout.FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedCardLast4 == null,
                            onClick = { selectedCardLast4 = null },
                            label = { Text("Cash / Account", fontSize = 11.sp) }
                        )
                        registeredCards.forEach { card ->
                            val isSelected = selectedCardLast4 == card.last4Digits
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCardLast4 = if (isSelected) null else card.last4Digits },
                                label = { Text("${card.bankName} •••• ${card.last4Digits}", fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    val finalType = if (selectedCategory.equals("Investments", ignoreCase = true) || selectedCategory.equals("Investment", ignoreCase = true)) "INVESTMENT" else selectedType
                    if (merchant.isNotBlank() && amount > 0) {
                        onConfirm(merchant, amount, finalType, selectedCategory, selectedTimestamp, selectedCardLast4 ?: "")
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionDialog(
    transaction: TransactionEntity,
    availableCategories: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (merchant: String, category: String, amount: Double, type: String) -> Unit
) {
    var merchant by remember { mutableStateOf(transaction.merchant) }
    var amountText by remember { mutableStateOf(transaction.amount.toString()) }
    var selectedType by remember { mutableStateOf(transaction.type) }
    var selectedCategory by remember { mutableStateOf(transaction.category) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Edit Transaction & Type", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Re-Assign Transaction Type (Expense vs Income vs Investment)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(
                        onClick = {
                            selectedType = "DEBIT"
                            if (selectedCategory == "Income" || selectedCategory == "Investments") selectedCategory = "General Expense"
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedType == "DEBIT") RedExpense else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selectedType == "DEBIT") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("Expense", fontSize = 11.sp, maxLines = 1, softWrap = false)
                    }
                    Button(
                        onClick = {
                            selectedType = "CREDIT"
                            selectedCategory = "Income"
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedType == "CREDIT") GreenSuccess else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selectedType == "CREDIT") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("Income", fontSize = 11.sp, maxLines = 1, softWrap = false)
                    }
                    Button(
                        onClick = {
                            selectedType = "INVESTMENT"
                            selectedCategory = "Investments"
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedType == "INVESTMENT") PrimaryBlue else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selectedType == "INVESTMENT") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("Investment", fontSize = 11.sp, maxLines = 1, softWrap = false)
                    }
                    Button(
                        onClick = {
                            selectedType = "TRANSFER"
                            selectedCategory = "Bills & Utilities"
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedType == "TRANSFER") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selectedType == "TRANSFER") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("Transfer", fontSize = 11.sp, maxLines = 1, softWrap = false)
                    }
                }

                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("Merchant / Title Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        availableCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    selectedCategory = cat
                                    if (cat.equals("Investments", ignoreCase = true) || cat.equals("Investment", ignoreCase = true)) {
                                        selectedType = "INVESTMENT"
                                    } else if (cat.equals("Income", ignoreCase = true)) {
                                        selectedType = "CREDIT"
                                    }
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: transaction.amount
                    val finalType = if (selectedCategory.equals("Investments", ignoreCase = true) || selectedCategory.equals("Investment", ignoreCase = true)) "INVESTMENT" else selectedType
                    if (merchant.isNotBlank() && amount > 0) {
                        onConfirm(merchant, selectedCategory, amount, finalType)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AnimatedCurrencyText(
    value: Double,
    prefix: String = "₹",
    style: androidx.compose.ui.text.TextStyle,
    color: Color
) {
    val isPrivacy = LocalPrivacyMode.current
    if (isPrivacy) {
        Text(
            text = "${prefix}•••••",
            style = style,
            color = color
        )
        return
    }
    val animatedValue by androidx.compose.animation.core.animateFloatAsState(
        targetValue = value.toFloat(),
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 550, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "currencyTickerAnim"
    )
    val formatted = if (animatedValue >= 0) "$prefix${String.format("%.2f", animatedValue)}" else "-₹${String.format("%.2f", -animatedValue)}"
    Text(
        text = formatted,
        style = style,
        color = color
    )
}
