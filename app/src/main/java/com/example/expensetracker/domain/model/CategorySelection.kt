package com.example.expensetracker.domain.model

/** A built-in or user-defined category chosen in the UI. */
sealed interface CategorySelection {
    val label: String

    data class BuiltIn(val category: Category) : CategorySelection {
        override val label: String get() = category.label
    }

    data class Custom(val id: Long, val name: String) : CategorySelection {
        override val label: String get() = name
    }
}

/** Filter for the transactions list: all, a built-in category, or a custom one. */
sealed interface CategoryFilter {
    data object All : CategoryFilter

    data class BuiltIn(val category: Category) : CategoryFilter

    data class Custom(val id: Long) : CategoryFilter
}

fun CategorySelection.toEntityFields(): Pair<Category?, Long?> = when (this) {
    is CategorySelection.BuiltIn -> category to null
    is CategorySelection.Custom -> null to id
}

fun resolveCategorySelection(
    category: Category?,
    customCategoryId: Long?,
    customNames: Map<Long, String>
): CategorySelection? {
    if (customCategoryId != null) {
        val name = customNames[customCategoryId] ?: "Custom"
        return CategorySelection.Custom(customCategoryId, name)
    }
    return category?.let { CategorySelection.BuiltIn(it) }
}

fun categoryLabel(
    category: Category?,
    customCategoryId: Long?,
    customNames: Map<Long, String>
): String = resolveCategorySelection(category, customCategoryId, customNames)?.label ?: "Uncategorized"
