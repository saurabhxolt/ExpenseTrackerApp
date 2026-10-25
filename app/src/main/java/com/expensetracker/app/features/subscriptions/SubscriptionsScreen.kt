package com.expensetracker.app.features.subscriptions

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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import com.expensetracker.app.core.analysis.PolicyProvisioningItem
import com.expensetracker.app.core.database.entity.InsurancePolicyEntity
import com.expensetracker.app.core.database.entity.RecurringIncomeEntity
import com.expensetracker.app.core.database.entity.SubscriptionEntity
import com.expensetracker.app.core.security.LocalPrivacyMode
import com.expensetracker.app.core.ui.theme.GreenSuccess
import com.expensetracker.app.core.ui.theme.PrimaryBlue
import com.expensetracker.app.core.ui.theme.RedExpense
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionsRoute(
    viewModel: SubscriptionsViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    var showAddSubscriptionDialog by remember { mutableStateOf(false) }
    var editingSubscription by remember { mutableStateOf<SubscriptionEntity?>(null) }

    var showAddIncomeDialog by remember { mutableStateOf(false) }
    var editingIncome by remember { mutableStateOf<RecurringIncomeEntity?>(null) }
    var incomeToMarkReceived by remember { mutableStateOf<RecurringIncomeEntity?>(null) }

    var showAddPolicyDialog by remember { mutableStateOf(false) }
    var editingPolicy by remember { mutableStateOf<InsurancePolicyEntity?>(null) }
    var policyToStash by remember { mutableStateOf<PolicyProvisioningItem?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (uiState.selectedTab) {
                        0 -> {
                            editingSubscription = null
                            showAddSubscriptionDialog = true
                        }
                        1 -> {
                            editingIncome = null
                            showAddIncomeDialog = true
                        }
                        2 -> {
                            editingPolicy = null
                            showAddPolicyDialog = true
                        }
                    }
                },
                containerColor = when (uiState.selectedTab) {
                    1 -> GreenSuccess
                    2 -> Color(0xFF8B5CF6)
                    else -> PrimaryBlue
                },
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = when (uiState.selectedTab) {
                        1 -> "Add Income Stream"
                        2 -> "Add Insurance Policy"
                        else -> "Add Subscription"
                    }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Segmented Tabs Header
                SecondaryTabRow(
                    selectedTabIndex = uiState.selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = uiState.selectedTab == 0,
                        onClick = { viewModel.setSelectedTab(0) },
                        text = { Text("💸 Mandates (${uiState.activeCount})", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) }
                    )
                    Tab(
                        selected = uiState.selectedTab == 1,
                        onClick = { viewModel.setSelectedTab(1) },
                        text = { Text("💰 Inflows (${uiState.recurringIncomes.size})", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) }
                    )
                    Tab(
                        selected = uiState.selectedTab == 2,
                        onClick = { viewModel.setSelectedTab(2) },
                        text = { Text("🛡️ Policies (${uiState.provisioningPlan.activePolicyCount})", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) }
                    )
                }

                when (uiState.selectedTab) {
                    0 -> SubscriptionsTabContent(
                        uiState = uiState,
                        onAddCandidate = { viewModel.addDetectedCandidate(it) },
                        onToggleActive = { viewModel.toggleSubscriptionActive(it.entity) },
                        onMarkPaid = { viewModel.markAsPaid(it) },
                        onEdit = { item ->
                            editingSubscription = item.entity
                            showAddSubscriptionDialog = true
                        },
                        onDelete = { viewModel.deleteSubscription(it.entity) }
                    )
                    1 -> RecurringInflowsTabContent(
                        uiState = uiState,
                        onToggleActive = { viewModel.toggleIncomeActive(it.entity) },
                        onMarkReceived = { incomeToMarkReceived = it.entity },
                        onEdit = { item ->
                            editingIncome = item.entity
                            showAddIncomeDialog = true
                        },
                        onDelete = { viewModel.deleteIncome(it.entity) }
                    )
                    2 -> InsurancePoliciesTabContent(
                        uiState = uiState,
                        onStashToJar = { policyToStash = it },
                        onMarkRenewed = { viewModel.markPolicyRenewed(it.policy) },
                        onToggleActive = { viewModel.togglePolicyActive(it.policy) },
                        onEdit = { item ->
                            editingPolicy = item.policy
                            showAddPolicyDialog = true
                        },
                        onDelete = { viewModel.deletePolicy(it.policy) }
                    )
                }
            }

            // --- Modals ---
            if (showAddSubscriptionDialog) {
                AddEditSubscriptionDialog(
                    initialEntity = editingSubscription,
                    availableCategories = uiState.categories,
                    onDismiss = { showAddSubscriptionDialog = false },
                    onConfirm = { name, amount, cycle, category, dueDateMs ->
                        if (editingSubscription == null) {
                            viewModel.saveSubscription(name, amount, cycle, category, dueDateMs)
                        } else {
                            viewModel.updateSubscription(editingSubscription!!, name, amount, cycle, category, dueDateMs)
                        }
                        showAddSubscriptionDialog = false
                    }
                )
            }

            if (showAddIncomeDialog) {
                AddEditRecurringIncomeDialog(
                    initialEntity = editingIncome,
                    onDismiss = { showAddIncomeDialog = false },
                    onConfirm = { name, amount, freq, day, cat, isSal, payer, dueMs ->
                        if (editingIncome == null) {
                            viewModel.saveRecurringIncome(name, amount, freq, day, cat, isSal, payer, dueMs)
                        } else {
                            viewModel.updateRecurringIncome(editingIncome!!, name, amount, freq, day, cat, isSal, payer, dueMs)
                        }
                        showAddIncomeDialog = false
                    }
                )
            }

            if (incomeToMarkReceived != null) {
                MarkIncomeReceivedDialog(
                    income = incomeToMarkReceived!!,
                    onDismiss = { incomeToMarkReceived = null },
                    onConfirm = { recordInLedger ->
                        viewModel.markIncomeReceived(incomeToMarkReceived!!, recordInLedger)
                        incomeToMarkReceived = null
                    }
                )
            }

            if (showAddPolicyDialog) {
                AddEditInsurancePolicyDialog(
                    initialEntity = editingPolicy,
                    onDismiss = { showAddPolicyDialog = false },
                    onConfirm = { name, type, insurer, num, prem, freq, dueMs, tax, createJar, jarColor ->
                        if (editingPolicy == null) {
                            viewModel.saveInsurancePolicy(name, type, insurer, num, prem, freq, dueMs, tax, createJar, jarColor)
                        } else {
                            viewModel.updateInsurancePolicy(editingPolicy!!, name, type, insurer, num, prem, freq, dueMs, tax, editingPolicy!!.linkedSavingsGoalId)
                        }
                        showAddPolicyDialog = false
                    }
                )
            }

            if (policyToStash != null) {
                SalaryStashDialog(
                    item = policyToStash!!,
                    onDismiss = { policyToStash = null },
                    onConfirm = { jarId, amount, recordInLedger ->
                        viewModel.stashToJar(jarId, amount, recordInLedger)
                        policyToStash = null
                    }
                )
            }
        }
    }
}

