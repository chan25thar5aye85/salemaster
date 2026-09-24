package com.akari.retailer.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akari.retailer.core.ui.theme.AppTypography
import com.akari.retailer.core.ui.theme.Spacing
import com.akari.retailer.features.customer.domain.models.Customer
import com.akari.retailer.features.money.domain.models.CreditAccount
import com.akari.retailer.features.money.domain.models.MoneyAccount

/**
 * A single payment row (account + amount + optional customer for credit).
 */
data class PaymentRow(
    val id: Long,
    val accountId: String = "default_cash",
    val amount: String = "",
    val customerId: String = ""
)

/**
 * Reusable component for splitting payments across multiple accounts
 * OR charging part of the sale to a customer's credit.
 */
@Composable
fun PaymentListComponent(
    paymentRows: List<PaymentRow>,
    accounts: List<MoneyAccount>,
    customers: List<Customer> = emptyList(),
    totalAmount: Int = 0,
    showSummary: Boolean = true,
    /** When false, the "Credit" option is hidden from the account dropdown. */
    showCreditOption: Boolean = true,
    onAccountSelected: (rowId: Long, account: MoneyAccount) -> Unit,
    onCreditSelected: (rowId: Long) -> Unit = {},
    onCustomerSelected: (rowId: Long, customer: Customer) -> Unit = { _, _ -> },
    onAmountChanged: (rowId: Long, amount: String) -> Unit,
    onAddRow: () -> Unit,
    onRemoveRow: (rowId: Long) -> Unit,
    modifier: Modifier = Modifier,
    showAddButton: Boolean = true
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Header
        Text(
            text = "Payments",
            style = AppTypography.title,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = Spacing.small)
        )

        // Payment rows
        paymentRows.forEach { row ->
            SinglePaymentRow(
                row = row,
                accounts = accounts,
                customers = customers,
                isOnlyRow = paymentRows.size == 1,
                showCreditOption = showCreditOption,
                onAccountSelected = { account -> onAccountSelected(row.id, account) },
                onCreditSelected = { onCreditSelected(row.id) },
                onCustomerSelected = { customer -> onCustomerSelected(row.id, customer) },
                onAmountChanged = { amount -> onAmountChanged(row.id, amount) },
                onRemove = { onRemoveRow(row.id) }
            )
            Spacer(modifier = Modifier.height(Spacing.small))
        }

        // Add Payment button
        if (showAddButton) {
            OutlinedButton(
                onClick = onAddRow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Another Payment")
            }
        }

        // Summary card
        if (showSummary && totalAmount > 0) {
            Spacer(modifier = Modifier.height(Spacing.small))

            val totalPaid = paymentRows.sumOf { it.amount.toIntOrNull() ?: 0 }
            val remaining = totalAmount - totalPaid
            val isComplete = totalPaid == totalAmount
            val isOver = totalPaid > totalAmount

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        isComplete -> MaterialTheme.colorScheme.primaryContainer
                        isOver -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.small),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total: $totalPaid / $totalAmount",
                        style = AppTypography.label,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when {
                            isComplete -> "✅ Complete"
                            isOver -> "⚠️ Over by ${-remaining}"
                            else -> "Remaining: $remaining"
                        },
                        style = AppTypography.label,
                        color = when {
                            isComplete -> MaterialTheme.colorScheme.primary
                            isOver -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Single payment row: Account dropdown (with Credit option) + Amount + optional customer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SinglePaymentRow(
    row: PaymentRow,
    accounts: List<MoneyAccount>,
    customers: List<Customer>,
    isOnlyRow: Boolean,
    showCreditOption: Boolean,
    onAccountSelected: (MoneyAccount) -> Unit,
    onCreditSelected: () -> Unit,
    onCustomerSelected: (Customer) -> Unit,
    onAmountChanged: (String) -> Unit,
    onRemove: () -> Unit
) {
    var accountExpanded by remember { mutableStateOf(false) }
    var customerExpanded by remember { mutableStateOf(false) }

    val isCredit = row.accountId == CreditAccount.ID
    val selectedAccount = accounts.find { it.id == row.accountId }
    val selectedCustomer = customers.find { it.id == row.customerId }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Main row: account + amount + delete
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Account dropdown (with Credit at top)
            ExposedDropdownMenuBox(
                expanded = accountExpanded,
                onExpandedChange = { accountExpanded = it },
                modifier = Modifier.weight(1.2f)
            ) {
                val displayValue = when {
                    isCredit -> "💳 ${CreditAccount.DISPLAY_NAME}"
                    selectedAccount != null -> selectedAccount.getDisplayName()
                    else -> "Select Account"
                }

                OutlinedTextField(
                    value = displayValue,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Account", fontSize = 12.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded)
                    },
                    singleLine = true
                )

                ExposedDropdownMenu(
                    expanded = accountExpanded,
                    onDismissRequest = { accountExpanded = false }
                ) {
                    // Credit option at the top (only if allowed)
                    if (showCreditOption) {
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("💳", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = CreditAccount.DISPLAY_NAME,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Charge to customer",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        },
                        onClick = {
                            onCreditSelected()
                            accountExpanded = false
                        }
                    )

                    HorizontalDivider()
                    }

                    // Money accounts
                    accounts.forEach { account ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(account.getDisplayName(), fontSize = 13.sp)
                                    Text(
                                        text = "${account.currentBalance}",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            },
                            onClick = {
                                onAccountSelected(account)
                                accountExpanded = false
                            }
                        )
                    }
                }
            }

            // Amount field
            OutlinedTextField(
                value = row.amount,
                onValueChange = { value ->
                    val digitsOnly = value.filter { it.isDigit() }
                    onAmountChanged(digitsOnly)
                },
                label = { Text("Amount", fontSize = 12.sp) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            // Remove button (only if more than one row)
            if (!isOnlyRow) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(40.dp))
            }
        }

        // Customer picker — only when this row is a credit row AND credit is allowed
        if (isCredit && showCreditOption) {
            Spacer(modifier = Modifier.height(Spacing.small))
            ExposedDropdownMenuBox(
                expanded = customerExpanded,
                onExpandedChange = { customerExpanded = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp)
            ) {
                OutlinedTextField(
                    value = selectedCustomer?.name ?: "Select Customer",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Customer (who owes)", fontSize = 12.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerExpanded)
                    },
                    singleLine = true,
                    isError = selectedCustomer == null
                )

                ExposedDropdownMenu(
                    expanded = customerExpanded,
                    onDismissRequest = { customerExpanded = false }
                ) {
                    if (customers.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No customers — add one first") },
                            onClick = { customerExpanded = false }
                        )
                    } else {
                        customers.forEach { customer ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(customer.name, fontSize = 13.sp)
                                        if (customer.phone.isNotEmpty()) {
                                            Text(
                                                text = customer.phone,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    onCustomerSelected(customer)
                                    customerExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
