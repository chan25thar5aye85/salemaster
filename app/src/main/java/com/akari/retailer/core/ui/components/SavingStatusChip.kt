package com.akari.retailer.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akari.retailer.core.ui.theme.AppTypography
import kotlinx.coroutines.delay

@Composable
fun SavingStatusChip(
    isSaving: Boolean,
    modifier: Modifier = Modifier
) {
    var elapsedSeconds by remember { mutableStateOf(0) }

    LaunchedEffect(isSaving) {
        if (isSaving) {
            elapsedSeconds = 0
            while (true) {
                delay(1000L)
                elapsedSeconds += 1
            }
        } else {
            elapsedSeconds = 0
        }
    }

    val message: String? = when {
        !isSaving -> null
        elapsedSeconds < 3 -> "Saving…"
        elapsedSeconds < 5 -> "Still saving…"
        else -> "Slow internet — trying to save, please wait…"
    }

    if (message == null) return

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = AppTypography.small,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
