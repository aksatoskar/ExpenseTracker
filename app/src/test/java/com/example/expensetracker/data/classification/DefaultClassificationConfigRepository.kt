package com.example.expensetracker.data.classification

import com.example.expensetracker.domain.classification.CompiledClassificationRules
import com.example.expensetracker.domain.repository.ClassificationConfigRepository

/** In-memory classification config for unit tests (bundled JSON). */
class DefaultClassificationConfigRepository : ClassificationConfigRepository {
    private val rules = BundledClassificationRulesTestFixtures.compiledRules()

    override fun current(): CompiledClassificationRules = rules

    override suspend fun refresh() = Unit
}
