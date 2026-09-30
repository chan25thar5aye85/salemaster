#!/usr/bin/env python3
"""
Replaces `close(error)` inside snapshot listeners with a log-and-continue
pattern so transient Firestore errors don't kill the flow permanently.

Scans every .kt file under features/ and data/ for the pattern:
    if (error != null) { close(error); return@addSnapshotListener }

and rewrites it to:
    if (error != null) { Log.w(TAG, "..."); return@addSnapshotListener }

Idempotent: skips if already patched.
"""
from __future__ import annotations
import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
SRC_ROOT = REPO_ROOT / "app/src/main/java/com/akari/retailer"

# Two common shapes:
#   A:  if (error != null) { close(error); return@addSnapshotListener }
#   B:  if (error != null) {
#           close(error)
#           return@addSnapshotListener
#       }
#
# We preserve the surrounding whitespace and produce a consistent log line.

ONE_LINE = re.compile(
    r"(?P<indent>[ \t]*)if \(error != null\) \{ close\(error\); return@addSnapshotListener \}"
)

MULTI_LINE = re.compile(
    r"(?P<indent>[ \t]*)if \(error != null\) \{\s*\n"
    r"[ \t]*close\(error\)\s*\n"
    r"[ \t]*return@addSnapshotListener\s*\n"
    r"[ \t]*\}"
)

PATCHED = []
SKIPPED = []
FAILED = []

for kt in SRC_ROOT.rglob("*.kt"):
    text = kt.read_text()
    original = text

    def replace(match: re.Match) -> str:
        indent = match.group("indent")
        tag = f'"{kt.stem}"'
        return (
            f'{indent}if (error != null) {{\n'
            f'{indent}    android.util.Log.w({tag},\n'
            f'{indent}        "listener error (transient, continuing): ${{error.message}}")\n'
            f'{indent}    return@addSnapshotListener\n'
            f'{indent}}}'
        )

    text, n1 = ONE_LINE.subn(replace, text)
    text, n2 = MULTI_LINE.subn(replace, text)

    total = n1 + n2
    if total == 0:
        continue

    if text == original:
        SKIPPED.append(f"{kt.relative_to(SRC_ROOT)}: no change")
        continue

    kt.write_text(text)
    PATCHED.append(f"{kt.relative_to(SRC_ROOT)}: {total} listener(s) fixed")


print()
print("═══ LISTENER CLEANUP ═══")
print()
if PATCHED:
    print(f"✅ Patched ({len(PATCHED)} files):")
    for line in PATCHED:
        print(f"   • {line}")
if SKIPPED:
    print()
    print(f"⏭  Skipped ({len(SKIPPED)}):")
    for line in SKIPPED:
        print(f"   • {line}")
if not PATCHED and not SKIPPED:
    print("Nothing to do — no matching pattern found.")
