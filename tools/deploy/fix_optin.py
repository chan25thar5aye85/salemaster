#!/usr/bin/env python3
"""
Fixes @file:OptIn placement in SaleHistoryScreen.kt:
1. Remove any @file:OptIn(...) that ended up after imports
2. Insert a fresh @file:OptIn(ExperimentalLayoutApi::class) BEFORE package
"""
from __future__ import annotations
import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
P = REPO_ROOT / "app/src/main/java/com/akari/retailer/features/sales/presentation/history/SaleHistoryScreen.kt"

if not P.exists():
    print(f"❌ Not found: {P}")
    sys.exit(1)

src = P.read_text()

# 1. Remove any existing @file:OptIn lines (wherever they are)
src = re.sub(r"^\s*@file:OptIn\([^)]*\)\s*\n", "", src, flags=re.M)

# 2. Make sure package line is there
if not re.search(r"^package ", src, re.M):
    print("❌ No package declaration")
    sys.exit(1)

# 3. Insert @file:OptIn line right before `package`
src = re.sub(
    r"^package ",
    "@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)\n\npackage ",
    src,
    count=1,
    flags=re.M,
)

P.write_text(src)
print("✅ Fixed: @file:OptIn placed before package declaration")
