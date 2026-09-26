#!/usr/bin/env python3
"""
Adds a `notes` field to Sale — flows from entry screen → model → Firestore →
back on read → displayed in Sale Detail and History card.
"""
from __future__ import annotations
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
PKG = REPO_ROOT / "app/src/main/java/com/akari/retailer"

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
# 1. Sale.kt — add notes
# ═══════════════════════════════════════════════════════════════════════════
patch_file(
    PKG / "features/sales/domain/models/Sale.kt",
    [
        (
            "    val timestamp: Long = System.currentTimeMillis(),\n"
            "    val cashierId: String = \"\"\n"
            ")",

            "    val timestamp: Long = System.currentTimeMillis(),\n"
            "    val cashierId: String = \"\",\n"
            "    val notes: String = \"\"\n"
            ")",
        ),
    ],
    "Sale: notes field",
)


# ═══════════════════════════════════════════════════════════════════════════
# 2. SaleEntryState.kt — add notes
# ═══════════════════════════════════════════════════════════════════════════
patch_file(
    PKG / "features/sales/presentation/entry/SaleEntryState.kt",
    [
        (
            "    val customers: List<Customer> = emptyList(),\n",

            "    val customers: List<Customer> = emptyList(),\n"
            "    val notes: String = \"\",\n",
        ),
    ],
    "SaleEntryState: notes field",
)


# ═══════════════════════════════════════════════════════════════════════════
# 3. SaleEntryEvent.kt — add NotesChanged
# ═══════════════════════════════════════════════════════════════════════════
patch_file(
    PKG / "features/sales/presentation/entry/SaleEntryEvent.kt",
    [
        (
            "    data object SaveSale : SaleEntryEvent()",

            "    data class NotesChanged(val value: String) : SaleEntryEvent()\n"
            "\n"
            "    data object SaveSale : SaleEntryEvent()",
        ),
    ],
    "SaleEntryEvent: NotesChanged",
)


# ═══════════════════════════════════════════════════════════════════════════
# 4. SaleEntryViewModel.kt — handle event + pass notes into Sale
# ═══════════════════════════════════════════════════════════════════════════
patch_file(
    PKG / "features/sales/presentation/entry/SaleEntryViewModel.kt",
    [
        # handleEvent
        (
            "            SaleEntryEvent.SaveSale -> saveSale()",

            "            is SaleEntryEvent.NotesChanged ->\n"
            "                _state.value = _state.value.copy(notes = event.value)\n"
            "            SaleEntryEvent.SaveSale -> saveSale()",
        ),
        # Sale construction
        (
            "                val sale = Sale(\n"
            "                    items = saleItems,\n"
            "                    total = total,\n"
            "                    payments = allPaymentEntries,\n"
            "                    cashierId = cashierId\n"
            "                )",

            "                val sale = Sale(\n"
            "                    items = saleItems,\n"
            "                    total = total,\n"
            "                    payments = allPaymentEntries,\n"
            "                    cashierId = cashierId,\n"
            "                    notes = currentState.notes.trim()\n"
            "                )",
        ),
        # reset after save — clear notes
        (
            "                    _state.value = stateManager.resetState().copy(\n"
            "                        isSaving = false,\n"
            "                        saveSuccess = true,\n"
            "                        recentSales = currentRecentSales,\n"
            "                        accounts = currentState.accounts,\n"
            "                        customers = currentState.customers,\n"
            "                        paymentRows = listOf(PaymentRow(1L, lastAccountId, \"\"))\n"
            "                    )",

            "                    _state.value = stateManager.resetState().copy(\n"
            "                        isSaving = false,\n"
            "                        saveSuccess = true,\n"
            "                        recentSales = currentRecentSales,\n"
            "                        accounts = currentState.accounts,\n"
            "                        customers = currentState.customers,\n"
            "                        paymentRows = listOf(PaymentRow(1L, lastAccountId, \"\")),\n"
            "                        notes = \"\"\n"
            "                    )",
        ),
    ],
    "SaleEntryViewModel: notes handling",
)


# ═══════════════════════════════════════════════════════════════════════════
# 5. SaleEntryScreen.kt — add the notes field after payments section
# ═══════════════════════════════════════════════════════════════════════════
src = (PKG / "features/sales/presentation/entry/SaleEntryScreen.kt").read_text()

# Insert after the payments section, before the error block
# Look for `state.error?.let { error ->` — insert a notes field before it
anchor = "                state.error?.let { error ->"
if anchor not in src:
    FAILED.append("SaleEntryScreen: state.error anchor missing")
