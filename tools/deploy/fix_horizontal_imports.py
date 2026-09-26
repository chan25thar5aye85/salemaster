#!/usr/bin/env python3
"""Adds `import androidx.compose.material3.HorizontalDivider` to files that
use HorizontalDivider but neither import it explicitly nor via wildcard."""
from __future__ import annotations
import re
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
SRC = REPO_ROOT / "app/src/main/java/com/akari/retailer"

IMPORT_LINE = "import androidx.compose.material3.HorizontalDivider\n"
WILDCARD = "import androidx.compose.material3.*"
SPECIFIC = re.compile(r"^import androidx\.compose\.material3\.HorizontalDivider\s*$", re.M)
FIRST_M3 = re.compile(r"^import androidx\.compose\.material3\.[A-Za-z]", re.M)

fixed = []
skipped_wildcard = []
skipped_specific = []

for kt in SRC.rglob("*.kt"):
    text = kt.read_text()
    if "HorizontalDivider(" not in text:
        continue
    if SPECIFIC.search(text):
        skipped_specific.append(kt)
        continue
    if WILDCARD in text:
        skipped_wildcard.append(kt)
        continue

    # Need to add the import.
    m = FIRST_M3.search(text)
    if m:
        text = text[:m.start()] + IMPORT_LINE + text[m.start():]
    else:
        first_import = re.search(r"^import ", text, re.M)
        if not first_import:
            print(f"⚠  {kt.name}: no imports at all, skipping")
            continue
        text = text[:first_import.start()] + IMPORT_LINE + text[first_import.start():]

    kt.write_text(text)
    fixed.append(kt)

print()
print("═══ HORIZONTAL DIVIDER IMPORT FIX ═══")
if fixed:
    print(f"✅ Added import ({len(fixed)}):")
    for kt in fixed:
        print(f"   • {kt.relative_to(REPO_ROOT)}")
if skipped_wildcard:
    print()
    print(f"⏭  Wildcard import present ({len(skipped_wildcard)}):")
    for kt in skipped_wildcard:
        print(f"   • {kt.relative_to(REPO_ROOT)}")
if skipped_specific:
    print()
    print(f"⏭  Already has specific import ({len(skipped_specific)}):")
    for kt in skipped_specific:
        print(f"   • {kt.relative_to(REPO_ROOT)}")
