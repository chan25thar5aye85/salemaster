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
import com.akari.retailer.R
import com.akari.retailer.RetailApplication
import com.akari.retailer.core.ui.components.AppCard
import com.akari.retailer.core.ui.components.AppScreen
import com.akari.retailer.core.ui.theme.AppTypography
import com.akari.retailer.core.ui.theme.Spacing
import com.akari.retailer.features.inventory.data.repository.FirestorePurchaseRepository
import com.akari.retailer.features.inventory.domain.models.Purchase
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun PurchaseDetailScreen(
    purchaseId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as RetailApplication
    
    val repository = remember { FirestorePurchaseRepository() }
    
    var purchase by remember { mutableStateOf<Purchase?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    
    LaunchedEffect(purchaseId) {
        isLoading = true
        try {
            repository.getPurchaseById(purchaseId).collect { loadedPurchase ->
                purchase = loadedPurchase
                isLoading = false
                if (loadedPurchase == null) {
                    error = context.getString(R.string.purchase_not_found)
                }
            }
        } catch (e: Exception) {
            isLoading = false
            error = e.message ?: context.getString(R.string.purchase_not_found)
        }
    }

    AppScreen(
        title = stringResource(R.string.purchase_detail),
        showBackButton = true,
        onBackClick = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Text(
                                text = stringResource(R.string.loading_purchases),
                                style = AppTypography.body,
                                modifier = Modifier.padding(top = Spacing.medium)
                            )
                        }
                    }
                }
                
                error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("❌", fontSize = 48.sp)
                            Text(error!!, style = AppTypography.body, color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(Spacing.medium))
                            TextButton(onClick = onBack) {
                                Text(stringResource(R.string.back))
                            }
                        }
                    }
                }
                
                purchase == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📭", fontSize = 48.sp)
                            Text(stringResource(R.string.purchase_not_found), style = AppTypography.header)
                            Spacer(modifier = Modifier.height(Spacing.medium))
                            TextButton(onClick = onBack) {
                                Text(stringResource(R.string.back))
                            }
                        }
                    }
                }
                
                else -> {
                    val currentPurchase = purchase!!
                    
                    // Receipt Header
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.medium)
                        ) {
                            Text(
                                text = "🧾 ${currentPurchase.receiptNumber}",
                                style = AppTypography.header,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "📦 ${currentPurchase.orderName}",
                                style = AppTypography.body
                            )
                            Text(
                                text = "🏢 ${currentPurchase.supplierName}",
                                style = AppTypography.body
                            )
                            Text(
                                text = "📅 ${dateFormat.format(currentPurchase.purchaseDate)}",
                                style = AppTypography.small,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "${stringResource(R.string.order_number)}: ${currentPurchase.orderNumber}",
                                style = AppTypography.small,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(Spacing.medium))
                    
                    // Items
                    AppCard {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${stringResource(R.string.items_count)} (${currentPurchase.items.size})",
                                style = AppTypography.title,
                                modifier = Modifier.padding(bottom = Spacing.medium)
                            )
                            
                            currentPurchase.items.forEachIndexed { index, item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = Spacing.small),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${index + 1}. ${item.productName}",
                                        style = AppTypography.body,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${item.quantity} x ${item.costPrice} = ${item.total}",
                                        style = AppTypography.body,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            
                            Divider(
                                modifier = Modifier.padding(vertical = Spacing.medium)
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(R.string.total_cost),
                                    style = AppTypography.title
                                )
                                Text(
                                    text = "${currentPurchase.totalCost}",
                                    style = AppTypography.header,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    
                    if (currentPurchase.notes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(Spacing.medium))
                        AppCard {
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = stringResource(R.string.notes),
                                    style = AppTypography.title,
                                    modifier = Modifier.padding(bottom = Spacing.medium)
                                )
                                Text(
                                    text = currentPurchase.notes,
                                    style = AppTypography.body
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(Spacing.xxlarge))
                }
            }
        }
    }
}
