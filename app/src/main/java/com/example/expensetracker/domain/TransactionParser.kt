package com.example.expensetracker.domain

import com.example.expensetracker.data.TransactionType
import java.util.Locale

data class ParsedTransaction(
    val amountPaise: Long,
    val merchant: String,
    val type: TransactionType,
    val timestamp: Long,
    val source: String,
    val rawText: String
)

class TransactionParser {
    private val amountRegex = Regex(
        "(?:INR|Rs\\.?|₹)\\s*([0-9,]+(?:\\.\\d{1,2})?)|([0-9,]+(?:\\.\\d{1,2})?)\\s*(?:INR|Rs\\.?|₹)",
        RegexOption.IGNORE_CASE
    )
    private val merchantRegexes = listOf(
        Regex("\\b(?:paid|sent|spent|transferred)\\s+(?:to|at)\\s+([A-Za-z0-9 &._-]{2,40})", RegexOption.IGNORE_CASE),
        Regex("\\b(?:to|at|from)\\s+([A-Za-z0-9 &._-]{2,40}?)(?:\\s+on|\\s+via|\\s+ref|\\.|,|$)", RegexOption.IGNORE_CASE),
        Regex("\\b(?:merchant|payee|beneficiary|receiver)\\s*[:\\-]\\s*([A-Za-z0-9 &._-]{2,40})", RegexOption.IGNORE_CASE),
        Regex("\\b(?:payment\\s+(?:to|for)|upi\\s+(?:to|payment\\s+to))\\s+([A-Za-z0-9 &._-]{2,40})", RegexOption.IGNORE_CASE)
    )

    fun parse(text: String, source: String, timestamp: Long = System.currentTimeMillis()): ParsedTransaction? {
        val compact = text.replace('\n', ' ').replace(Regex("\\s+"), " ").trim()
        if (!looksFinancial(compact)) return null

        val type = when {
            Regex("\\b(credited|received|refund|cashback|deposited|reversal)\\b", RegexOption.IGNORE_CASE).containsMatchIn(compact) -> TransactionType.Credit
            Regex("\\b(debited|paid|spent|sent|purchase|withdrawn|transfer(?:red|ring)?|deducted|payment\\s+(?:of|made|successful)|txn\\s+(?:successful|debit)|dr\\b)", RegexOption.IGNORE_CASE).containsMatchIn(compact) -> TransactionType.Debit
            else -> return null
        }
        if (type != TransactionType.Debit) return null

        val amountMatch = amountRegex.find(compact) ?: return null
        val amount = (amountMatch.groupValues[1].ifBlank { amountMatch.groupValues[2] })
            .replace(",", "")
            .toDoubleOrNull() ?: return null

        val merchant = merchantRegexes.firstNotNullOfOrNull { regex ->
            regex.find(compact)?.groupValues?.getOrNull(1)?.cleanupMerchant()
        } ?: inferMerchant(compact)

        if (merchant.length < 2) return null
        return ParsedTransaction(
            amountPaise = rupeesToPaise(amount),
            merchant = merchant,
            type = type,
            timestamp = timestamp,
            source = source,
            rawText = compact
        )
    }

    private fun looksFinancial(text: String): Boolean {
        val lower = text.lowercase(Locale.US)
        val hasPaymentWord = listOf(
            "upi", "debited", "paid", "spent", "sent", "transaction", "purchase",
            "transfer", "transferred", "payment", "successful", "txn", "imps", "neft",
            "withdrawn", "deducted", "paytm", "phonepe", "gpay", "google pay"
        ).any(lower::contains)
        return hasPaymentWord && amountRegex.containsMatchIn(text)
    }

    private fun inferMerchant(text: String): String {
        val tokens = text.split(" ")
            .map { it.cleanupMerchant() }
            .filter { it.length >= 3 && it.none(Char::isDigit) }
        return tokens.firstOrNull { token ->
            token.lowercase(Locale.US) !in ignoredWords
        } ?: "Unknown Merchant"
    }

    private fun String.cleanupMerchant(): String =
        replace(Regex("\\b(on|via|ref|upi|a/c|account|bank|txn|transaction).*$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("[^A-Za-z0-9 &._-]"), "")
            .trim()
            .ifBlank { "Unknown Merchant" }

    private val ignoredWords = setOf(
        "your", "account", "debited", "credited", "paid", "spent", "sent", "transaction",
        "bank", "amount", "successful", "payment", "transfer", "transferred", "from", "has", "been"
    )
}
