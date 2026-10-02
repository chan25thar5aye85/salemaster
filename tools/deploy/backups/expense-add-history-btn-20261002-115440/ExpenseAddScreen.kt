package com.akari.retailer.features.expense.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akari.retailer.R
import com.akari.retailer.RetailApplication
import com.akari.retailer.core.ui.components.AppScreen
import com.akari.retailer.core.ui.components.PaymentListComponent
import com.akari.retailer.core.ui.components.SavingStatusChip
import com.akari.retailer.core.ui.theme.AppTypography
import com.akari.retailer.core.ui.theme.Spacing
import com.akari.retailer.core.utils.MoneyFormatter
import com.akari.retailer.features.expense.domain.models.ExpenseType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseAddScreen(
    onBack: () -> Unit,
    onExpenseAdded: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as RetailApplication

    val viewModel: ExpenseAddViewModel = viewModel(
        factory = ExpenseAddViewModelFactory(
            application.container.expenseRepository,
            application.container.categoryRepository,
            application.container.moneyAccountRepository,
            application.container.supplierRepository,
            application.container.expenseFinalizer,
            application.container.paymentPreferences
        )
    )

    val state by viewModel.state.collectAsState()

    var categoryExpanded by remember { mutableStateOf(false) }
    var supplierExpanded by remember { mutableStateOf(false) }
    var showNotesSheet by remember { mutableStateOf(false) }
    var typeExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) onExpenseAdded()
    }

    // ── Sheets ──
    if (showNotesSheet) {
        NotesSheet(
            initialText = state.description,
            onApply = { text ->
                viewModel.handleEvent(ExpenseAddEvent.DescriptionChanged(text))
                showNotesSheet = false
            },
            onDismiss = { showNotesSheet = false }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AppScreen(
            title = stringResource(R.string.add_expense),
            showBackButton = true,
            onBackClick = onBack
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
            ) {
                // ── Row 1: Amount + Type ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                    verticalAlignment = Alignment.Top
                ) {
                    OutlinedTextField(
                        value = state.amount,
                        onValueChange = {
                            viewModel.handleEvent(ExpenseAddEvent.AmountChanged(it))
                        },
                        label = { Text(stringResource(R.string.amount), fontSize = 12.sp) },
                        modifier = Modifier.weight(1.2f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    ExposedDropdownMenuBox(
                        expanded = typeExpanded,
                        onExpandedChange = { typeExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = when (state.expenseType) {
                                ExpenseType.BUSINESS -> "💼 Business"
                                ExpenseType.PERSONAL -> "👤 Personal"
                                ExpenseType.MIXED -> "🔄 Mixed"
                            },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Type", fontSize = 12.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded)
                            },
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = typeExpanded,
                            onDismissRequest = { typeExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("💼 Business", fontSize = 13.sp) },
                                onClick = {
                                    viewModel.handleEvent(
                                        ExpenseAddEvent.ExpenseTypeChanged(ExpenseType.BUSINESS)
                                    )
                                    typeExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("👤 Personal", fontSize = 13.sp) },
                                onClick = {
                                    viewModel.handleEvent(
                                        ExpenseAddEvent.ExpenseTypeChanged(ExpenseType.PERSONAL)
                                    )
                                    typeExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("🔄 Mixed", fontSize = 13.sp) },
                                onClick = {
                                    viewModel.handleEvent(
                                        ExpenseAddEvent.ExpenseTypeChanged(ExpenseType.MIXED)
                                    )
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.small))

                // ── Row 2: Category + Supplier ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                    verticalAlignment = Alignment.Top
                ) {
                    // Category dropdown
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = state.selectedCategory?.name ?: "Category",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.category), fontSize = 12.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                            },
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            if (state.categories.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No categories", fontSize = 13.sp) },
                                    onClick = { categoryExpanded = false }
                                )
                            } else {
                                state.categories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text("${cat.icon} ${cat.name}", fontSize = 13.sp) },
                                        onClick = {
                                            viewModel.handleEvent(
                                                ExpenseAddEvent.CategorySelected(cat)
                                            )
                                            categoryExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Supplier dropdown (optional)
                    ExposedDropdownMenuBox(
                        expanded = supplierExpanded,
                        onExpandedChange = { supplierExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = state.selectedSupplier?.name ?: "Supplier (optional)",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Supplier", fontSize = 12.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = supplierExpanded)
                            },
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = supplierExpanded,
                            onDismissRequest = { supplierExpanded = false }
                        ) {
                            // "None" option
                            DropdownMenuItem(
                                text = { Text("— None —", fontSize = 13.sp) },
                                onClick = {
                                    viewModel.handleEvent(
                                        ExpenseAddEvent.SupplierSelected(null)
                                    )
                                    supplierExpanded = false
                                }
                            )
                            if (state.suppliers.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No suppliers yet", fontSize = 13.sp) },
                                    onClick = { supplierExpanded = false }
                                )
                            } else {
                                state.suppliers.forEach { sup ->
                                    DropdownMenuItem(
                                        text = { Text(sup.name, fontSize = 13.sp) },
                                        onClick = {
                                            viewModel.handleEvent(
                                                ExpenseAddEvent.SupplierSelected(sup)
                                            )
                                            supplierExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Business % (only when Mixed) ──
                if (state.expenseType == ExpenseType.MIXED) {
                    Spacer(modifier = Modifier.height(Spacing.small))
                    OutlinedTextField(
                        value = state.businessPercentage,
                        onValueChange = {
                            if (it.isEmpty() || it.toIntOrNull()?.let { n -> n in 0..100 } == true) {
                                viewModel.handleEvent(ExpenseAddEvent.BusinessPercentageChanged(it))
                            }
                        },
                        label = { Text(stringResource(R.string.business_percentage_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.medium))

                // ── Payments (existing component) ──
                PaymentListComponent(
                    paymentRows = state.paymentRows,
                    accounts = state.accounts,
                    totalAmount = state.getTotalAmount(),
                    showCreditOption = false,
                    onAccountSelected = { rowId, account ->
                        viewModel.handleEvent(ExpenseAddEvent.PaymentAccountChanged(rowId, account))
                    },
                    onAmountChanged = { rowId, amount ->
                        viewModel.handleEvent(ExpenseAddEvent.PaymentAmountChanged(rowId, amount))
                    },
                    onAddRow = { viewModel.handleEvent(ExpenseAddEvent.AddPaymentRow) },
                    onRemoveRow = { rowId ->
                        viewModel.handleEvent(ExpenseAddEvent.RemovePaymentRow(rowId))
                    }
                )

                Spacer(modifier = Modifier.height(Spacing.medium))

                // ── Error card ──
                state.error?.let { error ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = AppTypography.body,
                            modifier = Modifier.padding(Spacing.medium)
                        )
                    }
                    Spacer(modifier = Modifier.height(Spacing.medium))
                }

                Spacer(modifier = Modifier.height(96.dp))
            }
        }

        // ── Saving status chip ──
        if (state.isSaving) {
            SavingStatusChip(
                isSaving = state.isSaving,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 80.dp, end = 12.dp)
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(bottom = 80.dp)
            )
        }

        // ── Floating pill ──
        ExpenseAddPill(
            state = state,
            onNotesClick = { showNotesSheet = true },
            onSaveClick = {
                if (!state.isSaving) {
                    viewModel.handleEvent(ExpenseAddEvent.SaveExpense)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 80.dp, end = 12.dp)
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = 10.dp)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Floating pill
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun ExpenseAddPill(
    state: ExpenseAddState,
    onNotesClick: () -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 6.dp,
        shape = RoundedCornerShape(28.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // ── Amount ──
            Text(
                text = if (state.amount.isNotEmpty())
                    MoneyFormatter.format(state.amount.toIntOrNull() ?: 0)
                else
                    "0",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End,
                style = AppTypography.title.copy(fontSize = 17.sp),
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            // ── Notes ──
            Box {
                IconButton(
                    onClick = onNotesClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Notes",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        if (state.description.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(10.dp)
                                    .background(
                                        color = androidx.compose.ui.graphics.Color(0xFFFFC107),
                                        shape = CircleShape
                                    )
                                    .border(
                                        width = 1.5.dp,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                }
            }

            // ── Save ──
            FilledIconButton(
                onClick = onSaveClick,
                enabled = state.canSave && !state.isSaving,
                modifier = Modifier.size(40.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.onPrimary,
                    contentColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.35f),
                    disabledContentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Save",
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

private val ExpenseAddState.canSave: Boolean
    get() = amount.isNotBlank()
            && (amount.toIntOrNull() ?: 0) > 0
            && selectedCategory != null
            && isFullyPaid()

// ═══════════════════════════════════════════════════════════════════════════
// Payment sheet
// ═══════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentSheet(
    state: ExpenseAddState,
    viewModel: ExpenseAddViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.large)
                .padding(bottom = Spacing.large)
        ) {
            Text(
                text = "💰 " + stringResource(R.string.payment),
                style = AppTypography.title,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = Spacing.medium)
            )

            PaymentListComponent(
                paymentRows = state.paymentRows,
                accounts = state.accounts,
                totalAmount = state.getTotalAmount(),
                showCreditOption = false,
                onAccountSelected = { rowId, account ->
                    viewModel.handleEvent(ExpenseAddEvent.PaymentAccountChanged(rowId, account))
                },
                onAmountChanged = { rowId, amount ->
                    viewModel.handleEvent(ExpenseAddEvent.PaymentAmountChanged(rowId, amount))
                },
                onAddRow = { viewModel.handleEvent(ExpenseAddEvent.AddPaymentRow) },
                onRemoveRow = { rowId ->
                    viewModel.handleEvent(ExpenseAddEvent.RemovePaymentRow(rowId))
                }
            )

            Spacer(modifier = Modifier.height(Spacing.medium))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.ok))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Notes sheet
// ═══════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotesSheet(
    initialText: String,
    onApply: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.large)
                .padding(bottom = Spacing.large)
        ) {
            Text(
                text = "📝 " + stringResource(R.string.description),
                style = AppTypography.title,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = Spacing.medium)
            )

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 260.dp),
                maxLines = 8
            )

            Spacer(modifier = Modifier.height(Spacing.medium))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.cancel))
                }
                Button(
                    onClick = { onApply(text) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.ok))
                }
            }
        }
    }
}


// ═══════════════════════════════════════════════════════════════════════════
// Category picker sheet
// ═══════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryPickerSheet(
    categories: List<com.akari.retailer.features.expense.domain.models.ExpenseCategory>,
    selectedCategory: com.akari.retailer.features.expense.domain.models.ExpenseCategory?,
    onSelect: (com.akari.retailer.features.expense.domain.models.ExpenseCategory) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.large)
                .padding(bottom = Spacing.large)
        ) {
            Text(
                text = "📌 " + stringResource(R.string.category),
                style = AppTypography.title,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = Spacing.medium)
            )

            if (categories.isEmpty()) {
                Text(
                    text = "No categories yet. Add one from the Expense list.",
                    style = AppTypography.body,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            } else {
                categories.forEach { cat ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(cat) }
                            .padding(vertical = Spacing.small),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${cat.icon} ${cat.name}",
                            style = AppTypography.body,
                            modifier = Modifier.weight(1f)
                        )
                        if (cat.id == selectedCategory?.id) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    }
}
