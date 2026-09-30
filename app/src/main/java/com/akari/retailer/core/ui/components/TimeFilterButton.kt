package com.akari.retailer.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.akari.retailer.core.ui.theme.AppTypography

/**
 * Top-bar filter control: shows the current TimeFilter preset as a
 * labeled button. Tapping it opens a small dropdown of presets.
 *
 * Simple presets apply immediately.
 * "Specific Day…" opens a single date picker.
 * "Custom Range…" opens two sequential date pickers — first for the
 * starting date, then for the ending date.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeFilterButton(
    filter: TimeFilter,
    onPresetChange: (TimeFilter) -> Unit,
    onPickSpecificDay: (Long) -> Unit,
    onPickCustomRange: (Long, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var showDayPicker by remember { mutableStateOf(false) }
    var showRangeStartPicker by remember { mutableStateOf(false) }
    var showRangeEndPicker by remember { mutableStateOf(false) }
    var pendingStart: Long? by remember { mutableStateOf(null) }

    // Compute the display label from the filter's current state.
    // Shows actual date(s) when the preset is Specific Day or Custom Range.
    val label = run {
        val dayFmt = java.text.SimpleDateFormat("dd MMM", java.util.Locale.getDefault())
        when (filter.preset) {
            TimeFilterPreset.SPECIFIC_DAY -> {
                val ts = filter.specificDayMillis
                if (ts != null && ts > 0) dayFmt.format(java.util.Date(ts)) else "Specific Day"
            }
            TimeFilterPreset.CUSTOM_RANGE -> {
                val s = filter.customStartMillis
                val e = filter.customEndMillis
                if (s != null && e != null && s > 0 && e > 0) {
                    val lo = minOf(s, e)
                    val hi = maxOf(s, e)
                    "${dayFmt.format(java.util.Date(lo))} – ${dayFmt.format(java.util.Date(hi))}"
                } else "Custom Range"
            }
            TimeFilterPreset.TODAY -> "Today"
            TimeFilterPreset.THIS_WEEK -> "This Week"
            TimeFilterPreset.THIS_MONTH -> "This Month"
            TimeFilterPreset.THIS_YEAR -> "This Year"
        }
    }

    Box {
        TextButton(
            onClick = { expanded = true },
            modifier = modifier
        ) {
            Text(
                text = "📅 $label",
                style = AppTypography.small
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            listOf(
                TimeFilterPreset.TODAY to "Today",
                TimeFilterPreset.THIS_WEEK to "This Week",
                TimeFilterPreset.THIS_MONTH to "This Month",
                TimeFilterPreset.THIS_YEAR to "This Year",
            ).forEach { (preset, text) ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text, style = AppTypography.body)
                            if (filter.preset == preset) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "✓",
                                    style = AppTypography.body,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    onClick = {
                        expanded = false
                        onPresetChange(filter.copy(preset = preset))
                    }
                )
            }

            HorizontalDivider()

            DropdownMenuItem(
                text = { Text("Specific Day…", style = AppTypography.body) },
                onClick = {
                    expanded = false
                    showDayPicker = true
                }
            )
            DropdownMenuItem(
                text = { Text("Custom Range…", style = AppTypography.body) },
                onClick = {
                    expanded = false
                    pendingStart = null
                    showRangeStartPicker = true
                }
            )
        }
    }

    // ── Specific Day picker (single dialog) ──
    if (showDayPicker) {
        DatePickerDialog(
            onDateSelected = { timestamp ->
                onPickSpecificDay(timestamp)
                showDayPicker = false
            },
            onDismiss = { showDayPicker = false },
            title = "Specific Day"
        )
    }

    // ── Custom Range: step 1 — Starting date ──
    if (showRangeStartPicker) {
        DatePickerDialog(
            onDateSelected = { timestamp ->
                pendingStart = timestamp
                showRangeStartPicker = false
                showRangeEndPicker = true
            },
            onDismiss = {
                // IMPORTANT: Do NOT clear pendingStart here.
                // Material3's DatePickerDialog fires onDismiss after confirm too,
                // which would wipe the value before the end picker reads it.
                // pendingStart is reset when the flow starts (in the menu item)
                // and when the end picker finishes/dismisses.
                showRangeStartPicker = false
            },
            title = "Starting Date"
        )
    }

    // ── Custom Range: step 2 — Ending date ──
    if (showRangeEndPicker) {
        DatePickerDialog(
            onDateSelected = { endTimestamp ->
                val start = pendingStart
                if (start != null) {
                    // Normalize: ensure start <= end
                    val realStart = minOf(start, endTimestamp)
                    val realEnd = maxOf(start, endTimestamp)
                    onPickCustomRange(realStart, realEnd)
                }
                pendingStart = null
                showRangeEndPicker = false
            },
            onDismiss = {
                pendingStart = null
                showRangeEndPicker = false
            }
        )
    }
}
