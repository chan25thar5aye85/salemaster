package com.akari.retailer.features.debt.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.akari.retailer.R
import com.akari.retailer.RetailApplication
import com.akari.retailer.core.ui.components.AppCard
import com.akari.retailer.core.ui.components.AppScreen
import com.akari.retailer.core.ui.theme.AppTypography
import com.akari.retailer.core.ui.theme.Spacing
import com.akari.retailer.core.utils.MoneyFormatter
import com.akari.retailer.features.customer.domain.models.AgingBucket
import com.akari.retailer.features.customer.presentation.CustomerDetailScreen
import com.akari.retailer.features.sales.presentation.entry.CreditCustomerPickerDialog
import com.akari.retailer.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtOverviewScreen(
    navController: NavController,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as RetailApplication

    val viewModel: DebtOverviewViewModel = viewModel(
        factory = DebtOverviewViewModelFactory(
            application.container.customerRepository,
            application.container.creditRepository,
            application.container.supplierRepository,
            application.container.supplierCreditRepository,
            application.container.calculateAgingReportUseCase,
            application.container.extendCreditUseCase
        )
    )

    val state by viewModel.state.collectAsState()

    // Customer picker (for Add Debt on receivables)
    var showCustomerPicker by remember { mutableStateOf(false) }
    // Supplier picker (for Add Debt on payables)
    var showSupplierPicker by remember { mutableStateOf(false) }

    AppScreen(
        title = "💰 " + stringResource(R.string.debt_title),
        showBackButton = true,
        onBackClick = onBack,
        showAddButton = true,
        onAddClick = { viewModel.openAddDialog() }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Tabs ──
            TabRow(
                selectedTabIndex = state.tab.ordinal,
                containerColor = Color.Transparent,
                divider = {}
            ) {
                Tab(
                    selected = state.tab == DebtTab.RECEIVABLES,
                    onClick = { viewModel.setTab(DebtTab.RECEIVABLES) },
                    text = { Text(stringResource(R.string.debt_receivables), fontSize = 14.sp) }
                )
                Tab(
                    selected = state.tab == DebtTab.PAYABLES,
                    onClick = { viewModel.setTab(DebtTab.PAYABLES) },
                    text = { Text(stringResource(R.string.debt_payables), fontSize = 14.sp) }
                )
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            // ── Summary ──
            val totalLabel = if (state.tab == DebtTab.RECEIVABLES)
                stringResource(R.string.debt_they_owe_me)
            else
                stringResource(R.string.debt_i_owe_them)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (state.tab == DebtTab.RECEIVABLES)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(Modifier.fillMaxWidth().padding(Spacing.medium)) {
                    Text(totalLabel, style = AppTypography.body)
                    Text(
                        MoneyFormatter.formatTotal(state.activeTotal),
                        style = AppTypography.header.copy(fontSize = 28.sp),
                        fontWeight = FontWeight.Bold,
                        color = if (state.tab == DebtTab.RECEIVABLES)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                    )
                    val countLabel = if (state.tab == DebtTab.RECEIVABLES)
                        stringResource(R.string.debt_n_customers, state.receivablePartyCount)
                    else
                        stringResource(R.string.debt_n_suppliers, state.payablePartyCount)
                    Text(countLabel, style = AppTypography.small)
                }
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            // ── Aging bucket row ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                AgingBucket.values().forEach { bucket ->
                    val amount = state.activeBuckets[bucket] ?: 0
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(bucketShort(bucket), fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            Text(
                                MoneyFormatter.format(amount),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            // ── Party list ──
            if (state.activeParties.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.debt_no_outstanding),
                        style = AppTypography.body,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.small),
                    contentPadding = PaddingValues(bottom = Spacing.xxlarge)
                ) {
                    items(state.activeParties, key = { it.partyId }) { party ->
                        AppCard {
                            Row(
                                Modifier.fillMaxWidth().clickable {
                                    // Drill-in: receivables → customer detail; payables → supplier
                                    val route = if (state.tab == DebtTab.RECEIVABLES)
                                        Routes.CUSTOMER_DETAIL.replace("{customerId}", party.partyId)
                                    else
                                        Routes.SUPPLIER_DETAIL.replace("{supplierId}", party.partyId)
                                    navController.navigate(route)
                                },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(party.partyName, style = AppTypography.title)
                                    Text(
                                        stringResource(R.string.debt_oldest_days, party.oldestDays),
                                        style = AppTypography.small,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                                Text(
                                    MoneyFormatter.formatTotal(party.amount),
                                    style = AppTypography.title,
                                    fontWeight = FontWeight.Bold,
                                    color = if (state.tab == DebtTab.RECEIVABLES)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Add Debt dialog ──
    if (state.showAddDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.closeAddDialog() },
            title = {
                Text(
                    if (state.tab == DebtTab.RECEIVABLES)
                        stringResource(R.string.debt_add_customer_title)
                    else
                        stringResource(R.string.debt_add_supplier_title)
                )
            },
            text = {
                Column(Modifier.fillMaxWidth()) {
                    if (state.tab == DebtTab.RECEIVABLES) {
                        OutlinedButton(
                            onClick = { showCustomerPicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(state.selectedCustomerName.ifEmpty { stringResource(R.string.debt_select_customer) })
                        }
                    } else {
                        OutlinedButton(
                            onClick = { showSupplierPicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(state.selectedSupplierName.ifEmpty { stringResource(R.string.debt_select_supplier) })
                        }
                    }
                    Spacer(Modifier.height(Spacing.small))
                    OutlinedTextField(
                        value = state.addAmount,
                        onValueChange = { viewModel.updateAmount(it) },
                        label = { Text(stringResource(R.string.debt_amount)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        )
                    )
                    Spacer(Modifier.height(Spacing.small))
                    OutlinedTextField(
                        value = state.addNote,
                        onValueChange = { viewModel.updateNote(it) },
                        label = { Text(stringResource(R.string.debt_note_optional)) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                    state.addError?.let {
                        Spacer(Modifier.height(Spacing.small))
                        Text(it, color = MaterialTheme.colorScheme.error, style = AppTypography.small)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.save() },
                    enabled = !state.isSaving
                ) {
                    if (state.isSaving) Text(stringResource(R.string.debt_saving))
                    else Text(stringResource(R.string.debt_add))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeAddDialog() }) { Text(stringResource(R.string.debt_cancel)) }
            }
        )
    }

    // Customer picker
    if (showCustomerPicker) {
        val customers by application.container.customerRepository.getCustomers()
            .collectAsState(initial = emptyList())
        CreditCustomerPickerDialog(
            customers = customers,
            onCustomerSelected = {
                viewModel.selectCustomer(it.id, it.name)
                showCustomerPicker = false
            },
            onDismiss = { showCustomerPicker = false }
        )
    }
    // Supplier picker
    if (showSupplierPicker) {
        val suppliers by application.container.supplierRepository.getSuppliers()
            .collectAsState(initial = emptyList())
        // Reuse a simple list dialog
        AlertDialog(
            onDismissRequest = { showSupplierPicker = false },
            title = { Text(stringResource(R.string.debt_pick_supplier_title)) },
            text = {
                LazyColumn(Modifier.heightIn(max = 400.dp)) {
                    items(suppliers, key = { it.id }) { s ->
                        Surface(
                            Modifier.fillMaxWidth().clickable {
                                viewModel.selectSupplier(s.id, s.name)
                                showSupplierPicker = false
                            },
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                s.name,
                                modifier = Modifier.padding(12.dp),
                                style = AppTypography.body
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSupplierPicker = false }) { Text(stringResource(R.string.debt_cancel)) }
            }
        )
    }
}

private fun bucketShort(b: AgingBucket): String = when (b) {
    AgingBucket.CURRENT -> "0-30"
    AgingBucket.DAYS_30 -> "31-60"
    AgingBucket.DAYS_60 -> "61-90"
    AgingBucket.DAYS_90 -> "90+"
}
