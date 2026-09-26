#!/usr/bin/env python3
"""
Replaces the customer picker with a single editable search field +
DropdownMenu below it. No second search box, no ExposedDropdownMenuBox.
"""
from __future__ import annotations
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
P = REPO_ROOT / "app/src/main/java/com/akari/retailer/core/ui/components/PaymentListComponent.kt"

if not P.exists():
    print(f"❌ Not found: {P}")
    sys.exit(1)

src = P.read_text()

# ── Imports ────────────────────────────────────────────────────────────────
for imp in [
    "import androidx.compose.material.icons.filled.ArrowDropDown\n",
]:
    if imp not in src:
        idx = src.find("\nimport ")
        if idx != -1:
            src = src[:idx + 1] + imp + src[idx + 1:]

# ── Replace the whole customer picker ExposedDropdownMenuBox ───────────────
OLD = '''            ExposedDropdownMenuBox(
                expanded = customerExpanded,
                onExpandedChange = { customerExpanded = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp)
            ) {
                OutlinedTextField(
                    value = selectedCustomer?.name ?: "Select Customer",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Customer (who owes)", fontSize = 12.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerExpanded)
                    },
                    singleLine = true,
                    isError = selectedCustomer == null
                )

                ExposedDropdownMenu(
                    expanded = customerExpanded,
                    onDismissRequest = { customerExpanded = false }
                ) {
                    if (customers.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No customers — add one first") },
                            onClick = { customerExpanded = false }
                        )
                    } else {
                        customers.forEach { customer ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(customer.name, fontSize = 13.sp)
                                        if (customer.phone.isNotEmpty()) {
                                            Text(
                                                text = customer.phone,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    onCustomerSelected(customer)
                                    customerExpanded = false
                                }
                            )
                        }
                    }
                }
            }'''

NEW = '''            // Single editable field with live-filtering dropdown.
            // No ExposedDropdownMenuBox (which forces readOnly), no second
            // search box — the user types directly into this field.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp)
            ) {
                var query by rememberSaveable { mutableStateOf("") }
                var expanded by rememberSaveable { mutableStateOf(false) }

                // Keep the field in sync when a customer is preselected
                LaunchedEffect(selectedCustomer) {
                    if (selectedCustomer != null && query.isEmpty()) {
                        query = selectedCustomer.name
                    }
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = { newValue ->
                        query = newValue
                        expanded = true
                        // If the user is typing something different than the
                        // current selection, clear the selection so the field
                        // behaves like a fresh search.
                        if (selectedCustomer?.name != newValue) {
                            // no-op: parent keeps selectedCustomer until user picks
                        }
                    },
                    label = { Text("Customer (who owes)", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = "Show customers",
                            modifier = Modifier.clickable { expanded = !expanded }
                        )
                    },
                    singleLine = true,
                    isError = selectedCustomer == null
                )

                val filteredCustomers = if (query.isBlank()) {
                    customers
                } else {
                    val q = query.lowercase()
                    customers.filter {
                        it.name.lowercase().contains(q) ||
                        it.phone.lowercase().contains(q)
                    }
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.widthIn(min = 260.dp)
                ) {
                    // Plain Column — LazyColumn can't nest inside DropdownMenu
                    // (SubcomposeLayout intrinsic-measure crash).
                    Column(
                        modifier = Modifier
                            .widthIn(min = 260.dp)
                            .heightIn(max = 280.dp)
                    ) {
                        if (customers.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No customers — add one first", fontSize = 13.sp) },
                                onClick = { expanded = false }
                            )
                        } else if (filteredCustomers.isEmpty()) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "No matches",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                },
                                onClick = { },
                                enabled = false
                            )
                        } else {
                            filteredCustomers.take(20).forEach { customer ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(customer.name, fontSize = 13.sp)
                                            if (customer.phone.isNotEmpty()) {
                                                Text(
                                                    text = customer.phone,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                )
                                            }
                                            if (customer.owesCredit()) {
                                                Text(
                                                    text = "Owes: ${customer.creditBalance}",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        query = customer.name
                                        onCustomerSelected(customer)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }'''

if OLD not in src:
    print("❌ Could not find the original ExposedDropdownMenuBox block")
    print("   Make sure you restored PaymentListComponent.kt from the backup first.")
    sys.exit(1)

src = src.replace(OLD, NEW, 1)

# ── Remove the now-unused `customerSearchQuery` state (from earlier patch) ─
# Not strictly required, but avoids an unused-variable warning.
src = src.replace(
    '    var customerSearchQuery by remember { mutableStateOf("") }\n',
    ""
)

P.write_text(src)
print(f"✅ Patched {P.name}: single-field autocomplete picker")
