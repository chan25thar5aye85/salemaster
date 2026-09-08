package com.akari.retailer.features.sales.data.repository

import com.akari.retailer.features.sales.data.remote.FirestoreIncomeEntryService
import com.akari.retailer.features.sales.domain.models.IncomeEntry
import kotlinx.coroutines.flow.Flow

class FirestoreIncomeEntryRepository(
    private val service: FirestoreIncomeEntryService
) : IncomeEntryRepository {
    
    override suspend fun addIncomeEntry(entry: IncomeEntry): Result<String> {
        return service.addIncomeEntry(entry)
    }
    
    override suspend fun updateIncomeEntry(entry: IncomeEntry): Result<Unit> {
        return service.updateIncomeEntry(entry)
    }
    
    override suspend fun deleteIncomeEntry(entryId: String): Result<Unit> {
        return service.deleteIncomeEntry(entryId)
    }
    
    override fun getIncomeEntries(): Flow<List<IncomeEntry>> {
        return service.getIncomeEntries()
    }
    
    override fun getIncomeEntryById(entryId: String): Flow<IncomeEntry?> {
        return service.getIncomeEntryById(entryId)
    }
}
