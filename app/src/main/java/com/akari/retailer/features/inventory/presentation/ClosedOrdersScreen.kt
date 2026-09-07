package com.akari.retailer.features.inventory.presentation

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.akari.retailer.features.inventory.domain.models.PurchaseOrderStatus

@Composable
fun ClosedOrdersScreen(
    navController: NavController,
    onBack: () -> Unit
) {
    OrderListScreen(
        navController = navController,
        status = PurchaseOrderStatus.CLOSED,
        title = "Closed Orders",
        onBack = onBack,
        showAddButton = false
    )
}