else:
    NOTES_BLOCK = '''                Spacer(modifier = Modifier.height(Spacing.medium))

                SectionCard(
                    title = "📝 ${stringResource(R.string.notes_optional)}"
                ) {
                    OutlinedTextField(
                        value = state.notes,
                        onValueChange = {
                            viewModel.handleEvent(SaleEntryEvent.NotesChanged(it))
                        },
                        placeholder = {
                            Text(
                                stringResource(R.string.sale_notes_hint),
                                style = AppTypography.small,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }

'''
    if NOTES_BLOCK.strip() in src:
        SKIPPED.append("SaleEntryScreen: notes field already present")
    else:
        src = src.replace(anchor, NOTES_BLOCK + anchor, 1)
        (PKG / "features/sales/presentation/entry/SaleEntryScreen.kt").write_text(src)
        PATCHED.append("SaleEntryScreen: notes field added")


# ═══════════════════════════════════════════════════════════════════════════
# 6. FirestoreSaleFinalizer.kt — write notes
# ═══════════════════════════════════════════════════════════════════════════
patch_file(
    PKG / "features/sales/data/remote/FirestoreSaleFinalizer.kt",
    [
        (
            "                    \"timestamp\" to sale.timestamp,\n"
            "                    \"cashierId\" to sale.cashierId,",

            "                    \"timestamp\" to sale.timestamp,\n"
            "                    \"cashierId\" to sale.cashierId,\n"
            "                    \"notes\" to sale.notes,",
        ),
    ],
    "FirestoreSaleFinalizer: notes",
)


# ═══════════════════════════════════════════════════════════════════════════
# 7. FirestoreService.kt — read notes back
# ═══════════════════════════════════════════════════════════════════════════
patch_file(
    REPO_ROOT / "app/src/main/java/com/akari/retailer/data/remote/FirestoreService.kt",
    [
        (
            "            Sale(\n"
            "                id = id,\n"
            "                items = items,\n"
            "                total = (data[\"total\"] as? Number)?.toInt() ?: 0,\n"
            "                payments = finalPayments,\n"
            "                timestamp = (data[\"timestamp\"] as? Number)?.toLong() ?: System.currentTimeMillis(),\n"
            "                cashierId = data[\"cashierId\"] as? String ?: \"default\"\n"
            "            )",

            "            Sale(\n"
            "                id = id,\n"
            "                items = items,\n"
            "                total = (data[\"total\"] as? Number)?.toInt() ?: 0,\n"
            "                payments = finalPayments,\n"
            "                timestamp = (data[\"timestamp\"] as? Number)?.toLong() ?: System.currentTimeMillis(),\n"
            "                cashierId = data[\"cashierId\"] as? String ?: \"default\",\n"
            "                notes = data[\"notes\"] as? String ?: \"\"\n"
            "            )",
        ),
    ],
    "FirestoreService: read notes",
)


# ═══════════════════════════════════════════════════════════════════════════
# 8. SaleDetailScreen.kt — display notes if present
# ═══════════════════════════════════════════════════════════════════════════
src_detail = (PKG / "features/sales/presentation/history/SaleDetailScreen.kt").read_text()
if "sale.notes" not in src_detail:
    # Insert a notes card after the items card, before the payments card
    anchor = "                    // ── Payments card ──"
    if anchor in src_detail:
        NOTES_DETAIL = '''                    // ── Notes card (only if present) ──
                    if (sale.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(Spacing.medium))
                        AppCard {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "📝 ${stringResource(R.string.notes)}",
                                    style = AppTypography.title,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = Spacing.small)
                                )
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                )
                                Spacer(modifier = Modifier.height(Spacing.small))
                                Text(
                                    text = sale.notes,
                                    style = AppTypography.body,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

'''
        src_detail = src_detail.replace(anchor, NOTES_DETAIL + anchor, 1)
        (PKG / "features/sales/presentation/history/SaleDetailScreen.kt").write_text(src_detail)
        PATCHED.append("SaleDetailScreen: notes card")
    else:
        SKIPPED.append("SaleDetailScreen: payments anchor missing")


# ═══════════════════════════════════════════════════════════════════════════
# 9. SaleCard.kt — show 📝 if notes exist
# ═══════════════════════════════════════════════════════════════════════════
src_card = (PKG / "core/ui/components/SaleCard.kt").read_text()
if "sale.notes" not in src_card:
    # Append 📝 to the left text if notes present
    old_left = '''            Text(
                text = "🕐 $dateString  ·  ${sale.items.size} " +
                    "${stringResource(R.string.item).lowercase()}  ·  " +
                    MoneyFormatter.formatTotal(sale.total),
                style = AppTypography.body,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )'''
    new_left = '''            Text(
                text = "🕐 $dateString  ·  ${sale.items.size} " +
                    "${stringResource(R.string.item).lowercase()}  ·  " +
                    MoneyFormatter.formatTotal(sale.total) +
                    (if (sale.notes.isNotBlank()) "  📝" else ""),
                style = AppTypography.body,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )'''
    if old_left in src_card:
        src_card = src_card.replace(old_left, new_left, 1)
        (PKG / "core/ui/components/SaleCard.kt").write_text(src_card)
        PATCHED.append("SaleCard: 📝 indicator")
    else:
        SKIPPED.append("SaleCard: notes indicator anchor missing")


print()
print("═══ SALE NOTES PATCH ═══")
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
print("Done.")
