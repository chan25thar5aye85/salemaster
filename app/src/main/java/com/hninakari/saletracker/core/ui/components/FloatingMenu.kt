package com.hninakari.saletracker.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.hninakari.saletracker.R
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun FloatingMenu(
    modifier: Modifier = Modifier,
    onItemClick: (String) -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val density = LocalDensity.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    
    // Menu state
    var isExpanded by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }
    
    // Track the dynamic height of the menu after layout passes
    var measuredMenuHeightPx by remember { mutableStateOf(0f) }
    
    // Animated offsets
    val animOffsetX = remember { Animatable(0f) }
    val animOffsetY = remember { Animatable(0f) }
    
    // Constants
    val buttonSize = 56.dp
    val menuWidth = 130.dp // Added slight breathing room for text widths
    val gapBetweenMenuAndButton = 12.dp  
    val padding = 16.dp
    
    // Get navigation bar height
    val navBarHeight = with(density) {
        val resourceId = view.resources.getIdentifier("navigation_bar_height", "dimen", "android")
        if (resourceId > 0) view.resources.getDimensionPixelSize(resourceId).toFloat().toDp() else 0.dp
    }
    
    // Position button on the LEFT side initially
    val buttonStartX = padding
    val buttonStartY = screenHeight - buttonSize - padding - navBarHeight - 16.dp
    
    val startXIdPx = with(density) { buttonStartX.toPx() }
    val startYIdPx = with(density) { buttonStartY.toPx() }
    
    LaunchedEffect(Unit) {
        animOffsetX.snapTo(startXIdPx)
        animOffsetY.snapTo(startYIdPx)
    }
    
    val paddingPx = with(density) { padding.toPx() }
    val screenWidthPx = with(density) { screenWidth.toPx() }
    val screenHeightPx = with(density) { screenHeight.toPx() }
    val buttonSizePx = with(density) { buttonSize.toPx() }
    val menuWidthPx = with(density) { menuWidth.toPx() }
    val gapPx = with(density) { gapBetweenMenuAndButton.toPx() }
    val navBarHeightPx = with(density) { navBarHeight.toPx() }

    val currentButtonX = animOffsetX.value
    val currentButtonY = animOffsetY.value
    
    // Use fallback pixel measurement until runtime box renders
    val effectiveMenuHeightPx = if (measuredMenuHeightPx > 0f) measuredMenuHeightPx else with(density) { 110.dp.toPx() }

    // Position menu dynamically above the button, safe from left edge clipping
    val menuX = (currentButtonX + (buttonSizePx / 2) - (menuWidthPx / 2)).coerceIn(
        paddingPx,
        screenWidthPx - menuWidthPx - paddingPx
    )
    val menuY = currentButtonY - effectiveMenuHeightPx - gapPx
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(100f)
    ) {
        // Menu Items Card
        if (isExpanded) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(menuX.roundToInt(), menuY.roundToInt()) }
                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(12.dp))
                    .background(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(12.dp))
                    .width(menuWidth)
                    .wrapContentHeight() // Expands naturally around its rows completely
                    .onGloballyPositioned { coordinates ->
                        // Dynamically update height measurements to prevent clipping or button collisions
                        measuredMenuHeightPx = coordinates.size.height.toFloat()
                    }
                    .zIndex(101f)
            ) {
                Column(
                    modifier = Modifier
                        .wrapContentHeight()
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    MenuItem(
                        label = stringResource(R.string.menu_sales),
                        onClick = {
                            isExpanded = false
                            onItemClick("sales")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
                    
                    MenuItem(
                        label = stringResource(R.string.menu_settings),
                        onClick = {
                            isExpanded = false
                            onItemClick("settings")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        
        // FAB Button
        FloatingActionButton(
            onClick = {
                if (!isDragging) {
                    isExpanded = !isExpanded
                }
            },
            modifier = Modifier
                .offset { IntOffset(currentButtonX.roundToInt(), currentButtonY.roundToInt()) }
                .size(buttonSize)
                .zIndex(102f)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            coroutineScope.launch {
                                val targetX = (animOffsetX.value + dragAmount.x).coerceIn(
                                    paddingPx, 
                                    screenWidthPx - buttonSizePx - paddingPx
                                )
                                val targetY = (animOffsetY.value + dragAmount.y).coerceIn(
                                    paddingPx + effectiveMenuHeightPx + gapPx, 
                                    screenHeightPx - buttonSizePx - paddingPx - navBarHeightPx
                                )
                                animOffsetX.snapTo(targetX)
                                animOffsetY.snapTo(targetY)
                            }
                        },
                        onDragEnd = {
                            isDragging = false
                            coroutineScope.launch {
                                animOffsetX.animateTo(startXIdPx, spring())
                            }
                            coroutineScope.launch {
                                animOffsetY.animateTo(startYIdPx, spring())
                            }
                        }
                    )
                }
        ) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.Close else Icons.Default.Add,
                contentDescription = "Toggle Floating Menu"
            )
        }
    }
}

@Composable
fun MenuItem(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
