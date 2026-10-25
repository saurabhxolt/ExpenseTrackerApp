package com.expensetracker.app.features.cards

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.expensetracker.app.core.analysis.CreditCardRadarStatus
import com.expensetracker.app.core.analysis.DiscoveredCreditCard
import com.expensetracker.app.core.analysis.UtilizationTier
import com.expensetracker.app.core.database.entity.CreditCardEntity
import com.expensetracker.app.core.ui.theme.PrimaryBlue

@Composable
fun CreditCardRadarCard(
    uiState: CreditCardRadarUiState,
    onAddCard: (name: String, digits: String, limit: Double, stmtDay: Int, dueDay: Int, bank: String) -> Unit,
    onUpdateCard: (CreditCardEntity) -> Unit,
    onDeleteCard: (CreditCardEntity) -> Unit,
    onOnboardDiscovered: (DiscoveredCreditCard, limit: Double, stmtDay: Int, dueDay: Int) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var cardToEdit by remember { mutableStateOf<CreditCardEntity?>(null) }
    var discoveredToOnboard by remember { mutableStateOf<DiscoveredCreditCard?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
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
                            .background(PrimaryBlue.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("💳", fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Credit Card & CIBIL Radar",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "30% Limit Guard • Grace Period Countdown",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Card", tint = PrimaryBlue)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Auto-Discovered Cards Prompt (if any)
            if (uiState.discoveredCards.isNotEmpty()) {
                val discovered = uiState.discoveredCards.first()
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF59E0B).copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "💡 Found ${discovered.suggestedBank} Card (XX${discovered.last4Digits})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${discovered.detectedDebitsCount} recent debits • Set limit to track 30% utilization",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = { discoveredToOnboard = discovered },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Track", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }
                }
            }

            // Cards Content
            if (uiState.cardStatuses.isEmpty()) {
                // Empty State
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🛡️", fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Protect Your CIBIL Score",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Add your credit cards to track 30% utilization limits and interest-free grace periods.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { showAddDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("+ Add Credit Card", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            } else {
                // Card Carousel
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(uiState.cardStatuses) { _, status ->
                        CreditCardItem(
                            status = status,
                            onEditClick = { cardToEdit = status.card },
                            onDeleteClick = { onDeleteCard(status.card) }
                        )
                    }
                }
            }
        }
    }

    // Add Card Dialog
    if (showAddDialog) {
        CreditCardEditDialog(
            title = "Add Credit Card",
            initialName = "",
            initialDigits = "",
            initialLimit = "100000",
            initialStmtDay = "15",
            initialDueDay = "5",
            initialBank = "HDFC",
            onDismiss = { showAddDialog = false },
            onConfirm = { name, digits, limit, stmtDay, dueDay, bank ->
                onAddCard(name, digits, limit, stmtDay, dueDay, bank)
                showAddDialog = false
            }
        )
    }

    // Edit Card Dialog
    cardToEdit?.let { card ->
        CreditCardEditDialog(
            title = "Edit Credit Card",
            initialName = card.cardName,
            initialDigits = card.last4Digits,
            initialLimit = card.totalLimit.toInt().toString(),
            initialStmtDay = card.statementDayOfMonth.toString(),
            initialDueDay = card.dueDayOfMonth.toString(),
            initialBank = card.bankName,
            onDismiss = { cardToEdit = null },
            onConfirm = { name, digits, limit, stmtDay, dueDay, bank ->
                onUpdateCard(
                    card.copy(
                        cardName = name,
                        last4Digits = digits,
                        totalLimit = limit,
                        statementDayOfMonth = stmtDay,
                        dueDayOfMonth = dueDay,
                        bankName = bank
                    )
                )
                cardToEdit = null
            }
        )
    }

    // Onboard Discovered Card Dialog
    discoveredToOnboard?.let { discovered ->
        CreditCardEditDialog(
            title = "Track Discovered Card",
            initialName = discovered.suggestedName,
            initialDigits = discovered.last4Digits,
            initialLimit = "100000",
            initialStmtDay = "15",
            initialDueDay = "5",
            initialBank = discovered.suggestedBank,
            onDismiss = { discoveredToOnboard = null },
            onConfirm = { _, _, limit, stmtDay, dueDay, _ ->
                onOnboardDiscovered(discovered, limit, stmtDay, dueDay)
                discoveredToOnboard = null
            }
        )
    }
}

