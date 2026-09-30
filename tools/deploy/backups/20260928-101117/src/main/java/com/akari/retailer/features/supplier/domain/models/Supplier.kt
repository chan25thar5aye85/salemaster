package com.akari.retailer.features.supplier.domain.models

data class Supplier(
    val id: String = "",
    val name: String = "",
    val company: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val products: List<String> = emptyList(),
    val totalPurchased: Int = 0,
    val payableBalance: Int = 0,
    val lastOrderDate: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val notes: String = ""
) {
    /** We owe the supplier (positive balance) */
    fun owesPayable(): Boolean = payableBalance > 0

    /** Supplier owes us (negative balance) — overpayment, return, etc. */
    fun supplierOwesUs(): Boolean = payableBalance < 0

    /** Nothing outstanding either way */
    fun isSettled(): Boolean = payableBalance == 0

    /** Absolute value of balance */
    fun getBalanceAbsolute(): Int = kotlin.math.abs(payableBalance)
}
