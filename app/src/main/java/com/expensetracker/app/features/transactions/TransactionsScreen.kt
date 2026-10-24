package com.expensetracker.app.features.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensetracker.app.core.database.entity.TransactionEntity
import com.expensetracker.app.core.ui.theme.DarkCard
import com.expensetracker.app.core.ui.theme.GreenSuccess
import com.expensetracker.app.core.ui.theme.PrimaryBlue
import com.expensetracker.app.core.ui.theme.RedExpense
import com.expensetracker.app.core.ui.theme.TextSecondary
import com.expensetracker.app.features.dashboard.EditTransactionDialog
import com.expensetracker.app.features.dashboard.TransactionDetailsDialog
import com.expensetracker.app.features.dashboard.openMessagesApp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionsRoute(
    viewModel: TransactionsViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTransactionDetails by remember { mutableStateOf<TransactionEntity?>(null) }
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var showCustomDatePicker by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            TransactionsScreen(
                uiState = uiState,
                onFilterSelected = { viewModel.setFilter(it) },
                onCategoryFilterSelected = { viewModel.setCategoryFilter(it) },
                onBankFilterSelected = { viewModel.setBankFilter(it) },
                onMonthYearSelected = { viewModel.setMonthYearFilter(it) },
                onPeriodPresetSelected = { preset, startMs, endMs -> viewModel.setPeriodPreset(preset, startMs, endMs) },
                onOpenCustomDatePicker = { showCustomDatePicker = true },
                onSortOrderSelected = { viewModel.setSortOrder(it) },
                onSearchQueryChange = { viewModel.setSearchQuery(it) },
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
                }
            )

            if (showCustomDatePicker) {
                CustomDateRangePickerDialog(
                    onDismiss = { showCustomDatePicker = false },
                    onConfirm = { startMs, endMs ->
                        viewModel.setPeriodPreset("CUSTOM", startMs, endMs)
                        showCustomDatePicker = false
                    }
                )
            }

            selectedTransactionDetails?.let { trx ->
                TransactionDetailsDialog(
                    transaction = trx,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    uiState: TransactionsUiState,
    onFilterSelected: (String) -> Unit = {},
    onCategoryFilterSelected: (String) -> Unit = {},
    onBankFilterSelected: (String) -> Unit = {},
    onMonthYearSelected: (String) -> Unit = {},
    onPeriodPresetSelected: (preset: String, startMs: Long?, endMs: Long?) -> Unit = { _, _, _ -> },
    onOpenCustomDatePicker: () -> Unit = {},
    onSortOrderSelected: (String) -> Unit = {},
    onSearchQueryChange: (String) -> Unit = {},
    onTransactionClick: (TransactionEntity) -> Unit = {},
    onEditTransaction: (TransactionEntity) -> Unit = {},
    onDeleteTransaction: (TransactionEntity) -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Transaction History",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search by merchant, bank or category...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Collapsible Unified Filter Bar (Period, Bank, Category, Type)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                com.expensetracker.app.core.ui.components.UnifiedFilterBar(
                    periodOption = when (uiState.selectedPeriodPreset) {
                        "THIS_MONTH" -> "This Month"
                        "LAST_MONTH" -> "Last Month"
                        "LAST_3_MONTHS" -> "Last 3 Months"
                        "LAST_6_MONTHS" -> "Last 6 Months"
                        "YEARLY" -> "This Year"
                        "CUSTOM" -> "Custom Range"
                        else -> "All Time"
                    },
                    periodOptions = listOf("All Time", "This Month", "Last Month", "Last 3 Months", "Last 6 Months", "This Year"),
                    onPeriodSelected = { label ->
                        val preset = when (label) {
                            "This Month" -> "THIS_MONTH"
                            "Last Month" -> "LAST_MONTH"
                            "Last 3 Months" -> "LAST_3_MONTHS"
                            "Last 6 Months" -> "LAST_6_MONTHS"
                            "This Year" -> "YEARLY"
                            else -> "ALL"
                        }
                        onPeriodPresetSelected(preset, null, null)
                    },
                    bankOption = uiState.selectedBankFilter,
                    bankOptions = uiState.bankOptions,
                    onBankSelected = onBankFilterSelected,
                    categoryOption = uiState.selectedCategoryFilter,
                    categoryOptions = uiState.categories,
                    onCategorySelected = onCategoryFilterSelected,
                    typeOption = uiState.selectedFilter,
                    typeOptions = listOf("ALL", "DEBIT", "CREDIT", "INVESTMENT", "TRANSFER"),
                    onTypeSelected = onFilterSelected,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                var sortExpanded by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { sortExpanded = true }) {
                        Icon(imageVector = Icons.Default.Sort, contentDescription = "Sort", tint = PrimaryBlue)
                    }
                    DropdownMenu(
                        expanded = sortExpanded,
                        onDismissRequest = { sortExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Newest First", color = MaterialTheme.colorScheme.onSurface) },
                            onClick = { onSortOrderSelected("NEWEST"); sortExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Oldest First", color = MaterialTheme.colorScheme.onSurface) },
                            onClick = { onSortOrderSelected("OLDEST"); sortExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Amount: High to Low", color = MaterialTheme.colorScheme.onSurface) },
                            onClick = { onSortOrderSelected("HIGH_LOW"); sortExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Amount: Low to High", color = MaterialTheme.colorScheme.onSurface) },
                            onClick = { onSortOrderSelected("LOW_HIGH"); sortExpanded = false }
                        )
                    }
                }
            }
        }

        if (uiState.transactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
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
                            tint = TextSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Matching Transactions",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        } else {
            items(uiState.transactions, key = { it.id }) { transaction ->
                TransactionHistoryItem(
                    transaction = transaction,
                    onClick = { onTransactionClick(transaction) },
                    onEdit = { onEditTransaction(transaction) },
                    onDelete = { onDeleteTransaction(transaction) }
                )
            }
        }
    }
}

