package com.akari.retailer.features.customer.presentation

import com.akari.retailer.features.customer.domain.models.Customer

data class CustomerListState(
    val customers: List<Customer> = emptyList(),
    val allCustomers: List<Customer> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val searchQuery: String = ""
)
