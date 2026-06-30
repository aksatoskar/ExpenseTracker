package com.example.expensetracker.data.classification

import android.content.Context
import com.example.expensetracker.domain.classification.MessageClassificationInput
import com.example.expensetracker.domain.classification.MessageClassificationResult
import com.example.expensetracker.domain.classification.MessageType
import com.example.expensetracker.domain.classification.TransactionMessageClassifier
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Static rules handle unambiguous safety cases (OTP, phishing, credits, receipts) and well-known
 * completed bank debits. Remaining debit-looking messages are classified by TFLite.
 */
@Singleton
class TfliteMessageClassifier @Inject constructor(
    @ApplicationContext context: Context,
    private val featureExtractor: MessageFeatureExtractor,
    private val staticRules: MessageClassificationRules
) : TransactionMessageClassifier {

    private val model: MessageClassifierModel? = loadModel(context)

    override fun classify(input: MessageClassificationInput): MessageClassificationResult {
        staticRules.evaluate(input)?.let { return it }

        val model = model ?: return MessageClassificationResult(
            type = MessageType.Unknown,
            confidence = 0,
            reason = "model_unavailable"
        )

        val features = featureExtractor.featurize(input)
        return model.classify(features)
    }

    companion object {
        private const val MODEL_ASSET = "message_classifier.tflite"

        private fun loadModel(context: Context): MessageClassifierModel? = try {
            val bytes = context.assets.open(MODEL_ASSET).use { it.readBytes() }
            MessageClassifierModel(bytes)
        } catch (_: Exception) {
            null
        }
    }
}
