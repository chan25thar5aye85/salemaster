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
        title = stringResource(R.string.edit_customer),
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
                            text = stringResource(R.string.retry),
                            onClick = {
                                viewModel.handleEvent(CustomerEditEvent.LoadCustomer)
                            }
                        )
                    }
                }
                return@Column
            }

            OutlinedTextField(
                value = state.name,
                onValueChange = { viewModel.handleEvent(CustomerEditEvent.NameChanged(it)) },
                label = { Text(stringResource(R.string.name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            OutlinedTextField(
                value = state.phone,
                onValueChange = { viewModel.handleEvent(CustomerEditEvent.PhoneChanged(it)) },
                label = { Text(stringResource(R.string.phone)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            OutlinedTextField(
                value = state.email,
                onValueChange = { viewModel.handleEvent(CustomerEditEvent.EmailChanged(it)) },
                label = { Text(stringResource(R.string.email)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            OutlinedTextField(
                value = state.address,
                onValueChange = { viewModel.handleEvent(CustomerEditEvent.AddressChanged(it)) },
                label = { Text(stringResource(R.string.address)) },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            OutlinedTextField(
                value = state.notes,
                onValueChange = { viewModel.handleEvent(CustomerEditEvent.NotesChanged(it)) },
                label = { Text(stringResource(R.string.notes)) },
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
                text = if (state.isSaving) stringResource(R.string.updating) else stringResource(R.string.update_customer),
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
