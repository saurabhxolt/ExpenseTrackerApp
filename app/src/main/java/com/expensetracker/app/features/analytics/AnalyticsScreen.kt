package com.expensetracker.app.features.analytics

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensetracker.app.core.ui.theme.CategoryColorProvider
import com.expensetracker.app.core.ui.theme.CategoryEmojiProvider
import com.expensetracker.app.core.ui.theme.PrimaryBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsRoute(
    viewModel: AnalyticsViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    AnalyticsScreen(
        uiState = uiState,
        onPeriodPresetSelected = viewModel::onPeriodPresetSelected,
        onMonthYearSelected = viewModel::onMonthYearSelected,
        onTransactionTypeSelected = viewModel::onTransactionTypeSelected,
        onCustomDateRangeSelected = viewModel::onCustomDateRangeSelected
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    uiState: AnalyticsUiState,
    onPeriodPresetSelected: (String) -> Unit = {},
    onMonthYearSelected: (String) -> Unit = {},
    onTransactionTypeSelected: (String) -> Unit = {},
    onCustomDateRangeSelected: (startMs: Long?, endMs: Long?) -> Unit = { _, _ -> }
) {
    var showCustomDatePicker by remember { mutableStateOf(false) }

    if (showCustomDatePicker) {
        com.expensetracker.app.features.transactions.CustomDateRangePickerDialog(
            onDismiss = { showCustomDatePicker = false },
            onConfirm = { startMs, endMs ->
                onCustomDateRangeSelected(startMs, endMs)
                showCustomDatePicker = false
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Financial Analytics 📊",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Collapsible Unified Filter Bar for Time Period
        item {
            com.expensetracker.app.core.ui.components.UnifiedFilterBar(
                periodOption = when {
                    uiState.selectedMonthYear != "ALL" -> uiState.selectedMonthYear
                    uiState.selectedPeriodPreset == "THIS_MONTH" -> "This Month"
                    uiState.selectedPeriodPreset == "LAST_MONTH" -> "Last Month"
                    uiState.selectedPeriodPreset == "LAST_3_MONTHS" -> "Last 3 Months"
                    uiState.selectedPeriodPreset == "LAST_6_MONTHS" -> "Last 6 Months"
                    uiState.selectedPeriodPreset == "YEARLY" -> "This Year"
                    uiState.selectedPeriodPreset == "CUSTOM" -> "Custom Range"
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
                    onMonthYearSelected("ALL")
                    onPeriodPresetSelected(preset)
                }
            )
        }

        // Transaction Type Selector Bar for Donut Chart & Breakdown
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Breakdown by Transaction Type:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val typeOptions = listOf(
                    "DEBIT" to "Expenses 💸",
                    "CREDIT" to "Income 💰",
                    "INVESTMENT" to "Investments 📈",
                    "TRANSFER" to "Transfers 🔄",
                    "ALL" to "All Types 🌐"
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(typeOptions) { (typeKey, label) ->
                        val isSelected = uiState.selectedTransactionType == typeKey
                        FilterChip(
                            selected = isSelected,
                            onClick = { onTransactionTypeSelected(typeKey) },
                            label = { Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryBlue,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        // Visual Canvas Donut Chart Card
        item {
            val activeTypeLabel = when (uiState.selectedTransactionType) {
                "DEBIT" -> "Expenses"
                "CREDIT" -> "Income"
                "INVESTMENT" -> "Investments"
                "TRANSFER" -> "Transfers"
                else -> "Total Flow"
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$activeTypeLabel Donut Breakdown 🎨",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (uiState.categoryShares.isNotEmpty() && uiState.activeTypeTotal > 0) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(180.dp)
                        ) {
                            Canvas(modifier = Modifier.size(160.dp)) {
                                var startAngle = -90f
                                uiState.categoryShares.forEach { cat ->
                                    val sweepAngle = cat.percentage * 360f
                                    val color = CategoryColorProvider.getCategoryColor(cat.categoryName)
                                    drawArc(
                                        color = color,
                                        startAngle = startAngle,
                                        sweepAngle = sweepAngle,
                                        useCenter = false,
                                        style = Stroke(width = 36.dp.toPx(), cap = StrokeCap.Butt)
                                    )
                                    startAngle += sweepAngle
                                }
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Total $activeTypeLabel",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "₹${String.format("%.0f", uiState.activeTypeTotal)}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Top Donut Legend Items
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            uiState.categoryShares.take(5).forEach { cat ->
                                val catColor = CategoryColorProvider.getCategoryColor(cat.categoryName)
                                val emoji = CategoryEmojiProvider.getCategoryEmoji(cat.categoryName)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(text = emoji, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(catColor, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = cat.categoryName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "₹${String.format("%.2f", cat.totalAmount)} (${String.format("%.1f", cat.percentage * 100)}%)",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = catColor
                                    )
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No $activeTypeLabel recorded for selected period",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Incomes vs Expenses vs Investments Canvas Bar Chart Card
        item {
            val incomeBarColor = MaterialTheme.colorScheme.tertiary
            val expenseBarColor = MaterialTheme.colorScheme.error
            val investmentBarColor = PrimaryBlue
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Cashflow Bar Comparison 📊",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val maxAmount = remember(uiState.totalIncome, uiState.totalExpenses, uiState.totalInvestments) {
                        maxOf(uiState.totalIncome, uiState.totalExpenses, uiState.totalInvestments, 1.0)
                    }

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    ) {
                        val barWidth = 36.dp.toPx()
                        val canvasHeight = size.height
                        val canvasWidth = size.width

                        // Income Bar (Green)
                        val incomeHeight = (uiState.totalIncome / maxAmount).toFloat() * (canvasHeight - 20.dp.toPx())
                        drawRoundRect(
                            color = incomeBarColor,
                            topLeft = Offset(x = canvasWidth * 0.20f - barWidth / 2, y = canvasHeight - incomeHeight),
                            size = Size(barWidth, incomeHeight),
                            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                        )

                        // Expense Bar (Red)
                        val expenseHeight = (uiState.totalExpenses / maxAmount).toFloat() * (canvasHeight - 20.dp.toPx())
                        drawRoundRect(
                            color = expenseBarColor,
                            topLeft = Offset(x = canvasWidth * 0.50f - barWidth / 2, y = canvasHeight - expenseHeight),
                            size = Size(barWidth, expenseHeight),
                            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                        )

                        // Investment Bar (Blue)
                        val investmentHeight = (uiState.totalInvestments / maxAmount).toFloat() * (canvasHeight - 20.dp.toPx())
                        drawRoundRect(
                            color = investmentBarColor,
                            topLeft = Offset(x = canvasWidth * 0.80f - barWidth / 2, y = canvasHeight - investmentHeight),
                            size = Size(barWidth, investmentHeight),
                            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Incomes: ₹${String.format("%.0f", uiState.totalIncome)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = "Expenses: ₹${String.format("%.0f", uiState.totalExpenses)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Investments: ₹${String.format("%.0f", uiState.totalInvestments)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                    }
                }
            }
        }

        // 🏛️ Indian Tax Season Deductions Organizer
        item {
            TaxSeasonDeductionsCard(taxSummary = uiState.taxSummary)
        }

        // Top Category Badge Card
        item {
            val topCategoryColor = CategoryColorProvider.getCategoryColor(uiState.topSpendingCategory)
            val topEmoji = CategoryEmojiProvider.getCategoryEmoji(uiState.topSpendingCategory)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(topCategoryColor.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = topEmoji, fontSize = 22.sp)
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Top Spending Category",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = uiState.topSpendingCategory,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = topCategoryColor
                        )
                    }
                }
            }
        }

        // Category Leaderboard Section Title
        item {
            Text(
                text = "Category Breakdown Leaderboard",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        if (uiState.categoryShares.isEmpty()) {
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
                        Text(text = "No Transaction Data Available", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(uiState.categoryShares) { item ->
                CategoryBreakdownCard(item = item)
            }
        }
    }
}

@Composable
fun CategoryBreakdownCard(item: CategoryShare) {
    val categoryColor = CategoryColorProvider.getCategoryColor(item.categoryName)
    val emoji = CategoryEmojiProvider.getCategoryEmoji(item.categoryName)

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
                            .background(categoryColor.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = emoji, fontSize = 20.sp)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = item.categoryName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = categoryColor.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "₹${String.format("%.2f", item.totalAmount)} (${String.format("%.1f", item.percentage * 100)}%)",
                        fontWeight = FontWeight.Bold,
                        color = categoryColor,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { item.percentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = categoryColor,
                trackColor = categoryColor.copy(alpha = 0.15f),
                strokeCap = StrokeCap.Round
            )
        }
    }
}
