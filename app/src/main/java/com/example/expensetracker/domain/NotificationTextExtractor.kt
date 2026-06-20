package com.example.expensetracker.domain

import android.os.Bundle

object NotificationTextExtractor {

    private val textKeys = listOf(
        "android.title",
        "android.text",
        "android.subText",
        "android.infoText",
        "android.summaryText",
        "android.bigText"
    )

    fun extract(extras: Bundle): String {
        val parts = linkedSetOf<String>()

        textKeys.forEach { key ->
            extras.getCharSequence(key)?.toString()?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
        }

        extras.getParcelableArray("android.messages")?.forEach { parcel ->
            if (parcel is Bundle) {
                parcel.getCharSequence("text")?.toString()?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
            }
        }

        @Suppress("DEPRECATION")
        extras.get("android.textLines")?.let { lines ->
            when (lines) {
                is Array<*> -> lines.filterIsInstance<CharSequence>().forEach { parts.add(it.toString()) }
                is List<*> -> lines.filterIsInstance<CharSequence>().forEach { parts.add(it.toString()) }
            }
        }

        extras.keySet().forEach { key ->
            if (key.startsWith("android.") || key.startsWith("androidx.")) return@forEach
            val value = extras.get(key)
            when (value) {
                is CharSequence -> value.toString().takeIf { it.isNotBlank() }?.let { parts.add(it) }
                is Array<*> -> value.filterIsInstance<CharSequence>().forEach { parts.add(it.toString()) }
            }
        }

        return parts.joinToString(" ")
    }
}
