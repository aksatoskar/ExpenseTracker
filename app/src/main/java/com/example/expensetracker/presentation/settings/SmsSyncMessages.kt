package com.example.expensetracker.presentation.settings

/** User-facing toast/snackbar text after an SMS sync completes. */
fun smsSyncResultMessage(count: Int): String =
    when {
        count < 0 -> "Enable SMS access to sync"
        count == 0 -> "No new transactions found"
        count == 1 -> "1 new transaction added to review"
        else -> "$count new transactions added to review"
    }
