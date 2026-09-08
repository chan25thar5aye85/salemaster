package com.akari.retailer.features.expense.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.akari.retailer.R
import com.akari.retailer.RetailApplication
import com.akari.retailer.core.ui.components.AppCard
import com.akari.retailer.core.ui.components.AppScreen
import com.akari.retailer.core.ui.components.SearchBox
import com.akari.retailer.core.ui.theme.AppTypography
import com.akari.retailer.core.ui.theme.Spacing
import com.akari.retailer.features.expense.data.repository.FirestoreCategoryRepository
import com.akari.retailer.features.expense.data.repository.FirestoreExpenseRepository
import com.akari.retailer.features.expense.data.remote.FirestoreCategoryService
import com.akari.retailer.features.expense.data.remote.FirestoreExpenseService
import com.akari.retailer.features.expense.domain.models.ExpenseCategory
import com.akari.retailer.features.expense.domain.models.ExpenseType
import com.akari.retailer.navigation.Routes
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ExpenseListScreen(
    navController: NavController,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as RetailApplication
    
    val expenseService = remember { FirestoreExpenseService() }
    val expenseRepository = remember { FirestoreExpenseRepository(expenseService) }
    val categoryService = remember { FirestoreCategoryService() }
    val categoryRepository = remember { FirestoreCategoryRepository(categoryService) }
    
    val viewModel: ExpenseListViewModel = viewModel(
        factory = ExpenseListViewModelFactory(expenseRepository, categoryRepository)
    )
    
    val state by viewModel.state.collectAsState()
    
    var showDeleteDialog by remember { mutableStateOf(false) }
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }
    var showSearch by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog && pendingDeleteId != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                pendingDeleteId = null
            },
            title = { Text(stringResource(R.string.delete_expense)) },
            text = { Text(stringResource(R.string.delete_expense_confirmation)) },
            confirmButton = {
                Button(
                    onClick = {
                        pendingDeleteId?.let { viewModel.handleEvent(ExpenseListEvent.DeleteExpense(it)) }
                        showDeleteDialog = false
                        pendingDeleteId = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    pendingDeleteId = null
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    AppScreen(
        title = stringResource(R.string.expenses),
        showBackButton = true,
        onBackClick = onBack,
        showSearchButton = true,
        onSearchClick = { showSearch = !showSearch },
        showAddButton = true,
        onAddClick = { navController.navigate(Routes.EXPENSE_ADD) },
        showAnalyticsButton = true,
        onAnalyticsClick = { navController.navigate(Routes.EXPENSE_ANALYTICS) },
        showFilterButton = true,
        onFilterClick = { showFilterDialog = true }
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Manage Categories Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.medium)
                    .clickable { navController.navigate(Routes.CATEGORIES) },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.medium),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚙️ Manage Categories",
                        style = AppTypography.body
                    )
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Manage Categories",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Filter status bar
            if (state.selectedCategoryIds.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = Spacing.medium),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.medium),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🔍", fontSize = 16.sp)
                            Text(
                                text = "${state.selectedCategoryIds.size} category${if (state.selectedCategoryIds.size > 1) "ies" else ""} selected",
                                style = AppTypography.small,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        TextButton(
                            onClick = { viewModel.handleEvent(ExpenseListEvent.ClearCategoryFilters) }
                        ) {
                            Text("Clear")
                        }
                    }
                }
            }

            if (showSearch) {
                SearchBox(
                    query = state.searchQuery,
                    onQueryChange = { 
                        viewModel.handleEvent(ExpenseListEvent.SearchQueryChanged(it))
                    },
                    onSearch = {},
                    placeholder = stringResource(R.string.search_expenses)
                )
            }

            if (state.allExpenses.isNotEmpty()) {
                AppCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.total_expenses),
                            style = AppTypography.body,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "${state.totalExpenses}",
                            style = AppTypography.header,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (state.isLoading && state.expenses.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Text(
                            text = stringResource(R.string.loading_expenses),
                            style = AppTypography.body,
                            modifier = Modifier.padding(top = Spacing.medium)
                        )
                    }
                }
                return@Column
            }

            if (state.error != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "❌",
                            fontSize = 40.sp
                        )
                        Text(
                            text = state.error!!,
                            style = AppTypography.body,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = Spacing.medium)
                        )
                        TextButton(
                            onClick = {
                                viewModel.handleEvent(ExpenseListEvent.LoadExpenses)
                            }
                        ) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
                return@Column
            }

            if (state.expenses.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (state.searchQuery.isNotEmpty()) "🔍" else "💳",
                            fontSize = 48.sp
                        )
                        Text(
                            text = if (state.searchQuery.isNotEmpty()) 
                                "${stringResource(R.string.no_expenses_found)} '${state.searchQuery}'"
                            else 
                                stringResource(R.string.no_expenses_yet),
                            style = AppTypography.header,
                            modifier = Modifier.padding(top = Spacing.medium)
                        )
                        Text(
                            text = if (state.searchQuery.isNotEmpty()) 
                                stringResource(R.string.try_different_search)
                            else 
                                stringResource(R.string.tap_add_expense),
                            style = AppTypography.body,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                return@Column
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.medium),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (state.searchQuery.isNotEmpty()) 
                        "${state.expenses.size} ${stringResource(R.string.results)}" 
                    else 
                        "${state.expenses.size} ${stringResource(R.string.expenses)}",
                    style = AppTypography.label,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                if (state.searchQuery.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            viewModel.handleEvent(ExpenseListEvent.ClearSearch)
                            showSearch = false
                        }
                    ) {
                        Text(stringResource(R.string.clear))
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                contentPadding = PaddingValues(bottom = Spacing.xxlarge)
            ) {
                items(
                    items = state.expenses,
                    key = { it.id }
                ) { expense ->
                    ExpenseCard(
                        expense = expense,
                        categories = state.categories,
                        onDelete = {
                            pendingDeleteId = expense.id
                            showDeleteDialog = true
                        },
                        onClick = {
                            navController.navigate(Routes.EXPENSE_DETAIL.replace("{expenseId}", expense.id))
                        }
                    )
                }
            }
        }
    }

    // Filter Dialog
    if (showFilterDialog) {
        ExpenseFilterDialog(
            categories = state.categories,
            selectedCategories = state.selectedCategoryIds,
            onCategoryToggle = { categoryId ->
                viewModel.handleEvent(ExpenseListEvent.ToggleCategoryFilter(categoryId))
            },
            onClearAll = {
                viewModel.handleEvent(ExpenseListEvent.ClearCategoryFilters)
            },
            onApply = {
                showFilterDialog = false
            },
            onDismiss = {
                showFilterDialog = false
            }
        )
    }
}

