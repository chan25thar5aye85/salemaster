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
import com.akari.retailer.navigation.Routes

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
    
    var showDeleteDialog by remember { mutableStateOf(false) }
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }

    // Delete confirmation dialog
    if (showDeleteDialog && pendingDeleteId != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                pendingDeleteId = null
            },
            title = { Text("Delete Order") },
            text = { Text("Are you sure you want to delete this order? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        pendingDeleteId?.let { viewModel.handleEvent(PurchaseOrderListEvent.DeleteOrder(it)) }
                        showDeleteDialog = false
                        pendingDeleteId = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    pendingDeleteId = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

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

            if (state.orders.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📦", fontSize = 48.sp)
                        Text(stringResource(R.string.no_purchase_orders), style = AppTypography.header)
                        Text(stringResource(R.string.tap_add_purchase_order), style = AppTypography.body, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
                return@Column
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                contentPadding = PaddingValues(bottom = Spacing.xxlarge)
            ) {
                items(state.orders, key = { it.id }) { order ->
                    PurchaseOrderCardCompact(
                        order = order,
                        navController = navController,
                        orderName = order.orderName,
                        onDelete = {
                            if (order.status == com.akari.retailer.features.inventory.domain.models.PurchaseOrderStatus.ORDER) {
                                pendingDeleteId = order.id
                                showDeleteDialog = true
                            }
                        }
                    )
                }
            }
        }
    }
}
