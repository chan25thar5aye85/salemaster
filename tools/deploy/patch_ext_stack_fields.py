#!/usr/bin/env python3
"""
Restructure the External Transfer form:
- Before: [From Account ▾] [To Account (name)]  side by side
- After:  [From Account ▾]
          [To Account (name)]                    stacked

This removes the weight(1f) + Row alignment that breaks DropdownMenu anchors.
"""
from __future__ import annotations
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
P = REPO_ROOT / "app/src/main/java/com/akari/retailer/features/money/presentation/ExternalTransferScreen.kt"

if not P.exists():
    print(f"❌ Not found: {P}")
    sys.exit(1)

src = P.read_text()

# The current structure is a Row containing two Columns:
#   Row { Column(weight=1f) { Account }  Column(weight=1f) { External Name } }
#
# We want to unwrap it into:
#   Column { Account }
#   Column { External Name }

# Locate the Row
MARKER = "// Account + External Account Name in ONE ROW"
start = src.find(MARKER)
if start == -1:
    print("❌ Could not find 'Account + External Account Name in ONE ROW' marker")
    sys.exit(1)

# Find the Row( after the marker
row_start = src.find("Row(", start)
if row_start == -1:
    print("❌ Could not find Row( after the marker")
    sys.exit(1)

# Walk braces of the Row to find its end
brace_start = src.index("{", row_start)
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
row_end = i + 1

old_block = src[start:row_end]

# Extract the two Columns' inner content by matching `Column(\n...weight(1f)...) { ... }`
# Simpler: extract everything between Row { and Row } and strip the Row wrapper.
inner = old_block[old_block.index("{") + 1 : old_block.rindex("}")]

# Now `inner` contains two Column(...) { ... } blocks. We want them stacked with a spacer.
# Replace `Modifier.weight(1f)` with `Modifier.fillMaxWidth()`.

NEW_BLOCK = '''// Account + External Account Name — stacked vertically
            Column(modifier = Modifier.fillMaxWidth()) {

''' + inner.replace("Modifier.weight(1f)", "Modifier.fillMaxWidth()") + '''
            }
            '''

# Also insert a Spacer between the two columns if there isn't one.
# The inner has structure like:
#     Column(...) { ... }
#     Column(...) { ... }
# We want:  Column(...) { ... }
#           Spacer(Modifier.height(Spacing.medium))
#           Column(...) { ... }
# Use a simple replace: put the spacer between the two closing Column boundaries.
# Find the end of the first Column (first `}` at top-level in inner).
inner_depths = 0
first_col_end = -1
for idx, ch in enumerate(inner):
    if ch == "{":
        inner_depths += 1
    elif ch == "}":
        inner_depths -= 1
        if inner_depths == 0 and first_col_end == -1:
            first_col_end = idx + 1

if first_col_end != -1:
    inner_with_spacer = (
        inner[:first_col_end]
        + "\n\n                Spacer(modifier = Modifier.height(Spacing.medium))\n\n"
        + inner[first_col_end:]
    )
    NEW_BLOCK = '''// Account + External Account Name — stacked vertically
            Column(modifier = Modifier.fillMaxWidth()) {

''' + inner_with_spacer.replace("Modifier.weight(1f)", "Modifier.fillMaxWidth()") + '''
            }
            '''
else:
    NEW_BLOCK = '''// Account + External Account Name — stacked vertically
            Column(modifier = Modifier.fillMaxWidth()) {

''' + inner.replace("Modifier.weight(1f)", "Modifier.fillMaxWidth()") + '''
            }
            '''

src = src[:start] + NEW_BLOCK + src[row_end:]

P.write_text(src)
print("✅ Stacked Account and External Name fields vertically")
