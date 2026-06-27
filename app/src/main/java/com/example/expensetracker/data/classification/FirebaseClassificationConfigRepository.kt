package com.example.expensetracker.data.classification

import com.example.expensetracker.domain.classification.ClassificationConfigKeys
import com.example.expensetracker.domain.classification.CompiledClassificationRules
import com.example.expensetracker.domain.repository.ClassificationConfigRepository
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlinx.coroutines.tasks.await
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseClassificationConfigRepository @Inject constructor(
    remoteConfig: FirebaseRemoteConfig,
    bundledLoader: BundledClassificationConfigLoader
) : ClassificationConfigRepository {

    private val bundled = bundledLoader

    private val remoteConfig: FirebaseRemoteConfig = remoteConfig.apply {
        setDefaultsAsync(
            mapOf(ClassificationConfigKeys.RULES_JSON to bundled.json())
        )
    }

    private val rules = AtomicReference(defaultRules())

    override fun current(): CompiledClassificationRules = rules.get()

    override suspend fun refresh() {
        runCatching { remoteConfig.fetchAndActivate().await() }
        val json = remoteConfig.getString(ClassificationConfigKeys.RULES_JSON)
        rules.set(CompiledClassificationRules.from(ClassificationConfigParser.parse(json, bundled.config())))
    }

    private fun defaultRules(): CompiledClassificationRules =
        CompiledClassificationRules.from(bundled.config())
}
