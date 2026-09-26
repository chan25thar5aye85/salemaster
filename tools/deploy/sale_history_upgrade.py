#!/usr/bin/env python3
"""
Sale History upgrade — three pieces in one pass:

1. AppScreen.kt — add optional `subtitle: String? = null` param (backwards-compatible)
2. SaleHistoryState.kt — add paymentBreakdown / paid / credit fields
3. SaleHistoryViewModel.kt — compute the three from filtered sales
4. SaleHistoryScreen.kt — new compact SaleSummaryCard + header subtitle
5. SaleCard.kt — optional payments / accounts params, render payment line

Idempotent — checks for markers before patching.
"""
from __future__ import annotations
import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
PKG = REPO_ROOT / "app/src/main/java/com/akari/retailer"
STRINGS_EN = REPO_ROOT / "app/src/main/res/values/strings_reports.xml"
STRINGS_MY = REPO_ROOT / "app/src/main/res/values-my/strings_reports.xml"

PATCHED = []
SKIPPED = []
FAILED = []

def patch_file(path: Path, edits: list[tuple[str, str]], label: str) -> None:
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
    PATCHED.append(f"{label}: {path.relative_to(REPO_ROOT)}")


# ═══════════════════════════════════════════════════════════════════════════
# 1. AppScreen.kt — add optional subtitle
# ═══════════════════════════════════════════════════════════════════════════
patch_file(
    PKG / "core/ui/components/AppScreen.kt",
    [
        (
            # Constructor signature
            "fun AppScreen(\n"
            "    title: String,\n"
            "    modifier: Modifier = Modifier,\n",

            "fun AppScreen(\n"
            "    title: String,\n"
            "    subtitle: String? = null,\n"
            "    modifier: Modifier = Modifier,\n",
        ),
        (
            # TopAppBar title — show subtitle under the title if present
            "                    title = {\n"
            "                        Text(\n"
            "                            title,\n"
            "                            color = MaterialTheme.colorScheme.onSurface\n"
            "                        )\n"
            "                    },\n",

            "                    title = {\n"
            "                        Column {\n"
            "                            Text(\n"
            "                                title,\n"
            "                                color = MaterialTheme.colorScheme.onSurface\n"
            "                            )\n"
            "                            if (!subtitle.isNullOrBlank()) {\n"
            "                                Text(\n"
            "                                    subtitle,\n"
            "                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),\n"
            "                                    style = AppTypography.small\n"
            "                                )\n"
            "                            }\n"
            "                        }\n"
            "                    },\n",
        ),
    ],
    "AppScreen: subtitle param",
)


# ═══════════════════════════════════════════════════════════════════════════
# 2. SaleHistoryState.kt — add computed fields
# ═══════════════════════════════════════════════════════════════════════════
patch_file(
    PKG / "features/sales/presentation/history/SaleHistoryState.kt",
    [
        (
            "    // Summary totals (computed from the filtered list)\n"
            "    val totalSales: Int = 0,\n"
            "    val salesCount: Int = 0,\n"
            "    val totalPaid: Int = 0,\n"
            "    val totalCredit: Int = 0\n"
            ")",

            "    // Summary totals (computed from the filtered list)\n"
            "    val totalSales: Int = 0,\n"
            "    val salesCount: Int = 0,\n"
            "    val totalPaid: Int = 0,\n"
            "    val totalCredit: Int = 0,\n"
            "\n"
            "    /**\n"
            "     * Money-in totals per money account id.\n"
            "     * Excludes credit rows. Only accounts with a non-zero total are kept.\n"
            "     */\n"
            "    val paymentBreakdown: Map<String, Int> = emptyMap()\n"
            ")",
        ),
    ],
    "SaleHistoryState: paymentBreakdown field",
)


