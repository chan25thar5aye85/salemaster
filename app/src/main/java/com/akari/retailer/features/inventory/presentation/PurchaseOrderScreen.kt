package com.akari.retailer.features.inventory.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
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
import com.akari.retailer.features.inventory.data.repository.FirestorePurchaseOrderRepository
import com.akari.retailer.features.inventory.data.remote.FirestoreInventoryService
import com.akari.retailer.features.inventory.domain.models.PurchaseOrderItem
import com.akari.retailer.features.supplier.data.repository.FirestoreSupplierRepository
import com.akari.retailer.features.supplier.data.remote.FirestoreSupplierService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseOrderScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as RetailApplication
    
    val inventoryService = remember { FirestoreInventoryService() }
    val inventoryRepository = remember { FirestoreInventoryRepository(inventoryService) }
    val supplierService = remember { FirestoreSupplierService() }
    val supplierRepository = remember { FirestoreSupplierRepository(supplierService) }
    val purchaseOrderRepository = remember { FirestorePurchaseOrderRepository() }
    
    val viewModel: PurchaseOrderViewModel = viewModel(
        factory = PurchaseOrderViewModelFactory(
            inventoryRepository, 
            purchaseOrderRepository,
            supplierRepository
        )
    )
    
    val state by viewModel.state.collectAsState()
    
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    
    // Reset success state after navigation
    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) {
            kotlinx.coroutines.delay(500)
            onBack()
        }
    }

    AppScreen(
        title = stringResource(R.string.purchase_order_add),
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

            // Order Name
            OutlinedTextField(
                value = state.orderName,
                onValueChange = { viewModel.handleEvent(PurchaseOrderEvent.OrderNameChanged(it)) },
                label = { Text(stringResource(R.string.order_name_label)) },
                placeholder = { Text(stringResource(R.string.order_name_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(Spacing.medium))

            // Order Number info
            Text(
                text = stringResource(R.string.order_number_auto),
                style = AppTypography.small,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = Spacing.medium)
            )

            // Supplier Selector
            var supplierExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = supplierExpanded,
                onExpandedChange = { supplierExpanded = it }
            ) {
                OutlinedTextField(
                    value = state.selectedSupplier?.name ?: stringResource(R.string.select_supplier),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.supplier_label)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = supplierExpanded) }
                )
                ExposedDropdownMenu(
                    expanded = supplierExpanded,
                    onDismissRequest = { supplierExpanded = false }
                ) {
                    if (state.suppliers.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.no_suppliers)) },
                            onClick = {}
                        )
                    } else {
                        state.suppliers.forEach { supplier ->
                            DropdownMenuItem(
                                text = { Text(supplier.name) },
                                onClick = {
                                    viewModel.handleEvent(PurchaseOrderEvent.SupplierSelected(supplier))
                                    supplierExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            // Expected Delivery Date
            OutlinedTextField(
                value = if (state.expectedDeliveryDate > 0) dateFormat.format(Date(state.expectedDeliveryDate)) else "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Expected Delivery Date") },
                placeholder = { Text("Select date") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.Event, contentDescription = "Select date")
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            // Items Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.add_items),
                    style = AppTypography.title
                )
                TextButton(
                    onClick = {
                        viewModel.handleEvent(PurchaseOrderEvent.AddItem)
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                    Text(stringResource(R.string.add_items))
                }
            }
            
            // Items List
            if (state.tempItems.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        text = "No items added yet. Tap 'Add Item' to add products.",
                        style = AppTypography.body,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(Spacing.medium)
                    )
                }
            } else {
                state.tempItems.forEachIndexed { index, item ->
                    ItemRowWithDropdown(
                        item = item,
                        products = state.products,
                        onProductSelected = { product ->
                            viewModel.handleEvent(
                                PurchaseOrderEvent.UpdateItem(
                                    index = index,
                                    productId = product.id,
                                    productName = product.name,
                                    quantity = item.quantity,
                                    costPrice = item.costPrice
                                )
                            )
                        },
                        onQuantityChange = { qty ->
                            viewModel.handleEvent(
                                PurchaseOrderEvent.UpdateItem(
                                    index = index,
                                    productId = item.productId,
                                    productName = item.productName,
                                    quantity = qty,
                                    costPrice = item.costPrice
                                )
                            )
                        },
                        onPriceChange = { price ->
                            viewModel.handleEvent(
                                PurchaseOrderEvent.UpdateItem(
                                    index = index,
                                    productId = item.productId,
                                    productName = item.productName,
                                    quantity = item.quantity,
                                    costPrice = price
                                )
                            )
                        },
                        onRemove = {
                            viewModel.handleEvent(PurchaseOrderEvent.RemoveItem(index))
                        }
                    )
                    Spacer(modifier = Modifier.height(Spacing.small))
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            // Total
            val total = state.tempItems.sumOf { it.total }
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
                        text = stringResource(R.string.total_label),
                        style = AppTypography.title
                    )
                    Text(
                        text = "$total",
                        style = AppTypography.header,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            // Notes
            OutlinedTextField(
                value = state.notes,
                onValueChange = { viewModel.handleEvent(PurchaseOrderEvent.NotesChanged(it)) },
                label = { Text(stringResource(R.string.notes_optional)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            )
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            // Save Button
            val buttonEnabled = state.selectedSupplier != null && 
                                state.tempItems.isNotEmpty() && 
                                state.orderName.isNotBlank() &&
                                state.expectedDeliveryDate > 0 &&
                                !state.isSaving
            
            if (!buttonEnabled && state.error == null) {
                Text(
                    text = when {
                        state.orderName.isBlank() -> "⚠️ ${stringResource(R.string.order_name_label)}"
                        state.selectedSupplier == null -> "⚠️ ${stringResource(R.string.supplier_label)}"
                        state.tempItems.isEmpty() -> "⚠️ ${stringResource(R.string.add_items)}"
                        state.expectedDeliveryDate == 0L -> "⚠️ Select expected delivery date"
                        else -> ""
                    },
                    style = AppTypography.small,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = Spacing.small)
                )
            }
            
            AppPrimaryButton(
                text = if (state.isSaving) stringResource(R.string.saving) else stringResource(R.string.save_order),
                onClick = {
                    viewModel.handleEvent(PurchaseOrderEvent.SavePurchase)
                },
                isLoading = state.isSaving,
                enabled = buttonEnabled
            )
            
            Spacer(modifier = Modifier.height(Spacing.xxlarge))
        }
    }
    
    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = if (state.expectedDeliveryDate > 0) state.expectedDeliveryDate else System.currentTimeMillis()
        )
        
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { timestamp ->
                            viewModel.handleEvent(PurchaseOrderEvent.ExpectedDeliveryDateChanged(timestamp))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemRowWithDropdown(
    item: PurchaseOrderItem,
    products: List<com.akari.retailer.features.inventory.domain.models.Product>,
    onProductSelected: (com.akari.retailer.features.inventory.domain.models.Product) -> Unit,
    onQuantityChange: (Int) -> Unit,
    onPriceChange: (Int) -> Unit,
    onRemove: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium)
        ) {
            // Row 1: Product Dropdown + Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = if (item.productName.isEmpty()) stringResource(R.string.select_product) else item.productName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.product_label)) },
                        modifier = Modifier
                            .weight(2f)
                            .menuAnchor(),
                        singleLine = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        if (products.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.no_products)) },
                                onClick = { expanded = false }
                            )
                        } else {
                            products.forEach { product ->
                                DropdownMenuItem(
                                    text = { 
                                        Column {
                                            Text(product.name)
                                            Text(
                                                text = "${stringResource(R.string.current_stock)}: ${product.stockQuantity}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        onProductSelected(product)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(Spacing.small))
                
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.small))
            
            // Row 2: Quantity + Price
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
            ) {
                OutlinedTextField(
                    value = if (item.quantity > 0) item.quantity.toString() else "",
                    onValueChange = { 
                        val qty = it.toIntOrNull() ?: 0
                        onQuantityChange(qty)
                    },
                    label = { Text(stringResource(R.string.qty_label)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                
                OutlinedTextField(
                    value = if (item.costPrice > 0) item.costPrice.toString() else "",
                    onValueChange = { 
                        val price = it.toIntOrNull() ?: 0
                        onPriceChange(price)
                    },
                    label = { Text(stringResource(R.string.enter_price)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            
            if (item.total > 0) {
                Text(
                    text = "${stringResource(R.string.subtotal)}: ${item.total}",
                    style = AppTypography.small,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = Spacing.small)
                )
            }
        }
    }
}
