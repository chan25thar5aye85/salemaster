package com.akari.retailer.features.sales.presentation.income

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akari.retailer.R
import com.akari.retailer.RetailApplication
import com.akari.retailer.core.ui.components.AppPrimaryButton
import com.akari.retailer.core.ui.components.AppScreen
import com.akari.retailer.core.ui.components.PaymentListComponent
import com.akari.retailer.core.ui.theme.AppTypography
import com.akari.retailer.core.ui.theme.Spacing
import com.akari.retailer.features.sales.domain.models.IncomeEntryType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeEditScreen(
    entryId: String,
    onBack: () -> Unit,
    onEntryUpdated: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as RetailApplication

    val viewModel: IncomeEditViewModel = viewModel(
        factory = IncomeEditViewModelFactory(
            application.container.incomeEntryRepository,
            application.container.incomeStreamRepository,
            application.container.moneyAccountRepository,
            application.container.incomeFinalizer,
            application.container.paymentPreferences,
            entryId
        )
    )

    val state by viewModel.state.collectAsState()
    var streamExpanded by remember { mutableStateOf(false) }

    AppScreen(
        title = stringResource(R.string.edit_income),
        showBackButton = true,
        onBackClick = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
        ) {
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            if (state.error != null && state.id.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("❌", style = AppTypography.header)
                        Text(
                            text = state.error!!,
                            style = AppTypography.body,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = Spacing.medium)
                        )
                        AppPrimaryButton(
                            text = stringResource(R.string.retry),
                            onClick = { viewModel.handleEvent(IncomeEditEvent.LoadEntry) }
                        )
                    }
                }
                return@Column
            }

            // Amount
            OutlinedTextField(
                value = state.amount,
                onValueChange = { viewModel.handleEvent(IncomeEditEvent.AmountChanged(it)) },
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
                                viewModel.handleEvent(IncomeEditEvent.StreamSelected(stream))
                                streamExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            // Payment component
            PaymentListComponent(
                paymentRows = state.paymentRows,
                accounts = state.accounts,
                totalAmount = state.getTotalAmount(),
                showCreditOption = false,
                onAccountSelected = { rowId, account ->
                    viewModel.handleEvent(IncomeEditEvent.PaymentAccountChanged(rowId, account))
                },
                onAmountChanged = { rowId, amount ->
                    viewModel.handleEvent(IncomeEditEvent.PaymentAmountChanged(rowId, amount))
                },
                onAddRow = { viewModel.handleEvent(IncomeEditEvent.AddPaymentRow) },
                onRemoveRow = { rowId -> viewModel.handleEvent(IncomeEditEvent.RemovePaymentRow(rowId)) }
            )

            Spacer(modifier = Modifier.height(Spacing.medium))

            // Type toggle
            Text(
                text = stringResource(R.string.entry_category),
                style = AppTypography.label,
                modifier = Modifier.padding(bottom = Spacing.small)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IncomeEditTypeButton(
                    label = stringResource(R.string.income_business_icon),
                    isSelected = state.entryType == IncomeEntryType.BUSINESS,
                    onClick = { viewModel.handleEvent(IncomeEditEvent.EntryTypeChanged(IncomeEntryType.BUSINESS)) },
                    modifier = Modifier.weight(1f)
                )
                IncomeEditTypeButton(
                    label = stringResource(R.string.income_personal_icon),
                    isSelected = state.entryType == IncomeEntryType.PERSONAL,
                    onClick = { viewModel.handleEvent(IncomeEditEvent.EntryTypeChanged(IncomeEntryType.PERSONAL)) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            // Description
            OutlinedTextField(
                value = state.description,
                onValueChange = { viewModel.handleEvent(IncomeEditEvent.DescriptionChanged(it)) },
                label = { Text(stringResource(R.string.description)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            )

            Spacer(modifier = Modifier.height(Spacing.medium))

            state.error?.let { err ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        style = AppTypography.body,
                        modifier = Modifier.padding(Spacing.small)
                    )
                }
                Spacer(modifier = Modifier.height(Spacing.medium))
            }

            AppPrimaryButton(
                text = if (state.isSaving) stringResource(R.string.saving) else stringResource(R.string.update_income),
                onClick = { viewModel.handleEvent(IncomeEditEvent.SaveEntry) },
                isLoading = state.isSaving,
                enabled = state.amount.isNotEmpty() &&
                          state.selectedStream != null &&
                          state.isFullyPaid() &&
                          !state.isSaving
            )

            if (state.saveSuccess) {
                LaunchedEffect(Unit) { onEntryUpdated() }
            }

            Spacer(modifier = Modifier.height(Spacing.xxlarge))
        }
    }
}

@Composable
private fun IncomeEditTypeButton(
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
