package com.akari.retailer.features.inventory.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.akari.retailer.core.ui.components.AppScreen
import com.akari.retailer.core.ui.theme.AppTypography
import com.akari.retailer.core.ui.theme.Spacing
import com.akari.retailer.features.inventory.data.repository.FirestorePurchaseOrderRepository
import com.akari.retailer.features.inventory.domain.models.PurchaseOrderStatus
import com.akari.retailer.navigation.Routes
import java.util.Calendar
import java.util.Date

@Composable
fun PurchaseOrderTabsScreen(
    navController: NavController,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as RetailApplication
    
    val repository = remember { FirestorePurchaseOrderRepository() }
    
    val viewModel: PurchaseOrderListViewModel = viewModel(
        factory = PurchaseOrderListViewModelFactory(repository)
    )
    
    val state by viewModel.state.collectAsState()
    
    var selectedTab by remember { mutableStateOf(0) }
    
    // Get tomorrow's date (midnight)
    val tomorrow = remember {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.timeInMillis
    }

    // Filter out COMPLETED orders - only show ORDER and RECEIVED
    val activeOrders = state.orders.filter { 
        it.status != PurchaseOrderStatus.COMPLETED 
    }
    
    // Filter by tab
    val filteredOrders = when (selectedTab) {
        0 -> activeOrders // All
        1 -> activeOrders.filter { 
            // Check if expectedDeliveryDate is tomorrow
            val orderDate = Date(it.expectedDeliveryDate)
            val tomorrowDate = Date(tomorrow)
            
            val orderCal = Calendar.getInstance().apply { time = orderDate }
            val tomorrowCal = Calendar.getInstance().apply { time = tomorrowDate }
            
            orderCal.get(Calendar.YEAR) == tomorrowCal.get(Calendar.YEAR) &&
            orderCal.get(Calendar.DAY_OF_YEAR) == tomorrowCal.get(Calendar.DAY_OF_YEAR)
        }
        else -> activeOrders
    }
    
    // Calculate totals
    val totalOrders = filteredOrders.size
    val totalAmount = filteredOrders.sumOf { it.orderTotal }

    AppScreen(
        title = stringResource(R.string.purchase_orders),
        showBackButton = true,
        onBackClick = onBack,
        showAddButton = true,
        onAddClick = { navController.navigate(Routes.PURCHASE_ORDER_ADD) }
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            if (state.error != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("❌", fontSize = 40.sp)
                        Text(state.error!!, style = AppTypography.body, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = { viewModel.handleEvent(PurchaseOrderListEvent.LoadOrders) }) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
                return@Column
            }

            // Filter Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                edgePadding = 0.dp
            ) {
                val tabs = listOf("All", "Tomorrow")
                tabs.forEachIndexed { index, label ->
                    val count = when (index) {
                        0 -> activeOrders.size
                        1 -> activeOrders.filter { 
                            val orderDate = Date(it.expectedDeliveryDate)
                            val tomorrowDate = Date(tomorrow)
                            val orderCal = Calendar.getInstance().apply { time = orderDate }
                            val tomorrowCal = Calendar.getInstance().apply { time = tomorrowDate }
                            orderCal.get(Calendar.YEAR) == tomorrowCal.get(Calendar.YEAR) &&
                            orderCal.get(Calendar.DAY_OF_YEAR) == tomorrowCal.get(Calendar.DAY_OF_YEAR)
                        }.size
                        else -> 0
                    }
                    
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(label, style = AppTypography.label)
                                if (count > 0) {
                                    Badge(
                                        containerColor = if (selectedTab == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                        contentColor = if (selectedTab == index) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    ) {
                                        Text("$count", style = AppTypography.small)
                                    }
                                }
                            }
                        }
                    )
                }
            }

            // Summary Row
            if (filteredOrders.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.small),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.medium),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "📋 ${stringResource(R.string.total_orders)}",
                                style = AppTypography.small,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "$totalOrders",
                                style = AppTypography.header,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "💰 ${stringResource(R.string.total_cost)}",
                                style = AppTypography.small,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "$totalAmount",
                                style = AppTypography.header,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            if (filteredOrders.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (selectedTab == 0) "📦" else "📅",
                            fontSize = 48.sp
                        )
                        Text(
                            text = if (selectedTab == 0) "No active orders" else "No orders due tomorrow",
                            style = AppTypography.header
                        )
                        if (selectedTab == 0) {
                            Text(
                                "Create a new order or check purchases",
                                style = AppTypography.body,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(Spacing.medium))
                            TextButton(
                                onClick = { navController.navigate(Routes.PURCHASES) }
                            ) {
                                Text("View Purchases")
                            }
                        }
                    }
                }
                return@Column
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                contentPadding = PaddingValues(bottom = Spacing.xxlarge)
            ) {
                items(filteredOrders, key = { it.id }) { order ->
                    PurchaseOrderCardCompact(
                        order = order,
                        navController = navController,
                        orderName = order.orderName
                    )
                }
            }
        }
    }
}
