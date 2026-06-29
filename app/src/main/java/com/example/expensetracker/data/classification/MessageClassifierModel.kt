package com.example.expensetracker.data.classification

import com.example.expensetracker.domain.classification.MessageClassificationResult
import com.example.expensetracker.domain.classification.MessageType
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Runs on-device inference for debit-message meaning (actual vs future vs other). */
class MessageClassifierModel(modelBytes: ByteArray) {

    private val interpreter: Interpreter = Interpreter(
        ByteBuffer.allocateDirect(modelBytes.size).order(ByteOrder.nativeOrder()).also {
            it.put(modelBytes)
            it.rewind()
        },
        Interpreter.Options().setNumThreads(2)
    )

    fun classify(features: FloatArray): MessageClassificationResult {
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
        const val NUM_CLASSES = 8

        val INDEX_TO_TYPE = mapOf(
            0 to MessageType.ActualDebit,
            1 to MessageType.FutureDebit,
            2 to MessageType.Credit,
            3 to MessageType.Receipt,
            4 to MessageType.Otp,
            5 to MessageType.RewardCashback,
            6 to MessageType.PhishingSpam,
            7 to MessageType.Unknown
        )
    }
}
