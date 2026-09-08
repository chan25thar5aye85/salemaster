package com.akari.retailer.features.sales.presentation.stream

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.features.sales.data.repository.IncomeStreamRepository
import com.akari.retailer.features.sales.domain.models.IncomeStream
import com.akari.retailer.features.sales.domain.models.IncomeType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class IncomeStreamManagementViewModel(
    private val repository: IncomeStreamRepository
) : ViewModel() {

    private val _state = MutableStateFlow(IncomeStreamManagementState())
    val state: StateFlow<IncomeStreamManagementState> = _state.asStateFlow()

    init {
        loadStreams()
    }

    fun handleEvent(event: IncomeStreamManagementEvent) {
        when (event) {
            is IncomeStreamManagementEvent.LoadStreams -> loadStreams()
            is IncomeStreamManagementEvent.RefreshStreams -> loadStreams()
            is IncomeStreamManagementEvent.DeleteStream -> deleteStream(event.streamId)
            is IncomeStreamManagementEvent.ClearError -> clearError()
            is IncomeStreamManagementEvent.SelectTab -> selectTab(event.tabIndex)
            is IncomeStreamManagementEvent.ShowAddDialog -> showAddDialog()
            is IncomeStreamManagementEvent.ShowEditDialog -> showEditDialog(event.stream)
            is IncomeStreamManagementEvent.DismissDialog -> dismissDialog()
            is IncomeStreamManagementEvent.DialogNameChanged -> updateDialogName(event.name)
            is IncomeStreamManagementEvent.SaveStream -> saveStream()
        }
    }

    private fun loadStreams() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                repository.getIncomeStreams().collect { streams ->
                    val defaultStreams = streams.filter { it.isDefault }
                    val customStreams = streams.filter { !it.isDefault }
                    
                    _state.value = _state.value.copy(
                        streams = streams,
                        defaultStreams = defaultStreams,
                        customStreams = customStreams,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load income streams"
                )
            }
        }
    }

    private fun deleteStream(streamId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val result = repository.deleteIncomeStream(streamId)
                if (result.isSuccess) {
                    loadStreams()
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to delete income stream"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to delete income stream"
                )
            }
        }
    }

    private fun selectTab(tabIndex: Int) {
        _state.value = _state.value.copy(selectedTab = tabIndex)
    }

    private fun showAddDialog() {
        _state.value = _state.value.copy(
            showDialog = true,
            editingStream = null,
            dialogName = "",
            error = null
        )
    }

    private fun showEditDialog(stream: IncomeStream) {
        _state.value = _state.value.copy(
            showDialog = true,
            editingStream = stream,
            dialogName = stream.name,
            error = null
        )
    }

    private fun dismissDialog() {
        _state.value = _state.value.copy(
            showDialog = false,
            editingStream = null,
            dialogName = "",
            error = null,
            isSaving = false
        )
    }

    private fun updateDialogName(name: String) {
        _state.value = _state.value.copy(dialogName = name)
    }

    private fun saveStream() {
        val name = _state.value.dialogName.trim()
        
        if (name.isBlank()) {
            _state.value = _state.value.copy(error = "Income stream name is required")
            return
        }
        
        if (name.length < 2) {
            _state.value = _state.value.copy(error = "Name must be at least 2 characters")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, error = null)
            
            try {
                val editing = _state.value.editingStream
                
                if (editing != null) {
                    val updated = editing.copy(
                        name = name,
                        updatedAt = System.currentTimeMillis()
                    )
                    val result = repository.updateIncomeStream(updated)
                    if (result.isSuccess) {
                        dismissDialog()
                        loadStreams()
                    } else {
                        _state.value = _state.value.copy(
                            isSaving = false,
                            error = result.exceptionOrNull()?.message ?: "Failed to update income stream"
                        )
                    }
                } else {
                    val newStream = IncomeStream(
                        name = name,
                        type = IncomeType.OTHER,
                        icon = "💰",
                        color = "#636E72",
                        isDefault = false
                    )
                    val result = repository.addIncomeStream(newStream)
                    if (result.isSuccess) {
                        dismissDialog()
                        loadStreams()
                    } else {
                        _state.value = _state.value.copy(
                            isSaving = false,
                            error = result.exceptionOrNull()?.message ?: "Failed to add income stream"
                        )
                    }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isSaving = false,
                    error = e.message ?: "Failed to save income stream"
                )
            }
        }
    }

    private fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun getFilteredStreams(): List<IncomeStream> {
        return when (_state.value.selectedTab) {
            0 -> _state.value.streams
            1 -> _state.value.defaultStreams
            2 -> _state.value.customStreams
            else -> _state.value.streams
        }
    }
}

class IncomeStreamManagementViewModelFactory(
    private val repository: IncomeStreamRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(IncomeStreamManagementViewModel::class.java)) {
            return IncomeStreamManagementViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
