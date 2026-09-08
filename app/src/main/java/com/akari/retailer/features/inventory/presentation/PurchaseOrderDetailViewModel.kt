package com.akari.retailer.features.inventory.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.features.expense.data.repository.FirestoreExpenseRepository
import com.akari.retailer.features.expense.data.remote.FirestoreExpenseService
import com.akari.retailer.features.expense.domain.models.Expense
import com.akari.retailer.features.inventory.data.repository.FirestoreInventoryRepository
import com.akari.retailer.features.inventory.data.repository.FirestorePurchaseOrderRepository
import com.akari.retailer.features.inventory.data.repository.FirestorePurchaseRepository
import com.akari.retailer.features.inventory.data.remote.FirestoreInventoryService
import com.akari.retailer.features.inventory.domain.models.Purchase
import com.akari.retailer.features.inventory.domain.models.PurchaseItem
import com.akari.retailer.features.inventory.domain.models.PurchaseOrder
import com.akari.retailer.features.inventory.domain.models.PurchaseOrderItem
import com.akari.retailer.features.inventory.domain.models.PurchaseOrderStatus
import com.akari.retailer.features.supplier.data.repository.FirestoreSupplierRepository
import com.akari.retailer.features.supplier.data.remote.FirestoreSupplierService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PurchaseOrderDetailState(
    val order: PurchaseOrder? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isUpdating: Boolean = false,
    val editableOrderItems: List<PurchaseOrderItem> = emptyList()
)

