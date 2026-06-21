package com.example.expensetracker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.expensetracker.presentation.navigation.ExpenseApp
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single-activity host. Stays intentionally thin: it only wires Compose content and forwards the
 * optional "open this transaction for review" deep link from notifications.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val reviewId = intent.getLongExtra(EXTRA_REVIEW_ID, -1L)
        setContent {
            ExpenseApp(startReviewId = reviewId.takeIf { it > 0 })
        }
    }

    companion object {
        private const val EXTRA_REVIEW_ID = "review_transaction_id"

        fun intent(context: Context, reviewTransactionId: Long? = null): Intent =
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .apply { reviewTransactionId?.let { putExtra(EXTRA_REVIEW_ID, it) } }
    }
}
