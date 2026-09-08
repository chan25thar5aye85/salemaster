package com.akari.retailer.features.expense.presentation

import android.graphics.Color
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akari.retailer.R
import com.akari.retailer.RetailApplication
import com.akari.retailer.core.ui.components.AppCard
import com.akari.retailer.core.ui.components.AppScreen
import com.akari.retailer.core.ui.theme.AppTypography
import com.akari.retailer.core.ui.theme.Spacing
import com.akari.retailer.features.expense.data.repository.FirestoreCategoryRepository
import com.akari.retailer.features.expense.data.repository.FirestoreExpenseRepository
import com.akari.retailer.features.expense.data.remote.FirestoreCategoryService
import com.akari.retailer.features.expense.data.remote.FirestoreExpenseService
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseAnalyticsScreen(
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val application = context.applicationContext as RetailApplication
    
    val expenseService = remember { FirestoreExpenseService() }
    val expenseRepository = remember { FirestoreExpenseRepository(expenseService) }
    val categoryService = remember { FirestoreCategoryService() }
    val categoryRepository = remember { FirestoreCategoryRepository(categoryService) }
    
    val viewModel: ExpenseAnalyticsViewModel = viewModel(
        factory = ExpenseAnalyticsViewModelFactory(expenseRepository, categoryRepository)
    )
    
    val state by viewModel.state.collectAsState()
    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    
    var showRangeMenu by remember { mutableStateOf(false) }

    // Current month/year for comparison
    val currentCalendar = Calendar.getInstance()
    val currentMonth = currentCalendar.get(Calendar.MONTH)
    val currentYear = currentCalendar.get(Calendar.YEAR)
    
    // Chart colors
    val chartColors = listOf(
        "#4CAF50", "#FF5722", "#2196F3", "#FFC107", "#9C27B0",
        "#00BCD4", "#FF9800", "#795548", "#607D8B", "#E91E63",
        "#8BC34A", "#3F51B5", "#FF6F00", "#00E676", "#D500F9"
    ).map { Color.parseColor(it) }

    AppScreen(
        title = "📊 Expense Analytics",
        showBackButton = true,
        onBackClick = onBack
    ) {
        // ✅ Make the entire screen scrollable
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            if (state.error != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("❌", fontSize = 40.sp)
                        Text(
                            text = state.error!!,
                            style = AppTypography.body,
                            color = MaterialTheme.colorScheme.error
                        )
                        TextButton(
                            onClick = { viewModel.loadAnalytics() }
                        ) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
                return@Column
            }

            // ✅ Time Range Selector - Default to Today
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.medium)
                ) {
                    // Range selector row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Month navigation arrows
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { viewModel.previousMonth() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Previous", modifier = Modifier.size(20.dp))
                            }
                            
                            Text(
                                text = monthFormat.format(
                                    Calendar.getInstance()
                                        .apply { set(state.selectedYear, state.selectedMonth, 1) }
                                        .time
                                ),
                                style = AppTypography.title,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            
                            val isFutureMonth = state.selectedYear > currentYear || 
                                (state.selectedYear == currentYear && state.selectedMonth > currentMonth)
                            
                            IconButton(
                                onClick = { viewModel.nextMonth() },
                                enabled = !isFutureMonth,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.ArrowForward, 
                                    contentDescription = "Next",
                                    modifier = Modifier.size(20.dp),
                                    tint = if (isFutureMonth) 
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f) 
                                    else 
                                        MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        
                        // Right: Time range dropdown - Default to Today
                        ExposedDropdownMenuBox(
                            expanded = showRangeMenu,
                            onExpandedChange = { showRangeMenu = it }
                        ) {
                            OutlinedButton(
                                onClick = { showRangeMenu = true },
                                modifier = Modifier.menuAnchor()
                            ) {
                                Text(
                                    text = state.rangeLabel,
                                    style = AppTypography.small
                                )
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = "Select range",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            
                            ExposedDropdownMenu(
                                expanded = showRangeMenu,
                                onDismissRequest = { showRangeMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("📅 Today") },
                                    onClick = {
                                        viewModel.setTimeRange(AnalyticsTimeRange.TODAY)
                                        showRangeMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("📅 This Week") },
                                    onClick = {
                                        viewModel.setTimeRange(AnalyticsTimeRange.THIS_WEEK)
                                        showRangeMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("📅 This Month") },
                                    onClick = {
                                        viewModel.setTimeRange(AnalyticsTimeRange.THIS_MONTH)
                                        showRangeMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("📅 Last Month") },
                                    onClick = {
                                        viewModel.setTimeRange(AnalyticsTimeRange.LAST_MONTH)
                                        showRangeMenu = false
                                    }
                                )
                            }
                        }
                    }
                    
                    // Current range label
                    Text(
                        text = "Showing: ${state.rangeLabel}",
                        style = AppTypography.small,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            // ✅ Expense Type Filter Buttons
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.medium)
                ) {
                    Text(
                        text = "Filter by Type",
                        style = AppTypography.label,
                        modifier = Modifier.padding(bottom = Spacing.small)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TypeFilterButton(
                            label = "All",
                            icon = "📊",
                            isSelected = state.typeFilter == ExpenseTypeFilter.ALL,
                            onClick = { viewModel.setTypeFilter(ExpenseTypeFilter.ALL) },
                            modifier = Modifier.weight(1f)
                        )
                        TypeFilterButton(
                            label = "Business",
                            icon = "💼",
                            isSelected = state.typeFilter == ExpenseTypeFilter.BUSINESS,
                            onClick = { viewModel.setTypeFilter(ExpenseTypeFilter.BUSINESS) },
                            modifier = Modifier.weight(1f)
                        )
                        TypeFilterButton(
                            label = "Personal",
                            icon = "👤",
                            isSelected = state.typeFilter == ExpenseTypeFilter.PERSONAL,
                            onClick = { viewModel.setTypeFilter(ExpenseTypeFilter.PERSONAL) },
                            modifier = Modifier.weight(1f)
                        )
                        TypeFilterButton(
                            label = "Mixed",
                            icon = "🔄",
                            isSelected = state.typeFilter == ExpenseTypeFilter.MIXED,
                            onClick = { viewModel.setTypeFilter(ExpenseTypeFilter.MIXED) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            // Summary Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
            ) {
                AppCard(modifier = Modifier.weight(1f)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (state.totalExpenses > 0) "${state.totalExpenses}" else "0",
                            style = AppTypography.header,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Total Spent",
                            style = AppTypography.small,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                AppCard(modifier = Modifier.weight(1f)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (state.monthlyAverage > 0) "${state.monthlyAverage}" else "0",
                            style = AppTypography.header,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Avg per day",
                            style = AppTypography.small,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            // ✅ Pie Chart
            if (state.categorySpending.isNotEmpty()) {
                AppCard {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "📊 Spending by Category",
                            style = AppTypography.title,
                            modifier = Modifier.padding(bottom = Spacing.small)
                        )
                        
                        AndroidView(
                            factory = { context ->
                                PieChart(context).apply {
                                    description.isEnabled = false
                                    setTouchEnabled(true)
                                    setUsePercentValues(true)
                                    setEntryLabelTextSize(12f)
                                    setEntryLabelColor(Color.BLACK)
                                    legend.isEnabled = true
                                    legend.textColor = Color.DKGRAY
                                    legend.textSize = 12f
                                    setDrawEntryLabels(true)
                                    animateY(1000)
                                    animateX(1000)
                                }
                            },
                            update = { pieChart ->
                                val entries = state.categorySpending.map { spending ->
                                    PieEntry(
                                        spending.totalSpent.toFloat(),
                                        spending.category.name
                                    )
                                }
                                
                                val dataSet = PieDataSet(entries, "Categories").apply {
                                    colors = chartColors
                                    valueTextColor = Color.BLACK
                                    valueTextSize = 14f
                                    setDrawValues(true)
                                    valueFormatter = PercentFormatter()
                                }
                                
                                pieChart.data = PieData(dataSet)
                                pieChart.invalidate()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.medium))

                // Top Category
                state.topCategory?.let { top ->
                    AppCard {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "🏆 Top Category",
                                style = AppTypography.title,
                                modifier = Modifier.padding(bottom = Spacing.small)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${top.category.name}",
                                    style = AppTypography.body
                                )
                                Text(
                                    text = "${top.totalSpent} (${String.format("%.1f", top.percentage)}%)",
                                    style = AppTypography.body,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            LinearProgressIndicator(
                                progress = (top.percentage / 100).toFloat(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = Spacing.small)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.medium))

                // ✅ Bar Chart
                AppCard {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "📊 Category Comparison",
                            style = AppTypography.title,
                            modifier = Modifier.padding(bottom = Spacing.small)
                        )
                        
                        AndroidView(
                            factory = { context ->
                                BarChart(context).apply {
                                    description.isEnabled = false
                                    setTouchEnabled(true)
                                    setDragEnabled(true)
                                    setScaleEnabled(true)
                                    setPinchZoom(true)
                                    
                                    legend.isEnabled = true
                                    legend.textColor = Color.DKGRAY
                                    legend.textSize = 10f
                                    
                                    xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                                    xAxis.setDrawGridLines(false)
                                    xAxis.textColor = Color.DKGRAY
                                    xAxis.textSize = 10f
                                    xAxis.granularity = 1f
                                    
                                    axisLeft.textColor = Color.DKGRAY
                                    axisLeft.textSize = 10f
                                    axisLeft.setDrawGridLines(true)
                                    axisLeft.gridColor = Color.LTGRAY
                                    axisLeft.axisMinimum = 0f
                                    
                                    axisRight.isEnabled = false
                                    animateY(1000)
                                }
                            },
                            update = { barChart ->
                                val entries = state.categorySpending.mapIndexed { index, spending ->
                                    BarEntry(index.toFloat(), spending.totalSpent.toFloat())
                                }
                                
                                val dataSet = BarDataSet(entries, "Spending").apply {
                                    colors = chartColors
                                    valueTextColor = Color.DKGRAY
                                    valueTextSize = 10f
                                    setDrawValues(true)
                                }
                                
                                val barData = BarData(dataSet).apply {
                                    barWidth = 0.6f
                                }
                                
                                barChart.data = barData
                                
                                val labels = state.categorySpending.map { it.category.name }
                                barChart.xAxis.valueFormatter = 
                                    com.github.mikephil.charting.formatter.IndexAxisValueFormatter(labels)
                                
                                barChart.invalidate()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            // Category Breakdown List
            Text(
                text = "📋 Category Details",
                style = AppTypography.title,
                modifier = Modifier.padding(bottom = Spacing.small)
            )

            if (state.categorySpending.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = Spacing.large),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "📭",
                            fontSize = 48.sp
                        )
                        Text(
                            text = "No expenses for this period",
                            style = AppTypography.body,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = Spacing.medium)
                        )
                        Text(
                            text = "Try changing the time range or type filter",
                            style = AppTypography.small,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
                return@Column
            }

            // ✅ Category list with limited height
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                items(state.categorySpending) { spending ->
                    CategorySpendingItem(spending = spending)
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xxlarge))
        }
    }
}

@Composable
fun TypeFilterButton(
    label: String,
    icon: String,
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
                MaterialTheme.colorScheme.surface,
            contentColor = if (isSelected) 
                MaterialTheme.colorScheme.onPrimary 
            else 
                MaterialTheme.colorScheme.onSurface
        ),
        shape = MaterialTheme.shapes.small
    ) {
        Text("$icon $label")
    }
}

@Composable
fun CategorySpendingItem(spending: CategorySpending) {
    AppCard {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${spending.category.name}",
                    style = AppTypography.body
                )
                Text(
                    text = "${spending.totalSpent}",
                    style = AppTypography.body,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${spending.count} transactions",
                    style = AppTypography.small,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = "${String.format("%.1f", spending.percentage)}%",
                    style = AppTypography.small,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            LinearProgressIndicator(
                progress = (spending.percentage / 100).toFloat(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.small),
                color = getColorForPercentage(spending.percentage)
            )
        }
    }
}

@Composable
fun getColorForPercentage(percentage: Double): ComposeColor {
    return when {
        percentage > 30 -> ComposeColor(0xFF4CAF50)
        percentage > 20 -> ComposeColor(0xFFFF9800)
        percentage > 10 -> ComposeColor(0xFF2196F3)
        else -> ComposeColor(0xFF9E9E9E)
    }
}
