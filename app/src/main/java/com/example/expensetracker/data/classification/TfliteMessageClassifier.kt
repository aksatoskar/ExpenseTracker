package com.example.expensetracker.data.classification

import android.content.Context
import com.example.expensetracker.domain.classification.MessageClassificationInput
import com.example.expensetracker.domain.classification.MessageClassificationResult
import com.example.expensetracker.domain.classification.MessageType
import com.example.expensetracker.domain.classification.TransactionMessageClassifier
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Rules-first classifier with TFLite fallback for ambiguous messages (~10–15%).
 * Notifications fire only for [MessageType.ActualDebit] with confidence ≥ 80.
 */
@Singleton
class TfliteMessageClassifier @Inject constructor(
    @ApplicationContext context: Context,
    private val featureExtractor: MessageFeatureExtractor,
    private val rules: MessageClassificationRules
) : TransactionMessageClassifier {

    private val interpreter: Interpreter? = loadInterpreter(context)

    override fun classify(input: MessageClassificationInput): MessageClassificationResult {
        rules.evaluate(input)?.let { return it }

        val interpreter = interpreter ?: return MessageClassificationResult(
            type = MessageType.Unknown,
            confidence = 0,
            reason = "model_unavailable"
        )

        val features = featureExtractor.featurize(input)
        val output = Array(1) { FloatArray(NUM_CLASSES) }
        interpreter.run(toInputBuffer(features), output)

        val scores = output[0]
        val bestIndex = scores.indices.maxByOrNull(scores::get) ?: NUM_CLASSES - 1
        val type = INDEX_TO_TYPE[bestIndex] ?: MessageType.Unknown
        val confidence = (scores[bestIndex] * 100f).toInt().coerceIn(0, 100)

        if (type == MessageType.ActualDebit && confidence < MessageClassificationResult.NOTIFY_THRESHOLD) {
            val runnerUpIndex = scores.indices.filter { it != bestIndex }.maxByOrNull(scores::get) ?: bestIndex
            val runnerUpType = INDEX_TO_TYPE[runnerUpIndex] ?: MessageType.Unknown
            return MessageClassificationResult(
                type = runnerUpType,
                confidence = (scores[runnerUpIndex] * 100f).toInt().coerceIn(0, 100),
                reason = "ml_low_actual_debit_confidence"
            )
        }

        return MessageClassificationResult(
            type = type,
            confidence = confidence,
            reason = "ml_classifier"
        )
    }

    private fun toInputBuffer(features: FloatArray): ByteBuffer =
        ByteBuffer.allocateDirect(features.size * Float.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
            .also { buffer ->
                features.forEach(buffer::putFloat)
                buffer.rewind()
            }

    companion object {
        private const val MODEL_ASSET = "message_classifier.tflite"
        private const val NUM_CLASSES = 8

        private val INDEX_TO_TYPE = mapOf(
            0 to MessageType.ActualDebit,
            1 to MessageType.FutureDebit,
            2 to MessageType.Credit,
            3 to MessageType.Receipt,
            4 to MessageType.Otp,
            5 to MessageType.RewardCashback,
            6 to MessageType.PhishingSpam,
            7 to MessageType.Unknown
        )

        private fun loadInterpreter(context: Context): Interpreter? = try {
            val bytes = context.assets.open(MODEL_ASSET).use { it.readBytes() }
            val buffer = ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder())
            buffer.put(bytes)
            buffer.rewind()
            Interpreter(buffer, Interpreter.Options().setNumThreads(2))
        } catch (_: Exception) {
            null
        }
    }
}
