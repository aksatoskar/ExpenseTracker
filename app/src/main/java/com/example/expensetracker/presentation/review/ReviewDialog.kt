package com.example.expensetracker.presentation.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.expensetracker.core.money.formatInr
import com.example.expensetracker.core.money.rupeesToPaise
import com.example.expensetracker.data.local.entity.TransactionEntity
import com.example.expensetracker.domain.model.Category
import com.example.expensetracker.domain.model.Priority
import com.example.expensetracker.presentation.common.dateText

/**
 * Dialog to review a detected transaction: edit the amount, pick category/priority/notes, then
 * save (confirms it) or delete (with confirmation). Cancel simply dismisses without changes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewDialog(
    transaction: TransactionEntity,
    onSave: (Long, Category, Priority, String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var category by remember(transaction.id) { mutableStateOf(transaction.category ?: Category.Other) }
    var priority by remember(transaction.id) { mutableStateOf(transaction.priority ?: Priority.Optional) }
    var notes by remember(transaction.id) { mutableStateOf(transaction.notes) }
    var amount by remember(transaction.id) {
        val p = transaction.amountPaise
        mutableStateOf(if (p % 100L == 0L) (p / 100L).toString() else (p / 100.0).toString())
    }
    var categoryOpen by remember { mutableStateOf(false) }
    var priorityOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val amountValue = amount.toDoubleOrNull()
    val canSave = amountValue != null && amountValue > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Review Transaction") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() || it == '.' }
                        if (filtered.count { it == '.' } <= 1) amount = filtered
                    },
                    label = { Text("Amount (₹)") },
                    singleLine = true,
                    isError = !canSave,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Merchant: ${transaction.merchant}")
                Text("Date: ${dateText(transaction.timestamp)}")
                Text("Source: ${transaction.source}")
                ExposedDropdownMenuBox(expanded = categoryOpen, onExpandedChange = { categoryOpen = it }) {
                    OutlinedTextField(readOnly = true, value = category.label, onValueChange = {}, label = { Text("Category") }, modifier = Modifier.menuAnchor().fillMaxWidth())
                    ExposedDropdownMenu(expanded = categoryOpen, onDismissRequest = { categoryOpen = false }) {
                        Category.entries.forEach { DropdownMenuItem(text = { Text(it.label) }, onClick = { category = it; categoryOpen = false }) }
                    }
                }
                ExposedDropdownMenuBox(expanded = priorityOpen, onExpandedChange = { priorityOpen = it }) {
                    OutlinedTextField(readOnly = true, value = priority.name, onValueChange = {}, label = { Text("Priority") }, modifier = Modifier.menuAnchor().fillMaxWidth())
                    ExposedDropdownMenu(expanded = priorityOpen, onDismissRequest = { priorityOpen = false }) {
                        Priority.entries.forEach { DropdownMenuItem(text = { Text(it.name) }, onClick = { priority = it; priorityOpen = false }) }
                    }
                }
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
                TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(6.dp))
                    Text("Delete Transaction", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(rupeesToPaise(amountValue ?: 0.0), category, priority, notes) },
                enabled = canSave
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete transaction?") },
            text = { Text("This removes ${formatInr(transaction.amountPaise)} at ${transaction.merchant} from your history, spending totals, and budgets. This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = { confirmDelete = false; onDelete() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
}
