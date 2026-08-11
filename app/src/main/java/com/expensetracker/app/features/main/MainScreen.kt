package com.expensetracker.app.features.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.res.painterResource
import com.expensetracker.app.R
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.expensetracker.app.core.promotions.Promotion
import com.expensetracker.app.core.ui.theme.PrimaryBlue
import com.expensetracker.app.features.analytics.AnalyticsRoute
import com.expensetracker.app.features.analytics.AnalyticsViewModel
import com.expensetracker.app.features.budgets.BudgetsRoute
import com.expensetracker.app.features.budgets.BudgetsViewModel
import com.expensetracker.app.features.dashboard.DashboardRoute
import com.expensetracker.app.features.dashboard.DashboardViewModel
import com.expensetracker.app.features.settings.SettingsRoute
import com.expensetracker.app.features.settings.SettingsViewModel
import com.expensetracker.app.features.split.SplitRoute
import com.expensetracker.app.features.split.SplitViewModel
import com.expensetracker.app.features.transactions.TransactionsRoute
import com.expensetracker.app.features.transactions.TransactionsViewModel
import kotlinx.coroutines.delay

data class NavItem(
    val title: String,
    val icon: ImageVector
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    mainViewModel: MainViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var isSettingsOpen by remember { mutableStateOf(false) }
    val promotions by mainViewModel.promotions.collectAsState()
    val context = LocalContext.current
    var backPressedTime by remember { androidx.compose.runtime.mutableLongStateOf(0L) }

    val navItems = listOf(
        NavItem("Dashboard", Icons.Default.Dashboard),
        NavItem("History", Icons.Default.ReceiptLong),
        NavItem("Split Bills", Icons.Default.Group),
        NavItem("Budgets", Icons.Default.AccountBalanceWallet),
        NavItem("Analytics", Icons.Default.PieChart)
    )

    // System Back Gesture Handling Rules:
    // 1. If Settings screen is open, close Settings
    androidx.activity.compose.BackHandler(enabled = isSettingsOpen) {
        isSettingsOpen = false
    }

    // 2. If on a non-Dashboard tab (History, Split Bills, Budgets, Analytics), return to Dashboard
    androidx.activity.compose.BackHandler(enabled = !isSettingsOpen && selectedTab != 0) {
        selectedTab = 0
    }

    // 3. If on Dashboard tab, double-tap back gesture to exit app
    androidx.activity.compose.BackHandler(enabled = !isSettingsOpen && selectedTab == 0) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - backPressedTime < 2000) {
            (context as? android.app.Activity)?.finish()
        } else {
            backPressedTime = currentTime
            Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
        }
    }

    if (isSettingsOpen) {
        Scaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .statusBarsPadding()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    IconButton(onClick = { isSettingsOpen = false }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = PrimaryBlue)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "App Preferences & Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                val settingsVm: SettingsViewModel = hiltViewModel()
                SettingsRoute(viewModel = settingsVm)
            }
        }
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    navItems.forEachIndexed { index, item ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = { Icon(imageVector = item.icon, contentDescription = item.title) },
                            label = {
                                Text(
                                    text = item.title,
                                    maxLines = 1,
                                    softWrap = false,
                                    fontSize = 11.sp,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PrimaryBlue,
                                selectedTextColor = PrimaryBlue,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = PrimaryBlue.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .statusBarsPadding()
            ) {
                // App Name Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_vault_logo),
                            contentDescription = "VaultExpense Logo",
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "VaultExpense",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Private. Smart. Offline Expense Vault.",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryBlue
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { isSettingsOpen = true }) {
                            Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings", tint = PrimaryBlue)
                        }
                    }
                }

                // Global Header Banner Carousel
                if (promotions.isNotEmpty()) {
                    GlobalHeaderBannerCarousel(
                        promotions = promotions,
                        onPromotionClick = { promo ->
                            openUrlSafely(context, promo.actionUrl)
                        }
                    )
                }

                // Main Tab Content Area
                Box(modifier = Modifier.weight(1f)) {
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
                            val splitVm: SplitViewModel = hiltViewModel()
                            SplitRoute(viewModel = splitVm)
                        }
                        3 -> {
                            val budgetsVm: BudgetsViewModel = hiltViewModel()
                            BudgetsRoute(viewModel = budgetsVm)
                        }
                        4 -> {
                            val analyticsVm: AnalyticsViewModel = hiltViewModel()
                            AnalyticsRoute(viewModel = analyticsVm)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlobalHeaderBannerCarousel(
    promotions: List<Promotion>,
    onPromotionClick: (Promotion) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { promotions.size })

    // Auto-rotate ads every 4 seconds
    LaunchedEffect(promotions.size) {
        if (promotions.size > 1) {
            while (true) {
                delay(4000)
                val nextPage = (pagerState.currentPage + 1) % promotions.size
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val promo = promotions[page]
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .clickable { onPromotionClick(promo) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (!promo.imageUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = promo.imageUrl,
                                contentDescription = promo.title,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(PrimaryBlue.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RocketLaunch,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                        }

                        Column(verticalArrangement = Arrangement.Center) {
                            Text(
                                text = promo.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = promo.description,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .background(PrimaryBlue, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Learn More",
                            color = androidx.compose.ui.graphics.Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun openUrlSafely(context: Context, url: String) {
    if (url.isBlank()) return
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
    }
}
