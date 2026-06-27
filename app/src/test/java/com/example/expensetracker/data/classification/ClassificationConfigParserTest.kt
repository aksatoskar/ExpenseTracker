package com.example.expensetracker.data.classification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClassificationConfigParserTest {

    private val bundled = BundledClassificationRulesTestFixtures.config()

    @Test
    fun parseFull_readsBundledJson() {
        val config = ClassificationConfigParser.parseFull(BundledClassificationRulesTestFixtures.json())
        assertTrue(config.strongDebitKeywords.contains("debited"))
        assertEquals(80, config.notifyConfidenceThreshold)
    }

    @Test
    fun usesBundledDefaultsWhenJsonBlank() {
        val config = ClassificationConfigParser.parse("", bundled)
        assertTrue(config.strongDebitKeywords.contains("debited"))
        assertEquals(80, config.notifyConfidenceThreshold)
    }

    @Test
    fun mergesPartialRemoteJsonWithDefaults() {
        val config = ClassificationConfigParser.parse(
            """{"strong_debit_keywords":["spent"],"notify_confidence_threshold":85}""",
            bundled
        )

        assertEquals(listOf("spent"), config.strongDebitKeywords)
        assertEquals(85, config.notifyConfidenceThreshold)
        assertTrue(config.futureDebitKeywords.contains("will be debited"))
    }
}
