package com.akari.retailer.features.money.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.akari.retailer.core.ui.components.TimeFilterPreset
import com.akari.retailer.core.ui.components.TimeFilterSelector
import com.akari.retailer.core.ui.theme.AppTypography
import com.akari.retailer.core.ui.theme.Spacing
import com.akari.retailer.core.utils.MoneyFormatter
import com.akari.retailer.features.money.domain.models.FeeType
import com.akari.retailer.features.money.domain.models.MoneyTransaction
import com.akari.retailer.features.money.domain.models.MoneyTransactionType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExternalTransferHistoryScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as RetailApplication

    val viewModel: ExternalTransferHistoryViewModel = viewModel(
        factory = ExternalTransferHistoryViewModelFactory(
            application.container.moneyTransactionRepository,
            application.container.moneyAccountRepository
        )
    )

    val state by viewModel.state.collectAsState()
    var showFilterDialog by remember { mutableStateOf(false) }

    // Combined filter dialog
    if (showFilterDialog) {
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.filters))
                    TextButton(onClick = {
                        viewModel.handleEvent(ExternalTransferHistoryEvent.ClearFilters)
                    }) {
                        Text(stringResource(R.string.clear_all))
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 500.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Time
                    Text(
                        text = stringResource(R.string.time_range),
                        style = AppTypography.label,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    TimeFilterSelector(
                        filter = state.timeFilter,
                        onFilterChange = {
                            viewModel.handleEvent(ExternalTransferHistoryEvent.TimeFilterChanged(it))
                        }
                    )

                    Spacer(modifier = Modifier.height(Spacing.medium))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(Spacing.medium))

                    // Our account filter
                    Text(
                        text = stringResource(R.string.money_account),
                        style = AppTypography.label,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        FilterChip(
                            selected = state.selectedAccountId.isEmpty(),
                            onClick = {
                                viewModel.handleEvent(ExternalTransferHistoryEvent.AccountFilterChanged(""))
                            },
                            label = { Text(stringResource(R.string.all_accounts)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        state.accounts.forEach { account ->
                            FilterChip(
                                selected = state.selectedAccountId == account.id,
                                onClick = {
                                    viewModel.handleEvent(
                                        ExternalTransferHistoryEvent.AccountFilterChanged(account.id)
                                    )
                                },
                                label = { Text(account.getDisplayName()) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.medium))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(Spacing.medium))

                    // External account filter
                    Text(
                        text = stringResource(R.string.external_account_name),
                        style = AppTypography.label,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        FilterChip(
                            selected = state.selectedExternalName.isEmpty(),
                            onClick = {
                                viewModel.handleEvent(ExternalTransferHistoryEvent.ExternalFilterChanged(""))
                            },
                            label = { Text(stringResource(R.string.all_types)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        state.externalAccountNames.forEach { name ->
                            FilterChip(
                                selected = state.selectedExternalName == name,
                                onClick = {
                                    viewModel.handleEvent(
                                        ExternalTransferHistoryEvent.ExternalFilterChanged(name)
                                    )
                                },
                                label = { Text(name) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFilterDialog = false }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }

    AppScreen(
        title = stringResource(R.string.external_transfer_history),
        showBackButton = true,
        onBackClick = onBack,
        showFilterButton = true,
        onFilterClick = { showFilterDialog = true }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {

            if (state.isLoading && state.transfers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            if (state.error != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("❌", fontSize = 40.sp)
                        Text(
                            text = state.error!!,
                            style = AppTypography.body,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = Spacing.medium)
                        )
                        TextButton(
                            onClick = { viewModel.handleEvent(ExternalTransferHistoryEvent.LoadTransfers) }
                        ) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
                return@Column
            }

            // Active filter indicator
            val hasActiveFilters = state.selectedAccountId.isNotEmpty() ||
                state.selectedExternalName.isNotEmpty() ||
                state.timeFilter.preset != TimeFilterPreset.THIS_WEEK

            if (hasActiveFilters) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = Spacing.small),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(Spacing.small),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = buildFilterLabel(state),
                            style = AppTypography.small,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(
                            onClick = {
                                viewModel.handleEvent(ExternalTransferHistoryEvent.ClearFilters)
                                viewModel.handleEvent(
                                    ExternalTransferHistoryEvent.TimeFilterChanged(
                                        com.akari.retailer.core.ui.components.TimeFilter()
                                    )
                                )
                            }
                        ) {
                            Text(stringResource(R.string.clear))
                        }
                    }
                }
            }

            if (state.filteredTransfers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🌐", fontSize = 48.sp)
                        Text(
                            text = stringResource(R.string.no_external_transfers),
                            style = AppTypography.header,
                            modifier = Modifier.padding(top = Spacing.medium)
                        )
                        Text(
                            text = stringResource(R.string.transactions_will_appear_here),
                            style = AppTypography.body,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                return@Column
            }

            // ── Compact summary ──
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.small),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.medium)
                ) {
                    // Row 1: total transfers + net
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${state.filteredTransfers.size} transfers",
                            style = AppTypography.body,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "Net " + (if (state.netFlow >= 0) "+" else "") +
                                MoneyFormatter.format(state.netFlow),
                            style = AppTypography.body,
                            fontWeight = FontWeight.Bold,
                            color = if (state.netFlow >= 0)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Row 2: sent (count + amount) | received (count + amount)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "▲ ",
                                style = AppTypography.small,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "${state.sentCount} sent  ",
                                style = AppTypography.small,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = MoneyFormatter.format(state.totalSent),
                                style = AppTypography.body,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "▼ ",
                                style = AppTypography.small,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${state.receivedCount} received  ",
                                style = AppTypography.small,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = MoneyFormatter.format(state.totalReceived),
                                style = AppTypography.body,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            // ── Transactions ──
            Text(
                text = stringResource(R.string.transactions),
                style = AppTypography.title,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = Spacing.small)
            )

            // Flat list, newest first
            state.filteredTransfers
                .sortedByDescending { it.date }
                .forEach { txn ->
                    TransferRow(txn = txn)
                    Spacer(modifier = Modifier.height(Spacing.small))
                }

            Spacer(modifier = Modifier.height(Spacing.xxlarge))
        }
    }
}

private fun buildFilterLabel(state: ExternalTransferHistoryState): String {
    val parts = mutableListOf<String>()
    if (state.selectedAccountId.isNotEmpty()) {
        val name = state.accounts.find { it.id == state.selectedAccountId }?.name ?: "Account"
        parts.add(name)
    }
    if (state.selectedExternalName.isNotEmpty()) {
        parts.add(state.selectedExternalName)
    }
    if (state.timeFilter.preset != TimeFilterPreset.THIS_WEEK) {
        parts.add(state.timeFilter.label.ifEmpty { "Range" })
    }
    return parts.joinToString(" • ").ifEmpty { "Filters active" }
}

@Composable
private fun StatCell(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = AppTypography.header, color = color, fontWeight = FontWeight.Bold)
        Text(
            text = label,
            style = AppTypography.small,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun TransferRow(txn: MoneyTransaction) {
    val dateFormat = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }
    val isOutgoing = txn.type == MoneyTransactionType.EXTERNAL_OUT
    val color = if (isOutgoing) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.medium, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Direction badge
            Surface(
                modifier = Modifier.size(32.dp),
                shape = MaterialTheme.shapes.small,
                color = color.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (isOutgoing) "↑" else "↓",
                        style = AppTypography.body,
                        color = color,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(Spacing.medium))

            // Middle: time · name, description below
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = dateFormat.format(Date(txn.date)),
                        style = AppTypography.small,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                    Text(
                        text = "  ·  ",
                        style = AppTypography.small,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        fontSize = 11.sp
                    )
                    Text(
                        text = txn.externalAccountName.ifEmpty { "—" },
                        style = AppTypography.body,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
                if (txn.description.isNotEmpty()) {
                    Text(
                        text = txn.description,
                        style = AppTypography.small,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right: amount + optional fee
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = (if (isOutgoing) "-" else "+") + MoneyFormatter.format(txn.amount),
                    style = AppTypography.body,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
                if (txn.fee > 0) {
                    Text(
                        text = "fee " + (if (txn.feeType == FeeType.FEE_PAID) "-" else "+") +
                            MoneyFormatter.format(txn.fee),
                        style = AppTypography.small,
                        color = if (txn.feeType == FeeType.FEE_PAID)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
