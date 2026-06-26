package com.example.expensetracker.data.classification

import com.example.expensetracker.domain.classification.MessageClassificationInput

/** Builds the combined sender + body string used for ML featurization and training. */
object ClassificationTextBuilder {

    fun build(input: MessageClassificationInput): String =
        build(sender = input.sender ?: input.notificationPackage, message = input.rawText)

    fun build(sender: String?, message: String): String {
        val trimmedSender = sender?.trim().orEmpty()
        val trimmedMessage = message.trim()
        return if (trimmedSender.isBlank()) trimmedMessage else "$trimmedSender $trimmedMessage"
    }
}
