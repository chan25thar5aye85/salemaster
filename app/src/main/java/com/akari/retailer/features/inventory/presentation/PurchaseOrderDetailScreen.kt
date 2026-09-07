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
import androidx.navigation.NavController
import com.akari.retailer.RetailApplication
import com.akari.retailer.core.ui.components.AppPrimaryButton
import com.akari.retailer.core.ui.components.AppScreen
import com.akari.retailer.core.ui.theme.AppTypography
import com.akari.retailer.core.ui.theme.Spacing
import com.akari.retailer.features.inventory.data.repository.FirestorePurchaseOrderRepository
import com.akari.retailer.features.inventory.domain.models.PurchaseOrderStatus
import com.akari.retailer.navigation.Routes

@Composable
fun PurchaseOrderDetailScreen(
    navController: NavController,
    orderId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    
    val repository = remember { FirestorePurchaseOrderRepository() }
    
    var order by remember { mutableStateOf<com.akari.retailer.features.inventory.domain.models.PurchaseOrder?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    
    var selectedTab by remember { mutableStateOf(0) }
    
    val statusTabs = listOf(
        "DRAFT" to PurchaseOrderStatus.DRAFT,
        "SENT" to PurchaseOrderStatus.SENT,
        "RECEIVED" to PurchaseOrderStatus.RECEIVED
    )
    
    LaunchedEffect(orderId) {
        isLoading = true
        try {
            repository.getOrder(orderId).collect { loadedOrder ->
                order = loadedOrder
                isLoading = false
                if (loadedOrder == null) {
                    error = "Order not found"
                }
            }
        } catch (e: Exception) {
            isLoading = false
            error = e.message ?: "Failed to load order"
        }
    }

    AppScreen(
        title = order?.orderName ?: "Order Detail",
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
                
                error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("❌", fontSize = 48.sp)
                            Text(error!!, style = AppTypography.body, color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(Spacing.medium))
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
                            Spacer(modifier = Modifier.height(Spacing.medium))
                            AppPrimaryButton(text = "Go Back", onClick = onBack)
                        }
                    }
                }
                
                else -> {
                    val currentOrder = order!!
                    
                    // Status Filter Tabs
                    ScrollableTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        edgePadding = 0.dp
                    ) {
                        statusTabs.forEachIndexed { index, (label, status) ->
                            val items = currentOrder.getItemsForStatus(status)
                            val count = items.size
                            
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(label, style = AppTypography.label)
                                        if (count > 0) {
                                            Badge(
                                                containerColor = if (selectedTab == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                                contentColor = if (selectedTab == index) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                            ) {
                                                Text("$count", style = AppTypography.small)
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(Spacing.medium))
                    
                    // Show items for selected status
                    val selectedStatus = statusTabs[selectedTab].second
                    val currentItems = currentOrder.getItemsForStatus(selectedStatus)
                    val currentTotal = currentOrder.getTotalForStatus(selectedStatus)
                    
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.medium)
                        ) {
                            Text(
                                text = "Items (${selectedStatus.name})",
                                style = AppTypography.title,
                                modifier = Modifier.padding(bottom = Spacing.small)
                            )
                            
                            if (currentItems.isEmpty()) {
                                Text(
                                    text = "No items in ${selectedStatus.name}",
                                    style = AppTypography.body,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            } else {
                                currentItems.forEachIndexed { index, item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${index + 1}. ${item.productName}",
                                            style = AppTypography.body,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "${item.quantity} x ${item.costPrice} = ${item.total}",
                                            style = AppTypography.body,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                
                                Divider(
                                    modifier = Modifier.padding(vertical = Spacing.small)
                                )
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Total", style = AppTypography.title)
                                    Text(
                                        "$currentTotal",
                                        style = AppTypography.header,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(Spacing.xxlarge))
                }
            }
        }
    }
}
