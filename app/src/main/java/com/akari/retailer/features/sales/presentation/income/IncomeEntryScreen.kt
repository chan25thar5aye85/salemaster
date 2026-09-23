package com.akari.retailer.features.sales.presentation.income

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.akari.retailer.core.ui.components.PaymentListComponent
import com.akari.retailer.core.ui.theme.AppTypography
import com.akari.retailer.core.ui.theme.Spacing
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
    
    
    val viewModel: IncomeEntryViewModel = viewModel(
        factory = IncomeEntryViewModelFactory(
            application.container.incomeEntryRepository,
            application.container.incomeStreamRepository,
            application.container.moneyAccountRepository,
            application.container.processMoneyTransactionUseCase,
            application.container.paymentPreferences
        )
    )
    
    val state by viewModel.state.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }
    var streamExpanded by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    AppScreen(
        title = stringResource(R.string.add_income),
        showBackButton = true,
        onBackClick = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
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
            ExposedDropdownMenuBox(
                expanded = streamExpanded,
                onExpandedChange = { streamExpanded = it }
            ) {
                OutlinedTextField(
                    value = state.selectedStream?.getDisplayName() ?: stringResource(R.string.select_income_type),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.income_stream)) },
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
            
            // ✅ REUSABLE PAYMENT COMPONENT
            PaymentListComponent(
                paymentRows = state.paymentRows,
                accounts = state.accounts,
                totalAmount = state.getTotalAmount(),
                onAccountSelected = { rowId, account ->
                    viewModel.handleEvent(IncomeEntryEvent.PaymentAccountChanged(rowId, account))
                },
                onAmountChanged = { rowId, amount ->
                    viewModel.handleEvent(IncomeEntryEvent.PaymentAmountChanged(rowId, amount))
                },
                onAddRow = { viewModel.handleEvent(IncomeEntryEvent.AddPaymentRow) },
                onRemoveRow = { rowId -> viewModel.handleEvent(IncomeEntryEvent.RemovePaymentRow(rowId)) }
            )
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            // Category (Business/Personal)
            Text(
                text = stringResource(R.string.entry_category),
                style = AppTypography.label,
                modifier = Modifier.padding(bottom = Spacing.small)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IncomeTypeButton(
                    label = stringResource(R.string.income_business_icon),
                    isSelected = state.entryType == IncomeEntryType.BUSINESS,
                    onClick = { viewModel.handleEvent(IncomeEntryEvent.EntryTypeChanged(IncomeEntryType.BUSINESS)) },
                    modifier = Modifier.weight(1f)
                )
                IncomeTypeButton(
                    label = stringResource(R.string.income_personal_icon),
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
                    .height(80.dp)
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = AppTypography.body,
                        modifier = Modifier.padding(Spacing.small)
                    )
                }
                Spacer(modifier = Modifier.height(Spacing.medium))
            }
            
            AppPrimaryButton(
                text = if (state.isSaving) stringResource(R.string.saving) else stringResource(R.string.save_income),
                onClick = { viewModel.handleEvent(IncomeEntryEvent.SaveIncome) },
                isLoading = state.isSaving,
                enabled = state.amount.isNotEmpty() && 
                         state.selectedStream != null && 
                         state.isFullyPaid() &&
                         !state.isSaving
            )
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            OutlinedButton(
                onClick = { navController?.navigate(Routes.INCOME_LIST) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.view_income_list))
            }
            
            if (state.saveSuccess) {
                LaunchedEffect(Unit) { onIncomeAdded() }
            }
            
            Spacer(modifier = Modifier.height(Spacing.xxlarge))
        }
    }
    
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
                ) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun IncomeTypeButton(
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
