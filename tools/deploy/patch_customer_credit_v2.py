#!/usr/bin/env python3
"""Adds manual 'Extend Credit' to Customer Detail. Updates all 4 sites."""
from __future__ import annotations
import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
VM = REPO_ROOT / "app/src/main/java/com/akari/retailer/features/customer/presentation/CustomerDetailViewModel.kt"
SCR = REPO_ROOT / "app/src/main/java/com/akari/retailer/features/customer/presentation/CustomerDetailScreen.kt"

for p in (VM, SCR):
    if not p.exists():
        print(f"❌ Not found: {p}")
        sys.exit(1)

vm = VM.read_text()
scr = SCR.read_text()

# ═════════════════════════════════════════════════════════════════════════
# VIEWMODEL
# ═════════════════════════════════════════════════════════════════════════

# 1. Import
imp = "import com.akari.retailer.features.customer.domain.usecases.ExtendCreditUseCase\n"
if imp not in vm:
    anchor = "import com.akari.retailer.features.customer.domain.usecases.RecordCreditRefundUseCase\n"
    if anchor in vm:
        vm = vm.replace(anchor, anchor + imp, 1)
    else:
        idx = vm.find("\nimport ")
        vm = vm[:idx + 1] + imp + vm[idx + 1:]

# 2. State fields
if "showCreditDialog" not in vm:
    anchor = (
        "    // Refund dialog (we pay them)\n"
        "    val showRefundDialog: Boolean = false,\n"
    )
    add = (
        "    // Manual credit-extend dialog\n"
        "    val showCreditDialog: Boolean = false,\n"
        "    val creditAmount: String = \"\",\n"
        "    val creditNotes: String = \"\",\n"
        "    val isRecordingCredit: Boolean = false,\n"
        "    val creditSuccess: Boolean = false,\n"
        "    val creditError: String? = null,\n"
        "\n"
    )
    if anchor not in vm:
        print("❌ VM state anchor missing")
        sys.exit(1)
    vm = vm.replace(anchor, add + anchor, 1)

# 3. Events
if "OpenCreditDialog" not in vm:
    anchor = (
        "    // Refund events\n"
        "    data object OpenRefundDialog : CustomerDetailEvent()\n"
    )
    add = (
        "    // Manual credit events\n"
        "    data object OpenCreditDialog : CustomerDetailEvent()\n"
        "    data object CloseCreditDialog : CustomerDetailEvent()\n"
        "    data class CreditAmountChanged(val value: String) : CustomerDetailEvent()\n"
        "    data class CreditNotesChanged(val value: String) : CustomerDetailEvent()\n"
        "    data object SubmitCredit : CustomerDetailEvent()\n"
        "    data object ClearCreditSuccess : CustomerDetailEvent()\n"
        "\n"
    )
    if anchor not in vm:
        print("❌ VM event anchor missing")
        sys.exit(1)
    vm = vm.replace(anchor, add + anchor, 1)

# 4. ViewModel constructor
if "extendCreditUseCase: ExtendCreditUseCase" not in vm:
    anchor = (
        "    private val recordCreditRefundUseCase: RecordCreditRefundUseCase,\n"
        "    private val getCreditTransactionsUseCase: GetCreditTransactionsUseCase,\n"
        "    private val paymentPreferences: PaymentPreferences,\n"
        "    private val customerId: String\n"
        ") : ViewModel() {"
    )
    new = (
        "    private val recordCreditRefundUseCase: RecordCreditRefundUseCase,\n"
        "    private val extendCreditUseCase: ExtendCreditUseCase,\n"
        "    private val getCreditTransactionsUseCase: GetCreditTransactionsUseCase,\n"
        "    private val paymentPreferences: PaymentPreferences,\n"
        "    private val customerId: String\n"
        ") : ViewModel() {"
    )
    if anchor not in vm:
        print("❌ VM constructor anchor missing")
        sys.exit(1)
    vm = vm.replace(anchor, new, 1)

