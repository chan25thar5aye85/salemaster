package com.hninakari.saletracker.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.hninakari.saletracker.sales.presentation.viewmodels.SaleViewModel

@Composable
fun AppNavGraph(
    viewModel: SaleViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    
    SaleNavHost(
        navController = navController,
        viewModel = viewModel,
        modifier = modifier
    )
}
