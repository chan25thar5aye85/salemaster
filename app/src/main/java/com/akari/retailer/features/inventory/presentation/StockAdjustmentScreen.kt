package com.akari.retailer.features.inventory.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akari.retailer.R
import com.akari.retailer.RetailApplication
import com.akari.retailer.core.ui.components.AppPrimaryButton
import com.akari.retailer.core.ui.components.AppScreen
import com.akari.retailer.core.ui.theme.AppTypography
import com.akari.retailer.core.ui.theme.Spacing
import com.akari.retailer.features.inventory.data.repository.FirestoreInventoryRepository
import com.akari.retailer.features.inventory.data.repository.FirestoreStockRepository
import com.akari.retailer.features.inventory.data.remote.FirestoreInventoryService
import com.akari.retailer.features.inventory.data.remote.FirestoreStockService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockAdjustmentScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as RetailApplication
    
    val inventoryService = remember { FirestoreInventoryService() }
    val inventoryRepository = remember { FirestoreInventoryRepository(inventoryService) }
    val stockService = remember { FirestoreStockService() }
    val stockRepository = remember { FirestoreStockRepository(stockService) }
    
    val viewModel: StockAdjustmentViewModel = viewModel(
        factory = StockAdjustmentViewModelFactory(inventoryRepository, stockRepository)
    )
    
    val state by viewModel.state.collectAsState()

    AppScreen(
        title = stringResource(R.string.stock_adjustment),
        showBackButton = true,
        onBackClick = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            if (state.error != null && state.selectedProduct == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "❌",
                            style = AppTypography.header
                        )
                        Text(
                            text = state.error!!,
                            style = AppTypography.body,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = Spacing.medium)
                        )
                        AppPrimaryButton(
                            text = stringResource(R.string.retry),
                            onClick = {
                                viewModel.handleEvent(StockAdjustmentEvent.LoadProducts)
                            }
                        )
                    }
                }
                return@Column
            }

            // Product Selector
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = state.selectedProduct?.name ?: stringResource(R.string.select_product_stock),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.product)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    state.products.forEach { product ->
                        DropdownMenuItem(
                            text = { 
                                Column {
                                    Text(text = product.name, style = AppTypography.body)
                                    Text(
                                        text = "${stringResource(R.string.current_stock)}: ${product.stockQuantity} | ${stringResource(R.string.sell_price)}: ${product.sellPrice}",
                                        style = AppTypography.small,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            },
                            onClick = {
                                viewModel.handleEvent(StockAdjustmentEvent.ProductSelected(product))
                                expanded = false
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.medium))

            if (state.selectedProduct != null) {
                // Current Stock Display
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.medium),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.current_stock),
                            style = AppTypography.body
                        )
                        Text(
                            text = "${state.currentStock}",
                            style = AppTypography.header,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(Spacing.medium))
                
                // New Stock
                OutlinedTextField(
                    value = state.newStock,
                    onValueChange = { viewModel.handleEvent(StockAdjustmentEvent.NewStockChanged(it)) },
                    label = { Text(stringResource(R.string.new_stock)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                
                Spacer(modifier = Modifier.height(Spacing.medium))
                
                // Reason
                var reasonExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = reasonExpanded,
                    onExpandedChange = { reasonExpanded = it }
                ) {
                    OutlinedTextField(
                        value = state.reason,
                        onValueChange = { viewModel.handleEvent(StockAdjustmentEvent.ReasonChanged(it)) },
                        readOnly = false,
                        label = { Text(stringResource(R.string.reason)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        trailingIcon = { 
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = reasonExpanded) 
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = reasonExpanded,
                        onDismissRequest = { reasonExpanded = false }
                    ) {
                        listOf(
                            stringResource(R.string.reason_stock_count),
                            stringResource(R.string.reason_damaged),
                            stringResource(R.string.reason_lost),
                            stringResource(R.string.reason_return),
                            stringResource(R.string.reason_correction),
                            stringResource(R.string.reason_other)
                        ).forEach { reason ->
                            DropdownMenuItem(
                                text = { Text(reason) },
                                onClick = {
                                    viewModel.handleEvent(StockAdjustmentEvent.ReasonChanged(reason))
                                    reasonExpanded = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(Spacing.medium))
                
                // Notes
                OutlinedTextField(
                    value = state.notes,
                    onValueChange = { viewModel.handleEvent(StockAdjustmentEvent.NotesChanged(it)) },
                    label = { Text(stringResource(R.string.notes_optional)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            state.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = AppTypography.body,
                    modifier = Modifier.padding(bottom = Spacing.medium)
                )
            }
            
            AppPrimaryButton(
                text = if (state.isSaving) stringResource(R.string.saving_adjustment) else stringResource(R.string.save_adjustment),
                onClick = {
                    viewModel.handleEvent(StockAdjustmentEvent.SaveAdjustment)
                },
                isLoading = state.isSaving,
                enabled = state.selectedProduct != null && 
                         state.newStock.isNotEmpty() && 
                         state.reason.isNotEmpty() && 
                         !state.isSaving
            )
            
            if (state.saveSuccess) {
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(2000)
                    onBack()
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.xxlarge))
        }
    }
}
