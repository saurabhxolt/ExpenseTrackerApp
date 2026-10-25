package com.expensetracker.app.features.split

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensetracker.app.core.analysis.ItemizedSplitCalculator
import com.expensetracker.app.core.analysis.ItemizedSplitCalculator.ItemizedDish
import com.expensetracker.app.core.ui.theme.GreenSuccess
import com.expensetracker.app.core.ui.theme.PrimaryBlue
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ItemizedBillSplitterDialog(
    initialMembers: List<String> = emptyList(),
    groupName: String? = null,
    onDismiss: () -> Unit,
    onImportAsExpense: ((title: String, grandTotal: Double, paidBy: String, customShares: Map<String, Double>) -> Unit)? = null
) {
    val context = LocalContext.current

    var billTitle by remember { mutableStateOf(if (groupName != null) "$groupName Dinner" else "Restaurant Bill") }
    val members = remember {
        mutableStateListOf<String>().apply {
            if (initialMembers.isNotEmpty()) addAll(initialMembers)
            else addAll(listOf("You", "Friend 1", "Friend 2"))
        }
    }
    var newMemberName by remember { mutableStateOf("") }

    val dishes = remember { mutableStateListOf<ItemizedDish>() }
    var currentDishName by remember { mutableStateOf("") }
    var currentDishPriceStr by remember { mutableStateOf("") }
    val currentDishConsumers = remember { mutableStateListOf<String>() }

    var taxPercentageStr by remember { mutableStateOf("5") } // Default 5% GST
    var tipPercentageStr by remember { mutableStateOf("0") }
    var paidByMember by remember { mutableStateOf(members.firstOrNull() ?: "You") }
    var paidByDropdownExpanded by remember { mutableStateOf(false) }

    // Real-time calculation
    val taxPct = taxPercentageStr.toDoubleOrNull() ?: 0.0
    val tipPct = tipPercentageStr.toDoubleOrNull() ?: 0.0

    val splitResult = remember(dishes.toList(), members.toList(), taxPct, tipPct) {
        ItemizedSplitCalculator.calculateSplit(
            dishes = dishes,
            allMembers = members,
            taxPercentage = taxPct,
            tipPercentage = tipPct
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🍕", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Itemized Bill Splitter", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Proportional Tax & Tip Calculation", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Bill Title & Paid By
                item {
                    OutlinedTextField(
                        value = billTitle,
                        onValueChange = { billTitle = it },
                        label = { Text("Bill / Restaurant Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 2. Members Management Row
                item {
                    Text("Diners / Group Members (${members.size})", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        members.forEach { m ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = m,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newMemberName,
                            onValueChange = { newMemberName = it },
                            label = { Text("Add diner name") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                if (newMemberName.isNotBlank() && !members.contains(newMemberName.trim())) {
                                    members.add(newMemberName.trim())
                                    newMemberName = ""
                                }
                            },
                            enabled = newMemberName.isNotBlank(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("+ Add")
                        }
                    }
                }

                // 3. Add Dish Section
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Add Item / Dish", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = currentDishName,
                                    onValueChange = { currentDishName = it },
                                    label = { Text("Dish (e.g. Pasta)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1.3f)
                                )
                                OutlinedTextField(
                                    value = currentDishPriceStr,
                                    onValueChange = { currentDishPriceStr = it },
                                    label = { Text("Price (₹)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Text("Who shared this dish? (empty = all)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                members.forEach { m ->
                                    val isSelected = currentDishConsumers.contains(m)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            if (isSelected) currentDishConsumers.remove(m)
                                            else currentDishConsumers.add(m)
                                        },
                                        label = { Text(m, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = PrimaryBlue.copy(alpha = 0.2f),
                                            selectedLabelColor = PrimaryBlue
                                        )
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    val price = currentDishPriceStr.toDoubleOrNull() ?: 0.0
                                    if (currentDishName.isNotBlank() && price > 0) {
                                        dishes.add(
                                            ItemizedDish(
                                                name = currentDishName.trim(),
                                                price = price,
                                                consumers = currentDishConsumers.toSet()
                                            )
                                        )
                                        currentDishName = ""
                                        currentDishPriceStr = ""
                                        currentDishConsumers.clear()
                                    }
                                },
                                enabled = currentDishName.isNotBlank() && (currentDishPriceStr.toDoubleOrNull() ?: 0.0) > 0,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Dish to Bill")
                            }
                        }
                    }
                }

                // 4. Added Dishes List
                if (dishes.isNotEmpty()) {
                    item {
                        Text("Dishes on Bill (${dishes.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    }

                    items(dishes) { dish ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(dish.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    val consumerDesc = if (dish.consumers.isEmpty()) "Shared by all" else "Shared by: ${dish.consumers.joinToString(", ")}"
                                    Text(consumerDesc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("₹${String.format("%.2f", dish.price)}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PrimaryBlue)
                                IconButton(onClick = { dishes.remove(dish) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                // 5. Tax & Tip Rates
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = taxPercentageStr,
                            onValueChange = { taxPercentageStr = it },
                            label = { Text("Tax / GST (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = tipPercentageStr,
                            onValueChange = { tipPercentageStr = it },
                            label = { Text("Tip / Service (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 6. Paid By Dropdown
                item {
                    ExposedDropdownMenuBox(
                        expanded = paidByDropdownExpanded,
                        onExpandedChange = { paidByDropdownExpanded = !paidByDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = "Paid By: $paidByMember",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = paidByDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = paidByDropdownExpanded,
                            onDismissRequest = { paidByDropdownExpanded = false }
                        ) {
                            members.forEach { m ->
                                DropdownMenuItem(
                                    text = { Text(m) },
                                    onClick = {
                                        paidByMember = m
                                        paidByDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // 7. Live Summary Results Table
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Subtotal (Dishes):", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("₹${String.format(Locale.getDefault(), "%,.2f", splitResult.subtotal)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Proportional Tax ($taxPct%):", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("+₹${String.format(Locale.getDefault(), "%,.2f", splitResult.taxAmount)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            if (splitResult.tipAmount > 0) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Tip / Service Charge ($tipPct%):", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("+₹${String.format(Locale.getDefault(), "%,.2f", splitResult.tipAmount)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Grand Total:", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("₹${String.format(Locale.getDefault(), "%,.2f", splitResult.grandTotal)}", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = PrimaryBlue)
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Fair Proportional Breakdown:", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                            splitResult.breakdown.forEach { p ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("• ${p.personName}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    Text("₹${String.format(Locale.getDefault(), "%,.2f", p.totalAmount)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GreenSuccess)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        val report = ItemizedSplitCalculator.formatWhatsAppReport(billTitle, splitResult, paidByMember)
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        val clip = ClipData.newPlainText("Itemized Bill Split", report)
                        clipboard?.setPrimaryClip(clip)
                        Toast.makeText(context, "Copied WhatsApp Bill Breakdown! 🍕", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy Split")
                }

                if (onImportAsExpense != null && splitResult.grandTotal > 0) {
                    Button(
                        onClick = {
                            val customShares = splitResult.breakdown.associate { it.personName to it.totalAmount }
                            onImportAsExpense(billTitle, splitResult.grandTotal, paidByMember, customShares)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Text("Add to Ledger")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
