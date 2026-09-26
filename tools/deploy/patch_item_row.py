#!/usr/bin/env python3
"""Makes ItemRow taller and more comfortable."""
from __future__ import annotations
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
P = REPO_ROOT / "app/src/main/java/com/akari/retailer/core/ui/components/ItemRow.kt"

if not P.exists():
    print(f"❌ Not found: {P}")
    sys.exit(1)

src = P.read_text()
changed = False

# 1. Bigger row vertical padding
OLD = ".padding(vertical = 4.dp), // ✅ Added vertical padding for each row"
NEW = ".padding(vertical = 10.dp),"
if OLD in src:
    src = src.replace(OLD, NEW, 1); changed = True

# 2. Bigger label
OLD = (
    "        Text(\n"
    "            text = label,\n"
    "            style = AppTypography.body.copy(\n"
    "                fontSize = 15.sp // ✅ Increased font size\n"
    "            ),\n"
    "            modifier = Modifier\n"
    "                .width(70.dp) // ✅ Increased from 60dp to 70dp for better readability\n"
    "        )"
)
NEW = (
    "        Text(\n"
    "            text = label,\n"
    "            style = AppTypography.body.copy(\n"
    "                fontSize = 16.sp\n"
    "            ),\n"
    "            modifier = Modifier\n"
    "                .width(80.dp)\n"
    "                .padding(end = 4.dp)\n"
    "        )"
)
if OLD in src:
    src = src.replace(OLD, NEW, 1); changed = True

# 3. Bigger amount field — add heightIn(min = 56.dp)
OLD = (
    "        AmountField(\n"
    "            amount = amount,\n"
    "            onAmountChange = onAmountChange,\n"
    "            modifier = Modifier.weight(1f),"
)
NEW = (
    "        AmountField(\n"
    "            amount = amount,\n"
    "            onAmountChange = onAmountChange,\n"
    "            modifier = Modifier\n"
    "                .weight(1f)\n"
    "                .heightIn(min = 56.dp),"
)
if OLD in src:
    src = src.replace(OLD, NEW, 1); changed = True

# 4. Bigger delete button
OLD = (
    "                modifier = Modifier\n"
    "                    .padding(start = Spacing.medium) // ✅ Increased from small to medium\n"
    "                    .width(48.dp) // ✅ Fixed width for delete button"
)
NEW = (
    "                modifier = Modifier\n"
    "                    .padding(start = Spacing.medium)\n"
    "                    .size(56.dp)"
)
if OLD in src:
    src = src.replace(OLD, NEW, 1); changed = True

# 5. Ensure imports for heightIn and size exist
needed = [
    ("androidx.compose.foundation.layout.heightIn",
     "import androidx.compose.foundation.layout.heightIn\n"),
    ("androidx.compose.foundation.layout.size",
     "import androidx.compose.foundation.layout.size\n"),
]
first_import = src.find("\nimport ")
for check, imp in needed:
    if check not in src and first_import != -1:
        src = src[:first_import + 1] + imp + src[first_import + 1:]

if not changed:
    print("⏭  Nothing changed (already patched?)")
else:
    P.write_text(src)
    print(f"✅ Patched {P.name}")
