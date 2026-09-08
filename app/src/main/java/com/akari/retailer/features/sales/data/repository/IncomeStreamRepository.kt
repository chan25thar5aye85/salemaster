package com.akari.retailer.features.sales.data.repository

import com.akari.retailer.features.sales.domain.models.IncomeStream
import kotlinx.coroutines.flow.Flow

interface IncomeStreamRepository {
    suspend fun addIncomeStream(stream: IncomeStream): Result<String>
    suspend fun updateIncomeStream(stream: IncomeStream): Result<Unit>
    suspend fun deleteIncomeStream(streamId: String): Result<Unit>
    fun getIncomeStreams(): Flow<List<IncomeStream>>
    fun getIncomeStreamById(streamId: String): Flow<IncomeStream?>
    suspend fun seedDefaultIncomeStreams(): Result<Unit>
}
