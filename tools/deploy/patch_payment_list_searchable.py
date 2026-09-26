#!/usr/bin/env python3
"""
Makes the customer picker inside PaymentListComponent searchable.

Replaces the ExposedDropdownMenu-driven picker with a Button + DropdownMenu
combo. DropdownMenu is a plain Material3 menu (not an ExposedDropdownMenuBox)
so a search field inside it works reliably.
"""
from __future__ import annotations
import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
P = REPO_ROOT / "app/src/main/java/com/akari/retailer/core/ui/components/PaymentListComponent.kt"

if not P.exists():
    print(f"❌ Not found: {P}")
    sys.exit(1)

src = P.read_text()

# ── 1. Imports ─────────────────────────────────────────────────────────────
needed_imports = [
    "import androidx.compose.foundation.layout.widthIn\n",
    "import androidx.compose.foundation.lazy.LazyColumn\n",
    "import androidx.compose.foundation.lazy.items\n",
    "import androidx.compose.material.icons.Icons\n",
    "import androidx.compose.material.icons.filled.Clear\n",
    "import androidx.compose.material.icons.filled.Search\n",
    "import androidx.compose.runtime.saveable.rememberSaveable\n",
    "import androidx.compose.ui.unit.sp\n",
]

first_import = re.search(r"^import ", src, re.M)
if first_import:
    at = first_import.start()
    for imp in needed_imports:
        if imp not in src:
            src = src[:at] + imp + src[at:]
            at += len(imp)

# ── 2. Replace the customer picker block ───────────────────────────────────
# The credit-row customer picker lives right after the `if (isCredit && showCreditOption)` guard.
# We match from that guard's `ExposedDropdownMenuBox(` up to the end of its closing `}`.

# Find the ExposedDropdownMenuBox that references `customerExpanded`
cust_box_re = re.compile(
    r"ExposedDropdownMenuBox\(\s*\n"
    r"\s*expanded = customerExpanded,\s*\n"
    r".*?"                        # non-greedy
    r"customerExpanded\s*=\s*false\s*\n"   # closing onClick for customer select
    r".*?\n\s*\}\s*\n"            # close the inner forEach + ExposedDropdownMenu
    r"\s*\}\s*\n",                # close the ExposedDropdownMenuBox
    re.DOTALL,
)
m = cust_box_re.search(src)
if not m:
    print("❌ Could not locate the customer picker (ExposedDropdownMenuBox + customerExpanded)")
    print("   Grep the file for `customerExpanded` and paste the surrounding 30 lines.")
    sys.exit(1)

NEW_PICKER = '''                // Searchable customer picker.
                // Using a plain Button + DropdownMenu (NOT ExposedDropdownMenuBox)
                // because we need a text field inside the popup for search,
                // and ExposedDropdownMenuBox steals focus on inner text input.
                Box(modifier = Modifier.fillMaxWidth()) {
                    var pickerOpen by rememberSaveable { mutableStateOf(false) }

                    OutlinedButton(
                        onClick = { pickerOpen = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, end = 8.dp),
                    ) {
                        Text(
                            text = selectedCustomer?.name ?: "Select Customer",
                            fontSize = 13.sp
                        )
                    }

                    DropdownMenu(
                        expanded = pickerOpen,
                        onDismissRequest = { pickerOpen = false },
                        modifier = Modifier.widthIn(min = 260.dp)
                    ) {
                        var q by rememberSaveable { mutableStateOf("") }

                        val filtered = if (q.isBlank()) customers else {
                            val lower = q.lowercase()
                            customers.filter {
                                it.name.lowercase().contains(lower) ||
                                it.phone.lowercase().contains(lower)
                            }
                        }

                        // Search box
                        OutlinedTextField(
                            value = q,
                            onValueChange = { q = it },
                            placeholder = { Text("Search by name or phone…", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            trailingIcon = {
                                if (q.isNotEmpty()) {
                                    IconButton(
                                        onClick = { q = "" },
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

                        if (filtered.isEmpty()) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (customers.isEmpty())
                                            "No customers — add one first"
                                        else
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
                                    .widthIn(min = 260.dp)
                                    .heightIn(max = 320.dp)
                            ) {
                                items(filtered, key = { it.id }) { customer ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(customer.name, fontSize = 13.sp)
                                                if (customer.phone.isNotEmpty()) {
                                                    Text(
                                                        text = customer.phone,
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                            .copy(alpha = 0.6f)
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
                                            pickerOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
'''

src = src[:m.start()] + NEW_PICKER + src[m.end():]

P.write_text(src)
print(f"✅ Patched {P.name}: searchable customer picker (Button + DropdownMenu)")
