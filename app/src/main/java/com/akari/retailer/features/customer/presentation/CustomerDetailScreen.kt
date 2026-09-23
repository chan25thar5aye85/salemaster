package com.akari.retailer.features.customer.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import com.akari.retailer.features.customer.domain.models.CreditTransaction
import com.akari.retailer.features.customer.domain.models.Customer
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.text.input.KeyboardType
import com.akari.retailer.features.money.domain.models.MoneyAccount
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.akari.retailer.features.customer.domain.models.CreditTransactionType
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    navController: androidx.navigation.NavController,
    customerId: String,
    onBack: () -> Unit,
    onEdit: (Customer) -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as RetailApplication
    
    
    val viewModel: CustomerDetailViewModel = viewModel(
        factory = CustomerDetailViewModelFactory(
            application.container.customerRepository,
            application.container.moneyAccountRepository,
            application.container.recordCreditPaymentUseCase,
            application.container.getCreditTransactionsUseCase,
            application.container.paymentPreferences,
            customerId
        )
    )
    
    val state by viewModel.state.collectAsState()

    AppScreen(
        title = stringResource(R.string.customer_details),
        showBackButton = true,
        onBackClick = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Text(
                            text = stringResource(R.string.loading_customers),
                            style = AppTypography.body,
                            modifier = Modifier.padding(top = Spacing.medium)
                        )
                    }
                }
                return@Column
            }

            if (state.error != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "❌",
                            fontSize = 40.sp
                        )
                        Text(
                            text = state.error!!,
                            style = AppTypography.body,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = Spacing.medium)
                        )
                        AppPrimaryButton(
                            text = stringResource(R.string.retry),
                            onClick = {
                                viewModel.handleEvent(CustomerDetailEvent.LoadCustomer)
                            }
                        )
                    }
                }
                return@Column
            }

            state.customer?.let { customer ->
                AppCard {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = customer.name,
                            style = AppTypography.header
                        )
                        
                        Spacer(modifier = Modifier.height(Spacing.medium))
                        
                        if (customer.phone.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "📱 ${stringResource(R.string.phone)}",
                                    style = AppTypography.body,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = customer.phone,
                                    style = AppTypography.body
                                )
                            }
                            Spacer(modifier = Modifier.height(Spacing.small))
                        }
                        
                        if (customer.email.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "✉️ ${stringResource(R.string.email)}",
                                    style = AppTypography.body,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = customer.email,
                                    style = AppTypography.body
                                )
                            }
                            Spacer(modifier = Modifier.height(Spacing.small))
                        }
                        
                        if (customer.address.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "📍 ${stringResource(R.string.address)}",
                                    style = AppTypography.body,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = customer.address,
                                    style = AppTypography.body
                                )
                            }
                            Spacer(modifier = Modifier.height(Spacing.small))
                        }
                        
                        if (customer.notes.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "📝 ${stringResource(R.string.notes)}",
                                    style = AppTypography.body,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = customer.notes,
                                    style = AppTypography.body
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.medium))

                AppCard {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.stats),
                            style = AppTypography.title,
                            modifier = Modifier.padding(bottom = Spacing.medium)
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = stringResource(R.string.total_spent),
                                    style = AppTypography.small,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = "${customer.totalSpent}",
                                    style = AppTypography.header,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = stringResource(R.string.total_orders),
                                    style = AppTypography.small,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = "${customer.totalOrders}",
                                    style = AppTypography.header,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // ── Credit balance card ──
                if ((customer.creditBalance) > 0 || state.creditTransactions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(Spacing.medium))
                    AppCard {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "💳 ${stringResource(R.string.credit_balance_label)}",
                                    style = AppTypography.title
                                )
                                Text(
                                    text = "${customer.creditBalance}",
                                    style = AppTypography.header,
                                    color = if (customer.owesCredit())
                                        MaterialTheme.colorScheme.error
                                    else
                                        MaterialTheme.colorScheme.primary
                                )
                            }

                            if (customer.owesCredit()) {
                                Spacer(modifier = Modifier.height(Spacing.small))
                                Text(
                                    text = stringResource(R.string.customer_owes_amount, customer.creditBalance),
                                    style = AppTypography.small,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(Spacing.small))
                                Button(
                                    onClick = {
                                        viewModel.handleEvent(CustomerDetailEvent.OpenPaymentDialog)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(R.string.record_payment))
                                }
                            }
                        }
                    }
                }

                // ── Credit history (preview + full button) ──
                if (state.creditTransactions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(Spacing.medium))
                    AppCard {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = stringResource(R.string.credit_history),
                                style = AppTypography.title,
                                modifier = Modifier.padding(bottom = Spacing.small)
                            )
                            state.creditTransactions.take(3).forEach { txn ->
                                CreditHistoryRow(transaction = txn)
                            }
                            Spacer(modifier = Modifier.height(Spacing.small))
                            OutlinedButton(
                                onClick = {
                                    navController.navigate(
                                        com.akari.retailer.navigation.Routes.CREDIT_HISTORY
                                            .replace("{customerId}", customerId)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.view_full_history))
                            }
                        }
                    }
                }

                // ── Payment success toast ──
                if (state.paymentSuccess) {
                    Spacer(modifier = Modifier.height(Spacing.small))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            text = "✅ ${stringResource(R.string.payment_recorded)}",
                            style = AppTypography.body,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(Spacing.medium)
                        )
                    }
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(2000)
                        viewModel.handleEvent(CustomerDetailEvent.ClearPaymentSuccess)
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.medium))

                AppPrimaryButton(
                    text = stringResource(R.string.edit_customer),
                    onClick = {
                        onEdit(customer)
                    }
                )

                Spacer(modifier = Modifier.height(Spacing.xxlarge))
            }
        }
    }

    // ── Payment dialog ──
    if (state.showPaymentDialog && state.customer != null) {
        RecordCreditPaymentDialog(
            amount = state.paymentAmount,
            notes = state.paymentNotes,
            accounts = state.accounts,
            selectedAccount = state.selectedAccount,
            error = state.paymentError,
            isProcessing = state.isRecordingPayment,
            maxAmount = state.customer!!.creditBalance,
            onAmountChange = { viewModel.handleEvent(CustomerDetailEvent.PaymentAmountChanged(it)) },
            onNotesChange = { viewModel.handleEvent(CustomerDetailEvent.PaymentNotesChanged(it)) },
            onAccountSelected = { viewModel.handleEvent(CustomerDetailEvent.PaymentAccountSelected(it)) },
            onSubmit = { viewModel.handleEvent(CustomerDetailEvent.SubmitPayment) },
            onDismiss = { viewModel.handleEvent(CustomerDetailEvent.ClosePaymentDialog) }
        )
    }
}


