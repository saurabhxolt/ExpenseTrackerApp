package com.expensetracker.app.features.dashboard

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensetracker.app.core.ui.theme.DarkCard
import com.expensetracker.app.core.ui.theme.GreenSuccess
import com.expensetracker.app.core.ui.theme.RedExpense
import com.expensetracker.app.core.ui.theme.TextPrimary
import com.expensetracker.app.core.ui.theme.TextSecondary

@Composable
fun DashboardScreen(
    totalExpenses: Double = 14250.00,
    totalIncome: Double = 45000.00
) {
    val netBalance = totalIncome - totalExpenses

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Dashboard",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Net Balance Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Net Balance",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "₹${String.format("%.2f", netBalance)}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }
        }

        // Income vs Expense Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Income Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Income", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₹${String.format("%.2f", totalIncome)}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = GreenSuccess
                        )
                    }
                }

                // Expense Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Expenses", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₹${String.format("%.2f", totalExpenses)}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = RedExpense
                        )
                    }
                }
            }
        }

        // Section Title
        item {
            Text(
                text = "Recent Transactions",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        // Sample Transactions
        item {
            TransactionItem(merchant = "Swiggy", amount = 450.00, type = "DEBIT", category = "Food & Dining")
        }
        item {
            TransactionItem(merchant = "Salary Credit", amount = 45000.00, type = "CREDIT", category = "Income")
        }
        item {
            TransactionItem(merchant = "Uber Ride", amount = 230.00, type = "DEBIT", category = "Transportation")
        }
    }
}

@Composable
fun TransactionItem(merchant: String, amount: Double, type: String, category: String) {
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
            Column {
                Text(text = merchant, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(text = category, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                text = "${if (type == "CREDIT") "+" else "-"}₹${String.format("%.2f", amount)}",
                fontWeight = FontWeight.Bold,
                color = if (type == "CREDIT") GreenSuccess else RedExpense,
                fontSize = 16.sp
            )
        }
    }
}
