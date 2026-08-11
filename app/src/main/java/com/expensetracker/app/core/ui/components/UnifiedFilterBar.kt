package com.expensetracker.app.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensetracker.app.core.ui.theme.PrimaryBlue

@Composable
fun CollapsibleFilterChip(
    iconEmoji: String,
    label: String,
    selectedOption: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Card(
            modifier = Modifier.clickable { expanded = true },
            colors = CardDefaults.cardColors(
                containerColor = if (selectedOption != "ALL") PrimaryBlue.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (selectedOption != "ALL") PrimaryBlue else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$iconEmoji $label: $selectedOption",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selectedOption != "ALL") PrimaryBlue else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = " ▾",
                    fontSize = 12.sp,
                    color = if (selectedOption != "ALL") PrimaryBlue else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option,
                            fontWeight = if (option == selectedOption) FontWeight.Bold else FontWeight.Normal,
                            color = if (option == selectedOption) PrimaryBlue else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun UnifiedFilterBar(
    periodOption: String,
    periodOptions: List<String>,
    onPeriodSelected: (String) -> Unit,
    bankOption: String? = null,
    bankOptions: List<String> = emptyList(),
    onBankSelected: ((String) -> Unit)? = null,
    categoryOption: String? = null,
    categoryOptions: List<String> = emptyList(),
    onCategorySelected: ((String) -> Unit)? = null,
    typeOption: String? = null,
    typeOptions: List<String> = listOf("ALL", "DEBIT", "CREDIT", "INVESTMENT", "TRANSFER"),
    onTypeSelected: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Period Collapsible Chip
        item {
            CollapsibleFilterChip(
                iconEmoji = "📅",
                label = "Period",
                selectedOption = periodOption,
                options = periodOptions,
                onOptionSelected = onPeriodSelected
            )
        }

        // Bank Collapsible Chip (if provided)
        if (bankOption != null && onBankSelected != null && bankOptions.isNotEmpty()) {
            item {
                CollapsibleFilterChip(
                    iconEmoji = "🏦",
                    label = "Bank",
                    selectedOption = bankOption,
                    options = bankOptions,
                    onOptionSelected = onBankSelected
                )
            }
        }

        // Category Collapsible Chip (if provided)
        if (categoryOption != null && onCategorySelected != null && categoryOptions.isNotEmpty()) {
            item {
                val allCatOptions = if (categoryOptions.firstOrNull() == "ALL") categoryOptions else listOf("ALL") + categoryOptions
                CollapsibleFilterChip(
                    iconEmoji = "🏷️",
                    label = "Category",
                    selectedOption = categoryOption,
                    options = allCatOptions,
                    onOptionSelected = onCategorySelected
                )
            }
        }

        // Type Collapsible Chip (if provided)
        if (typeOption != null && onTypeSelected != null) {
            item {
                CollapsibleFilterChip(
                    iconEmoji = "💳",
                    label = "Type",
                    selectedOption = typeOption,
                    options = typeOptions,
                    onOptionSelected = onTypeSelected
                )
            }
        }
    }
}
