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

@Composable
fun OrderListScreen(
    navController: NavController,
    status: PurchaseOrderStatus? = null,
    title: String,
    onBack: () -> Unit,
    showAddButton: Boolean = true
) {
    val context = LocalContext.current
    val application = context.applicationContext as RetailApplication
    
    val repository = remember { FirestorePurchaseOrderRepository() }
    
    val viewModel: PurchaseOrderListViewModel = viewModel(
        factory = PurchaseOrderListViewModelFactory(repository)
    )
    
    val state by viewModel.state.collectAsState()
    
    // Apply filter for this screen
    LaunchedEffect(status) {
        viewModel.handleEvent(PurchaseOrderListEvent.FilterByStatus(status))
    }

    AppScreen(
        title = title,
        showBackButton = true,
        onBackClick = onBack,
        showAddButton = showAddButton,
        onAddClick = { navController.navigate(Routes.PURCHASE_ORDER_ADD) }
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Text(
                            text = stringResource(R.string.loading),
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
                                viewModel.handleEvent(PurchaseOrderListEvent.LoadOrders)
                            }
                        ) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
                return@Column
            }

            val orders = state.orders

            if (orders.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = when (status) {
                                PurchaseOrderStatus.DRAFT -> "📝"
                                PurchaseOrderStatus.SENT -> "📤"
                                PurchaseOrderStatus.RECEIVED -> "📦"
                                PurchaseOrderStatus.CLOSED -> "✅"
                                else -> "📦"
                            },
                            fontSize = 48.sp
                        )
                        Text(
                            text = when (status) {
                                PurchaseOrderStatus.DRAFT -> "No draft orders"
                                PurchaseOrderStatus.SENT -> "No sent orders"
                                PurchaseOrderStatus.RECEIVED -> "No received orders"
                                PurchaseOrderStatus.CLOSED -> "No closed orders"
                                else -> stringResource(R.string.no_purchase_orders)
                            },
                            style = AppTypography.header,
                            modifier = Modifier.padding(top = Spacing.medium)
                        )
                        if (status == PurchaseOrderStatus.DRAFT || status == null) {
                            Text(
                                text = stringResource(R.string.tap_add_purchase_order),
                                style = AppTypography.body,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
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
                items(
                    items = orders,
                    key = { it.id }
                ) { order ->
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
