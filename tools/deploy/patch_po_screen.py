#!/usr/bin/env python3
"""Injects PurchaseFinalizer into PurchaseOrderDetailScreen's factory call."""
from __future__ import annotations
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
PATH = REPO_ROOT / "app/src/main/java/com/akari/retailer/features/inventory/presentation/PurchaseOrderDetailScreen.kt"

if not PATH.exists():
    print(f"❌ File not found: {PATH}")
    sys.exit(1)

src = PATH.read_text()

OLD = (
    "        factory = PurchaseOrderDetailViewModelFactory("
    "application.container.purchaseOrderRepository, application.container.paymentPreferences)"
)
NEW = (
    "        factory = PurchaseOrderDetailViewModelFactory("
    "application.container.purchaseOrderRepository, "
    "application.container.purchaseFinalizer, "
    "application.container.paymentPreferences)"
)

if NEW in src:
    print("⏭  Already patched")
    sys.exit(0)

# Also handle the multi-line form
OLD_MULTILINE = (
    "        factory = PurchaseOrderDetailViewModelFactory(\n"
    "            application.container.purchaseOrderRepository,\n"
    "            application.container.paymentPreferences\n"
    "        )"
)
NEW_MULTILINE = (
    "        factory = PurchaseOrderDetailViewModelFactory(\n"
    "            application.container.purchaseOrderRepository,\n"
    "            application.container.purchaseFinalizer,\n"
    "            application.container.paymentPreferences\n"
    "        )"
)

if OLD in src:
    src = src.replace(OLD, NEW, 1)
elif OLD_MULTILINE in src:
    src = src.replace(OLD_MULTILINE, NEW_MULTILINE, 1)
else:
    print("❌ Could not find factory call site in screen")
    sys.exit(1)

PATH.write_text(src)
print(f"✅ Patched {PATH.name}")
