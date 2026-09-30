package com.akari.retailer.core.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.akari.retailer.R
import com.akari.retailer.core.ui.theme.AppTypography
import com.akari.retailer.core.ui.theme.Spacing

@Composable
fun SaleSummary(
    total: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = "${stringResource(R.string.total)}: $total",
        style = AppTypography.total,
        modifier = modifier.padding(bottom = Spacing.medium)
    )
}