# ═══════════════════════════════════════════════════════════════════════════
# 3. SaleHistoryViewModel.kt — compute the breakdown
# ═══════════════════════════════════════════════════════════════════════════
patch_file(
    PKG / "features/sales/presentation/history/SaleHistoryViewModel.kt",
    [
        (
            "        val totalSales = filtered.sumOf { it.total }\n"
            "        val totalPaid = filtered.sumOf { sale ->\n"
            "            sale.payments.filter { !it.isCredit }.sumOf { it.amount }\n"
            "        }\n"
            "        val totalCredit = filtered.sumOf { sale ->\n"
            "            sale.payments.filter { it.isCredit }.sumOf { it.amount }\n"
            "        }\n"
            "\n"
            "        _state.value = _state.value.copy(\n"
            "            sales = filtered,\n"
            "            totalSales = totalSales,\n"
            "            salesCount = filtered.size,\n"
            "            totalPaid = totalPaid,\n"
            "            totalCredit = totalCredit,\n"
            "            isLoading = false,\n"
            "            isRefreshing = false\n"
            "        )",

            "        val totalSales = filtered.sumOf { it.total }\n"
            "        val totalPaid = filtered.sumOf { sale ->\n"
            "            sale.payments.filter { !it.isCredit }.sumOf { it.amount }\n"
            "        }\n"
            "        val totalCredit = filtered.sumOf { sale ->\n"
            "            sale.payments.filter { it.isCredit }.sumOf { it.amount }\n"
            "        }\n"
            "\n"
            "        // Per-account money-in totals (excludes credit rows).\n"
            "        val breakdown = mutableMapOf<String, Int>()\n"
            "        filtered.forEach { sale ->\n"
            "            sale.payments.forEach { p ->\n"
            "                if (!p.isCredit && p.accountId.isNotBlank()) {\n"
            "                    breakdown[p.accountId] = (breakdown[p.accountId] ?: 0) + p.amount\n"
            "                }\n"
            "            }\n"
            "        }\n"
            "\n"
            "        _state.value = _state.value.copy(\n"
            "            sales = filtered,\n"
            "            totalSales = totalSales,\n"
            "            salesCount = filtered.size,\n"
            "            totalPaid = totalPaid,\n"
            "            totalCredit = totalCredit,\n"
            "            paymentBreakdown = breakdown.filterValues { it > 0 },\n"
            "            isLoading = false,\n"
            "            isRefreshing = false\n"
            "        )",
        ),
    ],
    "SaleHistoryViewModel: compute breakdown",
)


# ═══════════════════════════════════════════════════════════════════════════
# 4. SaleHistoryScreen.kt — new compact summary + header subtitle
# ═══════════════════════════════════════════════════════════════════════════
SCREEN = PKG / "features/sales/presentation/history/SaleHistoryScreen.kt"
src = SCREEN.read_text()

# 4a. Grab account info from the container — needed for chip icons/names
if "accountRepository" not in src:
    src = src.replace(
        "    val repository = remember { application.container.saleRepository }\n",
        "    val repository = remember { application.container.saleRepository }\n"
        "    val accountRepository = remember { application.container.moneyAccountRepository }\n"
        "    val accounts by accountRepository.getAccounts()\n"
        "        .collectAsState(initial = emptyList())\n",
        1,
    )

# 4b. Header — pass subtitle to AppScreen
src = src.replace(
    '    AppScreen(\n'
    '        title = stringResource(R.string.history),\n'
    '        showBackButton = true,\n'
    '        onBackClick = onBack,',
    '    val headerSubtitle = state.timeFilter.label\n'
    '        .ifBlank { state.timeFilter.preset.name.replace(\'_\', \' \').lowercase()\n'
    '            .replaceFirstChar { it.titlecase() } }\n'
    '\n'
    '    AppScreen(\n'
    '        title = stringResource(R.string.history),\n'
    '        subtitle = headerSubtitle,\n'
    '        showBackButton = true,\n'
    '        onBackClick = onBack,',
    1,
)

