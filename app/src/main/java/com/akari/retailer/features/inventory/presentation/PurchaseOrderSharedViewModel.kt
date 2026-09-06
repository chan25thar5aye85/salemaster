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
    
    fun addOrder(order: PurchaseOrder) {
        orders = orders + order
        listeners.forEach { it(orders) }
    }
    
    fun updateOrder(order: PurchaseOrder) {
        orders = orders.map { if (it.id == order.id) order else it }
        listeners.forEach { it(orders) }
    }
    
    fun removeOrder(orderId: String) {
        orders = orders.filter { it.id != orderId }
        listeners.forEach { it(orders) }
    }
    
    fun clear() {
        orders = emptyList()
        listeners.clear()
    }
    
    fun addListener(listener: (List<PurchaseOrder>) -> Unit) {
        listeners.add(listener)
        listener(orders)
    }
    
    fun removeListener(listener: (List<PurchaseOrder>) -> Unit) {
        listeners.remove(listener)
    }
}
