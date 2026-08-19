package com.hninakari.saletracker.sales.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.hninakari.saletracker.R
import com.hninakari.saletracker.core.ui.components.FloatingMenu
import com.hninakari.saletracker.core.utils.formatAmount
import com.hninakari.saletracker.sales.data.models.PaymentMethod
import com.hninakari.saletracker.sales.data.models.Sales
import com.hninakari.saletracker.sales.presentation.viewmodels.SaleViewModel
import java.time.format.DateTimeFormatter

@Composable
fun AddSaleScreen(
    viewModel: SaleViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSales: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val formState = uiState.saleFormState
    val keyboardController = LocalSoftwareKeyboardController.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    
    var amountError by remember { mutableStateOf<String?>(null) }
    
    // Get string resources
    val errorAmount = stringResource(R.string.add_sale_error_amount)
    val errorAmountZero = stringResource(R.string.add_sale_error_amount_zero)
    val successMessage = stringResource(R.string.add_sale_success)
    
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
            amountError = errorAmount
        } else if (amount <= 0.0) {
            amountError = errorAmountZero
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
                        message = successMessage,
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }
    
    // Get recent 5 sales
    val recentSales = uiState.sales.take(5)
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Main Content
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
            // Header Row with Title and View Sales button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.add_sale_title),
                    fontSize = 22.sp,
                    style = MaterialTheme.typography.headlineSmall
                )
                
                TextButton(
                    onClick = onNavigateToSales,
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(stringResource(R.string.nav_view_sales), fontSize = 13.sp)
                }
            }
            
            // Spacer between top bar and form
            Spacer(modifier = Modifier.height(8.dp))
            
            // Form Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Amount Field with floating label
                    Column {
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
                            label = { Text(stringResource(R.string.add_sale_amount)) },
                            placeholder = { Text(stringResource(R.string.add_sale_enter_amount), fontSize = 16.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = androidx.compose.ui.text.input.ImeAction.Done
                            ),
                            singleLine = true,
                            isError = amountError != null,
                            textStyle = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp)
                        )
                        if (amountError != null) {
                            Text(
                                text = amountError ?: "",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                            )
                        }
                    }
                    
                    // Payment - Label and Selector in a Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.add_sale_payment),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        
                        Row(
                            modifier = Modifier.weight(2.4f),
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
                                Text(stringResource(R.string.add_sale_cash), fontSize = 11.sp)
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
                                Text(stringResource(R.string.add_sale_kpay), fontSize = 11.sp)
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
                                Text(stringResource(R.string.add_sale_wave), fontSize = 11.sp)
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
                            Text(stringResource(R.string.add_sale_clear), fontSize = 13.sp)
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
                                Text(stringResource(R.string.add_sale_submit), fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
            
            // Spacer between form and table
            Spacer(modifier = Modifier.height(16.dp))
            
            // Recent Sales Section with larger font
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.recent_sales_title),
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
                            text = stringResource(R.string.recent_sales_empty),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Header Row with background - larger font
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.recent_sales_amount),
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.weight(1.5f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Text(
                                    text = stringResource(R.string.recent_sales_payment),
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.weight(1f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Text(
                                    text = stringResource(R.string.recent_sales_date),
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.weight(1f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                            
                            // Border line between header and rows
                            Divider(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.outline
                            )
                            
                            // Recent Sales List - larger font
                            recentSales.forEachIndexed { index, sale ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = formatAmount(sale.amount),
                                        fontSize = 16.sp,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.weight(1.5f),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                                    )
                                    
                                    Text(
                                        text = sale.paymentMethod.name,
                                        fontSize = 15.sp,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Start
                                    )
                                    
                                    Text(
                                        text = sale.saleDate.format(
                                            DateTimeFormatter.ofPattern("MMM dd, HH:mm")
                                        ),
                                        fontSize = 14.sp,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                                    )
                                }
                                
                                // Divider between rows (except after last row)
                                if (index < recentSales.size - 1) {
                                    Divider(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp),
                                        thickness = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Add bottom padding for the floating menu
            Spacer(modifier = Modifier.height(80.dp))
        }
        
        // Floating Menu
        FloatingMenu(
            onItemClick = { screen ->
                when (screen) {
                    "sales" -> onNavigateToSales()
                    "settings" -> onNavigateToSettings()
                }
            }
        )
    }
}
