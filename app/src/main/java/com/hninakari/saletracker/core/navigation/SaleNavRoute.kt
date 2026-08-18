package com.hninakari.saletracker.core.navigation

sealed class SaleNavRoute(val route: String) {
    object AddSale : SaleNavRoute("add_sale")
    object Sales : SaleNavRoute("sales")
    object Settings : SaleNavRoute("settings")
    object SaleDetail : SaleNavRoute("sale_detail/{saleId}") {
        fun passId(saleId: String): String = "sale_detail/$saleId"
    }
}

object NavArgs {
    const val SALE_ID = "saleId"
}
