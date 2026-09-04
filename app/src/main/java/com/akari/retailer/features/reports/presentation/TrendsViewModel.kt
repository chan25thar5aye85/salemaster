package com.akari.retailer.features.reports.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.data.repository.SaleRepository
import com.akari.retailer.features.sales.domain.models.Sale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class TrendsState(
    val dailyTotals: List<Int> = emptyList(),
    val dates: List<String> = emptyList(),
    val totalSales: Int = 0,
    val averageDaily: Int = 0,
    val highestDay: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null,
    val monthYear: String = ""
)

class TrendsViewModel(
    private val repository: SaleRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TrendsState())
    val state: StateFlow<TrendsState> = _state.asStateFlow()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            try {
                repository.getSales().collect { sales ->
                    val (totals, dates, monthYear) = calculateMonthlyTotals(sales)
                    val total = totals.sum()

                    _state.value = _state.value.copy(
                        dailyTotals = totals,
                        dates = dates,
                        monthYear = monthYear,
                        totalSales = total,
                        averageDaily = if (totals.isNotEmpty()) total / totals.size else 0,
                        highestDay = totals.maxOrNull() ?: 0,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load data"
                )
            }
        }
    }

    fun refresh() {
        loadData()
    }

    private fun calculateMonthlyTotals(sales: List<Sale>): Triple<List<Int>, List<String>, String> {
        val calendar = Calendar.getInstance()
        
        // Get first day of current month
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        val startDate = calendar.time
        val monthYear = displayFormat.format(startDate)
        
        // Get today
        val today = Calendar.getInstance()
        val todayDate = today.time
        
        // Get number of days in current month so far
        val daysInMonth = today.get(Calendar.DAY_OF_MONTH)
        
        // Initialize map for all days of the month
        val dailyMap = mutableMapOf<String, Int>()
        val dateList = mutableListOf<String>()
        
        val tempCalendar = Calendar.getInstance()
        tempCalendar.time = startDate
        
        for (i in 0 until daysInMonth) {
            val dateStr = dateFormat.format(tempCalendar.time)
            dailyMap[dateStr] = 0
            dateList.add(dateStr)
            tempCalendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        // Add sales to their corresponding days
        sales.forEach { sale ->
            val saleDate = Date(sale.timestamp)
            val saleDateString = dateFormat.format(saleDate)
            if (dailyMap.containsKey(saleDateString)) {
                dailyMap[saleDateString] = (dailyMap[saleDateString] ?: 0) + sale.total
            }
        }
        
        val totals = dateList.map { dailyMap[it] ?: 0 }
        return Triple(totals, dateList, monthYear)
    }
}
