package com.expensetracker.app.features.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.expensetracker.app.core.promotions.Promotion
import com.expensetracker.app.core.ui.theme.DarkCard
import com.expensetracker.app.core.ui.theme.GreenSuccess
import com.expensetracker.app.core.ui.theme.PrimaryBlue
import com.expensetracker.app.core.ui.theme.TextPrimary
import com.expensetracker.app.core.ui.theme.TextSecondary
import com.expensetracker.app.features.categories.CategoriesRoute
import com.expensetracker.app.features.categories.CategoriesViewModel
import com.expensetracker.app.features.subscriptions.SubscriptionsRoute
import com.expensetracker.app.features.subscriptions.SubscriptionsViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var subScreen by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadInitialState(context)
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val jsonStr = inputStream?.bufferedReader()?.use { reader -> reader.readText() } ?: ""
                if (jsonStr.isNotBlank()) {
                    viewModel.restoreBackupJson(jsonStr)
                    Toast.makeText(context, "Backup restored successfully!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to restore backup", Toast.LENGTH_SHORT).show()
            }
        }
    }

    when (subScreen) {
        "CATEGORIES" -> {
            val categoriesVm: CategoriesViewModel = hiltViewModel()
            CategoriesRoute(viewModel = categoriesVm)
        }
        "SUBSCRIPTIONS" -> {
            val subscriptionsVm: SubscriptionsViewModel = hiltViewModel()
            SubscriptionsRoute(viewModel = subscriptionsVm)
        }
        else -> {
            SettingsScreen(
                uiState = uiState,
                onToggleBiometric = { viewModel.toggleBiometric(context, it) },
                onExportCsv = {
                    scope.launch {
                        val csv = viewModel.exportCsvData()
                        shareTextFile(context, csv, "expense_report.csv", "text/csv")
                    }
                },
                onExportBackup = {
                    scope.launch {
                        val json = viewModel.exportBackupJson()
                        shareTextFile(context, json, "expense_backup.json", "application/json")
                    }
                },
                onImportBackup = {
                    importBackupLauncher.launch("*/*")
                },
                onOpenCategories = { subScreen = "CATEGORIES" },
                onOpenSubscriptions = { subScreen = "SUBSCRIPTIONS" }
            )
        }
    }
}

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onToggleBiometric: (Boolean) -> Unit = {},
    onExportCsv: () -> Unit = {},
    onExportBackup: () -> Unit = {},
    onImportBackup: () -> Unit = {},
    onOpenCategories: () -> Unit = {},
    onOpenSubscriptions: () -> Unit = {}
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
                text = "Settings & Security",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Privacy Badge Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Privacy-First Guarantee 🔒", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                        Text(
                            text = "100% Offline • Zero Company Storage • SQLCipher Hardware Encryption",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // Management Section
        item {
            Text("Management", style = MaterialTheme.typography.titleLarge)
        }

        item {
            SettingsActionItem(
                icon = Icons.Default.Category,
                title = "Custom Category Manager",
                subtitle = "Add, edit, or remove expense & income categories",
                onClick = onOpenCategories
            )
        }

        item {
            SettingsActionItem(
                icon = Icons.Default.Autorenew,
                title = "Recurring Mandates & Subscriptions",
                subtitle = "Track monthly autopays, rent, and subscriptions",
                onClick = onOpenSubscriptions
            )
        }

        // Security Section
        item {
            Text("Security", style = MaterialTheme.typography.titleLarge)
        }

        item {
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Fingerprint, contentDescription = null, tint = PrimaryBlue)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Biometric / PIN App Lock", fontWeight = FontWeight.SemiBold)
                            Text("Require fingerprint or PIN on app open", fontSize = 12.sp, color = TextSecondary)
                        }
                    }
                    Switch(
                        checked = uiState.isBiometricEnabled,
                        onCheckedChange = onToggleBiometric,
                        colors = SwitchDefaults.colors(checkedThumbColor = PrimaryBlue)
                    )
                }
            }
        }

        // Ecosystem Features & In-House Announcements
        if (uiState.promotions.isNotEmpty()) {
            item {
                Text("Ecosystem Announcements", style = MaterialTheme.typography.titleLarge)
            }
            items(uiState.promotions) { promo ->
                PromotionCard(promo = promo)
            }
        }

        // Data Backup & Export Section
        item {
            Text("Backup & Export", style = MaterialTheme.typography.titleLarge)
        }

        item {
            SettingsActionItem(
                icon = Icons.Default.Share,
                title = "Export CSV Spreadsheet Report",
                subtitle = "Export transaction history to CSV format for tax records",
                onClick = onExportCsv
            )
        }

        item {
            SettingsActionItem(
                icon = Icons.Default.Download,
                title = "Export Encrypted Backup JSON",
                subtitle = "Save a full backup of your transactions, categories, and budgets",
                onClick = onExportBackup
            )
        }

        item {
            SettingsActionItem(
                icon = Icons.Default.Upload,
                title = "Restore Backup JSON File",
                subtitle = "Import and restore a previously saved backup file",
                onClick = onImportBackup
            )
        }

        // App Information Section
        item {
            Text("App Info", style = MaterialTheme.typography.titleLarge)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = TextSecondary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Expense Tracker App", fontWeight = FontWeight.Bold)
                            Text("Version 1.0.0 (Privacy-First In-House System)", fontSize = 12.sp, color = TextSecondary)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Total Records in Local DB: ${uiState.transactionCount} Transactions, ${uiState.budgetCount} Budgets",
                        fontSize = 12.sp,
                        color = GreenSuccess
                    )
                }
            }
        }
    }
}

@Composable
fun PromotionCard(promo: Promotion) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (promo.actionUrl.isNotBlank()) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(promo.actionUrl)).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Cannot open link", Toast.LENGTH_SHORT).show()
                    }
                }
            },
        colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.RocketLaunch,
                contentDescription = null,
                tint = PrimaryBlue,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(promo.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                Spacer(modifier = Modifier.height(2.dp))
                Text(promo.description, fontSize = 12.sp, color = TextSecondary)
            }
        }
    }
}

@Composable
fun SettingsActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = PrimaryBlue)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(subtitle, fontSize = 12.sp, color = TextSecondary)
            }
        }
    }
}

private fun shareTextFile(context: Context, content: String, fileName: String, mimeType: String) {
    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_TEXT, content)
            putExtra(Intent.EXTRA_SUBJECT, fileName)
        }
        val chooser = Intent.createChooser(intent, "Export $fileName")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (e: Exception) {
        Toast.makeText(context, "Export error", Toast.LENGTH_SHORT).show()
    }
}
