package com.akari.retailer.features.customer.data.repository

import com.akari.retailer.features.customer.domain.models.Customer
import kotlinx.coroutines.flow.Flow

interface CustomerRepository {
    suspend fun addCustomer(customer: Customer): Result<String>
    suspend fun updateCustomer(customer: Customer): Result<Unit>
    suspend fun deleteCustomer(customerId: String): Result<Unit>
    fun getCustomers(): Flow<List<Customer>>
    fun getCustomerById(customerId: String): Flow<Customer?>
    suspend fun searchCustomers(query: String): Result<List<Customer>>
}
