package com.akari.retailer.features.inventory.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akari.retailer.core.ui.components.AppPrimaryButton
import com.akari.retailer.core.ui.components.AppScreen
import com.akari.retailer.core.ui.theme.AppTypography
import com.akari.retailer.core.ui.theme.Spacing
import com.akari.retailer.features.inventory.domain.models.PurchaseOrder
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun PurchaseOrderReceiptScreen(
    order: PurchaseOrder,
    onBack: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    val items = order.receivedItems
    val total = items.sumOf { it.total }

    AppScreen(
        title = "🧾 Purchase Receipt",
        showBackButton = true,
        onBackClick = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Receipt Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.large)
                ) {
                    // Header
                    Text(
                        text = "🧾 PURCHASE RECEIPT",
                        style = AppTypography.header.copy(fontSize = 20.sp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = Spacing.small)
                    )
                    
                    Divider(
                        modifier = Modifier.padding(vertical = Spacing.medium),
                        thickness = 2.dp
                    )
                    
                    // Order Info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Order #",
                            style = AppTypography.body,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = order.orderNumber,
                            style = AppTypography.body
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(Spacing.small))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Supplier",
                            style = AppTypography.body,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = order.supplierName,
                            style = AppTypography.body
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(Spacing.small))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Date",
                            style = AppTypography.body,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = dateFormat.format(order.orderDate),
                            style = AppTypography.body
                        )
                    }
                    
                    if (order.receivedDate > 0) {
                        Spacer(modifier = Modifier.height(Spacing.small))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Received Date",
                                style = AppTypography.body,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = dateFormat.format(order.receivedDate),
                                style = AppTypography.body
                            )
                        }
                    }
                    
                    Divider(
                        modifier = Modifier.padding(vertical = Spacing.medium),
                        thickness = 2.dp
                    )
                    
                    // Items Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Item",
                            style = AppTypography.body,
                            modifier = Modifier.weight(2f)
                        )
                        Text(
                            text = "Qty",
                            style = AppTypography.body,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "Price",
                            style = AppTypography.body,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "Total",
                            style = AppTypography.body,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    Divider(modifier = Modifier.padding(vertical = Spacing.small))
                    
                    // Items
                    items.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = item.productName,
                                style = AppTypography.body,
                                modifier = Modifier.weight(2f)
                            )
                            Text(
                                text = "${item.quantity}",
                                style = AppTypography.body,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${item.costPrice}",
                                style = AppTypography.body,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${item.total}",
                                style = AppTypography.body,
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    Divider(
                        modifier = Modifier.padding(vertical = Spacing.medium),
                        thickness = 2.dp
                    )
                    
                    // Total
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "TOTAL",
                            style = AppTypography.header
                        )
                        Text(
                            text = "$total",
                            style = AppTypography.header,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    if (order.notes.isNotEmpty()) {
                        Divider(modifier = Modifier.padding(vertical = Spacing.medium))
                        Text(
                            text = "Notes:",
                            style = AppTypography.body,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = order.notes,
                            style = AppTypography.body
                        )
                    }
                    
                    Divider(
                        modifier = Modifier.padding(vertical = Spacing.medium),
                        thickness = 2.dp
                    )
                    
                    // Footer
                    Text(
                        text = "Thank you for your business!",
                        style = AppTypography.body,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Spacing.small),
                        fontSize = 14.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            // Action buttons
            AppPrimaryButton(
                text = "⬅️ Back",
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(Spacing.xxlarge))
        }
    }
}
