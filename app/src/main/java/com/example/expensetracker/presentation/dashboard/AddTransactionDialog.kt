package com.example.expensetracker.presentation.dashboard

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.expensetracker.domain.model.CategorySelection
import com.example.expensetracker.domain.model.Priority
import com.example.expensetracker.presentation.common.CategoryDropdownField

/** Form dialog for manually recording a spend the automatic detection missed. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    onAdd: (String, String, CategorySelection, Priority, String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var amount by remember { mutableStateOf("") }
    var merchant by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<CategorySelection>(CategorySelection.BuiltIn(com.example.expensetracker.domain.model.Category.FoodDining)) }
    var priority by remember { mutableStateOf(Priority.Optional) }
    var notes by remember { mutableStateOf("") }
    var priorityOpen by remember { mutableStateOf(false) }

    val amountValue = amount.toDoubleOrNull()
    val canSave = amountValue != null && amountValue > 0 && merchant.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Transaction") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Manually record a spend that wasn't captured automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() || it == '.' }
                        if (filtered.count { it == '.' } <= 1) amount = filtered
                    },
                    label = { Text("Amount (₹)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("Merchant / Description") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                CategoryDropdownField(
                    selected = category,
                    onSelected = { category = it },
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(expanded = priorityOpen, onExpandedChange = { priorityOpen = it }) {
                    OutlinedTextField(
                        readOnly = true,
                        value = priority.name,
                        onValueChange = {},
                        label = { Text("Priority") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = priorityOpen) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = priorityOpen, onDismissRequest = { priorityOpen = false }) {
                        Priority.entries.forEach { DropdownMenuItem(text = { Text(it.name) }, onClick = { priority = it; priorityOpen = false }) }
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onAdd(amount, merchant, category, priority, notes)
                    Toast.makeText(context, "Transaction added", Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
                enabled = canSave
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
