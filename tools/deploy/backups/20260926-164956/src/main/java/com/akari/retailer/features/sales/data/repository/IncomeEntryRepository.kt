package com.akari.retailer.features.sales.data.repository

import com.akari.retailer.features.sales.domain.models.IncomeEntry
import kotlinx.coroutines.flow.Flow

interface IncomeEntryRepository {
    suspend fun addIncomeEntry(entry: IncomeEntry): Result<String>
    suspend fun updateIncomeEntry(entry: IncomeEntry): Result<Unit>
    suspend fun deleteIncomeEntry(entryId: String): Result<Unit>
    fun getIncomeEntries(): Flow<List<IncomeEntry>>
    fun getIncomeEntryById(entryId: String): Flow<IncomeEntry?>
}