// ==========================================
// TAB 0: MANDATES & SUBSCRIPTIONS (OUTFLOWS)
// ==========================================
@Composable
private fun SubscriptionsTabContent(
    uiState: SubscriptionsUiState,
    onAddCandidate: (com.expensetracker.app.core.data.repository.DetectedSubscriptionCandidate) -> Unit,
    onToggleActive: (SubscriptionUiModel) -> Unit,
    onMarkPaid: (SubscriptionUiModel) -> Unit,
    onEdit: (SubscriptionUiModel) -> Unit,
    onDelete: (SubscriptionUiModel) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentPadding = PaddingValues(bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        val isPrivacy = LocalPrivacyMode.current
                        Text("Monthly Outflow Mandates", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            if (isPrivacy) "₹•••••" else "₹${String.format(Locale.getDefault(), "%,.2f", uiState.totalMonthlyCost)}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = RedExpense
                        )
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "${uiState.activeCount} Active",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Radar Candidate Banner
        if (uiState.detectedCandidates.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🛰️ Smart Mandates Radar", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.weight(1f))
                            Text("${uiState.detectedCandidates.size} Detected", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        }
                        Text(
                            "Detected recurring debits in your history. Tap to add:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(uiState.detectedCandidates) { cand ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.clickable { onAddCandidate(cand) }
                                ) {
                                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text("${cand.name} (₹${cand.amount.toInt()})", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        Spacer(Modifier.width(6.dp))
                                        Text("+ Add", fontSize = 11.sp, color = PrimaryBlue, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (uiState.subscriptions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🔄", fontSize = 42.sp)
                        Spacer(Modifier.height(10.dp))
                        Text("No Active Mandates", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Add recurring subscriptions, SIPs, utilities, and EMIs with the + button.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(uiState.subscriptions) { sub ->
                SubscriptionItemCard(
                    item = sub,
                    onToggleActive = { onToggleActive(sub) },
                    onMarkPaid = { onMarkPaid(sub) },
                    onEdit = { onEdit(sub) },
                    onDelete = { onDelete(sub) }
                )
            }
        }
    }
}

// ==========================================
// TAB 1: RECURRING INFLOWS (INCOMES)
// ==========================================
@Composable
private fun RecurringInflowsTabContent(
    uiState: SubscriptionsUiState,
    onToggleActive: (RecurringIncomeUiModel) -> Unit,
    onMarkReceived: (RecurringIncomeUiModel) -> Unit,
    onEdit: (RecurringIncomeUiModel) -> Unit,
    onDelete: (RecurringIncomeUiModel) -> Unit
) {
    val isPrivacy = LocalPrivacyMode.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentPadding = PaddingValues(bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Monthly Inflow Horizon", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                if (isPrivacy) "₹•••••" else "+₹${String.format(Locale.getDefault(), "%,.2f", uiState.totalExpectedMonthlyIncome)}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = GreenSuccess
                            )
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = GreenSuccess.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "${uiState.recurringIncomes.count { it.entity.isActive }} Streams",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontWeight = FontWeight.Bold,
                                color = GreenSuccess,
                                fontSize = 13.sp
                            )
                        }
                    }

                    if (uiState.totalExpectedMonthlyIncome > 0) {
                        Spacer(Modifier.height(14.dp))
                        val progress = (uiState.totalReceivedMonthlyIncome / uiState.totalExpectedMonthlyIncome).coerceIn(0.0, 1.0).toFloat()
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = GreenSuccess,
                            trackColor = MaterialTheme.colorScheme.outlineVariant
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                "Realized: " + if (isPrivacy) "₹•••••" else "₹${String.format(Locale.getDefault(), "%,.0f", uiState.totalReceivedMonthlyIncome)}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val pending = (uiState.totalExpectedMonthlyIncome - uiState.totalReceivedMonthlyIncome).coerceAtLeast(0.0)
                            Text(
                                "Pending: " + if (isPrivacy) "₹•••••" else "₹${String.format(Locale.getDefault(), "%,.0f", pending)}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        if (uiState.recurringIncomes.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("💰", fontSize = 42.sp)
                        Spacer(Modifier.height(10.dp))
                        Text("No Scheduled Income Streams", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Add your regular monthly salary, flat rental income, quarterly dividends, or LIC payouts to track incoming cashflow.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(uiState.recurringIncomes) { item ->
                RecurringIncomeItemCard(
                    item = item,
                    onToggleActive = { onToggleActive(item) },
                    onMarkReceived = { onMarkReceived(item) },
                    onEdit = { onEdit(item) },
                    onDelete = { onDelete(item) }
                )
            }
        }
    }
}

// ==========================================
// TAB 2: POLICIES & SINKING VAULT
// ==========================================
@Composable
private fun InsurancePoliciesTabContent(
    uiState: SubscriptionsUiState,
    onStashToJar: (PolicyProvisioningItem) -> Unit,
    onMarkRenewed: (PolicyProvisioningItem) -> Unit,
    onToggleActive: (PolicyProvisioningItem) -> Unit,
    onEdit: (PolicyProvisioningItem) -> Unit,
    onDelete: (PolicyProvisioningItem) -> Unit
) {
    val plan = uiState.provisioningPlan
    val isPrivacy = LocalPrivacyMode.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentPadding = PaddingValues(bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Salary Day Provisioning Hero Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("🛡️ Salary Day Policy Stash", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                if (isPrivacy) "₹•••••" else "₹${String.format(Locale.getDefault(), "%,.0f", plan.totalRecommendedStash)}/mo",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF8B5CF6)
                            )
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF8B5CF6).copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "${plan.activePolicyCount} Policies",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF8B5CF6),
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Pacing annual lump-sum premiums across monthly salaries so renewal months never wipe out your savings.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (plan.items.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🛡️", fontSize = 42.sp)
                        Spacer(Modifier.height(10.dp))
                        Text("No Policies in Vault", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Add your Health Mediclaim, Term Life, Car, Bike, or LIC policies to auto-calculate your monthly salary stash pace.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(plan.items) { item ->
                PolicyItemCard(
                    item = item,
                    onStashToJar = { onStashToJar(item) },
                    onMarkRenewed = { onMarkRenewed(item) },
                    onToggleActive = { onToggleActive(item) },
                    onEdit = { onEdit(item) },
                    onDelete = { onDelete(item) }
                )
            }
        }
    }
}

// ==========================================
// ITEM CARDS
// ==========================================
@Composable
private fun SubscriptionItemCard(
    item: SubscriptionUiModel,
    onToggleActive: () -> Unit,
    onMarkPaid: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isPrivacy = LocalPrivacyMode.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (isPrivacy) "₹•••••" else "-₹${String.format(Locale.getDefault(), "%,.2f", item.amount)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = RedExpense
                )
            }

            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${item.billingCycle} • ${item.category}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = item.statusText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = when {
                        item.isOverdue -> RedExpense
                        item.isDueSoon -> Color(0xFFF59E0B)
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
            }

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onMarkPaid,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("✓ Mark Paid", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }

                Spacer(Modifier.weight(1f))
                IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RedExpense, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun RecurringIncomeItemCard(
    item: RecurringIncomeUiModel,
    onToggleActive: () -> Unit,
    onMarkReceived: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isPrivacy = LocalPrivacyMode.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.entity.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (item.entity.isSalary) {
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        color = PrimaryBlue.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "🏢 Salary",
                            color = PrimaryBlue,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (isPrivacy) "₹•••••" else "+₹${String.format(Locale.getDefault(), "%,.2f", item.entity.amount)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = GreenSuccess
                )
            }

            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${item.entity.frequency} • ${item.entity.category}" + if (item.entity.payerPpiOrBank.isNotBlank()) " (${item.entity.payerPpiOrBank})" else "",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = item.statusText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = when {
                        item.isReceivedThisCycle -> GreenSuccess
                        item.isOverdue -> RedExpense
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
            }

            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onMarkReceived,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("✓ Mark Received", fontSize = 11.sp, color = GreenSuccess, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.weight(1f))
                IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RedExpense, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun PolicyItemCard(
    item: PolicyProvisioningItem,
    onStashToJar: () -> Unit,
    onMarkRenewed: () -> Unit,
    onToggleActive: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isPrivacy = LocalPrivacyMode.current
    val p = item.policy
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = p.policyName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (p.taxSection != "NONE") {
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Sec ${p.taxSection}",
                            color = Color(0xFF10B981),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (isPrivacy) "₹•••••" else "₹${String.format(Locale.getDefault(), "%,.0f", p.premiumAmount)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF8B5CF6)
                )
            }

            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val insurerText = if (p.insurerName.isNotBlank()) "${p.insurerName} • " else ""
                Text(
                    text = "$insurerText${p.frequency} • Renews ${dateFormat.format(Date(p.renewalDueDateMs))}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Linked Sinking Jar Progress
            if (item.linkedJar != null) {
                Spacer(Modifier.height(10.dp))
                val progress = (item.currentSavedAmount / item.targetAmount).coerceIn(0.0, 1.0).toFloat()
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFF8B5CF6),
                    trackColor = MaterialTheme.colorScheme.outlineVariant
                )
                Spacer(Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "🏺 Saved in Jar: " + if (isPrivacy) "₹•••••" else "₹${String.format(Locale.getDefault(), "%,.0f", item.currentSavedAmount)} (${(progress * 100).toInt()}%)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        if (item.isFunded) "Fully Funded 🟢" else "Need ₹${String.format(Locale.getDefault(), "%,.0f", item.recommendedMonthlyStash)}/mo",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (item.isFunded) GreenSuccess else Color(0xFF8B5CF6)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item.linkedJar != null && !item.isFunded) {
                    Button(
                        onClick = onStashToJar,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6).copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "+ Stash ₹${item.recommendedMonthlyStash.toInt()}",
                            fontSize = 11.sp,
                            color = Color(0xFF8B5CF6),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                }

                Button(
                    onClick = onMarkRenewed,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("✓ Mark Renewed", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                }

                Spacer(Modifier.weight(1f))
                IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RedExpense, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// ==========================================
// DIALOGS
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRecurringIncomeDialog(
    initialEntity: RecurringIncomeEntity?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, amount: Double, freq: String, day: Int, cat: String, isSal: Boolean, payer: String, dueMs: Long) -> Unit
) {
    var name by remember { mutableStateOf(initialEntity?.name ?: "") }
    var amountStr by remember { mutableStateOf(initialEntity?.amount?.toString() ?: "") }
    var frequency by remember { mutableStateOf(initialEntity?.frequency ?: "Monthly") }
    var dayOfMonth by remember { mutableStateOf(initialEntity?.expectedDayOfMonth?.toString() ?: "1") }
    var category by remember { mutableStateOf(initialEntity?.category ?: "Salary") }
    var isSalary by remember { mutableStateOf(initialEntity?.isSalary ?: false) }
    var payer by remember { mutableStateOf(initialEntity?.payerPpiOrBank ?: "") }
    var nextExpectedDateMs by remember { mutableStateOf(initialEntity?.nextDueDateMs ?: System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    val presets = listOf(
        "🏢 Monthly Salary" to ("Salary" to true),
        "🏠 House Rent" to ("Rental Income" to false),
        "📈 Stock Dividend" to ("Investments" to false),
        "💼 Freelance" to ("Business" to false),
        "📜 LIC Annuity" to ("Pensions / Annuity" to false)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialEntity == null) "Add Recurring Income 💰" else "Edit Recurring Income") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Presets
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(presets) { (label, meta) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable {
                                name = label.substring(2).trim()
                                category = meta.first
                                isSalary = meta.second
                            }
                        ) {
                            Text(label, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Income Name (e.g. Infosys Salary, Flat Rent)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dayOfMonth,
                        onValueChange = { dayOfMonth = it },
                        label = { Text("Day of Month (1-31)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = frequency,
                        onValueChange = { frequency = it },
                        label = { Text("Frequency") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                ) {
                    OutlinedTextField(
                        value = dateFormat.format(Date(nextExpectedDateMs)),
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        label = { Text("Next Expected Date / Cycle Start") },
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Default.CalendarToday, contentDescription = "Pick Date")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = payer,
                    onValueChange = { payer = it },
                    label = { Text("Payer Name / Employer (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isSalary, onCheckedChange = { isSalary = it })
                    Text("This is my primary Monthly Salary", fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    val day = dayOfMonth.toIntOrNull()?.coerceIn(1, 31) ?: 1
                    if (name.isNotBlank() && amt > 0) {
                        onConfirm(name, amt, frequency, day, category, isSalary, payer, nextExpectedDateMs)
                    }
                }
            ) {
                Text("Save Income")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = nextExpectedDateMs)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { ms ->
                            nextExpectedDateMs = ms
                            val cal = Calendar.getInstance().apply { timeInMillis = ms }
                            dayOfMonth = cal.get(Calendar.DAY_OF_MONTH).toString()
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun MarkIncomeReceivedDialog(
    income: RecurringIncomeEntity,
    onDismiss: () -> Unit,
    onConfirm: (recordInLedger: Boolean) -> Unit
) {
    var recordInLedger by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mark Income Received ✅") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Did you receive ₹${income.amount.toInt()} for ${income.name}?")
                Text(
                    "This will advance the schedule date to the next cycle.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = recordInLedger, onCheckedChange = { recordInLedger = it })
                    Text("Also insert a Credit transaction into main ledger", fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(recordInLedger) }) {
                Text("Confirm Received")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditInsurancePolicyDialog(
    initialEntity: InsurancePolicyEntity?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: String, insurer: String, num: String, prem: Double, freq: String, dueMs: Long, tax: String, createJar: Boolean, jarColor: String) -> Unit
) {
    var policyName by remember { mutableStateOf(initialEntity?.policyName ?: "") }
    var policyType by remember { mutableStateOf(initialEntity?.policyType ?: "HEALTH") }
    var insurerName by remember { mutableStateOf(initialEntity?.insurerName ?: "") }
    var policyNumber by remember { mutableStateOf(initialEntity?.policyNumber ?: "") }
    var premiumStr by remember { mutableStateOf(initialEntity?.premiumAmount?.toString() ?: "") }
    var frequency by remember { mutableStateOf(initialEntity?.frequency ?: "Yearly") }
    var taxSection by remember { mutableStateOf(initialEntity?.taxSection ?: "80D") }
    var createLinkedJar by remember { mutableStateOf(initialEntity == null) }
    var renewalDueDateMs by remember {
        mutableStateOf(
            initialEntity?.renewalDueDateMs ?: (System.currentTimeMillis() + 365L * 24 * 3600 * 1000)
        )
    }
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    val policyPresets = listOf(
        "🏥 Health Mediclaim" to Triple("HEALTH", "80D", "#10B981"),
        "🛡️ Term Life 1Cr" to Triple("TERM", "80C", "#3B82F6"),
        "🚗 Car Insurance" to Triple("CAR", "NONE", "#F59E0B"),
        "🏍️ Bike Insurance" to Triple("BIKE", "NONE", "#EC4899"),
        "📜 LIC Endowment" to Triple("LIC", "80C", "#8B5CF6")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialEntity == null) "Add Insurance Policy 🛡️" else "Edit Insurance Policy") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Presets
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(policyPresets) { (label, meta) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable {
                                policyName = label.substring(2).trim()
                                policyType = meta.first
                                taxSection = meta.second
                            }
                        ) {
                            Text(label, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                }

                OutlinedTextField(
                    value = policyName,
                    onValueChange = { policyName = it },
                    label = { Text("Policy Name (e.g. Star Health Family)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = insurerName,
                        onValueChange = { insurerName = it },
                        label = { Text("Insurer (e.g. HDFC, Acko)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = premiumStr,
                        onValueChange = { premiumStr = it },
                        label = { Text("Premium (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = frequency,
                        onValueChange = { frequency = it },
                        label = { Text("Cycle (Yearly/Quarterly)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = taxSection,
                        onValueChange = { taxSection = it },
                        label = { Text("Tax Sec (80C/80D/NONE)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Explicit Renewal Due Date Picker
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                ) {
                    OutlinedTextField(
                        value = dateFormat.format(Date(renewalDueDateMs)),
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        label = { Text("Renewal Due Date") },
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Default.CalendarToday, contentDescription = "Pick Renewal Date")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = policyNumber,
                    onValueChange = { policyNumber = it },
                    label = { Text("Policy Number (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (initialEntity == null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = createLinkedJar, onCheckedChange = { createLinkedJar = it })
                        Text("Auto-create linked Sinking Fund Jar (Recommended)", fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val prem = premiumStr.toDoubleOrNull() ?: 0.0
                    if (policyName.isNotBlank() && prem > 0) {
                        onConfirm(policyName, policyType, insurerName, policyNumber, prem, frequency, renewalDueDateMs, taxSection, createLinkedJar, "#8B5CF6")
                    }
                }
            ) {
                Text("Save Policy")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = renewalDueDateMs)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { renewalDueDateMs = it }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun SalaryStashDialog(
    item: PolicyProvisioningItem,
    onDismiss: () -> Unit,
    onConfirm: (jarId: Long, amount: Double, recordInLedger: Boolean) -> Unit
) {
    val jarId = item.linkedJar?.id ?: return
    var amountStr by remember { mutableStateOf(item.recommendedMonthlyStash.toInt().toString()) }
    var recordInLedger by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Stash from Salary 💰") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Deposit into ${item.linkedJar.title}?")
                Text(
                    "Target: ₹${item.targetAmount.toInt()} | Currently Saved: ₹${item.currentSavedAmount.toInt()}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Stash Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = recordInLedger, onCheckedChange = { recordInLedger = it })
                    Text("Record as Investment in main ledger", fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        onConfirm(jarId, amt, recordInLedger)
                    }
                }
            ) {
                Text("Deposit to Jar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// Dialog for Subscriptions (Existing)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditSubscriptionDialog(
    initialEntity: SubscriptionEntity?,
    availableCategories: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, amount: Double, cycle: String, category: String, dueDateMs: Long) -> Unit
) {
    var name by remember { mutableStateOf(initialEntity?.name ?: "") }
    var amountStr by remember { mutableStateOf(initialEntity?.amount?.toString() ?: "") }
    var selectedCycle by remember { mutableStateOf(initialEntity?.billingCycle ?: "Monthly") }
    var selectedCategory by remember { mutableStateOf(initialEntity?.category ?: "Subscriptions") }
    var dueDateMs by remember { mutableStateOf(initialEntity?.nextDueDateMs ?: System.currentTimeMillis()) }

    var expandedCycle by remember { mutableStateOf(false) }
    var expandedCategory by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val cycles = listOf("Monthly", "Quarterly", "Yearly", "Weekly")
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialEntity == null) "Add Mandate / Subscription" else "Edit Mandate") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Service Name (e.g. Netflix, Rent, SIP)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = expandedCycle,
                    onExpandedChange = { expandedCycle = !expandedCycle }
                ) {
                    OutlinedTextField(
                        value = selectedCycle,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Billing Cycle") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCycle) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedCycle,
                        onDismissRequest = { expandedCycle = false }
                    ) {
                        cycles.forEach { cycle ->
                            DropdownMenuItem(
                                text = { Text(cycle) },
                                onClick = {
                                    selectedCycle = cycle
                                    expandedCycle = false
                                }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = expandedCategory,
                    onExpandedChange = { expandedCategory = !expandedCategory }
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedCategory,
                        onDismissRequest = { expandedCategory = false }
                    ) {
                        availableCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    selectedCategory = cat
                                    expandedCategory = false
                                }
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                ) {
                    OutlinedTextField(
                        value = dateFormat.format(Date(dueDateMs)),
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        label = { Text("Next Due Date") },
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Default.CalendarToday, contentDescription = "Pick Date")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank() && amt > 0) {
                        onConfirm(name, amt, selectedCycle, selectedCategory, dueDateMs)
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dueDateMs)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { dueDateMs = it }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
