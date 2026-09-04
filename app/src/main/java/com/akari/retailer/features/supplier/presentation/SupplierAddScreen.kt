package com.akari.retailer.features.supplier.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akari.retailer.RetailApplication
import com.akari.retailer.core.ui.components.AppPrimaryButton
import com.akari.retailer.core.ui.components.AppScreen
import com.akari.retailer.core.ui.theme.AppTypography
import com.akari.retailer.core.ui.theme.Spacing
import com.akari.retailer.features.supplier.data.repository.FirestoreSupplierRepository
import com.akari.retailer.features.supplier.data.remote.FirestoreSupplierService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierAddScreen(
    onBack: () -> Unit,
    onSupplierAdded: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as RetailApplication
    
    val service = remember { FirestoreSupplierService() }
    val repository = remember { FirestoreSupplierRepository(service) }
    
    val viewModel: SupplierAddViewModel = viewModel(
        factory = SupplierAddViewModelFactory(repository)
    )
    
    val state by viewModel.state.collectAsState()

    AppScreen(
        title = "Add Supplier",
        showBackButton = true,
        onBackClick = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = { viewModel.handleEvent(SupplierAddEvent.NameChanged(it)) },
                label = { Text("Name *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            OutlinedTextField(
                value = state.company,
                onValueChange = { viewModel.handleEvent(SupplierAddEvent.CompanyChanged(it)) },
                label = { Text("Company") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            OutlinedTextField(
                value = state.phone,
                onValueChange = { viewModel.handleEvent(SupplierAddEvent.PhoneChanged(it)) },
                label = { Text("Phone") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            OutlinedTextField(
                value = state.email,
                onValueChange = { viewModel.handleEvent(SupplierAddEvent.EmailChanged(it)) },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            OutlinedTextField(
                value = state.address,
                onValueChange = { viewModel.handleEvent(SupplierAddEvent.AddressChanged(it)) },
                label = { Text("Address") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            OutlinedTextField(
                value = state.notes,
                onValueChange = { viewModel.handleEvent(SupplierAddEvent.NotesChanged(it)) },
                label = { Text("Notes") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            )
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            state.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = AppTypography.body,
                    modifier = Modifier.padding(bottom = Spacing.medium)
                )
            }
            
            AppPrimaryButton(
                text = if (state.isSaving) "Saving..." else "Save Supplier",
                onClick = {
                    viewModel.handleEvent(SupplierAddEvent.SaveSupplier)
                },
                isLoading = state.isSaving,
                enabled = state.name.isNotEmpty() && !state.isSaving
            )
            
            if (state.saveSuccess) {
                LaunchedEffect(Unit) {
                    onSupplierAdded()
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.xxlarge))
        }
    }
}