# 4c. Replace the old SaleSummaryCard with the compact version
OLD_SUMMARY_CARD_RE = re.compile(
    r"@Composable\nprivate fun SaleSummaryCard\(state: SaleHistoryState\) \{.*?\n\}\n",
    re.DOTALL,
)
NEW_SUMMARY_CARD = '''@Composable
private fun SaleSummaryCard(
    state: SaleHistoryState,
    accounts: List<com.akari.retailer.features.money.domain.models.MoneyAccount>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium)
        ) {
            // Line 1: totals
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = MoneyFormatter.format(state.totalSales),
                    style = AppTypography.header,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(Spacing.small))
                Text(
                    text = "· ${state.salesCount} ${stringResource(R.string.sales).lowercase()}",
                    style = AppTypography.body,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            // Line 2: per-account breakdown
            if (state.paymentBreakdown.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.small))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )
                Spacer(modifier = Modifier.height(Spacing.small))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small)
                ) {
                    // Simple wrapping row using Column of Rows
                    androidx.compose.foundation.layout.FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        state.paymentBreakdown.forEach { (accountId, amount) ->
                            val account = accounts.find { it.id == accountId }
                            val icon = account?.icon ?: "💵"
                            val label = "${icon} ${MoneyFormatter.format(amount)}"

                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = label,
                                    style = AppTypography.small,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(
                                        horizontal = 8.dp,
                                        vertical = 4.dp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Line 3: paid vs credit
            Spacer(modifier = Modifier.height(Spacing.small))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )
            Spacer(modifier = Modifier.height(Spacing.small))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.paid),
                        style = AppTypography.small,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = MoneyFormatter.format(state.totalPaid),
                        style = AppTypography.body,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.credit),
                        style = AppTypography.small,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = MoneyFormatter.format(state.totalCredit),
                        style = AppTypography.body,
                        fontWeight = FontWeight.Medium,
                        color = if (state.totalCredit > 0)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}
'''
m = OLD_SUMMARY_CARD_RE.search(src)
if m:
    src = src[:m.start()] + NEW_SUMMARY_CARD + src[m.end():]

# 4d. Update the call site to pass accounts
src = src.replace(
    "SaleSummaryCard(state = state)",
    "SaleSummaryCard(state = state, accounts = accounts)",
    1,
)

# 4e. SaleCard call — pass payments and accounts so the row shows methods
src = src.replace(
    "                        SaleCard(\n"
    "                            sale = sale,\n"
    "                            onDelete = {",
    "                        SaleCard(\n"
    "                            sale = sale,\n"
    "                            accounts = accounts,\n"
    "                            onDelete = {",
    1,
)

# 4f. Ensure imports
for imp in [
    "import androidx.compose.material3.HorizontalDivider\n",
    "import androidx.compose.foundation.layout.FlowRow\n",
    "import androidx.compose.foundation.layout.ExperimentalLayoutApi\n",
    "import androidx.compose.runtime.collectAsState\n",
    "import androidx.compose.runtime.getValue\n",
]:
    if imp not in src:
        idx = src.find("\nimport ")
        src = src[:idx + 1] + imp + src[idx + 1:]

# 4g. Opt-in FlowRow
if "@OptIn(ExperimentalLayoutApi::class)" not in src and "FlowRow" in src:
    # Add at the top of the file, after the last import
    idx = src.rfind("\nimport ")
    end = src.find("\n", idx + 1)
    src = src[:end + 1] + "\n@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)\n" + src[end + 1:]

SCREEN.write_text(src)
PATCHED.append(f"SaleHistoryScreen: compact summary + header subtitle")


