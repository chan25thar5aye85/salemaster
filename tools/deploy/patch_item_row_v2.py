#!/usr/bin/env python3
"""Slightly bigger ItemRow still."""
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

# Bigger vertical padding
OLD = ".padding(vertical = 10.dp),"
NEW = ".padding(vertical = 14.dp),"
if OLD in src:
    src = src.replace(OLD, NEW, 1); changed = True

# Bigger label
OLD = "                fontSize = 16.sp\n"
NEW = "                fontSize = 17.sp\n"
if OLD in src:
    src = src.replace(OLD, NEW, 1); changed = True

OLD = "                .width(80.dp)\n"
NEW = "                .width(86.dp)\n"
if OLD in src:
    src = src.replace(OLD, NEW, 1); changed = True

# Taller amount field
OLD = "                .heightIn(min = 56.dp),"
NEW = "                .heightIn(min = 62.dp),"
if OLD in src:
    src = src.replace(OLD, NEW, 1); changed = True

# Bigger delete button
OLD = "                    .size(56.dp)"
NEW = "                    .size(62.dp)"
if OLD in src:
    src = src.replace(OLD, NEW, 1); changed = True

# Matching spacer for the no-delete branch
OLD = "Spacer(modifier = Modifier.width(56.dp))"
NEW = "Spacer(modifier = Modifier.width(62.dp))"
if OLD in src:
    src = src.replace(OLD, NEW, 1); changed = True

if not changed:
    print("⏭  Nothing changed (already patched?)")
else:
    P.write_text(src)
    print(f"✅ Patched {P.name}")
