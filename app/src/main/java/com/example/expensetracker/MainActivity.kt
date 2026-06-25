package com.example.expensetracker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.expensetracker.domain.notification.Notifier
import com.example.expensetracker.presentation.navigation.ExpenseApp
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Single-activity host. Stays intentionally thin: it only wires Compose content and forwards the
 * optional "open this transaction for review" deep link from notifications.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var notifier: Notifier

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val reviewId = consumeReviewIntent(intent)
        setContent {
            ExpenseApp(startReviewId = reviewId)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeReviewIntent(intent)
    }

    /** Dismisses the detection notification and returns the transaction id to open, if any. */
    private fun consumeReviewIntent(intent: Intent): Long? {
        val reviewId = intent.getLongExtra(EXTRA_REVIEW_ID, -1L)
        if (reviewId > 0) {
            notifier.cancel(reviewId)
            intent.removeExtra(EXTRA_REVIEW_ID)
            return reviewId
        }
        return null
    }

    companion object {
        private const val EXTRA_REVIEW_ID = "review_transaction_id"

        fun intent(context: Context, reviewTransactionId: Long? = null): Intent =
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .apply { reviewTransactionId?.let { putExtra(EXTRA_REVIEW_ID, it) } }
    }
}
