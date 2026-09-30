package com.akari.retailer.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.akari.retailer.R
import com.akari.retailer.core.ui.theme.AppTypography
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeFilterSelector(
    filter: TimeFilter,
    onFilterChange: (TimeFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showDayPicker by remember { mutableStateOf(false) }
    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker by remember { mutableStateOf(false) }

    val todayText = stringResource(R.string.today)
    val thisWeekText = stringResource(R.string.this_week)
    val thisMonthText = stringResource(R.string.this_month)
    val thisYearText = stringResource(R.string.this_year)

    val displayLabel = remember(filter, todayText, thisWeekText, thisMonthText, thisYearText) {
        filter.withComputedLabel(
            todayText = todayText,
            thisWeekText = thisWeekText,
            thisMonthText = thisMonthText,
            thisYearText = thisYearText
        ).label.ifEmpty { todayText }
    }

    Column(modifier = modifier) {
        // Trigger button
        ExposedDropdownMenuBox(
            expanded = menuExpanded,
            onExpandedChange = { menuExpanded = it }
        ) {
            OutlinedButton(
                onClick = { menuExpanded = true },
                modifier = Modifier.menuAnchor()
            ) {
                Text("📅 $displayLabel", style = AppTypography.small)
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = stringResource(R.string.select_range),
                    modifier = Modifier.size(16.dp)
                )
            }

            ExposedDropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                TimeFilterPreset.entries.forEach { preset ->
                    DropdownMenuItem(
                        text = { Text(presetLabel(preset)) },
                        onClick = {
                            menuExpanded = false
                            when (preset) {
                                TimeFilterPreset.SPECIFIC_DAY -> showDayPicker = true
                                TimeFilterPreset.CUSTOM_RANGE -> {
                                    // Default to "this month start/end" if not set yet
                                    val start = filter.customStartMillis
                                        ?: TimeFilter.monthStart(System.currentTimeMillis())
                                    val end = filter.customEndMillis
                                        ?: TimeFilter.monthEnd(System.currentTimeMillis())
                                    onFilterChange(
                                        filter.copy(
                                            preset = TimeFilterPreset.CUSTOM_RANGE,
                                            customStartMillis = start,
                                            customEndMillis = end
                                        )
                                    )
                                }
                                else -> onFilterChange(filter.copy(preset = preset))
                            }
                        }
                    )
                }
            }
        }

        // D1: inline From/To buttons, shown only when CUSTOM_RANGE is active
        if (filter.preset == TimeFilterPreset.CUSTOM_RANGE) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                val fmt = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }
                val startLabel = filter.customStartMillis?.let { fmt.format(Date(it)) } ?: "—"
                val endLabel = filter.customEndMillis?.let { fmt.format(Date(it)) } ?: "—"

                OutlinedButton(
                    onClick = { showFromPicker = true },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(startLabel, style = AppTypography.small)
                }
                Text("→", style = AppTypography.small)
                OutlinedButton(
                    onClick = { showToPicker = true },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(endLabel, style = AppTypography.small)
                }
            }
        }
    }

    // Day picker (specific day)
    if (showDayPicker) {
        DatePickerDialog(
            onDateSelected = { timestamp ->
                onFilterChange(
                    filter.copy(
                        preset = TimeFilterPreset.SPECIFIC_DAY,
                        specificDayMillis = timestamp
                    )
                )
                showDayPicker = false
            },
            onDismiss = { showDayPicker = false }
        )
    }

    // From picker
    if (showFromPicker) {
        DatePickerDialog(
            onDateSelected = { timestamp ->
                onFilterChange(filter.copy(customStartMillis = timestamp))
                showFromPicker = false
            },
            onDismiss = { showFromPicker = false }
        )
    }

    // To picker
    if (showToPicker) {
        DatePickerDialog(
            onDateSelected = { timestamp ->
                onFilterChange(filter.copy(customEndMillis = timestamp))
                showToPicker = false
            },
            onDismiss = { showToPicker = false }
        )
    }
}

@Composable
private fun presetLabel(preset: TimeFilterPreset): String {
    return when (preset) {
        TimeFilterPreset.TODAY -> stringResource(R.string.today)
        TimeFilterPreset.THIS_WEEK -> stringResource(R.string.this_week)
        TimeFilterPreset.THIS_MONTH -> stringResource(R.string.this_month)
        TimeFilterPreset.THIS_YEAR -> stringResource(R.string.this_year)
        TimeFilterPreset.SPECIFIC_DAY -> stringResource(R.string.specific_day)
        TimeFilterPreset.CUSTOM_RANGE -> stringResource(R.string.custom_range)
    }
}
