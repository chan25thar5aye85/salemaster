package com.akari.retailer.features.money.data.repository

import com.akari.retailer.features.money.data.remote.FirestoreMoneyTransactionService
import com.akari.retailer.features.money.domain.models.MoneyTransaction
import kotlinx.coroutines.flow.Flow

class FirestoreMoneyTransactionRepository(
    private val service: FirestoreMoneyTransactionService
) : MoneyTransactionRepository {

    override suspend fun addTransaction(transaction: MoneyTransaction): Result<String> {
        return service.addTransaction(transaction)
    }

    override fun getTransactions(): Flow<List<MoneyTransaction>> {
        return service.getTransactions()
    }

    override fun getTransactionsForAccount(accountId: String): Flow<List<MoneyTransaction>> {
        return service.getTransactionsForAccount(accountId)
    }

    override fun getExternalTransfers(): Flow<List<MoneyTransaction>> {
        return service.getExternalTransfers()
    }
}