@Composable
fun ExpenseCard(
    expense: com.akari.retailer.features.expense.domain.models.Expense,
    categories: List<ExpenseCategory>,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val category = categories.find { it.id == expense.categoryId }
    val categoryName = category?.name ?: "Other"
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Title
                Text(
                    text = expense.title,
                    style = AppTypography.title
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                // Category and Type badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Category badge
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = categoryName,
                            style = AppTypography.small,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    
                    // ✅ Type badge
                    Surface(
                        color = when (expense.type) {
                            ExpenseType.BUSINESS -> MaterialTheme.colorScheme.primaryContainer
                            ExpenseType.PERSONAL -> MaterialTheme.colorScheme.secondaryContainer
                            ExpenseType.MIXED -> MaterialTheme.colorScheme.tertiaryContainer
                        },
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = when (expense.type) {
                                ExpenseType.BUSINESS -> "💼 Business"
                                ExpenseType.PERSONAL -> "👤 Personal"
                                ExpenseType.MIXED -> "🔄 ${expense.businessPercentage}%"
                            },
                            style = AppTypography.small,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(2.dp))
                
                // Date
                Text(
                    text = dateFormat.format(expense.date),
                    style = AppTypography.small,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            
            // Amount
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${expense.amount}",
                    style = AppTypography.header,
                    color = MaterialTheme.colorScheme.error
                )
                
                // Show business amount if different
                if (expense.type == ExpenseType.MIXED) {
                    Text(
                        text = "Business: ${expense.getBusinessAmount()}",
                        style = AppTypography.small,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                )
            }
        }
    }
}
