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
    selectedIndices: Set<Int> = emptySet(),
    isEditable: Boolean = true,
    onItemSelect: (Int) -> Unit = {},
    onItemClick: (Int) -> Unit,
    onItemDelete: (Int) -> Unit = {},
    onAddClick: () -> Unit = {},
    onReceiveClick: (() -> Unit)? = null,
    isUpdating: Boolean = false,
    onSelectAll: () -> Unit = {},
    showReceiveButton: Boolean = true,
    showAddButton: Boolean = true,
    showDeleteButton: Boolean = true,
    isReadOnly: Boolean = false
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
                val statusLabel = when (status) {
                    PurchaseOrderStatus.ORDER -> "ORDER"
                    PurchaseOrderStatus.RECEIVED -> "RECEIVED"
                    PurchaseOrderStatus.COMPLETED -> "COMPLETED"
                }
                Text(
                    text = "Items ($statusLabel)",
                    style = AppTypography.title
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showReceiveButton && onReceiveClick != null && !isReadOnly) {
                        val selectedCount = selectedIndices.size
                        TextButton(
                            onClick = onReceiveClick,
                            enabled = !isUpdating && selectedCount > 0
                        ) {
                            Text(if (selectedCount > 0) "📦 Receive ($selectedCount)" else "📦 Receive")
                        }
                    }
                    if (showAddButton && !isReadOnly) {
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
                // Select All / Deselect All row (only for ORDER and not read-only)
                if (showReceiveButton && !isReadOnly) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val selectedCount = selectedIndices.size
                        Text(
                            text = if (selectedCount == items.size) "All selected" else "$selectedCount/${items.size} selected",
                            style = AppTypography.small,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        TextButton(onClick = onSelectAll) {
                            Text(if (selectedCount == items.size) "Deselect All" else "Select All")
                        }
                    }
                    Spacer(modifier = Modifier.height(Spacing.small))
                }
                
                val isReceivedTab = status == PurchaseOrderStatus.RECEIVED
                
                items.forEachIndexed { index, item ->
                    if (showDeleteButton && !isReadOnly && !isReceivedTab) {
                        // ORDER items - full editable with checkbox and delete
                        OrderItemRow(
                            index = index,
                            item = item,
                            isSelected = selectedIndices.contains(index),
                            onItemSelect = { onItemSelect(index) },
                            onItemClick = { onItemClick(index) },
                            onItemDelete = { onItemDelete(index) }
                        )
                    } else {
                        // RECEIVED or COMPLETED items - no checkbox, no delete, read-only
                        ReadOnlyItemRow(
                            index = index,
                            item = item
                        )
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
fun OrderItemRow(
    index: Int,
    item: PurchaseOrderItem,
    isSelected: Boolean,
    onItemSelect: () -> Unit,
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
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { _ ->
                    onItemSelect()
                },
                modifier = Modifier.size(20.dp)
            )
            
            Row(
                modifier = Modifier.clickable { onItemClick() },
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
fun ReadOnlyItemRow(
    index: Int,
    item: PurchaseOrderItem
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${index + 1}. ${item.productName}",
            style = AppTypography.body,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${item.quantity} x ${item.costPrice} = ${item.total}",
            style = AppTypography.body,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
