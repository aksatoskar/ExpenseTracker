package com.example.expensetracker.presentation.review

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.expensetracker.presentation.category.CustomCategoryViewModel
import com.example.expensetracker.presentation.common.TransactionRow

/** Full-screen list of transactions awaiting categorization. */
@Composable
fun PendingReviewScreen(
    onBack: () -> Unit,
    openReview: (Long) -> Unit
) {
    val vm: PendingReviewViewModel = hiltViewModel()
    val customCategoryVm: CustomCategoryViewModel = hiltViewModel()
    val pending by vm.pending.collectAsState()
    val customCategories by customCategoryVm.customCategories.collectAsState()
    val customCategoryNames = remember(customCategories) {
        customCategories.associate { it.id to it.name }
    }
    var showDeleteAll by remember { mutableStateOf(false) }
    var showDeleteSelected by remember { mutableStateOf(false) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(emptySet<Long>()) }

    fun exitSelectionMode() {
        selectionMode = false
        selectedIds = emptySet()
    }

    BackHandler {
        if (selectionMode) exitSelectionMode() else onBack()
    }

    if (showDeleteAll) {
        AlertDialog(
            onDismissRequest = { showDeleteAll = false },
            title = { Text("Delete all pending?") },
            text = { Text(deleteAllConfirmationMessage(pending.size)) },
            confirmButton = {
                Button(
                    onClick = {
                        vm.deleteAllPending()
                        showDeleteAll = false
                        exitSelectionMode()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete all") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAll = false }) { Text("Cancel") }
            }
        )
    }

    if (showDeleteSelected) {
        AlertDialog(
            onDismissRequest = { showDeleteSelected = false },
            title = { Text("Delete selected?") },
            text = { Text(deleteSelectedConfirmationMessage(selectedIds.size)) },
            confirmButton = {
                Button(
                    onClick = {
                        vm.deleteSelected(selectedIds)
                        showDeleteSelected = false
                        exitSelectionMode()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSelected = false }) { Text("Cancel") }
            }
        )
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { if (selectionMode) exitSelectionMode() else onBack() }
                ) {
                    Icon(
                        if (selectionMode) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = if (selectionMode) "Cancel selection" else "Back"
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        if (selectionMode) "${selectedIds.size} selected" else "Pending review",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!selectionMode) {
                        Text(
                            pendingCountLabel(pending.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                when {
                    selectionMode && selectedIds.isNotEmpty() -> {
                        TextButton(onClick = { showDeleteSelected = true }) {
                            Text(
                                "Delete (${selectedIds.size})",
                                color = MaterialTheme.colorScheme.error,
                                maxLines = 1
                            )
                        }
                    }
                    !selectionMode && pending.isNotEmpty() -> {
                        TextButton(onClick = { showDeleteAll = true }) {
                            Text(
                                "Delete all",
                                color = MaterialTheme.colorScheme.error,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            if (pending.isEmpty()) {
                Box(
                    Modifier.fillMaxSize().padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "All caught up!\nNo transactions waiting for review.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(pending, key = { it.id }) { transaction ->
                        val selected = transaction.id in selectedIds
                        TransactionRow(
                            transaction = transaction,
                            selectionMode = selectionMode,
                            selected = selected,
                            onClick = {
                                if (selectionMode) {
                                    val updated = if (selected) {
                                        selectedIds - transaction.id
                                    } else {
                                        selectedIds + transaction.id
                                    }
                                    selectedIds = updated
                                    if (updated.isEmpty()) selectionMode = false
                                } else {
                                    openReview(transaction.id)
                                }
                            },
                            onLongClick = {
                                if (!selectionMode) {
                                    selectionMode = true
                                    selectedIds = setOf(transaction.id)
                                }
                            },
                            customCategoryNames = customCategoryNames
                        )
                    }
                }
            }
        }
    }
}

private fun pendingCountLabel(count: Int): String =
    when (count) {
        0 -> "No transactions need review"
        1 -> "1 transaction needs review"
        else -> "$count transactions need review"
    }

private fun deleteAllConfirmationMessage(count: Int): String =
    when (count) {
        1 -> "This permanently removes 1 uncategorized transaction from your history. This cannot be undone."
        else -> "This permanently removes all $count uncategorized transactions from your history. This cannot be undone."
    }

private fun deleteSelectedConfirmationMessage(count: Int): String =
    when (count) {
        1 -> "This permanently removes 1 selected transaction from your history. This cannot be undone."
        else -> "This permanently removes $count selected transactions from your history. This cannot be undone."
    }