# ═══════════════════════════════════════════════════════════════════════════
# 5. SaleCard.kt — optional payments line
# ═══════════════════════════════════════════════════════════════════════════
patch_file(
    PKG / "core/ui/components/SaleCard.kt",
    [
        (
            "fun SaleCard(\n"
            "    sale: Sale,\n"
            "    onDelete: () -> Unit,\n"
            "    modifier: Modifier = Modifier,\n"
            "    onClick: (() -> Unit)? = null\n"
            ") {",

            "fun SaleCard(\n"
            "    sale: Sale,\n"
            "    onDelete: () -> Unit,\n"
            "    modifier: Modifier = Modifier,\n"
            "    onClick: (() -> Unit)? = null,\n"
            "    accounts: List<com.akari.retailer.features.money.domain.models.MoneyAccount> = emptyList()\n"
            ") {",
        ),
        (
            "                Row(verticalAlignment = Alignment.CenterVertically) {\n"
            "                    Text(\n"
            "                        text = \"${sale.items.size} ${stringResource(R.string.item).lowercase()}\",\n"
            "                        style = AppTypography.body,\n"
            "                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)\n"
            "                    )\n"
            "                    Text(\n"
            "                        text = \" • \",\n"
            "                        style = AppTypography.body,\n"
            "                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)\n"
            "                    )\n"
            "                    Text(\n"
            "                        text = \"${stringResource(R.string.total)}: ${MoneyFormatter.formatTotal(sale.total)}\",\n"
            "                        style = AppTypography.body,\n"
            "                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,\n"
            "                        color = MaterialTheme.colorScheme.primary\n"
            "                    )\n"
            "                }",

            "                Row(verticalAlignment = Alignment.CenterVertically) {\n"
            "                    Text(\n"
            "                        text = \"${sale.items.size} ${stringResource(R.string.item).lowercase()}\",\n"
            "                        style = AppTypography.body,\n"
            "                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)\n"
            "                    )\n"
            "                    Text(\n"
            "                        text = \" • \",\n"
            "                        style = AppTypography.body,\n"
            "                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)\n"
            "                    )\n"
            "                    Text(\n"
            "                        text = \"${stringResource(R.string.total)}: ${MoneyFormatter.formatTotal(sale.total)}\",\n"
            "                        style = AppTypography.body,\n"
            "                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,\n"
            "                        color = MaterialTheme.colorScheme.primary\n"
            "                    )\n"
            "                }\n"
            "\n"
            "                // Payment method line (only when accounts are provided)\n"
            "                if (accounts.isNotEmpty() && sale.payments.isNotEmpty()) {\n"
            "                    Spacer(modifier = Modifier.height(2.dp))\n"
            "                    Row(\n"
            "                        verticalAlignment = Alignment.CenterVertically\n"
            "                    ) {\n"
            "                        sale.payments.forEachIndexed { index, p ->\n"
            "                            if (index > 0) {\n"
            "                                Text(\n"
            "                                    text = \"  \",\n"
            "                                    style = AppTypography.small\n"
            "                                )\n"
            "                            }\n"
            "                            val isCredit = p.isCredit\n"
            "                            val label = if (isCredit) {\n"
            "                                \"💳\"\n"
            "                            } else {\n"
            "                                val acc = accounts.find { it.id == p.accountId }\n"
            "                                acc?.icon ?: \"💵\"\n"
            "                            }\n"
            "                            Text(\n"
            "                                text = \"$label ${MoneyFormatter.format(p.amount)}\",\n"
            "                                style = AppTypography.small,\n"
            "                                color = if (isCredit)\n"
            "                                    MaterialTheme.colorScheme.error\n"
            "                                else\n"
            "                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)\n"
            "                            )\n"
            "                        }\n"
            "                    }\n"
            "                }",
        ),
    ],
    "SaleCard: optional payments line",
)


# ═══════════════════════════════════════════════════════════════════════════
# Report
# ═══════════════════════════════════════════════════════════════════════════
print()
print("═══ SALE HISTORY UPGRADE ═══")
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
    print()
    print("   Some patches couldn't apply — check the anchors above.")
    sys.exit(1)
print()
print("Done.")
