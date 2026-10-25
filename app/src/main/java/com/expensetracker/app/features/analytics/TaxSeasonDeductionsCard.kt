package com.expensetracker.app.features.analytics

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensetracker.app.core.analysis.TaxDeductionAnalyzer
import com.expensetracker.app.core.database.entity.TransactionEntity
import com.expensetracker.app.core.security.LocalPrivacyMode
import com.expensetracker.app.core.ui.theme.GreenSuccess
import com.expensetracker.app.core.ui.theme.PrimaryBlue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TaxSeasonDeductionsCard(
    taxSummary: TaxDeductionAnalyzer.TaxSummary?,
    modifier: Modifier = Modifier
) {
    if (taxSummary == null) return

    val context = LocalContext.current
    val isPrivacyMode = LocalPrivacyMode.current
    var showDetailsDialog by remember { mutableStateOf(false) }

    val total80C = taxSummary.sectionTotals[TaxDeductionAnalyzer.SECTION_80C] ?: 0.0
    val total80D = taxSummary.sectionTotals[TaxDeductionAnalyzer.SECTION_80D] ?: 0.0
    val totalHra = taxSummary.sectionTotals[TaxDeductionAnalyzer.SECTION_10_13A] ?: 0.0

    val progress80C = (total80C / 150000.0).toFloat().coerceIn(0f, 1f)
    val progress80D = (total80D / 25000.0).toFloat().coerceIn(0f, 1f)

    val animated80C by animateFloatAsState(targetValue = progress80C, label = "80c_progress")
    val animated80D by animateFloatAsState(targetValue = progress80D, label = "80d_progress")

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header: Title & FY Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(PrimaryBlue.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🏛️", fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Tax Season Deductions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Indian Income Tax Organizer",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PrimaryBlue.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = taxSummary.financialYearLabel,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Total Claims & Estimated Tax Saved
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Total Exemptions Tracked",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (isPrivacyMode) "₹•••••" else String.format(Locale.getDefault(), "₹%,.0f", taxSummary.totalClaimedAmount),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = GreenSuccess.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (isPrivacyMode) "Save: ₹•••••" else String.format(Locale.getDefault(), "Save: ₹%,.0f", taxSummary.estimatedTaxSavings),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = GreenSuccess
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Section 80C Gauge
            SectionProgressRow(
                sectionName = "Section 80C (PPF, ELSS, LIC, Tuition)",
                currentAmount = total80C,
                limitAmount = 150000.0,
                progress = animated80C,
                isPrivacyMode = isPrivacyMode
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Section 80D Gauge
            SectionProgressRow(
                sectionName = "Section 80D (Health Insurance / Mediclaim)",
                currentAmount = total80D,
                limitAmount = 25000.0,
                progress = animated80D,
                isPrivacyMode = isPrivacyMode
            )

            // Section 10(13A) HRA
            if (totalHra > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Section 10(13A) (HRA / Rent)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isPrivacyMode) "₹•••••" else String.format(Locale.getDefault(), "₹%,.0f Paid", totalHra),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons: View Details & Copy CA Summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { showDetailsDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Inspect", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        val report = TaxDeductionAnalyzer.generateFormattedTaxReport(taxSummary)
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        val clip = ClipData.newPlainText("Tax Deductions Report", report)
                        clipboard?.setPrimaryClip(clip)
                        Toast.makeText(context, "Tax summary copied for CA / ITR! 📋", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showDetailsDialog) {
        TaxDetailsInspectionDialog(
            taxSummary = taxSummary,
            isPrivacyMode = isPrivacyMode,
            onDismiss = { showDetailsDialog = false }
        )
    }
}

@Composable
private fun SectionProgressRow(
    sectionName: String,
    currentAmount: Double,
    limitAmount: Double,
    progress: Float,
    isPrivacyMode: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = sectionName,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            val currentStr = if (isPrivacyMode) "₹•••••" else String.format(Locale.getDefault(), "₹%,.0f", currentAmount)
            val limitStr = String.format(Locale.getDefault(), "₹%,.0f", limitAmount)
            Text(
                text = "$currentStr / $limitStr",
                style = MaterialTheme.typography.labelSmall,
                color = if (currentAmount >= limitAmount) GreenSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = if (currentAmount >= limitAmount) GreenSuccess else PrimaryBlue,
            trackColor = MaterialTheme.colorScheme.surface,
            strokeCap = StrokeCap.Round
        )
    }
}

@Composable
private fun TaxDetailsInspectionDialog(
    taxSummary: TaxDeductionAnalyzer.TaxSummary,
    isPrivacyMode: Boolean,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Tax Deductions Breakdown (${taxSummary.financialYearLabel})",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (taxSummary.sectionTransactions.isEmpty()) {
                    item {
                        Text(
                            text = "No tax-deductible debits detected for this financial year yet. Payments to PPF, ELSS, Insurance, or Rent will be organized here automatically.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    taxSummary.sectionTransactions.forEach { (section, txList) ->
                        item {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = PrimaryBlue.copy(alpha = 0.12f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = TaxDeductionAnalyzer.getSectionDisplayName(section),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryBlue
                                    )
                                    val sum = txList.sumOf { it.amount }
                                    Text(
                                        text = if (isPrivacyMode) "₹•••••" else String.format(Locale.getDefault(), "₹%,.0f", sum),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = PrimaryBlue
                                    )
                                }
                            }
                        }

                        items(txList) { tx ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = tx.merchant,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${dateFormat.format(Date(tx.timestamp))} • ${tx.category}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Text(
                                        text = if (isPrivacyMode) "₹•••••" else String.format(Locale.getDefault(), "₹%,.2f", tx.amount),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
