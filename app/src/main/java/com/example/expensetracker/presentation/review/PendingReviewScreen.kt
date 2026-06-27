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
import com.example.expensetracker.presentation.common.TransactionRow

/** Full-screen list of transactions awaiting categorization. */
@Composable
fun PendingReviewScreen(
    onBack: () -> Unit,
    openReview: (Long) -> Unit
) {
    BackHandler(onBack = onBack)
    val vm: PendingReviewViewModel = hiltViewModel()
    val pending by vm.pending.collectAsState()
    var showDeleteAll by remember { mutableStateOf(false) }

    if (showDeleteAll) {
        AlertDialog(
            onDismissRequest = { showDeleteAll = false },
            title = { Text("Delete all pending?") },
            text = {
                Text(
                    deleteAllConfirmationMessage(pending.size)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.deleteAllPending()
                        showDeleteAll = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete all") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAll = false }) { Text("Cancel") }
            }
        )
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "Pending review",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        pendingCountLabel(pending.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (pending.isNotEmpty()) {
                    TextButton(onClick = { showDeleteAll = true }) {
                        Text(
                            "Delete all",
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 1
                        )
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
                        TransactionRow(transaction = transaction, onClick = { openReview(transaction.id) })
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
