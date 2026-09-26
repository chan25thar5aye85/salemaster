package com.akari.retailer.features.inventory.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akari.retailer.R
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
            // Compact header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val statusLabel = when (status) {
                    PurchaseOrderStatus.ORDER -> stringResource(R.string.order_status_order)
                    PurchaseOrderStatus.RECEIVED -> stringResource(R.string.order_status_received)
                    PurchaseOrderStatus.COMPLETED -> stringResource(R.string.order_status_completed)
                }
                Text(
                    text = "${stringResource(R.string.order_items)} ($statusLabel)",
                    style = AppTypography.title,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showReceiveButton && onReceiveClick != null && !isReadOnly) {
                        val selectedCount = selectedIndices.size
                        TextButton(
                            onClick = onReceiveClick,
                            enabled = !isUpdating && selectedCount > 0,
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(
                                if (selectedCount > 0) "📦 ${stringResource(R.string.receive_items)} ($selectedCount)" 
                                else "📦 ${stringResource(R.string.receive_items)}",
                                fontSize = 13.sp
                            )
                        }
                    }
                    if (showAddButton && !isReadOnly) {
                        TextButton(
                            onClick = onAddClick,
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("+ ${stringResource(R.string.add_items)}", fontSize = 13.sp)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            if (items.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_items_in_order),
                    style = AppTypography.body,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                // Select All / Deselect All row - compact
                if (showReceiveButton && !isReadOnly) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val selectedCount = selectedIndices.size
                        Text(
                            text = if (selectedCount == items.size) stringResource(R.string.all_selected) else "$selectedCount/${items.size} ${stringResource(R.string.items_selected)}",
                            style = AppTypography.small,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        TextButton(
                            onClick = onSelectAll,
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(
                                if (selectedCount == items.size) stringResource(R.string.deselect_all) else stringResource(R.string.select_all),
                                fontSize = 12.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
                
                val isReceivedTab = status == PurchaseOrderStatus.RECEIVED
                val isCompletedTab = status == PurchaseOrderStatus.COMPLETED
                
                items.forEachIndexed { index, item ->
                    if (!isReadOnly && !isCompletedTab && !isReceivedTab) {
                        // ORDER items - editable with checkbox and delete
                        OrderItemRow(
                            index = index,
                            item = item,
                            isSelected = selectedIndices.contains(index),
                            onItemSelect = { onItemSelect(index) },
                            onItemClick = { onItemClick(index) },
                            onItemDelete = { onItemDelete(index) }
                        )
                    } else {
                        // RECEIVED and COMPLETED items - read-only, no checkbox
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
                    Text(stringResource(R.string.order_total), style = AppTypography.title)
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
                modifier = Modifier.size(18.dp)
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
                modifier = Modifier.size(width = 28.dp, height = 28.dp)
            ) {
                Text("✕", fontSize = 11.sp)
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
