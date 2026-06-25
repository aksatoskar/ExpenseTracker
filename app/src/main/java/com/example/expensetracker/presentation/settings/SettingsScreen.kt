package com.example.expensetracker.presentation.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.expensetracker.presentation.common.PermissionStatusCard
import com.example.expensetracker.presentation.common.SectionHeader
import com.example.expensetracker.presentation.common.syncTimeText
import com.example.expensetracker.util.PermissionHelper

/** Settings tab: theme toggle, detection-reliability permission status and privacy info. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onOpenDetectedMessages: () -> Unit = {}) {
    val context = LocalContext.current
    val vm: SettingsViewModel = hiltViewModel()
    val darkTheme by vm.darkTheme.collectAsState()
    val currentUser by vm.currentUser.collectAsState()
    val lastCloudSync by vm.lastCloudSync.collectAsState()
    val isCloudSyncing by vm.isCloudSyncing.collectAsState()
    val detectedMessageCount by vm.detectedMessageCount.collectAsState()
    var refreshKey by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshKey++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationAccess = remember(refreshKey) { PermissionHelper.hasNotificationListenerAccess(context) }
    val appNotifications = remember(refreshKey) { PermissionHelper.areNotificationsEnabled(context) }
    val smsAccess = remember(refreshKey) { PermissionHelper.hasSmsPermission(context) }
    val batteryOk = remember(refreshKey) { PermissionHelper.isIgnoringBatteryOptimizations(context) }

    val smsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        refreshKey++
        if (result.values.none { it }) {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
        }
    }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (darkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Dark Mode", fontWeight = FontWeight.SemiBold)
                        Text(
                            if (darkTheme) "Dark theme enabled" else "Light theme enabled",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = darkTheme, onCheckedChange = vm::setDarkTheme)
                }
            }
        }
        item { SectionHeader("Account & Sync") }
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (currentUser != null) Icons.Default.CloudDone else Icons.Default.CloudSync,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            val user = currentUser
                            if (user != null) {
                                Text(user.displayName ?: "Signed in", fontWeight = FontWeight.SemiBold)
                                Text(
                                    user.email ?: "Google account",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text("Back up & sync", fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Sign in with Google to sync your data across devices",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (currentUser == null) {
                        Button(
                            onClick = {
                                (context as? Activity)?.let { activity ->
                                    vm.signIn(activity) { msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("Sign in with Google") }
                    } else {
                        Text(
                            "Last synced: ${syncTimeText(lastCloudSync)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Column(
                            Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Button(
                                onClick = {
                                    vm.cloudSyncNow { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }
                                },
                                enabled = !isCloudSyncing,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isCloudSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Syncing...")
                                } else {
                                    Text("Sync now")
                                }
                            }
                            TextButton(
                                onClick = { vm.signOut() },
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(vertical = 0.dp)
                            ) { Text("Sign out") }
                        }
                    }
                }
            }
        }
        item { SectionHeader("Detection Reliability") }
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                onClick = onOpenDetectedMessages
            ) {
                Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Detected messages", fontWeight = FontWeight.SemiBold)
                        Text(
                            if (detectedMessageCount == 0) {
                                "SMS & notifications parsed as debit transactions"
                            } else {
                                "$detectedMessageCount stored message${if (detectedMessageCount == 1) "" else "s"}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        "View",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        item {
            Text(
                "All permissions below must be enabled for expense detection to work when the app is closed.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            PermissionStatusCard(
                title = "Payment Notification Access",
                description = "Detects UPI and bank payment alerts from other apps",
                enabled = notificationAccess,
                actionLabel = "Enable",
                onAction = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
            )
        }
        item {
            PermissionStatusCard(
                title = "App Notifications",
                description = "Required to alert you when an expense is detected",
                enabled = appNotifications,
                actionLabel = "Enable",
                onAction = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        })
                    } else {
                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
                    }
                }
            )
        }
        item {
            PermissionStatusCard(
                title = "SMS Access",
                description = "Detects bank debit SMS when payment apps don't notify",
                enabled = smsAccess,
                actionLabel = "Enable",
                onAction = { smsLauncher.launch(PermissionHelper.smsPermissions) }
            )
        }
        item {
            OutlinedButton(
                onClick = {
                    val shown = vm.sendTestNotification()
                    Toast.makeText(
                        context,
                        if (shown) "Test notification sent" else "Enable notifications first",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Send test notification") }
        }
        item {
            PermissionStatusCard(
                title = "Battery Optimization",
                description = "Prevents the system from stopping background detection",
                enabled = batteryOk,
                actionLabel = "Disable",
                onAction = { context.startActivity(PermissionHelper.batteryOptimizationIntent(context)) }
            )
        }
        item { SectionHeader("Privacy") }
        item {
            Card(shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Privacy-first architecture", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "All transaction processing, merchant learning, budgets, and reports run locally on this device. Your data is only uploaded to your private account when you sign in and sync.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
