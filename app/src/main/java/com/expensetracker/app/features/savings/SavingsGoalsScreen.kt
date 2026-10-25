package com.expensetracker.app.features.savings

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensetracker.app.core.database.entity.SavingsGoalEntity
import com.expensetracker.app.core.security.LocalPrivacyMode
import com.expensetracker.app.core.ui.theme.GreenSuccess
import com.expensetracker.app.core.ui.theme.PrimaryBlue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun SavingsGoalsContent(
    uiState: SavingsGoalsUiState,
    onAddGoalClick: () -> Unit,
    onAddTemplateGoal: (Int) -> Unit = {},
    onDepositClick: (SavingsGoalEntity) -> Unit,
    onWithdrawClick: (SavingsGoalEntity) -> Unit,
    onEditGoalClick: (SavingsGoalEntity) -> Unit,
    onDeleteGoalClick: (SavingsGoalEntity) -> Unit,
    onToggleComplete: (SavingsGoalEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val isPrivacyMode = LocalPrivacyMode.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Hero Overview Card
        item {
            SavingsHeroOverviewCard(
                uiState = uiState,
                isPrivacyMode = isPrivacyMode
            )
        }

        // 2. Goal Jars Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Savings Jars & Targets",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${uiState.activeGoalsCount} Active • ${uiState.completedGoalsCount} Done",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 3. Goals List or Empty State
        if (uiState.goals.isEmpty()) {
            item {
                EmptySavingsGoalsCard(
                    onAddGoalClick = onAddGoalClick,
                    onAddTemplateGoal = onAddTemplateGoal
                )
            }
        } else {
            items(uiState.goals, key = { it.id }) { goal ->
                SavingsGoalJarCard(
                    goal = goal,
                    isPrivacyMode = isPrivacyMode,
                    onDepositClick = { onDepositClick(goal) },
                    onWithdrawClick = { onWithdrawClick(goal) },
                    onEditClick = { onEditGoalClick(goal) },
                    onDeleteClick = { onDeleteGoalClick(goal) },
                    onToggleComplete = { onToggleComplete(goal) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
private fun SavingsHeroOverviewCard(
    uiState: SavingsGoalsUiState,
    isPrivacyMode: Boolean
) {
    val animatedProgress by animateFloatAsState(targetValue = uiState.overallProgress, label = "overall_progress")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(PrimaryBlue.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Total Vault Savings",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    color = PrimaryBlue.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${(uiState.overallProgress * 100).toInt()}% Achieved",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
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
                        text = "Total Accumulated",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (isPrivacyMode) "₹•••••" else String.format(Locale.getDefault(), "₹%,.0f", uiState.totalSavedAmount),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Target Total",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (isPrivacyMode) "₹•••••" else String.format(Locale.getDefault(), "₹%,.0f", uiState.totalTargetAmount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = GreenSuccess,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun SavingsGoalJarCard(
    goal: SavingsGoalEntity,
    isPrivacyMode: Boolean,
    onDepositClick: () -> Unit,
    onWithdrawClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onToggleComplete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val parsedColor = remember(goal.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(goal.colorHex))
        } catch (e: Exception) {
            GreenSuccess
        }
    }

    val progress = if (goal.targetAmount > 0) {
        (goal.currentAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f)
    } else 0f
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "jar_progress")
    val isDone = goal.isCompleted || (goal.targetAmount > 0 && goal.currentAmount >= goal.targetAmount)

    // Deadline calculations
    val deadlineText = remember(goal.targetDate) {
        if (goal.targetDate <= 0) null
        else {
            val now = System.currentTimeMillis()
            val diffMs = goal.targetDate - now
            val days = TimeUnit.MILLISECONDS.toDays(diffMs)
            when {
                days < 0 -> "Overdue (${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(goal.targetDate))})"
                days == 0L -> "Due Today"
                days == 1L -> "Due Tomorrow"
                days < 30 -> "$days days left"
                else -> {
                    val months = (days / 30).coerceAtLeast(1)
                    "$months mos left (${SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date(goal.targetDate))})"
                }
            }
        }
    }

    // Monthly saving pace needed
    val monthlyPaceText = remember(goal.targetAmount, goal.currentAmount, goal.targetDate) {
        if (isDone || goal.targetDate <= System.currentTimeMillis()) null
        else {
            val diffMs = goal.targetDate - System.currentTimeMillis()
            val monthsRemaining = (TimeUnit.MILLISECONDS.toDays(diffMs) / 30.0).coerceAtLeast(0.5)
            val needed = (goal.targetAmount - goal.currentAmount).coerceAtLeast(0.0)
            val perMonth = needed / monthsRemaining
            if (perMonth > 0) String.format(Locale.getDefault(), "Save ₹%,.0f/mo", perMonth) else null
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Emoji Jar, Title, Status & Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(parsedColor.copy(alpha = 0.18f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = goal.emoji.ifBlank { "🎯" },
                            fontSize = 24.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = goal.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isDone) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Completed",
                                    tint = GreenSuccess,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        if (goal.note.isNotBlank()) {
                            Text(
                                text = goal.note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Goal Options")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Goal") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onEditClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (goal.isCompleted) "Mark Active" else "Mark Done") },
                            leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onToggleComplete()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Goal", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menuExpanded = false
                                onDeleteClick()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Amount stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Saved so far",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (isPrivacyMode) "₹•••••" else String.format(Locale.getDefault(), "₹%,.0f", goal.currentAmount),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDone) GreenSuccess else MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Target: ${if (isPrivacyMode) "₹•••••" else String.format(Locale.getDefault(), "₹%,.0f", goal.targetAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = parsedColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Animated Visual Jar Fill Bar
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(9.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = parsedColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )

            // Chips: Deadline & Savings Pace Needed
            if (deadlineText != null || monthlyPaceText != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (deadlineText != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "⏳ $deadlineText",
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (monthlyPaceText != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = parsedColor.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "💡 $monthlyPaceText",
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = parsedColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons: + Deposit and - Withdraw
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onDepositClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = parsedColor)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Deposit", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onWithdrawClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Withdraw", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun EmptySavingsGoalsCard(
    onAddGoalClick: () -> Unit,
    onAddTemplateGoal: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "🏺", fontSize = 42.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No Savings Jars Configured",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Start with a 1-tap starter template or create a custom jar to visually track your sinking funds.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "⚡ Quick-Start Templates (1-Tap):",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))

            val starterTemplates = listOf(
                Triple("🛡️ Emergency Fund", "Target ₹1,00,000 • 6 Mos", 0),
                Triple("🏖️ Vacation Trip", "Target ₹50,000 • 6 Mos", 1),
                Triple("💻 Gadget & Tech", "Target ₹60,000 • 3 Mos", 2),
                Triple("🚗 Vehicle Down Payment", "Target ₹1,50,000 • 12 Mos", 3)
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                starterTemplates.forEach { (name, desc, idx) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAddTemplateGoal(idx) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(text = desc, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Button(
                                onClick = { onAddTemplateGoal(idx) },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("+ Add", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            OutlinedButton(
                onClick = onAddGoalClick,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Create Custom Savings Jar", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AddEditSavingsGoalDialog(
    initialGoal: SavingsGoalEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (title: String, targetAmount: Double, initialAmount: Double, deadlineDays: Int, colorHex: String, emoji: String, note: String) -> Unit
) {
    var title by remember { mutableStateOf(initialGoal?.title ?: "") }
    var targetAmountStr by remember { mutableStateOf(initialGoal?.targetAmount?.let { if (it > 0) it.toInt().toString() else "" } ?: "") }
    var initialAmountStr by remember { mutableStateOf(initialGoal?.currentAmount?.let { if (it > 0) it.toInt().toString() else "" } ?: "") }
    var note by remember { mutableStateOf(initialGoal?.note ?: "") }
    var selectedEmoji by remember { mutableStateOf(initialGoal?.emoji ?: "🎯") }
    var selectedColor by remember { mutableStateOf(initialGoal?.colorHex ?: "#10B981") }
    var selectedDeadlineMonths by remember { mutableStateOf(if (initialGoal != null && initialGoal.targetDate > System.currentTimeMillis()) 6 else 0) }

    val emojis = listOf("🎯", "🏖️", "🚗", "🏠", "💻", "🛡️", "💍", "🎓", "👶", "✈️", "🎁", "🚀")
    val colors = listOf("#10B981", "#3B82F6", "#8B5CF6", "#EC4899", "#F59E0B", "#EF4444", "#06B6D4")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialGoal == null) "New Savings Goal Jar 🎯" else "Edit Savings Goal",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Goal Title (e.g. Emergency Fund)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = targetAmountStr,
                        onValueChange = { targetAmountStr = it },
                        label = { Text("Target (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = initialAmountStr,
                        onValueChange = { initialAmountStr = it },
                        label = { Text("Current (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Emoji Picker
                Text(text = "Jar Emoji", style = MaterialTheme.typography.labelSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(emojis) { emoji ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedEmoji == emoji) PrimaryBlue.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { selectedEmoji = emoji },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 20.sp)
                        }
                    }
                }

                // Color Picker
                Text(text = "Jar Accent Color", style = MaterialTheme.typography.labelSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(colors) { hex ->
                        val c = remember(hex) { Color(android.graphics.Color.parseColor(hex)) }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(c)
                                .clickable { selectedColor = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColor == hex) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(Color.White, CircleShape)
                                )
                            }
                        }
                    }
                }

                // Target Deadline Duration
                Text(text = "Target Timeframe", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0 to "None", 3 to "3 Mos", 6 to "6 Mos", 12 to "1 Yr", 24 to "2 Yrs").forEach { (months, label) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedDeadlineMonths == months) PrimaryBlue else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { selectedDeadlineMonths = months }
                        ) {
                            Text(
                                text = label,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedDeadlineMonths == months) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note / Motivation (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val target = targetAmountStr.toDoubleOrNull() ?: 0.0
                    val current = initialAmountStr.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && target > 0) {
                        val days = selectedDeadlineMonths * 30
                        onConfirm(title, target, current, days, selectedColor, selectedEmoji, note)
                    }
                },
                enabled = title.isNotBlank() && (targetAmountStr.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text(if (initialGoal == null) "Create Jar" else "Save Changes")
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
fun DepositWithdrawDialog(
    goal: SavingsGoalEntity,
    isDeposit: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, recordInLedger: Boolean) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    var recordInLedger by remember { mutableStateOf(true) }
    val quickChips = listOf(500.0, 1000.0, 2000.0, 5000.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isDeposit) "Deposit to ${goal.title} 💰" else "Withdraw from ${goal.title} 💸",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Current balance: ₹${String.format(Locale.getDefault(), "%,.0f", goal.currentAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickChips.forEach { chipAmount ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    val currentVal = amountStr.toDoubleOrNull() ?: 0.0
                                    amountStr = (currentVal + chipAmount).toInt().toString()
                                }
                        ) {
                            Text(
                                text = "+₹${chipAmount.toInt()}",
                                modifier = Modifier.padding(vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { recordInLedger = !recordInLedger }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.Checkbox(
                        checked = recordInLedger,
                        onCheckedChange = { recordInLedger = it }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isDeposit) "Record in ledger as Investment" else "Record in ledger as Income",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    if (amount > 0) {
                        onConfirm(amount, recordInLedger)
                    }
                },
                enabled = (amountStr.toDoubleOrNull() ?: 0.0) > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDeposit) GreenSuccess else MaterialTheme.colorScheme.error
                )
            ) {
                Text(if (isDeposit) "Confirm Deposit" else "Confirm Withdraw")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
