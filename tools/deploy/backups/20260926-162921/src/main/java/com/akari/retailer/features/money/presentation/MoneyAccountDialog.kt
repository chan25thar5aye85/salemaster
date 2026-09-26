package com.akari.retailer.features.money.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akari.retailer.R
import com.akari.retailer.core.ui.theme.AppTypography
import com.akari.retailer.core.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyAccountDialog(
    isEditing: Boolean,
    name: String,
    icon: String,
    color: String,
    openingBalance: String,
    accountNumber: String,
    notes: String,
    error: String?,
    isSaving: Boolean,
    onNameChange: (String) -> Unit,
    onIconChange: (String) -> Unit,
    onColorChange: (String) -> Unit,
    onOpeningBalanceChange: (String) -> Unit,
    onAccountNumberChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val availableIcons = listOf("💵", "📱", "🌊", "🏦", "💳", "💰", "🏪", "📊", "💎", "🎯")
    val availableColors = listOf(
        "#4CAF50", "#2196F3", "#FF9800", "#9C27B0", "#F44336",
        "#00BCD4", "#FF5722", "#795548", "#607D8B", "#E91E63"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) stringResource(R.string.edit_account) else stringResource(R.string.add_account),
                style = AppTypography.title
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.account_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isSaving
                )
                
                Spacer(modifier = Modifier.height(Spacing.medium))
                
                // Opening Balance
                OutlinedTextField(
                    value = openingBalance,
                    onValueChange = onOpeningBalanceChange,
                    label = { Text(stringResource(R.string.opening_balance)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isSaving,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                
                Spacer(modifier = Modifier.height(Spacing.medium))
                
                // Account Number (optional)
                OutlinedTextField(
                    value = accountNumber,
                    onValueChange = onAccountNumberChange,
                    label = { Text(stringResource(R.string.account_number)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isSaving
                )
                
                Spacer(modifier = Modifier.height(Spacing.medium))
                
                // Icon Selector
                Text(
                    text = stringResource(R.string.icon),
                    style = AppTypography.label,
                    modifier = Modifier.padding(bottom = Spacing.small)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    availableIcons.take(5).forEach { emoji ->
                        Surface(
                            modifier = Modifier
                                .size(40.dp)
                                .clickable(enabled = !isSaving) { onIconChange(emoji) },
                            shape = CircleShape,
                            color = if (icon == emoji) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(emoji, fontSize = 20.sp)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    availableIcons.drop(5).forEach { emoji ->
                        Surface(
                            modifier = Modifier
                                .size(40.dp)
                                .clickable(enabled = !isSaving) { onIconChange(emoji) },
                            shape = CircleShape,
                            color = if (icon == emoji) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(emoji, fontSize = 20.sp)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(Spacing.medium))
                
                // Color Selector
                Text(
                    text = stringResource(R.string.color),
                    style = AppTypography.label,
                    modifier = Modifier.padding(bottom = Spacing.small)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    availableColors.forEach { hexColor ->
                        val colorValue = Color(android.graphics.Color.parseColor(hexColor))
                        Surface(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable(enabled = !isSaving) { onColorChange(hexColor) }
                                .then(
                                    if (color == hexColor) {
                                        Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    } else {
                                        Modifier
                                    }
                                ),
                            color = colorValue
                        ) {}
                    }
                }
                
                Spacer(modifier = Modifier.height(Spacing.medium))
                
                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = onNotesChange,
                    label = { Text(stringResource(R.string.notes_optional)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    enabled = !isSaving
                )
                
                if (error != null) {
                    Spacer(modifier = Modifier.height(Spacing.small))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = AppTypography.small
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSave,
                enabled = !isSaving
            ) {
                Text(
                    text = if (isSaving) stringResource(R.string.saving) 
                           else if (isEditing) stringResource(R.string.update) 
                           else stringResource(R.string.add)
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSaving
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
