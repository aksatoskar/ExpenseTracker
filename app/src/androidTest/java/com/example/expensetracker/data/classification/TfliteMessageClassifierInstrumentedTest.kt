package com.example.expensetracker.data.classification

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.expensetracker.domain.classification.CompiledClassificationRules
import com.example.expensetracker.domain.classification.MessageClassificationInput
import com.example.expensetracker.domain.classification.MessageClassificationResult
import com.example.expensetracker.domain.classification.MessageType
import com.example.expensetracker.domain.repository.ClassificationConfigRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TfliteMessageClassifierInstrumentedTest {
    private lateinit var classifier: TfliteMessageClassifier

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val configRepository = AssetClassificationConfigRepository(context)
        classifier = TfliteMessageClassifier(
            context = context,
            featureExtractor = MessageFeatureExtractor(),
            staticRules = MessageClassificationRules(configRepository, SenderValidator(configRepository))
        )
    }

    private class AssetClassificationConfigRepository(context: Context) : ClassificationConfigRepository {
        private val rules = CompiledClassificationRules.from(
            ClassificationConfigParser.parseFull(
                context.assets.open("classification_rules.json").bufferedReader().use { it.readText() }
            )
        )

        override fun current(): CompiledClassificationRules = rules

        override suspend fun refresh() = Unit
    }

    @Test
    fun classifiesFederalBankDebitAsActualDebit() {
        val receivedAt = java.time.LocalDate.of(2026, 6, 27)
            .atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val result = classifier.classify(
            MessageClassificationInput(
                rawText = "Debited Rs 2.00 from a/c X2685 on 27Jun26 00:20 via UPI to aksatoskar-1. " +
                    "Ref 617817479920.Bal Rs 11660.38. Not you?Call 18004251199 -Federal Bank",
                source = "SMS",
                receivedAtMillis = receivedAt,
                sender = "VA-FEDBNK-T"
            )
        )

        assertEquals(MessageType.ActualDebit, result.type)
        assertTrue(result.confidence >= MessageClassificationResult.NOTIFY_THRESHOLD)
    }

    @Test
    fun classifiesHdfcSentFromAccountDebitAsActualDebit() {
        val receivedAt = java.time.LocalDate.of(2026, 6, 28)
            .atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val result = classifier.classify(
            MessageClassificationInput(
                rawText = "Sent Rs.3580.00 From HDFC Bank A/C *4915 To SHREEDEVA FOODS On 28/06/26 " +
                    "Ref 125451627257 Not You? Call 18002586161/SMS BLOCK UPI to 7308080808",
                source = "SMS",
                receivedAtMillis = receivedAt,
                sender = "JM-HDFCBK-T"
            )
        )

        assertEquals(MessageType.ActualDebit, result.type)
        assertTrue(result.confidence >= MessageClassificationResult.NOTIFY_THRESHOLD)
    }

    @Test
    fun classifiesGrowwSipReminderAsFutureDebit() {
        val result = classifier.classify(
            MessageClassificationInput(
                rawText = "SIP: Instalment due in 2 days ₹20,000.00 will be deducted for " +
                    "Parag Parikh Flexi Cap Fund Direct Growth. Please ensure sufficient bank balance.",
                source = "Notification",
                receivedAtMillis = System.currentTimeMillis(),
                notificationPackage = "com.nextbillion.groww"
            )
        )

        assertEquals(MessageType.FutureDebit, result.type)
    }

    @Test
    fun classifiesUpcomingAutopayAsFutureDebit() {
        val result = classifier.classify(
            MessageClassificationInput(
                rawText = "Your autopay of Rs 999 will be debited on 30-Jun-26 from A/c *1234",
                source = "SMS",
                receivedAtMillis = System.currentTimeMillis()
            )
        )

        assertEquals(MessageType.FutureDebit, result.type)
    }
}
