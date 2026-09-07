package com.akari.retailer.features.inventory.presentation

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.akari.retailer.features.inventory.domain.models.PurchaseOrderStatus

@Composable
fun ReceivedOrdersScreen(
    navController: NavController,
    onBack: () -> Unit
) {
    OrderListScreen(
        navController = navController,
        status = PurchaseOrderStatus.RECEIVED,
        title = "Received Orders",
        onBack = onBack,
        showAddButton = false
    )
}
