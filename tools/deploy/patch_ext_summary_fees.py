#!/usr/bin/env python3
"""
Adds fee totals to the External Transfer History summary card:
- feesPaid   (sum of fee where feeType == FEE_PAID)
- feesEarned (sum of fee where feeType == FEE_EARNED)

Only shows the fee row when at least one is non-zero.
"""
from __future__ import annotations
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
STATE = REPO_ROOT / "app/src/main/java/com/akari/retailer/features/money/presentation/ExternalTransferHistoryState.kt"
VM = REPO_ROOT / "app/src/main/java/com/akari/retailer/features/money/presentation/ExternalTransferHistoryViewModel.kt"
SCR = REPO_ROOT / "app/src/main/java/com/akari/retailer/features/money/presentation/ExternalTransferHistoryScreen.kt"

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
            FAILED.append(f"{label}: anchor missing")
            return
        src = src.replace(old, new, 1)
    if src == orig:
        SKIPPED.append(f"{label}: already patched")
        return
    path.write_text(src)
    PATCHED.append(f"{label}: {path.name}")


# ═══════════════════════════════════════════════════════════════════════════
# 1. State — add feesPaid, feesEarned
# ═══════════════════════════════════════════════════════════════════════════
patch_file(
    STATE,
    [
        (
            "    val sentCount: Int = 0,\n"
            "    val receivedCount: Int = 0,\n"
            "    val netFlow: Int = 0,",

            "    val sentCount: Int = 0,\n"
            "    val receivedCount: Int = 0,\n"
            "    val feesPaid: Int = 0,\n"
            "    val feesEarned: Int = 0,\n"
            "    val netFlow: Int = 0,",
        ),
    ],
    "State: feesPaid, feesEarned",
)


# ═══════════════════════════════════════════════════════════════════════════
# 2. ViewModel — compute fee totals
# ═══════════════════════════════════════════════════════════════════════════
patch_file(
    VM,
    [
        (
            "        val totalSent = sentTxns.sumOf { it.amount }\n"
            "        val totalReceived = receivedTxns.sumOf { it.amount }",

            "        val totalSent = sentTxns.sumOf { it.amount }\n"
            "        val totalReceived = receivedTxns.sumOf { it.amount }\n"
            "\n"
            "        val feesPaid = filtered\n"
            "            .filter { it.feeType == FeeType.FEE_PAID }\n"
            "            .sumOf { it.fee }\n"
            "        val feesEarned = filtered\n"
            "            .filter { it.feeType == FeeType.FEE_EARNED }\n"
            "            .sumOf { it.fee }",
        ),
        (
            "            sentCount = sentTxns.size,\n"
            "            receivedCount = receivedTxns.size,\n"
            "            netFlow = totalReceived - totalSent,",

            "            sentCount = sentTxns.size,\n"
            "            receivedCount = receivedTxns.size,\n"
            "            feesPaid = feesPaid,\n"
            "            feesEarned = feesEarned,\n"
            "            netFlow = totalReceived - totalSent,",
        ),
    ],
    "ViewModel: compute fee totals",
)


# ═══════════════════════════════════════════════════════════════════════════
# 3. Screen — add fee row to the summary card
# ═══════════════════════════════════════════════════════════════════════════
src = SCR.read_text()

# Insert the fee row after the sent/received Row, before the closing of the Column.
ANCHOR = '''                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "▼ ",
                                style = AppTypography.small,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${state.receivedCount} received  ",
                                style = AppTypography.small,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = MoneyFormatter.format(state.totalReceived),
                                style = AppTypography.body,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }'''

NEW = '''                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "▼ ",
                                style = AppTypography.small,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${state.receivedCount} received  ",
                                style = AppTypography.small,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = MoneyFormatter.format(state.totalReceived),
                                style = AppTypography.body,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Fees row (only if any fees exist)
                    if (state.feesPaid > 0 || state.feesEarned > 0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Fees paid  ",
                                    style = AppTypography.small,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = MoneyFormatter.format(state.feesPaid),
                                    style = AppTypography.body,
                                    color = if (state.feesPaid > 0)
                                        MaterialTheme.colorScheme.error
                                    else
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Fees earned  ",
                                    style = AppTypography.small,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = MoneyFormatter.format(state.feesEarned),
                                    style = AppTypography.body,
                                    color = if (state.feesEarned > 0)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }'''

if NEW.strip() in src:
    print("⏭  Fee row already present")
elif ANCHOR in src:
    src = src.replace(ANCHOR, NEW, 1)
    SCR.write_text(src)
    PATCHED.append("Screen: fee row added")
else:
    FAILED.append("Screen: fee row anchor missing — check the summary card shape")

# Ensure FeeType import
if "import com.akari.retailer.features.money.domain.models.FeeType" not in VM.read_text():
    vm_src = VM.read_text()
    idx = vm_src.find("\nimport ")
    vm_src = vm_src[:idx + 1] + "import com.akari.retailer.features.money.domain.models.FeeType\n" + vm_src[idx + 1:]
    VM.write_text(vm_src)
    PATCHED.append("ViewModel: FeeType import added")


print()
print("═══ FEE ROW PATCH ═══")
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
