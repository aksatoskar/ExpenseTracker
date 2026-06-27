package com.example.expensetracker.core.transaction

import com.example.expensetracker.core.money.rupeesToPaise
import kotlin.math.abs

/**
 * Detects when two parsed debits from different channels (SMS vs notification) describe the same
 * spend. Repeat purchases at the same merchant with different bank timestamps or balances are
 * kept as separate transactions.
 */
object TransactionDedupe {

    const val DEFAULT_WINDOW_MILLIS = 10 * 60 * 1000L

    private val referencePatterns = listOf(
        Regex("\\bref[:\\s#-]+([A-Za-z0-9]{6,})", RegexOption.IGNORE_CASE),
        Regex("\\b(?:rrn|utr|txn id|transaction id)[:\\s#-]+([A-Za-z0-9]{6,})", RegexOption.IGNORE_CASE)
    )

    private val embeddedInstantPatterns = listOf(
        Regex("\\bon\\s+(\\d{4}-\\d{2}-\\d{2}[:\\s]\\d{2}[:\\s]\\d{2}[:\\s]\\d{2})", RegexOption.IGNORE_CASE),
        Regex("\\bon\\s+(\\d{1,2}[A-Za-z]{3}\\d{2,4}\\s+\\d{2}:\\d{2})", RegexOption.IGNORE_CASE)
    )

    private val balancePattern = Regex(
        "\\bBal\\s+(?:Rs\\.?|INR|₹)\\s*([0-9,]+(?:\\.\\d{1,2})?)",
        RegexOption.IGNORE_CASE
    )

    fun extractReference(rawText: String): String? {
        for (pattern in referencePatterns) {
            val match = pattern.find(rawText) ?: continue
            return match.groupValues[1].uppercase()
        }
        return null
    }

    fun extractTransactionInstant(rawText: String): String? {
        for (pattern in embeddedInstantPatterns) {
            val match = pattern.find(rawText) ?: continue
            return normalizeInstant(match.groupValues[1])
        }
        return null
    }

    fun extractBalancePaise(rawText: String): Long? {
        val match = balancePattern.find(rawText) ?: return null
        val amount = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
        return rupeesToPaise(amount)
    }

    fun normalizeMessageCore(rawText: String): String = rawText
        .lowercase()
        .replace(Regex("\\bsim\\d+\\s*\\d+\\b"), "")
        .replace(Regex("[^a-z0-9@.\\s]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    fun isSameTransaction(
        amountA: Long,
        rawA: String,
        merchantA: String,
        timestampA: Long,
        amountB: Long,
        rawB: String,
        merchantB: String,
        timestampB: Long,
        windowMillis: Long = DEFAULT_WINDOW_MILLIS
    ): Boolean {
        if (amountA != amountB) return false
        if (abs(timestampA - timestampB) > windowMillis) return false

        val refA = extractReference(rawA)
        val refB = extractReference(rawB)
        if (refA != null && refB != null) return refA == refB

        val instantA = extractTransactionInstant(rawA)
        val instantB = extractTransactionInstant(rawB)
        if (instantA != null && instantB != null) {
            if (instantA != instantB) return false
        }

        val balanceA = extractBalancePaise(rawA)
        val balanceB = extractBalancePaise(rawB)
        if (balanceA != null && balanceB != null) {
            if (balanceA != balanceB) return false
        }

        val coreA = normalizeMessageCore(rawA)
        val coreB = normalizeMessageCore(rawB)
        if (coreA.isNotEmpty() && coreA == coreB) return true

        val minOverlap = 24
        if (coreA.length >= minOverlap && coreB.length >= minOverlap) {
            val shorter = if (coreA.length <= coreB.length) coreA else coreB
            val longer = if (coreA.length <= coreB.length) coreB else coreA
            if (longer.contains(shorter) && instantA == null && instantB == null) return true
        }
        return false
    }

    private fun normalizeInstant(raw: String): String =
        raw.lowercase().replace(Regex("[^a-z0-9]"), "")
}
