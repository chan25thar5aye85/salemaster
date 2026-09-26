#!/usr/bin/env python3
"""
Compact summary card for External Transfer History:
- Row 1: total count + net flow
- Row 2: sent (count + amount) | received (count + amount)

Needs two new fields in the ViewModel state: sentCount, receivedCount.
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
# 1. State — add sentCount, receivedCount
# ═══════════════════════════════════════════════════════════════════════════
patch_file(
    STATE,
    [
        (
            "    // Totals\n"
            "    val totalSent: Int = 0,\n"
            "    val totalReceived: Int = 0,\n"
            "    val netFlow: Int = 0,",

            "    // Totals\n"
            "    val totalSent: Int = 0,\n"
            "    val totalReceived: Int = 0,\n"
            "    val sentCount: Int = 0,\n"
            "    val receivedCount: Int = 0,\n"
            "    val netFlow: Int = 0,",
        ),
    ],
    "State: sentCount, receivedCount",
)


# ═══════════════════════════════════════════════════════════════════════════
# 2. ViewModel — compute the counts in applyFilters()
# ═══════════════════════════════════════════════════════════════════════════
patch_file(
    VM,
    [
        (
            "        val totalSent = filtered\n"
            "            .filter { it.type == MoneyTransactionType.EXTERNAL_OUT }\n"
            "            .sumOf { it.amount }\n"
            "\n"
            "        val totalReceived = filtered\n"
            "            .filter { it.type == MoneyTransactionType.EXTERNAL_IN }\n"
            "            .sumOf { it.amount }",

            "        val sentTxns = filtered.filter { it.type == MoneyTransactionType.EXTERNAL_OUT }\n"
            "        val receivedTxns = filtered.filter { it.type == MoneyTransactionType.EXTERNAL_IN }\n"
            "\n"
            "        val totalSent = sentTxns.sumOf { it.amount }\n"
            "        val totalReceived = receivedTxns.sumOf { it.amount }",
        ),
        (
            "        _state.value = _state.value.copy(\n"
            "            filteredTransfers = filtered,\n"
            "            totalSent = totalSent,\n"
            "            totalReceived = totalReceived,\n"
            "            netFlow = totalReceived - totalSent,",

            "        _state.value = _state.value.copy(\n"
            "            filteredTransfers = filtered,\n"
            "            totalSent = totalSent,\n"
            "            totalReceived = totalReceived,\n"
            "            sentCount = sentTxns.size,\n"
            "            receivedCount = receivedTxns.size,\n"
            "            netFlow = totalReceived - totalSent,",
        ),
    ],
    "ViewModel: compute sentCount, receivedCount",
)


# ═══════════════════════════════════════════════════════════════════════════
# 3. Screen — replace the summary card with compact version
# ═══════════════════════════════════════════════════════════════════════════
# Locate the current summary card (starts with `// ── Summary card (kept as-is) ──`)
src = SCR.read_text()

START_MARKER = "// ── Summary card"
END_MARKER = "Spacer(modifier = Modifier.height(Spacing.medium))"

start = src.find(START_MARKER)
end = src.find(END_MARKER, start) if start != -1 else -1

if start == -1 or end == -1:
    print("❌ Could not locate summary card in screen")
    print("   Grep for 'Summary card' and paste the surrounding lines.")
    sys.exit(1)

NEW_CARD = '''// ── Compact summary ──
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.small),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.medium)
                ) {
                    // Row 1: total transfers + net
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${state.filteredTransfers.size} transfers",
                            style = AppTypography.body,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "Net " + (if (state.netFlow >= 0) "+" else "") +
                                MoneyFormatter.format(state.netFlow),
                            style = AppTypography.body,
                            fontWeight = FontWeight.Bold,
                            color = if (state.netFlow >= 0)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Row 2: sent (count + amount) | received (count + amount)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "▲ ",
                                style = AppTypography.small,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "${state.sentCount} sent  ",
                                style = AppTypography.small,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = MoneyFormatter.format(state.totalSent),
                                style = AppTypography.body,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
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
            }

            '''

src = src[:start] + NEW_CARD + src[end:]
SCR.write_text(src)
PATCHED.append("Screen: compact summary with counts")


print()
print("═══ COMPACT SUMMARY PATCH ═══")
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
