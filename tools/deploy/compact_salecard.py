#!/usr/bin/env python3
"""
Reduces SaleCard to a single row:
  [🕐 time · N items · amount]  [payment icons + amounts]  [🗑]
"""
from __future__ import annotations
import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
P = REPO_ROOT / "app/src/main/java/com/akari/retailer/core/ui/components/SaleCard.kt"

if not P.exists():
    print(f"❌ Not found: {P}")
    sys.exit(1)

src = P.read_text()

# ── 1. Ensure needed imports ──────────────────────────────────────────────
for imp in [
    "import androidx.compose.foundation.layout.size\n",
    "import androidx.compose.material3.MaterialTheme\n",  # usually already there
    "import androidx.compose.ui.unit.sp\n",
]:
    if imp not in src:
        idx = src.find("\nimport ")
        src = src[:idx + 1] + imp + src[idx + 1:]

# ── 2. Replace the Column body inside the Row ─────────────────────────────
# The existing card structure is:
#     Row {
#         Column(weight=1f) {
#             Text("🕐 <date>", ...)
#             Spacer
#             Row { items · total }             ← Row 1
#             if (accounts.isNotEmpty() && payments.isNotEmpty()) {
#                 Spacer
#                 Row { payment icons + amounts }  ← Row 2
#             }
#         }
#         IconButton { delete }
#     }
#
# We want:
#     Row {
#         Row {
#             Text("🕐 <date> · N items · amount")
#             Spacer(weight=1f)
#             payments text
#         }
#         IconButton { delete }
#     }

# Find the Card block and its Column — we'll replace the whole Row inside the Card.
card_body_re = re.compile(
    r"Row\(\n"
    r"\s*modifier = Modifier\n"
    r"\s*\.fillMaxWidth\(\)\n"
    r"\s*\.padding\(Spacing\.medium\),\n"
    r"\s*verticalAlignment = Alignment\.CenterVertically\n"
    r"\s*\) \{.*?\n    \}",
    re.DOTALL,
)

NEW_CARD_BODY = '''Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side: time · items · total
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🕐 $dateString  ·  ${sale.items.size} " +
                        "${stringResource(R.string.item).lowercase()}  ·  " +
                        MoneyFormatter.formatTotal(sale.total),
                    style = AppTypography.body,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    maxLines = 1
                )
            }

            // Right side: payment icons + amounts
            if (accounts.isNotEmpty() && sale.payments.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    sale.payments.take(3).forEachIndexed { index, p ->
                        if (index > 0) {
                            Text(
                                text = "  ",
                                style = AppTypography.small
                            )
                        }
                        val icon = if (p.isCredit) {
                            "💳"
                        } else {
                            accounts.find { it.id == p.accountId }?.icon ?: "💵"
                        }
                        Text(
                            text = "$icon ${MoneyFormatter.format(p.amount)}",
                            style = AppTypography.small,
                            color = if (p.isCredit)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            maxLines = 1
                        )
                    }
                    if (sale.payments.size > 3) {
                        Text(
                            text = "  +${sale.payments.size - 3}",
                            style = AppTypography.small,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.width(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                )
            }
        }'''

m = card_body_re.search(src)
if not m:
    print("❌ Could not find the Card's Row body — anchors may have shifted")
    print("   Paste this and I'll re-anchor:")
    print(f'   sed -n "/^fun SaleCard/,/^}}/p" {P}')
    sys.exit(1)

src = src[:m.start()] + NEW_CARD_BODY + src[m.end():]

P.write_text(src)
print("✅ SaleCard: compacted to a single row")