@Composable
fun FilterButton(label: String, value: String, selectedValue: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val isSelected = value == selectedValue
    Button(
        onClick = onClick,
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 4.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Text(text = label, fontSize = 10.sp, softWrap = false)
    }
}

@Composable
fun TransactionHistoryItem(
    transaction: TransactionEntity,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
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
                        .background(avatarBg, androidx.compose.foundation.shape.CircleShape),
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
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
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
                Card(
                    colors = CardDefaults.cardColors(containerColor = amountColor.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${amountPrefix}₹${String.format("%.2f", transaction.amount)}",
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
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDateRangePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (startMs: Long, endMs: Long) -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
    var selectedStartMs by remember { mutableStateOf<Long?>(null) }

    val startPickerState = rememberDatePickerState()
    val endPickerState = rememberDatePickerState()

    if (step == 1) {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = {
                        val selected = startPickerState.selectedDateMillis
                        if (selected != null) {
                            selectedStartMs = selected
                            step = 2
                        }
                    }
                ) {
                    Text("Next: Pick End Date", fontWeight = FontWeight.Bold, color = PrimaryBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        ) {
            DatePicker(
                state = startPickerState,
                title = {
                    Text(
                        text = "Step 1: Pick Start Date 📅",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 24.dp, top = 16.dp)
                    )
                }
            )
        }
    } else {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = {
                        val startMs = selectedStartMs ?: 0L
                        val endMs = endPickerState.selectedDateMillis ?: System.currentTimeMillis()
                        onConfirm(startMs, endMs + (24 * 60 * 60 * 1000 - 1))
                    }
                ) {
                    Text("Apply Range", fontWeight = FontWeight.Bold, color = PrimaryBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { step = 1 }) {
                    Text("Back", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        ) {
            DatePicker(
                state = endPickerState,
                title = {
                    Text(
                        text = "Step 2: Pick End Date 📅",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 24.dp, top = 16.dp)
                    )
                }
            )
        }
    }
}
