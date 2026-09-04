package com.akari.retailer.features.sales.presentation.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akari.retailer.R
import com.akari.retailer.RetailApplication
import com.akari.retailer.core.ui.components.AppPrimaryButton
import com.akari.retailer.core.ui.components.AppScreen
import com.akari.retailer.core.ui.components.DatePickerDialog
import com.akari.retailer.core.ui.components.SaleCard
import com.akari.retailer.core.ui.theme.AppTypography
import com.akari.retailer.core.ui.theme.Spacing

@Composable
fun SaleHistoryScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as RetailApplication
    val repository = remember { application.container.saleRepository }
    
    val viewModel: SaleHistoryViewModel = viewModel(
        factory = SaleHistoryViewModelFactory(repository)
    )
    
    val state by viewModel.state.collectAsState()
    
    var showDeleteDialog by remember { mutableStateOf(false) }
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

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

    // Date Picker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDateSelected = { timestamp ->
                viewModel.handleEvent(SaleHistoryEvent.FilterByDate(timestamp))
                showDatePicker = false
            },
            onDismiss = {
                showDatePicker = false
            }
        )
    }

    AppScreen(
        title = stringResource(R.string.history),
        showBackButton = true,
        onBackClick = onBack,
        showSearchButton = false,  // ✅ Removed
        showDateFilter = true,
        onDateFilterClick = { showDatePicker = true },
        showHistoryButton = false
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Filter indicator
            if (state.filterDate != null) {
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
                            text = "📅 Filtered by: ${state.filterDateLabel}",
                            style = AppTypography.body
                        )
                        TextButton(
                            onClick = {
                                viewModel.handleEvent(SaleHistoryEvent.ClearDateFilter)
                            }
                        ) {
                            Text("Clear")
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
                        text = if (state.filterDate != null) "No sales for selected date" else "No sales yet",
                        style = AppTypography.header,
                        modifier = Modifier.padding(vertical = Spacing.medium)
                    )
                    Text(
                        text = if (state.filterDate != null) "Try selecting a different date" else "Add your first sale from the home screen",
                        style = AppTypography.body,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            } else {
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
                            onDelete = {
                                pendingDeleteId = sale.id
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
}
