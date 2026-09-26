#!/usr/bin/env python3
# ─────────────────────────────────────────────────────────────────────────────
# patch_existing.py
#
# Applies in-place patches for:
#   1. AppContainer — register PurchaseFinalizer
#   2. CalculateBusinessProfitUseCase — add (from, to) range params
#   3. ProfitLossViewModel — TimeFilter + job cancellation
#   4. ProfitLossScreen — filter button + active-filter chip + remove bad retry
#   5. SaleEntryViewModel — fix pendingOverpayment race (3 guards)
#   6. SaleEntryScreen — refuse taps while saving
#
# Idempotent: each patch checks for its anchor before applying.
# If anchor already transformed, it's skipped with a "already patched" note.
# ─────────────────────────────────────────────────────────────────────────────
from __future__ import annotations
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
PKG = REPO_ROOT / "app" / "src" / "main" / "java" / "com" / "akari" / "retailer"

PATCHED: list[str] = []
SKIPPED: list[str] = []
FAILED:  list[str] = []


def patch(path: Path, replacements: list[tuple[str, str]], label: str) -> None:
    """
    Apply a list of (old, new) string replacements to `path`.
    - If any 'old' contains a sentinel marker ('@@ALREADY_PATCHED@@'),
      we check for that first and skip.
    - If 'old' isn't found at all, we record as FAILED (anchor missing).
    """
    if not path.exists():
        FAILED.append(f"{label}: file not found → {path}")
        return

    text = path.read_text()
    original = text

    for old, new in replacements:
        if old in text:
            text = text.replace(old, new, 1)
        elif new in text:
            # Already patched — no-op
            continue
        else:
            FAILED.append(f"{label}: anchor not found in {path.name}")
            return

    if text == original:
        SKIPPED.append(f"{label}: already patched ({path.name})")
        return

    path.write_text(text)
    PATCHED.append(f"{label}: {path.relative_to(REPO_ROOT)}")


# ═══════════════════════════════════════════════════════════════════════════
# 1. AppContainer — register PurchaseFinalizer
# ═══════════════════════════════════════════════════════════════════════════
patch(
    PKG / "di" / "AppContainer.kt",
    [
        (
            # anchor — first import block line we can find
            "import com.akari.retailer.features.inventory.data.repository.PurchaseRepository\n",
            "import com.akari.retailer.features.inventory.data.repository.PurchaseRepository\n"
            "import com.akari.retailer.features.inventory.data.repository.PurchaseFinalizer\n"
            "import com.akari.retailer.features.inventory.data.remote.FirestorePurchaseFinalizer\n",
        ),
        (
            # anchor — the existing purchaseRepository declaration
            "    // Purchases\n"
            "    val purchaseRepository: PurchaseRepository by lazy {\n"
            "        FirestorePurchaseRepository()\n"
            "    }\n",
            "    // Purchases\n"
            "    val purchaseRepository: PurchaseRepository by lazy {\n"
            "        FirestorePurchaseRepository()\n"
            "    }\n"
            "    val purchaseFinalizer: PurchaseFinalizer by lazy {\n"
            "        FirestorePurchaseFinalizer()\n"
            "    }\n",
        ),
    ],
    "AppContainer: register PurchaseFinalizer",
)


# ═══════════════════════════════════════════════════════════════════════════
# 2. CalculateBusinessProfitUseCase — add (from, to) range params
# ═══════════════════════════════════════════════════════════════════════════
patch(
    PKG / "features" / "expense" / "domain" / "usecases" / "CalculateBusinessProfitUseCase.kt",
    [
        (
            "    suspend fun invoke(): Result<ProfitData> {\n"
            "        return try {\n"
            "            // Get all data\n"
            "            val sales = saleRepository.getSales().first()\n"
            "            val expenses = expenseRepository.getExpenses().first()\n"
            "            val incomeEntries = incomeEntryRepository.getIncomeEntries().first()\n"
            "            val moneyTransactions = moneyTransactionRepository.getTransactions().first()\n",

            "    /**\n"
            "     * @param from inclusive lower bound (epoch millis); null = no lower bound\n"
            "     * @param to   inclusive upper bound (epoch millis); null = no upper bound\n"
            "     */\n"
            "    suspend fun invoke(\n"
            "        from: Long? = null,\n"
            "        to: Long? = null\n"
            "    ): Result<ProfitData> {\n"
            "        return try {\n"
            "            // Get all data once, then filter in-memory by range.\n"
            "            val sales = saleRepository.getSales().first()\n"
            "                .filter { it.timestamp.inRange(from, to) }\n"
            "            val expenses = expenseRepository.getExpenses().first()\n"
            "                .filter { it.date.inRange(from, to) }\n"
            "            val incomeEntries = incomeEntryRepository.getIncomeEntries().first()\n"
            "                .filter { it.date.inRange(from, to) }\n"
            "            val moneyTransactions = moneyTransactionRepository.getTransactions().first()\n"
            "                .filter { it.date.inRange(from, to) }\n",
        ),
        (
            # append the private helper at the end of the file (before the final `}`)
            "}\n\n"
            "data class ProfitData(",
            "}\n\n"
            "private fun Long.inRange(from: Long?, to: Long?): Boolean {\n"
            "    if (from != null && this < from) return false\n"
            "    if (to != null && this > to) return false\n"
            "    return true\n"
            "}\n\n"
            "data class ProfitData(",
        ),
    ],
    "CalculateBusinessProfitUseCase: range params + helper",
)


