package com.akari.retailer.features.money.presentation

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akari.retailer.R
import com.akari.retailer.RetailApplication
import com.akari.retailer.core.ui.components.AppCard
import com.akari.retailer.core.ui.components.AppPrimaryButton
import com.akari.retailer.core.ui.components.AppScreen
import com.akari.retailer.core.ui.theme.AppTypography
import com.akari.retailer.core.ui.theme.Spacing
import com.akari.retailer.features.money.data.repository.FirestoreMoneyAccountRepository
import com.akari.retailer.features.money.data.repository.FirestoreMoneyTransactionRepository
import com.akari.retailer.features.money.data.remote.FirestoreMoneyService
import com.akari.retailer.features.money.data.remote.FirestoreMoneyTransactionService
import com.akari.retailer.features.money.domain.models.FeeType
import com.akari.retailer.features.money.domain.usecases.ExternalTransferUseCase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExternalTransferScreen(
    onBack: () -> Unit,
    onTransferSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    val application = context.applicationContext as RetailApplication
    
    val accountService = remember { FirestoreMoneyService() }
    val accountRepository = remember { FirestoreMoneyAccountRepository(accountService) }
    val transactionService = remember { FirestoreMoneyTransactionService() }
    val transactionRepository = remember { FirestoreMoneyTransactionRepository(transactionService) }
    
    val transferUseCase = remember { 
        ExternalTransferUseCase(accountRepository, transactionRepository)
    }
    
    val viewModel: ExternalTransferViewModel = viewModel(
        factory = ExternalTransferViewModelFactory(accountRepository, transferUseCase)
    )
    
    val state by viewModel.state.collectAsState()
    
    var accountExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) {
            kotlinx.coroutines.delay(1000)
            onTransferSuccess()
            onBack()
        }
    }

    AppScreen(
        title = "🌐 " + stringResource(R.string.external_transfer),
        showBackButton = true,
        onBackClick = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Direction Selector (Outgoing / Incoming)
            Text(
                text = stringResource(R.string.direction),
                style = AppTypography.label,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = Spacing.small)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DirectionButton(
                    label = "↗️ " + stringResource(R.string.outgoing),
                    isSelected = state.direction == ExternalTransferDirection.OUTGOING,
                    onClick = { 
                        viewModel.handleEvent(
                            ExternalTransferEvent.DirectionChanged(ExternalTransferDirection.OUTGOING)
                        ) 
                    },
                    modifier = Modifier.weight(1f)
                )
                DirectionButton(
                    label = "↙️ " + stringResource(R.string.incoming),
                    isSelected = state.direction == ExternalTransferDirection.INCOMING,
                    onClick = { 
                        viewModel.handleEvent(
                            ExternalTransferEvent.DirectionChanged(ExternalTransferDirection.INCOMING)
                        ) 
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            // Account Selector
            Text(
                text = if (state.isOutgoing()) 
                    stringResource(R.string.from_account) 
                else 
                    stringResource(R.string.to_account),
                style = AppTypography.label,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = Spacing.small)
            )
            
            ExposedDropdownMenuBox(
                expanded = accountExpanded,
                onExpandedChange = { accountExpanded = it }
            ) {
                OutlinedTextField(
                    value = state.selectedAccount?.getDisplayName() ?: stringResource(R.string.select_account),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.money_account)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded) }
                )
                ExposedDropdownMenu(
                    expanded = accountExpanded,
                    onDismissRequest = { accountExpanded = false }
                ) {
                    state.accounts.forEach { account ->
                        DropdownMenuItem(
                            text = { 
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(account.getDisplayName())
                                    Text(
                                        text = "${account.currentBalance}",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 12.sp
                                    )
                                }
                            },
                            onClick = {
                                viewModel.handleEvent(ExternalTransferEvent.AccountSelected(account))
                                accountExpanded = false
                            }
                        )
                    }
                }
            }
            
            if (state.selectedAccount != null) {
                Text(
                    text = "${stringResource(R.string.balance)}: ${state.selectedAccount!!.currentBalance}",
                    style = AppTypography.small,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 4.dp, bottom = Spacing.medium)
                )
            } else {
                Spacer(modifier = Modifier.height(Spacing.medium))
            }
            
            // External Account Name
            OutlinedTextField(
                value = state.externalAccountName,
                onValueChange = { viewModel.handleEvent(ExternalTransferEvent.ExternalAccountNameChanged(it)) },
                label = { Text(stringResource(R.string.external_account_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            // External Account Number
            OutlinedTextField(
                value = state.externalAccountNumber,
                onValueChange = { viewModel.handleEvent(ExternalTransferEvent.ExternalAccountNumberChanged(it)) },
                label = { Text(stringResource(R.string.external_account_number)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            // Amount
            OutlinedTextField(
                value = state.amount,
                onValueChange = { viewModel.handleEvent(ExternalTransferEvent.AmountChanged(it)) },
                label = { Text(stringResource(R.string.amount)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            // Fee Type Selector
            Text(
                text = stringResource(R.string.fee_type),
                style = AppTypography.label,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = Spacing.small)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FeeTypeButton(
                    label = stringResource(R.string.fee_none),
                    isSelected = state.feeType == FeeType.NONE,
                    onClick = { viewModel.handleEvent(ExternalTransferEvent.FeeTypeChanged(FeeType.NONE)) },
                    modifier = Modifier.weight(1f)
                )
                FeeTypeButton(
                    label = stringResource(R.string.fee_paid),
                    isSelected = state.feeType == FeeType.FEE_PAID,
                    onClick = { viewModel.handleEvent(ExternalTransferEvent.FeeTypeChanged(FeeType.FEE_PAID)) },
                    modifier = Modifier.weight(1f)
                )
                FeeTypeButton(
                    label = stringResource(R.string.fee_earned),
                    isSelected = state.feeType == FeeType.FEE_EARNED,
                    onClick = { viewModel.handleEvent(ExternalTransferEvent.FeeTypeChanged(FeeType.FEE_EARNED)) },
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            // Fee Amount (only if fee type is not NONE)
            if (state.feeType != FeeType.NONE) {
                OutlinedTextField(
                    value = state.fee,
                    onValueChange = { viewModel.handleEvent(ExternalTransferEvent.FeeChanged(it)) },
                    label = { 
                        Text(
                            if (state.feeType == FeeType.FEE_PAID) 
                                stringResource(R.string.fee_you_pay)
                            else 
                                stringResource(R.string.fee_you_earn)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.height(Spacing.medium))
            }
            
            // Description
            OutlinedTextField(
                value = state.description,
                onValueChange = { viewModel.handleEvent(ExternalTransferEvent.DescriptionChanged(it)) },
                label = { Text(stringResource(R.string.description_optional)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            )
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            // Preview Card
            if (state.selectedAccount != null && state.amount.isNotEmpty()) {
                AppCard {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.transfer_summary),
                            style = AppTypography.title,
                            modifier = Modifier.padding(bottom = Spacing.small)
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = state.selectedAccount!!.name,
                                style = AppTypography.body
                            )
                            Text(
                                text = "${state.selectedAccount!!.currentBalance} → ${state.getNewBalance()}",
                                style = AppTypography.body,
                                color = if (state.getAccountChange() >= 0) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(Spacing.small))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (state.isOutgoing()) 
                                    stringResource(R.string.to_account) 
                                else 
                                    stringResource(R.string.from_account),
                                style = AppTypography.body
                            )
                            Text(
                                text = state.externalAccountName.ifEmpty { "External Account" },
                                style = AppTypography.body,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(Spacing.medium))
            }
            
            state.error?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        style = AppTypography.body,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(Spacing.medium)
                    )
                }
                Spacer(modifier = Modifier.height(Spacing.medium))
            }
            
            AppPrimaryButton(
                text = if (state.isSaving) stringResource(R.string.saving) else stringResource(R.string.confirm_transfer),
                onClick = { viewModel.handleEvent(ExternalTransferEvent.SaveTransfer) },
                isLoading = state.isSaving,
                enabled = state.selectedAccount != null && 
                         state.externalAccountName.isNotEmpty() && 
                         state.amount.isNotEmpty() && 
                         !state.isSaving
            )
            
            Spacer(modifier = Modifier.height(Spacing.xxlarge))
        }
    }
}

@Composable
fun DirectionButton(
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
        ),
        shape = MaterialTheme.shapes.small
    ) {
        Text(label)
    }
}
