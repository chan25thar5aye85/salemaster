#!/usr/bin/env python3
"""
Adds labels to summary card lines:
- Line 1: "Total 125,000 · 8 transactions"
- Chips: "💵 Cash 45,000" (icon + name + amount)
- Sale card payment line: "💵 Cash 45,000"
"""
from __future__ import annotations
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
SCR = REPO_ROOT / "app/src/main/java/com/akari/retailer/features/sales/presentation/history/SaleHistoryScreen.kt"
CARD = REPO_ROOT / "app/src/main/java/com/akari/retailer/core/ui/components/SaleCard.kt"

# ── 1. SaleHistoryScreen.kt ────────────────────────────────────────────────
src = SCR.read_text()
changed = False

# 1a. Line 1: Total + transactions with labels
OLD_LINE1 = (
    "            // Line 1: totals\n"
    "            Row(verticalAlignment = Alignment.CenterVertically) {\n"
    "                Text(\n"
    "                    text = MoneyFormatter.format(state.totalSales),\n"
    "                    style = AppTypography.header,\n"
    "                    fontWeight = FontWeight.Bold,\n"
    "                    color = MaterialTheme.colorScheme.primary\n"
    "                )\n"
    "                Spacer(modifier = Modifier.width(Spacing.small))\n"
    "                Text(\n"
    "                    text = \"· ${state.salesCount} ${stringResource(R.string.sales).lowercase()}\",\n"
    "                    style = AppTypography.body,\n"
    "                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)\n"
    "                )\n"
    "            }"
)
NEW_LINE1 = (
    "            // Line 1: totals with labels\n"
    "            Row(verticalAlignment = Alignment.CenterVertically) {\n"
    "                Text(\n"
    "                    text = \"Total \",\n"
    "                    style = AppTypography.body,\n"
    "                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)\n"
    "                )\n"
    "                Text(\n"
    "                    text = MoneyFormatter.format(state.totalSales),\n"
    "                    style = AppTypography.header,\n"
    "                    fontWeight = FontWeight.Bold,\n"
    "                    color = MaterialTheme.colorScheme.primary\n"
    "                )\n"
    "                Spacer(modifier = Modifier.width(Spacing.small))\n"
    "                Text(\n"
    "                    text = \"·\",\n"
    "                    style = AppTypography.body,\n"
    "                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)\n"
    "                )\n"
    "                Spacer(modifier = Modifier.width(Spacing.small))\n"
    "                Text(\n"
    "                    text = \"${state.salesCount} transactions\",\n"
    "                    style = AppTypography.body,\n"
    "                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)\n"
    "                )\n"
    "            }"
)
if NEW_LINE1.strip() not in src:
    if OLD_LINE1 in src:
        src = src.replace(OLD_LINE1, NEW_LINE1, 1)
        changed = True
    else:
        print("⚠  Line 1 anchor not found — format may differ")

# 1b. Chips: include account name
OLD_CHIP = (
    "                            val account = accounts.find { it.id == accountId }\n"
    "                            val icon = account?.icon ?: \"💵\"\n"
    "                            val label = \"${icon} ${MoneyFormatter.format(amount)}\""
)
NEW_CHIP = (
    "                            val account = accounts.find { it.id == accountId }\n"
    "                            val icon = account?.icon ?: \"💵\"\n"
    "                            val name = account?.name ?: \"Account\"\n"
    "                            val label = \"${icon} ${name} ${MoneyFormatter.format(amount)}\""
)
if NEW_CHIP.strip() not in src:
    if OLD_CHIP in src:
        src = src.replace(OLD_CHIP, NEW_CHIP, 1)
        changed = True
    else:
        print("⚠  Chip anchor not found — format may differ")

if changed:
    SCR.write_text(src)
    print("✅ Patched SaleHistoryScreen.kt")
else:
    print("⏭  SaleHistoryScreen.kt: no changes applied (already patched or anchors missing)")

# ── 2. SaleCard.kt — include account name in payment line ─────────────────
src2 = CARD.read_text()
changed2 = False

OLD_LABEL = (
    "                            val isCredit = p.isCredit\n"
    "                            val label = if (isCredit) {\n"
    "                                \"💳\"\n"
    "                            } else {\n"
    "                                val acc = accounts.find { it.id == p.accountId }\n"
    "                                acc?.icon ?: \"💵\"\n"
    "                            }\n"
    "                            Text(\n"
    "                                text = \"$label ${MoneyFormatter.format(p.amount)}\","
)
NEW_LABEL = (
    "                            val isCredit = p.isCredit\n"
    "                            val label = if (isCredit) {\n"
    "                                \"💳 Credit\"\n"
    "                            } else {\n"
    "                                val acc = accounts.find { it.id == p.accountId }\n"
    "                                val icon = acc?.icon ?: \"💵\"\n"
    "                                val name = acc?.name ?: \"Account\"\n"
    "                                \"$icon $name\"\n"
    "                            }\n"
    "                            Text(\n"
    "                                text = \"$label ${MoneyFormatter.format(p.amount)}\","
)
if NEW_LABEL.strip() not in src2:
    if OLD_LABEL in src2:
        src2 = src2.replace(OLD_LABEL, NEW_LABEL, 1)
        changed2 = True
    else:
        print("⚠  SaleCard payment label anchor not found")

if changed2:
    CARD.write_text(src2)
    print("✅ Patched SaleCard.kt")
else:
    print("⏭  SaleCard.kt: no changes applied (already patched or anchors missing)")
