package com.akari.retailer.core.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.akari.retailer.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState()
    
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { timestamp ->
                        onDateSelected(timestamp)
                    }
                    onDismiss()
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangePickerDialog(
    onDateRangeSelected: (startDate: Long, endDate: Long) -> Unit,
    onDismiss: () -> Unit
) {
    var startDate by remember { mutableStateOf<Long?>(null) }
    var endDate by remember { mutableStateOf<Long?>(null) }
    
    val datePickerState = rememberDatePickerState()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (startDate == null) "Select Start Date" else "Select End Date"
            )
        },
        text = {
            DatePicker(state = datePickerState)
        },
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { timestamp ->
                        if (startDate == null) {
                            startDate = timestamp
                            // Reset picker for end date selection
                        } else if (endDate == null) {
                            endDate = timestamp
                            // Both dates selected, trigger callback
                            startDate?.let { start ->
                                endDate?.let { end ->
                                    onDateRangeSelected(start, end)
                                }
                            }
                            onDismiss()
                        }
                    }
                }
            ) {
                Text(
                    text = if (startDate == null) "Select Start" else "Select End"
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (startDate != null && endDate == null) {
                        // Cancel end date selection, go back to start
                        startDate = null
                    } else {
                        onDismiss()
                    }
                }
            ) {
                Text(
                    text = if (startDate != null && endDate == null) "Back" else stringResource(R.string.cancel)
                )
            }
        }
    )
}
