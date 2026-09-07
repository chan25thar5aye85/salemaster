package com.akari.retailer.features.inventory.presentation

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.akari.retailer.core.ui.theme.AppTypography
import com.akari.retailer.core.ui.theme.Spacing
import com.akari.retailer.features.inventory.domain.models.Product
import com.akari.retailer.features.inventory.domain.models.PurchaseOrderItem

@Composable
fun EditItemDialog(
    showDialog: Boolean,
    itemIndex: Int,
    productName: String,
    currentQuantity: String,
    currentPrice: String,
    onDismiss: () -> Unit,
    onSave: (Int, Int) -> Unit
) {
    if (!showDialog) return
    
    var tempQuantity by remember { mutableStateOf(currentQuantity) }
    var tempPrice by remember { mutableStateOf(currentPrice) }
    val context = LocalContext.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Item") },
        text = {
            Column {
                Text(
                    text = productName,
                    style = AppTypography.body,
                    modifier = Modifier.padding(bottom = Spacing.medium)
                )
                
                OutlinedTextField(
                    value = tempQuantity,
                    onValueChange = { tempQuantity = it },
                    label = { Text("Quantity") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(Spacing.medium))
                
                OutlinedTextField(
                    value = tempPrice,
                    onValueChange = { tempPrice = it },
                    label = { Text("Price") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val qty = tempQuantity.toIntOrNull() ?: 0
                    val price = tempPrice.toIntOrNull() ?: 0
                    if (qty > 0 && price > 0) {
                        onSave(qty, price)
                    } else {
                        Toast.makeText(context, "Enter valid quantity and price", Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddItemDialog(
    showDialog: Boolean,
    products: List<Product>,
    onDismiss: () -> Unit,
    onAdd: (Product, Int) -> Unit
) {
    if (!showDialog) return
    
    var productSearch by remember { mutableStateOf("") }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var addQuantity by remember { mutableStateOf("1") }
    val context = LocalContext.current
    
    val filteredProducts = products.filter { 
        it.name.contains(productSearch, ignoreCase = true) || 
        it.sku.contains(productSearch, ignoreCase = true)
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Item from Products") },
        text = {
            Column {
                OutlinedTextField(
                    value = productSearch,
                    onValueChange = { productSearch = it },
                    label = { Text("Search products...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(Spacing.medium))
                
                if (filteredProducts.isEmpty()) {
                    Text(
                        text = "No products found",
                        style = AppTypography.body,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                } else {
                    filteredProducts.forEach { product ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedProduct = product
                                    addQuantity = "1"
                                }
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedProduct?.id == product.id) 
                                    MaterialTheme.colorScheme.primaryContainer 
                                else 
                                    MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.medium),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(product.name, style = AppTypography.body)
                                    Text(
                                        "SKU: ${product.sku} | Stock: ${product.stockQuantity}",
                                        style = AppTypography.small,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                                Text(
                                    "${product.sellPrice}",
                                    style = AppTypography.body,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                
                if (selectedProduct != null) {
                    Spacer(modifier = Modifier.height(Spacing.medium))
                    
                    OutlinedTextField(
                        value = addQuantity,
                        onValueChange = { addQuantity = it },
                        label = { Text("Quantity") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val product = selectedProduct
                    if (product == null) {
                        Toast.makeText(context, "Select a product", Toast.LENGTH_SHORT).show()
                        return@TextButton
                    }
                    
                    val qty = addQuantity.toIntOrNull()
                    if (qty == null || qty <= 0) {
                        Toast.makeText(context, "Enter valid quantity", Toast.LENGTH_SHORT).show()
                        return@TextButton
                    }
                    
                    onAdd(product, qty)
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
