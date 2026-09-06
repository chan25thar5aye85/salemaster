package com.akari.retailer.features.inventory.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.features.inventory.data.repository.PurchaseOrderRepository
import com.akari.retailer.features.inventory.domain.models.PurchaseOrder
import com.akari.retailer.features.inventory.domain.models.PurchaseOrderStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PurchaseOrderListState(
    val orders: List<PurchaseOrder> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val filterStatus: PurchaseOrderStatus? = null
)

sealed class PurchaseOrderListEvent {
    data object LoadOrders : PurchaseOrderListEvent()
    data class FilterByStatus(val status: PurchaseOrderStatus?) : PurchaseOrderListEvent()
    data class DeleteOrder(val orderId: String) : PurchaseOrderListEvent()
    data class UpdateStatus(val orderId: String, val status: PurchaseOrderStatus) : PurchaseOrderListEvent()
    data object ClearError : PurchaseOrderListEvent()
}

class PurchaseOrderListViewModel(
    private val repository: PurchaseOrderRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PurchaseOrderListState())
    val state: StateFlow<PurchaseOrderListState> = _state.asStateFlow()

    init {
        loadOrders()
    }

    fun handleEvent(event: PurchaseOrderListEvent) {
        when (event) {
            is PurchaseOrderListEvent.LoadOrders -> loadOrders()
            is PurchaseOrderListEvent.FilterByStatus -> filterByStatus(event.status)
            is PurchaseOrderListEvent.DeleteOrder -> deleteOrder(event.orderId)
            is PurchaseOrderListEvent.UpdateStatus -> updateStatus(event.orderId, event.status)
            is PurchaseOrderListEvent.ClearError -> clearError()
        }
    }

    private fun loadOrders() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                repository.getOrders().collect { orders ->
                    _state.value = _state.value.copy(
                        orders = orders,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load orders"
                )
            }
        }
    }

    private fun filterByStatus(status: PurchaseOrderStatus?) {
        _state.value = _state.value.copy(filterStatus = status)
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                if (status == null) {
                    repository.getOrders().collect { orders ->
                        _state.value = _state.value.copy(
                            orders = orders,
                            isLoading = false
                        )
                    }
                } else {
                    repository.getOrdersByStatus(status).collect { orders ->
                        _state.value = _state.value.copy(
                            orders = orders,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to filter orders"
                )
            }
        }
    }

    private fun deleteOrder(orderId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val result = repository.deleteOrder(orderId)
                if (result.isSuccess) {
                    loadOrders()
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to delete order"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to delete order"
                )
            }
        }
    }

    private fun updateStatus(orderId: String, newStatus: PurchaseOrderStatus) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val result = repository.updateStatus(orderId, newStatus)
                if (result.isSuccess) {
                    loadOrders()
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to update status"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to update status"
                )
            }
        }
    }

    private fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}

class PurchaseOrderListViewModelFactory(
    private val repository: PurchaseOrderRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PurchaseOrderListViewModel::class.java)) {
            return PurchaseOrderListViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
