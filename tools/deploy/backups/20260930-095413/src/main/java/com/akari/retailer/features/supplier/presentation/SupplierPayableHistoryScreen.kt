package com.akari.retailer.features.supplier.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akari.retailer.R
import com.akari.retailer.RetailApplication
import com.akari.retailer.core.ui.components.AppCard
import com.akari.retailer.core.ui.components.AppScreen
import com.akari.retailer.core.ui.theme.AppTypography
import com.akari.retailer.core.ui.theme.Spacing
import com.akari.retailer.core.utils.MoneyFormatter
import com.akari.retailer.features.supplier.domain.models.SupplierTransaction
import com.akari.retailer.features.supplier.domain.models.SupplierTransactionType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierPayableHistoryScreen(
    supplierId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as RetailApplication

    val viewModel: SupplierPayableHistoryViewModel = viewModel(
        factory = SupplierPayableHistoryViewModelFactory(
            application.container.supplierRepository,
            application.container.getSupplierTransactionsUseCase,
            supplierId
        )
    )

    val state by viewModel.state.collectAsState()

    AppScreen(
        title = stringResource(R.string.payable_history),
        showBackButton = true,
        onBackClick = onBack
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            if (state.isLoading && state.allTransactions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            if (state.error != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("❌", fontSize = 40.sp)
                        Text(
                            text = state.error!!,
                            style = AppTypography.body,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = Spacing.medium)
                        )
                    }
                }
                return@Column
            }

            // ── Header: supplier name + current balance ──
            state.supplier?.let { s ->
                AppCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = s.name,
                            style = AppTypography.title
                        )
                        if (s.phone.isNotEmpty()) {
                            Text(
                                text = "📱 ${s.phone}",
                                style = AppTypography.small,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Spacer(modifier = Modifier.height(Spacing.small))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.payable_balance),
                                style = AppTypography.body
                            )
                            Text(
                                text = "${state.currentBalance}",
                                style = AppTypography.header,
                                color = if (state.currentBalance > 0)
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.medium))
            }

            // ── Summary: total credit received / total paid ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                AppCard(modifier = Modifier.weight(1f)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${state.totalCreditReceived}",
                            style = AppTypography.header,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = stringResource(R.string.total_bought_on_credit),
                            style = AppTypography.small,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                AppCard(modifier = Modifier.weight(1f)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${state.totalPaid}",
                            style = AppTypography.header,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.total_paid),
                            style = AppTypography.small,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            // ── Filter chips ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                FilterChip(
                    selected = state.filter == SupplierPayableFilter.ALL,
                    onClick = { viewModel.setFilter(SupplierPayableFilter.ALL) },
                    label = { Text(stringResource(R.string.all)) }
                )
                FilterChip(
                    selected = state.filter == SupplierPayableFilter.PURCHASES,
                    onClick = { viewModel.setFilter(SupplierPayableFilter.PURCHASES) },
                    label = { Text("🛒 ${stringResource(R.string.purchase_on_credit_label)}") }
                )
                FilterChip(
                    selected = state.filter == SupplierPayableFilter.PAYMENTS,
                    onClick = { viewModel.setFilter(SupplierPayableFilter.PAYMENTS) },
                    label = { Text("💰 ${stringResource(R.string.payment_to_supplier_label)}") }
                )
            }

            Spacer(modifier = Modifier.height(Spacing.small))

            Text(
                text = "${state.filteredTransactions.size} ${stringResource(R.string.transactions)}",
                style = AppTypography.small,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = Spacing.small)
            )

            if (state.filteredTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Spacing.large),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📭", fontSize = 48.sp)
                        Text(
                            text = stringResource(R.string.no_transactions_for_filter),
                            style = AppTypography.body,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                return@Column
            }

            // Running balance map: start at current, walk backward
            val runningBalances = remember(state.allTransactions, state.filteredTransactions) {
                val map = mutableMapOf<String, Int>()
                var running = state.currentBalance
                state.allTransactions.forEach { txn ->
                    map[txn.id] = running
                    running -= txn.amount
                }
                map
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(Spacing.small),
                contentPadding = PaddingValues(bottom = Spacing.xxlarge)
            ) {
                items(state.filteredTransactions, key = { it.id }) { txn ->
                    SupplierPayableTxnRow(
                        txn = txn,
                        runningBalance = runningBalances[txn.id]
                    )
                }
            }
        }
    }
}

@Composable
private fun SupplierPayableTxnRow(
    txn: SupplierTransaction,
    runningBalance: Int?
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    val isPurchase = txn.type == SupplierTransactionType.PURCHASE_ON_CREDIT

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isPurchase)
                            "🛒 ${stringResource(R.string.purchase_on_credit_label)}"
                        else
                            "💰 ${stringResource(R.string.payment_to_supplier_label)}",
                        style = AppTypography.body,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = dateFormat.format(Date(txn.date)),
                        style = AppTypography.small,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    if (txn.description.isNotEmpty()) {
                        Text(
                            text = "📝 ${txn.description}",
                            style = AppTypography.small,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }
                }
                Text(
                    text = (if (isPurchase) "+" else "") + "${txn.amount}",
                    style = AppTypography.title,
                    color = if (isPurchase)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            if (runningBalance != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Balance: $runningBalance",
                    style = AppTypography.small,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontSize = 10.sp
                )
            }
        }
    }
}
