package com.example.expensetracker.domain.repository

import com.example.expensetracker.domain.classification.CompiledClassificationRules

/** Provides SMS classification rules from Remote Config with bundled fallbacks. */
interface ClassificationConfigRepository {
    fun current(): CompiledClassificationRules
    suspend fun refresh()
}
