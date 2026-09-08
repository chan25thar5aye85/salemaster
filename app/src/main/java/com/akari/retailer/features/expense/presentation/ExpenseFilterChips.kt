package com.akari.retailer.features.expense.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.akari.retailer.core.ui.theme.Spacing
import com.akari.retailer.features.expense.domain.models.ExpenseCategory

@Composable
fun ExpenseFilterChips(
    categories: List<ExpenseCategory>,
    selectedCategories: Set<String>,
    onCategoryToggle: (String) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Filter by Category",
                style = MaterialTheme.typography.labelMedium
            )
            if (selectedCategories.isNotEmpty()) {
                TextButton(onClick = onClearAll) {
                    Text("Clear All")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(Spacing.small))
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 0.dp)
        ) {
            // "All" chip
            item {
                FilterChip(
                    selected = selectedCategories.isEmpty(),
                    onClick = {
                        if (selectedCategories.isNotEmpty()) {
                            onClearAll()
                        }
                    },
                    label = { Text("All") }
                )
            }
            
            items(categories) { category ->
                FilterChip(
                    selected = selectedCategories.contains(category.id),
                    onClick = { onCategoryToggle(category.id) },
                    label = { Text(category.name) }
                )
            }
        }
    }
}
