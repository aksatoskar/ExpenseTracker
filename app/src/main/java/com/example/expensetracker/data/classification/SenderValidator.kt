package com.example.expensetracker.data.classification

import com.example.expensetracker.domain.classification.CompiledClassificationRules
import com.example.expensetracker.domain.repository.ClassificationConfigRepository
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SenderValidator @Inject constructor(
    private val configRepository: ClassificationConfigRepository
) {
    private val rules: CompiledClassificationRules
        get() = configRepository.current()

    private val dltSender = Regex("^[A-Z]{2}-([A-Z0-9]{4,10})-[A-Z]$")

    fun isDltSender(sender: String?): Boolean =
        dltSender.matches(sender?.trim()?.uppercase(Locale.US).orEmpty())

    fun extractSenderCode(sender: String?): String? {
        val upper = sender?.trim()?.uppercase(Locale.US).orEmpty()
        if (upper.isBlank()) return null
        dltSender.matchEntire(upper)?.groupValues?.getOrNull(1)?.let { return it }
        return upper.filter { it.isLetterOrDigit() }.takeIf { it.length in 4..12 }
    }

    fun isLikelyBankMessage(sender: String?, messageBody: String): Boolean {
        val compiled = rules
        val body = messageBody.lowercase(Locale.US)
        val hasBankLanguage = compiled.bankBodyPattern.containsMatchIn(body)
        val hasAccountLanguage = compiled.accountBodyPattern.containsMatchIn(body)
        val hasDebitLanguage = compiled.debitBodyPattern.containsMatchIn(body)

        if (hasBankLanguage && hasAccountLanguage && hasDebitLanguage) return true

        if (!isDltSender(sender)) return false
        val code = extractSenderCode(sender) ?: return false
        if (compiled.bankEntityHintPattern.containsMatchIn(code)) return true
        if (hasBankLanguage && hasDebitLanguage) return true
        return hasAccountLanguage && hasDebitLanguage
    }

    fun isLikelyPaymentAppSender(sender: String?): Boolean {
        val code = extractSenderCode(sender) ?: return false
        return rules.paymentAppHintPattern.containsMatchIn(code)
    }

    fun isLikelyFinancialMessage(sender: String?, messageBody: String): Boolean =
        isLikelyBankMessage(sender, messageBody) || isLikelyPaymentAppSender(sender)
}
