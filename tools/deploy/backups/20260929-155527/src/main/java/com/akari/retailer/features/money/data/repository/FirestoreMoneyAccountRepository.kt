package com.akari.retailer.features.money.data.repository

import com.akari.retailer.features.money.data.remote.FirestoreMoneyService
import com.akari.retailer.features.money.domain.models.MoneyAccount
import kotlinx.coroutines.flow.Flow

class FirestoreMoneyAccountRepository(
    private val service: FirestoreMoneyService
) : MoneyAccountRepository {
    
    override suspend fun addAccount(account: MoneyAccount): Result<String> {
        return service.addAccount(account)
    }
    
    override suspend fun updateAccount(account: MoneyAccount): Result<Unit> {
        return service.updateAccount(account)
    }
    
    override suspend fun deleteAccount(accountId: String): Result<Unit> {
        return service.deleteAccount(accountId)
    }
    
    override fun getAccounts(): Flow<List<MoneyAccount>> {
        return service.getAccounts()
    }
    
    override fun getAccountById(accountId: String): Flow<MoneyAccount?> {
        return service.getAccountById(accountId)
    }
    
    override suspend fun getAccountByIdSync(accountId: String): MoneyAccount? {
        return service.getAccountByIdSync(accountId)
    }
    
    override suspend fun updateBalance(accountId: String, newBalance: Int): Result<Unit> {
        return service.updateBalance(accountId, newBalance)
    }
    
    override suspend fun adjustBalance(accountId: String, amount: Int): Result<Unit> {
        return service.adjustBalance(accountId, amount)
    }
    
    override suspend fun seedDefaultAccounts(): Result<Unit> {
        return service.seedDefaultAccounts()
    }

    override val firestore: com.google.firebase.firestore.FirebaseFirestore
        get() = com.google.firebase.firestore.FirebaseFirestore.getInstance()
}
