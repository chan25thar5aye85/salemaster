package com.akari.retailer.features.inventory.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akari.retailer.core.ui.theme.AppTypography
import com.akari.retailer.core.ui.theme.Spacing
import com.akari.retailer.features.inventory.domain.models.PurchaseOrderItem
import com.akari.retailer.features.inventory.domain.models.PurchaseOrderStatus

@Composable
fun PurchaseOrderItemsList(
    items: List<PurchaseOrderItem>,
    status: PurchaseOrderStatus,
    total: Int,
    onItemClick: (Int) -> Unit,
    onItemDelete: (Int) -> Unit,
    onAddClick: () -> Unit,
    onSendClick: (() -> Unit)? = null,
    isUpdating: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth()
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
                    text = "Items (${status.name})",
                    style = AppTypography.title
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Send button next to Add button (only for DRAFT)
                    if (status == PurchaseOrderStatus.DRAFT && onSendClick != null) {
                        TextButton(
                            onClick = onSendClick,
                            enabled = !isUpdating && items.isNotEmpty()
                        ) {
                            Text("📤 Send")
                        }
                    }
                    if (status == PurchaseOrderStatus.DRAFT) {
                        TextButton(onClick = onAddClick) {
                            Text("+ Add")
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.small))
            
            if (items.isEmpty()) {
                Text(
                    text = "No items in ${status.name}",
                    style = AppTypography.body,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                items.forEachIndexed { index, item ->
                    if (status == PurchaseOrderStatus.DRAFT) {
                        DraftItemRow(
                            index = index,
                            item = item,
                            onItemClick = { onItemClick(index) },
                            onItemDelete = { onItemDelete(index) }
                        )
                    } else {
                        ReadOnlyItemRow(index = index, item = item)
                    }
                }
                
                Divider(modifier = Modifier.padding(vertical = Spacing.small))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total", style = AppTypography.title)
                    Text(
                        "$total",
                        style = AppTypography.header,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun DraftItemRow(
    index: Int,
    item: PurchaseOrderItem,
    onItemClick: () -> Unit,
    onItemDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable { onItemClick() },
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "${index + 1}.",
                style = AppTypography.body,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Text(text = item.productName, style = AppTypography.body)
            Text(text = "✏️", style = AppTypography.small, fontSize = 10.sp)
        }
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${item.quantity} x ${item.costPrice} = ${item.total}",
                style = AppTypography.small,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary
            )
            TextButton(
                onClick = onItemDelete,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.size(width = 32.dp, height = 32.dp)
            ) {
                Text("✕", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun ReadOnlyItemRow(index: Int, item: PurchaseOrderItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
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
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
