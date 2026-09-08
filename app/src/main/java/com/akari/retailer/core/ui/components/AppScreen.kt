package com.akari.retailer.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.akari.retailer.R
import com.akari.retailer.core.ui.theme.AppTypography
import com.akari.retailer.core.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScreen(
    title: String,
    modifier: Modifier = Modifier,
    showBackButton: Boolean = false,
    onBackClick: (() -> Unit)? = null,
    showSearchButton: Boolean = false,
    onSearchClick: (() -> Unit)? = null,
    showDateFilter: Boolean = false,
    onDateFilterClick: (() -> Unit)? = null,
    showHistoryButton: Boolean = false,
    onHistoryClick: (() -> Unit)? = null,
    showAddButton: Boolean = false,
    onAddClick: (() -> Unit)? = null,
    showAnalyticsButton: Boolean = false,
    onAnalyticsClick: (() -> Unit)? = null,
    showFilterButton: Boolean = false,
    onFilterClick: (() -> Unit)? = null,
    showTopBar: Boolean = false,
    floatingActionButton: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val hasTopBar = showBackButton || showSearchButton || showDateFilter || showHistoryButton || showTopBar || showAddButton || showAnalyticsButton || showFilterButton

    Scaffold(
        topBar = {
            if (hasTopBar) {
                TopAppBar(
                    title = { 
                        Text(
                            title,
                            color = Color.Black
                        )
                    },
                    navigationIcon = {
                        if (showBackButton) {
                            IconButton(onClick = { onBackClick?.invoke() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.back),
                                    tint = Color.Black
                                )
                            }
                        }
                    },
                    actions = {
                        if (showFilterButton) {
                            IconButton(onClick = { onFilterClick?.invoke() }) {
                                Icon(
                                    Icons.Default.FilterList,
                                    contentDescription = "Filter",
                                    tint = Color.Black
                                )
                            }
                        }
                        if (showAnalyticsButton) {
                            IconButton(onClick = { onAnalyticsClick?.invoke() }) {
                                Icon(
                                    Icons.Default.Analytics,
                                    contentDescription = "Analytics",
                                    tint = Color.Black
                                )
                            }
                        }
                        if (showAddButton) {
                            IconButton(onClick = { onAddClick?.invoke() }) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Add",
                                    tint = Color.Black
                                )
                            }
                        }
                        if (showSearchButton) {
                            IconButton(onClick = { onSearchClick?.invoke() }) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = Color.Black
                                )
                            }
                        }
                        if (showDateFilter) {
                            IconButton(onClick = { onDateFilterClick?.invoke() }) {
                                Icon(
                                    Icons.Default.DateRange,
                                    contentDescription = "Filter by date",
                                    tint = Color.Black
                                )
                            }
                        }
                        if (showHistoryButton) {
                            IconButton(onClick = { onHistoryClick?.invoke() }) {
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = "History",
                                    tint = Color.Black
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    windowInsets = TopAppBarDefaults.windowInsets
                )
            }
        },
        floatingActionButton = {
            floatingActionButton?.invoke()
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = Spacing.large)
                .padding(top = 4.dp)
        ) {
            if (!hasTopBar) {
                Text(
                    title,
                    style = AppTypography.header,
                    modifier = Modifier.padding(bottom = Spacing.medium)
                )
            }
            content()
        }
    }
}
