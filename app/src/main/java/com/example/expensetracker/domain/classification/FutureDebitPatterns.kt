package com.example.expensetracker.domain.classification

/**
 * Phrases that indicate a payment reminder or scheduled debit, not a completed transaction.
 */
object FutureDebitPatterns {
    private val reminderPattern = Regex(
        "\\bwill be (?:debited|deducted|charged|withdrawn)\\b|" +
            "\\bdue in \\d+\\s+days?\\b|" +
            "\\binstal+ment due\\b|" +
            "ensure sufficient (?:bank )?balance",
        RegexOption.IGNORE_CASE
    )

    fun isFutureDebitReminder(text: String): Boolean = reminderPattern.containsMatchIn(text)
}
