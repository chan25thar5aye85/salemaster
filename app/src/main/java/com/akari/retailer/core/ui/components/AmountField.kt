package com.akari.retailer.core.ui.components

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.akari.retailer.core.ui.theme.AppTypography
import com.akari.retailer.core.utils.MoneyFormatter

@Composable
fun AmountField(
    amount: Int?,
    onAmountChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = FocusRequester(),
    imeAction: ImeAction = ImeAction.Next,
    onNext: () -> Unit = {},
    onFocus: () -> Unit = {},
    placeholder: String = "0"
) {
    var text by remember(amount) {
        mutableStateOf(
            amount?.takeIf { it > 0 }?.toString() ?: ""
        )
    }

    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(isFocused, amount) {
        if (isFocused) {
            text = amount?.takeIf { it > 0 }?.toString() ?: ""
        } else {
            text = amount?.takeIf { it > 0 }?.let {
                MoneyFormatter.formatForInput(it)
            } ?: ""
        }
    }

    OutlinedTextField(
        value = text,
        onValueChange = { value ->
            val cleaned = value.filter { it.isDigit() }
            text = cleaned
            onAmountChange(cleaned)
        },
        modifier = modifier
            .focusRequester(focusRequester)
            .onFocusChanged { focusState ->
                val wasFocused = isFocused
                isFocused = focusState.isFocused
                if (focusState.isFocused && !wasFocused) {
                    onFocus()
                }
            },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(
            onNext = { onNext() }
        ),
        singleLine = true,
        placeholder = {
            if (!isFocused) {
                Text(
                    text = placeholder,
                    style = AppTypography.body,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(
                        alpha = 0.3f
                    )
                )
            }
        }
    )
}
