package com.akari.retailer.features.inventory.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
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
import com.akari.retailer.core.ui.components.AppCard
import com.akari.retailer.core.ui.components.AppScreen
import com.akari.retailer.core.ui.components.SearchBox
import com.akari.retailer.core.ui.theme.AppTypography
import com.akari.retailer.core.ui.theme.Spacing
import com.akari.retailer.features.inventory.data.repository.FirestoreInventoryRepository
import com.akari.retailer.features.inventory.data.remote.FirestoreInventoryService
import com.akari.retailer.features.inventory.domain.models.Product
import com.akari.retailer.features.inventory.domain.models.StockStatus
import com.akari.retailer.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryListScreen(
    navController: NavController,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as RetailApplication
    
    val service = remember { FirestoreInventoryService() }
    val repository = remember { FirestoreInventoryRepository(service) }
    
    val viewModel: InventoryListViewModel = viewModel(
        factory = InventoryListViewModelFactory(repository)
    )
    
    val state by viewModel.state.collectAsState()
    
    var showDeleteDialog by remember { mutableStateOf(false) }
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }
    var showSearch by remember { mutableStateOf(false) }

    if (showDeleteDialog && pendingDeleteId != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                pendingDeleteId = null
            },
            title = { Text(stringResource(R.string.delete_product)) },
            text = { Text(stringResource(R.string.delete_product_confirmation)) },
            confirmButton = {
                Button(
                    onClick = {
                        pendingDeleteId?.let { viewModel.handleEvent(InventoryListEvent.DeleteProduct(it)) }
                        showDeleteDialog = false
                        pendingDeleteId = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    pendingDeleteId = null
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    AppScreen(
        title = stringResource(R.string.inventory),
        showBackButton = true,
        onBackClick = onBack,
        showSearchButton = true,
        onSearchClick = { showSearch = !showSearch },
        showAddButton = true,
        onAddClick = { navController.navigate(Routes.INVENTORY_ADD) }
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            if (showSearch) {
                SearchBox(
                    query = state.searchQuery,
                    onQueryChange = { 
                        viewModel.handleEvent(InventoryListEvent.SearchQueryChanged(it))
                    },
                    onSearch = {},
                    placeholder = stringResource(R.string.search_products)
                )
            }

            if (state.allProducts.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = Spacing.small),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
                ) {
                    AppCard(
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${state.allProducts.size}",
                                style = AppTypography.title
                            )
                            Text(
                                text = stringResource(R.string.total_products),
                                style = AppTypography.small,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                    AppCard(
                        modifier = Modifier.weight(1f)
                    ) {
                        val lowStockCount = state.allProducts.count { it.isLowStock || it.isOutOfStock }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$lowStockCount",
                                style = AppTypography.title,
                                color = if (lowStockCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.low_stock_products),
                                style = AppTypography.small,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            if (state.isLoading && state.products.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Text(
                            text = stringResource(R.string.loading_products),
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
                                viewModel.handleEvent(InventoryListEvent.LoadProducts)
                            }
                        ) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
                return@Column
            }

            if (state.products.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (state.searchQuery.isNotEmpty()) "🔍" else "📦",
                            fontSize = 48.sp
                        )
                        Text(
                            text = if (state.searchQuery.isNotEmpty()) 
                                "${stringResource(R.string.no_products_found)} '${state.searchQuery}'"
                            else 
                                stringResource(R.string.no_products_yet),
                            style = AppTypography.header,
                            modifier = Modifier.padding(top = Spacing.medium)
                        )
                        Text(
                            text = if (state.searchQuery.isNotEmpty()) 
                                stringResource(R.string.try_different_search)
                            else 
                                stringResource(R.string.tap_add_product),
                            style = AppTypography.body,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                return@Column
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.medium),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (state.searchQuery.isNotEmpty()) 
                        "${state.products.size} ${stringResource(R.string.results_for)} '${state.searchQuery}'" 
                    else 
                        "${state.products.size} ${stringResource(R.string.products)}",
                    style = AppTypography.label,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Row {
                    if (state.showLowStockOnly) {
                        TextButton(
                            onClick = {
                                viewModel.handleEvent(InventoryListEvent.ToggleLowStockFilter)
                            }
                        ) {
                            Text(stringResource(R.string.show_all))
                        }
                    } else {
                        TextButton(
                            onClick = {
                                viewModel.handleEvent(InventoryListEvent.ToggleLowStockFilter)
                            }
                        ) {
                            Text(stringResource(R.string.low_stock_products))
                        }
                    }
                    if (state.searchQuery.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                viewModel.handleEvent(InventoryListEvent.ClearSearch)
                                showSearch = false
                            }
                        ) {
                            Text(stringResource(R.string.clear))
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                contentPadding = PaddingValues(bottom = Spacing.xxlarge)
            ) {
                items(
                    items = state.products,
                    key = { it.id }
                ) { product ->
                    ProductCard(
                        product = product,
                        onDelete = {
                            pendingDeleteId = product.id
                            showDeleteDialog = true
                        },
                        onClick = {
                            navController.navigate(Routes.INVENTORY_DETAIL.replace("{productId}", product.id))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ProductCard(
    product: Product,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = product.name,
                        style = AppTypography.title
                    )
                    when (product.stockStatus) {
                        StockStatus.OUT_OF_STOCK -> {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = stringResource(R.string.out_of_stock),
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(start = Spacing.small)
                            )
                        }
                        StockStatus.LOW_STOCK -> {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = stringResource(R.string.low_stock),
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.padding(start = Spacing.small)
                            )
                        }
                        else -> {}
                    }
                }
                
                if (product.category.isNotEmpty()) {
                    Text(
                        text = "📂 ${product.category}",
                        style = AppTypography.body,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.small),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${stringResource(R.string.sell_price)}: ${product.sellPrice}",
                        style = AppTypography.small,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = when (product.stockStatus) {
                            StockStatus.IN_STOCK -> "${stringResource(R.string.in_stock)}: ${product.stockQuantity}"
                            StockStatus.LOW_STOCK -> "⚠️ ${stringResource(R.string.low_stock)}: ${product.stockQuantity}"
                            StockStatus.OUT_OF_STOCK -> "❌ ${stringResource(R.string.out_of_stock)}"
                        },
                        style = AppTypography.small,
                        color = when (product.stockStatus) {
                            StockStatus.IN_STOCK -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            StockStatus.LOW_STOCK -> MaterialTheme.colorScheme.tertiary
                            StockStatus.OUT_OF_STOCK -> MaterialTheme.colorScheme.error
                        }
                    )
                }
            }
            
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                )
            }
        }
    }
}
