package com.akari.retailer.features.inventory.presentation

import androidx.lifecycle.ViewModel
import com.akari.retailer.features.inventory.domain.models.PurchaseOrder

class PurchaseOrderSharedViewModel : ViewModel() {
    
    companion object {
        private var orders: List<PurchaseOrder> = emptyList()
        private var listeners: MutableList<(List<PurchaseOrder>) -> Unit> = mutableListOf()
    }
    
    fun getOrders(): List<PurchaseOrder> = orders
    
    fun setOrders(newOrders: List<PurchaseOrder>) {
        orders = newOrders
        listeners.forEach { it(orders) }
    }
    
    fun getOrderById(orderId: String): PurchaseOrder? {
        return orders.find { it.id == orderId }
    }
    
    fun addListener(listener: (List<PurchaseOrder>) -> Unit) {
        listeners.add(listener)
        listener(orders)
    }
    
    fun removeListener(listener: (List<PurchaseOrder>) -> Unit) {
        listeners.remove(listener)
    }
    
    fun clear() {
        orders = emptyList()
        listeners.clear()
    }
}
