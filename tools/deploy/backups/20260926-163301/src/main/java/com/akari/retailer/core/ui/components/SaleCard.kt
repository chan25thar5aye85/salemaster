package com.akari.retailer.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.akari.retailer.core.utils.MoneyFormatter
import com.akari.retailer.features.money.domain.models.MoneyAccount
import com.akari.retailer.features.sales.domain.models.Sale
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SaleCard(
    sale: Sale,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    accounts: List<MoneyAccount> = emptyList()
) {
    val dateFormat = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
    val dateString = dateFormat.format(Date(sale.timestamp))

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable { onClick() }
                else Modifier
            ),
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
            // LEFT: time · N items · amount  (single line, ellipsized)
            Text(
                text = "🕐 $dateString  ·  ${sale.items.size} " +
                    "${stringResource(R.string.item).lowercase()}  ·  " +
                    MoneyFormatter.formatTotal(sale.total),
                style = AppTypography.body,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )

            // RIGHT: payment icons + amounts (max 3, then "+N")
            if (accounts.isNotEmpty() && sale.payments.isNotEmpty()) {
                sale.payments.take(3).forEachIndexed { index, p ->
                    if (index > 0) {
                        Text(
                            text = "  ",
                            style = AppTypography.small
                        )
                    }
                    val icon = if (p.isCredit) {
                        "💳"
                    } else {
                        accounts.find { it.id == p.accountId }?.icon ?: "💵"
                    }
                    Text(
                        text = "$icon ${MoneyFormatter.format(p.amount)}",
                        style = AppTypography.small,
                        color = if (p.isCredit)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 1
                    )
                }
                if (sale.payments.size > 3) {
                    Text(
                        text = "  +${sale.payments.size - 3}",
                        style = AppTypography.small,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

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
