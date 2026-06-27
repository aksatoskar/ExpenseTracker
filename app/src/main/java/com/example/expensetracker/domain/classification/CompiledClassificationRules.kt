package com.example.expensetracker.domain.classification

import java.util.regex.Pattern
import kotlin.text.RegexOption

/** Precompiled patterns derived from [ClassificationConfig] for fast rule evaluation. */
data class CompiledClassificationRules(
    val config: ClassificationConfig,
    val otpPattern: Regex,
    val phishingPattern: Regex,
    val creditPattern: Regex,
    val debitPattern: Regex,
    val payeeCreditedPattern: Regex,
    val rewardPattern: Regex,
    val receiptPattern: Regex,
    val amountPattern: Regex,
    val bankBodyPattern: Regex,
    val accountBodyPattern: Regex,
    val debitBodyPattern: Regex,
    val bankEntityHintPattern: Regex,
    val paymentAppHintPattern: Regex,
    val suspiciousLinkPattern: Pattern,
    val bankSmsPrefixPattern: Pattern,
    val sentFromAccountPattern: Pattern
) {
    companion object {
        fun from(config: ClassificationConfig): CompiledClassificationRules =
            CompiledClassificationRules(
                config = config,
                otpPattern = config.otpPattern.toRegex(RegexOption.IGNORE_CASE),
                phishingPattern = config.phishingPattern.toRegex(RegexOption.IGNORE_CASE),
                creditPattern = config.creditPattern.toRegex(RegexOption.IGNORE_CASE),
                debitPattern = config.debitPattern.toRegex(RegexOption.IGNORE_CASE),
                payeeCreditedPattern = config.payeeCreditedPattern.toRegex(RegexOption.IGNORE_CASE),
                rewardPattern = config.rewardPattern.toRegex(RegexOption.IGNORE_CASE),
                receiptPattern = config.receiptPattern.toRegex(RegexOption.IGNORE_CASE),
                amountPattern = config.amountPattern.toRegex(RegexOption.IGNORE_CASE),
                bankBodyPattern = config.bankBodyPattern.toRegex(RegexOption.IGNORE_CASE),
                accountBodyPattern = config.accountBodyPattern.toRegex(RegexOption.IGNORE_CASE),
                debitBodyPattern = config.debitBodyPattern.toRegex(RegexOption.IGNORE_CASE),
                bankEntityHintPattern = config.bankEntityHintPattern.toRegex(RegexOption.IGNORE_CASE),
                paymentAppHintPattern = config.paymentAppHintPattern.toRegex(RegexOption.IGNORE_CASE),
                suspiciousLinkPattern = Pattern.compile(config.suspiciousLinkPattern, Pattern.CASE_INSENSITIVE),
                bankSmsPrefixPattern = Pattern.compile(config.bankSmsPrefixPattern),
                sentFromAccountPattern = Pattern.compile(config.sentFromAccountPattern, Pattern.CASE_INSENSITIVE)
            )
    }
}
