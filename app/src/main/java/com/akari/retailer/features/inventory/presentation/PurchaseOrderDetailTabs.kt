package com.akari.retailer.features.inventory.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.akari.retailer.core.ui.theme.AppTypography
import com.akari.retailer.features.inventory.domain.models.PurchaseOrderStatus

@Composable
fun PurchaseOrderStatusTabs(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    order: com.akari.retailer.features.inventory.domain.models.PurchaseOrder
) {
    val statusTabs = listOf(
        "DRAFT" to PurchaseOrderStatus.DRAFT,
        "SENT" to PurchaseOrderStatus.SENT,
        "RECEIVED" to PurchaseOrderStatus.RECEIVED
    )
    
    ScrollableTabRow(
        selectedTabIndex = selectedTab,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        edgePadding = 0.dp
    ) {
        statusTabs.forEachIndexed { index, (label, status) ->
            val items = order.getItemsForStatus(status)
            val count = items.size
            
            Tab(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
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
}
