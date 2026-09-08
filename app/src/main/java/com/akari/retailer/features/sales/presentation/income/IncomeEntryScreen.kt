package com.akari.retailer.features.sales.presentation.income

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.akari.retailer.R
import com.akari.retailer.RetailApplication
import com.akari.retailer.core.ui.components.AppPrimaryButton
import com.akari.retailer.core.ui.components.AppScreen
import com.akari.retailer.core.ui.theme.AppTypography
import com.akari.retailer.core.ui.theme.Spacing
import com.akari.retailer.features.sales.data.repository.FirestoreIncomeEntryRepository
import com.akari.retailer.features.sales.data.repository.FirestoreIncomeStreamRepository
import com.akari.retailer.features.sales.data.remote.FirestoreIncomeEntryService
import com.akari.retailer.features.sales.data.remote.FirestoreIncomeStreamService
import com.akari.retailer.features.sales.domain.models.IncomeEntryType
import com.akari.retailer.navigation.Routes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeEntryScreen(
    navController: NavController? = null,
    onBack: () -> Unit,
    onIncomeAdded: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as RetailApplication
    
    val entryService = remember { FirestoreIncomeEntryService() }
    val entryRepository = remember { FirestoreIncomeEntryRepository(entryService) }
    val streamService = remember { FirestoreIncomeStreamService() }
    val streamRepository = remember { FirestoreIncomeStreamRepository(streamService) }
    
    val viewModel: IncomeEntryViewModel = viewModel(
        factory = IncomeEntryViewModelFactory(entryRepository, streamRepository)
    )
    
    val state by viewModel.state.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    AppScreen(
        title = "💰 Add Income",
        showBackButton = true,
        onBackClick = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Amount
            OutlinedTextField(
                value = state.amount,
                onValueChange = { viewModel.handleEvent(IncomeEntryEvent.AmountChanged(it)) },
                label = { Text(stringResource(R.string.amount)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            // Income Stream Dropdown
            var streamExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = streamExpanded,
                onExpandedChange = { streamExpanded = it }
            ) {
                OutlinedTextField(
                    value = state.selectedStream?.getDisplayName() ?: "Select Income Type",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Income Type") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = streamExpanded) }
                )
                ExposedDropdownMenu(
                    expanded = streamExpanded,
                    onDismissRequest = { streamExpanded = false }
                ) {
                    state.streams.forEach { stream ->
                        DropdownMenuItem(
                            text = { Text(stream.getDisplayName()) },
                            onClick = {
                                viewModel.handleEvent(IncomeEntryEvent.StreamSelected(stream))
                                streamExpanded = false
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            // Entry Type Selector
            Text(
                text = "Income Type",
                style = AppTypography.label,
                modifier = Modifier.padding(bottom = Spacing.small)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                IncomeTypeButton(
                    type = IncomeEntryType.BUSINESS,
                    label = "💼 Business",
                    isSelected = state.entryType == IncomeEntryType.BUSINESS,
                    onClick = { viewModel.handleEvent(IncomeEntryEvent.EntryTypeChanged(IncomeEntryType.BUSINESS)) },
                    modifier = Modifier.weight(1f)
                )
                IncomeTypeButton(
                    type = IncomeEntryType.PERSONAL,
                    label = "👤 Personal",
                    isSelected = state.entryType == IncomeEntryType.PERSONAL,
                    onClick = { viewModel.handleEvent(IncomeEntryEvent.EntryTypeChanged(IncomeEntryType.PERSONAL)) },
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            // Description
            OutlinedTextField(
                value = state.description,
                onValueChange = { viewModel.handleEvent(IncomeEntryEvent.DescriptionChanged(it)) },
                label = { Text(stringResource(R.string.description)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            )
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            // Date
            OutlinedTextField(
                value = dateFormat.format(Date(state.date)),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.date)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
            )
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            state.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = AppTypography.body,
                    modifier = Modifier.padding(bottom = Spacing.medium)
                )
            }
            
            AppPrimaryButton(
                text = if (state.isSaving) "Saving..." else "Save Income",
                onClick = {
                    viewModel.handleEvent(IncomeEntryEvent.SaveIncome)
                },
                isLoading = state.isSaving,
                enabled = state.amount.isNotEmpty() && state.selectedStream != null && !state.isSaving
            )
            
            // ✅ "View Income List" Button
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            OutlinedButton(
                onClick = {
                    navController?.navigate(Routes.INCOME_LIST)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("📋 View Income List")
            }
            
            if (state.saveSuccess) {
                LaunchedEffect(Unit) {
                    onIncomeAdded()
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.xxlarge))
        }
    }
    
    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { timestamp ->
                            viewModel.handleEvent(IncomeEntryEvent.DateChanged(timestamp))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun IncomeTypeButton(
    type: IncomeEntryType,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isSelected) 
                MaterialTheme.colorScheme.onPrimary 
            else 
                MaterialTheme.colorScheme.onSurface
        )
    ) {
        Text(label)
    }
}
