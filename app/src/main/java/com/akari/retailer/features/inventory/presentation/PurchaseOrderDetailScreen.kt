package com.akari.retailer.features.inventory.presentation

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.akari.retailer.R
import com.akari.retailer.RetailApplication
import com.akari.retailer.core.ui.components.AppPrimaryButton
import com.akari.retailer.core.ui.components.AppScreen
import com.akari.retailer.core.ui.theme.AppTypography
import com.akari.retailer.core.ui.theme.Spacing
import com.akari.retailer.features.inventory.data.repository.FirestorePurchaseOrderRepository
import com.akari.retailer.features.inventory.data.repository.FirestoreInventoryRepository
import com.akari.retailer.features.inventory.data.remote.FirestoreInventoryService
import com.akari.retailer.features.inventory.domain.models.PurchaseOrderItem
import com.akari.retailer.features.inventory.domain.models.PurchaseOrderStatus
import com.akari.retailer.navigation.Routes
import kotlinx.coroutines.launch

@Composable
fun PurchaseOrderDetailScreen(
    navController: NavController,
    orderId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val repository = remember { FirestorePurchaseOrderRepository() }
    val inventoryService = remember { FirestoreInventoryService() }
    val inventoryRepository = remember { FirestoreInventoryRepository(inventoryService) }
    
    val viewModel: PurchaseOrderDetailViewModel = viewModel(
        factory = PurchaseOrderDetailViewModelFactory(repository)
    )
    
    val state by viewModel.state.collectAsState()
    
    var selectedTab by remember { mutableStateOf(0) }
    var products by remember { mutableStateOf<List<com.akari.retailer.features.inventory.domain.models.Product>>(emptyList()) }
    
    var selectedIndices by remember { mutableStateOf(setOf<Int>()) }
    
    var showEditDialog by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableStateOf(-1) }
    var editQuantity by remember { mutableStateOf("") }
    var editPrice by remember { mutableStateOf("") }
    var editProductName by remember { mutableStateOf("") }
    var editingStatus by remember { mutableStateOf<PurchaseOrderStatus?>(null) }
    
    var showAddDialog by remember { mutableStateOf(false) }
    
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var deleteIndex by remember { mutableStateOf(-1) }
    var deleteStatus by remember { mutableStateOf<PurchaseOrderStatus?>(null) }
    
    var showCreatePurchaseDialog by remember { mutableStateOf(false) }
    var isCreatingPurchase by remember { mutableStateOf(false) }
    
    // Load products
    LaunchedEffect(Unit) {
        inventoryRepository.getProducts().collect { productList ->
            products = productList
        }
    }
    
    // Load order
    LaunchedEffect(orderId) {
        viewModel.loadOrder(orderId)
    }
    
    // When order loads, reset selection
    LaunchedEffect(state.editableOrderItems) {
        selectedIndices = emptySet()
    }

    AppScreen(
        title = state.order?.orderName ?: stringResource(R.string.order_detail),
        showBackButton = true,
        onBackClick = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                
                state.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("❌", fontSize = 48.sp)
                            Text(state.error!!, style = AppTypography.body, color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(Spacing.medium))
                            AppPrimaryButton(text = stringResource(R.string.back), onClick = onBack)
                        }
                    }
                }
                
                state.order == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📭", fontSize = 48.sp)
                            Text(stringResource(R.string.order_not_found), style = AppTypography.header)
                            Spacer(modifier = Modifier.height(Spacing.medium))
                            AppPrimaryButton(text = stringResource(R.string.back), onClick = onBack)
                        }
                    }
                }
                
                else -> {
                    val currentOrder = state.order!!
                    val isCompleted = currentOrder.status == PurchaseOrderStatus.COMPLETED
                    
                    // Always show all 3 tabs
                    val statusTabs = listOf(
                        stringResource(R.string.order_status_order) to PurchaseOrderStatus.ORDER,
                        stringResource(R.string.order_status_received) to PurchaseOrderStatus.RECEIVED,
                        stringResource(R.string.order_status_completed) to PurchaseOrderStatus.COMPLETED
                    )
                    
                    // Make sure selectedTab is valid
                    if (selectedTab >= statusTabs.size) {
                        selectedTab = 0
                    }
                    
                    val selectedStatus = statusTabs[selectedTab].second
                    val isReceived = selectedStatus == PurchaseOrderStatus.RECEIVED
                    val isOrder = selectedStatus == PurchaseOrderStatus.ORDER
                    val isCompletedTab = selectedStatus == PurchaseOrderStatus.COMPLETED
                    
                    val displayItems = when (selectedStatus) {
                        PurchaseOrderStatus.ORDER -> state.editableOrderItems
                        PurchaseOrderStatus.RECEIVED -> currentOrder.receivedItems
                        PurchaseOrderStatus.COMPLETED -> currentOrder.receivedItems
                    }
                    
                    val displayTotal = when (selectedStatus) {
                        PurchaseOrderStatus.ORDER -> state.editableOrderItems.sumOf { it.total }
                        PurchaseOrderStatus.RECEIVED -> currentOrder.receivedItems.sumOf { it.total }
                        PurchaseOrderStatus.COMPLETED -> currentOrder.receivedItems.sumOf { it.total }
                    }
                    
                    // Only allow editing if NOT completed
                    val allowEditing = !isCompleted
                    
                    PurchaseOrderStatusTabs(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it },
                        order = currentOrder,
                        statusTabs = statusTabs
                    )
                    
                    Spacer(modifier = Modifier.height(Spacing.small))
                    
                    // RECEIVED tab - Show Purchase button only if not completed
                    if (isReceived && !isCompleted) {
                        AppPrimaryButton(
                            text = if (isCreatingPurchase) stringResource(R.string.saving) else stringResource(R.string.create_purchase),
                            onClick = {
                                showCreatePurchaseDialog = true
                            },
                            isLoading = isCreatingPurchase,
                            enabled = !isCreatingPurchase,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(Spacing.medium))
                    }
                    
                    // COMPLETED tab - Show completion badge
                    if (isCompletedTab) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Text(
                                text = "✅ ${stringResource(R.string.purchase_completed)}",
                                style = AppTypography.header,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.medium)
                            )
                        }
                        Spacer(modifier = Modifier.height(Spacing.medium))
                    }
                    
                    // Items List
                    PurchaseOrderItemsList(
                        items = displayItems,
                        status = selectedStatus,
                        total = displayTotal,
                        selectedIndices = selectedIndices,
                        onItemSelect = { index ->
                            if (allowEditing && isOrder) {
                                if (selectedIndices.contains(index)) {
                                    selectedIndices = selectedIndices - index
                                } else {
                                    selectedIndices = selectedIndices + index
                                }
                            }
                        },
                        onItemClick = { index ->
                            if (allowEditing) {
                                val item = when (selectedStatus) {
                                    PurchaseOrderStatus.ORDER -> state.editableOrderItems[index]
                                    PurchaseOrderStatus.RECEIVED -> currentOrder.receivedItems[index]
                                    PurchaseOrderStatus.COMPLETED -> currentOrder.receivedItems[index]
                                    else -> null
                                }
                                if (item != null) {
                                    editingIndex = index
                                    editQuantity = item.quantity.toString()
                                    editPrice = item.costPrice.toString()
                                    editProductName = item.productName
                                    editingStatus = selectedStatus
                                    showEditDialog = true
                                }
                            }
                        },
                        onItemDelete = { index ->
                            if (allowEditing) {
                                deleteIndex = index
                                deleteStatus = selectedStatus
                                showDeleteConfirmation = true
                            }
                        },
                        onAddClick = { 
                            if (allowEditing && isOrder) {
                                showAddDialog = true
                            }
                        },
                        onReceiveClick = {
                            if (allowEditing && isOrder && selectedIndices.isNotEmpty()) {
                                val selectedItems = selectedIndices.map { state.editableOrderItems[it] }
                                viewModel.receiveSelectedItems(orderId, selectedItems)
                                selectedIndices = emptySet()
                            }
                        },
                        isUpdating = state.isUpdating,
                        onSelectAll = {
                            if (allowEditing && isOrder) {
                                if (selectedIndices.size == displayItems.size) {
                                    selectedIndices = emptySet()
                                } else {
                                    selectedIndices = displayItems.indices.toSet()
                                }
                            }
                        },
                        showReceiveButton = allowEditing && isOrder,
                        showAddButton = allowEditing && isOrder,
                        showDeleteButton = allowEditing,
                        isReadOnly = isCompleted
                    )
                    
                    Spacer(modifier = Modifier.height(Spacing.xxlarge))
                }
            }
        }
    }
    
    // Create Purchase Confirmation Dialog
    if (showCreatePurchaseDialog) {
        val finalizePurchase = stringResource(R.string.finalize_purchase)
        val createPurchaseWarning = stringResource(R.string.create_purchase_warning)
        val purchaseWillUpdate = stringResource(R.string.purchase_will_update)
        val purchaseUpdateStock = stringResource(R.string.purchase_update_stock)
        val purchaseCreateExpense = stringResource(R.string.purchase_create_expense)
        val purchaseUpdateSupplier = stringResource(R.string.purchase_update_supplier)
        val purchaseGenerateReceipt = stringResource(R.string.purchase_generate_receipt)
        val purchaseCannotUndo = stringResource(R.string.purchase_cannot_undo)
        val yesCreatePurchase = stringResource(R.string.yes_create_purchase)
        val purchaseCreatedSuccess = stringResource(R.string.purchase_created_success)
        val purchaseFailed = stringResource(R.string.purchase_failed)
        val cancelText = stringResource(R.string.cancel)
        
        AlertDialog(
            onDismissRequest = { 
                if (!isCreatingPurchase) {
                    showCreatePurchaseDialog = false 
                }
            },
            title = { 
                Text(
                    "⚠️ $finalizePurchase",
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Column {
                    Text(createPurchaseWarning, style = AppTypography.body)
                    Spacer(modifier = Modifier.height(Spacing.small))
                    Text(purchaseWillUpdate, style = AppTypography.body)
                    Spacer(modifier = Modifier.height(Spacing.small))
                    Text(purchaseUpdateStock, style = AppTypography.small)
                    Text(purchaseCreateExpense, style = AppTypography.small)
                    Text(purchaseUpdateSupplier, style = AppTypography.small)
                    Text(purchaseGenerateReceipt, style = AppTypography.small)
                    Spacer(modifier = Modifier.height(Spacing.small))
                    Text(
                        purchaseCannotUndo,
                        style = AppTypography.body,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isCreatingPurchase = true
                        showCreatePurchaseDialog = false
                        scope.launch {
                            val result = viewModel.createPurchase(orderId)
                            // Always reset loading state regardless of result
                            isCreatingPurchase = false
                            if (result.isSuccess) {
                                Toast.makeText(context, purchaseCreatedSuccess, Toast.LENGTH_LONG).show()
                                navController.navigate(Routes.PURCHASES) {
                                    popUpTo(Routes.PURCHASE_ORDER_DETAIL) { inclusive = true }
                                }
                            } else {
                                val errorMsg = result.exceptionOrNull()?.message ?: purchaseFailed
                                Toast.makeText(context, "❌ $errorMsg", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    enabled = !isCreatingPurchase
                ) {
                    if (isCreatingPurchase) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(yesCreatePurchase)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        if (!isCreatingPurchase) {
                            showCreatePurchaseDialog = false 
                        }
                    },
                    enabled = !isCreatingPurchase
                ) {
                    Text(cancelText)
                }
            }
        )
    }
    
    // Delete Confirmation Dialog
    if (showDeleteConfirmation && deleteIndex >= 0) {
        val currentOrder = state.order
        if (currentOrder != null) {
            val isCompleted = currentOrder.status == PurchaseOrderStatus.COMPLETED
            if (!isCompleted) {
                val itemName = when (deleteStatus) {
                    PurchaseOrderStatus.ORDER -> {
                        if (deleteIndex < state.editableOrderItems.size) state.editableOrderItems[deleteIndex].productName else ""
                    }
                    PurchaseOrderStatus.RECEIVED -> {
                        if (deleteIndex < currentOrder.receivedItems.size) currentOrder.receivedItems[deleteIndex].productName else ""
                    }
                    else -> ""
                }
                
                val deleteOrderItem = stringResource(R.string.delete_order_item)
                val confirmDeleteOrder = stringResource(R.string.confirm_delete_order)
                val deleteText = stringResource(R.string.delete)
                val cancelText = stringResource(R.string.cancel)
                val orderDeleted = stringResource(R.string.order_deleted)
                
                AlertDialog(
                    onDismissRequest = { 
                        showDeleteConfirmation = false
                        deleteIndex = -1
                        deleteStatus = null
                    },
                    title = { Text(deleteOrderItem) },
                    text = { Text("$confirmDeleteOrder '${itemName}'?") },
                    confirmButton = {
                        Button(
                            onClick = {
                                scope.launch {
                                    when (deleteStatus) {
                                        PurchaseOrderStatus.ORDER -> {
                                            val newItems = state.editableOrderItems.filterIndexed { i, _ -> i != deleteIndex }
                                            viewModel.updateOrderItems(newItems, orderId)
                                            selectedIndices = emptySet()
                                        }
                                        PurchaseOrderStatus.RECEIVED -> {
                                            val updatedReceivedItems = currentOrder.receivedItems.filterIndexed { i, _ -> i != deleteIndex }
                                            val updatedOrder = currentOrder.copy(
                                                receivedItems = updatedReceivedItems,
                                                receivedTotal = updatedReceivedItems.sumOf { it.total },
                                                updatedAt = System.currentTimeMillis()
                                            )
                                            repository.updateOrder(updatedOrder)
                                            viewModel.loadOrder(orderId)
                                        }
                                        else -> {}
                                    }
                                    showDeleteConfirmation = false
                                    deleteIndex = -1
                                    deleteStatus = null
                                    Toast.makeText(context, orderDeleted, Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(deleteText)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { 
                            showDeleteConfirmation = false
                            deleteIndex = -1
                            deleteStatus = null
                        }) {
                            Text(cancelText)
                        }
                    }
                )
            }
        }
    }
    
    // Edit Dialog
    if (showEditDialog && editingIndex >= 0) {
        val currentOrder = state.order
        if (currentOrder != null) {
            val isCompleted = currentOrder.status == PurchaseOrderStatus.COMPLETED
            if (!isCompleted) {
                val items = when (editingStatus) {
                    PurchaseOrderStatus.ORDER -> state.editableOrderItems
                    PurchaseOrderStatus.RECEIVED -> currentOrder.receivedItems
                    PurchaseOrderStatus.COMPLETED -> currentOrder.receivedItems
                    else -> emptyList()
                }
                
                if (editingIndex < items.size) {
                    val editOrderItem = stringResource(R.string.edit_order_item)
                    val enterQuantity = stringResource(R.string.enter_quantity)
                    val enterPrice = stringResource(R.string.enter_price)
                    val saveOrder = stringResource(R.string.save_order)
                    val orderUpdated = stringResource(R.string.order_updated)
                    val enterValidAmount = stringResource(R.string.enter_valid_amount)
                    val cancelText = stringResource(R.string.cancel)
                    
                    AlertDialog(
                        onDismissRequest = { showEditDialog = false },
                        title = { Text(editOrderItem) },
                        text = {
                            Column {
                                Text(
                                    text = editProductName,
                                    style = AppTypography.body,
                                    modifier = Modifier.padding(bottom = Spacing.medium)
                                )
                                
                                OutlinedTextField(
                                    value = editQuantity,
                                    onValueChange = { editQuantity = it },
                                    label = { Text(enterQuantity) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                
                                Spacer(modifier = Modifier.height(Spacing.medium))
                                
                                OutlinedTextField(
                                    value = editPrice,
                                    onValueChange = { editPrice = it },
                                    label = { Text(enterPrice) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    val qty = editQuantity.toIntOrNull() ?: 0
                                    val price = editPrice.toIntOrNull() ?: 0
                                    
                                    if (qty > 0 && price > 0) {
                                        scope.launch {
                                            when (editingStatus) {
                                                PurchaseOrderStatus.ORDER -> {
                                                    val newItems = state.editableOrderItems.toMutableList()
                                                    val current = newItems[editingIndex]
                                                    newItems[editingIndex] = current.copy(
                                                        quantity = qty,
                                                        costPrice = price,
                                                        total = qty * price
                                                    )
                                                    viewModel.updateOrderItems(newItems, orderId)
                                                }
                                                PurchaseOrderStatus.RECEIVED -> {
                                                    val updatedReceivedItems = currentOrder.receivedItems.toMutableList()
                                                    val current = updatedReceivedItems[editingIndex]
                                                    updatedReceivedItems[editingIndex] = current.copy(
                                                        quantity = qty,
                                                        costPrice = price,
                                                        total = qty * price
                                                    )
                                                    val updatedOrder = currentOrder.copy(
                                                        receivedItems = updatedReceivedItems,
                                                        receivedTotal = updatedReceivedItems.sumOf { it.total },
                                                        updatedAt = System.currentTimeMillis()
                                                    )
                                                    repository.updateOrder(updatedOrder)
                                                    viewModel.loadOrder(orderId)
                                                }
                                                else -> {}
                                            }
                                        }
                                        showEditDialog = false
                                        Toast.makeText(context, orderUpdated, Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, enterValidAmount, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Text(saveOrder)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showEditDialog = false }) {
                                Text(cancelText)
                            }
                        }
                    )
                }
            }
        }
    }
    
    // Add Dialog
    if (showAddDialog) {
        val addItemToOrderText = stringResource(R.string.add_item_to_order)
        
        AddItemDialog(
            showDialog = showAddDialog,
            products = products,
            onDismiss = { showAddDialog = false },
            onAdd = { product, qty ->
                val existing = state.editableOrderItems.find { it.productId == product.id }
                val newItems = state.editableOrderItems.toMutableList()
                
                if (existing != null) {
                    val index = newItems.indexOf(existing)
                    newItems[index] = existing.copy(
                        quantity = existing.quantity + qty,
                        total = (existing.quantity + qty) * existing.costPrice
                    )
                } else {
                    newItems.add(
                        PurchaseOrderItem(
                            productId = product.id,
                            productName = product.name,
                            quantity = qty,
                            costPrice = product.sellPrice,
                            total = qty * product.sellPrice
                        )
                    )
                }
                
                viewModel.updateOrderItems(newItems, orderId)
                showAddDialog = false
                Toast.makeText(context, addItemToOrderText, Toast.LENGTH_SHORT).show()
            }
        )
    }
}
