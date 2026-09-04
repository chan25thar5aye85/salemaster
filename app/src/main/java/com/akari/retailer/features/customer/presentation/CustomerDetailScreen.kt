package com.akari.retailer.features.customer.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import com.akari.retailer.R
import com.akari.retailer.RetailApplication
import com.akari.retailer.core.ui.components.AppCard
import com.akari.retailer.core.ui.components.AppScreen
import com.akari.retailer.core.ui.components.AppPrimaryButton
import com.akari.retailer.core.ui.theme.AppTypography
import com.akari.retailer.core.ui.theme.Spacing
import com.akari.retailer.features.customer.data.repository.FirestoreCustomerRepository
import com.akari.retailer.features.customer.data.remote.FirestoreCustomerService
import com.akari.retailer.features.customer.domain.models.Customer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    customerId: String,
    onBack: () -> Unit,
    onEdit: (Customer) -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as RetailApplication
    
    val service = remember { FirestoreCustomerService() }
    val repository = remember { FirestoreCustomerRepository(service) }
    
    val viewModel: CustomerDetailViewModel = viewModel(
        factory = CustomerDetailViewModelFactory(repository, customerId)
    )
    
    val state by viewModel.state.collectAsState()

    AppScreen(
        title = "Customer Details",
        showBackButton = true,
        onBackClick = onBack,
        showSearchButton = false,
        showDateFilter = false
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Text(
                            text = "Loading customer...",
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
                        AppPrimaryButton(
                            text = "Retry",
                            onClick = {
                                viewModel.handleEvent(CustomerDetailEvent.LoadCustomer)
                            }
                        )
                    }
                }
                return@Column
            }

            state.customer?.let { customer ->
                // Customer Info Card
                AppCard {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Name
                        Text(
                            text = customer.name,
                            style = AppTypography.header
                        )
                        
                        Spacer(modifier = Modifier.height(Spacing.medium))
                        
                        // Phone
                        if (customer.phone.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "📱 Phone",
                                    style = AppTypography.body,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = customer.phone,
                                    style = AppTypography.body
                                )
                            }
                            Spacer(modifier = Modifier.height(Spacing.small))
                        }
                        
                        // Email
                        if (customer.email.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "✉️ Email",
                                    style = AppTypography.body,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = customer.email,
                                    style = AppTypography.body
                                )
                            }
                            Spacer(modifier = Modifier.height(Spacing.small))
                        }
                        
                        // Address
                        if (customer.address.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "📍 Address",
                                    style = AppTypography.body,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = customer.address,
                                    style = AppTypography.body
                                )
                            }
                            Spacer(modifier = Modifier.height(Spacing.small))
                        }
                        
                        // Notes
                        if (customer.notes.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "📝 Notes",
                                    style = AppTypography.body,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = customer.notes,
                                    style = AppTypography.body
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.medium))

                // Stats Card
                AppCard {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Stats",
                            style = AppTypography.title,
                            modifier = Modifier.padding(bottom = Spacing.medium)
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Total Spent",
                                    style = AppTypography.small,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = "${customer.totalSpent}",
                                    style = AppTypography.header,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Total Orders",
                                    style = AppTypography.small,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = "${customer.totalOrders}",
                                    style = AppTypography.header,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.medium))

                // Edit button
                AppPrimaryButton(
                    text = "Edit Customer",
                    onClick = {
                        onEdit(customer)
                    }
                )

                Spacer(modifier = Modifier.height(Spacing.xxlarge))
            }
        }
    }
}
