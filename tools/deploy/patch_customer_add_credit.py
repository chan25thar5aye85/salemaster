#!/usr/bin/env python3
"""
Adds a manual "Extend Credit" action to Customer Detail.

- New event: OpenCreditDialog / CloseCreditDialog / CreditAmountChanged /
  CreditNotesChanged / SubmitCredit / ClearCreditSuccess
- New state fields: showCreditDialog, creditAmount, creditNotes,
  isRecordingCredit, creditSuccess, creditError
- New handler that calls ExtendCreditUseCase
"""
from __future__ import annotations
import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
P = REPO_ROOT / "app/src/main/java/com/akari/retailer/features/customer/presentation/CustomerDetailViewModel.kt"

if not P.exists():
    print(f"❌ Not found: {P}")
    sys.exit(1)

src = P.read_text()

# ── 1. Import ExtendCreditUseCase ──────────────────────────────────────────
imp = "import com.akari.retailer.features.customer.domain.usecases.ExtendCreditUseCase\n"
if imp not in src:
    anchor = "import com.akari.retailer.features.customer.domain.usecases.RecordCreditRefundUseCase\n"
    if anchor in src:
        src = src.replace(anchor, anchor + imp, 1)
    else:
        m = re.search(r"^import ", src, re.M)
        src = src[:m.start()] + imp + src[m.start():]

# ── 2. Add state fields ────────────────────────────────────────────────────
anchor = (
    "    // Refund dialog (we pay them)\n"
    "    val showRefundDialog: Boolean = false,\n"
)
new_fields = (
    "    // Manual credit-extend dialog (add to their balance)\n"
    "    val showCreditDialog: Boolean = false,\n"
    "    val creditAmount: String = \"\",\n"
    "    val creditNotes: String = \"\",\n"
    "    val isRecordingCredit: Boolean = false,\n"
    "    val creditSuccess: Boolean = false,\n"
    "    val creditError: String? = null,\n"
    "\n"
)
if "val showCreditDialog" not in src:
    if anchor not in src:
        print("❌ Could not find state anchor (`Refund dialog`)")
        sys.exit(1)
    src = src.replace(anchor, new_fields + anchor, 1)

# ── 3. Add events ──────────────────────────────────────────────────────────
event_anchor = (
    "    // Refund events\n"
    "    data object OpenRefundDialog : CustomerDetailEvent()\n"
)
new_events = (
    "    // Manual credit-extend events\n"
    "    data object OpenCreditDialog : CustomerDetailEvent()\n"
    "    data object CloseCreditDialog : CustomerDetailEvent()\n"
    "    data class CreditAmountChanged(val value: String) : CustomerDetailEvent()\n"
    "    data class CreditNotesChanged(val value: String) : CustomerDetailEvent()\n"
    "    data object SubmitCredit : CustomerDetailEvent()\n"
    "    data object ClearCreditSuccess : CustomerDetailEvent()\n"
    "\n"
)
if "OpenCreditDialog" not in src:
    if event_anchor not in src:
        print("❌ Could not find event anchor (`Refund events`)")
        sys.exit(1)
    src = src.replace(event_anchor, new_events + event_anchor, 1)

# ── 4. Inject ExtendCreditUseCase into constructor + factory ───────────────
ctor_anchor = (
    "    private val recordCreditRefundUseCase: RecordCreditRefundUseCase,\n"
    "    private val getCreditTransactionsUseCase: GetCreditTransactionsUseCase,\n"
    "    private val paymentPreferences: PaymentPreferences,\n"
    "    private val customerId: String\n"
)
ctor_new = (
    "    private val recordCreditRefundUseCase: RecordCreditRefundUseCase,\n"
    "    private val extendCreditUseCase: ExtendCreditUseCase,\n"
    "    private val getCreditTransactionsUseCase: GetCreditTransactionsUseCase,\n"
    "    private val paymentPreferences: PaymentPreferences,\n"
    "    private val customerId: String\n"
)
if "extendCreditUseCase: ExtendCreditUseCase" not in src:
    if ctor_anchor not in src:
        print("❌ Could not find constructor anchor")
        sys.exit(1)
    src = src.replace(ctor_anchor, ctor_new, 1)

