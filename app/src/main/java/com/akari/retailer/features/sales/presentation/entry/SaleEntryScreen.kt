package com.akari.retailer.features.sales.presentation.entry

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.ExtendedFloatingActionButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaleEntryScreen(
    navController: NavController? = null
) {
    val context = LocalContext.current
    val application = context.applicationContext as RetailApplication

    val repository = remember { application.container.saleRepository }
    val moneyAccountRepository = remember { application.container.moneyAccountRepository }

    val viewModel: SaleEntryViewModel = viewModel(
        factory = SaleEntryViewModelFactory(
            repository,
            application.container.saleFinalizer,
            moneyAccountRepository,
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

    val focusRequesters = remember { mutableStateMapOf<Long, FocusRequester>() }

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

    // ── Overpayment attribution dialog ──
    if (state.showOverpaymentDialog) {
        var showOverpaymentCustomerPicker by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = {
                viewModel.handleEvent(SaleEntryEvent.DismissOverpaymentDialog)
            },
            title = { Text(stringResource(R.string.overpayment)) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.overpaid_amount, state.pendingOverpaymentAmount),
                        style = AppTypography.body
                    )
                    Spacer(modifier = Modifier.height(Spacing.medium))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.handleEvent(
                                    SaleEntryEvent.OverpaymentModeChanged(OverpaymentMode.CREDIT_TO_CUSTOMER)
                                )
                            }
                    ) {
                        RadioButton(
                            selected = state.overpaymentMode == OverpaymentMode.CREDIT_TO_CUSTOMER,
                            onClick = {
                                viewModel.handleEvent(
                                    SaleEntryEvent.OverpaymentModeChanged(OverpaymentMode.CREDIT_TO_CUSTOMER)
                                )
                            }
                        )
                        Text(stringResource(R.string.credit_to_customer))
                    }

                    if (state.overpaymentMode == OverpaymentMode.CREDIT_TO_CUSTOMER) {
                        OutlinedButton(
                            onClick = { showOverpaymentCustomerPicker = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 32.dp, top = 4.dp, bottom = 4.dp)
                        ) {
                            Text(
                                text = state.overpaymentCustomer?.name
                                    ?: stringResource(R.string.select_customer)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.small))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.handleEvent(
                                    SaleEntryEvent.OverpaymentModeChanged(OverpaymentMode.KEEP_IN_ACCOUNT)
                                )
                            }
                    ) {
                        RadioButton(
                            selected = state.overpaymentMode == OverpaymentMode.KEEP_IN_ACCOUNT,
                            onClick = {
                                viewModel.handleEvent(
                                    SaleEntryEvent.OverpaymentModeChanged(OverpaymentMode.KEEP_IN_ACCOUNT)
                                )
                            }
                        )
                        Text(stringResource(R.string.keep_in_account))
                    }

                    state.error?.let { err ->
                        Spacer(modifier = Modifier.height(Spacing.small))
                        Text(
                            text = err,
                            color = MaterialTheme.colorScheme.error,
                            style = AppTypography.small
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.handleEvent(SaleEntryEvent.ConfirmOverpayment) },
                    enabled = state.overpaymentMode != OverpaymentMode.NONE &&
                        (state.overpaymentMode != OverpaymentMode.CREDIT_TO_CUSTOMER ||
                         state.overpaymentCustomer != null)
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.handleEvent(SaleEntryEvent.DismissOverpaymentDialog) }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )

        if (showOverpaymentCustomerPicker) {
            CreditCustomerPickerDialog(
                customers = state.customers,
                onCustomerSelected = { customer ->
                    viewModel.handleEvent(SaleEntryEvent.OverpaymentCustomerSelected(customer))
                    showOverpaymentCustomerPicker = false
                },
                onDismiss = { showOverpaymentCustomerPicker = false }
            )
        }
    }


    AppScreen(
        title = stringResource(R.string.sale_entry),
        showBackButton = false,
        showTopBar = true,
        floatingActionButton = {
            AnimatedVisibility(
                visible = state.canSave || state.isSaving,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
            ) {
                ExtendedFloatingActionButton(
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.handleEvent(SaleEntryEvent.SaveSale)
                    },
                    icon = {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null
                            )
                        }
                    },
                    text = {
                        Text(
                            text = if (state.saveSuccess)
                                stringResource(R.string.saved)
                            else
                                stringResource(R.string.save_sale),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    expanded = true,
                    modifier = Modifier
                        .imePadding()
                        .navigationBarsPadding()
                )
        
            }
        }
    ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                                ) {

                if (!isOnline) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = Spacing.medium),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.medium),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(Spacing.small))
                            Text(
                                text = stringResource(R.string.offline_banner_sale),
                                style = AppTypography.small,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                SectionCard(
                    title = "🛒 ${stringResource(R.string.items)}",
                    trailing = {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "${state.rows.size}",
                                style = AppTypography.small,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                ) {
                    state.rows.forEachIndexed { index, row ->
                        key(row.id) {
                            val requester = focusRequesters.getOrPut(row.id) { FocusRequester() }
                            ItemRow(
                                index = index,
                                amount = row.amount,
                                onAmountChange = {
                                    viewModel.handleEvent(SaleEntryEvent.AmountChanged(row.id, it))
                                },
                                onFocus = { viewModel.handleEvent(SaleEntryEvent.RowFocused(row.id)) },
                                onNext = { viewModel.handleEvent(SaleEntryEvent.NextPressed(row.id)) },
                                onDelete = { viewModel.handleEvent(SaleEntryEvent.RowDeleted(row.id)) },
                                focusRequester = requester,
                                showDelete = state.rows.size > 1
                            )
                            if (index < state.rows.size - 1) {
                                Spacer(modifier = Modifier.height(Spacing.small))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.medium))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.large, vertical = Spacing.medium),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "💰 ${stringResource(R.string.total)}",
                            style = AppTypography.title,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                        )
                        Text(
                            text = viewModel.getFormattedTotal(),
                            style = AppTypography.total.copy(fontSize = 28.sp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.medium))

                SectionCard(
                    title = "💳 ${stringResource(R.string.payment)}",
                    trailing = {
                        TextButton(
                            onClick = { viewModel.handleEvent(SaleEntryEvent.AddPaymentRow) }
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add", fontSize = 13.sp)
                        }
                    }
                ) {
                    PaymentListComponent(
                            paymentRows = state.paymentRows,
                            accounts = state.accounts,
                            customers = state.customers,
                            totalAmount = viewModel.getTotal(),
                            showAddButton = false,
                            onAccountSelected = { rowId, account ->
                                viewModel.handleEvent(SaleEntryEvent.PaymentAccountChanged(rowId, account))
                            },
                            onCreditSelected = { rowId ->
                                viewModel.handleEvent(SaleEntryEvent.PaymentCreditSelected(rowId))
                            },
                            onCustomerSelected = { rowId, customer ->
                                viewModel.handleEvent(SaleEntryEvent.PaymentCustomerSelected(rowId, customer))
                            },
                            onAmountChanged = { rowId, amount ->
                                viewModel.handleEvent(SaleEntryEvent.PaymentAmountChanged(rowId, amount))
                            },
                            onAddRow = { viewModel.handleEvent(SaleEntryEvent.AddPaymentRow) },
                            onRemoveRow = { rowId ->
                                viewModel.handleEvent(SaleEntryEvent.RemovePaymentRow(rowId))
                            }
                        )

                    // Overpayment indicator (only when overpaid)
                        val totalPaid = state.paymentRows.sumOf { it.amount.toIntOrNull() ?: 0 }
                        val total = viewModel.getTotal()
                        val excess = totalPaid - total
                        if (excess > 0) {
                            Spacer(modifier = Modifier.height(Spacing.small))
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
                                        text = "💸 ${stringResource(R.string.overpayment)}",
                                        style = AppTypography.title,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(R.string.overpaid_amount, excess),
                                        style = AppTypography.body
                                    )
                                    Text(
                                        text = stringResource(R.string.select_customer_via_credit_toggle),
                                        style = AppTypography.small,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                }

                state.error?.let { error ->
                    Spacer(modifier = Modifier.height(Spacing.medium))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.medium),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(Spacing.small))
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error,
                                style = AppTypography.body
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.medium))

                RecentSalesCard(
                    sales = state.recentSales,
                    onViewAllClick = { navController?.navigate(Routes.HISTORY) }
                )

                Spacer(modifier = Modifier.height(Spacing.xxlarge))
            }
        }
    }

