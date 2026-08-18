package com.hninakari.saletracker.sales.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hninakari.saletracker.core.utils.formatCurrency
import com.hninakari.saletracker.sales.presentation.viewmodels.SaleViewModel
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaleDetailScreen(
    viewModel: SaleViewModel,
    saleId: String,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val sale = uiState.sales.find { it.id == saleId }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sale Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (sale != null) {
                        IconButton(onClick = { 
                            viewModel.deleteSale(saleId)
                            onNavigateBack()
                        }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (sale == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Sale not found",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Sale Details",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Divider()
                        DetailRow(
                            label = "Amount",
                            value = formatCurrency(sale.amount),
                            isHighlighted = true
                        )
                        DetailRow(
                            label = "Payment Method",
                            value = sale.paymentMethod.name
                        )
                        DetailRow(
                            label = "Date",
                            value = sale.saleDate.format(
                                DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")
                            )
                        )
                    }
                }
                
                Button(
                    onClick = onNavigateBack,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Back to Sales")
                }
            }
        }
    }
}

@Composable
fun DetailRow(
    label: String,
    value: String,
    isHighlighted: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = if (isHighlighted) 
                MaterialTheme.typography.titleMedium 
            else 
                MaterialTheme.typography.bodyMedium,
            color = if (isHighlighted)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurface
        )
    }
}
