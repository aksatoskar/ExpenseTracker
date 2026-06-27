package com.example.expensetracker.data.classification

import com.example.expensetracker.domain.classification.ClassificationConfig
import com.example.expensetracker.domain.classification.CompiledClassificationRules

/** Loads bundled classification rules from test classpath (mirrors app assets). */
object BundledClassificationRulesTestFixtures {
    fun json(): String = readResource()

    fun config(): ClassificationConfig = ClassificationConfigParser.parseFull(json())

    fun compiledRules(): CompiledClassificationRules = CompiledClassificationRules.from(config())

    private fun readResource(): String {
        val stream = BundledClassificationRulesTestFixtures::class.java.classLoader
            ?.getResourceAsStream("classification_rules.json")
            ?: error("Missing test resource classification_rules.json")
        return stream.bufferedReader().use { it.readText() }
    }
}