# Factory: same change twice (ctor + args)
factory_ctor_anchor = (
    "    private val recordCreditRefundUseCase: RecordCreditRefundUseCase,\n"
    "    private val getCreditTransactionsUseCase: GetCreditTransactionsUseCase,\n"
    "    private val paymentPreferences: PaymentPreferences,\n"
    "    private val customerId: String\n"
) 
# Replace remaining occurrence (in factory)
if factory_ctor_anchor in src:
    src = src.replace(factory_ctor_anchor, ctor_new, 1)

factory_call_anchor = (
    "                recordCreditRefundUseCase,\n"
    "                getCreditTransactionsUseCase,\n"
)
factory_call_new = (
    "                recordCreditRefundUseCase,\n"
    "                extendCreditUseCase,\n"
    "                getCreditTransactionsUseCase,\n"
)
if factory_call_anchor in src and "extendCreditUseCase,\n                getCreditTransactionsUseCase" not in src:
    src = src.replace(factory_call_anchor, factory_call_new, 1)

# ── 5. Wire events into handleEvent() ──────────────────────────────────────
handle_anchor = (
    "            is CustomerDetailEvent.OpenRefundDialog -> openRefundDialog()\n"
)
handle_new = (
    "            is CustomerDetailEvent.OpenCreditDialog -> openCreditDialog()\n"
    "            is CustomerDetailEvent.CloseCreditDialog -> closeCreditDialog()\n"
    "            is CustomerDetailEvent.CreditAmountChanged -> _state.value = _state.value.copy(creditAmount = event.value.filter { it.isDigit() })\n"
    "            is CustomerDetailEvent.CreditNotesChanged -> _state.value = _state.value.copy(creditNotes = event.value)\n"
    "            is CustomerDetailEvent.SubmitCredit -> submitCredit()\n"
    "            is CustomerDetailEvent.ClearCreditSuccess -> _state.value = _state.value.copy(creditSuccess = false)\n"
)
if "is CustomerDetailEvent.OpenCreditDialog" not in src:
    if handle_anchor not in src:
        print("❌ Could not find handleEvent anchor")
        sys.exit(1)
    src = src.replace(handle_anchor, handle_new + handle_anchor, 1)

# ── 6. Add the three new handler functions before `private fun clearError()` ──
handler_anchor = "    private fun clearError() {\n"
NEW_HANDLERS = '''    private fun openCreditDialog() {
        _state.value = _state.value.copy(
            showCreditDialog = true,
            creditAmount = "",
            creditNotes = "",
            creditError = null,
            creditSuccess = false
        )
    }

    private fun closeCreditDialog() {
        _state.value = _state.value.copy(
            showCreditDialog = false,
            creditAmount = "",
            creditNotes = "",
            creditError = null
        )
    }

    private fun submitCredit() {
        val customer = _state.value.customer ?: return
        val amount = _state.value.creditAmount.toIntOrNull() ?: 0

        if (amount <= 0) {
            _state.value = _state.value.copy(creditError = "Enter a valid amount")
            return
        }

        _state.value = _state.value.copy(isRecordingCredit = true, creditError = null)

        viewModelScope.launch {
            val result = extendCreditUseCase.invoke(
                customerId = customer.id,
                amount = amount,
                saleId = "",              // manual — not tied to a sale
                description = _state.value.creditNotes.trim().ifEmpty { "Manual credit" }
            )
            if (result.isSuccess) {
                _state.value = _state.value.copy(
                    isRecordingCredit = false,
                    creditSuccess = true,
                    showCreditDialog = false,
                    creditAmount = "",
                    creditNotes = ""
                )
            } else {
                _state.value = _state.value.copy(
                    isRecordingCredit = false,
                    creditError = result.exceptionOrNull()?.message ?: "Failed to add credit"
                )
            }
        }
    }

'''
if "private fun openCreditDialog()" not in src:
    if handler_anchor not in src:
        print("❌ Could not find clearError anchor")
        sys.exit(1)
    src = src.replace(handler_anchor, NEW_HANDLERS + handler_anchor, 1)

P.write_text(src)
print(f"✅ Patched {P.name}: manual credit-extend flow added")
