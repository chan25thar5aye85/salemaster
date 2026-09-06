package com.akari.retailer.features.inventory.presentation

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.akari.retailer.RetailApplication
import com.akari.retailer.core.ui.components.AppCard
import com.akari.retailer.core.ui.components.AppPrimaryButton
import com.akari.retailer.core.ui.components.AppScreen
import com.akari.retailer.core.ui.theme.AppTypography
import com.akari.retailer.core.ui.theme.Spacing
import com.akari.retailer.features.inventory.data.repository.FirestorePurchaseOrderRepository
import com.akari.retailer.features.inventory.domain.models.PurchaseOrder
import com.akari.retailer.features.inventory.domain.models.PurchaseOrderStatus
import com.akari.retailer.navigation.Routes
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun PurchaseOrderDetailScreen(
    navController: NavController,
    orderId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val repository = remember { FirestorePurchaseOrderRepository() }
    
    var order by remember { mutableStateOf<PurchaseOrder?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var isUpdating by remember { mutableStateOf(false) }
    
    var selectedStatusTab by remember { mutableStateOf(0) }
    
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    
    // All possible statuses in order
    val statusSteps = listOf(
        PurchaseOrderStatus.DRAFT,
        PurchaseOrderStatus.SENT,
        PurchaseOrderStatus.ACKNOWLEDGED,
        PurchaseOrderStatus.RECEIVED,
        PurchaseOrderStatus.INVOICED,
        PurchaseOrderStatus.CLOSED
    )
    
    // Load order from repository
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
        title = "Order Detail",
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
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Text(
                                text = "Loading order...",
                                style = AppTypography.body,
                                modifier = Modifier.padding(top = Spacing.medium)
                            )
                        }
                    }
                }
                
                error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("❌", fontSize = 48.sp)
                            Text(
                                text = error!!,
                                style = AppTypography.header,
                                modifier = Modifier.padding(top = Spacing.medium)
                            )
                            Text(
                                text = "Order ID: ${orderId.take(8)}",
                                style = AppTypography.small,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(Spacing.medium))
                            AppPrimaryButton(
                                text = "Go Back",
                                onClick = onBack
                            )
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
                            Text(
                                text = "Order not found",
                                style = AppTypography.header,
                                modifier = Modifier.padding(top = Spacing.medium)
                            )
                            Text(
                                text = "Order ID: ${orderId.take(8)}",
                                style = AppTypography.small,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(Spacing.medium))
                            AppPrimaryButton(
                                text = "Go Back",
                                onClick = onBack
                            )
                        }
                    }
                }
                
                else -> {
                    val currentOrder = order!!
                    
                    // ============ ORDER HEADER ============
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
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
                                    text = "📦 ${currentOrder.orderNumber}",
                                    style = AppTypography.header
                                )
                                StatusBadge(status = currentOrder.status)
                            }
                            Text(
                                text = "🏢 ${currentOrder.supplierName}",
                                style = AppTypography.body
                            )
                            Text(
                                text = "Created: ${dateFormat.format(currentOrder.orderDate)}",
                                style = AppTypography.small,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(Spacing.medium))
                    
                    // ============ TIMELINE TABS ============
                    Text(
                        text = "Order Progress",
                        style = AppTypography.title,
                        modifier = Modifier.padding(bottom = Spacing.small)
                    )
                    
                    // Status timeline with checkmarks
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Spacing.medium),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        statusSteps.forEachIndexed { index, status ->
                            val isCompleted = when (status) {
                                PurchaseOrderStatus.DRAFT -> currentOrder.orderDate > 0
                                PurchaseOrderStatus.SENT -> currentOrder.sentDate > 0
                                PurchaseOrderStatus.ACKNOWLEDGED -> currentOrder.acknowledgedDate > 0
                                PurchaseOrderStatus.RECEIVED -> currentOrder.receivedDate > 0
                                PurchaseOrderStatus.INVOICED -> currentOrder.invoicedDate > 0
                                PurchaseOrderStatus.CLOSED -> currentOrder.closedDate > 0
                            }
                            val isActive = currentOrder.status == status
                            
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                // Circle with background
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(
                                            color = when {
                                                isCompleted -> MaterialTheme.colorScheme.primary
                                                isActive -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isCompleted) {
                                        Text("✓", color = MaterialTheme.colorScheme.onPrimary)
                                    } else if (isActive) {
                                        Text("●", color = MaterialTheme.colorScheme.onPrimary)
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Text(
                                    text = when (status) {
                                        PurchaseOrderStatus.DRAFT -> "Draft"
                                        PurchaseOrderStatus.SENT -> "Sent"
                                        PurchaseOrderStatus.ACKNOWLEDGED -> "Ack"
                                        PurchaseOrderStatus.RECEIVED -> "Rec"
                                        PurchaseOrderStatus.INVOICED -> "Inv"
                                        PurchaseOrderStatus.CLOSED -> "Closed"
                                    },
                                    style = AppTypography.small,
                                    color = if (isCompleted || isActive) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                                
                                if (isCompleted) {
                                    Text(
                                        text = dateFormat.format(
                                            when (status) {
                                                PurchaseOrderStatus.DRAFT -> currentOrder.orderDate
                                                PurchaseOrderStatus.SENT -> currentOrder.sentDate
                                                PurchaseOrderStatus.ACKNOWLEDGED -> currentOrder.acknowledgedDate
                                                PurchaseOrderStatus.RECEIVED -> currentOrder.receivedDate
                                                PurchaseOrderStatus.INVOICED -> currentOrder.invoicedDate
                                                PurchaseOrderStatus.CLOSED -> currentOrder.closedDate
                                            }
                                        ),
                                        style = AppTypography.small,
                                        fontSize = 8.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(Spacing.medium))
                    
                    // ============ STATUS DETAIL TABS ============
                    // Tabs for each status step showing order at that stage
                    ScrollableTabRow(
                        selectedTabIndex = selectedStatusTab,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        edgePadding = 0.dp
                    ) {
                        statusSteps.forEachIndexed { index, status ->
                            val isCompleted = when (status) {
                                PurchaseOrderStatus.DRAFT -> currentOrder.orderDate > 0
                                PurchaseOrderStatus.SENT -> currentOrder.sentDate > 0
                                PurchaseOrderStatus.ACKNOWLEDGED -> currentOrder.acknowledgedDate > 0
                                PurchaseOrderStatus.RECEIVED -> currentOrder.receivedDate > 0
                                PurchaseOrderStatus.INVOICED -> currentOrder.invoicedDate > 0
                                PurchaseOrderStatus.CLOSED -> currentOrder.closedDate > 0
                            }
                            
                            Tab(
                                selected = selectedStatusTab == index,
                                onClick = { 
                                    if (isCompleted || status == currentOrder.status) {
                                        selectedStatusTab = index
                                    }
                                },
                                enabled = isCompleted || status == currentOrder.status,
                                text = {
                                    Text(
                                        when (status) {
                                            PurchaseOrderStatus.DRAFT -> "Draft"
                                            PurchaseOrderStatus.SENT -> "Sent"
                                            PurchaseOrderStatus.ACKNOWLEDGED -> "Ack"
                                            PurchaseOrderStatus.RECEIVED -> "Rec"
                                            PurchaseOrderStatus.INVOICED -> "Inv"
                                            PurchaseOrderStatus.CLOSED -> "Closed"
                                        },
                                        style = AppTypography.small
                                    )
                                }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(Spacing.medium))
                    
                    // ============ CONTENT FOR SELECTED TAB ============
                    val selectedStatus = statusSteps[selectedStatusTab]
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.medium)
                        ) {
                            Text(
                                text = "Status: ${selectedStatus.name}",
                                style = AppTypography.title,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            Spacer(modifier = Modifier.height(Spacing.small))
                            
                            // Show items at this status
                            Text(
                                text = "Items (${currentOrder.items.size})",
                                style = AppTypography.body
                            )
                            
                            currentOrder.items.forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = item.productName,
                                        style = AppTypography.body,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${item.quantity} × ${item.costPrice} = ${item.total}",
                                        style = AppTypography.body,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(Spacing.small))
                            
                            Divider()
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total", style = AppTypography.body)
                                Text(
                                    "${currentOrder.totalCost}",
                                    style = AppTypography.header,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(Spacing.medium))
                    
                    // ============ ACTION BUTTONS ============
                    when (currentOrder.status) {
                        PurchaseOrderStatus.DRAFT -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
                            ) {
                                AppPrimaryButton(
                                    text = "✏️ Edit",
                                    onClick = {
                                        navController.navigate(
                                            Routes.PURCHASE_ORDER_EDIT.replace("{orderId}", currentOrder.id)
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                AppPrimaryButton(
                                    text = "📤 Send",
                                    onClick = {
                                        scope.launch {
                                            isUpdating = true
                                            val result = repository.updateStatus(currentOrder.id, PurchaseOrderStatus.SENT)
                                            if (result.isSuccess) {
                                                Toast.makeText(context, "Order sent to supplier", Toast.LENGTH_SHORT).show()
                                                repository.getOrder(orderId).collect { updated -> order = updated }
                                            } else {
                                                Toast.makeText(context, "Failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                            }
                                            isUpdating = false
                                        }
                                    },
                                    isLoading = isUpdating,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        
                        PurchaseOrderStatus.SENT -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
                            ) {
                                AppPrimaryButton(
                                    text = "✅ Acknowledge",
                                    onClick = {
                                        scope.launch {
                                            isUpdating = true
                                            val result = repository.updateStatus(currentOrder.id, PurchaseOrderStatus.ACKNOWLEDGED)
                                            if (result.isSuccess) {
                                                Toast.makeText(context, "Order acknowledged", Toast.LENGTH_SHORT).show()
                                                repository.getOrder(orderId).collect { updated -> order = updated }
                                            } else {
                                                Toast.makeText(context, "Failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                            }
                                            isUpdating = false
                                        }
                                    },
                                    isLoading = isUpdating,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            isUpdating = true
                                            val result = repository.updateStatus(currentOrder.id, PurchaseOrderStatus.DRAFT)
                                            if (result.isSuccess) {
                                                Toast.makeText(context, "Order moved back to Draft", Toast.LENGTH_SHORT).show()
                                                repository.getOrder(orderId).collect { updated -> order = updated }
                                            } else {
                                                Toast.makeText(context, "Failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                            }
                                            isUpdating = false
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text("↩️ Back to Draft")
                                }
                            }
                        }
                        
                        PurchaseOrderStatus.ACKNOWLEDGED -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
                            ) {
                                AppPrimaryButton(
                                    text = "📦 Receive",
                                    onClick = {
                                        navController.navigate(Routes.RECEIVE_ORDER.replace("{orderId}", currentOrder.id))
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            isUpdating = true
                                            val result = repository.updateStatus(currentOrder.id, PurchaseOrderStatus.SENT)
                                            if (result.isSuccess) {
                                                Toast.makeText(context, "Order moved back to Sent", Toast.LENGTH_SHORT).show()
                                                repository.getOrder(orderId).collect { updated -> order = updated }
                                            } else {
                                                Toast.makeText(context, "Failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                            }
                                            isUpdating = false
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text("↩️ Back")
                                }
                            }
                        }
                        
                        PurchaseOrderStatus.RECEIVED -> {
                            if (!currentOrder.isFullyReceived()) {
                                AppPrimaryButton(
                                    text = "📦 Receive More",
                                    onClick = {
                                        navController.navigate(Routes.RECEIVE_ORDER.replace("{orderId}", currentOrder.id))
                                    }
                                )
                            } else {
                                AppPrimaryButton(
                                    text = "📄 Mark Invoiced",
                                    onClick = {
                                        scope.launch {
                                            isUpdating = true
                                            val result = repository.updateStatus(currentOrder.id, PurchaseOrderStatus.INVOICED)
                                            if (result.isSuccess) {
                                                Toast.makeText(context, "Order marked as Invoiced", Toast.LENGTH_SHORT).show()
                                                repository.getOrder(orderId).collect { updated -> order = updated }
                                            } else {
                                                Toast.makeText(context, "Failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                            }
                                            isUpdating = false
                                        }
                                    },
                                    isLoading = isUpdating
                                )
                            }
                        }
                        
                        PurchaseOrderStatus.INVOICED -> {
                            AppPrimaryButton(
                                text = "✅ Close Order",
                                onClick = {
                                    scope.launch {
                                        isUpdating = true
                                        val result = repository.updateStatus(currentOrder.id, PurchaseOrderStatus.CLOSED)
                                        if (result.isSuccess) {
                                            Toast.makeText(context, "Order closed", Toast.LENGTH_SHORT).show()
                                            repository.getOrder(orderId).collect { updated -> order = updated }
                                        } else {
                                            Toast.makeText(context, "Failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                        }
                                        isUpdating = false
                                    }
                                },
                                isLoading = isUpdating
                            )
                        }
                        
                        PurchaseOrderStatus.CLOSED -> {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(Spacing.medium),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("✅ Order Completed", style = AppTypography.header)
                                    Text(
                                        text = "Closed: ${dateFormat.format(currentOrder.closedDate)}",
                                        style = AppTypography.small,
                                        modifier = Modifier.padding(start = Spacing.medium)
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

@Composable
fun StatusBadge(status: PurchaseOrderStatus) {
    val (color, text) = when (status) {
        PurchaseOrderStatus.DRAFT -> 
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) to "Draft"
        PurchaseOrderStatus.SENT -> 
            MaterialTheme.colorScheme.primary to "Sent"
        PurchaseOrderStatus.ACKNOWLEDGED -> 
            MaterialTheme.colorScheme.tertiary to "Acknowledged"
        PurchaseOrderStatus.RECEIVED -> 
            MaterialTheme.colorScheme.primary to "Received"
        PurchaseOrderStatus.INVOICED -> 
            MaterialTheme.colorScheme.primary to "Invoiced"
        PurchaseOrderStatus.CLOSED -> 
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) to "Closed"
    }
    
    Card(
        modifier = Modifier.wrapContentWidth(),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.15f)
        )
    ) {
        Text(
            text = text,
            style = AppTypography.small,
            color = color,
            modifier = Modifier.padding(horizontal = Spacing.medium, vertical = Spacing.small)
        )
    }
}
