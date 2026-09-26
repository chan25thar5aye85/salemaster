#!/usr/bin/env python3
"""
Replaces the chip row (or dropdown) with inline auto-suggestions below the field.
Suggestions are a plain Column — they push content down, no popup, no overlap.
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

# Find the External Account Name block
start = src.find("// External Account Name")
if start == -1:
    print("❌ Could not find '// External Account Name' comment")
    sys.exit(1)

# Walk braces of the enclosing Column
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

NEW_BLOCK = '''// External Account Name — field + inline auto-suggestions below
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

                    val query = state.externalAccountName.trim()

                    // Live-filtered suggestions (only when typing and names exist)
                    val suggestions: List<String> =
                        if (query.isBlank()) {
                            emptyList()
                        } else {
                            state.knownExternalNames
                                .filter {
                                    !it.equals(query, ignoreCase = true) &&
                                        it.lowercase().contains(query.lowercase())
                                }
                                .take(5)
                        }

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

                    // Inline suggestions — a plain Column, not a popup.
                    // They push the rest of the form down instead of overlaying.
                    androidx.compose.animation.AnimatedVisibility(
                        visible = suggestions.isNotEmpty(),
                        enter = androidx.compose.animation.fadeIn() +
                            androidx.compose.animation.expandVertically(),
                        exit = androidx.compose.animation.fadeOut() +
                            androidx.compose.animation.shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            suggestions.forEach { name ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.handleEvent(
                                                ExternalTransferEvent.ExternalAccountNameChanged(name)
                                            )
                                        },
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        text = name,
                                        style = AppTypography.body,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(
                                            horizontal = 12.dp,
                                            vertical = 8.dp
                                        ),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }'''

src = src[:start] + NEW_BLOCK + src[block_end:]

# Ensure imports
for imp in [
    "import androidx.compose.foundation.clickable\n",
    "import androidx.compose.animation.AnimatedVisibility\n",
    "import androidx.compose.animation.fadeIn\n",
    "import androidx.compose.animation.fadeOut\n",
    "import androidx.compose.animation.expandVertically\n",
    "import androidx.compose.animation.shrinkVertically\n",
    "import androidx.compose.material3.Surface\n",
]:
    if imp not in src:
        idx = src.find("\nimport ")
        src = src[:idx + 1] + imp + src[idx + 1:]

P.write_text(src)
print("✅ Inline auto-suggestions applied")