@Composable
fun CreditCardItem(
    status: CreditCardRadarStatus,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val card = status.card
    val gradientColors = when (card.bankName.uppercase()) {
        "HDFC" -> listOf(Color(0xFF0D253F), Color(0xFF1E3A8A))
        "SBI" -> listOf(Color(0xFF0F766E), Color(0xFF0284C7))
        "ICICI" -> listOf(Color(0xFF831843), Color(0xFFB91C1C))
        "AXIS" -> listOf(Color(0xFF701A75), Color(0xFF9F1239))
        "ONECARD" -> listOf(Color(0xFF18181B), Color(0xFF27272A))
        else -> listOf(Color(0xFF1E293B), Color(0xFF334155))
    }

    val (tierBadge, tierColor) = when (status.utilizationTier) {
        UtilizationTier.OPTIMAL -> "🛡️ Safe (<30%)" to Color(0xFF10B981)
        UtilizationTier.CAUTION -> "⚠️ Caution (30-50%)" to Color(0xFFF59E0B)
        UtilizationTier.CRITICAL -> "🚨 CIBIL Alert (>50%)" to Color(0xFFEF4444)
    }

    Card(
        modifier = Modifier
            .width(280.dp)
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(Brush.linearGradient(gradientColors))
                .padding(14.dp)
        ) {
            Column {
                // Top row: Bank & Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = card.bankName.uppercase(),
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                    Row {
                        IconButton(onClick = onEditClick, modifier = Modifier.size(26.dp)) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(onClick = onDeleteClick, modifier = Modifier.size(26.dp)) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Card Name & Chip
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("💳", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = card.cardName,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Masked Number & Due Date Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "•••• ${card.last4Digits}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 2.sp
                    )
                    Box(
                        modifier = Modifier
                            .background(tierColor.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                            .border(1.dp, tierColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = tierBadge,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = tierColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Balance & Limit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Outstanding", fontSize = 9.sp, color = Color.White.copy(alpha = 0.7f))
                        Text(
                            text = "₹${status.currentOutstanding.toInt()}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Limit", fontSize = 9.sp, color = Color.White.copy(alpha = 0.7f))
                        Text(
                            text = "₹${card.totalLimit.toInt()}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Utilization Progress Bar
                LinearProgressIndicator(
                    progress = { (status.utilizationPercent / 100.0).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = tierColor,
                    trackColor = Color.White.copy(alpha = 0.2f),
                    strokeCap = StrokeCap.Round
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Grace Period Ticker
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (status.isOverdue) "🚨 Overdue" else "⏳ ${status.graceDaysRemaining}d grace left",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (status.isOverdue) Color(0xFFEF4444) else Color.White.copy(alpha = 0.9f)
                    )
                    Text(
                        text = "Due Day ${card.dueDayOfMonth}",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }

                if (status.safe30RemainingHeadroom > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Safe headroom: ₹${status.safe30RemainingHeadroom.toInt()} before 30%",
                        fontSize = 9.sp,
                        color = Color(0xFF6EE7B7)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Inspect Matched Transactions
                var showTransactions by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showTransactions = !showTransactions }
                        .background(Color.White.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cycle Transactions (${status.matchingTransactions.size})",
                        fontSize = 10.sp,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (showTransactions) "▲ Hide" else "▼ Inspect",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }

                if (showTransactions) {
                    Spacer(modifier = Modifier.height(6.dp))
                    if (status.matchingTransactions.isEmpty()) {
                        val startStr = java.text.SimpleDateFormat("dd MMM", java.util.Locale.getDefault()).format(java.util.Date(status.cycleStartDateMs))
                        Text(
                            text = "No transactions matched yet in this cycle ($startStr – Today). SMS alerts for •••• ${card.last4Digits} or manual expenses linked to this card appear here automatically.",
                            fontSize = 9.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            lineHeight = 13.sp
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            status.matchingTransactions.take(6).forEach { tx ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val txDateStr = java.text.SimpleDateFormat("dd MMM", java.util.Locale.getDefault()).format(java.util.Date(tx.timestamp))
                                    Text(
                                        text = "$txDateStr • ${tx.merchant.take(16)}",
                                        fontSize = 9.sp,
                                        color = Color.White.copy(alpha = 0.9f),
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "₹${tx.amount.toInt()}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (tx.type == "DEBIT") Color.White else Color(0xFF6EE7B7)
                                    )
                                }
                            }
                            if (status.matchingTransactions.size > 6) {
                                Text(
                                    text = "+ ${status.matchingTransactions.size - 6} more transactions in this cycle",
                                    fontSize = 9.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreditCardEditDialog(
    title: String,
    initialName: String,
    initialDigits: String,
    initialLimit: String,
    initialStmtDay: String,
    initialDueDay: String,
    initialBank: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, digits: String, limit: Double, stmtDay: Int, dueDay: Int, bank: String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var digits by remember { mutableStateOf(initialDigits) }
    var limitText by remember { mutableStateOf(initialLimit) }
    var stmtDayText by remember { mutableStateOf(initialStmtDay) }
    var dueDayText by remember { mutableStateOf(initialDueDay) }
    var bank by remember { mutableStateOf(initialBank) }

    val bankPresets = listOf("HDFC", "SBI", "ICICI", "Axis", "Kotak", "OneCard", "BOB", "Amex", "RBL", "IndusInd", "Federal")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(androidx.compose.foundation.rememberScrollState())
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Explanatory helper banner
                Card(
                    colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("💡", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Card details are used 100% offline to calculate 30% CIBIL score utilization and interest-free grace period countdown.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bank Chips
                Text("Select Bank Issuer:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(bankPresets.size) { idx ->
                        val b = bankPresets[idx]
                        val isSelected = bank.equals(b, ignoreCase = true)
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.clickable { bank = b }
                        ) {
                            Text(
                                text = b,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Card Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Card Nickname / Variant") },
                    supportingText = { Text("e.g. HDFC Regalia Gold, Amazon Pay ICICI, Millennia", fontSize = 10.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Last 4 Digits & Limit
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = digits,
                        onValueChange = { if (it.length <= 4) digits = it.filter { c -> c.isDigit() } },
                        label = { Text("Last 4 Digits") },
                        supportingText = { Text("For SMS match", fontSize = 10.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = limitText,
                        onValueChange = { limitText = it.filter { c -> c.isDigit() } },
                        label = { Text("Total Limit (₹)") },
                        supportingText = { Text("For 30% CIBIL guard", fontSize = 10.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Statement Date & Due Date
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = stmtDayText,
                        onValueChange = { stmtDayText = it.filter { c -> c.isDigit() } },
                        label = { Text("Bill Date (1-31)") },
                        supportingText = { Text("Bill generation date", fontSize = 10.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = dueDayText,
                        onValueChange = { dueDayText = it.filter { c -> c.isDigit() } },
                        label = { Text("Due Date (1-31)") },
                        supportingText = { Text("Last day to pay", fontSize = 10.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                    }

                    Button(
                        onClick = {
                            val limit = limitText.toDoubleOrNull() ?: 100000.0
                            val stmtDay = (stmtDayText.toIntOrNull() ?: 15).coerceIn(1, 31)
                            val dueDay = (dueDayText.toIntOrNull() ?: 5).coerceIn(1, 31)
                            onConfirm(name.ifBlank { "$bank Card" }, digits, limit, stmtDay, dueDay, bank)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save Card", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
