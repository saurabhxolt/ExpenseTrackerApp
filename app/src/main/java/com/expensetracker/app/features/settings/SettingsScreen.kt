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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
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
import com.expensetracker.app.core.promotions.Announcement
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
                        Text("Privacy-First Guarantee 🔒", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            text = "100% Offline • Zero Company Storage • SQLCipher Hardware Encryption",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Ecosystem Announcements Section
        if (uiState.announcements.isNotEmpty()) {
            item {
                Text("App Announcements & Release Notes", style = MaterialTheme.typography.titleLarge)
            }
            items(uiState.announcements) { ann ->
                AnnouncementCard(announcement = ann)
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
            Text("Security", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
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
                            Text("Biometric / PIN App Lock", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            Text("Require fingerprint or PIN on app open", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

        // Data Backup & Export Section
        item {
            Text("Backup & Export", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
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

        // App Information & About Us Section
        item {
            Text("About VaultExpense & ArivTek LLP", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = PrimaryBlue)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("VaultExpense", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("Private. Smart. Offline Expense Vault.", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PrimaryBlue)
                            Text("Version 1.0.0 • 100% Offline & SQLCipher Encrypted", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Total Records in Local DB: ${uiState.transactionCount} Transactions, ${uiState.budgetCount} Budgets",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }

        item {
            ArivTekAboutUsCard()
        }
    }
}

@Composable
fun ArivTekAboutUsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(PrimaryBlue.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = PrimaryBlue)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Designed & Developed by ArivTek LLP",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Building the Next Era of Digital Ventures",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "ArivTek LLP was established to bridge the gap between premium enterprise consulting and the agile product building of a startup. Under a single parent ecosystem, we act as both a technical execution studio for corporate clients and an active builder of tech ventures.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )

            Text(
                text = "Our Mission",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "To empower companies and startups to launch reliable, secure, and highly scalable technologies without the friction of fractured engineering coordination. We make elite software execution accessible.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )

            Text(
                text = "Our Vision",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "To become a globally recognized venture incubator, driving technical innovations and building an interconnected digital suite of software startups that enhance MSME and enterprise workflows.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )

            Text(
                text = "Core Values - The Principles Guarding Our Code",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            val values = listOf(
                "🚀 Innovation" to "Constantly challenging boundaries to invent and re-engineer digital frameworks.",
                "✨ Simplicity" to "Distilling complex computational backends into intuitive interfaces.",
                "🛡️ Transparency" to "Uncompromising open logs, clear pricing schemas, and direct technical consultations.",
                "📈 Customer Success" to "Aligning our service outputs directly with our clients balance sheet metrics.",
                "💡 Continuous Learning" to "Dedicating engineering hours to research emerging technologies, LLMs, and rust frameworks."
            )

            values.forEach { (title, desc) ->
                Column(modifier = Modifier.padding(start = 4.dp)) {
                    Text(title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                    Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun AnnouncementCard(announcement: Announcement) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (announcement.actionUrl.isNotBlank()) {
                    try {
                        val formattedUrl = if (!announcement.actionUrl.startsWith("http://") && !announcement.actionUrl.startsWith("https://")) {
                            "https://${announcement.actionUrl}"
                        } else {
                            announcement.actionUrl
                        }
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(formattedUrl)).apply {
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
                imageVector = Icons.Default.Campaign,
                contentDescription = null,
                tint = PrimaryBlue,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(announcement.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(2.dp))
                Text(announcement.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
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
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Export error", Toast.LENGTH_SHORT).show()
    }
}
