package com.akari.retailer.features.sales.presentation.income

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
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
import com.akari.retailer.features.sales.data.repository.FirestoreIncomeEntryRepository
import com.akari.retailer.features.sales.data.repository.FirestoreIncomeStreamRepository
import com.akari.retailer.features.sales.data.remote.FirestoreIncomeEntryService
import com.akari.retailer.features.sales.data.remote.FirestoreIncomeStreamService
import com.akari.retailer.features.sales.domain.models.IncomeEntry
import com.akari.retailer.features.sales.domain.models.IncomeEntryType
import com.akari.retailer.navigation.Routes
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun IncomeListScreen(
    navController: NavController,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as RetailApplication
    
    val entryService = remember { FirestoreIncomeEntryService() }
    val entryRepository = remember { FirestoreIncomeEntryRepository(entryService) }
    val streamService = remember { FirestoreIncomeStreamService() }
    val streamRepository = remember { FirestoreIncomeStreamRepository(streamService) }
    
    val viewModel: IncomeListViewModel = viewModel(
        factory = IncomeListViewModelFactory(entryRepository, streamRepository)
    )
    
    val state by viewModel.state.collectAsState()
    
    var showDeleteDialog by remember { mutableStateOf(false) }
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }
    var showSearch by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog && pendingDeleteId != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                pendingDeleteId = null
            },
            title = { Text("Delete Income") },
            text = { Text("Are you sure you want to delete this income entry?") },
            confirmButton = {
                Button(
                    onClick = {
                        pendingDeleteId?.let { viewModel.handleEvent(IncomeListEvent.DeleteEntry(it)) }
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
        title = "💰 Income",
        showBackButton = true,
        onBackClick = onBack,
        showSearchButton = true,
        onSearchClick = { showSearch = !showSearch },
        showAddButton = true,
        onAddClick = { navController.navigate(Routes.INCOME_ENTRY) },
        showFilterButton = true,
        onFilterClick = { showFilterDialog = true },
        showAnalyticsButton = true,
        onAnalyticsClick = { navController.navigate(Routes.INCOME_ANALYTICS) }
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Income Summary
            if (state.allEntries.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
                ) {
                    AppCard(modifier = Modifier.weight(1f)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${state.totalIncome}",
                                style = AppTypography.header,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Total Income",
                                style = AppTypography.small,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                    AppCard(modifier = Modifier.weight(1f)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${state.businessIncome}",
                                style = AppTypography.header,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Business",
                                style = AppTypography.small,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                    AppCard(modifier = Modifier.weight(1f)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${state.personalIncome}",
                                style = AppTypography.header,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Personal",
                                style = AppTypography.small,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            // Manage Income Streams Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.medium)
                    .clickable { navController.navigate(Routes.INCOME_STREAMS) },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.medium),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚙️ Manage Income Streams",
                        style = AppTypography.body
                    )
                    Icon(
                        Icons.Default.FilterList,
                        contentDescription = "Manage",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (showSearch) {
                SearchBox(
                    query = state.searchQuery,
                    onQueryChange = { 
                        viewModel.handleEvent(IncomeListEvent.SearchQueryChanged(it))
                    },
                    onSearch = {},
                    placeholder = "Search income..."
                )
            }

            if (state.isLoading && state.entries.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Text(
                            text = "Loading income...",
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
                        Text("❌", fontSize = 40.sp)
                        Text(
                            text = state.error!!,
                            style = AppTypography.body,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = Spacing.medium)
                        )
                        TextButton(
                            onClick = {
                                viewModel.handleEvent(IncomeListEvent.LoadEntries)
                            }
                        ) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
                return@Column
            }

            if (state.entries.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("💰", fontSize = 48.sp)
                        Text(
                            text = if (state.searchQuery.isNotEmpty()) 
                                "No income found for '${state.searchQuery}'"
                            else 
                                "No income yet",
                            style = AppTypography.header,
                            modifier = Modifier.padding(top = Spacing.medium)
                        )
                        Text(
                            text = if (state.searchQuery.isNotEmpty()) 
                                "Try a different search"
                            else 
                                "Tap + to add your first income",
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
                        "${state.entries.size} results for '${state.searchQuery}'" 
                    else 
                        "${state.entries.size} income entries",
                    style = AppTypography.label,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                if (state.searchQuery.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            viewModel.handleEvent(IncomeListEvent.ClearSearch)
                            showSearch = false
                        }
                    ) {
                        Text(stringResource(R.string.clear))
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                contentPadding = PaddingValues(bottom = Spacing.xxlarge)
            ) {
                items(
                    items = state.entries,
                    key = { it.id }
                ) { entry ->
                    IncomeCard(
                        entry = entry,
                        streams = state.streams,
                        onDelete = {
                            pendingDeleteId = entry.id
                            showDeleteDialog = true
                        }
                    )
                }
            }
        }
    }

    // Filter Dialog
    if (showFilterDialog) {
        IncomeFilterDialog(
            streams = state.streams,
            selectedStreams = state.selectedStreamIds,
            onStreamToggle = { streamId ->
                viewModel.handleEvent(IncomeListEvent.ToggleStreamFilter(streamId))
            },
            onClearAll = {
                viewModel.handleEvent(IncomeListEvent.ClearStreamFilters)
            },
            onApply = {
                showFilterDialog = false
            },
            onDismiss = {
                showFilterDialog = false
            }
        )
    }
}

@Composable
fun IncomeCard(
    entry: IncomeEntry,
    streams: List<com.akari.retailer.features.sales.domain.models.IncomeStream>,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val stream = streams.find { it.id == entry.incomeStreamId }
    val streamName = stream?.getDisplayName() ?: "Unknown"
    
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                Text(
                    text = streamName,
                    style = AppTypography.title
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        color = when (entry.type) {
                            IncomeEntryType.BUSINESS -> MaterialTheme.colorScheme.primaryContainer
                            IncomeEntryType.PERSONAL -> MaterialTheme.colorScheme.secondaryContainer
                        },
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = if (entry.type == IncomeEntryType.BUSINESS) "💼 Business" else "👤 Personal",
                            style = AppTypography.small,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                
                if (entry.description.isNotEmpty()) {
                    Text(
                        text = entry.description,
                        style = AppTypography.body,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                
                Text(
                    text = dateFormat.format(entry.date),
                    style = AppTypography.small,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
            
            Text(
                text = "${entry.amount}",
                style = AppTypography.header,
                color = MaterialTheme.colorScheme.primary
            )
            
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
