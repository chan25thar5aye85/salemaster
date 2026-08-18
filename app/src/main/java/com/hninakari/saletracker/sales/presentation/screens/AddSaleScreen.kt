package com.hninakari.saletracker.sales.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.hninakari.saletracker.core.utils.formatCurrency
import com.hninakari.saletracker.sales.data.models.PaymentMethod
import com.hninakari.saletracker.sales.data.models.Sales
import com.hninakari.saletracker.sales.presentation.viewmodels.SaleViewModel
import java.time.format.DateTimeFormatter

@Composable
fun AddSaleScreen(
    viewModel: SaleViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val formState = uiState.saleFormState
    val keyboardController = LocalSoftwareKeyboardController.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    
    var amountError by remember { mutableStateOf<String?>(null) }
    
    // Clear form function
    fun clearForm() {
        viewModel.updateFormField(amount = "")
        viewModel.updateFormField(paymentMethod = PaymentMethod.CASH)
        amountError = null
        viewModel.clearError()
        keyboardController?.hide()
    }
    
    fun isFormValid(): Boolean {
        val amount = formState.amount.toDoubleOrNull()
        return amount != null && amount > 0.0
    }
    
    fun validateAndSubmit() {
        val amount = formState.amount.toDoubleOrNull()
        
        if (amount == null) {
            amountError = "Please enter a valid amount"
        } else if (amount <= 0.0) {
            amountError = "Amount must be greater than 0"
        } else {
            amountError = null
            keyboardController?.hide()
            viewModel.submitSale()
            if (uiState.error == null) {
                viewModel.updateFormField(amount = "")
                viewModel.updateFormField(paymentMethod = PaymentMethod.CASH)
                viewModel.clearError()
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = "✓ Sale added successfully!",
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }
    
    // Get recent 5 sales
    val recentSales = uiState.sales.take(5)
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .imePadding()
            .navigationBarsPadding()
            .systemBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Add New Sale",
            fontSize = 22.sp,
            style = MaterialTheme.typography.headlineSmall
        )
        
        // Form Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)  // Increased spacing between fields
            ) {
                // Amount Field
                Column {
                    Text(
                        text = "Amount *",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 4.dp)  // Added more space below label
                    )
                    OutlinedTextField(
                        value = formState.amount,
                        onValueChange = { 
                            if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                                viewModel.updateFormField(amount = it)
                                if (it.isNotEmpty()) {
                                    val amount = it.toDoubleOrNull()
                                    if (amount != null && amount > 0.0) {
                                        amountError = null
                                    }
                                } else {
                                    amountError = null
                                }
                            }
                        },
                        placeholder = { Text("Enter amount", fontSize = 16.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = androidx.compose.ui.text.input.ImeAction.Done
                        ),
                        singleLine = true,
                        isError = amountError != null,
                        textStyle = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp),
                        leadingIcon = {
                            Text(
                                text = "$",
                                fontSize = 20.sp,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                    if (amountError != null) {
                        Text(
                            text = amountError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)  // More space above error
                        )
                    }
                }
                
                // Payment - Label and Selector in a Row
                Column {
                    Text(
                        text = "Payment",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 4.dp)  // Added space below label
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.updateFormField(paymentMethod = PaymentMethod.CASH)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (formState.paymentMethod == PaymentMethod.CASH) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (formState.paymentMethod == PaymentMethod.CASH)
                                        MaterialTheme.colorScheme.onPrimary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                            ) {
                                Text("Cash", fontSize = 11.sp)
                            }
                            
                            Button(
                                onClick = {
                                    viewModel.updateFormField(paymentMethod = PaymentMethod.KPAY)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (formState.paymentMethod == PaymentMethod.KPAY) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (formState.paymentMethod == PaymentMethod.KPAY)
                                        MaterialTheme.colorScheme.onPrimary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                            ) {
                                Text("Kpay", fontSize = 11.sp)
                            }
                            
                            Button(
                                onClick = {
                                    viewModel.updateFormField(paymentMethod = PaymentMethod.WAVE_PAY)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (formState.paymentMethod == PaymentMethod.WAVE_PAY) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (formState.paymentMethod == PaymentMethod.WAVE_PAY)
                                        MaterialTheme.colorScheme.onPrimary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                            ) {
                                Text("Wave", fontSize = 11.sp)
                            }
                        }
                    }
                }
                
                Divider()
                
                // Action Buttons inside Card
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { clearForm() },
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text("Clear", fontSize = 13.sp)
                    }
                    
                    Button(
                        onClick = { validateAndSubmit() },
                        modifier = Modifier
                            .weight(2f)
                            .height(38.dp),
                        enabled = isFormValid() && !uiState.isLoading,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Add Sale", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
        
        // Recent Sales Section
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Recent Sales",
                fontSize = 18.sp,
                style = MaterialTheme.typography.titleMedium
            )
            
            if (recentSales.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = "No sales yet. Add your first sale!",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Header Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Amount",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.weight(2f)
                            )
                            Text(
                                text = "Payment",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "Date",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        Divider()
                        
                        // Recent Sales List
                        recentSales.forEach { sale ->
                            RecentSaleRow(sale = sale)
                            if (sale != recentSales.last()) {
                                Divider()
                            }
                        }
                    }
                }
            }
        }
        
        // Snackbar at bottom of screen
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )
    }
}

@Composable
fun RecentSaleRow(sale: Sales) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatCurrency(sale.amount),
            fontSize = 14.sp,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(2f)
        )
        
        Text(
            text = sale.paymentMethod.name,
            fontSize = 13.sp,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        
        Text(
            text = sale.saleDate.format(
                DateTimeFormatter.ofPattern("MMM dd, HH:mm")
            ),
            fontSize = 12.sp,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}
