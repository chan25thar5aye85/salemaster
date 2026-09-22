package com.akari.retailer.features.reports.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.data.repository.SaleRepository
import com.akari.retailer.features.sales.domain.models.Sale
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class SalesTimeRange {
    TODAY, THIS_WEEK, THIS_MONTH, LAST_MONTH, CUSTOM
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

    private var loadJob: Job? = null

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())

    init {
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
            SalesTimeRange.TODAY -> Triple(getDayStart(now), getDayEnd(now), "Today")
            SalesTimeRange.THIS_WEEK -> Triple(getWeekStart(now), getWeekEnd(now), "This Week")
            SalesTimeRange.THIS_MONTH -> Triple(getMonthStart(now), getMonthEnd(now), "This Month")
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
                Triple(start, end, displayFormat.format(Date(start)))
            }
        }

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

    private fun getDayStart(t: Long): Long = Calendar.getInstance().apply {
        timeInMillis = t; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun getDayEnd(t: Long): Long = Calendar.getInstance().apply {
        timeInMillis = t; set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
    }.timeInMillis

    private fun getWeekStart(t: Long): Long = Calendar.getInstance().apply {
        timeInMillis = t; set(Calendar.DAY_OF_WEEK, firstDayOfWeek); set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun getWeekEnd(t: Long): Long = Calendar.getInstance().apply {
        timeInMillis = t; set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
        add(Calendar.DAY_OF_WEEK, 6); set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
    }.timeInMillis

    private fun getMonthStart(t: Long): Long = Calendar.getInstance().apply {
        timeInMillis = t; set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun getMonthEnd(t: Long): Long = Calendar.getInstance().apply {
        timeInMillis = t; set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
        set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
    }.timeInMillis

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
        val now = Calendar.getInstance()
        // Guard against navigating into the future
        if (calendar.get(Calendar.YEAR) > now.get(Calendar.YEAR) ||
            (calendar.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
             calendar.get(Calendar.MONTH) > now.get(Calendar.MONTH))) return
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
