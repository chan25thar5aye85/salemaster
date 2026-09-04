package com.akari.retailer.features.customer.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akari.retailer.R
import com.akari.retailer.RetailApplication
import com.akari.retailer.core.ui.components.AppPrimaryButton
import com.akari.retailer.core.ui.components.AppScreen
import com.akari.retailer.core.ui.theme.AppTypography
import com.akari.retailer.core.ui.theme.Spacing
import com.akari.retailer.features.customer.data.repository.FirestoreCustomerRepository
import com.akari.retailer.features.customer.data.remote.FirestoreCustomerService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerAddScreen(
    onBack: () -> Unit,
    onCustomerAdded: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as RetailApplication
    
    val service = remember { FirestoreCustomerService() }
    val repository = remember { FirestoreCustomerRepository(service) }
    
    val viewModel: CustomerAddViewModel = viewModel(
        factory = CustomerAddViewModelFactory(repository)
    )
    
    val state by viewModel.state.collectAsState()

    AppScreen(
        title = "Add Customer",
        showBackButton = true,
        onBackClick = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Name
            OutlinedTextField(
                value = state.name,
                onValueChange = { viewModel.handleEvent(CustomerAddEvent.NameChanged(it)) },
                label = { Text("Name *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            // Phone
            OutlinedTextField(
                value = state.phone,
                onValueChange = { viewModel.handleEvent(CustomerAddEvent.PhoneChanged(it)) },
                label = { Text("Phone") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            // Email
            OutlinedTextField(
                value = state.email,
                onValueChange = { viewModel.handleEvent(CustomerAddEvent.EmailChanged(it)) },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            // Address
            OutlinedTextField(
                value = state.address,
                onValueChange = { viewModel.handleEvent(CustomerAddEvent.AddressChanged(it)) },
                label = { Text("Address") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            // Notes
            OutlinedTextField(
                value = state.notes,
                onValueChange = { viewModel.handleEvent(CustomerAddEvent.NotesChanged(it)) },
                label = { Text("Notes") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            )
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            // Error message
            state.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = AppTypography.body,
                    modifier = Modifier.padding(bottom = Spacing.medium)
                )
            }
            
            // Save button
            AppPrimaryButton(
                text = if (state.isSaving) "Saving..." else "Save Customer",
                onClick = {
                    viewModel.handleEvent(CustomerAddEvent.SaveCustomer)
                },
                isLoading = state.isSaving,
                enabled = state.name.isNotEmpty() && !state.isSaving
            )
            
            if (state.saveSuccess) {
                LaunchedEffect(Unit) {
                    onCustomerAdded()
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.xxlarge))
        }
    }
}
