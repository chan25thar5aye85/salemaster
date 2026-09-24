package com.akari.retailer.features.supplier.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.akari.retailer.features.supplier.domain.models.Supplier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import com.akari.retailer.features.money.domain.models.MoneyAccount
import com.akari.retailer.features.supplier.domain.models.SupplierTransaction
import com.akari.retailer.features.supplier.domain.models.SupplierTransactionType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierDetailScreen(
    navController: androidx.navigation.NavController,
    supplierId: String,
    onBack: () -> Unit,
    onEdit: (Supplier) -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as RetailApplication
    
    
    val viewModel: SupplierDetailViewModel = viewModel(
        factory = SupplierDetailViewModelFactory(
            application.container.supplierRepository,
            application.container.moneyAccountRepository,
            application.container.recordSupplierPaymentUseCase,
            application.container.getSupplierTransactionsUseCase,
            application.container.paymentPreferences,
            supplierId
        )
    )
    
    val state by viewModel.state.collectAsState()

    AppScreen(
        title = stringResource(R.string.supplier_details),
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
                            text = stringResource(R.string.loading_suppliers),
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
                                viewModel.handleEvent(SupplierDetailEvent.LoadSupplier)
                            }
                        )
                    }
                }
                return@Column
            }

            state.supplier?.let { supplier ->
                AppCard {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = supplier.name,
                            style = AppTypography.header
                        )
                        
                        Spacer(modifier = Modifier.height(Spacing.medium))
                        
                        if (supplier.company.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "🏢 ${stringResource(R.string.company)}",
                                    style = AppTypography.body,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = supplier.company,
                                    style = AppTypography.body
                                )
                            }
                            Spacer(modifier = Modifier.height(Spacing.small))
                        }
                        
                        if (supplier.phone.isNotEmpty()) {
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
                                    text = supplier.phone,
                                    style = AppTypography.body
                                )
                            }
                            Spacer(modifier = Modifier.height(Spacing.small))
                        }
                        
                        if (supplier.email.isNotEmpty()) {
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
                                    text = supplier.email,
                                    style = AppTypography.body
                                )
                            }
                            Spacer(modifier = Modifier.height(Spacing.small))
                        }
                        
                        if (supplier.address.isNotEmpty()) {
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
                                    text = supplier.address,
                                    style = AppTypography.body
                                )
                            }
                            Spacer(modifier = Modifier.height(Spacing.small))
                        }
                        
                        if (supplier.products.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "📦 ${stringResource(R.string.products)}",
                                    style = AppTypography.body,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = supplier.products.joinToString(", "),
                                    style = AppTypography.body
                                )
                            }
                            Spacer(modifier = Modifier.height(Spacing.small))
                        }
                        
                        if (supplier.notes.isNotEmpty()) {
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
                                    text = supplier.notes,
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
                                    text = stringResource(R.string.total_purchased),
                                    style = AppTypography.small,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = "${supplier.totalPurchased}",
                                    style = AppTypography.header,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = stringResource(R.string.products),
                                    style = AppTypography.small,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = "${supplier.products.size}",
                                    style = AppTypography.header,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.medium))

                // ── Payable balance card ──
                if (supplier.payableBalance > 0 || state.payableTransactions.isNotEmpty()) {
                    AppCard {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "💰 ${stringResource(R.string.payable_balance)}",
                                    style = AppTypography.title
                                )
                                Text(
                                    text = "${supplier.payableBalance}",
                                    style = AppTypography.header,
                                    color = if (supplier.payableBalance > 0)
                                        MaterialTheme.colorScheme.error
                                    else
                                        MaterialTheme.colorScheme.primary
                                )
                            }

                            if (supplier.payableBalance > 0) {
                                Spacer(modifier = Modifier.height(Spacing.small))
                                Text(
                                    text = stringResource(R.string.you_owe_supplier, supplier.payableBalance),
                                    style = AppTypography.small,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(Spacing.small))
                                Button(
                                    onClick = {
                                        viewModel.handleEvent(SupplierDetailEvent.OpenPaymentDialog)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(R.string.record_supplier_payment))
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(Spacing.medium))
                }

                // ── Payable history (preview) ──
                if (state.payableTransactions.isNotEmpty()) {
                    AppCard {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = stringResource(R.string.payable_history),
                                style = AppTypography.title,
                                modifier = Modifier.padding(bottom = Spacing.small)
                            )
                            state.payableTransactions.take(3).forEach { txn ->
                                SupplierPayableRow(transaction = txn)
                            }
                            Spacer(modifier = Modifier.height(Spacing.small))
                            OutlinedButton(
                                onClick = {
                                    navController.navigate(
                                        com.akari.retailer.navigation.Routes.SUPPLIER_PAYABLE_HISTORY
                                            .replace("{supplierId}", supplierId)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.view_full_history))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(Spacing.medium))
                }

                // ── Payment success toast ──
                if (state.paymentSuccess) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            text = "✅ ${stringResource(R.string.supplier_payment_recorded)}",
                            style = AppTypography.body,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(Spacing.medium)
                        )
                    }
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(2000)
                        viewModel.handleEvent(SupplierDetailEvent.ClearPaymentSuccess)
                    }
                    Spacer(modifier = Modifier.height(Spacing.medium))
                }

                AppPrimaryButton(
                    text = stringResource(R.string.edit_supplier),
                    onClick = {
                        onEdit(supplier)
                    }
                )

                // ── Payment dialog ──
                if (state.showPaymentDialog && state.supplier != null) {
                    RecordSupplierPaymentDialog(
                        amount = state.paymentAmount,
                        notes = state.paymentNotes,
                        accounts = state.accounts,
                        selectedAccount = state.selectedAccount,
                        error = state.paymentError,
                        isProcessing = state.isRecordingPayment,
                        maxAmount = state.supplier!!.payableBalance,
                        onAmountChange = {
                            viewModel.handleEvent(SupplierDetailEvent.PaymentAmountChanged(it))
                        },
                        onNotesChange = {
                            viewModel.handleEvent(SupplierDetailEvent.PaymentNotesChanged(it))
                        },
                        onAccountSelected = {
                            viewModel.handleEvent(SupplierDetailEvent.PaymentAccountSelected(it))
                        },
                        onSubmit = { viewModel.handleEvent(SupplierDetailEvent.SubmitPayment) },
                        onDismiss = { viewModel.handleEvent(SupplierDetailEvent.ClosePaymentDialog) }
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.xxlarge))
            }
        }
    }
}

@Composable
private fun SupplierPayableRow(transaction: SupplierTransaction) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    val isPurchase = transaction.type == SupplierTransactionType.PURCHASE_ON_CREDIT

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.small),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isPurchase)
                    "🛒 ${stringResource(R.string.purchase_on_credit_label)}"
                else
                    "💰 ${stringResource(R.string.payment_to_supplier_label)}",
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
            text = (if (isPurchase) "+" else "") + "${transaction.amount}",
            style = AppTypography.body,
            fontWeight = FontWeight.Bold,
            color = if (isPurchase)
                MaterialTheme.colorScheme.error
            else
                MaterialTheme.colorScheme.primary
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun RecordSupplierPaymentDialog(
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
        title = { Text(stringResource(R.string.record_supplier_payment)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.amount_owed_to_supplier, maxAmount),
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
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(acc.getDisplayName(), fontSize = 13.sp)
                                        Text(
                                            text = "${acc.currentBalance}",
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                },
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
