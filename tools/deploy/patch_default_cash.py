#!/usr/bin/env python3
"""
Forces every payment row to default to 'Cash' by making
PaymentPreferences.getLastUsedAccountId() always return the default.
The write side (setLastUsedAccountId) is left intact so the feature can
be re-enabled by reverting one line.
"""
from __future__ import annotations
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
P = REPO_ROOT / "app/src/main/java/com/akari/retailer/core/utils/PaymentPreferences.kt"

if not P.exists():
    print(f"❌ Not found: {P}")
    sys.exit(1)

src = P.read_text()

OLD = '''    // ── Last used payment account (used by Expense / Income / Sale / Purchase) ──
    fun getLastUsedAccountId(): String =
        prefs.getString(KEY_LAST_ACCOUNT, DEFAULT_ACCOUNT_ID) ?: DEFAULT_ACCOUNT_ID

    fun setLastUsedAccountId(accountId: String) {
        prefs.edit().putString(KEY_LAST_ACCOUNT, accountId).apply()
    }'''

NEW = '''    // ── Last used payment account (used by Expense / Income / Sale / Purchase) ──
    // NOTE: We intentionally ignore the stored value and always return the
    // default (Cash) so every new payment row starts at Cash. The stored
    // value is still written by setLastUsedAccountId — flip the read back to
    // `prefs.getString(...)` if you want the "remember" behaviour returned.
    fun getLastUsedAccountId(): String = DEFAULT_ACCOUNT_ID

    @Suppress("unused")
    fun setLastUsedAccountId(accountId: String) {
        prefs.edit().putString(KEY_LAST_ACCOUNT, accountId).apply()
    }'''

if OLD not in src:
    if "fun getLastUsedAccountId(): String = DEFAULT_ACCOUNT_ID" in src:
        print("⏭  Already patched")
        sys.exit(0)
    print("❌ Could not find the getLastUsedAccountId block")
    sys.exit(1)

src = src.replace(OLD, NEW, 1)
P.write_text(src)
print(f"✅ Patched {P.name}: payment default is always Cash")
