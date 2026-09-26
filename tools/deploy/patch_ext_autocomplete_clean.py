#!/usr/bin/env python3
"""
Adds autocomplete to the External Account Name field:
- knownExternalNames in state (unique, recent first)
- loaded in the ViewModel
- dropdown field with reliable M3 pattern (no offsets, no LazyColumn)
"""
from __future__ import annotations
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
PKG = REPO_ROOT / "app/src/main/java/com/akari/retailer/features/money/presentation"

STATE = PKG / "ExternalTransferState.kt"
VM    = PKG / "ExternalTransferViewModel.kt"
SCR   = PKG / "ExternalTransferScreen.kt"

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
            FAILED.append(f"{label}: anchor missing in {path.name}")
            return
        src = src.replace(old, new, 1)
    if src == orig:
        SKIPPED.append(f"{label}: already patched")
        return
    path.write_text(src)
    PATCHED.append(f"{label}: {path.name}")


# ═══════════════════════════════════════════════════════════════════════════
# 1. STATE — add knownExternalNames
# ═══════════════════════════════════════════════════════════════════════════
patch_file(
    STATE,
    [
        (
            "    val externalAccountName: String = \"\",\n",
            "    val externalAccountName: String = \"\",\n"
            "    /**\n"
            "     * Unique external-account names from past transfers, most recent first.\n"
            "     * Drives the autocomplete dropdown on the External Account Name field.\n"
            "     */\n"
            "    val knownExternalNames: List<String> = emptyList(),\n",
        ),
    ],
    "State: knownExternalNames",
)


# ═══════════════════════════════════════════════════════════════════════════
# 2. VIEWMODEL — load names
# ═══════════════════════════════════════════════════════════════════════════
# The earlier attempt added transactionRepository. We need to add it back.
VM_EDITS = []

# 2a. Import
vm_src = VM.read_text()
if "import com.akari.retailer.features.money.data.repository.MoneyTransactionRepository" not in vm_src:
    VM_EDITS.append((
        "import com.akari.retailer.features.money.data.repository.MoneyAccountRepository\n",
        "import com.akari.retailer.features.money.data.repository.MoneyAccountRepository\n"
        "import com.akari.retailer.features.money.data.repository.MoneyTransactionRepository\n",
    ))

# 2b. Constructor
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

# 2c. Job field
VM_EDITS.append((
    "    private var loadAccountsJob: Job? = null\n",
    "    private var loadAccountsJob: Job? = null\n"
    "    private var loadKnownNamesJob: Job? = null\n",
))

# 2d. Call it in init
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

# 2e. Loader function before saveTransfer
VM_EDITS.append((
    "    private fun saveTransfer() {",
    "    private fun loadKnownExternalNames() {\n"
    "        loadKnownNamesJob?.cancel()\n"
    "        loadKnownNamesJob = viewModelScope.launch {\n"
    "            try {\n"
    "                transactionRepository.getExternalTransfers().collect { transfers ->\n"
    "                    val names = transfers\n"
    "                        .sortedByDescending { it.date }\n"
    "                        .map { it.externalAccountName.trim() }\n"
    "                        .filter { it.isNotBlank() }\n"
    "                        .distinctBy { it.lowercase() }\n"
    "                    _state.value = _state.value.copy(knownExternalNames = names)\n"
    "                }\n"
    "            } catch (e: Exception) { }\n"
    "        }\n"
    "    }\n"
    "\n"
    "    private fun saveTransfer() {",
))

patch_file(VM, VM_EDITS, "ViewModel: load known names")


# ═══════════════════════════════════════════════════════════════════════════
# 3. VIEWMODEL FACTORY — pass transactionRepository
# ═══════════════════════════════════════════════════════════════════════════
vm_src = VM.read_text()

if "private val transactionRepository: MoneyTransactionRepository" not in vm_src.split("class ExternalTransferViewModelFactory")[-1]:
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
    if old_factory in vm_src:
        vm_src = vm_src.replace(old_factory, new_factory, 1)
        PATCHED.append("ViewModelFactory ctor updated")
    else:
        FAILED.append("ViewModelFactory ctor anchor missing")

    old_ctor = "return ExternalTransferViewModel(accountRepository, externalTransferUseCase, paymentPreferences) as T"
    new_ctor = "return ExternalTransferViewModel(accountRepository, transactionRepository, externalTransferUseCase, paymentPreferences) as T"
    if old_ctor in vm_src:
        vm_src = vm_src.replace(old_ctor, new_ctor, 1)
        PATCHED.append("ViewModelFactory call updated")

    VM.write_text(vm_src)


