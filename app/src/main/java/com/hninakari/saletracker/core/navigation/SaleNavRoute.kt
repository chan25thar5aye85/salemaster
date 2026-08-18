package com.hninakari.saletracker.core.navigation

sealed class SaleNavRoute(val route: String) {
    object AddSale : SaleNavRoute("add_sale")      // AddSale is now first
    object Sales : SaleNavRoute("sales")
    object SaleDetail : SaleNavRoute("sale_detail/{saleId}") {
        fun passId(saleId: String): String = "sale_detail/$saleId"
    }
}

object NavArgs {
    const val SALE_ID = "saleId"
}
