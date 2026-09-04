package com.akari.retailer.features.customer.presentation

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
import com.akari.retailer.features.customer.data.repository.FirestoreCustomerRepository
import com.akari.retailer.features.customer.data.remote.FirestoreCustomerService
import com.akari.retailer.features.customer.domain.models.Customer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerEditScreen(
    customerId: String,
    onBack: () -> Unit,
    onCustomerUpdated: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as RetailApplication
    
    val service = remember { FirestoreCustomerService() }
    val repository = remember { FirestoreCustomerRepository(service) }
    
    val viewModel: CustomerEditViewModel = viewModel(
        factory = CustomerEditViewModelFactory(repository, customerId)
    )
    
    val state by viewModel.state.collectAsState()

    AppScreen(
        title = "Edit Customer",
        showBackButton = true,
        onBackClick = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
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
                        Text(
                            text = "❌",
                            style = AppTypography.header
                        )
                        Text(
                            text = state.error!!,
                            style = AppTypography.body,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = Spacing.medium)
                        )
                        AppPrimaryButton(
                            text = "Retry",
                            onClick = {
                                viewModel.handleEvent(CustomerEditEvent.LoadCustomer)
                            }
                        )
                    }
                }
                return@Column
            }

            // Name
            OutlinedTextField(
                value = state.name,
                onValueChange = { viewModel.handleEvent(CustomerEditEvent.NameChanged(it)) },
                label = { Text("Name *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            // Phone
            OutlinedTextField(
                value = state.phone,
                onValueChange = { viewModel.handleEvent(CustomerEditEvent.PhoneChanged(it)) },
                label = { Text("Phone") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            // Email
            OutlinedTextField(
                value = state.email,
                onValueChange = { viewModel.handleEvent(CustomerEditEvent.EmailChanged(it)) },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            // Address
            OutlinedTextField(
                value = state.address,
                onValueChange = { viewModel.handleEvent(CustomerEditEvent.AddressChanged(it)) },
                label = { Text("Address") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            // Notes
            OutlinedTextField(
                value = state.notes,
                onValueChange = { viewModel.handleEvent(CustomerEditEvent.NotesChanged(it)) },
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
                text = if (state.isSaving) "Updating..." else "Update Customer",
                onClick = {
                    viewModel.handleEvent(CustomerEditEvent.SaveCustomer)
                },
                isLoading = state.isSaving,
                enabled = state.name.isNotEmpty() && !state.isSaving
            )
            
            if (state.saveSuccess) {
                LaunchedEffect(Unit) {
                    onCustomerUpdated()
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.xxlarge))
        }
    }
}
