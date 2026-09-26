#!/usr/bin/env python3
"""
External Transfer History:
1. Compact summary card (count + sent/received + net)
2. List grouped by day with per-day header (date · count · day net)
3. Fee shown inline on each row when non-zero
"""
from __future__ import annotations
import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
SCR = REPO_ROOT / "app/src/main/java/com/akari/retailer/features/money/presentation/ExternalTransferHistoryScreen.kt"
STATE = REPO_ROOT / "app/src/main/java/com/akari/retailer/features/money/presentation/ExternalTransferHistoryState.kt"

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
# 1. State — add groupedByDay helper (computed in screen, but add type)
# ═══════════════════════════════════════════════════════════════════════════
# (No state change needed — we compute grouping in the screen from
#  state.filteredTransfers.)


# ═══════════════════════════════════════════════════════════════════════════
# 2. Screen — compact summary card
# ═══════════════════════════════════════════════════════════════════════════
SCR_EDITS = []

# Locate the existing summary card block and replace with the compact version.
summary_re = re.compile(
    r"(// Summary card\n"
    r"\s*Card\(\n"
    r".*?\n"
    r"\s*\}\n"
    r"\s*\}\n)",
    re.DOTALL,
)

m = summary_re.search(SCR.read_text())
if not m:
    print("❌ Could not find the summary card block")
    print("   Grep the file for 'Summary card' and paste the next 30 lines.")
    sys.exit(1)

NEW_SUMMARY = '''// Compact summary card
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
                    // Row 1: count
                    Text(
                        text = "Total transfers  ${state.filteredTransfers.size}",
                        style = AppTypography.body,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Row 2: sent / received
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
                                text = "Sent ",
                                style = AppTypography.small,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
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
                                text = "Received ",
                                style = AppTypography.small,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Text(
                                text = MoneyFormatter.format(state.totalReceived),
                                style = AppTypography.body,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Row 3: net
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Net",
                            style = AppTypography.body,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            text = (if (state.netFlow >= 0) "+" else "") +
                                MoneyFormatter.format(state.netFlow),
                            style = AppTypography.header,
                            color = if (state.netFlow >= 0)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }'''

src = SCR.read_text()
src = src[:m.start()] + NEW_SUMMARY + src[m.end():]
SCR_EDITS.append(("__done__", "__done__"))  # marker so we track the file
SCR.write_text(src)
PATCHED.append("ExternalTransferHistoryScreen: compact summary")


# ═══════════════════════════════════════════════════════════════════════════
# 3. Screen — group list by day
# ═══════════════════════════════════════════════════════════════════════════
src = SCR.read_text()

# Find the current LazyColumn that lists transactions
# It looks like:
#     LazyColumn(
#         modifier = Modifier.fillMaxSize(),
#         verticalArrangement = Arrangement.spacedBy(Spacing.small),
#         contentPadding = PaddingValues(bottom = Spacing.xxlarge)
#     ) {
#         items(state.filteredTransfers, key = { it.id }) { txn ->
#             TransferRow(txn = txn)
#         }
#     }

OLD_LAZY_RE = re.compile(
    r"LazyColumn\(\n"
    r"\s*modifier = Modifier\.fillMaxSize\(\),\n"
    r"\s*verticalArrangement = Arrangement\.spacedBy\(Spacing\.small\),\n"
    r"\s*contentPadding = PaddingValues\(bottom = Spacing\.xxlarge\)\n"
    r"\s*\) \{\n"
    r"\s*items\(state\.filteredTransfers, key = \{ it\.id \}\) \{ txn ->\n"
    r"\s*TransferRow\(txn = txn\)\n"
    r"\s*\}\n"
    r"\s*\}",
    re.DOTALL,
)

m = OLD_LAZY_RE.search(src)
if not m:
    print("⚠  Could not find the LazyColumn block — will try a more tolerant match")

    # Tolerant: match any `items(state.filteredTransfers` inside a LazyColumn
    tolerant = re.compile(
        r"LazyColumn\([^)]*\)\s*\{\s*items\(state\.filteredTransfers[^}]*\}\s*\}",
        re.DOTALL,
    )
    m = tolerant.search(src)
    if not m:
        FAILED.append("ExternalTransferHistoryScreen: could not locate LazyColumn")
        src = None

if src is not None and m:
    NEW_LAZY = '''// Group transfers by calendar day (yyyy-MM-dd), newest day first
            val groupedByDay: List<Pair<String, List<MoneyTransaction>>> = remember(state.filteredTransfers) {
                val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                state.filteredTransfers
                    .groupBy { fmt.format(Date(it.date)) }
                    .toList()
                    .sortedByDescending { it.first }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(Spacing.small),
                contentPadding = PaddingValues(bottom = Spacing.xxlarge)
            ) {
                groupedByDay.forEach { (dayKey, txns) ->
                    // ── Day header ──
                    item(key = "hdr-$dayKey") {
                        DayHeader(dayKey = dayKey, txns = txns)
                    }

                    // ── Rows under this day ──
                    items(txns, key = { it.id }) { txn ->
                        TransferRow(txn = txn)
                    }
                }
            }'''

    src = src[:m.start()] + NEW_LAZY + src[m.end():]
    SCR.write_text(src)
    PATCHED.append("ExternalTransferHistoryScreen: day grouping")

