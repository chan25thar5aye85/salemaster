#!/usr/bin/env python3
"""
Sets every TimeFilter default to TODAY (except Sale History which is
already TODAY).

Handles three shapes:
  - TimeFilter()                                  bare
  - TimeFilter(preset = TimeFilterPreset.THIS_WEEK)   explicit preset
  - FQN: com.akari.retailer.core.ui.components.TimeFilter()  fully qualified
"""
from __future__ import annotations
import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
SRC = REPO_ROOT / "app/src/main/java/com/akari/retailer"

# Explicit replacements — one (old, new) per file. Idempotent.
TARGETS = [
    # TrendsViewModel
    ("features/reports/presentation/TrendsViewModel.kt",
     "val timeFilter: TimeFilter = TimeFilter(),",
     "val timeFilter: TimeFilter = TimeFilter(preset = TimeFilterPreset.TODAY),"),

    # ProfitLossViewModel
    ("features/reports/presentation/ProfitLossViewModel.kt",
     "val timeFilter: TimeFilter = TimeFilter(preset = TimeFilterPreset.THIS_MONTH)",
     "val timeFilter: TimeFilter = TimeFilter(preset = TimeFilterPreset.TODAY)"),

    # ExpenseAnalyticsState
    ("features/expense/presentation/ExpenseAnalyticsState.kt",
     "val timeFilter: TimeFilter = TimeFilter(),",
     "val timeFilter: TimeFilter = TimeFilter(preset = TimeFilterPreset.TODAY),"),

    # ExpenseListState
    ("features/expense/presentation/ExpenseListState.kt",
     "val timeFilter: TimeFilter = TimeFilter(),",
     "val timeFilter: TimeFilter = TimeFilter(preset = TimeFilterPreset.TODAY),"),

    # ExternalTransferHistoryState
    ("features/money/presentation/ExternalTransferHistoryState.kt",
     "val timeFilter: TimeFilter = TimeFilter(preset = TimeFilterPreset.THIS_WEEK),",
     "val timeFilter: TimeFilter = TimeFilter(preset = TimeFilterPreset.TODAY),"),

    # MoneyTransactionsViewModel (line 57)
    ("features/money/presentation/MoneyTransactionsViewModel.kt",
     "timeFilter = TimeFilter(),",
     "timeFilter = TimeFilter(preset = TimeFilterPreset.TODAY),"),

    # MoneyAnalyticsState
    ("features/money/presentation/MoneyAnalyticsState.kt",
     "val timeFilter: TimeFilter = TimeFilter(),",
     "val timeFilter: TimeFilter = TimeFilter(preset = TimeFilterPreset.TODAY),"),

    # MoneyTransactionsState
    ("features/money/presentation/MoneyTransactionsState.kt",
     "val timeFilter: TimeFilter = TimeFilter(),",
     "val timeFilter: TimeFilter = TimeFilter(preset = TimeFilterPreset.TODAY),"),

    # IncomeAnalyticsState
    ("features/sales/presentation/income/IncomeAnalyticsState.kt",
     "val timeFilter: TimeFilter = TimeFilter(),",
     "val timeFilter: TimeFilter = TimeFilter(preset = TimeFilterPreset.TODAY),"),

    # IncomeListViewModel (FQN)
    ("features/sales/presentation/income/IncomeListViewModel.kt",
     "val timeFilter: com.akari.retailer.core.ui.components.TimeFilter = com.akari.retailer.core.ui.components.TimeFilter(),",
     "val timeFilter: com.akari.retailer.core.ui.components.TimeFilter = com.akari.retailer.core.ui.components.TimeFilter(preset = com.akari.retailer.core.ui.components.TimeFilterPreset.TODAY),"),
]

PATCHED, SKIPPED, FAILED = [], [], []

for rel, old, new in TARGETS:
    p = SRC / rel
    if not p.exists():
        FAILED.append(f"{rel}: not found")
        continue

    src = p.read_text()

    if new in src:
        SKIPPED.append(f"{rel}: already TODAY")
        continue

    if old not in src:
        FAILED.append(f"{rel}: anchor not found")
        continue

    src = src.replace(old, new, 1)

    # Ensure TimeFilterPreset import exists (unless the file uses FQN)
    if (
        "TimeFilterPreset" in new
        and "TimeFilterPreset.TODAY" in new
        and "import com.akari.retailer.core.ui.components.TimeFilterPreset" not in src
        and "com.akari.retailer.core.ui.components.TimeFilterPreset" not in new.replace("com.akari.retailer.core.ui.components.TimeFilterPreset.TODAY", "")
    ):
        # The new string uses bare `TimeFilterPreset`, so we need the import
        anchor_imp = "import com.akari.retailer.core.ui.components.TimeFilter\n"
        if anchor_imp in src:
            src = src.replace(
                anchor_imp,
                anchor_imp + "import com.akari.retailer.core.ui.components.TimeFilterPreset\n",
                1,
            )
        else:
            idx = src.find("\nimport ")
            if idx != -1:
                src = (
                    src[:idx + 1]
                    + "import com.akari.retailer.core.ui.components.TimeFilterPreset\n"
                    + src[idx + 1:]
                )

    p.write_text(src)
    PATCHED.append(rel)

print()
print("═══ DEFAULT-TO-TODAY ═══")
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
