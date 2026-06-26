package com.example.expensetracker.data.classification

import com.example.expensetracker.domain.classification.MessageClassificationInput
import com.example.expensetracker.domain.classification.MessageClassificationResult
import com.example.expensetracker.domain.classification.MessageType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import java.util.regex.Pattern
import javax.inject.Inject

/**
 * Layered rule engine (Stages 1–5, 8). Returns a definitive result for ~85–90% of messages;
 * returns `null` when confidence is ambiguous so [TfliteMessageClassifier] can decide.
 */
class MessageClassificationRules @Inject constructor() {

    /**
     * @return A high-confidence classification, or `null` to defer to the ML fallback.
     */
    fun evaluate(input: MessageClassificationInput): MessageClassificationResult? {
        val lower = input.rawText.lowercase(Locale.US)
        val sender = input.sender ?: input.notificationPackage

        if (OTP_PATTERN.containsMatchIn(lower)) {
            return result(MessageType.Otp, 98, "otp")
        }
        if (PHISHING_PATTERN.containsMatchIn(lower)) {
            return result(MessageType.PhishingSpam, 97, "phishing_fraud")
        }
        if (CREDIT_PATTERN.containsMatchIn(lower)) {
            return result(MessageType.Credit, 96, "credit")
        }
        if (REWARD_PATTERN.containsMatchIn(lower)) {
            return result(MessageType.RewardCashback, 95, "reward_cashback")
        }
        if (isFutureDebit(lower)) {
            return result(MessageType.FutureDebit, 96, "future_debit")
        }
        if (RECEIPT_PATTERN.containsMatchIn(lower)) {
            return result(MessageType.Receipt, 97, "receipt")
        }
        if (isMessagingAppMirror(input)) {
            return result(MessageType.Unknown, 92, "sms_mirror_notification")
        }
        if (isFutureTransaction(input.rawText, input.receivedAtMillis)) {
            return result(MessageType.FutureDebit, 94, "future_transaction_date")
        }
        if (isSuspiciousLinkPhishing(input)) {
            return result(MessageType.PhishingSpam, 96, "phishing_suspicious_link")
        }

        val hasAmount = AMOUNT_PATTERN.containsMatchIn(lower)
        val hasStrongDebit = containsAny(lower, STRONG_DEBIT_KEYWORDS)
        val hasUpiDebit = hasUpiDebitSignal(lower)

        if (!hasAmount) {
            return result(MessageType.Unknown, 25, "missing_amount")
        }
        if (!hasStrongDebit && !hasUpiDebit) {
            return null
        }

        var confidence = 72
        if (hasStrongDebit) confidence += 12
        if (hasUpiDebit) confidence += 8
        if (SenderValidator.isTrustedBankSender(sender)) confidence += 14
        if (SenderValidator.isTrustedPaymentAppSender(sender)) confidence += 10
        if (hasAmount && hasStrongDebit && SenderValidator.isTrustedFinancialSender(sender)) {
            confidence += 4
        }
        confidence = confidence.coerceAtMost(98)

        return if (confidence >= MessageClassificationResult.NOTIFY_THRESHOLD) {
            result(MessageType.ActualDebit, confidence, "rule_engine_actual_debit")
        } else {
            null
        }
    }

    private fun hasUpiDebitSignal(lower: String): Boolean {
        if (containsAny(lower, STRONG_UPI_KEYWORDS)) return true
        if (!containsAny(lower, WEAK_UPI_KEYWORDS)) return false
        return lower.contains("upi") ||
            containsAny(lower, listOf("phonepe", "paytm", "gpay", "google pay", "amazon pay"))
    }

    private fun isFutureDebit(lower: String): Boolean {
        if (!containsAny(lower, FUTURE_DEBIT_KEYWORDS)) return false
        if (containsAny(lower, COMPLETED_EXECUTION_KEYWORDS)) return false
        return true
    }

    private fun containsAny(text: String, keywords: List<String>): Boolean =
        keywords.any(text::contains)