# ═══════════════════════════════════════════════════════════════════════════
# 4. Screen — add DayHeader composable + update TransferRow for fee inline
# ═══════════════════════════════════════════════════════════════════════════
src = SCR.read_text()

# Add DayHeader before the TransferRow function
if "private fun DayHeader(" not in src:
    transfer_row_match = re.search(r"@Composable\nprivate fun TransferRow\(", src)
    if transfer_row_match:
        NEW_HEADER = '''@Composable
private fun DayHeader(
    dayKey: String,
    txns: List<MoneyTransaction>
) {
    val dayLabel = remember(dayKey) {
        try {
            val inFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outFmt = SimpleDateFormat("EEE · MMM dd", Locale.getDefault())
            outFmt.format(inFmt.parse(dayKey)!!)
        } catch (e: Exception) {
            dayKey
        }
    }

    val daySent = txns.filter { it.type == MoneyTransactionType.EXTERNAL_OUT }.sumOf { it.amount }
    val dayReceived = txns.filter { it.type == MoneyTransactionType.EXTERNAL_IN }.sumOf { it.amount }
    val dayNet = dayReceived - daySent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.medium, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = dayLabel,
                style = AppTypography.title,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "· ${txns.size}",
                style = AppTypography.small,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
        Text(
            text = (if (dayNet >= 0) "+" else "") + MoneyFormatter.format(dayNet),
            style = AppTypography.small,
            color = if (dayNet >= 0)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Medium
        )
    }
}

'''
        src = src[:transfer_row_match.start()] + NEW_HEADER + src[transfer_row_match.start():]
        SCR.write_text(src)
        PATCHED.append("ExternalTransferHistoryScreen: DayHeader added")

# Update TransferRow to show fee inline
OLD_TRANSFER_ROW = '''@Composable
private fun TransferRow(txn: MoneyTransaction) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    val isOutgoing = txn.type == MoneyTransactionType.EXTERNAL_OUT
    val color = if (isOutgoing) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = MaterialTheme.shapes.small,
                color = color.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (isOutgoing) "↑" else "↓",
                        style = AppTypography.title,
                        color = color,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(Spacing.medium))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = txn.externalAccountName.ifEmpty { "—" },
                    style = AppTypography.body,
                    fontWeight = FontWeight.Medium
                )
                if (txn.externalAccountNumber.isNotEmpty()) {
                    Text(
                        text = txn.externalAccountNumber,
                        style = AppTypography.small,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                }
                Text(
                    text = dateFormat.format(Date(txn.date)),
                    style = AppTypography.small,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
                if (txn.fee > 0) {
                    Text(
                        text = if (txn.feeType == FeeType.FEE_PAID) "Fee: -${txn.fee}" else "Fee: +${txn.fee}",
                        style = AppTypography.small,
                        color = if (txn.feeType == FeeType.FEE_PAID)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp
                    )
                }
            }

            Text(
                text = (if (isOutgoing) "-" else "+") + MoneyFormatter.format(txn.amount),
                style = AppTypography.title,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}'''

NEW_TRANSFER_ROW = '''@Composable
private fun TransferRow(txn: MoneyTransaction) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val isOutgoing = txn.type == MoneyTransactionType.EXTERNAL_OUT
    val color = if (isOutgoing) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.medium, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Direction badge
            Surface(
                modifier = Modifier.size(32.dp),
                shape = MaterialTheme.shapes.small,
                color = color.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (isOutgoing) "↑" else "↓",
                        style = AppTypography.body,
                        color = color,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(Spacing.medium))

            // Middle: name + description + time
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = timeFormat.format(Date(txn.date)),
                        style = AppTypography.small,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                    Text(
                        text = "  ·  ",
                        style = AppTypography.small,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        fontSize = 11.sp
                    )
                    Text(
                        text = txn.externalAccountName.ifEmpty { "—" },
                        style = AppTypography.body,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
                if (txn.description.isNotEmpty()) {
                    Text(
                        text = txn.description,
                        style = AppTypography.small,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right: amount, with fee inline below if present
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = (if (isOutgoing) "-" else "+") + MoneyFormatter.format(txn.amount),
                    style = AppTypography.body,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
                if (txn.fee > 0) {
                    Text(
                        text = "fee " + (if (txn.feeType == FeeType.FEE_PAID) "-" else "+") +
                            MoneyFormatter.format(txn.fee),
                        style = AppTypography.small,
                        color = if (txn.feeType == FeeType.FEE_PAID)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}'''

if OLD_TRANSFER_ROW in src:
    src = src.replace(OLD_TRANSFER_ROW, NEW_TRANSFER_ROW, 1)
    SCR.write_text(src)
    PATCHED.append("ExternalTransferHistoryScreen: TransferRow updated with fee inline")
elif NEW_TRANSFER_ROW in src:
    SKIPPED.append("TransferRow: already updated")
else:
    FAILED.append("TransferRow: anchor not found — check the current implementation")

# Ensure needed imports
src = SCR.read_text()
for imp in [
    "import androidx.compose.ui.text.font.FontWeight\n",
    "import com.akari.retailer.core.utils.MoneyFormatter\n",
    "import androidx.compose.foundation.layout.size\n",
    "import androidx.compose.foundation.layout.width\n",
]:
    if imp not in src:
        idx = src.find("\nimport ")
        src = src[:idx + 1] + imp + src[idx + 1:]
SCR.write_text(src)


print()
print("═══ EXTERNAL TRANSFER HISTORY UPGRADE ═══")
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
