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

@Composable
fun SaleEntryScreen(
    navController: NavController? = null
) {
    val context = LocalContext.current
    val application = context.applicationContext as RetailApplication
    
    val repository = remember { application.container.saleRepository }
    
    val viewModel: SaleEntryViewModel = viewModel(
        factory = SaleEntryViewModelFactory(repository)
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
        focusRequesters.keys
            .filter { it !in currentIds }
            .toList()
            .forEach { focusRequesters.remove(it) }
    }

    LaunchedEffect(state.rows) {
        val focusedRow = state.rows.firstOrNull { it.isFocused }
        if (focusedRow != null) {
            val requester = focusRequesters.getOrPut(focusedRow.id) {
                FocusRequester()
            }
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
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "📡 No internet connection. Sales will sync when online.",
                            style = AppTypography.body,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                state.rows.forEachIndexed { index, row ->
                    key(row.id) {
                        val requester = focusRequesters.getOrPut(row.id) {
                            FocusRequester()
                        }

                        ItemRow(
                            index = index,
                            amount = row.amount,
                            onAmountChange = { value ->
                                viewModel.handleEvent(
                                    SaleEntryEvent.AmountChanged(row.id, value)
                                )
                            },
                            onFocus = {
                                viewModel.handleEvent(SaleEntryEvent.RowFocused(row.id))
                            },
                            onNext = {
                                viewModel.handleEvent(SaleEntryEvent.NextPressed(row.id))
                            },
                            onDelete = {
                                viewModel.handleEvent(SaleEntryEvent.RowDeleted(row.id))
                            },
                            focusRequester = requester,
                            showDelete = !(state.rows.size == 1)
                        )

                        Spacer(modifier = Modifier.height(Spacing.medium))
                    }
                }

                Divider(
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                SaleSummary(
                    total = viewModel.getFormattedTotal(),
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.payment),
                        style = AppTypography.body,
                        modifier = Modifier.width(80.dp)
                    )

                    PaymentSelector(
                        selectedMethod = state.paymentMethod,
                        onMethodSelected = { method ->
                            viewModel.handleEvent(SaleEntryEvent.PaymentSelected(method))
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                state.error?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = AppTypography.body,
                        modifier = Modifier.padding(vertical = 8.dp)
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
                    enabled = !state.isSaving
                )
                
                if (!isOnline && !state.saveSuccess) {
                    Text(
                        text = "⏳ Will save when online",
                        style = AppTypography.small,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            RecentSalesTable(
                sales = state.recentSales,
                onViewAllClick = {
                    navController?.navigate(Routes.HISTORY)
                },
                maxItems = 2
            )

            Spacer(modifier = Modifier.height(Spacing.xxlarge))
        }
    }
}
