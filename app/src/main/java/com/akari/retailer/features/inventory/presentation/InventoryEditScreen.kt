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
import com.akari.retailer.features.inventory.data.remote.FirestoreInventoryService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryEditScreen(
    productId: String,
    onBack: () -> Unit,
    onProductUpdated: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as RetailApplication
    
    val service = remember { FirestoreInventoryService() }
    val repository = remember { FirestoreInventoryRepository(service) }
    
    val viewModel: InventoryEditViewModel = viewModel(
        factory = InventoryEditViewModelFactory(repository, productId)
    )
    
    val state by viewModel.state.collectAsState()

    AppScreen(
        title = stringResource(R.string.edit_product),
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

            if (state.error != null) {
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
                                viewModel.handleEvent(InventoryEditEvent.LoadProduct)
                            }
                        )
                    }
                }
                return@Column
            }

            OutlinedTextField(
                value = state.name,
                onValueChange = { viewModel.handleEvent(InventoryEditEvent.NameChanged(it)) },
                label = { Text(stringResource(R.string.product_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            OutlinedTextField(
                value = state.category,
                onValueChange = { viewModel.handleEvent(InventoryEditEvent.CategoryChanged(it)) },
                label = { Text(stringResource(R.string.product_category)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            OutlinedTextField(
                value = state.sku,
                onValueChange = { viewModel.handleEvent(InventoryEditEvent.SkuChanged(it)) },
                label = { Text(stringResource(R.string.product_sku)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            OutlinedTextField(
                value = state.costPrice,
                onValueChange = { viewModel.handleEvent(InventoryEditEvent.CostPriceChanged(it)) },
                label = { Text(stringResource(R.string.product_cost_price)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            OutlinedTextField(
                value = state.sellPrice,
                onValueChange = { viewModel.handleEvent(InventoryEditEvent.SellPriceChanged(it)) },
                label = { Text(stringResource(R.string.product_sell_price)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            OutlinedTextField(
                value = state.stockQuantity,
                onValueChange = { viewModel.handleEvent(InventoryEditEvent.StockQuantityChanged(it)) },
                label = { Text(stringResource(R.string.product_stock_quantity)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            OutlinedTextField(
                value = state.minStockLevel,
                onValueChange = { viewModel.handleEvent(InventoryEditEvent.MinStockLevelChanged(it)) },
                label = { Text(stringResource(R.string.product_min_stock)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            OutlinedTextField(
                value = state.supplierId,
                onValueChange = { viewModel.handleEvent(InventoryEditEvent.SupplierIdChanged(it)) },
                label = { Text(stringResource(R.string.product_supplier_id)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
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
                text = if (state.isSaving) stringResource(R.string.updating) else stringResource(R.string.update_product),
                onClick = {
                    viewModel.handleEvent(InventoryEditEvent.SaveProduct)
                },
                isLoading = state.isSaving,
                enabled = state.name.isNotEmpty() && state.sellPrice.isNotEmpty() && !state.isSaving
            )
            
            if (state.saveSuccess) {
                LaunchedEffect(Unit) {
                    onProductUpdated()
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.xxlarge))
        }
    }
}
