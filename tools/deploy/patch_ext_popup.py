#!/usr/bin/env python3
"""
Replaces the overlay hack with a real Popup.

Popup renders in its own window — cannot push content, captures touches,
and the offset is set in pixels by us (no anchor math).
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

NEW_BLOCK = '''// External Account Name — field + Popup-suggestions
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

                    // Track the field's height in px for the popup offset
                    var fieldHeightPx by remember { mutableStateOf(0) }
                    val density = LocalDensity.current

                    Box(modifier = Modifier.fillMaxWidth()) {
                        // The actual field
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { coords ->
                                    fieldHeightPx = coords.size.height
                                },
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

                        // Real Popup — floats in its own window.
                        // Cannot push content, captures touches, offset set by us.
                        if (suggestions.isNotEmpty() && fieldHeightPx > 0) {
                            androidx.compose.ui.window.Popup(
                                alignment = Alignment.TopStart,
                                offset = androidx.compose.ui.unit.IntOffset(
                                    x = 0,
                                    y = fieldHeightPx + with(density) { 4.dp.roundToPx() }
                                ),
                                onDismissRequest = {
                                    viewModel.handleEvent(
                                        ExternalTransferEvent.ExternalAccountNameChanged(
                                            state.externalAccountName
                                        )
                                    )
                                },
                                properties = androidx.compose.ui.window.PopupProperties(
                                    focusable = false,
                                    dismissOnBackPress = false,
                                    dismissOnClickOutside = true
                                )
                            ) {
                                // The suggestion list — matches the field's width
                                Surface(
                                    modifier = Modifier
                                        .width(with(density) { 200.dp })   // fixed width so it doesn't stretch
                                        .shadow(6.dp, MaterialTheme.shapes.small),
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 4.dp
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
                    }
                }'''

src = src[:start] + NEW_BLOCK + src[block_end:]

# Ensure imports
for imp in [
    "import androidx.compose.ui.platform.LocalDensity\n",
    "import androidx.compose.ui.layout.onGloballyPositioned\n",
    "import androidx.compose.ui.unit.IntOffset\n",
    "import androidx.compose.ui.window.Popup\n",
    "import androidx.compose.ui.window.PopupProperties\n",
    "import androidx.compose.ui.draw.shadow\n",
    "import kotlin.math.roundToInt\n",
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
print("✅ Popup-based suggestions applied")
