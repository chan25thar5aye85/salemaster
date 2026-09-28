#!/usr/bin/env python3
"""
Makes the external account name optional.
- Blank name → stored as "External"
- Field label updated to signal optional
- Validation relaxed
- History display shows "External" for blanks
"""
from __future__ import annotations
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
PKG = REPO_ROOT / "app/src/main/java/com/akari/retailer/features/money/presentation"

VM = PKG / "ExternalTransferViewModel.kt"
SCR = PKG / "ExternalTransferScreen.kt"
HIST = PKG / "ExternalTransferHistoryScreen.kt"

PATCHED, SKIPPED, FAILED = [], [], []

def patch_file(path: Path, edits, label: str) -> None:
    if not path.exists():
        FAILED.append(f"{label}: not found → {path}")
        return
    src = path.read_text()
    orig = src
    for old, new in edits:
        if new in src:
            continue
        if old not in src:
            FAILED.append(f"{label}: anchor missing in {path.name}")
            return
        src = src.replace(old, new, 1)
    if src == orig:
        SKIPPED.append(f"{label}: already patched")
        return
    path.write_text(src)
    PATCHED.append(f"{label}: {path.name}")


# ═══════════════════════════════════════════════════════════════════════════
# 1. VIEWMODEL — remove the blank check + default to "External"
# ═══════════════════════════════════════════════════════════════════════════
vm_src = VM.read_text()

# 1a. Remove the "External account name is required" validation block
import re
validation_pat = re.compile(
    r"\s*if \(currentState\.externalAccountName\.isBlank\(\)\) \{\s*\n"
    r"\s*_state\.value = _state\.value\.copy\(error = \"External account name is required\"\)\s*\n"
    r"\s*return\s*\n"
    r"\s*\}\s*\n",
    re.DOTALL,
)
if validation_pat.search(vm_src):
    vm_src = validation_pat.sub("\n", vm_src, count=1)
    PATCHED.append("ViewModel: removed blank-name validation")
else:
    SKIPPED.append("ViewModel: validation already removed or shape differs")

# 1b. Default blank to "External" when building Params
old_assign = "externalAccountName = currentState.externalAccountName.trim(),"
new_assign = "externalAccountName = currentState.externalAccountName.trim().ifBlank { \"External\" },"
if new_assign in vm_src:
    SKIPPED.append("ViewModel: default-to-External already applied")
elif old_assign in vm_src:
    vm_src = vm_src.replace(old_assign, new_assign, 1)
    PATCHED.append("ViewModel: blank defaults to 'External'")
else:
    FAILED.append("ViewModel: Params assignment anchor missing")

VM.write_text(vm_src)


# ═══════════════════════════════════════════════════════════════════════════
# 2. SCREEN — update the field label to signal optional
# ═══════════════════════════════════════════════════════════════════════════
scr_src = SCR.read_text()

# The label is currently:
#     text = if (state.isOutgoing())
#         stringResource(R.string.to_account)
#     else
#         stringResource(R.string.from_account),
# around line 190-195 (before the field).
# We change the string used when the label refers to the external side.
#
# Simplest: append " (optional)" to whichever label the external side uses,
# by using a new string resource.

# Find the label Text in the External Account Name block:
# it's a `Text(text = if (state.isOutgoing()) stringResource(R.string.to_account) else ...`
# We'll swap in a new string resource name for the external side.

# Two identical blocks may exist — one for the money-account, one for the
# external-name. We only want the second one (the external name column).
# We key on the fact that the external name column is followed by
# `val query = state.externalAccountName.trim()`.

# Simplest safe patch: append "(optional)" as a separate small text after the label
anchor_label = '''                    Text(
                        text = if (state.isOutgoing())
                            stringResource(R.string.to_account)
                        else
                            stringResource(R.string.from_account),
                        style = AppTypography.label,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    val query = state.externalAccountName.trim()'''

new_label = '''                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (state.isOutgoing())
                                stringResource(R.string.to_account)
                            else
                                stringResource(R.string.from_account),
                            style = AppTypography.label,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "(${stringResource(R.string.optional)})",
                            style = AppTypography.small,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontSize = 10.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))

                    val query = state.externalAccountName.trim()'''

if new_label.strip() in scr_src:
    SKIPPED.append("Screen: optional label already applied")
elif anchor_label in scr_src:
    scr_src = scr_src.replace(anchor_label, new_label, 1)
    PATCHED.append("Screen: optional label added")
else:
    FAILED.append("Screen: label anchor missing — check around the External Account Name block")

SCR.write_text(scr_src)


# ═══════════════════════════════════════════════════════════════════════════
# 3. SCREEN — fix the "can submit" condition (line ~580)
# ═══════════════════════════════════════════════════════════════════════════
scr_src = SCR.read_text()

# Currently:
#     enabled = state.selectedAccount != null &&
#              state.externalAccountName.isNotEmpty() &&
#              state.amount.isNotEmpty() &&
#              !state.isSaving

old_enabled = (
    "                enabled = state.selectedAccount != null && \n"
    "                         state.externalAccountName.isNotEmpty() && \n"
    "                         state.amount.isNotEmpty() && \n"
    "                         !state.isSaving"
)

# Try a few whitespace variants
variants = [
    old_enabled,
    old_enabled.replace(" && \n", " &&\n"),
    old_enabled.replace(" && \n", " && \n").replace("isNotEmpty", "isNotBlank"),
]

new_enabled = (
    "                enabled = state.selectedAccount != null &&\n"
    "                         state.amount.isNotEmpty() &&\n"
    "                         !state.isSaving"
)

patched_condition = False
for v in variants:
    if new_enabled in scr_src:
        SKIPPED.append("Screen: submit condition already relaxed")
        patched_condition = True
        break
    if v in scr_src:
        scr_src = scr_src.replace(v, new_enabled, 1)
        PATCHED.append("Screen: submit no longer requires external name")
        patched_condition = True
        break

if not patched_condition:
    FAILED.append("Screen: submit-condition anchor missing — check the 'enabled =' line")

SCR.write_text(scr_src)


# ═══════════════════════════════════════════════════════════════════════════
# 4. HISTORY — show "External" instead of "—"
# ═══════════════════════════════════════════════════════════════════════════
patch_file(
    HIST,
    [
        (
            "text = txn.externalAccountName.ifEmpty { \"—\" },",
            "text = txn.externalAccountName.ifBlank { \"External\" },",
        ),
    ],
    "History: show 'External' for blanks",
)


# ═══════════════════════════════════════════════════════════════════════════
# 5. STRINGS — add "optional"
# ═══════════════════════════════════════════════════════════════════════════
EN = REPO_ROOT / "app/src/main/res/values/strings_money.xml"
MY = REPO_ROOT / "app/src/main/res/values-my/strings_money.xml"

for path, txt in [(EN, "optional"), (MY, "မထည့်လည်းရ")]:
    if path.exists():
        s = path.read_text()
        if 'name="optional"' not in s:
            s = s.replace("</resources>", f'    <string name="optional">{txt}</string>\n</resources>', 1)
            path.write_text(s)
            PATCHED.append(f"{path.name}: optional string")


print()
print("═══ OPTIONAL EXTERNAL NAME PATCH ═══")
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
