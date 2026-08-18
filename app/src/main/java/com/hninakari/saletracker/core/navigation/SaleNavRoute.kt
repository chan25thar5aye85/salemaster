package com.hninakari.saletracker.core.navigation

sealed class SaleNavRoute(val route: String) {
    object Sales : SaleNavRoute("sales")
    object AddSale : SaleNavRoute("add_sale")
    object SaleDetail : SaleNavRoute("sale_detail/{saleId}") {
        fun passId(saleId: String): String = "sale_detail/$saleId"
    }
}

object NavArgs {
    const val SALE_ID = "saleId"
}
