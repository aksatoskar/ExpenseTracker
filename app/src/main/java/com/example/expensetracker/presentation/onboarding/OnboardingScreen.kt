package com.example.expensetracker.presentation.onboarding

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.expensetracker.util.PermissionHelper

/** First-run permission walkthrough: notification access, SMS access and POST_NOTIFICATIONS. */
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        onDone()
    }
    val smsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        step = 2
    }

    val permissions = listOf(
        Triple("Notification Access", "Allows the app to detect payment notifications from UPI and banking apps.", "Open Settings"),
        Triple("SMS Access", "Allows local parsing of bank SMS messages for automatic expense detection.", "Allow SMS"),
        Triple("Notification Permission", "Allows the app to show review prompts, reminders, digests, and reports.", "Allow Notifications")
    )
    val current = permissions[step.coerceAtMost(2)]

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Column {
            Text("Track your expenses automatically from SMS and payment notifications.", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(24.dp))
            listOf("Automatic transaction detection", "Smart categorization", "Spending insights", "Budget tracking").forEach {
                Text("• $it", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 4.dp))
            }
            Spacer(Modifier.height(32.dp))
            if (step > 0) {
                Text("Permission $step of 3", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(12.dp))
            Text(current.first, style = MaterialTheme.typography.titleLarge)
            Text(current.second, modifier = Modifier.padding(top = 8.dp))
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                when (step) {
                    0 -> {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        step = 1
                    }
                    1 -> smsLauncher.launch(PermissionHelper.smsPermissions)
                    else -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            onDone()
                        }
                    }
                }
            }
        ) {
            Text(if (step == 0) "Get Started" else current.third)
        }
    }
}
