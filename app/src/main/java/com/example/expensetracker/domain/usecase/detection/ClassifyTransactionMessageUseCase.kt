package com.example.expensetracker.domain.usecase.detection

import com.example.expensetracker.domain.classification.MessageClassificationInput
import com.example.expensetracker.domain.classification.MessageClassificationResult
import com.example.expensetracker.domain.classification.TransactionMessageClassifier
import javax.inject.Inject

/** Runs the layered rule engine and ML fallback on a debit-looking message body. */
class ClassifyTransactionMessageUseCase @Inject constructor(
    private val classifier: TransactionMessageClassifier
) {
    operator fun invoke(input: MessageClassificationInput): MessageClassificationResult =
        classifier.classify(input)

    fun shouldNotify(input: MessageClassificationInput): Boolean =
        invoke(input).shouldNotify
}
