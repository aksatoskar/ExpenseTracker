package com.example.expensetracker.data.classification

import com.example.expensetracker.domain.classification.MessageClassificationInput
import java.util.Locale
import java.util.regex.Pattern
import javax.inject.Inject
import kotlin.math.abs

/** Builds the same hashed bag-of-words vector used to train [message_classifier.tflite]. */
class MessageFeatureExtractor @Inject constructor() {

    fun featurize(text: String): FloatArray {
        val vector = FloatArray(HASH_DIM)
        TOKEN_PATTERN.matcher(text.lowercase(Locale.US)).let { matcher ->
            while (matcher.find()) {
                val token = matcher.group() ?: continue
                val bucket = abs(token.hashCode()) % HASH_DIM
                vector[bucket] += 1f
            }
        }
        normalize(vector)
        return vector
    }

    fun featurize(input: MessageClassificationInput): FloatArray = featurize(input.rawText)

    private fun normalize(vector: FloatArray) {
        var sumSquares = 0f
        for (value in vector) {
            sumSquares += value * value
        }
        if (sumSquares <= 0f) return
        val scale = 1f / kotlin.math.sqrt(sumSquares)
        for (index in vector.indices) {
            vector[index] *= scale
        }
    }

    companion object {
        const val HASH_DIM = 256
        private val TOKEN_PATTERN = Pattern.compile("[a-z0-9]+")
    }
}
