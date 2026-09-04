package com.akari.retailer.core.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akari.retailer.R
import com.akari.retailer.features.sales.domain.models.PaymentMethod
import com.akari.retailer.core.ui.theme.AppTypography
import com.akari.retailer.core.ui.theme.Dimensions

@Composable
fun PaymentSelector(
    selectedMethod: PaymentMethod,
    onMethodSelected: (PaymentMethod) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
    ) {
        PaymentButton(
            method = PaymentMethod.CASH,
            isSelected = selectedMethod == PaymentMethod.CASH,
            onClick = { onMethodSelected(PaymentMethod.CASH) },
            modifier = Modifier.weight(1f)
        )
        
        PaymentButton(
            method = PaymentMethod.KPAY,
            isSelected = selectedMethod == PaymentMethod.KPAY,
            onClick = { onMethodSelected(PaymentMethod.KPAY) },
            modifier = Modifier.weight(1f)
        )
        
        PaymentButton(
            method = PaymentMethod.WAVEPAY,
            isSelected = selectedMethod == PaymentMethod.WAVEPAY,
            onClick = { onMethodSelected(PaymentMethod.WAVEPAY) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun PaymentButton(
    method: PaymentMethod,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(Dimensions.buttonHeight)
            .padding(horizontal = 0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            contentColor = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        ),
        shape = RoundedCornerShape(24.dp),  // ✅ Fully rounded (pill shape)
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 4.dp,
            vertical = 0.dp
        )
    ) {
        Text(
            text = when (method) {
                PaymentMethod.CASH -> stringResource(R.string.cash)
                PaymentMethod.KPAY -> stringResource(R.string.kpay)
                PaymentMethod.WAVEPAY -> stringResource(R.string.wave)
            },
            style = AppTypography.label,
            fontSize = 13.sp
        )
    }
}
