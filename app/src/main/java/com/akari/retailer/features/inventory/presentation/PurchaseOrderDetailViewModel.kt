package com.akari.retailer.features.inventory.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.features.inventory.data.repository.FirestorePurchaseOrderRepository
import com.akari.retailer.features.inventory.domain.models.PurchaseOrder
import com.akari.retailer.features.inventory.domain.models.PurchaseOrderItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PurchaseOrderDetailState(
    val order: PurchaseOrder? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isUpdating: Boolean = false,
    val editableDraftItems: List<PurchaseOrderItem> = emptyList()
)

class PurchaseOrderDetailViewModel(
    private val repository: FirestorePurchaseOrderRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PurchaseOrderDetailState())
    val state: StateFlow<PurchaseOrderDetailState> = _state.asStateFlow()

    fun loadOrder(orderId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                repository.getOrder(orderId).collect { loadedOrder ->
                    _state.value = _state.value.copy(
                        order = loadedOrder,
                        isLoading = false,
                        editableDraftItems = loadedOrder?.draftItems ?: emptyList(),
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

    fun updateDraftItems(items: List<PurchaseOrderItem>, orderId: String) {
        viewModelScope.launch {
            val currentOrder = _state.value.order ?: return@launch
            val updatedOrder = currentOrder.copy(
                draftItems = items,
                draftTotal = items.sumOf { it.total },
                updatedAt = System.currentTimeMillis()
            )
            repository.updateOrder(updatedOrder)
            _state.value = _state.value.copy(editableDraftItems = items)
        }
    }

    fun sendSelectedItems(orderId: String, selectedItems: List<PurchaseOrderItem>) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isUpdating = true)
            
            val currentOrder = _state.value.order ?: return@launch
            
            // Send only selected items to SENT
            val currentSentItems = currentOrder.sentItems.toMutableList()
            // Add only items that aren't already in sent
            selectedItems.forEach { item ->
                if (!currentSentItems.any { it.productId == item.productId }) {
                    currentSentItems.add(item)
                }
            }
            
            val updatedOrder = currentOrder.copy(
                sentItems = currentSentItems,
                sentTotal = currentSentItems.sumOf { it.total },
                sentDate = if (currentSentItems.isNotEmpty()) System.currentTimeMillis() else currentOrder.sentDate,
                updatedAt = System.currentTimeMillis()
            )
            
            repository.updateOrder(updatedOrder)
            loadOrder(orderId)
            _state.value = _state.value.copy(isUpdating = false)
        }
    }

    fun updateStatus(orderId: String, newStatus: com.akari.retailer.features.inventory.domain.models.PurchaseOrderStatus) {
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
}
