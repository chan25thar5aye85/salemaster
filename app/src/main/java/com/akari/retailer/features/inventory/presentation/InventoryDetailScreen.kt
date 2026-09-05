package com.akari.retailer.features.inventory.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.akari.retailer.core.ui.components.AppPrimaryButton
import com.akari.retailer.core.ui.components.AppScreen
import com.akari.retailer.core.ui.theme.AppTypography
import com.akari.retailer.core.ui.theme.Spacing
import com.akari.retailer.features.inventory.data.repository.FirestoreInventoryRepository
import com.akari.retailer.features.inventory.data.remote.FirestoreInventoryService
import com.akari.retailer.features.inventory.domain.models.Product
import com.akari.retailer.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryDetailScreen(
    productId: String,
    navController: NavController,
    onBack: () -> Unit,
    onEdit: (Product) -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as RetailApplication
    
    val service = remember { FirestoreInventoryService() }
    val repository = remember { FirestoreInventoryRepository(service) }
    
    val viewModel: InventoryDetailViewModel = viewModel(
        factory = InventoryDetailViewModelFactory(repository, productId)
    )
    
    val state by viewModel.state.collectAsState()

    AppScreen(
        title = stringResource(R.string.product_details),
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
                        AppPrimaryButton(
                            text = stringResource(R.string.retry),
                            onClick = {
                                viewModel.handleEvent(InventoryDetailEvent.LoadProduct)
                            }
                        )
                    }
                }
                return@Column
            }

            state.product?.let { product ->
                AppCard {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = product.name,
                            style = AppTypography.header
                        )
                        
                        Spacer(modifier = Modifier.height(Spacing.medium))
                        
                        if (product.category.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "📂 ${stringResource(R.string.product_category)}",
                                    style = AppTypography.body,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = product.category,
                                    style = AppTypography.body
                                )
                            }
                            Spacer(modifier = Modifier.height(Spacing.small))
                        }
                        
                        if (product.sku.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "🏷️ ${stringResource(R.string.product_sku)}",
                                    style = AppTypography.body,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = product.sku,
                                    style = AppTypography.body
                                )
                            }
                            Spacer(modifier = Modifier.height(Spacing.small))
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "💰 ${stringResource(R.string.product_cost_price)}",
                                style = AppTypography.body,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "${product.costPrice}",
                                style = AppTypography.body
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(Spacing.small))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "💵 ${stringResource(R.string.product_sell_price)}",
                                style = AppTypography.body,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "${product.sellPrice}",
                                style = AppTypography.body,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(Spacing.small))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "📦 ${stringResource(R.string.product_stock_quantity)}",
                                style = AppTypography.body,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = when {
                                    product.isOutOfStock -> stringResource(R.string.out_of_stock)
                                    product.isLowStock -> "⚠️ ${stringResource(R.string.low_stock)}: ${product.stockQuantity}"
                                    else -> "${product.stockQuantity} ${stringResource(R.string.in_stock)}"
                                },
                                style = AppTypography.body,
                                color = when {
                                    product.isOutOfStock -> MaterialTheme.colorScheme.error
                                    product.isLowStock -> MaterialTheme.colorScheme.tertiary
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                        
                        if (product.minStockLevel > 0) {
                            Spacer(modifier = Modifier.height(Spacing.small))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "⚠️ ${stringResource(R.string.product_min_stock)}",
                                    style = AppTypography.body,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = "${product.minStockLevel}",
                                    style = AppTypography.body
                                )
                            }
                        }
                        
                        if (product.supplierId.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(Spacing.small))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "🏢 ${stringResource(R.string.product_supplier_id)}",
                                    style = AppTypography.body,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = product.supplierId,
                                    style = AppTypography.body
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.medium))

                AppPrimaryButton(
                    text = stringResource(R.string.edit_product),
                    onClick = {
                        onEdit(product)
                    }
                )
                
                Spacer(modifier = Modifier.height(Spacing.medium))

                AppPrimaryButton(
                    text = "View Stock History",
                    onClick = {
                        navController.navigate(
                            Routes.STOCK_HISTORY.replace("{productId}", product.id).replace("{productName}", product.name)
                        )
                    }
                )

                Spacer(modifier = Modifier.height(Spacing.xxlarge))
            }
        }
    }
}
