package com.expensetracker.app.features.budgets

import androidx.compose.animation.core.animateFloatAsState
import com.expensetracker.app.core.security.LocalPrivacyMode
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensetracker.app.core.ui.theme.GreenSuccess
import com.expensetracker.app.core.ui.theme.PrimaryBlue
import com.expensetracker.app.core.ui.theme.RedExpense

@Composable
fun BudgetsRoute(
    viewModel: BudgetsViewModel,
    savingsViewModel: com.expensetracker.app.features.savings.SavingsGoalsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val savingsUiState by savingsViewModel.uiState.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: Budgets, 1: Savings Goals
    var showAddBudgetDialog by remember { mutableStateOf(false) }
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<com.expensetracker.app.core.database.entity.SavingsGoalEntity?>(null) }
    var depositGoal by remember { mutableStateOf<com.expensetracker.app.core.database.entity.SavingsGoalEntity?>(null) }
    var withdrawGoal by remember { mutableStateOf<com.expensetracker.app.core.database.entity.SavingsGoalEntity?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTab == 0) showAddBudgetDialog = true else showAddGoalDialog = true
                },
                containerColor = PrimaryBlue,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = if (selectedTab == 0) "Add Budget" else "Add Goal")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Segmented Header Switcher
            androidx.compose.material3.TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = PrimaryBlue
            ) {
                androidx.compose.material3.Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "Monthly Budgets 💳",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                androidx.compose.material3.Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "Savings Jars 🎯",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                if (selectedTab == 0) {
                    BudgetsScreen(
                        uiState = uiState,
                        onDeleteBudget = { item -> viewModel.deleteBudget(item.categoryName, item.limitAmount, item.budgetId) }
                    )
                    val context = androidx.compose.ui.platform.LocalContext.current
                    com.expensetracker.app.features.savings.SavingsGoalsContent(
                        uiState = savingsUiState,
                        onAddGoalClick = { showAddGoalDialog = true },
                        onAddTemplateGoal = { idx ->
                            savingsViewModel.createStarterTemplate(idx)
                            android.widget.Toast.makeText(context, "Starter Savings Jar created! 🏺", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        onDepositClick = { depositGoal = it },
                        onWithdrawClick = { withdrawGoal = it },
                        onEditGoalClick = { editingGoal = it },
                        onDeleteGoalClick = { savingsViewModel.deleteGoal(it) },
                        onToggleComplete = { savingsViewModel.toggleComplete(it) }
                    )
                }
            }
        }

        if (showAddBudgetDialog) {
            AddBudgetDialog(
                availableCategories = uiState.availableCategories,
                onDismiss = { showAddBudgetDialog = false },
                onConfirm = { category, limit ->
                    viewModel.saveBudget(category, limit)
                    showAddBudgetDialog = false
                }
            )
        }

        if (showAddGoalDialog) {
            com.expensetracker.app.features.savings.AddEditSavingsGoalDialog(
                initialGoal = null,
                onDismiss = { showAddGoalDialog = false },
                onConfirm = { title, target, current, days, colorHex, emoji, note ->
                    val deadline = if (days > 0) System.currentTimeMillis() + (days.toLong() * 24 * 60 * 60 * 1000) else 0L
                    savingsViewModel.createGoal(title, target, current, deadline, colorHex, emoji, note)
                    showAddGoalDialog = false
                }
            )
        }

        editingGoal?.let { goal ->
            com.expensetracker.app.features.savings.AddEditSavingsGoalDialog(
                initialGoal = goal,
                onDismiss = { editingGoal = null },
                onConfirm = { title, target, current, days, colorHex, emoji, note ->
                    val deadline = if (days > 0) System.currentTimeMillis() + (days.toLong() * 24 * 60 * 60 * 1000) else goal.targetDate
                    savingsViewModel.updateGoal(
                        goal.copy(
                            title = title,
                            targetAmount = target,
                            currentAmount = current,
                            targetDate = deadline,
                            colorHex = colorHex,
                            emoji = emoji,
                            note = note
                        )
                    )
                    editingGoal = null
                }
            )
        }

        val ctx = androidx.compose.ui.platform.LocalContext.current
        depositGoal?.let { goal ->
            com.expensetracker.app.features.savings.DepositWithdrawDialog(
                goal = goal,
                isDeposit = true,
                onDismiss = { depositGoal = null },
                onConfirm = { amount, recordInLedger ->
                    savingsViewModel.depositAmount(goal.id, amount, recordInLedger)
                    android.widget.Toast.makeText(ctx, "Deposited ₹${amount.toInt()} into ${goal.title} Jar! 🎯", android.widget.Toast.LENGTH_SHORT).show()
                    depositGoal = null
                }
            )
        }

        withdrawGoal?.let { goal ->
            com.expensetracker.app.features.savings.DepositWithdrawDialog(
                goal = goal,
                isDeposit = false,
                onDismiss = { withdrawGoal = null },
                onConfirm = { amount, recordInLedger ->
                    savingsViewModel.withdrawAmount(goal.id, amount, recordInLedger)
                    android.widget.Toast.makeText(ctx, "Withdrew ₹${amount.toInt()} from ${goal.title} Jar", android.widget.Toast.LENGTH_SHORT).show()
                    withdrawGoal = null
                }
            )
        }
    }
}

