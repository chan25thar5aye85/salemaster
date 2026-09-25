package com.akari.retailer.core.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.akari.retailer.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.math.sqrt

enum class FABSubmenu {
    NONE,
    REPORTS,
    MONEY
}

@Composable
fun FABMenu(
    onMenuItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    var currentSubmenu by remember { mutableStateOf(FABSubmenu.NONE) }

    // ── Drag state ──
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var isDragging by remember { mutableStateOf(false) }

    val springSpec = spring<Float>(
        dampingRatio = 0.55f,
        stiffness = Spring.StiffnessMediumLow
    )

    // When drag ends, decide whether to linger or snap home.
    // Cancelled automatically if another effect key changes (e.g. user taps FAB).
    LaunchedEffect(isDragging) {
        if (isDragging) return@LaunchedEffect

        val magnitude = sqrt(
            offsetX.value * offsetX.value +
            offsetY.value * offsetY.value
        )
        val thresholdPx = with(density) { 40.dp.toPx() }

        if (magnitude > thresholdPx) {
            // Deliberate move — linger 3s, then spring home
            delay(3000L)
        }
        // Spring home (parallel)
        launch { offsetX.animateTo(0f, animationSpec = springSpec) }
        launch { offsetY.animateTo(0f, animationSpec = springSpec) }
    }

    // Main menu items
    val mainItems = listOf(
        FABMenuItemData(Icons.Default.PointOfSale, stringResource(R.string.fab_sale_entry), "sale_entry"),
        FABMenuItemData(Icons.Default.AttachMoney, stringResource(R.string.fab_add_income), "income_entry"),
        FABMenuItemData(Icons.Default.Receipt, stringResource(R.string.fab_expenses), "expenses"),
        FABMenuItemData(Icons.Default.Public, stringResource(R.string.fab_external_transfer), "external_transfer"),
        FABMenuItemData(Icons.Default.BarChart, stringResource(R.string.fab_submenu_reports), "SUBMENU_REPORTS"),
        FABMenuItemData(Icons.Default.AccountBalanceWallet, stringResource(R.string.fab_submenu_money), "SUBMENU_MONEY"),
        FABMenuItemData(Icons.Default.Inventory, stringResource(R.string.fab_inventory), "inventory"),
        FABMenuItemData(Icons.Default.People, stringResource(R.string.fab_customers), "customers"),
        FABMenuItemData(Icons.Default.Business, stringResource(R.string.fab_suppliers), "suppliers"),
        FABMenuItemData(Icons.Default.ShoppingCart, stringResource(R.string.fab_purchase_orders), "purchase_orders"),
        FABMenuItemData(Icons.Default.History, stringResource(R.string.fab_purchases), "purchases"),
        FABMenuItemData(Icons.Default.Settings, stringResource(R.string.fab_settings), "settings")
    )

    // Reports submenu
    val reportsItems = listOf(
        FABMenuItemData(Icons.Default.BarChart, stringResource(R.string.fab_sales_trends), "reports"),
        FABMenuItemData(Icons.Default.Insights, stringResource(R.string.fab_expense_analytics), "expense_analytics"),
        FABMenuItemData(Icons.Default.Insights, stringResource(R.string.fab_income_analytics), "income_analytics"),
        FABMenuItemData(Icons.Default.PieChart, stringResource(R.string.fab_profit_loss), "profit_loss"),
        FABMenuItemData(Icons.Default.Schedule, stringResource(R.string.fab_aging_report), "aging_report")
    )

    // Money submenu
    val moneyItems = listOf(
        FABMenuItemData(Icons.Default.AccountBalanceWallet, stringResource(R.string.fab_money_accounts), "money_accounts"),
        FABMenuItemData(Icons.Default.SwapHoriz, stringResource(R.string.fab_transfer_money), "transfer_money"),
        FABMenuItemData(Icons.Default.Public, stringResource(R.string.fab_external_transfer), "external_transfer"),
        FABMenuItemData(Icons.Default.ReceiptLong, stringResource(R.string.fab_transactions), "money_transactions"),
        FABMenuItemData(Icons.Default.Insights, stringResource(R.string.fab_money_analytics), "money_analytics")
    )

    // Determine which items to show
    val currentItems = when (currentSubmenu) {
        FABSubmenu.NONE -> mainItems
        FABSubmenu.REPORTS -> reportsItems
        FABSubmenu.MONEY -> moneyItems
    }

    // Split into two columns
    val halfSize = (currentItems.size + 1) / 2
    val firstColumn = currentItems.take(halfSize)
    val secondColumn = currentItems.drop(halfSize)

    Box(
        modifier = modifier
    ) {
        // Scrim layer
        if (isExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { 
                        isExpanded = false
                        currentSubmenu = FABSubmenu.NONE
                    }
            )
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomStart
        ) {
            // Menu items
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
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                offsetX.value.roundToInt(),
                                offsetY.value.roundToInt()
                            )
                        }
                        .padding(start = 16.dp, bottom = 80.dp),
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
                                    handleMenuClick(
                                        item.route,
                                        onMenuItemClick,
                                        onSubmenuChange = { currentSubmenu = it },
                                        onClose = { isExpanded = false }
                                    )
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
                                    handleMenuClick(
                                        item.route,
                                        onMenuItemClick,
                                        onSubmenuChange = { currentSubmenu = it },
                                        onClose = { isExpanded = false }
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // FAB button
            FloatingActionButton(
                onClick = {
                    // Cancel any pending linger and spring home
                    scope.launch {
                        launch { offsetX.animateTo(0f, animationSpec = springSpec) }
                        launch { offsetY.animateTo(0f, animationSpec = springSpec) }
                    }
                    if (isExpanded && currentSubmenu != FABSubmenu.NONE) {
                        currentSubmenu = FABSubmenu.NONE
                    } else {
                        isExpanded = !isExpanded
                        if (!isExpanded) {
                            currentSubmenu = FABSubmenu.NONE
                        }
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .offset {
                        IntOffset(
                            offsetX.value.roundToInt(),
                            offsetY.value.roundToInt()
                        )
                    }
                    .pointerInput(isExpanded) {
                        // Drag only when the menu is closed — when open, taps close the menu.
                        if (!isExpanded) {
                            detectDragGestures(
                                onDragStart = {
                                    isDragging = true
                                    scope.launch { offsetX.stop() }
                                    scope.launch { offsetY.stop() }
                                },
                                onDragEnd = {
                                    isDragging = false
                                },
                                onDragCancel = {
                                    isDragging = false
                                }
                            ) { change, drag ->
                                change.consume()
                                scope.launch {
                                    offsetX.snapTo(offsetX.value + drag.x)
                                }
                                scope.launch {
                                    offsetY.snapTo(offsetY.value + drag.y)
                                }
                            }
                        }
                    }
                    .padding(start = 16.dp, bottom = 16.dp)
                    .size(56.dp)
                    .clip(CircleShape)
            ) {
                Icon(
                    imageVector = when {
                        !isExpanded -> Icons.Default.Add
                        currentSubmenu != FABSubmenu.NONE -> Icons.Default.ArrowBack
                        else -> Icons.Default.Close
                    },
                    contentDescription = when {
                        !isExpanded -> "Open menu"
                        currentSubmenu != FABSubmenu.NONE -> "Back"
                        else -> "Close menu"
                    }
                )
            }
        }
    }
}

private fun handleMenuClick(
    route: String,
    onMenuItemClick: (String) -> Unit,
    onSubmenuChange: (FABSubmenu) -> Unit,
    onClose: () -> Unit
) {
    when (route) {
        "SUBMENU_REPORTS" -> onSubmenuChange(FABSubmenu.REPORTS)
        "SUBMENU_MONEY" -> onSubmenuChange(FABSubmenu.MONEY)
        else -> {
            onMenuItemClick(route)
            onClose()
        }
    }
}

data class FABMenuItemData(
    val icon: ImageVector,
    val label: String,
    val route: String
)