# 5. Factory constructor
if "extendCreditUseCase: ExtendCreditUseCase" not in vm.split("class CustomerDetailViewModelFactory")[-1]:
    anchor = (
        "class CustomerDetailViewModelFactory(\n"
        "    private val repository: CustomerRepository,\n"
        "    private val moneyAccountRepository: MoneyAccountRepository,\n"
        "    private val recordCreditPaymentUseCase: RecordCreditPaymentUseCase,\n"
        "    private val recordCreditRefundUseCase: RecordCreditRefundUseCase,\n"
        "    private val getCreditTransactionsUseCase: GetCreditTransactionsUseCase,\n"
        "    private val paymentPreferences: PaymentPreferences,\n"
        "    private val customerId: String\n"
    )
    new = (
        "class CustomerDetailViewModelFactory(\n"
        "    private val repository: CustomerRepository,\n"
        "    private val moneyAccountRepository: MoneyAccountRepository,\n"
        "    private val recordCreditPaymentUseCase: RecordCreditPaymentUseCase,\n"
        "    private val recordCreditRefundUseCase: RecordCreditRefundUseCase,\n"
        "    private val extendCreditUseCase: ExtendCreditUseCase,\n"
        "    private val getCreditTransactionsUseCase: GetCreditTransactionsUseCase,\n"
        "    private val paymentPreferences: PaymentPreferences,\n"
        "    private val customerId: String\n"
    )
    if anchor not in vm:
        print("❌ VM factory ctor anchor missing")
        sys.exit(1)
    vm = vm.replace(anchor, new, 1)

# 6. Factory call site
if "extendCreditUseCase,\n" not in vm.split("class CustomerDetailViewModelFactory")[-1]:
    anchor = (
        "            return CustomerDetailViewModel(\n"
        "                repository,\n"
        "                moneyAccountRepository,\n"
        "                recordCreditPaymentUseCase,\n"
        "                recordCreditRefundUseCase,\n"
        "                getCreditTransactionsUseCase,\n"
    )
    new = (
        "            return CustomerDetailViewModel(\n"
        "                repository,\n"
        "                moneyAccountRepository,\n"
        "                recordCreditPaymentUseCase,\n"
        "                recordCreditRefundUseCase,\n"
        "                extendCreditUseCase,\n"
        "                getCreditTransactionsUseCase,\n"
    )
    if anchor not in vm:
        print("❌ VM factory call anchor missing")
        sys.exit(1)
    vm = vm.replace(anchor, new, 1)

# 7. handleEvent wiring
if "is CustomerDetailEvent.OpenCreditDialog" not in vm:
    anchor = "            is CustomerDetailEvent.OpenRefundDialog -> openRefundDialog()\n"
    add = (
        "            is CustomerDetailEvent.OpenCreditDialog -> openCreditDialog()\n"
        "            is CustomerDetailEvent.CloseCreditDialog -> closeCreditDialog()\n"
        "            is CustomerDetailEvent.CreditAmountChanged -> _state.value = _state.value.copy(creditAmount = event.value.filter { it.isDigit() })\n"
        "            is CustomerDetailEvent.CreditNotesChanged -> _state.value = _state.value.copy(creditNotes = event.value)\n"
        "            is CustomerDetailEvent.SubmitCredit -> submitCredit()\n"
        "            is CustomerDetailEvent.ClearCreditSuccess -> _state.value = _state.value.copy(creditSuccess = false)\n"
    )
    if anchor not in vm:
        print("❌ VM handleEvent anchor missing")
        sys.exit(1)
    vm = vm.replace(anchor, add + anchor, 1)

# 8. Handlers
if "private fun openCreditDialog()" not in vm:
    anchor = "    private fun clearError() {\n"
    add = '''    private fun openCreditDialog() {
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
                saleId = "",
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
    if anchor not in vm:
        print("❌ VM clearError anchor missing")
        sys.exit(1)
    vm = vm.replace(anchor, add + anchor, 1)

VM.write_text(vm)
print(f"✅ Patched {VM.name}")

# ═════════════════════════════════════════════════════════════════════════
# SCREEN — update factory call site
# ═════════════════════════════════════════════════════════════════════════

anchor = (
    "        factory = CustomerDetailViewModelFactory(\n"
    "            application.container.customerRepository,\n"
    "            application.container.moneyAccountRepository,\n"
    "            application.container.recordCreditPaymentUseCase,\n"
    "            application.container.recordCreditRefundUseCase,\n"
    "            application.container.getCreditTransactionsUseCase,\n"
    "            application.container.paymentPreferences,\n"
    "            customerId\n"
    "        )"
)
new = (
    "        factory = CustomerDetailViewModelFactory(\n"
    "            application.container.customerRepository,\n"
    "            application.container.moneyAccountRepository,\n"
    "            application.container.recordCreditPaymentUseCase,\n"
    "            application.container.recordCreditRefundUseCase,\n"
    "            application.container.extendCreditUseCase,\n"
    "            application.container.getCreditTransactionsUseCase,\n"
    "            application.container.paymentPreferences,\n"
    "            customerId\n"
    "        )"
)

if "application.container.extendCreditUseCase" in scr:
    print(f"⏭  {SCR.name}: already patched")
else:
    if anchor not in scr:
        print("❌ Screen factory call anchor missing")
        sys.exit(1)
    scr = scr.replace(anchor, new, 1)
    SCR.write_text(scr)
    print(f"✅ Patched {SCR.name}")
