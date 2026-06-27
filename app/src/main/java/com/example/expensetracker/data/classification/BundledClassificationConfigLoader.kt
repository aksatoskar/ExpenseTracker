package com.example.expensetracker.data.classification

import android.content.Context
import com.example.expensetracker.domain.classification.ClassificationConfig
import com.example.expensetracker.domain.classification.ClassificationConfigAsset
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Loads bundled classification rules from [ClassificationConfigAsset.FILE_NAME]. */
@Singleton
class BundledClassificationConfigLoader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json by lazy { readAsset() }
    private val config by lazy { ClassificationConfigParser.parseFull(json) }

    fun json(): String = json

    fun config(): ClassificationConfig = config

    private fun readAsset(): String =
        context.assets.open(ClassificationConfigAsset.FILE_NAME).bufferedReader().use { it.readText() }
}
