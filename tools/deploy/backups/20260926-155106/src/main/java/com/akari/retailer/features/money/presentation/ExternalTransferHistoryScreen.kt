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

            // ── Summary card (kept as-is) ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(Spacing.medium)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatCell(
                            label = stringResource(R.string.total_sent),
                            value = MoneyFormatter.format(state.totalSent),
                            color = MaterialTheme.colorScheme.error
                        )
                        StatCell(
                            label = stringResource(R.string.total_received),
                            value = MoneyFormatter.format(state.totalReceived),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(Spacing.small))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(Spacing.small))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.net_external_flow),
                            style = AppTypography.body
                        )
                        Text(
                            text = (if (state.netFlow >= 0) "+" else "") + MoneyFormatter.format(state.netFlow),
                            style = AppTypography.header,
                            color = if (state.netFlow >= 0)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            // ── By external account ──
            if (state.byExternalAccount.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.by_external_account),
                    style = AppTypography.title,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = Spacing.small)
                )
                state.byExternalAccount.forEach { account ->
                    AppCard {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(account.name, style = AppTypography.body, fontWeight = FontWeight.Medium)
                                Text(
                                    text = "${account.count}",
                                    style = AppTypography.small,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
                            ) {
                                if (account.sent > 0) {
                                    Text(
                                        text = "▲ ${MoneyFormatter.format(account.sent)}",
                                        style = AppTypography.small,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                if (account.received > 0) {
                                    Text(
                                        text = "▼ ${MoneyFormatter.format(account.received)}",
                                        style = AppTypography.small,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(Spacing.small))
                }
                Spacer(modifier = Modifier.height(Spacing.small))
            }

            // ── Transactions ──
            Text(
                text = stringResource(R.string.transactions),
                style = AppTypography.title,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = Spacing.small)
            )

            // Group transfers by calendar day (newest first)
            val groupedByDay: List<Pair<String, List<MoneyTransaction>>> =
                remember(state.filteredTransfers) {
                    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    state.filteredTransfers
                        .groupBy { fmt.format(Date(it.date)) }
                        .toList()
                        .sortedByDescending { it.first }
                }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(Spacing.small),
                contentPadding = PaddingValues(bottom = Spacing.xxlarge)
            ) {
                groupedByDay.forEach { (dayKey, txns) ->
                    item(key = "hdr-$dayKey") {
                        DayHeader(dayKey = dayKey, txns = txns)
                    }
                    items(txns, key = { it.id }) { txn ->
                        TransferRow(txn = txn)
                    }
                }
            }
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
private fun DayHeader(
    dayKey: String,
    txns: List<MoneyTransaction>
) {
    val dayLabel = remember(dayKey) {
        try {
            val inFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outFmt = SimpleDateFormat("EEE · MMM dd", Locale.getDefault())
            outFmt.format(inFmt.parse(dayKey)!!)
        } catch (e: Exception) {
            dayKey
        }
    }

    val daySent = txns
        .filter { it.type == MoneyTransactionType.EXTERNAL_OUT }
        .sumOf { it.amount }
    val dayReceived = txns
        .filter { it.type == MoneyTransactionType.EXTERNAL_IN }
        .sumOf { it.amount }
    val dayNet = dayReceived - daySent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.medium, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = dayLabel,
                style = AppTypography.title,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "· ${txns.size}",
                style = AppTypography.small,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
        Text(
            text = (if (dayNet >= 0) "+" else "") + MoneyFormatter.format(dayNet),
            style = AppTypography.small,
            color = if (dayNet >= 0)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun TransferRow(txn: MoneyTransaction) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
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
                        text = timeFormat.format(Date(txn.date)),
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
