package com.akari.retailer.features.inventory.presentation

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.akari.retailer.features.inventory.domain.models.PurchaseOrderStatus

@Composable
fun SentOrdersScreen(
    navController: NavController,
    onBack: () -> Unit
) {
    OrderListScreen(
        navController = navController,
        status = PurchaseOrderStatus.SENT,
        title = "Sent Orders",
        onBack = onBack,
        showAddButton = false
    )
}
