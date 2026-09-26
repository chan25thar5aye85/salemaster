#!/usr/bin/env python3
"""
Simplifies RecentSalesCard. Uses brace matching from the line where
`private fun RecentSalesCard(` appears — no decorator assumption.
"""
from __future__ import annotations
import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
P = REPO_ROOT / "app/src/main/java/com/akari/retailer/features/sales/presentation/entry/SaleEntryScreen.kt"

if not P.exists():
    print(f"❌ Not found: {P}")
    sys.exit(1)

src = P.read_text()

# 1) Find `private fun RecentSalesCard(` (or `fun RecentSalesCard(`)
name_re = re.compile(r"(?:private\s+)?fun\s+RecentSalesCard\s*\(")
m = name_re.search(src)
if not m:
    print("❌ `fun RecentSalesCard(` not found")
    sys.exit(1)

# 2) Find the parameter list's closing `)`
paren_start = src.index("(", m.start())
depth = 0
i = paren_start
while i < len(src):
    if src[i] == "(":
        depth += 1
    elif src[i] == ")":
        depth -= 1
        if depth == 0:
            break
    i += 1

# 3) Find the body's opening `{`
brace_start = src.index("{", i)

# 4) Match its closing `}`
depth = 0
j = brace_start
while j < len(src):
    c = src[j]
    if c == "{":
        depth += 1
    elif c == "}":
        depth -= 1
        if depth == 0:
            break
    j += 1
body_end = j + 1

# 5) Extend backward to include the preceding `@Composable` (or any `@...`)
line_start = src.rfind("\n", 0, m.start()) + 1
scan = line_start
while True:
    prev_nl = src.rfind("\n", 0, scan - 1)
    if prev_nl == -1:
        break
    prev_line = src[prev_nl + 1 : scan - 1].strip()
    if prev_line.startswith("@"):
        scan = prev_nl + 1
        continue
    break
replace_start = scan

NEW_CARD = '''@Composable
private fun RecentSalesCard(
    sales: List<com.akari.retailer.features.sales.domain.models.Sale>,
    onViewAllClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // ── Header ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.medium),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📋 ${stringResource(R.string.recent_sales)}",
                        style = AppTypography.title,
                        fontWeight = FontWeight.Bold
                    )
                    if (sales.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(Spacing.small))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "${sales.size}",
                                style = AppTypography.small,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                TextButton(onClick = onViewAllClick) {
                    Text(stringResource(R.string.view_all), fontSize = 13.sp)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            // ── List — always visible, up to 5 ──
            if (sales.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.large),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_sales_today),
                        style = AppTypography.body,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            } else {
                val visible = sales.take(5)
                visible.forEachIndexed { index, sale ->
                    CompactSaleRow(sale = sale)
                    if (index < visible.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = Spacing.medium),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                        )
                    }
                }
            }
        }
    }
}
'''

src = src[:replace_start] + NEW_CARD + src[body_end:]
P.write_text(src)
print(f"✅ Patched {P.name}")
print(f"   Replaced bytes {replace_start}–{body_end}")
