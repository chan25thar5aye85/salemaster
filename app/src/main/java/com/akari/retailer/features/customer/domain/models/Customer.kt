package com.akari.retailer.features.customer.domain.models

data class Customer(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val totalSpent: Int = 0,
    val totalOrders: Int = 0,
    val creditBalance: Int = 0,
    val lastOrderDate: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val notes: String = ""
) {
    fun getDisplayName(): String = name.ifEmpty { "Unknown Customer" }
    
    fun getPhoneDisplay(): String = phone.ifEmpty { "No phone" }
    
    fun getTotalSpentFormatted(): String = totalSpent.toString()

    /**
     * Customer owes us money (positive balance).
     */
    fun owesCredit(): Boolean = creditBalance > 0

    /**
     * We owe the customer money (negative balance) — refund, overpayment, etc.
     */
    fun weOweCustomer(): Boolean = creditBalance < 0

    /**
     * Nothing outstanding in either direction.
     */
    fun isSettled(): Boolean = creditBalance == 0

    /**
     * Absolute value — for display when we don't care about direction.
     */
    fun getBalanceAbsolute(): Int = kotlin.math.abs(creditBalance)

    fun getCreditBalanceFormatted(): String = creditBalance.toString()
}
