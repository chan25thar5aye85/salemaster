#!/usr/bin/env python3
"""
Patch B — Autocomplete external account name.

Adds to ExternalTransferViewModel:
- knownExternalNames: List<String> (unique, recent-first)
- loads them from moneyTransactionRepository.getExternalTransfers()

Updates ExternalTransferState to carry knownExternalNames.

Rewrites the External Account Name field in ExternalTransferScreen as
an editable text field with a dropdown (same pattern as customer picker).
"""
from __future__ import annotations
import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
PKG = REPO_ROOT / "app/src/main/java/com/akari/retailer/features/money/presentation"

VM = PKG / "ExternalTransferViewModel.kt"
STATE = PKG / "ExternalTransferState.kt"
SCR = PKG / "ExternalTransferScreen.kt"

PATCHED, SKIPPED, FAILED = [], [], []

def patch_file(path: Path, edits, label: str) -> None:
    if not path.exists():
        FAILED.append(f"{label}: not found → {path}")
        return
    src = path.read_text()
    orig = src
    for old, new in edits:
        if new in src:
            continue
        if old not in src:
            FAILED.append(f"{label}: anchor missing")
            return
        src = src.replace(old, new, 1)
    if src == orig:
        SKIPPED.append(f"{label}: already patched")
        return
    path.write_text(src)
    PATCHED.append(f"{label}: {path.name}")


# ═══════════════════════════════════════════════════════════════════════════
# 1. ExternalTransferState.kt — add knownExternalNames
# ═══════════════════════════════════════════════════════════════════════════
patch_file(
    STATE,
    [
        (
            "    val lastUsedAccountId: String = \"\",  // ✅ NEW - Remember last used\n"
            "    val isSaving: Boolean = false,\n"
            "    val saveSuccess: Boolean = false,\n"
            "    val error: String? = null\n"
            ")",

            "    val lastUsedAccountId: String = \"\",\n"
            "    /**\n"
            "     * Unique external-account names from past transfers, most recent first.\n"
            "     * Used to power the autocomplete dropdown on the External Account Name field.\n"
            "     */\n"
            "    val knownExternalNames: List<String> = emptyList(),\n"
            "    val isSaving: Boolean = false,\n"
            "    val saveSuccess: Boolean = false,\n"
            "    val error: String? = null\n"
            ")",
        ),
    ],
    "ExternalTransferState: knownExternalNames",
)


# ═══════════════════════════════════════════════════════════════════════════
# 2. ExternalTransferViewModel.kt — load known names
# ═══════════════════════════════════════════════════════════════════════════
VM_EDITS = []

# 2a. Import the transaction repository
if "import com.akari.retailer.features.money.data.repository.MoneyTransactionRepository\n" not in VM.read_text():
    VM_EDITS.append((
        "import com.akari.retailer.features.money.data.repository.MoneyAccountRepository\n",
        "import com.akari.retailer.features.money.data.repository.MoneyAccountRepository\n"
        "import com.akari.retailer.features.money.data.repository.MoneyTransactionRepository\n",
    ))

# 2b. Add constructor param
VM_EDITS.append((
    "class ExternalTransferViewModel(\n"
    "    private val accountRepository: MoneyAccountRepository,\n"
    "    private val externalTransferUseCase: ExternalTransferUseCase,\n"
    "    private val paymentPreferences: PaymentPreferences\n"
    ") : ViewModel() {",

    "class ExternalTransferViewModel(\n"
    "    private val accountRepository: MoneyAccountRepository,\n"
    "    private val transactionRepository: MoneyTransactionRepository,\n"
    "    private val externalTransferUseCase: ExternalTransferUseCase,\n"
    "    private val paymentPreferences: PaymentPreferences\n"
    ") : ViewModel() {",
))

# 2c. Add job field
VM_EDITS.append((
    "    private var loadAccountsJob: Job? = null\n",
    "    private var loadAccountsJob: Job? = null\n"
    "    private var loadKnownNamesJob: Job? = null\n",
))

# 2d. Call it in init (after loadAccounts())
VM_EDITS.append((
    "    init {\n"
    "        loadLastUsedAccount()\n"
    "        loadAccounts()\n"
    "    }",
    "    init {\n"
    "        loadLastUsedAccount()\n"
    "        loadAccounts()\n"
    "        loadKnownExternalNames()\n"
    "    }",
))

