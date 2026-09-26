@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.akari.retailer.features.sales.presentation.history

import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.HorizontalDivider
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
import com.akari.retailer.core.ui.components.AppPrimaryButton
import com.akari.retailer.core.ui.components.AppScreen
import com.akari.retailer.core.ui.components.SaleCard
import com.akari.retailer.core.ui.components.TimeFilter
import com.akari.retailer.core.ui.components.TimeFilterPreset
import com.akari.retailer.core.ui.components.TimeFilterSelector
import com.akari.retailer.core.ui.theme.AppTypography
import com.akari.retailer.core.ui.theme.Spacing
import com.akari.retailer.core.utils.MoneyFormatter
import com.akari.retailer.navigation.Routes
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaleHistoryScreen(
    navController: androidx.navigation.NavController,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as RetailApplication
    val repository = remember { application.container.saleRepository }
    val accountRepository = remember { application.container.moneyAccountRepository }
    val accounts by accountRepository.getAccounts()
        .collectAsState(initial = emptyList())

    val viewModel: SaleHistoryViewModel = viewModel(
        factory = SaleHistoryViewModelFactory(repository, application.container.saleFinalizer)
    )

    val state by viewModel.state.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }
    var showTimeFilterDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog && pendingDeleteId != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                pendingDeleteId = null
            },
            title = {
                Text(
                    text = stringResource(R.string.delete_sale),
                    style = AppTypography.header
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.delete_confirmation),
                    style = AppTypography.body
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingDeleteId?.let { viewModel.handleEvent(SaleHistoryEvent.DeleteSale(it)) }
                        showDeleteDialog = false
                        pendingDeleteId = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    pendingDeleteId = null
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Time filter dialog
    if (showTimeFilterDialog) {
        AlertDialog(
            onDismissRequest = { showTimeFilterDialog = false },
            title = { Text(stringResource(R.string.date_range)) },
            text = {
                TimeFilterSelector(
                    filter = state.timeFilter,
                    onFilterChange = {
                        viewModel.handleEvent(SaleHistoryEvent.TimeFilterChanged(it))
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = { showTimeFilterDialog = false }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }

    val headerSubtitle = state.timeFilter.label
        .ifBlank { state.timeFilter.preset.name.replace('_', ' ').lowercase()
            .replaceFirstChar { it.titlecase() } }

    AppScreen(
        title = stringResource(R.string.history),
        subtitle = headerSubtitle,
        showBackButton = true,
        onBackClick = onBack,
        showSearchButton = false,
        showFilterButton = true,
        onFilterClick = { showTimeFilterDialog = true },
        showHistoryButton = false
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Filter indicator (only when non-default)
            if (state.timeFilter.preset != TimeFilterPreset.TODAY) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = Spacing.medium),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.medium),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📅 ${state.timeFilter.label.ifEmpty { "Filter active" }}",
                            style = AppTypography.body
                        )
                        TextButton(
                            onClick = {
                                viewModel.handleEvent(
                                    SaleHistoryEvent.TimeFilterChanged(
                                        TimeFilter(preset = TimeFilterPreset.TODAY)
                                    )
                                )
                            }
                        ) {
                            Text(stringResource(R.string.clear))
                        }
                    }
                }
            }

            if (state.isLoading && state.sales.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (state.error != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Spacing.large),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "❌",
                        fontSize = 48.sp
                    )
                    Text(
                        text = state.error!!,
                        style = AppTypography.body,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(vertical = Spacing.medium)
                    )
                    AppPrimaryButton(
                        text = "Retry",
                        onClick = {
                            viewModel.handleEvent(SaleHistoryEvent.LoadSales)
                        }
                    )
                }
            } else if (state.sales.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Spacing.large),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "📭",
                        fontSize = 48.sp
                    )
                    Text(
                        text = "No sales yet",
                        style = AppTypography.header,
                        modifier = Modifier.padding(vertical = Spacing.medium)
                    )
                    Text(
                        text = "Add your first sale from the home screen",
                        style = AppTypography.body,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            } else {
                // Summary card
                SaleSummaryCard(state = state, accounts = accounts)
                Spacer(modifier = Modifier.height(Spacing.medium))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                    contentPadding = PaddingValues(bottom = Spacing.xxlarge)
                ) {
                    items(
                        items = state.sales,
                        key = { it.id }
                    ) { sale ->
                        SaleCard(
                            sale = sale,
                            accounts = accounts,
                            onDelete = {
                                pendingDeleteId = sale.id
                                showDeleteDialog = true
                            },
                            onClick = {
                                navController.navigate(
                                    Routes.SALE_DETAIL.replace("{saleId}", sale.id)
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SaleSummaryCard(
    state: SaleHistoryState,
    accounts: List<com.akari.retailer.features.money.domain.models.MoneyAccount>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium)
        ) {
            // Line 1: totals with labels
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Total ",
                    style = AppTypography.body,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = MoneyFormatter.format(state.totalSales),
                    style = AppTypography.header,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(Spacing.small))
                Text(
                    text = "·",
                    style = AppTypography.body,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.width(Spacing.small))
                Text(
                    text = "${state.salesCount} transactions",
                    style = AppTypography.body,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            // Line 2: per-account breakdown
            if (state.paymentBreakdown.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.small))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )
                Spacer(modifier = Modifier.height(Spacing.small))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small)
                ) {
                    // Simple wrapping row using Column of Rows
                    androidx.compose.foundation.layout.FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        state.paymentBreakdown.forEach { (accountId, amount) ->
                            val account = accounts.find { it.id == accountId }
                            val name = account?.name ?: "Account"
                            val label = "${name} ${MoneyFormatter.format(amount)}"

                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = label,
                                    style = AppTypography.small,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(
                                        horizontal = 8.dp,
                                        vertical = 4.dp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Line 3: paid vs credit
            Spacer(modifier = Modifier.height(Spacing.small))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )
            Spacer(modifier = Modifier.height(Spacing.small))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.paid),
                        style = AppTypography.small,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = MoneyFormatter.format(state.totalPaid),
                        style = AppTypography.body,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.credit),
                        style = AppTypography.small,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = MoneyFormatter.format(state.totalCredit),
                        style = AppTypography.body,
                        fontWeight = FontWeight.Medium,
                        color = if (state.totalCredit > 0)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCell(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = AppTypography.header,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = AppTypography.small,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}
