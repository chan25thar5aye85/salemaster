package com.akari.retailer.features.supplier.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.akari.retailer.features.supplier.data.repository.SupplierRepository

class SupplierListViewModelFactory(
    private val repository: SupplierRepository
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SupplierListViewModel::class.java)) {
            return SupplierListViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
