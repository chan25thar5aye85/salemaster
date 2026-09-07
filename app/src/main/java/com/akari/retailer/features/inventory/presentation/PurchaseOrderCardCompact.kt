package com.akari.retailer.features.inventory.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
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
    orderName: String = "Order"
) {
    val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
    
    val statusColor = when (order.status) {
        PurchaseOrderStatus.DRAFT -> Color(0xFFFF9800)
        PurchaseOrderStatus.SENT -> Color(0xFF2196F3)
        PurchaseOrderStatus.RECEIVED -> Color(0xFF4CAF50)
    }
    
    val statusText = when (order.status) {
        PurchaseOrderStatus.DRAFT -> "Draft"
        PurchaseOrderStatus.SENT -> "Sent"
        PurchaseOrderStatus.RECEIVED -> "Received"
    }
    
    // Get items and total for current status
    val currentItems = order.getItemsForStatus(order.status)
    val currentTotal = order.getTotalForStatus(order.status)
    
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📦 $orderName",
                    style = AppTypography.title
                )
                Card(
                    modifier = Modifier.wrapContentWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = statusColor.copy(alpha = 0.15f)
                    )
                ) {
                    Text(
                        text = statusText,
                        style = AppTypography.small,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = Spacing.medium, vertical = Spacing.small)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.small))
            
            Text(
                text = "🔢 ${order.orderNumber}",
                style = AppTypography.body,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            
            if (order.supplierName.isNotEmpty()) {
                Text(
                    text = "🏢 ${order.supplierName}",
                    style = AppTypography.body,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.small),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "📋 ${currentItems.size} items",
                    style = AppTypography.small,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = "💰 $currentTotal",
                    style = AppTypography.small,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            }
            
            Text(
                text = "📅 ${dateFormat.format(order.orderDate)}",
                style = AppTypography.small,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.padding(top = Spacing.small)
            )
        }
    }
}