# ═══════════════════════════════════════════════════════════════════════════
# 3. ProfitLossViewModel — TimeFilter + job cancellation + range call
# ═══════════════════════════════════════════════════════════════════════════
patch(
    PKG / "features" / "reports" / "presentation" / "ProfitLossViewModel.kt",
    [
        (
            "import com.akari.retailer.features.expense.domain.usecases.CalculateBusinessProfitUseCase\n"
            "import com.akari.retailer.features.expense.domain.usecases.ProfitData\n"
            "import kotlinx.coroutines.flow.MutableStateFlow\n"
            "import kotlinx.coroutines.flow.StateFlow\n"
            "import kotlinx.coroutines.flow.asStateFlow\n"
            "import kotlinx.coroutines.launch\n",

            "import com.akari.retailer.features.expense.domain.usecases.CalculateBusinessProfitUseCase\n"
            "import com.akari.retailer.features.expense.domain.usecases.ProfitData\n"
            "import com.akari.retailer.core.ui.components.TimeFilter\n"
            "import com.akari.retailer.core.ui.components.TimeFilterPreset\n"
            "import kotlinx.coroutines.Job\n"
            "import kotlinx.coroutines.flow.MutableStateFlow\n"
            "import kotlinx.coroutines.flow.StateFlow\n"
            "import kotlinx.coroutines.flow.asStateFlow\n"
            "import kotlinx.coroutines.launch\n",
        ),
        (
            "data class ProfitLossState(\n"
            "    val profitData: ProfitData? = null,\n"
            "    val isLoading: Boolean = true,\n"
            "    val error: String? = null\n"
            ")\n",

            "data class ProfitLossState(\n"
            "    val profitData: ProfitData? = null,\n"
            "    val isLoading: Boolean = true,\n"
            "    val error: String? = null,\n"
            "    val timeFilter: TimeFilter = TimeFilter(preset = TimeFilterPreset.THIS_MONTH)\n"
            ")\n",
        ),
        (
            "    private val _state = MutableStateFlow(ProfitLossState())\n"
            "    val state: StateFlow<ProfitLossState> = _state.asStateFlow()\n"
            "\n"
            "    fun loadData() {\n"
            "        viewModelScope.launch {\n"
            "            _state.value = _state.value.copy(isLoading = true, error = null)\n"
            "            \n"
            "            try {\n"
            "                val result = calculateProfitUseCase.invoke()\n",

            "    private val _state = MutableStateFlow(ProfitLossState())\n"
            "    val state: StateFlow<ProfitLossState> = _state.asStateFlow()\n"
            "\n"
            "    private var loadJob: Job? = null\n"
            "\n"
            "    fun setTimeFilter(filter: TimeFilter) {\n"
            "        _state.value = _state.value.copy(timeFilter = filter)\n"
            "        loadData()\n"
            "    }\n"
            "\n"
            "    fun loadData() {\n"
            "        loadJob?.cancel()\n"
            "        loadJob = viewModelScope.launch {\n"
            "            _state.value = _state.value.copy(isLoading = true, error = null)\n"
            "\n"
            "            try {\n"
            "                val range = _state.value.timeFilter.resolveRange()\n"
            "                val result = calculateProfitUseCase.invoke(\n"
            "                    from = range.first,\n"
            "                    to = range.last\n"
            "                )\n",
        ),
    ],
    "ProfitLossViewModel: TimeFilter + job cancel",
)


