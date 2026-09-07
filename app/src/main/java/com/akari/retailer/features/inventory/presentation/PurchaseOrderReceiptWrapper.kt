package com.akari.retailer.features.inventory.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.akari.retailer.features.inventory.data.repository.FirestorePurchaseOrderRepository

@Composable
fun PurchaseOrderReceiptWrapper(
    orderId: String,
    onBack: () -> Unit
) {
    val repository = remember { FirestorePurchaseOrderRepository() }
    var order by remember { mutableStateOf<com.akari.retailer.features.inventory.domain.models.PurchaseOrder?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(orderId) {
        repository.getOrder(orderId).collect { loadedOrder ->
            order = loadedOrder
            isLoading = false
        }
    }
    
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (order != null) {
        PurchaseOrderReceiptScreen(
            order = order!!,
            onBack = onBack
        )
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Order not found")
        }
    }
}
