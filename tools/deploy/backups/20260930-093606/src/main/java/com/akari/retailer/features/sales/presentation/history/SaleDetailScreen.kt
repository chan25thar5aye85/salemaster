package com.akari.retailer.features.sales.presentation.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.akari.retailer.R
import com.akari.retailer.RetailApplication
import com.akari.retailer.core.ui.components.AppCard
import com.akari.retailer.core.ui.components.AppPrimaryButton
import com.akari.retailer.core.ui.components.AppScreen
import com.akari.retailer.core.ui.theme.AppTypography
import com.akari.retailer.core.ui.theme.Spacing
import com.akari.retailer.core.utils.MoneyFormatter
import com.akari.retailer.features.money.domain.models.CreditAccount
import com.akari.retailer.navigation.Routes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaleDetailScreen(
    navController: NavController,
    saleId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as RetailApplication

    val viewModel: SaleDetailViewModel = viewModel(
        factory = SaleDetailViewModelFactory(
            application.container.saleRepository,
            application.container.moneyAccountRepository,
            application.container.customerRepository,
            application.container.saleFinalizer,
            saleId
        )
    )

    val state by viewModel.state.collectAsState()
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.deleteSuccess) {
        if (state.deleteSuccess) {
            onBack()
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_sale)) },
            text = { Text(stringResource(R.string.delete_confirmation)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSale()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    AppScreen(
        title = stringResource(R.string.sale_detail),
        showBackButton = true,
        onBackClick = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                state.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("❌", fontSize = 40.sp)
                            Text(
                                text = state.error!!,
                                style = AppTypography.body,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                state.sale != null -> {
                    val sale = state.sale!!

                    // ── Header card: total + date ──
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.large)
                        ) {
                            Text(
                                text = stringResource(R.string.total),
                                style = AppTypography.body,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                            )
                            Text(
                                text = MoneyFormatter.formatTotal(sale.total),
                                style = AppTypography.total.copy(fontSize = 32.sp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "🕐 ${dateFormat.format(Date(sale.timestamp))}",
                                style = AppTypography.small,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.medium))

                    // ── Items card ──
                    AppCard {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "🛒 ${stringResource(R.string.items)}",
                                style = AppTypography.title,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = Spacing.small)
                            )
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            )
                            Spacer(modifier = Modifier.height(Spacing.small))

                            if (sale.items.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.no_items_in_order),
                                    style = AppTypography.small,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            } else {
                                sale.items.forEachIndexed { index, item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Item ${index + 1}  ×${item.quantity}",
                                            style = AppTypography.body
                                        )
                                        Text(
                                            text = MoneyFormatter.formatTotal(item.total),
                                            style = AppTypography.body,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.medium))

                    // ── Notes card (only if present) ──
                    if (sale.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(Spacing.medium))
                        AppCard {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "📝 ${stringResource(R.string.notes)}",
                                    style = AppTypography.title,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = Spacing.small)
                                )
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                )
                                Spacer(modifier = Modifier.height(Spacing.small))
                                Text(
                                    text = sale.notes,
                                    style = AppTypography.body,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    // ── Payments card ──
                    AppCard {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "💳 ${stringResource(R.string.payment)}",
                                style = AppTypography.title,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = Spacing.small)
                            )
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            )
                            Spacer(modifier = Modifier.height(Spacing.small))

                            if (sale.payments.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.no_transactions),
                                    style = AppTypography.small,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            } else {
                                sale.payments.forEach { payment ->
                                    val isCredit = payment.accountId == CreditAccount.ID
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = viewModel.paymentLabel(payment.accountId, payment.customerId),
                                            style = AppTypography.body,
                                            color = if (isCredit)
                                                MaterialTheme.colorScheme.error
                                            else
                                                MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = MoneyFormatter.formatTotal(payment.amount),
                                            style = AppTypography.body,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCredit)
                                                MaterialTheme.colorScheme.error
                                            else
                                                MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.large))

                    // ── Delete button ──
                    Button(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(stringResource(R.string.delete_sale))
                    }

                    Spacer(modifier = Modifier.height(Spacing.xxlarge))
                }
            }
        }
    }
}
