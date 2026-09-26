#!/usr/bin/env python3
"""
Wires PurchaseFinalizer into PurchaseOrderDetailViewModel.createPurchase().

Replaces the inline db.runTransaction with a call to purchaseFinalizer.finalizePurchase().
Removes the now-unused repository.firestore usage.
Injects PurchaseFinalizer into the ViewModel constructor + factory.
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

# ── Step 1: Add import ─────────────────────────────────────────────────────
IMPORT_ANCHOR = "import com.akari.retailer.features.inventory.data.repository.PurchaseOrderRepository\n"
IMPORT_ADD = "import com.akari.retailer.features.inventory.data.repository.PurchaseFinalizer\n"
if IMPORT_ADD not in src:
    if IMPORT_ANCHOR not in src:
        print("❌ Could not find import anchor")
        sys.exit(1)
    src = src.replace(IMPORT_ANCHOR, IMPORT_ANCHOR + IMPORT_ADD, 1)

# ── Step 2: Inject finalizer into constructor ──────────────────────────────
# Match the constructor parameter list.
CTOR_OLD = (
    "class PurchaseOrderDetailViewModel(\n"
    "    private val repository: PurchaseOrderRepository,\n"
    "    private val paymentPreferences: PaymentPreferences\n"
    ") : ViewModel() {"
)
CTOR_NEW = (
    "class PurchaseOrderDetailViewModel(\n"
    "    private val repository: PurchaseOrderRepository,\n"
    "    private val purchaseFinalizer: PurchaseFinalizer,\n"
    "    private val paymentPreferences: PaymentPreferences\n"
    ") : ViewModel() {"
)
if CTOR_NEW not in src:
    if CTOR_OLD not in src:
        print("❌ Could not find ViewModel constructor")
        sys.exit(1)
    src = src.replace(CTOR_OLD, CTOR_NEW, 1)

# ── Step 3: Replace createPurchase() body ──────────────────────────────────
# Find `suspend fun createPurchase(...)` and its closing `}` (balanced braces).
func_start_re = re.compile(
    r"    suspend fun createPurchase\(orderId: String\): Result<Unit> \{\n"
)
m = func_start_re.search(src)
if not m:
    print("❌ Could not find createPurchase() function")
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

func_end = i + 1   # position after the closing `}`

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

src = src[:m.start()] + NEW_FUNC + src[func_end:]

# ── Step 4: Update factory to inject finalizer ─────────────────────────────
FACTORY_CTOR_OLD = (
    "class PurchaseOrderDetailViewModelFactory(\n"
    "    private val repository: PurchaseOrderRepository,\n"
    "    private val paymentPreferences: PaymentPreferences\n"
    ") : androidx.lifecycle.ViewModelProvider.Factory {"
)
FACTORY_CTOR_NEW = (
    "class PurchaseOrderDetailViewModelFactory(\n"
    "    private val repository: PurchaseOrderRepository,\n"
    "    private val purchaseFinalizer: PurchaseFinalizer,\n"
    "    private val paymentPreferences: PaymentPreferences\n"
    ") : androidx.lifecycle.ViewModelProvider.Factory {"
)
if FACTORY_CTOR_NEW not in src:
    if FACTORY_CTOR_OLD not in src:
        print("❌ Could not find factory constructor")
        sys.exit(1)
    src = src.replace(FACTORY_CTOR_OLD, FACTORY_CTOR_NEW, 1)

FACTORY_CALL_OLD = (
    "            return PurchaseOrderDetailViewModel(\n"
    "                repository,\n"
    "                paymentPreferences\n"
    "            ) as T"
)
FACTORY_CALL_NEW = (
    "            return PurchaseOrderDetailViewModel(\n"
    "                repository,\n"
    "                purchaseFinalizer,\n"
    "                paymentPreferences\n"
    "            ) as T"
)
if FACTORY_CALL_NEW not in src:
    if FACTORY_CALL_OLD not in src:
        print("❌ Could not find factory call site")
        sys.exit(1)
    src = src.replace(FACTORY_CALL_OLD, FACTORY_CALL_NEW, 1)

VM_PATH.write_text(src)
print(f"✅ Patched {VM_PATH.name}")
