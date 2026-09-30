package com.akari.retailer.features.money.presentation

import androidx.compose.material3.HorizontalDivider
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
import com.akari.retailer.features.money.domain.models.FeeType
import com.akari.retailer.features.money.domain.usecases.TransferMoneyUseCase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferMoneyScreen(
    onBack: () -> Unit,
    onTransferSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    val application = context.applicationContext as RetailApplication
    
    
    val transferUseCase = remember { 
        application.container.transferMoneyUseCase
    }
    
    val viewModel: TransferMoneyViewModel = viewModel(
        factory = TransferMoneyViewModelFactory(application.container.moneyAccountRepository, application.container.transferMoneyUseCase)
    )
    
    val state by viewModel.state.collectAsState()
    
    var fromExpanded by remember { mutableStateOf(false) }
    var toExpanded by remember { mutableStateOf(false) }

    // ✅ Show success for 3s, then reset — NO navigation
    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) {
            kotlinx.coroutines.delay(3000)
            viewModel.handleEvent(TransferMoneyEvent.ResetSuccess)
        }
    }

    AppScreen(
        title = "💸 " + stringResource(R.string.transfer_money),
        showBackButton = true,
        onBackClick = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
        ) {
            // FROM Account
            Text(
                text = stringResource(R.string.from_account),
                style = AppTypography.label,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = Spacing.small)
            )
            
            ExposedDropdownMenuBox(
                expanded = fromExpanded,
                onExpandedChange = { fromExpanded = it }
            ) {
                OutlinedTextField(
                    value = state.fromAccount?.getDisplayName() ?: stringResource(R.string.select_account),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.from_account)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fromExpanded) }
                )
                ExposedDropdownMenu(
                    expanded = fromExpanded,
                    onDismissRequest = { fromExpanded = false }
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
                                viewModel.handleEvent(TransferMoneyEvent.FromAccountSelected(account))
                                fromExpanded = false
                            }
                        )
                    }
                }
            }
            
            if (state.fromAccount != null) {
                Text(
                    text = "${stringResource(R.string.balance)}: ${state.fromAccount!!.currentBalance}",
                    style = AppTypography.small,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 4.dp, bottom = Spacing.medium)
                )
            } else {
                Spacer(modifier = Modifier.height(Spacing.medium))
            }
            
            // TO Account
            Text(
                text = stringResource(R.string.to_account),
                style = AppTypography.label,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = Spacing.small)
            )
            
            ExposedDropdownMenuBox(
                expanded = toExpanded,
                onExpandedChange = { toExpanded = it }
            ) {
                OutlinedTextField(
                    value = state.toAccount?.getDisplayName() ?: stringResource(R.string.select_account),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.to_account)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = toExpanded) }
                )
                ExposedDropdownMenu(
                    expanded = toExpanded,
                    onDismissRequest = { toExpanded = false }
                ) {
                    state.accounts
                        .filter { it.id != state.fromAccount?.id }
                        .forEach { account ->
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
                                    viewModel.handleEvent(TransferMoneyEvent.ToAccountSelected(account))
                                    toExpanded = false
                                }
                            )
                        }
                }
            }
            
            if (state.toAccount != null) {
                Text(
                    text = "${stringResource(R.string.balance)}: ${state.toAccount!!.currentBalance}",
                    style = AppTypography.small,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 4.dp, bottom = Spacing.medium)
                )
            } else {
                Spacer(modifier = Modifier.height(Spacing.medium))
            }
            
            // Amount
            OutlinedTextField(
                value = state.amount,
                onValueChange = { viewModel.handleEvent(TransferMoneyEvent.AmountChanged(it)) },
                label = { Text(stringResource(R.string.amount)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            // Fee Type
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
                    onClick = { viewModel.handleEvent(TransferMoneyEvent.FeeTypeChanged(FeeType.NONE)) },
                    modifier = Modifier.weight(1f)
                )
                FeeTypeButton(
                    label = stringResource(R.string.fee_paid),
                    isSelected = state.feeType == FeeType.FEE_PAID,
                    onClick = { viewModel.handleEvent(TransferMoneyEvent.FeeTypeChanged(FeeType.FEE_PAID)) },
                    modifier = Modifier.weight(1f)
                )
                FeeTypeButton(
                    label = stringResource(R.string.fee_earned),
                    isSelected = state.feeType == FeeType.FEE_EARNED,
                    onClick = { viewModel.handleEvent(TransferMoneyEvent.FeeTypeChanged(FeeType.FEE_EARNED)) },
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            // Fee Amount
            if (state.feeType != FeeType.NONE) {
                OutlinedTextField(
                    value = state.fee,
                    onValueChange = { viewModel.handleEvent(TransferMoneyEvent.FeeChanged(it)) },
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
                onValueChange = { viewModel.handleEvent(TransferMoneyEvent.DescriptionChanged(it)) },
                label = { Text(stringResource(R.string.description_optional)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            )
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            // Preview Card
            if (state.fromAccount != null && state.toAccount != null && state.amount.isNotEmpty()) {
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
                            Text(stringResource(R.string.from_account), style = AppTypography.body)
                            Text(
                                text = "${state.fromAccount!!.currentBalance} → ${state.getFromNewBalance()}",
                                style = AppTypography.body,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(stringResource(R.string.to_account), style = AppTypography.body)
                            Text(
                                text = "${state.toAccount!!.currentBalance} → ${state.getToNewBalance()}",
                                style = AppTypography.body,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        if (state.feeType != FeeType.NONE && (state.fee.toIntOrNull() ?: 0) > 0) {
                            Spacer(modifier = Modifier.height(Spacing.small))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(Spacing.small))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (state.feeType == FeeType.FEE_PAID) 
                                        stringResource(R.string.fee_you_pay)
                                    else 
                                        stringResource(R.string.fee_you_earn),
                                    style = AppTypography.body
                                )
                                Text(
                                    text = "${state.fee}",
                                    style = AppTypography.body,
                                    color = if (state.feeType == FeeType.FEE_PAID) 
                                        MaterialTheme.colorScheme.error
                                    else 
                                        MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(Spacing.medium))
            }
            
            // ✅ Success message
            if (state.saveSuccess) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.medium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "✅ Transfer successful!",
                            color = MaterialTheme.colorScheme.primary,
                            style = AppTypography.body,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.medium))
            }
            
            // Error
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
                onClick = { viewModel.handleEvent(TransferMoneyEvent.SaveTransfer) },
                isLoading = state.isSaving,
                enabled = state.fromAccount != null && 
                         state.toAccount != null && 
                         state.amount.isNotEmpty() && 
                         !state.isSaving
            )
            
            Spacer(modifier = Modifier.height(Spacing.xxlarge))
        }
    }
}

@Composable
fun FeeTypeButton(
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
        shape = MaterialTheme.shapes.small,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Text(label, fontSize = 12.sp)
    }
}
