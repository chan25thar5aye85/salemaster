package com.akari.retailer.features.inventory.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.akari.retailer.core.ui.components.AppPrimaryButton
import com.akari.retailer.core.ui.theme.Spacing
import com.akari.retailer.features.inventory.domain.models.PurchaseOrderStatus

@Composable
fun PurchaseOrderActions(
    status: PurchaseOrderStatus,
    isUpdating: Boolean,
    onSend: () -> Unit,
    onDelete: () -> Unit,
    onReceive: () -> Unit,
    onBackToDraft: () -> Unit,
    onBackToSent: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium)
        ) {
            when (status) {
                PurchaseOrderStatus.DRAFT -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
                    ) {
                        AppPrimaryButton(
                            text = "📤 Send",
                            onClick = onSend,
                            isLoading = isUpdating,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(
                            onClick = onDelete,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("🗑️ Delete")
                        }
                    }
                }
                
                PurchaseOrderStatus.SENT -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
                    ) {
                        AppPrimaryButton(
                            text = "📦 Receive",
                            onClick = onReceive,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(
                            onClick = onBackToDraft,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("↩️ Back to Draft")
                        }
                    }
                }
                
                PurchaseOrderStatus.RECEIVED -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "✅ Order Received",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
