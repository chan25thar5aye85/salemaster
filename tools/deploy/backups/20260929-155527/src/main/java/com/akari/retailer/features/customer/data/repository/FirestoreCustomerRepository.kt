package com.akari.retailer.features.customer.data.repository

import com.akari.retailer.features.customer.data.remote.FirestoreCustomerService
import com.akari.retailer.features.customer.domain.models.Customer
import kotlinx.coroutines.flow.Flow

class FirestoreCustomerRepository(
    private val service: FirestoreCustomerService
) : CustomerRepository {
    
    override suspend fun addCustomer(customer: Customer): Result<String> {
        return service.addCustomer(customer)
    }
    
    override suspend fun updateCustomer(customer: Customer): Result<Unit> {
        return service.updateCustomer(customer)
    }
    
    override suspend fun deleteCustomer(customerId: String): Result<Unit> {
        return service.deleteCustomer(customerId)
    }
    
    override fun getCustomers(): Flow<List<Customer>> {
        return service.getCustomers()
    }
    
    override fun getCustomerById(customerId: String): Flow<Customer?> {
        return service.getCustomerById(customerId)
    }
    
    override suspend fun searchCustomers(query: String): Result<List<Customer>> {
        return service.searchCustomers(query)
    }
}
