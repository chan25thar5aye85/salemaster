#!/usr/bin/env python3
"""
After a successful save, reset the payment row's account to 'default_cash'
instead of preserving the account the user just used.

Handles Add screens (SaleEntry, ExpenseAdd, IncomeEntry, PurchaseOrderDetail).
Edit screens are left alone — they legitimately carry the existing account.
"""
from __future__ import annotations
import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]

# Files where we want to reset to cash after save.
TARGETS = [
    # sale entry — the reset uses `lastAccountId`
    ("app/src/main/java/com/akari/retailer/features/sales/presentation/entry/SaleEntryViewModel.kt",
     [
         # capture the reset block
         (
             "val currentRecentSales = currentState.recentSales\n"
             "        val lastAccountId = currentState.paymentRows.firstOrNull()?.accountId ?: \"default_cash\"\n",
             "val currentRecentSales = currentState.recentSales\n"
             "        // Reset to Cash, not the account the user just used.\n"
             "        val lastAccountId = \"default_cash\"\n",
         ),
     ]),
    # expense add
    ("app/src/main/java/com/akari/retailer/features/expense/presentation/ExpenseAddViewModel.kt",
     [
         (
             "val lastAccountId = paymentPreferences.getLastUsedAccountId()\n",
             "// Always start at Cash on a new expense.\n"
             "val lastAccountId = \"default_cash\"\n",
         ),
     ]),
    # income entry
    ("app/src/main/java/com/akari/retailer/features/sales/presentation/income/IncomeEntryViewModel.kt",
     [
         (
             "val lastAccountId = paymentPreferences.getLastUsedAccountId()\n",
             "// Always start at Cash on a new income entry.\n"
             "val lastAccountId = \"default_cash\"\n",
         ),
     ]),
    # purchase order detail (reset after purchase completes)
    ("app/src/main/java/com/akari/retailer/features/inventory/presentation/PurchaseOrderDetailViewModel.kt",
     [
         (
             "val lastAccountId = paymentPreferences.getLastUsedAccountId()\n",
             "// Always start at Cash on a new payment.\n"
             "val lastAccountId = \"default_cash\"\n",
         ),
     ]),
]

PATCHED = []
SKIPPED = []
FAILED = []

for rel, subs in TARGETS:
    p = REPO_ROOT / rel
    if not p.exists():
        FAILED.append(f"{rel}: file not found")
        continue
    src = p.read_text()
    orig = src
    for old, new in subs:
        if new.strip() in src:
            continue
        if old not in src:
            FAILED.append(f"{rel}: anchor missing")
            continue
        src = src.replace(old, new, 1)
    if src == orig:
        SKIPPED.append(rel)
    else:
        p.write_text(src)
        PATCHED.append(rel)

print()
print("═══ RESET-TO-CASH PATCH REPORT ═══")
if PATCHED:
    print(f"✅ Patched ({len(PATCHED)}):")
    for line in PATCHED:
        print(f"   • {line}")
if SKIPPED:
    print()
    print(f"⏭  Skipped ({len(SKIPPED)}):")
    for line in SKIPPED:
        print(f"   • {line}")
if FAILED:
    print()
    print(f"❌ Failed ({len(FAILED)}):")
    for line in FAILED:
        print(f"   • {line}")
