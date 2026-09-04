package com.akari.retailer.features.sales.presentation.entry

class SaleEntryValidator {

    fun validateItems(items: List<Int>): ValidationResult {
        if (items.isEmpty()) {
            return ValidationResult.Error("Add at least one item")
        }
        return ValidationResult.Success
    }

    fun validateAmount(value: String): Boolean {
        if (value.isEmpty()) return true
        val parsed = value.toIntOrNull()
        return parsed != null && parsed > 0
    }

    sealed class ValidationResult {
        data object Success : ValidationResult()
        data class Error(val message: String) : ValidationResult()
    }
}
