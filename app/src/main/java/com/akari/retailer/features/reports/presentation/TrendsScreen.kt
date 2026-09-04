package com.akari.retailer.features.reports.presentation

import android.graphics.Color
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
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

    AppScreen(
        title = "Monthly Sales Trend",
        showBackButton = true,
        onBackClick = onBack,
        showDateFilter = true,
        onDateFilterClick = { /* TODO: Implement date filter */ }
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
                            text = "Loading sales data...",
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

            if (state.dailyTotals.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "📊", fontSize = 40.sp)
                        Text(
                            text = "No sales data for ${state.monthYear}",
                            style = AppTypography.body,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = Spacing.medium)
                        )
                        Text(
                            text = "Start recording sales to see trends!",
                            style = AppTypography.small,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
                return@Column
            }

            // Summary stats using AppCard
            AppCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.small),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = state.monthYear,
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
                            text = "Avg/Day",
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
                            text = "Highest",
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

            // Chart using AppCard
            AppCard {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${state.monthYear} Sales",
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
                            
                            val dataSet = BarDataSet(entries, "Sales").apply {
                                color = Color.parseColor("#1976D2")
                                valueTextColor = Color.DKGRAY
                                valueTextSize = 10f
                                setDrawValues(true)
                            }
                            
                            val barData = BarData(dataSet).apply {
                                barWidth = 0.6f
                            }
                            
                            barChart.data = barData
                            
                            val dayLabels = (1..state.dailyTotals.size).map { it.toString() }
                            barChart.xAxis.valueFormatter = IndexAxisValueFormatter(dayLabels)
                            
                            barChart.invalidate()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xxlarge))
        }
    }
}
