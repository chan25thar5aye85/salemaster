package com.akari.retailer.features.sales.data.repository

import com.akari.retailer.features.sales.data.remote.FirestoreIncomeStreamService
import com.akari.retailer.features.sales.domain.models.IncomeStream
import kotlinx.coroutines.flow.Flow

class FirestoreIncomeStreamRepository(
    private val service: FirestoreIncomeStreamService
) : IncomeStreamRepository {
    
    override suspend fun addIncomeStream(stream: IncomeStream): Result<String> {
        return service.addIncomeStream(stream)
    }
    
    override suspend fun updateIncomeStream(stream: IncomeStream): Result<Unit> {
        return service.updateIncomeStream(stream)
    }
    
    override suspend fun deleteIncomeStream(streamId: String): Result<Unit> {
        return service.deleteIncomeStream(streamId)
    }
    
    override fun getIncomeStreams(): Flow<List<IncomeStream>> {
        return service.getIncomeStreams()
    }
    
    override fun getIncomeStreamById(streamId: String): Flow<IncomeStream?> {
        return service.getIncomeStreamById(streamId)
    }
    
    override suspend fun seedDefaultIncomeStreams(): Result<Unit> {
        return service.seedDefaultIncomeStreams()
    }
}
