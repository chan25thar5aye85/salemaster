#!/usr/bin/env python3
"""
Replaces the autocomplete dropdown with a chip row below the field.

- Text field stays fully editable (no dropdown, no popup).
- Chips below show recent external account names.
- Tapping a chip fills the field.
- Chip row hidden when no known names exist.
"""
from __future__ import annotations
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
P = REPO_ROOT / "app/src/main/java/com/akari/retailer/features/money/presentation/ExternalTransferScreen.kt"

if not P.exists():
    print(f"❌ Not found: {P}")
    sys.exit(1)

src = P.read_text()

# Locate the whole "External Account Name" block
start = src.find("// External Account Name")
if start == -1:
    start = src.find("// External Account Name — autocomplete")
if start == -1:
    print("❌ Could not find External Account Name block")
    sys.exit(1)

# Walk to the end of the enclosing Column
brace_start = src.index("{", start)
depth = 0
i = brace_start
while i < len(src):
    c = src[i]
    if c == "{":
        depth += 1
    elif c == "}":
        depth -= 1
        if depth == 0:
            break
    i += 1
block_end = i + 1

NEW_BLOCK = '''// External Account Name — plain field + chip suggestions
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (state.isOutgoing())
                            stringResource(R.string.to_account)
                        else
                            stringResource(R.string.from_account),
                        style = AppTypography.label,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    // Plain text field (fully editable, no popup)
                    OutlinedTextField(
                        value = state.externalAccountName,
                        onValueChange = {
                            viewModel.handleEvent(
                                ExternalTransferEvent.ExternalAccountNameChanged(it)
                            )
                        },
                        placeholder = {
                            Text(stringResource(R.string.name_field), fontSize = 13.sp)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Chip suggestions below the field
                    if (state.knownExternalNames.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Recent",
                            style = AppTypography.small,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        // Flowing chips — use androidx FlowRow
                        androidx.compose.foundation.layout.FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            state.knownExternalNames.take(8).forEach { name ->
                                androidx.compose.material3.AssistChip(
                                    onClick = {
                                        viewModel.handleEvent(
                                            ExternalTransferEvent.ExternalAccountNameChanged(name)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = name,
                                            style = AppTypography.small,
                                            fontSize = 12.sp
                                        )
                                    }
                                )
                            }
                        }
                    }
                }'''

src = src[:start] + NEW_BLOCK + src[block_end:]

# Add the FlowRow opt-in at the top of the file (before package)
if "@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)" not in src:
    src = "@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)\n\n" + src

P.write_text(src)
print("✅ Replaced autocomplete with chip suggestions")
