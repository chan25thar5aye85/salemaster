package com.akari.retailer.features.customer.presentation

sealed class CustomerListEvent {
    data object LoadCustomers : CustomerListEvent()
    data object RefreshCustomers : CustomerListEvent()
    data class DeleteCustomer(val customerId: String) : CustomerListEvent()
    data object ClearError : CustomerListEvent()
    data class SearchQueryChanged(val query: String) : CustomerListEvent()
    data object ClearSearch : CustomerListEvent()
}
