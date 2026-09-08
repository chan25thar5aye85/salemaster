package com.akari.retailer.features.expense.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akari.retailer.R
import com.akari.retailer.RetailApplication
import com.akari.retailer.core.ui.components.AppPrimaryButton
import com.akari.retailer.core.ui.components.AppScreen
import com.akari.retailer.core.ui.theme.AppTypography
import com.akari.retailer.core.ui.theme.Spacing
import com.akari.retailer.features.expense.data.repository.FirestoreCategoryRepository
import com.akari.retailer.features.expense.data.repository.FirestoreExpenseRepository
import com.akari.retailer.features.expense.data.remote.FirestoreCategoryService
import com.akari.retailer.features.expense.data.remote.FirestoreExpenseService
import com.akari.retailer.features.expense.domain.models.ExpenseCategory
import com.akari.retailer.features.expense.domain.models.ExpenseType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseAddScreen(
    onBack: () -> Unit,
    onExpenseAdded: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as RetailApplication
    
    val expenseService = remember { FirestoreExpenseService() }
    val expenseRepository = remember { FirestoreExpenseRepository(expenseService) }
    val categoryService = remember { FirestoreCategoryService() }
    val categoryRepository = remember { FirestoreCategoryRepository(categoryService) }
    
    val viewModel: ExpenseAddViewModel = viewModel(
        factory = ExpenseAddViewModelFactory(expenseRepository, categoryRepository)
    )
    
    val state by viewModel.state.collectAsState()

    AppScreen(
        title = stringResource(R.string.add_expense),
        showBackButton = true,
        onBackClick = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Title
            OutlinedTextField(
                value = state.title,
                onValueChange = { viewModel.handleEvent(ExpenseAddEvent.TitleChanged(it)) },
                label = { Text(stringResource(R.string.title)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            // Amount
            OutlinedTextField(
                value = state.amount,
                onValueChange = { viewModel.handleEvent(ExpenseAddEvent.AmountChanged(it)) },
                label = { Text(stringResource(R.string.amount)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            // Category Dropdown
            var categoryExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = it }
            ) {
                OutlinedTextField(
                    value = state.selectedCategory?.name ?: "Select Category",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.category)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) }
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    state.categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.name) },
                            onClick = {
                                viewModel.handleEvent(ExpenseAddEvent.CategorySelected(category))
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            // Expense Type Selector
            Text(
                text = "Expense Type",
                style = AppTypography.label,
                modifier = Modifier.padding(bottom = Spacing.small)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                ExpenseTypeButton(
                    type = ExpenseType.BUSINESS,
                    label = "💼 Business",
                    isSelected = state.expenseType == ExpenseType.BUSINESS,
                    onClick = { viewModel.handleEvent(ExpenseAddEvent.ExpenseTypeChanged(ExpenseType.BUSINESS)) },
                    modifier = Modifier.weight(1f)
                )
                ExpenseTypeButton(
                    type = ExpenseType.PERSONAL,
                    label = "👤 Personal",
                    isSelected = state.expenseType == ExpenseType.PERSONAL,
                    onClick = { viewModel.handleEvent(ExpenseAddEvent.ExpenseTypeChanged(ExpenseType.PERSONAL)) },
                    modifier = Modifier.weight(1f)
                )
                ExpenseTypeButton(
                    type = ExpenseType.MIXED,
                    label = "🔄 Mixed",
                    isSelected = state.expenseType == ExpenseType.MIXED,
                    onClick = { viewModel.handleEvent(ExpenseAddEvent.ExpenseTypeChanged(ExpenseType.MIXED)) },
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Business Percentage (only for MIXED)
            if (state.expenseType == ExpenseType.MIXED) {
                Spacer(modifier = Modifier.height(Spacing.small))
                OutlinedTextField(
                    value = state.businessPercentage,
                    onValueChange = { 
                        if (it.isEmpty() || it.toIntOrNull()?.let { it in 0..100 } == true) {
                            viewModel.handleEvent(ExpenseAddEvent.BusinessPercentageChanged(it))
                        }
                    },
                    label = { Text("Business % (0-100)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Text(
                    text = "Percentage of this expense that is business-related",
                    style = AppTypography.small,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            // Description
            OutlinedTextField(
                value = state.description,
                onValueChange = { viewModel.handleEvent(ExpenseAddEvent.DescriptionChanged(it)) },
                label = { Text(stringResource(R.string.description)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            )
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            state.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = AppTypography.body,
                    modifier = Modifier.padding(bottom = Spacing.medium)
                )
            }
            
            AppPrimaryButton(
                text = if (state.isSaving) stringResource(R.string.saving) else stringResource(R.string.save_expense),
                onClick = {
                    viewModel.handleEvent(ExpenseAddEvent.SaveExpense)
                },
                isLoading = state.isSaving,
                enabled = state.title.isNotEmpty() && state.amount.isNotEmpty() && !state.isSaving
            )
            
            if (state.saveSuccess) {
                LaunchedEffect(Unit) {
                    onExpenseAdded()
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.xxlarge))
        }
    }
}

@Composable
fun ExpenseTypeButton(
    type: ExpenseType,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isSelected) 
                MaterialTheme.colorScheme.onPrimary 
            else 
                MaterialTheme.colorScheme.onSurface
        )
    ) {
        Text(label)
    }
}
