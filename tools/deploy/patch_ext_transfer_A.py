#!/usr/bin/env python3
"""
Patch A for External Transfer:
1. Direction labels: "Outgoing" → "Send Money", "Incoming" → "Receive Money"
2. Fix stale balance: re-derive selectedAccount from live accounts flow

Adds strings for both EN and MY.
"""
from __future__ import annotations
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
PKG = REPO_ROOT / "app/src/main/java/com/akari/retailer/features/money"

VM = PKG / "presentation/ExternalTransferViewModel.kt"
SCR = PKG / "presentation/ExternalTransferScreen.kt"
STR_EN = REPO_ROOT / "app/src/main/res/values/strings_money.xml"
STR_MY = REPO_ROOT / "app/src/main/res/values-my/strings_money.xml"

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
    PATCHED.append(f"{label}: {path.relative_to(REPO_ROOT)}")


# ═══════════════════════════════════════════════════════════════════════════
# 1. ExternalTransferViewModel.kt — refresh selectedAccount on flow emit
# ═══════════════════════════════════════════════════════════════════════════
patch_file(
    VM,
    [
        (
            "    private fun loadAccounts() {\n"
            "        loadAccountsJob?.cancel()\n"
            "        loadAccountsJob = viewModelScope.launch {\n"
            "            try {\n"
            "                accountRepository.getAccounts().collect { accounts ->\n"
            "                    val active = accounts.filter { it.isActive }\n"
            "                    val lastUsedId = _state.value.lastUsedAccountId\n"
            "                    val autoSelected = if (_state.value.selectedAccount == null && lastUsedId.isNotEmpty()) {\n"
            "                        active.find { it.id == lastUsedId }\n"
            "                    } else {\n"
            "                        _state.value.selectedAccount\n"
            "                    }\n"
            "                    _state.value = _state.value.copy(\n"
            "                        accounts = active,\n"
            "                        selectedAccount = autoSelected\n"
            "                    )\n"
            "                }\n"
            "            } catch (e: Exception) { }\n"
            "        }\n"
            "    }",

            "    private fun loadAccounts() {\n"
            "        loadAccountsJob?.cancel()\n"
            "        loadAccountsJob = viewModelScope.launch {\n"
            "            try {\n"
            "                accountRepository.getAccounts().collect { accounts ->\n"
            "                    val active = accounts.filter { it.isActive }\n"
            "                    val currentSelected = _state.value.selectedAccount\n"
            "\n"
            "                    // Re-resolve the selected account from the fresh list so\n"
            "                    // its balance stays in sync after transfers / external changes.\n"
            "                    val freshSelected = when {\n"
            "                        currentSelected != null -> active.find { it.id == currentSelected.id }\n"
            "                        _state.value.lastUsedAccountId.isNotEmpty() ->\n"
            "                            active.find { it.id == _state.value.lastUsedAccountId }\n"
            "                        else -> null\n"
            "                    }\n"
            "\n"
            "                    _state.value = _state.value.copy(\n"
            "                        accounts = active,\n"
            "                        selectedAccount = freshSelected\n"
            "                    )\n"
            "                }\n"
            "            } catch (e: Exception) { }\n"
            "        }\n"
            "    }",
        ),
    ],
    "ExternalTransferViewModel: refresh selectedAccount",
)


# ═══════════════════════════════════════════════════════════════════════════
# 2. ExternalTransferScreen.kt — direction labels
# ═══════════════════════════════════════════════════════════════════════════
patch_file(
    SCR,
    [
        (
            "                DirectionButton(\n"
            "                    label = \"↗️ \" + stringResource(R.string.outgoing),",

            "                DirectionButton(\n"
            "                    label = \"📤 \" + stringResource(R.string.send_money),",
        ),
        (
            "                DirectionButton(\n"
            "                    label = \"↙️ \" + stringResource(R.string.incoming),",

            "                DirectionButton(\n"
            "                    label = \"📥 \" + stringResource(R.string.receive_money),",
        ),
    ],
    "ExternalTransferScreen: direction labels",
)


# ═══════════════════════════════════════════════════════════════════════════
# 3. Strings — EN
# ═══════════════════════════════════════════════════════════════════════════
new_strings_en = (
    '    <string name="send_money">Send Money</string>\n'
    '    <string name="receive_money">Receive Money</string>\n'
)
if STR_EN.exists():
    s = STR_EN.read_text()
    if 'name="send_money"' not in s:
        s = s.replace("</resources>", new_strings_en + "</resources>", 1)
        STR_EN.write_text(s)
        PATCHED.append("strings_money.xml (EN): send_money, receive_money")
    else:
        SKIPPED.append("strings_money.xml (EN): already present")

# ═══════════════════════════════════════════════════════════════════════════
# 4. Strings — MY
# ═══════════════════════════════════════════════════════════════════════════
new_strings_my = (
    '    <string name="send_money">ငွေလွှဲပို့ရန်</string>\n'
    '    <string name="receive_money">ငွေလက်ခံရန်</string>\n'
)
if STR_MY.exists():
    s = STR_MY.read_text()
    if 'name="send_money"' not in s:
        s = s.replace("</resources>", new_strings_my + "</resources>", 1)
        STR_MY.write_text(s)
        PATCHED.append("strings_money.xml (MY): send_money, receive_money")
    else:
        SKIPPED.append("strings_money.xml (MY): already present")


print()
print("═══ PATCH A REPORT ═══")
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
