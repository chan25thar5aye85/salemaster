package com.akari.retailer.features.inventory.presentation

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    
    LaunchedEffect(orderId) {
        isLoading = true
        try {
            repository.getOrder(orderId).collect { loadedOrder ->
                order = loadedOrder
                isLoading = false
            }
        } catch (e: Exception) {
            isLoading = false
            errorMessage = e.message
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
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            if (order == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Order not found", style = AppTypography.header)
                        AppPrimaryButton(text = "Go Back", onClick = onBack)
                    }
                }
                return@Column
            }

            val currentOrder = order!!
            val currentItems = currentOrder.getItemsForStatus(PurchaseOrderStatus.SENT)
            val receivedItems = currentOrder.getItemsForStatus(PurchaseOrderStatus.RECEIVED)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(Spacing.medium)) {
                    Text("Receiving: ${currentOrder.orderName}", style = AppTypography.title)
                    Text("Supplier: ${currentOrder.supplierName}", style = AppTypography.body)
                }
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            errorMessage?.let {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(it, modifier = Modifier.padding(Spacing.medium), color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.height(Spacing.medium))
            }

            Text("Items to receive:", style = AppTypography.body, modifier = Modifier.padding(bottom = Spacing.medium))

            currentItems.forEach { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.medium),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(item.productName, style = AppTypography.body, modifier = Modifier.weight(1f))
                        Text("Qty: ${item.quantity}", style = AppTypography.body, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.small))
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            AppPrimaryButton(
                text = if (isReceiving) "Receiving..." else "Receive All Items",
                onClick = {
                    isReceiving = true
                    scope.launch {
                        try {
                            // Receive all items from SENT status
                            val sentItems = currentOrder.getItemsForStatus(PurchaseOrderStatus.SENT)
                            
                            // Update stock for each item
                            sentItems.forEach { item ->
                                val productResult = inventoryRepository.getProductByIdSync(item.productId)
                                if (productResult.isSuccess) {
                                    val product = productResult.getOrNull()
                                    if (product != null) {
                                        val newStock = product.stockQuantity + item.quantity
                                        val updatedProduct = product.copy(
                                            stockQuantity = newStock,
                                            updatedAt = System.currentTimeMillis()
                                        )
                                        inventoryRepository.updateProduct(updatedProduct)
                                        
                                        val movement = StockMovement(
                                            productId = item.productId,
                                            type = MovementType.PURCHASE,
                                            quantity = item.quantity,
                                            previousStock = product.stockQuantity,
                                            newStock = newStock,
                                            reason = "Purchase Order: ${currentOrder.orderNumber}",
                                            purchaseOrderId = currentOrder.id
                                        )
                                        stockRepository.addMovement(movement)
                                    }
                                }
                            }
                            
                            // Update order to RECEIVED
                            val result = repository.updateStatus(currentOrder.id, PurchaseOrderStatus.RECEIVED)
                            if (result.isSuccess) {
                                Toast.makeText(context, "Items received!", Toast.LENGTH_SHORT).show()
                                isReceiving = false
                                onBack()
                            } else {
                                errorMessage = result.exceptionOrNull()?.message
                                isReceiving = false
                            }
                        } catch (e: Exception) {
                            errorMessage = e.message
                            isReceiving = false
                        }
                    }
                },
                isLoading = isReceiving,
                enabled = !isReceiving && currentItems.isNotEmpty()
            )

            Spacer(modifier = Modifier.height(Spacing.xxlarge))
        }
    }
}
