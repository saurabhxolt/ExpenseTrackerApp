package com.expensetracker.app.features.split

import android.widget.Toast
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensetracker.app.core.database.entity.GroupEntity
import com.expensetracker.app.core.database.entity.SplitExpenseEntity
import com.expensetracker.app.core.ui.theme.GreenSuccess
import com.expensetracker.app.core.ui.theme.PrimaryBlue
import com.expensetracker.app.core.ui.theme.RedExpense

@Composable
fun SplitRoute(
    viewModel: SplitViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    SplitScreen(
        uiState = uiState,
        onSelectGroup = viewModel::selectGroup,
        onCreateGroup = viewModel::createGroup,
        onDeleteGroup = viewModel::deleteGroup,
        onAddExpense = viewModel::addExpense,
        onDeleteExpense = viewModel::deleteExpense,
        onRecordSettlement = viewModel::recordSettlement,
        onCopyWhatsAppSummary = {
            val success = viewModel.copyWhatsAppSummaryToClipboard(context)
            if (success) {
                Toast.makeText(context, "Copied WhatsApp Settlement Summary!", Toast.LENGTH_SHORT).show()
            }
        }
    )
}

@Composable
fun SplitScreen(
    uiState: SplitUiState,
    onSelectGroup: (Long?) -> Unit,
    onCreateGroup: (name: String, description: String, emoji: String, members: List<String>) -> Unit,
    onDeleteGroup: (GroupEntity) -> Unit,
    onAddExpense: (groupId: Long, title: String, amount: Double, paidBy: String, category: String, customShares: Map<String, Double>?) -> Unit,
    onDeleteExpense: (SplitExpenseEntity) -> Unit,
    onRecordSettlement: (groupId: Long, fromMember: String, toMember: String, amount: Double) -> Unit,
    onCopyWhatsAppSummary: () -> Unit
) {
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showSettlementDialog by remember { mutableStateOf(false) }

    if (showCreateGroupDialog) {
        CreateGroupDialog(
            onDismiss = { showCreateGroupDialog = false },
            onConfirm = { name, desc, emoji, members ->
                onCreateGroup(name, desc, emoji, members)
                showCreateGroupDialog = false
            }
        )
    }

    val selectedSummary = uiState.selectedGroupSummary

    if (showAddExpenseDialog && selectedSummary != null) {
        AddSplitExpenseDialog(
            groupMembers = selectedSummary.members,
            onDismiss = { showAddExpenseDialog = false },
            onConfirm = { title, amount, paidBy, category ->
                onAddExpense(selectedSummary.group.id, title, amount, paidBy, category, null)
                showAddExpenseDialog = false
            }
        )
    }

    if (showSettlementDialog && selectedSummary != null) {
        RecordSettlementDialog(
            groupMembers = selectedSummary.members,
            onDismiss = { showSettlementDialog = false },
            onConfirm = { fromMember, toMember, amount ->
                onRecordSettlement(selectedSummary.group.id, fromMember, toMember, amount)
                showSettlementDialog = false
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            if (uiState.selectedGroupId == null) {
                FloatingActionButton(
                    onClick = { showCreateGroupDialog = true },
                    containerColor = PrimaryBlue,
                    contentColor = Color.White
                ) {
                    Icon(imageVector = Icons.Default.GroupAdd, contentDescription = "Create Group")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.selectedGroupId == null) {
                // ALL GROUPS LIST VIEW
                Text(
                    text = "Split Bills & Group Ledger 👥",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Track shared trip expenses, apartment bills, & dinner splits 100% offline.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (uiState.groups.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("👥 No Split Groups Yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Create a group for a trip, apartment, or outing!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showCreateGroupDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Create Group")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(uiState.groups, key = { it.id }) { group ->
                            GroupCardItem(
                                group = group,
                                onClick = { onSelectGroup(group.id) },
                                onDelete = { onDeleteGroup(group) }
                            )
                        }
                    }
                }
            } else if (selectedSummary != null) {
                // GROUP DETAIL VIEW
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = { onSelectGroup(null) }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = PrimaryBlue)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${selectedSummary.group.emoji} ${selectedSummary.group.name}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Summary Banner Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Total Group Spend", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("₹${String.format("%.2f", selectedSummary.totalSpend)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Your Position", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        val net = selectedSummary.userNetBalance
                                        val color = if (net > 0.01) GreenSuccess else if (net < -0.01) RedExpense else MaterialTheme.colorScheme.onSurface
                                        val sign = if (net > 0.01) "+" else ""
                                        Text("$sign₹${String.format("%.2f", net)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
                                    }
                                }

                                // Who Owes Whom List
                                Text("Who Owes Whom:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                if (selectedSummary.debtTransfers.isEmpty()) {
                                    Text("✅ All expenses are settled up!", style = MaterialTheme.typography.bodySmall, color = GreenSuccess)
                                } else {
                                    selectedSummary.debtTransfers.forEach { dt ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("👉 ", fontSize = 12.sp)
                                            Text(dt.fromMember, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(" owes ", fontSize = 13.sp)
                                            Text(dt.toMember, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Spacer(modifier = Modifier.weight(1f))
                                            Text("₹${String.format("%.2f", dt.amount)}", fontWeight = FontWeight.Bold, color = PrimaryBlue, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Action Buttons Row
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showAddExpenseDialog = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Expense", fontSize = 11.sp, maxLines = 1, softWrap = false)
                            }
                            Button(
                                onClick = { showSettlementDialog = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess)
                            ) {
                                Icon(imageVector = Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Settle Up", fontSize = 11.sp, maxLines = 1, softWrap = false)
                            }
                            Button(
                                onClick = onCopyWhatsAppSummary,
                                modifier = Modifier.weight(1.1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)
                            ) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("WhatsApp Summary", fontSize = 10.sp, maxLines = 1, softWrap = false)
                            }
                        }
                    }

                    // Expenses List Header
                    item {
                        Text("Group Expense History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }

                    if (uiState.selectedGroupExpenses.isEmpty()) {
                        item {
                            Text("No expenses logged for this group yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        items(uiState.selectedGroupExpenses, key = { it.id }) { expense ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(expense.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("Paid by ${expense.paidByMember} • ${expense.category}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("₹${String.format("%.2f", expense.totalAmount)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        IconButton(onClick = { onDeleteExpense(expense) }, modifier = Modifier.size(24.dp)) {
                                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
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

@Composable
fun GroupCardItem(
    group: GroupEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(PrimaryBlue.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(group.emoji, fontSize = 22.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(group.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    if (group.description.isNotBlank()) {
                        Text(group.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Group", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
fun CreateGroupDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String, emoji: String, members: List<String>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("✈️") }
    var membersInput by remember { mutableStateOf("Rahul, Amit, Priya") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Split Group 👥", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Group Name (e.g. Goa Trip)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = emoji,
                    onValueChange = { emoji = it },
                    label = { Text("Emoji Icon") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = membersInput,
                    onValueChange = { membersInput = it },
                    label = { Text("Members (Comma-separated)") },
                    supportingText = { Text("Note: You are automatically included as group creator.") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val memberList = membersInput.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        onConfirm(name, description, emoji, memberList)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSplitExpenseDialog(
    groupMembers: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (title: String, amount: Double, paidBy: String, category: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var paidBy by remember { mutableStateOf(groupMembers.firstOrNull() ?: "You") }
    var category by remember { mutableStateOf("Food & Dining") }
    var paidByExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Group Expense 💳", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Expense Title (e.g. Dinner)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Total Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Paid By Dropdown
                ExposedDropdownMenuBox(
                    expanded = paidByExpanded,
                    onExpandedChange = { paidByExpanded = !paidByExpanded }
                ) {
                    OutlinedTextField(
                        value = paidBy,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Paid By") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = paidByExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = paidByExpanded,
                        onDismissRequest = { paidByExpanded = false }
                    ) {
                        groupMembers.forEach { member ->
                            DropdownMenuItem(
                                text = { Text(member) },
                                onClick = {
                                    paidBy = member
                                    paidByExpanded = false
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
                    if (title.isNotBlank() && amount > 0) {
                        onConfirm(title, amount, paidBy, category)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordSettlementDialog(
    groupMembers: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (fromMember: String, toMember: String, amount: Double) -> Unit
) {
    var fromMember by remember { mutableStateOf(groupMembers.firstOrNull() ?: "You") }
    var toMember by remember { mutableStateOf(groupMembers.getOrNull(1) ?: "Rahul") }
    var amountText by remember { mutableStateOf("") }
    var fromExpanded by remember { mutableStateOf(false) }
    var toExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Settlement Payment 💸", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // From Member
                ExposedDropdownMenuBox(
                    expanded = fromExpanded,
                    onExpandedChange = { fromExpanded = !fromExpanded }
                ) {
                    OutlinedTextField(
                        value = fromMember,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("From (Payer)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fromExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = fromExpanded,
                        onDismissRequest = { fromExpanded = false }
                    ) {
                        groupMembers.forEach { member ->
                            DropdownMenuItem(
                                text = { Text(member) },
                                onClick = {
                                    fromMember = member
                                    fromExpanded = false
                                }
                            )
                        }
                    }
                }

                // To Member
                ExposedDropdownMenuBox(
                    expanded = toExpanded,
                    onExpandedChange = { toExpanded = !toExpanded }
                ) {
                    OutlinedTextField(
                        value = toMember,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("To (Recipient)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = toExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = toExpanded,
                        onDismissRequest = { toExpanded = false }
                    ) {
                        groupMembers.forEach { member ->
                            DropdownMenuItem(
                                text = { Text(member) },
                                onClick = {
                                    toMember = member
                                    toExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Settlement Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (amount > 0 && fromMember != toMember) {
                        onConfirm(fromMember, toMember, amount)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess)
            ) {
                Text("Record")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
