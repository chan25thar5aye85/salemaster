#!/usr/bin/env python3
"""
Overlays suggestions on top of the form (instead of pushing content down).

Wraps the field + everything below in a Box, positions the suggestion list
absolutely below the field with zIndex so it floats over the siblings.
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

# Find the External Account Name block (currently the polished version)
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

# The new block uses Box + overlay technique.
# We must also move the Amount field down into the same Box so the overlay
# can float over it. Since moving siblings is complex, we use a simpler trick:
# the field itself sits inside a Box; suggestions overlay the field's own
# Box (which extends downward due to a large fixed height reservation).
#
# Concretely: put the field in a Box with a *reserved* height below it (empty
# space) that suggestions can occupy. The reserved space has height 0 in the
# normal state and grows when suggestions appear — but this pushes content.
#
# Real solution: reserve 0 height always, and let suggestions draw *outside*
# the parent bounds using Modifier.wrapContentHeight(unbounded = true).
# Unfortunately Compose doesn't easily allow drawing outside bounds without
# a popup.
#
# Practical compromise: keep suggestions *inline* but make the surrounding
# layout not jump by pre-reserving space that's small (e.g. one row).
# When more suggestions appear, they overlay below.

NEW_BLOCK = '''// External Account Name — field + overlaid suggestions
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
                        if (query.isBlank()) emptyList()
                        else state.knownExternalNames
                            .filter {
                                !it.equals(query, ignoreCase = true) &&
                                    it.lowercase().contains(query.lowercase())
                            }
                            .take(4)

                    // Box: field is normal, suggestions overlay downward
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .zIndex(2f)
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (suggestions.isNotEmpty())
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                            )
                        ) {
                            BasicTextField(
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
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 14.dp),
                                decorationBox = { innerTextField ->
                                    if (state.externalAccountName.isEmpty()) {
                                        Text(
                                            text = stringResource(R.string.name_field),
                                            style = AppTypography.body,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                        }

                        // Overlay: draws on top of the fields below.
                        // Uses offset to appear right below the field.
                        if (suggestions.isNotEmpty()) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .offset(y = 54.dp)     // field height + small gap
                                    .zIndex(3f),
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 4.dp,
                                shadowElevation = 6.dp
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
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
                                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
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
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
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
    "import androidx.compose.ui.zIndex\n",
    "import androidx.compose.foundation.layout.offset\n",
    "import androidx.compose.material3.Surface\n",
    "import androidx.compose.material3.Icon\n",
    "import androidx.compose.material3.HorizontalDivider\n",
    "import androidx.compose.material.icons.Icons\n",
    "import androidx.compose.material.icons.filled.Search\n",
    "import androidx.compose.foundation.clickable\n",
    "import androidx.compose.foundation.text.BasicTextField\n",
    "import androidx.compose.ui.graphics.SolidColor\n",
]:
    if imp not in src:
        idx = src.find("\nimport ")
        src = src[:idx + 1] + imp + src[idx + 1:]

P.write_text(src)
print("✅ Overlay suggestions applied")