# ═══════════════════════════════════════════════════════════════════════════
# 4. SCREEN — replace the External Account Name field with autocomplete
# ═══════════════════════════════════════════════════════════════════════════
src = SCR.read_text()

# Find the block starting at "// External Account Name"
start = src.find("// External Account Name")
if start == -1:
    print("❌ Could not find '// External Account Name' comment")
    sys.exit(1)

# Walk braces to find end of the Column
brace_start = src.index("{", start)
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
block_end = i + 1

NEW_BLOCK = '''// External Account Name — autocomplete
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (state.isOutgoing())
                            stringResource(R.string.to_account)
                        else
                            stringResource(R.string.from_account),
                        style = AppTypography.label,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    var nameExpanded by rememberSaveable { mutableStateOf(false) }

                    val filteredNames = if (state.externalAccountName.isBlank()) {
                        state.knownExternalNames
                    } else {
                        val q = state.externalAccountName.lowercase().trim()
                        state.knownExternalNames.filter { it.lowercase().contains(q) }
                    }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = state.externalAccountName,
                            onValueChange = {
                                viewModel.handleEvent(
                                    ExternalTransferEvent.ExternalAccountNameChanged(it)
                                )
                                nameExpanded = true
                            },
                            placeholder = {
                                Text(stringResource(R.string.name_field), fontSize = 13.sp)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { nameExpanded = !nameExpanded }) {
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        contentDescription = "Show names",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        )

                        DropdownMenu(
                            expanded = nameExpanded && filteredNames.isNotEmpty(),
                            onDismissRequest = { nameExpanded = false },
                            modifier = Modifier
                                .widthIn(min = 220.dp)
                                .heightIn(max = 280.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .widthIn(min = 220.dp)
                                    .heightIn(max = 280.dp)
                            ) {
                                filteredNames.take(20).forEach { name ->
                                    DropdownMenuItem(
                                        text = { Text(name, fontSize = 13.sp) },
                                        onClick = {
                                            viewModel.handleEvent(
                                                ExternalTransferEvent.ExternalAccountNameChanged(name)
                                            )
                                            nameExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }'''

src = src[:start] + NEW_BLOCK + src[block_end:]
SCR.write_text(src)
PATCHED.append("Screen: autocomplete field")

# Ensure imports
for imp in [
    "import androidx.compose.foundation.layout.Box\n",
    "import androidx.compose.foundation.layout.heightIn\n",
    "import androidx.compose.foundation.layout.widthIn\n",
    "import androidx.compose.material.icons.Icons\n",
    "import androidx.compose.material.icons.filled.ArrowDropDown\n",
    "import androidx.compose.material3.DropdownMenu\n",
    "import androidx.compose.material3.DropdownMenuItem\n",
    "import androidx.compose.runtime.mutableStateOf\n",
    "import androidx.compose.runtime.saveable.rememberSaveable\n",
    "import androidx.compose.runtime.setValue\n",
]:
    if imp not in src:
        idx = src.find("\nimport ")
        src = src[:idx + 1] + imp + src[idx + 1:]
SCR.write_text(src)

# Ensure the factory call in the screen passes transactionRepository
scr_src = SCR.read_text()
if "moneyTransactionRepository" not in scr_src.split("ExternalTransferViewModelFactory")[1][:200]:
    old_call = (
        "ExternalTransferViewModelFactory(\n"
        "            application.container.moneyAccountRepository,\n"
        "            application.container.externalTransferUseCase,\n"
        "            application.container.paymentPreferences\n"
        "        )"
    )
    new_call = (
        "ExternalTransferViewModelFactory(\n"
        "            application.container.moneyAccountRepository,\n"
        "            application.container.moneyTransactionRepository,\n"
        "            application.container.externalTransferUseCase,\n"
        "            application.container.paymentPreferences\n"
        "        )"
    )
    if old_call in scr_src:
        scr_src = scr_src.replace(old_call, new_call, 1)
        SCR.write_text(scr_src)
        PATCHED.append("Screen: factory call updated")


print()
print("═══ AUTOCOMPLETE PATCH ═══")
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
