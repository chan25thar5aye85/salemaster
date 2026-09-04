package com.akari.retailer.features.supplier.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
import com.akari.retailer.core.ui.components.AppScreen
import com.akari.retailer.core.ui.components.SearchBox
import com.akari.retailer.core.ui.theme.AppTypography
import com.akari.retailer.core.ui.theme.Spacing
import com.akari.retailer.features.supplier.data.repository.FirestoreSupplierRepository
import com.akari.retailer.features.supplier.data.remote.FirestoreSupplierService
import com.akari.retailer.features.supplier.domain.models.Supplier
import com.akari.retailer.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierListScreen(
    navController: NavController,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as RetailApplication
    
    val service = remember { FirestoreSupplierService() }
    val repository = remember { FirestoreSupplierRepository(service) }
    
    val viewModel: SupplierListViewModel = viewModel(
        factory = SupplierListViewModelFactory(repository)
    )
    
    val state by viewModel.state.collectAsState()
    
    var showDeleteDialog by remember { mutableStateOf(false) }
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }
    var showSearch by remember { mutableStateOf(false) }

    if (showDeleteDialog && pendingDeleteId != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                pendingDeleteId = null
            },
            title = { Text("Delete Supplier") },
            text = { Text("Are you sure you want to delete this supplier?") },
            confirmButton = {
                Button(
                    onClick = {
                        pendingDeleteId?.let { viewModel.handleEvent(SupplierListEvent.DeleteSupplier(it)) }
                        showDeleteDialog = false
                        pendingDeleteId = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
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
        title = "Suppliers",
        showBackButton = true,
        onBackClick = onBack,
        showSearchButton = true,
        onSearchClick = { showSearch = !showSearch },
        showAddButton = true,
        onAddClick = { navController.navigate(Routes.SUPPLIER_ADD) }
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            if (showSearch) {
                SearchBox(
                    query = state.searchQuery,
                    onQueryChange = { 
                        viewModel.handleEvent(SupplierListEvent.SearchQueryChanged(it))
                    },
                    onSearch = {},
                    placeholder = "Search by name or phone...",
                    modifier = Modifier.padding(bottom = Spacing.medium)
                )
            }

            if (state.isLoading && state.suppliers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Text(
                            text = "Loading suppliers...",
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
                        TextButton(
                            onClick = {
                                viewModel.handleEvent(SupplierListEvent.LoadSuppliers)
                            }
                        ) {
                            Text("Retry")
                        }
                    }
                }
                return@Column
            }

            if (state.suppliers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (state.searchQuery.isNotEmpty()) "🔍" else "🏪",
                            fontSize = 48.sp
                        )
                        Text(
                            text = if (state.searchQuery.isNotEmpty()) "No suppliers found for '${state.searchQuery}'" else "No suppliers yet",
                            style = AppTypography.header,
                            modifier = Modifier.padding(top = Spacing.medium)
                        )
                        Text(
                            text = if (state.searchQuery.isNotEmpty()) "Try a different search" else "Tap + to add your first supplier",
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
                        "${state.suppliers.size} results for '${state.searchQuery}'" 
                    else 
                        "${state.suppliers.size} suppliers",
                    style = AppTypography.label,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                if (state.searchQuery.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            viewModel.handleEvent(SupplierListEvent.ClearSearch)
                            showSearch = false
                        }
                    ) {
                        Text("Clear")
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                contentPadding = PaddingValues(bottom = Spacing.xxlarge)
            ) {
                items(
                    items = state.suppliers,
                    key = { it.id }
                ) { supplier ->
                    SupplierCard(
                        supplier = supplier,
                        onDelete = {
                            pendingDeleteId = supplier.id
                            showDeleteDialog = true
                        },
                        onClick = {
                            navController.navigate(Routes.SUPPLIER_DETAIL.replace("{supplierId}", supplier.id))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SupplierCard(
    supplier: Supplier,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
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
                    text = supplier.name,
                    style = AppTypography.title
                )
                
                if (supplier.company.isNotEmpty()) {
                    Text(
                        text = "🏢 ${supplier.company}",
                        style = AppTypography.body,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                
                if (supplier.phone.isNotEmpty()) {
                    Text(
                        text = "📱 ${supplier.phone}",
                        style = AppTypography.body,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.small),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Total: ${supplier.totalPurchased}",
                        style = AppTypography.small,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (supplier.products.isNotEmpty()) {
                        Text(
                            text = "${supplier.products.size} products",
                            style = AppTypography.small,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
            
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                )
            }
        }
    }
}
