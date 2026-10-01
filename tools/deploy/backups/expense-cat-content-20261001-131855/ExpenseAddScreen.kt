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
import com.akari.retailer.features.expense.domain.models.ExpenseCategory
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
            application.container.expenseFinalizer,
            application.container.paymentPreferences
        )
    )

    val state by viewModel.state.collectAsState()

    var showCategorySheet by remember { mutableStateOf(false) }
    var showTypeSheet by remember { mutableStateOf(false) }
    var showNotesSheet by remember { mutableStateOf(false) }

    // Navigate away on success
    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) {
            onExpenseAdded()
        }
    }

    // ── Sheets ──
    if (showCategorySheet) {
        CategoryPickerSheet(
            categories = state.categories,
            selectedCategory = state.selectedCategory,
            onSelect = { cat ->
                viewModel.handleEvent(ExpenseAddEvent.CategorySelected(cat))
                showCategorySheet = false
            },
            onDismiss = { showCategorySheet = false }
        )
    }

    if (showTypeSheet) {
        TypeSheet(
            type = state.expenseType,
            businessPercentage = state.businessPercentage,
            onTypeChange = {
                viewModel.handleEvent(ExpenseAddEvent.ExpenseTypeChanged(it))
            },
            onPercentageChange = {
                viewModel.handleEvent(ExpenseAddEvent.BusinessPercentageChanged(it))
            },
            onDismiss = { showTypeSheet = false }
        )
    }

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
                // ── Title ──
                OutlinedTextField(
                    value = state.title,
                    onValueChange = {
                        viewModel.handleEvent(ExpenseAddEvent.TitleChanged(it))
                    },
                    label = { Text(stringResource(R.string.title)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(Spacing.medium))

                // ── Amount ──
                OutlinedTextField(
                    value = state.amount,
                    onValueChange = {
                        viewModel.handleEvent(ExpenseAddEvent.AmountChanged(it))
                    },
                    label = { Text(stringResource(R.string.amount)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

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

                // Extra space so the floating pill doesn't cover the last card
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
            onCategoryClick = { showCategorySheet = true },
            onTypeClick = { showTypeSheet = true },
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
    onCategoryClick: () -> Unit,
    onTypeClick: () -> Unit,
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
            // ── Category chip ──
            val categoryLabel = state.selectedCategory?.let {
                "${it.icon} ${it.name}"
            } ?: "📌 Category"

            OutlinedButton(
                onClick = onCategoryClick,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = categoryLabel,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    maxLines = 1
                )
            }

            // ── Type chip ──
            val typeLabel = when (state.expenseType) {
                ExpenseType.BUSINESS -> "💼"
                ExpenseType.PERSONAL -> "👤"
                ExpenseType.MIXED -> "🔄"
            }

            OutlinedButton(
                onClick = onTypeClick,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = typeLabel,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
            }

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

            // ── Notes icon ──
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

// Computed: whether the pill's save button is enabled
private val ExpenseAddState.canSave: Boolean
    get() = title.isNotBlank()
            && amount.isNotBlank()
            && (amount.toIntOrNull() ?: 0) > 0
            && selectedCategory != null
            && isFullyPaid()

// ═══════════════════════════════════════════════════════════════════════════
// Category picker sheet
// ═══════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryPickerSheet(
    categories: List<ExpenseCategory>,
    selectedCategory: ExpenseCategory?,
    onSelect: (ExpenseCategory) -> Unit,
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

// ═══════════════════════════════════════════════════════════════════════════
// Type sheet (Business / Personal / Mixed + % field)
// ═══════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TypeSheet(
    type: ExpenseType,
    businessPercentage: String,
    onTypeChange: (ExpenseType) -> Unit,
    onPercentageChange: (String) -> Unit,
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
                text = "🏷 " + stringResource(R.string.expense_details),
                style = AppTypography.title,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = Spacing.medium)
            )

            listOf(
                ExpenseType.BUSINESS to "💼 " + stringResource(R.string.business_type),
                ExpenseType.PERSONAL to "👤 " + stringResource(R.string.personal_type),
                ExpenseType.MIXED to "🔄 " + stringResource(R.string.mixed_type)
            ).forEach { (optionType, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTypeChange(optionType) }
                        .padding(vertical = Spacing.small),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = type == optionType,
                        onClick = { onTypeChange(optionType) }
                    )
                    Spacer(modifier = Modifier.width(Spacing.small))
                    Text(text = label, style = AppTypography.body)
                }
            }

            if (type == ExpenseType.MIXED) {
                Spacer(modifier = Modifier.height(Spacing.medium))
                OutlinedTextField(
                    value = businessPercentage,
                    onValueChange = {
                        if (it.isEmpty() || it.toIntOrNull()?.let { n -> n in 0..100 } == true) {
                            onPercentageChange(it)
                        }
                    },
                    label = { Text(stringResource(R.string.business_percentage_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

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
