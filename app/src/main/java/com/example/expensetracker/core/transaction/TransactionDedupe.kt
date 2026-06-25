package com.example.expensetracker.core.transaction

import com.example.expensetracker.core.money.normalizeMerchant
import kotlin.math.abs

/**
 * Detects when two parsed debits from different channels (SMS vs notification) describe the same
 * spend. Bank SMS mirrored by Google Messages often differs in merchant text but shares amount,
 * reference number and core message body.
 */
object TransactionDedupe {

    const val DEFAULT_WINDOW_MILLIS = 10 * 60 * 1000L

    private val referencePatterns = listOf(
        Regex("\\bref[:\\s#-]+([A-Za-z0-9]{6,})", RegexOption.IGNORE_CASE),
        Regex("\\b(?:rrn|utr|txn id|transaction id)[:\\s#-]+([A-Za-z0-9]{6,})", RegexOption.IGNORE_CASE)
    )

    fun extractReference(rawText: String): String? {
        for (pattern in referencePatterns) {
            val match = pattern.find(rawText) ?: continue
            return match.groupValues[1].uppercase()
        }
        return null
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

        if (normalizeMerchant(merchantA) == normalizeMerchant(merchantB)) return true

        val coreA = normalizeMessageCore(rawA)
        val coreB = normalizeMessageCore(rawB)
        if (coreA.isNotEmpty() && coreA == coreB) return true

        val minOverlap = 24
        if (coreA.length >= minOverlap && coreB.length >= minOverlap) {
            val shorter = if (coreA.length <= coreB.length) coreA else coreB
            val longer = if (coreA.length <= coreB.length) coreB else coreA
            if (longer.contains(shorter)) return true
        }
        return false
    }
}
