package com.expensetracker.app.features.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import com.expensetracker.app.core.ui.theme.DarkCard
import com.expensetracker.app.core.ui.theme.PrimaryBlue
import com.expensetracker.app.core.ui.theme.TextSecondary
import com.expensetracker.app.features.analytics.AnalyticsRoute
import com.expensetracker.app.features.analytics.AnalyticsViewModel
import com.expensetracker.app.features.budgets.BudgetsRoute
import com.expensetracker.app.features.budgets.BudgetsViewModel
import com.expensetracker.app.features.dashboard.DashboardRoute
import com.expensetracker.app.features.dashboard.DashboardViewModel
import com.expensetracker.app.features.settings.SettingsRoute
import com.expensetracker.app.features.settings.SettingsViewModel
import com.expensetracker.app.features.transactions.TransactionsRoute
import com.expensetracker.app.features.transactions.TransactionsViewModel

data class NavItem(
    val title: String,
    val icon: ImageVector
)

@Composable
fun MainScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }

    val navItems = listOf(
        NavItem("Dashboard", Icons.Default.Dashboard),
        NavItem("Transactions", Icons.Default.ReceiptLong),
        NavItem("Budgets", Icons.Default.AccountBalanceWallet),
        NavItem("Analytics", Icons.Default.PieChart),
        NavItem("Settings", Icons.Default.Settings)
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = DarkCard
            ) {
                navItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(imageVector = item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryBlue,
                            selectedTextColor = PrimaryBlue,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = PrimaryBlue.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> {
                    val dashboardVm: DashboardViewModel = hiltViewModel()
                    DashboardRoute(viewModel = dashboardVm)
                }
                1 -> {
                    val transactionsVm: TransactionsViewModel = hiltViewModel()
                    TransactionsRoute(viewModel = transactionsVm)
                }
                2 -> {
                    val budgetsVm: BudgetsViewModel = hiltViewModel()
                    BudgetsRoute(viewModel = budgetsVm)
                }
                3 -> {
                    val analyticsVm: AnalyticsViewModel = hiltViewModel()
                    AnalyticsRoute(viewModel = analyticsVm)
                }
                4 -> {
                    val settingsVm: SettingsViewModel = hiltViewModel()
                    SettingsRoute(viewModel = settingsVm)
                }
            }
        }
    }
}
