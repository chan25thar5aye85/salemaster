package com.akari.retailer.features.reports.presentation

import android.graphics.Color
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akari.retailer.R
import com.akari.retailer.RetailApplication
import com.akari.retailer.core.ui.components.AppCard
import com.akari.retailer.core.ui.components.AppScreen
import com.akari.retailer.core.ui.theme.AppTypography
import com.akari.retailer.core.ui.theme.Spacing
import com.akari.retailer.data.repository.FirestoreSaleRepository
import com.akari.retailer.data.remote.FirestoreService
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as RetailApplication
    val repository = FirestoreSaleRepository(FirestoreService())
    
    val viewModel: TrendsViewModel = viewModel(
        factory = TrendsViewModelFactory(repository)
    )
    
    val state by viewModel.state.collectAsState()
    val salesLabel = stringResource(R.string.sales)
    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    
    var showRangeMenu by remember { mutableStateOf(false) }

    val currentCalendar = Calendar.getInstance()
    val currentMonth = currentCalendar.get(Calendar.MONTH)
    val currentYear = currentCalendar.get(Calendar.YEAR)

    AppScreen(
        title = stringResource(R.string.monthly_sales_trend),
        showBackButton = true,
        onBackClick = onBack
    ) {
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Text(
                            text = stringResource(R.string.loading_sales_data),
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
                        Text(text = "❌", fontSize = 40.sp)
                        Text(
                            text = state.error!!,
                            style = AppTypography.body,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = Spacing.medium)
                        )
                    }
                }
                return@Column
            }

            // ✅ Time Range Selector
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Month navigation
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
                        
                        // Time range dropdown
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
                                        viewModel.setTimeRange(SalesTimeRange.TODAY)
                                        showRangeMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("📅 This Week") },
                                    onClick = {
                                        viewModel.setTimeRange(SalesTimeRange.THIS_WEEK)
                                        showRangeMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("📅 This Month") },
                                    onClick = {
                                        viewModel.setTimeRange(SalesTimeRange.THIS_MONTH)
                                        showRangeMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("📅 Last Month") },
                                    onClick = {
                                        viewModel.setTimeRange(SalesTimeRange.LAST_MONTH)
                                        showRangeMenu = false
                                    }
                                )
                            }
                        }
                    }
                    
                    Text(
                        text = "Showing: ${state.rangeLabel}",
                        style = AppTypography.small,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            // Summary Cards
            AppCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.small),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = state.rangeLabel,
                            style = AppTypography.label,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "${state.totalSales}",
                            style = AppTypography.header,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.avg_day),
                            style = AppTypography.label,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "${state.averageDaily}",
                            style = AppTypography.header,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.highest),
                            style = AppTypography.label,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "${state.highestDay}",
                            style = AppTypography.header,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            // Bar Chart
            if (state.dailyTotals.isNotEmpty()) {
                AppCard {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${state.rangeLabel} ${stringResource(R.string.sales)}",
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
                                    
                                    legend.isEnabled = true
                                    legend.textColor = Color.DKGRAY
                                    legend.textSize = 10f
                                    
                                    xAxis.position = XAxis.XAxisPosition.BOTTOM
                                    xAxis.setDrawGridLines(false)
                                    xAxis.textColor = Color.DKGRAY
                                    xAxis.textSize = 10f
                                    xAxis.granularity = 1f
                                    xAxis.labelCount = 5
                                    xAxis.labelRotationAngle = 0f
                                    
                                    axisLeft.textColor = Color.DKGRAY
                                    axisLeft.textSize = 10f
                                    axisLeft.setDrawGridLines(true)
                                    axisLeft.gridColor = Color.LTGRAY
                                    axisLeft.axisMinimum = 0f
                                    
                                    axisRight.isEnabled = false
                                }
                            },
                            update = { barChart ->
                                val entries = state.dailyTotals.mapIndexed { index, value ->
                                    BarEntry(index.toFloat(), value.toFloat())
                                }
                                
                                val dataSet = BarDataSet(entries, salesLabel).apply {
                                    color = Color.parseColor("#1976D2")
                                    valueTextColor = Color.DKGRAY
                                    valueTextSize = 10f
                                    setDrawValues(true)
                                }
                                
                                val barData = BarData(dataSet).apply {
                                    barWidth = 0.6f
                                }
                                
                                barChart.data = barData
                                
                                val dayLabels = state.dates.map { dateStr ->
                                    try {
                                        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)
                                        SimpleDateFormat("dd", Locale.getDefault()).format(date!!)
                                    } catch (e: Exception) {
                                        dateStr
                                    }
                                }
                                barChart.xAxis.valueFormatter = IndexAxisValueFormatter(dayLabels)
                                
                                barChart.invalidate()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xxlarge))
        }
    }
}
