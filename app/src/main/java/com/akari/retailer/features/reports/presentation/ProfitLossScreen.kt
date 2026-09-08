package com.akari.retailer.features.reports.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.akari.retailer.R
import com.akari.retailer.RetailApplication
import com.akari.retailer.core.ui.components.AppCard
import com.akari.retailer.core.ui.components.AppScreen
import com.akari.retailer.core.ui.theme.AppTypography
import com.akari.retailer.core.ui.theme.Spacing
import com.akari.retailer.features.expense.domain.usecases.CalculateBusinessProfitUseCase
import com.akari.retailer.features.expense.domain.usecases.ProfitData
import com.akari.retailer.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfitLossScreen(
    navController: NavController? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as RetailApplication
    
    val saleRepository = application.container.saleRepository
    val expenseRepository = application.container.expenseRepository
    
    val calculateProfitUseCase = remember { 
        CalculateBusinessProfitUseCase(saleRepository, expenseRepository)
    }
    
    val viewModel: ProfitLossViewModel = viewModel(
        factory = ProfitLossViewModelFactory(calculateProfitUseCase)
    )
    
    val state by viewModel.state.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    AppScreen(
        title = "📊 Profit & Loss",
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
                        Spacer(modifier = Modifier.height(Spacing.medium))
                        Text(
                            text = "Calculating...",
                            style = AppTypography.body,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        TextButton(
                            onClick = { viewModel.loadData() },
                            modifier = Modifier.padding(top = Spacing.medium)
                        ) {
                            Text("Retry")
                        }
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
                        Text("❌", fontSize = 40.sp)
                        Text(
                            text = state.error!!,
                            style = AppTypography.body,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = Spacing.medium)
                        )
                        TextButton(
                            onClick = { viewModel.loadData() }
                        ) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
                return@Column
            }

            state.profitData?.let { data ->
                // Summary Cards - Only Revenue and Expense are clickable
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
                ) {
                    // ✅ Revenue Card - Click to Sales Trends
                    AppCard(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                navController?.navigate(Routes.TRENDS)
                            }
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "💰",
                                fontSize = 24.sp
                            )
                            Text(
                                text = "${data.revenue}",
                                style = AppTypography.header,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Revenue",
                                style = AppTypography.small,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "📈 View Trends",
                                style = AppTypography.small,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                fontSize = 10.sp
                            )
                        }
                    }
                    
                    // ✅ Expenses Card - Click to Expense Analytics
                    AppCard(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                navController?.navigate(Routes.EXPENSE_ANALYTICS)
                            }
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "💳",
                                fontSize = 24.sp
                            )
                            Text(
                                text = "${data.businessExpenses}",
                                style = AppTypography.header,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "Business Expenses",
                                style = AppTypography.small,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "📊 View Analytics",
                                style = AppTypography.small,
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.medium))

                // ✅ Profit Card - Display only (not clickable)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (data.profit > 0) 
                            MaterialTheme.colorScheme.primaryContainer
                        else 
                            MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.large),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (data.profit > 0) "📈" else "📉",
                            fontSize = 32.sp
                        )
                        Text(
                            text = "${data.profit}",
                            style = AppTypography.header.copy(fontSize = 28.sp),
                            color = if (data.profit > 0) 
                                MaterialTheme.colorScheme.primary
                            else 
                                MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Net Profit",
                            style = AppTypography.body,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        
                        Spacer(modifier = Modifier.height(Spacing.small))
                        Surface(
                            color = if (data.profitMargin >= 20) 
                                Color(0xFF4CAF50).copy(alpha = 0.15f)
                            else if (data.profitMargin >= 10) 
                                Color(0xFFFF9800).copy(alpha = 0.15f)
                            else 
                                Color(0xFFF44336).copy(alpha = 0.15f),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "Profit Margin: ${String.format("%.1f", data.profitMargin)}%",
                                style = AppTypography.small,
                                color = if (data.profitMargin >= 20) 
                                    Color(0xFF4CAF50)
                                else if (data.profitMargin >= 10) 
                                    Color(0xFFFF9800)
                                else 
                                    Color(0xFFF44336),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.medium))

                // Detail Breakdown - Display only (not clickable)
                AppCard {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "📋 Detailed Breakdown",
                            style = AppTypography.title,
                            modifier = Modifier.padding(bottom = Spacing.medium)
                        )
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = Spacing.small),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "💰 Total Revenue",
                                style = AppTypography.body
                            )
                            Text(
                                text = "${data.revenue}",
                                style = AppTypography.body,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(Spacing.small))
                        Divider()
                        Spacer(modifier = Modifier.height(Spacing.small))
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = Spacing.small),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "💳 Business Expenses",
                                style = AppTypography.body
                            )
                            Text(
                                text = "${data.businessExpenses}",
                                style = AppTypography.body,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = Spacing.small),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "👤 Personal Expenses",
                                style = AppTypography.body,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "${data.personalExpenses}",
                                style = AppTypography.body,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(Spacing.small))
                        Divider()
                        Spacer(modifier = Modifier.height(Spacing.small))
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = Spacing.small),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "📊 Net Profit",
                                style = AppTypography.title
                            )
                            Text(
                                text = "${data.profit}",
                                style = AppTypography.header,
                                color = if (data.profit > 0) 
                                    MaterialTheme.colorScheme.primary
                                else 
                                    MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.medium))

                // Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.medium)
                    ) {
                        Text(
                            text = "💡 How is this calculated?",
                            style = AppTypography.title,
                            modifier = Modifier.padding(bottom = Spacing.small)
                        )
                        Text(
                            text = "• Revenue: Total from all sales",
                            style = AppTypography.small
                        )
                        Text(
                            text = "• Business Expenses: Business + Mixed (business %) expenses",
                            style = AppTypography.small
                        )
                        Text(
                            text = "• Personal Expenses: Personal expenses only (not deducted from profit)",
                            style = AppTypography.small
                        )
                        Text(
                            text = "• Profit Margin: (Profit / Revenue) × 100",
                            style = AppTypography.small
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.xxlarge))
            }
        }
    }
}
