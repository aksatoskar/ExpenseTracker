package com.example.expensetracker.presentation.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.expensetracker.domain.model.Category
import com.example.expensetracker.domain.model.Priority

/** Rotating palette for charts and bars. */
val ChartColors = listOf(
    Color(0xFF6366F1), Color(0xFFEC4899), Color(0xFF14B8A6), Color(0xFFF59E0B),
    Color(0xFF8B5CF6), Color(0xFFEF4444), Color(0xFF06B6D4), Color(0xFF84CC16)
)

val PriorityEssentialColor = Color(0xFF059669)
val PriorityOptionalColor = Color(0xFFEA580C)
val PriorityWastefulColor = Color(0xFFEC4899)

fun priorityColor(priority: Priority): Color = when (priority) {
    Priority.Essential -> PriorityEssentialColor
    Priority.Optional -> PriorityOptionalColor
    Priority.Wasteful -> PriorityWastefulColor
}

/** Distinct accent for user-defined categories. */
fun customCategoryColor() = Color(0xFF7C3AED)

/** Stable accent color per category (null/Other render neutral grey). */
fun categoryColor(category: Category?, isCustom: Boolean = false): Color {
    if (isCustom) return customCategoryColor()
    return when (category) {
    Category.FoodDining -> Color(0xFFEF4444)
    Category.Shopping -> Color(0xFFEC4899)
    Category.Travel -> Color(0xFF06B6D4)
    Category.BillsUtilities -> Color(0xFF6366F1)
    Category.RentHome -> Color(0xFF8B5CF6)
    Category.Health -> Color(0xFF10B981)
    Category.Education -> Color(0xFF3B82F6)
    Category.Investments -> Color(0xFF059669)
    Category.Entertainment -> Color(0xFFF59E0B)
    Category.Other, null -> Color(0xFF94A3B8)
    }
}

/** Representative icon per category. */
fun categoryIcon(category: Category?, isCustom: Boolean = false): ImageVector {
    if (isCustom) return Icons.Default.Star
    return when (category) {
    Category.FoodDining -> Icons.Default.Restaurant
    Category.Shopping -> Icons.Default.ShoppingCart
    Category.Travel -> Icons.Default.Flight
    Category.BillsUtilities -> Icons.Default.ReceiptLong
    Category.RentHome -> Icons.Default.Home
    Category.Health -> Icons.Default.LocalHospital
    Category.Education -> Icons.Default.School
    Category.Investments -> Icons.Default.TrendingUp
    Category.Entertainment -> Icons.Default.Movie
    Category.Other, null -> Icons.Default.Category
    }
}
