package com.akari.retailer.features.expense.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.features.expense.data.repository.CategoryRepository
import com.akari.retailer.features.expense.domain.models.ExpenseCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CategoryManagementViewModel(
    private val repository: CategoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CategoryManagementState())
    val state: StateFlow<CategoryManagementState> = _state.asStateFlow()

    init {
        loadCategories()
    }

    fun handleEvent(event: CategoryManagementEvent) {
        when (event) {
            is CategoryManagementEvent.LoadCategories -> loadCategories()
            is CategoryManagementEvent.RefreshCategories -> loadCategories()
            is CategoryManagementEvent.DeleteCategory -> deleteCategory(event.categoryId)
            is CategoryManagementEvent.ClearError -> clearError()
            is CategoryManagementEvent.SelectTab -> selectTab(event.tabIndex)
            is CategoryManagementEvent.ShowAddDialog -> showAddDialog()
            is CategoryManagementEvent.ShowEditDialog -> showEditDialog(event.category)
            is CategoryManagementEvent.DismissDialog -> dismissDialog()
            is CategoryManagementEvent.DialogNameChanged -> updateDialogName(event.name)
            is CategoryManagementEvent.SaveCategory -> saveCategory()
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                repository.getCategories().collect { categories ->
                    val defaultCats = categories.filter { it.isDefault }
                    val customCats = categories.filter { !it.isDefault }
                    
                    _state.value = _state.value.copy(
                        categories = categories,
                        defaultCategories = defaultCats,
                        customCategories = customCats,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load categories"
                )
            }
        }
    }

    private fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val result = repository.deleteCategory(categoryId)
                if (result.isSuccess) {
                    loadCategories()
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to delete category"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to delete category"
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
            editingCategory = null,
            dialogName = "",
            error = null
        )
    }

    private fun showEditDialog(category: ExpenseCategory) {
        _state.value = _state.value.copy(
            showDialog = true,
            editingCategory = category,
            dialogName = category.name,
            error = null
        )
    }

    private fun dismissDialog() {
        _state.value = _state.value.copy(
            showDialog = false,
            editingCategory = null,
            dialogName = "",
            error = null,
            isSaving = false
        )
    }

    private fun updateDialogName(name: String) {
        _state.value = _state.value.copy(dialogName = name)
    }

    private fun saveCategory() {
        val name = _state.value.dialogName.trim()
        
        if (name.isBlank()) {
            _state.value = _state.value.copy(error = "Category name is required")
            return
        }
        
        if (name.length < 2) {
            _state.value = _state.value.copy(error = "Name must be at least 2 characters")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, error = null)
            
            try {
                val editing = _state.value.editingCategory
                
                if (editing != null) {
                    // Update existing category
                    val updated = editing.copy(
                        name = name,
                        updatedAt = System.currentTimeMillis()
                    )
                    val result = repository.updateCategory(updated)
                    if (result.isSuccess) {
                        dismissDialog()
                        loadCategories()
                    } else {
                        _state.value = _state.value.copy(
                            isSaving = false,
                            error = result.exceptionOrNull()?.message ?: "Failed to update category"
                        )
                    }
                } else {
                    // Add new category
                    val newCategory = ExpenseCategory(
                        name = name,
                        isDefault = false
                    )
                    val result = repository.addCategory(newCategory)
                    if (result.isSuccess) {
                        dismissDialog()
                        loadCategories()
                    } else {
                        _state.value = _state.value.copy(
                            isSaving = false,
                            error = result.exceptionOrNull()?.message ?: "Failed to add category"
                        )
                    }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isSaving = false,
                    error = e.message ?: "Failed to save category"
                )
            }
        }
    }

    private fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun getFilteredCategories(): List<ExpenseCategory> {
        return when (_state.value.selectedTab) {
            0 -> _state.value.categories
            1 -> _state.value.defaultCategories
            2 -> _state.value.customCategories
            else -> _state.value.categories
        }
    }
}
