package com.akari.retailer.features.sales.domain.models

enum class IncomeEntryType {
    BUSINESS,
    PERSONAL
}

data class IncomeEntry(
    val id: String = "",
    val amount: Int = 0,
    val incomeStreamId: String = "default_product_sales",
    val description: String = "",
    val type: IncomeEntryType = IncomeEntryType.BUSINESS,
    val date: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun getBusinessAmount(): Int {
        return when (type) {
            IncomeEntryType.BUSINESS -> amount
            IncomeEntryType.PERSONAL -> 0
        }
    }
    
    fun affectsProfit(): Boolean {
        return type == IncomeEntryType.BUSINESS
    }
}
