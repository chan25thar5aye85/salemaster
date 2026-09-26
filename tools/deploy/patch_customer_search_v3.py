#!/usr/bin/env python3
"""
Makes the customer picker searchable by replacing ONLY the inner
ExposedDropdownMenu block. Anchors are exact strings from the file.
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

# ── Imports (only add if missing) ──────────────────────────────────────────
for imp in [
    "import androidx.compose.foundation.layout.widthIn\n",
    "import androidx.compose.foundation.lazy.LazyColumn\n",
    "import androidx.compose.foundation.lazy.items\n",
    "import androidx.compose.material.icons.Icons\n",
    "import androidx.compose.material.icons.filled.Clear\n",
    "import androidx.compose.material.icons.filled.Search\n",
    "import androidx.compose.runtime.saveable.rememberSaveable\n",
]:
    if imp not in src:
        # Insert after the first import line
        idx = src.find("\nimport ")
        if idx != -1:
            src = src[:idx + 1] + imp + src[idx + 1:]

# ── The exact block to replace (from your grep, lines 319–350) ─────────────
OLD = '''                ExposedDropdownMenu(
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
                }'''

NEW = '''                ExposedDropdownMenu(
                    expanded = customerExpanded,
                    onDismissRequest = {
                        customerExpanded = false
                        customerSearchQuery = ""
                    }
                ) {
                    // Search field — stays focused while the user types.
                    OutlinedTextField(
                        value = customerSearchQuery,
                        onValueChange = { customerSearchQuery = it },
                        placeholder = { Text("Search customers…", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        trailingIcon = {
                            if (customerSearchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { customerSearchQuery = "" },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )

                    HorizontalDivider()

                    val filteredCustomers = if (customerSearchQuery.isBlank()) {
                        customers
                    } else {
                        val q = customerSearchQuery.lowercase()
                        customers.filter {
                            it.name.lowercase().contains(q) ||
                            it.phone.lowercase().contains(q)
                        }
                    }

                    if (customers.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No customers — add one first") },
                            onClick = { customerExpanded = false }
                        )
                    } else if (filteredCustomers.isEmpty()) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "No matches",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            },
                            onClick = { },
                            enabled = false
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .widthIn(min = 240.dp)
                                .heightIn(max = 280.dp)
                        ) {
                            items(filteredCustomers, key = { it.id }) { customer ->
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
                                        onCustomerSelected(customer)
                                        customerExpanded = false
                                        customerSearchQuery = ""
                                    }
                                )
                            }
                        }
                    }
                }'''

if OLD not in src:
    print("❌ Could not find the ExposedDropdownMenu block — it may already be patched or shifted.")
    print("   Grep for `customerSearchQuery` to check.")
    sys.exit(1)

src = src.replace(OLD, NEW, 1)

# ── Add the `customerSearchQuery` state near `customerExpanded` ────────────
# Find the existing `var customerExpanded by remember` (if any) or add near it.
if "var customerSearchQuery" not in src:
    # The picker composable is `SinglePaymentRow` (private).
    # We add the query state right after the customerExpanded declaration.
    anchor = "    var customerExpanded by remember { mutableStateOf(false) }"
    if anchor in src:
        src = src.replace(
            anchor,
            anchor + "\n    var customerSearchQuery by remember { mutableStateOf(\"\") }",
            1,
        )
    else:
        print("⚠  `var customerExpanded by remember` not found — you may need to declare customerSearchQuery manually.")
        print("   Look for `customerExpanded` in the file and add:")
        print('       var customerSearchQuery by remember { mutableStateOf("") }')
        print("   next to it.")

P.write_text(src)
print(f"✅ Patched {P.name}: searchable customer picker")
