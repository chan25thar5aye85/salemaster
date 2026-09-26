#!/usr/bin/env python3
"""Bump SaleEntryViewModel recent sales cap from 2 → 5."""
from __future__ import annotations
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
P = REPO_ROOT / "app/src/main/java/com/akari/retailer/features/sales/presentation/entry/SaleEntryViewModel.kt"

if not P.exists():
    print(f"❌ Not found: {P}")
    sys.exit(1)

src = P.read_text()

OLD = "_state.value = _state.value.copy(recentSales = sales.take(2))"
NEW = "_state.value = _state.value.copy(recentSales = sales.take(5))"

if NEW in src:
    print("⏭  Already patched (take(5))")
    sys.exit(0)

if OLD not in src:
    print("❌ Anchor not found — expected: `sales.take(2)`")
    print("   Grep the file for `recentSales` to see the current line.")
    sys.exit(1)

src = src.replace(OLD, NEW, 1)
P.write_text(src)
print(f"✅ Patched {P.name}: recent sales cap 2 → 5")
