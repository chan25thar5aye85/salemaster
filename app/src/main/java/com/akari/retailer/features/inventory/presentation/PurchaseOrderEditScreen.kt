package com.akari.retailer.features.inventory.presentation

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.akari.retailer.RetailApplication
import com.akari.retailer.core.ui.components.AppPrimaryButton
import com.akari.retailer.core.ui.components.AppScreen
import com.akari.retailer.core.ui.theme.AppTypography
import com.akari.retailer.core.ui.theme.Spacing
import com.akari.retailer.features.inventory.data.repository.FirestoreInventoryRepository
import com.akari.retailer.features.inventory.data.repository.FirestorePurchaseOrderRepository
import com.akari.retailer.features.inventory.data.remote.FirestoreInventoryService
import com.akari.retailer.features.inventory.domain.models.PurchaseOrder
import com.akari.retailer.features.inventory.domain.models.PurchaseOrderItem
import com.akari.retailer.features.inventory.domain.models.PurchaseOrderStatus
import com.akari.retailer.navigation.Routes
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseOrderEditScreen(
    orderId: String,
    onBack: () -> Unit,
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val repository = remember { FirestorePurchaseOrderRepository() }
    val inventoryService = remember { FirestoreInventoryService() }
    val inventoryRepository = remember { FirestoreInventoryRepository(inventoryService) }
    
    var order by remember { mutableStateOf<PurchaseOrder?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var items by remember { mutableStateOf<List<PurchaseOrderItem>>(emptyList()) }
    var notes by remember { mutableStateOf("") }
    var products by remember { mutableStateOf<List<com.akari.retailer.features.inventory.domain.models.Product>>(emptyList()) }
    
    // Load order and products
    LaunchedEffect(orderId) {
        isLoading = true
        try {
            // Load products first
            inventoryRepository.getProducts().collect { productList ->
                products = productList
            }
            
            // Load order
            repository.getOrder(orderId).collect { loadedOrder ->
                order = loadedOrder
                if (loadedOrder != null) {
                    items = loadedOrder.items
                    notes = loadedOrder.notes
                }
                isLoading = false
            }
        } catch (e: Exception) {
            isLoading = false
            errorMessage = e.message ?: "Failed to load order"
        }
    }

    AppScreen(
        title = "Edit Order",
        showBackButton = true,
        onBackClick = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                
                errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("❌", style = AppTypography.header)
                            Text(
                                text = errorMessage!!,
                                style = AppTypography.body,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = Spacing.medium)
                            )
                            AppPrimaryButton(
                                text = "Go Back",
                                onClick = onBack
                            )
                        }
                    }
                }
                
                order == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📭", fontSize = 48.sp)
                            Text("Order not found", style = AppTypography.header)
                            AppPrimaryButton(
                                text = "Go Back",
                                onClick = onBack
                            )
                        }
                    }
                }
                
                order?.status != PurchaseOrderStatus.DRAFT -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔒", fontSize = 48.sp)
                            Text(
                                text = "Only DRAFT orders can be edited",
                                style = AppTypography.header,
                                modifier = Modifier.padding(top = Spacing.medium)
                            )
                            Text(
                                text = "Current status: ${order?.status}",
                                style = AppTypography.body,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(Spacing.medium))
                            AppPrimaryButton(
                                text = "View Details",
                                onClick = {
                                    navController.navigate(
                                        Routes.PURCHASE_ORDER_DETAIL.replace("{orderId}", orderId)
                                    )
                                }
                            )
                        }
                    }
                }
                
                else -> {
                    val currentOrder = order!!
                    
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
                            Text("Order: ${currentOrder.orderNumber}", style = AppTypography.body)
                            Text("Status: DRAFT", style = AppTypography.body)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(Spacing.medium))
                    
                    errorMessage?.let { error ->
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
                    
                    Text(
                        text = "Edit items below. Only DRAFT orders can be modified.",
                        style = AppTypography.small,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = Spacing.medium)
                    )
                    
                    // Items Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Items (${items.size})",
                            style = AppTypography.title
                        )
                        TextButton(
                            onClick = {
                                items = items + PurchaseOrderItem()
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                            Text(" Add Item")
                        }
                    }
                    
                    // Items List
                    if (items.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        ) {
                            Text(
                                text = "No items. Tap 'Add Item' to add products.",
                                style = AppTypography.body,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.padding(Spacing.medium)
                            )
                        }
                    } else {
                        items.forEachIndexed { index, item ->
                            EditItemRow(
                                item = item,
                                products = products,
                                onProductSelected = { product ->
                                    val updatedItems = items.toMutableList()
                                    updatedItems[index] = PurchaseOrderItem(
                                        productId = product.id,
                                        productName = product.name,
                                        quantity = updatedItems[index].quantity,
                                        costPrice = product.sellPrice,
                                        total = updatedItems[index].quantity * product.sellPrice,
                                        receivedQuantity = 0
                                    )
                                    items = updatedItems
                                },
                                onQuantityChange = { qty ->
                                    val updatedItems = items.toMutableList()
                                    val current = updatedItems[index]
                                    updatedItems[index] = current.copy(
                                        quantity = qty,
                                        total = qty * current.costPrice
                                    )
                                    items = updatedItems
                                },
                                onPriceChange = { price ->
                                    val updatedItems = items.toMutableList()
                                    val current = updatedItems[index]
                                    updatedItems[index] = current.copy(
                                        costPrice = price,
                                        total = current.quantity * price
                                    )
                                    items = updatedItems
                                },
                                onRemove = {
                                    items = items.filterIndexed { i, _ -> i != index }
                                }
                            )
                            Spacer(modifier = Modifier.height(Spacing.small))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(Spacing.medium))
                    
                    // Total
                    val total = items.sumOf { it.total }
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
                            Text("Total", style = AppTypography.title)
                            Text("$total", style = AppTypography.header, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(Spacing.medium))
                    
                    // Notes
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (Optional)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(Spacing.medium))
                    
                    // Save Button
                    val buttonEnabled = items.isNotEmpty() && !isSaving
                    
                    if (!buttonEnabled && errorMessage == null) {
                        Text(
                            text = if (items.isEmpty()) "⚠️ Add at least one item" else "",
                            style = AppTypography.small,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = Spacing.small)
                        )
                    }
                    
                    AppPrimaryButton(
                        text = if (isSaving) "Saving..." else "Update Order",
                        onClick = {
                            if (items.isEmpty()) {
                                errorMessage = "Add at least one item"
                            } else if (items.any { it.productId.isEmpty() || it.quantity <= 0 || it.costPrice <= 0 }) {
                                errorMessage = "Fill in all item fields correctly"
                            } else {
                                errorMessage = null
                                isSaving = true
                                scope.launch {
                                    try {
                                        val updatedOrder = currentOrder.copy(
                                            items = items,
                                            notes = notes,
                                            totalCost = items.sumOf { it.total },
                                            updatedAt = System.currentTimeMillis()
                                        )
                                        val result = repository.updateOrder(updatedOrder)
                                        if (result.isSuccess) {
                                            Toast.makeText(context, "Order updated", Toast.LENGTH_SHORT).show()
                                            onBack()
                                        } else {
                                            errorMessage = result.exceptionOrNull()?.message ?: "Failed to update order"
                                            Toast.makeText(context, "Error: ${errorMessage}", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        errorMessage = e.message ?: "Failed to update order"
                                        Toast.makeText(context, "Error: ${errorMessage}", Toast.LENGTH_SHORT).show()
                                    }
                                    isSaving = false
                                }
                            }
                        },
                        isLoading = isSaving,
                        enabled = buttonEnabled
                    )
                    
                    Spacer(modifier = Modifier.height(Spacing.xxlarge))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditItemRow(
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
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = if (item.productName.isEmpty()) "Select Product" else item.productName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Product") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    singleLine = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    products.forEach { product ->
                        DropdownMenuItem(
                            text = { 
                                Column {
                                    Text(product.name)
                                    Text(
                                        text = "Stock: ${product.stockQuantity} | Price: ${product.sellPrice}",
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
            
            Spacer(modifier = Modifier.height(Spacing.small))
            
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
                    label = { Text("Qty") },
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
                    label = { Text("Price") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                }
            }
            
            if (item.total > 0) {
                Text(
                    text = "Subtotal: ${item.total}",
                    style = AppTypography.small,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = Spacing.small)
                )
            }
        }
    }
}
