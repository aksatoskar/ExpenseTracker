package com.example.expensetracker.data.classification

/** Validates SMS sender IDs against known bank and payment-app patterns. */
object SenderValidator {

    private val TRUSTED_BANK_CODES = setOf(
        "HDFCBK", "ICICIB", "SBIINB", "AXISBK", "KOTAKB", "IDFCFB", "FEDBNK",
        "YESBNK", "RBLBNK", "AUBANK", "INDUSB", "PNBSMS", "CANBNK", "BOBTXN",
        "UNIONB", "IDBIBK", "CSBBNK", "SCBANK", "CITIBK", "HSBCIN"
    )

    private val TRUSTED_PAYMENT_APP_CODES = setOf(
        "PHONEPE", "PAYTM", "GPAY", "GOOGLEPAY", "AMAZONPAY", "CRED", "BHIM"
    )

    private val DLT_SENDER = Regex("^[A-Z]{2}-([A-Z0-9]{4,10})-[A-Z]$")

    fun extractSenderCode(sender: String?): String? {
        val upper = sender?.trim()?.uppercase().orEmpty()
        if (upper.isBlank()) return null
        DLT_SENDER.matchEntire(upper)?.groupValues?.getOrNull(1)?.let { return it }
        return upper.filter { it.isLetterOrDigit() }.takeIf { it.length in 4..12 }
    }

    fun isTrustedBankSender(sender: String?): Boolean {
        val code = extractSenderCode(sender) ?: return false
        return TRUSTED_BANK_CODES.any(code::contains)
    }

    fun isTrustedPaymentAppSender(sender: String?): Boolean {
        val code = extractSenderCode(sender) ?: return false
        return TRUSTED_PAYMENT_APP_CODES.any(code::contains)
    }

    fun isTrustedFinancialSender(sender: String?): Boolean =
        isTrustedBankSender(sender) || isTrustedPaymentAppSender(sender)
}
