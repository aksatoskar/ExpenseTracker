package com.example.expensetracker.data.classification

import com.example.expensetracker.domain.classification.MessageClassificationInput
import com.example.expensetracker.domain.classification.MessageClassificationResult
import com.example.expensetracker.domain.classification.MessageType
import com.example.expensetracker.domain.classification.CompiledClassificationRules
import com.example.expensetracker.domain.repository.ClassificationConfigRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import java.util.regex.Pattern
import javax.inject.Inject

/**
 * Static safety rules only — OTP, phishing, credits, receipts, confirmed bank debits, and
 * future-dated transactions. Debit tense and meaning (actual vs upcoming) are decided by TFLite
 * when static rules do not apply.
 */
class MessageClassificationRules @Inject constructor(
    private val configRepository: ClassificationConfigRepository,
    private val senderValidator: SenderValidator
) {

    private val rules: CompiledClassificationRules
        get() = configRepository.current()

    /**
     * Returns a definitive result for clear non-debit or structural cases; `null` means defer to ML.
     */
    fun evaluate(input: MessageClassificationInput): MessageClassificationResult? {
        val compiled = rules
        val lower = input.rawText.lowercase(Locale.US)

        if (compiled.otpPattern.containsMatchIn(lower)) {
            return result(MessageType.Otp, 98, "otp")
        }
        if (compiled.phishingPattern.containsMatchIn(lower)) {
            return result(MessageType.PhishingSpam, 97, "phishing_fraud")
        }
        if (isAccountCredit(lower, compiled)) {
            return result(MessageType.Credit, 96, "credit")
        }
        if (compiled.rewardPattern.containsMatchIn(lower)) {
            return result(MessageType.RewardCashback, 95, "reward_cashback")
        }
        if (compiled.receiptPattern.containsMatchIn(lower)) {
            return result(MessageType.Receipt, 97, "purchase_receipt")
        }
        if (isFutureTransaction(input.rawText, input.receivedAtMillis)) {
            return result(MessageType.FutureDebit, 94, "future_transaction_date")
        }
        if (containsAny(lower, compiled.config.futureDebitKeywords)) {
            return result(MessageType.FutureDebit, 93, "future_debit_keyword")
        }
        if (isConfirmedActualBankDebit(input.rawText, compiled)) {
            return result(MessageType.ActualDebit, 95, "confirmed_bank_debit")
        }
        if (isSuspiciousLinkPhishing(input, compiled)) {
            return result(MessageType.PhishingSpam, 96, "phishing_suspicious_link")
        }
        if (!compiled.amountPattern.containsMatchIn(lower)) {
            return result(MessageType.Unknown, 25, "missing_amount")
        }

        return null
    }

    private fun isAccountCredit(lower: String, compiled: CompiledClassificationRules): Boolean {
        if (!compiled.creditPattern.containsMatchIn(lower)) return false
        if (compiled.debitPattern.containsMatchIn(lower) && compiled.payeeCreditedPattern.containsMatchIn(lower)) {
            return false
        }
        return true
    }

    private fun containsAny(text: String, keywords: List<String>): Boolean =
        keywords.any(text::contains)

    private fun isConfirmedActualBankDebit(text: String, compiled: CompiledClassificationRules): Boolean {
        val lower = text.lowercase(Locale.US)
        if (!compiled.amountPattern.containsMatchIn(lower)) return false

        if (compiled.sentFromAccountPattern.matcher(text).find()) return true
        if (DEBITED_FROM_BANK_PATTERN.containsMatchIn(lower)) return true

        val hasDebitSignal = compiled.debitBodyPattern.containsMatchIn(lower) ||
            SENT_RUPEES_PATTERN.containsMatchIn(text)
        if (!hasDebitSignal) return false

        return compiled.bankSmsPrefixPattern.matcher(text).find() ||
            (compiled.bankBodyPattern.containsMatchIn(lower) && compiled.accountBodyPattern.containsMatchIn(lower)) ||
            (compiled.debitPattern.containsMatchIn(lower) && compiled.accountBodyPattern.containsMatchIn(lower))
    }

    private fun isSuspiciousLinkPhishing(
        input: MessageClassificationInput,
        compiled: CompiledClassificationRules
    ): Boolean {
        if (!compiled.suspiciousLinkPattern.matcher(input.rawText).find()) return false
        if (containsAny(input.rawText.lowercase(Locale.US), compiled.config.strongDebitKeywords) &&
            senderValidator.isLikelyBankMessage(input.sender, input.rawText)
        ) {
            return false
        }
        return true
    }

    private fun isFutureTransaction(text: String, receivedAtMillis: Long): Boolean {
        val receivedDate = Instant.ofEpochMilli(receivedAtMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        val latestAllowed = receivedDate.plusDays(1)

        return DATE_PATTERNS.any { pattern ->
            val matcher = pattern.matcher(text)
            generateSequence { if (matcher.find()) matcher.group() else null }
                .any { candidate -> parseDate(candidate)?.isAfter(latestAllowed) == true }
        }
    }

    private fun parseDate(raw: String): LocalDate? {
        val trimmed = raw.trim()
        for (formatter in DATE_FORMATTERS) {
            try {
                return LocalDate.parse(trimmed, formatter)
            } catch (_: DateTimeParseException) {
            }
        }
        COMPACT_DATE.matchEntire(trimmed)?.destructured?.let { (day, month, year) ->
            val fullYear = expandYear(year.toInt())
            val monthNumber = MONTHS[month.lowercase(Locale.US)] ?: return@let null
            return runCatching { LocalDate.of(fullYear, monthNumber, day.toInt()) }.getOrNull()
        }
        return null
    }

    private fun expandYear(twoOrFourDigit: Int): Int =
        if (twoOrFourDigit < 100) 2000 + twoOrFourDigit else twoOrFourDigit

    private fun result(type: MessageType, confidence: Int, reason: String): MessageClassificationResult =
        MessageClassificationResult(type = type, confidence = confidence, reason = reason)

    companion object {
        private val SENT_RUPEES_PATTERN = Regex("\\bsent\\s+rs", RegexOption.IGNORE_CASE)
        private val DEBITED_FROM_BANK_PATTERN = Regex(
            "\\bdebited\\s+from\\s+[a-z0-9 ]+bank\\b",
            RegexOption.IGNORE_CASE
        )

        private val COMPACT_DATE = Regex(
            "\\b([0-3]?\\d)(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)(\\d{2,4})\\b",
            RegexOption.IGNORE_CASE
        )

        private val DATE_PATTERNS = listOf(
            Pattern.compile("\\b\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}\\b"),
            Pattern.compile("\\b\\d{1,2}-(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)-\\d{2,4}\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\s+\\d{1,2},?\\s+\\d{2,4}\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b\\d{1,2}(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\d{2,4}\\b", Pattern.CASE_INSENSITIVE)
        )

        private val DATE_FORMATTERS = listOf(
            DateTimeFormatter.ofPattern("d/M/uuuu"),
            DateTimeFormatter.ofPattern("d-M-uuuu"),
            DateTimeFormatter.ofPattern("d/M/uu"),
            DateTimeFormatter.ofPattern("d-M-uu"),
            DateTimeFormatter.ofPattern("d-MMM-uuuu", Locale.US),
            DateTimeFormatter.ofPattern("d-MMM-uu", Locale.US),
            DateTimeFormatter.ofPattern("MMM d, uuuu", Locale.US),
            DateTimeFormatter.ofPattern("MMM d uuuu", Locale.US)
        )

        private val MONTHS = mapOf(
            "jan" to 1, "feb" to 2, "mar" to 3, "apr" to 4, "may" to 5, "jun" to 6,
            "jul" to 7, "aug" to 8, "sep" to 9, "oct" to 10, "nov" to 11, "dec" to 12
        )
    }
}
