#!/usr/bin/env python3
"""
Fix B — compute the field's actual height and offset the dropdown
by exactly that amount so it never overlaps.
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

# The old block (from Patch B / autocomplete)
OLD = '''                    var nameExpanded by rememberSaveable { mutableStateOf(false) }

                    val filteredNames = if (state.externalAccountName.isBlank()) {
                        state.knownExternalNames
                    } else {
                        val q = state.externalAccountName.lowercase().trim()
                        state.knownExternalNames.filter { it.lowercase().contains(q) }
                    }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = state.externalAccountName,
                            onValueChange = {
                                viewModel.handleEvent(
                                    ExternalTransferEvent.ExternalAccountNameChanged(it)
                                )
                                nameExpanded = true
                            },
                            placeholder = {
                                Text(stringResource(R.string.name_field), fontSize = 13.sp)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { nameExpanded = !nameExpanded }) {
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        contentDescription = "Show names",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        )

                        DropdownMenu(
                            expanded = nameExpanded && filteredNames.isNotEmpty(),
                            onDismissRequest = { nameExpanded = false },
                            offset = androidx.compose.ui.unit.DpOffset(x = 0.dp, y = 56.dp),
                            modifier = Modifier
                                .widthIn(min = 220.dp)
                                .heightIn(max = 280.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .widthIn(min = 220.dp)
                                    .heightIn(max = 280.dp)
                            ) {
                                filteredNames.take(20).forEach { name ->
                                    DropdownMenuItem(
                                        text = { Text(name, fontSize = 13.sp) },
                                        onClick = {
                                            viewModel.handleEvent(
                                                ExternalTransferEvent.ExternalAccountNameChanged(name)
                                            )
                                            nameExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }'''

NEW = '''                    var nameExpanded by rememberSaveable { mutableStateOf(false) }
                    var fieldHeightPx by remember { mutableStateOf(0) }
                    val density = androidx.compose.ui.platform.LocalDensity.current

                    val filteredNames = if (state.externalAccountName.isBlank()) {
                        state.knownExternalNames
                    } else {
                        val q = state.externalAccountName.lowercase().trim()
                        state.knownExternalNames.filter { it.lowercase().contains(q) }
                    }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = state.externalAccountName,
                            onValueChange = {
                                viewModel.handleEvent(
                                    ExternalTransferEvent.ExternalAccountNameChanged(it)
                                )
                                nameExpanded = true
                            },
                            placeholder = {
                                Text(stringResource(R.string.name_field), fontSize = 13.sp)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { coords ->
                                    fieldHeightPx = coords.size.height
                                },
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { nameExpanded = !nameExpanded }) {
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        contentDescription = "Show names",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        )

                        DropdownMenu(
                            expanded = nameExpanded && filteredNames.isNotEmpty(),
                            onDismissRequest = { nameExpanded = false },
                            offset = with(density) {
                                androidx.compose.ui.unit.DpOffset(
                                    x = 0.dp,
                                    y = fieldHeightPx.toDp()
                                )
                            },
                            modifier = Modifier
                                .widthIn(min = 220.dp)
                                .heightIn(max = 280.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .widthIn(min = 220.dp)
                                    .heightIn(max = 280.dp)
                            ) {
                                filteredNames.take(20).forEach { name ->
                                    DropdownMenuItem(
                                        text = { Text(name, fontSize = 13.sp) },
                                        onClick = {
                                            viewModel.handleEvent(
                                                ExternalTransferEvent.ExternalAccountNameChanged(name)
                                            )
                                            nameExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }'''

if NEW.strip() in src:
    print("⏭  Fix B already applied")
elif OLD in src:
    src = src.replace(OLD, NEW, 1)
    P.write_text(src)
    print("✅ Fix B applied: exact field-height offset")
else:
    print("❌ Anchor block not found — current state:")
    idx = src.find("var nameExpanded")
    if idx != -1:
        print(src[idx:idx + 1200])
    sys.exit(1)

# Ensure needed imports
for imp in [
    "import androidx.compose.ui.layout.onGloballyPositioned\n",
    "import androidx.compose.ui.unit.dp\n",
]:
    if imp not in src:
        idx = src.find("\nimport ")
        src = src[:idx + 1] + imp + src[idx + 1:]
P.write_text(src)
print("Done.")
