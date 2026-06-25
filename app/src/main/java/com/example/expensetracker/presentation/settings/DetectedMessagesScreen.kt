package com.example.expensetracker.presentation.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
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
import com.example.expensetracker.core.money.formatInr
import com.example.expensetracker.data.local.entity.DetectedMessageEntity
import com.example.expensetracker.presentation.common.dateText

/** Full-screen list of SMS/notifications the parser treated as debit transactions. */
@Composable
fun DetectedMessagesScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val vm: DetectedMessagesViewModel = hiltViewModel()
    val todayMessages by vm.todayMessages.collectAsState()
    val pastMessages by vm.pastMessages.collectAsState()
    val pastLoading by vm.pastLoading.collectAsState()
    val pastHasMore by vm.pastHasMore.collectAsState()
    val selectedTab by vm.selectedTab.collectAsState()
    val hasAnyMessages by vm.hasAnyMessages.collectAsState()
    var showClearAll by remember { mutableStateOf(false) }

    if (showClearAll) {
        AlertDialog(
            onDismissRequest = { showClearAll = false },
            title = { Text("Clear all messages?") },
            text = {
                Text(
                    "This removes every stored detection log from this device. " +
                        "It does not delete your transactions."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.clearAll()
                    showClearAll = false
                }) { Text("Clear all") }
            },
            dismissButton = {
                TextButton(onClick = { showClearAll = false }) { Text("Cancel") }
            }
        )
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Detected messages",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "SMS & notifications parsed as debits",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (hasAnyMessages) {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 12.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showClearAll = true }) {
                            Text("Clear all", maxLines = 1)
                        }
                    }
                }
            }

            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                modifier = Modifier.padding(horizontal = 16.dp),
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == DetectedMessagesTab.Today,
                    onClick = { vm.selectTab(DetectedMessagesTab.Today) },
                    text = { Text("Today") }
                )
                Tab(
                    selected = selectedTab == DetectedMessagesTab.Past,
                    onClick = { vm.selectTab(DetectedMessagesTab.Past) },
                    text = { Text("Past") }
                )
            }

            Spacer(Modifier.height(8.dp))

            when (selectedTab) {
                DetectedMessagesTab.Today -> TodayMessagesList(
                    messages = todayMessages,
                    onDelete = vm::delete
                )
                DetectedMessagesTab.Past -> PastMessagesList(
                    messages = pastMessages,
                    loading = pastLoading,
                    hasMore = pastHasMore,
                    onDelete = vm::delete,
                    onLoadMore = vm::loadNextPastPage
                )
            }
        }
    }
}

@Composable
private fun TodayMessagesList(
    messages: List<DetectedMessageEntity>,
    onDelete: (Long) -> Unit
) {
    if (messages.isEmpty()) {
        EmptyMessagesHint(
            "No messages detected today.\nNew SMS and payment notifications will appear here."
        )
        return
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(messages, key = { it.id }) { message ->
            DetectedMessageCard(message = message, onDelete = { onDelete(message.id) })
        }
    }
}

@Composable
private fun PastMessagesList(
    messages: List<DetectedMessageEntity>,
    loading: Boolean,
    hasMore: Boolean,
    onDelete: (Long) -> Unit,
    onLoadMore: () -> Unit
) {
    val listState = rememberLazyListState()
    val shouldLoadMore by remember {
        derivedStateOf {
            if (!hasMore || loading || messages.isEmpty()) return@derivedStateOf false
            val layoutInfo = listState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= layoutInfo.totalItemsCount - 3
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onLoadMore()
    }

    if (messages.isEmpty() && !loading) {
        EmptyMessagesHint("No past messages.")
        return
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(messages, key = { it.id }) { message ->
            DetectedMessageCard(message = message, onDelete = { onDelete(message.id) })
        }
        if (loading) {
            item(key = "loading") {
                Box(
                    Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun EmptyMessagesHint(message: String) {
    Box(
        Modifier.fillMaxSize().padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DetectedMessageCard(message: DetectedMessageEntity, onDelete: () -> Unit) {
    var showDelete by remember { mutableStateOf(false) }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete this message?") },
            text = { Text("This only removes the log entry, not any transaction created from it.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDelete = false
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) { Text("Cancel") }
            }
        )
    }

    val isSms = message.source.contains("SMS", ignoreCase = true)
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (isSms) Icons.Default.Sms else Icons.Default.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(message.source, fontWeight = FontWeight.SemiBold)
                    Text(
                        dateText(message.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    message.sender?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = { showDelete = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete message")
                }
            }
            Text(
                "${formatInr(message.amountPaise)} · ${message.merchant}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                message.rawText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
