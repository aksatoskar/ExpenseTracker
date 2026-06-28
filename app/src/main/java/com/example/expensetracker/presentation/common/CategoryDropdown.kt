package com.example.expensetracker.presentation.common

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.expensetracker.data.local.entity.CustomCategoryEntity
import com.example.expensetracker.domain.model.Category
import com.example.expensetracker.domain.model.CategoryFilter
import com.example.expensetracker.domain.model.CategorySelection
import com.example.expensetracker.domain.usecase.category.CreateCustomCategoryUseCase
import com.example.expensetracker.presentation.category.CustomCategoryViewModel

/** Category picker with custom categories first, built-in below, and a create action at the bottom. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDropdownField(
    selected: CategorySelection,
    onSelected: (CategorySelection) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Category",
    customCategories: List<CustomCategoryEntity>? = null,
    viewModel: CustomCategoryViewModel = hiltViewModel()
) {
    val vmCategories by viewModel.customCategories.collectAsState()
    val categories = customCategories ?: vmCategories
    var expanded by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var createError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            readOnly = true,
            value = selected.label,
            onValueChange = {},
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            if (categories.isNotEmpty()) {
                DropdownMenuItem(
                    text = { CustomSectionHeader("Your categories") },
                    onClick = {},
                    enabled = false
                )
                categories.forEach { custom ->
                    DropdownMenuItem(
                        text = { CustomCategoryMenuLabel(custom.name) },
                        onClick = {
                            onSelected(CategorySelection.Custom(custom.id, custom.name))
                            expanded = false
                        }
                    )
                }
                HorizontalDivider()
            }
            DropdownMenuItem(
                text = {
                    Text(
                        "Built-in categories",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                onClick = {},
                enabled = false
            )
            Category.entries.forEach { builtIn ->
                DropdownMenuItem(
                    text = { Text(builtIn.label) },
                    onClick = {
                        onSelected(CategorySelection.BuiltIn(builtIn))
                        expanded = false
                    }
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            "Create custom category",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                onClick = {
                    expanded = false
                    createError = null
                    showCreateDialog = true
                }
            )
        }
    }

    if (showCreateDialog) {
        CreateCustomCategoryDialog(
            onDismiss = { showCreateDialog = false },
            errorMessage = createError,
            onCreate = { name ->
                viewModel.create(name) { result ->
                    when (result) {
                        is CreateCustomCategoryUseCase.Result.Success -> {
                            onSelected(result.selection)
                            Toast.makeText(context, "Category created", Toast.LENGTH_SHORT).show()
                            showCreateDialog = false
                        }
                        CreateCustomCategoryUseCase.Result.EmptyName ->
                            createError = "Enter a category name"
                        CreateCustomCategoryUseCase.Result.DuplicateName ->
                            createError = "A category with this name already exists"
                    }
                }
            }
        )
    }
}

/** Category filter dropdown for the transactions list (includes All + custom + built-in). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryFilterDropdown(
    selected: CategoryFilter,
    customCategories: List<CustomCategoryEntity>,
    onSelected: (CategoryFilter) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Category",
    viewModel: CustomCategoryViewModel = hiltViewModel()
) {
    val vmCategories by viewModel.customCategories.collectAsState()
    val categories = customCategories.ifEmpty { vmCategories }
    var expanded by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var createError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    val displayValue = when (selected) {
        CategoryFilter.All -> "All"
        is CategoryFilter.BuiltIn -> selected.category.label
        is CategoryFilter.Custom -> categories.find { it.id == selected.id }?.name ?: "Custom"
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            readOnly = true,
            value = displayValue,
            onValueChange = {},
            singleLine = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("All") },
                onClick = { onSelected(CategoryFilter.All); expanded = false }
            )
            if (categories.isNotEmpty()) {
                HorizontalDivider()
                DropdownMenuItem(
                    text = { CustomSectionHeader("Your categories") },
                    onClick = {},
                    enabled = false
                )
                categories.forEach { custom ->
                    DropdownMenuItem(
                        text = { CustomCategoryMenuLabel(custom.name) },
                        onClick = {
                            onSelected(CategoryFilter.Custom(custom.id))
                            expanded = false
                        }
                    )
                }
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = {
                    Text(
                        "Built-in categories",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                onClick = {},
                enabled = false
            )
            Category.entries.forEach { builtIn ->
                DropdownMenuItem(
                    text = { Text(builtIn.label) },
                    onClick = {
                        onSelected(CategoryFilter.BuiltIn(builtIn))
                        expanded = false
                    }
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            "Create custom category",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                onClick = {
                    expanded = false
                    createError = null
                    showCreateDialog = true
                }
            )
        }
    }

    if (showCreateDialog) {
        CreateCustomCategoryDialog(
            onDismiss = { showCreateDialog = false },
            errorMessage = createError,
            onCreate = { name ->
                viewModel.create(name) { result ->
                    when (result) {
                        is CreateCustomCategoryUseCase.Result.Success -> {
                            onSelected(CategoryFilter.Custom(result.selection.id))
                            Toast.makeText(context, "Category created", Toast.LENGTH_SHORT).show()
                            showCreateDialog = false
                        }
                        CreateCustomCategoryUseCase.Result.EmptyName ->
                            createError = "Enter a category name"
                        CreateCustomCategoryUseCase.Result.DuplicateName ->
                            createError = "A category with this name already exists"
                    }
                }
            }
        )
    }
}

@Composable
private fun CustomSectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun CustomCategoryMenuLabel(name: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(customCategoryColor().copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                tint = customCategoryColor(),
                modifier = Modifier.size(16.dp)
            )
        }
        Column(Modifier.weight(1f)) {
            Text(name, fontWeight = FontWeight.SemiBold)
            Text(
                "Custom",
                style = MaterialTheme.typography.labelSmall,
                color = customCategoryColor()
            )
        }
    }
}
