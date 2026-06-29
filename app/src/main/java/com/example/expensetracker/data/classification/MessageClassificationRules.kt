package com.example.expensetracker.data.classification

import com.example.expensetracker.domain.classification.FutureDebitPatterns
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
 * Layered rule engine driven by [ClassificationConfigRepository] (Remote Config + bundled fallback).
 */
class MessageClassificationRules @Inject constructor(
    private val configRepository: ClassificationConfigRepository,
    private val senderValidator: SenderValidator
) {

    private val rules: CompiledClassificationRules
        get() = configRepository.current()

    fun evaluate(input: MessageClassificationInput): MessageClassificationResult? {
        val compiled = rules
        val cfg = compiled.config
        val lower = input.rawText.lowercase(Locale.US)
        val sender = input.sender ?: input.notificationPackage

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
        if (isFutureDebit(lower, cfg)) {
            return result(MessageType.FutureDebit, 96, "future_debit")
        }
        if (compiled.receiptPattern.containsMatchIn(lower)) {
            return result(MessageType.Receipt, 97, "purchase_receipt")
        }
        if (isMessagingAppMirror(input, compiled)) {
            return result(MessageType.Unknown, 92, "sms_mirror_notification")
        }
        if (isFutureTransaction(input.rawText, input.receivedAtMillis)) {
            return result(MessageType.FutureDebit, 94, "future_transaction_date")
        }
        if (isSuspiciousLinkPhishing(input, compiled)) {
            return result(MessageType.PhishingSpam, 96, "phishing_suspicious_link")
        }

        val hasAmount = compiled.amountPattern.containsMatchIn(lower)
        val hasStrongDebit = containsAny(lower, cfg.strongDebitKeywords)
        val hasSentFromAccount = compiled.sentFromAccountPattern.matcher(input.rawText).find()
        val hasUpiDebit = hasUpiDebitSignal(lower, cfg)
        val hasDebitSignal = hasStrongDebit || hasSentFromAccount || hasUpiDebit

        if (!hasAmount) {
            return result(MessageType.Unknown, 25, "missing_amount")
        }
        if (!hasDebitSignal) {
            return null
        }

        var confidence = 72
        if (hasStrongDebit) confidence += 12
        if (hasSentFromAccount) confidence += 12
        if (hasUpiDebit) confidence += 8
        if (senderValidator.isLikelyBankMessage(sender, input.rawText)) confidence += 14
        else if (senderValidator.isDltSender(sender)) confidence += 6
        if (senderValidator.isLikelyPaymentAppSender(sender)) confidence += 10
        if (hasAmount && hasDebitSignal && senderValidator.isLikelyFinancialMessage(sender, input.rawText)) {
            confidence += 4
        }
        confidence = confidence.coerceAtMost(98)

        return if (confidence >= cfg.notifyConfidenceThreshold) {
            result(MessageType.ActualDebit, confidence, "rule_engine_actual_debit")
        } else {
            null
        }
    }

    private fun hasUpiDebitSignal(lower: String, cfg: com.example.expensetracker.domain.classification.ClassificationConfig): Boolean {
        if (containsAny(lower, cfg.strongUpiKeywords)) return true
        if (!containsAny(lower, cfg.weakUpiKeywords)) return false
        return lower.contains("upi") || containsAny(lower, cfg.upiBrandKeywords)
    }

    private fun isAccountCredit(lower: String, compiled: CompiledClassificationRules): Boolean {
        if (!compiled.creditPattern.containsMatchIn(lower)) return false
        if (compiled.debitPattern.containsMatchIn(lower) && compiled.payeeCreditedPattern.containsMatchIn(lower)) {
            return false
        }
        return true
    }

    private fun isFutureDebit(lower: String, cfg: com.example.expensetracker.domain.classification.ClassificationConfig): Boolean {
        if (FutureDebitPatterns.isFutureDebitReminder(lower)) return true
        if (!containsAny(lower, cfg.futureDebitKeywords)) return false
        if (containsAny(lower, cfg.completedExecutionKeywords)) return false
        return true
    }

    private fun containsAny(text: String, keywords: List<String>): Boolean =
        keywords.any(text::contains)

    private fun isMessagingAppMirror(input: MessageClassificationInput, compiled: CompiledClassificationRules): Boolean {
        if (input.source != "Notification") return false
        val pkg = input.notificationPackage ?: return false
        if (pkg !in compiled.config.messagingAppPackages) return false
        return compiled.bankSmsPrefixPattern.matcher(input.rawText).find() ||
            compiled.sentFromAccountPattern.matcher(input.rawText).find()
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
