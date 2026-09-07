package com.akari.retailer.features.inventory.presentation

import androidx.compose.foundation.clickable
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
import androidx.navigation.NavController
import com.akari.retailer.R
import com.akari.retailer.RetailApplication
import com.akari.retailer.core.ui.components.AppScreen
import com.akari.retailer.core.ui.theme.AppTypography
import com.akari.retailer.core.ui.theme.Spacing
import com.akari.retailer.features.inventory.data.repository.FirestorePurchaseRepository
import com.akari.retailer.features.inventory.domain.models.Purchase
import com.akari.retailer.navigation.Routes
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun PurchaseListScreen(
    navController: NavController,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as RetailApplication
    
    val repository = remember { FirestorePurchaseRepository() }
    
    var purchases by remember { mutableStateOf<List<Purchase>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var retryKey by remember { mutableStateOf(0) }
    
    // Load purchases - triggered by retryKey change
    LaunchedEffect(retryKey) {
        isLoading = true
        error = null
        try {
            repository.getPurchases().collect { list ->
                purchases = list
                isLoading = false
            }
        } catch (e: Exception) {
            isLoading = false
            error = e.message
        }
    }

    AppScreen(
        title = stringResource(R.string.purchases),
        showBackButton = true,
        onBackClick = onBack
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Text(
                            text = stringResource(R.string.loading_purchases),
                            modifier = Modifier.padding(top = Spacing.medium)
                        )
                    }
                }
                return@Column
            }

            if (error != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("❌", fontSize = 40.sp)
                        Text(error!!, style = AppTypography.body, color = MaterialTheme.colorScheme.error)
                        TextButton(
                            onClick = { 
                                retryKey++
                            }
                        ) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
                return@Column
            }

            if (purchases.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📦", fontSize = 48.sp)
                        Text(
                            text = stringResource(R.string.no_purchases_yet),
                            style = AppTypography.header
                        )
                        Text(
                            text = stringResource(R.string.create_purchase_from_orders),
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
                items(purchases, key = { it.id }) { purchase ->
                    PurchaseCard(
                        purchase = purchase,
                        onClick = {
                            navController.navigate(
                                Routes.PURCHASE_DETAIL.replace("{purchaseId}", purchase.id)
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PurchaseCard(
    purchase: Purchase,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
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
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "🧾 ${purchase.receiptNumber}",
                    style = AppTypography.title
                )
                Text(
                    text = "${purchase.totalCost}",
                    style = AppTypography.header,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = "📦 ${purchase.orderName}",
                style = AppTypography.body,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Text(
                text = "🏢 ${purchase.supplierName}",
                style = AppTypography.body,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Text(
                text = "📋 ${purchase.items.size} ${stringResource(R.string.items_count)}",
                style = AppTypography.small,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = "📅 ${dateFormat.format(purchase.purchaseDate)}",
                style = AppTypography.small,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}
