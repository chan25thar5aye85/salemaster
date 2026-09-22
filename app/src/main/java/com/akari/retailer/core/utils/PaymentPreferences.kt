package com.akari.retailer.core.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Shared preferences for remembering last used payment account.
 * Used across Expense, Income, Sale, Purchase entry screens.
 */
object PaymentPreferences {
    private const val PREFS_NAME = "payment_prefs"
    private const val KEY_LAST_ACCOUNT = "last_used_account_id"
    private const val DEFAULT_ACCOUNT_ID = "default_cash"
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    fun getLastUsedAccountId(context: Context): String {
        return getPrefs(context).getString(KEY_LAST_ACCOUNT, DEFAULT_ACCOUNT_ID) 
            ?: DEFAULT_ACCOUNT_ID
    }
    
    fun setLastUsedAccountId(context: Context, accountId: String) {
        getPrefs(context).edit().putString(KEY_LAST_ACCOUNT, accountId).apply()
    }
}
