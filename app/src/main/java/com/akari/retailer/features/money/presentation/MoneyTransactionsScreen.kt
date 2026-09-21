package com.akari.retailer.features.money.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
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
import com.akari.retailer.core.ui.components.SearchBox
import com.akari.retailer.core.ui.theme.AppTypography
import com.akari.retailer.core.ui.theme.Spacing
import com.akari.retailer.features.money.domain.models.FeeType
import com.akari.retailer.features.money.domain.models.MoneyTransaction
import com.akari.retailer.features.money.domain.models.MoneyTransactionType
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyTransactionsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as RetailApplication
    
    val transactionRepository = remember { application.container.moneyTransactionRepository }
    val accountRepository = remember { application.container.moneyAccountRepository }
    
    val viewModel: MoneyTransactionsViewModel = viewModel(
        factory = MoneyTransactionsViewModelFactory(transactionRepository, accountRepository)
    )
    
    val state by viewModel.state.collectAsState()
    var showSearch by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false }
        ) {
            FilterSheet(
                state = state,
                onAccountSelect = { 
                    viewModel.handleEvent(MoneyTransactionsEvent.AccountFilterChanged(it))
                },
                onTypeSelect = { 
                    viewModel.handleEvent(MoneyTransactionsEvent.TypeFilterChanged(it))
                },
                onTimeRangeSelect = { 
                    viewModel.handleEvent(MoneyTransactionsEvent.TimeRangeChanged(it))
                },
                onClearFilters = { 
                    viewModel.handleEvent(MoneyTransactionsEvent.ClearFilters)
                }
            )
        }
    }

    AppScreen(
        title = "📋 " + stringResource(R.string.transactions),
        showBackButton = true,
        onBackClick = onBack,
        showSearchButton = true,
        onSearchClick = { showSearch = !showSearch },
        showFilterButton = true,
        onFilterClick = { showFilterSheet = true }
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Summary Cards
            if (state.filteredTransactions.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small)
                ) {
                    AppCard(modifier = Modifier.weight(1f)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "↑",
                                style = AppTypography.label,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${state.totalIn}",
                                style = AppTypography.body,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = stringResource(R.string.money_in),
                                style = AppTypography.small,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                fontSize = 10.sp
                            )
                        }
                    }
                    AppCard(modifier = Modifier.weight(1f)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "↓",
                                style = AppTypography.label,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "${state.totalOut}",
                                style = AppTypography.body,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = stringResource(R.string.money_out),
                                style = AppTypography.small,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                fontSize = 10.sp
                            )
                        }
                    }
                    AppCard(modifier = Modifier.weight(1f)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "💸",
                                style = AppTypography.label
                            )
                            Text(
                                text = "${state.totalFeePaid}",
                                style = AppTypography.body,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = stringResource(R.string.fees_paid),
                                style = AppTypography.small,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                fontSize = 10.sp
                            )
                        }
                    }
                    AppCard(modifier = Modifier.weight(1f)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "💵",
                                style = AppTypography.label
                            )
                            Text(
                                text = "${state.totalFeeEarned}",
                                style = AppTypography.body,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = stringResource(R.string.fees_earned),
                                style = AppTypography.small,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.medium))
            }

            // Active filters display
            if (state.selectedAccountId.isNotEmpty() || state.selectedType != null || state.timeRange != TransactionTimeRange.ALL) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = Spacing.small),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.small),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = buildFilterLabel(state),
                            style = AppTypography.small,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(
                            onClick = { viewModel.handleEvent(MoneyTransactionsEvent.ClearFilters) }
                        ) {
                            Text(stringResource(R.string.clear))
                        }
                    }
                }
            }

            if (showSearch) {
                SearchBox(
                    query = state.searchQuery,
                    onQueryChange = { 
                        viewModel.handleEvent(MoneyTransactionsEvent.SearchQueryChanged(it))
                    },
                    onSearch = {},
                    placeholder = stringResource(R.string.search_transactions)
                )
                Spacer(modifier = Modifier.height(Spacing.small))
            }

            // Loading
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            // Error
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
                        TextButton(
                            onClick = { viewModel.handleEvent(MoneyTransactionsEvent.LoadTransactions) }
                        ) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
                return@Column
            }

            // Empty
            if (state.filteredTransactions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📭", fontSize = 48.sp)
                        Text(
                            text = stringResource(R.string.no_transactions),
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

            // Count
            Text(
                text = "${state.filteredTransactions.size} ${stringResource(R.string.transactions)}",
                style = AppTypography.label,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = Spacing.small)
            )

            // Transaction List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(Spacing.small),
                contentPadding = PaddingValues(bottom = Spacing.xxlarge)
            ) {
                items(
                    items = state.filteredTransactions,
                    key = { it.id }
                ) { transaction ->
                    TransactionCard(
                        transaction = transaction,
                        accounts = state.accounts
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionCard(
    transaction: MoneyTransaction,
    accounts: List<com.akari.retailer.features.money.domain.models.MoneyAccount>
) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    val fromAccount = accounts.find { it.id == transaction.fromAccountId }
    val toAccount = accounts.find { it.id == transaction.toAccountId }
    
    val isIncoming = transaction.type in listOf(
        MoneyTransactionType.SALE_IN,
        MoneyTransactionType.INCOME_IN,
        MoneyTransactionType.TRANSFER_IN,
        MoneyTransactionType.EXTERNAL_IN,
        MoneyTransactionType.FEE_IN
    )
    
    val color = if (isIncoming) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Surface(
                modifier = Modifier.size(40.dp),
                shape = MaterialTheme.shapes.small,
                color = color.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (isIncoming) "↑" else "↓",
                        style = AppTypography.title,
                        color = color,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(Spacing.medium))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = transaction.getDisplayType(),
                    style = AppTypography.body,
                    fontWeight = FontWeight.Medium
                )
                
                if (transaction.description.isNotEmpty()) {
                    Text(
                        text = transaction.description,
                        style = AppTypography.small,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 1
                    )
                }
                
                // Account info
                val accountInfo = when {
                    transaction.type == MoneyTransactionType.TRANSFER_OUT || 
                    transaction.type == MoneyTransactionType.TRANSFER_IN -> {
                        "${fromAccount?.name ?: "?"} → ${toAccount?.name ?: "?"}"
                    }
                    transaction.type == MoneyTransactionType.EXTERNAL_OUT -> {
                        "${fromAccount?.name ?: "?"} → ${transaction.externalAccountName}"
                    }
                    transaction.type == MoneyTransactionType.EXTERNAL_IN -> {
                        "${transaction.externalAccountName} → ${toAccount?.name ?: "?"}"
                    }
                    isIncoming -> toAccount?.name ?: ""
                    else -> fromAccount?.name ?: ""
                }
                
                if (accountInfo.isNotEmpty()) {
                    Text(
                        text = accountInfo,
                        style = AppTypography.small,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = dateFormat.format(Date(transaction.date)),
                        style = AppTypography.small,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        fontSize = 10.sp
                    )
                    
                    if (transaction.fee > 0) {
                        Text(
                            text = if (transaction.feeType == FeeType.FEE_PAID) 
                                "Fee: -${transaction.fee}" 
                            else 
                                "Fee: +${transaction.fee}",
                            style = AppTypography.small,
                            color = if (transaction.feeType == FeeType.FEE_PAID) 
                                MaterialTheme.colorScheme.error 
                            else 
                                MaterialTheme.colorScheme.primary,
                            fontSize = 10.sp
                        )
                    }
                }
            }
            
            Text(
                text = (if (isIncoming) "+" else "-") + "${transaction.amount}",
                style = AppTypography.title,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterSheet(
    state: MoneyTransactionsState,
    onAccountSelect: (String) -> Unit,
    onTypeSelect: (MoneyTransactionType?) -> Unit,
    onTimeRangeSelect: (TransactionTimeRange) -> Unit,
    onClearFilters: () -> Unit
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
                text = stringResource(R.string.filters),
                style = AppTypography.title
            )
            TextButton(onClick = onClearFilters) {
                Text(stringResource(R.string.clear_all))
            }
        }
        
        Spacer(modifier = Modifier.height(Spacing.medium))
        
        // Time Range
        Text(
            text = stringResource(R.string.time_range),
            style = AppTypography.label,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TransactionTimeRange.values().forEach { range ->
                FilterChip(
                    selected = state.timeRange == range,
                    onClick = { onTimeRangeSelect(range) },
                    label = { 
                        Text(
                            text = when (range) {
                                TransactionTimeRange.TODAY -> stringResource(R.string.today_range)
                                TransactionTimeRange.THIS_WEEK -> stringResource(R.string.this_week)
                                TransactionTimeRange.THIS_MONTH -> stringResource(R.string.this_month)
                                TransactionTimeRange.ALL -> stringResource(R.string.all_types)
                            },
                            fontSize = 11.sp
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(Spacing.medium))
        
        // Account Filter
        Text(
            text = stringResource(R.string.money_account),
            style = AppTypography.label,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            FilterChip(
                selected = state.selectedAccountId.isEmpty(),
                onClick = { onAccountSelect("") },
                label = { Text(stringResource(R.string.all_accounts)) },
                modifier = Modifier.fillMaxWidth()
            )
            state.accounts.forEach { account ->
                FilterChip(
                    selected = state.selectedAccountId == account.id,
                    onClick = { onAccountSelect(account.id) },
                    label = { Text(account.getDisplayName()) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        Spacer(modifier = Modifier.height(Spacing.medium))
        
        // Type Filter
        Text(
            text = stringResource(R.string.transaction_type),
            style = AppTypography.label,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FilterChip(
                selected = state.selectedType == null,
                onClick = { onTypeSelect(null) },
                label = { Text(stringResource(R.string.all_types), fontSize = 11.sp) }
            )
            MoneyTransactionType.values().forEach { type ->
                FilterChip(
                    selected = state.selectedType == type,
                    onClick = { onTypeSelect(type) },
                    label = { Text(type.name.replace("_", " "), fontSize = 10.sp) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(Spacing.xxlarge))
    }
}

private fun buildFilterLabel(state: MoneyTransactionsState): String {
    val parts = mutableListOf<String>()
    if (state.selectedAccountId.isNotEmpty()) {
        parts.add("Account")
    }
    if (state.selectedType != null) {
        parts.add("Type: ${state.selectedType.name}")
    }
    if (state.timeRange != TransactionTimeRange.ALL) {
        parts.add(state.timeRange.name.replace("_", " "))
    }
    return parts.joinToString(" • ")
}
