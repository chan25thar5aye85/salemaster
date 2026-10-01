@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.akari.retailer.features.money.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akari.retailer.R
import com.akari.retailer.RetailApplication
import com.akari.retailer.core.ui.components.AppCard
import com.akari.retailer.core.ui.components.AppScreen
import com.akari.retailer.core.ui.theme.AppTypography
import com.akari.retailer.core.ui.theme.Spacing
import com.akari.retailer.core.utils.MoneyFormatter
import com.akari.retailer.features.money.domain.models.FeeType
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExternalTransferScreen(
    onBack: () -> Unit,
    onTransferSuccess: () -> Unit = {},
    onViewHistory: () -> Unit = {}
) {
    val context = LocalContext.current
    val application = context.applicationContext as RetailApplication

    val viewModel: ExternalTransferViewModel = viewModel(
        factory = ExternalTransferViewModelFactory(
            application.container.moneyAccountRepository,
            application.container.moneyTransactionRepository,
            application.container.externalTransferUseCase,
            application.container.paymentPreferences
        )
    )

    val state by viewModel.state.collectAsState()

    var showAccountPicker by remember { mutableStateOf(false) }
    var showFeeSheet by remember { mutableStateOf(false) }
    var showNotesSheet by remember { mutableStateOf(false) }

    // Show success for 3s, then reset.
    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) {
            kotlinx.coroutines.delay(3000)
            viewModel.handleEvent(ExternalTransferEvent.ResetSuccess)
        }
    }

    // ── Account picker sheet ──
    if (showAccountPicker) {
        AccountPickerSheet(
            accounts = state.accounts,
            selectedAccount = state.selectedAccount,
            onSelect = { account ->
                viewModel.handleEvent(ExternalTransferEvent.AccountSelected(account))
                showAccountPicker = false
            },
            onDismiss = { showAccountPicker = false }
        )
    }

    // ── Fee sheet ──
    if (showFeeSheet) {
        FeeSheet(
            feeType = state.feeType,
            fee = state.fee,
            onFeeTypeChange = {
                viewModel.handleEvent(ExternalTransferEvent.FeeTypeChanged(it))
            },
            onFeeChange = {
                viewModel.handleEvent(ExternalTransferEvent.FeeChanged(it))
            },
            onDismiss = { showFeeSheet = false }
        )
    }

    // ── Notes sheet ──
    if (showNotesSheet) {
        NotesSheet(
            initialText = state.description,
            onApply = { text ->
                viewModel.handleEvent(ExternalTransferEvent.DescriptionChanged(text))
                showNotesSheet = false
            },
            onDismiss = { showNotesSheet = false }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AppScreen(
            title = "🌐 " + stringResource(R.string.external_transfer),
            showBackButton = true,
            onBackClick = onBack
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // ── Amount (main input, stays in content) ──
                OutlinedTextField(
                    value = state.amount,
                    onValueChange = { viewModel.handleEvent(ExternalTransferEvent.AmountChanged(it)) },
                    label = { Text(stringResource(R.string.amount)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(Spacing.medium))

                // ── External Account Name (autocomplete) ──
                ExternalAccountNameField(
                    value = state.externalAccountName,
                    onValueChange = {
                        viewModel.handleEvent(ExternalTransferEvent.ExternalAccountNameChanged(it))
                    },
                    knownNames = state.knownExternalNames,
                    isOutgoing = state.isOutgoing(),
                    labelOutgoing = stringResource(R.string.to_account),
                    labelIncoming = stringResource(R.string.from_account),
                    optionalHint = stringResource(R.string.optional)
                )

                Spacer(modifier = Modifier.height(Spacing.medium))

                // ── Preview card ──
                if (state.selectedAccount != null && state.amount.isNotEmpty()) {
                    AppCard {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = stringResource(R.string.transfer_summary),
                                style = AppTypography.title,
                                modifier = Modifier.padding(bottom = Spacing.small)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = state.selectedAccount!!.name,
                                    style = AppTypography.body
                                )
                                Text(
                                    text = "${state.selectedAccount!!.currentBalance} → ${state.getNewBalance()}",
                                    style = AppTypography.body,
                                    color = if (state.getAccountChange() >= 0)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(Spacing.small))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (state.isOutgoing())
                                        stringResource(R.string.to_account)
                                    else
                                        stringResource(R.string.from_account),
                                    style = AppTypography.body
                                )
                                Text(
                                    text = state.externalAccountName.ifEmpty { "External" },
                                    style = AppTypography.body,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }

                            if (state.feeType != FeeType.NONE && state.getFeeInt() > 0) {
                                Spacer(modifier = Modifier.height(Spacing.small))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(Spacing.small))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (state.feeType == FeeType.FEE_PAID)
                                            stringResource(R.string.fee_you_pay)
                                        else
                                            stringResource(R.string.fee_you_earn),
                                        style = AppTypography.body
                                    )
                                    Text(
                                        text = MoneyFormatter.format(state.getFeeInt()),
                                        style = AppTypography.body,
                                        color = if (state.feeType == FeeType.FEE_PAID)
                                            MaterialTheme.colorScheme.error
                                        else
                                            MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(Spacing.medium))
                }

                // ── Success card ──
                if (state.saveSuccess) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.medium),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "✅ Transfer successful!",
                                color = MaterialTheme.colorScheme.primary,
                                style = AppTypography.body,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(Spacing.medium))
                }

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
                            style = AppTypography.body,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(Spacing.medium)
                        )
                    }
                    Spacer(modifier = Modifier.height(Spacing.medium))
                }

                // ── View History shortcut ──
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = Spacing.medium)
                        .clickable { onViewHistory() },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.medium),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "📋 " + stringResource(R.string.external_transfer_history),
                                style = AppTypography.title
                            )
                            Text(
                                text = stringResource(R.string.view_all),
                                style = AppTypography.small,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        OutlinedButton(onClick = onViewHistory) {
                            Text(stringResource(R.string.view_all))
                        }
                    }
                }

                // Extra space so the floating pill doesn't cover the last card
                Spacer(modifier = Modifier.height(96.dp))
            }
        }

        // ── Floating action pill above the keyboard ──
        ExternalTransferPill(
            state = state,
            onDirectionToggle = {
                val next = if (state.isOutgoing())
                    ExternalTransferDirection.INCOMING
                else
                    ExternalTransferDirection.OUTGOING
                viewModel.handleEvent(ExternalTransferEvent.DirectionChanged(next))
            },
            onAccountClick = { showAccountPicker = true },
            onFeeClick = { showFeeSheet = true },
            onNotesClick = { showNotesSheet = true },
            onConfirmClick = {
                if (!state.isSaving) {
                    viewModel.handleEvent(ExternalTransferEvent.SaveTransfer)
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
private fun ExternalTransferPill(
    state: ExternalTransferState,
    onDirectionToggle: () -> Unit,
    onAccountClick: () -> Unit,
    onFeeClick: () -> Unit,
    onNotesClick: () -> Unit,
    onConfirmClick: () -> Unit,
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
            // ── Direction chip ──
            val directionLabel = if (state.isOutgoing()) "📤" else "📥"
            OutlinedButton(
                onClick = onDirectionToggle,
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
                    text = directionLabel,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
            }

            // ── Account chip ──
            val accountLabel = state.selectedAccount?.let {
                "${it.icon} ${it.name}"
            } ?: "💰 Account"

            OutlinedButton(
                onClick = onAccountClick,
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
                    text = accountLabel,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    maxLines = 1
                )
            }

            // ── Amount (weighted to fill) ──
            Text(
                text = if (state.amount.isNotEmpty())
                    MoneyFormatter.format(state.getAmountInt())
                else
                    "0",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End,
                style = AppTypography.title.copy(fontSize = 17.sp),
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            // ── Fee chip (only if fee set) ──
            if (state.feeType != FeeType.NONE) {
                val feeIcon = if (state.feeType == FeeType.FEE_PAID) "💸" else "💵"
                Box {
                    IconButton(
                        onClick = onFeeClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box {
                            Text(
                                text = feeIcon,
                                fontSize = 20.sp
                            )
                            if (state.getFeeInt() > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(10.dp)
                                        .background(
                                            color = androidx.compose.ui.graphics.Color(0xFFFFC107),
                                            shape = CircleShape
                                        )
                                )
                            }
                        }
                    }
                }
            } else {
                // Show fee placeholder so the user knows they can tap to add one
                IconButton(
                    onClick = onFeeClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Text(
                        text = "💸",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                    )
                }
            }

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

            // ── Confirm icon ──
            FilledIconButton(
                onClick = onConfirmClick,
                enabled = state.canConfirm && !state.isSaving,
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
                        contentDescription = "Confirm",
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Helper: canConfirm
// ═══════════════════════════════════════════════════════════════════════════

private val ExternalTransferState.canConfirm: Boolean
    get() = selectedAccount != null && getAmountInt() > 0

// ═══════════════════════════════════════════════════════════════════════════
// External Account Name field with autocomplete
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun ExternalAccountNameField(
    value: String,
    onValueChange: (String) -> Unit,
    knownNames: List<String>,
    isOutgoing: Boolean,
    labelOutgoing: String,
    labelIncoming: String,
    optionalHint: String
) {
    val query = value.trim()
    val suggestions: List<String> = if (query.isBlank()) emptyList()
        else knownNames
            .filter {
                !it.equals(query, ignoreCase = true) &&
                    it.lowercase().contains(query.lowercase())
            }
            .take(4)

    var fieldHeightPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (isOutgoing) labelOutgoing else labelIncoming,
                style = AppTypography.label,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "($optionalHint)",
                style = AppTypography.small,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                fontSize = 10.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
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
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = AppTypography.body.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    decorationBox = { innerTextField ->
                        if (value.isEmpty()) {
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

            if (suggestions.isNotEmpty() && fieldHeightPx > 0) {
                Popup(
                    alignment = Alignment.TopStart,
                    offset = IntOffset(
                        x = 0,
                        y = fieldHeightPx + with(density) { 4.dp.roundToPx() }
                    ),
                    onDismissRequest = { },
                    properties = PopupProperties(
                        focusable = false,
                        dismissOnBackPress = false,
                        dismissOnClickOutside = true
                    )
                ) {
                    Surface(
                        modifier = Modifier
                            .width(with(density) { 200.dp })
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
                                        .clickable { onValueChange(name) }
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
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Account picker sheet
// ═══════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountPickerSheet(
    accounts: List<com.akari.retailer.features.money.domain.models.MoneyAccount>,
    selectedAccount: com.akari.retailer.features.money.domain.models.MoneyAccount?,
    onSelect: (com.akari.retailer.features.money.domain.models.MoneyAccount) -> Unit,
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
                text = "💰 " + stringResource(R.string.select_account),
                style = AppTypography.title,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = Spacing.medium)
            )

            accounts.forEach { account ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(account) }
                        .padding(vertical = Spacing.small),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = account.getDisplayName(),
                        style = AppTypography.body,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = MoneyFormatter.format(account.currentBalance),
                        style = AppTypography.body,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    if (account.id == selectedAccount?.id) {
                        Spacer(modifier = Modifier.width(Spacing.small))
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
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
// Fee sheet
// ═══════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeeSheet(
    feeType: FeeType,
    fee: String,
    onFeeTypeChange: (FeeType) -> Unit,
    onFeeChange: (String) -> Unit,
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
                text = "💸 " + stringResource(R.string.fee_type),
                style = AppTypography.title,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = Spacing.medium)
            )

            FeeType.entries.forEach { type ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onFeeTypeChange(type) }
                        .padding(vertical = Spacing.small),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = feeType == type,
                        onClick = { onFeeTypeChange(type) }
                    )
                    Spacer(modifier = Modifier.width(Spacing.small))
                    Text(
                        text = when (type) {
                            FeeType.NONE -> stringResource(R.string.fee_none)
                            FeeType.FEE_PAID -> stringResource(R.string.fee_paid)
                            FeeType.FEE_EARNED -> stringResource(R.string.fee_earned)
                        },
                        style = AppTypography.body
                    )
                }
            }

            if (feeType != FeeType.NONE) {
                Spacer(modifier = Modifier.height(Spacing.medium))
                OutlinedTextField(
                    value = fee,
                    onValueChange = { onFeeChange(it.filter { c -> c.isDigit() }) },
                    label = {
                        Text(
                            if (feeType == FeeType.FEE_PAID)
                                stringResource(R.string.fee_you_pay)
                            else
                                stringResource(R.string.fee_you_earn)
                        )
                    },
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
                text = "📝 " + stringResource(R.string.description_optional),
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
