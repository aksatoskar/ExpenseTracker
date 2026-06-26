package com.example.expensetracker.domain.classification

/** Classifies whether a debit-looking message is a real spend, spam, or otherwise invalid. */
interface TransactionMessageClassifier {
    fun classify(input: MessageClassificationInput): MessageClassificationResult
}
