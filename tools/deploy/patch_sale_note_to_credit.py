#!/usr/bin/env python3
"""
Copies the sale's notes into the credit transaction description when
the sale is on credit. Falls back to the existing hardcoded strings.
"""
from __future__ import annotations
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
P = REPO_ROOT / "app/src/main/java/com/akari/retailer/features/sales/data/remote/FirestoreSaleFinalizer.kt"

if not P.exists():
    print(f"❌ Not found: {P}")
    sys.exit(1)

src = P.read_text()

# 1. Credit row description
OLD1 = '''                        "description" to "Sale on credit (partial)",'''
NEW1 = '''                        "description" to if (sale.notes.isNotBlank())
                            sale.notes
                        else
                            "Sale on credit (partial)",'''

if NEW1.strip() in src:
    print("⏭  Credit row already patched")
elif OLD1 in src:
    src = src.replace(OLD1, NEW1, 1)
    print("✅ Credit row description now uses sale notes")
else:
    print("❌ Credit row anchor not found")

# 2. Overpayment credit row description
OLD2 = '''                        "description" to "Sale overpayment credit",'''
NEW2 = '''                        "description" to if (sale.notes.isNotBlank())
                            "Overpayment — " + sale.notes
                        else
                            "Sale overpayment credit",'''

if NEW2.strip() in src:
    print("⏭  Overpayment row already patched")
elif OLD2 in src:
    src = src.replace(OLD2, NEW2, 1)
    print("✅ Overpayment row description uses sale notes")
else:
    print("⏭  Overpayment row anchor not found (may not exist in this version)")

P.write_text(src)
print("Done.")
