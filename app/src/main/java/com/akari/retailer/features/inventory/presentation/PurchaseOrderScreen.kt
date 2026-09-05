package com.akari.retailer.features.inventory.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akari.retailer.RetailApplication
import com.akari.retailer.core.ui.components.AppPrimaryButton
import com.akari.retailer.core.ui.components.AppScreen
import com.akari.retailer.core.ui.theme.AppTypography
import com.akari.retailer.core.ui.theme.Spacing
import com.akari.retailer.features.inventory.data.repository.FirestoreInventoryRepository
import com.akari.retailer.features.inventory.data.repository.FirestoreStockRepository
import com.akari.retailer.features.inventory.data.remote.FirestoreInventoryService
import com.akari.retailer.features.inventory.data.remote.FirestoreStockService
import com.akari.retailer.features.inventory.domain.models.PurchaseOrderStatus
import com.akari.retailer.features.supplier.data.repository.FirestoreSupplierRepository
import com.akari.retailer.features.supplier.data.remote.FirestoreSupplierService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseOrderScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as RetailApplication
    
    val inventoryService = remember { FirestoreInventoryService() }
    val inventoryRepository = remember { FirestoreInventoryRepository(inventoryService) }
    val stockService = remember { FirestoreStockService() }
    val stockRepository = remember { FirestoreStockRepository(stockService) }
    val supplierService = remember { FirestoreSupplierService() }
    val supplierRepository = remember { FirestoreSupplierRepository(supplierService) }
    
    val viewModel: PurchaseOrderViewModel = viewModel(
        factory = PurchaseOrderViewModelFactory(inventoryRepository, stockRepository, supplierRepository)
    )
    
    val state by viewModel.state.collectAsState()
    
    var supplierSearch by remember { mutableStateOf("") }
    var productSearch by remember { mutableStateOf("") }
    var showSupplierDropdown by remember { mutableStateOf(false) }
    var showProductDropdown by remember { mutableStateOf(false) }
    var showStatusDropdown by remember { mutableStateOf(false) }
    
    val filteredSuppliers = state.suppliers.filter { 
        it.name.contains(supplierSearch, ignoreCase = true) 
    }
    val filteredProducts = state.products.filter { 
        it.name.contains(productSearch, ignoreCase = true) 
    }

    AppScreen(
        title = "Add Purchase Order",
        showBackButton = true,
        onBackClick = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            state.error?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        style = AppTypography.body,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(Spacing.medium)
                    )
                }
                Spacer(modifier = Modifier.height(Spacing.medium))
            }

            // Supplier Searchable Dropdown
            Column {
                OutlinedTextField(
                    value = supplierSearch,
                    onValueChange = { 
                        supplierSearch = it
                        showSupplierDropdown = true
                    },
                    label = { Text("Search Supplier *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused && state.suppliers.isNotEmpty()) {
                                showSupplierDropdown = true
                            }
                        },
                    singleLine = true
                )
                
                state.selectedSupplier?.let { supplier ->
                    Text(
                        text = "✅ Selected: ${supplier.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                
                if (showSupplierDropdown && state.suppliers.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 150.dp),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        LazyColumn {
                            if (filteredSuppliers.isEmpty()) {
                                item {
                                    Text(
                                        "No suppliers match",
                                        modifier = Modifier.padding(Spacing.medium),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            } else {
                                items(filteredSuppliers) { supplier ->
                                    Text(
                                        text = supplier.name,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.handleEvent(PurchaseOrderEvent.SupplierSelected(supplier))
                                                supplierSearch = supplier.name
                                                showSupplierDropdown = false
                                            }
                                            .padding(Spacing.medium)
                                    )
                                    Divider()
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            // Product Searchable Dropdown
            Column {
                OutlinedTextField(
                    value = productSearch,
                    onValueChange = { 
                        productSearch = it
                        showProductDropdown = true
                    },
                    label = { Text("Search Product *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused && state.products.isNotEmpty()) {
                                showProductDropdown = true
                            }
                        },
                    singleLine = true
                )
                
                state.selectedProduct?.let { product ->
                    Text(
                        text = "✅ Selected: ${product.name} (Stock: ${product.stockQuantity})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                
                if (showProductDropdown && state.products.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 150.dp),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        LazyColumn {
                            if (filteredProducts.isEmpty()) {
                                item {
                                    Text(
                                        "No products match",
                                        modifier = Modifier.padding(Spacing.medium),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            } else {
                                items(filteredProducts) { product ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.handleEvent(PurchaseOrderEvent.ProductSelected(product))
                                                productSearch = product.name
                                                showProductDropdown = false
                                            }
                                            .padding(Spacing.medium)
                                    ) {
                                        Text(text = product.name)
                                        Text(
                                            text = "Stock: ${product.stockQuantity} | Price: ${product.sellPrice}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Divider()
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            // Quantity
            OutlinedTextField(
                value = state.quantity,
                onValueChange = { viewModel.handleEvent(PurchaseOrderEvent.QuantityChanged(it)) },
                label = { Text("Quantity *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            // Cost Price
            OutlinedTextField(
                value = state.costPrice,
                onValueChange = { viewModel.handleEvent(PurchaseOrderEvent.CostPriceChanged(it)) },
                label = { Text("Cost Price *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            // Total Display
            if (state.totalCost > 0) {
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
                            text = "Total Cost",
                            style = AppTypography.body
                        )
                        Text(
                            text = "${state.totalCost}",
                            style = AppTypography.header,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(Spacing.medium))
            }
            
            // Status Dropdown
            Column {
                OutlinedTextField(
                    value = when (state.selectedStatus) {
                        PurchaseOrderStatus.DRAFT -> "Draft"
                        PurchaseOrderStatus.SENT -> "Sent"
                        PurchaseOrderStatus.ACKNOWLEDGED -> "Acknowledged"
                        PurchaseOrderStatus.RECEIVED -> "Received"
                        PurchaseOrderStatus.INVOICED -> "Invoiced"
                        PurchaseOrderStatus.CLOSED -> "Closed"
                    },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Status") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showStatusDropdown = !showStatusDropdown }
                )
                
                if (showStatusDropdown) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 150.dp),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        LazyColumn {
                            PurchaseOrderStatus.values().forEach { status ->
                                val statusText = when (status) {
                                    PurchaseOrderStatus.DRAFT -> "Draft"
                                    PurchaseOrderStatus.SENT -> "Sent"
                                    PurchaseOrderStatus.ACKNOWLEDGED -> "Acknowledged"
                                    PurchaseOrderStatus.RECEIVED -> "Received"
                                    PurchaseOrderStatus.INVOICED -> "Invoiced"
                                    PurchaseOrderStatus.CLOSED -> "Closed"
                                }
                                item {
                                    Text(
                                        text = statusText,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.handleEvent(PurchaseOrderEvent.StatusChanged(status))
                                                showStatusDropdown = false
                                            }
                                            .padding(Spacing.medium)
                                    )
                                    Divider()
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            // Invoice Number
            OutlinedTextField(
                value = state.invoiceNumber,
                onValueChange = { viewModel.handleEvent(PurchaseOrderEvent.InvoiceNumberChanged(it)) },
                label = { Text("Invoice Number") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            // Invoice Amount
            OutlinedTextField(
                value = state.invoiceAmount,
                onValueChange = { viewModel.handleEvent(PurchaseOrderEvent.InvoiceAmountChanged(it)) },
                label = { Text("Invoice Amount") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            // Notes
            OutlinedTextField(
                value = state.notes,
                onValueChange = { viewModel.handleEvent(PurchaseOrderEvent.NotesChanged(it)) },
                label = { Text("Notes (Optional)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            )
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            // Save Button
            val buttonEnabled = state.selectedSupplier != null &&
                                state.selectedProduct != null &&
                                state.quantity.isNotEmpty() &&
                                state.costPrice.isNotEmpty() &&
                                !state.isSaving
            
            AppPrimaryButton(
                text = if (state.isSaving) "Saving..." else "Save Purchase",
                onClick = {
                    viewModel.handleEvent(PurchaseOrderEvent.SavePurchase)
                },
                isLoading = state.isSaving,
                enabled = buttonEnabled
            )
            
            if (state.saveSuccess) {
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(1500)
                    onBack()
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.xxlarge))
        }
    }
}
