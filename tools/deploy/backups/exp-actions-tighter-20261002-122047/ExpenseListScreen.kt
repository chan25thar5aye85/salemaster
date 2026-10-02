package com.akari.retailer.features.expense.presentation

import com.akari.retailer.core.ui.components.TimeFilterPreset
import com.akari.retailer.core.ui.components.TimeFilterButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import com.akari.retailer.core.utils.MoneyFormatter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Search
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
    
    val viewModel: ExpenseListViewModel = viewModel(
        factory = ExpenseListViewModelFactory(
            application.container.expenseRepository,
            application.container.categoryRepository,
            application.container.supplierRepository,
            application.container.expenseFinalizer
        )
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
        onBackClick = onBack
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // ── Actions row ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.small),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Manage categories
                IconButton(onClick = { navController.navigate(Routes.CATEGORIES) }) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Manage Categories"
                    )
                }

                // Time filter (dropdown)
                TimeFilterButton(
                    filter = state.timeFilter,
                    onPresetChange = { viewModel.handleEvent(ExpenseListEvent.TimeFilterChanged(it)) },
                    onPickSpecificDay = { timestamp ->
                        viewModel.handleEvent(
                            ExpenseListEvent.TimeFilterChanged(
                                state.timeFilter.copy(
                                    preset = TimeFilterPreset.SPECIFIC_DAY,
                                    specificDayMillis = timestamp
                                )
                            )
                        )
                    },
                    onPickCustomRange = { start, end ->
                        viewModel.handleEvent(
                            ExpenseListEvent.TimeFilterChanged(
                                state.timeFilter.copy(
                                    preset = TimeFilterPreset.CUSTOM_RANGE,
                                    customStartMillis = start,
                                    customEndMillis = end
                                )
                            )
                        )
                    }
                )

                Spacer(modifier = Modifier.weight(1f))

                // Search
                IconButton(onClick = { showSearch = !showSearch }) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search"
                    )
                }

                // Analytics
                IconButton(onClick = { navController.navigate(Routes.EXPENSE_ANALYTICS) }) {
                    Icon(
                        Icons.Default.Analytics,
                        contentDescription = "Analytics"
                    )
                }

                // Add
                IconButton(onClick = { navController.navigate(Routes.EXPENSE_ADD) }) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add Expense"
                    )
                }
            }

            // ── Summary card ──
            if (state.allExpenses.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = Spacing.medium),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.medium)
                    ) {
                        // Row 1: total
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = MoneyFormatter.format(state.totalExpenses),
                                style = AppTypography.header,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(Spacing.small))
                            Text(
                                text = "· ${state.expenses.size} ${stringResource(R.string.expenses).lowercase()}",
                                style = AppTypography.body,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }

                        // Row 2: type breakdown
                        if (state.businessTotal > 0 ||
                            state.personalTotal > 0 ||
                            state.mixedTotal > 0
                        ) {
                            Spacer(modifier = Modifier.height(Spacing.small))
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                            )
                            Spacer(modifier = Modifier.height(Spacing.small))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                TypeChip(
                                    icon = "💼",
                                    label = "Business",
                                    amount = state.businessTotal
                                )
                                TypeChip(
                                    icon = "👤",
                                    label = "Personal",
                                    amount = state.personalTotal
                                )
                                TypeChip(
                                    icon = "🔄",
                                    label = "Mixed",
                                    amount = state.mixedTotal
                                )
                            }
                        }

                        // Row 3: top category
                        if (state.topCategoryName.isNotBlank() && state.topCategoryAmount > 0) {
                            Spacer(modifier = Modifier.height(Spacing.small))
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                            )
                            Spacer(modifier = Modifier.height(Spacing.small))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "🏆 Top  ",
                                        style = AppTypography.small,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = state.topCategoryName,
                                        style = AppTypography.body,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Text(
                                    text = MoneyFormatter.format(state.topCategoryAmount),
                                    style = AppTypography.body,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
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
                        suppliers = state.suppliers,
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
            timeFilter = state.timeFilter,
            onTimeFilterChange = {
                viewModel.handleEvent(ExpenseListEvent.TimeFilterChanged(it))
            },
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
    suppliers: List<com.akari.retailer.features.supplier.domain.models.Supplier> = emptyList(),
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val category = categories.find { it.id == expense.categoryId }
    val categoryName = category?.name ?: stringResource(R.string.uncategorized)
    val supplierName = if (expense.supplierId.isNotBlank()) {
        suppliers.find { it.id == expense.supplierId }?.name
    } else null
    
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
                .padding(horizontal = Spacing.medium, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Row 1: category name (left) + amount (right)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = categoryName,
                        style = AppTypography.title,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${expense.amount}",
                        style = AppTypography.title,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Row 2: supplier · date · type (compact, muted)
                val metaParts = buildList {
                    if (supplierName != null) add("🏢 $supplierName")
                    add("📅 " + dateFormat.format(expense.date))
                    add(
                        when (expense.type) {
                            ExpenseType.BUSINESS -> "💼"
                            ExpenseType.PERSONAL -> "👤"
                            ExpenseType.MIXED -> "🔄 ${expense.businessPercentage}%"
                        }
                    )
                }

                Text(
                    text = metaParts.joinToString("  ·  "),
                    style = AppTypography.small,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    maxLines = 1
                )

                if (expense.type == ExpenseType.MIXED) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Business portion: ${expense.getBusinessAmount()}",
                        style = AppTypography.small,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}


@Composable
private fun TypeChip(
    icon: String,
    label: String,
    amount: Int
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$icon ${MoneyFormatter.format(amount)}",
            style = AppTypography.body,
            fontWeight = FontWeight.Medium,
            color = if (amount > 0)
                MaterialTheme.colorScheme.onSurface
            else
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
        Text(
            text = label,
            style = AppTypography.small,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            fontSize = 11.sp
        )
    }
}
