package com.akari.retailer.features.sales.presentation.entry

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.akari.retailer.R
import com.akari.retailer.RetailApplication
import com.akari.retailer.core.ui.components.*
import com.akari.retailer.core.ui.theme.AppTypography
import com.akari.retailer.core.ui.theme.Spacing
import com.akari.retailer.core.utils.NetworkUtils
import com.akari.retailer.navigation.Routes
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaleEntryScreen(
    navController: NavController? = null
) {
    val context = LocalContext.current
    val application = context.applicationContext as RetailApplication
    
    val repository = remember { application.container.saleRepository }
    val moneyAccountRepository = remember { application.container.moneyAccountRepository }
    val processMoneyTransactionUseCase = remember { application.container.processMoneyTransactionUseCase }
    
    val viewModel: SaleEntryViewModel = viewModel(
        factory = SaleEntryViewModelFactory(
            repository,
            moneyAccountRepository,
            processMoneyTransactionUseCase,
            application.container.customerRepository,
            application.container.extendCreditUseCase,
            application.container.paymentPreferences
        )
    )
    
    val state by viewModel.state.collectAsState()
    val focusManager = LocalFocusManager.current
    
    var isOnline by remember { mutableStateOf(NetworkUtils.isNetworkAvailable(context)) }

    LaunchedEffect(Unit) {
        while (true) {
            isOnline = NetworkUtils.isNetworkAvailable(context)
            delay(3000)
        }
    }

    val focusRequesters = remember {
        mutableStateMapOf<Long, FocusRequester>()
    }

    LaunchedEffect(state.rows) {
        val currentIds = state.rows.map { it.id }.toSet()
        focusRequesters.keys.filter { it !in currentIds }.toList().forEach { focusRequesters.remove(it) }
    }

    LaunchedEffect(state.rows) {
        val focusedRow = state.rows.firstOrNull { it.isFocused }
        if (focusedRow != null) {
            val requester = focusRequesters.getOrPut(focusedRow.id) { FocusRequester() }
            delay(100)
            requester.requestFocus()
        }
    }

    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) {
            delay(2000)
            viewModel.handleEvent(SaleEntryEvent.ResetSaveSuccess)
        }
    }

    if (state.showCreditCustomerPicker) {
        CreditCustomerPickerDialog(
            customers = state.customers,
            onCustomerSelected = { customer ->
                viewModel.handleEvent(SaleEntryEvent.CreditCustomerSelected(customer))
            },
            onDismiss = { viewModel.handleEvent(SaleEntryEvent.CloseCreditCustomerPicker) }
        )
    }

    AppScreen(
        title = stringResource(R.string.sale_entry),
        showBackButton = false,
        showTopBar = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
        ) {
            if (!isOnline) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.medium),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                    )
                ) {
                    Text(
                        text = "📡 No internet. Sales will sync when online.",
                        style = AppTypography.body,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(Spacing.medium)
                    )
                }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                // Item rows
                state.rows.forEachIndexed { index, row ->
                    key(row.id) {
                        val requester = focusRequesters.getOrPut(row.id) { FocusRequester() }
                        ItemRow(
                            index = index,
                            amount = row.amount,
                            onAmountChange = { viewModel.handleEvent(SaleEntryEvent.AmountChanged(row.id, it)) },
                            onFocus = { viewModel.handleEvent(SaleEntryEvent.RowFocused(row.id)) },
                            onNext = { viewModel.handleEvent(SaleEntryEvent.NextPressed(row.id)) },
                            onDelete = { viewModel.handleEvent(SaleEntryEvent.RowDeleted(row.id)) },
                            focusRequester = requester,
                            showDelete = state.rows.size > 1
                        )
                        Spacer(modifier = Modifier.height(Spacing.medium))
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 16.dp))

                SaleSummary(
                    total = viewModel.getFormattedTotal(),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                
                Spacer(modifier = Modifier.height(Spacing.medium))

                // ── Payment mode toggle ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small)
                ) {
                    CreditModeButton(
                        text = stringResource(R.string.normal_payment),
                        isSelected = !state.isCreditSale,
                        onClick = {
                            if (state.isCreditSale) {
                                viewModel.handleEvent(SaleEntryEvent.ToggleCreditSale)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    CreditModeButton(
                        text = "💳 ${stringResource(R.string.credit)}",
                        isSelected = state.isCreditSale,
                        onClick = {
                            if (!state.isCreditSale) {
                                viewModel.handleEvent(SaleEntryEvent.ToggleCreditSale)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.small))

                if (state.isCreditSale) {
                    // Credit sale UI
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.medium)
                        ) {
                            Text(
                                text = "💳 ${stringResource(R.string.sell_on_credit)}",
                                style = AppTypography.title
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = state.creditCustomer?.let {
                                    "${stringResource(R.string.customer)}: ${it.name}" +
                                    if (it.owesCredit()) " (owes ${it.creditBalance})" else ""
                                } ?: stringResource(R.string.no_customer_selected),
                                style = AppTypography.body
                            )
                            Spacer(modifier = Modifier.height(Spacing.small))
                            OutlinedButton(
                                onClick = { viewModel.handleEvent(SaleEntryEvent.OpenCreditCustomerPicker) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    if (state.creditCustomer == null)
                                        stringResource(R.string.select_customer)
                                    else
                                        stringResource(R.string.change_customer)
                                )
                            }

                            if (state.creditCustomer != null) {
                                Spacer(modifier = Modifier.height(Spacing.small))
                                OutlinedTextField(
                                    value = state.creditNotes,
                                    onValueChange = {
                                        viewModel.handleEvent(SaleEntryEvent.CreditNotesChanged(it))
                                    },
                                    label = { Text(stringResource(R.string.notes_optional)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 2
                                )
                            }
                        }
                    }
                } else {
                    // Normal payment component
                    PaymentListComponent(
                        paymentRows = state.paymentRows,
                        accounts = state.accounts,
                        totalAmount = viewModel.getTotal(),
                        onAccountSelected = { rowId, account ->
                            viewModel.handleEvent(SaleEntryEvent.PaymentAccountChanged(rowId, account))
                        },
                        onAmountChanged = { rowId, amount ->
                            viewModel.handleEvent(SaleEntryEvent.PaymentAmountChanged(rowId, amount))
                        },
                        onAddRow = { viewModel.handleEvent(SaleEntryEvent.AddPaymentRow) },
                        onRemoveRow = { rowId -> viewModel.handleEvent(SaleEntryEvent.RemovePaymentRow(rowId)) }
                    )
                }

                state.error?.let { error ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = AppTypography.body
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                AppPrimaryButton(
                    text = if (state.saveSuccess) stringResource(R.string.saved) else stringResource(R.string.save_sale),
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.handleEvent(SaleEntryEvent.SaveSale)
                    },
                    isLoading = state.isSaving,
                    enabled = !state.isSaving &&
                        if (state.isCreditSale) state.creditCustomer != null else true
                )
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            RecentSalesTable(
                sales = state.recentSales,
                onViewAllClick = { navController?.navigate(Routes.HISTORY) },
                maxItems = 2
            )

            Spacer(modifier = Modifier.height(Spacing.xxlarge))
        }
    }
}


@Composable
fun CreditModeButton(
    text: String,
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
        Text(text, fontSize = 13.sp)
    }
}
