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
import java.util.*

enum class SalesTimeRange {
    TODAY,
    THIS_WEEK,
    THIS_MONTH,
    LAST_MONTH,
    CUSTOM
}

data class TrendsState(
    val dailyTotals: List<Int> = emptyList(),
    val dates: List<String> = emptyList(),
    val totalSales: Int = 0,
    val averageDaily: Int = 0,
    val highestDay: Int = 0,
    val salesCount: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null,
    val monthYear: String = "",
    val selectedMonth: Int = 0,
    val selectedYear: Int = 0,
    val timeRange: SalesTimeRange = SalesTimeRange.THIS_MONTH,
    val rangeLabel: String = "This Month"
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
                    val result = calculateData(sales)
                    _state.value = _state.value.copy(
                        dailyTotals = result.dailyTotals,
                        dates = result.dates,
                        totalSales = result.totalSales,
                        averageDaily = result.averageDaily,
                        highestDay = result.highestDay,
                        salesCount = result.salesCount,
                        monthYear = result.rangeLabel,
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
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        
        val (startDate, endDate, label) = when (_state.value.timeRange) {
            SalesTimeRange.TODAY -> {
                val start = getDayStart(now)
                val end = getDayEnd(now)
                Triple(start, end, "Today")
            }
            SalesTimeRange.THIS_WEEK -> {
                val start = getWeekStart(now)
                val end = getWeekEnd(now)
                Triple(start, end, "This Week")
            }
            SalesTimeRange.THIS_MONTH -> {
                val start = getMonthStart(now)
                val end = getMonthEnd(now)
                Triple(start, end, "This Month")
            }
            SalesTimeRange.LAST_MONTH -> {
                calendar.add(Calendar.MONTH, -1)
                val start = getMonthStart(calendar.timeInMillis)
                val end = getMonthEnd(calendar.timeInMillis)
                calendar.add(Calendar.MONTH, 1)
                val monthName = displayFormat.format(Date(start))
                Triple(start, end, "Last Month ($monthName)")
            }
            SalesTimeRange.CUSTOM -> {
                calendar.set(_state.value.selectedYear, _state.value.selectedMonth, 1)
                val start = getMonthStart(calendar.timeInMillis)
                val end = getMonthEnd(calendar.timeInMillis)
                val label = displayFormat.format(Date(start))
                Triple(start, end, label)
            }
        }
        
        _state.value = _state.value.copy(rangeLabel = label)
        
        // Filter sales by date range
        val filteredSales = sales.filter { it.timestamp in startDate..endDate }
        
        // Group by day
        val dailyMap = mutableMapOf<String, Int>()
        val dateList = mutableListOf<String>()
        
        // Initialize all days in range
        val tempCal = Calendar.getInstance().apply { timeInMillis = startDate }
        while (tempCal.timeInMillis <= endDate) {
            val dateStr = dateFormat.format(tempCal.time)
            dailyMap[dateStr] = 0
            dateList.add(dateStr)
            tempCal.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        // Add sales to days
        filteredSales.forEach { sale ->
            val dateStr = dateFormat.format(Date(sale.timestamp))
            if (dailyMap.containsKey(dateStr)) {
                dailyMap[dateStr] = (dailyMap[dateStr] ?: 0) + sale.total
            }
        }
        
        val totals = dateList.map { dailyMap[it] ?: 0 }
        val totalSales = filteredSales.sumOf { it.total }
        val averageDaily = if (datesIsNotEmpty(dateList)) totalSales / dateList.size else 0
        val highestDay = totals.maxOrNull() ?: 0
        
        return CalculatedData(
            dailyTotals = totals,
            dates = dateList,
            totalSales = totalSales,
            averageDaily = averageDaily,
            highestDay = highestDay,
            salesCount = filteredSales.size,
            rangeLabel = label
        )
    }

    private fun datesIsNotEmpty(dates: List<String>): Boolean = dates.isNotEmpty()

    private fun getDayStart(timestamp: Long): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getDayEnd(timestamp: Long): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }

    private fun getWeekStart(timestamp: Long): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getWeekEnd(timestamp: Long): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        calendar.add(Calendar.DAY_OF_WEEK, 6)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }

    private fun getMonthStart(timestamp: Long): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getMonthEnd(timestamp: Long): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }

    fun setTimeRange(range: SalesTimeRange) {
        val calendar = Calendar.getInstance()
        _state.value = _state.value.copy(
            timeRange = range,
            selectedMonth = calendar.get(Calendar.MONTH),
            selectedYear = calendar.get(Calendar.YEAR)
        )
        loadData()
    }

    fun previousMonth() {
        val calendar = Calendar.getInstance()
        calendar.set(_state.value.selectedYear, _state.value.selectedMonth, 1)
        calendar.add(Calendar.MONTH, -1)
        _state.value = _state.value.copy(
            timeRange = SalesTimeRange.CUSTOM,
            selectedMonth = calendar.get(Calendar.MONTH),
            selectedYear = calendar.get(Calendar.YEAR)
        )
        loadData()
    }

    fun nextMonth() {
        val calendar = Calendar.getInstance()
        calendar.set(_state.value.selectedYear, _state.value.selectedMonth, 1)
        calendar.add(Calendar.MONTH, 1)
        _state.value = _state.value.copy(
            timeRange = SalesTimeRange.CUSTOM,
            selectedMonth = calendar.get(Calendar.MONTH),
            selectedYear = calendar.get(Calendar.YEAR)
        )
        loadData()
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
