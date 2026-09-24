package com.akari.retailer.features.sales.presentation.entry

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
        showTopBar = true,
        floatingActionButton = {
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
                    title = "💳 ${stringResource(R.string.payment)}"
                ) {
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SegmentedButton(
                            selected = !state.isCreditSale,
                            onClick = {
                                if (state.isCreditSale) {
                                    viewModel.handleEvent(SaleEntryEvent.ToggleCreditSale)
                                }
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            label = { Text(stringResource(R.string.normal_payment), fontSize = 13.sp) }
                        )
                        SegmentedButton(
                            selected = state.isCreditSale,
                            onClick = {
                                if (!state.isCreditSale) {
                                    viewModel.handleEvent(SaleEntryEvent.ToggleCreditSale)
                                }
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            label = { Text("💳 ${stringResource(R.string.credit)}", fontSize = 13.sp) }
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.medium))

                    if (state.isCreditSale) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(8.dp)
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
                        PaymentListComponent(
                            paymentRows = state.paymentRows,
                            accounts = state.accounts,
                            customers = state.customers,
                            totalAmount = viewModel.getTotal(),
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
