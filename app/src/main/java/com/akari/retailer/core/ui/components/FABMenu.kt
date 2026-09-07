package com.akari.retailer.core.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
        FABMenuItemData(Icons.Default.Receipt, stringResource(R.string.expenses), "expenses"),
        FABMenuItemData(Icons.Default.Inventory, stringResource(R.string.inventory), "inventory"),
        FABMenuItemData(Icons.Default.People, stringResource(R.string.customers), "customers"),
        FABMenuItemData(Icons.Default.PieChart, stringResource(R.string.reports), "reports"),
        FABMenuItemData(Icons.Default.Business, stringResource(R.string.suppliers), "suppliers"),
        FABMenuItemData(Icons.Default.ShoppingCart, stringResource(R.string.purchase_orders), "purchase_orders"),
        FABMenuItemData(Icons.Default.History, "Purchases", "purchases"),
        FABMenuItemData(Icons.Default.Settings, stringResource(R.string.settings), "settings")
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomEnd
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.padding(bottom = 70.dp)
        ) {
            menuItems.forEachIndexed { index, item ->
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = fadeIn() + slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = tween(durationMillis = 150 + index * 50)
                    ),
                    exit = fadeOut() + slideOutVertically(
                        targetOffsetY = { it / 2 },
                        animationSpec = tween(durationMillis = 100)
                    )
                ) {
                    FABMenuItem(
                        icon = item.icon,
                        label = item.label,
                        onClick = {
                            onMenuItemClick(item.route)
                            isExpanded = false
                        },
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { isExpanded = !isExpanded },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
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

data class FABMenuItemData(
    val icon: ImageVector,
    val label: String,
    val route: String
)