@Composable
private fun CreditHistoryRow(transaction: CreditTransaction) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    val isCredit = transaction.type == CreditTransactionType.SALE_ON_CREDIT

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.small),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isCredit) "🛒 ${stringResource(R.string.credit_sale)}" 
                       else "💰 ${stringResource(R.string.credit_payment)}",
                style = AppTypography.body
            )
            Text(
                text = dateFormat.format(Date(transaction.date)),
                style = AppTypography.small,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            if (transaction.description.isNotEmpty()) {
                Text(
                    text = "📝 ${transaction.description}",
                    style = AppTypography.small,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }
        }
        Text(
            text = (if (isCredit) "+" else "") + "${transaction.amount}",
            style = AppTypography.body,
            color = if (isCredit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordCreditPaymentDialog(
    amount: String,
    notes: String,
    accounts: List<MoneyAccount>,
    selectedAccount: MoneyAccount?,
    error: String?,
    isProcessing: Boolean,
    maxAmount: Int,
    onAmountChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onAccountSelected: (MoneyAccount) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    var accountExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        title = { Text(stringResource(R.string.record_payment)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.amount_owed, maxAmount),
                    style = AppTypography.small,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = Spacing.small)
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = onAmountChange,
                    label = { Text(stringResource(R.string.amount)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isProcessing,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(Spacing.small))

                OutlinedTextField(
                    value = notes,
                    onValueChange = onNotesChange,
                    label = { Text(stringResource(R.string.notes_optional)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isProcessing,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(Spacing.small))

                ExposedDropdownMenuBox(
                    expanded = accountExpanded,
                    onExpandedChange = { if (!isProcessing) accountExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedAccount?.getDisplayName() ?: stringResource(R.string.select_account),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.money_account)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        enabled = !isProcessing,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = accountExpanded,
                        onDismissRequest = { accountExpanded = false }
                    ) {
                        accounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text(acc.getDisplayName()) },
                                onClick = {
                                    onAccountSelected(acc)
                                    accountExpanded = false
                                }
                            )
                        }
                    }
                }

                if (error != null) {
                    Spacer(modifier = Modifier.height(Spacing.small))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = AppTypography.small
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSubmit,
                enabled = !isProcessing
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(stringResource(R.string.confirm))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isProcessing
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )

}
