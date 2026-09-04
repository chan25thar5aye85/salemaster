package com.akari.retailer.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.akari.retailer.core.ui.components.FABMenu
import com.akari.retailer.data.remote.FirestoreService
import com.akari.retailer.data.repository.FirestoreSaleRepository
import com.akari.retailer.features.customer.presentation.CustomerAddScreen
import com.akari.retailer.features.customer.presentation.CustomerDetailScreen
import com.akari.retailer.features.customer.presentation.CustomerEditScreen
import com.akari.retailer.features.customer.presentation.CustomerListScreen
import com.akari.retailer.features.expense.presentation.ExpenseAddScreen
import com.akari.retailer.features.expense.presentation.ExpenseDetailScreen
import com.akari.retailer.features.expense.presentation.ExpenseEditScreen
import com.akari.retailer.features.expense.presentation.ExpenseListScreen
import com.akari.retailer.features.reports.presentation.TrendsScreen
import com.akari.retailer.features.sales.presentation.entry.SaleEntryScreen
import com.akari.retailer.features.sales.presentation.history.SaleHistoryScreen
import com.akari.retailer.features.settings.presentation.SettingsScreen
import com.akari.retailer.features.supplier.presentation.SupplierAddScreen
import com.akari.retailer.features.supplier.presentation.SupplierDetailScreen
import com.akari.retailer.features.supplier.presentation.SupplierEditScreen
import com.akari.retailer.features.supplier.presentation.SupplierListScreen
import kotlinx.coroutines.launch

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val imeHeight = WindowInsets.ime.asPaddingValues(density).calculateBottomPadding()
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues(density).calculateBottomPadding()
    
    val bottomPadding = if (imeHeight > 0.dp) imeHeight else navBarHeight

    val firestoreService = remember { FirestoreService() }
    val repository = remember { FirestoreSaleRepository(firestoreService) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            NavHost(
                navController = navController,
                startDestination = Routes.SALE_ENTRY
            ) {
                composable(Routes.SALE_ENTRY) {
                    SaleEntryScreen(navController = navController)
                }
                
                composable(Routes.HISTORY) {
                    SaleHistoryScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
                
                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
                
                composable(Routes.TRENDS) {
                    TrendsScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
                
                composable(Routes.CUSTOMERS) {
                    CustomerListScreen(
                        navController = navController,
                        onBack = { navController.popBackStack() }
                    )
                }
                
                composable(Routes.CUSTOMER_ADD) {
                    CustomerAddScreen(
                        onBack = { navController.popBackStack() },
                        onCustomerAdded = { navController.popBackStack() }
                    )
                }
                
                composable(
                    route = Routes.CUSTOMER_DETAIL,
                    arguments = listOf(navArgument("customerId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val customerId = backStackEntry.arguments?.getString("customerId") ?: ""
                    CustomerDetailScreen(
                        customerId = customerId,
                        onBack = { navController.popBackStack() },
                        onEdit = { customer ->
                            navController.navigate(Routes.CUSTOMER_EDIT.replace("{customerId}", customer.id))
                        }
                    )
                }
                
                composable(
                    route = Routes.CUSTOMER_EDIT,
                    arguments = listOf(navArgument("customerId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val customerId = backStackEntry.arguments?.getString("customerId") ?: ""
                    CustomerEditScreen(
                        customerId = customerId,
                        onBack = { navController.popBackStack() },
                        onCustomerUpdated = { navController.popBackStack() }
                    )
                }
                
                composable(Routes.SUPPLIERS) {
                    SupplierListScreen(
                        navController = navController,
                        onBack = { navController.popBackStack() }
                    )
                }
                
                composable(Routes.SUPPLIER_ADD) {
                    SupplierAddScreen(
                        onBack = { navController.popBackStack() },
                        onSupplierAdded = { navController.popBackStack() }
                    )
                }
                
                composable(
                    route = Routes.SUPPLIER_DETAIL,
                    arguments = listOf(navArgument("supplierId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val supplierId = backStackEntry.arguments?.getString("supplierId") ?: ""
                    SupplierDetailScreen(
                        supplierId = supplierId,
                        onBack = { navController.popBackStack() },
                        onEdit = { supplier ->
                            navController.navigate(Routes.SUPPLIER_EDIT.replace("{supplierId}", supplier.id))
                        }
                    )
                }
                
                composable(
                    route = Routes.SUPPLIER_EDIT,
                    arguments = listOf(navArgument("supplierId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val supplierId = backStackEntry.arguments?.getString("supplierId") ?: ""
                    SupplierEditScreen(
                        supplierId = supplierId,
                        onBack = { navController.popBackStack() },
                        onSupplierUpdated = { navController.popBackStack() }
                    )
                }
                
                composable(Routes.EXPENSES) {
                    ExpenseListScreen(
                        navController = navController,
                        onBack = { navController.popBackStack() }
                    )
                }
                
                composable(Routes.EXPENSE_ADD) {
                    ExpenseAddScreen(
                        onBack = { navController.popBackStack() },
                        onExpenseAdded = { navController.popBackStack() }
                    )
                }
                
                composable(
                    route = Routes.EXPENSE_DETAIL,
                    arguments = listOf(navArgument("expenseId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val expenseId = backStackEntry.arguments?.getString("expenseId") ?: ""
                    ExpenseDetailScreen(
                        expenseId = expenseId,
                        onBack = { navController.popBackStack() },
                        onEdit = { expense ->
                            navController.navigate(Routes.EXPENSE_EDIT.replace("{expenseId}", expense.id))
                        }
                    )
                }
                
                composable(
                    route = Routes.EXPENSE_EDIT,
                    arguments = listOf(navArgument("expenseId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val expenseId = backStackEntry.arguments?.getString("expenseId") ?: ""
                    ExpenseEditScreen(
                        expenseId = expenseId,
                        onBack = { navController.popBackStack() },
                        onExpenseUpdated = { navController.popBackStack() }
                    )
                }
            }

            val currentRoute = navController.currentBackStackEntry?.destination?.route
            if (currentRoute == Routes.SALE_ENTRY) {
                FABMenu(
                    onMenuItemClick = { route ->
                        when (route) {
                            "settings" -> navController.navigate(Routes.SETTINGS)
                            "history" -> navController.navigate(Routes.HISTORY)
                            "reports" -> navController.navigate(Routes.TRENDS)
                            "customers" -> navController.navigate(Routes.CUSTOMERS)
                            "suppliers" -> navController.navigate(Routes.SUPPLIERS)
                            "expenses" -> navController.navigate(Routes.EXPENSES)
                            "inventory" -> {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Inventory coming soon!")
                                }
                            }
                            else -> {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Coming soon!")
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .matchParentSize()
                        .padding(bottom = bottomPadding + 16.dp)
                        .padding(horizontal = 16.dp)
                )
            }
        }
    }
}
