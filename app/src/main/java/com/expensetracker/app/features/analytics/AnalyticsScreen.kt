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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.expensetracker.app.core.ui.theme.CategoryColorProvider
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
        onCustomDateRangeSelected = viewModel::onCustomDateRangeSelected
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    uiState: AnalyticsUiState,
    onPeriodPresetSelected: (String) -> Unit = {},
    onMonthYearSelected: (String) -> Unit = {},
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Financial Analytics",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                var periodExpanded by remember { mutableStateOf(false) }

                Box {
                    Button(
                        onClick = { periodExpanded = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        val label = when {
                            uiState.selectedMonthYear != "ALL" -> "🗓️ ${uiState.selectedMonthYear}"
                            uiState.selectedPeriodPreset == "THIS_MONTH" -> "📅 Current Month"
                            uiState.selectedPeriodPreset == "LAST_MONTH" -> "🗓️ Last Month"
                            uiState.selectedPeriodPreset == "LAST_3_MONTHS" -> "📊 Quarter"
                            uiState.selectedPeriodPreset == "LAST_6_MONTHS" -> "📈 6 Months"
                            uiState.selectedPeriodPreset == "YEARLY" -> "📆 Yearly"
                            uiState.selectedPeriodPreset == "CUSTOM" -> "⏱️ Custom Dates"
                            else -> "📅 Period: All Time"
                        }
                        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    }

                    DropdownMenu(
                        expanded = periodExpanded,
                        onDismissRequest = { periodExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("📅 All Time", color = MaterialTheme.colorScheme.onSurface) },
                            onClick = {
                                onMonthYearSelected("ALL")
                                onPeriodPresetSelected("ALL")
                                periodExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("📅 Current Month", color = MaterialTheme.colorScheme.onSurface) },
                            onClick = {
                                onMonthYearSelected("ALL")
                                onPeriodPresetSelected("THIS_MONTH")
                                periodExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("🗓️ Last Month", color = MaterialTheme.colorScheme.onSurface) },
                            onClick = {
                                onMonthYearSelected("ALL")
                                onPeriodPresetSelected("LAST_MONTH")
                                periodExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("📊 Quarter (Last 3 Months)", color = MaterialTheme.colorScheme.onSurface) },
                            onClick = {
                                onMonthYearSelected("ALL")
                                onPeriodPresetSelected("LAST_3_MONTHS")
                                periodExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("📈 Last 6 Months", color = MaterialTheme.colorScheme.onSurface) },
                            onClick = {
                                onMonthYearSelected("ALL")
                                onPeriodPresetSelected("LAST_6_MONTHS")
                                periodExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("📆 Yearly", color = MaterialTheme.colorScheme.onSurface) },
                            onClick = {
                                onMonthYearSelected("ALL")
                                onPeriodPresetSelected("YEARLY")
                                periodExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("⏱️ Custom Dates...", color = MaterialTheme.colorScheme.onSurface) },
                            onClick = {
                                periodExpanded = false
                                showCustomDatePicker = true
                            }
                        )
                        if (uiState.monthYearOptions.size > 1) {
                            uiState.monthYearOptions.filter { it != "ALL" }.forEach { monthOpt ->
                                DropdownMenuItem(
                                    text = { Text("🗓️ $monthOpt", color = MaterialTheme.colorScheme.onSurface) },
                                    onClick = {
                                        onPeriodPresetSelected("ALL")
                                        onMonthYearSelected(monthOpt)
                                        periodExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Visual Canvas Donut Chart Card
        if (uiState.categoryShares.isNotEmpty() && uiState.totalExpenses > 0) {
            item {
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
                            text = "Category Distribution Donut Chart 🎨",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.align(Alignment.Start)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

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
                                    text = "Total Spent",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "₹${String.format("%.0f", uiState.totalExpenses)}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Legend Items
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            uiState.categoryShares.take(4).forEach { cat ->
                                val catColor = CategoryColorProvider.getCategoryColor(cat.categoryName)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
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

        // Top Category Badge Card
        item {
            val topCategoryColor = CategoryColorProvider.getCategoryColor(uiState.topSpendingCategory)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                        Text(text = "No Expense Data Available", color = MaterialTheme.colorScheme.onSurfaceVariant)
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(categoryColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.categoryName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "₹${String.format("%.2f", item.totalAmount)} (${String.format("%.1f", item.percentage * 100)}%)",
                    fontWeight = FontWeight.Bold,
                    color = categoryColor
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { item.percentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = categoryColor,
                trackColor = categoryColor.copy(alpha = 0.2f),
                strokeCap = StrokeCap.Round
            )
        }
    }
}