@Composable
fun BudgetsScreen(
    uiState: BudgetsUiState,
    onDeleteBudget: (CategoryBudgetUiModel) -> Unit = {}
) {
    val overallPercentage = if (uiState.totalBudgetLimit > 0) {
        (uiState.totalSpent / uiState.totalBudgetLimit).toFloat().coerceIn(0f, 1f)
    } else 0f

    val progressAnim by animateFloatAsState(targetValue = overallPercentage, label = "overallProgress")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Budgets & Spending Caps",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Summary Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Monthly Budget Summary",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val isPrivacy = LocalPrivacyMode.current
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = if (isPrivacy) "₹•••••" else "₹${String.format("%.2f", uiState.totalSpent)}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isPrivacy) "of ₹•••••" else "of ₹${String.format("%.2f", uiState.totalBudgetLimit)}",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { progressAnim },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = if (overallPercentage >= 1.0f) RedExpense else if (overallPercentage >= 0.8f) Color(0xFFF59E0B) else GreenSuccess,
                        trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                    )
                }
            }
        }

        // Section Title
        item {
            Text(
                text = "Category Budgets",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        if (uiState.budgetItems.isEmpty()) {
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
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Budgets Set",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap (+) to set category spending caps and track monthly limits.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(uiState.budgetItems, key = { it.budgetId }) { item ->
                BudgetItem(item = item, onDelete = { onDeleteBudget(item) })
            }
        }

        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun BudgetItem(item: CategoryBudgetUiModel, onDelete: () -> Unit) {
    val progressAnim by animateFloatAsState(targetValue = item.percentage.coerceIn(0f, 1f), label = "itemProgress")
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    val (emoji, avatarBg) = when (item.categoryName.lowercase()) {
        "food & dining", "food", "dining" -> "🍕" to (if (isDark) Color(0xFF78350F) else Color(0xFFFEF3C7))
        "transportation", "fuel", "travel" -> "🚗" to (if (isDark) Color(0xFF134E4A) else Color(0xFFCCFBF1))
        "shopping" -> "🛒" to (if (isDark) Color(0xFF4C1D95) else Color(0xFFF3E8FF))
        "bills & utilities", "credit card bill" -> "💡" to (if (isDark) Color(0xFF0C4A6E) else Color(0xFFE0F2FE))
        "investments", "investment" -> "📈" to (if (isDark) Color(0xFF064E3B) else Color(0xFFD1FAE5))
        "health & medical" -> "🏥" to (if (isDark) Color(0xFF881337) else Color(0xFFFFE4E6))
        "income" -> "💰" to (if (isDark) Color(0xFF064E3B) else Color(0xFFD1FAE5))
        else -> "💸" to (if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9))
    }

    val progressColor = when {
        item.percentage >= 1.0f -> RedExpense
        item.percentage >= 0.8f -> Color(0xFFF59E0B) // Amber warning
        else -> GreenSuccess
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(avatarBg, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = emoji, fontSize = 18.sp)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = item.categoryName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Budget",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        val isPrivacy = LocalPrivacyMode.current
                        Text(
                            text = if (isPrivacy) "₹••••• of ₹•••••" else "₹${String.format("%.2f", item.spentAmount)} spent of ₹${String.format("%.2f", item.limitAmount)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = progressColor.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${(item.percentage * 100).toInt()}%",
                        fontWeight = FontWeight.Bold,
                        color = progressColor,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progressAnim },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBudgetDialog(
    availableCategories: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (category: String, limitAmount: Double) -> Unit
) {
    val defaultCats = listOf("General Expense", "Food & Dining", "Transportation", "Shopping", "Bills & Utilities", "Rent", "EMI", "Groceries & Vegetables", "Subscriptions", "Entertainment", "Fuel", "Health & Medical", "Insurance", "Government & Fees", "Investments", "Other")
    val categories = if (availableCategories.isNotEmpty()) availableCategories else defaultCats
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull() ?: "Food & Dining") }
    var limitText by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Set Category Budget", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat, color = MaterialTheme.colorScheme.onSurface) },
                                onClick = {
                                    selectedCategory = cat
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = limitText,
                    onValueChange = { limitText = it },
                    label = { Text("Monthly Limit (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val limit = limitText.toDoubleOrNull() ?: 0.0
                    if (limit > 0) {
                        onConfirm(selectedCategory, limit)
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