@Composable
private fun SectionCard(
    title: String,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = AppTypography.title,
                    fontWeight = FontWeight.Bold
                )
                trailing?.invoke()
            }
            Spacer(modifier = Modifier.height(Spacing.small))
            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(Spacing.small))
            content()
        }
    }
}

@Composable
private fun RecentSalesCard(
    sales: List<com.akari.retailer.features.sales.domain.models.Sale>,
    onViewAllClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.medium),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = Spacing.small),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📋 ${stringResource(R.string.recent_sales)}",
                        style = AppTypography.title,
                        fontWeight = FontWeight.Bold
                    )
                    if (sales.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(Spacing.small))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "${sales.size}",
                                style = AppTypography.small,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row {
                    TextButton(onClick = onViewAllClick) {
                        Text(stringResource(R.string.view_all), fontSize = 13.sp)
                    }
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Collapse" else "Expand"
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    if (sales.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = Spacing.large),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.no_sales_today),
                                style = AppTypography.body,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                    } else {
                        sales.take(5).forEachIndexed { index, sale ->
                            CompactSaleRow(sale = sale)
                            if (index < sales.take(5).size - 1) {
                                Divider(
                                    modifier = Modifier.padding(horizontal = Spacing.medium),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactSaleRow(sale: com.akari.retailer.features.sales.domain.models.Sale) {
    val dateFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.medium, vertical = Spacing.small),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "🕐 ${dateFormat.format(Date(sale.timestamp))}",
            style = AppTypography.small,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = "${sale.items.size} item${if (sale.items.size != 1) "s" else ""}",
            style = AppTypography.small,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = "${sale.total}",
            style = AppTypography.body,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )
    }
}
