package com.akari.retailer.features.inventory.presentation

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.akari.retailer.R
import com.akari.retailer.features.inventory.domain.models.PurchaseOrderStatus

@Composable
fun DraftOrdersScreen(
    navController: NavController,
    onBack: () -> Unit
) {
    OrderListScreen(
        navController = navController,
        status = PurchaseOrderStatus.DRAFT,
        title = "Draft Orders",
        onBack = onBack,
        showAddButton = true
    )
}
