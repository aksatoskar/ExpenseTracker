package com.example.expensetracker.data.classification

import android.content.Context
import com.example.expensetracker.domain.classification.MessageClassificationInput
import com.example.expensetracker.domain.classification.MessageClassificationResult
import com.example.expensetracker.domain.classification.MessageLabel
import com.example.expensetracker.domain.classification.TransactionMessageClassifier
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
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
            label = MessageLabel.ValidDebit,
            confidence = 0f,
            reason = "model_unavailable"
        )

        val features = featureExtractor.featurize(input)
        val output = Array(1) { FloatArray(NUM_CLASSES) }
        interpreter.run(toInputBuffer(features), output)

        val scores = output[0]
        val bestIndex = scores.indices.maxByOrNull(scores::get) ?: 0
        val label = INDEX_TO_LABEL[bestIndex] ?: MessageLabel.ValidDebit
        val confidence = scores[bestIndex]

        if (label == MessageLabel.ValidDebit && confidence < MIN_VALID_CONFIDENCE) {
            val runnerUpIndex = scores.indices.filter { it != bestIndex }.maxByOrNull(scores::get) ?: bestIndex
            val runnerUpLabel = INDEX_TO_LABEL[runnerUpIndex] ?: MessageLabel.Spam
            return MessageClassificationResult(
                label = runnerUpLabel,
                confidence = scores[runnerUpIndex],
                reason = "low_valid_confidence"
            )
        }

        return MessageClassificationResult(label = label, confidence = confidence)
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
        private const val NUM_CLASSES = 3
        private const val MIN_VALID_CONFIDENCE = 0.55f

        private val INDEX_TO_LABEL = mapOf(
            0 to MessageLabel.ValidDebit,
            1 to MessageLabel.Spam,
            2 to MessageLabel.Invalid
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
