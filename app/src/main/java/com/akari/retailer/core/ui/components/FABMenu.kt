package com.akari.retailer.core.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.akari.retailer.R

@Composable
fun FABMenu(
    onMenuItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    val menuItems = listOf(
        FABMenuItemData(Icons.Default.PointOfSale, stringResource(R.string.fab_sale_entry), "sale_entry"),
        FABMenuItemData(Icons.Default.AttachMoney, "💰 Add Income", "income_entry"),
        FABMenuItemData(Icons.Default.Receipt, stringResource(R.string.fab_expenses), "expenses"),
        FABMenuItemData(Icons.Default.Inventory, stringResource(R.string.fab_inventory), "inventory"),
        FABMenuItemData(Icons.Default.People, stringResource(R.string.fab_customers), "customers"),
        FABMenuItemData(Icons.Default.PieChart, stringResource(R.string.fab_reports), "reports"),
        FABMenuItemData(Icons.Default.PieChart, "Profit & Loss", "profit_loss"),
        FABMenuItemData(Icons.Default.Business, stringResource(R.string.fab_suppliers), "suppliers"),
        FABMenuItemData(Icons.Default.ShoppingCart, stringResource(R.string.fab_purchase_orders), "purchase_orders"),
        FABMenuItemData(Icons.Default.History, stringResource(R.string.fab_purchases), "purchases"),
        FABMenuItemData(Icons.Default.Settings, stringResource(R.string.fab_settings), "settings")
    )

    // Split into two columns
    val firstColumn = menuItems.take(5)
    val secondColumn = menuItems.drop(5)

    Box(
        modifier = modifier
    ) {
        if (isExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { isExpanded = false }
            )
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomStart
        ) {
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + slideInHorizontally(
                    initialOffsetX = { -it },
                    animationSpec = tween(durationMillis = 200)
                ),
                exit = fadeOut() + slideOutHorizontally(
                    targetOffsetX = { -it },
                    animationSpec = tween(durationMillis = 100)
                )
            ) {
                Row(
                    modifier = Modifier.padding(start = 16.dp, bottom = 80.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        firstColumn.forEach { item ->
                            FABMenuItem(
                                icon = item.icon,
                                label = item.label,
                                onClick = {
                                    onMenuItemClick(item.route)
                                    isExpanded = false
                                }
                            )
                        }
                    }
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        secondColumn.forEach { item ->
                            FABMenuItem(
                                icon = item.icon,
                                label = item.label,
                                onClick = {
                                    onMenuItemClick(item.route)
                                    isExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            FloatingActionButton(
                onClick = { isExpanded = !isExpanded },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .padding(start = 16.dp, bottom = 16.dp)
                    .size(56.dp)
                    .clip(CircleShape)
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.Close else Icons.Default.Add,
                    contentDescription = if (isExpanded) "Close menu" else "Open menu"
                )
            }
        }
    }
}

data class FABMenuItemData(
    val icon: ImageVector,
    val label: String,
    val route: String
)
