#!/usr/bin/env python3
"""Minimal, targeted patch for PurchaseOrderDetailViewModel.kt.

Uses exact string anchors based on the actual file layout.
Idempotent — safe to re-run.
"""
from __future__ import annotations
import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
VM_PATH = REPO_ROOT / "app/src/main/java/com/akari/retailer/features/inventory/presentation/PurchaseOrderDetailViewModel.kt"

if not VM_PATH.exists():
    print(f"❌ File not found: {VM_PATH}")
    sys.exit(1)

src = VM_PATH.read_text()
changed = False


def sub_once(needle: str, replacement: str, label: str) -> bool:
    """Replace first occurrence; return True if changed."""
    global src, changed
    if replacement in src and needle not in src:
        print(f"⏭  {label}: already patched")
        return False
    if needle not in src:
        print(f"❌ {label}: anchor missing")
        return False
    src = src.replace(needle, replacement, 1)
    changed = True
    print(f"✅ {label}")
    return True


# ── 1. import ──────────────────────────────────────────────────────────────
if "import com.akari.retailer.features.inventory.data.repository.PurchaseFinalizer\n" not in src:
    anchor = "import com.akari.retailer.features.inventory.data.repository.PurchaseOrderRepository\n"
    if anchor in src:
        src = src.replace(
            anchor,
            anchor
            + "import com.akari.retailer.features.inventory.data.repository.PurchaseFinalizer\n",
            1,
        )
        changed = True
        print("✅ import added")
    else:
        print("❌ import anchor missing")
        sys.exit(1)
else:
    print("⏭  import: already present")


# ── 2. ViewModel constructor ───────────────────────────────────────────────
sub_once(
    "class PurchaseOrderDetailViewModel(\n"
    "    private val repository: PurchaseOrderRepository,\n"
    "    private val paymentPreferences: PaymentPreferences\n"
    ") : ViewModel() {",
    "class PurchaseOrderDetailViewModel(\n"
    "    private val repository: PurchaseOrderRepository,\n"
    "    private val purchaseFinalizer: PurchaseFinalizer,\n"
    "    private val paymentPreferences: PaymentPreferences\n"
    ") : ViewModel() {",
    "ViewModel constructor",
)


# ── 3. createPurchase() body ───────────────────────────────────────────────
if "Atomic cascade via PurchaseFinalizer" in src:
    print("⏭  createPurchase: already patched")
else:
    func_re = re.compile(
        r"    suspend fun createPurchase\(orderId: String\): Result<Unit> \{\n"
    )
    m = func_re.search(src)
    if not m:
        print("❌ createPurchase() not found")
        sys.exit(1)

    brace_start = src.index("{", m.start())
    depth = 0
    i = brace_start
    while i < len(src):
        c = src[i]
        if c == "{":
            depth += 1
        elif c == "}":
            depth -= 1
            if depth == 0:
                break
        i += 1

    NEW_FUNC = """    suspend fun createPurchase(orderId: String): Result<Unit> {
        val currentOrder = _state.value.order
            ?: return Result.failure(Exception("Order not found"))

        if (currentOrder.status == PurchaseOrderStatus.COMPLETED) {
            return Result.failure(Exception("Purchase already created"))
        }
        if (currentOrder.receivedItems.isEmpty()) {
            return Result.failure(Exception("No items to purchase"))
        }

        val payments = _state.value.getPayments()
        val totalCost = currentOrder.receivedItems.sumOf { it.total }
        val totalPaid = payments.sumOf { it.amount }

        if (totalPaid > totalCost) {
            return Result.failure(
                Exception("Payments ($totalPaid) cannot exceed total ($totalCost)")
            )
        }

        val receiptNumber = generateReceiptNumber()

        // Atomic cascade via PurchaseFinalizer (purchase + stock + expense +
        // money + supplier + payable + order status). Idempotent on order id.
        val result = purchaseFinalizer.finalizePurchase(
            order = currentOrder,
            payments = payments,
            receiptNumber = receiptNumber
        )

        if (result.isSuccess) {
            loadOrder(orderId)
        }
        return result.map { }
    }
"""
    src = src[:m.start()] + NEW_FUNC + src[i + 1:]
    changed = True
    print("✅ createPurchase() replaced")


# ── 4. Factory constructor ─────────────────────────────────────────────────
sub_once(
    "class PurchaseOrderDetailViewModelFactory(\n"
    "    private val repository: PurchaseOrderRepository,\n"
    "    private val paymentPreferences: PaymentPreferences\n"
    ") : androidx.lifecycle.ViewModelProvider.Factory {",
    "class PurchaseOrderDetailViewModelFactory(\n"
    "    private val repository: PurchaseOrderRepository,\n"
    "    private val purchaseFinalizer: PurchaseFinalizer,\n"
    "    private val paymentPreferences: PaymentPreferences\n"
    ") : androidx.lifecycle.ViewModelProvider.Factory {",
    "Factory constructor",
)


# ── 5. Factory create() call site (single-line form) ───────────────────────
sub_once(
    "            return PurchaseOrderDetailViewModel(repository, paymentPreferences) as T",
    "            return PurchaseOrderDetailViewModel(repository, purchaseFinalizer, paymentPreferences) as T",
    "Factory call site",
)


# ── write ──────────────────────────────────────────────────────────────────
if changed:
    VM_PATH.write_text(src)
    print()
    print(f"💾 Saved {VM_PATH.name}")
else:
    print()
    print("Nothing to write — already fully patched.")
