package com.akari.retailer.navigation

object Routes {
    const val SALE_ENTRY = "sale_entry"
    const val SETTINGS = "settings"
    const val HISTORY = "history"
    const val REPORTS = "reports"
    const val TRENDS = "trends"
    const val EXPENSES = "expenses"
    const val EXPENSE_ADD = "expense_add"
    const val EXPENSE_DETAIL = "expense_detail/{expenseId}"
    const val EXPENSE_EDIT = "expense_edit/{expenseId}"
    const val EXPENSE_ANALYTICS = "expense_analytics"
    
    const val CATEGORIES = "categories"
    const val PROFIT_LOSS = "profit_loss"
    
    const val INCOME_ENTRY = "income_entry"
    const val INCOME_LIST = "income_list"
    const val INCOME_STREAMS = "income_streams"
    const val INCOME_ANALYTICS = "income_analytics"
    
    // Money
    const val MONEY_ACCOUNTS = "money_accounts"
    const val TRANSFER_MONEY = "transfer_money"
    const val EXTERNAL_TRANSFER = "external_transfer"
    const val MONEY_TRANSACTIONS = "money_transactions"
    const val MONEY_ANALYTICS = "money_analytics"
    
    const val INVENTORY = "inventory"
    const val INVENTORY_ADD = "inventory_add"
    const val INVENTORY_DETAIL = "inventory_detail/{productId}"
    const val INVENTORY_EDIT = "inventory_edit/{productId}"
    const val STOCK_HISTORY = "stock_history/{productId}/{productName}"
    const val STOCK_ADJUSTMENT = "stock_adjustment"
    
    const val PURCHASE_ORDERS = "purchase_orders"
    const val PURCHASE_ORDER_ADD = "purchase_order_add"
    const val PURCHASE_ORDER_DETAIL = "purchase_order_detail/{orderId}"
    const val PURCHASE_ORDER_DETAIL_READONLY = "purchase_order_detail_readonly/{orderId}"
    const val PURCHASE_ORDER_RECEIPT = "purchase_order_receipt/{orderId}"
    
    const val PURCHASES = "purchases"
    const val PURCHASE_DETAIL = "purchase_detail/{purchaseId}"
    
    const val CUSTOMERS = "customers"
    const val CUSTOMER_ADD = "customer_add"
    const val CUSTOMER_EDIT = "customer_edit/{customerId}"
    const val CUSTOMER_DETAIL = "customer_detail/{customerId}"
    const val CREDIT_HISTORY = "credit_history/{customerId}"
    const val SUPPLIERS = "suppliers"
    const val SUPPLIER_ADD = "supplier_add"
    const val SUPPLIER_DETAIL = "supplier_detail/{supplierId}"
    const val SUPPLIER_EDIT = "supplier_edit/{supplierId}"
}
