#!/usr/bin/env python3
"""
Removes the account icon (emoji) prefix from:
- Summary card chips → "Cash 45,000" instead of "💵 Cash 45,000"
- SaleCard payment line → "Cash 45,000" instead of "💵 Cash 45,000"
- Credit line keeps the 💳 for clarity (or remove it too — see CREDIT_ICON below)
"""
from __future__ import annotations
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
SCR = REPO_ROOT / "app/src/main/java/com/akari/retailer/features/sales/presentation/history/SaleHistoryScreen.kt"
CARD = REPO_ROOT / "app/src/main/java/com/akari/retailer/core/ui/components/SaleCard.kt"

# Set to True if you also want the credit line to lose its 💳 emoji.
KEEP_CREDIT_ICON = True

# ── 1. SaleHistoryScreen.kt — chips ────────────────────────────────────────
src = SCR.read_text()
OLD_CHIP = (
    '                            val account = accounts.find { it.id == accountId }\n'
    '                            val icon = account?.icon ?: "💵"\n'
    '                            val name = account?.name ?: "Account"\n'
    '                            val label = "${icon} ${name} ${MoneyFormatter.format(amount)}"'
)
NEW_CHIP = (
    '                            val account = accounts.find { it.id == accountId }\n'
    '                            val name = account?.name ?: "Account"\n'
    '                            val label = "${name} ${MoneyFormatter.format(amount)}"'
)
if NEW_CHIP in src:
    print("⏭  SaleHistoryScreen chips: already patched")
elif OLD_CHIP in src:
    src = src.replace(OLD_CHIP, NEW_CHIP, 1)
    SCR.write_text(src)
    print("✅ SaleHistoryScreen chips: icons removed")
else:
    print("⚠  SaleHistoryScreen chip anchor not found")

# ── 2. SaleCard.kt — payment line ──────────────────────────────────────────
src2 = CARD.read_text()
credit_literal = '"💳 Credit"' if KEEP_CREDIT_ICON else '"Credit"'
OLD_LABEL = (
    '                            val isCredit = p.isCredit\n'
    '                            val label = if (isCredit) {\n'
    '                                "💳 Credit"\n'
    '                            } else {\n'
    '                                val acc = accounts.find { it.id == p.accountId }\n'
    '                                val icon = acc?.icon ?: "💵"\n'
    '                                val name = acc?.name ?: "Account"\n'
    '                                "$icon $name"\n'
    '                            }'
)
NEW_LABEL = (
    '                            val isCredit = p.isCredit\n'
    '                            val label = if (isCredit) {\n'
    f'                                {credit_literal}\n'
    '                            } else {\n'
    '                                val acc = accounts.find { it.id == p.accountId }\n'
    '                                val name = acc?.name ?: "Account"\n'
    '                                name\n'
    '                            }'
)
if NEW_LABEL in src2:
    print("⏭  SaleCard payment line: already patched")
elif OLD_LABEL in src2:
    src2 = src2.replace(OLD_LABEL, NEW_LABEL, 1)
    CARD.write_text(src2)
    print("✅ SaleCard payment line: icons removed")
else:
    print("⚠  SaleCard payment line anchor not found — check the current structure:")
    print('     grep -n "val isCredit = p.isCredit" ' + str(CARD))
