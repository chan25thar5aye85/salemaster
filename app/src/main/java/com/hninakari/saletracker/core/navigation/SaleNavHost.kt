package com.hninakari.saletracker.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.hninakari.saletracker.sales.presentation.screens.SalesScreen
import com.hninakari.saletracker.sales.presentation.screens.AddSaleScreen
import com.hninakari.saletracker.sales.presentation.screens.SaleDetailScreen
import com.hninakari.saletracker.sales.presentation.viewmodels.SaleViewModel

@Composable
fun SaleNavHost(
    navController: NavHostController,
    viewModel: SaleViewModel,
    modifier: Modifier = Modifier,
    startDestination: String = SaleNavRoute.AddSale.route  // Now starts at AddSale
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(route = SaleNavRoute.AddSale.route) {
            AddSaleScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToSales = {
                    navController.navigate(SaleNavRoute.Sales.route)
                }
            )
        }
        
        composable(route = SaleNavRoute.Sales.route) {
            SalesScreen(
                viewModel = viewModel,
                onNavigateToAddSale = {
                    navController.popBackStack()  // Go back to AddSale
                },
                onNavigateToDetail = { saleId ->
                    navController.navigate(SaleNavRoute.SaleDetail.passId(saleId))
                }
            )
        }
        
        composable(
            route = SaleNavRoute.SaleDetail.route,
            arguments = listOf(
                navArgument(NavArgs.SALE_ID) { defaultValue = "" }
            )
        ) { backStackEntry ->
            val saleId = backStackEntry.arguments?.getString(NavArgs.SALE_ID) ?: ""
            SaleDetailScreen(
                viewModel = viewModel,
                saleId = saleId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
