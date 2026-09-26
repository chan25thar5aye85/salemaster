package com.akari.retailer.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.akari.retailer.core.ui.theme.AppTypography
import com.akari.retailer.core.ui.theme.Spacing

@Composable
fun AppSection(
    title: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier) {
        if (title != null) {
            Text(
                title,
                style = AppTypography.title,
                modifier = Modifier.padding(bottom = Spacing.medium)
            )
        }
        
        content()
        
        Divider(
            modifier = Modifier.padding(vertical = Spacing.large)
        )
    }
}
