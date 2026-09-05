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
import com.akari.retailer.features.inventory.domain.models.PurchaseOrder
import com.akari.retailer.features.inventory.domain.models.PurchaseOrderStatus
import com.akari.retailer.features.supplier.data.repository.FirestoreSupplierRepository
import com.akari.retailer.features.supplier.data.remote.FirestoreSupplierService
import com.akari.retailer.navigation.Routes
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun PurchaseOrderListScreen(
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
    
    val viewModel: PurchaseOrderListViewModel = viewModel(
        factory = PurchaseOrderListViewModelFactory(inventoryRepository, stockRepository, supplierRepository)
    )
    
    val state by viewModel.state.collectAsState()
    
    var productNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    
    LaunchedEffect(Unit) {
        inventoryRepository.getProducts().collect { products ->
            productNames = products.associate { it.id to it.name }
        }
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

            if (state.orders.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "📦",
                            fontSize = 48.sp
                        )
                        Text(
                            text = stringResource(R.string.no_purchase_orders),
                            style = AppTypography.header,
                            modifier = Modifier.padding(top = Spacing.medium)
                        )
                        Text(
                            text = stringResource(R.string.tap_add_purchase_order),
                            style = AppTypography.body,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
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
                    items = state.orders,
                    key = { it.id }
                ) { order ->
                    PurchaseOrderCard(
                        order = order,
                        productNames = productNames
                    )
                }
            }
        }
    }
}

@Composable
fun PurchaseOrderCard(
    order: PurchaseOrder,
    productNames: Map<String, String>
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                    text = "📦 ${stringResource(R.string.purchase_order_title)}",
                    style = AppTypography.title
                )
                StatusBadge(status = order.status)
            }
            
            Spacer(modifier = Modifier.height(Spacing.small))
            
            // Supplier
            if (order.supplierName.isNotEmpty()) {
                Text(
                    text = "🏢 ${order.supplierName}",
                    style = AppTypography.body,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            // Items
            order.items.forEach { item ->
                val productName = productNames[item.productId] ?: item.productId.take(8)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "• $productName",
                        style = AppTypography.body,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${stringResource(R.string.qty_label)}: ${item.quantity} × ${item.costPrice} = ${item.total}",
                        style = AppTypography.body,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.small))
            
            // Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.total_cost),
                    style = AppTypography.body,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                Text(
                    text = "${order.totalCost}",
                    style = AppTypography.body,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // Invoice
            if (order.invoiceNumber.isNotEmpty()) {
                Text(
                    text = "🧾 ${stringResource(R.string.invoice_number)}: ${order.invoiceNumber}",
                    style = AppTypography.small,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            
            if (order.notes.isNotEmpty()) {
                Text(
                    text = "📝 ${order.notes}",
                    style = AppTypography.small,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = Spacing.small)
                )
            }
        }
    }
}

@Composable
fun StatusBadge(status: PurchaseOrderStatus) {
    val (color, text) = when (status) {
        PurchaseOrderStatus.DRAFT -> 
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) to stringResource(R.string.status_draft)
        PurchaseOrderStatus.SENT -> 
            MaterialTheme.colorScheme.primary to stringResource(R.string.status_sent)
        PurchaseOrderStatus.ACKNOWLEDGED -> 
            MaterialTheme.colorScheme.tertiary to stringResource(R.string.status_acknowledged)
        PurchaseOrderStatus.RECEIVED -> 
            MaterialTheme.colorScheme.primary to stringResource(R.string.status_received)
        PurchaseOrderStatus.INVOICED -> 
            MaterialTheme.colorScheme.primary to stringResource(R.string.status_invoiced)
        PurchaseOrderStatus.CLOSED -> 
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) to stringResource(R.string.status_closed)
    }
    
    Card(
        modifier = Modifier
            .wrapContentWidth(),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.15f)
        )
    ) {
        Text(
            text = text,
            style = AppTypography.small,
            color = color,
            modifier = Modifier.padding(horizontal = Spacing.medium, vertical = Spacing.small)
        )
    }
}
