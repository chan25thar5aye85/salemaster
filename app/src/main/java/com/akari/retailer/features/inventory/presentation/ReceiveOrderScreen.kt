package com.akari.retailer.features.inventory.presentation

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akari.retailer.RetailApplication
import com.akari.retailer.core.ui.components.AppPrimaryButton
import com.akari.retailer.core.ui.components.AppScreen
import com.akari.retailer.core.ui.theme.AppTypography
import com.akari.retailer.core.ui.theme.Spacing
import com.akari.retailer.features.inventory.data.repository.FirestoreInventoryRepository
import com.akari.retailer.features.inventory.data.repository.FirestorePurchaseOrderRepository
import com.akari.retailer.features.inventory.data.repository.FirestoreStockRepository
import com.akari.retailer.features.inventory.data.remote.FirestoreInventoryService
import com.akari.retailer.features.inventory.data.remote.FirestoreStockService
import com.akari.retailer.features.inventory.domain.models.MovementType
import com.akari.retailer.features.inventory.domain.models.PurchaseOrderItem
import com.akari.retailer.features.inventory.domain.models.PurchaseOrderStatus
import com.akari.retailer.features.inventory.domain.models.StockMovement
import kotlinx.coroutines.launch

@Composable
fun ReceiveOrderScreen(
    orderId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val repository = remember { FirestorePurchaseOrderRepository() }
    val inventoryService = remember { FirestoreInventoryService() }
    val inventoryRepository = remember { FirestoreInventoryRepository(inventoryService) }
    val stockService = remember { FirestoreStockService() }
    val stockRepository = remember { FirestoreStockRepository(stockService) }
    
    var order by remember { mutableStateOf<com.akari.retailer.features.inventory.domain.models.PurchaseOrder?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isReceiving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var receiveQuantities by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    
    LaunchedEffect(orderId) {
        isLoading = true
        try {
            repository.getOrder(orderId).collect { loadedOrder ->
                order = loadedOrder
                if (loadedOrder != null) {
                    // Initialize receive quantities with remaining amounts
                    receiveQuantities = loadedOrder.items.associate { item ->
                        val remaining = item.quantity - item.receivedQuantity
                        item.productId to (if (remaining > 0) remaining.toString() else "0")
                    }
                }
                isLoading = false
            }
        } catch (e: Exception) {
            isLoading = false
            errorMessage = e.message ?: "Failed to load order"
        }
    }

    AppScreen(
        title = "Receive Items",
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
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Text("Loading order...", modifier = Modifier.padding(top = Spacing.medium))
                        }
                    }
                }
                
                errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("❌", fontSize = 40.sp)
                            Text(errorMessage!!, style = AppTypography.body, color = MaterialTheme.colorScheme.error)
                            AppPrimaryButton(text = "Go Back", onClick = onBack)
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
                            AppPrimaryButton(text = "Go Back", onClick = onBack)
                        }
                    }
                }
                
                else -> {
                    val currentOrder = order!!
                    
                    // Order info
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
                            Text("Order: ${currentOrder.orderNumber}", style = AppTypography.title)
                            Text("Supplier: ${currentOrder.supplierName}", style = AppTypography.body)
                            Text(
                                text = "Status: ${currentOrder.status}",
                                style = AppTypography.small,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
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
                        text = "Enter quantities to receive for each item:",
                        style = AppTypography.body,
                        modifier = Modifier.padding(bottom = Spacing.medium)
                    )
                    
                    // Items list with receive quantities
                    currentOrder.items.forEach { item ->
                        val remaining = item.quantity - item.receivedQuantity
                        val isFullyReceived = remaining <= 0
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isFullyReceived) 
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                else 
                                    MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.medium)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = item.productName,
                                        style = AppTypography.title
                                    )
                                    if (isFullyReceived) {
                                        Text(
                                            text = "✅ Fully Received",
                                            style = AppTypography.small,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Ordered: ${item.quantity}",
                                        style = AppTypography.small,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = "Received: ${item.receivedQuantity}",
                                        style = AppTypography.small,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = "Remaining: $remaining",
                                        style = AppTypography.small,
                                        color = if (remaining > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                                
                                if (!isFullyReceived) {
                                    Spacer(modifier = Modifier.height(Spacing.small))
                                    
                                    OutlinedTextField(
                                        value = receiveQuantities[item.productId] ?: "",
                                        onValueChange = { value ->
                                            // Only allow numbers
                                            val cleaned = value.filter { it.isDigit() }
                                            val maxRemaining = item.quantity - item.receivedQuantity
                                            val parsed = cleaned.toIntOrNull() ?: 0
                                            val finalValue = if (parsed > maxRemaining) maxRemaining.toString() else cleaned
                                            receiveQuantities = receiveQuantities.toMutableMap().apply {
                                                put(item.productId, finalValue)
                                            }
                                        },
                                        label = { Text("Quantity to receive") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(Spacing.small))
                    }
                    
                    Spacer(modifier = Modifier.height(Spacing.medium))
                    
                    // Total receiving summary
                    val totalToReceive = receiveQuantities.values.sumOf { it.toIntOrNull() ?: 0 }
                    if (totalToReceive > 0) {
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
                                Text("Total items to receive", style = AppTypography.body)
                                Text("$totalToReceive", style = AppTypography.header, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Spacer(modifier = Modifier.height(Spacing.medium))
                    }
                    
                    // Receive Button
                    val hasItemsToReceive = receiveQuantities.any { (_, value) -> 
                        value.toIntOrNull() ?: 0 > 0 
                    }
                    
                    AppPrimaryButton(
                        text = if (isReceiving) "Receiving..." else "Receive Items",
                        onClick = {
                            if (!hasItemsToReceive) {
                                errorMessage = "Enter at least one quantity to receive"
                                return@AppPrimaryButton
                            }
                            
                            isReceiving = true
                            errorMessage = null
                            
                            scope.launch {
                                try {
                                    val receivedItems = receiveQuantities
                                        .filter { (_, value) -> value.toIntOrNull() ?: 0 > 0 }
                                        .map { (productId, quantity) ->
                                            val qty = quantity.toInt()
                                            val orderedItem = currentOrder.items.find { it.productId == productId }
                                            PurchaseOrderItem(
                                                productId = productId,
                                                productName = orderedItem?.productName ?: "",
                                                quantity = qty,
                                                costPrice = orderedItem?.costPrice ?: 0,
                                                total = qty * (orderedItem?.costPrice ?: 0),
                                                receivedQuantity = 0
                                            )
                                        }
                                    
                                    // Update stock for each received item
                                    receivedItems.forEach { receivedItem ->
                                        val productResult = inventoryRepository.getProductByIdSync(receivedItem.productId)
                                        if (productResult.isSuccess) {
                                            val product = productResult.getOrNull()
                                            if (product != null) {
                                                val newStock = product.stockQuantity + receivedItem.quantity
                                                val updatedProduct = product.copy(
                                                    stockQuantity = newStock,
                                                    updatedAt = System.currentTimeMillis()
                                                )
                                                inventoryRepository.updateProduct(updatedProduct)
                                                
                                                // Record stock movement
                                                val movement = StockMovement(
                                                    productId = receivedItem.productId,
                                                    type = MovementType.PURCHASE,
                                                    quantity = receivedItem.quantity,
                                                    previousStock = product.stockQuantity,
                                                    newStock = newStock,
                                                    reason = "Purchase Order: ${currentOrder.orderNumber}",
                                                    purchaseOrderId = currentOrder.id,
                                                    createdAt = System.currentTimeMillis()
                                                )
                                                stockRepository.addMovement(movement)
                                            }
                                        }
                                    }
                                    
                                    // Update order with received quantities
                                    val updatedItems = currentOrder.items.map { orderedItem ->
                                        val received = receiveQuantities[orderedItem.productId]?.toIntOrNull() ?: 0
                                        orderedItem.copy(
                                            receivedQuantity = orderedItem.receivedQuantity + received
                                        )
                                    }
                                    
                                    val newReceivedCost = updatedItems.sumOf { it.getReceivedTotal() }
                                    val isFullyReceived = updatedItems.all { it.isFullyReceived() }
                                    
                                    val updatedOrder = currentOrder.copy(
                                        items = updatedItems,
                                        receivedCost = newReceivedCost,
                                        status = PurchaseOrderStatus.RECEIVED,
                                        receivedDate = if (isFullyReceived) System.currentTimeMillis() else currentOrder.receivedDate,
                                        updatedAt = System.currentTimeMillis()
                                    )
                                    
                                    repository.updateOrder(updatedOrder)
                                    
                                    Toast.makeText(context, "Items received successfully!", Toast.LENGTH_SHORT).show()
                                    isReceiving = false
                                    onBack()
                                    
                                } catch (e: Exception) {
                                    isReceiving = false
                                    errorMessage = e.message ?: "Failed to receive items"
                                    Toast.makeText(context, "Error: ${errorMessage}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        isLoading = isReceiving,
                        enabled = !isReceiving
                    )
                    
                    Spacer(modifier = Modifier.height(Spacing.xxlarge))
                }
            }
        }
    }
}
