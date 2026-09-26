#!/usr/bin/env python3
"""
Replaces deprecated `Divider(` with `HorizontalDivider(` in Kotlin files,
adding the import when missing. Skips files that already use the new name.
"""
from __future__ import annotations
import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
SRC = REPO_ROOT / "app/src/main/java/com/akari/retailer"

DIVIDER_CALL = re.compile(r"(?<![A-Za-z0-9_])Divider\s*\(")
IMPORT_LINE = "import androidx.compose.material3.HorizontalDivider\n"
M3_IMPORT_ANCHOR = re.compile(r"^import androidx\.compose\.material3\.[A-Za-z]", re.M)

patched_files = []
skipped_files = []

for kt in SRC.rglob("*.kt"):
    text = kt.read_text()
    if "Divider(" not in text and "HorizontalDivider(" not in text:
        continue
    if "HorizontalDivider(" in text and not DIVIDER_CALL.search(text):
        skipped_files.append(kt)
        continue

    # Replace bare calls.
    new_text, n = DIVIDER_CALL.subn("HorizontalDivider(", text)
    if n == 0:
        continue

    # Add the import if the file now uses HorizontalDivider and doesn't import it.
    if "HorizontalDivider(" in new_text and IMPORT_LINE not in new_text:
        m = M3_IMPORT_ANCHOR.search(new_text)
        if m:
            # Insert alphabetically after the first material3 import
            insert_at = m.start()
            new_text = new_text[:insert_at] + IMPORT_LINE + new_text[insert_at:]
        else:
            # No material3 imports yet — put it before the first import of any kind
            first_import = re.search(r"^import ", new_text, re.M)
            if first_import:
                new_text = (
                    new_text[:first_import.start()]
                    + IMPORT_LINE
                    + new_text[first_import.start():]
                )
            else:
                # No imports at all — skip (unlikely)
                print(f"⚠  {kt.name}: no import anchor found")
                continue

    kt.write_text(new_text)
    patched_files.append((kt, n))

print()
print("═══ DIVIDER SWEEP ═══")
if patched_files:
    print(f"✅ Patched ({len(patched_files)} files):")
    for kt, n in patched_files:
        print(f"   • {kt.relative_to(REPO_ROOT)}  ({n} call{'s' if n != 1 else ''})")
if skipped_files:
    print()
    print(f"⏭  Already using HorizontalDivider ({len(skipped_files)}):")
    for kt in skipped_files:
        print(f"   • {kt.relative_to(REPO_ROOT)}")
if not patched_files and not skipped_files:
    print("Nothing to do.")
