package com.akari.retailer.features.sales.presentation.entry

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
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
import androidx.compose.ui.text.style.TextAlign
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
import com.akari.retailer.features.money.domain.models.CreditAccount
import com.akari.retailer.navigation.Routes
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.layout.imePadding

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

    // ── Payment sheet ──
    if (state.showPaymentDialog) {
        PaymentSheet(
            state = state,
            viewModel = viewModel,
            onDismiss = {
                viewModel.handleEvent(SaleEntryEvent.ClosePaymentDialog)
            }
        )
    }

    // ── Notes sheet ──
    if (state.showNotesDialog) {
        NotesSheet(
            initialText = state.notes,
            onApply = { text ->
                viewModel.handleEvent(SaleEntryEvent.NotesChanged(text))
                viewModel.handleEvent(SaleEntryEvent.CloseNotesDialog)
            },
            onDismiss = {
                viewModel.handleEvent(SaleEntryEvent.CloseNotesDialog)
            }
        )
    }

    // ── Overpayment attribution sheet ──
    if (state.showOverpaymentDialog) {
        var showOverpaymentCustomerPicker by remember { mutableStateOf(false) }

        val overpaymentSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = {
                viewModel.handleEvent(SaleEntryEvent.DismissOverpaymentDialog)
            },
            sheetState = overpaymentSheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.large)
                    .padding(bottom = Spacing.large)
            ) {
                Text(
                    text = stringResource(R.string.overpayment),
                    style = AppTypography.title,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = Spacing.small)
                )
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

                Spacer(modifier = Modifier.height(Spacing.medium))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.handleEvent(SaleEntryEvent.DismissOverpaymentDialog) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                    Button(
                        onClick = { viewModel.handleEvent(SaleEntryEvent.ConfirmOverpayment) },
                        enabled = state.overpaymentMode != OverpaymentMode.NONE &&
                            (state.overpaymentMode != OverpaymentMode.CREDIT_TO_CUSTOMER ||
                             state.overpaymentCustomer != null),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.confirm))
                    }
                }
            }
        }

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

    Box(modifier = Modifier.fillMaxSize()) {
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

            // ── Items ──
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
                            showDelete = state.rows.size > 1,
                            verticalPadding = 14.dp,
                            fieldMinHeight = 64.dp
                        )
                        if (index < state.rows.size - 1) {
                            Spacer(modifier = Modifier.height(Spacing.small))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

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

            // Extra bottom space so the floating pill doesn't cover the
            // last item when scrolled to the bottom.
            Spacer(modifier = Modifier.height(72.dp))
        }
    }

        // ── Floating action pill, sits above the keyboard ──
        SaleEntryPill(
            state = state,
            viewModel = viewModel,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 80.dp, end = 12.dp, bottom = 12.dp)
                .imePadding()
        )
    }
}

@Composable
private fun SaleEntryPill(
    state: SaleEntryState,
    viewModel: SaleEntryViewModel,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 6.dp,
        shape = RoundedCornerShape(28.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // ── Payment chip ──
            val primaryPayment = state.paymentRows.firstOrNull()
            val primaryAccount = state.accounts.find {
                it.id == primaryPayment?.accountId
            }
            val extraCount = (state.paymentRows.size - 1).coerceAtLeast(0)
            val isCredit = primaryPayment?.accountId == CreditAccount.ID
            val paymentLabel = when {
                isCredit -> "💳 Credit"
                primaryAccount != null -> "${primaryAccount.icon} ${primaryAccount.name}"
                else -> "💰 Payment"
            } + if (extraCount > 0) " +$extraCount" else ""

            OutlinedButton(
                onClick = {
                    viewModel.handleEvent(SaleEntryEvent.OpenPaymentDialog)
                },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = paymentLabel,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
            }

            // ── Notes icon ──
            Box {
                IconButton(
                    onClick = {
                        viewModel.handleEvent(SaleEntryEvent.OpenNotesDialog)
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Box {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Notes",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        if (state.notes.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(10.dp)
                                    .background(
                                        color = androidx.compose.ui.graphics.Color(0xFFFFC107),
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    )
                                    .border(
                                        width = 1.5.dp,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    )
                            )
                        }
                    }
                }
            }

            // ── Total ──
            Text(
                text = viewModel.getFormattedTotal(),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End,
                style = AppTypography.title.copy(fontSize = 17.sp),
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            // ── Save ──
            Button(
                onClick = {
                    if (!state.isSaving) {
                        viewModel.handleEvent(SaleEntryEvent.SaveSale)
                    }
                },
                enabled = (state.canSave || state.isSaving) && !state.isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onPrimary,
                    contentColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.35f),
                    disabledContentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                } else {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = if (state.saveSuccess) "Saved" else "Save",
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
            }
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
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
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
                    modifier = Modifier.weight(1f),
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
                TextButton(onClick = onViewAllClick) {
                    Text(stringResource(R.string.view_all), fontSize = 13.sp)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

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
                val visible = sales.take(5)
                visible.forEachIndexed { index, sale ->
                    CompactSaleRow(sale = sale)
                    if (index < visible.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = Spacing.medium),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                        )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentSheet(
    state: SaleEntryState,
    viewModel: SaleEntryViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.large)
                .padding(bottom = Spacing.large)
        ) {
            Text(
                text = "💳 ${stringResource(R.string.payment)}",
                style = AppTypography.title,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = Spacing.medium)
            )

            PaymentListComponent(
                paymentRows = state.paymentRows,
                accounts = state.accounts,
                customers = state.customers,
                totalAmount = viewModel.getTotal(),
                showAddButton = true,
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

            Spacer(modifier = Modifier.height(Spacing.medium))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.ok))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotesSheet(
    initialText: String,
    onApply: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.large)
                .padding(bottom = Spacing.large)
        ) {
            Text(
                text = "📝 " + stringResource(R.string.notes_optional),
                style = AppTypography.title,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = Spacing.medium)
            )

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = {
                    Text(
                        stringResource(R.string.sale_notes_hint),
                        style = AppTypography.small,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 260.dp),
                maxLines = 8
            )

            Spacer(modifier = Modifier.height(Spacing.medium))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.cancel))
                }
                Button(
                    onClick = { onApply(text) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.ok))
                }
            }
        }
    }
}
