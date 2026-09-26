package com.akari.retailer.core.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akari.retailer.R
import com.akari.retailer.core.ui.theme.AppTypography
import com.akari.retailer.core.ui.theme.Spacing

@Composable
fun ItemRow(
    index: Int,
    amount: Int?,
    onAmountChange: (String) -> Unit,
    onNext: () -> Unit = {},
    onFocus: () -> Unit = {},
    onDelete: () -> Unit = {},
    focusRequester: FocusRequester = FocusRequester(),
    showDelete: Boolean = true,
    label: String = stringResource(R.string.item_number, index + 1),
    placeholder: String = "0"
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp), // ✅ Added vertical padding for each row
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = AppTypography.body.copy(
                fontSize = 15.sp // ✅ Increased font size
            ),
            modifier = Modifier
                .width(70.dp) // ✅ Increased from 60dp to 70dp for better readability
        )

        AmountField(
            amount = amount,
            onAmountChange = onAmountChange,
            modifier = Modifier.weight(1f),
            focusRequester = focusRequester,
            imeAction = androidx.compose.ui.text.input.ImeAction.Next,
            onNext = onNext,
            onFocus = onFocus,
            placeholder = placeholder
        )

        if (showDelete) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .padding(start = Spacing.medium) // ✅ Increased from small to medium
                    .width(48.dp) // ✅ Fixed width for delete button
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete item",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        } else {
            Spacer(modifier = Modifier.width(56.dp)) // ✅ Increased from 48dp to 56dp
        }
    }
}
