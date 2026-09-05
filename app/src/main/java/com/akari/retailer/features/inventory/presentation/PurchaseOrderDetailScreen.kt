package com.akari.retailer.features.inventory.presentation

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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akari.retailer.RetailApplication
import com.akari.retailer.core.ui.components.AppCard
import com.akari.retailer.core.ui.components.AppPrimaryButton
import com.akari.retailer.core.ui.components.AppScreen
import com.akari.retailer.core.ui.theme.AppTypography
import com.akari.retailer.core.ui.theme.Spacing
import com.akari.retailer.features.inventory.data.repository.FirestoreInventoryRepository
import com.akari.retailer.features.inventory.data.remote.FirestoreInventoryService
import com.akari.retailer.features.inventory.domain.models.PurchaseOrderStatus
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun PurchaseOrderDetailScreen(
    orderId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as RetailApplication
    
    val sharedViewModel: PurchaseOrderSharedViewModel = viewModel()
    val inventoryService = remember { FirestoreInventoryService() }
    val inventoryRepository = remember { FirestoreInventoryRepository(inventoryService) }
    
    var productNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    val order = sharedViewModel.getOrderById(orderId)
    
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    
    // Load product names
    LaunchedEffect(Unit) {
        inventoryRepository.getProducts().collect { products ->
            productNames = products.associate { it.id to it.name }
        }
    }

    AppScreen(
        title = "Order Detail",
        showBackButton = true,
        onBackClick = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            if (order == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📭", fontSize = 48.sp)
                        Text("Order not found", style = AppTypography.header, modifier = Modifier.padding(top = Spacing.medium))
                        Text("Order ID: ${orderId.take(8)}", style = AppTypography.small, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.height(Spacing.medium))
                        AppPrimaryButton(text = "Go Back", onClick = onBack)
                    }
                }
                return@Column
            }
            
            val statusText = when (order.status) {
                PurchaseOrderStatus.DRAFT -> "Draft"
                PurchaseOrderStatus.SENT -> "Sent"
                PurchaseOrderStatus.ACKNOWLEDGED -> "Acknowledged"
                PurchaseOrderStatus.RECEIVED -> "Received"
                PurchaseOrderStatus.INVOICED -> "Invoiced"
                PurchaseOrderStatus.CLOSED -> "Closed"
                else -> "Unknown"
            }
            
            // Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(Spacing.medium),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Status", style = AppTypography.small, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text(statusText, style = AppTypography.header, color = MaterialTheme.colorScheme.primary)
                    }
                    Text("📦", fontSize = 24.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            // Order Info
            AppCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Order Details", style = AppTypography.title, modifier = Modifier.padding(bottom = Spacing.medium))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Order ID", style = AppTypography.body, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text(order.id.take(8), style = AppTypography.body)
                    }
                    Spacer(modifier = Modifier.height(Spacing.small))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Items", style = AppTypography.body, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text("${order.items.size}", style = AppTypography.body)
                    }
                    Spacer(modifier = Modifier.height(Spacing.small))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Items", style = AppTypography.body, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text("${order.items.sumOf { it.quantity }}", style = AppTypography.body)
                    }
                    Spacer(modifier = Modifier.height(Spacing.small))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Cost", style = AppTypography.body, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text("${order.totalCost}", style = AppTypography.header, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.height(Spacing.small))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Order Date", style = AppTypography.body, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text(dateFormat.format(order.orderedAt), style = AppTypography.body)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            // Items List
            AppCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Items", style = AppTypography.title, modifier = Modifier.padding(bottom = Spacing.medium))
                    
                    order.items.forEach { item ->
                        val productName = productNames[item.productId] ?: "Unknown Product"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = Spacing.small),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = productName,
                                style = AppTypography.body,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${item.quantity} × ${item.costPrice} = ${item.total}",
                                style = AppTypography.body,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
            
            if (order.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.medium))
                AppCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Notes", style = AppTypography.title, modifier = Modifier.padding(bottom = Spacing.medium))
                        Text(order.notes, style = AppTypography.body)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.xxlarge))
            
            AppPrimaryButton(
                text = "Go Back",
                onClick = onBack
            )
        }
    }
}
