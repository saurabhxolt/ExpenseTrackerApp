package com.expensetracker.app.features.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensetracker.app.core.database.entity.TransactionEntity
import com.expensetracker.app.core.ui.theme.DarkCard
import com.expensetracker.app.core.ui.theme.GreenSuccess
import com.expensetracker.app.core.ui.theme.PrimaryBlue
import com.expensetracker.app.core.ui.theme.RedExpense
import com.expensetracker.app.core.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionsRoute(
    viewModel: TransactionsViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    TransactionsScreen(
        uiState = uiState,
        onFilterSelected = { viewModel.setFilter(it) },
        onSearchQueryChange = { viewModel.setSearchQuery(it) },
        onDeleteTransaction = { viewModel.deleteTransaction(it) }
    )
}

@Composable
fun TransactionsScreen(
    uiState: TransactionsUiState,
    onFilterSelected: (String) -> Unit = {},
    onSearchQueryChange: (String) -> Unit = {},
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
                placeholder = { Text("Search by merchant or category...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Filter Pills
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterButton("All", "ALL", uiState.selectedFilter) { onFilterSelected("ALL") }
                FilterButton("Expenses", "DEBIT", uiState.selectedFilter) { onFilterSelected("DEBIT") }
                FilterButton("Income", "CREDIT", uiState.selectedFilter) { onFilterSelected("CREDIT") }
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
                    onDelete = { onDeleteTransaction(transaction) }
                )
            }
        }
    }
}

@Composable
fun FilterButton(label: String, value: String, selectedValue: String, onClick: () -> Unit) {
    val isSelected = value == selectedValue
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) PrimaryBlue else DarkCard
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Text(text = label, fontSize = 13.sp)
    }
}

@Composable
fun TransactionHistoryItem(transaction: TransactionEntity, onDelete: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val dateStr = remember(transaction.timestamp) { dateFormat.format(Date(transaction.timestamp)) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.merchant,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${transaction.category} • $dateStr",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${if (transaction.type == "CREDIT") "+" else "-"}₹${String.format("%.2f", transaction.amount)}",
                    fontWeight = FontWeight.Bold,
                    color = if (transaction.type == "CREDIT") GreenSuccess else RedExpense,
                    fontSize = 16.sp
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = TextSecondary.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
