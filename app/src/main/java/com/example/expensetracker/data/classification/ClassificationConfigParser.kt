package com.example.expensetracker.data.classification

import com.example.expensetracker.domain.classification.ClassificationConfig
import org.json.JSONObject

object ClassificationConfigParser {

    /** Parses a complete bundled JSON file (all fields required in file). */
    fun parseFull(json: String): ClassificationConfig {
        require(json.isNotBlank()) { "Classification rules JSON must not be blank" }
        val root = JSONObject(json)
        return ClassificationConfig(
            strongDebitKeywords = root.requireStringList("strong_debit_keywords"),
            futureDebitKeywords = root.requireStringList("future_debit_keywords"),
            completedExecutionKeywords = root.requireStringList("completed_execution_keywords"),
            strongUpiKeywords = root.requireStringList("strong_upi_keywords"),
            weakUpiKeywords = root.requireStringList("weak_upi_keywords"),
            upiBrandKeywords = root.requireStringList("upi_brand_keywords"),
            messagingAppPackages = root.requireStringSet("messaging_app_packages"),
            bankEntityHintPattern = root.requirePattern("bank_entity_hint_pattern"),
            paymentAppHintPattern = root.requirePattern("payment_app_hint_pattern"),
            bankBodyPattern = root.requirePattern("bank_body_pattern"),
            accountBodyPattern = root.requirePattern("account_body_pattern"),
            debitBodyPattern = root.requirePattern("debit_body_pattern"),
            otpPattern = root.requirePattern("otp_pattern"),
            phishingPattern = root.requirePattern("phishing_pattern"),
            creditPattern = root.requirePattern("credit_pattern"),
            debitPattern = root.requirePattern("debit_pattern"),
            payeeCreditedPattern = root.requirePattern("payee_credited_pattern"),
            rewardPattern = root.requirePattern("reward_pattern"),
            receiptPattern = root.requirePattern("receipt_pattern"),
            amountPattern = root.requirePattern("amount_pattern"),
            suspiciousLinkPattern = root.requirePattern("suspicious_link_pattern"),
            bankSmsPrefixPattern = root.requirePattern("bank_sms_prefix_pattern"),
            sentFromAccountPattern = root.requirePattern("sent_from_account_pattern"),
            notifyConfidenceThreshold = root.getInt("notify_confidence_threshold")
        )
    }

    /** Merges remote JSON with bundled fallback; blank remote returns bundled unchanged. */
    fun parse(json: String, bundled: ClassificationConfig): ClassificationConfig {
        if (json.isBlank()) return bundled
        return runCatching { merge(JSONObject(json), bundled) }.getOrDefault(bundled)
    }

    private fun merge(root: JSONObject, defaults: ClassificationConfig): ClassificationConfig =
        defaults.copy(
            strongDebitKeywords = root.optStringList("strong_debit_keywords", defaults.strongDebitKeywords),
            futureDebitKeywords = root.optStringList("future_debit_keywords", defaults.futureDebitKeywords),
            completedExecutionKeywords = root.optStringList(
                "completed_execution_keywords",
                defaults.completedExecutionKeywords
            ),
            strongUpiKeywords = root.optStringList("strong_upi_keywords", defaults.strongUpiKeywords),
            weakUpiKeywords = root.optStringList("weak_upi_keywords", defaults.weakUpiKeywords),
            upiBrandKeywords = root.optStringList("upi_brand_keywords", defaults.upiBrandKeywords),
            messagingAppPackages = root.optStringSet("messaging_app_packages", defaults.messagingAppPackages),
            bankEntityHintPattern = root.optPattern("bank_entity_hint_pattern", defaults.bankEntityHintPattern),
            paymentAppHintPattern = root.optPattern("payment_app_hint_pattern", defaults.paymentAppHintPattern),
            bankBodyPattern = root.optPattern("bank_body_pattern", defaults.bankBodyPattern),
            accountBodyPattern = root.optPattern("account_body_pattern", defaults.accountBodyPattern),
            debitBodyPattern = root.optPattern("debit_body_pattern", defaults.debitBodyPattern),
            otpPattern = root.optPattern("otp_pattern", defaults.otpPattern),
            phishingPattern = root.optPattern("phishing_pattern", defaults.phishingPattern),
            creditPattern = root.optPattern("credit_pattern", defaults.creditPattern),
            debitPattern = root.optPattern("debit_pattern", defaults.debitPattern),
            payeeCreditedPattern = root.optPattern("payee_credited_pattern", defaults.payeeCreditedPattern),
            rewardPattern = root.optPattern("reward_pattern", defaults.rewardPattern),
            receiptPattern = root.optPattern("receipt_pattern", defaults.receiptPattern),
            amountPattern = root.optPattern("amount_pattern", defaults.amountPattern),
            suspiciousLinkPattern = root.optPattern("suspicious_link_pattern", defaults.suspiciousLinkPattern),
            bankSmsPrefixPattern = root.optPattern("bank_sms_prefix_pattern", defaults.bankSmsPrefixPattern),
            sentFromAccountPattern = root.optPattern("sent_from_account_pattern", defaults.sentFromAccountPattern),
            notifyConfidenceThreshold = root.optInt(
                "notify_confidence_threshold",
                defaults.notifyConfidenceThreshold
            )
        )

    private fun JSONObject.requireStringList(key: String): List<String> =
        optStringList(key, emptyList()).also { require(it.isNotEmpty()) { "Missing or empty: $key" } }

    private fun JSONObject.requireStringSet(key: String): Set<String> =
        requireStringList(key).toSet()

    private fun JSONObject.requirePattern(key: String): String =
        optPattern(key, "").also { require(it.isNotBlank()) { "Missing or blank: $key" } }

    private fun JSONObject.optStringList(key: String, fallback: List<String>): List<String> {
        if (!has(key) || isNull(key)) return fallback
        val array = optJSONArray(key) ?: return fallback
        return buildList {
            for (index in 0 until array.length()) {
                array.optString(index)?.takeIf { it.isNotBlank() }?.let(::add)
            }
        }.ifEmpty { fallback }
    }

    private fun JSONObject.optStringSet(key: String, fallback: Set<String>): Set<String> =
        optStringList(key, fallback.toList()).toSet()

    private fun JSONObject.optPattern(key: String, fallback: String): String {
        val value = optString(key)?.trim().orEmpty()
        return value.ifBlank { fallback }
    }
}
