#!/usr/bin/env python3
"""
Adds a "Pay All" affordance to the customer Record Payment dialog.
Tapping it fills the amount field with the full outstanding balance.
"""
from __future__ import annotations
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
P = REPO_ROOT / "app/src/main/java/com/akari/retailer/features/customer/presentation/CustomerDetailScreen.kt"

if not P.exists():
    print(f"❌ Not found: {P}")
    sys.exit(1)

src = P.read_text()

# The RecordCreditPaymentDialog currently has this structure:
#     OutlinedTextField(
#         value = amount,
#         onValueChange = onAmountChange,
#         label = { Text(stringResource(R.string.amount)) },
#         modifier = Modifier.fillMaxWidth(),
#         singleLine = true,
#         enabled = !isProcessing,
#         keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
#     )
#
# We add:
#   1. A trailingIcon with an "All" TextButton
#   2. Clear any previous error when the user taps All

OLD = '''                OutlinedTextField(
                    value = amount,
                    onValueChange = onAmountChange,
                    label = { Text(stringResource(R.string.amount)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isProcessing,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(Spacing.small))

                OutlinedTextField(
                    value = notes,
                    onValueChange = onNotesChange,
                    label = { Text(stringResource(R.string.notes_optional)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isProcessing,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(Spacing.small))

                ExposedDropdownMenuBox(
                    expanded = accountExpanded,
                    onExpandedChange = { if (!isProcessing) accountExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedAccount?.getDisplayName() ?: stringResource(R.string.select_account),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.money_account)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        enabled = !isProcessing,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded) }
                    )'''

NEW = '''                OutlinedTextField(
                    value = amount,
                    onValueChange = onAmountChange,
                    label = { Text(stringResource(R.string.amount)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isProcessing,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    trailingIcon = {
                        if (maxAmount > 0) {
                            TextButton(
                                onClick = { onAmountChange(maxAmount.toString()) },
                                enabled = !isProcessing
                            ) {
                                Text(
                                    text = stringResource(R.string.pay_all),
                                    style = AppTypography.small,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                                )
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(Spacing.small))

                OutlinedTextField(
                    value = notes,
                    onValueChange = onNotesChange,
                    label = { Text(stringResource(R.string.notes_optional)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isProcessing,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(Spacing.small))

                ExposedDropdownMenuBox(
                    expanded = accountExpanded,
                    onExpandedChange = { if (!isProcessing) accountExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedAccount?.getDisplayName() ?: stringResource(R.string.select_account),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.money_account)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        enabled = !isProcessing,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded) }
                    )'''

if NEW.strip() in src:
    print("⏭  Already patched")
    sys.exit(0)

if OLD not in src:
    print("❌ Anchor block not found. Check the current RecordCreditPaymentDialog shape.")
    idx = src.find("private fun RecordCreditPaymentDialog")
    if idx != -1:
        print(src[idx:idx + 1200])
    sys.exit(1)

src = src.replace(OLD, NEW, 1)
P.write_text(src)
print("✅ Added Pay All button to RecordCreditPaymentDialog")
