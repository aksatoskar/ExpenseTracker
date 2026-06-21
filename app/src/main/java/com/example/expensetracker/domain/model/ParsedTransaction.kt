package com.example.expensetracker.domain.model

/**
 * A transaction extracted from raw text (SMS or notification) by the parser, before it is
 * persisted. Amounts are in paise.
 */
data class ParsedTransaction(
    val amountPaise: Long,
    val merchant: String,
    val type: TransactionType,
    val timestamp: Long,
    val source: String,
    val rawText: String
)
