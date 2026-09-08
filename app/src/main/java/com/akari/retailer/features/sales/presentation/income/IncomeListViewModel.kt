package com.akari.retailer.features.sales.presentation.income

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.features.sales.data.repository.IncomeEntryRepository
import com.akari.retailer.features.sales.data.repository.IncomeStreamRepository
import com.akari.retailer.features.sales.domain.models.IncomeEntryType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class IncomeListState(
    val entries: List<com.akari.retailer.features.sales.domain.models.IncomeEntry> = emptyList(),
    val allEntries: List<com.akari.retailer.features.sales.domain.models.IncomeEntry> = emptyList(),
    val streams: List<com.akari.retailer.features.sales.domain.models.IncomeStream> = emptyList(),
    val selectedStreamIds: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val searchQuery: String = "",
    val totalIncome: Int = 0,
    val businessIncome: Int = 0,
    val personalIncome: Int = 0
)

sealed class IncomeListEvent {
    data object LoadEntries : IncomeListEvent()
    data object RefreshEntries : IncomeListEvent()
    data class DeleteEntry(val entryId: String) : IncomeListEvent()
    data object ClearError : IncomeListEvent()
    data class SearchQueryChanged(val query: String) : IncomeListEvent()
    data object ClearSearch : IncomeListEvent()
    data class ToggleStreamFilter(val streamId: String) : IncomeListEvent()
    data object ClearStreamFilters : IncomeListEvent()
}

class IncomeListViewModel(
    private val entryRepository: IncomeEntryRepository,
    private val streamRepository: IncomeStreamRepository
) : ViewModel() {

    private val _state = MutableStateFlow(IncomeListState())
    val state: StateFlow<IncomeListState> = _state.asStateFlow()

    init {
        loadEntries()
        loadStreams()
    }

    fun handleEvent(event: IncomeListEvent) {
        when (event) {
            is IncomeListEvent.LoadEntries -> loadEntries()
            is IncomeListEvent.RefreshEntries -> refreshEntries()
            is IncomeListEvent.DeleteEntry -> deleteEntry(event.entryId)
            is IncomeListEvent.ClearError -> clearError()
            is IncomeListEvent.SearchQueryChanged -> searchQueryChanged(event.query)
            is IncomeListEvent.ClearSearch -> clearSearch()
            is IncomeListEvent.ToggleStreamFilter -> toggleStreamFilter(event.streamId)
            is IncomeListEvent.ClearStreamFilters -> clearStreamFilters()
        }
    }

    private fun loadStreams() {
        viewModelScope.launch {
            try {
                streamRepository.getIncomeStreams().collect { streams ->
                    _state.value = _state.value.copy(streams = streams)
                }
            } catch (e: Exception) {
                // Handle silently
            }
        }
    }

    private fun loadEntries() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                entryRepository.getIncomeEntries().collect { entries ->
                    val total = entries.sumOf { it.amount }
                    val business = entries.filter { it.type == IncomeEntryType.BUSINESS }.sumOf { it.amount }
                    val personal = entries.filter { it.type == IncomeEntryType.PERSONAL }.sumOf { it.amount }
                    
                    _state.value = _state.value.copy(
                        allEntries = entries,
                        isLoading = false,
                        error = null,
                        totalIncome = total,
                        businessIncome = business,
                        personalIncome = personal
                    )
                    applyFilters()
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load income entries"
                )
            }
        }
    }

    private fun refreshEntries() {
        loadEntries()
    }

    private fun deleteEntry(entryId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val result = entryRepository.deleteIncomeEntry(entryId)
                if (result.isSuccess) {
                    loadEntries()
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to delete income entry"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to delete income entry"
                )
            }
        }
    }

    private fun toggleStreamFilter(streamId: String) {
        val currentSelected = _state.value.selectedStreamIds.toMutableSet()
        if (currentSelected.contains(streamId)) {
            currentSelected.remove(streamId)
        } else {
            currentSelected.add(streamId)
        }
        _state.value = _state.value.copy(selectedStreamIds = currentSelected.toSet())
        applyFilters()
    }

    private fun clearStreamFilters() {
        _state.value = _state.value.copy(selectedStreamIds = emptySet())
        applyFilters()
    }

    private fun searchQueryChanged(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        applyFilters()
    }

    private fun clearSearch() {
        _state.value = _state.value.copy(searchQuery = "")
        applyFilters()
    }

    private fun applyFilters() {
        val query = _state.value.searchQuery.lowercase().trim()
        val allEntries = _state.value.allEntries
        val streams = _state.value.streams
        val selectedStreamIds = _state.value.selectedStreamIds
        
        var filtered = allEntries
        
        if (selectedStreamIds.isNotEmpty()) {
            filtered = filtered.filter { entry ->
                selectedStreamIds.contains(entry.incomeStreamId)
            }
        }
        
        if (query.isNotEmpty()) {
            filtered = filtered.filter { entry ->
                entry.description.lowercase().contains(query) ||
                streams.find { it.id == entry.incomeStreamId }?.name?.lowercase()?.contains(query) == true
            }
        }
        
        _state.value = _state.value.copy(entries = filtered)
    }

    private fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}

class IncomeListViewModelFactory(
    private val entryRepository: IncomeEntryRepository,
    private val streamRepository: IncomeStreamRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(IncomeListViewModel::class.java)) {
            return IncomeListViewModel(entryRepository, streamRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
