package com.akari.retailer.features.money.presentation

import android.graphics.Color
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akari.retailer.R
import com.akari.retailer.RetailApplication
import com.akari.retailer.core.ui.components.AppCard
import com.akari.retailer.core.ui.components.TimeFilterSelector
import com.akari.retailer.core.ui.components.AppScreen
import com.akari.retailer.core.ui.theme.AppTypography
import com.akari.retailer.core.ui.theme.Spacing
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.PercentFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyAnalyticsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as RetailApplication
    
    val accountRepository = remember { application.container.moneyAccountRepository }
    val transactionRepository = remember { application.container.moneyTransactionRepository }
    
    val viewModel: MoneyAnalyticsViewModel = viewModel(
        factory = MoneyAnalyticsViewModelFactory(accountRepository, transactionRepository)
    )
    
    val state by viewModel.state.collectAsState()
    
    
    val chartColors = listOf(
        "#4CAF50", "#2196F3", "#FF9800", "#9C27B0", "#F44336",
        "#00BCD4", "#FF5722", "#795548", "#607D8B", "#E91E63"
    ).map { Color.parseColor(it) }

    AppScreen(
        title = "📊 " + stringResource(R.string.money_analytics),
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
                            onClick = { viewModel.handleEvent(MoneyAnalyticsEvent.LoadAnalytics) }
                        ) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
                return@Column
            }

            // ✅ Unified time filter
            Card(
                modifier = Modifier.fillMaxWidth(),
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
                    Text(
                        text = "📊 Money Analytics",
                        style = AppTypography.body,
                        fontWeight = FontWeight.Medium
                    )
                    TimeFilterSelector(
                        filter = state.timeFilter,
                        onFilterChange = { viewModel.handleEvent(MoneyAnalyticsEvent.TimeFilterChanged(it)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            // Total Balance Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.large),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.total_balance),
                        style = AppTypography.body,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "${state.totalBalance}",
                        style = AppTypography.header.copy(fontSize = 32.sp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${state.accountBalances.size} ${stringResource(R.string.accounts)}",
                        style = AppTypography.small,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            // Money Flow Summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
            ) {
                AppCard(modifier = Modifier.weight(1f)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "↑",
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${state.moneyFlow.totalIn}",
                            style = AppTypography.header,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.money_in),
                            style = AppTypography.small,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                AppCard(modifier = Modifier.weight(1f)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "↓",
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${state.moneyFlow.totalOut}",
                            style = AppTypography.header,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.money_out),
                            style = AppTypography.small,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.small))

            // Net Flow
            AppCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.net_flow),
                        style = AppTypography.body
                    )
                    Text(
                        text = (if (state.moneyFlow.netFlow >= 0) "+" else "") + "${state.moneyFlow.netFlow}",
                        style = AppTypography.header,
                        color = if (state.moneyFlow.netFlow >= 0) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "${state.moneyFlow.transactionCount} ${stringResource(R.string.transactions)}",
                    style = AppTypography.small,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            // Pie Chart - Balance Distribution
            if (state.accountBalances.isNotEmpty() && state.totalBalance > 0) {
                AppCard {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.balance_distribution),
                            style = AppTypography.title,
                            modifier = Modifier.padding(bottom = Spacing.small)
                        )
                        
                        AndroidView(
                            factory = { ctx ->
                                PieChart(ctx).apply {
                                    description.isEnabled = false
                                    setTouchEnabled(true)
                                    setUsePercentValues(true)
                                    setEntryLabelTextSize(11f)
                                    setEntryLabelColor(Color.BLACK)
                                    legend.isEnabled = true
                                    legend.textColor = Color.DKGRAY
                                    legend.textSize = 11f
                                    setDrawEntryLabels(true)
                                    animateY(1000)
                                    animateX(1000)
                                }
                            },
                            update = { pieChart ->
                                val entries = state.accountBalances.map { acc ->
                                    PieEntry(
                                        acc.balance.toFloat(),
                                        acc.account.name
                                    )
                                }
                                
                                val dataSet = PieDataSet(entries, "Accounts").apply {
                                    colors = chartColors
                                    valueTextColor = Color.BLACK
                                    valueTextSize = 12f
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

                // Account Balances List
                Text(
                    text = stringResource(R.string.account_balances),
                    style = AppTypography.title,
                    modifier = Modifier.padding(bottom = Spacing.small)
                )
                
                state.accountBalances.forEach { acc ->
                    AccountBalanceItem(accountBalance = acc)
                    Spacer(modifier = Modifier.height(Spacing.small))
                }

                Spacer(modifier = Modifier.height(Spacing.medium))
            }

            // Fee Summary
            AppCard {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.fee_summary),
                        style = AppTypography.title,
                        modifier = Modifier.padding(bottom = Spacing.small)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.fees_paid),
                            style = AppTypography.body
                        )
                        Text(
                            text = "-${state.feeSummary.totalPaid}",
                            style = AppTypography.body,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.fees_earned),
                            style = AppTypography.body
                        )
                        Text(
                            text = "+${state.feeSummary.totalEarned}",
                            style = AppTypography.body,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(Spacing.small))
                    Divider()
                    Spacer(modifier = Modifier.height(Spacing.small))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.net_fee),
                            style = AppTypography.title
                        )
                        Text(
                            text = (if (state.feeSummary.netFee >= 0) "+" else "") + "${state.feeSummary.netFee}",
                            style = AppTypography.header,
                            color = if (state.feeSummary.netFee >= 0) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            // Fees Bar Chart
            if (state.feeSummary.totalPaid > 0 || state.feeSummary.totalEarned > 0) {
                AppCard {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.fee_comparison),
                            style = AppTypography.title,
                            modifier = Modifier.padding(bottom = Spacing.small)
                        )
                        
                        AndroidView(
                            factory = { ctx ->
                                BarChart(ctx).apply {
                                    description.isEnabled = false
                                    setTouchEnabled(true)
                                    legend.isEnabled = false
                                    
                                    xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                                    xAxis.setDrawGridLines(false)
                                    xAxis.textColor = Color.DKGRAY
                                    xAxis.textSize = 11f
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
                                val entries = listOf(
                                    BarEntry(0f, state.feeSummary.totalPaid.toFloat()),
                                    BarEntry(1f, state.feeSummary.totalEarned.toFloat())
                                )
                                
                                val dataSet = BarDataSet(entries, "Fees").apply {
                                    colors = listOf(Color.parseColor("#F44336"), Color.parseColor("#4CAF50"))
                                    valueTextColor = Color.DKGRAY
                                    valueTextSize = 12f
                                    setDrawValues(true)
                                }
                                
                                val barData = BarData(dataSet).apply {
                                    barWidth = 0.6f
                                }
                                
                                barChart.data = barData
                                barChart.xAxis.valueFormatter = 
                                    com.github.mikephil.charting.formatter.IndexAxisValueFormatter(
                                        listOf("Paid", "Earned")
                                    )
                                
                                barChart.invalidate()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xxlarge))
        }
    }
}

@Composable
fun AccountBalanceItem(accountBalance: AccountBalance) {
    val colorValue = ComposeColor(android.graphics.Color.parseColor(accountBalance.account.color))
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                Text(
                    text = accountBalance.account.getDisplayName(),
                    style = AppTypography.body,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${accountBalance.balance}",
                    style = AppTypography.body,
                    color = colorValue,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LinearProgressIndicator(
                    progress = (accountBalance.percentage / 100).toFloat(),
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    color = colorValue
                )
                Text(
                    text = "${String.format("%.1f", accountBalance.percentage)}%",
                    style = AppTypography.small,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}
