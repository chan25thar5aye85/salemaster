package com.akari.retailer.features.sales.presentation.stream

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import com.akari.retailer.core.ui.theme.AppTypography
import com.akari.retailer.core.ui.theme.Spacing
import com.akari.retailer.features.sales.data.repository.FirestoreIncomeStreamRepository
import com.akari.retailer.features.sales.data.remote.FirestoreIncomeStreamService
import com.akari.retailer.features.sales.domain.models.IncomeStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeStreamManagementScreen(
    navController: NavController,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as RetailApplication
    
    val service = remember { FirestoreIncomeStreamService() }
    val repository = remember { FirestoreIncomeStreamRepository(service) }
    
    val viewModel: IncomeStreamManagementViewModel = viewModel(
        factory = IncomeStreamManagementViewModelFactory(repository)
    )
    
    val state by viewModel.state.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }

    if (showDeleteDialog && pendingDeleteId != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                pendingDeleteId = null
            },
            title = { Text("Delete Income Stream") },
            text = { Text("Are you sure you want to delete this income stream?") },
            confirmButton = {
                Button(
                    onClick = {
                        pendingDeleteId?.let { 
                            viewModel.handleEvent(IncomeStreamManagementEvent.DeleteStream(it))
                        }
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
        title = "Manage Income Streams",
        showBackButton = true,
        onBackClick = onBack,
        showAddButton = true,
        onAddClick = { viewModel.handleEvent(IncomeStreamManagementEvent.ShowAddDialog) }
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            TabRow(selectedTabIndex = state.selectedTab) {
                listOf("All", "Default", "Custom").forEachIndexed { index, label ->
                    val count = when (index) {
                        0 -> state.streams.size
                        1 -> state.defaultStreams.size
                        2 -> state.customStreams.size
                        else -> 0
                    }
                    Tab(
                        selected = state.selectedTab == index,
                        onClick = { viewModel.handleEvent(IncomeStreamManagementEvent.SelectTab(index)) },
                        text = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(label)
                                if (count > 0) {
                                    Badge(
                                        containerColor = if (state.selectedTab == index) 
                                            MaterialTheme.colorScheme.primary 
                                        else 
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                    ) {
                                        Text("$count")
                                    }
                                }
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            val filteredStreams = viewModel.getFilteredStreams()

            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
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
                                viewModel.handleEvent(IncomeStreamManagementEvent.LoadStreams)
                            }
                        ) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
                return@Column
            }

            if (filteredStreams.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📂", fontSize = 48.sp)
                        Text(
                            text = when (state.selectedTab) {
                                1 -> "No default income streams"
                                2 -> "No custom income streams yet"
                                else -> "No income streams found"
                            },
                            style = AppTypography.header,
                            modifier = Modifier.padding(top = Spacing.medium)
                        )
                        if (state.selectedTab == 2) {
                            Text(
                                text = "Tap + to add your first custom income stream",
                                style = AppTypography.body,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
                return@Column
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(Spacing.small),
                contentPadding = PaddingValues(bottom = Spacing.xxlarge)
            ) {
                items(
                    items = filteredStreams,
                    key = { it.id }
                ) { stream ->
                    AppCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = Spacing.small),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "${stream.icon} ${stream.name}",
                                    style = AppTypography.body
                                )
                                if (stream.isDefault) {
                                    Text(
                                        text = "Default",
                                        style = AppTypography.small,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }
                            
                            if (!stream.isDefault) {
                                IconButton(
                                    onClick = { 
                                        viewModel.handleEvent(IncomeStreamManagementEvent.ShowEditDialog(stream))
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        pendingDeleteId = stream.id
                                        showDeleteDialog = true
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            } else {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        text = "Default",
                                        style = AppTypography.small,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (state.showDialog) {
        IncomeStreamDialog(
            isEditing = state.editingStream != null,
            name = state.dialogName,
            error = state.error,
            isSaving = state.isSaving,
            onNameChange = { 
                viewModel.handleEvent(IncomeStreamManagementEvent.DialogNameChanged(it))
            },
            onSave = {
                viewModel.handleEvent(IncomeStreamManagementEvent.SaveStream)
            },
            onDismiss = {
                viewModel.handleEvent(IncomeStreamManagementEvent.DismissDialog)
            }
        )
    }
}
