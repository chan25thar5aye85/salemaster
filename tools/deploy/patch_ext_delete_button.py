#!/usr/bin/env python3
"""
Adds a visible trash icon button to each TransferRow.
Keeps long-press as an additional gesture.
"""
from __future__ import annotations
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
P = REPO_ROOT / "app/src/main/java/com/akari/retailer/features/money/presentation/ExternalTransferHistoryScreen.kt"

if not P.exists():
    print(f"❌ Not found: {P}")
    sys.exit(1)

src = P.read_text()

# ── 1. TransferRow signature: change onLongPress to onDelete ──
# We'll pass a single callback used for both gestures.
old_sig = '''private fun TransferRow(
    txn: MoneyTransaction,
    onLongPress: (() -> Unit)? = null
) {'''

new_sig = '''private fun TransferRow(
    txn: MoneyTransaction,
    onDelete: (() -> Unit)? = null
) {'''

if new_sig in src:
    print("⏭  Signature already patched")
elif old_sig in src:
    src = src.replace(old_sig, new_sig, 1)
    print("✅ Signature updated")
else:
    print("❌ TransferRow signature anchor missing")
    sys.exit(1)

# ── 2. combinedClickable → onLongPress renamed to onDelete ──
old_click = '''                if (onLongPress != null)
                    Modifier.combinedClickable(
                        onClick = { },
                        onLongClick = { onLongPress() }
                    )
                else Modifier'''

new_click = '''                if (onDelete != null)
                    Modifier.combinedClickable(
                        onClick = { },
                        onLongClick = { onDelete() }
                    )
                else Modifier'''

if new_click.strip() in src:
    print("⏭  Click modifier already patched")
elif old_click in src:
    src = src.replace(old_click, new_click, 1)
    print("✅ Click modifier updated")
else:
    print("⚠  Click modifier anchor missing — check manually")

# ── 3. Add the trash IconButton to the row layout ──
# Find the row's `Row(...)` and append the delete button before the closing.
# The row currently ends with the "amount + fee" Column:
old_end = '''            // Right: amount + optional fee
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

new_end = '''            // Right: amount + optional fee
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

            // Visible delete button — matches the pattern used by SaleCard,
            // ExpenseCard, CustomerCard, etc.
            if (onDelete != null) {
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = { onDelete() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}'''

if new_end.strip() in src:
    print("⏭  Delete button already added")
elif old_end in src:
    src = src.replace(old_end, new_end, 1)
    print("✅ Delete button added to TransferRow")
else:
    print("❌ Row end anchor missing — the TransferRow body may differ")
    sys.exit(1)

# ── 4. Call site: onLongPress → onDelete ──
old_call = '''                    TransferRow(
                        txn = txn,
                        onLongPress = {
                            viewModel.handleEvent(
                                ExternalTransferHistoryEvent.RequestDelete(txn.id)
                            )
                        }
                    )'''

new_call = '''                    TransferRow(
                        txn = txn,
                        onDelete = {
                            viewModel.handleEvent(
                                ExternalTransferHistoryEvent.RequestDelete(txn.id)
                            )
                        }
                    )'''

if new_call in src:
    print("⏭  Call site already patched")
elif old_call in src:
    src = src.replace(old_call, new_call, 1)
    print("✅ Call site updated")
else:
    print("⚠  Call site anchor missing — check manually")

# ── 5. Ensure Icons.Default.Delete and IconButton imports ──
for imp in [
    "import androidx.compose.material.icons.Icons\n",
    "import androidx.compose.material.icons.filled.Delete\n",
    "import androidx.compose.material3.Icon\n",
    "import androidx.compose.material3.IconButton\n",
]:
    if imp not in src:
        idx = src.find("\nimport ")
        src = src[:idx + 1] + imp + src[idx + 1:]

P.write_text(src)
print("Done.")
