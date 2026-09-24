package com.akari.retailer.features.customer.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.akari.retailer.core.utils.MoneyFormatter
import com.akari.retailer.features.customer.domain.models.AgingBucket
import com.akari.retailer.features.customer.domain.models.CustomerAging
import com.akari.retailer.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgingReportScreen(
    navController: NavController,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as RetailApplication

    val viewModel: AgingReportViewModel = viewModel(
        factory = AgingReportViewModelFactory(
            application.container.customerRepository,
            application.container.creditRepository,
            application.container.calculateAgingReportUseCase
        )
    )

    val state by viewModel.state.collectAsState()

    // Bucket colors
    val bucketColors = remember {
        mapOf(
            AgingBucket.CURRENT to Color(0xFF4CAF50),
            AgingBucket.DAYS_30 to Color(0xFFFFC107),
            AgingBucket.DAYS_60 to Color(0xFFFF9800),
            AgingBucket.DAYS_90 to Color(0xFFF44336)
        )
    }

    AppScreen(
        title = stringResource(R.string.aging_report),
        showBackButton = true,
        onBackClick = onBack
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            if (state.isLoading && state.report == null) {
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
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = Spacing.medium)
                        )
                        TextButton(onClick = { viewModel.refresh() }) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
                return@Column
            }

            val report = state.report

            if (report == null || report.totalOwed == 0) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🎉", fontSize = 48.sp)
                        Text(
                            text = stringResource(R.string.no_outstanding_credit),
                            style = AppTypography.header,
                            modifier = Modifier.padding(top = Spacing.medium)
                        )
                        Text(
                            text = stringResource(R.string.everyone_paid_up),
                            style = AppTypography.body,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                return@Column
            }

            // Hero card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.large)
                ) {
                    Text(
                        text = stringResource(R.string.total_outstanding),
                        style = AppTypography.body,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                    )
                    Text(
                        text = MoneyFormatter.formatTotal(report.totalOwed),
                        style = AppTypography.total.copy(fontSize = 30.sp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.n_customers_owe, report.totalCustomersOwing),
                        style = AppTypography.small,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            // Bucket summary — 2x2 grid
            Text(
                text = stringResource(R.string.by_age),
                style = AppTypography.title,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = Spacing.small)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                BucketSummaryCard(
                    bucket = AgingBucket.CURRENT,
                    amount = report.bucketTotals[AgingBucket.CURRENT] ?: 0,
                    customerCount = report.bucketCustomerCounts[AgingBucket.CURRENT] ?: 0,
                    color = bucketColors[AgingBucket.CURRENT]!!,
                    modifier = Modifier.weight(1f)
                )
                BucketSummaryCard(
                    bucket = AgingBucket.DAYS_30,
                    amount = report.bucketTotals[AgingBucket.DAYS_30] ?: 0,
                    customerCount = report.bucketCustomerCounts[AgingBucket.DAYS_30] ?: 0,
                    color = bucketColors[AgingBucket.DAYS_30]!!,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.small))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                BucketSummaryCard(
                    bucket = AgingBucket.DAYS_60,
                    amount = report.bucketTotals[AgingBucket.DAYS_60] ?: 0,
                    customerCount = report.bucketCustomerCounts[AgingBucket.DAYS_60] ?: 0,
                    color = bucketColors[AgingBucket.DAYS_60]!!,
                    modifier = Modifier.weight(1f)
                )
                BucketSummaryCard(
                    bucket = AgingBucket.DAYS_90,
                    amount = report.bucketTotals[AgingBucket.DAYS_90] ?: 0,
                    customerCount = report.bucketCustomerCounts[AgingBucket.DAYS_90] ?: 0,
                    color = bucketColors[AgingBucket.DAYS_90]!!,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            // Filter chips
            ScrollableTabRow(
                selectedTabIndex = state.filter.ordinal,
                edgePadding = 0.dp,
                containerColor = Color.Transparent,
                divider = {}
            ) {
                AgingFilter.values().forEachIndexed { index, filter ->
                    Tab(
                        selected = state.filter == filter,
                        onClick = { viewModel.setFilter(filter) },
                        text = {
                            Text(
                                text = filterLabel(filter),
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.small))

            // Customer count
            Text(
                text = stringResource(R.string.n_customers, state.filteredCustomers.size),
                style = AppTypography.small,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = Spacing.small)
            )

            if (state.filteredCustomers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Spacing.large),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_customers_in_bucket),
                        style = AppTypography.body,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                return@Column
            }

            // Customer list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(Spacing.small),
                contentPadding = PaddingValues(bottom = Spacing.xxlarge)
            ) {
                items(state.filteredCustomers, key = { it.customerId }) { customerAging ->
                    CustomerAgingCard(
                        customerAging = customerAging,
                        bucketColors = bucketColors,
                        onClick = {
                            navController.navigate(
                                Routes.CUSTOMER_DETAIL.replace("{customerId}", customerAging.customerId)
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BucketSummaryCard(
    bucket: AgingBucket,
    amount: Int,
    customerCount: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.12f)
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(color, shape = RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = bucketLabel(bucket),
                    style = AppTypography.small,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = MoneyFormatter.formatTotal(amount),
                style = AppTypography.title,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = "$customerCount customers",
                style = AppTypography.small,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun CustomerAgingCard(
    customerAging: CustomerAging,
    bucketColors: Map<AgingBucket, Color>,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                    text = customerAging.customerName,
                    style = AppTypography.title,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = MoneyFormatter.formatTotal(customerAging.totalOwed),
                    style = AppTypography.title,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Oldest: ${customerAging.oldestDays} days",
                style = AppTypography.small,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(Spacing.small))

            // Bucket breakdown bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(3.dp)
                    )
            ) {
                AgingBucket.values().forEach { bucket ->
                    val amount = customerAging.bucketTotals[bucket] ?: 0
                    if (amount > 0) {
                        val fraction = amount.toFloat() / customerAging.totalOwed.toFloat()
                        Box(
                            modifier = Modifier
                                .weight(fraction)
                                .fillMaxHeight()
                                .background(bucketColors[bucket] ?: Color.Gray)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Bucket chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                customerAging.bucketTotals.forEach { (bucket, amount) ->
                    if (amount > 0) {
                        Surface(
                            color = (bucketColors[bucket] ?: Color.Gray).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "${bucketShortLabel(bucket)}: ${MoneyFormatter.formatTotal(amount)}",
                                style = AppTypography.small,
                                fontSize = 10.sp,
                                color = bucketColors[bucket] ?: Color.Gray,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun filterLabel(filter: AgingFilter): String {
    return when (filter) {
        AgingFilter.ALL -> stringResource(R.string.all)
        AgingFilter.CURRENT -> stringResource(R.string.bucket_current)
        AgingFilter.DAYS_30 -> stringResource(R.string.bucket_30)
        AgingFilter.DAYS_60 -> stringResource(R.string.bucket_60)
        AgingFilter.DAYS_90 -> stringResource(R.string.bucket_90)
    }
}

@Composable
private fun bucketLabel(bucket: AgingBucket): String {
    return when (bucket) {
        AgingBucket.CURRENT -> stringResource(R.string.bucket_current)
        AgingBucket.DAYS_30 -> stringResource(R.string.bucket_30)
        AgingBucket.DAYS_60 -> stringResource(R.string.bucket_60)
        AgingBucket.DAYS_90 -> stringResource(R.string.bucket_90)
    }
}

private fun bucketShortLabel(bucket: AgingBucket): String {
    return when (bucket) {
        AgingBucket.CURRENT -> "0-30"
        AgingBucket.DAYS_30 -> "31-60"
        AgingBucket.DAYS_60 -> "61-90"
        AgingBucket.DAYS_90 -> "90+"
    }
}
