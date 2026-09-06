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
fun PurchaseOrderCardKanban(
    order: PurchaseOrder,
    navController: NavController,
    onStatusChange: (PurchaseOrderStatus) -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    
    val statusConfig = when (order.status) {
        PurchaseOrderStatus.DRAFT -> Triple(Color(0xFFFF9800), "🟡 Draft", "Created: ${dateFormat.format(order.orderDate)}")
        PurchaseOrderStatus.SENT -> Triple(Color(0xFF2196F3), "🔵 Sent", "Sent: ${dateFormat.format(order.sentDate)}")
        PurchaseOrderStatus.ACKNOWLEDGED -> Triple(Color(0xFF9C27B0), "🟣 Acknowledged", "Ack: ${dateFormat.format(order.acknowledgedDate)}")
        PurchaseOrderStatus.RECEIVED -> Triple(Color(0xFF4CAF50), "🟢 Received", "Received: ${dateFormat.format(order.receivedDate)}")
        PurchaseOrderStatus.INVOICED -> Triple(Color(0xFFFF5722), "🟠 Invoiced", "Invoiced: ${dateFormat.format(order.invoicedDate)}")
        PurchaseOrderStatus.CLOSED -> Triple(Color(0xFF78909C), "⚪ Closed", "Closed: ${dateFormat.format(order.closedDate)}")
    }
    
    val (statusColor, statusIcon, statusDate) = statusConfig
    
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
            // Header: Order Number + Supplier
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📦 ${order.orderNumber}",
                    style = AppTypography.title
                )
                // Status badge
                Card(
                    modifier = Modifier.wrapContentWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = statusColor.copy(alpha = 0.15f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = Spacing.medium, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(statusIcon, style = AppTypography.small)
                        Text(
                            text = order.status.name,
                            style = AppTypography.small,
                            color = statusColor
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.small))
            
            // Supplier
            Text(
                text = "🏢 ${order.supplierName}",
                style = AppTypography.body,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            
            // Items and total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "📋 ${order.items.size} items",
                    style = AppTypography.body,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = "💰 ${order.totalCost}",
                    style = AppTypography.body,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(Spacing.small))
            
            // Status date
            Text(
                text = statusDate,
                style = AppTypography.small,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            
            // Progress bar for RECEIVED orders
            if (order.status == PurchaseOrderStatus.RECEIVED) {
                Spacer(modifier = Modifier.height(Spacing.small))
                val progress = order.getReceivedProgress()
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Received: ${order.receivedCost} / ${order.totalCost}",
                        style = AppTypography.small,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    if (order.isFullyReceived()) {
                        Text(
                            text = "✅ Complete",
                            style = AppTypography.small,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.small))
            
            // Quick action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (order.status) {
                    PurchaseOrderStatus.DRAFT -> {
                        // Edit button - navigate to edit screen
                        OutlinedButton(
                            onClick = {
                                navController.navigate(
                                    Routes.PURCHASE_ORDER_EDIT.replace("{orderId}", order.id)
                                )
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("✏️ Edit", style = AppTypography.small)
                        }
                        // Send button
                        Button(
                            onClick = { onStatusChange(PurchaseOrderStatus.SENT) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("📤 Send", style = AppTypography.small)
                        }
                    }
                    
                    PurchaseOrderStatus.SENT -> {
                        // Acknowledge button
                        Button(
                            onClick = { onStatusChange(PurchaseOrderStatus.ACKNOWLEDGED) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("✅ Ack", style = AppTypography.small)
                        }
                        // Back to Draft (optional)
                        OutlinedButton(
                            onClick = { onStatusChange(PurchaseOrderStatus.DRAFT) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("↩️ Back", style = AppTypography.small)
                        }
                    }
                    
                    PurchaseOrderStatus.ACKNOWLEDGED -> {
                        // Receive button
                        Button(
                            onClick = {
                                navController.navigate("receive_order/${order.id}")
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("📦 Receive", style = AppTypography.small)
                        }
                        // Back to Sent
                        OutlinedButton(
                            onClick = { onStatusChange(PurchaseOrderStatus.SENT) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("↩️ Back", style = AppTypography.small)
                        }
                    }
                    
                    PurchaseOrderStatus.RECEIVED -> {
                        if (!order.isFullyReceived()) {
                            Button(
                                onClick = {
                                    navController.navigate("receive_order/${order.id}")
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("📦 Receive More", style = AppTypography.small)
                            }
                        } else {
                            Button(
                                onClick = { onStatusChange(PurchaseOrderStatus.INVOICED) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("📄 Invoice", style = AppTypography.small)
                            }
                        }
                    }
                    
                    PurchaseOrderStatus.INVOICED -> {
                        Button(
                            onClick = { onStatusChange(PurchaseOrderStatus.CLOSED) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("✅ Close", style = AppTypography.small)
                        }
                    }
                    
                    PurchaseOrderStatus.CLOSED -> {
                        Text(
                            text = "✅ Completed",
                            style = AppTypography.small,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
