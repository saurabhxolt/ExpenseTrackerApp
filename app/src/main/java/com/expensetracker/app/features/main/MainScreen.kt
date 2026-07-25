package com.expensetracker.app.features.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import com.expensetracker.app.core.ui.theme.DarkCard
import com.expensetracker.app.core.ui.theme.PrimaryBlue
import com.expensetracker.app.core.ui.theme.TextPrimary
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
    val promotions by mainViewModel.promotions.collectAsState()
    val context = LocalContext.current

    val navItems = listOf(
        NavItem("Dashboard", Icons.Default.Dashboard),
        NavItem("Transactions", Icons.Default.ReceiptLong),
        NavItem("Budgets", Icons.Default.AccountBalanceWallet),
        NavItem("Analytics", Icons.Default.PieChart),
        NavItem("Settings", Icons.Default.Settings)
    )

    Scaffold(
        topBar = {
            if (promotions.isNotEmpty()) {
                GlobalHeaderBannerCarousel(
                    promotions = promotions,
                    onPromotionClick = { promo ->
                        openUrlSafely(context, promo.actionUrl)
                    }
                )
            }
        },
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.16f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(8.dp)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) { page ->
                val promo = promotions[page]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .clickable { onPromotionClick(promo) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (promo.imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = promo.imageUrl,
                            contentDescription = promo.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.RocketLaunch,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .wrapContentHeight()
                    ) {
                        Text(
                            text = promo.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = promo.description,
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Indicator Dots for Multiple Ads
            if (promotions.size > 1) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(promotions.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(if (isSelected) 6.dp else 4.dp)
                                .background(
                                    color = if (isSelected) PrimaryBlue else TextSecondary.copy(alpha = 0.4f),
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }
        }
    }
}

fun openUrlSafely(context: Context, url: String) {
    if (url.isBlank()) return
    try {
        val formattedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
            "https://$url"
        } else {
            url
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(formattedUrl)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Cannot open URL: $url", Toast.LENGTH_SHORT).show()
    }
}
