package com.example.expensetracker.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private const val SYNC_LOOKBACK_YEARS = 5

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomSmsSyncDialog(
    onDismiss: () -> Unit,
    onConfirm: (from: LocalDate, to: LocalDate) -> Unit
) {
    val zone = ZoneId.systemDefault()
    val today = remember { LocalDate.now() }
    val earliest = remember { today.minusYears(SYNC_LOOKBACK_YEARS.toLong()) }
    val todayMillis = today.atStartOfDay(zone).toInstant().toEpochMilli()
    val pickerColors = dateRangePickerColors()

    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = todayMillis,
        initialSelectedEndDateMillis = todayMillis,
        selectableDates = remember(earliest, today) {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val date = Instant.ofEpochMilli(utcTimeMillis).atZone(zone).toLocalDate()
                    return !date.isBefore(earliest) && !date.isAfter(today)
                }

                override fun isSelectableYear(year: Int): Boolean =
                    year in earliest.year..today.year
            }
        }
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val fromMillis = state.selectedStartDateMillis ?: return@TextButton
                    val toMillis = state.selectedEndDateMillis ?: return@TextButton
                    val from = Instant.ofEpochMilli(fromMillis).atZone(zone).toLocalDate()
                    val to = Instant.ofEpochMilli(toMillis).atZone(zone).toLocalDate()
                    onConfirm(from, to)
                },
                enabled = state.selectedStartDateMillis != null && state.selectedEndDateMillis != null
            ) { Text("Sync") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    ) {
        Column(Modifier.fillMaxWidth()) {
            DateRangePicker(
                state = state,
                colors = pickerColors,
                title = {
                    Text(
                        modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
                        text = "Custom SMS sync"
                    )
                },
                headline = {
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    ) {
                        Text("Select start and end dates")
                        Text(
                            "Scans your SMS inbox for bank debits in the selected range.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                showModeToggle = false
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun dateRangePickerColors() = with(MaterialTheme.colorScheme) {
    val isLight = background.luminance() > 0.5f
    DatePickerDefaults.colors(
        selectedDayContainerColor = primary,
        selectedDayContentColor = onPrimary,
        dayInSelectionRangeContainerColor = if (isLight) {
            Color(0xFFA78BFA)
        } else {
            Color(0xFF5B21B6)
        },
        dayInSelectionRangeContentColor = if (isLight) {
            Color(0xFF3B0764)
        } else {
            Color(0xFFF3EEFF)
        },
        todayDateBorderColor = primary,
        todayContentColor = primary
    )
}
