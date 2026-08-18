package com.hninakari.saletracker.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
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
    
    // Animated offsets for smooth snapping animations
    val animOffsetX = remember { Animatable(0f) }
    val animOffsetY = remember { Animatable(0f) }
    
    // Constants
    val buttonSize = 56.dp
    val menuWidth = 140.dp
    val menuHeight = 130.dp  
    val gapBetweenMenuAndButton = 12.dp  
    val padding = 16.dp
    
    // Get navigation bar height securely
    val navBarHeight = with(density) {
        val resourceId = view.resources.getIdentifier("navigation_bar_height", "dimen", "android")
        if (resourceId > 0) view.resources.getDimensionPixelSize(resourceId).toFloat().toDp() else 0.dp
    }
    
    // Position button on the LEFT side of the screen initially
    val buttonStartX = padding
    val buttonStartY = screenHeight - buttonSize - padding - navBarHeight - 16.dp
    
    // Convert starting positions to pixels to reference during spring reset
    val startXIdPx = with(density) { buttonStartX.toPx() }
    val startYIdPx = with(density) { buttonStartY.toPx() }
    
    // Initialize starting position
    LaunchedEffect(Unit) {
        animOffsetX.snapTo(startXIdPx)
        animOffsetY.snapTo(startYIdPx)
    }
    
    // Convert current DP boundaries to Pixels for calculations
    val paddingPx = with(density) { padding.toPx() }
    val screenWidthPx = with(density) { screenWidth.toPx() }
    val screenHeightPx = with(density) { screenHeight.toPx() }
    val buttonSizePx = with(density) { buttonSize.toPx() }
    val menuWidthPx = with(density) { menuWidth.toPx() }
    val menuHeightPx = with(density) { menuHeight.toPx() }
    val gapPx = with(density) { gapBetweenMenuAndButton.toPx() }
    val navBarHeightPx = with(density) { navBarHeight.toPx() }

    // Current real-time positions
    val currentButtonX = animOffsetX.value
    val currentButtonY = animOffsetY.value
    
    // Calculate menu position dynamically above the button, but force it to stay completely on-screen
    val menuX = (currentButtonX + (buttonSizePx / 2) - (menuWidthPx / 2)).coerceIn(
        paddingPx,
        screenWidthPx - menuWidthPx - paddingPx
    )
    val menuY = currentButtonY - menuHeightPx - gapPx
    
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
                    .height(menuHeight)
                    .zIndex(101f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    MenuItem(
                        icon = Icons.Default.List,
                        label = stringResource(R.string.menu_sales),
                        onClick = {
                            isExpanded = false
                            onItemClick("sales")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
                    
                    MenuItem(
                        icon = Icons.Default.Settings,
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
        
        // Draggable FAB Button
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
                                // Real-time drag tracking with boundary safety
                                val targetX = (animOffsetX.value + dragAmount.x).coerceIn(
                                    paddingPx, 
                                    screenWidthPx - buttonSizePx - paddingPx
                                )
                                val targetY = (animOffsetY.value + dragAmount.y).coerceIn(
                                    paddingPx + if (isExpanded) menuHeightPx + gapPx else 0f, 
                                    screenHeightPx - buttonSizePx - paddingPx - navBarHeightPx
                                )
                                animOffsetX.snapTo(targetX)
                                animOffsetY.snapTo(targetY)
                            }
                        },
                        onDragEnd = {
                            isDragging = false
                            // Spring back to the exact initial left-side layout positions
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
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
