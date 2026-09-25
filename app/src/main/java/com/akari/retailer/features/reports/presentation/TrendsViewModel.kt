package com.akari.retailer.features.reports.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.data.repository.SaleRepository
import com.akari.retailer.features.sales.domain.models.Sale
import com.akari.retailer.core.ui.components.TimeFilter
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class TrendsState(
    val dailyTotals: List<Int> = emptyList(),
    val dates: List<String> = emptyList(),
    val totalSales: Int = 0,
    val averageDaily: Int = 0,
    val highestDay: Int = 0,
    val salesCount: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null,
    val timeFilter: TimeFilter = TimeFilter(),
    val rangeLabel: String = "This Month"
)

class TrendsViewModel(
    private val repository: SaleRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TrendsState())
    val state: StateFlow<TrendsState> = _state.asStateFlow()

    private var loadJob: Job? = null

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())

    init {
        loadData()
    }

    fun setTimeFilter(filter: TimeFilter) {
        _state.value = _state.value.copy(timeFilter = filter)
        loadData()
    }

    fun loadData() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                repository.getSales().collect { sales ->
                    val result = calculateData(sales)
                    _state.value = _state.value.copy(
                        dailyTotals = result.dailyTotals,
                        dates = result.dates,
                        totalSales = result.totalSales,
                        averageDaily = result.averageDaily,
                        highestDay = result.highestDay,
                        salesCount = result.salesCount,
                        rangeLabel = result.rangeLabel,
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

    private data class CalculatedData(
        val dailyTotals: List<Int>,
        val dates: List<String>,
        val totalSales: Int,
        val averageDaily: Int,
        val highestDay: Int,
        val salesCount: Int,
        val rangeLabel: String
    )

    private fun calculateData(sales: List<Sale>): CalculatedData {
        val rawRange = _state.value.timeFilter.resolveRange()
        // Defensive cap: never iterate more than 1 year of days in the chart.
        val oneYearMs = 366L * 24 * 60 * 60 * 1000
        val capEnd = System.currentTimeMillis()
        val startDate = maxOf(rawRange.first, minOf(rawRange.last, capEnd) - oneYearMs)
        val endDate = minOf(rawRange.last, capEnd)
        val label = _state.value.timeFilter.label

        val filteredSales = sales.filter { it.timestamp in startDate..endDate }

        val dailyMap = mutableMapOf<String, Int>()
        val dateList = mutableListOf<String>()

        val tempCal = Calendar.getInstance().apply { timeInMillis = startDate }
        while (tempCal.timeInMillis <= endDate) {
            val dateStr = dateFormat.format(tempCal.time)
            dailyMap[dateStr] = 0
            dateList.add(dateStr)
            tempCal.add(Calendar.DAY_OF_YEAR, 1)
        }

        filteredSales.forEach { sale ->
            val dateStr = dateFormat.format(Date(sale.timestamp))
            if (dailyMap.containsKey(dateStr)) {
                dailyMap[dateStr] = (dailyMap[dateStr] ?: 0) + sale.total
            }
        }

        val totals = dateList.map { dailyMap[it] ?: 0 }
        val totalSales = filteredSales.sumOf { it.total }
        val averageDaily = if (dateList.isNotEmpty()) totalSales / dateList.size else 0

        return CalculatedData(
            dailyTotals = totals,
            dates = dateList,
            totalSales = totalSales,
            averageDaily = averageDaily,
            highestDay = totals.maxOrNull() ?: 0,
            salesCount = filteredSales.size,
            rangeLabel = label
        )
    }



}

class TrendsViewModelFactory(
    private val repository: SaleRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TrendsViewModel::class.java)) {
            return TrendsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