    private fun isMessagingAppMirror(input: MessageClassificationInput): Boolean {
        if (input.source != "Notification") return false
        val pkg = input.notificationPackage ?: return false
        if (pkg !in MESSAGING_APP_PACKAGES) return false
        return BANK_SMS_PREFIX.matcher(input.rawText).find() ||
            SENT_FROM_ACCOUNT.matcher(input.rawText).find()
    }

    /**
     * Flags link-based phishing only. "Not you? Call 1800…" is standard on real bank debit SMS
     * and must not be treated as fraud by itself.
     */
    private fun isSuspiciousLinkPhishing(input: MessageClassificationInput): Boolean {
        if (!SUSPICIOUS_LINK.matcher(input.rawText).find()) return false
        if (containsAny(input.rawText.lowercase(Locale.US), STRONG_DEBIT_KEYWORDS) &&
            SenderValidator.isTrustedBankSender(input.sender)
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
        val STRONG_DEBIT_KEYWORDS = listOf(
            "debited",
            "spent",
            "withdrawn",
            "cash withdrawal",
            "purchase using card",
            "purchase on card",
            "debit alert",
            "deducted",
            "nach debit",
            "emi deducted",
            "autopay debit",
            "paid using",
            "txn successful"
        )

        private val FUTURE_DEBIT_KEYWORDS = listOf(
            "will be debited",
            "will be charged",
            "due on",
            "upcoming payment",
            "upcoming debit",
            "scheduled payment",
            "autopay scheduled",
            "mandate will be",
            "e-mandate",
            "standing instruction:",
            "payment due",
            "due on 5th",
            "subscription renewal scheduled",
            "nach mandate will be presented"
        )

        private val COMPLETED_EXECUTION_KEYWORDS = listOf(
            "executed",
            "completed",
            "processed successfully",
            "debited",
            "deducted",
            "autopay debit of"
        )

        private val OTP_PATTERN = Regex(
            "\\botp\\b|one time password|verification code|authentication code|do not share otp",
            RegexOption.IGNORE_CASE
        )

        private val PHISHING_PATTERN = Regex(
            "click here|bit\\.ly|tinyurl|verify kyc|update pan|account blocked|account frozen|" +
                "loan approved|claim reward|win money|lottery|congratulations! win|unlock your bank",
            RegexOption.IGNORE_CASE
        )

        private val CREDIT_PATTERN = Regex(
            "\\bcredited\\b|refund|salary|interest credited|credit alert|credit received|neft credit|" +
                "imps credit received",
            RegexOption.IGNORE_CASE
        )

        private val REWARD_PATTERN = Regex(
            "reward points|cashback earned|bonus points|loyalty points|claim your reward points",
            RegexOption.IGNORE_CASE
        )

        private val RECEIPT_PATTERN = Regex(
            "invoice|receipt|download your bill|tax invoice|order confirmation|" +
                "thank you for your purchase|thank you for shopping|purchase receipt attached|" +
                "order confirmed|order has been delivered|download invoice",
            RegexOption.IGNORE_CASE
        )

        private val STRONG_UPI_KEYWORDS = listOf(
            "upi txn",
            "upi payment",
            "upi transaction successful"
        )

        private val WEAK_UPI_KEYWORDS = listOf(
            "paid to",
            "sent to",
            "transferred to"
        )

        private val AMOUNT_PATTERN = Regex(
            "(?:rs\\.?|inr|₹)\\s*[0-9,]+(?:\\.[0-9]{1,2})?",
            RegexOption.IGNORE_CASE
        )

        private val MESSAGING_APP_PACKAGES = setOf(
            "com.google.android.apps.messaging",
            "com.samsung.android.messaging",
            "com.android.mms"
        )

        private val BANK_SMS_PREFIX = Pattern.compile("\\b[A-Z]{2}-[A-Z]{4,8}-[A-Z]\\b")

        private val SENT_FROM_ACCOUNT = Pattern.compile(
            "Sent\\s+Rs\\.?\\s*[0-9,.]+\\s+From\\s+[A-Za-z ]+Bank\\s+A/C",
            Pattern.CASE_INSENSITIVE
        )

        private val SUSPICIOUS_LINK = Pattern.compile(
            "bit\\.ly|tinyurl|http://|https://",
            Pattern.CASE_INSENSITIVE
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