# 2e. Add the loader function right after loadAccounts()
VM_EDITS.append((
    "    private fun saveTransfer() {",
    "    private fun loadKnownExternalNames() {\n"
    "        loadKnownNamesJob?.cancel()\n"
    "        loadKnownNamesJob = viewModelScope.launch {\n"
    "            try {\n"
    "                transactionRepository.getExternalTransfers().collect { transfers ->\n"
    "                    val names = transfers\n"
    "                        .sortedByDescending { it.date }\n"
    "                        .map { it.externalAccountName }\n"
    "                        .filter { it.isNotBlank() }\n"
    "                        .distinct()\n"
    "                    _state.value = _state.value.copy(knownExternalNames = names)\n"
    "                }\n"
    "            } catch (e: Exception) { }\n"
    "        }\n"
    "    }\n"
    "\n"
    "    private fun saveTransfer() {",
))

patch_file(VM, VM_EDITS, "ExternalTransferViewModel: load known names")


# ═══════════════════════════════════════════════════════════════════════════
# 3. ExternalTransferScreen.kt — replace External Account Name field
# ═══════════════════════════════════════════════════════════════════════════
SCR_EDITS = []

# 3a. The current field, from the earlier snapshot, is inside a Column with
#     label "External Account Name". We replace the OutlinedTextField with
#     a Box containing a text field + DropdownMenu.
SCR_EDITS.append((
    "                // External Account Name\n"
    "                Column(\n"
    "                    modifier = Modifier.weight(1f)\n"
    "                ) {\n"
    "                    Text(\n"
    "                        text = if (state.isOutgoing()) \n"
    "                            stringResource(R.string.to_account) \n"
    "                        else \n"
    "                            stringResource(R.string.from_account),\n"
    "                        style = AppTypography.label,\n"
    "                        fontWeight = FontWeight.Medium,\n"
    "                        modifier = Modifier.padding(bottom = 4.dp)\n"
    "                    )\n"
    "                    \n"
    "                    OutlinedTextField(\n"
    "                        value = state.externalAccountName,\n"
    "                        onValueChange = { \n"
    "                            viewModel.handleEvent(ExternalTransferEvent.ExternalAccountNameChanged(it)) \n"
    "                        },\n"
    "                        placeholder = { Text(stringResource(R.string.name_field), fontSize = 13.sp) },\n"
    "                        modifier = Modifier.fillMaxWidth(),\n"
    "                        singleLine = true\n"
    "                    )\n"
    "                }",

    "                // External Account Name — autocomplete\n"
    "                Column(\n"
    "                    modifier = Modifier.weight(1f)\n"
    "                ) {\n"
    "                    Text(\n"
    "                        text = if (state.isOutgoing())\n"
    "                            stringResource(R.string.to_account)\n"
    "                        else\n"
    "                            stringResource(R.string.from_account),\n"
    "                        style = AppTypography.label,\n"
    "                        fontWeight = FontWeight.Medium,\n"
    "                        modifier = Modifier.padding(bottom = 4.dp)\n"
    "                    )\n"
    "\n"
    "                    Box {\n"
    "                        var nameExpanded by rememberSaveable { mutableStateOf(false) }\n"
    "\n"
    "                        val filteredNames = if (state.externalAccountName.isBlank()) {\n"
    "                            state.knownExternalNames\n"
    "                        } else {\n"
    "                            val q = state.externalAccountName.lowercase()\n"
    "                            state.knownExternalNames.filter { it.lowercase().contains(q) }\n"
    "                        }\n"
    "\n"
    "                        OutlinedTextField(\n"
    "                            value = state.externalAccountName,\n"
    "                            onValueChange = {\n"
    "                                viewModel.handleEvent(ExternalTransferEvent.ExternalAccountNameChanged(it))\n"
    "                                nameExpanded = true\n"
    "                            },\n"
    "                            placeholder = { Text(stringResource(R.string.name_field), fontSize = 13.sp) },\n"
    "                            modifier = Modifier.fillMaxWidth(),\n"
    "                            singleLine = true\n"
    "                        )\n"
    "\n"
    "                        DropdownMenu(\n"
    "                            expanded = nameExpanded && filteredNames.isNotEmpty(),\n"
    "                            onDismissRequest = { nameExpanded = false },\n"
    "                            modifier = Modifier.widthIn(min = 200.dp)\n"
    "                        ) {\n"
    "                            Column(\n"
    "                                modifier = Modifier\n"
    "                                    .widthIn(min = 200.dp)\n"
    "                                    .heightIn(max = 240.dp)\n"
    "                            ) {\n"
    "                                filteredNames.take(10).forEach { name ->\n"
    "                                    DropdownMenuItem(\n"
    "                                        text = { Text(name, fontSize = 13.sp) },\n"
    "                                        onClick = {\n"
    "                                            viewModel.handleEvent(\n"
    "                                                ExternalTransferEvent.ExternalAccountNameChanged(name)\n"
    "                                            )\n"
    "                                            nameExpanded = false\n"
    "                                        }\n"
    "                                    )\n"
    "                                }\n"
    "                            }\n"
    "                        }\n"
    "                    }\n"
    "                }",
))

