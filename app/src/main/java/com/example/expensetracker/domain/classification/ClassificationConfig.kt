package com.example.expensetracker.domain.classification

/** Remote-updatable SMS classification rules; bundled JSON in assets supplies fallbacks. */
data class ClassificationConfig(
    val strongDebitKeywords: List<String>,
    val futureDebitKeywords: List<String>,
    val completedExecutionKeywords: List<String>,
    val strongUpiKeywords: List<String>,
    val weakUpiKeywords: List<String>,
    val upiBrandKeywords: List<String>,
    val messagingAppPackages: Set<String>,
    val bankEntityHintPattern: String,
    val paymentAppHintPattern: String,
    val bankBodyPattern: String,
    val accountBodyPattern: String,
    val debitBodyPattern: String,
    val otpPattern: String,
    val phishingPattern: String,
    val creditPattern: String,
    val debitPattern: String,
    val payeeCreditedPattern: String,
    val rewardPattern: String,
    val receiptPattern: String,
    val amountPattern: String,
    val suspiciousLinkPattern: String,
    val bankSmsPrefixPattern: String,
    val sentFromAccountPattern: String,
    val notifyConfidenceThreshold: Int
)
