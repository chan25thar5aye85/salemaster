#!/usr/bin/env python3
"""
Fixes the SubcomposeLayout crash:
- Replaces LazyColumn inside ExposedDropdownMenu with a plain Column.
- Caps visible items (avoids unbounded height).
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

# The problematic block was added by the previous patch.
OLD = '''                        LazyColumn(
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
                        }'''

NEW = '''                        // NOTE: Cannot use LazyColumn here.
                        // ExposedDropdownMenu is built on SubcomposeLayout, and
                        // LazyColumn doesn't support intrinsic measurements, so
                        // nesting causes an IllegalStateException at runtime.
                        // A plain Column with a capped item count works fine.
                        Column(
                            modifier = Modifier
                                .widthIn(min = 240.dp)
                                .heightIn(max = 280.dp)
                        ) {
                            filteredCustomers.take(50).forEach { customer ->
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
                        }'''

if OLD not in src:
    print("❌ Could not find the LazyColumn block inside the customer dropdown")
    print("   It may already be patched. Grep for 'LazyColumn' in the file.")
    sys.exit(1)

src = src.replace(OLD, NEW, 1)
P.write_text(src)
print(f"✅ Patched {P.name}: LazyColumn → Column")
