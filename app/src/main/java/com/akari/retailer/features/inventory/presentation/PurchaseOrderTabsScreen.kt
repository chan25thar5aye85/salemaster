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
import com.akari.retailer.features.inventory.data.repository.FirestoreInventoryRepository
import com.akari.retailer.features.inventory.data.repository.FirestoreStockRepository
import com.akari.retailer.features.inventory.data.remote.FirestoreInventoryService
import com.akari.retailer.features.inventory.data.remote.FirestoreStockService
import com.akari.retailer.features.inventory.domain.models.PurchaseOrderStatus
import com.akari.retailer.features.supplier.data.repository.FirestoreSupplierRepository
import com.akari.retailer.features.supplier.data.remote.FirestoreSupplierService
import com.akari.retailer.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseOrderTabsScreen(
    navController: NavController,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as RetailApplication
    
    val inventoryService = remember { FirestoreInventoryService() }
    val inventoryRepository = remember { FirestoreInventoryRepository(inventoryService) }
    val stockService = remember { FirestoreStockService() }
    val stockRepository = remember { FirestoreStockRepository(stockService) }
    val supplierService = remember { FirestoreSupplierService() }
    val supplierRepository = remember { FirestoreSupplierRepository(supplierService) }
    
    // Shared ViewModel for passing orders to detail screen
    val sharedViewModel: PurchaseOrderSharedViewModel = viewModel()
    
    val viewModel: PurchaseOrderTabsViewModel = viewModel(
        factory = PurchaseOrderTabsViewModelFactory(inventoryRepository, stockRepository, supplierRepository, sharedViewModel)
    )
    
    val state by viewModel.state.collectAsState()
    
    var productNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    LaunchedEffect(Unit) {
        inventoryRepository.getProducts().collect { products ->
            productNames = products.associate { it.id to it.name }
        }
    }
    
    var selectedTab by remember { mutableStateOf(0) }
    
    val tabs = listOf(
        stringResource(R.string.status_all),
        stringResource(R.string.status_draft),
        stringResource(R.string.status_sent),
        stringResource(R.string.status_acknowledged),
        stringResource(R.string.status_received),
        stringResource(R.string.status_invoiced),
        stringResource(R.string.status_closed)
    )
    
    val statusFilter = when (selectedTab) {
        0 -> null
        1 -> PurchaseOrderStatus.DRAFT
        2 -> PurchaseOrderStatus.SENT
        3 -> PurchaseOrderStatus.ACKNOWLEDGED
        4 -> PurchaseOrderStatus.RECEIVED
        5 -> PurchaseOrderStatus.INVOICED
        6 -> PurchaseOrderStatus.CLOSED
        else -> null
    }
    
    val filteredOrders = state.orders.filter { order ->
        if (statusFilter == null) true
        else order.status == statusFilter
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
                                viewModel.loadOrders()
                            }
                        ) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
                return@Column
            }

            // Status Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                edgePadding = 0.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    val count = when (index) {
                        0 -> state.orders.size
                        1 -> state.orders.count { it.status == PurchaseOrderStatus.DRAFT }
                        2 -> state.orders.count { it.status == PurchaseOrderStatus.SENT }
                        3 -> state.orders.count { it.status == PurchaseOrderStatus.ACKNOWLEDGED }
                        4 -> state.orders.count { it.status == PurchaseOrderStatus.RECEIVED }
                        5 -> state.orders.count { it.status == PurchaseOrderStatus.INVOICED }
                        6 -> state.orders.count { it.status == PurchaseOrderStatus.CLOSED }
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
                                Text(title)
                                if (count > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ) {
                                        Text("$count")
                                    }
                                }
                            }
                        }
                    )
                }
            }

            if (filteredOrders.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (selectedTab == 0) "📦" else "📭",
                            fontSize = 48.sp
                        )
                        Text(
                            text = if (selectedTab == 0) 
                                stringResource(R.string.no_purchase_orders)
                            else 
                                "No ${tabs[selectedTab]} orders",
                            style = AppTypography.header,
                            modifier = Modifier.padding(top = Spacing.medium)
                        )
                        if (selectedTab == 0) {
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
                    items = filteredOrders,
                    key = { it.id }
                ) { order ->
                    val productName = if (order.items.isNotEmpty()) {
                        productNames[order.items.first().productId] ?: "Product"
                    } else {
                        "Product"
                    }
                    
                    PurchaseOrderCardCompact(
                        order = order,
                        navController = navController,
                        productName = productName
                    )
                }
            }
        }
    }
}
