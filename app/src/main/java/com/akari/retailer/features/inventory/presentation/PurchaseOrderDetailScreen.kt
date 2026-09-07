package com.akari.retailer.features.inventory.presentation

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
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
    
    var showAddDialog by remember { mutableStateOf(false) }
    
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
    LaunchedEffect(state.editableDraftItems) {
        selectedIndices = emptySet()
    }

    AppScreen(
        title = state.order?.orderName ?: "Order Detail",
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
                            AppPrimaryButton(text = "Go Back", onClick = onBack)
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
                            Text("Order not found", style = AppTypography.header)
                            Spacer(modifier = Modifier.height(Spacing.medium))
                            AppPrimaryButton(text = "Go Back", onClick = onBack)
                        }
                    }
                }
                
                else -> {
                    val currentOrder = state.order!!
                    val statusTabs = listOf(
                        "DRAFT" to PurchaseOrderStatus.DRAFT,
                        "SENT" to PurchaseOrderStatus.SENT
                    )
                    
                    val selectedStatus = statusTabs[selectedTab].second
                    val displayItems = if (selectedStatus == PurchaseOrderStatus.DRAFT) {
                        state.editableDraftItems
                    } else {
                        currentOrder.getItemsForStatus(selectedStatus)
                    }
                    
                    val displayTotal = if (selectedStatus == PurchaseOrderStatus.DRAFT) {
                        state.editableDraftItems.sumOf { it.total }
                    } else {
                        currentOrder.getTotalForStatus(selectedStatus)
                    }
                    
                    // Status Tabs
                    PurchaseOrderStatusTabs(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it },
                        order = currentOrder,
                        statusTabs = statusTabs
                    )
                    
                    Spacer(modifier = Modifier.height(Spacing.small))
                    
                    // Items List with selection
                    PurchaseOrderItemsList(
                        items = displayItems,
                        status = selectedStatus,
                        total = displayTotal,
                        selectedIndices = selectedIndices,
                        onItemSelect = { index ->
                            if (selectedIndices.contains(index)) {
                                selectedIndices = selectedIndices - index
                            } else {
                                selectedIndices = selectedIndices + index
                            }
                        },
                        onItemClick = { index ->
                            if (selectedStatus == PurchaseOrderStatus.DRAFT) {
                                val item = state.editableDraftItems[index]
                                editingIndex = index
                                editQuantity = item.quantity.toString()
                                editPrice = item.costPrice.toString()
                                editProductName = item.productName
                                showEditDialog = true
                            }
                        },
                        onItemDelete = { index ->
                            if (selectedStatus == PurchaseOrderStatus.DRAFT) {
                                val newItems = state.editableDraftItems.filterIndexed { i, _ -> i != index }
                                viewModel.updateDraftItems(newItems, orderId)
                                selectedIndices = emptySet()
                            }
                        },
                        onAddClick = { showAddDialog = true },
                        onSendClick = {
                            if (selectedStatus == PurchaseOrderStatus.DRAFT && selectedIndices.isNotEmpty()) {
                                val selectedItems = selectedIndices.map { state.editableDraftItems[it] }
                                viewModel.sendSelectedItems(orderId, selectedItems)
                                selectedIndices = emptySet()
                            }
                        },
                        isUpdating = state.isUpdating,
                        onSelectAll = {
                            if (selectedStatus == PurchaseOrderStatus.DRAFT) {
                                if (selectedIndices.size == displayItems.size) {
                                    selectedIndices = emptySet()
                                } else {
                                    selectedIndices = displayItems.indices.toSet()
                                }
                            }
                        }
                    )
                    
                    // Actions for SENT (only Back to Draft)
                    if (selectedStatus == PurchaseOrderStatus.SENT) {
                        Spacer(modifier = Modifier.height(Spacing.medium))
                        OutlinedButton(
                            onClick = {
                                viewModel.updateStatus(orderId, PurchaseOrderStatus.DRAFT)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("↩️ Back to Draft")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(Spacing.xxlarge))
                }
            }
        }
    }
    
    // Edit Dialog
    EditItemDialog(
        showDialog = showEditDialog,
        itemIndex = editingIndex,
        productName = editProductName,
        currentQuantity = editQuantity,
        currentPrice = editPrice,
        onDismiss = { showEditDialog = false },
        onSave = { qty, price ->
            if (editingIndex >= 0 && editingIndex < state.editableDraftItems.size) {
                val newItems = state.editableDraftItems.toMutableList()
                val current = newItems[editingIndex]
                newItems[editingIndex] = current.copy(
                    quantity = qty,
                    costPrice = price,
                    total = qty * price
                )
                viewModel.updateDraftItems(newItems, orderId)
                showEditDialog = false
                Toast.makeText(context, "Updated!", Toast.LENGTH_SHORT).show()
            }
        }
    )
    
    // Add Dialog
    AddItemDialog(
        showDialog = showAddDialog,
        products = products,
        onDismiss = { showAddDialog = false },
        onAdd = { product, qty ->
            val existing = state.editableDraftItems.find { it.productId == product.id }
            val newItems = state.editableDraftItems.toMutableList()
            
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
            
            viewModel.updateDraftItems(newItems, orderId)
            showAddDialog = false
            Toast.makeText(context, "Item added!", Toast.LENGTH_SHORT).show()
        }
    )
}

class PurchaseOrderDetailViewModelFactory(
    private val repository: FirestorePurchaseOrderRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PurchaseOrderDetailViewModel::class.java)) {
            return PurchaseOrderDetailViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
