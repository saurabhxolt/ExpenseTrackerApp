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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardRoute(
    viewModel: DashboardViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedTransactionDetails by remember { mutableStateOf<TransactionEntity?>(null) }
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }

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
                onRequestPermissions = { requestPermissionsAndScan() }
            )

            if (showAddDialog) {
                AddTransactionDialog(
                    availableCategories = uiState.categories,
                    onDismiss = { showAddDialog = false },
                    onConfirm = { merchant, amount, type, category, timestamp ->
                        viewModel.addTransaction(merchant, amount, type, category, timestamp = timestamp)
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
    onPeriodFilterSelected: (PeriodFilter) -> Unit = {},
    onMonthYearSelected: (String) -> Unit = {},
    onSortOrderSelected: (String) -> Unit = {},
    onTransactionClick: (TransactionEntity) -> Unit = {},
    onEditTransaction: (TransactionEntity) -> Unit = {},
    onDeleteTransaction: (TransactionEntity) -> Unit = {},
    onScanInbox: () -> Unit = {},
    onRequestPermissions: () -> Unit = {}
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
                Text(
                    text = "Dashboard",
                    style = MaterialTheme.typography.headlineLarge
                )

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
                uiState.savingsRate >= 30.0 -> "Optimal Savings 🟢"
                uiState.savingsRate > 0.0 -> "Balanced Flow 🟡"
                else -> "High Outflow 🔴"
            }
            val healthColor = when {
                uiState.savingsRate >= 30.0 -> GreenSuccess
                uiState.savingsRate > 0.0 -> Color(0xFFF59E0B)
                else -> MaterialTheme.colorScheme.error
            }

            val gradientStart = if (isPositive) {
                if (isDark) Color(0xFF0F2942) else Color(0xFFEFF6FF)
            } else {
                if (isDark) Color(0xFF3B1E2B) else Color(0xFFFFF1F2)
            }
            val gradientEnd = MaterialTheme.colorScheme.surfaceVariant

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                colors = listOf(gradientStart, gradientEnd)
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Executive Financial Summary",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = uiState.selectedPeriodFilter.label,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Card(
                                colors = CardDefaults.cardColors(containerColor = healthColor.copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = healthStatus,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = healthColor
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
                                    text = "Net Cashflow",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                                AnimatedCurrencyText(
                                    value = uiState.netCashflow,
                                    prefix = if (isPositive) "+" else "",
                                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 28.sp, fontWeight = FontWeight.ExtraBold),
                                    color = cashflowColor
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Avg Daily: ₹${String.format("%.1f", uiState.avgDailySpend)}/day",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Savings: ${String.format("%.1f", uiState.savingsRate)}%",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryBlue
                                )
                            }
                        }

                        if (uiState.totalIncome > 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress = { (uiState.savingsRate.toFloat() / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp),
                                color = PrimaryBlue,
                                trackColor = PrimaryBlue.copy(alpha = 0.15f),
                                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        }
                    }
                }
            }
        }

        // Financial Breakdown: Incomes vs Expenses vs Investments Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "Income 💰", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        AnimatedCurrencyText(
                            value = uiState.totalIncome,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "Expenses 💸", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        AnimatedCurrencyText(
                            value = uiState.totalExpenses,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "Investments 📈", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        AnimatedCurrencyText(
                            value = uiState.totalInvestments,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                            color = PrimaryBlue
                        )
                    }
                }
            }
        }

        // ⚡ Safe-to-Spend Today Radar Card
        item {
            val isPrivacy = LocalPrivacyMode.current
            val radarStatus = when {
                uiState.safeToSpendToday >= 500.0 -> "On Track 🟢"
                uiState.safeToSpendToday > 0.0 -> "Pacing Tight 🟡"
                else -> "Cap Reached 🔴"
            }
            val radarColor = when {
                uiState.safeToSpendToday >= 500.0 -> GreenSuccess
                uiState.safeToSpendToday > 0.0 -> androidx.compose.ui.graphics.Color(0xFFF59E0B)
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
                                    text = "Safe-to-Spend Today",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Daily discretionary pacing",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = radarColor.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(10.dp)
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
                                text = "Daily Allowance",
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
                                text = "${uiState.daysRemainingInMonth} days left in cycle",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryBlue
                            )
                            Text(
                                text = if (uiState.hasBudgetSet) {
                                    if (isPrivacy) "Budget: ₹•••••" else "Budget: ₹${String.format("%.0f", uiState.totalBudgetLimit)}"
                                } else {
                                    "Paced from income"
                                },
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Unspent Pool", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isPrivacy) "₹•••••" else "₹${String.format("%.2f", uiState.remainingMonthlyBudget)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Bills Reserved", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isPrivacy) "₹•••••" else "₹${String.format("%.2f", uiState.totalPendingBillsThisMonth)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }

                    if (!uiState.hasBudgetSet) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "💡 Tip: Set monthly budgets in Budgets tab for a custom daily spending cap.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
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
                                if (isPrivacy) "Total ₹•••••/mo" else "Total ₹${String.format("%.0f", uiState.totalMonthlyCommitted)}/mo",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isPrivacy) "Paid: ₹••••• 🟢" else "Paid: ₹${String.format("%.0f", uiState.totalPaidCommitted)} 🟢", fontSize = 12.sp, color = GreenSuccess, fontWeight = FontWeight.Medium)
                            Text(if (isPrivacy) "Pending: ₹••••• ⏳" else "Pending: ₹${String.format("%.0f", uiState.totalPendingCommitted)} ⏳", fontSize = 12.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }

        // Quick Stats: Today's Spend & Avg Daily Spend
        item {
            val isPrivacy = LocalPrivacyMode.current
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(text = "Today's Spend 📅", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = PrimaryBlue)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isPrivacy) "₹•••••" else "₹${String.format("%.2f", uiState.todayExpenses)}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(text = "Avg Daily Spend 📊", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = PrimaryBlue)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isPrivacy) "₹•••••" else "₹${String.format("%.2f", uiState.avgDailySpend)}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Section Title
        item {
            Text(
                text = "Transactions (${uiState.selectedPeriodFilter.label})",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 4.dp)
            )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    availableCategories: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (merchant: String, amount: Double, type: String, category: String, timestamp: Long) -> Unit
) {
    var merchant by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("DEBIT") }
    var selectedCategory by remember { mutableStateOf(availableCategories.firstOrNull() ?: "Food & Dining") }
    var selectedTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    val finalType = if (selectedCategory.equals("Investments", ignoreCase = true) || selectedCategory.equals("Investment", ignoreCase = true)) "INVESTMENT" else selectedType
                    if (merchant.isNotBlank() && amount > 0) {
                        onConfirm(merchant, amount, finalType, selectedCategory, selectedTimestamp)
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
