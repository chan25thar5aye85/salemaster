#!/usr/bin/env python3
"""
Polishes the inline auto-suggestions:
- Wraps field + suggestions in a shared bordered Surface
- Suggestion rows have a search icon + hover background
- Faster expand animation (150ms)
- Smaller, denser look
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

# Find the "External Account Name" block we added earlier
start = src.find("// External Account Name")
if start == -1:
    print("❌ Could not find '// External Account Name' comment")
    sys.exit(1)

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

NEW_BLOCK = '''// External Account Name — field + attached suggestions
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
                    val suggestions: List<String> =
                        if (query.isBlank()) {
                            emptyList()
                        } else {
                            state.knownExternalNames
                                .filter {
                                    !it.equals(query, ignoreCase = true) &&
                                        it.lowercase().contains(query.lowercase())
                                }
                                .take(4)
                        }

                    val hasSuggestions = suggestions.isNotEmpty()

                    // Bordered container that holds field + suggestions.
                    // When suggestions are hidden, it looks like a normal field.
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (hasSuggestions)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                        )
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Text field — borderless, the Surface provides the border
                            androidx.compose.foundation.text.BasicTextField(
                                value = state.externalAccountName,
                                onValueChange = {
                                    viewModel.handleEvent(
                                        ExternalTransferEvent.ExternalAccountNameChanged(it)
                                    )
                                },
                                singleLine = true,
                                textStyle = AppTypography.body.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = androidx.compose.ui.graphics.SolidColor(
                                    MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 14.dp),
                                decorationBox = { innerTextField ->
                                    if (state.externalAccountName.isEmpty()) {
                                        Text(
                                            text = stringResource(R.string.name_field),
                                            style = AppTypography.body,
                                            color = MaterialTheme.colorScheme.onSurface
                                                .copy(alpha = 0.4f)
                                        )
                                    }
                                    innerTextField()
                                }
                            )

                            // Divider + suggestions inside the same Surface
                            androidx.compose.animation.AnimatedVisibility(
                                visible = hasSuggestions,
                                enter = androidx.compose.animation.fadeIn(
                                    animationSpec = androidx.compose.animation.core.tween(120)
                                ) + androidx.compose.animation.expandVertically(
                                    animationSpec = androidx.compose.animation.core.tween(150)
                                ),
                                exit = androidx.compose.animation.fadeOut(
                                    animationSpec = androidx.compose.animation.core.tween(100)
                                ) + androidx.compose.animation.shrinkVertically(
                                    animationSpec = androidx.compose.animation.core.tween(120)
                                )
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                    )
                                    suggestions.forEachIndexed { index, name ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    viewModel.handleEvent(
                                                        ExternalTransferEvent.ExternalAccountNameChanged(name)
                                                    )
                                                }
                                                .padding(horizontal = 12.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Search,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.onSurface
                                                    .copy(alpha = 0.5f)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = name,
                                                style = AppTypography.body,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1
                                            )
                                        }
                                        if (index < suggestions.size - 1) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(start = 38.dp),
                                                color = MaterialTheme.colorScheme.onSurface
                                                    .copy(alpha = 0.05f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }'''

src = src[:start] + NEW_BLOCK + src[block_end:]

# Ensure imports
for imp in [
    "import androidx.compose.foundation.BorderStroke\n",
    "import androidx.compose.material.icons.filled.Search\n",
    "import androidx.compose.ui.graphics.SolidColor\n",
    "import androidx.compose.foundation.text.BasicTextField\n",
    "import androidx.compose.animation.core.tween\n",
    "import androidx.compose.material3.Icon\n",
    "import androidx.compose.material3.HorizontalDivider\n",
    "import androidx.compose.material3.Surface\n",
]:
    if imp not in src:
        idx = src.find("\nimport ")
        src = src[:idx + 1] + imp + src[idx + 1:]

P.write_text(src)
print("✅ Polished inline suggestions")
