package com.akari.retailer.features.inventory.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.akari.retailer.R
import com.akari.retailer.core.ui.theme.AppTypography
import com.akari.retailer.core.ui.theme.Spacing
import com.akari.retailer.features.inventory.domain.models.PurchaseOrder
import com.akari.retailer.features.inventory.domain.models.PurchaseOrderStatus
import com.akari.retailer.navigation.Routes
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun PurchaseOrderCardCompact(
    order: PurchaseOrder,
    navController: NavController,
    productName: String = "Product"
) {
    val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
    
    val statusColor = when (order.status) {
        PurchaseOrderStatus.DRAFT -> Color(0xFFFF9800)
        PurchaseOrderStatus.SENT -> Color(0xFF4CAF50)
        PurchaseOrderStatus.ACKNOWLEDGED -> Color(0xFF2196F3)
        PurchaseOrderStatus.RECEIVED -> Color(0xFF9C27B0)
        PurchaseOrderStatus.INVOICED -> Color(0xFFFF5722)
        PurchaseOrderStatus.CLOSED -> Color(0xFF78909C)
    }
    
    val statusText = when (order.status) {
        PurchaseOrderStatus.DRAFT -> "Draft"
        PurchaseOrderStatus.SENT -> "Sent"
        PurchaseOrderStatus.ACKNOWLEDGED -> "Acknowledged"
        PurchaseOrderStatus.RECEIVED -> "Received"
        PurchaseOrderStatus.INVOICED -> "Invoiced"
        PurchaseOrderStatus.CLOSED -> "Closed"
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { 
                navController.navigate(
                    Routes.PURCHASE_ORDER_DETAIL.replace("{orderId}", order.id)
                )
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📦",
                        style = AppTypography.title
                    )
                    Text(
                        text = productName,
                        style = AppTypography.title
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${order.items.size} items",
                        style = AppTypography.small,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "${stringResource(R.string.total)}: ${order.totalCost}",
                        style = AppTypography.small,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = dateFormat.format(order.orderedAt),
                        style = AppTypography.small,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            
            // Status Badge
            Card(
                modifier = Modifier
                    .wrapContentWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = statusColor.copy(alpha = 0.15f)
                )
            ) {
                Text(
                    text = statusText,
                    style = AppTypography.small,
                    color = statusColor,
                    modifier = Modifier.padding(
                        horizontal = Spacing.medium,
                        vertical = 4.dp
                    )
                )
            }
        }
    }
}
