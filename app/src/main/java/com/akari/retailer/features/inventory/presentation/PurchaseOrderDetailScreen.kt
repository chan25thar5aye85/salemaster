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
import com.akari.retailer.core.ui.components.PaymentListComponent
import com.akari.retailer.core.ui.theme.AppTypography
import com.akari.retailer.core.ui.theme.Spacing
import com.akari.retailer.features.inventory.domain.models.PurchaseOrderItem
import com.akari.retailer.features.inventory.domain.models.PurchaseOrderStatus
import com.akari.retailer.navigation.Routes
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseOrderDetailScreen(
    navController: NavController,
    orderId: String,
    isReadOnly: Boolean = false,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as com.akari.retailer.RetailApplication
    val scope = rememberCoroutineScope()

    val viewModel: PurchaseOrderDetailViewModel = viewModel(
        factory = PurchaseOrderDetailViewModelFactory(application.container.purchaseOrderRepository, context)
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
    
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    
    // Load products
    LaunchedEffect(Unit) {
        application.container.inventoryRepository.getProducts().collect { productList ->
            products = productList
        }
    }
    
    // Load accounts
    LaunchedEffect(Unit) {
        viewModel.loadAccounts()
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
        title = if (isReadOnly) "Order History" else (state.order?.orderName ?: stringResource(R.string.order_detail)),
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
                    
                    val statusTabs = listOf(
                        stringResource(R.string.order_status_order) to PurchaseOrderStatus.ORDER,
                        stringResource(R.string.order_status_received) to PurchaseOrderStatus.RECEIVED
                    )
                    
                    if (selectedTab >= statusTabs.size) {
                        selectedTab = 0
                    }
                    
                    val selectedStatus = statusTabs[selectedTab].second
                    val isReceived = selectedStatus == PurchaseOrderStatus.RECEIVED
                    val isOrder = selectedStatus == PurchaseOrderStatus.ORDER
                    
                    val displayItems = when (selectedStatus) {
                        PurchaseOrderStatus.ORDER -> state.editableOrderItems
                        PurchaseOrderStatus.RECEIVED -> currentOrder.receivedItems
                        else -> emptyList()
                    }
                    
                    val displayTotal = when (selectedStatus) {
                        PurchaseOrderStatus.ORDER -> state.editableOrderItems.sumOf { it.total }
                        PurchaseOrderStatus.RECEIVED -> currentOrder.receivedItems.sumOf { it.total }
                        else -> 0
                    }
                    
                    // Show dates in header for RECEIVED tab
                    if (isReceived) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(Spacing.medium)
                            ) {
                                Text(
                                    text = "📦 ${currentOrder.orderName}",
                                    style = AppTypography.title
                                )
                                Text(
                                    text = "📅 Order Date: ${dateFormat.format(currentOrder.orderDate)}",
                                    style = AppTypography.small,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                if (currentOrder.receivedDate > 0) {
                                    Text(
                                        text = "📥 Received Date: ${dateFormat.format(currentOrder.receivedDate)}",
                                        style = AppTypography.small,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(Spacing.small))
                    }
                    
                    PurchaseOrderStatusTabs(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it },
                        order = currentOrder,
                        statusTabs = statusTabs
                    )
                    
                    Spacer(modifier = Modifier.height(Spacing.small))
                    
                    // RECEIVED tab - Show Purchase button only if NOT read-only
                    if (isReceived && !isReadOnly) {
                        // ✅ SPLIT PAYMENT COMPONENT
                        PaymentListComponent(
                            paymentRows = state.paymentRows,
                            accounts = state.accounts,
                            totalAmount = state.getTotalCost(),
                            onAccountSelected = { rowId, account ->
                                viewModel.updatePaymentAccount(rowId, account)
                            },
                            onAmountChanged = { rowId, amount ->
                                viewModel.updatePaymentAmount(rowId, amount)
                            },
                            onAddRow = { viewModel.addPaymentRow() },
                            onRemoveRow = { rowId -> viewModel.removePaymentRow(rowId) }
                        )
                        
                        Spacer(modifier = Modifier.height(Spacing.medium))
                        
                        AppPrimaryButton(
                            text = if (isCreatingPurchase) stringResource(R.string.saving) else stringResource(R.string.create_purchase),
                            onClick = {
                                showCreatePurchaseDialog = true
                            },
                            isLoading = isCreatingPurchase,
                            enabled = !isCreatingPurchase && state.isFullyPaid(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(Spacing.medium))
                    }
                    
                    // Items List
                    PurchaseOrderItemsList(
                        items = displayItems,
                        status = selectedStatus,
                        total = displayTotal,
                        selectedIndices = selectedIndices,
                        onItemSelect = { index ->
                            selectedIndices = if (selectedIndices.contains(index)) {
                                selectedIndices - index
                            } else {
                                selectedIndices + index
                            }
                        },
                        onItemClick = { index ->
                            if (isOrder && !isReadOnly) {
                                val item = state.editableOrderItems[index]
                                editingIndex = index
                                editQuantity = item.quantity.toString()
                                editPrice = item.costPrice.toString()
                                editProductName = item.productName
                                editingStatus = selectedStatus
                                showEditDialog = true
                            }
                        },
                        onItemDelete = { index ->
                            if (isOrder && !isReadOnly) {
                                deleteIndex = index
                                deleteStatus = selectedStatus
                                showDeleteConfirmation = true
                            }
                        },
                        onAddClick = {
                            if (isOrder && !isReadOnly) {
                                showAddDialog = true
                            }
                        },
                        onReceiveClick = if (isOrder && !isReadOnly) {
                            {
                                val selectedItems = selectedIndices
                                    .sorted()
                                    .mapNotNull { state.editableOrderItems.getOrNull(it) }
                                if (selectedItems.isNotEmpty()) {
                                    viewModel.receiveSelectedItems(orderId, selectedItems)
                                    selectedIndices = emptySet()
                                }
                            }
                        } else null,
                        isUpdating = state.isUpdating,
                        onSelectAll = {
                            selectedIndices = if (selectedIndices.size == displayItems.size) {
                                emptySet()
                            } else {
                                displayItems.indices.toSet()
                            }
                        },
                        showReceiveButton = isOrder && !isReadOnly,
                        showAddButton = isOrder && !isReadOnly,
                        showDeleteButton = isOrder && !isReadOnly,
                        isReadOnly = isReadOnly
                    )
                    
                    // Read-only info message
                    if (isReadOnly) {
                        Spacer(modifier = Modifier.height(Spacing.medium))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Text(
                                text = "🔒 Read-only view - Original order from purchase",
                                style = AppTypography.body,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.medium)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(Spacing.xxlarge))
                }
            }
        }
    }
    
    // Edit Item Dialog
    if (showEditDialog && editingIndex >= 0 && editingStatus != null) {
        AlertDialog(
            onDismissRequest = { 
                showEditDialog = false
                editingIndex = -1
            },
            title = { Text("Edit Item") },
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
                        label = { Text("Quantity") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(Spacing.medium))
                    
                    OutlinedTextField(
                        value = editPrice,
                        onValueChange = { editPrice = it },
                        label = { Text("Price") },
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
                        if (qty > 0 && price > 0 && editingStatus != null) {
                            val updatedItems = state.editableOrderItems.toMutableList()
                            updatedItems[editingIndex] = updatedItems[editingIndex].copy(
                                quantity = qty,
                                costPrice = price,
                                total = qty * price
                            )
                            viewModel.updateOrderItems(updatedItems, orderId)
                        }
                        showEditDialog = false
                        editingIndex = -1
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showEditDialog = false
                    editingIndex = -1
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    
    // Delete Confirmation Dialog
    if (showDeleteConfirmation && deleteIndex >= 0) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteConfirmation = false
                deleteIndex = -1
            },
            title = { Text("Delete Item") },
            text = { Text("Are you sure you want to delete this item?") },
            confirmButton = {
                Button(
                    onClick = {
                        val updatedItems = state.editableOrderItems.toMutableList()
                        if (deleteIndex in updatedItems.indices) {
                            updatedItems.removeAt(deleteIndex)
                            viewModel.updateOrderItems(updatedItems, orderId)
                        }
                        showDeleteConfirmation = false
                        deleteIndex = -1
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showDeleteConfirmation = false
                    deleteIndex = -1
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    
    // Add Item Dialog
    if (showAddDialog) {
        var searchQuery by remember { mutableStateOf("") }
        var selectedProduct by remember { mutableStateOf<com.akari.retailer.features.inventory.domain.models.Product?>(null) }
        var addQuantity by remember { mutableStateOf("1") }
        var addPrice by remember { mutableStateOf("") }
        
        val filteredProducts = products.filter { 
            it.name.contains(searchQuery, ignoreCase = true) || 
            it.sku.contains(searchQuery, ignoreCase = true)
        }
        
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Item") },
            text = {
                Column {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search products...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(Spacing.small))
                    
                    if (filteredProducts.isEmpty()) {
                        Text(
                            text = "No products found",
                            style = AppTypography.body,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    } else {
                        filteredProducts.take(5).forEach { product ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                onClick = {
                                    selectedProduct = product
                                    addPrice = product.costPrice.toString()
                                },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedProduct?.id == product.id) 
                                        MaterialTheme.colorScheme.primaryContainer 
                                    else 
                                        MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(Spacing.small),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(product.name, style = AppTypography.body)
                                        Text(
                                            "SKU: ${product.sku}",
                                            style = AppTypography.small,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                    Text(
                                        "${product.costPrice}",
                                        style = AppTypography.body,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                    
                    if (selectedProduct != null) {
                        Spacer(modifier = Modifier.height(Spacing.small))
                        
                        OutlinedTextField(
                            value = addQuantity,
                            onValueChange = { addQuantity = it },
                            label = { Text("Quantity") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(Spacing.small))
                        
                        OutlinedTextField(
                            value = addPrice,
                            onValueChange = { addPrice = it },
                            label = { Text("Price") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val product = selectedProduct
                        if (product != null) {
                            val qty = addQuantity.toIntOrNull() ?: 0
                            val price = addPrice.toIntOrNull() ?: 0
                            if (qty > 0 && price > 0) {
                                val newItem = PurchaseOrderItem(
                                    productId = product.id,
                                    productName = product.name,
                                    quantity = qty,
                                    costPrice = price,
                                    total = qty * price
                                )
                                val updatedItems = state.editableOrderItems + newItem
                                viewModel.updateOrderItems(updatedItems, orderId)
                            }
                        }
                        showAddDialog = false
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    
    // Create Purchase Confirmation Dialog
    if (showCreatePurchaseDialog && !isReadOnly) {
        AlertDialog(
            onDismissRequest = { 
                if (!isCreatingPurchase) {
                    showCreatePurchaseDialog = false 
                }
            },
            title = { 
                Text(
                    "⚠️ ${stringResource(R.string.finalize_purchase)}",
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Column {
                    Text(stringResource(R.string.create_purchase_warning), style = AppTypography.body)
                    Spacer(modifier = Modifier.height(Spacing.small))
                    Text(stringResource(R.string.purchase_will_update), style = AppTypography.body)
                    Spacer(modifier = Modifier.height(Spacing.small))
                    Text(stringResource(R.string.purchase_update_stock), style = AppTypography.small)
                    Text(stringResource(R.string.purchase_create_expense), style = AppTypography.small)
                    Text(stringResource(R.string.purchase_update_supplier), style = AppTypography.small)
                    Text(stringResource(R.string.purchase_generate_receipt), style = AppTypography.small)
                    Spacer(modifier = Modifier.height(Spacing.small))
                    Text(
                        stringResource(R.string.purchase_cannot_undo),
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
                            isCreatingPurchase = false
                            if (result.isSuccess) {
                                Toast.makeText(
                                    context, 
                                    context.getString(R.string.purchase_created_success), 
                                    Toast.LENGTH_LONG
                                ).show()
                                navController.navigate(Routes.PURCHASES) {
                                    popUpTo(Routes.PURCHASE_ORDER_DETAIL) { inclusive = true }
                                }
                            } else {
                                val errorMsg = result.exceptionOrNull()?.message 
                                    ?: context.getString(R.string.purchase_failed)
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
                        Text(stringResource(R.string.yes_create_purchase))
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
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
