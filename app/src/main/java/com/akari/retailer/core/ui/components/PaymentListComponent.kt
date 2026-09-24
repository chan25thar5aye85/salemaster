package com.akari.retailer.core.ui.components

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
import com.akari.retailer.features.money.domain.models.MoneyAccount
import androidx.compose.ui.res.stringResource
import com.akari.retailer.R

/**
 * A single payment row (account + amount).
 * Used by all entry screens for split payments.
 */
data class PaymentRow(
    val id: Long,
    val accountId: String = "default_cash",
    val amount: String = "",
    val customerId: String = ""
)

/**
 * Reusable component for splitting payments across multiple accounts.
 *
 * @param paymentRows List of payment rows
 * @param accounts Available money accounts to choose from
 * @param totalAmount Target total amount to pay (0 = no validation)
 * @param showSummary Whether to show the "Total: X / Y" summary card
 * @param onAccountSelected Called when account changes for a row
 * @param onAmountChanged Called when amount changes for a row
 * @param onAddRow Called when user taps "+ Add Payment"
 * @param onRemoveRow Called when user taps "✕" on a row
 */
@Composable
fun PaymentListComponent(
    paymentRows: List<PaymentRow>,
    accounts: List<MoneyAccount>,
    totalAmount: Int = 0,
    showSummary: Boolean = true,
    onAccountSelected: (rowId: Long, account: MoneyAccount) -> Unit,
    onAmountChanged: (rowId: Long, amount: String) -> Unit,
    onAddRow: () -> Unit,
    onRemoveRow: (rowId: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Header
        Text(
            text = stringResource(R.string.payments),
            style = AppTypography.title,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = Spacing.small)
        )
        
        // Payment rows
        paymentRows.forEachIndexed { index, row ->
            SinglePaymentRow(
                row = row,
                accounts = accounts,
                isOnlyRow = paymentRows.size == 1,
                onAccountSelected = { account -> onAccountSelected(row.id, account) },
                onAmountChanged = { amount -> onAmountChanged(row.id, amount) },
                onRemove = { onRemoveRow(row.id) }
            )
            Spacer(modifier = Modifier.height(Spacing.small))
        }
        
        // Add Payment button
        OutlinedButton(
            onClick = onAddRow,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.add_another_payment))
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
 * Single payment row: Account dropdown + Amount field + Remove button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SinglePaymentRow(
    row: PaymentRow,
    accounts: List<MoneyAccount>,
    isOnlyRow: Boolean,
    onAccountSelected: (MoneyAccount) -> Unit,
    onAmountChanged: (String) -> Unit,
    onRemove: () -> Unit
) {
    var accountExpanded by remember { mutableStateOf(false) }
    val selectedAccount = accounts.find { it.id == row.accountId }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Account Dropdown
        ExposedDropdownMenuBox(
            expanded = accountExpanded,
            onExpandedChange = { accountExpanded = it },
            modifier = Modifier.weight(1.2f)
        ) {
            OutlinedTextField(
                value = selectedAccount?.getDisplayName() ?: "Select Account",
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.account_field), fontSize = 12.sp) },
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
            label = { Text(stringResource(R.string.amount_field), fontSize = 12.sp) },
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
}
