package com.akari.retailer.features.sales.presentation.entry

import com.akari.retailer.features.sales.domain.models.PaymentMethod
import com.akari.retailer.features.sales.domain.models.Sale

class SaleEntryStateManager {

    companion object {
        private const val FIRST_ROW_ID = 1L
    }

    private var nextRowId = FIRST_ROW_ID + 1

    fun updateAmount(state: SaleEntryState, rowId: Long, value: String): SaleEntryState {
        val digitsOnly = value.filter { it.isDigit() }
        val parsed = if (digitsOnly.isEmpty()) {
            null
        } else {
            digitsOnly.toIntOrNull()
        }

        val updatedRows = state.rows.map { row ->
            if (row.id == rowId) {
                row.copy(amount = parsed)
            } else {
                row
            }
        }

        return state.copy(rows = updatedRows)
    }

    fun focusRow(state: SaleEntryState, rowId: Long): SaleEntryState {
        val updatedRows = state.rows.map { row ->
            row.copy(isFocused = row.id == rowId)
        }
        return state.copy(rows = updatedRows)
    }

    fun addRow(state: SaleEntryState): SaleEntryState {
        val newRow = SaleItemRow(
            id = nextRowId++,
            isFocused = true
        )

        val updatedRows = state.rows.map {
            it.copy(isFocused = false)
        }.toMutableList()

        updatedRows.add(newRow)

        return state.copy(rows = updatedRows)
    }

    fun nextRow(state: SaleEntryState, rowId: Long): SaleEntryState {
        val currentIndex = state.rows.indexOfFirst { it.id == rowId }
        if (currentIndex == -1) return state

        return if (currentIndex < state.rows.lastIndex) {
            val nextId = state.rows[currentIndex + 1].id
            focusRow(state, nextId)
        } else {
            addRow(state)
        }
    }

    fun deleteRow(state: SaleEntryState, rowId: Long): SaleEntryState {
        if (state.rows.size <= 1) return state

        val index = state.rows.indexOfFirst { it.id == rowId }
        if (index == -1) return state

        val updatedRows = state.rows.toMutableList()
        updatedRows.removeAt(index)

        val focusIndex = when {
            index - 1 >= 0 -> index - 1
            index < updatedRows.size -> index
            else -> 0
        }

        val focusedRows = updatedRows.mapIndexed { i, row ->
            row.copy(isFocused = i == focusIndex)
        }

        return state.copy(rows = focusedRows)
    }

    fun selectPaymentMethod(state: SaleEntryState, method: PaymentMethod): SaleEntryState {
        return state.copy(paymentMethod = method)
    }

    fun resetState(keepRecentSales: List<Sale> = emptyList()): SaleEntryState {
        nextRowId = FIRST_ROW_ID + 1
        return SaleEntryState(
            rows = listOf(SaleItemRow(id = FIRST_ROW_ID, isFocused = true)),
            recentSales = keepRecentSales
        )
    }

    fun resetState(): SaleEntryState {
        nextRowId = FIRST_ROW_ID + 1
        return SaleEntryState(
            rows = listOf(SaleItemRow(id = FIRST_ROW_ID, isFocused = true))
        )
    }

    fun getItems(state: SaleEntryState): List<Int> {
        return state.rows.mapNotNull { it.amount }.filter { it > 0 }
    }

    fun setSaving(state: SaleEntryState, isSaving: Boolean): SaleEntryState {
        return state.copy(isSaving = isSaving)
    }

    fun setError(state: SaleEntryState, error: String?): SaleEntryState {
        return state.copy(error = error)
    }

    fun setSaveSuccess(state: SaleEntryState, success: Boolean): SaleEntryState {
        return state.copy(saveSuccess = success)
    }
}
