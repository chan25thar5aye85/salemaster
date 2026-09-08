package com.akari.retailer.features.sales.presentation.income

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.features.sales.data.repository.IncomeEntryRepository
import com.akari.retailer.features.sales.data.repository.IncomeStreamRepository
import com.akari.retailer.features.sales.domain.models.IncomeEntry
import com.akari.retailer.features.sales.domain.models.IncomeEntryType
import com.akari.retailer.features.sales.domain.models.IncomeStream
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class IncomeAnalyticsViewModel(
    private val entryRepository: IncomeEntryRepository,
    private val streamRepository: IncomeStreamRepository
) : ViewModel() {

    private val _state = MutableStateFlow(IncomeAnalyticsState())
    val state: StateFlow<IncomeAnalyticsState> = _state.asStateFlow()

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    init {
        loadAnalytics()
    }

    fun loadAnalytics() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            try {
                val calendar = Calendar.getInstance()
                _state.value = _state.value.copy(
                    selectedMonth = calendar.get(Calendar.MONTH),
                    selectedYear = calendar.get(Calendar.YEAR)
                )
                
                combine(
                    entryRepository.getIncomeEntries(),
                    streamRepository.getIncomeStreams()
                ) { entries, streams ->
                    calculateAnalytics(entries, streams)
                }.collect { analytics ->
                    _state.value = _state.value.copy(
                        totalIncome = analytics.totalIncome,
                        businessIncome = analytics.businessIncome,
                        personalIncome = analytics.personalIncome,
                        streamSpending = analytics.streamSpending,
                        topStream = analytics.topStream,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load income analytics"
                )
            }
        }
    }

    private fun calculateAnalytics(
        entries: List<IncomeEntry>,
        streams: List<IncomeStream>
    ): IncomeAnalyticsState {
        val timeRange = _state.value.timeRange
        val typeFilter = _state.value.typeFilter
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        
        val (startDate, endDate, label) = when (timeRange) {
            IncomeTimeRange.TODAY -> {
                val start = getDayStart(now)
                val end = getDayEnd(now)
                Triple(start, end, "Today")
            }
            IncomeTimeRange.THIS_WEEK -> {
                val start = getWeekStart(now)
                val end = getWeekEnd(now)
                Triple(start, end, "This Week")
            }
            IncomeTimeRange.THIS_MONTH -> {
                val start = getMonthStart(now)
                val end = getMonthEnd(now)
                Triple(start, end, "This Month")
            }
            IncomeTimeRange.LAST_MONTH -> {
                calendar.add(Calendar.MONTH, -1)
                val start = getMonthStart(calendar.timeInMillis)
                val end = getMonthEnd(calendar.timeInMillis)
                calendar.add(Calendar.MONTH, 1)
                val monthName = SimpleDateFormat("MMMM", Locale.getDefault()).format(Date(start))
                Triple(start, end, "Last Month ($monthName)")
            }
            IncomeTimeRange.CUSTOM -> {
                val start = _state.value.selectedMonth.let { 
                    calendar.set(_state.value.selectedYear, it, 1)
                    getMonthStart(calendar.timeInMillis)
                }
                val end = _state.value.selectedMonth.let {
                    calendar.set(_state.value.selectedYear, it, 1)
                    getMonthEnd(calendar.timeInMillis)
                }
                val label = "${dateFormat.format(Date(start))} - ${dateFormat.format(Date(end))}"
                Triple(start, end, label)
            }
        }
        
        _state.value = _state.value.copy(rangeLabel = label)
        
        val filteredEntries = entries.filter { entry ->
            val dateMatch = entry.date in startDate..endDate
            val typeMatch = when (typeFilter) {
                IncomeTypeFilter.ALL -> true
                IncomeTypeFilter.BUSINESS -> entry.type == IncomeEntryType.BUSINESS
                IncomeTypeFilter.PERSONAL -> entry.type == IncomeEntryType.PERSONAL
            }
            dateMatch && typeMatch
        }
        
        val total = filteredEntries.sumOf { it.amount }
        val business = filteredEntries.filter { it.type == IncomeEntryType.BUSINESS }.sumOf { it.amount }
        val personal = filteredEntries.filter { it.type == IncomeEntryType.PERSONAL }.sumOf { it.amount }
        
        val streamMap = mutableMapOf<IncomeStream, Pair<Int, Int>>()
        
        filteredEntries.forEach { entry ->
            val stream = streams.find { it.id == entry.incomeStreamId }
                ?: streams.find { it.id == "default_product_sales" }
                ?: return@forEach
            
            val current = streamMap[stream] ?: (0 to 0)
            streamMap[stream] = (current.first + entry.amount) to (current.second + 1)
        }
        
        val streamSpending = streamMap.map { (stream, data) ->
            IncomeStreamSpending(
                stream = stream,
                totalIncome = data.first,
                count = data.second,
                percentage = if (total > 0) (data.first.toDouble() / total) * 100 else 0.0
            )
        }.sortedByDescending { it.totalIncome }
        
        val topStream = streamSpending.firstOrNull()
        
        return IncomeAnalyticsState(
            totalIncome = total,
            businessIncome = business,
            personalIncome = personal,
            streamSpending = streamSpending,
            topStream = topStream,
            selectedMonth = _state.value.selectedMonth,
            selectedYear = _state.value.selectedYear,
            timeRange = timeRange,
            typeFilter = typeFilter,
            rangeLabel = label
        )
    }

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

    fun setTimeRange(range: IncomeTimeRange) {
        _state.value = _state.value.copy(timeRange = range)
        loadAnalytics()
    }

    fun setTypeFilter(filter: IncomeTypeFilter) {
        _state.value = _state.value.copy(typeFilter = filter)
        loadAnalytics()
    }

    fun changeMonth(month: Int, year: Int) {
        _state.value = _state.value.copy(
            selectedMonth = month, 
            selectedYear = year,
            timeRange = IncomeTimeRange.CUSTOM
        )
        loadAnalytics()
    }

    fun previousMonth() {
        val calendar = Calendar.getInstance()
        calendar.set(_state.value.selectedYear, _state.value.selectedMonth, 1)
        calendar.add(Calendar.MONTH, -1)
        changeMonth(calendar.get(Calendar.MONTH), calendar.get(Calendar.YEAR))
    }

    fun nextMonth() {
        val calendar = Calendar.getInstance()
        calendar.set(_state.value.selectedYear, _state.value.selectedMonth, 1)
        calendar.add(Calendar.MONTH, 1)
        changeMonth(calendar.get(Calendar.MONTH), calendar.get(Calendar.YEAR))
    }
}

class IncomeAnalyticsViewModelFactory(
    private val entryRepository: IncomeEntryRepository,
    private val streamRepository: IncomeStreamRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(IncomeAnalyticsViewModel::class.java)) {
            return IncomeAnalyticsViewModel(entryRepository, streamRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
