package com.akari.retailer.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.akari.retailer.R
import com.akari.retailer.core.ui.theme.AppTypography
import com.akari.retailer.core.ui.theme.Spacing
import com.akari.retailer.features.sales.domain.models.PaymentMethod
import com.akari.retailer.features.sales.domain.models.Sale
import com.akari.retailer.core.utils.MoneyFormatter
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SaleCard(
    sale: Sale,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
    val dateString = dateFormat.format(Date(sale.timestamp))
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Date and time
                Text(
                    text = "🕐 $dateString",
                    style = AppTypography.small,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Items count and total
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${sale.items.size} ${stringResource(R.string.item).lowercase()}",
                        style = AppTypography.body,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = " • ",
                        style = AppTypography.body,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "${stringResource(R.string.total)}: ${MoneyFormatter.formatTotal(sale.total)}",
                        style = AppTypography.body,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                // Payment method
                Text(
                    text = "💳 ${getPaymentDisplayName(sale.paymentMethod)}",
                    style = AppTypography.small,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            
            // Delete button
            IconButton(
                onClick = onDelete,
                modifier = Modifier.width(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                )
            }
        }
    }
}

private fun getPaymentDisplayName(method: PaymentMethod): String {
    return when (method) {
        PaymentMethod.CASH -> "Cash"
        PaymentMethod.KPAY -> "KPay"
        PaymentMethod.WAVEPAY -> "Wave"
    }
}