# ═══════════════════════════════════════════════════════════════════════════
# 4. ProfitLossScreen — filter button + chip + remove bad retry
# ═══════════════════════════════════════════════════════════════════════════
patch(
    PKG / "features" / "reports" / "presentation" / "ProfitLossScreen.kt",
    [
        (
            "import com.akari.retailer.core.ui.components.AppCard\n"
            "import com.akari.retailer.core.ui.components.AppScreen\n"
            "import com.akari.retailer.core.ui.theme.AppTypography\n"
            "import com.akari.retailer.core.ui.theme.Spacing\n"
            "import com.akari.retailer.features.expense.domain.usecases.ProfitData\n",

            "import com.akari.retailer.core.ui.components.AppCard\n"
            "import com.akari.retailer.core.ui.components.AppScreen\n"
            "import com.akari.retailer.core.ui.components.TimeFilter\n"
            "import com.akari.retailer.core.ui.components.TimeFilterPreset\n"
            "import com.akari.retailer.core.ui.components.TimeFilterSelector\n"
            "import com.akari.retailer.core.ui.theme.AppTypography\n"
            "import com.akari.retailer.core.ui.theme.Spacing\n"
            "import com.akari.retailer.features.expense.domain.usecases.ProfitData\n",
        ),
        (
            "    val state by viewModel.state.collectAsState()\n"
            "    \n"
            "    LaunchedEffect(Unit) {\n"
            "        viewModel.loadData()\n"
            "    }\n"
            "\n"
            "    AppScreen(\n"
            "        title = stringResource(R.string.profit_loss_title),\n"
            "        showBackButton = true,\n"
            "        onBackClick = onBack\n"
            "    ) {",

            "    val state by viewModel.state.collectAsState()\n"
            "    var showTimeFilterDialog by remember { mutableStateOf(false) }\n"
            "\n"
            "    LaunchedEffect(Unit) {\n"
            "        viewModel.loadData()\n"
            "    }\n"
            "\n"
            "    if (showTimeFilterDialog) {\n"
            "        AlertDialog(\n"
            "            onDismissRequest = { showTimeFilterDialog = false },\n"
            "            title = { Text(stringResource(R.string.date_range)) },\n"
            "            text = {\n"
            "                TimeFilterSelector(\n"
            "                    filter = state.timeFilter,\n"
            "                    onFilterChange = { viewModel.setTimeFilter(it) }\n"
            "                )\n"
            "            },\n"
            "            confirmButton = {\n"
            "                TextButton(onClick = { showTimeFilterDialog = false }) {\n"
            "                    Text(stringResource(R.string.ok))\n"
            "                }\n"
            "            }\n"
            "        )\n"
            "    }\n"
            "\n"
            "    AppScreen(\n"
            "        title = stringResource(R.string.profit_loss_title),\n"
            "        showBackButton = true,\n"
            "        onBackClick = onBack,\n"
            "        showFilterButton = true,\n"
            "        onFilterClick = { showTimeFilterDialog = true }\n"
            "    ) {",
        ),
        (
            # remove the retry button inside the loading block
            "                        TextButton(\n"
            "                            onClick = { viewModel.loadData() },\n"
            "                            modifier = Modifier.padding(top = Spacing.medium)\n"
            "                        ) {\n"
            "                            Text(stringResource(R.string.retry))\n"
            "                        }\n"
            "                    }\n"
            "                }\n"
            "                return@Column\n"
            "            }\n"
            "\n"
            "            if (state.error != null) {",

            "                    }\n"
            "                }\n"
            "                return@Column\n"
            "            }\n"
            "\n"
            "            if (state.error != null) {",
        ),
        (
            # inject active-filter chip before the three summary cards
            "            state.profitData?.let { data ->\n"
            "                // Three Summary Cards\n",

            "            state.profitData?.let { data ->\n"
            "                Card(\n"
            "                    modifier = Modifier\n"
            "                        .fillMaxWidth()\n"
            "                        .padding(bottom = Spacing.medium),\n"
            "                    colors = CardDefaults.cardColors(\n"
            "                        containerColor = MaterialTheme.colorScheme.secondaryContainer\n"
            "                    )\n"
            "                ) {\n"
            "                    Row(\n"
            "                        modifier = Modifier\n"
            "                            .fillMaxWidth()\n"
            "                            .padding(Spacing.medium),\n"
            "                        horizontalArrangement = Arrangement.SpaceBetween,\n"
            "                        verticalAlignment = Alignment.CenterVertically\n"
            "                    ) {\n"
            "                        Text(\n"
            "                            text = \"📅 \" + state.timeFilter.label.ifEmpty { stringResource(R.string.this_month) },\n"
            "                            style = AppTypography.body\n"
            "                        )\n"
            "                        TextButton(\n"
            "                            onClick = {\n"
            "                                viewModel.setTimeFilter(\n"
            "                                    TimeFilter(preset = TimeFilterPreset.THIS_MONTH)\n"
            "                                )\n"
            "                            }\n"
            "                        ) {\n"
            "                            Text(stringResource(R.string.clear))\n"
            "                        }\n"
            "                    }\n"
            "                }\n"
            "\n"
            "                // Three Summary Cards\n",
        ),
    ],
    "ProfitLossScreen: filter UI + chip",
)


