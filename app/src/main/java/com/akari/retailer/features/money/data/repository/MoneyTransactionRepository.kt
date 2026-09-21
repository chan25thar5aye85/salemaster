package com.akari.retailer.features.money.data.repository

import com.akari.retailer.features.money.domain.models.MoneyTransaction
import kotlinx.coroutines.flow.Flow

interface MoneyTransactionRepository {
    suspend fun addTransaction(transaction: MoneyTransaction): Result<String>
    fun getTransactions(): Flow<List<MoneyTransaction>>
    fun getTransactionsForAccount(accountId: String): Flow<List<MoneyTransaction>>
}
