package com.akari.retailer.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akari.retailer.R
import com.akari.retailer.core.ui.theme.AppTypography
import com.akari.retailer.core.ui.theme.Spacing
import com.akari.retailer.core.utils.MoneyFormatter
import com.akari.retailer.features.sales.domain.models.PaymentMethod
import com.akari.retailer.features.sales.domain.models.Sale
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RecentSalesTable(
    sales: List<Sale>,
    onViewAllClick: () -> Unit,
    modifier: Modifier = Modifier,
    maxItems: Int = 2
) {
    val recentSales = sales.take(maxItems)
    val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(Spacing.medium)
    ) {
        // Header - Increased height with more padding
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp) // ✅ Increased from default to 56dp
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📋 ${stringResource(R.string.recent_sales)}",
                style = AppTypography.title,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 8.dp) // ✅ Added vertical padding
            )
            
            TextButton(
                onClick = onViewAllClick,
                modifier = Modifier.height(40.dp) // ✅ Increased from 32dp to 40dp
            ) {
                Text(
                    text = stringResource(R.string.view_all),
                    fontSize = 13.sp, // ✅ Increased from 12sp to 13sp
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(Spacing.small))
        
        // Table Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp) // ✅ Added explicit height for table header
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = Spacing.small, vertical = Spacing.small)
        ) {
            TableCell(
                text = "Time",
                modifier = Modifier.weight(1f),
                isHeader = true
            )
            TableCell(
                text = "Amount",
                modifier = Modifier.weight(1.2f),
                isHeader = true
            )
            TableCell(
                text = "Payment",
                modifier = Modifier.weight(1f),
                isHeader = true
            )
        }
        
        if (recentSales.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.large),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No recent sales",
                    style = AppTypography.body,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    fontSize = 13.sp
                )
            }
        } else {
            // Sales rows with increased height
            recentSales.forEachIndexed { index, sale ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp) // ✅ Added explicit height for each row
                        .padding(horizontal = Spacing.small, vertical = 6.dp)
                        .background(
                            color = if (index % 2 == 0) {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f)
                            } else {
                                Color.Transparent
                            },
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = Spacing.small, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TableCell(
                        text = dateFormat.format(Date(sale.timestamp)),
                        modifier = Modifier.weight(1f),
                        isHeader = false
                    )
                    TableCell(
                        text = MoneyFormatter.formatTotal(sale.total),
                        modifier = Modifier.weight(1.2f),
                        isHeader = false,
                        isAmount = true
                    )
                    TableCell(
                        text = getPaymentDisplayName(sale.paymentMethod),
                        modifier = Modifier.weight(1f),
                        isHeader = false
                    )
                }
            }
        }
    }
}

@Composable
fun TableCell(
    text: String,
    modifier: Modifier = Modifier,
    isHeader: Boolean = false,
    isAmount: Boolean = false
) {
    Text(
        text = text,
        modifier = modifier,
        style = if (isHeader) {
            AppTypography.label.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp // ✅ Increased from 11sp to 12sp
            )
        } else if (isAmount) {
            AppTypography.body.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp, // ✅ Increased from 13sp to 14sp
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            AppTypography.body.copy(
                fontSize = 13.sp // ✅ Increased from default to 13sp
            )
        },
        color = if (isHeader) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        maxLines = 1
    )
}

private fun getPaymentDisplayName(method: PaymentMethod): String {
    return when (method) {
        PaymentMethod.CASH -> "Cash"
        PaymentMethod.KPAY -> "KPay"
        PaymentMethod.WAVEPAY -> "Wave"
    }
}
