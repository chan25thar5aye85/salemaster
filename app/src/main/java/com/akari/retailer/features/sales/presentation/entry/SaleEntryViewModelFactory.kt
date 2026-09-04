package com.akari.retailer.features.sales.presentation.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.akari.retailer.data.repository.SaleRepository

class SaleEntryViewModelFactory(
    private val repository: SaleRepository
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SaleEntryViewModel::class.java)) {
            return SaleEntryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
