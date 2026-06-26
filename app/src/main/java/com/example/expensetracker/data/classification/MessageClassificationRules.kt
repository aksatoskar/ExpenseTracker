package com.example.expensetracker.data.classification

import com.example.expensetracker.domain.classification.MessageClassificationInput
import com.example.expensetracker.domain.classification.MessageClassificationResult
import com.example.expensetracker.domain.classification.MessageLabel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import java.util.regex.Pattern
import javax.inject.Inject

/**
 * Two-step debit validation:
 * 1. Negative match — reject upcoming payments, receipts, OTPs, credits, mirrors, etc.
 * 2. Positive match — require at least one strong completed-debit indicator.
 *
 * Runs before the on-device TFLite model in [TfliteMessageClassifier].
 */
class MessageClassificationRules @Inject constructor() {

    fun evaluate(input: MessageClassificationInput): MessageClassificationResult? {
        val lower = input.rawText.lowercase(Locale.US)

        if (containsAny(lower, FUTURE_PAYMENT_KEYWORDS)) {
            return reject(MessageLabel.Invalid, "upcoming_payment")
        }
        if (containsAny(lower, RECEIPT_KEYWORDS)) {
            return reject(MessageLabel.Invalid, "purchase_receipt")
        }
        if (containsAny(lower, HARD_REJECT_KEYWORDS)) {
            return reject(MessageLabel.Invalid, "hard_reject_keyword")
        }
        if (containsAny(lower, OTP_OR_PROMO_KEYWORDS)) {
            return reject(MessageLabel.Spam, "otp_or_promo")
        }
        if (containsAny(lower, CREDIT_OR_REWARD_KEYWORDS)) {
            return reject(MessageLabel.Invalid, "credit_or_reward")
        }
        if (isMessagingAppMirror(input)) {
            return reject(MessageLabel.Invalid, "sms_mirror_notification")
        }
        if (isFutureTransaction(input.rawText, input.receivedAtMillis)) {
            return reject(MessageLabel.Invalid, "future_transaction_date")
        }
        if (isLikelyPhishingSms(input)) {
            return reject(MessageLabel.Spam, "phishing_pattern")
        }
        if (!containsAny(lower, STRONG_DEBIT_KEYWORDS)) {
            return reject(MessageLabel.Invalid, "missing_strong_debit_keyword")
        }
        return null
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

    private fun isLikelyPhishingSms(input: MessageClassificationInput): Boolean {
        if (input.source != "SMS") return false
        val sender = input.sender?.uppercase(Locale.US).orEmpty()
        val looksLikeBankSender = sender.matches(Regex("^[A-Z]{2}-[A-Z]{4,8}-[A-Z]$"))
        val hasPhishingCues = PHISHING_CUES.matcher(input.rawText).find()
        val tinyAmount = TINY_AMOUNT.matcher(input.rawText).find()
        return looksLikeBankSender && hasPhishingCues && tinyAmount
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

    private fun reject(label: MessageLabel, reason: String): MessageClassificationResult =
        MessageClassificationResult(label = label, confidence = 1f, reason = reason)

    companion object {
        val FUTURE_PAYMENT_KEYWORDS = listOf(
            "will be debited",
            "will be charged",
            "scheduled payment",
            "auto debit on",
            "mandate",
            "e-mandate",
            "standing instruction",
            "due on",
            "payment due",
            "scheduled",
            "autopay"
        )

        val RECEIPT_KEYWORDS = listOf(
            "download your bill",
            "invoice",
            "receipt",
            "thank you for your purchase",
            "tax invoice",
            "e-bill",
            "order confirmation"
        )

        val STRONG_DEBIT_KEYWORDS = listOf(
            "debited",
            "spent",
            "withdrawn",
            "paid using",
            "txn successful",
            "upi txn",
            "purchase using card",
            "purchase on card",
            "cash withdrawal"
        )

        val HARD_REJECT_KEYWORDS = listOf(
            "will be",
            "invoice",
            "receipt",
            "download your bill",
            "otp",
            "mandate"
        )

        private val OTP_OR_PROMO_KEYWORDS = listOf(
            "one time password",
            "lottery",
            "congratulations",
            "click here",
            "verify kyc",
            "account blocked",
            "account suspended",
            "instant loan",
            "whatsapp"
        )

        private val CREDIT_OR_REWARD_KEYWORDS = listOf(
            "credited",
            "received in your account",
            "refund",
            "cashback",
            "deposited",
            "reversal",
            "reward points",
            "credit alert"
        )

        private val MESSAGING_APP_PACKAGES = setOf(
            "com.google.android.apps.messaging",
            "com.samsung.android.messaging",
            "com.android.mms"
        )

        private val BANK_SMS_PREFIX = Pattern.compile(
            "\\b[A-Z]{2}-[A-Z]{4,8}-[A-Z]\\b"
        )

        private val SENT_FROM_ACCOUNT = Pattern.compile(
            "Sent\\s+Rs\\.?\\s*[0-9,.]+\\s+From\\s+[A-Za-z ]+Bank\\s+A/C",
            Pattern.CASE_INSENSITIVE
        )

        private val PHISHING_CUES = Pattern.compile(
            "Not you\\?\\s*Call|bit\\.ly|http://|https://|verify|blocked|suspended",
            Pattern.CASE_INSENSITIVE
        )

        private val TINY_AMOUNT = Pattern.compile(
            "(?:Rs\\.?|INR|₹)\\s*[0-9]{1,2}(?:\\.00)?\\b",
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