# 3b. Ensure imports exist
scr_text = SCR.read_text()
needed_imports = [
    "import androidx.compose.foundation.layout.Box\n",
    "import androidx.compose.foundation.layout.heightIn\n",
    "import androidx.compose.foundation.layout.widthIn\n",
    "import androidx.compose.material3.DropdownMenu\n",
    "import androidx.compose.material3.DropdownMenuItem\n",
    "import androidx.compose.runtime.mutableStateOf\n",
    "import androidx.compose.runtime.saveable.rememberSaveable\n",
    "import androidx.compose.runtime.setValue\n",
]
missing = [imp for imp in needed_imports if imp not in scr_text]
if missing:
    idx = scr_text.find("\nimport ")
    for imp in missing:
        scr_text = scr_text[:idx + 1] + imp + scr_text[idx + 1:]
        idx += len(imp)
    SCR.write_text(scr_text)

patch_file(SCR, SCR_EDITS, "ExternalTransferScreen: autocomplete field")


# ═══════════════════════════════════════════════════════════════════════════
# 4. Update factory call site in ExternalTransferScreen
# ═══════════════════════════════════════════════════════════════════════════
# The factory now needs transactionRepository injected.
SCR2 = SCR.read_text()
old_call = "ExternalTransferViewModelFactory(application.container.moneyAccountRepository, application.container.externalTransferUseCase, application.container.paymentPreferences)"
new_call = "ExternalTransferViewModelFactory(application.container.moneyAccountRepository, application.container.moneyTransactionRepository, application.container.externalTransferUseCase, application.container.paymentPreferences)"
if old_call in SCR2 and new_call not in SCR2:
    SCR2 = SCR2.replace(old_call, new_call, 1)
    SCR.write_text(SCR2)
    PATCHED.append("ExternalTransferScreen: factory call updated")

# Also update the factory definition itself in the ViewModel file
vm_text = VM.read_text()
old_factory = (
    "class ExternalTransferViewModelFactory(\n"
    "    private val accountRepository: MoneyAccountRepository,\n"
    "    private val externalTransferUseCase: ExternalTransferUseCase,\n"
    "    private val paymentPreferences: PaymentPreferences\n"
    ") : androidx.lifecycle.ViewModelProvider.Factory {"
)
new_factory = (
    "class ExternalTransferViewModelFactory(\n"
    "    private val accountRepository: MoneyAccountRepository,\n"
    "    private val transactionRepository: MoneyTransactionRepository,\n"
    "    private val externalTransferUseCase: ExternalTransferUseCase,\n"
    "    private val paymentPreferences: PaymentPreferences\n"
    ") : androidx.lifecycle.ViewModelProvider.Factory {"
)
if old_factory in vm_text and new_factory not in vm_text:
    vm_text = vm_text.replace(old_factory, new_factory, 1)
    PATCHED.append("ExternalTransferViewModelFactory ctor updated")

old_ctor_call = (
    "            return ExternalTransferViewModel(\n"
    "                accountRepository,\n"
    "                externalTransferUseCase,\n"
    "                paymentPreferences\n"
    "            ) as T"
)
new_ctor_call = (
    "            return ExternalTransferViewModel(\n"
    "                accountRepository,\n"
    "                transactionRepository,\n"
    "                externalTransferUseCase,\n"
    "                paymentPreferences\n"
    "            ) as T"
)
if old_ctor_call in vm_text and new_ctor_call not in vm_text:
    vm_text = vm_text.replace(old_ctor_call, new_ctor_call, 1)
    PATCHED.append("ExternalTransferViewModelFactory call updated")

VM.write_text(vm_text)


print()
print("═══ PATCH B REPORT ═══")
print()
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
    sys.exit(1)
print()
print("Done.")
