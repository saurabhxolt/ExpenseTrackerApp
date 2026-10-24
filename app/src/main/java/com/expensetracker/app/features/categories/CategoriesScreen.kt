package com.expensetracker.app.features.categories

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensetracker.app.core.database.entity.CategoryEntity
import com.expensetracker.app.core.ui.theme.CategoryColorProvider
import com.expensetracker.app.core.ui.theme.CategoryEmojiProvider
import com.expensetracker.app.core.ui.theme.GreenSuccess
import com.expensetracker.app.core.ui.theme.PrimaryBlue
import com.expensetracker.app.core.ui.theme.RedExpense

@Composable
fun CategoriesRoute(
    viewModel: CategoriesViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = PrimaryBlue,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Custom Category")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            CategoriesScreen(
                uiState = uiState,
                onEditCategory = { editingCategory = it },
                onDeleteCategory = { viewModel.deleteCategory(it) }
            )

            if (showAddDialog) {
                AddEditCategoryDialog(
                    initialCategory = null,
                    onDismiss = { showAddDialog = false },
                    onConfirm = { name, type, emoji ->
                        viewModel.addCategory(name = name, type = type, userEmoji = emoji)
                        showAddDialog = false
                    }
                )
            }

            editingCategory?.let { category ->
                AddEditCategoryDialog(
                    initialCategory = category,
                    onDismiss = { editingCategory = null },
                    onConfirm = { name, type, emoji ->
                        viewModel.updateCategory(category, newName = name, newType = type, newEmoji = emoji)
                        editingCategory = null
                    }
                )
            }
        }
    }
}

@Composable
fun CategoriesScreen(
    uiState: CategoriesUiState,
    onEditCategory: (CategoryEntity) -> Unit = {},
    onDeleteCategory: (CategoryEntity) -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Category Manager 🏷️",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "All system and custom categories. System auto-assigns non-conflicting colors.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        items(uiState.categories, key = { "${it.id}_${it.name}" }) { category ->
            CategoryItem(
                category = category,
                onEdit = { onEditCategory(category) },
                onDelete = { onDeleteCategory(category) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun CategoryItem(
    category: CategoryEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val categoryColor = remember(category.name, category.colorHex) {
        CategoryColorProvider.getCategoryColor(category.name)
    }
    val emoji = remember(category.name, category.emoji) {
        if (category.emoji.isNotBlank()) category.emoji else CategoryEmojiProvider.getCategoryEmoji(category.name)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(categoryColor.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = emoji, fontSize = 20.sp)
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = category.name,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(categoryColor, CircleShape)
                        )
                    }
                    Text(
                        text = "${category.type}${if (category.isCustom) " • Custom" else " • System"}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Category",
                        tint = PrimaryBlue
                    )
                }

                if (category.isCustom && category.id > 0) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Category",
                            tint = RedExpense
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddEditCategoryDialog(
    initialCategory: CategoryEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: String, emoji: String) -> Unit
) {
    var name by remember { mutableStateOf(initialCategory?.name ?: "") }
    var selectedType by remember { mutableStateOf(initialCategory?.type ?: "EXPENSE") }
    var emojiInput by remember { mutableStateOf(initialCategory?.emoji ?: "") }

    val autoResolvedEmoji = remember(name) {
        if (emojiInput.isNotBlank()) emojiInput else CategoryEmojiProvider.getCategoryEmoji(name)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialCategory == null) "Add Custom Category 🏷️" else "Edit Category ✏️",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Select Transaction Type:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(
                        onClick = { selectedType = "EXPENSE" },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedType == "EXPENSE") RedExpense else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selectedType == "EXPENSE") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("Expense", fontSize = 11.sp)
                    }
                    Button(
                        onClick = { selectedType = "INCOME" },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedType == "INCOME") GreenSuccess else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selectedType == "INCOME") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("Income", fontSize = 11.sp)
                    }
                    Button(
                        onClick = { selectedType = "INVESTMENT" },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedType == "INVESTMENT") PrimaryBlue else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selectedType == "INVESTMENT") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("Invest", fontSize = 11.sp)
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = emojiInput,
                    onValueChange = { emojiInput = it },
                    label = { Text("Category Emoji (Optional)") },
                    placeholder = { Text(autoResolvedEmoji) },
                    trailingIcon = { Text(autoResolvedEmoji, fontSize = 20.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "ℹ️ Category color will be assigned automatically by the system to prevent duplicate color collisions.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val finalEmoji = if (emojiInput.isNotBlank()) emojiInput else CategoryEmojiProvider.getCategoryEmoji(name)
                        onConfirm(name, selectedType, finalEmoji)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text(if (initialCategory == null) "Add" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
