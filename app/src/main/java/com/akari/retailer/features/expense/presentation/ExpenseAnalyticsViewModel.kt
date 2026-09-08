package com.akari.retailer.features.expense.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.features.expense.data.repository.CategoryRepository
import com.akari.retailer.features.expense.data.repository.ExpenseRepository
import com.akari.retailer.features.expense.domain.models.Expense
import com.akari.retailer.features.expense.domain.models.ExpenseCategory
import com.akari.retailer.features.expense.domain.models.ExpenseType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ExpenseAnalyticsViewModel(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ExpenseAnalyticsState())
    val state: StateFlow<ExpenseAnalyticsState> = _state.asStateFlow()

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
                    expenseRepository.getExpenses(),
                    categoryRepository.getCategories()
                ) { expenses, categories ->
                    calculateAnalytics(expenses, categories)
                }.collect { analytics ->
                    _state.value = _state.value.copy(
                        totalExpenses = analytics.totalExpenses,
                        categorySpending = analytics.categorySpending,
                        topCategory = analytics.topCategory,
                        monthlyAverage = analytics.monthlyAverage,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load analytics"
                )
            }
        }
    }

    private fun calculateAnalytics(
        expenses: List<Expense>,
        categories: List<ExpenseCategory>
    ): ExpenseAnalyticsState {
        val timeRange = _state.value.timeRange
        val typeFilter = _state.value.typeFilter  // ✅ NEW
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        
        // Calculate date range
        val (startDate, endDate, label) = when (timeRange) {
            AnalyticsTimeRange.TODAY -> {
                val start = getDayStart(now)
                val end = getDayEnd(now)
                Triple(start, end, "Today")
            }
            AnalyticsTimeRange.THIS_WEEK -> {
                val start = getWeekStart(now)
                val end = getWeekEnd(now)
                Triple(start, end, "This Week")
            }
            AnalyticsTimeRange.THIS_MONTH -> {
                val start = getMonthStart(now)
                val end = getMonthEnd(now)
                Triple(start, end, "This Month")
            }
            AnalyticsTimeRange.LAST_MONTH -> {
                calendar.add(Calendar.MONTH, -1)
                val start = getMonthStart(calendar.timeInMillis)
                val end = getMonthEnd(calendar.timeInMillis)
                calendar.add(Calendar.MONTH, 1)
                val monthName = SimpleDateFormat("MMMM", Locale.getDefault()).format(Date(start))
                Triple(start, end, "Last Month ($monthName)")
            }
            AnalyticsTimeRange.CUSTOM -> {
                val start = _state.value.customStartDate ?: getMonthStart(now)
                val end = _state.value.customEndDate ?: getMonthEnd(now)
                val label = "${dateFormat.format(Date(start))} - ${dateFormat.format(Date(end))}"
                Triple(start, end, label)
            }
        }
        
        _state.value = _state.value.copy(rangeLabel = label)
        
        // ✅ Filter by date range AND expense type
        val filteredExpenses = expenses.filter { expense ->
            // Date filter
            val dateMatch = expense.date in startDate..endDate
            
            // Type filter
            val typeMatch = when (typeFilter) {
                ExpenseTypeFilter.ALL -> true
                ExpenseTypeFilter.BUSINESS -> expense.type == ExpenseType.BUSINESS
                ExpenseTypeFilter.PERSONAL -> expense.type == ExpenseType.PERSONAL
                ExpenseTypeFilter.MIXED -> expense.type == ExpenseType.MIXED
            }
            
            dateMatch && typeMatch
        }
        
        val total = filteredExpenses.sumOf { it.amount }
        
        // Group by category
        val categoryMap = mutableMapOf<ExpenseCategory, Pair<Int, Int>>()
        
        filteredExpenses.forEach { expense ->
            val category = categories.find { it.id == expense.categoryId }
                ?: categories.find { it.id == "default_other" }
                ?: return@forEach
            
            val current = categoryMap[category] ?: (0 to 0)
            categoryMap[category] = (current.first + expense.amount) to (current.second + 1)
        }
        
        val categorySpending = categoryMap.map { (category, data) ->
            CategorySpending(
                category = category,
                totalSpent = data.first,
                count = data.second,
                percentage = if (total > 0) (data.first.toDouble() / total) * 100 else 0.0
            )
        }.sortedByDescending { it.totalSpent }
        
        val topCategory = categorySpending.firstOrNull()
        val monthlyAverage = if (filteredExpenses.isNotEmpty()) total / filteredExpenses.size else 0
        
        return ExpenseAnalyticsState(
            totalExpenses = total,
            categorySpending = categorySpending,
            topCategory = topCategory,
            monthlyAverage = monthlyAverage,
            selectedMonth = _state.value.selectedMonth,
            selectedYear = _state.value.selectedYear,
            timeRange = timeRange,
            typeFilter = typeFilter,
            customStartDate = _state.value.customStartDate,
            customEndDate = _state.value.customEndDate,
            rangeLabel = label
        )
    }

    // Date helper functions
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

    fun setTimeRange(range: AnalyticsTimeRange) {
        _state.value = _state.value.copy(timeRange = range)
        loadAnalytics()
    }

    fun setTypeFilter(filter: ExpenseTypeFilter) {  // ✅ NEW
        _state.value = _state.value.copy(typeFilter = filter)
        loadAnalytics()
    }

    fun setCustomDateRange(start: Long, end: Long) {
        _state.value = _state.value.copy(
            timeRange = AnalyticsTimeRange.CUSTOM,
            customStartDate = start,
            customEndDate = end
        )
        loadAnalytics()
    }

    fun changeMonth(month: Int, year: Int) {
        _state.value = _state.value.copy(selectedMonth = month, selectedYear = year)
        val calendar = Calendar.getInstance().apply {
            set(year, month, 1)
        }
        val start = getMonthStart(calendar.timeInMillis)
        val end = getMonthEnd(calendar.timeInMillis)
        setCustomDateRange(start, end)
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

class ExpenseAnalyticsViewModelFactory(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpenseAnalyticsViewModel::class.java)) {
            return ExpenseAnalyticsViewModel(expenseRepository, categoryRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
