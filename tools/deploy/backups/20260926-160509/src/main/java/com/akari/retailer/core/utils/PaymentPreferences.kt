package com.akari.retailer.core.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Shared preferences for remembering last used payment account.
 * Used across Expense, Income, Sale, Purchase entry screens.
 *
 * Instance-based (not object) so it can be injected via AppContainer
 * and mocked in tests.
 */
class PaymentPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Last used payment account (used by Expense / Income / Sale / Purchase) ──
    // NOTE: We intentionally ignore the stored value and always return the
    // default (Cash) so every new payment row starts at Cash. The stored
    // value is still written by setLastUsedAccountId — flip the read back to
    // `prefs.getString(...)` if you want the "remember" behaviour returned.
    fun getLastUsedAccountId(): String = DEFAULT_ACCOUNT_ID

    @Suppress("unused")
    fun setLastUsedAccountId(accountId: String) {
        prefs.edit().putString(KEY_LAST_ACCOUNT, accountId).apply()
    }

    // ── External-transfer last used account (separate key) ──
    fun getLastExternalTransferAccountId(): String =
        prefs.getString(KEY_LAST_EXTERNAL_ACCOUNT, "") ?: ""

    fun setLastExternalTransferAccountId(accountId: String) {
        prefs.edit().putString(KEY_LAST_EXTERNAL_ACCOUNT, accountId).apply()
    }

    private companion object {
        const val PREFS_NAME = "payment_prefs"
        const val KEY_LAST_ACCOUNT = "last_used_account_id"
        const val KEY_LAST_EXTERNAL_ACCOUNT = "last_external_transfer_account_id"
        const val DEFAULT_ACCOUNT_ID = "default_cash"
    }
}
