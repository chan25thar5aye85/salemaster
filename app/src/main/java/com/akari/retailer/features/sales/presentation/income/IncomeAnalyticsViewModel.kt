package com.akari.retailer.features.sales.presentation.income

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.features.sales.data.repository.IncomeEntryRepository
import com.akari.retailer.core.ui.components.TimeFilter
import com.akari.retailer.features.sales.data.repository.IncomeStreamRepository
import com.akari.retailer.features.sales.domain.models.IncomeEntry
import com.akari.retailer.features.sales.domain.models.IncomeEntryType
import com.akari.retailer.features.sales.domain.models.IncomeStream
import kotlinx.coroutines.Job
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

    private var loadJob: Job? = null

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    init {
        loadAnalytics()
    }

    fun loadAnalytics() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
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
        val typeFilter = _state.value.typeFilter
        val range = _state.value.timeFilter.resolveRange()
        val startDate = range.first
        val endDate = range.last

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

        return IncomeAnalyticsState(
            totalIncome = total,
            businessIncome = business,
            personalIncome = personal,
            streamSpending = streamSpending,
            topStream = streamSpending.firstOrNull(),
            timeFilter = _state.value.timeFilter,
            typeFilter = typeFilter
        )
    }








    fun setTypeFilter(filter: IncomeTypeFilter) {
        _state.value = _state.value.copy(typeFilter = filter)
        loadAnalytics()
    }

    fun setTimeFilter(filter: TimeFilter) {
        _state.value = _state.value.copy(timeFilter = filter)
        loadAnalytics()
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