# ═══════════════════════════════════════════════════════════════════════════
# 5. SaleEntryViewModel — fix pendingOverpayment race
# ═══════════════════════════════════════════════════════════════════════════
patch(
    PKG / "features" / "sales" / "presentation" / "entry" / "SaleEntryViewModel.kt",
    [
        (
            # saveSale guard
            "    private fun saveSale(cashierId: String = \"default\") {\n"
            "        val currentState = _state.value\n"
            "        val items = stateManager.getItems(currentState)\n"
            "\n"
            "        if (items.isEmpty()) {",

            "    private fun saveSale(cashierId: String = \"default\") {\n"
            "        val currentState = _state.value\n"
            "\n"
            "        // ── Re-entrancy guard ──\n"
            "        // Reject Save taps while a previous save is in flight. Prevents\n"
            "        // double-finalization (which would double-credit an overpayment).\n"
            "        if (currentState.isSaving) return\n"
            "\n"
            "        val items = stateManager.getItems(currentState)\n"
            "\n"
            "        if (items.isEmpty()) {",
        ),
        (
            # executeSaleSave guard + consume overpayment before await
            "    private fun executeSaleSave(cashierId: String) {\n"
            "        val currentState = _state.value\n"
            "        val items = stateManager.getItems(currentState)\n"
            "        val total = items.sum()\n",

            "    private fun executeSaleSave(cashierId: String) {\n"
            "        val currentState = _state.value\n"
            "\n"
            "        // ── Re-entrancy guard (also covers confirmOverpayment path) ──\n"
            "        if (currentState.isSaving) return\n"
            "\n"
            "        val items = stateManager.getItems(currentState)\n"
            "        val total = items.sum()\n",
        ),
        (
            "        _state.value = currentState.copy(isSaving = true, error = null)\n"
            "\n"
            "        viewModelScope.launch {\n"
            "            try {\n"
            "                val sale = Sale(\n"
            "                    items = saleItems,\n"
            "                    total = total,\n"
            "                    payments = allPaymentEntries,\n"
            "                    cashierId = cashierId\n"
            "                )\n"
            "\n"
            "                val result = saleFinalizer.finalizeSale(sale, pendingOverpayment)\n"
            "                pendingOverpayment = null\n",

            "        _state.value = currentState.copy(isSaving = true, error = null)\n"
            "\n"
            "        // ── Consume pendingOverpayment NOW, before suspending ──\n"
            "        // If we cleared it after `finalizeSale`, a second concurrent save\n"
            "        // would read the same pendingOverpayment and double-credit the customer.\n"
            "        val overpaymentToApply = pendingOverpayment\n"
            "        pendingOverpayment = null\n"
            "\n"
            "        viewModelScope.launch {\n"
            "            try {\n"
            "                val sale = Sale(\n"
            "                    items = saleItems,\n"
            "                    total = total,\n"
            "                    payments = allPaymentEntries,\n"
            "                    cashierId = cashierId\n"
            "                )\n"
            "\n"
            "                val result = saleFinalizer.finalizeSale(sale, overpaymentToApply)\n",
        ),
    ],
    "SaleEntryViewModel: pendingOverpayment race fix",
)


# ═══════════════════════════════════════════════════════════════════════════
# 6. SaleEntryScreen — refuse taps while saving
# ═══════════════════════════════════════════════════════════════════════════
patch(
    PKG / "features" / "sales" / "presentation" / "entry" / "SaleEntryScreen.kt",
    [
        (
            "                ExtendedFloatingActionButton(\n"
            "                    onClick = {\n"
            "                        focusManager.clearFocus()\n"
            "                        viewModel.handleEvent(SaleEntryEvent.SaveSale)\n"
            "                    },",

            "                ExtendedFloatingActionButton(\n"
            "                    onClick = {\n"
            "                        if (!state.isSaving) {\n"
            "                            focusManager.clearFocus()\n"
            "                            viewModel.handleEvent(SaleEntryEvent.SaveSale)\n"
            "                        }\n"
            "                    },",
        ),
    ],
    "SaleEntryScreen: disable FAB during save",
)


# ═══════════════════════════════════════════════════════════════════════════
# Report
# ═══════════════════════════════════════════════════════════════════════════
print()
print("═══ PATCH REPORT ═══")
print()
if PATCHED:
    print(f"✅ Patched ({len(PATCHED)}):")
    for line in PATCHED:
        print(f"   • {line}")
if SKIPPED:
    print()
    print(f"⏭  Skipped ({len(SKIPPED)}):")
    for line in SKIPPED:
        print(f"   • {line}")
if FAILED:
    print()
    print(f"❌ FAILED ({len(FAILED)}):")
    for line in FAILED:
        print(f"   • {line}")
    print()
    print("   → A patch failed. The file may have diverged from the expected anchor.")
    print("     Restore from backup and inspect manually.")
    sys.exit(1)

print()
print("Done.")