class PurchaseOrderDetailViewModel(
    private val repository: FirestorePurchaseOrderRepository
) : ViewModel() {

    private val TAG = "PurchaseOrderDetailVM"
    private val _state = MutableStateFlow(PurchaseOrderDetailState())
    val state: StateFlow<PurchaseOrderDetailState> = _state.asStateFlow()

    // Lazy initialize repositories
    private val inventoryService by lazy { FirestoreInventoryService() }
    private val inventoryRepository by lazy { FirestoreInventoryRepository(inventoryService) }
    private val purchaseRepo by lazy { FirestorePurchaseRepository() }
    private val expenseService by lazy { FirestoreExpenseService() }
    private val expenseRepository by lazy { FirestoreExpenseRepository(expenseService) }
    private val supplierService by lazy { FirestoreSupplierService() }
    private val supplierRepository by lazy { FirestoreSupplierRepository(supplierService) }

    fun loadOrder(orderId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                repository.getOrder(orderId).collect { loadedOrder ->
                    _state.value = _state.value.copy(
                        order = loadedOrder,
                        isLoading = false,
                        editableOrderItems = loadedOrder?.orderItems ?: emptyList(),
                        error = if (loadedOrder == null) "Order not found" else null
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load order"
                )
            }
        }
    }

    fun updateOrderItems(items: List<PurchaseOrderItem>, orderId: String) {
        viewModelScope.launch {
            val currentOrder = _state.value.order ?: return@launch
            val updatedOrder = currentOrder.copy(
                orderItems = items,
                orderTotal = items.sumOf { it.total },
                updatedAt = System.currentTimeMillis()
            )
            repository.updateOrder(updatedOrder)
            _state.value = _state.value.copy(editableOrderItems = items)
        }
    }

    fun receiveSelectedItems(orderId: String, selectedItems: List<PurchaseOrderItem>) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isUpdating = true)
            
            val currentOrder = _state.value.order ?: return@launch
            
            val currentReceivedItems = currentOrder.receivedItems.toMutableList()
            selectedItems.forEach { item ->
                if (!currentReceivedItems.any { it.productId == item.productId }) {
                    currentReceivedItems.add(item)
                }
            }
            
            val updatedOrder = currentOrder.copy(
                receivedItems = currentReceivedItems,
                receivedTotal = currentReceivedItems.sumOf { it.total },
                receivedDate = if (currentReceivedItems.isNotEmpty()) System.currentTimeMillis() else currentOrder.receivedDate,
                updatedAt = System.currentTimeMillis()
            )
            
            repository.updateOrder(updatedOrder)
            loadOrder(orderId)
            _state.value = _state.value.copy(isUpdating = false)
        }
    }

    suspend fun createPurchase(orderId: String): Result<Unit> {
        return try {
            Log.d(TAG, "🔄 Starting createPurchase for order: $orderId")
            
            val currentOrder = _state.value.order
            if (currentOrder == null) {
                Log.e(TAG, "❌ Order not found")
                return Result.failure(Exception("Order not found"))
            }
            
            Log.d(TAG, "📋 Current order status: ${currentOrder.status}")
            
            if (currentOrder.status == PurchaseOrderStatus.COMPLETED) {
                Log.e(TAG, "❌ Purchase already created")
                return Result.failure(Exception("Purchase already created for this order"))
            }
            
            if (currentOrder.receivedItems.isEmpty()) {
                Log.e(TAG, "❌ No items to purchase")
                return Result.failure(Exception("No items to purchase"))
            }
            
            Log.d(TAG, "📦 Received items count: ${currentOrder.receivedItems.size}")
            
            val receiptNumber = generateReceiptNumber()
            Log.d(TAG, "🧾 Receipt number: $receiptNumber")
            
            // 1. Create Purchase record
            val purchase = Purchase(
                orderId = currentOrder.id,
                orderName = currentOrder.orderName,
                orderNumber = currentOrder.orderNumber,
                supplierId = currentOrder.supplierId,
                supplierName = currentOrder.supplierName,
                items = currentOrder.receivedItems.map { item ->
                    PurchaseItem(
                        productId = item.productId,
                        productName = item.productName,
                        quantity = item.quantity,
                        costPrice = item.costPrice,
                        total = item.total
                    )
                },
                totalCost = currentOrder.receivedItems.sumOf { it.total },
                purchaseDate = System.currentTimeMillis(),
                notes = currentOrder.notes,
                receiptNumber = receiptNumber
            )
            
            Log.d(TAG, "💾 Saving purchase to Firestore...")
            val purchaseResult = purchaseRepo.createPurchase(purchase)
            
            if (purchaseResult.isFailure) {
                val error = purchaseResult.exceptionOrNull()?.message ?: "Failed to create purchase"
                Log.e(TAG, "❌ Failed to create purchase: $error")
                return Result.failure(Exception(error))
            }
            
            Log.d(TAG, "✅ Purchase created: ${purchaseResult.getOrNull()}")
            
            // 2. Update stock for each item
            Log.d(TAG, "📦 Updating stock...")
            currentOrder.receivedItems.forEach { item ->
                val productResult = inventoryRepository.getProductByIdSync(item.productId)
                if (productResult.isSuccess) {
                    val product = productResult.getOrNull()
                    if (product != null) {
                        val newStock = product.stockQuantity + item.quantity
                        val updatedProduct = product.copy(
                            stockQuantity = newStock,
                            updatedAt = System.currentTimeMillis()
                        )
                        inventoryRepository.updateProduct(updatedProduct)
                        Log.d(TAG, "✅ Stock updated for ${item.productName}: ${product.stockQuantity} → $newStock")
                    }
                }
            }
            
            // 3. Create expense record using categoryId (string) instead of enum
            Log.d(TAG, "💰 Creating expense...")
            val totalCost = currentOrder.receivedItems.sumOf { it.total }
            val expense = Expense(
                title = "Purchase Order: ${currentOrder.orderName}",
                amount = totalCost,
                categoryId = "default_inventory",  // ✅ Fixed - using categoryId string
                description = "PO #${currentOrder.orderNumber} from ${currentOrder.supplierName}",
                date = System.currentTimeMillis()
            )
            val expenseResult = expenseRepository.addExpense(expense)
            if (expenseResult.isSuccess) {
                Log.d(TAG, "✅ Expense created: ${expenseResult.getOrNull()}")
            } else {
                Log.w(TAG, "⚠️ Failed to create expense: ${expenseResult.exceptionOrNull()?.message}")
            }
            
            // 4. Update supplier stats
            Log.d(TAG, "📊 Updating supplier stats...")
            try {
                val suppliers = supplierRepository.getSuppliers().first()
                val supplier = suppliers.find { it.id == currentOrder.supplierId }
                if (supplier != null) {
                    val updatedSupplier = supplier.copy(
                        totalPurchased = supplier.totalPurchased + totalCost,
                        lastOrderDate = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    supplierRepository.updateSupplier(updatedSupplier)
                    Log.d(TAG, "✅ Supplier stats updated: ${supplier.name} total: ${supplier.totalPurchased} → ${updatedSupplier.totalPurchased}")
                } else {
                    Log.w(TAG, "⚠️ Supplier not found: ${currentOrder.supplierId}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Failed to update supplier stats: ${e.message}")
            }
            
            // 5. Mark order as COMPLETED
            Log.d(TAG, "🔄 Updating order status to COMPLETED...")
            val statusResult = repository.updateStatus(orderId, PurchaseOrderStatus.COMPLETED)
            if (statusResult.isFailure) {
                val error = statusResult.exceptionOrNull()?.message ?: "Failed to update order status"
                Log.e(TAG, "❌ Failed to update status: $error")
                return Result.failure(Exception(error))
            }
            
            Log.d(TAG, "✅ Order status updated to COMPLETED")
            loadOrder(orderId)
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Create purchase error: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun updateStatus(orderId: String, newStatus: PurchaseOrderStatus) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isUpdating = true)
            val result = repository.updateStatus(orderId, newStatus)
            if (result.isSuccess) {
                loadOrder(orderId)
            }
            _state.value = _state.value.copy(isUpdating = false)
        }
    }

    suspend fun deleteOrder(orderId: String): Result<Unit> {
        return repository.deleteOrder(orderId)
    }
    
    private fun generateReceiptNumber(): String {
        val date = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val random = (1000..9999).random()
        return "RCP-$date-$random"
    }
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
